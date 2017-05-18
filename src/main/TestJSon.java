package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import utils.JsonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class TestJSon {
	private static String PLAYERS = "players";
	private static String STRATEGIES = "strategies";
	private static String PROFILES = "profiles";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filePathName = "/Users/thanhnguyen/Documents/WORKS/ATTACK_GRAPH/EXPERIMENTS/E3_SepLay/1627-ga.json";
		final String inputString = JsonUtils.linesAsString(filePathName);
		final JsonObject inputJson = 
				new JsonParser().parse(inputString).getAsJsonObject();
		JsonObject players = inputJson.get(PLAYERS).getAsJsonObject(); 
		JsonObject strategies = inputJson.get(STRATEGIES).getAsJsonObject();
		JsonArray profiles = inputJson.get(PROFILES).getAsJsonArray();
		List<String> playerList = new ArrayList<String>();
		for(Entry<String, JsonElement> entry : players.entrySet()) {
			playerList.add(entry.getKey());
		}
		
//		for (Entry<String, JsonElement> entry : strategies.entrySet()) {
//			String player = entry.getKey();
//			System.out.println("\n" + player + ": ");
//			JsonArray strategySet = entry.getValue().getAsJsonArray();
//			for (int i = 0; i < strategySet.size(); i++) {
//				String strategyName = strategySet.get(i).getAsString();
//				System.out.println(strategyName);
//			}
//		}
		
		for (int i = 0; i < 1; i++) {
			JsonObject profile = profiles.get(i).getAsJsonObject();
			double attPayoff = profile.get("attacker").getAsJsonArray().get(0).getAsJsonArray().get(2).getAsDouble();
//			double defPayoff = profile.get("defender").getAsDouble();
			System.out.println(attPayoff);
		}
	}

}
