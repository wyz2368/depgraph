package agent;

import game.GameOracle;
import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.AttackCandidate;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderCandidate;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import agent.RandomWalkAttacker.RandomWalkTuple;

public final class RandomWalkvsDefender extends Defender {
	private double logisParam; // Logistic parameter to randomize defense strategies
	private double discFact; // reward discount factor
	private double thres; // to remove game state from belief
	
	private double qrParam; // for the attacker
	private static final int DEFAULT_NUM_RW_SAMPLE = 200;
	private int numRWSample = DEFAULT_NUM_RW_SAMPLE; // number of random walks for the attacker
	
	// number of samples to update the defender's belief.
	private static final int DEFAULT_NUM_STATE_SAMPLE = 50;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE; // number of states to sample
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE; // number of attack actions to sample
	
	/*****************************************************************************************
	 * Initialization.
	 * @param logisParam defense parameter for randomizing defenses.
	 * @param discFact reward discount factor
	 * @param thres threshold used to limit defender's belief
	 * @param qrParam attack parameter for randomizing attacks
	 * @param numRWSample number of random walk samples
	 * @param numStateSample number of game state samples
	 * @param numAttActionSample number of attack action samples
	 *****************************************************************************************/
	public RandomWalkvsDefender(final double logisParam
			, final double discFact
			, final double thres
			, final double qrParam
			, final int numRWSample
			, final int numStateSample
			, final int numAttActionSample) {
		this(logisParam, discFact, thres
				, qrParam, numRWSample);
		this.numStateSample = numStateSample;
		this.numAttActionSample = numAttActionSample;
	}
	
	/*****************************************************************************************
	 * Initialization.
	 * @param logisParam defense parameter for randomizing defenses
	 * @param discFact reward discount factor
	 * @param thres threshold used to limit defender's belief
	 * @param qrParam attack parameter for randomizing attacks
	 * @param numRWSample number of random walk samples
	 *****************************************************************************************/
	public RandomWalkvsDefender(final double logisParam
			, final double discFact
			, final double thres
			, final double qrParam
			, final double numRWSample) {
		super(DefenderType.vsRANDOM_WALK);
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.numRWSample = (int) numRWSample;
	}
	
