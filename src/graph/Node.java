package graph;

public class Node implements INode {
	private int id; // starting from 1
	private NODE_TYPE type = NODE_TYPE.NONTARGET;
	private NODE_ACTIVATION_TYPE eType = NODE_ACTIVATION_TYPE.OR;
	private NODE_STATE state = NODE_STATE.INACTIVE; // use during each game simulation
	private double aReward = 0.0; // meaningful for targets only
	private double dPenalty = 0.0; // meaningful for targets only
	
	private double dCost = 0.0; // cost of disabling this node
	private double aCost = 0.0; // meaningful for AND node only
	
	private double posActiveProb = 1.0;// prob of sending positive signal if node is active
	private double posInactiveProb = 0.0; // prob of sending positive signal if node is inactive
	
	private double actProb = 1.0; // prob of becoming active if being activated, for AND node only
	
	int topoPosition = -1; // position of node in the topological order of the graph

	private static int counter = 1;
	
	public Node() {
		this(NODE_TYPE.NONTARGET,NODE_ACTIVATION_TYPE.OR
				, 0.0, 0.0
				, 0.0, 0.0
				, 0.0, 0.0
				, 0.0);
	}

	public Node(NODE_TYPE type, NODE_ACTIVATION_TYPE eType
			, double aReward, double dPenalty
			, double dCost, double aCost
			, double posActiveProb, double posInactiveProb
			, double actProb) {
		super();
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
	public Node(int id, NODE_TYPE type, NODE_ACTIVATION_TYPE eType
			, double aReward, double dPenalty
			, double dCost, double aCost
			, double posActiveProb, double posInactiveProb
			, double actProb){
		super();
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
	public NODE_STATE getState()
	{
		return this.state;
	}
	@Override
	public NODE_TYPE getType() {
		return this.type;
	}
	@Override
	public NODE_ACTIVATION_TYPE getActivationType()
	{
		return this.eType;
	}
	@Override
	public double getAReward() {
		return this.aReward;
	}
	@Override
	public double getDPenalty(){
		return this.dPenalty;
	}
	@Override
	public double getDCost(){
		return this.dCost;
	}
	
	@Override
	public double getACost(){
		return this.aCost;
	}
	@Override
	public double getActProb(){
		return this.actProb;
	}
	@Override 
	public int getTopoPosition(){
		return this.topoPosition;
	}
	
	@Override
	public void setID(int id)
	{
		this.id = id;
	}
	@Override
	public void setState(NODE_STATE state){
		this.state = state;
	}
	@Override
	public void setType(NODE_TYPE type) {
		this.type = type;
	}
	@Override
	public void setActivationType(NODE_ACTIVATION_TYPE eType)
	{
		this.eType = eType;
	}
	
	@Override
	public void setAReward(double aReward) {
		this.aReward = aReward;
	}
	@Override
	public void setDPenalty(double dPenalty){
		this.dPenalty = dPenalty;
	}
	
	@Override
	public void setDCost(double dCost){
		this.dCost = dCost;
	}
	@Override
	public void setACost(double aCost){
		this.aCost = aCost;
	}
	@Override
	public void setActProb(double actProb){
		this.actProb = actProb;
	}
	@Override
	public void setTopoPosition(int position){
		this.topoPosition = position;
	}
	
	@Override
	public String toString() {
		return type.toString()+(new Integer(id)).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		return true;
	}

	
	public static void resetCounter(){
		counter = 1;
	}
	public void print()
	{
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
		// TODO Auto-generated method stub
		return this.posActiveProb;
	}

	@Override
	public void setPosActiveProb(double posActiveProb) {
		// TODO Auto-generated method stub
		this.posActiveProb = posActiveProb;
	}

	@Override
	public double getPosInactiveProb() {
		// TODO Auto-generated method stub
		return this.posInactiveProb;
	}

	@Override
	public void setPosInactiveProb(double posInactiveProb) {
		// TODO Auto-generated method stub
		this.posInactiveProb = posInactiveProb;
	}

}
