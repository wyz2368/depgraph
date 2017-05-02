package main;

import java.io.File;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;

import utils.DGraphUtils;
import utils.JsonUtils;

public final class MainGraphGen02 {
	
	private MainGraphGen02() {
		// private constructor
	}

	public static void main(final String[] args) {
		if (args == null || args.length != 1) {
			throw new IllegalStateException(
				"Need 1 argument: graphFolder");
		}
		String folderPath = args[0];
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.5;
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
//		
		Node.resetCounter();
		Edge.resetCounter();
		RandomDataGenerator rnd = new RandomDataGenerator();
		rnd.reSeed(System.currentTimeMillis());

		int numLayer = 10;
		int numNode1Layer = 25;
		double numNodeRatio = 0.8;
		double numEdgeRatio = 0.5;
		
		double aNodeCostFactor = 1.5;
		double aEdgeCostFactor = 1.5;
		double dCostFactor = 1.5;
		
		int numSample = 50;
		
		genSepLayerGraph(
				folderPath, 
				numLayer, numNode1Layer, 
				numNodeRatio, numEdgeRatio, 
				aNodeCostFactor, aEdgeCostFactor, 
				dCostFactor, 
				numTarget, nodeActTypeRatio, 
				aRewardLB, aRewardUB, 
				dPenaltyLB, dPenaltyUB, 
				aNodeCostLB, aNodeCostUB, 
				aEdgeCostLB, aEdgeCostUB, 
				dCostLB, dCostUB, 
				aNodeActProbLB, aNodeActProbUB,
				aEdgeActProbLB, aEdgeActProbUB, 
				minPosActiveProb, maxPosActiveProb, 
				minPosInactiveProb, maxPosInactiveProb, 
				rnd, numSample);
	}
	public static void genRandomGraph(
			final String folderPath, 
			final int numNode,
			final int numEdge,
			final int numTarget,
			final double nodeActTypeRatio,
			final double aRewardLB,
			final double aRewardUB,
			final double dPenaltyLB,
			final double dPenaltyUB,
			final double aNodeCostLB,
			final double aNodeCostUB,
			final double aEdgeCostLB,
			final double aEdgeCostUB,
			final double dCostLB,
			final double dCostUB,
			final double aNodeActProbLB,
			final double aNodeActProbUB,
			final double aEdgeActProbLB,
			final double aEdgeActProbUB,
			final double minPosActiveProb,
			final double maxPosActiveProb,
			final double minPosInactiveProb,
			final double maxPosInactiveProb,
			final RandomDataGenerator rnd,
			final int numSample) {
		for (int idx = 0; idx < numSample; idx++) {
			Node.resetCounter();
			Edge.resetCounter();
			String filePathName = folderPath + File.separator
				+ "RandomGraph" + numNode + "N" + numEdge + "E" 
				+ numTarget + "T" + idx + JsonUtils.JSON_SUFFIX;
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
			DGraphUtils.save(filePathName, depGraph);
		}
		
	}
	public static void genSepLayerGraph(
			final String folderPath, 
			final int numLayer,
			final int numNode1Layer,
			final double numNodeRatio,
			final double numEdgeRatio,
			final double aNodeCostFactor,
			final double aEdgeCostFactor,
			final double dCostFactor,
			final int numTarget,
			final double nodeActTypeRatio,
			final double aRewardLB,
			final double aRewardUB,
			final double dPenaltyLB,
			final double dPenaltyUB,
			final double aNodeCostLB,
			final double aNodeCostUB,
			final double aEdgeCostLB,
			final double aEdgeCostUB,
			final double dCostLB,
			final double dCostUB,
			final double aNodeActProbLB,
			final double aNodeActProbUB,
			final double aEdgeActProbLB,
			final double aEdgeActProbUB,
			final double minPosActiveProb,
			final double maxPosActiveProb,
			final double minPosInactiveProb,
			final double maxPosInactiveProb,
			final RandomDataGenerator rnd,
			final int numSample) {
		for (int idx = 0; idx < numSample; idx++) {
			Node.resetCounter();
			Edge.resetCounter();
			String filePathName = folderPath + File.separator
				+ "SepLayerGraph" + idx + JsonUtils.JSON_SUFFIX;
			DependencyGraph depGraph =
					DagGenerator.genRandomSepLayDAG(numLayer, numNode1Layer, numNodeRatio, numEdgeRatio, rnd);
			DGraphGenerator.genSepLayGraph(depGraph, rnd, 
					numTarget, nodeActTypeRatio, 
					aRewardLB, aRewardUB, 
					dPenaltyLB, dPenaltyUB, 
					aNodeCostLB, aNodeCostUB, 
					aEdgeCostLB, aEdgeCostUB, 
					dCostLB, dCostUB, 
					aNodeActProbLB, aNodeActProbUB, 
					aEdgeActProbLB, aEdgeActProbUB, 
					minPosActiveProb, maxPosActiveProb, 
					minPosInactiveProb, maxPosInactiveProb, 
					aNodeCostFactor, aEdgeCostFactor, dCostFactor);
			DGraphGenerator.findMinCut(depGraph);
			
			DGraphUtils.save(filePathName, depGraph);
		}
		
	}
}
