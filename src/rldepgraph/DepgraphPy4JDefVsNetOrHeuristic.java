package rldepgraph;

import java.util.List;
import java.util.Map;

import agent.AgentFactory;
import agent.Attacker;
import game.GameSimulationSpec;
import py4j.GatewayServer;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * Wrapper for a game of depgraph, 
 * to be used by Py4J.
 * 
 * Assumes learning agent will select nodes to defend
 * one at a time, in a greedy fashion.
 * Game state indicates which nodes are currently in the
 * list to defend.
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class DepgraphPy4JDefVsNetOrHeuristic {
	
	/**
	 * The object that runs the game against an attacker network.
	 */
	private final DepgraphPy4JGreedyConfigBoth dgForNetwork;
	
	/**
	 * The object that runs the game against an attacker heuristic.
	 */
	private final DepgraphPy4JGreedyConfig dgForHeuristic;
	
	/**
	 * If true, the attacker is a heuristic attacker.
	 * Otherwise, it's a neural network attacker.
	 */
	private boolean isAttackerHeuristic;

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JDefVsNetOrHeuristic singleton;
	
	/**
	 * The discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * Public constructor.
	 * 
	 * @param aProbGreedySelectionCutOff likelihood that after each
	 * round of adding one node to the set to defend in an episode,
	 * the defender agent will not be allowed to add more nodes.
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param graphFileName the name of the graph file to use
	 */
	private DepgraphPy4JDefVsNetOrHeuristic(
		final double aProbGreedySelectionCutOff,
		final String simSpecFolderName,
		final String graphFileName
	) {
		if (aProbGreedySelectionCutOff < 0.0
			|| aProbGreedySelectionCutOff >= 1.0
			|| simSpecFolderName == null) {
			throw new IllegalArgumentException();
		}

		this.dgForNetwork = new DepgraphPy4JGreedyConfigBoth(
			aProbGreedySelectionCutOff,
			simSpecFolderName,
			graphFileName);
		
		this.dgForHeuristic = new DepgraphPy4JGreedyConfig(
			aProbGreedySelectionCutOff,
			simSpecFolderName,
			null,
			graphFileName
		);
		this.discFact = getDiscFact(simSpecFolderName);
	}

	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args has one arg: graphFileName
	 */
	public static void main(final String[] args) {
		final int argsCount = 1;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
				"Need 1 arg: graphFileName"
			);
		}
		final String simSpecFolderName = "simspecs/";
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		final String graphFileName = args[0];
		
		final double probGreedySelectCutOff = 0.1;
		// set up Py4J server
		singleton = new DepgraphPy4JDefVsNetOrHeuristic(
			probGreedySelectCutOff,
			simSpecFolderName,
			graphFileName
		);
		final GatewayServer gatewayServer = new GatewayServer(singleton);
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Get a new DepgraphPy4JDefVsNetOrHeuristic object for Py4J.
	 * @return the DepgraphPy4JDefVsNetOrHeuristic for Py4J to use.
	 */
	public static DepgraphPy4JDefVsNetOrHeuristic getGame() {
		return singleton;
	}
	
	/**
	 * Load the simulation specification, and return the
	 * discount factor.
	 * 
	 * @param simSpecFolderName the folder the simulation spec
	 * file should come from
	 * @return the discount factor of the environment
	 */
	private static double getDiscFact(
		final String simSpecFolderName
	) {
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simSpecFolderName);	
		final double curDiscFact = simSpec.getDiscFact();
		return curDiscFact;
	}
	
	/**
	 * Reset the game (clear all actions and reset time steps left).
	 * 
	 * @param inputs a list of 2 strings.
	 * the first is "True" iff
	 * the attacker strategy is a heuristic, otherwise something else.
	 * the second describes the heuristic attacker,
	 * or is empty string if not a heuristic (i.e., a network attacker).
	 * @return the state of the game as a list of doubles
	 */
	public List<Double> reset(
		final List<String> inputs
	) {
		if (inputs.size() != 2) {
			throw new IllegalArgumentException();
		}
		final String trueString = "True";
		this.isAttackerHeuristic = inputs.get(0).equals(trueString);
		final String attackerString = inputs.get(1);
		
		if (this.isAttackerHeuristic) {
			this.dgForHeuristic.reset();
			setAttacker(attackerString);
			return this.dgForHeuristic.getDefObsAsListDouble();
		}
		
		if (!attackerString.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.dgForNetwork.reset();
		return this.dgForNetwork.getDefObsAsListDouble();
	}
	
	/**
	 * Construct a heuristic attacker based on the input string,
	 * and set the simulation's attacker to be that agent.
	 * 
	 * @param attackerString the string describing the heuristic
	 * attacker
	 */
	private void setAttacker(final String attackerString) {
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		
		final Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, this.discFact);
		this.dgForHeuristic.setAttacker(attacker);
	}
	
	/**
	 * Take a step based on the action of the defender, against either
	 * a heuristic or network attacker.
	 * 
	 * @param action an int indicating the action of the defender.
	 * @return the result format depends on whether playing against a heuristic
	 * or network attacker.
	 */
	public List<Double> step(final Integer action) {
		if (this.isAttackerHeuristic) {
			return this.dgForHeuristic.step(action);
		} 
		
		return this.dgForNetwork.stepCurrent(action);
	}
	
	/**
	 * Get the defender's marginal discounted payoff.
	 * @return the defender's marginal discounted payoff
	 */
	public double getSelfMarginalPayoff() {
		if (this.isAttackerHeuristic) {
			throw new IllegalStateException();
		}
		
		return this.dgForNetwork.getDefenderMarginalPayoff();
	}
	
	/**
	 * @return the total discounted reward of the attacker
	 * in this game instance.
	 */
	public double getOpponentTotalPayoff() {
		if (this.isAttackerHeuristic) {
			return this.dgForHeuristic.getOpponentTotalPayoff();
		}
		return this.dgForNetwork.getAttackerTotalPayoff();
	}

	/**
	 * @return the total discounted reward of the defender
	 * in this game instance.
	 */
	public double getSelfTotalPayoff() {
		if (this.isAttackerHeuristic) {
			return this.dgForHeuristic.getSelfTotalPayoff();
		}
		return this.dgForNetwork.getDefenderTotalPayoff();
	}
	
	/**
	 * Get a human-readable game state string.
	 * @return the string representing the human-readable game state.
	 */
	public String render() {
		if (this.isAttackerHeuristic) {
			return this.dgForHeuristic.render();
		}
		return this.dgForNetwork.render();
	}
}
