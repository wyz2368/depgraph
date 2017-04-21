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
	
	public GameSimulationSpec(final int numTimeStep, final int numSim,
		final int graphID, final int numNode,
		final int numEdge, final int numTarget,
		final double discFact) {
		if (numTimeStep < 1 || numSim < 1 || numNode < 1 || numEdge < 0
			|| numTarget < 1 || discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		this.numTimeStep = numTimeStep;
		this.numSim = numSim;
		this.graphID = graphID;
		this.numNode = numNode;
		this.numEdge = numEdge;
		this.numTarget = numTarget;
		this.discFact = discFact;
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
}
