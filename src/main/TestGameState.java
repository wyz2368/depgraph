package main;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.INode.NODE_TYPE;
import graph.Node;
import model.DefenderBelief;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;

public class TestGameState {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numNode = 50;
		int numEdge = 150;
		int numTarget = 10;
		double nodeActTypeRatio = 0.3;
		double aRewardLB = 1.0;
		double aRewardUB = 10.0;
		double dPenaltyLB = -10.0;
		double dPenaltyUB = -1.0;
		double aNodeCostLB = -0.5;
		double aNodeCostUB = -0.1;
		double aEdgeCostLB = -0.5;
		double aEdgeCostUB = -0.1;
		double dCostLB = -0.5;
		double dCostUB = -0.1;
		double aNodeActProbLB = 0.8;
		double aNodeActProbUB = 1.0;
		double aEdgeActProbLB = 0.6;
		double aEdgeActProbUB = 0.8;
		double minPosActiveProb = 0.8;
		double maxPosActiveProb = 1.0;
		double minPosInactiveProb = 0.0;
		double maxPosInactiveProb = 0.2;
		
		Node.resetCounter();
		Edge.resetCounter();
		RandomDataGenerator rnd = new RandomDataGenerator();
		rnd.reSeed(System.currentTimeMillis());
		DependencyGraph depGraph = DagGenerator.genRandomDAG(numNode, numEdge, rnd);
		DGraphGenerator.genGraph(depGraph, rnd
				, numTarget, nodeActTypeRatio
				, aRewardLB, aRewardUB
				, dPenaltyLB, dPenaltyUB
				, aNodeCostLB, aNodeCostUB
				, aEdgeCostLB, aEdgeCostUB
				, dCostLB, dCostUB
				, aNodeActProbLB, aNodeActProbUB
				, aEdgeActProbLB, aEdgeActProbUB
				, minPosActiveProb, maxPosActiveProb
				, minPosInactiveProb, maxPosInactiveProb);
		DGraphGenerator.findMinCut(depGraph);
		
		GameState gameState1 = new GameState();
		for(Node node : depGraph.vertexSet())
			gameState1.addEnabledNode(node);
		
		GameState gameState2 = new GameState();
		for(Node node : depGraph.vertexSet())
		if(node.getType() == NODE_TYPE.TARGET)
			gameState2.addEnabledNode(node);
		
		boolean isEqual = gameState2.equals(gameState1);
		if(isEqual)
			System.out.println("Correct");
		else
			System.out.println("Wrong");
		
		DefenderBelief dBelief = new DefenderBelief();
		dBelief.addState(gameState1, 1.0);
		Double value = dBelief.getProbability(gameState2);
		if(value == null)
			System.out.println("Mistake");
	}

}
