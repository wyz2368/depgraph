package agent;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.INode.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderCandidate;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import agent.RandomWalkAttacker.RandomWalkTuple;

public final class RandomWalkVsDefender extends Defender {
	private static final double DEFAULT_LOGIS_PARAM = 5.0;
	private static final double DEFAULT_DISC_FACT = 0.9;
	private static final double DEFAULT_THRES = 0.001;
	private static final double DEFAULT_QR_PARAM = 5.0;
	
	private double logisParam = DEFAULT_LOGIS_PARAM; // Logistic parameter to randomize defense strategies
	private double discFact = DEFAULT_DISC_FACT; // reward discount factor
	private double thres = DEFAULT_THRES; // to remove game state from belief
	
	private double qrParam = DEFAULT_QR_PARAM; // for the attacker
	private static final int DEFAULT_NUM_RW_SAMPLE = 30;
	private int numRWSample = DEFAULT_NUM_RW_SAMPLE; // number of random walks for the attacker
	
	// number of simulation to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 20;
	private static final int DEFAULT_NUM_ACTION_SAMPLE = 20;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_ACTION_SAMPLE;
	
	// Randomization over defender's action
	private boolean isRandomized = false;
	
	/*****************************************************************************************
	 * Initialization.
	 * @param logisParam defense parameter for randomizing defenses
	 * @param discFact reward discount factor
	 * @param thres threshold used to limit defender's belief
	 * @param qrParam attack parameter for randomizing attacks
	 * @param numRWSample number of random walk samples
	 *****************************************************************************************/
	public RandomWalkVsDefender(
		final double logisParam, 
		final double discFact, 
		final double thres, 
		final double qrParam, 
		final double numRWSample,
		final double isRandomized) {
		super(DefenderType.vsRANDOM_WALK);
		if (discFact <= 0.0 || discFact > 1.0 || thres < 0.0 || thres > 1.0
			|| numRWSample < 1 || qrParam < 0.0) {
			throw new IllegalArgumentException();
		}
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.numRWSample = (int) numRWSample;
		
		if (isRandomized == 0.0) {
			this.isRandomized = false;
		} else {
			this.isRandomized = true;
		}
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
	public DefenderAction sampleAction(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {	
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		if (this.isRandomized) {
			return sampleActionRandomize(depGraph, curTimeStep, numTimeStep, dBelief, rng);
		}
		return sampleActionStatic(depGraph, curTimeStep, numTimeStep, dBelief, rng);
	}
	
	public DefenderAction sampleActionRandomize(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {	
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
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
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		
		RandomWalkTuple[][][] rwTuplesLists = new RandomWalkTuple[dBelief.getGameStateMap().size()][][];
		AttackerAction[][] attActionLists = new AttackerAction[dBelief.getGameStateMap().size()][];
		double[][] attProbs = new double[dBelief.getGameStateMap().size()][];
		int idx = 0;
		// iterate over all possible game states
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // a possible game state
			depGraph.setState(gameState); // temporarily set game state to the graph
			
			RandomWalkTuple[][] rwTuplesList =
				new RandomWalkTuple[this.numRWSample][]; // list of all random walk tuples sampled
			AttackerAction[] attActionList =
				new AttackerAction[this.numRWSample]; // corresponding list of attack candidates
			double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for (int i = 0; i < this.numRWSample; i++) {
				RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(
					depGraph, 
					curTimeStep, 
					rng); // sample random walk
				AttackerAction attAction = new AttackerAction();
				attValue[i] = RandomWalkAttacker.greedyAction(
					depGraph, // greedy attack
					rwTuples, 
					attAction, // attCandidate is an outcome as well
					numTimeStep, 
					this.discFact); 
				rwTuplesList[i] = rwTuples;
				attActionList[i] = attAction;
			}
			DefenderAction defAction = new DefenderAction();
			// attack probability
			double[] attProb = Attacker.computeCandidateProb(attValue, this.qrParam);
			greedyAction(
				depGraph, // greedy defense with respect to each possible game state
				rwTuplesList, 
				attActionList, 
				attProb, 
				defAction, // this is outcome
				curTimeStep, 
				numTimeStep,
				this.discFact);
			rwTuplesLists[idx] = rwTuplesList;
			attActionLists[idx] = attActionList;
			attProbs[idx] = attProb;
		
			candidates[idx] = defAction;
			idx++;
		}
		// set back to the true game state
		depGraph.setState(savedGameState);
		
		for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
			candidateValues[i] = computeDValue(depGraph, dBelief, rwTuplesLists, attActionLists, attProbs
				, candidates[i], curTimeStep, numTimeStep, this.discFact);
		}
		
		// probability for each possible candidate action for the defender
		double[] probabilities =
			computeCandidateProb(dBelief.getGameStateMap().size(), candidateValues, this.logisParam);
		
		// Start sampling
		int[] nodeIndexes = new int[dBelief.getGameStateMap().size()];
		for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		int sampleIdx = rnd.sample();
		
		return candidates[sampleIdx];
	}
	
	public DefenderAction sampleActionStatic(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief,
		final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		DefenderAction defAction = new DefenderAction();
		greedyAction(depGraph, curTimeStep, numTimeStep, dBelief, rng, defAction);
		return defAction;
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
	public DefenderBelief updateBelief(
		final DependencyGraph depGraph, 
		final DefenderBelief dBelief,
		final DefenderAction dAction,
		final DefenderObservation dObservation,
		final int curTimeStep, 
		final int numTimeStep,
		final RandomGenerator rng) {		
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null
			|| dObservation == null || dAction == null
		) {
			throw new IllegalArgumentException();
		}
		Attacker attacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		return updateBelief(depGraph
			, dBelief
			, dAction
			, dObservation
			, curTimeStep, numTimeStep
			, rng
			, attacker
			, this.numAttActionSample
			, this.numStateSample
			, this.thres); 
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
	public static double greedyAction(
		final DependencyGraph depGraph, // depGraph has current game state the defender is examining
		final RandomWalkTuple[][] rwTuplesList,
		final AttackerAction[] attActionList,
		final double[] attProb,
		final DefenderAction defAction,
		final int curTimeStep, 
		final int numTimeStep,
		final double discFact) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| rwTuplesList == null || attActionList == null || attProb == null
			|| defAction == null || discFact <= 0.0 || discFact > 1.0
		) {
			throw new IllegalArgumentException();
		}
		double value = 0.0;
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		DefenderCandidate defCandidateAll = new DefenderCandidate(); // all candidate nodes for the defender
		for (AttackerAction attAction : attActionList) {
			for (Entry<Node, Set<Edge>> entry : attAction.getActionCopy().entrySet()) {
				defCandidateAll.addNodeCandidate(entry.getKey());
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
		
		boolean[][] isInQueue = new boolean[attActionList.length][depGraph.vertexSet().size()];
		// Initialize queue, this queue is used for checking if any node is still in the queue of activating
		// when the defender starts disabling nodes
		for (int i = 0; i < attActionList.length; i++) {
			for (int j = 0; j < depGraph.vertexSet().size(); j++) {
				isInQueue[i][j] = false;
			}
		}
		
		
		int idx = 0;
		for (AttackerAction attAction : attActionList) {
			for (Entry<Node, Set<Edge>> entry : attAction.getActionCopy().entrySet()) {
				Node node = entry.getKey();
				isInQueue[idx][node.getId() - 1] = true;
			}
			idx++;
		}
		for (idx = 0; idx < attActionList.length; idx++) {
			for (int i = 0; i < depGraph.vertexSet().size(); i++) {
				Node node = topoOrder[i];
				List<Edge> preEdgeList = rwTuplesList[idx][node.getId() - 1].getPreAct();
				if (!isInQueue[idx][node.getId() - 1] && preEdgeList != null) {
					// if not set in queue yet, not root or active nodes
					if (node.getActivationType() == NodeActivationType.OR) { // if OR node
						Node preNode = preEdgeList.get(0).getsource();
						if (isInQueue[idx][preNode.getId() - 1]) {
							isInQueue[idx][node.getId() - 1] = true;
						}
					} else {
						boolean temp = true;
						for (Edge preEdge : preEdgeList) {
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
		
		// Current estimated value for the defender
		for (Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE) {
				value += target.getDPenalty() * Math.pow(discFact, curTimeStep - 1);
			} else {
				for (idx = 0; idx < attActionList.length; idx++) {
					if (isInQueue[idx][target.getId() - 1]) {
						RandomWalkTuple[] rwTuples = rwTuplesList[idx];
						int actTime = rwTuples[target.getId() - 1].getTAct();
						value += attProb[idx] 
								* rwTuples[target.getId() - 1].getPAct() 
								* target.getDPenalty() 
								* Math.pow(discFact, actTime - 1);
					}
				}
			}
		}
		// Start greedy process----------------------------------------------------------------------------
		boolean[][] maxQueue =
			new boolean[attActionList.length][depGraph.vertexSet().size()]; // corresponding best queue
		boolean[][] isInCurrentQueue = new boolean[attActionList.length][depGraph.vertexSet().size()];
		boolean isStop = false;
		while (!isStop) { // only stop when no new candidate node can increase the defender's utility
			isStop = true;
			int candidateIdx = 0;
			
			double maxValue = 0.0; // best value of the defender
			Node maxCandidateNode = null; // best node to add
			int maxIdx = -1;
			for (Node dCandidateNode : defCandidateListAll) { // iterate over candidate nodes of the defender
				double curValue = 0.0;
				// Initialize queue, this queue is used for checking if any node is still in the queue of activating
				// when the defender starts disabling nodes
				for (int i = 0; i < attActionList.length; i++) {
					for (int j = 0; j < depGraph.vertexSet().size(); j++) {
						isInCurrentQueue[i][j] = isInQueue[i][j];
					}
				}
				if (!isChosen[candidateIdx]) { // if not chosen yet, start examining
					if (dCandidateNode.getState() != NodeState.ACTIVE) { // not active targets
						idx = 0;
						for (RandomWalkTuple[] rwTuples : rwTuplesList) { // iterate over tuples
							List<Node> queue = new ArrayList<Node>();
							queue.add(dCandidateNode);
							isInCurrentQueue[idx][dCandidateNode.getId() - 1] = false;
							
							while (!queue.isEmpty()) { // forward tracking
								Node node = queue.remove(0);
								for (Edge postEdge : depGraph.outgoingEdgesOf(node)) {
									Node postNode = postEdge.gettarget();
									if (isInCurrentQueue[idx][postNode.getId() - 1]) {
										// if this postNode is in the current queue
										if (postNode.getActivationType() == NodeActivationType.OR) {
											Node preNode =
												rwTuples[postNode.getId() - 1].getPreAct().get(0).getsource();
											if (preNode.getId() == node.getId()) {
												isInCurrentQueue[idx][postNode.getId() - 1] = false;
												queue.add(postNode);
											}
										} else {
											isInCurrentQueue[idx][postNode.getId() - 1] = false;
											queue.add(postNode);
										}
									}
								}
							}
							
							for (Node target : depGraph.getTargetSet()) {
								int actTime = rwTuples[target.getId() - 1].getTAct();
								if (actTime <= numTimeStep
									&& !isInCurrentQueue[idx][target.getId() - 1] 
									&& isInQueue[idx][target.getId() - 1]) {
									curValue -= attProb[idx] 
										* rwTuples[target.getId() - 1].getPAct() 
										* target.getDPenalty() 
										* Math.pow(discFact, actTime - 1);
								}
							}
							idx++;
						}
					} else { // this is an active target
						curValue -= dCandidateNode.getDPenalty() * Math.pow(discFact, curTimeStep - 1);
					}
					double tempValue = curValue + dCandidateNode.getDCost() * Math.pow(discFact, curTimeStep - 1);
					if (maxValue < tempValue) {
						for (int i = 0; i < attActionList.length; i++) {
							for (int j = 0; j < depGraph.vertexSet().size(); j++) {
								maxQueue[i][j] = isInCurrentQueue[i][j];
							}
						}
						maxValue = tempValue;
						maxCandidateNode = dCandidateNode;
						maxIdx = candidateIdx;
						isStop = false;
					}
				}
				candidateIdx++;
			}
			if (!isStop) {
				for (int i = 0; i < attActionList.length; i++) {
					for (int j = 0; j < depGraph.vertexSet().size(); j++) {
						isInQueue[i][j] = maxQueue[i][j];
					}
				}
				defAction.addNodetoProtect(maxCandidateNode);
				value += maxValue;
				isChosen[maxIdx] = true;
			}
		}
		
		return value;
	}
	
	public static double computeDValue(
		final DependencyGraph depGraph,
		final DefenderBelief dBelief,
		final RandomWalkTuple[][][] rwTuplesLists,
		final AttackerAction[][] attActionLists,
		final double[][] attProbs,
		final DefenderAction defAction,
		final int curTimeStep, 
		final int numTimeStep,
		final double discFact) {
		double dValue = 0.0;
		// True game state
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		int idx = 0;
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // a possible game state
			Double gameStateProb = entry.getValue(); // corresponding state probability
			depGraph.setState(gameState); // temporarily set game state to the graph
			RandomWalkTuple[][] rwTuplesList =  rwTuplesLists[idx];
			AttackerAction[] attActionList = attActionLists[idx];
			double[] attProb = attProbs[idx];
			
			dValue += gameStateProb * computeDValue(depGraph, rwTuplesList, attActionList, attProb
					, defAction, curTimeStep, numTimeStep, discFact);
			idx++;
		}
		depGraph.setState(savedGameState);
		
		for (Node node : defAction.getAction()) {
			dValue += node.getDCost() * Math.pow(discFact, curTimeStep - 1);
		}
		return dValue;
	}
	
	// depGraph has current game state the defender is examining
	public static double computeDValue(
		final DependencyGraph depGraph,
		final RandomWalkTuple[][] rwTuplesList,
		final AttackerAction[] attActionList,
		final double[] attProb,
		final DefenderAction defAction,
		final int curTimeStep, final int numTimeStep,
		final double discFact) {
		double dValue = 0.0;
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		boolean[] isBlock = new boolean[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			isBlock[i] = false;
		}
		for (Node node : defAction.getAction()) {
			isBlock[node.getId() - 1] = true;
		}
		boolean[][] isInQueue = new boolean[attActionList.length][depGraph.vertexSet().size()];
		// Initialize queue, this queue is used for checking if any node is still in the queue of activating
		// when the defender starts disabling nodes
		for (int i = 0; i < attActionList.length; i++) {
			for (int j = 0; j < depGraph.vertexSet().size(); j++) {
				isInQueue[i][j] = false;
			}
		}
		
		int idx = 0;
		for (AttackerAction attAction : attActionList) {
			for (Entry<Node, Set<Edge>> entry : attAction.getActionCopy().entrySet()) {
				Node node = entry.getKey();
				if (!isBlock[node.getId() - 1]) {
					isInQueue[idx][node.getId() - 1] = true;
				}
			}
			idx++;
		}
		for (idx = 0; idx < attActionList.length; idx++) {
			RandomWalkTuple[] rwTuples = rwTuplesList[idx];
			for (int i = 0; i < depGraph.vertexSet().size(); i++) {
				Node node = topoOrder[i];
				List<Edge> preEdgeList = rwTuples[node.getId() - 1].getPreAct();
				// if not set in queue yet, not root or active nodes
				if (!isInQueue[idx][node.getId() - 1] && preEdgeList != null) {
					if (node.getActivationType() == NodeActivationType.OR) { // if OR node
						Node preNode = preEdgeList.get(0).getsource();
						if (isInQueue[idx][preNode.getId() - 1] && !isBlock[node.getId() - 1]) {					
							isInQueue[idx][node.getId() - 1] = true;
						}
					} else {
						boolean temp = true;
						for (Edge preEdge : preEdgeList) {
							Node preNode = preEdge.getsource();
							if (!isInQueue[idx][preNode.getId() - 1] || isBlock[preNode.getId() - 1]) {
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
			
			for (Node target : depGraph.getTargetSet()) {
				int actTime = rwTuples[target.getId() - 1].getTAct();
				if (actTime <= numTimeStep && isInQueue[idx][target.getId() - 1]) {
					dValue += attProb[idx] * rwTuples[target.getId() - 1].getPAct() * target.getDPenalty() 
						* Math.pow(discFact, actTime - 1);
				}
			}
		}
		for (Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE && !isBlock[target.getId() - 1]) {
				dValue += target.getDPenalty() * Math.pow(discFact, curTimeStep - 1);
			}
		}
		return dValue;
	}
	
	public double greedyAction(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng, 
		final DefenderAction defAction) { // this is outcome
		double greedyValue = 0.0;
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		// True game state
		GameState savedGameState = new GameState();
		for (Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				savedGameState.addEnabledNode(node);
			}
		}
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		int beliefSize = dBelief.getGameStateMap().size();
		RandomWalkTuple[][][] rwTuplesAll = new RandomWalkTuple[beliefSize][][];
		AttackerAction[][] rwAttActionAll = new AttackerAction[beliefSize][];
		double[][] rwAttProbAll = new double[beliefSize][];
		int bIdx = 0;
		// iterate over all possible game states
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // a possible game state
			depGraph.setState(gameState); // temporarily set game state to the graph
			
			// list of all random walk tuples sampled
			RandomWalkTuple[][] rwTuplesList = new RandomWalkTuple[this.numRWSample][];
			// corresponding list of attack candidates
			AttackerAction[] attActionList = new AttackerAction[this.numRWSample];
			double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for (int i = 0; i < this.numRWSample; i++) {
				RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(depGraph, curTimeStep, rng); // sample random walk
				AttackerAction attAction = new AttackerAction();
				attValue[i] = RandomWalkAttacker.greedyAction(
					depGraph, // greedy attack
					rwTuples, 
					attAction, // attAction is an outcome as well
					numTimeStep, 
					this.discFact); 
				rwTuplesList[i] = rwTuples;
				attActionList[i] = attAction;
			}
			// attack probability
			double[] attProb = Attacker.computeCandidateProb(attValue, this.qrParam);
			
			rwTuplesAll[bIdx] = rwTuplesList;
			rwAttActionAll[bIdx] = attActionList;
			rwAttProbAll[bIdx] = attProb;
			bIdx++;
		}
		// set back to the true game state
		depGraph.setState(savedGameState);
		
		// Initialize block status of nodes in the graph
		boolean[][][] isBlock = new boolean[beliefSize][this.numRWSample][depGraph.vertexSet().size()];
		bIdx = 0;
		for (bIdx = 0; bIdx < beliefSize; bIdx++) { // iterate over all possible game states
			for (int j = 0; j < this.numRWSample; j++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					isBlock[bIdx][j][k] = true;
				}
				AttackerAction attAction = rwAttActionAll[bIdx][j];
				for (Entry<Node, Set<Edge>> entry : attAction.getActionCopy().entrySet()) {
					isBlock[bIdx][j][entry.getKey().getId() - 1] = false;
				}
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					Node node = topoOrder[k];
					RandomWalkTuple rwTuple = rwTuplesAll[bIdx][j][node.getId() - 1];
					List<Edge> preEdgeList = rwTuple.getPreAct();
					if (preEdgeList != null && isBlock[bIdx][j][node.getId() - 1]
							&& rwTuple.getTAct() <= numTimeStep) {
						if (node.getActivationType() == NodeActivationType.OR) {
							Node preNode = preEdgeList.get(0).getsource();
							if (!isBlock[bIdx][j][preNode.getId() - 1]) {
								isBlock[bIdx][j][node.getId() - 1] = false;
							}
						} else {
							boolean tempBlock = false;
							for (Edge edge : preEdgeList) {
								if (isBlock[bIdx][j][edge.getsource().getId() - 1]) {
									tempBlock = true;
									break;
								}
							}
							if (!tempBlock) {
								isBlock[bIdx][j][node.getId() - 1] = false;
							}
						}
					}
				}
			}
			bIdx++;
		}
		// Compute current value of the defender
		bIdx = 0;
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			for (int j = 0; j < this.numRWSample; j++) {
				for (Node target : depGraph.getTargetSet()) {
					if (!isBlock[bIdx][j][target.getId() - 1]) {
						greedyValue += entry.getValue() 
								* rwAttProbAll[bIdx][j]
								* target.getDPenalty() 
								* Math.pow(this.discFact, rwTuplesAll[bIdx][j][target.getId() - 1].getTAct() - 1);
					}
				}
			}
			for (Node node : entry.getKey().getEnabledNodeSet()) {
				if (node.getType() == NodeType.TARGET) {
					greedyValue += entry.getValue() 
							* node.getDPenalty() 
							* Math.pow(this.discFact, curTimeStep - 1);
				}
			}
			bIdx++;
		}
		
		
		// Defender candidates
		DefenderCandidate defCandidate = new DefenderCandidate();
		bIdx = 0;
		// iterate over all possible game states
		for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			GameState gameState = entry.getKey(); // a possible game state
			for (int j = 0; j < this.numRWSample; j++) {
				for (Node node : gameState.getEnabledNodeSet()) {
					if (node.getType() == NodeType.TARGET) {
						defCandidate.addNodeCandidate(node);
					}
				}
				
				AttackerAction attAction = rwAttActionAll[bIdx][j];
				for (Entry<Node, Set<Edge>> attActionEntry : attAction.getActionCopy().entrySet()) {
					defCandidate.addNodeCandidate(attActionEntry.getKey());
				}
			}
			bIdx++;
		}
		List<Node> defCandidateList = new ArrayList<Node>(defCandidate.getNodeCandidateSet());
		boolean[] isGreedyChosen = new boolean[defCandidateList.size()];
		for (int i = 0; i < defCandidateList.size(); i++) {
			isGreedyChosen[i] = false;
		}
		
		// Start greedy
		boolean isStop = false;
		boolean[][][] isBlockClone =
			new boolean[dBelief.getGameStateMap().size()][this.numRWSample][depGraph.vertexSet().size()];
		boolean[][][] maxIsBlock =
			new boolean[dBelief.getGameStateMap().size()][this.numRWSample][depGraph.vertexSet().size()];
		while (!isStop) {
			int dCandidateIdx = 0;
			double maxValue = 0.0;
			int maxNodeIdx = -1;
			for (Node dCandidateNode : defCandidateList) {
				if (!isGreedyChosen[dCandidateIdx]) { // Examining this node
					double curValue = dCandidateNode.getDCost() * Math.pow(this.discFact, curTimeStep - 1);
					for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
						for (int j = 0; j < this.numRWSample; j++) {
							for (int k = 0; k < depGraph.vertexSet().size(); k++) {
								isBlockClone[i][j][k] = isBlock[i][j][k];
							}
						}
					}
					bIdx = 0;
					// iterate over all possible game states
					for (Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
						GameState gameState = entry.getKey();
						Double gameStateProb = entry.getValue();
						if (dCandidateNode.getType() == NodeType.TARGET && gameState.containsNode(dCandidateNode)) {
							curValue -= dCandidateNode.getDPenalty() 
								* Math.pow(this.discFact, curTimeStep - 1)
								* gameStateProb;
						}
						for (int j = 0; j < this.numRWSample; j++) {
							isBlockClone[bIdx][j][dCandidateNode.getId() - 1] = true;
							for (int k = 0; k < depGraph.vertexSet().size(); k++) {
								Node node = topoOrder[k];
								List<Edge> preEdgeList = rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct();
								if (preEdgeList != null && !isBlockClone[bIdx][j][node.getId() - 1]
									&& rwTuplesAll[bIdx][j][node.getId() - 1].getTAct() <= numTimeStep) {
									if (node.getActivationType() == NodeActivationType.OR) { // OR nodes
										Node preNode = preEdgeList.get(0).getsource();
										if (isBlockClone[bIdx][j][preNode.getId() - 1]) {
											isBlockClone[bIdx][j][node.getId() - 1] = true;
										}
									} else { // AND nodes
										for (Edge edge : preEdgeList) {
											if (isBlockClone[bIdx][j][edge.getsource().getId() - 1]) {
												isBlockClone[bIdx][j][node.getId() - 1] = true;
												break;
											}
										}
									}
								}
							}
							
							for (Node target : depGraph.getTargetSet()) {
								RandomWalkTuple rwTuple = rwTuplesAll[bIdx][j][target.getId() - 1]; 
								if (isBlockClone[bIdx][j][target.getId() - 1] 
									&& !isBlock[bIdx][j][target.getId() - 1]
										&& rwTuple.getTAct() <= numTimeStep) {
									curValue -= gameStateProb * rwAttProbAll[bIdx][j]
											* rwTuple.getPAct() 
											* Math.pow(this.discFact, rwTuple.getTAct() - 1)
											* target.getDPenalty();
								}
							}
						}
						
						bIdx++;
					}
					if (curValue > maxValue) {
						maxValue = curValue;
						maxNodeIdx = dCandidateIdx;
						for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
							for (int j = 0; j < this.numRWSample; j++) {
								for (int k = 0; k < depGraph.vertexSet().size(); k++) {
									maxIsBlock[i][j][k] = isBlockClone[i][j][k];
								}
							}
						}
					}
					
				}
				dCandidateIdx++;
			}
			if (maxNodeIdx != -1) {
				greedyValue += maxValue;
				isGreedyChosen[maxNodeIdx] = true;
				for (int i = 0; i < dBelief.getGameStateMap().size(); i++) {
					for (int j = 0; j < this.numRWSample; j++) {
						for (int k = 0; k < depGraph.vertexSet().size(); k++) {
							isBlock[i][j][k] = maxIsBlock[i][j][k];
						}
					}
				}
				defAction.addNodetoProtect(defCandidateList.get(maxNodeIdx));
			} else {
				isStop = true;
			}
		}
		
		return greedyValue;
	}
}
