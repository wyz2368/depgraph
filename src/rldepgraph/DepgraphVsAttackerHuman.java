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
import agent.Defender;
import game.GameSimulationSpec;
import model.AttackerAction;
import model.DefenderBelief;
import model.DependencyGraph;
import rl.RLAttackerRawObservation;
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
		playGame();
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
	
	/**
	 * Prompts AI and human for moves until the game
	 * is over, then declares the winner.
	 */
	public static void playGame() {
		final boolean showState = false;
		sim.reset();
		AttackerAction attAction = null;
		DefenderBelief curDefBelief = new DefenderBelief();
		curDefBelief.addState(sim.getGameState(), 1.0);
		while (!sim.isGameOver()) {
			if (showState) {
				System.out.println("\n" + sim.stateToString() + "\n");
			}
			if (attAction == null) {
				System.out.println(
					new RLAttackerRawObservation(sim.getNumTimeStep()));
			} else {
				System.out.println(
					sim.getAttackerObservation(attAction));
			}
			
			Set<Integer> nodeIdsToAttack = getHumanNodeIdsToDefend();
			Set<Integer> edgeIdsToAttack = getHumanEdgeIdsToDefend();
			while (!sim.isValidMove(nodeIdsToAttack, edgeIdsToAttack)) {
				System.out.println("Invalid move.");
				nodeIdsToAttack = getHumanNodeIdsToDefend();
				edgeIdsToAttack = getHumanEdgeIdsToDefend();
			}
			
			attAction = sim.generateAttackerAction(
				nodeIdsToAttack, edgeIdsToAttack);
			curDefBelief =
				sim.step(nodeIdsToAttack, edgeIdsToAttack, curDefBelief);
		}
		if (showState) {
			System.out.println(sim.stateToString());
		}
		if (attAction == null) {
			throw new IllegalStateException();
		}
		System.out.println(sim.getAttackerObservation(attAction));
		final double attackerTotalPayoff = sim.getAttackerTotalPayoff();
		System.out.println("Attacker total payoff: " + attackerTotalPayoff);
	}
	
	/**
	 * @return a set of nodeIds of AND nodes to attack, from
	 * human command-line input.
	 */
	private static Set<Integer> getHumanNodeIdsToDefend() {
		System.out.println(
			"Enter AND nodes to attack, as comma-separated IDs from "
				+ sim.validAndNodesToAttack());
		final Set<Integer> result = new HashSet<Integer>();
		final BufferedReader reader =
			new BufferedReader(new InputStreamReader(System.in));
		try {
			final String input = reader.readLine();
			if (input == null) {
				System.out.println("Please try again.");
				return getHumanNodeIdsToDefend();
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
				    return getHumanNodeIdsToDefend();
				}
			}

			return result;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		System.out.println("Failed to get input. Try again.");
		return getHumanNodeIdsToDefend();
	}
	
	/**
	 * @return a set of edgeIds of edges to OR nodes to attack, from
	 * human command-line input.
	 */
	private static Set<Integer> getHumanEdgeIdsToDefend() {
		System.out.println(
			"Enter edges to OR nodes to attack, as comma-separated IDs from "
				+ sim.validEdgesToOrNodeToAttack());
		final Set<Integer> result = new HashSet<Integer>();
		final BufferedReader reader =
			new BufferedReader(new InputStreamReader(System.in));
		try {
			final String input = reader.readLine();
			if (input == null) {
				System.out.println("Please try again.");
				return getHumanEdgeIdsToDefend();
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
				    return getHumanNodeIdsToDefend();
				}
			}

			return result;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		System.out.println("Failed to get input. Try again.");
		return getHumanEdgeIdsToDefend();
	}
}
