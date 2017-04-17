package game;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.random.RandomDataGenerator;

import model.DefenderObservation;
import model.AttackerAction;
import model.DefenderAction;
import model.DependencyGraph;
import model.GameState;
import model.SecurityAlert;


public final class GameOracle {
	static final int MAX_ITER = 200;
	
	/*****************************************************************************************
	 * 
	 * @param pastState: past game state
	 * @param attAction: attacker action
	 * @param defAction: defender action
	 * @param rnd: random data generator
	 * @return: new game state
	 *****************************************************************************************/
	public static GameState generateStateSample(GameState pastState
			, AttackerAction attAction, DefenderAction defAction
			, RandomDataGenerator rnd) {
		List<GameState> stateSampleList = generateStateSample(pastState
				, attAction, defAction
				,  rnd, 1, false);
		return stateSampleList.get(0);
	}
	
	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph with node states included
	 * @param gameState: current game state
	 * @param rnd: random data generator
	 * @return observation of the defender
	 *****************************************************************************************/
	public static DefenderObservation generateDefObservation(DependencyGraph depGraph, GameState gameState, RandomDataGenerator rnd) {
		DefenderObservation defObservation = new DefenderObservation();
		boolean[] isActive = new boolean[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			isActive[i] = false;
		}
		
		for (Node node : gameState.getEnabledNodeSet()) {
			isActive[node.getId() - 1] = true;
		}
		for (Node node : depGraph.vertexSet()) {
			SecurityAlert alert = null;
			double pivot = rnd.nextUniform(0.0, 1.0, true);
			if (isActive[node.getId() - 1]) {
				double prob = node.getPosActiveProb();
				
				if (pivot < prob) {
					alert = new SecurityAlert(node, true);
				} else {
					alert = new SecurityAlert(node, false);	
				}
			} else {
				double prob = node.getPosInactiveProb();
				if (pivot < prob) {
					alert = new SecurityAlert(node, true);
				} else {
					alert = new SecurityAlert(node, false);	
				}
			}
			defObservation.addAlert(alert);
		}
		return defObservation;
	}
	
	/*****************************************************************************************
	 * 
	 * @param gameState: current game state
	 * @param dObservation: observation resulted from gameState
	 * @return probability of the observation
	 *****************************************************************************************/
	public static double computeObservationProb(GameState gameState, DefenderObservation dObservation)
	{
		double prob = 1.0;
		for (SecurityAlert alert : dObservation.getAlertSet()) {
			Node node = alert.getNode();
			boolean alertType = alert.getAlert();
			if (gameState.contain(node)) { // this is the active node
				if (alertType) { // positive alert
					prob *= node.getPosActiveProb();
				} else { // negative alert
					prob *= (1 - node.getPosActiveProb()); 
				}
			} else { // this is inactive node
				if (alertType) { // positive alert
					prob *= node.getPosInactiveProb();
				} else { // negative alert
					prob *= (1 - node.getPosInactiveProb());
				}
			}
		}
		return prob;
	}
	
	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graphs with node states included
	 * @param dAction: action of the defender
	 * @param aAction: action of the attacker
	 * @param pastState: previous game state
	 * @param newState: new game state
	 * @return probability of the new  game state state
	 *****************************************************************************************/
	public static double computeStateTransitionProb(
		    DefenderAction dAction, AttackerAction aAction
			, GameState pastState, GameState newState) {
		for (Node node : pastState.getEnabledNodeSet()) { // active nodes in past state will remain active if not disable
			if (!dAction.contain(node) && !newState.contain(node)) {
				return 0.0;
			}
		}
		for (Node node : newState.getEnabledNodeSet()) { // nodes which are disabled by the defender can not be active
			if (dAction.contain(node)) {
				return 0.0;
			}
		}
		
		double prob = 1.0;
		for (Entry<Node, Set<Edge>> entry : aAction.getAction().entrySet()) { // iterate over nodes the attacker aims at activating
			Node node = entry.getKey();
			assert !pastState.contain(node); // if node is already enabled, the attacker should not enable it again
			List<Edge> edgeList = new ArrayList<Edge>(entry.getValue());
			if (!dAction.getAction().contains(node)) { // if the defender doesn't disable this node
				double enableProb = 1.0;
				if (node.getActivationType() == NodeActivationType.AND) {
					enableProb = node.getActProb();
				} else {
					for (int i = 0; i < edgeList.size(); i++) {
						enableProb *= (1 - edgeList.get(i).getActProb());
					}
					enableProb = 1 - enableProb;
				}
				if (newState.contain(node)) {
					prob *= enableProb;
				} else {
					prob *= (1 - enableProb);
				}
			}
		}
		
		return prob;
	}
	
