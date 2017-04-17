package agent;

import game.GameOracle;
import graph.Node;
import graph.INode.NODE_STATE;

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

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class UniformvsDefender extends Defender{
	double logisParam; // Logistic parameter to randomize defense strategies
	double discFact; // reward discount factor
	double thres; // to remove game state from belief
	
	int maxNumSelectACandidate;
	int minNumSelectACandidate;
	double numSelectACandidateRatio;
	
	int numStateSample = 50; // number of states to sample
	int numAttActionSample = 50; // number of attack actions to sample
	public UniformvsDefender(double logisParam, double discFact, double thres
			, int maxNumSelectACandidate, int minNumSelectACandidate, double numSelectACandidateRatio
			, int numStateSample, int numAttActionSample){
		this(logisParam, discFact, thres
				, maxNumSelectACandidate, minNumSelectACandidate, numSelectACandidateRatio);
		this.numStateSample = numStateSample;
		this.numAttActionSample = numAttActionSample;
	}
	public UniformvsDefender(double logisParam, double discFact, double thres
			, int maxNumSelectACandidate, int minNumSelectACandidate, double numSelectACandidateRatio){
		super(DEFENDER_TYPE.vsUNIFORM);
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		this.maxNumSelectACandidate = maxNumSelectACandidate;
		this.minNumSelectACandidate = minNumSelectACandidate;
		this.numSelectACandidateRatio = numSelectACandidateRatio;
	}
	
	@Override
	public DefenderBelief updateBelief(DependencyGraph depGraph
			, DefenderBelief dBelief
			, DefenderAction dAction
			, DefenderObservation dObservation
			, int curTimeStep, int numTimeStep
			, RandomGenerator rng) {
		
		RandomDataGenerator rnd = new RandomDataGenerator(rng);
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		
		DefenderBelief newBelief = new DefenderBelief(); // new belief of the defender
		Map<GameState, Double> observationProbMap = new HashMap<GameState, Double>(); // probability of observation given game state
		
		Attacker attacker = new UniformAttacker(this.maxNumSelectACandidate, this.minNumSelectACandidate, this.numSelectACandidateRatio);
		
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over current belief of the defender
		{
			GameState gameState = entry.getKey(); // one of possible game state
			Double curStateProb = entry.getValue(); // probability of the game state
		
			depGraph.setState(gameState); // for each possible state
			
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // Sample attacker actions
			
			for(int attActionSample = 0; attActionSample < this.numAttActionSample; attActionSample++)
			{// Iterate over all samples of attack actions
				AttackerAction attAction = attActionList.get(attActionSample); // current sample of attack action
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
						, rnd, this.numStateSample, true); // s' <- s, a, d, // Sample new game states
				int curNumStateSample = gameStateList.size();
				for(int stateSample = 0; stateSample < curNumStateSample; stateSample++)
				{
					GameState newGameState = gameStateList.get(stateSample);
					Double curProb = newBelief.getProbability(newGameState); // check if this new game state is already generated
					double observationProb = 0.0;
					if(curProb == null) // new game state
					{
						observationProb = GameOracle.computeObservationProb(newGameState, dObservation);
						observationProbMap.put(newGameState, observationProb);
						curProb = 0.0;
					}
					else // already generated
					{
						observationProb = observationProbMap.get(newGameState);
					}
					double addedProb = observationProb * curStateProb 
							* GameOracle.computeStateTransitionProb(depGraph
									, dAction, attAction
									, gameState, newGameState);
					
					newBelief.addState(newGameState, curProb + addedProb);
				}
			}
			
		}
		
		// Restore game state
		depGraph.setState(savedGameState);
		
		// Normalization
		double sumProb = 0.0;
		for(Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet())
		{
			sumProb += entry.getValue();
		}
		for(Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet())
		{
			entry.setValue(entry.getValue() / sumProb); 
		}
		
		// Belief revision
		DefenderBelief revisedBelief = new DefenderBelief();
		for(Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet())
		{
			if(entry.getValue() > this.thres)
				revisedBelief.addState(entry.getKey(), entry.getValue());
		}
		newBelief.clear();
		//Re-normalize again
		sumProb = 0.0;
		for(Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet())
		{
			sumProb += entry.getValue();
		}
		for(Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet())
		{
			entry.setValue(entry.getValue() / sumProb); 
		}
		return revisedBelief;
	}
	
	@Override
	public DefenderAction sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep, DefenderBelief dBelief,
			RandomGenerator rng) {
		// TODO Auto-generated method stub
		return null;
	}
}
