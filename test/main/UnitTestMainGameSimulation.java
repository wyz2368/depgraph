package main;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import game.GameSimulation;

import static org.junit.Assert.assertTrue;


import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

@SuppressWarnings("static-method")
public final class UnitTestMainGameSimulation {

	public UnitTestMainGameSimulation() {
		// default constuctor
	}
	
	@Test
	@SuppressWarnings("all")
	public void assertionTest() {
		boolean assertionsOn = false;
		// assigns true if assertions are on.
		assert assertionsOn = true;
		assertTrue(assertionsOn);
	}

	@Test
	public void basicTest() {		
		final int numNode = 50;
		final int numEdge = 150;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.3;
		final double aRewardLB = 5.0;
		final double aRewardUB = 10.0;
		final double dPenaltyLB = -10.0;
		final double dPenaltyUB = -5.0;
		final double aNodeCostLB = -1.0;
		final double aNodeCostUB = -0.5;
		final double aEdgeCostLB = -1.0;
		final double aEdgeCostUB = -0.5;
		final double dCostLB = -2.0;
		final double dCostUB = -1.0;
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
	 
		final int numSample = 5;
		for (int idx = 0; idx < numSample; idx++) {
			Node.resetCounter();
			Edge.resetCounter();
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
		}
	}
}
