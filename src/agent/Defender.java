package agent;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import game.GameOracle;
import graph.Node;
import graph.INode.NodeState;

public abstract class Defender {
	public enum DefenderType {
		UNIFORM, MINCUT, GOAL_ONLY, ROOT_ONLY
		, vsVALUE_PROPAGATION, vsRANDOM_WALK, vsUNIFORM, NOOP;
		
		@Override
		public String toString() {
			switch(this) {
			case UNIFORM: return "UN";
			case MINCUT: return "MC";
			case GOAL_ONLY: return "GO";
			case ROOT_ONLY: return "RO";
			case vsVALUE_PROPAGATION: return "vVP";
			case vsRANDOM_WALK: return "vRW";
			case vsUNIFORM: return "vUN";
			case NOOP: return "NOOP";
			default: return "";
			}	
		}
	}
	
	// isRandomized is for the vs-random walk defender only
	public enum DefenderParam {
		maxNumRes, minNumRes, numResRatio, 
		maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio, 
		logisParam, bThres, isRandomized,
		qrParam, numRWSample, stdev, numAttCandStdev,
		isTopo;
		
		@Override
		public String toString() {
			switch(this) {
			case maxNumRes: return "maxNumRes";
			case minNumRes: return "minNumRes";
			case numResRatio: return "numResRatio";
			
			case maxNumAttCandidate: return "maxNumAttCandidate";
			case numAttCandidateRatio: return "numAttCandidateRatio";
			case minNumAttCandidate: return "minNumAttCandidate";
			
			case logisParam: return "logisParam";
			case bThres: return "bThres";
			case isRandomized: return "isRandomized";
			
			case qrParam: return "qrParam";
			case numRWSample: return "numRWSample";
			case stdev: return "stdev";
			case numAttCandStdev: return "numAttCandStdev";
			
			case isTopo: return "isTopo";
			default: return "";
			}	
		}
	}

	private DefenderType dType;
	
	public Defender(final DefenderType dTypeCur) {
		if (dTypeCur == null) {
			throw new IllegalArgumentException();
		}
		this.dType = dTypeCur;
	}
	
	public final DefenderType getDType() {
		return this.dType;
	}
	
	public abstract DefenderAction sampleAction(
		DependencyGraph depGraph,
		int curTimeStep, 
		int numTimeStep, 
		DefenderBelief dBelief, 
		RandomGenerator rng);
	
	public abstract DefenderBelief updateBelief(
		DependencyGraph depGraph,
		DefenderBelief currentBelief, 
		DefenderAction dAction,
		DefenderObservation dObservation, 
		int curTimeStep, 
		int numTimeStep,
		RandomGenerator rng);
	
	public static final DefenderAction simpleSampleAction(
		final List<Node> dCandidateNodeList,
		final int numNodetoProtect,
		final AbstractIntegerDistribution rnd) {
		if (dCandidateNodeList == null || numNodetoProtect < 0 
				|| numNodetoProtect > dCandidateNodeList.size() || rnd == null) {
			throw new IllegalArgumentException();
		}
		DefenderAction action = new DefenderAction();
		
		boolean[] isChosen = new boolean[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			isChosen[i] = false;
		}
		int count = 0;
		while (count < numNodetoProtect) {
			int idx = rnd.sample();
			if (!isChosen[idx]) {
				action.addNodetoProtect(dCandidateNodeList.get(idx));
				isChosen[idx] = true;
				count++;
			}
				
		}
		return action;
	}
	
