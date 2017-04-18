package game;

public final class MeanGameSimulationResult {
	private double meanDefPayoff = 0.0;
	private double meanAttPayoff = 0.0; 
	private int numSimulation = 0;
	public void updateMeanSimulationResult(final GameSimulationResult newSimulationResult) {
		this.meanDefPayoff += newSimulationResult.getDefPayoff();
		this.meanAttPayoff += newSimulationResult.getAttPayoff();
		this.numSimulation++;
	}
	public double getMeanDefPayoff() {
		return this.meanDefPayoff / this.numSimulation;
	}
	public double getMeanAttPayoff() {
		return this.meanAttPayoff / this.numSimulation;
	}
}
