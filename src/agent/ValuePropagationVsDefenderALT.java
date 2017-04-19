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
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class ValuePropagationVsDefenderALT extends ValuePropVsDefSuper {
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
	
	// number of simulation to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 50;
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
	public ValuePropagationVsDefenderALT(final int maxNumRes, final int minNumRes, final double numResRatio,
		final double logisParam, final double discFact, final double thres,
		final double qrParam, final int maxNumAttCandidate, final int minNumAttCandidate,
		final double numAttCandidateRatio) {
		super(DefenderType.vsVALUE_PROPAGATION, discFact, thres, qrParam,
			maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio);
		if (
			minNumRes < 1 || minNumRes > maxNumRes || !isProb(numResRatio)
			|| discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
		) {
			throw new IllegalArgumentException();
		}
		
		this.maxNumRes = maxNumRes;
		this.minNumRes = minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		
		this.qrParam = qrParam;
		this.maxNumAttCandidate = maxNumAttCandidate;
		this.minNumAttCandidate = minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
	}

	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph, final int curTimeStep, final int numTimeStep,
		final DefenderBelief dBelief, final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		Map<Node, Double> dValueMap = computeCandidateValueTopo(depGraph, dBelief, curTimeStep, numTimeStep
			, this.discFact, rng);
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

		return sampleAction(dCandidateNodeList, numNodetoProtect, rnd);
	}
	
	private Map<Node, Double> computeCandidateValueTopo(final DependencyGraph depGraph
		, final DefenderBelief dBelief
		, final int curTimeStep, final int numTimeStep, final double discountFactor
		, final RandomGenerator rng) {
		if (
			depGraph == null || dBelief == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
		) {
			throw new IllegalArgumentException();
		}
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact); // assumption about the attacker
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		System.out.println("Start defender belief:........");
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			// iterate over current belief of the defender
			GameState gameState = entry.getKey();
			gameState.print();
			System.out.println("Prob: " + entry.getValue());
			Double curStateProb = entry.getValue();
			
			depGraph.setState(gameState); // for each possible state
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // Sample attacker actions
			Map<Node, Double> curDValueMap = computeCandidateValueTopo(depGraph, attActionList
					, curTimeStep, numTimeStep, discountFactor);
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
		System.out.println("End defender belief:........");
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
	
	private static Map<Node, Double> computeCandidateValueTopo(
		final DependencyGraph depGraph, final List<AttackerAction> attActionList,
		final int curTimeStep, final int numTimeStep,
		final double discountFactor) {
		if (
			depGraph == null || attActionList == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
		) {
			throw new IllegalArgumentException();
		}
		// System.out.println("Defender compute candidate value");
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
					if (postNode.getState() != NodeState.ACTIVE) {
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
										r[i][j][node.getId() - 1] = discountFactor * rHat;
									}
								}
							}
						}
					}
				}
			}
		
		}
		// Sum of value for candidates
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
			// attAction.print();
			for (Entry<Node, Set<Edge>> attEntry : attAction.getAction().entrySet()) {
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
			if (target.getState() == NodeState.ACTIVE) {
				// System.out.println("This is active target...........");
				double dValue = target.getDPenalty();
				if (rSum[target.getId() - 1] != Double.POSITIVE_INFINITY) {
					dValue += rSum[target.getId() - 1];
				}
				dValueMap.put(target, dValue);
			}
		}
		return dValueMap;
	}
	
	private static DefenderAction sampleAction(
		final List<Node> dCandidateNodeList,
		final int numNodetoProtect,
		final AbstractIntegerDistribution rnd) {
		if (dCandidateNodeList == null || numNodetoProtect < 0 || rnd == null) {
			throw new IllegalArgumentException();
		}
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
	
	private static double[] computeCandidateProb(
		final int totalNumCandidate, final double[] candidateValue, final double logisParam) {
		if (totalNumCandidate < 0 || candidateValue == null) {
			throw new IllegalArgumentException();
		}
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
		} else  {
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
			probabilities[i] = Math.exp(logisParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for (int i = 0; i < totalNumCandidate; i++) {
			probabilities[i] /= sumProb;
		}
		
		return probabilities;
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
