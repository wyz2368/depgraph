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
	
	public GameSimulationResult(final GameState aInitialState,
		final List<GameSample> aGameSampleList,
		final double aDefPayoff, final double aAttPayoff) {
		if (aInitialState == null || aGameSampleList == null) {
			throw new IllegalArgumentException();
		}
		this.initialState = aInitialState;
		this.gameSampleList = aGameSampleList;
		this.defPayoff = aDefPayoff;
		this.attPayoff = aAttPayoff;
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
	
	public GameState getInitialState() {
		return this.initialState;
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
		this.defPayoff = 0.0;
		this.attPayoff = 0.0;
	}

	@Override
	public String toString() {
		return "GameSimulationResult [initialState=" + this.initialState
			+ ", defPayoff=" + this.defPayoff
			+ ", attPayoff=" + this.attPayoff + "]";
	}
}
