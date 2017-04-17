package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

public class DefenderAction {
	Set<Node> action;
	public DefenderAction()
	{
		this.action = new HashSet<Node>();
	}
	public DefenderAction(Set<Node> action)
	{
		this.action = action;
	}
	public void setAction(Set<Node> action)
	{
		this.action = action;
	}
	public Set<Node> getAction()
	{
		return this.action;
	}
	public boolean addNodetoProtect(Node node)
	{
		return this.action.add(node);
	}
	public boolean contain(Node node)
	{
		return this.action.contains(node);
	}
	public void clear()
	{
		this.action.clear();
	}
	public void print()
	{
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Defender Action...");
		for(Node node : this.action)
		{
			System.out.println("Protect node: " + node.getId() + "\t Node type" + node.getType() + "\t Activation Type: " + node.getActivationType().toString());
		}
		System.out.println("--------------------------------------------------------------------");
	}
}
