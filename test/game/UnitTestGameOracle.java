package game;

import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Test;

import agent.Attacker;
import agent.Defender;
import agent.UniformAttacker;
import agent.UniformDefender;
import graph.DGraphGenerator;
import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;
import model.SecurityAlert;
import utils.DGraphUtils;

@SuppressWarnings("static-method")
public final class UnitTestGameOracle {

	public UnitTestGameOracle() {
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
	public void testAllAlertProbs() {
		final double threeQuarters = 0.75;
		final double quarter = 0.25;
		testAlertProbs(threeQuarters, quarter);
		testAlertProbs(1.0, 1.0);
		testAlertProbs(0.0, 0.0);
	}
	
	@Test
	public void testOrRand() {
		final int numNode = 40;
		
		// Load graph
		DependencyGraph depGraph = DGraphUtils.loadGraph("testDirs/graphs1/RandomGraph4N3E2T0.json");

		RandomDataGenerator rnd = new RandomDataGenerator();
		final int numTimeStep = 4;
		GameState gameState = new GameState();
		gameState.createID();
		
		
		DefenderBelief dBelief = new DefenderBelief();
		dBelief.addState(gameState, 1.0);
		depGraph.setState(gameState);
		int timeStep = 0;
		final Attacker uniformAttacker = new UniformAttacker(numNode, numNode, 1.0, 0.0);
		AttackerAction attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		DefenderAction doNothingDefAction = new DefenderAction();
		final GameState oldGameState = GameOracle.generateStateSample(gameState, attAction, doNothingDefAction, rnd);
		final int initialActive = 3;
		assertTrue(oldGameState.getEnabledNodeSet().size() == initialActive);
		
		depGraph.setState(oldGameState);
		attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		
		int totalGoalActives = 0;
		final int trials = 5000;
		for (int i = 0; i < trials; i++) {
			GameState tempGameState = GameOracle.generateStateSample(oldGameState, attAction, doNothingDefAction, rnd);
			totalGoalActives += tempGameState.getEnabledNodeSet().size() - initialActive;
		}
		final double meanGoalActives = totalGoalActives * 1.0 / trials;
		final double prob1 = 0.4;
		final double prob2 = 0.3;
		final double prob3 = 0.5;
		final double expectedMeanGoalActives = 1 - (1 - prob1) * (1 - prob2) * (1 - prob3);
		final double tolerance = 0.05;
		assertTrue(Math.abs(meanGoalActives - expectedMeanGoalActives) < tolerance);
	}
	
	@Test
	public void testAllRootRand() {
		final int numNode = 40;
		final int numEdge = 0;
		final int numTarget = 40;
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
		final double aNodeActProbLB = 0.5;
		final double aNodeActProbUB = 0.5;
		final double aEdgeActProbLB = 1.0;
		final double aEdgeActProbUB = 1.0;
		final double minPosActiveProb = 0.8;
		final double maxPosActiveProb = 1.0;
		final double minPosInactiveProb = 0.0;
		final double maxPosInactiveProb = 0.2;
		
		// Load graph
		RandomDataGenerator rnd = new RandomDataGenerator();
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
		
		final int numTimeStep = 4;
		GameState gameState = new GameState();
		gameState.createID();
		DefenderBelief dBelief = new DefenderBelief();
		dBelief.addState(gameState, 1.0);
		depGraph.setState(gameState);
		int timeStep = 0;
		final Attacker uniformAttacker = new UniformAttacker(numNode, numNode, 1.0, 0.0);
		AttackerAction attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		DefenderAction doNothingDefAction = new DefenderAction();
		int totalActives = 0;
		final int trials = 1000;
		for (int i = 0; i < trials; i++) {
			GameState tempGameState = GameOracle.generateStateSample(gameState, attAction, doNothingDefAction, rnd);
			totalActives += tempGameState.getEnabledNodeSet().size();
		}
		final double meanActives = totalActives * 1.0 / trials;
		
		final double tolerance = 0.4;
		final double expectedMeanActives = numNode * aNodeActProbLB;
		assertTrue(Math.abs(meanActives - expectedMeanActives) < tolerance);
	}
	
	@Test
	public void testAllRootNoRand() {
		final int numNode = 40;
		final int numEdge = 0;
		final int numTarget = 40;
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
		final double aNodeActProbLB = 1.0;
		final double aNodeActProbUB = 1.0;
		final double aEdgeActProbLB = 1.0;
		final double aEdgeActProbUB = 1.0;
		final double minPosActiveProb = 0.8;
		final double maxPosActiveProb = 1.0;
		final double minPosInactiveProb = 0.0;
		final double maxPosInactiveProb = 0.2;
		
		// Load graph
		RandomDataGenerator rnd = new RandomDataGenerator();
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
		final Defender uniformDefender = new UniformDefender(numNode, numNode, 1.0, 0.0);
		final Attacker uniformAttacker = new UniformAttacker(numNode, numNode, 1.0, 0.0);
		
		final int numTimeStep = 4;
		GameState gameState = new GameState();
		gameState.createID();
		DefenderBelief dBelief = new DefenderBelief();
		dBelief.addState(gameState, 1.0);
		depGraph.setState(gameState);
		int timeStep = 0;
		AttackerAction attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		DefenderAction defAction = uniformDefender.sampleAction(
			depGraph,
			timeStep,
			numTimeStep,
			dBelief,
			rnd.getRandomGenerator()
		);
		
		timeStep++;
		gameState = GameOracle.generateStateSample(gameState, attAction, defAction, rnd);
		// if defender protects all, and attacker attacks all, and all were inactive, all remain inactive
		assertTrue(gameState.getEnabledNodeSet().size() == 0);
		dBelief.addState(gameState, 2.0);
		depGraph.setState(gameState);
		attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		defAction = uniformDefender.sampleAction(
			depGraph,
			timeStep,
			numTimeStep,
			dBelief,
			rnd.getRandomGenerator()
		);
		gameState = GameOracle.generateStateSample(gameState, attAction, defAction, rnd);
		// if defender protects all, and attacker attacks all, and all were inactive, all remain inactive
		assertTrue(gameState.getEnabledNodeSet().size() == 0);
		
		timeStep++;
		DefenderAction doNothingDefAction = new DefenderAction();
		depGraph.setState(gameState);
		attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep, 
			numTimeStep,
			rnd.getRandomGenerator()
		);
		gameState = GameOracle.generateStateSample(gameState, attAction, doNothingDefAction, rnd);
		// if defender does nothing, and attacker attacks all, and all were inactive, all remain inactive
		assertTrue(gameState.getEnabledNodeSet().size() == numNode);
		
		AttackerAction doNothingAttAction = new AttackerAction();
		gameState = GameOracle.generateStateSample(gameState, doNothingAttAction, doNothingDefAction, rnd);
		// if defender does nothing, and attacker does nothing, all nodes remain same
		assertTrue(gameState.getEnabledNodeSet().size() == numNode);
		
		timeStep++;
		depGraph.setState(gameState);
		attAction = uniformAttacker.sampleAction(
			depGraph, 
			timeStep,
			numTimeStep,
			rnd.getRandomGenerator()
		);
		gameState = GameOracle.generateStateSample(gameState, attAction, defAction, rnd);
		// if defender protects all, and attacker attacks all, and all were active, all become inactive
		assertTrue(gameState.getEnabledNodeSet().size() == 0);
		
		gameState = GameOracle.generateStateSample(gameState, doNothingAttAction, doNothingDefAction, rnd);
		// if defender does nothing, and attacker does nothing, all nodes remain same
		assertTrue(gameState.getEnabledNodeSet().size() == 0);
	}
	
