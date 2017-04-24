package utils;

import game.GameSimulationSpec;
import game.MeanGameSimulationResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;

public final class JsonUtils {
	/**
	* Name of the file with default parameters.
	*/
	public static final String DEFAULT_FILE_NAME = "defaults.json";
	/**
	* Name of the folder that may contain simulation spec files.
	*/
	public static final String SIMSPEC_FOLDER_NAME = "simspecs";
	/**
	* The field in simulation_spec.json that has the environment
	* parameters.
	*/
	public static final String SIMSPEC_FIELD_NAME = "configuration";
	/**
	* The name of a simulation specification file.
	*/
	public static final String SIMSPEC_FILE_NAME = "simulation_spec.json";
	/**
	* The name prefix of an observation output file.
	*/
	public static final String OBS_FILE_PREFIX = "observation";
	/**
	* The file extension for Json files.
	*/
	public static final String JSON_SUFFIX = ".json";
	
	private JsonUtils() {
		// private constructor
	}
	
	public static GameSimulationSpec getSimSpecOrDefaults(final String folderName) {
		assert folderName != null;
		final String inputPath =
			folderName + File.separator + SIMSPEC_FILE_NAME;
		// read in the input file
		final String inputString = linesAsString(inputPath);
		final JsonObject inputJson = 
			new JsonParser().parse(inputString).getAsJsonObject();
		// get JsonObject of input file's appropriate field
		final JsonObject inputConfig =
			(JsonObject) inputJson.get(SIMSPEC_FIELD_NAME);
		
		// read in the default file
		final String defaultJsonString = linesAsString(DEFAULT_FILE_NAME);
		final JsonObject defaultAsJson = 
			new JsonParser().parse(defaultJsonString).getAsJsonObject();
		// get JsonObject of default file's appropriate field
		final JsonObject defaultConfig =
			(JsonObject) defaultAsJson.get(SIMSPEC_FIELD_NAME);
		
		// fill in any missing entries in inputConfig,
		// with data from the defaultConfig
		for (final Entry<String, JsonElement> entry: defaultConfig.entrySet()) {
			// iterate over all keys in default
			if (!inputConfig.has(entry.getKey())) {
				// missing from input. add to input.
				inputConfig.add(entry.getKey(), entry.getValue());
			}
		}
		final Gson gson = new Gson();
		// build object from input, with missing fields added from default.
		final GameSimulationSpec result =
			gson.fromJson(
				inputConfig,
				GameSimulationSpec.class
			);
		return result;
	}
	
	public static String getAttackerString(final String folderName) {
		assert folderName != null;
		final String inputPath =
			folderName + File.separator + SIMSPEC_FILE_NAME;
		final String jsonString = linesAsString(inputPath);
		final JsonObject fileAsJson = 
			new JsonParser().parse(jsonString).getAsJsonObject();
		final String assignment = "assignment";
		final JsonObject assignmentObject =
			(JsonObject) fileAsJson.get(assignment);
		final String attacker = "attacker";
		final JsonArray attackerArray =
			(JsonArray) assignmentObject.get(attacker);
		final JsonElement attackerElement = attackerArray.get(0);
		return attackerElement.toString().replaceAll("\"", "");
	}
	
	public static String getDefenderString(final String folderName) {
		assert folderName != null;
		final String inputPath =
			folderName + File.separator + SIMSPEC_FILE_NAME;
		final String jsonString = linesAsString(inputPath);
		final JsonObject fileAsJson = 
			new JsonParser().parse(jsonString).getAsJsonObject();
		final String assignment = "assignment";
		final JsonObject assignmentObject =
			(JsonObject) fileAsJson.get(assignment);
		final String defender = "defender";
		final JsonArray defenderArray =
			(JsonArray) assignmentObject.get(defender);
		final JsonElement defenderElement = defenderArray.get(0);
		return defenderElement.toString().replaceAll("\"", "");
	}
	
	public static ObservationStruct fromObservationFile(final String pathName) {
		if (pathName == null) {
			throw new IllegalArgumentException();
		}
		// read in the observation file
		final String obsString = linesAsString(pathName);
		return fromObservationString(obsString);
	}
	
