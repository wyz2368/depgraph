package model;

import graph.Edge;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

public final class AttackCandidate {
	private final Set<Node> nodeCandidateSet; // for AND node
	private final Set<Edge> edgeCandidateSet; // for OR edge
	
	public AttackCandidate() {
		this.nodeCandidateSet = new HashSet<Node>();
		this.edgeCandidateSet = new HashSet<Edge>();
	}
	public boolean isEmpty()
	{
		if(this.nodeCandidateSet.isEmpty() && this.edgeCandidateSet.isEmpty())
			return true;
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
