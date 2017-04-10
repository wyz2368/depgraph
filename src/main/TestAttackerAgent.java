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

public class TestAttackerAgent {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numTimeStep = 6;
		int curTimeStep = 2;
		
		int numNode = 30;
		int numEdge = 100;
		int numTarget = 5;
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
		
		int maxNumSelectCandidate = 10;
		int minNumSelectCandidate = 5;
		double numSelectCandidateRatio = 0.7;
		
		double qrParam = 1.0;
		double discFact = 0.9;
		
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
		
		int numRWSample = 10;
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
		AttackerAction rwAction = rwAttacker.sampleAction(depGraph, curTimeStep, numTimeStep, rnd.getRandomGenerator());
		rwAction.print();
	}
		
}
