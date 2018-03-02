package rldepgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import game.GameOracle;
import graph.Edge;
import graph.Node;
import graph.INode.NodeType;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;
import rl.RLAttackerRawObservation;
import rl.RLDefenderRawObservation;

/**
 * This class contains the dependency graph game
 * logic, for running the game with a reinforcement
 * learning agent.
 */
public final class RLGameSimulationBoth {

	/**
	 * The total time steps per simulation.
	 */
	private final int numTimeStep;
	
	/**
	 * The graph environment.
	 */
	private final DependencyGraph depGraph;
	
	/**
	 * The discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * A random number generator for state updates.
	 */
	private final RandomDataGenerator rng;
	
	/**
	 * Past defender observation states, if any.
	 * Most recent observation is last.
	 */
	private final List<DefenderObservation> dObservations;
	
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
	 * Defender's total discounted payoff so far in a simulation episode.
	 */
	private double defenderTotalPayoff;
	
	/**
	 * Defender's discounted payoff in most recent time step only.
	 */
	private double defenderMarginalPayoff;
	
	/**
	 * Past defender actions, if any. Most recent is last.
	 */
	private List<DefenderAction> mostRecentDefActs;
	
	/**
	 * Past attacker actions, if any. Most recent is last.
	 */
	private List<AttackerAction> mostRecentAttActs;
	
	/**
	 * The worst defender reward that is possible in one time step.
	 */
	private double worstDefenderReward;
	
	/**
	 * The worst attacker reward that is possible in one time step.
	 */
	private double worstAttackerDefenderReward;
	
	/**
	 * Attacker's total discounted payoff so far in a simulation episode.
	 */
	private double attackerTotalPayoff;
	
	/**
	 * Attacker's discounted payoff in most recent time step only.
	 */
	private double attackerMarginalPayoff;
	
