package agent;

import java.util.ArrayList;
import java.util.List;

import graph.Node;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class GoalOnlyDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	private double logisParam;
	private double discFact;
	
	public GoalOnlyDefender(
			final double maxNumRes,
			final double minNumRes, 
			final double numResRatio,
			final double logisParam, 
			final double discFact) {
		super(DefenderType.GOAL_ONLY);
		if (maxNumRes < minNumRes || minNumRes < 0 || !isProb(numResRatio)
			|| discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		this.discFact = discFact;
	}
	
	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {
		List<Node> dCandidateNodeList = new ArrayList<Node>(depGraph.getTargetSet());
		double[] candidateValue = computeCandidateValue(dCandidateNodeList, this.discFact, curTimeStep);
		double[] probabilities = computeCandidateProb(dCandidateNodeList.size(), candidateValue, this.logisParam);
		
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
		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);	
	}
	
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph,
		final DefenderBelief currentBelief, final DefenderAction dAction,
		final DefenderObservation dObservation, final int curTimeStep, final int numTimeStep,
		final RandomGenerator rng) {
		return new DefenderBelief(); // an empty belief
	}
	
	public int getMaxNumRes() {
		return this.maxNumRes;
	}

	public int getMinNumRes() {
		return this.minNumRes;
	}

	public double getNumResRatio() {
		return this.numResRatio;
	}

	public double getLogisParam() {
		return this.logisParam;
	}

	public double getDiscFact() {
		return this.discFact;
	}

	@Override
	public String toString() {
		return "GoalOnlyDefender [maxNumRes=" + this.maxNumRes + ", minNumRes="
			+ this.minNumRes + ", numResRatio=" + this.numResRatio + ", logisParam="
			+ this.logisParam + ", discFact=" + this.discFact + "]";
	}

	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
	
	
	private static double[] computeCandidateValue(
			final List<Node> dCandidateNodeList, 
			final double discountFactor, 
			final int curTimeStep) {
		double[] candidateValue = new double[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			candidateValue[i] = Math.pow(discountFactor, curTimeStep - 1) 
				* (-dCandidateNodeList.get(i).getDPenalty() + dCandidateNodeList.get(i).getDCost());
		}
		return candidateValue;
	}
}
