package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class DefenderBelief {
	private final Map<GameState, Double> gameStateMap;
	
	public DefenderBelief() {
		this.gameStateMap = new HashMap<GameState, Double>();
	}
	
	public Double getProbability(final GameState gameState) {
		if (gameState == null) {
			throw new IllegalArgumentException();
		}
		return this.gameStateMap.get(gameState);
	}
	
	public void addState(final GameState gameState, final Double prob) {
		this.gameStateMap.put(gameState, prob);
	}
	
	public Map<GameState, Double> getGameStateMap() {
		return this.gameStateMap;
	}
	
	public void print() {
		for (Entry<GameState, Double> entry : this.gameStateMap.entrySet()) {
			entry.getKey().print();
			System.out.println("State Prob:" + entry.getValue());
		}
	}
}
