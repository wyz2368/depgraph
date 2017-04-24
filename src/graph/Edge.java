package graph;

import org.jgrapht.graph.DefaultWeightedEdge;

import game.GameSimulation;

public final class Edge extends DefaultWeightedEdge {
	
	public enum EdgeType { 
		NORMAL, VIRTUAL;
		
		@Override
		public String toString() {
			switch(this) {
			case NORMAL: return "NORMAL";
			case VIRTUAL: return "VIRTUAL";
			default: return "";
			}	
		}
	}
	
	private static final long serialVersionUID = 1L;

	private int id = -1;
	private EdgeType type = EdgeType.NORMAL;
	
	private double cost = 0.0; // for OR node only
	private double actProb = 1.0; // probability of successfully activating, for OR node only 
	
	private static int counter = 1;

	public Edge() {
		this(EdgeType.NORMAL, 0.0, 0.0);
	}
	
	private Edge(
		final EdgeType aType, 
		final double aCost,
		final double curActProb
	) {
		super();
		if (aType == null || !isProb(curActProb) || aCost > 0.0) {
			throw new IllegalArgumentException();
		}
		this.id = counter;
		counter++;
		
		this.type = aType;
		
		this.cost = aCost;
		this.actProb = curActProb;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(final int aId) {
		this.id = aId;
	}
	
	public EdgeType getType() {
		return this.type;
	}
	
	public void setType(final EdgeType aType) {
		if (aType == null) {
			throw new IllegalArgumentException();
		}
		this.type = aType;
	}
	
	public double getACost() {
		return this.cost;
	}	
	
	public void setACost(final double aCost) {
		this.cost = aCost;
	}	
	
	public double getActProb() {
		return this.actProb;
	}
	
	public void setActProb(final double aActProb) {
		if (!isProb(aActProb)) {
			throw new IllegalArgumentException();
		}
		this.actProb = aActProb;
	}
	
	public Node getsource() {
		return (Node) this.getSource();
	}
	
	public Node gettarget() {
		return (Node) this.getTarget();
	}
	
	public double getweight() {
		return this.getWeight();
	}

	@Override
	public String toString() {
		if (this.type == EdgeType.NORMAL) {
			return "" + this.id;
		}
		return "" + this.id + "(V)";
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
		Edge other = (Edge) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}
	
	public static void resetCounter() {
		counter = 1;
	}
	
	public void print() {
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("ID: " + this.getId() + "\t" + "Source: "
			+ this.getsource().getId() + "\t" + "Des: " + this.gettarget().getId());
		GameSimulation.printIfDebug("Type: " + this.getType().toString());
		GameSimulation.printIfDebug("aCost: " + this.getACost());
		GameSimulation.printIfDebug("actProb: " + this.getActProb());
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
