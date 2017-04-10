package main;

import game.GameSimulation;
import game.GameSimulationSpec;
import game.MeanGameSimulationResult;

import java.io.File;
import java.util.Map;

import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;

import agent.AgentFactory;
import agent.Attacker;
import agent.Defender;
import utils.DGraphUtils;
import utils.EncodingUtils;
import utils.JsonUtils;

public class MainGameSimulation {

	public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            throw new IllegalStateException(
        		"Need two arguments: simspecFolder, graphFolder"
    		);
        }
        final long startTime = System.nanoTime();
        runSimulationsAndPrint(args[0], args[1]);
        final long endTime = System.nanoTime();
        final long diff = endTime - startTime;
        final long millis = diff / 1000000;
        System.out.println("time taken in millis: " + millis);
    }
	 /**
     * @param folderName the local path to the folder
     * containing simulation_spec.json
     * @param obsCount how many observation files
     * to produce, each using simSpec.numSims
     * runs.
     */
    public static void runSimulationsAndPrint(final String simspecFolderName, final String graphFolderName) {
        if (simspecFolderName == null) {
            throw new IllegalArgumentException();
        }
  
        final GameSimulationSpec simSpec = JsonUtils.getSimSpecOrDefaults(simspecFolderName);
        
        // Load graph
        String filePathName = graphFolderName + File.separator + simSpec.getNumNode() + "N" + simSpec.getNumEdge() + "E" 
        		+ simSpec.getNumTarget() + "T" + simSpec.getTotalNumAlert() + "TA" + simSpec.getMinNumAlert() + "MIA" 
        		+ simSpec.getMaxNumAlert() + "MAA" + simSpec.getARewardLB() + "ARL" + simSpec.getARewardUB() + "ARU"
        		+ simSpec.getDPenaltyLB() + "DPL" + simSpec.getDPenaltyUB() + "DPU" + simSpec.getGraphID() + JsonUtils.JSON_SUFFIX;
        DependencyGraph depGraph = DGraphUtils.loadGraph(filePathName);
        		
        // Load players
        final String attackerString = JsonUtils.getAttackerString(simspecFolderName);
        final String defenderString = JsonUtils.getDefenderString(simspecFolderName);
        final String attackerName = EncodingUtils.getStrategyName(attackerString);
        final String defenderName = EncodingUtils.getStrategyName(defenderString);
        final Map<String, Double> attackerParams = EncodingUtils.getStrategyParams(attackerString);
        final Map<String, Double> defenderParams = EncodingUtils.getStrategyParams(defenderString);
        
        final MeanGameSimulationResult simResult = runSimulations(depGraph, simSpec, attackerName, attackerParams, defenderName, defenderParams,
            		simSpec.getNumSim());
            final String obsString = JsonUtils.getObservationString(simResult, attackerString, defenderString, simSpec);
            JsonUtils.printObservationToFile(simspecFolderName, obsString);

    }
	private static MeanGameSimulationResult runSimulations(DependencyGraph depGraph,
			GameSimulationSpec simSpec, String attackerName,
			Map<String, Double> attackerParams, String defenderName,
			Map<String, Double> defenderParams, int numSim) {
		MeanGameSimulationResult meanGameSimResult = new MeanGameSimulationResult();
		Attacker attacker = AgentFactory.createAttacker(attackerName, attackerParams, simSpec.getDiscFact());
		Defender defender = AgentFactory.createDefender(defenderName, defenderParams, simSpec.getDiscFact());
		// TODO Auto-generated method stub
		RandomDataGenerator rng = new RandomDataGenerator();
		GameSimulation gameSim = new GameSimulation(depGraph, attacker, defender, rng
				, simSpec.getNumTimeStep(), simSpec.getDiscFact());
		
		for(int i = 0; i < numSim; i++)
		{
			if(i % 1000 == 0)
				System.out.println("Simulation: " + i);
			gameSim.runSimulation();
			meanGameSimResult.updateMeanSimulationResult(gameSim.getSimulationResult());
			gameSim.reset();
		}
		return meanGameSimResult;
	}
}
