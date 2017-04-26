package agent;

import graph.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class UniformVsDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	private double logisParam; // Logistic parameter to randomize defense strategies
	private double discFact; // reward discount factor
	private double thres; // to remove game state from belief
	
	private int maxNumSelectACandidate;
	private int minNumSelectACandidate;
	private double numSelectACandidateRatio;
	
	private static final int DEFAULT_NUM_STATE_SAMPLE = 30;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE; // number of states to sample
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE; // number of attack actions to sample
	
	public UniformVsDefender(final double logisParam, final double discFact, final double thres
		, final double maxNumRes, final double minNumRes, final double numResRatio
		, final double maxNumSelectACandidate,
		final double minNumSelectACandidate, final double numSelectACandidateRatio) {
		super(DefenderType.vsUNIFORM);
		if (discFact <= 0.0 || discFact > 1.0 || thres < 0.0 || thres > 1.0
			|| minNumRes < 1 || maxNumRes < minNumRes || numResRatio < 0.0 || numResRatio > 1.0
			|| minNumSelectACandidate < 1 || maxNumSelectACandidate < minNumSelectACandidate
			|| numSelectACandidateRatio < 0.0 || numSelectACandidateRatio > 1.0
		) {
			throw new IllegalArgumentException();
		}
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = (int) numResRatio;
		
		this.maxNumSelectACandidate = (int) maxNumSelectACandidate;
		this.minNumSelectACandidate = (int) minNumSelectACandidate;
		this.numSelectACandidateRatio = numSelectACandidateRatio;
	}
	
	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep
		, final DefenderBelief dBelief
		, final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		Attacker attacker = new UniformAttacker(
			this.maxNumSelectACandidate, this.minNumSelectACandidate, this.numSelectACandidateRatio, 0.0);
		Map<Node, Double> dValueMap = ValuePropagationVsDefender.computeCandidateValueTopo(depGraph, dBelief, 
				curTimeStep, numTimeStep, this.discFact, rng,
				attacker, this.numAttActionSample);
		List<Node> dCandidateNodeList = new ArrayList<Node>();
		double[] candidateValue = new double[dValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		int totalNumCandidate = dValueMap.size();
		
		// Compute probability to choose each node
		double[] probabilities = computeCandidateProb(totalNumCandidate, candidateValue, this.logisParam);

		// Only keep candidates with non-trivial probability
		int numGoodCandidate = 0;
		for (int i = 0; i < totalNumCandidate; i++) {
			if (probabilities[i] >= this.thres) {
				numGoodCandidate++;
			}
		}
		// Compute number of candidates to select
		int numNodetoProtect = 0;
		if (dCandidateNodeList.size() < this.minNumRes) {
			numNodetoProtect = dCandidateNodeList.size();
		} else {
			numNodetoProtect = Math.max(this.minNumRes, (int) (this.numResRatio * dCandidateNodeList.size()));
			numNodetoProtect = Math.min(this.maxNumRes, numNodetoProtect);
		}
		if (numNodetoProtect > numGoodCandidate) {
			numNodetoProtect = numGoodCandidate;
		}
		
		if (numNodetoProtect == 0) { // if there is no candidate
			return new DefenderAction();
		}
		
		// Sampling
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);
	}
	
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph
		, final DefenderBelief dBelief
		, final DefenderAction dAction
		, final DefenderObservation dObservation
		, final int curTimeStep, final int numTimeStep
		, final RandomGenerator rng) {
		if (depGraph == null || dBelief == null || dAction == null || dObservation == null 
			|| curTimeStep < 0 || numTimeStep < curTimeStep || rng == null
		) {
			throw new IllegalArgumentException();
		}
		Attacker attacker = new UniformAttacker(
			this.maxNumSelectACandidate, this.minNumSelectACandidate, this.numSelectACandidateRatio, 0.0);
		return updateBelief(depGraph
				, dBelief
				, dAction
				, dObservation
				, curTimeStep, numTimeStep
				, rng
				, attacker
				, this.numAttActionSample
				, this.numStateSample
				, this.thres); 
	}
	
}
