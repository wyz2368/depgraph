package model;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class AttackerAction {
	private Map<Node, Set<Edge>> action;
	
	public AttackerAction() {
		this.action = new HashMap<Node, Set<Edge>>();
	}
	
	public boolean containNode(final Node node) {
		return this.action.containsKey(node);
	}
	
	public AttackerAction(final Map<Node, Set<Edge>> curAction) {
		this.action = curAction;
	}
	
	public void setAction(final Map<Node, Set<Edge>> curAction) {
		this.action = curAction;
	}
	
	public Map<Node, Set<Edge>> getAction() {
		return this.action;
	}
	
	public Set<Edge> addNodetoActive(final Node node, final Set<Edge> edgeSet) {
		return this.action.put(node, edgeSet);
	}
	
	public void clear() {
		// for(Entry<Node, Set<Edge>> entry : action.entrySet())
		// {
		// entry.getValue().clear();
		// }
		this.action.clear();
	}
	
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Attacker Action...");
		for (Entry<Node, Set<Edge>> entry : this.action.entrySet()) {
			System.out.println("Activating node: " + entry.getKey().getId() 
					+ "\t Node type: " + entry.getKey().getType().toString()
					+ "\t Activation Type: " + entry.getKey().getActivationType().toString());
			if (entry.getKey().getActivationType() == NodeActivationType.OR) {
				System.out.println("Via edge: ");
				for (Edge edge : entry.getValue()) {
					System.out.println(edge.getsource().getId() + "(" + edge.getsource().getState().toString() 
							+ ")-->" + edge.gettarget().getId());
				}
				System.out.println();
			}
		}
		System.out.println("--------------------------------------------------------------------");
	}
}
