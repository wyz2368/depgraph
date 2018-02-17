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
 * Get the mean payoff for each old attacker strategy,
 * against the given defender strategy.
 */
public final class CheckOldAttStratPayoffsConfig {

	/**
	 * Used to run the game logic.
	 */
	private GameSimulation sim;

	/**
	 * Lists weight of each defender type in the mixed strategy,
	 * in order matching defenders.
	 */
	private List<Double> defenderWeights;

	/**
	 * Lists agent for each defender type of the mixed strategy,
	 * in order matching defenderWeights.
	 */
	private List<Defender> defenders;

	/**
	 * The name of the folder with simulation_spec.json.
	 */
	private final String simSpecFolderName;

	/**
	 * Used to get random values for selection cutoff.
	 */
	private static final Random RAND = new Random();

	/**
	 * Set up the simulation spec and defender mixed
	 * strategy from the inputs.
	 * @param simSpecFolder the folder with the simulation_spec.json
	 * file
	 * @param defMixedStratFile the file with the defender mixed
	 * strategy
	 */
	private CheckOldAttStratPayoffsConfig(
		final String simSpecFolder,
		final String defMixedStratFile
	) {
		if (simSpecFolder == null || defMixedStratFile == null) {
			throw new IllegalArgumentException();
		}
		this.simSpecFolderName = simSpecFolder;
		this.defenders = new ArrayList<Defender>();
		this.defenderWeights = new ArrayList<Double>();
		setupDefendersAndWeights(defMixedStratFile, getDiscFact());
	}

	/**
	 * Main method.
	 * 
	 * @param args has the number of iterations per attacker strategy,
	 * the folder with the simulation_spec.json file,
	 * the file with the defender mixed strategy, and the 
	 * graph file.
	 */
	public static void main(final String[] args) {
		final int expectedArgs = 4;
		if (args == null || args.length != expectedArgs) {
			throw new IllegalArgumentException(
			"Need 4 args: iterationsPerStrategy, simSpecFolder, "
			+ "defMixedStratFile, graphFile");
		}
		final int iterationsPerStrategy = Integer.parseInt(args[0]);
		if (iterationsPerStrategy < 2) {
			throw new IllegalArgumentException();
		}
		final String simSpecFolder = args[1];
		final String defMixedStratFileName = args[2];
		final int nextIndex = 3;
		final String graphFileName = args[nextIndex];

		final CheckOldAttStratPayoffsConfig checker =
			new CheckOldAttStratPayoffsConfig(
				simSpecFolder, defMixedStratFileName);

		final String attStratStringFileName = "attStratStrings.txt";
		final List<String> attStratStrings =
			getAttackerStrings(attStratStringFileName);
		checker.printAllPayoffs(
			attStratStrings, iterationsPerStrategy,
			graphFileName);
	}

