package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.INode.NodeType;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

public final class DGraphGenerator {
	
	private DGraphGenerator() {
		// private constructor
	}
	
	// Number simulations per observation such that: 1-2 mins
	// All leaf nodes are targets, all costs, reward, penalty are within [0,1]
	public static void genGraph(final DependencyGraph dag, final RandomDataGenerator rand
		, final int numTarget, final double nodeActTypeRatio
		, final double aRewardLB, final double aRewardUB
		, final double dPenaltyLB, final double dPenaltyUB
		, final double aNodeCostLB, final double aNodeCostUB
		, final double aEdgeCostLB, final double aEdgeCostUB
		, final double dCostLB, final double dCostUB
		, final double aNodeActProbLB, final double aNodeActProbUB
		, final double aEdgeActProbLB, final double aEdgeActProbUB
		, final double minPosActiveProb, final double maxPosActiveProb
		, final double minPosInactiveProb, final double maxPosInactiveProb) {
		if (dag == null || rand == null || numTarget < 0 || !isProb(nodeActTypeRatio)
			|| aRewardLB > aRewardUB || dPenaltyLB > dPenaltyUB || aNodeCostLB > aNodeActProbUB
			|| aEdgeActProbLB > aEdgeActProbUB || dCostLB > dCostUB
			|| !isProb(aNodeActProbLB) || !isProb(aNodeActProbUB) || aNodeActProbLB > aNodeActProbUB
			|| !isProb(aEdgeActProbLB) || !isProb(aEdgeActProbUB) || aEdgeActProbLB > aEdgeActProbUB 
			|| !isProb(minPosActiveProb) || !isProb(maxPosActiveProb) || minPosActiveProb > maxPosActiveProb
			|| !isProb(minPosInactiveProb) || !isProb(maxPosInactiveProb) || minPosInactiveProb > maxPosInactiveProb
		) {
			throw new IllegalArgumentException();
		}
		DependencyGraph depGraph = dag;
		setTopologicalOrder(depGraph);
		selectTargetRandom(depGraph, rand, numTarget);
		setRootSet(depGraph);
		for (Node node : depGraph.vertexSet()) {
			setNodeTypeRandom(depGraph, node, rand, nodeActTypeRatio);
			genNodePayoffRandom(
				node, rand, aRewardLB, aRewardUB, dPenaltyLB, dPenaltyUB, aNodeCostLB, aNodeCostUB, dCostLB, dCostUB);
			genActivationProbRandom(node, rand, aNodeActProbLB, aNodeActProbUB);
			if (node.getType() != NodeType.TARGET) {
				node.setAReward(0.0);
				node.setDPenalty(0.0);
			}
			
			if (node.getActivationType() != NodeActivationType.AND) {
				node.setActProb(0.0);
				node.setACost(0.0);
			}
			genAlertProbRandom(node, rand, minPosActiveProb, maxPosActiveProb, minPosInactiveProb, maxPosInactiveProb);
		}
		for (Edge edge : depGraph.edgeSet()) {
			genEdgePayoffRandom(edge, rand, aEdgeCostLB, aEdgeCostUB);
			if (edge.gettarget().getActivationType() == NodeActivationType.OR) {
				genActivationProbRandom(edge, rand, aEdgeActProbLB, aEdgeActProbUB);
			}
		}
	}
	
