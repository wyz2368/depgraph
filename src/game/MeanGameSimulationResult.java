package game;

public final class MeanGameSimulationResult {
	
	private double meanDefPayoff;
	private double meanAttPayoff; 
	private int numSimulation;
	private int numTimeStep;
	
	public MeanGameSimulationResult() {
		this.meanDefPayoff = 0.0;
		this.meanAttPayoff = 0.0;
		this.numSimulation = 0;
		this.numTimeStep = 0;
	}
	
	public MeanGameSimulationResult(final double aMeanDefPayoff, final double aMeanAttPayoff,
		final int aNumSimulation, final int aNumTimeStep) {
		if (aNumSimulation < 1 || aNumTimeStep < 1) {
			throw new IllegalArgumentException();
		}
		this.meanDefPayoff = aMeanDefPayoff;
		this.meanAttPayoff = aMeanAttPayoff;
		this.numSimulation = aNumSimulation;
		this.numTimeStep = aNumTimeStep;
	}
	
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

	@Override
	public String toString() {
		return "MeanGameSimulationResult [meanDefPayoff=" + this.meanDefPayoff
			+ ", meanAttPayoff=" + this.meanAttPayoff + ", numSimulation="
			+ this.numSimulation + ", numTimeStep=" + this.numTimeStep + "]";
	}
}
