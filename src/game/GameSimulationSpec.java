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

	// private double aRewardLB;
	//	private double aRewardUB;
	//	private double dPenaltyLB;
	//	private double dPenaltyUB;
	//	
	//	private int totalNumAlert; // total number of security alerts
	//	private int minNumAlert; // minimum number of alerts for each node
	//	private int maxNumAlert; // maximum number of alerts for each node
	
	// For strategy generation
	// Note: should we use uniform distribution to generate number of nodes to enable or disable each time step???
	//	private double fixPoissonParam; // used if fixed mean is used
	//	private double adaptPoissonRatio; // used if mean is chosen according to the number of available nodes
	//	private double qrParam; // for attacker 
	//	private double logisParam; // for defender
	
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
