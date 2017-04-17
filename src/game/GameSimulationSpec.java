package game;

public final class GameSimulationSpec {
	private int numTimeStep; // total number of time steps
	private int numSim; // number of simulation per game configuration
	
	// For graph generation
	private int graphID;
	private int numNode; // number of nodes 
	private int numEdge; // number of edges
	private int numTarget; // minimum number of targets
	
	private double discFact; // for computing payoff

//	private double aRewardLB;
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
	
	
	
	public GameSimulationSpec(final int numTimeStep, final int numSim, final int graphID, final int numNode, final int numEdge, final int numTarget
			, final double discFact) {
		this.numTimeStep = numTimeStep;
		this.numSim = numSim;
		this.graphID = graphID;
		this.numNode = numNode;
		this.numEdge = numEdge;
		this.numTarget = numTarget;
//		this.aRewardLB = aRewardLB;
//		this.aRewardUB = aRewardUB;
//		this.dPenaltyLB = dPenaltyLB;
//		this.dPenaltyUB = dPenaltyUB;
//		this.totalNumAlert = totalNumAlert;
//		this.minNumAlert = minNumAlert;
//		this.maxNumAlert = maxNumAlert;
//		this.fixPoissonParam = fixPoissonParam;
//		this.adaptPoissonRatio = adaptPoissonRatio;
//		this.qrParam = qrParam;
//		this.logisParam = logisParam;
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
//	public double getARewardLB()
//	{
//		return this.aRewardLB;
//	}
//	public double getARewardUB()
//	{
//		return this.aRewardUB;
//	}
//	public double getDPenaltyLB()
//	{
//		return this.dPenaltyLB;
//	}
//	public double getDPenaltyUB()
//	{
//		return this.dPenaltyUB;
//	}
//	public int getTotalNumAlert()
//	{
//		return this.totalNumAlert;
//	}
//	public int getMinNumAlert()
//	{
//		return this.minNumAlert;
//	}
//	public int getMaxNumAlert()
//	{
//		return this.maxNumAlert;
//	}
//	public double getFixPoissonParam()
//	{
//		return this.fixPoissonParam;
//	}
//	public double getAdaptPoissonRatio()
//	{
//		return this.adaptPoissonRatio;
//	}
//	public double getQRParam()
//	{
//		return this.qrParam;
//	}
//	public double getLogisParam()
//	{
//		return this.logisParam;
//	}
	public double getDiscFact() {
		return this.discFact;
	}
}
