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
import agent.UniformVsDefender;

public final class TestUniformVsDefender {
	
	private TestUniformVsDefender() {
		// private constructor
	}

	public static void main(final String[] args) {
		final int numNode = 30;
		final int numEdge = 90;
		final int numTarget = 20;
		final double nodeActTypeRatio = 0.5;
		final double aRewardLB = 2.0;
		final double aRewardUB = 10.0;
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
		final double minPosActiveProb = 0.99;
		final double maxPosActiveProb = 1.0;
		final double minPosInactiveProb = 0.0;
		final double maxPosInactiveProb = 0.01;
		
		Node.resetCounter();
		Edge.resetCounter();
		RandomDataGenerator rnd = new RandomDataGenerator();
		rnd.reSeed(System.currentTimeMillis());
		DependencyGraph depGraph =
			DagGenerator.genRandomDAG(numNode, numEdge, rnd);
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
		
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.7;
		
		final double discFact = 0.9;
		
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.7;
		final double logisParam = 5.0;
		final double thres = 1e-3;
		
		final int numTimeStep = 6;
		final int numSim = 50;
		Defender goalOnlyDefender = new GoalOnlyDefender(
			maxNumRes, minNumRes, numResRatio, logisParam, discFact, 0.0);
		Defender uniformvsDefender = new UniformVsDefender(
			logisParam, discFact, thres
			, maxNumRes, minNumRes, numResRatio
			, maxNumSelectCandidate, minNumSelectCandidate,
			numSelectCandidateRatio);
		
		Attacker uniformAttacker =
			new UniformAttacker(maxNumSelectCandidate, minNumSelectCandidate
			, numSelectCandidateRatio, 0.0);
		
		GameSimulation gameSimUvsGO =
			new GameSimulation(depGraph, uniformAttacker,
				goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffUvsGO = 0.0;
		double attPayoffUvsGO = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimUvsGO.runSimulation();
			gameSimUvsGO.printPayoff();
			defPayoffUvsGO += gameSimUvsGO.getSimulationResult().getDefPayoff();
			attPayoffUvsGO += gameSimUvsGO.getSimulationResult().getAttPayoff();
			gameSimUvsGO.reset();
		}
		defPayoffUvsGO /= numSim;
		attPayoffUvsGO /= numSim;
		
		GameSimulation gameSimUvsUvs =
			new GameSimulation(depGraph, uniformAttacker,
				uniformvsDefender, rnd, numTimeStep, discFact);
		double defPayoffUvsUvs = 0.0;
		double attPayoffUvsUvs = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimUvsUvs.runSimulation();
			gameSimUvsUvs.printPayoff();
			defPayoffUvsUvs +=
				gameSimUvsUvs.getSimulationResult().getDefPayoff();
			attPayoffUvsUvs +=
				gameSimUvsUvs.getSimulationResult().getAttPayoff();
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
