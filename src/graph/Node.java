package graph;

import game.GameSimulation;

public final class Node implements INode {
	private int id; // starting from 1
	private NodeType type = NodeType.NONTARGET;
	private NodeActivationType eType = NodeActivationType.OR;
	// use during each game simulation
	private NodeState state = NodeState.INACTIVE;
	private double aReward = 0.0; // meaningful for targets only
	private double dPenalty = 0.0; // meaningful for targets only
	
	private double dCost = 0.0; // cost of disabling this node
	private double aCost = 0.0; // meaningful for AND node only
	
	// prob of sending positive signal if node is active
	private double posActiveProb = 1.0;
	// prob of sending positive signal if node is inactive
	private double posInactiveProb = 0.0;
	
	// prob of becoming active if being activated, for AND node only
	private double actProb = 1.0;
	
	// position of node in the topological order of the graph
	private int topoPosition = -1;

	private static int counter = 1;
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
	
	public Node() {
		this(NodeType.NONTARGET, NodeActivationType.OR
			, 0.0, 0.0
			, 0.0, 0.0
			, 0.0, 0.0
			, 0.0);
	}
	
	public Node(final int aId, final NodeType aType,
		final NodeActivationType aEType
		, final double aAReward, final double aDPenalty
		, final double aDCost, final double aACost
		, final double aPosActiveProb, final double aPosInactiveProb
		, final double aActProb) {
		if (
			aType == null || aEType == null
			|| !isProb(aPosActiveProb) || !isProb(aPosInactiveProb)
			|| !isProb(aActProb)
		) {
			throw new IllegalArgumentException();
		}
		if (aAReward < 0.0 || aDPenalty > 0.0 || aDCost > 0.0 || aACost > 0.0) {
			throw new IllegalArgumentException();
		}
		if (aEType == NodeActivationType.OR
			&& (aACost != 0.0 || aActProb != 0.0)) {
			// OR nodes must have placeholder aCost and actProb of 0.0
			throw new IllegalArgumentException();
		}
		this.id = aId;
		this.type = aType; 
		this.eType = aEType;
		this.aReward = aAReward;
		this.dPenalty = aDPenalty;
		this.dCost = aDCost;
		this.aCost = aACost;
		this.posActiveProb = aPosActiveProb;
		this.posInactiveProb = aPosInactiveProb;
		this.actProb = aActProb;
	}
	
	private Node(final NodeType aType, final NodeActivationType aEType
		, final double aAReward, final double aDPenalty
		, final double aDCost, final double aACost
		, final double aPosActiveProb, final double aPosInactiveProb
		, final double aActProb) {
		if (
			aType == null || aEType == null
			|| !isProb(aPosActiveProb) || !isProb(aPosInactiveProb)
			|| !isProb(aActProb)
		) {
			throw new IllegalArgumentException();
		}
		this.id = counter;
		counter++;
		this.type = aType; 
		this.eType = aEType;
		this.aReward = aAReward;
		this.dPenalty = aDPenalty;
		this.dCost = aDCost;
		this.aCost = aACost;
		this.posActiveProb = aPosActiveProb;
		this.posInactiveProb = aPosInactiveProb;
		this.actProb = aActProb;
	}
	
	public boolean isOrType() {
		return this.eType == NodeActivationType.OR;
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
		if (aState == null) {
			throw new IllegalArgumentException();
		}
		this.state = aState;
	}
	
	@Override
	public void setType(final NodeType aType) {
		if (aType == null) {
			throw new IllegalArgumentException();
		}
		this.type = aType;
	}
	
	@Override
	public void setActivationType(final NodeActivationType eTypeCur) {
		if (eTypeCur == null) {
			throw new IllegalArgumentException();
		}
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
		if (!isProb(actProbCur)) {
			throw new IllegalArgumentException();
		}
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
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
		GameSimulation.printIfDebug("ID: " + this.getId());
		GameSimulation.printIfDebug("State: " + this.getState().toString());
		GameSimulation.printIfDebug("Type: " + this.getType().toString());
		GameSimulation.printIfDebug("Exploit Type: "
			+ this.getActivationType().toString());
		GameSimulation.printIfDebug("aReward: "
			+ this.getAReward() + "\t" + "dPenalty: " + this.getDPenalty());
		GameSimulation.printIfDebug("aCost: "
			+ this.getACost() + "\t" + "dCost: " + this.getDCost());
		GameSimulation.printIfDebug(
			"Probability of positive signal given active: "
				+ this.posActiveProb);
		GameSimulation.printIfDebug(
			"Probability of positive signal given inactive: "
				+ this.posInactiveProb);
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
	}

	@Override
	public double getPosActiveProb() {
		return this.posActiveProb;
	}

	@Override
	public void setPosActiveProb(final double aPosActiveProb) {
		if (!isProb(aPosActiveProb)) {
			throw new IllegalArgumentException();
		}
		this.posActiveProb = aPosActiveProb;
	}

	@Override
	public double getPosInactiveProb() {
		return this.posInactiveProb;
	}

	@Override
	public void setPosInactiveProb(final double aPosInactiveProb) {
		if (!isProb(aPosInactiveProb)) {
			throw new IllegalArgumentException();
		}
		this.posInactiveProb = aPosInactiveProb;
	}
}
