package rldepgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

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
import rl.RLAttackerRawObservation;

/**
 * This class contains the dependency graph game
 * logic, for running the game with a reinforcement
 * learning agent, from the attacker's perspective.
 */
public final class RLAttackerGameSimulation {

	/**
	 * The total time steps per simulation.
	 */
	private final int numTimeStep;
	
	/**
	 * The graph environment.
	 */
	private final DependencyGraph depGraph;
	
	/**
	 * The defender agent to play against.
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
	 * Past attacker observation states, if any.
	 * Most recent observation is last.
	 */
	private final List<RLAttackerRawObservation> attackerObservations;
	
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
	 * Past attacker actions, if any. Most recent is last.
	 */
	private List<AttackerAction> mostRecentAttActs;
	
	/**
	 * The worst reward that is possible in one time step.
	 */
	private double worstReward;
	
	/**
	 * The nodeIds of AND nodes, ascending.
	 */
	private final List<Integer> andNodeIds;
	
	/**
	 * The edgeIds of edges to OR nodes, ascending.
	 */
	private final List<Integer> edgeToOrNodeIds;
	
	/**
	 * Constructor for the game logic class.
	 * @param aDepGraph the game's dependency graph
	 * @param aDefender the defender agent
	 * @param aRng random number generator for game randomness
	 * @param aRDataG random data generator for game randomness
	 * @param aNumTimeStep how long each episode is
	 * @param aDiscFact discount factor for agent rewards
	 */
	public RLAttackerGameSimulation(
		final DependencyGraph aDepGraph,
		final Defender aDefender,
		final RandomGenerator aRng,
		final RandomDataGenerator aRDataG,
		final int aNumTimeStep, final double aDiscFact) {
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
		
		this.defender = aDefender;
		
		this.rng = aRng;
		this.rDataG = aRDataG;
		this.attackerObservations = new ArrayList<RLAttackerRawObservation>();
		this.mostRecentAttActs = new ArrayList<AttackerAction>();
		setupWorstReward();
		
		this.andNodeIds = getAndNodeIds();
		this.edgeToOrNodeIds = getEdgeToOrNodeIds();
	}
	
	/**
	 * Set up the worst (minimum) reward for a single time step,
	 * which is the sum over nodes of the min of their reward if compromised
	 * and their reward (cost in R-) of defending.
	 */
	private void setupWorstReward() {
		double result = 0.0;
		for (final Node node: this.depGraph.vertexSet()) {
			result += node.getACost();
		}
		for (final Edge edge: this.depGraph.edgeSet()) {
			result += edge.getACost();
		}
		this.worstReward = result;
	}
	