	/*****************************************************************************************
	 * 
	 * @param pastState: game state in the previous time step
	 * @param attAction: action of the attacker
	 * @param defAction: action of the defender
	 * @param rnd: random data generator
	 * @param numStateSample: number of new game states to sample
	 * @param isReplacement: randomization with replacement or not
	 * @return List of new game state samples
	 *****************************************************************************************/
	public static List<GameState> generateStateSample(GameState pastState
			, AttackerAction attAction, DefenderAction defAction
			, RandomDataGenerator rnd, int numStateSample, boolean isReplacement) {
		Map<Node, Double> enableProbMap = new HashMap<Node, Double>(); // probability each node is active
		// Check if the defender disables any previously active nodes
		for (Node node : pastState.getEnabledNodeSet()) {
			if (!defAction.contain(node)) {
				enableProbMap.put(node, 1.0);
			}
		}
		
		// Iterate over all nodes the attacker aims at activating
		for (Entry<Node, Set<Edge>> entry : attAction.getAction().entrySet()) {
			Node node = entry.getKey();
			assert !pastState.contain(node); // if node is already enabled, the attacker should not enable it again
			if (!defAction.getAction().contains(node)) { // if the defender is not protecting this node
				double enableProb = 1.0;
				if (node.getActivationType() == NodeActivationType.AND) {
					enableProb = node.getActProb();
				} else {
					Set<Edge> edgeSet = entry.getValue();
					for (Edge edge : edgeSet) {
						enableProb *= (1 - edge.getActProb());
					}
					enableProb = 1 - enableProb;
				}
				enableProbMap.put(node, enableProb);
			}
		}
		
		// Start sampling 
		if (isReplacement) { // this is currently used.
			Set<GameState> gameStateSet = new HashSet<GameState>();
			int i = 0;
			int count = 0; // maximum number of iterations
			while (i < numStateSample) {
				count++;
				GameState gameState = new GameState();
				for (Entry<Node, Double> entry : enableProbMap.entrySet()) {
					Node node = entry.getKey();
					Double enableProb = entry.getValue();
					Double pivot = rnd.nextUniform(0.0, 1.0, true);
					if (pivot <= enableProb) { // Check if this node will become active
						gameState.addEnabledNode(node);
					}
				}
				gameState.createID();
				boolean isAdd = gameStateSet.add(gameState); // check if this is a new state
				if (isAdd) {
					i++;
				}
				if (count > MAX_ITER) { // if the maximum number of iterations reached before obtaining the required number of samples
					break;
				}
			}
			return new ArrayList<GameState>(gameStateSet); // return the new set of samples
		}
		 // not check if this is correctly coded, temporarily ignored :D
		List<GameState> gameStateList = new ArrayList<GameState>();
		for (int i = 0; i < numStateSample; i++) {
			GameState gameState = new GameState();
			for (Entry<Node, Double> entry : enableProbMap.entrySet()) {
				Node node = entry.getKey();
				Double enableProb = entry.getValue();
				Double pivot = rnd.nextUniform(0.0, 1.0, true);
				if (pivot <= enableProb) {
					gameState.addEnabledNode(node);
				}
			}
			gameState.createID();
			gameStateList.add(gameState);
		}
		return gameStateList;
	}
}
