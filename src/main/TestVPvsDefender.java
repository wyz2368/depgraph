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
import agent.RandomWalkAttacker;
import agent.RandomWalkVsDefender;
//import agent.GoalOnlyDefender;
import agent.ValuePropagationAttacker;
import agent.ValuePropagationVsDefender;

public final class TestVPvsDefender {
	
	private TestVPvsDefender() {
		// private constructor
	}

	public static void main(final String[] args) {
//		final int numNode = 50;
//		final int numEdge = 150;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.0;
		final double aRewardLB = 10.0;
		final double aRewardUB = 20.0;
		final double dPenaltyLB = -20.0;
		final double dPenaltyUB = -10.0;
//		final double aRewardLB = 10.0;
//		final double aRewardUB = 20.0;
//		final double dPenaltyLB = -20.0;
//		final double dPenaltyUB = -10.0;
		
		final double aNodeCostLB = -1.0;
		final double aNodeCostUB = -0.5;
		final double aEdgeCostLB = -1.0;
		final double aEdgeCostUB = -0.5;
		final double dCostLB = -1.0;
		final double dCostUB = -0.5;
//		final double dCostLB = -1.0;
//		final double dCostUB = -0.5;
		final double aNodeActProbLB = 0.8;
		final double aNodeActProbUB = 1.0;
		final double aEdgeActProbLB = 0.6;
		final double aEdgeActProbUB = 0.8;
		final double minPosActiveProb = 0.8;
		final double maxPosActiveProb = 1.0;
		final double minPosInactiveProb = 0.0;
		final double maxPosInactiveProb = 0.2;
//		
		Node.resetCounter();
		Edge.resetCounter();
		RandomDataGenerator rnd = new RandomDataGenerator();
		rnd.reSeed(System.currentTimeMillis());

//		DependencyGraph depGraph = DagGenerator.genRandomDAG(numNode, numEdge, rnd);
//		DGraphGenerator.genGraph(depGraph, rnd
//			, numTarget, nodeActTypeRatio
//			, aRewardLB, aRewardUB
//			, dPenaltyLB, dPenaltyUB
//			, aNodeCostLB, aNodeCostUB
//			, aEdgeCostLB, aEdgeCostUB
//			, dCostLB, dCostUB
//			, aNodeActProbLB, aNodeActProbUB
//			, aEdgeActProbLB, aEdgeActProbUB
//			, minPosActiveProb, maxPosActiveProb
//			, minPosInactiveProb, maxPosInactiveProb);
		
		int numLayer = 5;
		int numNode1Layer = 25;
		double numNodeRatio = 0.8;
		double numEdgeRatio = 0.5;
		
		double aNodeCostFactor = 1.2;
		double aEdgeCostFactor = 1.2;
		double dCostFactor = 1.2;
		double aRewardFactor = 1.2;
		double dPenaltyFactor = 1.2;
		
		DependencyGraph depGraph =
          DagGenerator.genRandomSepLayDAG(numLayer, numNode1Layer, numNodeRatio, numEdgeRatio, rnd);
		DGraphGenerator.genSepLayGraph(depGraph, rnd, 
				numTarget, nodeActTypeRatio, 
				aRewardLB, aRewardUB, 
				dPenaltyLB, dPenaltyUB, 
				aNodeCostLB, aNodeCostUB, 
				aEdgeCostLB, aEdgeCostUB, 
				dCostLB, dCostUB, 
				aNodeActProbLB, aNodeActProbUB, 
				aEdgeActProbLB, aEdgeActProbUB, 
				minPosActiveProb, maxPosActiveProb, 
				minPosInactiveProb, maxPosInactiveProb, 
				aNodeCostFactor, aEdgeCostFactor, dCostFactor,
				aRewardFactor, dPenaltyFactor);
		
		
		DGraphGenerator.findMinCut(depGraph);
		
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.5;
		
		final double qrParam = 1.0;
		final double discFact = 0.9;
		
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.5;
		final double logisParam = 1.0;
		final double thres = 1e-2;
		
		final double numRWSample = 100;
		
		final int numTimeStep = 10;
		final int numSim = 1;
		
		Defender valuePropagationvsDefender = new ValuePropagationVsDefender(
			maxNumRes, minNumRes, numResRatio,
			logisParam, discFact, thres,
			qrParam, maxNumSelectCandidate, minNumSelectCandidate,
			numSelectCandidateRatio, 0.0, 0.0);
		
		Defender randomWalkvsDefender =
			new RandomWalkVsDefender(logisParam, discFact, thres, qrParam, numRWSample, 1.0);
		
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate
			, numSelectCandidateRatio, qrParam, discFact, 0.0);
		
		Attacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
				
		double thousand = 1000.0;
		
		long start = System.currentTimeMillis();
		GameSimulation gameSimVPvsVP =
			new GameSimulation(depGraph, vpAttacker, valuePropagationvsDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsVP = 0.0;
		double attPayoffVPvsVP = 0.0;
		
		double defPayoffStdVPvsVP = 0.0;
		double attPayoffStdVPvsVP = 0.0;
		double timeVPvsVP = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimVPvsVP.runSimulation();
			gameSimVPvsVP.printPayoff();
			defPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getDefPayoff();
			attPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getAttPayoff();
			
			defPayoffStdVPvsVP += Math.pow(gameSimVPvsVP.getSimulationResult().getDefPayoff(), 2);
			attPayoffStdVPvsVP += Math.pow(gameSimVPvsVP.getSimulationResult().getAttPayoff(), 2);
			gameSimVPvsVP.reset();
		}
		long end = System.currentTimeMillis();
		defPayoffVPvsVP /= numSim;
		attPayoffVPvsVP /= numSim;
		
