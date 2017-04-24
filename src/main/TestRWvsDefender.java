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
import agent.RandomWalkVsDefender;
import agent.ValuePropagationAttacker;

public final class TestRWvsDefender {

	private TestRWvsDefender() {
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
		final double thres = 1e-3 * 0.5;
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.7;
		
		final double qrParamDef = 5.0;
		final int numRWSampleDef = 30;
		
		final double qrParam = 5.0;
		final double discFact = 0.9;
		final int numRWSample = 100;
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.7;
		
		final int numTimeStep = 10;

		final int numSim = 1;

		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		RandomWalkVsDefender rwDefenderRandomized =
			new RandomWalkVsDefender(logisParam, discFact, thres, qrParamDef, numRWSampleDef, 1.0);
		RandomWalkVsDefender rwDefenderStatic =
			new RandomWalkVsDefender(logisParam, discFact, thres, qrParamDef, numRWSampleDef, 0.0);
		
		Attacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate
				, minNumSelectCandidate, numSelectCandidateRatio
				, qrParam, discFact);
		
		long start = System.currentTimeMillis();
		GameSimulation gameSimRWvsRW =
			new GameSimulation(depGraph, rwAttacker, rwDefenderRandomized, rnd, numTimeStep, discFact);
		double defPayoffRWvsRW = 0.0;
		double attPayoffRWvsRW = 0.0;
		double timeRWvsRW = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			rnd.reSeed(System.currentTimeMillis());
			gameSimRWvsRW.runSimulation();
			gameSimRWvsRW.printPayoff();
			defPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getDefPayoff();
			attPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getAttPayoff();
			gameSimRWvsRW.reset();
		}
		final double thousand = 1000.0;
		long end = System.currentTimeMillis();
		defPayoffRWvsRW /= numSim;
		attPayoffRWvsRW /= numSim;
		timeRWvsRW = (end - start) / thousand / numSim;
		
		
		
		start = System.currentTimeMillis();
		GameSimulation gameSimRWvsGO =
			new GameSimulation(depGraph, rwAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsGO = 0.0;
		double attPayoffRWvsGO = 0.0;
		double timeRWvsGO = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			rnd.reSeed(System.currentTimeMillis());
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
		
		start = System.currentTimeMillis();
		GameSimulation gameSimVPvsRW =
			new GameSimulation(depGraph, vpAttacker, rwDefenderRandomized, rnd, numTimeStep, discFact);
		double defPayoffVPvsRW = 0.0;
		double attPayoffVPvsRW = 0.0;
		final double initialTimeVPvsRW = 0.9;
		double timeVPvsRW = initialTimeVPvsRW;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			rnd.reSeed(System.currentTimeMillis());
			gameSimVPvsRW.runSimulation();
			gameSimVPvsRW.printPayoff();
			defPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getDefPayoff();
			attPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getAttPayoff();
			gameSimVPvsRW.reset();
		}
		end = System.currentTimeMillis();
		defPayoffVPvsRW /= numSim;
		attPayoffVPvsRW /= numSim;
		timeVPvsRW = (end - start) / thousand / numSim;
		
		start = System.currentTimeMillis();
		GameSimulation gameSimRWvsRWAlt =
			new GameSimulation(depGraph, rwAttacker, rwDefenderStatic, rnd, numTimeStep, discFact);
		double defPayoffRWvsRWAlt = 0.0;
		double attPayoffRWvsRWAlt = 0.0;
		double timeRWvsRWAlt = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			rnd.reSeed(System.currentTimeMillis());
			gameSimRWvsRWAlt.runSimulation();
			gameSimRWvsRWAlt.printPayoff();
			defPayoffRWvsRWAlt += gameSimRWvsRWAlt.getSimulationResult().getDefPayoff();
			attPayoffRWvsRWAlt += gameSimRWvsRWAlt.getSimulationResult().getAttPayoff();
			gameSimRWvsRWAlt.reset();
		}
		end = System.currentTimeMillis();
		defPayoffRWvsRWAlt /= numSim;
		attPayoffRWvsRWAlt /= numSim;
		timeRWvsRWAlt = (end - start) / thousand / numSim;
		
		System.out.println("Final result: ");
		System.out.println("Defender random walk payoff: " + defPayoffRWvsRW);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsRW);
		System.out.println("Runtime per simulation: " + timeRWvsRW);
		System.out.println();
		
		System.out.println("Defender random walk payoff alternative: " + defPayoffRWvsRWAlt);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsRWAlt);
		System.out.println("Runtime per simulation: " + timeRWvsRWAlt);
		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffRWvsGO);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsGO);
		System.out.println("Runtime per simulation: " + timeRWvsGO);
		System.out.println();
		
		System.out.println("Defender random walk payoff: " + defPayoffVPvsRW);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsRW);
		System.out.println("Runtime per simulation: " + timeVPvsRW);
		System.out.println();
	}
}
