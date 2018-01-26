package rldepgraph;

import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.Attacker;
import game.GameOracle;
import graph.Node;
import graph.INode.NodeType;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;
import rl.RLDefenderRawObservation;

/**
 * This class contains the dependency graph game
 * logic, for running the game with a reinforcement
 * learning agent.
 */
public final class RLGameSimulation {

	/**
	 * The total time steps per simulation.
	 */
	private final int numTimeStep;
	
	/**
	 * The graph environment.
	 */
	private final DependencyGraph depGraph;
	
	/**
	 * The attacker agent to play against.
	 */
	private final Attacker attacker;
	
	/**
	 * The discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * A random number generator for state updates.
	 */
	private final RandomDataGenerator rng;
	
	/**
	 * The defender's current observation state.
	 */
	private DefenderObservation dObservation;
	
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
	 * Most recent defender action, if any.
	 */
	private DefenderAction mostRecentDefAct;
	
	/**
	 * The worst reward that is possible in one time step.
	 */
	private double worstReward;
	
	/**
	 * Constructor for the game logic class.
	 * @param aDepGraph the game's dependency graph
	 * @param aAttacker the attacker agent
	 * @param aRng random number generator for game randomness
	 * @param aNumTimeStep how long each episode is
	 * @param aDiscFact discount factor for agent rewards
	 */
	public RLGameSimulation(
		final DependencyGraph aDepGraph, final Attacker aAttacker,
		final RandomDataGenerator aRng,
		final int aNumTimeStep, final double aDiscFact) {
		if (aDepGraph == null || aAttacker == null
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
		
		this.attacker = aAttacker;
		
		this.rng = aRng;
		this.mostRecentDefAct = null;
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
			result += Math.min(node.getDPenalty(), node.getDCost());
		}
		this.worstReward = result;
	}
	
	/**
	 * Get the lowest discounted reward possible in this time step.
	 * @return the lowest possible discounted reward in this time step.
	 */
	public double getMinTimeStepReward() {
		final int timeStep = this.numTimeStep - this.timeStepsLeft;
		final double discFactPow = Math.pow(this.discFact, timeStep);
		return this.worstReward * discFactPow;
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
		this.dObservation = new DefenderObservation(this.numTimeStep);
		this.gameState = new GameState();
		this.gameState.createID();
		this.depGraph.setState(this.gameState);
		this.timeStepsLeft = this.numTimeStep;
		this.defenderTotalPayoff = 0.0;
		this.defenderMarginalPayoff = 0.0;
		this.mostRecentDefAct = null;
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
	 * Update the game state to the next time step, using the given
	 * defender action.
	 * @param idsToDefend the set of node IDs for the defender to
	 * protect.
	 */
	public void step(
		final Set<Integer> idsToDefend
	) {
		if (!isValidMove(idsToDefend)) {
			throw new IllegalArgumentException("illegal move: " + idsToDefend);
		}
		final int t = this.numTimeStep - this.timeStepsLeft + 1;
		final AttackerAction attAction = this.attacker.sampleAction(
			this.depGraph, 
			t, 
			this.numTimeStep,
			this.rng.getRandomGenerator()
		);
		
		this.mostRecentDefAct = new DefenderAction();
		for (final int idToDefend: idsToDefend) {
			this.mostRecentDefAct.addNodetoProtect(
				this.depGraph.getNodeById(idToDefend));
		}
		
		this.gameState = GameOracle.generateStateSample(
			this.gameState, attAction, this.mostRecentDefAct, this.rng);
		this.depGraph.setState(this.gameState);
		
		this.timeStepsLeft--;
		this.dObservation = GameOracle.generateDefObservation(
			this.depGraph, this.gameState, this.rng, this.timeStepsLeft);
		
		final double defenderCurPayoff =
			getDefenderPayoffCurrentTimeStep(this.mostRecentDefAct);
		this.defenderTotalPayoff += defenderCurPayoff;
		this.defenderMarginalPayoff = defenderCurPayoff;
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
			this.dObservation, this.mostRecentDefAct);
	}
	
	/**
	 * Returns true if game is over (no more time steps left).
	 * @return true if game is over
	 */
	public boolean isGameOver() {
		return this.timeStepsLeft == 0;
	}
}
