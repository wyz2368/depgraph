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
	private double numCandStdev;
	
	public GoalOnlyDefender(
		final double aMaxNumRes,
		final double aMinNumRes, 
		final double aNumResRatio,
		final double aLogisParam, 
		final double aDiscFact,
		final double aNumCandStdev) {
		super(DefenderType.GOAL_ONLY);
		if (aMaxNumRes < aMinNumRes || aMinNumRes < 0 || !isProb(aNumResRatio)
			|| aDiscFact <= 0.0 || aDiscFact > 1.0
			|| aNumCandStdev < 0.0) {
			throw new IllegalArgumentException();
		}
		this.maxNumRes = (int) aMaxNumRes;
		this.minNumRes = (int) aMinNumRes;
		this.numResRatio = aNumResRatio;
		this.logisParam = aLogisParam;
		this.discFact = aDiscFact;
		this.numCandStdev = aNumCandStdev;
	}
	
	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {
		List<Node> dCandidateNodeList =
			new ArrayList<Node>(depGraph.getTargetSet());
		double[] candidateValue = computeCandidateValue(dCandidateNodeList);
		double[] probabilities = computeCandidateProb(
			dCandidateNodeList.size(), candidateValue, this.logisParam);
		
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd =
			new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		final int goalCount =
			(int) (dCandidateNodeList.size()
				* this.numResRatio + rng.nextGaussian() * this.numCandStdev);
		final int numNodetoProtect =
			Attacker.getActionCount(this.minNumRes, this.maxNumRes,
				dCandidateNodeList.size(), goalCount);
		if (dCandidateNodeList.size() == 0) {
			return new DefenderAction();
		}
		// Sample nodes
		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);	
	}
	
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph,
		final DefenderBelief currentBelief, final DefenderAction dAction,
		final DefenderObservation dObservation,
		final int curTimeStep, final int numTimeStep,
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
	
	public double getNumCandStdev() {
		return this.numCandStdev;
	}

	@Override
	public String toString() {
		return "GoalOnlyDefender [maxNumRes=" + this.maxNumRes + ", minNumRes="
			+ this.minNumRes + ", numResRatio="
			+ this.numResRatio + ", logisParam="
			+ this.logisParam + ", discFact="
			+ this.discFact + ", numCandStdev="
			+ this.numCandStdev + "]";
	}
	
	// TODO no need to multiply by discFact^{timeStepsLeft}, not useful
	private static double[] computeCandidateValue(
		final List<Node> dCandidateNodeList) {
		double[] candidateValue = new double[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			candidateValue[i] = -dCandidateNodeList.get(i).getDPenalty()
				+ dCandidateNodeList.get(i).getDCost();
		}
		return candidateValue;
	}
}
