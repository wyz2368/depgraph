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
import agent.RandomWalkVsDefender;
import agent.RootOnlyDefender;
import agent.UniformAttacker;
import agent.UniformDefender;
import agent.UniformVsDefender;
import agent.ValuePropagationAttacker;
import agent.ValuePropagationVsDefender;
import agent.Attacker.AttackerType;
import agent.Defender.DefenderType;

public final class TestGameSimulation {

	private TestGameSimulation() {
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
		final double thres = 0.01;
		final int maxNumRes = 10;
		final int minNumRes = 2;
		final double numResRatio = 0.7;
		
		final double qrParamDef = 5.0;
		final int numRWSampleDef = 50;
		final int maxNumSelectCandidateDef = 10;
		final int minNumSelectCandidateDef = 2;
		final double numSelectCandidateRatioDef = 0.7;
		
		final double qrParam = 5.0;
		final double discFact = 0.9;
		final int numRWSample = 50;
		final int maxNumSelectCandidate = 10;
		final int minNumSelectCandidate = 2;
		final double numSelectCandidateRatio = 0.7;
		
		final int numTimeStep = 10;
		final int numSim = 10;
		
		AttackerType[] aTypeList = AttackerType.values();
		Attacker[] attList = new Attacker[aTypeList.length];
		for (int i = 0; i < aTypeList.length; i++) {
			switch (aTypeList[i]) {
			case UNIFORM:
				attList[i] =
					new UniformAttacker(maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio, 0.0);
				break;
			case VALUE_PROPAGATION:
				attList[i] = new ValuePropagationAttacker(
					maxNumSelectCandidate, minNumSelectCandidate, numSelectCandidateRatio
					, qrParam, discFact);
				break;
			case RANDOM_WALK:
				attList[i] = new RandomWalkAttacker(numRWSample, qrParam, discFact);
				break;
			default:
				System.out.println("Attacker type does not exist");
				break;
			}
		}
		DefenderType[] dTypeList = DefenderType.values();
		Defender[] defList = new Defender[dTypeList.length];
		for (int i = 0; i < dTypeList.length; i++) {
			switch (dTypeList[i]) {
			case UNIFORM:
				defList[i] = new UniformDefender(maxNumRes, minNumRes, numResRatio);
				break;
			case MINCUT:
				defList[i] = new MinCutDefender(maxNumRes, minNumRes, numResRatio);
				break;
			case GOAL_ONLY:
				defList[i] = new GoalOnlyDefender(maxNumRes, minNumRes, numResRatio, logisParam, discFact);
				break;
			case ROOT_ONLY:
				defList[i] = new RootOnlyDefender(maxNumRes, minNumRes, numResRatio);
				break;
			case vsVALUE_PROPAGATION:
				defList[i] = new ValuePropagationVsDefender(maxNumRes, minNumRes, numResRatio
					, logisParam, discFact, thres
					, qrParamDef
					, maxNumSelectCandidateDef, minNumSelectCandidateDef, numSelectCandidateRatioDef);
				break;
			case vsRANDOM_WALK:
				defList[i] = new RandomWalkVsDefender(logisParam, discFact, thres, qrParamDef, numRWSampleDef, 1.0);
				break;
			case vsUNIFORM:
				defList[i] = new UniformVsDefender(logisParam, discFact, thres, maxNumRes, minNumRes, numResRatio
					, maxNumSelectCandidateDef, minNumSelectCandidateDef, numSelectCandidateRatioDef);
				break;
			default:
				System.out.println("Defender type does not exist");
				break;
			}
		}
		
		GameSimulation[][] gameSimList = new GameSimulation[defList.length][attList.length];
		double[][] defPayoffList = new double[defList.length][attList.length];
		double[][] attPayoffList = new double[defList.length][attList.length];
		double[][] runtimeList = new double[defList.length][attList.length];
		for (int i = 0; i < defList.length; i++) {
			Defender defender = defList[i];
			for (int j = 0; j < attList.length; j++) {
				Attacker attacker = attList[j];
				gameSimList[i][j] = new GameSimulation(depGraph, attacker, defender, rnd, numTimeStep, discFact);
				defPayoffList[i][j] = 0.0;
				attPayoffList[i][j] = 0.0;
				runtimeList[i][j] = 0.0;
			}
		}
		final double thousand = 1000.0;
		for (int i = 0; i < defList.length; i++) {
			for (int j = 0; j < attList.length; j++) {
				System.out.println("Attacker type: " + aTypeList[j].toString());
				System.out.println("Defender type: " + dTypeList[i].toString());
				for (int k = 0; k < numSim; k++) {
					long start = System.currentTimeMillis();
					gameSimList[i][j].runSimulation();
					long end = System.currentTimeMillis();
					gameSimList[i][j].printPayoff();
					defPayoffList[i][j] += gameSimList[i][j].getSimulationResult().getDefPayoff();
					attPayoffList[i][j] += gameSimList[i][j].getSimulationResult().getAttPayoff();
					runtimeList[i][j] += (end - start) / thousand;
					gameSimList[i][j].reset();
				}
				defPayoffList[i][j] /= numSim;
				attPayoffList[i][j] /= numSim;
				runtimeList[i][j] /= numSim;
			}
		}
		for (int i = 0; i < defList.length; i++) {
			for (int j = 0; j < attList.length; j++) {
				System.out.println("----------------------------------------------------");
				System.out.println("Attacker type: " + aTypeList[j].toString());
				System.out.println("Defender type: " + dTypeList[i].toString());
				System.out.println("Defender payoff: " + defPayoffList[i][j]);
				System.out.println("Attacker payoff: " + attPayoffList[i][j]);
				System.out.println("Simulation runtime: " + runtimeList[i][j]);
				System.out.println("----------------------------------------------------");
			}
		}
	}
}
