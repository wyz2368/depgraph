package main;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.RandomWalkAttacker;
import agent.UniformAttacker;
import agent.ValuePropagationAttacker;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("static-method")
public final class UnitTestAttackerAgent {
	
	private static DependencyGraph depGraph;
	private static RandomDataGenerator rnd;
	private static UniformAttacker uniformAttacker;
	private static ValuePropagationAttacker vpAttacker;
	private static RandomWalkAttacker rwAttacker;
	
	public UnitTestAttackerAgent() {
		// default constructor
	}
	
	@Before
	public void setup() {
		final int numNode = 30;
		final int numEdge = 100;
		final int numTarget = 5;
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
		
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 5;
		final double numSelectCandidateRatio = 0.7;
		
		final double qrParam = 1.0;
		final double discFact = 0.9;
		final int numRWSample = 10;
		
		Node.resetCounter();
		Edge.resetCounter();
		rnd = new RandomDataGenerator();
		rnd.reSeed(System.currentTimeMillis());
		depGraph = DagGenerator.genRandomDAG(numNode, numEdge, rnd);
		
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
		final double pivot = 0.2;
		DGraphGenerator.randomizeInitialGraphState(depGraph, rnd, pivot);
		
		uniformAttacker = new UniformAttacker(
			maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio);
		vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate,
			minNumSelectCandidate, numSelectCandidateRatio
			, qrParam, discFact);
		rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
	}

	@Test
	public void testActions() {
		final int numTimeStep = 6;
		final int curTimeStep = 2;
		uniformAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		
		vpAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		
		rwAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
	}
}
