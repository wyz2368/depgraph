package model;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import game.GameSimulation;

import java.util.Set;

public final class AttackerAction {
	private Map<Node, Set<Edge>> action;
	
	public AttackerAction() {
		this.action = new HashMap<Node, Set<Edge>>();
	}
	
	public Map<Node, Set<Edge>> getActionCopy() {
		final Map<Node, Set<Edge>> result = new HashMap<Node, Set<Edge>>();
		for (Entry<Node, Set<Edge>> entry: this.action.entrySet()) {
			final Set<Edge> setCopy = new HashSet<Edge>();
			setCopy.addAll(entry.getValue());
			result.put(entry.getKey(), setCopy);
		}
		return result;
	}
	
	public boolean isEmpty() {
		return this.action.isEmpty();
	}
	
	public void addAndNodeAttack(final Node targetAndNode, final Set<Edge> inEdges) {
		if (targetAndNode == null || inEdges == null) {
			throw new IllegalArgumentException();
		}
		if (targetAndNode.getActivationType() != NodeActivationType.AND) {
			throw new IllegalArgumentException();
		}
		for (final Edge edge: inEdges) {
			if (!edge.gettarget().equals(targetAndNode)) {
				throw new IllegalArgumentException();
			}
		}
		if (!this.action.containsKey(targetAndNode)) {
			this.action.put(targetAndNode, inEdges);
		}
	}
	
	public void addOrNodeAttack(final Node targetOrNode, final Edge edge) {
		if (targetOrNode == null || edge == null) {
			throw new IllegalArgumentException();
		}
		if (targetOrNode.getActivationType() != NodeActivationType.OR) {
			throw new IllegalArgumentException();
		}
		if (!edge.gettarget().equals(targetOrNode)) {
			throw new IllegalArgumentException();
		}
		if (this.action.containsKey(targetOrNode)) {
			this.action.get(targetOrNode).add(edge);
		} else {
			final Set<Edge> newSet = new HashSet<Edge>();
			newSet.add(edge);
			this.action.put(targetOrNode, newSet);
		}
	}
	
	public void clear() {
		this.action.clear();
	}
	
	public void print() {
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Attacker Action...");
		for (Entry<Node, Set<Edge>> entry : this.action.entrySet()) {
			GameSimulation.printIfDebug("Activating node: " + entry.getKey().getId() 
				+ "\t Node type: " + entry.getKey().getType().toString()
				+ "\t Activation Type: " + entry.getKey().getActivationType().toString());
			if (entry.getKey().getActivationType() == NodeActivationType.OR) {
				GameSimulation.printIfDebug("Via edge: ");
				for (Edge edge : entry.getValue()) {
					GameSimulation.printIfDebug(edge.getsource().getId() + "(" + edge.getsource().getState().toString() 
						+ ")-->" + edge.gettarget().getId());
				}
				GameSimulation.printIfDebug("");
			}
		}
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
	}
	
	public String getActionString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (Node node : this.action.keySet()) {
			builder.append(node.getId()).append(",").append(node.getType()).append("\t");
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public String toString() {
		return "AttackerAction [action=" + getActionString() + "]";
	}
}
