package main;

import game.GameOracle;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import graph.INode.NodeState;
import model.DefenderAction;
import model.DefenderBelief;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.GoalOnlyDefender;
import agent.MinCutDefender;
import agent.UniformDefender;

public final class TestDefenderAgent {

	public static void main(String[] args) {
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
		
		final double discFact = 0.9;
		
		final int maxNumRes = 5;
		final int minNumRes = 2;
		final double numResRatio = 0.7;
		final double logisParam = 1.0;
		
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
		DGraphGenerator.findMinCut(depGraph);
		depGraph.print();
		
		GameState gameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				gameState.addEnabledNode(node);
			}
		}
		GameOracle.generateDefObservation(depGraph, gameState, rnd);
		DefenderBelief defBelief = new DefenderBelief();
		defBelief.addState(gameState, 0.7);
		
		UniformDefender uniformDefender = new UniformDefender(maxNumRes, minNumRes, numResRatio);
		DefenderAction dUniformAction = uniformDefender.sampleAction(depGraph, curTimeStep, numTimeStep
				, defBelief
				, rnd.getRandomGenerator());
		dUniformAction.print();
		
		MinCutDefender minCutDefender = new MinCutDefender(maxNumRes, minNumRes, numResRatio);
		DefenderAction dMinCutAction = minCutDefender.sampleAction(depGraph, curTimeStep, numTimeStep
				, defBelief
				, rnd.getRandomGenerator());
		dMinCutAction.print();
		
		GoalOnlyDefender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		DefenderAction dGoalOnlyAction = goalOnlyDefender.sampleAction(depGraph, curTimeStep, numTimeStep
				, defBelief
				, rnd.getRandomGenerator());
		dGoalOnlyAction.print();
	}
}
