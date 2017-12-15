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
	
	// Logistic parameter to randomize defense strategies
	private double logisParam = DEFAULT_LOGIS_PARAM;
	private double discFact = DEFAULT_DISC_FACT; // reward discount factor
	private double thres = DEFAULT_THRES; // to remove game state from belief
	
	private double qrParam = DEFAULT_QR_PARAM; // for the attacker
	private static final int DEFAULT_NUM_RW_SAMPLE = 30;
	// number of random walks for the attacker
	private int numRWSample = DEFAULT_NUM_RW_SAMPLE;
	
	// number of simulation to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 20;
	private static final int DEFAULT_NUM_ACTION_SAMPLE = 20;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_ACTION_SAMPLE;
	
	// Randomization over defender's action
	private boolean isRandomized = false;
	
	/*****************************************
	 * Initialization.
	 * @param logisParam defense parameter for randomizing defenses
	 * @param discFact reward discount factor
	 * @param thres threshold used to limit defender's belief
	 * @param qrParam attack parameter for randomizing attacks
	 * @param numRWSample number of random walk samples
	 *****************************************/
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
	
	/*****************************************
	 * Sampling defense actions.
	 * @param depGraph dependency graph with true game state
	 * @param curTimeStep current time step of the game
	 * @param numTimeStep total number of time steps
	 * @param dBelief belief of the defender over possible game states
	 * @param rng random generator
	 * @return action for the defender
	 *****************************************/
	@Override
	public DefenderAction sampleAction(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {	
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		if (this.isRandomized) {
			return sampleActionRandomize(
				depGraph, curTimeStep, numTimeStep, dBelief, rng);
		}
		return sampleActionStatic(
			depGraph, curTimeStep, numTimeStep, dBelief, rng);
	}
	
	private DefenderAction sampleActionRandomize(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng) {	
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		// Used for storing true game state of the game
		final GameState savedGameState = depGraph.getGameState();
		
		// Assumption about the attacker
		final RandomWalkAttacker rwAttacker =
			new RandomWalkAttacker(
				this.numRWSample, this.qrParam, this.discFact);
		final DefenderAction[] candidates =
			new DefenderAction[dBelief.getGameStateMap().size()];
		final RandomWalkTuple[][][] rwTuplesLists =
			new RandomWalkTuple[dBelief.getGameStateMap().size()][][];
		final AttackerAction[][] attActionLists =
			new AttackerAction[dBelief.getGameStateMap().size()][];
		final double[][] attProbs =
			new double[dBelief.getGameStateMap().size()][];
		int idx = 0;
		// iterate over all possible game states
		for (final Entry<GameState, Double> entry
			: dBelief.getGameStateMap().entrySet()) {
			// a possible game state
			final GameState curGameState = entry.getKey();
			// temporarily set game state to the graph
			depGraph.setState(curGameState);
			
			// list of all random walk tuples sampled
			final RandomWalkTuple[][] rwTuplesList =
				new RandomWalkTuple[this.numRWSample][];
			// corresponding list of attack candidates
			final AttackerAction[] attActionList =
				new AttackerAction[this.numRWSample];
			// values of corresponding action of the attacker
			final double[] attValue = new double[this.numRWSample];
			for (int i = 0; i < this.numRWSample; i++) {
				final RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(
					depGraph, 
					curTimeStep, 
					rng); // sample random walk
				final AttackerAction attAction = new AttackerAction();
				attValue[i] = RandomWalkAttacker.greedyAction(
					depGraph, // greedy attack
					rwTuples, 
					attAction, // attCandidate is an outcome as well
					numTimeStep, 
					this.discFact); 
				rwTuplesList[i] = rwTuples;
				attActionList[i] = attAction;
			}
			final DefenderAction defAction = new DefenderAction();
			// attack probability
			final double[] attProb =
				Attacker.computeCandidateProb(attValue, this.qrParam);
			greedyAction(
				depGraph, // greedy defense with respect
						// to each possible game state
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
		
		final double[] candidateValues =
			new double[dBelief.getGameStateMap().size()];
		for (int i = 0; i < candidateValues.length; i++) {
			candidateValues[i] =
				computeDValue(depGraph, dBelief, rwTuplesLists, 
						attActionLists, attProbs
				, candidates[i], curTimeStep, numTimeStep, this.discFact);
		}
		
		// probability for each possible candidate action for the defender
		final double[] probabilities =
			computeCandidateProb(
				dBelief.getGameStateMap().size(), 
				candidateValues, this.logisParam);
		
		// Start sampling
		final int[] nodeIndexes =
			Attacker.getIndexArray(dBelief.getGameStateMap().size());
		final EnumeratedIntegerDistribution rnd =
			new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		final int sampleIdx = rnd.sample();
		return candidates[sampleIdx];
	}
	
	private DefenderAction sampleActionStatic(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief,
		final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null) {
			throw new IllegalArgumentException();
		}
		final DefenderAction defAction = new DefenderAction();
		greedyAction(depGraph, curTimeStep, 
			numTimeStep, dBelief, rng, defAction);
		return defAction;
	}
	
	@Override
	/*****************************************
	 * Update defender's belief.
	 * @param depGraph dependency graph with true game state
	 * @param dBelief belief of the defender over states
	 * @param dAction action of the defender
	 * @param dObservation observations of the defender
	 * @param curTimeStep current time step
	 * @param numTimeStep total number of time steps
	 * @param rng Random Generator
	 * @return new belief of the defender
	 *****************************************/
	public DefenderBelief updateBelief(
		final DependencyGraph depGraph, 
		final DefenderBelief dBelief,
		final DefenderAction dAction,
		final DefenderObservation dObservation,
		final int curTimeStep, 
		final int numTimeStep,
		final RandomGenerator rng) {		
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null
			|| dObservation == null || dAction == null
		) {
			throw new IllegalArgumentException();
		}
		final Attacker attacker = new RandomWalkAttacker(
			this.numRWSample, this.qrParam, this.discFact);
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
	
	/*****************************************
	* Note: defAction outcome of greedy, need to be pre-initialized.
	* @param depGraph dependency graph with game
	* state which is being examined by the defender
	* @param rwTuplesList list of random walk tuples 
	* @param attActions corresponding
	* list of attack candidates
	* @param attProb attack probability
	* @param defAction defender action
	* @param curTimeStep current time step
	* @param numTimeStep total number of time step
	* @param discFact reward discount factor
	* @return a candidate
	 *****************************************/
	public static double greedyAction(
		// depGraph has current game state the defender is examining
		final DependencyGraph depGraph,
		final RandomWalkTuple[][] rwTuplesList,
		final AttackerAction[] attActions,
		final double[] attProb,
		final DefenderAction defAction,
		final int curTimeStep, 
		final int numTimeStep,
		final double discFact) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep
			|| rwTuplesList == null || attActions == null || attProb == null
			|| defAction == null || discFact <= 0.0 || discFact > 1.0
		) {
			throw new IllegalArgumentException();
		}

		// all candidate nodes for the defender
		final DefenderCandidate defCandidateAll = new DefenderCandidate();
		for (final AttackerAction attAction : attActions) {
			for (final Entry<Node, Set<Edge>> entry
				: attAction.getActionCopy().entrySet()) {
				defCandidateAll.addNodeCandidate(entry.getKey());
			}
		}
		for (final Node target : depGraph.getTargetSet()) { // active targets
			if (target.getState() == NodeState.ACTIVE) {
				defCandidateAll.addNodeCandidate(target);
			}
		}
		
		boolean[][] isInQueue =
			new boolean[attActions.length][depGraph.vertexSet().size()];
		for (int i = 0; i < attActions.length; i++) {
			final AttackerAction attAction = attActions[i];
			for (final Node node : attAction.getActionCopy().keySet()) {
				isInQueue[i][node.getId() - 1] = true;
			}
		}

		// Get topological order, starting from zero
		final Node[] topoOrder = Attacker.getTopoOrder(depGraph);
		for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
			for (int topoIndex = 0; topoIndex < depGraph.vertexSet().size(); topoIndex++) {
				final Node node = topoOrder[topoIndex];
				final List<Edge> inEdges = rwTuplesList[actionIndex][node.getId() - 1].getPreAct();
				if (!isInQueue[actionIndex][node.getId() - 1] && inEdges != null) {
					// if not set in queue yet, not root or active nodes
					if (node.getActivationType() == NodeActivationType.OR) { // if OR node
						final Node parent = inEdges.get(0).getsource();
						if (isInQueue[actionIndex][parent.getId() - 1]) {
							isInQueue[actionIndex][node.getId() - 1] = true;
						}
					} else {
						boolean shouldAdd = true;
						for (final Edge inEdge : inEdges) {
							if (!isInQueue[actionIndex][
	                            inEdge.getsource().getId() - 1]) {
								shouldAdd = false;
								break;
							}
						}
						if (shouldAdd) {
							isInQueue[actionIndex][node.getId() - 1] = true;
						}
					}
				}
			}
		}
		
		double value = 0.0;
		// Current estimated value for the defender
		for (final Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE) {
				value += target.getDPenalty() * Math.pow(discFact, curTimeStep - 1);
			} else {
				for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
					if (isInQueue[actionIndex][target.getId() - 1]) {
						final RandomWalkTuple[] rwTuples = rwTuplesList[actionIndex];
						final int actTime = rwTuples[target.getId() - 1].getTAct();
						value += attProb[actionIndex] 
							* rwTuples[target.getId() - 1].getPAct() 
							* target.getDPenalty() 
							* Math.pow(discFact, actTime - 1);
					}
				}
			}
		}

		// Start greedy process----------------------------------------------------------------------------
		final boolean[][] maxQueue =
			new boolean[attActions.length][depGraph.vertexSet().size()]; // corresponding best queue
		final boolean[][] isInCurrentQueue = new boolean[attActions.length][depGraph.vertexSet().size()];	
		final List<Node> defCandidateListAll = new ArrayList<Node>(defCandidateAll.getNodeCandidateSet());
		final boolean[] isChosen = new boolean[defCandidateListAll.size()];
		
		boolean isStop = false;
		int idx = 0;
		while (!isStop) { // only stop when no new candidate node can increase the defender's utility
			isStop = true;
			int candidateIdx = 0;
			double maxValue = 0.0; // best value of the defender
			Node maxCandidateNode = null; // best node to add
			int maxIdx = -1;
			for (final Node dCandidateNode : defCandidateListAll) { // iterate over candidate nodes of the defender
				double curValue = 0.0;
				// Initialize queue, this queue is used for checking if any node is still in the queue of activating
				// when the defender starts disabling nodes
				for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
					for (int nodeIndex = 0; nodeIndex < depGraph.vertexSet().size(); nodeIndex++) {
						isInCurrentQueue[actionIndex][nodeIndex] = isInQueue[actionIndex][nodeIndex];
					}
				}
				if (!isChosen[candidateIdx]) { // if not chosen yet, start examining
					if (dCandidateNode.getState() == NodeState.INACTIVE) { // INACTIVE targets
						idx = 0;
						for (final RandomWalkTuple[] rwTuples : rwTuplesList) { // iterate over tuples
							final List<Node> queue = new ArrayList<Node>();
							queue.add(dCandidateNode);
							isInCurrentQueue[idx][dCandidateNode.getId() - 1] = false;
							
							while (!queue.isEmpty()) { // forward tracking
								final Node node = queue.remove(0);
								for (final Edge outEdge : depGraph.outgoingEdgesOf(node)) {
									final Node childNode = outEdge.gettarget();
									if (isInCurrentQueue[idx][childNode.getId() - 1]) {
										// if this postNode is in the current queue
										if (childNode.getActivationType() == NodeActivationType.OR) {
											final Node preNode =
												rwTuples[childNode.getId() - 1].getPreAct().get(0).getsource();
											if (preNode.getId() == node.getId()) {
												isInCurrentQueue[idx][childNode.getId() - 1] = false;
												queue.add(childNode);
											}
										} else {
											isInCurrentQueue[idx][childNode.getId() - 1] = false;
											queue.add(childNode);
										}
									}
								}
							}
							
							for (final Node target : depGraph.getTargetSet()) {
								final int actTime = rwTuples[target.getId() - 1].getTAct();
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
					final double tempValue = curValue + dCandidateNode.getDCost() * Math.pow(discFact, curTimeStep - 1);
					if (maxValue < tempValue) {
						for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
							for (int nodeIndex = 0; nodeIndex < depGraph.vertexSet().size(); nodeIndex++) {
								maxQueue[actionIndex][nodeIndex] = isInCurrentQueue[actionIndex][nodeIndex];
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
				for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
					for (int nodeIndex = 0; nodeIndex < depGraph.vertexSet().size(); nodeIndex++) {
						isInQueue[actionIndex][nodeIndex] = maxQueue[actionIndex][nodeIndex];
					}
				}
				defAction.addNodetoProtect(maxCandidateNode);
				value += maxValue;
				isChosen[maxIdx] = true;
			}
		}
		
		return value;
	}
	
	static double computeDValue(
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
		final GameState savedGameState = depGraph.getGameState();
		
		int idx = 0;
		for (final Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			final GameState curGameState = entry.getKey(); // a possible game state
			final Double gameStateProb = entry.getValue(); // corresponding state probability
			
			final RandomWalkTuple[][] rwTuplesList =  rwTuplesLists[idx];
			final AttackerAction[] attActionList = attActionLists[idx];
			final double[] attProb = attProbs[idx];

			depGraph.setState(curGameState); // temporarily set game state to the graph
			dValue += gameStateProb * computeDValue(depGraph, rwTuplesList, attActionList, attProb
				, defAction, curTimeStep, numTimeStep, discFact);
			idx++;
		}
		depGraph.setState(savedGameState);
		
		for (final Node node : defAction.getAction()) {
			dValue += node.getDCost() * Math.pow(discFact, curTimeStep - 1);
		}
		return dValue;
	}
	
	// depGraph has current game state the defender is examining
	private static double computeDValue(
		final DependencyGraph depGraph,
		final RandomWalkTuple[][] rwTuplesList,
		final AttackerAction[] attActions,
		final double[] attProb,
		final DefenderAction defAction,
		final int curTimeStep, final int numTimeStep,
		final double discFact) {
		
		final boolean[] isBlock = new boolean[depGraph.vertexSet().size()];
		for (final Node node : defAction.getAction()) {
			isBlock[node.getId() - 1] = true;
		}
		boolean[][] isInQueue = new boolean[attActions.length][depGraph.vertexSet().size()];
		for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
			final AttackerAction attAction = attActions[actionIndex];
			for (final Node node : attAction.getActionCopy().keySet()) {
				if (!isBlock[node.getId() - 1]) {
					isInQueue[actionIndex][node.getId() - 1] = true;
				}
			}
		}

		final Node[] topoOrder = Attacker.getTopoOrder(depGraph);
		double dValue = 0.0;		
		for (int actionIndex = 0; actionIndex < attActions.length; actionIndex++) {
			final RandomWalkTuple[] rwTuples = rwTuplesList[actionIndex];
			for (int topoIndex = 0; topoIndex < depGraph.vertexSet().size(); topoIndex++) {
				final Node node = topoOrder[topoIndex];
				final List<Edge> inEdges = rwTuples[node.getId() - 1].getPreAct();
				// if not set in queue yet, not root or active nodes
				if (!isInQueue[actionIndex][node.getId() - 1] && inEdges != null) {
					if (node.getActivationType() == NodeActivationType.OR) { // if OR node
						final Node parent = inEdges.get(0).getsource();
						if (isInQueue[actionIndex][parent.getId() - 1]
							&& !isBlock[node.getId() - 1]) {					
							isInQueue[actionIndex][node.getId() - 1] = true;
						}
					} else {
						boolean shouldAdd = true;
						for (final Edge inEdge : inEdges) {
							final Node parent = inEdge.getsource();
							if (!isInQueue[actionIndex][parent.getId() - 1] || isBlock[parent.getId() - 1]) {
								shouldAdd = false;
								break;
							}
						}
						if (shouldAdd) {
							isInQueue[actionIndex][node.getId() - 1] = true;
						}
					}
				}
			}
			
			for (final Node target : depGraph.getTargetSet()) {
				final int actTime = rwTuples[target.getId() - 1].getTAct();
				if (actTime <= numTimeStep && isInQueue[actionIndex][target.getId() - 1]) {
					dValue += attProb[actionIndex] * rwTuples[target.getId() - 1].getPAct() * target.getDPenalty() 
						* Math.pow(discFact, actTime - 1);
				}
			}
		}
		for (final Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.ACTIVE && !isBlock[target.getId() - 1]) {
				dValue += target.getDPenalty() * Math.pow(discFact, curTimeStep - 1);
			}
		}
		return dValue;
	}
	
	private double greedyAction(
		final DependencyGraph depGraph, 
		final int curTimeStep, 
		final int numTimeStep, 
		final DefenderBelief dBelief, 
		final RandomGenerator rng, 
		final DefenderAction defAction) { // this is outcome
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep
			|| dBelief == null || rng == null || defAction == null) {
			throw new IllegalArgumentException();
		}
		// True game state
		final GameState savedGameState = depGraph.getGameState();
		
		final int beliefSize = dBelief.getGameStateMap().size();
		final RandomWalkTuple[][][] rwTuplesAll = new RandomWalkTuple[beliefSize][][];
		final AttackerAction[][] rwAttActionAll = new AttackerAction[beliefSize][];
		final double[][] rwAttProbAll = new double[beliefSize][];
		int bIdx = 0;
		// iterate over all possible game states
		final RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		for (final GameState gameState : dBelief.getGameStateMap().keySet()) {
			depGraph.setState(gameState); // temporarily set game state to the graph
			
			// list of all random walk tuples sampled
			final RandomWalkTuple[][] rwTuplesList = new RandomWalkTuple[this.numRWSample][];
			// corresponding list of attack candidates
			final AttackerAction[] attActionList = new AttackerAction[this.numRWSample];
			final double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for (int i = 0; i < this.numRWSample; i++) {
				final RandomWalkTuple[] rwTuples =
					rwAttacker.randomWalk(depGraph, curTimeStep, rng); // sample random walk
				final AttackerAction attAction = new AttackerAction();
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
			final double[] attProb = Attacker.computeCandidateProb(attValue, this.qrParam);
			rwTuplesAll[bIdx] = rwTuplesList;
			rwAttActionAll[bIdx] = attActionList;
			rwAttProbAll[bIdx] = attProb;
			bIdx++;
		}
		// set back to the true game state
		depGraph.setState(savedGameState);
		
		// Initialize block status of nodes in the graph
		final boolean[][][] isBlock = new boolean[beliefSize][this.numRWSample][depGraph.vertexSet().size()];
		// Get topological order, starting from zero
		final Node[] topoOrder = Attacker.getTopoOrder(depGraph);
		for (bIdx = 0; bIdx < beliefSize; bIdx++) { // iterate over all possible game states
			for (int actionIndex = 0; actionIndex < this.numRWSample; actionIndex++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					isBlock[bIdx][actionIndex][k] = true;
				}
				final AttackerAction attAction = rwAttActionAll[bIdx][actionIndex];
				for (final Node node : attAction.getActionCopy().keySet()) {
					isBlock[bIdx][actionIndex][node.getId() - 1] = false;
				}
				for (int topoIndex = 0; topoIndex < depGraph.vertexSet().size(); topoIndex++) {
					final Node node = topoOrder[topoIndex];
					final RandomWalkTuple rwTuple = rwTuplesAll[bIdx][actionIndex][node.getId() - 1];
					final List<Edge> inEdges = rwTuple.getPreAct();
					if (inEdges != null && isBlock[bIdx][actionIndex][node.getId() - 1]
						&& rwTuple.getTAct() <= numTimeStep) {
						if (node.getActivationType() == NodeActivationType.OR) {
							final Node parent = inEdges.get(0).getsource();
							if (!isBlock[bIdx][actionIndex][parent.getId() - 1]) {
								isBlock[bIdx][actionIndex][node.getId() - 1] = false;
							}
						} else {
							boolean shouldKeep = false;
							for (final Edge inEdge : inEdges) {
								if (isBlock[bIdx][actionIndex][inEdge.getsource().getId() - 1]) {
									shouldKeep = true;
									break;
								}
							}
							if (!shouldKeep) {
								isBlock[bIdx][actionIndex][node.getId() - 1] = false;
							}
						}
					}
				}
			}
			bIdx++;
		}

		// Compute current value of the defender
		bIdx = 0;
		double greedyValue = 0.0;
		for (final Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			for (int j = 0; j < this.numRWSample; j++) {
				for (final Node target : depGraph.getTargetSet()) {
					if (!isBlock[bIdx][j][target.getId() - 1]) {
						greedyValue += entry.getValue() 
							* rwAttProbAll[bIdx][j]
							* target.getDPenalty() 
							* Math.pow(this.discFact, rwTuplesAll[bIdx][j][target.getId() - 1].getTAct() - 1);
					}
				}
			}
			for (final Node node : entry.getKey().getEnabledNodeSet()) {
				if (node.getType() == NodeType.TARGET) {
					greedyValue += entry.getValue() 
						* node.getDPenalty() 
						* Math.pow(this.discFact, curTimeStep - 1);
				}
			}
			bIdx++;
		}

		// Defender candidates
		final DefenderCandidate defCandidate = new DefenderCandidate();
		bIdx = 0;
		// iterate over all possible game states
		for (final GameState gameState : dBelief.getGameStateMap().keySet()) {
			for (int actionIndex = 0; actionIndex < this.numRWSample; actionIndex++) {
				for (final Node node : gameState.getEnabledNodeSet()) {
					if (node.getType() == NodeType.TARGET) {
						defCandidate.addNodeCandidate(node);
					}
				}
				
				final AttackerAction attAction = rwAttActionAll[bIdx][actionIndex];
				for (Node node : attAction.getActionCopy().keySet()) {
					defCandidate.addNodeCandidate(node);
				}
			}
			bIdx++;
		}
		final List<Node> defCandidateList = new ArrayList<Node>(defCandidate.getNodeCandidateSet());
		final boolean[] isGreedyChosen = new boolean[defCandidateList.size()];
		
		// Start greedy
		boolean isStop = false;
		final boolean[][][] isBlockClone =
			new boolean[dBelief.getGameStateMap().size()][this.numRWSample][depGraph.vertexSet().size()];
		final boolean[][][] maxIsBlock =
			new boolean[dBelief.getGameStateMap().size()][this.numRWSample][depGraph.vertexSet().size()];
		while (!isStop) {
			int dCandidateIdx = 0;
			double maxValue = 0.0;
			int maxNodeIdx = -1;
			for (final Node dCandidateNode : defCandidateList) {
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
					for (final Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
						final GameState gameState = entry.getKey();
						final Double gameStateProb = entry.getValue();
						if (dCandidateNode.getType() == NodeType.TARGET && gameState.containsNode(dCandidateNode)) {
							curValue -= dCandidateNode.getDPenalty() 
								* Math.pow(this.discFact, curTimeStep - 1)
								* gameStateProb;
						}
						for (int j = 0; j < this.numRWSample; j++) {
							isBlockClone[bIdx][j][dCandidateNode.getId() - 1] = true;
							for (int k = 0; k < depGraph.vertexSet().size(); k++) {
								final Node node = topoOrder[k];
								final List<Edge> inEdges = rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct();
								if (inEdges != null && !isBlockClone[bIdx][j][node.getId() - 1]
									&& rwTuplesAll[bIdx][j][node.getId() - 1].getTAct() <= numTimeStep) {
									if (node.getActivationType() == NodeActivationType.OR) { // OR nodes
										final Node parent = inEdges.get(0).getsource();
										if (isBlockClone[bIdx][j][parent.getId() - 1]) {
											isBlockClone[bIdx][j][node.getId() - 1] = true;
										}
									} else { // AND nodes
										for (final Edge inEdge : inEdges) {
											if (isBlockClone[bIdx][j][inEdge.getsource().getId() - 1]) {
												isBlockClone[bIdx][j][node.getId() - 1] = true;
												break;
											}
										}
									}
								}
							}
							
							for (final Node target : depGraph.getTargetSet()) {
								final RandomWalkTuple rwTuple = rwTuplesAll[bIdx][j][target.getId() - 1]; 
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

	@Override
	public String toString() {
		return "RandomWalkVsDefender [logisParam="
			+ this.logisParam + ", discFact="
			+ this.discFact + ", thres=" + this.thres
			+ ", qrParam=" + this.qrParam
			+ ", numRWSample=" + this.numRWSample + ", numStateSample="
			+ this.numStateSample + ", numAttActionSample="
			+ this.numAttActionSample
			+ ", isRandomized=" + this.isRandomized + "]";
	}
}
