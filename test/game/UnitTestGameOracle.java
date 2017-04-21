package game;

import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;
import model.SecurityAlert;

@SuppressWarnings("static-method")
public final class UnitTestGameOracle {

	public UnitTestGameOracle() {
		// default constructor
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
	public void testAllAlertProbs() {
		final double threeQuarters = 0.75;
		final double quarter = 0.25;
		testAlertProbs(threeQuarters, quarter);
		testAlertProbs(1.0, 1.0);
		testAlertProbs(0.0, 0.0);
	}
	
	private void testAlertProbs(
		final double minPosActiveProb,
		final double minPosInactiveProb
	) {
		assert minPosActiveProb >= minPosInactiveProb;
		final int numNode = 40;
		final double maxPosActiveProb = minPosActiveProb;
		final double maxPosInactiveProb = minPosInactiveProb;
		
		final double nodeActTypeRatio = 0.5;
		final int numEdge = 0;
		final int numTarget = 5;
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
		
		final int iters = 1000;
		int countAllEnabled = 0;
		int countNoneEnabled = 0;
		for (int i = 0; i < iters; i++) {
			final GameState allNodesEnabled = allNodes(depGraph);
			final DefenderObservation defObsAllEnabled =
				GameOracle.generateDefObservation(depGraph, allNodesEnabled, rnd);
			countAllEnabled += posAlertCount(defObsAllEnabled);
			
			final GameState noneEnabled = new GameState();
			final DefenderObservation defObsNoneEnabled =
				GameOracle.generateDefObservation(depGraph, noneEnabled, rnd);
			countNoneEnabled += posAlertCount(defObsNoneEnabled);
		}
		
		final double meanAllEnabled = countAllEnabled * 1.0 / iters;
		final double meanNoneEnabled = countNoneEnabled * 1.0 / iters;
		final double tolerance = 0.9;
		final double expectedAllEnabled = numNode * minPosActiveProb;
		final double expectedNoneEnabled = numNode * minPosInactiveProb;
		assertTrue(Math.abs(meanAllEnabled - expectedAllEnabled) < tolerance);
		assertTrue(Math.abs(meanNoneEnabled - expectedNoneEnabled) < tolerance);
	}
	
	private int posAlertCount(final DefenderObservation defObs) {
		int result = 0;
		for (final SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				result++;
			}
		}
		return result;
	}
	
	private GameState allNodes(final DependencyGraph depGraph) {
		final GameState result = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			result.addEnabledNode(node);
		}
		return result;
	}
}
