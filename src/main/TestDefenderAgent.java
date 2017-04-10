package main;

import game.GameOracle;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import graph.INode.NODE_STATE;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.GoalOnlyDefender;
import agent.MinCutDefender;
import agent.UniformDefender;

public class TestDefenderAgent {

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
		double numSelectCandidateRatio = 0.7;
		
		double qrParam = 1.0;
		double discFact = 0.9;
		
		int maxNumRes = 5;
		int minNumRes = 2;
		double numResRatio = 0.7;
		double logisParam = 1.0;
		
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
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				gameState.addEnabledNode(node);
		}
		DefenderObservation defObservation = GameOracle.generateDefObservation(depGraph, gameState, rnd);
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
