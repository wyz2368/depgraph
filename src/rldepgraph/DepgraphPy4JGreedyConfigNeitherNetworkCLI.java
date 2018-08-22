package rldepgraph;

import java.io.File;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;
import game.GameSimulation;
import game.GameSimulationSpec;
import model.DependencyGraph;
import py4j.GatewayServer;
import utils.DGraphUtils;
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
public final class DepgraphPy4JGreedyConfigNeitherNetworkCLI {

	/**
	 * Inner object that represents the game state.
	 */
	private GameSimulation sim;
	
	/**
	 * The defender agent.
	 */
	private Defender defender;
	
	/**
	 * The attacker agent.
	 */
	private Attacker attacker;

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JGreedyConfigNeitherNetworkCLI singleton;
	
	/**
	 * Discount factor for future rewards.
	 */
	private final double discFact;
	
	/**
	 * Public constructor.
	 * 
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param defStratName the defender pure strategy name
	 * @param attackStratName the attacker pure strategy name
	 * @param graphFileName the name of the graph file to use
	 */
	private DepgraphPy4JGreedyConfigNeitherNetworkCLI(
		final String simSpecFolderName,
		final String defStratName,
		final String attackStratName,
		final String graphFileName
	) {
		if (simSpecFolderName == null
			|| defStratName == null
			|| attackStratName == null) {
			throw new IllegalArgumentException();
		}
		this.defender = null;
		this.attacker = null;
		
		this.discFact = setupEnvironment(
			simSpecFolderName, graphFileName);
		setupDefender(defStratName);
		setupAttacker(attackStratName);
	}

	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args has two args: attackerStratName and graphFileName
	 */
	public static void main(final String[] args) {
		final int argsCount = 3;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
		"Need 3 args: defStratName, attackerStratName, graphFileName "
			);
		}
		final String simSpecFolderName = "simspecs/";
		final String defStratName = args[0];
		final String attackerStratName = args[1];
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		final String graphFileName = args[2];
		
		// set up Py4J server
		singleton = new DepgraphPy4JGreedyConfigNeitherNetworkCLI(
			simSpecFolderName,
			defStratName,
			attackerStratName,
			graphFileName
		);
		final GatewayServer gatewayServer = new GatewayServer(singleton);
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Initialize defender.
	 * @param defenderString the defender pure strategy
	 */
	private void setupDefender(
		final String defenderString) {
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		this.defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, this.discFact);
	}
	
	/**
	 * Initialize attacker.
	 * @param attackerString the attacker pure strategy
	 */
	private void setupAttacker(
		final String attackerString
	) {
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		this.attacker = AgentFactory.createAttacker(
			attackerName, attackerParams, this.discFact);
	}
	
	/**
	 * Load the graph and simulation specification, and initialize
	 * the attacker opponent and environment.
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
		
		final String defenderString =
			JsonUtils.getDefenderString(simSpecFolderName);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defParams =
			EncodingUtils.getStrategyParams(defenderString);
		final Defender curDefender =
			AgentFactory.createDefender(
				defenderName, defParams, simSpec.getDiscFact());
				
		// Load players
		final String attackerString =
			JsonUtils.getAttackerString(simSpecFolderName);
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Attacker curAttacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, simSpec.getDiscFact());

		RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		this.sim = new GameSimulation(
			depGraph, curAttacker, curDefender, rng, numTimeStep, discFact);
		return discFact;
	}
	
	/**
	 * Get a new DepgraphPy4JGreedyConfigNeitherNetworkCLI object for Py4J.
	 * @return the DepgraphPy4JGreedyConfigNeitherNetworkCLI for Py4J to use.
	 */
	public static DepgraphPy4JGreedyConfigNeitherNetworkCLI getGame() {
		return singleton;
	}
	
	/**
	 * Reset and run the game once, storing the attacker and defender 
	 * discounted payoffs.
	 */
	public void resetAndRunOnce() {
		// clear the game state.
		this.sim.reset();
		this.sim.setDefender(this.defender);
		this.sim.setAttacker(this.attacker);
		
		this.sim.runSimulation();
	}
	
	/**
	 * Set attacker and defender to the given agents, reset, and
	 * run runCount times.
	 * @param defString the new defender string
	 * @param attString the new attacker string
	 * @param runCount how many runs to perform with the new agents
	 */
	public void resetAndRunForGivenAgents(
		final String defString,
		final String attString,
		final Integer runCount
	) {
		setupDefender(defString);
		setupAttacker(attString);
		
		this.sim.reset();
		this.sim.setDefender(this.defender);
		this.sim.setAttacker(this.attacker);
		
		for (int i = 0; i < runCount; i++) {
			this.sim.runSimulation();
		}
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
}
