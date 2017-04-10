package main;


import org.apache.commons.math3.random.RandomDataGenerator;

import utils.DGraphUtils;
import model.DependencyGraph;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;

public class TestGraphGen {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numNode = 30;
		int numEdge = 100;
		int numTarget = 2;
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
        DGraphUtils.save("./TestGraphUtil_1.json", depGraph);
        DependencyGraph depGraphClone = DGraphUtils.loadGraph("./TestGraphUtil_1.json");
        DGraphUtils.save("./TestGraphUtil_2.json", depGraphClone);
		
	}

}
