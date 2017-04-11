package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import graph.INode.NODE_ACTIVATION_TYPE;
import graph.INode.NODE_STATE;
import graph.INode.NODE_TYPE;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class DGraphGenerator {
	static double probLB = 0.8;
	static double negProbUB = 0.1;
	static double eProbLB = 0.9900;
	
	// All leaf nodes are targets, all costs, reward, penalty are within [0,1]
	public static void genGraph(DependencyGraph dag, RandomDataGenerator rand
			, int numTarget, double nodeActTypeRatio
			, double aRewardLB, double aRewardUB
			, double dPenaltyLB, double dPenaltyUB
			, double aNodeCostLB, double aNodeCostUB
			, double aEdgeCostLB, double aEdgeCostUB
			, double dCostLB, double dCostUB
			, double aNodeActProbLB, double aNodeActProbUB
			, double aEdgeActProbLB, double aEdgeActProbUB
			, double minPosActiveProb, double maxPosActiveProb
			, double minPosInactiveProb, double maxPosInactiveProb)
	{
		DependencyGraph depGraph = dag;
		setTopologicalOrder(depGraph);
		selectTargetRandom(depGraph, rand, numTarget);
		for(Node node : depGraph.vertexSet())
		{
			setNodeTypeRandom(depGraph, node, rand, nodeActTypeRatio);
			genNodePayoffRandom(node, rand, aRewardLB, aRewardUB, dPenaltyLB, dPenaltyUB, aNodeCostLB, aNodeCostUB, dCostLB, dCostUB);
			genActivationProbRandom(depGraph, node, rand, aNodeActProbLB, aNodeActProbUB);
			if(node.getType() != NODE_TYPE.TARGET)
			{
				node.setAReward(0.0);
				node.setDPenalty(0.0);
			}
			
			if(node.getActivationType() != NODE_ACTIVATION_TYPE.AND)
			{
				node.setActProb(0.0);
				node.setACost(0.0);
			}
			genAlertProbRandom(node, rand, minPosActiveProb, maxPosActiveProb, minPosInactiveProb, maxPosInactiveProb);
		}
		for(Edge edge : depGraph.edgeSet())
		{
			genEdgePayoffRandom(depGraph, edge, rand, aEdgeCostLB, aEdgeCostUB);
			if(edge.gettarget().getActivationType() == NODE_ACTIVATION_TYPE.OR)
				genActivationProbRandom(depGraph, edge, rand, aEdgeActProbLB, aEdgeActProbUB);
		}
	}
	public static GameState randomizeInitialGraphState(DependencyGraph depGraph, RandomDataGenerator rand, double pivot)
	{
		GameState gameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			double value = rand.nextUniform(0, 1, true);
			if(value <= pivot)
			{
				node.setState(NODE_STATE.ACTIVE);
				gameState.addEnabledNode(node);
			}
			else
				node.setState(NODE_STATE.INACTIVE);
		}
		return gameState;
	}
	public static void selectTargetRandom(DependencyGraph depGraph, RandomDataGenerator rand, int numTarget)
	{
		List<Node> nodeList = new ArrayList<Node>(depGraph.vertexSet());
		for(Node node : depGraph.vertexSet())
		{
			if(depGraph.outDegreeOf(node) == 0)
			{
				node.setType(NODE_TYPE.TARGET);
				depGraph.addTarget(node);
			}
		}
		int curNumTarget = depGraph.getTargetSet().size();
		while(curNumTarget < numTarget)
		{
			int idx = rand.nextInt(0, nodeList.size() - 1);
			Node node = nodeList.get(idx);
			if(node.getType() != NODE_TYPE.TARGET)
			{
				node.setType(NODE_TYPE.TARGET);
				depGraph.addTarget(node);
				curNumTarget++;
			}
		}
	}
	public static void setNodeTypeRandom(DependencyGraph depGraph, Node node, RandomDataGenerator rand, double typePivot)
	{
		if(depGraph.inDegreeOf(node) != 0) // non-root nodes
		{
			double value = rand.nextUniform(0, 1, true);
			if(value <= typePivot)
				node.setActivationType(NODE_ACTIVATION_TYPE.AND);
		}
		else // root nodes
			node.setActivationType(NODE_ACTIVATION_TYPE.AND);
	}
	public static void genAlertProbRandom(Node node, RandomDataGenerator rand
			, double minPosActiveProb, double maxPosActiveProb
			, double minPosInactiveProb, double maxPosInactiveProb)
	{
		double posActiveProb = rand.nextUniform(minPosActiveProb, maxPosActiveProb, true);
		double posInactiveProb = rand.nextUniform(minPosInactiveProb, maxPosInactiveProb, true);
		node.setPosActiveProb(posActiveProb);
		node.setPosInactiveProb(posInactiveProb);
	}
	public static void genNodePayoffRandom(Node node, RandomDataGenerator rand
			, double aRewardLB, double aRewardUB
			, double dPenaltyLB, double dPenaltyUB
			, double aCostLB, double aCostUB
			, double dCostLB, double dCostUB)
	{
		double aReward = rand.nextUniform(aRewardLB, aRewardUB, true);
		double dPenalty = rand.nextUniform(dPenaltyLB, dPenaltyUB, true);
		node.setAReward(aReward);
		node.setDPenalty(dPenalty);
		
		if(node.getType() == NODE_TYPE.TARGET)
		{
			double aCost = 2 * rand.nextUniform(aCostLB, aCostUB, true);
			double dCost = 2 * rand.nextUniform(dCostLB, dCostUB, true);
			node.setACost(aCost);
			node.setDCost(dCost);
		}
		else
		{
			double aCost = rand.nextUniform(aCostLB, aCostUB, true);
			double dCost = rand.nextUniform(dCostLB, dCostUB, true);
			node.setACost(aCost);
			node.setDCost(dCost);
		}
		
	}
	public static void genEdgePayoffRandom(DependencyGraph depGraph, Edge edge, RandomDataGenerator rand
			, double aCostLB, double aCostUB)
	{
		double aCost = rand.nextUniform(aCostLB, aCostUB, true);
		edge.setACost(aCost);
	}
	
	public static void genActivationProbRandom(DependencyGraph depGraph, Node node, RandomDataGenerator rand
			, double aActProbLB, double aActProbUB)
	{
		double aActProb = rand.nextUniform(aActProbLB, aActProbUB, true);
		node.setActProb(aActProb);
	}
	public static void genActivationProbRandom(DependencyGraph depGraph, Edge edge, RandomDataGenerator rand
			, double aActProbLB, double aActProbUB)
	{
		double aActProb = rand.nextUniform(aActProbLB, aActProbUB, true);
		edge.setActProb(aActProb);
	}
	public static void setTopologicalOrder(DependencyGraph depGraph)
	{
		TopologicalOrderIterator<Node, Edge> topoOrderIter = new TopologicalOrderIterator<Node, Edge>(depGraph);
		int pos = 0;
		while(topoOrderIter.hasNext())
		{
			Node node = (Node) topoOrderIter.next();
			node.setTopoPosition(pos++);
		}
	}
	public static void findMinCut(DependencyGraph depGraph)
	{
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
    			cloneGraph.setEdgeWeight(newEdge, Double.POSITIVE_INFINITY);
        	}
        		
        	if(node.getTopoPosition() != -1 && cloneGraph.outDegreeOf(node) == 0)
        	{
        		Edge newEdge = new Edge();
        		cloneGraph.addEdge(node, sink, newEdge);
        		cloneGraph.setEdgeWeight(newEdge, Double.POSITIVE_INFINITY);
        	}
        }
        EdmondsKarpMFImpl<Node, Edge> minCutAlgo = new EdmondsKarpMFImpl<Node, Edge>(cloneGraph);
        minCutAlgo.calculateMinCut(source, sink);
        Set<Edge> minCut = minCutAlgo.getCutEdges();
        for(Edge edge : minCut)
        {
        	depGraph.addMinCut(edge.getsource());
        }
	}
}