	/**
	 * Initialize defenders and defenderWeights from the given file.
	 * @param defMixedStratFileName a file name for the mixed strategy.
	 * The mixed strategy should have a defender type per line,
	 * with the type string followed by tab, followed by the weight as a double.
	 * @param discFact the discount factor of the game
	 */
	private void setupDefendersAndWeights(
		final String defMixedStratFileName,
		final double discFact
	) {
		this.defenders.clear();
		this.defenderWeights.clear();

		final List<String> lines = getLines(defMixedStratFileName);
		double totalWeight = 0.0;
		for (final String line: lines) {
			final String strippedLine = line.trim();
			if (strippedLine.length() > 0) {
				final String[] lineSplit = strippedLine.split("\t");
				if (lineSplit.length != 2) {
					throw new IllegalStateException(
						"Wrong split: " + strippedLine);					
				}
				final String defenderString = lineSplit[0];
				final String weightString = lineSplit[1];
				final double weight = Double.parseDouble(weightString);
				if (weight <= 0.0 || weight > 1.0) {
					throw new IllegalStateException(
						"Weight is not in [0, 1): " + weight);
				}
				totalWeight += weight;
				
				final String defenderName =
					EncodingUtils.getStrategyName(defenderString);
				final Map<String, Double> defenderParams =
					EncodingUtils.getStrategyParams(defenderString);
				
				final Defender defender =
					AgentFactory.createDefender(
						defenderName, defenderParams, discFact);
				this.defenders.add(defender);
				this.defenderWeights.add(weight);
			}
		}
		final double tol = 0.001;
		if (Math.abs(totalWeight - 1.0) > tol) {
			throw new IllegalStateException(
				"Weights do not sum to 1.0: " + this.defenderWeights);
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
	 * payoffs for each given attacker strategy.
	 * 
	 * @param attackerStrings list of attacker strategy strings
	 * @param iterationsPerStrategy how many simulations to run
	 * per strategy
	 * @param graphFileName the name of the graph file to load
	 */
	private void printAllPayoffs(
		final List<String> attackerStrings,
		final int iterationsPerStrategy,
		final String graphFileName
	) {
		System.out.println(
			"Iterations per strategy: " + iterationsPerStrategy + "\n");
		System.out.println("Defender strats: " + this.defenders + "\n");
		System.out.println("Defender weights: " + this.defenderWeights + "\n");
		final DecimalFormat format = new DecimalFormat("#.##");

		final long startTime = System.currentTimeMillis();
		for (final String attackerString: attackerStrings) {
			setupEnvironment(attackerString, graphFileName);
			final double[] payoffStats  = getPayoffStats(iterationsPerStrategy);
			final double meanPayoff = payoffStats[0];
			final double stdev = payoffStats[1];
			final double standardError = payoffStats[2];
			System.out.println(attackerString);
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
	 * of the attacker payoffs.
	 * 
	 * @param simulationCount how many simulations to use
	 * @return a double array with the mean attacker payoff,
	 * standard deviation, and standard error of the mean in order.
	 */
	private double[] getPayoffStats(
		final int simulationCount
	) {
		double defPayoffTotal = 0.0;
		double sumSquaredPayoff = 0.0;
		for (int i = 0; i < simulationCount; i++) {
			this.sim.reset();
			// update the defender at random from the mixed strategy.
			this.sim.setDefender(drawRandomDefender());
			this.sim.runSimulation();
			final double curPayoff =
				this.sim.getSimulationResult().getAttPayoff();
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
	 * Draw a random defender from defenders, based on the probabilities
	 * in defenderWeights.
	 * @return a randomly drawn defender from defenders
	 */
	private Defender drawRandomDefender() {
		if (this.defenders == null || this.defenders.isEmpty()) {
			throw new IllegalStateException();
		}
		
		final double randDraw = RAND.nextDouble();
		double total = 0.0;
		for (int i = 0; i < this.defenderWeights.size(); i++) {
			total += this.defenderWeights.get(i);
			if (randDraw <= total) {
				return this.defenders.get(i);
			}
		}
		return this.defenders.get(this.defenders.size() - 1);
	}

	/**
	 * Get the list of attacker strategy strings from the given file.
	 * 
	 * @param fileName the name of the file with the attacker strategy
	 * strings
	 * @return a list of the attacker strategy strings from the file
	 */
	private static List<String> getAttackerStrings(final String fileName) {
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
	 * @param attackerString the string name of the attacker
	 * strategy
	 * @param graphFileName the name of the graph file to load
	 */
	private void setupEnvironment(
		final String attackerString,
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

		final String defenderString =
			JsonUtils.getDefenderString(this.simSpecFolderName);
		final String defenderName =
			EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, discFact);
		
		final String attackerName =
			EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, discFact);

		RandomDataGenerator rng = new RandomDataGenerator();
		final int numTimeStep = simSpec.getNumTimeStep();
		this.sim = new GameSimulation(
			depGraph, attacker, defender, rng, numTimeStep, discFact);
	}
}
