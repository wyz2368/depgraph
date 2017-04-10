package model;

import graph.Edge;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

public class AttackCandidate {
	Set<Node> nodeCandidateSet; // for AND node
	Set<Edge> edgeCandidateSet; // for OR edge
	public AttackCandidate(){
		this.nodeCandidateSet = new HashSet<Node>();
		this.edgeCandidateSet = new HashSet<Edge>();
	}
	public boolean addNodeCandidate(Node node){
		return this.nodeCandidateSet.add(node);
	}
	public boolean addEdgeCandidate(Edge edge){
		return this.edgeCandidateSet.add(edge);
	}
	public Set<Node> getNodeCandidateSet(){
		return this.nodeCandidateSet;
	}
	public Set<Edge> getEdgeCandidateSet(){
		return this.edgeCandidateSet;
	}
	public void clear(){
		this.nodeCandidateSet.clear();
		this.edgeCandidateSet.clear();
	}
}