	/**
	 * Get the lowest discounted reward possible in this time step.
	 * @return the lowest possible discounted reward in this time step.
	 */
	public double getWorstRemainingReward() {
		final int timeStep = this.numTimeStep - this.timeStepsLeft;
		double discFactPow = Math.pow(this.discFact, timeStep);
		double totalProduct = discFactPow;
		for (int t = timeStep + 1; t <= this.numTimeStep; t++) {
			discFactPow *= this.discFact;
			totalProduct += discFactPow;
		}
		return this.worstReward * totalProduct;
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
		this.attackerObservations.clear();		
		this.gameState = new GameState();
		this.gameState.createID();
		this.depGraph.setState(this.gameState);
		this.timeStepsLeft = this.numTimeStep;
		
		this.attackerObservations.add(
			new RLAttackerRawObservation(
				getLegalToAttackNodeIds(), this.andNodeIds, this.numTimeStep));
		
		this.attackerTotalPayoff = 0.0;
		this.attackerMarginalPayoff = 0.0;
		this.mostRecentAttActs.clear();
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
			if (!isNodeInactive(targetNodeId)) {
				return false;
			}
			if (!areAllParentsOfNodeActive(targetNodeId)) {
				return false;
			}
		}
		for (final int targetEdgeId: edgeIdsToAttack) {
			if (!isValidIdOfEdgeToORNode(targetEdgeId)) {
				return false;
			}
			if (!isEdgeTargetInactive(targetEdgeId)) {
				return false;
			}
			if (!isParentOfEdgeActive(targetEdgeId)) {
				return false;
			}
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
	 * @param nodeIdsToAttack nodeIds of AND nodes to attack.
	 * @param edgeIdsToAttack edgeIds of edges (to OR nodes) to attack.
	 * @return an AttackerAction with the given targets
	 */
	public AttackerAction generateAttackerAction(
		final Set<Integer> nodeIdsToAttack,
		final Set<Integer> edgeIdsToAttack
	) {
		final AttackerAction result = new AttackerAction();
		
		for (final int nodeId: nodeIdsToAttack) {
			final Node andNode = this.depGraph.getNodeById(nodeId);
			final Set<Edge> inEdges = this.depGraph.incomingEdgesOf(andNode);
			result.addAndNodeAttack(andNode, inEdges);
		}
		for (final int edgeId: edgeIdsToAttack) {
			final Edge edge = this.depGraph.getEdgeById(edgeId);
			final Node targetNode = edge.gettarget();
			result.addOrNodeAttack(targetNode, edge);
		}
		return result;
	}
	
	/**
	 * Update the game state to the next time step, using the given
	 * attacker action.
	 * @param nodeIdsToAttack the set of (AND) node IDs
	 * for the attacker to strike.
	 * @param edgeIdsToAttack the set of edge (to OR node) IDs
	 * for the attacker to strike.
	 * @param dBeliefInput the defender's belief about the game state
	 * 
	 * @return the defender's updated belief about the game state
	 */
	public DefenderBelief step(
		final Set<Integer> nodeIdsToAttack,
		final Set<Integer> edgeIdsToAttack,
		final DefenderBelief dBeliefInput
	) {
		if (!isValidMove(nodeIdsToAttack, edgeIdsToAttack)) {
			throw new IllegalArgumentException(
				"illegal move: " + nodeIdsToAttack + "\n" + edgeIdsToAttack);
		}
		final int t = this.numTimeStep - this.timeStepsLeft + 1;
		
		final AttackerAction attAction =
			generateAttackerAction(nodeIdsToAttack, edgeIdsToAttack);
		this.mostRecentAttActs.add(attAction);
		
		final DefenderAction defAction = this.defender.sampleAction(
			this.depGraph, t, this.numTimeStep, dBeliefInput, this.rng);
		
		final DefenderObservation dObservation =
			GameOracle.generateDefObservation(
				this.depGraph, this.gameState,
				this.rDataG, this.numTimeStep - t);
		final DefenderBelief dBeliefResult = this.defender.updateBelief(
			this.depGraph, dBeliefInput, defAction,
			dObservation, t, this.numTimeStep,
			this.rng);
		
		this.gameState = GameOracle.generateStateSample(
			this.gameState, attAction, defAction, this.rDataG);
		this.depGraph.setState(this.gameState);
		
		this.timeStepsLeft--;
		this.attackerObservations.add(getAttackerObservation(attAction));
		
		final double attackerCurPayoff =
			getAttackerPayoffCurrentTimeStep(attAction);
		this.attackerTotalPayoff += attackerCurPayoff;
		this.attackerMarginalPayoff = attackerCurPayoff;
		
		return dBeliefResult;
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
	private List<Integer> getEdgeToOrNodeIds() {
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
	 * whose parent nodes are all ACTIVE.
	 */
	public List<Integer> getLegalToAttackNodeIds() {
		final List<Integer> result = new ArrayList<Integer>();
		for (final int nodeId: this.andNodeIds) {
			if (areAllParentsOfNodeActive(nodeId)) {
				result.add(nodeId);
			}
		}
		return result;
	}
	
	/**
	 * @return get the list of edgeIds that it is legal to
	 * attack, ascending. They must be edgeIds of edges to OR nodes
	 * whose source node is ACTIVE.
	 */
	private List<Integer> getLegalToAttackEdgeToOrNodeIds() {
		final List<Integer> result = new ArrayList<Integer>();
		for (final int edgeId: this.edgeToOrNodeIds) {
			if (isParentOfEdgeActive(edgeId)) {
				result.add(edgeId);
			}
		}
		return result;
	}
	
	/**
	 * @return the history of active node ids, before the current
	 * episode occurred.
	 */
	private List<List<Integer>> activeNodeIdsHistory() {
		if (this.attackerObservations.isEmpty()) {
			throw new IllegalStateException();
		}
		return this.attackerObservations.
			get(this.attackerObservations.size() - 1).
			getActiveNodeIdsHistory();
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
	 * @param attAction the attacker's current action.
	 * @return the observation of the attacker after the action
	 * is taken and game state is updated
	 */
	public RLAttackerRawObservation getAttackerObservation(
		final AttackerAction attAction
	) {
		final List<Integer> attackedNodeIds = new ArrayList<Integer>();
		attackedNodeIds.addAll(attAction.getAttackedAndNodeIds());
		Collections.sort(attackedNodeIds);
		
		final List<Integer> attackedEdgeIds = new ArrayList<Integer>();
		attackedEdgeIds.addAll(attAction.getAttackedEdgeToOrNodeIds());
		Collections.sort(attackedEdgeIds);
		
		final List<Integer> legalToAttackNodeIds = getLegalToAttackNodeIds();
		final List<Integer> legalToAttackEdgeIds =
			getLegalToAttackEdgeToOrNodeIds();
		
		final List<List<Integer>> activeNodeIdsHistory = activeNodeIdsHistory();
		activeNodeIdsHistory.add(getActiveNodeIds());
		
		final int nodeCount = this.depGraph.vertexSet().size();

		return new RLAttackerRawObservation(
			attackedNodeIds,
			attackedEdgeIds, 
			legalToAttackNodeIds,
			legalToAttackEdgeIds,
			activeNodeIdsHistory,
			this.timeStepsLeft,
			nodeCount,
			this.andNodeIds,
			this.edgeToOrNodeIds
		);
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
}
