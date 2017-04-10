package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DefenderBelief {
	Map<GameState, Double> gameStateMap;
	public DefenderBelief()
	{
		this.gameStateMap = new HashMap<GameState, Double>();
	}
	public Double getProbability(GameState gameState)
	{
		return this.gameStateMap.get(gameState);
	}
	public void addState(GameState gameState, Double prob)
	{
		this.gameStateMap.put(gameState, prob);
	}
	public Map<GameState, Double> getGameStateMap()
	{
		return this.gameStateMap;
	}
	public void clear()
	{
		for(Entry<GameState, Double> entry : gameStateMap.entrySet())
		{
			entry.getKey().clear();
		}
		this.gameStateMap.clear();
	}
	public void print()
	{
		for(Entry<GameState, Double> entry : gameStateMap.entrySet())
		{
			entry.getKey().print();
			System.out.println("State Prob:" + entry.getValue());
		}
	}
}
