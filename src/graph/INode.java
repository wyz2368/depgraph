package graph;

public interface INode{	
	enum NodeType { 
		NONTARGET, TARGET;
		
		@Override
		public String toString() {
			switch(this) {
			case TARGET: return "TARGET";
			case NONTARGET: return "NONTARGET";
			default: return "";
			}	
		}
	}
	enum NodeActivationType {
		AND, OR;
	
		@Override
		public String toString() {
			switch(this) {
			case AND: return "AND";
			case OR: return "OR";
			default: return "";
			}	
		}
	}
	
	public enum NodeState {
		ACTIVE, INACTIVE;
	
		@Override
		public String toString() {
			switch(this) {
			case ACTIVE: return "ACTIVE";
			case INACTIVE: return "INACTIVE"; 
			default: return "";
			}	
		}
	}
	int getId();
	void setID(int id);
	
	NodeState getState();
	void setState(NodeState s);
	
	NodeType getType();
	void setType(NodeType t);
	
	NodeActivationType getActivationType();
	void setActivationType(NodeActivationType eT);
	
	double getAReward();
	void setAReward(double aReward);
	
	double getDPenalty();
	void setDPenalty(double dPenalty);
	
	double getDCost();
	void setDCost(double dCost);
	
	double getACost();
	void setACost(double aCost);
	
	double getActProb();
	void setActProb(double actProb);
	
	int getTopoPosition();
	void setTopoPosition(int position);
	
	double getPosActiveProb();
	void setPosActiveProb(double posActiveProb);
	
	double getPosInactiveProb();
	void setPosInactiveProb(double posInactiveProbe);	
}
