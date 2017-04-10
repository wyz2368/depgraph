package graph;

public interface INode{	
	public enum NODE_TYPE {NONTARGET, TARGET;
		
	public String toString(){
		switch(this){
		case TARGET: return "TARGET";
		case NONTARGET: return "NONTARGET";
		default: return "";
		}	
	}
	};
	public enum NODE_ACTIVATION_TYPE {AND, OR;
	
	public String toString(){
		switch(this){
		case AND: return "AND";
		case OR: return "OR";
		default: return "";
		}	
	}
	};
	
	public enum NODE_STATE {ACTIVE, INACTIVE;
	
	public String toString(){
		switch(this){
		case ACTIVE: return "ACTIVE";
		case INACTIVE: return "INACTIVE"; 
		default: return "";
		}	
	}
	};
	public int getId() ;
	public void setID(int id);
	
	public NODE_STATE getState();
	public void setState(NODE_STATE s);
	
	public NODE_TYPE getType();
	public void setType(NODE_TYPE t);
	
	public NODE_ACTIVATION_TYPE getActivationType();
	public void setActivationType(NODE_ACTIVATION_TYPE eT);
	
	public double getAReward();
	public void setAReward(double aReward);
	
	public double getDPenalty();
	public void setDPenalty(double dPenalty);
	
	public double getDCost();
	public void setDCost(double dCost);
	
	public double getACost();
	public void setACost(double aCost);
	
	public double getActProb();
	public void setActProb(double actProb);
	
	public int getTopoPosition();
	public void setTopoPosition(int position);
	
	public double getPosActiveProb();
	public void setPosActiveProb(double posActiveProb);
	
	public double getPosInactiveProb();
	public void setPosInactiveProb(double posInactiveProbe);
	
	public int hashCode();
	public boolean equals(Object obj);
	
	public String toString();
}
