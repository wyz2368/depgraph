package main;

import org.apache.commons.math3.random.RandomDataGenerator;

import utils.DGraphUtils;
import model.DependencyGraph;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;

public final class TestGraphGen {
	
	private TestGraphGen() {
		// private constructor
	}
	
	public static void main(final String[] args) {
		final int numNode = 30;
		final int numEdge = 100;
		final int numTarget = 2;
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
        DGraphUtils.save("./TestGraphUtil_1.json", depGraph);
        DependencyGraph depGraphClone = DGraphUtils.loadGraph("./TestGraphUtil_1.json");
        DGraphUtils.save("./TestGraphUtil_2.json", depGraphClone);
	}
}
