package rldepgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.Defender;
import graph.Edge;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.Node;
import model.AttackerAction;
import model.DependencyGraph;
import model.GameState;
import rl.RLAttackerRawObservation;

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
	private final RandomDataGenerator rng;
	
	/**
	 * Past attacker observation states, if any.
	 * Most recent observation is last.
	 */
	private final List<RLAttackerRawObservation> aObservations;
	
	/**
	 * The current game state, including which nodes are compromised.
	 */
	private GameState gameState;
	
	/**
	 * How many time steps are left in the simulation.
	 */
	private int timeStepsLeft;
	
	/**
	 * Defender's total discounted payoff so far in a simulation episode.
	 */
	private double defenderTotalPayoff;
	
	/**
	 * Defender's discounted payoff in most recent time step only.
	 */
	private double defenderMarginalPayoff;
	
	/**
	 * Past attacker actions, if any. Most recent is last.
	 */
	private List<AttackerAction> mostRecentAttActs;
	
	/**
	 * The worst reward that is possible in one time step.
	 */
	private double worstReward;
	
	/**
	 * Constructor for the game logic class.
	 * @param aDepGraph the game's dependency graph
	 * @param aDefender the defender agent
	 * @param aRng random number generator for game randomness
	 * @param aNumTimeStep how long each episode is
	 * @param aDiscFact discount factor for agent rewards
	 */
	public RLAttackerGameSimulation(
		final DependencyGraph aDepGraph,
		final Defender aDefender,
		final RandomDataGenerator aRng,
		final int aNumTimeStep, final double aDiscFact) {
		if (aDepGraph == null || aDefender == null
			|| aRng == null || aNumTimeStep < 1
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
		this.aObservations = new ArrayList<RLAttackerRawObservation>();
		this.mostRecentAttActs = new ArrayList<AttackerAction>();
		setupWorstReward();
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
	 * Return to the initial game state.
	 * Set the observation to the initial "empty" observation,
	 * reset game state to no nodes compromised, 
	 * set the timeStepsLeft to the max value,
	 * and set the total payoff for defender to 0.
	 */
	public void reset() {
		this.aObservations.clear();		
		this.aObservations.add(new RLAttackerRawObservation(this.numTimeStep));
		this.gameState = new GameState();
		this.gameState.createID();
		this.depGraph.setState(this.gameState);
		this.timeStepsLeft = this.numTimeStep;
		this.defenderTotalPayoff = 0.0;
		this.defenderMarginalPayoff = 0.0;
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
	 * all the AND nodeIds have only ACTIVE parent nodes,
	 * and all the OR edgeIds have ACTIVE source nodes.
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
			if (!areAllParentsOfNodeActive(targetNodeId)) {
				return false;
			}
		}
		for (final int targetEdgeId: edgeIdsToAttack) {
			if (!isValidIdOfEdgeToORNode(targetEdgeId)) {
				return false;
			}
			if (!isParentOfEdgeActive(targetEdgeId)) {
				return false;
			}
		}
		return true;
	}
}
