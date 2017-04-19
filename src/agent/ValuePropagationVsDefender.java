package agent;

import graph.Edge;
import graph.INode.NodeType;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import model.AttackCandidate;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderCandidate;
import model.DependencyGraph;
import model.GameState;

public final class ValuePropagationVsDefender extends ValuePropVsDefSuper {
	
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
	private static final double DEFAULT_PROP_PARAM = 0.5;
	private double propagationParam = DEFAULT_PROP_PARAM;
	
	public ValuePropagationVsDefender(
		final double maxNumRes, final double minNumRes, final double numResRatio,
		final double logisParam, final double discFact, final double thres,
		final double qrParam, final double maxNumAttCandidate, final double minNumAttCandidate,
		final double numAttCandidateRatio) {
		super(DefenderType.vsVALUE_PROPAGATION, discFact, thres, qrParam,
			(int) maxNumAttCandidate, (int) minNumAttCandidate, numAttCandidateRatio);
		if (
			minNumRes < 1 || minNumRes < maxNumRes || !isProb(numResRatio)
			|| discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
		) {
			throw new IllegalArgumentException();
		}
		this.maxNumRes = (int) maxNumRes;
		this.minNumRes = (int) minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		
		this.qrParam = qrParam;
		this.maxNumAttCandidate = (int) maxNumAttCandidate;
		this.minNumAttCandidate = (int) minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
	}
	
	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep, final DefenderBelief dBelief, final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		
		// Compute value of each candidate node for the defender
		Map<Node, Double> dCandidateValueMap = computeCandidateValueTopo(depGraph, dBelief
				, curTimeStep, numTimeStep, this.discFact, this.propagationParam);
		
		List<Node> dCandidateNodeList = new ArrayList<Node>();
		double[] candidateValue = new double[dCandidateValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for (Entry<Node, Double> entry : dCandidateValueMap.entrySet()) {
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		int totalNumCandidate = dCandidateValueMap.size();
		
		
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

	/*****************************************************************************************
	* 
	* @param depGraph: dependency graph
	* @param dBelief: belief of the defender
	* @param curTimeStep: current time step
	* @param numTimeStep: total number of time step
	* @param discountFactor: reward discount factor
	* @param propagationParam: for the AND node
	* @return: nodes and corresponding values
	*****************************************************************************************/
	private Map<Node, Double> computeCandidateValueTopo(
		final DependencyGraph depGraph, final DefenderBelief dBelief,
		final int curTimeStep, final int numTimeStep,
		final double discountFactor, final double propagationParamCur) {
		if (
			depGraph == null || dBelief == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
		) {
			throw new IllegalArgumentException();
		}
		Map<Node, Double> dCandidateMap = new HashMap<Node, Double>();
		
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}

		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState curGameState = entry.getKey();
			double stateProb = entry.getValue();
			depGraph.setState(curGameState);
			
			AttackCandidate curAttCandidate = ValuePropagationAttacker.selectCandidate(depGraph);
			
			double[] curACandidateProb = ValuePropagationAttacker.computeCandidateProb(depGraph, curAttCandidate
				, curTimeStep, numTimeStep, this.qrParam, discountFactor, propagationParamCur
				, this.maxNumAttCandidate, this.minNumAttCandidate, this.numAttCandidateRatio);
			// for(int i = 0; i < curACandidateProb.length; i++)
				// System.out.println(curACandidateProb[i]);
			
			DefenderCandidate curDefCandidate = selectDCandidate(curGameState, curAttCandidate);
			double[] curDCandidateValue = computeCandidateValueTopo(depGraph
					, curAttCandidate, curDefCandidate
					, curTimeStep, numTimeStep
					, discountFactor);
			// for(int i = 0; i < curDCandidateValue.length; i++)
				// System.out.println(curDCandidateValue[i]);
			
			List<Node> curDefCandidateList = new ArrayList<Node>(curDefCandidate.getNodeCandidateSet());
			List<Edge> curEdgeACandidateList = new ArrayList<Edge>(curAttCandidate.getEdgeCandidateSet());
			List<Node> curNodeACandidateList = new ArrayList<Node>(curAttCandidate.getNodeCandidateSet());
			int dIdx = 0;
			for (Node node : curDefCandidateList) {
				double tempValue = 0.0;
				double tempAttProb = 1.0;
				if (node.getType() != NodeType.TARGET || node.getState() != NodeState.ACTIVE) {
					if (node.getActivationType() == NodeActivationType.AND) {
						int idx = curNodeACandidateList.indexOf(node);
						tempAttProb = curACandidateProb[idx + curEdgeACandidateList.size()];
						tempValue += tempAttProb * curDCandidateValue[dIdx];
						// tempValue += tempAttProb;
					} else { // OR candidate
						int idx = 0;
						for (Edge edge : curEdgeACandidateList) {
							if (edge.gettarget().getId() == node.getId()) {
								tempAttProb = curACandidateProb[idx];
								tempValue += tempAttProb * curDCandidateValue[dIdx];
								// tempValue += tempAttProb;
							}
							idx++;
						}
					}
				} else { // active targets
					tempValue += curDCandidateValue[dIdx];
				}
				Double curValue = dCandidateMap.get(node);
				if (curValue == null) { // if this is a new candidate
					 dCandidateMap.put(node, tempValue * stateProb);
				} else {
					dCandidateMap.replace(node, curValue + tempValue * stateProb);
				}
				dIdx++;
			}
		}
		for (Entry<Node, Double> entry : dCandidateMap.entrySet()) {
			Node node = entry.getKey();
			Double value = entry.getValue();
			entry.setValue(value + node.getDCost() * Math.pow(discountFactor, curTimeStep - 1));
		}
		depGraph.setState(savedGameState);
		
		return dCandidateMap;
	}
	
