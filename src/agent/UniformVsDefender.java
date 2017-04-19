package agent;

import game.GameOracle;
import graph.Node;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public final class UniformVsDefender extends Defender {
	private int maxNumRes;
	private int minNumRes;
	private double numResRatio;
	private double logisParam; // Logistic parameter to randomize defense strategies
	private double discFact; // reward discount factor
	private double thres; // to remove game state from belief
	
	private int maxNumSelectACandidate;
	private int minNumSelectACandidate;
	private double numSelectACandidateRatio;
	
	private static final int DEFAULT_NUM_STATE_SAMPLE = 50;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE; // number of states to sample
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE; // number of attack actions to sample
	
	public UniformVsDefender(final double logisParam, final double discFact, final double thres
		, final int maxNumRes, final int minNumRes, final double numResRatio
		, final int maxNumSelectACandidate, final int minNumSelectACandidate, final double numSelectACandidateRatio) {
		super(DefenderType.vsUNIFORM);
		if (discFact <= 0.0 || discFact > 1.0 || thres < 0.0 || thres > 1.0
			|| minNumRes < 1 || maxNumRes < minNumRes || numResRatio < 0.0 || numResRatio > 1.0
			|| minNumSelectACandidate < 1 || maxNumSelectACandidate < minNumSelectACandidate
			|| numSelectACandidateRatio < 0.0 || numSelectACandidateRatio > 1.0
		) {
			throw new IllegalArgumentException();
		}
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		
		this.maxNumRes = maxNumRes;
		this.minNumRes = minNumRes;
		this.numResRatio = numResRatio;
		
		this.maxNumSelectACandidate = maxNumSelectACandidate;
		this.minNumSelectACandidate = minNumSelectACandidate;
		this.numSelectACandidateRatio = numSelectACandidateRatio;
	}
	
	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep
		, final DefenderBelief dBelief
		, final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		Map<Node, Double> dValueMap = computeCandidateValueTopo(depGraph, dBelief, curTimeStep, numTimeStep
			, this.discFact, rng);
		List<Node> dCandidateNodeList = new ArrayList<Node>();
		double[] candidateValue = new double[dValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		int totalNumCandidate = dValueMap.size();
		
		// Compute probability to choose each node
		double[] probabilities = computeCandidateProb(totalNumCandidate, candidateValue, this.logisParam);

		// Only keep candidates with high probability
		int numGoodCandidate = 0;
		for (int i = 0; i < totalNumCandidate; i++) {
			if (probabilities[i] >= this.thres) {
				numGoodCandidate++;
			}
		}
		// Compute number of candidates to select
		int numNodetoProtect = 0;
		if (dCandidateNodeList.size() < this.minNumRes) {
			numNodetoProtect = dCandidateNodeList.size();
		} else {
			numNodetoProtect = Math.max(this.minNumRes, (int) (this.numResRatio * dCandidateNodeList.size()));
			numNodetoProtect = Math.min(this.maxNumRes, numNodetoProtect);
		}
		if (numNodetoProtect > numGoodCandidate) {
			numNodetoProtect = numGoodCandidate;
		}
		
		if (numNodetoProtect == 0) { // if there is no candidate
			return new DefenderAction();
		}
		
		// Sampling
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return simpleSampleAction(dCandidateNodeList, numNodetoProtect, rnd);
	}
	
	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph
		, final DefenderBelief dBelief
		, final DefenderAction dAction
		, final DefenderObservation dObservation
		, final int curTimeStep, final int numTimeStep
		, final RandomGenerator rng) {
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
		
		Attacker attacker = new UniformAttacker(
			this.maxNumSelectACandidate, this.minNumSelectACandidate, this.numSelectACandidateRatio);
		
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
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
					, rnd, this.numStateSample, true); // s' <- s, a, d, // Sample new game states
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
		//Re-normalize again
		sumProb = 0.0;
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			sumProb += entry.getValue();
		}
		for (Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet()) {
			entry.setValue(entry.getValue() / sumProb); 
		}
		System.out.println("Belief size: " + revisedBelief.getGameStateMap().size());
		return revisedBelief;
	}
	
	private Map<Node, Double> computeCandidateValueTopo(
		final DependencyGraph depGraph,
		final DefenderBelief dBelief,
		final int curTimeStep,
		final int numTimeStep,
		final double discountFactor,
		final RandomGenerator rng) {
		if (depGraph == null || dBelief == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0 || rng == null
		) {
			throw new IllegalArgumentException();
		}
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		Attacker attacker = new UniformAttacker(
			this.maxNumSelectACandidate, this.minNumSelectACandidate, this.numSelectACandidateRatio);
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		// iterate over current belief of the defender
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey();
			Double curStateProb = entry.getValue();
			
			depGraph.setState(gameState); // for each possible state
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
				, rng, this.numAttActionSample, false); // Sample attacker actions
			Map<Node, Double> curDValueMap = computeCandidateValueTopo(depGraph, attActionList
				, curTimeStep, numTimeStep, discountFactor);
			for (Entry<Node, Double> dEntry : curDValueMap.entrySet()) {
				Node node = dEntry.getKey();
				Double value = dEntry.getValue();
				
				Double curDValue = dValueMap.get(node);
				if (curDValue == null) {
					curDValue = value * curStateProb;
				} else {
					curDValue += value * curStateProb;
				}
				dValueMap.put(node, curDValue);
			}
		}
		for (Entry<Node, Double> entry : dValueMap.entrySet()) {
			Node node = entry.getKey();
			Double value = entry.getValue();
			if (value == Double.POSITIVE_INFINITY) {
				value = 0.0;
			}
			entry.setValue((-value + node.getDCost()) * Math.pow(discountFactor, curTimeStep - 1));
		}
		depGraph.setState(savedGameState);
		return dValueMap;
	}
	
	private static double[] computeCandidateProb(final int totalNumCandidate,
		final double[] candidateValue, final double logisParam) {
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
		} else {
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
}