	public static DefenderBelief updateBelief(final DependencyGraph depGraph
		, final DefenderBelief dBelief
		, final DefenderAction dAction
		, final DefenderObservation dObservation
		, final int curTimeStep, final int numTimeStep
		, final RandomGenerator rng
		, final Attacker attacker
		, final int numAttActionSample
		, final int numStateSample
		, final double thres) {
		if (depGraph == null || dBelief == null || dAction == null || dObservation == null 
			|| curTimeStep < 0 || numTimeStep < curTimeStep || rng == null
		) {
			throw new IllegalArgumentException();
		}
		
		RandomDataGenerator rnd = new RandomDataGenerator(rng);
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		
		DefenderBelief newBelief = new DefenderBelief(); // new belief of the defender
		// probability of observation given game state
		Map<GameState, Double> observationProbMap = new HashMap<GameState, Double>();
		
		// iterate over current belief of the defender
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // one of possible game state
			Double curStateProb = entry.getValue(); // probability of the game state
		
			depGraph.setState(gameState); // for each possible state
			
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
				, rng, numAttActionSample, false); // Sample attacker actions
			int trueNumAttActionSample = attActionList.size();
			for (int attActionSample = 0; attActionSample < trueNumAttActionSample; attActionSample++) {
				// Iterate over all samples of attack actions
				AttackerAction attAction = attActionList.get(attActionSample); // current sample of attack action
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
					, rnd, numStateSample, true); // s' <- s, a, d, // Sample new game states
				int curNumStateSample = gameStateList.size();
				for (int stateSample = 0; stateSample < curNumStateSample; stateSample++) {
					GameState newGameState = gameStateList.get(stateSample);
					// check if this new game state is already generated
					Double curProb = newBelief.getProbability(newGameState);
					double observationProb = 0.0;
					if (curProb == null) { // new game state
						observationProb = GameOracle.computeObservationProb(newGameState, dObservation);
						observationProbMap.put(newGameState, observationProb);
						curProb = 0.0;
					} else { // already generated
						observationProb = observationProbMap.get(newGameState);
					}
					double addedProb = observationProb * curStateProb 
						* GameOracle.computeStateTransitionProb(
							dAction, 
							attAction, 
							gameState, 
							newGameState);
					
					newBelief.addState(newGameState, curProb + addedProb);
				}
			}
		}
		
		// Restore game state
		depGraph.setState(savedGameState);
		
		// Normalization
		double sumProb = 0.0;
		for (Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet()) {
			sumProb += entry.getValue();
		}
		for (Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet()) {
			entry.setValue(entry.getValue() / sumProb); 
		}
		
		// Belief revision
		DefenderBelief revisedBelief = new DefenderBelief();
		for (Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet()) {
//			System.out.println(entry.getValue());
			if (entry.getValue() > thres) {
				revisedBelief.addState(entry.getKey(), entry.getValue());
			}
		}
		//Re-normalize again
		sumProb = 0.0;
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			sumProb += entry.getValue();
		}
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			entry.setValue(entry.getValue() / sumProb); 
		}
		return revisedBelief;
	}
	
	public static double[] computeCandidateProb(
		final int totalNumCandidate, final double[] candidateValue, final double logisParam) {
		if (totalNumCandidate < 0 || candidateValue == null) {
			throw new IllegalArgumentException();
		}
		//Normalize candidate value
		double minValue = Double.POSITIVE_INFINITY;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < totalNumCandidate; i++) {
			if (minValue > candidateValue[i]) {
				minValue = candidateValue[i];
			}
			if (maxValue < candidateValue[i]) {
				maxValue = candidateValue[i];
			}
		}
		if (maxValue > minValue) {
			for (int i = 0; i < totalNumCandidate; i++) {
				candidateValue[i] = (candidateValue[i] - minValue) / (maxValue - minValue);
			}
		} else  {
			for (int i = 0; i < totalNumCandidate; i++) {
				candidateValue[i] = 0.0;
			}
		}
		
		// Compute probability
		double[] probabilities = new double[totalNumCandidate];
		int[] nodeList = new int[totalNumCandidate];
		double sumProb = 0.0;
		for (int i = 0; i < totalNumCandidate; i++) {
			nodeList[i] = i;
			probabilities[i] = Math.exp(logisParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for (int i = 0; i < totalNumCandidate; i++) {
			probabilities[i] /= sumProb;
		}
		
		return probabilities;
	}
	
	public static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}