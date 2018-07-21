package rldepgraph;

import java.util.List;
import java.util.Map;

import agent.AgentFactory;
import agent.Defender;
import game.GameSimulationSpec;
import py4j.GatewayServer;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * Wrapper for a game of depgraph, 
 * to be used by Py4J.
 * 
 * Assumes learning agent will select nodes or edges
 * to attack one at a time.
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class DepgraphPy4JAttVsNetOrHeuristic {
	
	/**
	 * The object that runs the game against an defender network.
	 */
	private final DepgraphPy4JGreedyConfigBoth dgForNetwork;
	
	/**
	 * The object that runs the game against an defender heuristic.
	 */
	private final DepgraphPy4JAttGreedyConfig dgForHeuristic;
	
	/**
	 * If true, the defender is a heuristic defender.
	 * Otherwise, it's a neural network defender.
	 */
	private boolean isDefenderHeuristic;

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JAttVsNetOrHeuristic singleton;
	
	/**
	 * The discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * Public constructor.
	 * 
	 * @param aProbGreedySelectionCutOff likelihood that after each
	 * round of adding one node/edge to the set to attack in an episode,
	 * the attacker agent will not be allowed to add more items.
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param graphFileName the name of the graph file to use
	 */
	private DepgraphPy4JAttVsNetOrHeuristic(
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
		
		this.dgForHeuristic = new DepgraphPy4JAttGreedyConfig(
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
		final int argsCount = 2;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
				"Need 2 args: graphFileName attPort"
			);
		}
		final String simSpecFolderName = "simspecs/";
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		final String graphFileName = args[0];
		final int attPort = Integer.parseInt(args[1]);
		final int minPort = 25335;
		if (attPort < minPort || attPort % 2 == 0) {
			throw new IllegalArgumentException("Invalid attPort: " + attPort);
		}
		
		final double probGreedySelectCutOff = 0.1;
		// set up Py4J server
		singleton = new DepgraphPy4JAttVsNetOrHeuristic(
			probGreedySelectCutOff,
			simSpecFolderName,
			graphFileName
		);
		final GatewayServer gatewayServer =
			new GatewayServer(singleton, attPort, attPort + 1, 0, 0, null);
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Get a new DepgraphPy4JAttVsNetOrHeuristic object for Py4J.
	 * @return the DepgraphPy4JAttVsNetOrHeuristic for Py4J to use.
	 */
	public static DepgraphPy4JAttVsNetOrHeuristic getGame() {
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
	 * the defender strategy is a heuristic, otherwise something else.
	 * the second describes the heuristic defender,
	 * or is empty string if not a heuristic (i.e., a network defender).
	 * @return the state of the game as a list of doubles
	 */
	public List<Double> reset(
		final List<String> inputs
	) {
		if (inputs.size() != 2) {
			throw new IllegalArgumentException();
		}
		final String trueString = "True";
		this.isDefenderHeuristic = inputs.get(0).equals(trueString);
		final String defenderString = inputs.get(1);
		
		if (this.isDefenderHeuristic) {
			this.dgForHeuristic.reset();
			setDefender(defenderString);
			return this.dgForHeuristic.getAttObsAsListDouble();
		}
		
		if (!defenderString.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.dgForNetwork.reset();
		return this.dgForNetwork.getAttObsAsListDouble();
	}
	
	/**
	 * Construct a heuristic defender based on the input string,
	 * and set the simulation's defender to be that agent.
	 * 
	 * @param defenderString the string describing the heuristic
	 * defender
	 */
	private void setDefender(final String defenderString) {
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		
		final Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, this.discFact);
		this.dgForHeuristic.setDefender(defender);
	}
	
	/**
	 * Take a step based on the action of the attacker, against either
	 * a heuristic or network defender.
	 * 
	 * @param action an int indicating the action of the attacker.
	 * @return the result format depends on whether playing against a heuristic
	 * or network defender.
	 */
	public List<Double> step(final Integer action) {
		if (this.isDefenderHeuristic) {
			return this.dgForHeuristic.step(action);
		} 
		
		return this.dgForNetwork.stepCurrent(action);
	}
	
	/**
	 * Get the attacker's marginal discounted payoff.
	 * @return the attacker's marginal discounted payoff
	 */
	public double getSelfMarginalPayoff() {
		if (this.isDefenderHeuristic) {
			throw new IllegalStateException();
		}
		return this.dgForNetwork.getAttackerMarginalPayoff();
	}
	
	/**
	 * @return the total discounted reward of the attacker
	 * in this game instance.
	 */
	public double getOpponentTotalPayoff() {
		if (this.isDefenderHeuristic) {
			return this.dgForHeuristic.getOpponentTotalPayoff();
		}
		return this.dgForNetwork.getDefenderTotalPayoff();
	}

	/**
	 * @return the total discounted reward of the defender
	 * in this game instance.
	 */
	public double getSelfTotalPayoff() {
		if (this.isDefenderHeuristic) {
			return this.dgForHeuristic.getSelfTotalPayoff();
		}
		return this.dgForNetwork.getAttackerTotalPayoff();
	}
	
	/**
	 * Get a human-readable game state string.
	 * @return the string representing the human-readable game state.
	 */
	public String render() {
		if (this.isDefenderHeuristic) {
			return this.dgForHeuristic.render();
		}
		return this.dgForNetwork.render();
	}
}
