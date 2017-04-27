package agent;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import game.GameSimulationSpec;
import game.MeanGameSimulationResult;
import graph.INode.NodeType;
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
public final class UnitTestGoalOnlyDefender {

	public UnitTestGoalOnlyDefender() {
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
	public void canRunTest() {
		final String simspecFolderName = "testDirs/simSpecGoalDefPosStdev";
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
		
		final GoalOnlyDefender testDefender =
			(GoalOnlyDefender) AgentFactory.createDefender(defenderName, defenderParams, simSpec.getDiscFact());
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
		final int numTimeSteps = 11;
		final int iters = 100;
		final int stratMin = 1;
		final int stratMax = 7;
		final Set<Integer> nodeCounts = new HashSet<Integer>();
		final Set<Node> allDefendedNodes = new HashSet<Node>();
		for (int i = 0; i < iters; i++) {
			final DefenderAction defAction =
				testDefender.sampleAction(depGraph, 1, numTimeSteps, belief, rnd.getRandomGenerator());
			final int nodeCount = defAction.getAction().size();
			nodeCounts.add(nodeCount);
			assertTrue(nodeCount >= stratMin);
			assertTrue(nodeCount <= stratMax);
			
			allDefendedNodes.addAll(defAction.getAction());
		}
		assertTrue(nodeCounts.size() > 1);
		for (final Node node: depGraph.vertexSet()) {
			if (node.getType() == NodeType.TARGET) {
				assertTrue(allDefendedNodes.contains(node));
			} else {
				assertTrue(!allDefendedNodes.contains(node));
			}
		}
	}
}
