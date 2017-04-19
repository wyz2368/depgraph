package agent;

import graph.Node;

import java.util.ArrayList;
import java.util.List;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class MinCutDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	
	public MinCutDefender(final double maxNumRes, final double minNumRes, final double numResRatio) {
		super(DefenderType.MINCUT);
		if (maxNumRes < minNumRes || minNumRes < 0 || !isProb(numResRatio)) {
			throw new IllegalArgumentException();
		}
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = numResRatio;
	}
	
	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph,
		final int curTimeStep,
		final int numTimeStep,
		final DefenderBelief dBelief,
		final RandomGenerator rng
	) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		List<Node> dCandidateNodeList = new ArrayList<Node>(depGraph.getMinCut());
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
		UniformIntegerDistribution rnd = new UniformIntegerDistribution(rng, 0, dCandidateNodeList.size() - 1);
		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);	
	}
	
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph,
		final DefenderBelief currentBelief, final DefenderAction dAction,
		final DefenderObservation dObservation, final int curTimeStep, final int numTimeStep,
		final RandomGenerator rng) {
		return null;
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
