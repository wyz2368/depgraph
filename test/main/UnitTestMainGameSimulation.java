package main;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import agent.AgentFactory;
import agent.GoalOnlyDefender;
import agent.UniformAttacker;
import game.GameSimulation;
import game.GameSimulationSpec;
import game.MeanGameSimulationResult;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Edge.EdgeType;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.INode.NodeType;
import graph.Node;
import model.DependencyGraph;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

@SuppressWarnings("static-method")
public final class UnitTestMainGameSimulation {

	public UnitTestMainGameSimulation() {
		// default constuctor
	}
	
	@Test
	@SuppressWarnings("all")
	public void assertionTest() {
		boolean assertionsOn = false;
		// assigns true if assertions are on.
		assert assertionsOn = true;
		assertTrue(assertionsOn);
	}

	@Test
	public void basicTest() {		
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
	 
		final int numSample = 5;
		for (int idx = 0; idx < numSample; idx++) {
			Node.resetCounter();
			Edge.resetCounter();
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
		}
	}
	
	@Test
	public void testFromSimSpec() {
		final String simspecFolderName = "simspecs";
		final String graphFolderName = "graphs";
  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode() + "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
		GameSimulation.printIfDebug(filePathName);

		// Load players
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);
	}
	
	@Test
	public void testReadSimSpec() {
		final String simspecFolderName = "testDirs/simSpec0";
		final String graphFolderName = "testDirs/graphs0";
  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode() + "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		// DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
		GameSimulation.printIfDebug(filePathName);

		// Load players
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		
		final UniformAttacker testAttacker =
			(UniformAttacker) AgentFactory.createAttacker("UNIFORM", attackerParams, simSpec.getDiscFact());
		final GoalOnlyDefender testDefender =
			(GoalOnlyDefender) AgentFactory.createDefender("GOAL_ONLY", defenderParams, simSpec.getDiscFact());
		
		final double tolerance = 0.01;
		// UNIFORM:maxNumSelectCandidate_4.0_minNumSelectCandidate_3.0_numSelectCandidateRatio_0.6_qrParam_2.0
		final double maxNumSelectCand = 4.0;
		final double minNumSelectCand = 3.0;
		final double numSelectCandidateRatio = 0.6;
		assertTrue(Math.abs(testAttacker.getMaxNumSelectCandidate() - maxNumSelectCand) < tolerance);
		assertTrue(Math.abs(testAttacker.getMinNumSelectCandidate() - minNumSelectCand) < tolerance);
		assertTrue(Math.abs(testAttacker.getNumSelectCandidateRatio() - numSelectCandidateRatio) < tolerance);
		
		// GOAL_ONLY:maxNumRes_5.0_minNumRes_2.0_numResRatio_0.8_logisParam_6.0
		final double maxNumRes = 5.0;
		final double minNumRes = 2.0;
		final double numResRatio = 0.8;
		final double logisParam = 6.0;
		final double discFact = 0.4;
		assertTrue(Math.abs(testDefender.getMaxNumRes() - maxNumRes) < tolerance);
		assertTrue(Math.abs(testDefender.getMinNumRes() - minNumRes) < tolerance);
		assertTrue(Math.abs(testDefender.getNumResRatio() - numResRatio) < tolerance);
		assertTrue(Math.abs(testDefender.getLogisParam() - logisParam) < tolerance);
		assertTrue(Math.abs(testDefender.getDiscFact() - discFact) < tolerance);
		
		final DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);

		final int numSim = 7;
		assertTrue(simSpec.getNumSim() == numSim);
		assertTrue(simResult.getNumSims() == numSim);
		final int numTimeStep = 11;
		assertTrue(simSpec.getNumTimeStep() == numTimeStep);
		assertTrue(simResult.getNumTimeStep() == numTimeStep);
		final int graphId = 0;
		assertTrue(simSpec.getGraphID() == graphId);
		final int numNode = 5;
		assertTrue(simSpec.getNumNode() == numNode);
		final int numEdge = 3;
		assertTrue(simSpec.getNumEdge() == numEdge);
		final int numTarget = 2;
		assertTrue(simSpec.getNumTarget() == numTarget);
		assertTrue(simSpec.getDiscFact() == discFact);
		
		final int nodeId = 1;
		final Node node = depGraph.getNodeById(nodeId);
		assertTrue(node.getId() == nodeId);
		assertTrue(node.getTopoPosition() == 0);
		assertTrue(node.getType() == NodeType.NONTARGET);
		assertTrue(depGraph.getRootSet().contains(node));
		assertTrue(node.getActivationType() == NodeActivationType.AND);
		assertTrue(node.getState() == NodeState.INACTIVE);
		assertTrue(Math.abs(node.getAReward()) < tolerance);
		assertTrue(Math.abs(node.getDPenalty()) < tolerance);
		final double dCost = -1.06;
		assertTrue(Math.abs(node.getDCost() - dCost) < tolerance);
		final double posActiveProb = 0.86;
		assertTrue(Math.abs(node.getPosActiveProb() - posActiveProb) < tolerance);
		final double posInactiveProb = 0.13;
		assertTrue(Math.abs(node.getPosInactiveProb() - posInactiveProb) < tolerance);
		final double aActivationCost = -0.58;
		assertTrue(Math.abs(node.getACost() - aActivationCost) < tolerance);
		final double aActivationProb = 0.95;
		assertTrue(Math.abs(node.getActProb() - aActivationProb) < tolerance);
		
		final int targetId = 4;
		final Node target = depGraph.getNodeById(targetId);
		assertTrue(target.getType() == NodeType.TARGET);
		assertTrue(!depGraph.getRootSet().contains(target));
		final double targetAReward = 4.0;
		assertTrue(Math.abs(target.getAReward() - targetAReward) < tolerance);
		final double targetDPenalty = -3.0;
		assertTrue(Math.abs(target.getDPenalty() - targetDPenalty) < tolerance);
		
		final int edgeId = 1;
		final Edge edge = depGraph.getEdgeById(edgeId);
		assertTrue(edge.getId() == edgeId);
		final int srcId = 2;
		assertTrue(edge.getsource().getId() == srcId);
		final int desId = 4;
		assertTrue(edge.gettarget().getId() == desId);
		assertTrue(edge.getType() == EdgeType.NORMAL);
		final double edgeActivationCost = 0.5;
		assertTrue(Math.abs(edge.getACost() - edgeActivationCost) < tolerance);
		final double edgeActivationProb = 0.4;
		assertTrue(Math.abs(edge.getActProb() - edgeActivationProb) < tolerance);
	}
}
