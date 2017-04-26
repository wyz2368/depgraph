package model;

import graph.Edge;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

public final class AttackCandidate {
	// for AND nodes to attack. does not include OR nodes to attack.
	private final Set<Node> nodeCandidateSet;
	// for edges to OR nodes to attack. does not include edges to AND nodes to attack.
	private final Set<Edge> edgeCandidateSet;
	
	public AttackCandidate() {
		this.nodeCandidateSet = new HashSet<Node>();
		this.edgeCandidateSet = new HashSet<Edge>();
	}
	
	public boolean isEmpty() {
		if (this.nodeCandidateSet.isEmpty() && this.edgeCandidateSet.isEmpty()) {
			return true;
		}
		return false;
	}
	
	public boolean addNodeCandidate(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return this.nodeCandidateSet.add(node);
	}
	
	public boolean addEdgeCandidate(final Edge edge) {
		if (edge == null) {
			throw new IllegalArgumentException();
		}
		return this.edgeCandidateSet.add(edge);
	}
	
	public Set<Node> getNodeCandidateSet() {
		return this.nodeCandidateSet;
	}
	
	public Set<Edge> getEdgeCandidateSet() {
		return this.edgeCandidateSet;
	}
}