	private void testAlertProbs(
		final double minPosActiveProb,
		final double minPosInactiveProb
	) {
		assert minPosActiveProb >= minPosInactiveProb;
		final int numNode = 40;
		final double maxPosActiveProb = minPosActiveProb;
		final double maxPosInactiveProb = minPosInactiveProb;
		
		final double nodeActTypeRatio = 0.5;
		final int numEdge = 0;
		final int numTarget = 5;
		final double aRewardLB = 1.0;
		final double aRewardUB = 10.0;
		final double dPenaltyLB = -10.0;
		final double dPenaltyUB = -1.0;
		final double aNodeCostLB = -0.5;
		final double aNodeCostUB = -0.1;
		final double aEdgeCostLB = -0.5;
		final double aEdgeCostUB = -0.1;
		final double dCostLB = -0.5;
		final double dCostUB = -0.1;
		final double aNodeActProbLB = 0.8;
		final double aNodeActProbUB = 1.0;
		final double aEdgeActProbLB = 0.6;
		final double aEdgeActProbUB = 0.8;
		
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
		
		final int iters = 1000;
		int countAllEnabled = 0;
		int countNoneEnabled = 0;
		for (int i = 0; i < iters; i++) {
			final GameState allNodesEnabled = allNodes(depGraph);
			final DefenderObservation defObsAllEnabled =
				GameOracle.generateDefObservation(depGraph, allNodesEnabled, rnd);
			countAllEnabled += posAlertCount(defObsAllEnabled);
			
			final GameState noneEnabled = new GameState();
			final DefenderObservation defObsNoneEnabled =
				GameOracle.generateDefObservation(depGraph, noneEnabled, rnd);
			countNoneEnabled += posAlertCount(defObsNoneEnabled);
		}
		
		final double meanAllEnabled = countAllEnabled * 1.0 / iters;
		final double meanNoneEnabled = countNoneEnabled * 1.0 / iters;
		final double tolerance = 0.9;
		final double expectedAllEnabled = numNode * minPosActiveProb;
		final double expectedNoneEnabled = numNode * minPosInactiveProb;
		assertTrue(Math.abs(meanAllEnabled - expectedAllEnabled) < tolerance);
		assertTrue(Math.abs(meanNoneEnabled - expectedNoneEnabled) < tolerance);
	}
	
	private int posAlertCount(final DefenderObservation defObs) {
		int result = 0;
		for (final SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				result++;
			}
		}
		return result;
	}
	
	private GameState allNodes(final DependencyGraph depGraph) {
		final GameState result = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			result.addEnabledNode(node);
		}
		return result;
	}
}
