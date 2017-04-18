package game;

import graph.Node;

import java.util.ArrayList;
import java.util.List;

import model.GameSample;
import model.GameState;

public final class GameSimulationResult {
	private GameState initialState;
	private List<GameSample> gameSampleList;
	private double defPayoff;
	private double attPayoff;
	
	public GameSimulationResult(final GameState initialState, final List<GameSample> gameSampleList,
		final double defPayoff, final double attPayoff) {
		this.initialState = initialState;
		this.gameSampleList = gameSampleList;
		this.defPayoff = defPayoff;
		this.attPayoff = attPayoff;
	}
	
	public GameSimulationResult() {
		this.initialState = new GameState();
		this.gameSampleList = new ArrayList<GameSample>();
		this.defPayoff = 0.0;
		this.attPayoff = 0.0;
	}
	
	public void addEnabledNodetoInitialState(final Node node) {
		this.initialState.addEnabledNode(node);
	}
	
	public void setInitialState(final GameState aInitialState) {
		this.initialState = aInitialState;
	}
	
	public void setGameSampleList(final List<GameSample> aGameSampleList) {
		this.gameSampleList = aGameSampleList;
	}
	
	public void addGameSample(final GameSample gameSample) {
		this.gameSampleList.add(gameSample);
	}
	
	public List<GameSample> getGameSampleList() {
		return this.gameSampleList;
	}
	
	public void setDefPayoff(final double aDefPayoff) {
		this.defPayoff = aDefPayoff;
	}
	
	public void setAttPayoff(final double aAttPayoff) {
		this.attPayoff = aAttPayoff;
	}
	
	public double getDefPayoff() {
		return this.defPayoff;
	}
	
	public double getAttPayoff() {
		return this.attPayoff;
	}
	
	/**
	 * @param folderName  
	 */
	public void printResult(final String folderName) {
		// do nothing
	}
	
	/**
	 * @param folderName  
	 */
	public void printPayoff(final String folderName) {
		// do nothing
	}
	
	public void printPayoff() {
		System.out.println("Defender payoff: " + this.defPayoff);
		System.out.println("Attacker payoff: " + this.attPayoff);
	}
	
	public void clear() {
		this.initialState.clear();
		for (GameSample gameSample : this.gameSampleList) {
			gameSample.clear();
		}
		this.gameSampleList.clear();
	}
}
