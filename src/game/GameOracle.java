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
	private static final int MAX_ITER = 200;
	
	private GameOracle() {
		// private constructor
	}
	
	/**
	 * 
	 * @param pastState past game state.
	 * @param attAction attacker action
	 * @param defAction defender action
	 * @param rnd random data generator
	 * @return new game state
	 */
	public static GameState generateStateSample(
		final GameState pastState,
		final AttackerAction attAction,
		final DefenderAction defAction, 
		final RandomDataGenerator rnd
	) {
		if (pastState == null || attAction == null
			|| defAction == null || rnd == null) {
			throw new IllegalArgumentException();
		}
		final List<GameState> stateSampleList = generateStateSample(
			pastState, attAction, defAction, rnd, 1, false);
		return stateSampleList.get(0);
	}
	
	/**
	 * 
	 * @param depGraph dependency graph with node states included
	 * @param gameState current game state
	 * @param rnd random data generator
	 * @param timeStepsLeft how many time steps remain
	 * @return observation of the defender
	 */
	public static DefenderObservation generateDefObservation(
		final DependencyGraph depGraph,
		final GameState gameState,
		final RandomDataGenerator rnd,
		final int timeStepsLeft
	) {
		if (depGraph == null || gameState == null || rnd == null) {
			throw new IllegalArgumentException();
		}
		DefenderObservation defObservation =
			new DefenderObservation(timeStepsLeft);
		final boolean[] isActive = new boolean[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			isActive[i] = false;
		}
		for (final Node node : gameState.getEnabledNodeSet()) {
			isActive[node.getId() - 1] = true;
		}
		for (final Node node : depGraph.vertexSet()) {
			double trueAlertProb = node.getPosInactiveProb();
			if (isActive[node.getId() - 1]) {
				trueAlertProb = node.getPosActiveProb();
			}
			final double pivot = rnd.nextUniform(0.0, 1.0, true);
			final SecurityAlert alert =
				new SecurityAlert(node, pivot < trueAlertProb);
			defObservation.addAlert(alert);
		}
		if (defObservation.getAlertSet().size()
			!= depGraph.vertexSet().size()) {
			throw new IllegalStateException();
		}
		return defObservation;
	}

	/**
	 * 
	 * @param gameState current game state
	 * @param dObservation observation resulted from gameState
	 * @return probability of the observation
	 */
	public static double computeObservationProb(
		final GameState gameState, final DefenderObservation dObservation) {
		if (gameState == null || dObservation == null) {
			throw new IllegalArgumentException();
		}
		double prob = 1.0;
		for (SecurityAlert alert : dObservation.getAlertSet()) {
			Node node = alert.getNode();
			boolean alertType = alert.isActiveAlert();
			if (gameState.containsNode(node)) { // this is the active node
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

	/**
	 * 
	 * @param dAction action of the defender
	 * @param aAction action of the attacker
	 * @param pastState previous game state
	 * @param newState new game state
	 * @return probability of the new  game state state
	 */
	public static double computeStateTransitionProb(
		final DefenderAction dAction, final AttackerAction aAction
		, final GameState pastState, final GameState newState) {
		if (dAction == null || aAction == null
			|| pastState == null || newState == null) {
			throw new IllegalArgumentException();
		}
		// active nodes in past state will remain active if not disable
		for (Node node : pastState.getEnabledNodeSet()) {
			if (!dAction.containsNode(node) && !newState.containsNode(node)) {
				return 0.0;
			}
		}
		// nodes which are disabled by the defender can not be active
		for (Node node : newState.getEnabledNodeSet()) {
			if (dAction.containsNode(node)) {
				return 0.0;
			}
		}
		
		double prob = 1.0;
		// iterate over nodes the attacker aims at activating
		for (Entry<Node, Set<Edge>> entry
			: aAction.getActionCopy().entrySet()) {
			Node node = entry.getKey();
			// if node is already enabled, the attacker should not
			// enable it again
			assert !pastState.containsNode(node);
			List<Edge> edgeList = new ArrayList<Edge>(entry.getValue());
			// if the defender doesn't disable this node
			if (!dAction.getAction().contains(node)) {
				double enableProb = 1.0;
				if (node.getActivationType() == NodeActivationType.AND) {
					enableProb = node.getActProb();
				} else {
					for (int i = 0; i < edgeList.size(); i++) {
						enableProb *= (1 - edgeList.get(i).getActProb());
					}
					enableProb = 1 - enableProb;
				}
				if (newState.containsNode(node)) {
					prob *= enableProb;
				} else {
					prob *= (1 - enableProb);
				}
			}
		}
		
		return prob;
	}
	
	/**
	 * 
	 * @param pastState game state in the previous time step
	 * @param attAction action of the attacker
	 * @param defAction action of the defender
	 * @param rnd random data generator
	 * @param numStateSample number of new game states to sample
	 * @param isReplacement randomization with replacement or not
	 * @return List of new game state samples
	 */
	public static List<GameState> generateStateSample(final GameState pastState
		, final AttackerAction attAction, final DefenderAction defAction
		, final RandomDataGenerator rnd, final int numStateSample,
		final boolean isReplacement) {
		if (pastState == null || attAction == null
			|| defAction == null || rnd == null || numStateSample < 1) {
			throw new IllegalArgumentException();
		}
		// probability each node is active
		Map<Node, Double> enableProbMap = new HashMap<Node, Double>();
		for (Node node : pastState.getEnabledNodeSet()) {
			if (!defAction.containsNode(node)) {
				// nodes currently enabled and not protected
				// by defender remain enabled
				enableProbMap.put(node, 1.0);
			}
		}
		
		// Iterate over all nodes the attacker aims at activating
		for (Entry<Node, Set<Edge>> entry
			: attAction.getActionCopy().entrySet()) {
			final Node node = entry.getKey();
			final Set<Edge> edges = entry.getValue();
			if (pastState.containsNode(node)) {
				// if node is already enabled, the attacker 
				// must not be allowed to enable it again
				throw new IllegalStateException(
					"can't enable node already enabled: " + node);
			}
			// if the defender is not protecting this node
			if (!defAction.getAction().contains(node)) {
				for (final Edge inEdge: edges) {
					if (!pastState.containsNode(inEdge.getsource())) {
						throw new IllegalStateException(
							"attacked over edge with inactive source");
					}
				}
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
					// Check if this node will become active
					if (pivot <= enableProb) {
						gameState.addEnabledNode(node);
					}
				}
				gameState.createID();
				// check if this is a new state
				boolean isAdd = gameStateSet.add(gameState);
				if (isAdd) {
					i++;
				}
				// if the maximum number of iterations reached
				// before obtaining the required number of samples
				if (count > MAX_ITER) {
					break;
				}
			}
			// return the new set of samples
			return new ArrayList<GameState>(gameStateSet);
		}
		
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
