package agent;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import game.GameSimulationSpec;
import game.MeanGameSimulationResult;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import main.MainGameSimulation;
import model.DependencyGraph;
import utils.EncodingUtils;
import utils.JsonUtils;

@SuppressWarnings("static-method")
public final class UnitTestNoopAttacker {

	public UnitTestNoopAttacker() {
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
		final String simspecFolderName = "testDirs/simSpecNoopAttacker";
  
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

		// Load players
		final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
		final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
		final Map<String, Double> attackerParams =
			EncodingUtils.getStrategyParams(attackerString);
		final Map<String, Double> defenderParams =
			EncodingUtils.getStrategyParams(defenderString);
		final String attackerName = EncodingUtils.getStrategyName(attackerString);
		final String defenderName = EncodingUtils.getStrategyName(defenderString);
		
		@SuppressWarnings("unused")
		final NoopAttacker testAttacker =
			(NoopAttacker) AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);
	}
}
