package rldepgraph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import game.GameSimulationSpec;
import model.AttackerAction;
import model.DependencyGraph;
import py4j.GatewayServer;
import rl.RLAttackerRawObservation;
import rl.RLDefenderRawObservation;
import utils.DGraphUtils;
import utils.JsonUtils;

/**
 * Wrapper for a game of depgraph, 
 * to be used by Py4J.
 * 
 * Assumes learning agent will select nodes to defend
 * and to attack
 * one at a time, in a greedy fashion.
 * Game state indicates which nodes are currently in the
 * list to defend and attack, and which player's
 * turn it is (although agents act simultaneously).
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class DepgraphPy4JGreedyConfigBoth {
	
	/**
	 * Inner object that represents the game state.
	 */
	private RLGameSimulationBoth sim;
	
	/**
	 * The likelihood that after each
	 * round of adding one node to the set to attack/defend in an episode,
	 * the current agent will not be allowed to add more items.
	 */
	private final double probGreedySelectionCutOff;
	
	/**
	 * The set of node IDs to be defended in a given episode.
	 */
	private final Set<Integer> nodesToDefend;
	
	/**
	 * The set of AND node IDs to be attacked in a given episode.
	 */
	private final Set<Integer> nodesToAttack;
	
	/**
	 * The set of edge IDs (to OR nodes) to be attacked in a given episode.
	 */
	private final Set<Integer> edgesToAttack;
	
	/**
	 * Maps action integers in {1, . . ., count(AND nodes)}
	 * to increasing indexes of AND nodes in the graph.
	 * (If there are no AND nodes, it will be empty.)
	 */
	private final Map<Integer, Integer> actionToAndNodeIndex;
	
	/**
	 * Maps action integers in 
	 * {count(AND nodes) + 1, . . .,
	 * count(AND nodes) + count(edges to OR nodes)}
	 * to increasing indexes of edges to OR nodes in the graph.
	 * (If there are no edges to OR nodes, it will be empty.)
	 */
	private final Map<Integer, Integer> actionToEdgeToOrNodeIndex;
	
	/**
	 * Used to get random values for selection cutoff.
	 */
	private static final Random RAND = new Random();

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JGreedyConfigBoth singleton;
	
	/**
	 * Used to get the observation of the attacker.
	 */
	private AttackerAction attAction = null;
	
	/**
	 * Indicates whether it's the defender's turn,
	 * else attacker's.
	 */
	private boolean isDefTurn;
	
	/**
	 * Public constructor.
	 * 
	 * @param aProbGreedySelectionCutOff likelihood that after each
	 * round of adding one item to the set to attack or defend in an episode,
	 * the agent will not be allowed to add more items.
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param graphFileName the name of the graph file to use
	 */
	private DepgraphPy4JGreedyConfigBoth(
		final double aProbGreedySelectionCutOff,
		final String simSpecFolderName,
		final String graphFileName
	) {
		if (aProbGreedySelectionCutOff < 0.0
			|| aProbGreedySelectionCutOff >= 1.0
			|| simSpecFolderName == null) {
			throw new IllegalArgumentException();
		}
		this.probGreedySelectionCutOff = aProbGreedySelectionCutOff;
		this.nodesToDefend = new HashSet<Integer>();
		this.nodesToAttack = new HashSet<Integer>();
		this.edgesToAttack = new HashSet<Integer>();
		this.actionToAndNodeIndex = new HashMap<Integer, Integer>();
		this.actionToEdgeToOrNodeIndex = new HashMap<Integer, Integer>();
		this.isDefTurn = true;
		
		setupEnvironment(simSpecFolderName, graphFileName);
		setupActionMaps();
		
		System.out.println("Node count: " + this.sim.getNodeCount());
		System.out.println(
			"AND node count: " + this.actionToAndNodeIndex.keySet().size());
		System.out.println(
			"Edge to OR node count: "
			+ this.actionToEdgeToOrNodeIndex.keySet().size());
		System.out.println("Pass action value: "
			+ (this.actionToAndNodeIndex.keySet().size()
			+ this.actionToEdgeToOrNodeIndex.keySet().size()
			+ 1));
	}

	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args has two args: simSpecFolder and graphFileName
	 */
	public static void main(final String[] args) {
		final int argsCount = 2;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
		"Need 2 args: simSpecFolder, graphFileName"
			);
		}
		final String simSpecFolderName = args[0];
		final String graphFileName = args[1];
		
		final double probGreedySelectCutOff = 0.1;
		// set up Py4J server
		singleton = new DepgraphPy4JGreedyConfigBoth(
			probGreedySelectCutOff,
			simSpecFolderName,
			graphFileName
		);
		final GatewayServer gatewayServer = new GatewayServer(singleton);
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Get a new DepgraphPy4JGreedyConfigBoth object for Py4J.
	 * @return the DepgraphPy4JGreedyConfigBoth for Py4J to use.
	 */
	public static DepgraphPy4JGreedyConfigBoth getGame() {
		return singleton;
	}
	
	/**
	 * Reset the game (clear all actions and reset time steps left).
	 * 
	 * @return the state of the game as a list of doubles.
	 */
	public List<Double> reset() {
		// clear the game state.
		this.sim.reset();
		this.nodesToDefend.clear();
		this.nodesToAttack.clear();
		this.edgesToAttack.clear();
		
		this.attAction = null;
		this.isDefTurn = true;
		
		final List<Double> defObs = getDefObsAsListDouble();
		final List<Double> attObs = getAttObsAsListDouble();

		final List<Double> result = new ArrayList<Double>();
		result.addAll(defObs);
		result.addAll(attObs);
		
		final double isOverDouble = 1.0;
		result.add(isOverDouble);
		
		final double isDefTurnDouble = 1.0;
		result.add(isDefTurnDouble);
		return result;
	}
	
	/**
	 * Initially, the defender will call this method, and this.defTurn
	 * is true.
	 * If the defender's play is an invalid move, ending the game, 
	 * both the defender and
	 * attacker observations will indicate the game is over.
	 * Otherwise, if the defender passes or is forced to pass or makes
	 * a duplicate move thus passing, this.isDefTurn will be switched to false
	 * in defenderStep(), which will be reflected in isDefTurnDouble of the
	 * returned list.
	 * Otherwise, the defender will get to select again.
	 * 
	 * Otherwise, the attacker would be calling, and this.defTurn would be
	 * false.
	 * If the attacker's play is an invalid move, ending the game, both
	 * attacker and defender observations will indicate game over.
	 * Otherwise, if the attacker passed, was forced to pass, or made
	 * a duplicate move resulting in a pass, the defender's action choice
	 * and attacker's action choice will be combined to generate the next
	 * step forward in the game; the result will be returned immediately.
	 * 
	 * If you did not take a step and return, you now append the attacker
	 * observation to the defender observation, and put an indicator of 1.0
	 * for defender's turn or 0.0 for attacker's turn at the end, and return.
	 * 
	 * @param actionInt the action of the current agent, defender
	 * if this.isDefTurn, else attacker.
	 * @return the observations and current player as a flat list,
	 * first defender observation list, then attacker observation
	 * list, then 1.0 if game is over, then 1.0 if it's defender's
	 * turn now.
	 */
	public List<Double> stepCurrent(
		final Integer actionInt
	) {
		if (actionInt == null) {
			throw new IllegalArgumentException();
		}

		List<Double> defObs = null;
		List<Double> attObs = null;
		if (this.isDefTurn) {
			System.out.println("Defender's action input.");
			defObs = defenderStep(actionInt);
			attObs = getAttObsAsListDouble();
		} else {
			System.out.println("Attacker's action input.");
			attObs = attackerStep(actionInt);
			final boolean attPassed = this.isDefTurn;
			if (attPassed && !this.sim.isGameOver()) {
				// attacker and defender have passed. take step.
				System.out.println(
					"Attacker passed, but game isn't over. Will take step.");
				return takeStep();
			}
			System.out.println("Either attacker didn't pass, or game is over");
			defObs = getDefObsAsListDouble();
		}
		System.out.println("Not time to take a step yet.");
		final List<Double> result = new ArrayList<Double>();
		result.addAll(defObs);
		result.addAll(attObs);
		
		double isOverDouble = 1.0;
		if (!this.sim.isGameOver()) {
			isOverDouble = 0.0;
		}
		result.add(isOverDouble);
		
		double isDefTurnDouble = 1.0;
		if (!this.isDefTurn) {
			isDefTurnDouble = 0.0;
		}
		result.add(isDefTurnDouble);
		
		final int expectedAttackerLength =
			((this.actionToAndNodeIndex.keySet().size()
				+ this.actionToEdgeToOrNodeIndex.keySet().size()) * 2)
			+ (this.sim.getNodeCount()
				* RLAttackerRawObservation.ATTACKER_OBS_LENGTH)
			+ 1;
		final int expectedDefenderLength = (2 + 2
			* RLDefenderRawObservation.DEFENDER_OBS_LENGTH)
			* this.sim.getNodeCount();
		final int expectedLength = expectedAttackerLength
			+ expectedDefenderLength + 2;
		if (result.size() != expectedLength) {
			throw new IllegalStateException(
			"Length should be: " + expectedLength
			+ ", but is: " + result.size() + ".\n" + result);
		}
				
		return result;
	}

	/**
	 * @return the total discounted reward of the defender
	 * in this game instance.
	 */
	public double getDefenderTotalPayoff() {
		return this.sim.getDefenderTotalPayoff();
	}

	/**
	 * @return the total discounted reward of the attacker
	 * in this game instance.
	 */
	public double getAttackerTotalPayoff() {
		return this.sim.getAttackerTotalPayoff();
	}

	/**
	 * Get a human-readable game state string.
	 * @return the string representing the human-readable game state.
	 * Has the defender observation, then the attacker observation,
	 * then an indicator for whether it's the defender's turn.
	 */
	public String render() {
		return this.sim.getDefenderObservation().toString() + "\n\n"
			+ this.sim.getAttackerObservation(new AttackerAction()).toString()
			+ "\n\nthis.isDefTurn: " + this.isDefTurn;
	}

	/**
	 * Load the graph and simulation specification, and initialize
	 * the attacker opponent and environment.
	 * 
	 * @param simSpecFolderName the folder the simulation spec
	 * file should come from
	 * @param graphFileName the file name of the graph file
	 */
	private void setupEnvironment(
		final String simSpecFolderName,
		final String graphFileName
	) {
		final String graphFolderName = "graphs";
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simSpecFolderName);	
		// Load graph
		final String filePathName = graphFolderName + File.separator
			+ graphFileName;
		final DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
		
		final RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		
		this.sim = new RLGameSimulationBoth(
			depGraph, rng, numTimeStep, discFact);
	}

	/**
	 * Initialize the maps from action integers to the AND node ID
	 * or edge to OR node ID to attack.
	 * Actions {1, . . ., count(AND nodes)} refer to the AND nodes
	 * in increasing ID order.
	 * Actions 
	 * {count(AND nodes) + 1, . . ., count(AND nodes) + count(edges to OR node)}
	 * refer to the edges to OR nodes in increasing ID order.
	 */
	private void setupActionMaps() {
		final List<Integer> andNodeIds = this.sim.getAndNodeIds();
		for (int action = 1; action <= andNodeIds.size(); action++) {
			// will be skipped if no AND nodes
			this.actionToAndNodeIndex.put(action, andNodeIds.get(action - 1));
		}
		
		final List<Integer> edgeToOrNodeIds = this.sim.getEdgeToOrNodeIds();
		for (int action = andNodeIds.size() + 1;
			action <= andNodeIds.size() + edgeToOrNodeIds.size();
			action++
		) {
			// will be skipped if no edges to OR nodes
			this.actionToEdgeToOrNodeIndex.put(
				action, 
				edgeToOrNodeIds.get(action - andNodeIds.size() - 1)
			);
		}
	}
	
	/**
	 * Update the game state with the current actions for attacker and
	 * defender.
	 * Clear those actions for the next step.
	 * 
	 * @return the observation of the defender, then of the attacker,
	 * then 1.0 to indicate it is now the defender's turn.
	 */
	private List<Double> takeStep() {
		this.sim.step(
			this.nodesToDefend, this.nodesToAttack, this.edgesToAttack);
		this.nodesToDefend.clear();
		this.nodesToAttack.clear();
		this.edgesToAttack.clear();
		
		final List<Double> result = new ArrayList<Double>();
		result.addAll(getDefObsAsListDouble());
		result.addAll(getAttObsAsListDouble());
		
		double isOver = 0.0;
		if (this.sim.isGameOver()) {
			isOver = 1.0;
		}
		result.add(isOver);
		
		final double isDefTurnDouble = 1.0;
		result.add(isDefTurnDouble);
		return result;
	}
	
	/**
	 * Take a step based on the given action, represented as
	 * an integer.
	 * 
	 * Return a flat list representing, in order:
	 * the new defender observation state,
	 * the reward of the step for player taking action (in R-),
	 * whether the game is done (in {0, 1}).
	 * 
	 * Legal actions are (NODE_COUNT + 1) to pass, or any integer that
	 * is a node ID in the graph but is NOT in this.nodesToDefend.
	 * 
	 * If the action is illegal, do not update the game state,
	 * but consider the game as lost (i.e., minimal reward)
	 * and thus done (i.e., 1).
	 * 
	 * If the move is (NODE_COUNT + 1), 
	 * or if this.nodesToDefend is not empty and with 
	 * probability this.probGreedySelectionCutOff,
	 * the self agent (defender) and opponent (attacker)
	 * move simultaneously, where the defender protects this.nodesToDefend
	 * without adding any more items to it.
	 * 
	 * Otherwise, the agent's selected node ID is added to this.nodesToDefend
	 * and control returns to the defender without the attack making a move,
	 * the marginal reward is 0.0, and the time step does not advance.
	 * 
	 * @param action an Integer, the action to take.
	 * The action should be the index of a node to add to
	 * this.nodesToDefend, or (NODE_COUNT + 1) to indicate
	 * no more nodes should be added.
	 * @return the list representing the new game state,
	 * including the defender observation, reward, and whether the game is over,
	 * as one flat list.
	 */
	private List<Double> defenderStep(final Integer action) {
		if (action == null) {
			throw new IllegalArgumentException();
		}
		final int nodeCount = this.sim.getNodeCount();
		if (action == (nodeCount + 1)
			|| (!this.nodesToDefend.isEmpty()
				&& RAND.nextDouble() < this.probGreedySelectionCutOff)
			|| (this.nodesToDefend.contains(action))
		) {
			// no more selections allowed.
			// either action was (nodeCount + 1) (pass),
			// or there is some nodesToDefend selected already
			// AND the random draw is below probGreedySelectionCutoff,
			// or the action is already in nodesToDefend AND
			// !LOSE_IF_REPEAT, so repeated selection counts as "pass".
			if (!this.sim.isValidMove(this.nodesToDefend)) {
				// illegal move. game is lost.
				this.sim.addDefenderWorstPayoff();
				this.sim.setGameOver();
				return getDefObsAsListDouble();
			}
			
			// move is valid.
			this.isDefTurn = false;
			return getDefObsAsListDouble();
		}
		
		// selection is allowed; will try to add to nodesToDefend.
		
		if (!this.sim.isValidId(action)) {
			// illegal move. game is lost.
			this.sim.addDefenderWorstPayoff();
			this.sim.setGameOver();
			return getDefObsAsListDouble();
		}

		// selection is valid and not a repeat. add to nodesToDefend.
		this.nodesToDefend.add(action);
		return getDefObsAsListDouble();
	}

	/**
	 * Take a step based on the given action, represented as
	 * an integer.
	 * 
	 * Return a flat list representing, in order:
	 * the new attacker observation state,
	 * the reward of the step for player taking action (in R),
	 * whether the game is done (in {0, 1}).
	 * 
	 * Legal actions are 
	 * (count(AND nodes) + count(edges to OR node) + 1) to pass,
	 * or any integer in {1, . . ., count(AND nodes)} that maps to an AND node
	 * not currently in nodesToAttack,
	 * or any integer in 
	 * {count(AND nodes) + 1, . . ., count(AND nodes) + count(edges to OR node)}
	 * that maps to an edge to an OR node not currently in edgesToAttack.
	 * 
	 * If the action is illegal, do not update the game state,
	 * but consider the game as lost (i.e., minimal reward)
	 * and thus done (i.e., 1).
	 * 
	 * If the move is (count(AND nodes) + count(edges to OR node) + 1), 
	 * or if this.nodesToAttack or this.edgesToAttack is not empty and with 
	 * probability this.probGreedySelectionCutOff,
	 * the self agent (attacker) and opponent (defender)
	 * move simultaneously, where the attacker strikes this.nodesToAttack
	 * and this.edgesToAttack without adding any more items to them.
	 * 
	 * Otherwise, the agent's selected node ID or edge ID is added to
	 * this.nodesToAttack or this.edgesToAttack
	 * and control returns to the attacker without the defender making a move,
	 * the marginal reward is 0.0, and the time step does not advance.
	 * 
	 * @param action an Integer, the action to take.
	 * The action should be an integer in {1, . . .,  
	 * (count(AND nodes) + count(edges to OR node) + 1)}.
	 * The first count(AND nodes) values map to increasing indexes of
	 * AND nodes.
	 * The next count(edges to OR node) values map to increasing
	 * indexes of edges to OR nodes.
	 * The last value maps to the "pass" action.
	 * @return the list representing the new game state,
	 * including the attacker observation, reward, and whether the game is over,
	 * as one flat list.
	 */
	private List<Double> attackerStep(final Integer action) {
		if (action == null) {
			throw new IllegalArgumentException();
		}		
		final int passAction =
			this.sim.getAndNodeIds().size()
			+ this.sim.getEdgeToOrNodeIds().size()
			+ 1;

		// can't be made to pass at random if no attack selected yet
		final boolean canPassRandomly =
			!this.nodesToAttack.isEmpty() || !this.edgesToAttack.isEmpty();
		
		if (action == passAction
			|| (canPassRandomly
				&& RAND.nextDouble() < this.probGreedySelectionCutOff)
			|| (isActionDuplicate(action))
			|| (!isCandidateActionId(action))
		) {
			// "pass": no more selections allowed.
			// either action was 
			// ((count(AND nodes) + count(edges to OR node) + 1)) [pass],
			// or there is some nodesToAttack or edgesToAttack selected already
			// AND the random draw is below probGreedySelectionCutoff,
			// or the action is already in nodesToAttack/edgesToAttack AND
			// !LOSE_IF_REPEAT_OR_NOT_CANDIDATE,
			// so repeated selection counts as "pass",
			// or the action is not a candidate action AND
			// !LOSE_IF_REPEAT_OR_NOT_CANDIDATE.
			if (!this.sim.isValidAttackerMove(
				this.nodesToAttack, this.edgesToAttack)) {
				// illegal move. game is lost.
				this.sim.addAttackerWorstPayoff();
				this.sim.setGameOver();
				return getAttObsAsListDouble();
			}
			
			// move is valid.
			// take a step.
			this.isDefTurn = true;
			return getAttObsAsListDouble();
		}
		
		// selection is allowed; will try to add to nodesToDefend.
		if (!isValidActionId(action)) {
			// illegal action selection. game is lost.
			// no need to update this.attAction.
			this.sim.addAttackerWorstPayoff();
			this.sim.setGameOver();
			return getAttObsAsListDouble();
		}

		if (isActionDuplicate(action) || !isCandidateActionId(action)) {
			throw new IllegalStateException("Should be unreachable.");
		}

		// selection is valid and not a duplicate or "pass".
		// add to nodesToAttack or edgesToAttack.
		if (isActionAndNode(action)) {
			this.nodesToAttack.add(this.actionToAndNodeIndex.get(action));
		} else if (isActionEdgeToOrNode(action)) {
			this.edgesToAttack.add(this.actionToEdgeToOrNodeIndex.get(action));
		} else {
			throw new IllegalStateException();
		}
		this.attAction = this.sim.generateAttackerAction(
			this.nodesToAttack, this.edgesToAttack);
		return getAttObsAsListDouble();
	}
	
	/**
	 * Observation list is of size (2 + OBS_LENGTH) * N,
	 * where N is the number of nodes in the graph.
	 * 
	 * First N items are 1.0 if the node is
	 * currently in set to defend, else 0.0.
	 * Next N items are 1.0 * timeStepsLeft.
	 * 
	 * For each i in {0, OBS_LENGTH - 1}:
	 * Next N items are 1.0 if an attack was observed i steps ago, else 0.0.
	 * Next N items are 1.0 if the node was defended i steps ago, else 0.0.
	 * @return get the defender observation as a list of Double
	 */
	private List<Double> getDefObsAsListDouble() {
		final List<Double> result = new ArrayList<Double>();
		final RLDefenderRawObservation defObs = 
			this.sim.getDefenderObservation();
		final int timeStepsLeft = defObs.getTimeStepsLeft();
		for (int i = 1; i <= this.sim.getNodeCount(); i++) {
			if (this.nodesToDefend.contains(i)) {
				result.add(1.0);
			} else {
				result.add(0.0);
			}
		}
		for (int i = 1; i <= this.sim.getNodeCount(); i++) {
			result.add((double) timeStepsLeft);
		}
		for (int t = 0; t < RLDefenderRawObservation.DEFENDER_OBS_LENGTH; t++) {
			final Set<Integer> activeObservedIds =
				defObs.activeObservedIdSet(t);
			final Set<Integer> defendedIds = defObs.getDefendedIds(t);
			for (int i = 1; i <= this.sim.getNodeCount(); i++) {
				if (activeObservedIds.contains(i)) {
					result.add(1.0);
				} else {
					result.add(0.0);
				}
			}
			for (int i = 1; i <= this.sim.getNodeCount(); i++) {
				if (defendedIds.contains(i)) {
					result.add(1.0);
				} else {
					result.add(0.0);
				}
			}
		}
		
		final int expectedLength =
			(2 + 2 * RLDefenderRawObservation.DEFENDER_OBS_LENGTH)
			* this.sim.getNodeCount();
		if (result.size() != expectedLength) {
			throw new IllegalStateException(
				"Length should be: " + expectedLength
				+ ", but is: " + result.size() + ".\n" + result);
		}
		
		return result;
	}
	
	/**
	 * @param action an integer action indicator, which should be in 
	 * {1, . . ., count(AND nodes) + count(edges to OR nodes) + 1}.
	 * @return true if the action refers to an AND node already in
	 * this.nodesToAttack or to an edge to OR node already in
	 * this.edgesToAttack. This must be looked up by checking if
	 * the action refers to a node or edge, then looking up the
	 * corresponding node ID or edge ID, and seeing if that ID
	 * is in the node or edge attack list.
	 */
	private boolean isActionDuplicate(final int action) {
		if (!isValidActionId(action)) {
			throw new IllegalArgumentException("Invalid action: " + action);
		}
		if (isActionAndNode(action)) {
			return this.nodesToAttack.contains(
				this.actionToAndNodeIndex.get(action));
		}
		if (isActionEdgeToOrNode(action)) {
			return this.edgesToAttack.contains(
				this.actionToEdgeToOrNodeIndex.get(action));
		}
		return false;
	}
	
	/**
	 * @param action an integer action indicator, which should be in 
	 * {1, . . ., count(AND nodes) + count(edges to OR nodes) + 1}.
	 * @return true if the action refers to an AND node to
	 * attack. In other words, if action is in
	 * {1, . . ., count(AND nodes)}.
	 */
	private boolean isActionAndNode(final int action) {
		if (!isValidActionId(action)) {
			throw new IllegalArgumentException("Invalid action: " + action);
		}
		return action <= this.actionToAndNodeIndex.keySet().size();
	}
	
	/**
	 * @param action an action value
	 * @return true if the action is valid, meaning it is in
	 * {1, . . ., count(AND nodes) + count(edge to OR nodes) + 1}.
	 */
	private boolean isValidActionId(final int action) {
		return action >= 1
			&& action <= this.actionToAndNodeIndex.keySet().size()
				+ this.actionToEdgeToOrNodeIndex.keySet().size() + 1;
	}
	
	/**
	 * @param action an integer action indicator, which should be in 
	 * {1, . . ., count(AND nodes) + count(edges to OR nodes) + 1}.
	 * @return true if the action refers to an edge to OR node to
	 * attack. In other words, if action is in
	 * {count(AND nodes) + 1, . . ., 
	 * count(AND nodes) + count(edges to OR nodes)}.
	 */
	private boolean isActionEdgeToOrNode(final int action) {
		if (!isValidActionId(action)) {
			throw new IllegalArgumentException("Invalid action: " + action);
		}
		return action > this.actionToAndNodeIndex.keySet().size()
			&& action <= this.actionToAndNodeIndex.keySet().size()
				+ this.actionToEdgeToOrNodeIndex.keySet().size();
	}
	
	/**
	 * @param action the action index, must be in
	 * {1, . . ., count(AND nodes) + count(edge to OR nodes) + 1}.
	 * @return true if the action is not only "valid" (i.e.,
	 * corresponds to an AND node, edge to OR node, or "pass"),
	 * but is also for an "attackable" item or "pass".
	 * That is, true if the action is an AND node, and the corresponding
	 * nodeId is "attackable"; or
	 * if the action is an edge to OR node, and the corresponding
	 * edgeId is "attackable"; or
	 * the action is "pass".
	 */
	private boolean isCandidateActionId(final int action) {
		if (!isValidActionId(action)) {
			throw new IllegalArgumentException();
		}
		if (isActionAndNode(action)) {
			return this.sim.isAttackableAndNodeId(
				this.actionToAndNodeIndex.get(action));
		}
		if (isActionEdgeToOrNode(action)) {
			return this.sim.isAttackableEdgeToOrNodeId(
				this.actionToEdgeToOrNodeIndex.get(action));
		}
		// action is "pass"
		return true;
	}
	
	/**
	 * Observation list is of size [count(AND nodes) + count(edges to OR)] * 2 +
	 *     count(nodes) * ATTACKER_OBS_LENGTH +
	 *     1.
	 * 
	 * First count(AND nodes) items are 1.0 if node is currently
	 * in set to attack, else 0.0.
	 * Next count(edges to OR) items are 1.0 if edge is currently
	 * in set to attack, else 0.0.
	 * 
	 * Next count(AND nodes) items are 1.0 if node is legal to attack,
	 * else 0.0 [all parents ACTIVE, self INACTIVE].
	 * Next count(edges to OR) items are 1.0 if edge is legal to attack,
	 * else 0.0 [source ACTIVE, target INACTIVE].
	 * 
	 * For each of ATTACKER_OBS_LENGTH times,
	 * next count(nodes) items are 1.0 if node was ACTIVE i time steps
	 * ago, else 0.0.
	 * If there are fewer than ATTACKER_OBS_LENGTH previous time steps,
	 * pad with 0's for nonexistent times.
	 * 
	 * Next item is the number of time steps left in the simulation.
	 * 
	 * @return get the attacker observation as a list of Double
	 */
	private List<Double> getAttObsAsListDouble() {
		final List<Double> result = new ArrayList<Double>();
		for (final int nodeId: this.sim.getAndNodeIds()) {
			if (this.nodesToAttack.contains(nodeId)) {
				result.add(1.0);
			} else {
				result.add(0.0);
			}
		}
		for (final int edgeId: this.sim.getEdgeToOrNodeIds()) {
			if (this.edgesToAttack.contains(edgeId)) {
				result.add(1.0);
			} else {
				result.add(0.0);
			}
		}
		
		final List<Integer> legalNodeIds =
			this.sim.getLegalToAttackNodeIds();
		for (final int nodeId: this.sim.getAndNodeIds()) {
			if (legalNodeIds.contains(nodeId)) {
				result.add(1.0);
			} else {
				result.add(0.0);
			}
		}
		final List<Integer> legalEdgeIds =
			this.sim.getLegalToAttackEdgeToOrNodeIds();
		for (final int edgeId: this.sim.getEdgeToOrNodeIds()) {
			if (legalEdgeIds.contains(edgeId)) {
				result.add(1.0);
			} else {
				result.add(0.0);
			}
		}
		
		RLAttackerRawObservation attObs = null;
		if (this.attAction == null) {
			attObs = new RLAttackerRawObservation(
				this.sim.getLegalToAttackNodeIds(),
				this.sim.getAndNodeIds(),
				this.sim.getNumTimeStep());
		} else {
			attObs = this.sim.getAttackerObservation(this.attAction);
		}
		final List<List<Integer>> activeNodeIdsHistory =
			attObs.getActiveNodeIdsHistory();
		for (int t = 0; t < RLAttackerRawObservation.ATTACKER_OBS_LENGTH; t++) {
			final List<Integer> curActiveNodeIds =
				activeNodeIdsHistory.get(activeNodeIdsHistory.size() - 1 - t);
			for (int nodeId = 1; nodeId <= this.sim.getNodeCount(); nodeId++) {
				if (curActiveNodeIds.contains(nodeId)) {
					result.add(1.0);
				} else {
					result.add(0.0);
				}
			}
		}
		
		final int timeStepsLeft = attObs.getTimeStepsLeft();
		result.add((double) timeStepsLeft);
		
		final int expectedLength = 
			((this.actionToAndNodeIndex.keySet().size()
				+ this.actionToEdgeToOrNodeIndex.keySet().size()) * 2)
			+ (this.sim.getNodeCount()
				* RLAttackerRawObservation.ATTACKER_OBS_LENGTH)
			+ 1;
		if (result.size() != expectedLength) {
			throw new IllegalStateException();
		}
		return result;
	}
}
