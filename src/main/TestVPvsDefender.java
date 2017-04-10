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
//import agent.GoalOnlyDefender;
import agent.ValuePropagationAttacker;
import agent.ValuePropagationvsDefender;

public class TestVPvsDefender {

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
		
		int maxNumRes = 10;
		int minNumRes = 2;
		double numResRatio = 0.7;
		double logisParam = 5.0;
		double thres = 1e-2;
		
		int numTimeStep = 6;
		int numSim = 10;
//		Defender goalOnlyDefender = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
		Defender valuePropagationvsDefender = new ValuePropagationvsDefender(maxNumRes, minNumRes, numResRatio
				, logisParam, discFact, thres
				, qrParam, maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio);
		Attacker vpAttacker = new ValuePropagationAttacker(maxNumSelectCandidate, minNumSelectCandidate
				, numSelectCandidateRatio, qrParam, discFact);
		
//		
//		GameSimulation gameSimVPvsGO = new GameSimulation(depGraph, vpAttacker, goalOnlyDefender, rnd, numTimeStep, discFact);
//		double defPayoffVPvsGO = 0.0;
//		double attPayoffVPvsGO = 0.0;
//		for(int i = 0; i < numSim; i++)
//		{
//			System.out.println("Simulation " + i);
//			gameSimVPvsGO.runSimulation();
//			gameSimVPvsGO.printPayoff();
//			defPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getDefPayoff();
//			attPayoffVPvsGO += gameSimVPvsGO.getSimulationResult().getAttPayoff();
//			gameSimVPvsGO.reset();
//		}
//		defPayoffVPvsGO /= numSim;
//		attPayoffVPvsGO /= numSim;
		
		GameSimulation gameSimVPvsVP = new GameSimulation(depGraph, vpAttacker, valuePropagationvsDefender, rnd, numTimeStep, discFact);
		double defPayoffVPvsVP = 0.0;
		double attPayoffVPvsVP = 0.0;
		for(int i = 0; i < numSim; i++)
		{
			System.out.println("Simulation " + i);
			gameSimVPvsVP.runSimulation();
			gameSimVPvsVP.printPayoff();
			defPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getDefPayoff();
			attPayoffVPvsVP += gameSimVPvsVP.getSimulationResult().getAttPayoff();
			gameSimVPvsVP.reset();
		}
		defPayoffVPvsVP /= numSim;
		attPayoffVPvsVP /= numSim;
		
		System.out.println("Defender value propagation payoff: " + defPayoffVPvsVP);
		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsVP);
		System.out.println();
		
//		System.out.println("Defender goal node payoff: " + defPayoffVPvsGO);
//		System.out.println("Attacker value propagation payoff: " + attPayoffVPvsGO);
//		System.out.println();
	}

}
