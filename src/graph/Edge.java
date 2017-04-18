package graph;

import org.jgrapht.graph.DefaultWeightedEdge;

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
		this(EdgeType.NORMAL
				, 0.0, 0.0);
	}
	
	public Edge(final EdgeType aType
		, final double aCost, final double curActProb) {
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
		this.actProb = aActProb;
	}
	
	public Node getsource() {
		return (Node) this.getSource();
	}
	
	public Node gettarget() {
		return (Node) this.getTarget();
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
		System.out.println("--------------------------------------------------------------------");
		System.out.println("ID: " + this.getId() + "\t" + "Source: " + this.getsource().getId() + "\t" + "Des: " + this.gettarget().getId());
		System.out.println("Type: " + this.getType().toString());
		System.out.println("aCost: " + this.getACost());
		System.out.println("actProb: " + this.getActProb());
		System.out.println("--------------------------------------------------------------------");
	}
}
