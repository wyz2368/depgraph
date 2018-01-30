package rldepgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import game.GameSimulationSpec;
import model.DependencyGraph;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

/**
 * This main class runs a game between the baseline
 * DepGraph attacker (random attacker) and
 * the human, giving input over standard input.
 */
public final class DepgraphVsHuman {

	/**
	 * Handles the game simulation logic.
	 */
	private static RLGameSimulation sim;
	
	/**
	 * Main method for playing the game.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		setupEnvironment();
		playGame();
	}
	
	/**
	 * Private constructor for utility class.
	 */
	private DepgraphVsHuman() {
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
		sim = new RLGameSimulation(
			depGraph, attacker, rng, numTimeStep, discFact);
	}
	
	/**
	 * Prompts AI and human for moves until the game
	 * is over, then declares the winner.
	 */
	public static void playGame() {
		final boolean showState = false;
		
		sim.reset();
		while (!sim.isGameOver()) {
			if (showState) {
				System.out.println("\n" + sim.stateToString() + "\n");
			}
			System.out.println(sim.getDefenderObservation());
			Set<Integer> idsToDefend = getHumanIdsToDefend();
			while (!sim.isValidMove(idsToDefend)) {
				System.out.println("Invalid move.");
				idsToDefend = getHumanIdsToDefend();
			}
			
			sim.step(idsToDefend);
		}
		if (showState) {
			System.out.println(sim.stateToString());
		}
		System.out.println(sim.getDefenderObservation());
		final double defenderPayoff = sim.getDefenderTotalPayoff();
		System.out.println("Defender total payoff: " + defenderPayoff);
	}
	
	/**
	 * Get the set of node IDs for the human player to defend in this
	 * time step.
	 * @return a set of IDs to defend.
	 */
	private static Set<Integer> getHumanIdsToDefend() {
		System.out.println("Enter your move, as IDs 0-29, comma-separated:");
		final Set<Integer> result = new HashSet<Integer>();
		final BufferedReader reader =
			new BufferedReader(new InputStreamReader(System.in));
		try {
			final String input = reader.readLine();
			if (input == null) {
				System.out.println("Please try again.");
				return getHumanIdsToDefend();
			}
			List<String> items = Arrays.asList(input.split(","));
			for (final String item: items) {
				final String itemStripped = item.trim();
				if (itemStripped.isEmpty()) {
					return result; // empty set to defend
				}
				try {
					final int cur = Integer.parseInt(itemStripped);
				    result.add(cur);
				} catch (NumberFormatException e) {
				    System.out.println(
			    		"That is not an integer: " + itemStripped);
				    return getHumanIdsToDefend();
				}
			}

			return result;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		System.out.println("Failed to get input. Try again.");
		return getHumanIdsToDefend();
	}
}
