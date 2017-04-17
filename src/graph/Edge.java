package graph;



import org.jgrapht.graph.DefaultWeightedEdge;


public class Edge extends DefaultWeightedEdge{
	public enum EDGE_TYPE {NORMAL, VIRTUAL;
	@Override
	public String toString(){
		switch(this){
		case NORMAL: return "NORMAL";
		case VIRTUAL: return "VIRTUAL";
		default: return "";
		}	
	}}
	private static final long serialVersionUID = 1L;

	private int id = -1;
	private EDGE_TYPE type = EDGE_TYPE.NORMAL;
	
	private double aCost = 0.0; // for OR node only
	private double actProb = 1.0; // probability of successfully activating, for OR node only 
	
	private static int counter = 1;
	

	public Edge() {
		this(EDGE_TYPE.NORMAL
				, 0.0, 0.0);
	}
	public Edge(EDGE_TYPE type
			, double aCost, double actProb){
		super();
		this.id = counter;
		counter++;
		
		this.type = type;
		
		this.aCost = aCost;
		this.actProb = actProb;
	}
	
	public int getId() {
		return this.id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public EDGE_TYPE getType() {
		return this.type;
	}
	public void setType(EDGE_TYPE type) {
		this.type = type;
	}
	
	public double getACost()
	{
		return this.aCost;
	}	
	public void setACost(double aCost)
	{
		this.aCost = aCost;
	}
	
	public double getActProb()
	{
		return this.actProb;
	}
	public void setActProb(double actProb)
	{
		this.actProb = actProb;
	}
	public Node getsource()
	{
		return (Node)this.getSource();
	}
	public Node gettarget()
	{
		return (Node)this.getTarget();
	}

	@Override
	public String toString() {
		if ( this.type == EDGE_TYPE.NORMAL ) {
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (this.id != other.id)
			return false;
		return true;
	}
	
	public static void resetCounter() {
		counter = 1;
	}

	public void print()
	{
		System.out.println("--------------------------------------------------------------------");
		System.out.println("ID: " + this.getId() + "\t" + "Source: " + this.getsource().getId() + "\t" + "Des: " + this.gettarget().getId());
		System.out.println("Type: " + this.getType().toString());
		System.out.println("aCost: " + this.getACost());
		System.out.println("actProb: " + this.getActProb());
		System.out.println("--------------------------------------------------------------------");
	}
}

