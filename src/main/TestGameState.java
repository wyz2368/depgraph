package main;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.INode.NodeType;
import graph.Node;
import model.DefenderBelief;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;

public final class TestGameState {

	public static void main(final String[] args) {
		final int numNode = 50;
		final int numEdge = 150;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.3;
		final double aRewardLB = 1.0;
		final double aRewardUB = 10.0;
		final double dPenaltyLB = -10.0;
		final double dPenaltyUB = -1.0;
		final double aNodeCostLB = -0.5;
		final double aNodeCostUB = -0.1;
		final double aEdgeCostLB = -0.5;
		final double aEdgeCostUB = -0.1;
		final double dCostLB = -0.5;
		final double dCostUB = -0.1;
		final double aNodeActProbLB = 0.8;
		final double aNodeActProbUB = 1.0;
		final double aEdgeActProbLB = 0.6;
		final double aEdgeActProbUB = 0.8;
		final double minPosActiveProb = 0.8;
		final double maxPosActiveProb = 1.0;
		final double minPosInactiveProb = 0.0;
		final double maxPosInactiveProb = 0.2;
		
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
		for (Node node : depGraph.vertexSet()) {
			gameState1.addEnabledNode(node);
		}
		gameState1.createID();
		
		GameState gameState2 = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getType() == NodeType.TARGET) {
				gameState2.addEnabledNode(node);
			}
		}
		gameState2.createID();
		
		System.out.println(gameState1.getID() + "\t" + gameState1.getEnabledNodeSet().size());
		System.out.println(gameState2.getID() + "\t" + gameState2.getEnabledNodeSet().size());
		
		boolean isEqual = gameState2.equals(gameState1);
		if (isEqual) {
			System.out.println("Wrong");
		} else {
			System.out.println("Correct");
		}
		
		DefenderBelief dBelief = new DefenderBelief();
		dBelief.addState(gameState1, 1.0);
		Double value = dBelief.getProbability(gameState2);
		if (value == null) {
			System.out.println("Correct");
		}
	}
}