	/*****************************************************************************************
	 * Sampling defense actions.
	 * @param depGraph dependency graph with true game state
	 * @param curTimeStep current time step of the game
	 * @param numTimeStep total number of time steps
	 * @param dBelief belief of the defender over possible game states
	 * @param rng random generator
	 * @return action for the defender
	 *****************************************************************************************/
	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph
			, final int curTimeStep, final int numTimeStep
			, final DefenderBelief dBelief
			, final RandomGenerator rng) {
		
		// True game state
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		
		// Compute value of each candidate action for the defender
		// Each candidate action corresponds to a possible game state
		double[] candidateValues = new double[dBelief.getGameStateMap().size()];
		DefenderAction[] candidates = new DefenderAction[dBelief.getGameStateMap().size()];
		
		// Assumption about the attacker
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, curTimeStep, numTimeStep);
		
		int idx = 0;
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) { // iterate over all possible game states
			GameState gameState = entry.getKey(); // a possible game state
			Double gameStateProb = entry.getValue(); // corresponding state probability
			depGraph.setState(gameState); // temporarily set game state to the graph
			
			List<RandomWalkTuple[]> rwTuplesList = new ArrayList<RandomWalkTuple[]>(); // list of all random walk tuples sampled
			List<AttackCandidate> attCandidateList = new ArrayList<AttackCandidate>(); // corresponding list of attack candidates
			double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for (int i = 0; i < this.numRWSample; i++) {
				RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(depGraph, curTimeStep, rng); // sample random walk
				AttackCandidate attCandidate = new AttackCandidate();
				attValue[i] = RandomWalkAttacker.greedyCandidate(depGraph // greedy attack
						, rwTuples, attCandidate // attCandidate is an outcome as well
						, numTimeStep, this.discFact); 
				rwTuplesList.add(rwTuples);
				attCandidateList.add(attCandidate);
			}
			DefenderAction defAction = new DefenderAction();
			double[] attProb = RandomWalkAttacker.computecandidateProb(this.numRWSample, attValue, this.qrParam); // attack probability
			double dValue = greedyCandidate(depGraph // greedy defense with respect to each possible game state
					, rwTuplesList, attCandidateList, attProb
					, defAction // this is outcome
					, curTimeStep, numTimeStep
					, this.discFact);
		
			candidateValues[idx] = dValue * gameStateProb;
			candidates[idx] = defAction;
			idx++;
		}
		
		// set back to the true game state
		depGraph.setState(savedGameState);
		
		// probability for each possible candidate action for the defender
		double[] probabilities = computecandidateProb(dBelief.getGameStateMap().size(), candidateValues, this.logisParam);
		
		// Start sampling
		int[] nodeIndexes = new int[dBelief.getGameStateMap().size()];
		for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		int sampleIdx = rnd.sample();
		
		return candidates[sampleIdx];
	}
	
	@Override
	/*****************************************************************************************
	 * Update defender's belief.
	 * @param depGraph dependency graph with true game state
	 * @param dBelief belief of the defender over states
	 * @param dAction action of the defender
	 * @param dObservation observations of the defender
	 * @param curTimeStep current time step
	 * @param numTimeStep total number of time steps
	 * @param rng Random Generator
	 * @return new belief of the defender
	 *****************************************************************************************/
	public DefenderBelief updateBelief(final DependencyGraph depGraph
			, final DefenderBelief dBelief
			, final DefenderAction dAction
			, final DefenderObservation dObservation
			, final int curTimeStep, final int numTimeStep
			, final RandomGenerator rng) {
		
		RandomDataGenerator rnd = new RandomDataGenerator(rng);
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		
		DefenderBelief newBelief = new DefenderBelief(); // new belief of the defender
		Map<GameState, Double> observationProbMap = new HashMap<GameState, Double>(); // probability of observation given game state
		
		Attacker attacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) { // iterate over current belief of the defender
			GameState gameState = entry.getKey(); // one of possible game state
			Double curStateProb = entry.getValue(); // probability of the game state
		
			depGraph.setState(gameState); // for each possible state
			
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // sample attacker actions
			
			for (int attActionSample = 0; attActionSample < this.numAttActionSample; attActionSample++) {
			    // Iterate over all samples of attack actions
				AttackerAction attAction = attActionList.get(attActionSample); // current sample of attack action
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
						, rnd, this.numStateSample, true); // s' <- s, a, d, // Sample new game states
				int curNumStateSample = gameStateList.size();
				for (int stateSample = 0; stateSample < curNumStateSample; stateSample++) {
					GameState newGameState = gameStateList.get(stateSample);
					Double curProb = newBelief.getProbability(newGameState); // check if this new game state is already generated
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
		return revisedBelief;
	}
	
	/*****************************************************************************************
	 * Note: defAction outcome of greedy, need to be pre-initialized.
	 * @param depGraph dependency graph with game state which is being examined by the defender
	 * @param rwTuplesList list of random walk tuples 
	 * @param attCandidateList corresponding list of attack candidates
	 * @param attProb attack probability
	 * @param defAction defender action
	 * @param curTimeStep current time step
	 * @param numTimeStep total number of time step
	 * @param discFact reward discount factor
	 * @return a candidate
	 *****************************************************************************************/
	public static double greedyCandidate(final DependencyGraph depGraph // depGraph has current game state the defender is examining
			, final List<RandomWalkTuple[]> rwTuplesList
			, final List<AttackCandidate> attCandidateList
			, final double[] attProb
			, final DefenderAction defAction
			, final int curTimeStep, final int numTimeStep
			, final double discFact) {
		double value = 0.0;
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		
		DefenderCandidate defCandidateAll = new DefenderCandidate(); // all candidate nodes for the defender
		for (AttackCandidate attCandidate : attCandidateList) { // all these are inactive
			for (Node node : attCandidate.getNodeCandidateSet()) {
				defCandidateAll.addNodeCandidate(node);
			}
			for (Edge edge : attCandidate.getEdgeCandidateSet()) {
				defCandidateAll.addNodeCandidate(edge.gettarget());
			}
		}
		for (Node target : depGraph.getTargetSet()) { // active targets
			if (target.getState() == NodeState.ACTIVE) {
				defCandidateAll.addNodeCandidate(target);
			}
		}
		
		List<Node> defCandidateListAll = new ArrayList<Node>(defCandidateAll.getNodeCandidateSet());
		boolean[] isChosen = new boolean[defCandidateListAll.size()];
		for (int i = 0; i < defCandidateListAll.size(); i++) {
			isChosen[i] = false;
		}
		if (attCandidateList.isEmpty()) {
			System.out.println("Attacker candidate is empty");
		}
		
		boolean[][] isInQueue = new boolean[attCandidateList.size()][depGraph.vertexSet().size()];
		// Initialize queue, this queue is used for checking if any node is still in the queue of activating
		// when the defender starts disabling nodes
		for (int i = 0; i < attCandidateList.size(); i++) {
			for (int j = 0; j < depGraph.vertexSet().size(); j++) {
				isInQueue[i][j] = false;
			}
		}
		
		int idx = 0;
		for (AttackCandidate attCandidate : attCandidateList) {
			for (Node node : attCandidate.getNodeCandidateSet()) {
				isInQueue[idx][node.getId() - 1] = true;
			}
			for (Edge edge : attCandidate.getEdgeCandidateSet()) {
				isInQueue[idx][edge.gettarget().getId() - 1] = true;
			}
			idx++;
		}
		for (idx = 0; idx < attCandidateList.size(); idx++) {
			for (int i = 0; i < depGraph.vertexSet().size(); i++) {
				Node node = topoOrder[i];
				if (node.getState() == NodeState.ACTIVE) {
					isInQueue[idx][node.getId() - 1] = true;
				}
				if (!isInQueue[idx][node.getId() - 1] && depGraph.inDegreeOf(node) > 0) { // if not set in queue yet and not root node
					if (node.getActivationType() == NodeActivationType.OR) { // if OR node
						Node preNode = rwTuplesList.get(idx)[node.getId() - 1].getPreAct().get(0).getsource();
						if (isInQueue[idx][preNode.getId() - 1]) {
							isInQueue[idx][node.getId() - 1] = true;
						}
					} else {
						boolean temp = true;
						for (Edge preEdge : rwTuplesList.get(idx)[node.getId() - 1].getPreAct()) {
							if (!isInQueue[idx][preEdge.getsource().getId() - 1]) {
								temp = false;
								break;
							}
						}
						if (temp) {
							isInQueue[idx][node.getId() - 1] = true;
						}
					}
				}
			}
		}
		
		// Start greedy process----------------------------------------------------------------------------
		boolean isStop = false;
		while (!isStop) { // only stop when no new candidate node can increase the defender's utility
			isStop = true;
			int candidateIdx = 0;
			double maxValue = Double.NEGATIVE_INFINITY; // best value of the defender
			boolean[][] maxQueue = null; // corresponding queue
			Node maxCandidateNode = null; // best node to add
			int maxIdx = -1;
			for (Node dCandidateNode : defCandidateListAll) { // iterate over candidate nodes of the defender
				double curValue = 0.0;
				if (!isChosen[candidateIdx]) { // if not chosen yet, start examining
					if (isInQueue == null) {
						throw new IllegalStateException();
					}
					boolean[][] isInCurrentQueue = isInQueue.clone(); // used to examine new node
					idx = 0;
					for (RandomWalkTuple[] rwTuples : rwTuplesList) { // iterate over tuples
						List<Node> queue = new ArrayList<Node>();
						queue.add(dCandidateNode);
						isInCurrentQueue[idx][dCandidateNode.getId() - 1] = false;
						while (!queue.isEmpty()) { // forward tracking
							Node node = queue.remove(0);
							for (Edge postEdge : depGraph.outgoingEdgesOf(node)) {
								Node postNode = postEdge.gettarget();
								if (isInCurrentQueue[idx][postNode.getId() - 1]) { // if this postNode is in the current queue
									if (postNode.getActivationType() == NodeActivationType.OR) {
										Node preNode = rwTuples[postNode.getId() - 1].getPreAct().get(0).getsource();
										if (!isInCurrentQueue[idx][preNode.getId() - 1]) {
											isInCurrentQueue[idx][postNode.getId() - 1] = false;
											queue.add(postNode);
										}
									} else {
										boolean temp = true;
										for (Edge edge : rwTuples[postNode.getId() - 1].getPreAct()) {
											Node preNode = edge.getsource();
											if (!isInCurrentQueue[idx][preNode.getId() - 1]) {
												temp = false;
												break;
											}
										}
										if (!temp) {
											isInCurrentQueue[idx][postNode.getId() - 1] = false;
											queue.add(postNode);
										}
									}
								}
							}
						}
						for (Node target : depGraph.getTargetSet()) {
							int actTime = rwTuples[target.getId() - 1].getTAct();
							if (actTime <= numTimeStep && isInCurrentQueue[idx][target.getId() - 1]) {
								curValue += attProb[idx] * rwTuples[target.getId() - 1].getPAct() * target.getDPenalty() 
												* Math.pow(discFact, actTime - 1);
							}
						}
						idx++;
					}
					double tempValue = curValue + dCandidateNode.getDCost() * Math.pow(discFact, curTimeStep - 1);
					if (maxValue < tempValue) {
						maxValue = tempValue;
						maxQueue = isInCurrentQueue;
						maxCandidateNode = dCandidateNode;
						maxIdx = candidateIdx;
						isStop = false;
					}
				}
				candidateIdx++;
			}
			if (!isStop) {
				isInQueue = maxQueue;
				defAction.addNodetoProtect(maxCandidateNode);
				value = maxValue;
				isChosen[maxIdx] = true;
			}
		}
		
		return value;
	}
	
	/*****************************************************************************************
	 * Compute defense probability.
	 * @param totalNumCandidate total number of candidate actions
	 * @param candidateValue array of candidate values
	 * @param logisParam defense parameter for randomization
	 * @return defense probability for every candidate action
	 *****************************************************************************************/
	public static double[] computecandidateProb(final int totalNumCandidate, final double[] candidateValue, final double logisParam) {
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
	
}