package model;

import graph.Edge;
import graph.INode;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import game.GameSimulation;

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
				System.out.println("Root not type AND");
				return false;
			}
		}
		final Set<Integer> nodeIds = new HashSet<Integer>();
		final Set<Integer> topoPositions = new HashSet<Integer>();
		for (final Node node: vertexSet()) {
			if (nodeIds.contains(node.getId())) {
				System.out.println("Duplicate Node id");
				return false;
			}
			if (topoPositions.contains(node.getTopoPosition())) {
				System.out.println("Duplicate Node topo position");
				return false;
			}
			nodeIds.add(node.getId());
			topoPositions.add(node.getTopoPosition());
		}
		final Set<Integer> edgeIds = new HashSet<Integer>();
		for (final Edge edge: edgeSet()) {
			if (edgeIds.contains(edge.getId())) {
				System.out.println("Duplicate Edge id");
				return false;
			}
			edgeIds.add(edge.getId());
			if (edge.gettarget().equals(edge.getsource())) {
				System.out.println("Self-edge");
				return false;
			}
			if (edge.gettarget().getActivationType() == NodeActivationType.AND
				&& (edge.getActProb() != 0.0 || edge.getACost() != 0.0)) {
				// edges to AND nodes must have placeholder actProb and aCost of 0.0
				System.out.println("Edge to AND node with nonzero actProb or aCost");
				System.out.println(edge);
				System.out.println(edge.getActProb() + "\t" + edge.getACost());
				System.out.println(edge.gettarget());
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
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Target set: ");
		for (Node target : this.targetSet) {
			GameSimulation.printIfDebug(target.getId() + "\t");
		}
		GameSimulation.printIfDebug("");
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Root set: ");
		for (Node root : this.rootSet) {
			GameSimulation.printIfDebug(root.getId() + "\t");
		}
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Mincut set: ");
		for (Node node : this.minCut) {
			GameSimulation.printIfDebug(node.getId() + "\t");
		}
		GameSimulation.printIfDebug("");
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
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
					System.out.println("Root set omits Node with 0 in-degree");
					return false;
				}
			} else {
				if (this.rootSet.contains(node)) {
					System.out.println("Root set contains node with nonzero in-degree");
					return false;
				}
			}
		}
		return true;
	}
}
