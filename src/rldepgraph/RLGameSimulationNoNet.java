package rldepgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import agent.Attacker;
import agent.Defender;
import game.GameOracle;
import graph.Edge;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.INode.NodeType;
import graph.Node;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

/**
 * This class contains the dependency graph game
 * logic, for running the game with a reinforcement
 * learning agent, from the attacker's perspective.
 */
public final class RLGameSimulationNoNet {

	/**
	 * The total time steps per simulation.
	 */
	private final int numTimeStep;
	
	/**
	 * The graph environment.
	 */
	private final DependencyGraph depGraph;
	
	/**
	 * The attacker agent.
	 */
	private Attacker attacker;
	
	/**
	 * The defender agent.
	 */
	private Defender defender;
	
	/**
	 * The discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * A random number generator for state updates.
	 */
	private final RandomGenerator rng;
	
	/**
	 * A random number generator for state updates.
	 */
	private final RandomDataGenerator rDataG;
	
	/**
	 * The current game state, including which nodes are compromised.
	 */
	private GameState gameState;
	
	/**
	 * How many time steps are left in the simulation.
	 */
	private int timeStepsLeft;
	
	/**
	 * Attacker's total discounted payoff so far in a simulation episode.
	 */
	private double attackerTotalPayoff;
	
	/**
	 * Attacker's discounted payoff in most recent time step only.
	 */
	private double attackerMarginalPayoff;
	
	/**
	 * The nodeIds of AND nodes, ascending.
	 */
	private final List<Integer> andNodeIds;
	
	/**
	 * The edgeIds of edges to OR nodes, ascending.
	 */
	private final List<Integer> edgeToOrNodeIds;
	
	/**
	 * Stores total discounted payoff of defender agent
	 * in most recent time step only.
	 */
	private double defenderMarginalPayoff;
	
	/**
	 * Stores total discounted payoff of defender agent
	 * during the current game.
	 */
	private double defenderTotalPayoff;
	
	/**
	 * Json data from list of previous games for storage.
	 */
	private JsonObject games = new JsonObject();
	
	/**
	 * Json data from the current game.
	 */
	private JsonObject curGame;
	
	/**
	 * File name for storing Json.
	 */
	private final String outputJsonFileName;
	
	/**
	 * Name of the graph file for recording to Json.
	 */
	private final String myGraphFileName;
	
	private DefenderBelief defBelief;
	
	/**
	 * Constructor for the game logic class.
	 * @param aDepGraph the game's dependency graph
	 * @param aDefender the defender agent
	 * @param aRng random number generator for game randomness
	 * @param aRDataG random data generator for game randomness
	 * @param aNumTimeStep how long each episode is
	 * @param aDiscFact discount factor for agent rewards
	 * @param aMyGraphFileName the name of the graph file to use
	 * @param aOutputJsonFileName the name of the Json file to store in
	 */
	public RLGameSimulationNoNet(
		final DependencyGraph aDepGraph,
		final Attacker aAttacker,
		final Defender aDefender,
		final RandomGenerator aRng,
		final RandomDataGenerator aRDataG,
		final int aNumTimeStep,
		final double aDiscFact,
		final String aMyGraphFileName,
		final String aOutputJsonFileName) {
		if (aDepGraph == null || aDefender == null
			|| aRng == null || aRDataG == null || aNumTimeStep < 1
			|| aDiscFact <= 0.0 || aDiscFact > 1.0) {
			throw new IllegalArgumentException();
		}
		if (!aDepGraph.isValid()) {
			throw new IllegalArgumentException();
		}
		this.depGraph = aDepGraph;
		this.numTimeStep = aNumTimeStep;
		this.discFact = aDiscFact;
		
		this.attacker = aAttacker;
		this.defender = aDefender;
		
		this.rng = aRng;
		this.rDataG = aRDataG;
		
		this.andNodeIds = getAndNodeIds();
		this.edgeToOrNodeIds = getEdgeToOrNodeIds();
		this.defenderTotalPayoff = 0.0;
		this.myGraphFileName = aMyGraphFileName;
		this.outputJsonFileName = aOutputJsonFileName;
	}

