package game;

public class GameSimulationSpec {
	int numTimeStep; // total number of time steps
	int numSim; // number of simulation per game configuration
	
	// For graph generation
	int graphID;
	int numNode; // number of nodes 
	int numEdge; // number of edges
	int numTarget; // minimum number of targets
	double aRewardLB;
	double aRewardUB;
	double dPenaltyLB;
	double dPenaltyUB;
	
	int totalNumAlert; // total number of security alerts
	int minNumAlert; // minimum number of alerts for each node
	int maxNumAlert; // maximum number of alerts for each node
	
	// For strategy generation
	// Note: should we use uniform distribution to generate number of nodes to enable or disable each time step???
//	double fixPoissonParam; // used if fixed mean is used
//	double adaptPoissonRatio; // used if mean is chosen according to the number of available nodes
//	double qrParam; // for attacker 
//	double logisParam; // for defender
	double discFact; // for computing payoff
	
	
	
	public GameSimulationSpec(int numTimeStep, int numSim, int graphID, int numNode, int numEdge, int numTarget
			, double aRewardLB, double aRewardUB, double dPenaltyLB, double dPenaltyUB
			, int totalNumAlert, int minNumAlert, int maxNumAlert
			, double fixPoissonParam, double adaptPoissonRatio, double qrParam, double logisParam, double discFact)
	{
		this.numTimeStep = numTimeStep;
		this.numSim = numSim;
		this.graphID = graphID;
		this.numNode = numNode;
		this.numEdge = numEdge;
		this.numTarget = numTarget;
		this.aRewardLB = aRewardLB;
		this.aRewardUB = aRewardUB;
		this.dPenaltyLB = dPenaltyLB;
		this.dPenaltyUB = dPenaltyUB;
		this.totalNumAlert = totalNumAlert;
		this.minNumAlert = minNumAlert;
		this.maxNumAlert = maxNumAlert;
//		this.fixPoissonParam = fixPoissonParam;
//		this.adaptPoissonRatio = adaptPoissonRatio;
//		this.qrParam = qrParam;
//		this.logisParam = logisParam;
		this.discFact = discFact;
	}
	public int getNumTimeStep()
	{
		return this.numTimeStep;
	}
	public int getNumSim()
	{
		return this.numSim;
	}
	public int getGraphID()
	{
		return this.graphID;
	}
	public int getNumNode()
	{
		return this.numNode;
	}
	public int getNumEdge()
	{
		return this.numEdge;
	}
	public int getNumTarget()
	{
		return this.numTarget;
	}
	public double getARewardLB()
	{
		return this.aRewardLB;
	}
	public double getARewardUB()
	{
		return this.aRewardUB;
	}
	public double getDPenaltyLB()
	{
		return this.dPenaltyLB;
	}
	public double getDPenaltyUB()
	{
		return this.dPenaltyUB;
	}
	public int getTotalNumAlert()
	{
		return this.totalNumAlert;
	}
	public int getMinNumAlert()
	{
		return this.minNumAlert;
	}
	public int getMaxNumAlert()
	{
		return this.maxNumAlert;
	}
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
	public double getDiscFact()
	{
		return this.discFact;
	}
}
