package agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import game.GameOracle;
import graph.Node;
import graph.INode.NodeState;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

public abstract class ValuePropVsDefSuper extends Defender {
	
	private double thres; // to remove game state from belief
	private double discFact;

	// defender's assumption about attacker
	private double qrParam; 
	private int maxNumAttCandidate; 
	private int minNumAttCandidate;
	private double numAttCandidateRatio;
	
	// number of simulations to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 30;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE;
	
	public ValuePropVsDefSuper(
		final DefenderType type,
		final double discFact, 
		final double thres,
		final double qrParam, 
		final double maxNumAttCandidate, 
		final double minNumAttCandidate,
		final double numAttCandidateRatio
	) {
		super(type);
		if (
			discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
		) {
			throw new IllegalArgumentException();
		}
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.maxNumAttCandidate = (int) maxNumAttCandidate;
		this.minNumAttCandidate = (int) minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
	}

	@Override
	public final DefenderBelief updateBelief(
			final DependencyGraph depGraph,
			final DefenderBelief dBelief, 
			final DefenderAction dAction,
			final DefenderObservation dObservation, 
			final int curTimeStep, 
			final int numTimeStep,
			final RandomGenerator rng) {
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
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact);
		
		// iterate over current belief of the defender
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // one of possible game state
			Double curStateProb = entry.getValue(); // probability of the game state
		
			depGraph.setState(gameState); // for each possible state
			
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
				, rng, this.numAttActionSample, false); // Sample attacker actions
			
			for (int attActionSample = 0; attActionSample < this.numAttActionSample; attActionSample++) {
				// Iterate over all samples of attack actions
				AttackerAction attAction = attActionList.get(attActionSample); // current sample of attack action
				// attAction.print();
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
					, rnd, this.numStateSample, true); // s' <- s, a, d, // Sample new game states
				int curNumStateSample = gameStateList.size();
				for (int stateSample = 0; stateSample < curNumStateSample; stateSample++) {
					GameState newGameState = gameStateList.get(stateSample);
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
								dAction, attAction
								, gameState, newGameState);
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
			if (entry.getValue() > this.thres) {
				revisedBelief.addState(entry.getKey(), entry.getValue());
			}
		}
		// Re-normalize again
		sumProb = 0.0;
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			sumProb += entry.getValue();
		}
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			entry.setValue(entry.getValue() / sumProb); 
		}
		return revisedBelief;
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
