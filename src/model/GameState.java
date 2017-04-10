package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

public class GameState{
	Set<Node> enabledNodeSet;
	public GameState()
	{
		this.enabledNodeSet = new HashSet<Node>();
	}
	public boolean addEnabledNode(Node node)
	{
		return this.enabledNodeSet.add(node);
	}
	public Set<Node> getEnabledNodeSet()
	{
		return enabledNodeSet;
	}
	public boolean contain(Node node)
	{
		return this.enabledNodeSet.contains(node);
	}
	public void clear()
	{
		this.enabledNodeSet.clear();
	}
	public void print() {
		// TODO Auto-generated method stub
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Active Nodes");
		for(Node node : this.enabledNodeSet)
			System.out.print(node.getId() + "\t");
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
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
		GameState other = (GameState) obj;
		for(Node node : other.getEnabledNodeSet())
			if(!this.enabledNodeSet.contains(node))
				return false;
		return true;
	}
}