	/*****************************************************************************************
	* 
	* @param gameState: game state
	* @param attCandidate: attack candidate
	* @return defender candidate
	*****************************************************************************************/
	private static DefenderCandidate selectDCandidate(final GameState gameState, final AttackCandidate attCandidate) {
		if (gameState == null || attCandidate == null) {
			throw new IllegalArgumentException();
		}
		DefenderCandidate dCandidate = new DefenderCandidate();
		for (Edge edge : attCandidate.getEdgeCandidateSet()) { // post-conditions of OR nodes
			dCandidate.addNodeCandidate(edge.gettarget());
			// if(edge.gettarget().getType() == NODE_TYPE.TARGET)
				// System.out.println("Candidate has targets");
		}
		for (Node node : attCandidate.getNodeCandidateSet()) { // AND nodes 
			dCandidate.addNodeCandidate(node);
			// if(node.getType() == NODE_TYPE.TARGET)
				// System.out.println("Candidate has targets");
		}
		for (Node node : gameState.getEnabledNodeSet()) { // active target nodes
			if (node.getType() == NodeType.TARGET) {
				dCandidate.addNodeCandidate(node);
				// System.out.println("Candidate has active target");
			}
		}
		return dCandidate;
	}
	
	/*****************************************************************************************
	* 
	* @param depGraph: dependency graph with current game state the defender is examining 
	* @param attackCandidate: candidate of the attacker
	* @param dCandidate: candidate of the defender
	* @param curTimeStep: current time step
	* @param numTimeStep: total number of time steps
	* @param discountFactor: reward discount factor
	* @param propagationParam: for propagating value over AND nodes
	* @return value for each candidate of the defender
	*****************************************************************************************/
	private static double[] computeCandidateValueTopo(final DependencyGraph depGraph
		, final AttackCandidate attackCandidate, final DefenderCandidate dCandidate
		, final int curTimeStep, final int numTimeStep, final double discountFactor) {
		if (depGraph == null || attackCandidate == null
			|| curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
		) {
			throw new IllegalArgumentException();
		}
		List<Node> dCandidateList = new ArrayList<Node>(dCandidate.getNodeCandidateSet());
		
		double[] dCandidateValue = new double[dCandidateList.size()];
		for (int i = 0; i < dCandidateList.size(); i++) {
			dCandidateValue[i] = 0.0; // initialize value of candidate nodes for the defender
		}
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
		int idx = 0;
		for (Node node : dCandidateList) {
			dCandidateValue[idx] += node.getDCost();
			if (node.getState() != NodeState.ACTIVE) { // not active targets, then belonging to attack candidate set
				if (node.getActivationType() == NodeActivationType.OR) { // OR nodes, then belong to attack edge set
					double prob = 1.0;
					for (Edge edge : attackCandidate.getEdgeCandidateSet()) {
						if (edge.gettarget().getId() == node.getId()) {
							prob *= (1 - edge.getActProb());
						}
					}
					prob = 1.0 - prob;
					dCandidateValue[idx] -= prob * rSum[node.getId() - 1];
				} else { // AND nodes, then belong to attack node set
					dCandidateValue[idx] -= node.getActProb() * rSum[node.getId() - 1];
				}
			} else { // if this is active target 
				// System.out.println("Active targets");
				dCandidateValue[idx] -= node.getDPenalty();
				if (rSum[node.getId() - 1] != Double.POSITIVE_INFINITY) {
					dCandidateValue[idx] -= rSum[node.getId() - 1];
				}
			}
			dCandidateValue[idx] *= Math.pow(discountFactor, curTimeStep - 1); 
			idx++;
		}
		dCandidateList.clear();
		targetList.clear();
		return dCandidateValue;
	}
	
	/*****************************************************************************************
	* 
	* @param totalNumCandidate
	* @param candidateValue
	* @param logisParam
	* @return
	*****************************************************************************************/
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
