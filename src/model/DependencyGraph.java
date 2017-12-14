package model;

import graph.Edge;
import graph.INode;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.INode.NodeType;
import graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import game.GameSimulation;

// There is a dummy node connecting to entry nodes.
public final class DependencyGraph extends DirectedAcyclicGraph<Node, Edge> {
	private static final long serialVersionUID = 1L; 
	private final Set<Node> targetSet;
	private Set<Node> minCut;
	private Set<Node> rootSet;
	private final Map<Integer, Integer> distToGoal = 
		new HashMap<Integer, Integer>();
	private final Map<Integer, Double> subtreeAttReward = 
		new HashMap<Integer, Double>();
	private final Map<Integer, Double> subtreeDefPenalty = 
		new HashMap<Integer, Double>();
	
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
				// edges to AND nodes must have placeholder
				// actProb and aCost of 0.0
				System.out.println(
					"Edge to AND node with nonzero actProb or aCost");
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
	
	public Node getNodeById(final int id) {
		for (final Node node: vertexSet()) {
			if (node.getId() == id) {
				return node;
			}
		}
		return null;
	}
	
	public Edge getEdgeById(final int id) {
		for (final Edge edge: edgeSet()) {
			if (edge.getId() == id) {
				return edge;
			}
		}
		return null;
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
	
	public GameState getGameState() {
		final GameState result = new GameState();
		for (Node node: this.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				result.addEnabledNode(node);
			}
		}
		return result;
	}
	
	public void print() {
		for (Node node : this.vertexSet()) {
			node.print();
		}
		for (Edge edge: this.edgeSet()) {
			edge.print();
		}
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Target set: ");
		for (Node target : this.targetSet) {
			GameSimulation.printIfDebug(target.getId() + "\t");
		}
		GameSimulation.printIfDebug("");
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Root set: ");
		for (Node root : this.rootSet) {
			GameSimulation.printIfDebug(root.getId() + "\t");
		}
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Mincut set: ");
		for (Node node : this.minCut) {
			GameSimulation.printIfDebug(node.getId() + "\t");
		}
		GameSimulation.printIfDebug("");
		GameSimulation.printIfDebug(
		"--------------------------------------------------------------------");
	}
	
	public double subtreeDefensePenalty(final Node node) {
		if (this.subtreeDefPenalty.containsKey(node.getId())) {
			return this.subtreeDefPenalty.get(node.getId());
		}
		
		final List<Node> queue = new ArrayList<Node>();
		Node curNode = node;
		queue.add(curNode);
		double totalPenalty = 0.0;
		while (!queue.isEmpty()) {
			curNode = queue.remove(0);
			
			if (curNode.getType() == NodeType.TARGET) {
				totalPenalty += curNode.getDPenalty();
			}
			
			for (final Edge edge: outgoingEdgesOf(curNode)) {
				queue.add(edge.gettarget());
			}
		}
		
		this.subtreeDefPenalty.put(node.getId(), totalPenalty);
		return totalPenalty;
	}
	
	public double subtreeAttackReward(final Node node) {
		if (this.subtreeAttReward.containsKey(node.getId())) {
			return this.subtreeAttReward.get(node.getId());
		}
		
		final List<Node> queue = new ArrayList<Node>();
		Node curNode = node;
		queue.add(curNode);
		double totalReward = 0.0;
		while (!queue.isEmpty()) {
			curNode = queue.remove(0);
			
			if (curNode.getType() == NodeType.TARGET) {
				totalReward += curNode.getAReward();
			}
			
			for (final Edge edge: outgoingEdgesOf(curNode)) {
				queue.add(edge.gettarget());
			}
		}
		
		this.subtreeAttReward.put(node.getId(), totalReward);
		return totalReward;
	}
	
	public int distanceToGoal(final Node node) {
		if (this.distToGoal.containsKey(node.getId())) {
			return this.distToGoal.get(node.getId());
		}
		
		final List<Node> queue = new ArrayList<Node>();
		final List<Integer> depths = new ArrayList<Integer>();
		Node curNode = node;
		int curDepth = 0;
		queue.add(curNode);
		depths.add(curDepth);
		while (!queue.isEmpty()) {
			curNode = queue.remove(0);
			curDepth = depths.remove(0);
			
			if (curNode.getType() == NodeType.TARGET) {
				this.distToGoal.put(node.getId(), curDepth);
				return curDepth;
			}
			
			for (final Edge edge: outgoingEdgesOf(curNode)) {
				queue.add(edge.gettarget());
				depths.add(curDepth + 1);
			}
		}
		
		// no path to a goal node
		this.distToGoal.put(node.getId(), -1);
		return -1;
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
					System.out.println(
						"Root set contains node with nonzero in-degree");
					return false;
				}
			}
		}
		return true;
	}
}