	/**
	 * Get a string representation of current game state.
	 * @return a string showing the full game state
	 */
	public String stateToString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(this.gameState.getEnabledSetString());
		builder.append(", ").append(this.timeStepsLeft).append(" remaining");
		return builder.toString();
	}
	
	/**
	 * Return to the initial game state.
	 * Set the observation to the initial "empty" observation,
	 * reset game state to no nodes compromised, 
	 * set the timeStepsLeft to the max value,
	 * and set the total payoff for attacker to 0.
	 */
	public void reset() {
		this.gameState = new GameState();
		this.gameState.createID();
		this.depGraph.setState(this.gameState);
		this.timeStepsLeft = this.numTimeStep;
		this.attackerTotalPayoff = 0.0;
		this.attackerMarginalPayoff = 0.0;
		this.defenderTotalPayoff = 0.0;
		this.defenderMarginalPayoff = 0.0;
		
		this.defBelief = new DefenderBelief();
		this.defBelief.addState(getGameState(), 1.0);
		resetJson();
	}
	
	/**
	 * Set the defender agent to a new one.
	 * Used for playing back mixed defender strategies without
	 * generating new simulation objects.
	 * @param aDefender the new defender to use
	 */
	public void setDefender(final Defender aDefender) {
		this.defender = aDefender;
	}
	
	/**
	 * Returns true if the nodeId is the ID of an AND node in the graph.
	 * @param nodeId a nodeId to check
	 * @return true if nodeId is in {1, . . ., nodeCount} and the node
	 * is an AND node.
	 */
	public boolean isValidANDNodeId(final int nodeId) {
		return nodeId >= 1
			&& nodeId <= this.depGraph.vertexSet().size()
			&& this.depGraph.getNodeById(nodeId).getActivationType()
				== NodeActivationType.AND;
	}
	
	/**
	 * @return a list of nodeIds of AND nodes, where each parent
	 * node is ACTIVE, and the node itself is INACTIVE, ascending.
	 */
	public List<Integer> validAndNodesToAttack() {
		final List<Integer> result = new ArrayList<Integer>();
		for (int i = 1; i <= this.depGraph.vertexSet().size(); i++) {
			if (isNodeInactive(i)
				&& isValidANDNodeId(i)
				&& areAllParentsOfNodeActive(i)) {
				result.add(i);
			}
		}
		return result;
	}
	
	/**
	 * @return a list of edgeIds of edges to OR nodes, where the source
	 * node is ACTIVE, and target node is INACTIVE, ascending.
	 */
	public List<Integer> validEdgesToOrNodeToAttack() {
		final List<Integer> result = new ArrayList<Integer>();
		for (int i = 1; i <= this.depGraph.edgeSet().size(); i++) {
			if (isEdgeTargetInactive(i)
				&& isValidIdOfEdgeToORNode(i)
				&& isParentOfEdgeActive(i)) {
				result.add(i);
			}
		}
		return result;
	}
	
	/**
	 * Returns true if the edgeID is the ID of an edge to an
	 * OR node in the graph.
	 * @param edgeId an edgeId to check
	 * @return true if edgeId is in {1, . . ., edgeCount} and the edge
	 * is to an OR node.
	 */
	public boolean isValidIdOfEdgeToORNode(final int edgeId) {
		return edgeId >= 1
			&& edgeId <= this.depGraph.edgeSet().size()
			&& this.depGraph.getEdgeById(edgeId).gettarget().getActivationType()
				== NodeActivationType.OR;
	}
	
	/**
	 * @param nodeId a nodeId to check.
	 * @return true if the node is in the INACTIVE state.
	 */
	public boolean isNodeInactive(final int nodeId) {
		return this.depGraph.getNodeById(nodeId).
			getState() == NodeState.INACTIVE;
	}
	
	/**
	 * @param edgeId the edgeId of an edge to check.
	 * @return true if the edge's target node is in the
	 * INACTIVE state.
	 */
	public boolean isEdgeTargetInactive(final int edgeId) {
		return this.depGraph.getEdgeById(edgeId).
			gettarget().getState() == NodeState.INACTIVE;
	}
	
	/**
	 * @param edgeId the edgeId of an edge to check.
	 * @return true if the edge's parent node is ACTIVE, so the
	 * edge can be attacked
	 */
	public boolean isParentOfEdgeActive(final int edgeId) {
		return this.depGraph.getEdgeById(edgeId).getsource().getState()
			== NodeState.ACTIVE;
	}
	
	/**
	 * @param nodeId the nodeId of a node to check.
	 * @return true if all incoming edges of the node are from
	 * ACTIVE nodes (if any), so the AND node can be attacked
	 */
	public boolean areAllParentsOfNodeActive(final int nodeId) {
		final Node node = this.depGraph.getNodeById(nodeId);
		for (final Edge inEdge : this.depGraph.incomingEdgesOf(node)) {
			if (inEdge.getsource().getState() == NodeState.INACTIVE) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param nodeId an ID that must be the ID of an AND
	 * node, or exception is thrown.
	 * @return true if the AND node is INACTIVE but its
	 * parents are all ACTIVE, so it can be attacked.
	 */
	public boolean isAttackableAndNodeId(final int nodeId) {
		if (!isValidANDNodeId(nodeId)) {
			throw new IllegalArgumentException();
		}
		return isNodeInactive(nodeId) && areAllParentsOfNodeActive(nodeId);
	}
	
	/**
	 * @param edgeId an ID that must be the ID of an edge to OR node
	 * node, or exception is thrown.
	 * @return true if the source node of the edge is ACTIVE and its
	 * target is INACTIVE, so the edge can be attacked.
	 */
	public boolean isAttackableEdgeToOrNodeId(final int edgeId) {
		if (!isValidIdOfEdgeToORNode(edgeId)) {
			throw new IllegalArgumentException();
		}
		return isEdgeTargetInactive(edgeId) && isParentOfEdgeActive(edgeId);
	}
	
	/**
	 * @param nodeIdsToAttack the set of nodeIds to attack
	 * @param edgeIdsToAttack the set of edgeIds to attack
	 * @return true if the move is valid. The move is valid
	 * if all nodeIds refer to AND nodes in the graph, all
	 * edgeIds refer to edge to OR nodes in the graph,
	 * all the AND nodeIds have only ACTIVE parent nodes and
	 * an INACTIVE self node,
	 * and all the OR edgeIds have ACTIVE source nodes
	 * and INACTIVE target nodes.
	 */
	public boolean isValidMove(
		final Set<Integer> nodeIdsToAttack,
		final Set<Integer> edgeIdsToAttack
	) {
		if (nodeIdsToAttack == null || edgeIdsToAttack == null) {
			return false;
		}
		for (final int targetNodeId: nodeIdsToAttack) {
			if (!isValidANDNodeId(targetNodeId)) {
				return false;
			}
			return isAttackableAndNodeId(targetNodeId);
		}
		for (final int targetEdgeId: edgeIdsToAttack) {
			if (!isValidIdOfEdgeToORNode(targetEdgeId)) {
				return false;
			}
			return isAttackableEdgeToOrNodeId(targetEdgeId);
		}
		return true;
	}
	
	/**
	 * Returns the discounted payoff to the attacker in current time step.
	 * @param attAction the AND nodes and edges to OR nodes the attacker
	 * attacked in this time step
	 * @return the discounted value of this time step's payoff,
	 * including the cost of nodes and edges attacked and reward of nodes owned
	 * by attacker after the time step's update
	 */
	private double getAttackerPayoffCurrentTimeStep(
		final AttackerAction attAction) {
		double result = 0.0;

		for (final Node node: this.gameState.getEnabledNodeSet()) {
			if (node.getType() == NodeType.TARGET) {
				result += node.getAReward();
			}
		}
		for (final int nodeId: attAction.getAttackedAndNodeIds()) {
			result += this.depGraph.getNodeById(nodeId).getACost();
		}
		for (final int edgeId: attAction.getAttackedEdgeToOrNodeIds()) {
			result += this.depGraph.getEdgeById(edgeId).getACost();
		}
		
		final int timeStep = this.numTimeStep - this.timeStepsLeft;
		final double discFactPow = Math.pow(this.discFact, timeStep);
		result *= discFactPow;
		return result;
	}
	
	/**
	 * @return the total discounted reward of the defender
	 * in this game instance.
	 */
	public double getDefenderTotalPayoff() {
		return this.defenderTotalPayoff;
	}
	
	/**
	 * Update the game state to the next time step, using the given
	 * attacker action.
	 * @param dBeliefInput the defender's belief about the game state
	 * 
	 * @return the defender's updated belief about the game state
	 */
	public void step() {
		final int t = this.numTimeStep - this.timeStepsLeft + 1;
		
		final AttackerAction attAction =
			this.attacker.sampleAction(depGraph, t, this.numTimeStep, this.rng);
		
		final DefenderAction defAction = this.defender.sampleAction(
			this.depGraph, t, this.numTimeStep, this.defBelief, this.rng);
		
		final DefenderObservation dObservation =
			GameOracle.generateDefObservation(
				this.depGraph, this.gameState,
				this.rDataG, this.numTimeStep - t);
		final DefenderBelief dBeliefResult = this.defender.updateBelief(
			this.depGraph, this.defBelief, defAction,
			dObservation, t, this.numTimeStep,
			this.rng);
		this.defBelief = dBeliefResult;
		
		this.gameState = GameOracle.generateStateSample(
			this.gameState, attAction, defAction, this.rDataG);
		this.depGraph.setState(this.gameState);
		
		this.timeStepsLeft--;
		
		final double attackerCurPayoff =
			getAttackerPayoffCurrentTimeStep(attAction);
		this.attackerTotalPayoff += attackerCurPayoff;
		this.attackerMarginalPayoff = attackerCurPayoff;
		
		final double defenderCurPayoff =
			getDefenderPayoffCurrentTimeStep(defAction);
		this.defenderMarginalPayoff = defenderCurPayoff;
		this.defenderTotalPayoff += defenderCurPayoff;
		
		final Set<Integer> curAttackerNodesAfter = new HashSet<Integer>();
		curAttackerNodesAfter.addAll(getActiveNodeIds());
		final int timeStepAfter = getNumTimeStep()
			- getTimeStepsLeft();
		final double attackerScore = getAttackerMarginalPayoff();
		final double defenderScore = getDefenderMarginalPayoff();
		final boolean isOverBoolean = isGameOver();
		
		final Set<Integer> curNodesDefended = getIdsFromNodes(defAction.getAction());
		final Set<Integer> curNodesAttacked = attAction.getAttackedAndNodeIds();
		final Set<Integer> curEdgesAttacked = attAction.getAttackedEdgeToOrNodeIds();
		stepJson(timeStepAfter, curNodesAttacked, 
			curEdgesAttacked, curNodesDefended, curAttackerNodesAfter, 
			attackerScore, defenderScore, isOverBoolean);		
	}
	
	public Set<Integer> getIdsFromNodes(final Set<Node> nodes) {
		final Set<Integer> result = new HashSet<Integer>();
		for (final Node node: nodes) {
			result.add(node.getId());
		}
		return result;
	}
	
	/**
	 * Returns the discounted payoff to the defender in current time step.
	 * @param defAction the set of nodes the defender protected this step
	 * @return the discounted value of this time step's payoff,
	 * including the cost of nodes defended and penalty of nodes owned
	 * by attacker after the time step's update
	 */
	private double getDefenderPayoffCurrentTimeStep(
		final DefenderAction defAction) {
		double result = 0.0;
		final int timeStep = this.numTimeStep - this.timeStepsLeft;
		final double discFactPow = Math.pow(this.discFact, timeStep);
		for (final Node node: this.gameState.getEnabledNodeSet()) {
			if (node.getType() == NodeType.TARGET) {
				result += discFactPow * node.getDPenalty();
			}
		}
		for (final Node node : defAction.getAction()) {
			result += discFactPow * node.getDCost();
		}
		return result;
	}
	
	/**
	 * @return the list of nodeIds of AND type nodes, ascending.
	 */
	public List<Integer> getAndNodeIds() {
		if (this.andNodeIds == null) {
			final List<Integer> result = new ArrayList<Integer>();
			for (final Node node: this.depGraph.vertexSet()) {
				if (node.getActivationType() == NodeActivationType.AND) {
					result.add(node.getId());
				}
			}
			Collections.sort(result);
			return result;
		}

		return this.andNodeIds;
	}
	
	/**
	 * @return the list of edgeIds of edges to OR type nodes, ascending.
	 */
	public List<Integer> getEdgeToOrNodeIds() {
		if (this.edgeToOrNodeIds == null) {
			final List<Integer> result = new ArrayList<Integer>();
			for (final Edge edge: this.depGraph.edgeSet()) {
				if (edge.gettarget().getActivationType()
					== NodeActivationType.OR) {
					result.add(edge.getId());
				}
			}
			Collections.sort(result);
			return result;
		}

		return this.edgeToOrNodeIds;
	}
	
	/**
	 * @return get the list of nodeIds that it is legal to
	 * attack, ascending. They must be nodeIds of AND nodes
	 * whose parent nodes are all ACTIVE and that are INACTIVE.
	 */
	public List<Integer> getLegalToAttackNodeIds() {
		final List<Integer> result = new ArrayList<Integer>();
		for (final int nodeId: this.andNodeIds) {
			if (isAttackableAndNodeId(nodeId)) {
				result.add(nodeId);
			}
		}
		return result;
	}
	
	/**
	 * @return get the list of edgeIds that it is legal to
	 * attack, ascending. They must be edgeIds of edges to OR nodes
	 * whose source node is ACTIVE and which are INACTIVE.
	 */
	public List<Integer> getLegalToAttackEdgeToOrNodeIds() {
		final List<Integer> result = new ArrayList<Integer>();
		for (final int edgeId: this.edgeToOrNodeIds) {
			if (isAttackableEdgeToOrNodeId(edgeId)) {
				result.add(edgeId);
			}
		}
		return result;
	}
	
	/**
	 * @return the nodeIds of currently active nodes, ascending.
	 */
	private List<Integer> getActiveNodeIds() {
		final List<Integer> result = new ArrayList<Integer>();
		for (final Node node: this.gameState.getEnabledNodeSet()) {
			result.add(node.getId());
		}
		Collections.sort(result);
		return result;
	}
	
	/**
	 * Returns the discounted payoff of the current episode so far.
	 * @return the discounted payoff of the episode so far
	 */
	public double getAttackerTotalPayoff() {
		return this.attackerTotalPayoff;
	}

	/**
	 * Returns the discounted payoff of the most recent time step.
	 * @return the discounted payoff of the most recent time step
	 */
	public double getAttackerMarginalPayoff() {
		return this.attackerMarginalPayoff;
	}
	
	/**
	 * Returns the discounted payoff of the most recent time step.
	 * @return the discounted payoff of the most recent time step
	 */
	public double getDefenderMarginalPayoff() {
		return this.defenderMarginalPayoff;
	}

	/**
	 * Return number of nodes in graph.
	 * @return graph node count
	 */
	public int getNodeCount() {
		return this.depGraph.vertexSet().size();
	}
	
	/**
	 * @return the total time steps per simulation.
	 */
	public int getNumTimeStep() {
		return this.numTimeStep;
	}
	
	/**
	 * @return how many time steps remain in game.
	 */
	public int getTimeStepsLeft() {
		return this.timeStepsLeft;
	}

	/**
	 * Returns true if game is over (no more time steps left).
	 * @return true if game is over
	 */
	public boolean isGameOver() {
		return this.timeStepsLeft == 0;
	}
	
	/**
	 * @return the GameState object.
	 */
	public GameState getGameState() {
		return this.gameState;
	}
	
	/**
	 * Record this.games to a Json output file. 
	 */
	public void printJsonToFile() {
		assert !this.games.isEmpty();
		try (final FileWriter file = new FileWriter(this.outputJsonFileName)) {
			this.games.toJson(file);
            file.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        }
		System.out.println(this.games.toJson());
	}
	
	/**
	 * Reset the Json data in curGame, for a new game.
	 */
	private void resetJson() {
		if (this.games.isEmpty()) {
			setupGamesJson();
		}

		this.curGame = new JsonObject();

		final int gameId = getGamesArray().size();
		this.curGame.put(DepgraphPy4JGreedyConfigBothJson.GAME_INDEX, gameId);
		
		final JsonArray timeSteps = new JsonArray();
		this.curGame.put(DepgraphPy4JGreedyConfigBothJson.TIME_STEPS, timeSteps);
		
		final JsonObject curStep = new JsonObject();
		curStep.put(DepgraphPy4JGreedyConfigBothJson.TIME, 0);
		curStep.put(DepgraphPy4JGreedyConfigBothJson.NODES_ATTACKED,
			new ArrayList<Integer>());
		curStep.put(DepgraphPy4JGreedyConfigBothJson.EDGES_ATTACKED,
			new ArrayList<Integer>());
		curStep.put(DepgraphPy4JGreedyConfigBothJson.NODES_DEFENDED,
			new ArrayList<Integer>());
		curStep.put(DepgraphPy4JGreedyConfigBothJson.ATTACKER_NODES_AFTER,
			new ArrayList<Integer>());
		curStep.put(DepgraphPy4JGreedyConfigBothJson.ATTACKER_SCORE, 0.0);
		curStep.put(DepgraphPy4JGreedyConfigBothJson.DEFENDER_SCORE, 0.0);
		timeSteps.add(curStep);
	}
	
	/**
	 * Reset the data for the Json games object.
	 */
	private void setupGamesJson() {
		this.games.put(DepgraphPy4JGreedyConfigBothJson.GRAPH_NAME,
			this.myGraphFileName);
		this.games.put(DepgraphPy4JGreedyConfigBothJson.GAMES,
			new JsonArray());
	}
	
	/**
	 * @return the JsonArray of data from each time step
	 * of the current game
	 */
	private JsonArray getCurGameTimeSteps() {
		return (JsonArray) this.curGame.get(
			DepgraphPy4JGreedyConfigBothJson.TIME_STEPS);
	}
	
	/**
	 * @return the JsonArray of game data objects
	 */
	private JsonArray getGamesArray() {
		return (JsonArray) this.games.get(
			DepgraphPy4JGreedyConfigBothJson.GAMES);
	}
	
	/**
	 * Update the Json data for time step.
	 * 
	 * @param timeStepAfter the time after this step occurs
	 * @param curNodesAttacked the set of node IDs attacked
	 * @param curEdgesAttacked the set of edge IDs attacked
	 * @param curNodesDefended the set of node IDs defended
	 * @param curAttackerNodesAfter the set of node IDs controlled by attacker
	 * after this time step
	 * @param attackerScore the marginal score of the attacker
	 * @param defenderScore the marginal score of the defender
	 * @param isOver true if the game is over after this step
	 */
	private void stepJson(
		final int timeStepAfter,
		final Set<Integer> curNodesAttacked,
		final Set<Integer> curEdgesAttacked,
		final Set<Integer> curNodesDefended,
		final Set<Integer> curAttackerNodesAfter,
		final double attackerScore,
		final double defenderScore,
		final boolean isOver
	) {
		final JsonObject curStep = new JsonObject();

		curStep.put(DepgraphPy4JGreedyConfigBothJson.TIME, timeStepAfter);

		curStep.put(DepgraphPy4JGreedyConfigBothJson.NODES_ATTACKED,
			DepgraphPy4JGreedyConfigBothJson.sortedFromSet(curNodesAttacked));
		curStep.put(DepgraphPy4JGreedyConfigBothJson.EDGES_ATTACKED,
			DepgraphPy4JGreedyConfigBothJson.sortedFromSet(curEdgesAttacked));
		curStep.put(DepgraphPy4JGreedyConfigBothJson.NODES_DEFENDED,
			DepgraphPy4JGreedyConfigBothJson.sortedFromSet(curNodesDefended));
		curStep.put(DepgraphPy4JGreedyConfigBothJson.ATTACKER_NODES_AFTER,
			DepgraphPy4JGreedyConfigBothJson.sortedFromSet(curAttackerNodesAfter));
		
		curStep.put(DepgraphPy4JGreedyConfigBothJson.ATTACKER_SCORE, attackerScore);
		curStep.put(DepgraphPy4JGreedyConfigBothJson.DEFENDER_SCORE, defenderScore);
		
		getCurGameTimeSteps().add(curStep);
		
		if (isOver) {
			getGamesArray().add(this.curGame);
		}
	}
}
