package rldepgraph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Defender;
import game.GameSimulationSpec;
import model.AttackerAction;
import model.DefenderBelief;
import model.DependencyGraph;
import py4j.GatewayServer;
import rl.RLAttackerRawObservation;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * Wrapper for a game of depgraph, 
 * to be used by Py4J.
 * 
 * Assumes learning agent will select nodes or edges to attack
 * one at a time, in a greedy fashion.
 * Game state indicates which nodes or edges are currently in the
 * list to attack.
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class DepgraphPy4JAttGreedyConfigCLI {

	/**
	 * Inner object that represents the game state.
	 */
	private RLAttackerGameSimulation sim;
	
	/**
	 * The likelihood that after each
	 * round of adding one node to the set to defend in an episode,
	 * the defender agent will not be allowed to add more nodes.
	 */
	private final double probGreedySelectionCutOff;
	
	/**
	 * The set of AND node IDs to be attacked in a given episode.
	 */
	private final Set<Integer> nodesToAttack;
	
	/**
	 * The set of edge IDs (to OR nodes) to be attacked in a given episode.
	 */
	private final Set<Integer> edgesToAttack;
	
	/**
	 * Used to get random values for selection cutoff.
	 */
	private static final Random RAND = new Random();
	
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
	 * If true, adding the same node to nodesToAttack
	 * or edge to edgesToAttack
	 * repeatedly in one turn loses the game;
	 * moreover, adding a non-candidate node or edge
	 * loses the game.
	 * 
	 * Otherwise, doing so is equivalent to the "pass"
	 * move and leads to selecting the current nodesToAttack
	 * and edgesToAttack.
	 */
	private static final boolean LOSE_IF_REPEAT_OR_NOT_CANDIDATE = false;

	/**
	 * The Defender agent.
	 */
	private Defender defender;

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JAttGreedyConfigCLI singleton;
	
	/**
	 * Used to get the observation of the attacker.
	 */
	private AttackerAction attAction = null;

	/**
	 * Used to preserve the belief state of the defender
	 * within a game simulation run.
	 */
	private DefenderBelief curDefBelief;
	
	/**
	 * Public constructor.
	 * 
	 * @param aProbGreedySelectionCutOff likelihood that after each
	 * round of adding one node or edge to the set to attack in an episode,
	 * the attacker agent will not be allowed to add more.
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param defStratName the defender strategy name
	 * @param graphFileName the name of the graph file to use
	 */
	private DepgraphPy4JAttGreedyConfigCLI(
		final double aProbGreedySelectionCutOff,
		final String simSpecFolderName,
		final String defStratName,
		final String graphFileName
	) {
		if (aProbGreedySelectionCutOff < 0.0
			|| aProbGreedySelectionCutOff >= 1.0
			|| simSpecFolderName == null
			|| defStratName == null
			|| graphFileName == null) {
			throw new IllegalArgumentException();
		}
		this.probGreedySelectionCutOff = aProbGreedySelectionCutOff;
		this.nodesToAttack = new HashSet<Integer>();
		this.edgesToAttack = new HashSet<Integer>();
		this.defender = null;
		this.actionToAndNodeIndex = new HashMap<Integer, Integer>();
		this.actionToEdgeToOrNodeIndex = new HashMap<Integer, Integer>();
		
		final double discFact = setupEnvironment(
			simSpecFolderName, graphFileName);
		setupDefender(defStratName, discFact);
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
	 * @param args has two args: defenderStratName and graphFileName
	 */
	public static void main(final String[] args) {
		final int argsCount = 2;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
		"Need 2 args: defenderStratName, graphFileName"
			);
		}
		final String simSpecFolderName = "simspecs/";
		final String defenderStratName = args[0];
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		final String graphFileName = args[1];
		
		final double probGreedySelectCutOff = 0.1;
		// set up Py4J server
		singleton = new DepgraphPy4JAttGreedyConfigCLI(
			probGreedySelectCutOff,
			simSpecFolderName,
			defenderStratName,
			graphFileName
		);
		final GatewayServer gatewayServer = new GatewayServer(singleton);
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Get a new DepgraphPy4JAttGreedyConfigCLI object for Py4J.
	 * @return the DepgraphPy4JAttGreedyConfigCLI for Py4J to use.
	 */
	public static DepgraphPy4JAttGreedyConfigCLI getGame() {
		return singleton;
	}

	/**
	 * Reset the game (clear all actions and reset time steps left).
	 * 
	 * @return the state of the game as a list of doubles
	 */
	public List<Double> reset() {
		// clear the game state.
		this.sim.reset();
		
		// update the defender.
		this.sim.setDefender(this.defender);
		// reset and initialize the defender's belief state
		this.curDefBelief = new DefenderBelief();
		this.curDefBelief.addState(this.sim.getGameState(), 1.0);
		
		// no nodesToAttack or edgesToAttack so far
		this.nodesToAttack.clear();
		this.edgesToAttack.clear();
		// no action was taken by attacker
		this.attAction = null;

		return getAttObsAsListDouble();
	}

	/**
	 * Get a human-readable game state string.
	 * @return the string representing the human-readable game state.
	 */
	public String render() {
		if (this.attAction == null) {
			final RLAttackerRawObservation attObs =
				new RLAttackerRawObservation(
					this.sim.getLegalToAttackNodeIds(),
					this.sim.getAndNodeIds(),
					this.sim.getNumTimeStep());
			return attObs + ", legalActions:\n"
			+ legalActionsString(attObs);
		}
		final RLAttackerRawObservation attObs =
			this.sim.getAttackerObservation(this.attAction);
		return attObs + ", legalActions:\n"
			+ legalActionsString(attObs);
	}
	
	/**
	 * @param rawAttObs the observation of the attacker agent.
	 * @return a string indicating the indexes in the attacker's values
	 * from {1, . . ., maxActionIndex} corresponding to legal actions.
	 */
	private String legalActionsString(
		final RLAttackerRawObservation rawAttObs) {
		final StringBuilder builder = new StringBuilder();
		builder.append("legalAttackNodeIndexes=[");
		final List<Integer> nodeIds = rawAttObs.getLegalToAttackNodeIds();
		for (int i = 0; i < nodeIds.size(); i++) {
			final int nodeId = nodeIds.get(i);
			for (final Entry<Integer, Integer> entry
				: this.actionToAndNodeIndex.entrySet()) {
				if (entry.getValue() == nodeId) {
					builder.append(entry.getKey());
					break;
				}
			}
			if (i < nodeIds.size() - 1) {
				builder.append(',');
			}
		}
		builder.append("],\n");
		
		builder.append("legalAttackEdgeIndexes=[");
		final List<Integer> edgeIds = rawAttObs.getLegalToAttackEdgeIds();
		for (int i = 0; i < edgeIds.size(); i++) {
			final int edgeId = edgeIds.get(i);
			for (final Entry<Integer, Integer> entry
				: this.actionToEdgeToOrNodeIndex.entrySet()) {
				if (entry.getValue() == edgeId) {
					builder.append(entry.getKey());
					break;
				}
			}
			if (i < edgeIds.size() - 1) {
				builder.append(',');
			}
		}
		builder.append("]\n");
		return builder.toString();
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
	public List<Double> step(final Integer action) {
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
		
		final List<Double> result = new ArrayList<Double>();
		if (action == passAction
			|| (canPassRandomly
				&& RAND.nextDouble() < this.probGreedySelectionCutOff)
			|| (isActionDuplicate(action) && !LOSE_IF_REPEAT_OR_NOT_CANDIDATE)
			|| (!isCandidateActionId(action)
				&& !LOSE_IF_REPEAT_OR_NOT_CANDIDATE)
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
			if (!this.sim.isValidMove(this.nodesToAttack, this.edgesToAttack)) {
				// illegal move. game is lost.
				final List<Double> attObs = getAttObsAsListDouble();
				// self player (attacker) gets minimal reward for illegal move.
				final double reward = this.sim.getWorstRemainingReward();
				// game is over.
				final double isOver = 1.0;
				result.addAll(attObs);
				result.add(reward);
				result.add(isOver);
				return result;
			}
			
			// move is valid.
			// take a step and update defender's stored belief.
			this.curDefBelief = this.sim.step(
				this.nodesToAttack, this.edgesToAttack, this.curDefBelief);
			
			// reset nodesToAttack and edgesToAttack to empty set
			// before next move, after storing them in this.attAction.
			this.attAction = this.sim.generateAttackerAction(
				this.nodesToAttack, this.edgesToAttack);
			this.nodesToAttack.clear();
			this.edgesToAttack.clear();
			
			final List<Double> attObs = getAttObsAsListDouble();
			final double reward = this.sim.getAttackerMarginalPayoff();
			double isOver = 0.0;
			if (this.sim.isGameOver()) {
				isOver = 1.0;
			}
			result.addAll(attObs);
			result.add(reward);
			result.add(isOver);
			return result;
		}
		
		// selection is allowed; will try to add to nodesToDefend.
		if (!isValidActionId(action)
			|| (isActionDuplicate(action) && LOSE_IF_REPEAT_OR_NOT_CANDIDATE)
			|| (!isCandidateActionId(action) && LOSE_IF_REPEAT_OR_NOT_CANDIDATE)
		) {
			// illegal action selection. game is lost.
			// no need to update this.attAction.
			final List<Double> attObs = getAttObsAsListDouble();
			// self player (attacker) gets minimal reward for illegal move.
			final double reward = this.sim.getWorstRemainingReward();
			// game is over.
			final double isOver = 1.0;
			result.addAll(attObs);
			result.add(reward);
			result.add(isOver);
			return result;
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
		final List<Double> attObs = getAttObsAsListDouble();
		final double reward = 0.0; // no marginal reward for adding nodes to set
		final double isOver = 0.0; // game is not over.
		result.addAll(attObs);
		result.add(reward);
		result.add(isOver);
		return result;
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
	 * Initialize Defender.
	 * @param defStratName the defender's strategy name
	 * @param discFact the discount factor of the game
	 */
	private void setupDefender(
		final String defStratName,
		final double discFact
	) {

		final String defenderName =
			EncodingUtils.getStrategyName(defStratName);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defStratName);
		this.defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, discFact);
	}
	
	/**
	 * Load the graph and simulation specification, and initialize
	 * the defender opponent and environment.
	 * 
	 * @param simSpecFolderName the folder the simulation spec
	 * file should come from
	 * @param graphFileName the file name of the graph file
	 * @return the discount factor of the environment
	 */
	private double setupEnvironment(
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
				
		// Load players
		final String defenderString =
			JsonUtils.getDefenderString(simSpecFolderName);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		this.defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, simSpec.getDiscFact());

		final RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		this.sim = new RLAttackerGameSimulation(
			depGraph, this.defender,
			rng.getRandomGenerator(), rng,
			numTimeStep, discFact);
		return discFact;
	}
	
	/**
	 * @return the total discounted reward of the defender
	 * in this game instance.
	 */
	public double getOpponentTotalPayoff() {
		return this.sim.getDefenderTotalPayoff();
	}
	
	/**
	 * @return the total discounted reward of the attacker
	 * in this game instance.
	 */
	public double getSelfTotalPayoff() {
		return this.sim.getAttackerTotalPayoff();
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
				this.sim.getTimeStepsLeft());
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
