package game;

public class MeanGameSimulationResult {
	double meanDefPayoff = 0.0;
	double meanAttPayoff = 0.0; 
	int numSimulation = 0;
	public void updateMeanSimulationResult(GameSimulationResult newSimulationResult){
		meanDefPayoff += newSimulationResult.getDefPayoff();
		meanAttPayoff += newSimulationResult.getAttPayoff();
		numSimulation++;
	}
	public double getMeanDefPayoff()
	{
		return this.meanDefPayoff / this.numSimulation;
	}
	public double getMeanAttPayoff()
	{
		return this.meanAttPayoff / this.numSimulation;
	}
}
