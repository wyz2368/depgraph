package agent;

import java.util.ArrayList;
import java.util.List;

import graph.Node;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class GoalOnlyDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	private double logisParam;
	private double discFact;
	public GoalOnlyDefender(final double maxNumRes, final double minNumRes, final double numResRatio, final double logisParam, final double discFact) {
		super(DefenderType.GOAL_ONLY);
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		this.discFact = discFact;
	}
	double[] computeCandidateProb(final int totalNumCandidate, final double[] candidateValue) {
		//Normalize candidate value
		double minValue = Double.POSITIVE_INFINITY;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < totalNumCandidate; i++) {
			if (minValue > candidateValue[i]) {
				minValue = candidateValue[i];
			}
			if (maxValue < candidateValue[i]) {
				maxValue = candidateValue[i];
			}
		}
		if (maxValue > minValue) {
			for (int i = 0; i < totalNumCandidate; i++) {
				candidateValue[i] = (candidateValue[i] - minValue) / (maxValue - minValue);
			}
		} else {
			for (int i = 0; i < totalNumCandidate; i++) {
				candidateValue[i] = 0.0;
			}
		}
		
		// Compute probability
		double[] probabilities = new double[totalNumCandidate];
		int[] nodeList = new int[totalNumCandidate];
		double sumProb = 0.0;
		for (int i = 0; i < totalNumCandidate; i++) {
			nodeList[i] = i;
			probabilities[i] = Math.exp(this.logisParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for (int i = 0; i < totalNumCandidate; i++) {
			probabilities[i] /= sumProb;
		}
		
		return probabilities;
	}
	public static DefenderAction sampleAction(final List<Node> dCandidateNodeList, final int numNodetoProtect,
			final AbstractIntegerDistribution rnd) {
		DefenderAction action = new DefenderAction();
		
		boolean[] isChosen = new boolean[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			isChosen[i] = false;
		}
		int count = 0;
		while (count < numNodetoProtect) {
			int idx = rnd.sample();
			if (!isChosen[idx]) {
				action.addNodetoProtect(dCandidateNodeList.get(idx));
				isChosen[idx] = true;
				count++;
			}
				
		}
		return action;
	}
	
	public static double[] computeCandidateValue(final List<Node> dCandidateNodeList
			, final double discountFactor, final int curTimeStep) {
		double[] candidateValue = new double[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			candidateValue[i] = Math.pow(discountFactor, curTimeStep - 1) 
				* (-dCandidateNodeList.get(i).getDPenalty() + dCandidateNodeList.get(i).getDCost());
		}
		return candidateValue;
	}
	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph,
			final int curTimeStep, final int numTimeStep, final DefenderBelief dBelief, final RandomGenerator rng) {
		List<Node> dCandidateNodeList = new ArrayList<Node>(depGraph.getTargetSet());
		double[] candidateValue = computeCandidateValue(dCandidateNodeList, this.discFact, curTimeStep);
		double[] probabilities = computeCandidateProb(dCandidateNodeList.size(), candidateValue);
		
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		int numNodetoProtect = 0;
		if (dCandidateNodeList.size() < this.minNumRes) {
			numNodetoProtect = dCandidateNodeList.size();
		} else {
			numNodetoProtect = Math.max(this.minNumRes, (int) (this.numResRatio * dCandidateNodeList.size()));
			numNodetoProtect = Math.min(this.maxNumRes, numNodetoProtect);
		}
		if (dCandidateNodeList.size() == 0) {
			return new DefenderAction();
		}
		// Sample nodes
//		System.out.println(numNodetoProtect);
		return sampleAction(dCandidateNodeList, numNodetoProtect, rnd);	
	}
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph,
			final DefenderBelief currentBelief, final DefenderAction dAction,
			final DefenderObservation dObservation, final int curTimeStep, final int numTimeStep,
			final RandomGenerator rng) {
		return null;
	}
}
