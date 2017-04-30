package agent;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import game.GameSimulationSpec;
import graph.Edge;
import graph.INode.NodeType;
import graph.Node;
import model.AttackCandidate;
import model.DependencyGraph;
import model.GameState;
import utils.DGraphUtils;
import utils.JsonUtils;

@SuppressWarnings("static-method")
public final class UnitTestAttacker {
	
	public UnitTestAttacker() {
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
		final String simspecFolderName = "testDirs/simSpec0";
		final String graphFolderName = "testDirs/graphs0";
  
		final GameSimulationSpec simSpec =
			JsonUtils.getSimSpecOrDefaults(simspecFolderName);
		// Load graph
		String filePathName = graphFolderName + File.separator
			+ "RandomGraph" + simSpec.getNumNode() + "N" + simSpec.getNumEdge() + "E" 
			+ simSpec.getNumTarget() + "T"
			+ simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
		
		final DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);

		GameState gameState = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			gameState.addEnabledNode(node);
		}
		depGraph.setState(gameState);
		// should be empty because all nodes are active
		AttackCandidate candidate = Attacker.getAttackCandidate(depGraph);
		assertTrue(candidate.getEdgeCandidateSet().isEmpty());
		assertTrue(candidate.getNodeCandidateSet().isEmpty());
		
		gameState = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			if (node.getType() == NodeType.TARGET) {
				gameState.addEnabledNode(node);
			}
		}
		depGraph.setState(gameState);
		// should be empty because all target nodes are enabled
		candidate = Attacker.getAttackCandidate(depGraph);
		assertTrue(candidate.getEdgeCandidateSet().isEmpty());
		assertTrue(candidate.getNodeCandidateSet().isEmpty());
		
		gameState = new GameState();
		depGraph.setState(gameState);
		// should have 3 candidate nodes, no edges
		candidate = Attacker.getAttackCandidate(depGraph);
		assertTrue(candidate.getEdgeCandidateSet().isEmpty());
		final int initialCands = 3;
		assertTrue(candidate.getNodeCandidateSet().size() == initialCands);
		
		for (final Node node: depGraph.vertexSet()) {
			if (node.getId() == 1) {
				gameState.addEnabledNode(node);
			}
		}
		depGraph.setState(gameState);
		candidate = Attacker.getAttackCandidate(depGraph);
		// should have node 5 as a candidate, because it's an AND node
		// with all parents active
		assertTrue(candidate.getEdgeCandidateSet().isEmpty());
		assertTrue(candidate.getNodeCandidateSet().size() == initialCands);
		boolean foundFive = false;
		final int targetFive = 5;
		for (final Node node: depGraph.vertexSet()) {
			if (node.getId() == targetFive) {
				assertTrue(candidate.getNodeCandidateSet().contains(node));
				foundFive = true;
			}
		}
		assertTrue(foundFive);
		
		gameState = new GameState();
		for (final Node node: depGraph.vertexSet()) {
			if (node.getId() == 2) {
				gameState.addEnabledNode(node);
			}
		}
		depGraph.setState(gameState);
		candidate = Attacker.getAttackCandidate(depGraph);
		// should have just 2 candidate nodes, and edge 1 as a candidate,
		// because edge 1 it goes to an inactive OR node (node 4) from an active node (node 2)
		assertTrue(candidate.getEdgeCandidateSet().size() == 1);
		final int newNodeCandCount = 2;
		assertTrue(candidate.getNodeCandidateSet().size() == newNodeCandCount);
		boolean foundEdge = false;
		for (final Edge edge: depGraph.edgeSet()) {
			if (edge.getId() == 1) {
				assertTrue(candidate.getEdgeCandidateSet().contains(edge));
				foundEdge = true;
			}
		}
		assertTrue(foundEdge);
	}
}
