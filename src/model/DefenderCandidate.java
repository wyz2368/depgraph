package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

import game.GameSimulation;

public final class DefenderCandidate {
	private final Set<Node> nodeCandidateSet; // for AND node
	
	public DefenderCandidate() {
		this.nodeCandidateSet = new HashSet<Node>();
	}
	
	public boolean addNodeCandidate(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return this.nodeCandidateSet.add(node);
	}
	
	public Set<Node> getNodeCandidateSet() {
		return this.nodeCandidateSet;
	}

	public void print() {
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Defender Candidate...");
		for (Node node : this.nodeCandidateSet) {
			GameSimulation.printIfDebug("Candidate node: " + node.getId()
				+ "\t Node type: " + node.getType()
				+ "\t Activation Type: " + node.getActivationType().toString());
		}
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
	}
}
