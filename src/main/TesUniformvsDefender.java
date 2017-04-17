package main;

import game.GameSimulation;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.Attacker;
import agent.Defender;
import agent.GoalOnlyDefender;
import agent.UniformAttacker;
import agent.UniformvsDefender;

public class TesUniformvsDefender {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numNode = 100;
		int numEdge = 300;
		int numTarget = 20;
		double nodeActTypeRatio = 0.5;
		double aRewardLB = 2.0;
		double aRewardUB = 10.0;
		double dPenaltyLB = -10.0;
		double dPenaltyUB = -2.0;
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
		double minPosActiveProb = 0.99;
		double maxPosActiveProb = 1.0;
		double minPosInactiveProb = 0.0;
		double maxPosInactiveProb = 0.01;
		
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
		
		int maxNumSelectCandidate = 10;
		int minNumSelectCandidate = 2;
		double numSelectCandidateRatio = 0.7;
		
		double discFact = 0.9;
		
		int maxNumRes = 10;
		int minNumRes = 2;
		double numResRatio = 0.7;
		double logisParam = 5.0;
		double thres = 1e-3;
		
		int numTimeStep = 6;
		int numSim = 200;
		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		Defender uniformvsDefender = new UniformvsDefender(logisParam, discFact, thres
				, maxNumRes, minNumRes, numResRatio
				, maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio);
		
		Attacker uniformAttacker = new UniformAttacker(maxNumSelectCandidate, minNumSelectCandidate
				, numSelectCandidateRatio);
		
		
		GameSimulation gameSimUvsGO = new GameSimulation(depGraph, uniformAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffUvsGO = 0.0;
		double attPayoffUvsGO = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimUvsGO.runSimulation();
			gameSimUvsGO.printPayoff();
			defPayoffUvsGO += gameSimUvsGO.getSimulationResult().getDefPayoff();
			attPayoffUvsGO += gameSimUvsGO.getSimulationResult().getAttPayoff();
			gameSimUvsGO.reset();
		}
		defPayoffUvsGO /= numSim;
		attPayoffUvsGO /= numSim;
		
		GameSimulation gameSimUvsUvs = new GameSimulation(depGraph, uniformAttacker, uniformvsDefender, rnd, numTimeStep, discFact);
		double defPayoffUvsUvs = 0.0;
		double attPayoffUvsUvs = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimUvsUvs.runSimulation();
			gameSimUvsUvs.printPayoff();
			defPayoffUvsUvs += gameSimUvsUvs.getSimulationResult().getDefPayoff();
			attPayoffUvsUvs += gameSimUvsUvs.getSimulationResult().getAttPayoff();
			gameSimUvsUvs.reset();
		}
		defPayoffUvsUvs /= numSim;
		attPayoffUvsUvs /= numSim;
		
		System.out.println("Defender goal only payoff: " + defPayoffUvsGO);
		System.out.println("Attacker uniform payoff: " + attPayoffUvsGO);
		System.out.println();
		
		System.out.println("Defender uniform vs payoff: " + defPayoffUvsUvs);
		System.out.println("Attacker uniform payoff: " + attPayoffUvsUvs);
		System.out.println();
	}

}
