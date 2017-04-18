package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

public final class DefenderAction {
	private final Set<Node> action;
	
	public DefenderAction() {
		this.action = new HashSet<Node>();
	}
	
	public DefenderAction(final Set<Node> curAction) {
		this.action = curAction;
	}
	
	public void setAction(final Set<Node> curAction) {
		this.action.clear();
		this.action.addAll(curAction);
	}
	
	public Set<Node> getAction() {
		return this.action;
	}
	
	public boolean addNodetoProtect(final Node node) {
		return this.action.add(node);
	}
	
	public boolean contain(final Node node) {
		return this.action.contains(node);
	}
	
	public void clear() {
		this.action.clear();
	}
	
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Defender Action...");
		for (Node node : this.action) {
			System.out.println("Protect node: " + node.getId() + "\t Node type" + node.getType()
				+ "\t Activation Type: " + node.getActivationType().toString());
		}
		System.out.println("--------------------------------------------------------------------");
	}
}
