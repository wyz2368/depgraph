package model;

import graph.Edge;
import graph.INode;
import graph.INode.NodeState;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

// There is a dummy node connecting to entry nodes.
public final class DependencyGraph extends DirectedAcyclicGraph<Node, Edge> {
	private static final long serialVersionUID = 1L; // I dont know what this is for :)))
	private final Set<Node> targetSet;
	private Set<Node> minCut;
	private Set<Node> rootSet;
	
	public DependencyGraph() {
		super(Edge.class);
		this.targetSet = new HashSet<Node>();
		this.minCut = new HashSet<Node>();
		this.rootSet = new HashSet<Node>();
	}
	
	public boolean isValid() {
		if (this.vertexSet().isEmpty()) {
			return false;
		}
		for (final Node root: getRootSet()) {
			if (root.getActivationType() != INode.NodeActivationType.AND) {
				return false;
			}
		}
		return true;
	}
	
	public boolean addTarget(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return this.targetSet.add(node);
	}
	
	public Set<Node> getTargetSet() {
		return this.targetSet;
	}
	
	public Set<Node> getMinCut() {
		return this.minCut;
	}
	
	public void addMinCut(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		this.minCut.add(node);
	}
	
	public Set<Node> getRootSet() {
		assert validRootSet();
		return this.rootSet;
	}
	
	public boolean addRoot(final Node node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return this.rootSet.add(node);
	}
	
	public void setState(final GameState gameState) {
		if (gameState == null) {
			throw new IllegalArgumentException();
		}
		this.resetState();
		for (Node node : gameState.getEnabledNodeSet()) {
			node.setState(NodeState.ACTIVE);
		}
	}
	
	public void print() {
		for (Node node : this.vertexSet()) {
			node.print();
		}
		for (Edge edge: this.edgeSet()) {
			edge.print();
		}
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Target set: ");
		for (Node target : this.targetSet) {
			System.out.print(target.getId() + "\t");
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Root set: ");
		for (Node root : this.rootSet) {
			System.out.print(root.getId() + "\t");
		}
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Mincut set: ");
		for (Node node : this.minCut) {
			System.out.print(node.getId() + "\t");
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
	
	private void resetState() {
		for (Node node : this.vertexSet()) {
			node.setState(NodeState.INACTIVE);
		}
	}
	
	private boolean validRootSet() {
		for (Node node : vertexSet()) {
			if (inDegreeOf(node) == 0) {
				if (!this.rootSet.contains(node)) {
					return false;
				}
			} else {
				if (this.rootSet.contains(node)) {
					return false;
				}
			}
		}
		return true;
	}
}
