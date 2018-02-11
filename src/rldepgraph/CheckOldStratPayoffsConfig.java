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
import java.util.Random;

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
public final class CheckOldStratPayoffsConfig {

	/**
	 * Used to run the game logic.
	 */
	private GameSimulation sim;

	/**
	 * Lists weight of each attacker type in the mixed strategy,
	 * in order matching attackers.
	 */
	private List<Double> attackerWeights;

	/**
	 * Lists agent for each attacker type of the mixed strategy,
	 * in order matching attackerWeights.
	 */
	private List<Attacker> attackers;

	/**
	 * The name of the folder with simulation_spec.json.
	 */
	private final String simSpecFolderName;

	/**
	 * Used to get random values for selection cutoff.
	 */
	private static final Random RAND = new Random();

	/**
	 * Set up the simulation spec and attacker mixed
	 * strategy from the inputs.
	 * @param simSpecFolder the folder with the simulation_spec.json
	 * file
	 * @param attackMixedStratFile the file with the attacker mixed
	 * strategy
	 */
	private CheckOldStratPayoffsConfig(
		final String simSpecFolder,
		final String attackMixedStratFile
	) {
		if (simSpecFolder == null || attackMixedStratFile == null) {
			throw new IllegalArgumentException();
		}
		this.simSpecFolderName = simSpecFolder;
		this.attackers = new ArrayList<Attacker>();
		this.attackerWeights = new ArrayList<Double>();
		setupAttackersAndWeights(attackMixedStratFile, getDiscFact());
	}

	/**
	 * Main method.
	 * 
	 * @param args has the number of iterations per defender strategy,
	 * the folder with the simulation_spec.json file, and
	 * the file with the attacker mixed strategy
	 */
	public static void main(final String[] args) {
		final int expectedArgs = 4;
		if (args == null || args.length != expectedArgs) {
			throw new IllegalArgumentException(
			"Need 4 args: iterationsPerStrategy, simSpecFolder, "
			+ "attackMixedStratFile, graphFile");
		}
		final int iterationsPerStrategy = Integer.parseInt(args[0]);
		if (iterationsPerStrategy < 2) {
			throw new IllegalArgumentException();
		}
		final String simSpecFolder = args[1];
		final String attackMixedStratFileName = args[2];
		final int nextIndex = 3;
		final String graphFileName = args[nextIndex];
		
		final String defStratStringFileName = "defStratStrings.txt";
		final List<String> defStratStrings =
			getDefenderStrings(defStratStringFileName);
		final CheckOldStratPayoffsConfig checker =
			new CheckOldStratPayoffsConfig(
				simSpecFolder, attackMixedStratFileName);
		checker.printAllPayoffs(
			defStratStrings, iterationsPerStrategy,
			graphFileName);
	}

	/**
	 * Initialize attackers and attackerWeights from the given file.
	 * @param attackMixedStratFileName a file name for the mixed strategy.
	 * The mixed strategy should have an attacker type per line,
	 * with the type string followed by tab, followed by the weight as a double.
	 * @param discFact the discount factor of the game
	 */
	private void setupAttackersAndWeights(
		final String attackMixedStratFileName,
		final double discFact
	) {
		this.attackers.clear();
		this.attackerWeights.clear();

		final List<String> lines = getLines(attackMixedStratFileName);
		double totalWeight = 0.0;
		for (final String line: lines) {
			final String strippedLine = line.trim();
			if (strippedLine.length() > 0) {
				String[] lineSplit = strippedLine.split("\t");
				if (lineSplit.length != 2) {
					throw new IllegalStateException(
						"Wrong split: " + strippedLine);					
				}
				final String attackerString = lineSplit[0];
				final String weightString = lineSplit[1];
				final double weight = Double.parseDouble(weightString);
				if (weight <= 0.0 || weight > 1.0) {
					throw new IllegalStateException(
						"Weight is not in [0, 1): " + weight);
				}
				totalWeight += weight;
				
				final String attackerName =
					EncodingUtils.getStrategyName(attackerString);
				final Map<String, Double> attackerParams =
					EncodingUtils.getStrategyParams(attackerString);
				
				final Attacker attacker =
					AgentFactory.createAttacker(
						attackerName, attackerParams, discFact);
				this.attackers.add(attacker);
				this.attackerWeights.add(weight);
			}
		}
		final double tol = 0.001;
		if (Math.abs(totalWeight - 1.0) > tol) {
			throw new IllegalStateException(
				"Weights do not sum to 1.0: " + this.attackerWeights);
		}
	}

	/**
	 * Return the lines from the given file name in order.
	 * @param fileName the file name to draw from
	 * @return a list of the lines of the file as strings
	 */
	private static List<String> getLines(final String fileName) {
		final List<String> result = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       result.add(line);
		    }
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Print the mean, standard deviation, and standard error of
	 * payoffs for each given defender strategy.
	 * 
	 * @param defenderStrings list of defender strategy strings
	 * @param iterationsPerStrategy how many simulations to run
	 * per strategy
	 * @param graphFileName the name of the graph file to load
	 */
	private void printAllPayoffs(
		final List<String> defenderStrings,
		final int iterationsPerStrategy,
		final String graphFileName
	) {
		System.out.println(
			"Iterations per strategy: " + iterationsPerStrategy + "\n");
		System.out.println("Attacker strats: " + this.attackers + "\n");
		System.out.println("Attacker weights: " + this.attackerWeights + "\n");
		final DecimalFormat format = new DecimalFormat("#.##");

		final long startTime = System.currentTimeMillis();
		for (final String defenderString: defenderStrings) {
			setupEnvironment(defenderString, graphFileName);
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
			// update the attacker at random from the mixed strategy.
			this.sim.setAttacker(drawRandomAttacker());
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
		double stdev = 0.0;
		if (variance > 0.0) {
			stdev = Math.sqrt(variance);
		}
		final double standardError = stdev / Math.sqrt(simulationCount);
		return new double[]{meanResult, stdev, standardError};
	}

	/**
	 * Draw a random attacker from attackers, based on the probabilities
	 * in attackerWeights.
	 * @return a randomly drawn attacker from attackers
	 */
	private Attacker drawRandomAttacker() {
		if (this.attackers == null || this.attackers.isEmpty()) {
			throw new IllegalStateException();
		}
		
		final double randDraw = RAND.nextDouble();
		double total = 0.0;
		for (int i = 0; i < this.attackerWeights.size(); i++) {
			total += this.attackerWeights.get(i);
			if (randDraw <= total) {
				return this.attackers.get(i);
			}
		}
		return this.attackers.get(this.attackers.size() - 1);
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
		    	String lineStripped = line.trim();
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
	 * Get the simulation spec's discount factor.
	 * @return the discount factor of the game
	 */
	private double getDiscFact() {
		if (this.simSpecFolderName == null) {
			throw new IllegalStateException();
		}
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(this.simSpecFolderName);	
		return simSpec.getDiscFact();
	}

	/**
	 * Set up the game environment.
	 * @param defenderString the string name of the defender
	 * strategy
	 * @param graphFileName the name of the graph file to load
	 */
	private void setupEnvironment(
		final String defenderString,
		final String graphFileName) {
		final String graphFolderName = "graphs";
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(this.simSpecFolderName);	
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ graphFileName;
		DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
				
		// Load players
		final double discFact = simSpec.getDiscFact();

		final String attackerString =
			JsonUtils.getAttackerString(this.simSpecFolderName);
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
