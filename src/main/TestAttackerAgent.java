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
import agent.RandomWalkAttacker;
import agent.UniformAttacker;
import agent.ValuePropagationAttacker;

public final class TestAttackerAgent {
	
	private TestAttackerAgent() {
		// private constructor
	}

	public static void main(final String[] args) {
		final int numNode = 50;
		final int numEdge = 150;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.3;
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
		depGraph.print();
		
		final double logisParam = 5.0;
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.7;
		
		
		final double qrParam = 5.0;
		final double discFact = 0.9;
		final int numRWSample = 100;
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.7;
		
		final int numTimeStep = 10;
		final int numSim = 200;
		
		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact, 0.0);
		
		UniformAttacker unAttacker =
			new UniformAttacker(maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio, 0.0);
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate
				, numSelectCandidateRatio, qrParam, discFact);
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
		
		final double thousand = 1000.0;
		
		long start = System.currentTimeMillis();
		GameSimulation gameSimUNvsGO =
			new GameSimulation(depGraph, unAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffUNvsGO = 0.0;
		double attPayoffUNvsGO = 0.0;
		double timeUNvsGO = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimUNvsGO.runSimulation();
			gameSimUNvsGO.printPayoff();
			defPayoffUNvsGO += gameSimUNvsGO.getSimulationResult().getDefPayoff();
			attPayoffUNvsGO += gameSimUNvsGO.getSimulationResult().getAttPayoff();
			gameSimUNvsGO.reset();
		}
		long end = System.currentTimeMillis();
		defPayoffUNvsGO /= numSim;
		attPayoffUNvsGO /= numSim;
		timeUNvsGO = (end - start) / thousand / numSim;
				
		start = System.currentTimeMillis();
		GameSimulation gameSimVPvsGO =
			new GameSimulation(depGraph, vpAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsGO = 0.0;
		double attPayoffVPvsGO = 0.0;
		double timeVPvsGO = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimVPvsGO.runSimulation();
			gameSimVPvsGO.printPayoff();
			defPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getDefPayoff();
			attPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getAttPayoff();
			gameSimVPvsGO.reset();
		}
		end = System.currentTimeMillis();
		defPayoffVPvsGO /= numSim;
		attPayoffVPvsGO /= numSim;
		timeVPvsGO = (end - start) / thousand / numSim;
		
		start = System.currentTimeMillis();
		GameSimulation gameSimRWvsGO =
			new GameSimulation(depGraph, rwAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsGO = 0.0;
		double attPayoffRWvsGO = 0.0;
		double timeRWvsGO = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimRWvsGO.runSimulation();
			gameSimRWvsGO.printPayoff();
			defPayoffRWvsGO += gameSimRWvsGO.getSimulationResult().getDefPayoff();
			attPayoffRWvsGO += gameSimRWvsGO.getSimulationResult().getAttPayoff();
			gameSimRWvsGO.reset();
		}
		end = System.currentTimeMillis();
		defPayoffRWvsGO /= numSim;
		attPayoffRWvsGO /= numSim;
		timeRWvsGO = (end - start) / thousand / numSim;
		
		
		System.out.println("Defender goal node payoff: " + defPayoffUNvsGO);
		System.out.println("Attacker value uniform payoff: " + attPayoffUNvsGO);
		System.out.println("Runtime per simulation: " + timeUNvsGO);
		System.out.println();
		
		System.out.println();
		System.out.println("Defender goal payoff: " + defPayoffVPvsGO);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsGO);
		System.out.println("Runtime per simulation: " + timeVPvsGO);
		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffRWvsGO);
		System.out.println("Attacker value random walk payoff: " + attPayoffRWvsGO);
		System.out.println("Runtime per simulation: " + timeRWvsGO);
		System.out.println();
	}
}