	/**
	 * Constructor for the game logic class.
	 * @param aDepGraph the game's dependency graph
	 * @param aRng random number generator for game randomness
	 * @param aNumTimeStep how long each episode is
	 * @param aDiscFact discount factor for agent rewards
	 */
	public RLGameSimulationBoth(
		final DependencyGraph aDepGraph,
		final RandomDataGenerator aRng,
		final int aNumTimeStep, final double aDiscFact) {
		if (aDepGraph == null
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
		
		this.attackerTotalPayoff = 0.0;
		this.attackerMarginalPayoff = 0.0;
		this.defenderMarginalPayoff = 0.0;
		this.defenderTotalPayoff = 0.0;
		
		this.rng = aRng;
		this.dObservations = new ArrayList<DefenderObservation>();
		this.mostRecentDefActs = new ArrayList<DefenderAction>();
		this.mostRecentAttActs = new ArrayList<AttackerAction>();
		this.attackerObservations = new ArrayList<RLAttackerRawObservation>();
		setupDefenderWorstReward();
	}
	
	/**
	 * Set up the worst (minimum) reward for a single time step,
	 * which is the sum over nodes of the min of their reward if compromised
	 * and their reward (cost in R-) of defending.
	 */
	private void setupDefenderWorstReward() {
		double result = 0.0;
		for (final Node node: this.depGraph.vertexSet()) {
			result += Math.min(node.getDPenalty(), node.getDCost());
		}
		this.worstDefenderReward = result;
	}
	
	/**
	 * Get the lowest discounted reward possible in this time step.
	 * @return the lowest possible discounted reward in this time step.
	 */
	public double getWorstDefenderRemainingReward() {
		final int timeStep = this.numTimeStep - this.timeStepsLeft;
		double discFactPow = Math.pow(this.discFact, timeStep);
		double totalProduct = discFactPow;
		for (int t = timeStep + 1; t <= this.numTimeStep; t++) {
			discFactPow *= this.discFact;
			totalProduct += discFactPow;
		}
		return this.worstDefenderReward * totalProduct;
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
	 * and set the total payoff for defender to 0.
	 */
	public void reset() {
		this.dObservations.clear();
		this.dObservations.add(new DefenderObservation(this.numTimeStep));
		this.gameState = new GameState();
		this.gameState.createID();
		this.depGraph.setState(this.gameState);
		this.timeStepsLeft = this.numTimeStep;
		this.defenderTotalPayoff = 0.0;
		this.defenderMarginalPayoff = 0.0;
		this.attackerTotalPayoff = 0.0;
		this.mostRecentDefActs.clear();
	}
	
	/**
	 * Returns true if the nodeId is the ID of a node in the graph.
	 * @param nodeId a nodeId to check
	 * @return true if nodeId is in {1, . . ., nodeCount}.
	 */
	public boolean isValidId(final int nodeId) {
		return nodeId >= 1 && nodeId <= this.depGraph.vertexSet().size();
	}
	
	/**
	 * Whether the defender move is legal.
	 * @param idsToDefend the set of node IDs to protect
	 * @return true if all IDs refer to nodes in the network,
	 * meaning they are all in {1, . . ., nodeCount}
	 */
	public boolean isValidMove(final Set<Integer> idsToDefend) {
		if (idsToDefend == null) {
			return false;
		}
		for (final int target: idsToDefend) {
			if (!isValidId(target)) {
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
		final AttackerAction attAction
	) {
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
	 * @return the total discounted reward of the attacker
	 * in this game instance.
	 */
	public double getAttackerTotalPayoff() {
		return this.attackerTotalPayoff;
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
	 * defender action.
	 * @param idsToDefend the set of node IDs for the defender to
	 * protect.
	 * @param nodeIdsToAttack the set of node IDs for the attacker to strike.
	 * @param edgeIdsToAttack the set of edge IDs to or nodes for
	 * the attacker to strike.
	 */
	public void step(
		final Set<Integer> idsToDefend,
		final Set<Integer> nodeIdsToAttack,
		final Set<Integer> edgeIdsToAttack
	) {
		if (!isValidMove(idsToDefend)) {
			throw new IllegalArgumentException("illegal move: " + idsToDefend);
		}
		final AttackerAction attAction =
			generateAttackerAction(nodeIdsToAttack, edgeIdsToAttack);
		
		final DefenderAction curDefAct = new DefenderAction();
		for (final int idToDefend: idsToDefend) {
			curDefAct.addNodetoProtect(
				this.depGraph.getNodeById(idToDefend));
		}
		this.mostRecentDefActs.add(curDefAct);
		
		this.gameState = GameOracle.generateStateSample(
			this.gameState, attAction, curDefAct, this.rng);
		this.depGraph.setState(this.gameState);
		
		this.timeStepsLeft--;
		this.dObservations.add(GameOracle.generateDefObservation(
			this.depGraph, this.gameState, this.rng, this.timeStepsLeft));
		
		final double defenderCurPayoff =
			getDefenderPayoffCurrentTimeStep(curDefAct);
		this.defenderTotalPayoff += defenderCurPayoff;
		this.defenderMarginalPayoff = defenderCurPayoff;
		
		this.attackerTotalPayoff += getAttackerPayoffCurrentTimeStep(attAction);
	}

	/**
	 * Returns the discounted payoff of the current episode so far.
	 * @return the discounted payoff of the episode so far
	 */
	public double getDefenderTotalPayoff() {
		return this.defenderTotalPayoff;
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
	 * Returns a "raw" version of the defender's observation.
	 * @return the defender's observation of current game state
	 */
	public RLDefenderRawObservation getDefenderObservation() {
		return new RLDefenderRawObservation(
			this.dObservations,
			this.mostRecentDefActs);
	}

	/**
	 * Returns true if game is over (no more time steps left).
	 * @return true if game is over
	 */
	public boolean isGameOver() {
		return this.timeStepsLeft == 0;
	}
}
