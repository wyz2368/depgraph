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
import agent.MinCutDefender;
import agent.RandomWalkAttacker;
import agent.UniformAttacker;
import agent.UniformDefender;
import agent.ValuePropagationAttacker;
import agent.ValuePropagationvsDefender;

public class TestGameSimulation {

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
		
		int maxNumSelectCandidate = 10;
		int minNumSelectCandidate = 2;
		double numSelectCandidateRatio = 0.7;
		
		double qrParam = 5.0;
		double discFact = 0.9;
		int numRWSample = 300;
		
		int maxNumRes = 10;
		int minNumRes = 2;
		double numResRatio = 0.7;
		double logisParam = 5.0;
		double thres = 1e-2;
		
		int numTimeStep = 6;
		int numSim = 1000;
		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		Defender valuePropagationvsDefender = new ValuePropagationvsDefender(maxNumRes, minNumRes, numResRatio
				, logisParam, discFact, thres
				, qrParam, maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio);
//		Defender uniformDefender = new UniformDefender(maxNumRes, minNumRes, numResRatio);
//		Defender mincutDefender = new MinCutDefender(maxNumRes, minNumRes, numResRatio);
		
		Attacker rwAttacker = new RandomWalkAttacker(numRWSample, qrParam, discFact);
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
		
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate
				, numSelectCandidateRatio, qrParam, discFact);
		GameSimulation gameSimVPvsGO = new GameSimulation(depGraph, vpAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsGO = 0.0;
		double attPayoffVPvsGO = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimVPvsGO.runSimulation();
			gameSimVPvsGO.printPayoff();
			defPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getDefPayoff();
			attPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getAttPayoff();
			gameSimVPvsGO.reset();
//			System.out.println();
		}
		defPayoffVPvsGO /= numSim;
		attPayoffVPvsGO /= numSim;
		
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
//			System.out.println();
		}
		defPayoffUvsGO /= numSim;
		attPayoffUvsGO /= numSim;
		
//		GameSimulation gameSimRWvsU = new GameSimulation(depGraph, rwAttacker, uniformDefender, rnd, numTimeStep, discFact);
//		double defPayoffRWvsU = 0.0;
//		double attPayoffRWvsU = 0.0;
//		for(int i = 0; i < numSim; i++)
//		{
//			System.out.println("Simulation " + i);
//			gameSimRWvsU.runSimulation();
//			gameSimRWvsU.printPayoff();
//			defPayoffRWvsU += gameSimRWvsU.getSimulationResult().getDefPayoff();
//			attPayoffRWvsU += gameSimRWvsU.getSimulationResult().getAttPayoff();
//			gameSimRWvsU.reset();
////			System.out.println();
//		}
//		defPayoffRWvsU /= numSim;
//		attPayoffRWvsU /= numSim;
//		
//		GameSimulation gameSimVPvsU = new GameSimulation(depGraph, vpAttacker, uniformDefender, rnd, numTimeStep, discFact);
//		double defPayoffVPvsU = 0.0;
//		double attPayoffVPvsU = 0.0;
//		for(int i = 0; i < numSim; i++)
//		{
//			System.out.println("Simulation " + i);
//			gameSimVPvsU.runSimulation();
//			gameSimVPvsU.printPayoff();
//			defPayoffVPvsU += gameSimVPvsU.getSimulationResult().getDefPayoff();
//			attPayoffVPvsU += gameSimVPvsU.getSimulationResult().getAttPayoff();
//			gameSimVPvsU.reset();
////			System.out.println();
//		}
//		defPayoffVPvsU /= numSim;
//		attPayoffVPvsU /= numSim;
//		
//
//		GameSimulation gameSimUvsU = new GameSimulation(depGraph, uniformAttacker, uniformDefender, rnd, numTimeStep, discFact);
//		double defPayoffUvsU = 0.0;
//		double attPayoffUvsU = 0.0;
//		for(int i = 0; i < numSim; i++)
//		{
//			System.out.println("Simulation " + i);
//			gameSimUvsU.runSimulation();
//			gameSimUvsU.printPayoff();
//			defPayoffUvsU += gameSimUvsU.getSimulationResult().getDefPayoff();
//			attPayoffUvsU += gameSimUvsU.getSimulationResult().getAttPayoff();
//			gameSimUvsU.reset();
////			System.out.println();
//		}
//		defPayoffUvsU /= numSim;
//		attPayoffUvsU /= numSim;
		
//		GameSimulation gameSimVPvsVP = new GameSimulation(depGraph, vpAttacker, valuePropagationvsDefender, rnd, numTimeStep, discFact);
//		double defPayoffVPvsVP = 0.0;
//		double attPayoffVPvsVP = 0.0;
//		for(int i = 0; i < numSim; i++)
//		{
//			System.out.println("Simulation " + i);
//			gameSimVPvsVP.runSimulation();
//			gameSimVPvsVP.printPayoff();
//			defPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getDefPayoff();
//			attPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getAttPayoff();
//			gameSimVPvsVP.reset();
////			System.out.println();
//		}
//		defPayoffVPvsVP /= numSim;
//		attPayoffVPvsVP /= numSim;
		
		System.out.println("Final result: ");
		System.out.println("Defender goal node payoff: " + defPayoffRWvsGO);
		System.out.println("Attacker random walk payoff: " + attPayoffRWvsGO);
		System.out.println();
		
//		System.out.println("Defender value propagation payoff: " + defPayoffVPvsVP);
//		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsVP);
//		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffVPvsGO);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsGO);
		System.out.println();
		
		System.out.println("Defender goal node payoff: " + defPayoffUvsGO);
		System.out.println("Attacker uniform payoff: " + attPayoffUvsGO);
		System.out.println();
//		
		
//		System.out.println("Defender uniform payoff: " + defPayoffRWvsU);
//		System.out.println("Attacker random walk payoff: " + attPayoffRWvsU);
//		System.out.println();
//		System.out.println("Defender uniform payoff: " + defPayoffVPvsU);
//		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsU);
//		System.out.println();
//		System.out.println("Defender uniform payoff: " + defPayoffUvsU);
//		System.out.println("Attacker uniform payoff: " + attPayoffUvsU);
//		System.out.println();
		
	}

}