	public static ObservationStruct fromObservationString(final String obsString) {
		if (obsString == null) {
			throw new IllegalArgumentException();
		}
		final JsonObject obsJson = 
			new JsonParser().parse(obsString).getAsJsonObject();
		final JsonObject simSpecJson = (JsonObject) obsJson.get("features");
		final Gson gson = new Gson();
		// build object from input, with missing fields added from default.
		final GameSimulationSpec gameSimSpec =
			gson.fromJson(
				simSpecJson,
				GameSimulationSpec.class
			);
		
		final JsonArray playersJson = (JsonArray) obsJson.get("players");
		final JsonObject attackerJson = (JsonObject) playersJson.get(0);
		final JsonObject defenderJson = (JsonObject) playersJson.get(1);
		final String attackerStratString = attackerJson.get("strategy").toString().replaceAll("\"", "");
		final String defenderStratString = defenderJson.get("strategy").toString().replaceAll("\"", "");
		final String attackerName = EncodingUtils.getStrategyName(attackerStratString);
		final String defenderName = EncodingUtils.getStrategyName(defenderStratString);

		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerStratString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderStratString);
		final Attacker attacker =
			AgentFactory.createAttacker(
				attackerName, attackerParams, gameSimSpec.getDiscFact());
		final Defender defender =
			AgentFactory.createDefender(
				defenderName, defenderParams, gameSimSpec.getDiscFact());
		// TODO
		return null;
	}

	public static String getObservationString(
		final MeanGameSimulationResult simResult, final String attackerStrategyString
		, final String defenderStrategyString, final GameSimulationSpec simSpec) {
		assert simResult != null
			&& attackerStrategyString != null
			&& defenderStrategyString != null
			&& simSpec != null;
		final JsonObject obsObject = new JsonObject();
		
		final JsonArray playersArray = new JsonArray();
		
		final JsonObject attackerObject = new JsonObject();
		final String role = "role";
		final String attacker = "attacker";
		final String strategy = "strategy";
		final String payoff = "payoff";
		final String features = "features";
		attackerObject.addProperty(role, attacker);
		attackerObject.addProperty(strategy, attackerStrategyString);
		attackerObject.addProperty(payoff, simResult.getMeanAttPayoff());
		attackerObject.add(features, new JsonObject());
		playersArray.add(attackerObject);
		
		final JsonObject defenderObject = new JsonObject();
		final String defender = "defender";
		defenderObject.addProperty(role, defender);
		defenderObject.addProperty(strategy, defenderStrategyString);
		defenderObject.addProperty(payoff, simResult.getMeanDefPayoff());
		defenderObject.add(features, new JsonObject());
		playersArray.add(defenderObject);
		
		final String simSpecJsonString = new Gson().toJson(simSpec);
		final JsonObject simSpecJsonObject =
			(JsonObject) new JsonParser().parse(simSpecJsonString);
		obsObject.add(features, simSpecJsonObject);
		
		final String players = "players";
		obsObject.add(players, playersArray);
		
		// for pretty-printing
		final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
		return gson.toJson(obsObject).toString();
	}
	
	public static void printObservationToFile(final String folderName, final String obsString) {
		assert folderName != null && obsString != null;
		final int maxSuffixValue =
			maxSuffixValue(folderName, OBS_FILE_PREFIX);
		final String fileName = folderName + File.separator
			+ OBS_FILE_PREFIX + (maxSuffixValue + 1) + JSON_SUFFIX;
		final File file = new File(fileName);
		try {
			final BufferedWriter output =
				new BufferedWriter(new FileWriter(file));
			output.write(obsString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String linesAsString(final String fileName) {
		assert fileName != null;
		final StringBuilder builder = new StringBuilder();
		try {
			 final BufferedReader br =
				new BufferedReader(new FileReader(new File(fileName)));
			String line = null;
			while ((line = br.readLine()) != null) {
				builder.append(line);
				builder.append('\n');
			}
			br.close();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
		return builder.toString();
	}
		
	private static int maxSuffixValue(final String folderPath, final String fileNamePrefix) {
		assert folderPath != null && fileNamePrefix != null;
		final Set<Integer> values = new HashSet<Integer>();
		final File[] files = new File(folderPath).listFiles();
		if (files == null) {
			return -1;
		}
		for (final File file: files) {
			if (file.getName().startsWith(fileNamePrefix)) {
				final String fileName = file.getName();
				final String nameNumberSuffix =
					fileName.substring(
						fileNamePrefix.length(),
						fileName.length() - JSON_SUFFIX.length()
					);
				try {
					final Integer intVal = Integer.parseInt(nameNumberSuffix);
					values.add(intVal);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		int result = -1;
		for (int value: values) {
			if (value > result) {
				result = value;
			}
		}
		return result;
	 }
	
	public static void main(final String[] args) {
		final String pathName = "simspecs/observation0.json";
		fromObservationFile(pathName);
	}
	
	public final class ObservationStruct {
		private final MeanGameSimulationResult simResult;
		private final String attackerStrategyString;
		private final String defenderStrategyString;
		private final GameSimulationSpec simSpec;
		
		public ObservationStruct(final MeanGameSimulationResult aSimResult,
			final String aAttackerStrategyString,
			final String aDefenderStrategyString,
			final GameSimulationSpec aSimSpec
		) {
			if (aSimResult == null || aAttackerStrategyString == null
				|| aDefenderStrategyString == null || aSimSpec == null
			) {
				throw new IllegalArgumentException();
			}
			this.simResult = aSimResult;
			this.attackerStrategyString = aAttackerStrategyString;
			this.defenderStrategyString = aDefenderStrategyString;
			this.simSpec = aSimSpec;
		}

		public MeanGameSimulationResult getSimResult() {
			return this.simResult;
		}

		public String getAttackerStrategyString() {
			return this.attackerStrategyString;
		}

		public String getDefenderStrategyString() {
			return this.defenderStrategyString;
		}

		public GameSimulationSpec getSimSpec() {
			return this.simSpec;
		}
	}
}
