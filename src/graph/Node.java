package graph;

public final class Node implements INode {
	private int id; // starting from 1
	private NodeType type = NodeType.NONTARGET;
	private NodeActivationType eType = NodeActivationType.OR;
	private NodeState state = NodeState.INACTIVE; // use during each game simulation
	private double aReward = 0.0; // meaningful for targets only
	private double dPenalty = 0.0; // meaningful for targets only
	
	private double dCost = 0.0; // cost of disabling this node
	private double aCost = 0.0; // meaningful for AND node only
	
	private double posActiveProb = 1.0; // prob of sending positive signal if node is active
	private double posInactiveProb = 0.0; // prob of sending positive signal if node is inactive
	
	private double actProb = 1.0; // prob of becoming active if being activated, for AND node only
	
	private int topoPosition = -1; // position of node in the topological order of the graph

	private static int counter = 1;
	
	public Node() {
		this(NodeType.NONTARGET, NodeActivationType.OR
				, 0.0, 0.0
				, 0.0, 0.0
				, 0.0, 0.0
				, 0.0);
	}

	public Node(final NodeType type, final NodeActivationType eType
			, final double aReward, final double dPenalty
			, final double dCost, final double aCost
			, final double posActiveProb, final double posInactiveProb
			, final double actProb) {
		this.id = counter;
		counter++;
		this.type = type; 
		this.eType = eType;
		this.aReward = aReward;
		this.dPenalty = dPenalty;
		this.dCost = dCost;
		this.aCost = aCost;
		this.posActiveProb = posActiveProb;
		this.posInactiveProb = posInactiveProb;
		this.actProb = actProb;
	}
	public Node(final int id, final NodeType type, final NodeActivationType eType
			, final double aReward, final double dPenalty
			, final double dCost, final double aCost
			, final double posActiveProb, final double posInactiveProb
			, final double actProb) {
		this.id = id;
		this.type = type; 
		this.eType = eType;
		this.aReward = aReward;
		this.dPenalty = dPenalty;
		this.dCost = dCost;
		this.aCost = aCost;
		this.posActiveProb = posActiveProb;
		this.posInactiveProb = posInactiveProb;
		this.actProb = actProb;
	}
	
	@Override
	public int getId() {
		return this.id;
	}
	@Override
	public NodeState getState() {
		return this.state;
	}
	@Override
	public NodeType getType() {
		return this.type;
	}
	@Override
	public NodeActivationType getActivationType() {
		return this.eType;
	}
	@Override
	public double getAReward() {
		return this.aReward;
	}
	@Override
	public double getDPenalty() {
		return this.dPenalty;
	}
	@Override
	public double getDCost() {
		return this.dCost;
	}
	
	@Override
	public double getACost() {
		return this.aCost;
	}
	@Override
	public double getActProb() {
		return this.actProb;
	}
	@Override 
	public int getTopoPosition() {
		return this.topoPosition;
	}
	
	@Override
	public void setID(final int aId) {
		this.id = aId;
	}
	@Override
	public void setState(final NodeState aState) {
		this.state = aState;
	}
	@Override
	public void setType(final NodeType aType) {
		this.type = aType;
	}
	@Override
	public void setActivationType(final NodeActivationType eTypeCur) {
		this.eType = eTypeCur;
	}
	
	@Override
	public void setAReward(final double aRewardCur) {
		this.aReward = aRewardCur;
	}
	@Override
	public void setDPenalty(final double dPenaltyCur) {
		this.dPenalty = dPenaltyCur;
	}
	
	@Override
	public void setDCost(final double dCostCur) {
		this.dCost = dCostCur;
	}
	@Override
	public void setACost(final double aCostCur) {
		this.aCost = aCostCur;
	}
	@Override
	public void setActProb(final double actProbCur) {
		this.actProb = actProbCur;
	}
	@Override
	public void setTopoPosition(final int positionCur) {
		this.topoPosition = positionCur;
	}
	
	@Override
	public String toString() {
		return this.type.toString() + (new Integer(this.id)).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Node other = (Node) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	public static void resetCounter() {
		counter = 1;
	}
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("ID: " + this.getId());
		System.out.println("State: " + this.getState().toString());
		System.out.println("Type: " + this.getType().toString());
		System.out.println("Exploit Type: " + this.getActivationType().toString());
		System.out.println("aReward: " + this.getAReward() + "\t" + "dPenalty: " + this.getDPenalty());
		System.out.println("aCost: " + this.getACost() + "\t" + "dCost: " + this.getDCost());
		System.out.println("Probability of positive signal given active: " + this.posActiveProb);
		System.out.println("Probability of positive signal given inactive: " + this.posInactiveProb);
		System.out.println("--------------------------------------------------------------------");
	}

	@Override
	public double getPosActiveProb() {
		return this.posActiveProb;
	}

	@Override
	public void setPosActiveProb(final double aPosActiveProb) {
		this.posActiveProb = aPosActiveProb;
	}

	@Override
	public double getPosInactiveProb() {
		return this.posInactiveProb;
	}

	@Override
	public void setPosInactiveProb(final double aPosInactiveProb) {
		this.posInactiveProb = aPosInactiveProb;
	}
}
