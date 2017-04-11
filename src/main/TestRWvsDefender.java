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
import agent.RandomWalkvsDefender;
import agent.ValuePropagationAttacker;

public class TestRWvsDefender {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numNode = 50;
		int numEdge = 150;
		int numTarget = 10;
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
		
		double qrParam = 5.0;
		double discFact = 0.9;
		int numRWSample = 200;
		
		double logisParam = 5.0;
		double thres = 1e-2;
		
		int maxNumRes = 10;
		int minNumRes = 2;
		double numResRatio = 0.7;
		
		int maxNumSelectCandidate = 10;
		int minNumSelectCandidate = 2;
		double numSelectCandidateRatio = 0.7;
		
		int numTimeStep = 10;
		int numSim = 1;
		
		RandomWalkvsDefender rwDefender = new RandomWalkvsDefender(logisParam, discFact, thres, qrParam, numRWSample);
		Attacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate
				, minNumSelectCandidate, numSelectCandidateRatio
				, qrParam, discFact);
		GameSimulation gameSimRWvsRW = new GameSimulation(depGraph, rwAttacker, rwDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsRW = 0.0;
		double attPayoffRWvsRW = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimRWvsRW.runSimulation();
			gameSimRWvsRW.printPayoff();
			defPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getDefPayoff();
			attPayoffRWvsRW += gameSimRWvsRW.getSimulationResult().getAttPayoff();
			gameSimRWvsRW.reset();
//			System.out.println();
		}
		defPayoffRWvsRW /= numSim;
		attPayoffRWvsRW /= numSim;
		
		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		GameSimulation gameSimRWvsGO = new GameSimulation(depGraph, rwAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffRWvsGO = 0.0;
		double attPayoffRWvsGO = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimRWvsGO.runSimulation();
			gameSimRWvsGO.printPayoff();
			defPayoffRWvsGO += gameSimRWvsGO.getSimulationResult().getDefPayoff();
			attPayoffRWvsGO += gameSimRWvsGO.getSimulationResult().getAttPayoff();
			gameSimRWvsGO.reset();
//			System.out.println();
		}
		defPayoffRWvsGO /= numSim;
		attPayoffRWvsGO /= numSim;
		
		
		GameSimulation gameSimVPvsRW = new GameSimulation(depGraph, vpAttacker, rwDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsRW = 0.0;
		double attPayoffVPvsRW = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimVPvsRW.runSimulation();
			gameSimVPvsRW.printPayoff();
			defPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getDefPayoff();
			attPayoffVPvsRW += gameSimVPvsRW.getSimulationResult().getAttPayoff();
			gameSimVPvsRW.reset();
//			System.out.println();
		}
		defPayoffVPvsRW /= numSim;
		attPayoffVPvsRW /= numSim;
		
		System.out.println("Final result: ");
		System.out.println("Defender random walk payoff: " + defPayoffRWvsRW);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsRW);
		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffRWvsGO);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsGO);
		System.out.println();
		
		System.out.println("Defender random walk payoff: " + defPayoffVPvsRW);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsRW);
		System.out.println();

	}

}
