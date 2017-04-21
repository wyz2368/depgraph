package game;

import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import agent.Attacker;
import agent.Defender;
import agent.UniformAttacker;
import agent.UniformDefender;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

@SuppressWarnings("static-method")
public final class UnitTestGameSimulation {

	public UnitTestGameSimulation() {
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
	public void testing() {
		final int numNode = 40;
		final int numEdge = 0;
		final int numTarget = 40;
		final double nodeActTypeRatio = 0.5;
		final double aRewardLB = 2.0;
		final double aRewardUB = 2.0;
		final double dPenaltyLB = -10.0;
		final double dPenaltyUB = -2.0;
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

		final double discFact = 0.9;
				
		final int numTimeStep = 10;
		final Defender uniformDefender = new UniformDefender(numNode, numNode, 1.0);
		final Attacker uniformAttacker = new UniformAttacker(numNode, numNode, 1.0);
				
		final GameSimulation gameSim =
			new GameSimulation(depGraph, uniformAttacker, uniformDefender, rnd, numTimeStep, discFact);
		GameSimulationResult curSimResult = gameSim.getSimulationResult();
		final double tolerance = 0.01;
		assertTrue(Math.abs(curSimResult.getAttPayoff()) < tolerance);
		assertTrue(Math.abs(curSimResult.getDefPayoff()) < tolerance);
		
		gameSim.runSimulation();
		curSimResult = gameSim.getSimulationResult();
	}
}
