package rldepgraph;

import java.io.File;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;
import game.GameSimulationSpec;
import model.DependencyGraph;
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
public final class DepgraphPy4JGreedyConfigNeitherNetworkCLIJson {

	/**
	 * Inner object that represents the game state.
	 */
	private RLGameSimulationNoNet sim;

	/**
	 * Used to reply to getGame().
	 */
	private static DepgraphPy4JGreedyConfigNeitherNetworkCLIJson singleton;
		
	/**
	 * Public constructor.
	 * 
	 * @param simSpecFolderName the folder from which simulation_spec.json
	 * will be taken
	 * @param defStratName the defender pure strategy name
	 * @param attackStratName the attacker pure strategy name
	 * @param graphFileName the name of the graph file to use
	 * @param outputJsonFileName the output Json file name to use
	 * @param runCount how many runs to do
	 */
	private DepgraphPy4JGreedyConfigNeitherNetworkCLIJson(
		final String simSpecFolderName,
		final String defStratName,
		final String attackStratName,
		final String graphFileName,
		final String outputJsonFileName,
		final int runCount
	) {
		if (simSpecFolderName == null
			|| defStratName == null
			|| attackStratName == null) {
			throw new IllegalArgumentException();
		}
		
		setupEnvironment(
			simSpecFolderName,
			graphFileName,
			outputJsonFileName);
	}

	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args has two args: attackerStratName and graphFileName
	 */
	public static void main(final String[] args) {
		final int argsCount = 5;
		if (args == null || args.length != argsCount) {
			throw new IllegalArgumentException(
		"Need 4 args: defStratName, attackerStratName, " + 
			"graphFileName, outputJsonFileName, runCount"
			);
		}
		final String simSpecFolderName = "simspecs/";
		final String defStratName = args[0];
		final String attackerStratName = args[1];
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		final String graphFileName = args[2];
		final String outputJsonFileName = args[3];
		final int runCount = Integer.parseInt(args[4]);
		
		// set up Py4J server
		singleton = new DepgraphPy4JGreedyConfigNeitherNetworkCLIJson(
			simSpecFolderName,
			defStratName,
			attackerStratName,
			graphFileName,
			outputJsonFileName,
			runCount
		);
		
		for (int i = 0; i < runCount; i++) {
			singleton.resetAndRunSim();
		}
		singleton.printJson();
	}
	
	/**
	 * Load the graph and simulation specification, and initialize
	 * the attacker opponent and environment.
	 * 
	 * @param simSpecFolderName the folder the simulation spec
	 * file should come from
	 * @param graphFileName the file name of the graph file
	 * @param outputJsonFileName the output Json file name to use
	 * @return the discount factor of the environment
	 */
	private void setupEnvironment(
		final String simSpecFolderName,
		final String graphFileName,
		final String outputJsonFileName
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

		final RandomDataGenerator rdg = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		this.sim = new RLGameSimulationNoNet(
			depGraph, curAttacker, curDefender, 
			rdg.getRandomGenerator(), rdg, numTimeStep, discFact,
			graphFileName, outputJsonFileName);
	}
	
	/**
	 * Get a new DepgraphPy4JGreedyConfigNeitherNetworkCLI object for Py4J.
	 * @return the DepgraphPy4JGreedyConfigNeitherNetworkCLI for Py4J to use.
	 */
	public static DepgraphPy4JGreedyConfigNeitherNetworkCLIJson getGame() {
		return singleton;
	}
	
	/**
	 * Runs a complete simulation and prints Json.
	 */
	public void resetAndRunSim() {
		this.sim.reset();
		while (!this.sim.isGameOver()) {
			this.sim.step();
		}
	}
	
	/**
	 * Record this.games to a Json output file. 
	 */
	public void printJson() {
		this.sim.printJsonToFile();
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
