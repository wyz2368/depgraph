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
	private static final int DEFAULT_NUM_STATE_SAMPLE = 30;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE;
	
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
		final double numAttCandidateRatio, final double numAttCandStdev) {
		super(DefenderType.vsVALUE_PROPAGATION);
		if (
			discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
			|| minNumRes < 1 || minNumRes > maxNumRes || !isProb(numResRatio)
			|| discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
			|| numAttCandStdev < 0.0
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
	}

	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph, final int curTimeStep, final int numTimeStep,
		final DefenderBelief dBelief, final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		// assumption about the attacker
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact, this.numAttCandStdev);
		Map<Node, Double> dValueMap = computeCandidateValueTopo(depGraph, dBelief, curTimeStep, numTimeStep
			, this.discFact, rng, attacker, this.numAttActionSample);
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
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);
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
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
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
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		// iterate over current belief of the defender
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey();
			Double curStateProb = entry.getValue();
			
			depGraph.setState(gameState); // for each possible state
			List<AttackerAction> attActionList = attacker.sampleAction(
				depGraph, 
				curTimeStep, 
				numTimeStep, 
				rng, 
				numAttActionSample, 
				false); // Sample attacker actions
			Map<Node, Double> curDValueMap = computeCandidateValueTopo(
				depGraph, 
				attActionList, 
				curTimeStep, 
				numTimeStep, 
				discountFactor);
			for (Entry<Node, Double> dEntry : curDValueMap.entrySet()) {
				Node node = dEntry.getKey();
				Double value = dEntry.getValue();
				
				Double curDValue = dValueMap.get(node);
				if (curDValue == null) {
					curDValue = value * curStateProb;
				} else {
					curDValue += value * curStateProb;
				}
				dValueMap.put(node, curDValue);
			}
		}
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			Node node = entry.getKey();
			Double value = entry.getValue();
			if (value == Double.POSITIVE_INFINITY) {
				value = 0.0;
			}
			entry.setValue((-value + node.getDCost()) * Math.pow(discountFactor, curTimeStep - 1));
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
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		// Compute values for each node in the graph
		List<Node> targetList = new ArrayList<Node>(depGraph.getTargetSet()); // list of targets
		Node[] topoOrder = new Node[depGraph.vertexSet().size()]; // topological order of nodes in the graph

		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		
		double[][][] r = new double[targetList.size()][numTimeStep - curTimeStep + 1][depGraph.vertexSet().size()];
		for (int i = 0; i < targetList.size(); i++) {
			for (int j = 0; j <= numTimeStep - curTimeStep; j++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					r[i][j][k] = Double.POSITIVE_INFINITY;
				}
			}
		}
		for (int i = 0; i < targetList.size(); i++) {
			Node node = targetList.get(i);
			if (node.getState() != NodeState.ACTIVE) { // for non-active targets only
				r[i][0][node.getId() - 1] = node.getDPenalty();
			}
		}
		for (int k = depGraph.vertexSet().size() - 1; k >= 0; k--) { // starting propagate values for the defender 
			Node node = topoOrder[k];

			Set<Edge> edgeSet = depGraph.outgoingEdgesOf(node);
			if (edgeSet != null && !edgeSet.isEmpty()) { // if non-leaf
				for (Edge edge : edgeSet) {
					Node postNode = edge.gettarget();
					if (postNode.getState() != NodeState.ACTIVE) { // consider non-active postconditions only
						for (int i = 0; i < targetList.size(); i++) {
							if (targetList.get(i).getState() != NodeState.ACTIVE) {
								for (int j = 1; j <= numTimeStep - curTimeStep; j++) {
									double rHat = 0.0;
									if (postNode.getActivationType() == NodeActivationType.OR) {
										rHat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
									} else {
										rHat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
									}
									if (r[i][j][node.getId() - 1] > discountFactor * rHat) {
										// find the worst case scenario
										r[i][j][node.getId() - 1] = discountFactor * rHat;
									}
								}
							}
						}
					}
				}
			}
		
		}
		// Min of value for candidates
		double[] rSum = new double[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			rSum[i] = Double.POSITIVE_INFINITY;
		}
		for (int i = 0; i < targetList.size(); i++) {
			if (targetList.get(i).getState() != NodeState.ACTIVE) {
				for (int j = 0; j <= numTimeStep - curTimeStep; j++) {
					for (int k = 0; k < depGraph.vertexSet().size(); k++) {
						if (rSum[k] > r[i][j][k]) {
							rSum[k] = r[i][j][k];
						}
					}
				}
			}
		}
		
		/*****************************************************************************************/
		for (AttackerAction attAction : attActionList) {
			for (Entry<Node, Set<Edge>> attEntry : attAction.getActionCopy().entrySet()) {
				Node node = attEntry.getKey();
				Set<Edge> edgeSet = attEntry.getValue();
				
				double addedDValue = rSum[node.getId() - 1];
				double actProb = 1.0;
				if (node.getActivationType() == NodeActivationType.OR) {
					for (Edge edge : edgeSet) {
						actProb *= (1 - edge.getActProb());
					}
					actProb = 1 - actProb;
				} else {
					actProb *= node.getActProb();
				}
				addedDValue *= actProb;
				
				Double curDValue = dValueMap.get(node);
				if (curDValue == null) { // if this is new
					curDValue = addedDValue;
				} else {
					curDValue += addedDValue;
				}
				dValueMap.put(node, curDValue);
			}
		}
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			double value = entry.getValue();
			entry.setValue(value / attActionList.size());
		}
		for (Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE) { // Examine active targets
				double dValue = target.getDPenalty();
				if (rSum[target.getId() - 1] != Double.POSITIVE_INFINITY) {
					dValue += rSum[target.getId() - 1];
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

	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