		defPayoffStdVPvsVP = Math.pow(defPayoffStdVPvsVP / numSim - Math.pow(defPayoffVPvsVP, 2), 0.5);
		attPayoffStdVPvsVP = Math.pow(attPayoffStdVPvsVP / numSim - Math.pow(attPayoffVPvsVP, 2), 0.5);
		
		timeVPvsVP = (end - start) / thousand / numSim;
		
		GameSimulation gameSimVPvsRW =
				new GameSimulation(depGraph, vpAttacker, randomWalkvsDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsRW = 0.0;
		double attPayoffVPvsRW = 0.0;
		double defPayoffStdVPvsRW = 0.0;
		double attPayoffStdVPvsRW = 0.0;
		double timeVPvsRW = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimVPvsRW.runSimulation();
			gameSimVPvsRW.printPayoff();
			defPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getDefPayoff();
			attPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getAttPayoff();
			
			defPayoffStdVPvsRW += Math.pow(gameSimVPvsRW.getSimulationResult().getDefPayoff(), 2);
			attPayoffStdVPvsRW += Math.pow(gameSimVPvsRW.getSimulationResult().getAttPayoff(), 2);
			gameSimVPvsRW.reset();
		}
		end = System.currentTimeMillis();
		defPayoffVPvsRW /= numSim;
		attPayoffVPvsRW /= numSim;
		
		defPayoffStdVPvsRW = Math.pow(defPayoffStdVPvsRW / numSim - Math.pow(defPayoffVPvsRW, 2), 0.5);
		attPayoffStdVPvsRW = Math.pow(attPayoffStdVPvsRW / numSim - Math.pow(attPayoffVPvsRW, 2), 0.5);
		timeVPvsRW = (end - start) / thousand / numSim;
//		
		
		
		start = System.currentTimeMillis();
		GameSimulation gameSimRWvsVP =
			new GameSimulation(depGraph, rwAttacker, valuePropagationvsDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsVP = 0.0;
		double attPayoffRWvsVP = 0.0;
		double defPayoffStdRWvsVP = 0.0;
		double attPayoffStdRWvsVP = 0.0;
		double timeRWvsVP = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimRWvsVP.runSimulation();
			gameSimRWvsVP.printPayoff();
			defPayoffRWvsVP += gameSimRWvsVP.getSimulationResult().getDefPayoff();
			attPayoffRWvsVP += gameSimRWvsVP.getSimulationResult().getAttPayoff();
			
			defPayoffStdRWvsVP += Math.pow(gameSimRWvsVP.getSimulationResult().getDefPayoff(), 2);
			attPayoffStdRWvsVP += Math.pow(gameSimRWvsVP.getSimulationResult().getAttPayoff(), 2);
			gameSimRWvsVP.reset();
		}
		end = System.currentTimeMillis();
		defPayoffRWvsVP /= numSim;
		attPayoffRWvsVP /= numSim;
		defPayoffStdRWvsVP = Math.pow(defPayoffStdRWvsVP / numSim - Math.pow(defPayoffRWvsVP, 2), 0.5);
		attPayoffStdRWvsVP = Math.pow(attPayoffStdRWvsVP / numSim - Math.pow(attPayoffRWvsVP, 2), 0.5);
		
		timeRWvsVP = (end - start) / thousand / numSim;
		
		GameSimulation gameSimRWvsRW =
				new GameSimulation(depGraph, rwAttacker, randomWalkvsDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsRW = 0.0;
		double attPayoffRWvsRW = 0.0;
		double defPayoffStdRWvsRW = 0.0;
		double attPayoffStdRWvsRW = 0.0;
		double timeRWvsRW = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimRWvsRW.runSimulation();
			gameSimRWvsRW.printPayoff();
			defPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getDefPayoff();
			attPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getAttPayoff();
			
			defPayoffStdRWvsRW += Math.pow(gameSimRWvsRW.getSimulationResult().getDefPayoff(), 2);
			attPayoffStdRWvsRW += Math.pow(gameSimRWvsRW.getSimulationResult().getAttPayoff(), 2);
			gameSimRWvsRW.reset();
		}
		end = System.currentTimeMillis();
		defPayoffRWvsRW /= numSim;
		attPayoffRWvsRW /= numSim;
		
		defPayoffStdRWvsRW = Math.pow(defPayoffStdRWvsRW / numSim - Math.pow(defPayoffRWvsRW, 2), 0.5);
		attPayoffStdRWvsRW = Math.pow(attPayoffStdRWvsRW / numSim - Math.pow(attPayoffRWvsRW, 2), 0.5);
		timeRWvsRW = (end - start) / thousand / numSim;
	
		System.out.println("Defender value propagation payoff: " + defPayoffVPvsVP + "\t Std " + defPayoffStdVPvsVP);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsVP + "\t Std " + attPayoffStdVPvsVP);
		System.out.println("Runtime per simulation: " + timeVPvsVP);
		System.out.println();
		
		System.out.println("Defender random walk payoff: " + defPayoffVPvsRW + "\t Std " + defPayoffStdVPvsRW);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsRW + "\t Std " + attPayoffStdVPvsRW);
		System.out.println("Runtime per simulation: " + timeVPvsRW);
		System.out.println();
		
		System.out.println("Defender value propagation payoff: " + defPayoffRWvsVP + "\t Std " + defPayoffStdRWvsVP);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsVP + "\t Std " + attPayoffStdRWvsVP);
		System.out.println("Runtime per simulation: " + timeRWvsVP);
		System.out.println();
		
		System.out.println("Defender random walk payoff: " + defPayoffRWvsRW + "\t Std " + defPayoffStdRWvsRW);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsRW + "\t Std " + attPayoffStdRWvsRW);
		System.out.println("Runtime per simulation: " + timeRWvsRW);
		System.out.println();
		
	}
}
