package main;

import game.GameSimulation;
import game.GameSimulationSpec;
import game.MeanGameSimulationResult;

import java.io.File;
import java.util.Map;

import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

public final class MainGameFixedGraph {
	
	private MainGameFixedGraph() {
		// private constructor
	}

	public static void main(final String[] args) {
		if (args == null || args.length != 1) {
			throw new IllegalStateException(
				"Need 1 argument: simulationFolder"
			);
		}
		final long startTime = System.nanoTime();
		runSimulationsAndPrint(args[0]);
		final long endTime = System.nanoTime();
		final long diff = endTime - startTime;
		final long millis = diff / 1000000;
		GameSimulation.printIfDebug("time taken in millis: " + millis);
	}
	
	/**
	 * @param simspecFolderName the local path to the folder
	 * containing simulation_spec.json
	 */
	public static void runSimulationsAndPrint(
		final String simspecFolderName) {
		if (simspecFolderName == null) {
			throw new IllegalArgumentException();
		}
  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);	
		// Load graph
		final int numNodes = 30;
		final int numEdge = 100;
		String filePathName = "graphs" + File.separator
			+ "RandomGraph" + numNodes
			+ "N" + numEdge + "E" 
			+ 2 + "T"
			+ 1 + JsonUtils.JSON_SUFFIX;
		DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
		GameSimulation.printIfDebug(filePathName);
				
		// Load players
		final String attackerString =
			JsonUtils.getAttackerString(simspecFolderName);
		final String defenderString =
			JsonUtils.getDefenderString(simspecFolderName);
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		
		final MeanGameSimulationResult simResult = runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		final String obsString =
			JsonUtils.getObservationString(
				simResult, attackerString, defenderString, simSpec);
		JsonUtils.printObservationToFile(simspecFolderName, obsString);
	}
	
	public static MeanGameSimulationResult runSimulations(
		final DependencyGraph depGraph,
		final GameSimulationSpec simSpec, final String attackerName,
		final Map<String, Double> attackerParams, final String defenderName,
		final Map<String, Double> defenderParams, final int numSim) {
		MeanGameSimulationResult meanGameSimResult =
			new MeanGameSimulationResult();
		Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, simSpec.getDiscFact());
		Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, simSpec.getDiscFact());
		RandomDataGenerator rng = new RandomDataGenerator();
		GameSimulation gameSim = new GameSimulation(
			depGraph, attacker, defender, rng
			, simSpec.getNumTimeStep(), simSpec.getDiscFact());
		
		final int thousand = 1000;
		for (int i = 0; i < numSim; i++) {
			if (i % thousand == 0) {
				GameSimulation.printIfDebug("Simulation: " + i);
			}
			gameSim.runSimulation();
			meanGameSimResult.updateMeanSimulationResult(
				gameSim.getSimulationResult());
			gameSim.reset();
		}
		return meanGameSimResult;
	}
}