	public static GameState randomizeInitialGraphState(
		final DependencyGraph depGraph,
		final RandomDataGenerator rand,
		final double pivot
	) {
		if (depGraph == null || rand == null || !isProb(pivot)) {
			throw new IllegalArgumentException();
		}
		GameState gameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			double value = rand.nextUniform(0, 1, true);
			if (value <= pivot) {
				node.setState(NodeState.ACTIVE);
				gameState.addEnabledNode(node);
			} else {
				node.setState(NodeState.INACTIVE);
			}
		}
		return gameState;
	}
	
	public static void findMinCut(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		SimpleDirectedWeightedGraph<Node, Edge> cloneGraph = new SimpleDirectedWeightedGraph<Node, Edge>(Edge.class);
        for(Node node : depGraph.vertexSet())
        	cloneGraph.addVertex(node);
        for(Edge edge : depGraph.edgeSet())
        {
        	Edge newEdge = new Edge();
        	cloneGraph.addEdge(edge.getsource(), edge.gettarget(), newEdge);
        	cloneGraph.setEdgeWeight(newEdge, 1.0);
        	
        }
        Node source = new Node();
        Node sink = new Node();
        cloneGraph.addVertex(source);
        cloneGraph.addVertex(sink);
        
        for(Node node : cloneGraph.vertexSet())
        {
        	if(node.getTopoPosition() != -1 && cloneGraph.inDegreeOf(node) == 0)
        	{
        		Edge newEdge = new Edge();
    			cloneGraph.addEdge(source, node, newEdge);
//	    			cloneGraph.setEdgeWeight(newEdge, Double.POSITIVE_INFINITY);
    			cloneGraph.setEdgeWeight(newEdge, 1E10);
        	}
        		
        	if (node.getTopoPosition() != -1 && cloneGraph.outDegreeOf(node) == 0)
        	{
        		Edge newEdge = new Edge();
        		cloneGraph.addEdge(node, sink, newEdge);
//	        		cloneGraph.setEdgeWeight(newEdge, Double.POSITIVE_INFINITY);
        		cloneGraph.setEdgeWeight(newEdge, 1E10);
        		
        	}
        }
        EdmondsKarpMFImpl<Node, Edge> minCutAlgo = new EdmondsKarpMFImpl<Node, Edge>(cloneGraph);
        minCutAlgo.calculateMinCut(source, sink);
        Set<Edge> minCut = minCutAlgo.getCutEdges();
        
        System.out.println("Min cut: ");
        for (Edge edge : minCut) {
        	if (edge.getsource().getId() != source.getId())
        		depGraph.addMinCut(edge.getsource());
        	else if (edge.gettarget().getId() != sink.getId())
        		depGraph.addMinCut(edge.gettarget());
        	System.out.println(edge.getsource().getId() + "\t" + edge.gettarget().getId() + "\t" + edge.getweight());
        }
        System.out.println("Edges of new graph clone: ");
        for(Edge edge : cloneGraph.edgeSet())
        {
        	System.out.println(edge.getsource().getId() + "\t" + edge.gettarget().getId() + "\t" + edge.getweight());
        }
	}
	
	private static void selectTargetRandom(
		final DependencyGraph depGraph,
		final RandomDataGenerator rand,
		final int numTarget
	) {
		if (depGraph == null || rand == null || numTarget < 1) {
			throw new IllegalArgumentException();
		}
		List<Node> nodeList = new ArrayList<Node>(depGraph.vertexSet());
		for (Node node : depGraph.vertexSet()) {
			if (depGraph.outDegreeOf(node) == 0) {
				node.setType(NodeType.TARGET);
				depGraph.addTarget(node);
			}
		}
		int curNumTarget = depGraph.getTargetSet().size();
		while (curNumTarget < numTarget) {
			int idx = rand.nextInt(0, nodeList.size() - 1);
			Node node = nodeList.get(idx);
			if (node.getType() != NodeType.TARGET) {
				node.setType(NodeType.TARGET);
				depGraph.addTarget(node);
				curNumTarget++;
			}
		}
	}
	
	private static void setNodeTypeRandom(
		final DependencyGraph depGraph,
		final Node node,
		final RandomDataGenerator rand,
		final double typePivot
	) {
		if (depGraph == null || node == null || rand == null || !isProb(typePivot)) {
			throw new IllegalArgumentException();
		}
		if (depGraph.inDegreeOf(node) != 0) { // non-root nodes
			double value = rand.nextUniform(0, 1, true);
			if (value <= typePivot) {
				node.setActivationType(NodeActivationType.AND);
			}
		} else { // root nodes
			node.setActivationType(NodeActivationType.AND);
		}
	}
	
	private static void genAlertProbRandom(
		final Node node,
		final RandomDataGenerator rand, 
		final double minPosActiveProb, 
		final double maxPosActiveProb, 
		final double minPosInactiveProb, 
		final double maxPosInactiveProb
	) {
		if (
			node == null || rand == null
			|| !isProb(minPosActiveProb) || !isProb(maxPosActiveProb) || minPosActiveProb > maxPosActiveProb
			|| !isProb(minPosInactiveProb) || !isProb(maxPosInactiveProb) || minPosInactiveProb > maxPosInactiveProb
		) {
			throw new IllegalArgumentException();
		}
		double posActiveProb = rand.nextUniform(minPosActiveProb, maxPosActiveProb, true);
		double posInactiveProb = rand.nextUniform(minPosInactiveProb, maxPosInactiveProb, true);
		node.setPosActiveProb(posActiveProb);
		node.setPosInactiveProb(posInactiveProb);
	}
	
	private static void genNodePayoffRandom(
		final Node node, 
		final RandomDataGenerator rand, 
		final double aRewardLB, 
		final double aRewardUB, 
		final double dPenaltyLB, 
		final double dPenaltyUB, 
		final double aCostLB, 
		final double aCostUB, 
		final double dCostLB, 
		final double dCostUB
	) {
		if (
			node == null || rand == null
			|| aRewardLB > aRewardUB || dPenaltyLB > dPenaltyUB || aCostLB > aCostUB || dCostLB > dCostUB
		) {
			throw new IllegalArgumentException();
		}
		double aReward = rand.nextUniform(aRewardLB, aRewardUB, true);
		double dPenalty = rand.nextUniform(dPenaltyLB, dPenaltyUB, true);
		node.setAReward(aReward);
		node.setDPenalty(dPenalty);
		
//		if (node.getType() == NodeType.TARGET) {
//			double aCost = 2 * rand.nextUniform(aCostLB, aCostUB, true);
//			double dCost = 2 * rand.nextUniform(dCostLB, dCostUB, true);
//			node.setACost(aCost);
//			node.setDCost(dCost);
//		} else 
		{
			double aCost = rand.nextUniform(aCostLB, aCostUB, true);
			double dCost = rand.nextUniform(dCostLB, dCostUB, true);
			node.setACost(aCost);
			node.setDCost(dCost);
		}
		
	}
	
	private static void genEdgePayoffRandom(final Edge edge, final RandomDataGenerator rand
		, final double aCostLB, final double aCostUB) {
		double aCost = rand.nextUniform(aCostLB, aCostUB, true);
		edge.setACost(aCost);
	}
	
	private static void genActivationProbRandom(final Node node, final RandomDataGenerator rand
		, final double aActProbLB, final double aActProbUB) {
		double aActProb = rand.nextUniform(aActProbLB, aActProbUB, true);
		node.setActProb(aActProb);
	}
	
	private static void genActivationProbRandom(final Edge edge, final RandomDataGenerator rand
		, final double aActProbLB, final double aActProbUB) {
		double aActProb = rand.nextUniform(aActProbLB, aActProbUB, true);
		edge.setActProb(aActProb);
	}
	
	private static void setTopologicalOrder(final DependencyGraph depGraph) {
		TopologicalOrderIterator<Node, Edge> topoOrderIter = new TopologicalOrderIterator<Node, Edge>(depGraph);
		int pos = 0;
		while (topoOrderIter.hasNext()) {
			Node node = topoOrderIter.next();
			node.setTopoPosition(pos++);
		}
	}
	
	private static void setRootSet(final DependencyGraph depGraph) {
		for (Node node : depGraph.vertexSet()) {
			if (depGraph.inDegreeOf(node) == 0) {
				depGraph.addRoot(node);
			}
		}
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
