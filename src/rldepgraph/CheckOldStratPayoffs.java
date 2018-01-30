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

/**
 * Get the mean payoff for each old defense strategy,
 * against the given attack strategy.
 */
public final class CheckOldStratPayoffs {
	
	/**
	 * Used to run the game logic.
	 */
	private GameSimulation sim;

	/**
	 * Main method.
	 * 
	 * @param args not used
	 */
	public static void main(final String[] args) {
		final String defStratStringFileName = "defStratStrings.txt";
		final List<String> defStratStrings =
			getDefenderStrings(defStratStringFileName);
		final int iterationsPerStrategy = 2;
		final CheckOldStratPayoffs checker = new CheckOldStratPayoffs();
		checker.printAllPayoffs(defStratStrings, iterationsPerStrategy);
	}
	
	/**
	 * Print the mean, standard deviation, and standard error of
	 * payoffs for each given defender strategy.
	 * 
	 * @param defenderStrings list of defender strategy strings
	 * @param iterationsPerStrategy how many simulations to run
	 * per strategy
	 */
	private void printAllPayoffs(
		final List<String> defenderStrings,
		final int iterationsPerStrategy
	) {
		System.out.println(
			"Iterations per strategy: " + iterationsPerStrategy + "\n");
		final DecimalFormat format = new DecimalFormat("#.##");

		final long startTime = System.currentTimeMillis();
		for (final String defenderString: defenderStrings) {
			setupEnvironment(defenderString);
			final double[] payoffStats  = getPayoffStats(iterationsPerStrategy);
			final double meanPayoff = payoffStats[0];
			final double stdev = payoffStats[1];
			final double standardError = payoffStats[2];
			System.out.println(defenderString);
			System.out.println("\t" + format.format(meanPayoff) + ", "
				+ format.format(stdev) + ", " + format.format(standardError));
		}
		
		final long stopTime = System.currentTimeMillis();
	    final long elapsedTime = stopTime - startTime;
	    final double minutesTaken = elapsedTime / (1000.0 * 60.0);
	    System.out.println("Minutes taken: " + format.format(minutesTaken));
	}
	
	/**
	 * Run the given number of simulations, and return
	 * the mean, standard deviation, and standard error
	 * of the defender payoffs.
	 * 
	 * @param simulationCount how many simulations to use
	 * @return a double array with the mean defender payoff,
	 * standard deviation, and standard error of the mean in order.
	 */
	private double[] getPayoffStats(
		final int simulationCount
	) {
		double defPayoffTotal = 0.0;
		double sumSquaredPayoff = 0.0;
		for (int i = 0; i < simulationCount; i++) {
			this.sim.reset();
			this.sim.runSimulation();
			final double curPayoff =
				this.sim.getSimulationResult().getDefPayoff();
			defPayoffTotal += curPayoff;
			sumSquaredPayoff += curPayoff * curPayoff;
		}
		final double meanResult = defPayoffTotal * 1.0 / simulationCount;
		final double meanSquareResult =
			sumSquaredPayoff * 1.0 / simulationCount;
		final double variance = meanSquareResult - meanResult * meanResult;
		final double stdev = Math.sqrt(variance);
		final double standardError = stdev / Math.sqrt(simulationCount);
		return new double[]{meanResult, stdev, standardError};
	}
	
	/**
	 * Get the list of defender strategy strings from the given file.
	 * 
	 * @param fileName the name of the file with the defender strategy
	 * strings
	 * @return a list of the defender strategy strings from the file
	 */
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
	
	/**
	 * Set up the game environment.
	 * @param defenderString the string name of the defender
	 * strategy
	 */
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
