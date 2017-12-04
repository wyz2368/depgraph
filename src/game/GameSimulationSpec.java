package game;

public final class GameSimulationSpec {
	private final int numTimeStep; // total number of time steps
	private final int numSim; // number of simulation per game configuration
	
	// For graph generation
	private final int graphID;
	private final int numNode; // number of nodes 
	private final int numEdge; // number of edges
	private final int numTarget; // minimum number of targets
	
	private final double discFact; // for computing payoff
	
	public GameSimulationSpec(final int aNumTimeStep, final int aNumSim,
		final int aGraphID, final int aNumNode,
		final int aNumEdge, final int aNumTarget,
		final double aDiscFact) {
		if (aNumTimeStep < 1 || aNumSim < 1 || aNumNode < 1 || aNumEdge < 0
			|| aNumTarget < 1 || aDiscFact <= 0.0 || aDiscFact > 1.0) {
			throw new IllegalArgumentException();
		}
		this.numTimeStep = aNumTimeStep;
		this.numSim = aNumSim;
		this.graphID = aGraphID;
		this.numNode = aNumNode;
		this.numEdge = aNumEdge;
		this.numTarget = aNumTarget;
		this.discFact = aDiscFact;
	}
	
	public int getNumTimeStep() {
		return this.numTimeStep;
	}
	
	public int getNumSim() {
		return this.numSim;
	}
	
	public int getGraphID() {
		return this.graphID;
	}
	
	public int getNumNode() {
		return this.numNode;
	}
	
	public int getNumEdge() {
		return this.numEdge;
	}
	
	public int getNumTarget() {
		return this.numTarget;
	}
	
	public double getDiscFact() {
		return this.discFact;
	}

	@Override
	public String toString() {
		return "GameSimulationSpec [numTimeStep="
			+ this.numTimeStep + ", numSim="
			+ this.numSim + ", graphID=" + this.graphID
			+ ", numNode=" + this.numNode
			+ ", numEdge=" + this.numEdge + ", numTarget=" + this.numTarget
			+ ", discFact=" + this.discFact + "]";
	}
}
