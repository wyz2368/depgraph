package rldepgraph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import game.GameSimulationSpec;
import model.DependencyGraph;
import py4j.GatewayServer;
import rl.RLDefenderRawObservation;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * Wrapper for a game of depgraph, 
 * to be used by Py4J.
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class DepgraphPy4J {

	/**
	 * Inner object that represents the game state.
	 */
	private RLGameSimulation sim;
	
	/**
	 * Public constructor.
	 */
	public DepgraphPy4J() {
		setupEnvironment();
	}
	
	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		// set up Py4J server
		final GatewayServer gatewayServer =
			new GatewayServer(new DepgraphPy4J());
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Load the graph and simulation specification, and initialize
	 * the attacker opponent and environment.
	 */
	private void setupEnvironment() {
		final String simspecFolderName = "simspecs";
		final String graphFolderName = "graphs";
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);	
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode()
			+ "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
				
		// Load players
		final String attackerString =
			JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		
		Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, simSpec.getDiscFact());
		RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		this.sim = new RLGameSimulation(
			depGraph, attacker, rng, numTimeStep, discFact);
	}
	
	/**
	 * Get a new DepgraphPy4J object for Py4J.
	 * @return the DepgraphPy4J for Py4J to use.
	 */
	public static DepgraphPy4J getBoard() {
		return new DepgraphPy4J();
	}
	
	/**
	 * Reset the game (clear all actions and reset time steps left).
	 * 
	 * @return the state of the game as a list of doubles
	 */
	public List<Double> reset() {
		// clear the game state.
		this.sim.reset();
		
		return getDefObsAsListDouble();
	}

	/**
	 * Take a step based on the given action, represented as
	 * a list of integers.
	 * 
	 * Return a flat list representing, in order:
	 * the new defender observation state,
	 * the reward of the step for player taking action (in R-),
	 * whether the game is done (in {0, 1}).
	 * 
	 * If the action is illegal, do not update the game state,
	 * but consider the game as lost (i.e., minimal reward)
	 * and thus done (i.e., 1).
	 * 
	 * The self agent (defender) and opponent (attacker)
	 * move simultaneously, unless the defender move was illegal,
	 * in which case the game ends immediately.
	 * 
	 * @param action the list representing the action to take.
	 * The action list will have indexes of nodes to defend.
	 * @return the list representing the new game state,
	 * including the defender observation, reward, and whether the game is over,
	 * as one flat list.
	 */
	public List<Double> step(final List<Integer> action) {
		if (action == null) {
			throw new IllegalArgumentException();
		}
		
		final Set<Integer> idsToDefend = new HashSet<Integer>(action);
		final List<Double> result = new ArrayList<Double>();
		if (!this.sim.isValidMove(idsToDefend)) {
			// illegal move. game is lost.
			final List<Double> defObs = getDefObsAsListDouble();
			// self player (defender) gets minimal reward for illegal move.
			final double reward = this.sim.getWorstRemainingReward();
			// game is over.
			final double isOver = 1.0;
			result.addAll(defObs);
			result.add(reward);
			result.add(isOver);
			return result;
		}
		
		// move is valid.
		this.sim.step(idsToDefend);
		
		final List<Double> defObs = getDefObsAsListDouble();
		final double reward = this.sim.getDefenderMarginalPayoff();
		double isOver = 0.0;
		if (this.sim.isGameOver()) {
			isOver = 1.0;
		}
		result.addAll(defObs);
		result.add(reward);
		result.add(isOver);
		return result;
	}
	
	/**
	 * Get a human-readable game state string.
	 * @return the string representing the human-readable game state.
	 */
	public String render() {
		return this.sim.getDefenderObservation().toString();
	}
	
	/**
	 * Observation list is of size 3 * N,
	 * where N is the number of nodes in the graph.
	 * 
	 * First N items are 1.0 if an attack was observed, else 0.0.
	 * Next N items are 1.0 if the node was defended, else 0.0.
	 * Next N items are 1.0 * timeStepsLeft.
	 * 
	 * @return get the defender observation as a list of Double
	 */
	private List<Double> getDefObsAsListDouble() {
		final List<Double> result = new ArrayList<Double>();
		final RLDefenderRawObservation defObs = 
			this.sim.getDefenderObservation();
		final Set<Integer> activeObservedIds = defObs.activeObservedIdSet();
		final int timeStepsLeft = defObs.getTimeStepsLeft();
		final Set<Integer> defendedIds = defObs.getDefendedIds();
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
		for (int i = 1; i <= this.sim.getNodeCount(); i++) {
			result.add((double) timeStepsLeft);
		}
		return result;
	}
}
