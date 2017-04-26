package agent;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import game.GameSimulationSpec;
import game.MeanGameSimulationResult;
import main.MainGameSimulation;
import model.DependencyGraph;
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
		
		@SuppressWarnings("unused")
		final UniformDefender testDefender =
			(UniformDefender) AgentFactory.createDefender(defenderName, defenderParams, simSpec.getDiscFact());
		final MeanGameSimulationResult simResult = MainGameSimulation.runSimulations(
			depGraph, simSpec, attackerName,
			attackerParams, defenderName, defenderParams,
			simSpec.getNumSim());
		JsonUtils.getObservationString(
			simResult, attackerString, defenderString, simSpec);
	}
}
