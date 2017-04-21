package agent;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

public abstract class Defender {
	public enum DefenderType {
		UNIFORM, MINCUT, GOAL_ONLY, ROOT_ONLY
		, vsVALUE_PROPAGATION, vsRANDOM_WALK, vsUNIFORM;
		
		@Override
		public String toString() {
			switch(this) {
			case UNIFORM: return "UN";
			case MINCUT: return "MC";
			case GOAL_ONLY: return "GO";
			case ROOT_ONLY: return "RO";
			case vsVALUE_PROPAGATION: return "vVP";
			case vsRANDOM_WALK: return "vRW";
			case vsUNIFORM: return "vUN";
			default: return "";
			}	
		}
	}
	
	public enum DefenderParam {
		maxNumRes, minNumRes, numResRatio, 
		maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio, 
		logisParam, bThres, isRandomized,
		qrParam, 
		numRWSample;
		
		@Override
		public String toString() {
			switch(this) {
			case maxNumRes: return "maxNumRes";
			case minNumRes: return "minNumRes";
			case numResRatio: return "numResRatio";
			
			case maxNumAttCandidate: return "maxNumAttCandidate";
			case numAttCandidateRatio: return "numAttCandidateRatio";
			case minNumAttCandidate: return "minNumAttCandidate";
			
			case logisParam: return "logisParam";
			case bThres: return "bThres";
			case isRandomized: return "isRandomized";
			
			case qrParam: return "qrParam";
			case numRWSample: return "numRWSample";
			default: return "";
			}	
		}
	}

	private DefenderType dType;
	
	public Defender(final DefenderType dTypeCur) {
		if (dTypeCur == null) {
			throw new IllegalArgumentException();
		}
		this.dType = dTypeCur;
	}
	
	public final DefenderType getDType() {
		return this.dType;
	}
	
	public abstract DefenderAction sampleAction(
			DependencyGraph depGraph,
			int curTimeStep, 
			int numTimeStep, 
			DefenderBelief dBelief, 
			RandomGenerator rng);
	
	public abstract DefenderBelief updateBelief(
			DependencyGraph depGraph,
			DefenderBelief currentBelief, 
			DefenderAction dAction,
			DefenderObservation dObservation, 
			int curTimeStep, 
			int numTimeStep,
			RandomGenerator rng);
	
	public static final DefenderAction simpleSampleAction(
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
	
	public static final Map<Node, Double> computeCandidateValueTopo(
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
										r[i][j][node.getId() - 1] = discountFactor * rHat; // find the worst case scenario
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
}