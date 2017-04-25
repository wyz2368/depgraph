package main;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import agent.AgentFactory;
import agent.GoalOnlyDefender;
import agent.UniformAttacker;
import agent.Attacker.AttackerType;
import agent.Defender.DefenderType;
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
import utils.JsonUtils.ObservationStruct;

@SuppressWarnings("static-method")
public final class UnitTestMainGameSimulation {

	public UnitTestMainGameSimulation() {
		// default constructor
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
		final int numNode = 30;
		final int numEdge = 100;
		final int numTarget = 2;
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

	@Test(expected = IllegalArgumentException.class)
	public void testExtraAttParam() {
		String simspecFolderName = "testDirs/simSpecExtraAttParam";  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testMissingAttParam() {
		String simspecFolderName = "testDirs/simSpecMissingAttParam";  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWrongAttParam() {
		String simspecFolderName = "testDirs/simSpecWrongAttParam";  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testWrongAttType() {
		String simspecFolderName = "testDirs/simSpecWrongAttType";  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
	}
	
	@Test(expected = IllegalStateException.class)
	public void testExtraConfigParam() {
		String simspecFolderName = "testDirs/simSpecExtraConfigParam";  
		JsonUtils.getSimSpecOrDefaults(simspecFolderName);
	}
	
	@Test
	public void testMissingConfigParam() {
		String simspecFolderName = "testDirs/simSpecMissingConfigParam";
		// should get defaults for missing value
		final GameSimulationSpec simSpec = JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		
		// read in the default file
		final String defaultJsonString = JsonUtils.linesAsString(JsonUtils.DEFAULT_FILE_NAME);
		final JsonObject defaultAsJson = 
			new JsonParser().parse(defaultJsonString).getAsJsonObject();
		// get JsonObject of default file's appropriate field
		final JsonObject defaultConfig =
			(JsonObject) defaultAsJson.get(JsonUtils.SIMSPEC_FIELD_NAME);
		final String numSim = "numSim";
		assertTrue(defaultConfig.get(numSim).getAsInt() == simSpec.getNumSim());
	}
	
	@Test(expected = IllegalStateException.class)
	public void testWrongConfigParam() {
		String simspecFolderName = "testDirs/simSpecWrongConfigParam";  
		JsonUtils.getSimSpecOrDefaults(simspecFolderName);
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
	public void testObservationWrite() {
		final String simspecFolderName = "testDirs/simSpec0";
		final String graphFolderName = "testDirs/graphs0";
		
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
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		final String obsStringToWrite = JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);
		final String obsFileName = JsonUtils.printObservationToFile(simspecFolderName, obsStringToWrite);
		assertTrue(obsFileName != null);
		
		final ObservationStruct obsFromFile = JsonUtils.fromObservationFile(obsFileName);
		final GameSimulationSpec obsSimSpec = obsFromFile.getSimSpec();
		final int numTimeStep = 11;
		assertTrue(obsSimSpec.getNumTimeStep() == numTimeStep);
		final int numSim = 7;
		assertTrue(obsSimSpec.getNumSim() == numSim);
		final int graphId = 0;
		assertTrue(obsSimSpec.getGraphID() == graphId);
		final int numNode = 5;
		assertTrue(obsSimSpec.getNumNode() == numNode);
		final int numEdge = 3;
		assertTrue(obsSimSpec.getNumEdge() == numEdge);
		final int numTarget = 2;
		assertTrue(obsSimSpec.getNumTarget() == numTarget);
		final double discFact = 0.4;
		final double tolerance = 0.01;
		assertTrue(Math.abs(obsSimSpec.getDiscFact() - discFact) < tolerance);
		final UniformAttacker obsAttacker = (UniformAttacker) obsFromFile.getAttacker();
		assertTrue(obsAttacker.getAType() == AttackerType.UNIFORM);
		final double maxNumSelectCandidate = 4.0;
		assertTrue(Math.abs(obsAttacker.getMaxNumSelectCandidate() - maxNumSelectCandidate) < tolerance);
		final GoalOnlyDefender obsDefender = (GoalOnlyDefender) obsFromFile.getDefender();
		assertTrue(obsDefender.getDType() == DefenderType.GOAL_ONLY);
		final double maxNumRes = 5.0;
		assertTrue(Math.abs(obsDefender.getMaxNumRes() - maxNumRes) < tolerance);
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
