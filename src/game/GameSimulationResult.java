package game;

import graph.Node;

import java.util.ArrayList;
import java.util.List;

import model.GameSample;
import model.GameState;

public class GameSimulationResult {
	GameState initialState;
	List<GameSample> gameSampleList;
	double defPayoff;
	double attPayoff;
	public GameSimulationResult(GameState initialState, List<GameSample> gameSampleList, double defPayoff, double attPayoff)
	{
		this.initialState = initialState;
		this.gameSampleList = gameSampleList;
		this.defPayoff = defPayoff;
		this.attPayoff = attPayoff;
	}
	public GameSimulationResult()
	{
		this.initialState = new GameState();
		this.gameSampleList = new ArrayList<GameSample>();
		this.defPayoff = 0.0;
		this.attPayoff = 0.0;
	}
	public void addEnabledNodetoInitialState(Node node)
	{
		this.initialState.addEnabledNode(node);
	}
	public void setInitialState(GameState initialState)
	{
		this.initialState = initialState;
	}
	public void setGameSampleList(List<GameSample> gameSampleList)
	{
		this.gameSampleList = gameSampleList;
	}
	public void addGameSample(GameSample gameSample)
	{
		this.gameSampleList.add(gameSample);
	}
	public List<GameSample> getGameSampleList()
	{
		return this.gameSampleList;
	}
	public void setDefPayoff(double defPayoff)
	{
		this.defPayoff = defPayoff;
	}
	public void setAttPayoff(double attPayoff)
	{
		this.attPayoff = attPayoff;
	}
	public double getDefPayoff()
	{
		return this.defPayoff;
	}
	public double getAttPayoff()
	{
		return this.attPayoff;
	}
	
	/**
	 * @param folderName  
	 */
	public void printResult(String folderName)
	{
		// do nothing
	}
	/**
	 * @param folderName  
	 */
	public void printPayoff(String folderName)
	{
		// do nothing
	}
	public void printPayoff()
	{
		System.out.println("Defender payoff: " + this.defPayoff);
		System.out.println("Attacker payoff: " + this.attPayoff);
	}
	public void clear()
	{
		this.initialState.clear();
		for(GameSample gameSample : this.gameSampleList)
			gameSample.clear();
		this.gameSampleList.clear();
	}
}
