package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

import game.GameSimulation;

public final class DefenderAction {
	private final Set<Node> action;
	
	public DefenderAction() {
		this.action = new HashSet<Node>();
	}
	
	public Set<Node> getAction() {
		return this.action;
	}
	
	public void addNodetoProtect(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		this.action.add(node);
	}
	
	public boolean containsNode(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return this.action.contains(node);
	}
	
	public void clear() {
		this.action.clear();
	}
	
	public void print() {
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Defender Action...");
		for (Node node : this.action) {
			GameSimulation.printIfDebug("Protect node: " + node.getId() + "\t Node type: " + node.getType()
				+ "\t Activation Type: " + node.getActivationType().toString());
		}
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("DefenderAction [action=\n\t");
		for (final Node node: this.action) {
			builder.append(node.getId()).append("\t");
		}
		builder.append("\n]");
		return builder.toString();
	}
}
