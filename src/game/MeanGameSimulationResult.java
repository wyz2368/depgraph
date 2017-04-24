package game;

public final class MeanGameSimulationResult {
	
	private double meanDefPayoff = 0.0;
	private double meanAttPayoff = 0.0; 
	private int numSimulation = 0;
	private int numTimeStep = 0;
	
	public void updateMeanSimulationResult(final GameSimulationResult newSimulationResult) {
		assert newSimulationResult != null;
		this.meanDefPayoff += newSimulationResult.getDefPayoff();
		this.meanAttPayoff += newSimulationResult.getAttPayoff();
		this.numSimulation++;
		this.numTimeStep = newSimulationResult.getGameSampleList().size();
	}
	
	public double getMeanDefPayoff() {
		return this.meanDefPayoff / this.numSimulation;
	}
	
	public double getMeanAttPayoff() {
		return this.meanAttPayoff / this.numSimulation;
	}
	
	public int getNumSims() {
		return this.numSimulation;
	}
	
	public int getNumTimeStep() {
		return this.numTimeStep;
	}
}
