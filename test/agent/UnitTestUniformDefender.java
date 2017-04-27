package agent;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import game.GameSimulationSpec;
import game.MeanGameSimulationResult;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import main.MainGameSimulation;
import model.DefenderAction;
import model.DefenderBelief;
import model.DependencyGraph;
import model.GameState;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

@SuppressWarnings("static-method")
public final class UnitTestUniformDefender {

	public UnitTestUniformDefender() {
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
	public void testDifferentAttackCounts() {
		final String simspecFolderName = "testDirs/simSpecUniformDefPosStdev";
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);

		final int numNode = 10;
		final int numEdge = 0;
		final int numTarget = 10;
		final double nodeActTypeRatio = 0.5;
		
		final double aRewardLB = 2.0;
		final double aRewardUB = 2.0;
		final double aNodeCostLB = -0.3;
		final double aNodeCostUB = -0.3;
		final double aEdgeCostLB = 0.0;
		final double aEdgeCostUB = 0.0;

		final double dPenaltyLB = -5.0;
		final double dPenaltyUB = -5.0;

		final double dCostLB = -0.4;
		final double dCostUB = -0.4;
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

		final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		final UniformDefender testDefender =
			(UniformDefender) AgentFactory.createDefender(defenderName, defenderParams, simSpec.getDiscFact());
		
		final int numTimeSteps = 11;
		final int iters = 100;
		final Set<Integer> defendCounts = new HashSet<Integer>();
		final int minSize = 5;
		final int maxSize = 12;
		final DefenderBelief belief = new DefenderBelief();
		for (int i = 0; i < iters; i++) {
			final DefenderAction defAction =
				testDefender.sampleAction(depGraph, 1, numTimeSteps, belief, rnd.getRandomGenerator());
			final int defendCount = defAction.getAction().size();
			defendCounts.add(defendCount);
			assertTrue(defendCount >= minSize);
			assertTrue(defendCount <= maxSize);
		}
		assertTrue(defendCounts.size() > 1);
	}
	
	@Test
	public void canRunTest() {
		final String simspecFolderName = "testDirs/simSpecUniformDef";
		final String graphFolderName = "testDirs/graphs0";
  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode() + "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		final DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);

		// Load players
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		
		final UniformDefender testDefender =
			(UniformDefender) AgentFactory.createDefender(defenderName, defenderParams, simSpec.getDiscFact());
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);
		
		RandomDataGenerator rnd = new RandomDataGenerator();
		DefenderBelief belief = new DefenderBelief();
		GameState gameState = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			gameState.addEnabledNode(node);
		}
		depGraph.setState(gameState);
		final int numTimeSteps = 10;
		final int iters = 1000;
		final Map<Node, Integer> nodeCounts = new HashMap<Node, Integer>();
		for (int i = 0; i < iters; i++) {
			final DefenderAction defAction =
				testDefender.sampleAction(depGraph, 1, numTimeSteps, belief, rnd.getRandomGenerator());
			for (final Node node: defAction.getAction()) {
				if (!nodeCounts.containsKey(node)) {
					nodeCounts.put(node, 0);
				}
				nodeCounts.put(node, nodeCounts.get(node) + 1);
			}
		}
		for (final Node node: depGraph.vertexSet()) {
			assertTrue(nodeCounts.get(node) > 0);
		}
	}
}
