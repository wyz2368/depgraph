package rldepgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;
import game.GameSimulation;
import game.GameSimulationSpec;
import model.DependencyGraph;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

public final class CheckOldStratPayoffs {
	
	private GameSimulation sim;

	public static void main(final String[] args) {
		final String defStratStringFileName = "defStratStrings.txt";
		final List<String> defStratStrings =
			getDefenderStrings(defStratStringFileName);
		final int iterationsPerStrategy = 10;
		final CheckOldStratPayoffs checker = new CheckOldStratPayoffs();
		checker.printAllPayoffs(defStratStrings, iterationsPerStrategy);
	}
	
	private void printAllPayoffs(
		final List<String> defenderStrings,
		final int iterationsPerStrategy
	) {
		// TODO: show time taken overall
		// TODO: also show variance, SEM for each
		System.out.println(
			"Iterations per strategy: " + iterationsPerStrategy + "\n");
		final DecimalFormat format = new DecimalFormat("#.##"); 
		for (final String defenderString: defenderStrings) {
			setupEnvironment(defenderString);
			final double meanPayoff = getMeanPayoff(iterationsPerStrategy);
			System.out.println(defenderString);
			System.out.println("\t" + format.format(meanPayoff) + "\n");
		}
	}
	
	private double getMeanPayoff(
		final int simulationCount
	) {
		// TODO: also get variance, SEM for each
		double defPayoffTotal = 0.0;
		for (int i = 0; i < simulationCount; i++) {
			this.sim.reset();
			this.sim.runSimulation();
			defPayoffTotal += this.sim.getSimulationResult().getDefPayoff();
		}
		return defPayoffTotal * 1.0 / simulationCount;
	}
	
	private static List<String> getDefenderStrings(final String fileName) {
		final List<String> result = new ArrayList<String>();
		try (final BufferedReader br =
			new BufferedReader(new FileReader(fileName))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	String lineStripped = StringUtils.strip(line);
		    	if (lineStripped.length() > 0) {
			    	result.add(lineStripped);
		    	}
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private void setupEnvironment(final String defenderString) {
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
		final double discFact = simSpec.getDiscFact();

		final String attackerString =
			JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, discFact);
		
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, discFact);

		RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		this.sim = new GameSimulation(
			depGraph, attacker, defender, rng, numTimeStep, discFact);
	}
}
