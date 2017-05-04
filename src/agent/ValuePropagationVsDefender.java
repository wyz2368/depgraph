package agent;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import agent.RandomWalkAttacker.RandomWalkTuple;

public final class ValuePropagationVsDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	private double logisParam;
	private double discFact;
	private double thres; // to remove game state from belief
	
	// defender's assumption about attacker
	private double qrParam; 
	private int maxNumAttCandidate; 
	private int minNumAttCandidate;
	private double numAttCandidateRatio;
	private double numAttCandStdev;
	
	// number of simulation to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 20;
	private static final int DEFAULT_NUM_ACTION_SAMPLE = 20;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_ACTION_SAMPLE;
	
	private boolean isTopo = true;
	
	/*****************************************************************************************
	 * 
	 * @param maxNumRes
	 * @param minNumRes
	 * @param numResRatio
	 * @param logisParam
	 * @param discFact
	 * @param thres
	 * @param qrParam
	 * @param maxNumAttCandidate
	 * @param minNumAttCandidate
	 * @param numAttCandidateRatio
	 *****************************************************************************************/
	public ValuePropagationVsDefender(final double maxNumRes, final double minNumRes, final double numResRatio,
		final double logisParam, final double discFact, final double thres,
		final double qrParam, final double maxNumAttCandidate, final double minNumAttCandidate,
		final double numAttCandidateRatio, final double numAttCandStdev,
		final double isTopo) {
		super(DefenderType.vsVALUE_PROPAGATION);
		if (
			discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
			|| minNumRes < 1 || minNumRes > maxNumRes || !isProb(numResRatio)
			|| discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio) || numAttCandStdev < 0.0
			|| qrParam < 0.0
		) {
			throw new IllegalArgumentException();
		}
		
		this.discFact = discFact;
		this.thres = thres;
		
		this.qrParam = qrParam;
		this.maxNumAttCandidate = (int) maxNumAttCandidate;
		this.minNumAttCandidate = (int) minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
		this.numAttCandStdev = numAttCandStdev;
		
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		
		final double tolerance = 0.00001;
		this.isTopo = (Math.abs(isTopo - 1.0) < tolerance);
	}

	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph, final int curTimeStep, final int numTimeStep,
		final DefenderBelief dBelief, final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		// assumption about the attacker
		final Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact, this.numAttCandStdev);

		if (this.isTopo) {
			return sampleActionTopo(
				depGraph, 
				curTimeStep, 
				numTimeStep, 
				dBelief, 
				rng, 
				attacker);
		}

		final int defaultNumRWSample = 10;
		int numRWSample = defaultNumRWSample;
		return sampleActionRandomWalk(
			depGraph, 
			curTimeStep, 
			numTimeStep, 
			dBelief, 
			rng,
			attacker, 
			numRWSample);
	}
	
	@Override
	public DefenderBelief updateBelief(
		final DependencyGraph depGraph,
		final DefenderBelief dBelief, 
		final DefenderAction dAction,
		final DefenderObservation dObservation, 
		final int curTimeStep, 
		final int numTimeStep,
		final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null
			|| dObservation == null || dAction == null
		) {
			throw new IllegalArgumentException();
		}
		
		final Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact, this.numAttCandStdev);
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
	
	public DefenderAction sampleActionTopo(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep,
		final DefenderBelief dBelief, 
		final RandomGenerator rng,
		final Attacker attacker
	) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null || attacker == null) {
			throw new IllegalArgumentException();
		}
		
		final Map<Node, Double> dValueMap = computeCandidateValueTopo(depGraph, dBelief, curTimeStep, numTimeStep
			, this.discFact, rng, attacker, this.numAttActionSample);
		final List<Node> dCandidateNodeList = new ArrayList<Node>();
		final double[] candidateValue = new double[dValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		final int totalNumCandidate = dValueMap.size();
		
		// Compute probability to choose each node
		final double[] probabilities = computeCandidateProb(totalNumCandidate, candidateValue, this.logisParam);

		// Only keep candidates with high probability
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
		final int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		final EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);
	}
	
	public DefenderAction sampleActionRandomWalk(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng,
		final Attacker attacker, // this is value-propagation attacker
		final int numRWSample) { // this is for the random-walk process 
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null || attacker == null || numRWSample < 1) {
			throw new IllegalArgumentException();
		}

		// Used for storing true game state of the game
		final GameState savedGameState = depGraph.getGameState();

		// possible defender actions to take
		final DefenderAction[] candidates = new DefenderAction[dBelief.getGameStateMap().size()];

		final RandomWalkTuple[][][] rwTuplesLists = new RandomWalkTuple[dBelief.getGameStateMap().size()][][];
		final AttackerAction[][] attActionLists = new AttackerAction[dBelief.getGameStateMap().size()][];
		final double[][] attProbs = new double[dBelief.getGameStateMap().size()][];
		int idx = 0;
		// iterate over current belief of the defender
		for (final Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			final GameState curGameState = entry.getKey();
			depGraph.setState(curGameState); // for each possible state

			// sample actions from the attacker, given this current game state from the belief
			final List<AttackerAction> attActionList = attacker.sampleAction(
				depGraph,
				curTimeStep,
				numTimeStep,
				rng,
				this.numAttActionSample, 
				false);
			final AttackerAction[] attActions =
				attActionList.toArray(new AttackerAction[attActionList.size()]);

			final RandomWalkTuple[][] rwTuplesListSample =
				new RandomWalkTuple[numRWSample][]; // list of all random walk tuples sampled
			final RandomWalkAttacker rwAttacker = new RandomWalkAttacker(numRWSample, this.qrParam, this.discFact);
			for (int i = 0; i < numRWSample; i++) {
				final RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(
					depGraph, 
					curTimeStep, 
					rng); // sample random walk
				rwTuplesListSample[i] = rwTuples;
			}
			final RandomWalkTuple[][] rwTuplesList =
				new RandomWalkTuple[attActions.length][]; // list of all random walk tuples sampled
			for (int attActionIndex = 0; attActionIndex < attActions.length; attActionIndex++) {
				final AttackerAction attAction = attActions[attActionIndex];
				double maxValue = Double.NEGATIVE_INFINITY;
				int maxIdx = -1;
				for (int rwSampleIndex = 0; rwSampleIndex < numRWSample; rwSampleIndex++) {
					final RandomWalkTuple[] rwTuples = rwTuplesListSample[rwSampleIndex];
					final double curValue = RandomWalkAttacker.computeAttackerValue(
						depGraph, attAction, rwTuples, 
						this.discFact, curTimeStep, numTimeStep);
					if (maxValue < curValue) {
						maxValue = curValue;
						maxIdx = rwSampleIndex;
					}
				}
				if (maxIdx == -1) {
					throw new IllegalStateException();
				}
				rwTuplesList[attActionIndex] = rwTuplesListSample[maxIdx];
			}
			
			// values of corresponding action of the attacker
			final double[] attValue = new double[attActions.length];
			// attack probability
			double[] attProb = Attacker.computeCandidateProb(attValue, this.qrParam);
			DefenderAction defAction = new DefenderAction();
			RandomWalkVsDefender.greedyAction(
				depGraph, // greedy defense with respect to each possible game state
				rwTuplesList, 
				attActions, 
				attProb, 
				defAction, // this is outcome
				curTimeStep, 
				numTimeStep,
				this.discFact);

			rwTuplesLists[idx] = rwTuplesList;
			attActionLists[idx] = attActions;
			attProbs[idx] = attProb;
			candidates[idx] = defAction;
			idx++;
		}
		depGraph.setState(savedGameState);
		
		final double[] candidateValues = new double[dBelief.getGameStateMap().size()];
		for (int i = 0; i < candidateValues.length; i++) {
			candidateValues[i] =
				RandomWalkVsDefender.computeDValue(depGraph, dBelief, rwTuplesLists, attActionLists, attProbs
					, candidates[i], curTimeStep, numTimeStep, this.discFact);
		}
		
		// probability for each possible candidate action for the defender
		final double[] probabilities =
			computeCandidateProb(dBelief.getGameStateMap().size(), candidateValues, this.logisParam);
		
		// Start sampling
		final int[] nodeIndexes = Attacker.getIndexArray(dBelief.getGameStateMap().size());
		final EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		final int sampleIdx = rnd.sample();
		return candidates[sampleIdx];
	}
	
	public static Map<Node, Double> computeCandidateValueTopo(
		final DependencyGraph depGraph,
		final DefenderBelief dBelief,
		final int curTimeStep,
		final int numTimeStep,
		final double discountFactor,
		final RandomGenerator rng,
		final Attacker attacker,
		final int numAttActionSample) {
		if (depGraph == null || dBelief == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0 || rng == null) {
			throw new IllegalArgumentException();
		}
		
		// Used for storing true game state of the game
		final GameState savedGameState = depGraph.getGameState();
		
		final Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		// iterate over current belief of the defender
		for (Entry<GameState, Double> entry: dBelief.getGameStateMap().entrySet()) {
			final GameState gameState = entry.getKey();
			final Double curStateProb = entry.getValue();

			depGraph.setState(gameState); // for each possible state
			final List<AttackerAction> attActionList = attacker.sampleAction(
				depGraph, 
				curTimeStep, 
				numTimeStep, 
				rng, 
				numAttActionSample, 
				false); // Sample attacker actions
			final Map<Node, Double> curDValueMap = computeCandidateValueTopo(
				depGraph, 
				attActionList, 
				curTimeStep, 
				numTimeStep, 
				discountFactor);
			for (Entry<Node, Double> dEntry : curDValueMap.entrySet()) {
				final Node node = dEntry.getKey();
				final Double value = dEntry.getValue();
				
				Double curDValue = dValueMap.get(node);
				if (curDValue == null) {
					curDValue = value * curStateProb;
				} else {
					curDValue += value * curStateProb;
				}
				dValueMap.put(node, curDValue);
			}
		}
		for (Entry<Node, Double> entry: dValueMap.entrySet()) {
			final Node node = entry.getKey();
			Double value = entry.getValue();
			if (value == Double.POSITIVE_INFINITY) {
				value = 0.0;
			}
			// no need to discount, as discount is the same factor for all nodes
			entry.setValue(-value + node.getDCost());
		}
		
		depGraph.setState(savedGameState);
		return dValueMap;
	}
	
	public static Map<Node, Double> computeCandidateValueTopo(
		final DependencyGraph depGraph,
		final List<AttackerAction> attActionList,
		final int curTimeStep,
		final int numTimeStep,
		final double discountFactor) {
		if (depGraph == null || attActionList == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0) {
			throw new IllegalArgumentException();
		}
		
		final List<Node> targets = new ArrayList<Node>(depGraph.getTargetSet());

		final double[][][] r =
			new double[targets.size()][numTimeStep - curTimeStep + 1][depGraph.vertexSet().size()];
		for (int i = 0; i < targets.size(); i++) {
			for (int j = 0; j < numTimeStep - curTimeStep + 1; j++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					r[i][j][k] = Double.POSITIVE_INFINITY;
				}
			}
		}

		for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
			final Node target = targets.get(targetIndex);
			if (target.getState() == NodeState.INACTIVE) { // for non-active targets only
				r[targetIndex][0][target.getId() - 1] = target.getDPenalty();
			}
		}

		// Compute values for each node in the graph
		final Node[] topoOrder = Attacker.getTopoOrder(depGraph);
		
		// iterate over nodes in reverse topological order (i.e., leaf node first)
		for (int topoIndex = depGraph.vertexSet().size() - 1; topoIndex >= 0; topoIndex--) {
			final Node curNode = topoOrder[topoIndex];
			final Set<Edge> curOutEdges = depGraph.outgoingEdgesOf(curNode);
			for (final Edge outEdge: curOutEdges) {
				final Node childNode = outEdge.gettarget();
				if (childNode.getState() == NodeState.ACTIVE) {
					continue;
				}
				for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
					if (targets.get(targetIndex).getState() == NodeState.INACTIVE) {
						for (int timeIndex = 1; timeIndex <= numTimeStep - curTimeStep; timeIndex++) {
							double rHat = 0.0;
							
							if (childNode.getActivationType() == NodeActivationType.AND) {
								// don't consider node cost
								rHat = childNode.getActProb() * r[targetIndex][timeIndex - 1][childNode.getId() - 1];
								// don't normalize for the multiple inactive parents of the AND node
							} else {
								// don't consider edge cost
								rHat = outEdge.getActProb() * r[targetIndex][timeIndex - 1][childNode.getId() - 1];
							}
							
							// take the minimum payoff (worst case)
							if (r[targetIndex][timeIndex][curNode.getId() - 1] > discountFactor * rHat) {
								// find the worst-case scenario
								r[targetIndex][timeIndex][curNode.getId() - 1] = discountFactor * rHat;
							}
						}
					}
				}
			}
		}
		
		double[] rAggregate = new double[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			rAggregate[i] = Double.POSITIVE_INFINITY;
		}
		for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
			if (targets.get(targetIndex).getState() == NodeState.ACTIVE) {
				continue;
			}
			for (int timeIndex = 0; timeIndex <= numTimeStep - curTimeStep; timeIndex++) {
				for (int nodeIndex = 0; nodeIndex < depGraph.vertexSet().size(); nodeIndex++) {
					// take the minimum (worst) value here
					if (rAggregate[nodeIndex] > r[targetIndex][timeIndex][nodeIndex]) {
						rAggregate[nodeIndex] = r[targetIndex][timeIndex][nodeIndex];
					}
				}
			}
		}
		
		final Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		for (final AttackerAction attAction : attActionList) {
			for (final Entry<Node, Set<Edge>> attEntry : attAction.getActionCopy().entrySet()) {
				final Node attNode = attEntry.getKey();
				final Set<Edge> attEdges = attEntry.getValue();

				double actProb = 1.0;
				if (attNode.getActivationType() == NodeActivationType.OR) {
					for (final Edge attEdge: attEdges) {
						actProb *= (1 - attEdge.getActProb());
					}
					actProb = 1 - actProb;
				} else {
					actProb *= attNode.getActProb();
				}

				final double addedDValue = rAggregate[attNode.getId() - 1] * actProb;
				Double curDValue = dValueMap.get(attNode);
				if (curDValue == null) { // if this is new
					curDValue = addedDValue;
				} else {
					curDValue += addedDValue;
				}
				dValueMap.put(attNode, curDValue);
			}
		}
		for (final Entry<Node, Double> entry : dValueMap.entrySet()) {
			entry.setValue(entry.getValue() / attActionList.size());
		}
		for (final Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE) { // Examine active targets
				double dValue = target.getDPenalty();
				if (rAggregate[target.getId() - 1] != Double.POSITIVE_INFINITY) {
					dValue += rAggregate[target.getId() - 1];
				}
				dValueMap.put(target, dValue);
			}
		}
		return dValueMap;
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

	public double getThres() {
		return this.thres;
	}

	public double getQrParam() {
		return this.qrParam;
	}

	public int getMaxNumAttCandidate() {
		return this.maxNumAttCandidate;
	}

	public int getMinNumAttCandidate() {
		return this.minNumAttCandidate;
	}

	public double getNumAttCandidateRatio() {
		return this.numAttCandidateRatio;
	}

	public double getNumAttCandStdev() {
		return this.numAttCandStdev;
	}

	public int getNumStateSample() {
		return this.numStateSample;
	}

	public int getNumAttActionSample() {
		return this.numAttActionSample;
	}

	@Override
	public String toString() {
		return "ValuePropagationVsDefender [maxNumRes=" + this.maxNumRes
			+ ", minNumRes=" + this.minNumRes + ", numResRatio=" + this.numResRatio
			+ ", logisParam=" + this.logisParam + ", discFact=" + this.discFact
			+ ", thres=" + this.thres + ", qrParam=" + this.qrParam
			+ ", maxNumAttCandidate=" + this.maxNumAttCandidate
			+ ", minNumAttCandidate=" + this.minNumAttCandidate
			+ ", numAttCandidateRatio=" + this.numAttCandidateRatio
			+ ", numAttCandStdev=" + this.numAttCandStdev + ", numStateSample="
			+ this.numStateSample + ", numAttActionSample=" + this.numAttActionSample
			+ "]";
	}
}