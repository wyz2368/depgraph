package rldepgraph;

import java.io.File;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Defender;
import game.GameSimulationSpec;
import model.DependencyGraph;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * This main class runs a game between the baseline
 * DepGraph defender (UniformDefender) and
 * the attacker human, giving input over standard input.
 */
public final class DepgraphVsAttackerHuman {

	/**
	 * Handles the game simulation logic.
	 */
	private static RLAttackerGameSimulation sim;
	
	/**
	 * Main method for playing the game.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		setupEnvironment();
		// playGame();
	}
	
	/**
	 * Private constructor for utility class.
	 */
	private DepgraphVsAttackerHuman() {
		// not called
	}
	
	/**
	 * Load the graph and simulation specification, and initialize
	 * the attacker opponent and environment.
	 */
	public static void setupEnvironment() {
		final String simspecFolderName = "simspecs";
		final String graphFolderName = "graphs";
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);	
		// Load graph
		final String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode()
			+ "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		final DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
				
		// Load players
		final String defenderString =
			JsonUtils.getDefenderString(simspecFolderName);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		
		final Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, simSpec.getDiscFact());
		final RandomDataGenerator rDataG = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		final double discFact = simSpec.getDiscFact();
		sim = new RLAttackerGameSimulation(
			depGraph, defender,
			rDataG.getRandomGenerator(),
			rDataG, numTimeStep, discFact);
	}
}
