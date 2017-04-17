package main;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.RandomWalkAttacker;
import agent.UniformAttacker;
import agent.ValuePropagationAttacker;

public final class TestAttackerAgent {

	public static void main(final String[] args) {
		final int numTimeStep = 6;
		final int curTimeStep = 2;
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
		DGraphGenerator.randomizeInitialGraphState(depGraph, rnd, 0.2);
		depGraph.print();
		
		UniformAttacker uniformAttacker = new UniformAttacker(maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio);
		AttackerAction uniformAction = uniformAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		uniformAction.print();
		
		ValuePropagationAttacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio
				, qrParam, discFact);
		AttackerAction vpAction = vpAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		vpAction.print();
		
		final int numRWSample = 10;
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
		AttackerAction rwAction = rwAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		rwAction.print();
	}
}
