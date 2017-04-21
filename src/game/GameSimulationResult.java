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
		if (initialState == null || gameSampleList == null) {
			throw new IllegalArgumentException();
		}
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
		assert node != null;
		this.initialState.addEnabledNode(node);
	}
	
	public void setInitialState(final GameState aInitialState) {
		assert aInitialState != null;
		this.initialState = aInitialState;
	}
	
	public void setGameSampleList(final List<GameSample> aGameSampleList) {
		assert aGameSampleList != null;
		this.gameSampleList = aGameSampleList;
	}
	
	public void addGameSample(final GameSample gameSample) {
		assert gameSample != null;
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
	public static void printResult(final String folderName) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param folderName  
	 */
	public static void printPayoff(final String folderName) {
		throw new UnsupportedOperationException();
	}
	
	public void printPayoff() {
		GameSimulation.printIfDebug("Defender payoff: " + this.defPayoff);
		GameSimulation.printIfDebug("Attacker payoff: " + this.attPayoff);
	}
	
	public void clear() {
		this.initialState.clear();
		for (GameSample gameSample : this.gameSampleList) {
			gameSample.clear();
		}
		this.gameSampleList.clear();
	}
}
