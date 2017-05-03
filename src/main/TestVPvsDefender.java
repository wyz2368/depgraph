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
import agent.RandomWalkVsDefender;
//import agent.GoalOnlyDefender;
import agent.ValuePropagationAttacker;
import agent.ValuePropagationVsDefender;

public final class TestVPvsDefender {
	
	private TestVPvsDefender() {
		// private constructor
	}

	public static void main(final String[] args) {
		// final int numNode = 50;
		// final int numEdge = 150;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.5;
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
		
		final int numLayer = 10;
		final int numNode1Layer = 25;
		final double numNodeRatio = 0.8;
		final double numEdgeRatio = 0.5;
		
		final double aNodeCostFactor = 1.5;
		final double aEdgeCostFactor = 1.5;
		final double dCostFactor = 1.5;
		
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
			aNodeCostFactor, aEdgeCostFactor, dCostFactor);
	
		DGraphGenerator.findMinCut(depGraph);
		
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.7;
		
		final double qrParam = 5.0;
		final double discFact = 0.9;
		
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.3;
		final double logisParam = 5.0;
		final double thres = 1e-3;
		
		final double numRWSample = 100;
		
		final int numTimeStep = 15;
		final int numSim = 1;
		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact, 0.0);
		
		Defender valuePropagationvsDefender = new ValuePropagationVsDefender(
			maxNumRes, minNumRes, numResRatio,
			logisParam, discFact, thres,
			qrParam, maxNumSelectCandidate, minNumSelectCandidate,
			numSelectCandidateRatio, 0.0, 0.0);
		
		Defender randomWalkvsDefender =
			new RandomWalkVsDefender(logisParam, discFact, thres, qrParam, numRWSample, 1.0);
		
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate
			, numSelectCandidateRatio, qrParam, discFact, 0.0);
				
		long start = System.currentTimeMillis();
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
		long end = System.currentTimeMillis();
		defPayoffVPvsGO /= numSim;
		attPayoffVPvsGO /= numSim;
		final double thousand = 1000.0;
		timeVPvsGO = (end - start) / thousand / numSim;
		
		start = System.currentTimeMillis();
		GameSimulation gameSimVPvsVP =
			new GameSimulation(depGraph, vpAttacker, valuePropagationvsDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsVP = 0.0;
		double attPayoffVPvsVP = 0.0;
		double timeVPvsVP = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
			gameSimVPvsVP.runSimulation();
			gameSimVPvsVP.printPayoff();
			defPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getDefPayoff();
			attPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getAttPayoff();
			gameSimVPvsVP.reset();
		}
		end = System.currentTimeMillis();
		defPayoffVPvsVP /= numSim;
		attPayoffVPvsVP /= numSim;
		timeVPvsVP = (end - start) / thousand / numSim;
		
		GameSimulation gameSimVPvsRW =
			new GameSimulation(depGraph, vpAttacker, randomWalkvsDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsRW = 0.0;
		double attPayoffVPvsRW = 0.0;
		double timeVPvsRW = 0.0;
		for (int i = 0; i < numSim; i++) {
			System.out.println("Simulation " + i);
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
		
		System.out.println("Defender value propagation payoff: " + defPayoffVPvsVP);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsVP);
		System.out.println("Runtime per simulation: " + timeVPvsVP);
		System.out.println();
		
		System.out.println("Defender random walk payoff: " + defPayoffVPvsRW);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsRW);
		System.out.println("Runtime per simulation: " + timeVPvsRW);
		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffVPvsGO);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsGO);
		System.out.println("Runtime per simulation: " + timeVPvsGO);
		System.out.println();
	}
}
