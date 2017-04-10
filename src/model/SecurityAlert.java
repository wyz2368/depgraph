package model;

import graph.Node;

public class SecurityAlert{
	Node node;
	boolean isActive;
	public SecurityAlert(Node node, boolean isActive)
	{
		this.node = node;
		this.isActive = isActive;
	}
	
	public boolean getAlert()
	{
		return this.isActive;
	}
	public Node getNode()
	{
		return this.node;
	}
}
