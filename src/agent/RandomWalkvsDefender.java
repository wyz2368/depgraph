package agent;


import game.GameOracle;
import graph.Edge;
import graph.Node;
import graph.INode.NODE_ACTIVATION_TYPE;
import graph.INode.NODE_STATE;

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

public class RandomWalkvsDefender extends Defender{
	double logisParam; // Logistic parameter to randomize defense strategies
	double discFact; // reward discount factor
	double thres; // to remove game state from belief
	
	double qrParam; // for the attacker
	int numRWSample = 100; // number of random walks for the attacker
	
	// number of samples to update the defender's belief.
	int numStateSample = 50; // number of states to sample
	int numAttActionSample = 50; // number of attack actions to sample
	
	public RandomWalkvsDefender(double logisParam, double discFact, double thres
			, double qrParam, int numRWSample
			, int numStateSample, int numAttActionSample) {
		this(logisParam, discFact, thres
				, qrParam, numRWSample);
		this.numStateSample = numStateSample;
		this.numAttActionSample = numAttActionSample;
		// TODO Auto-generated constructor stub
	}
	public RandomWalkvsDefender(double logisParam, double discFact, double thres
			, double qrParam, int numRWSample) {
		super(DEFENDER_TYPE.vsRANDOM_WALK);
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.numRWSample = numRWSample;
	}
	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph with true game state
	 * @param curTimeStep: current time step of the game
	 * @param numTimeStep: total number of time steps
	 * @param dBelief: belief of the defender over possible game states
	 * @param rng: random generator
	 * @return: action for the defender
	 *****************************************************************************************/
	@Override
	public DefenderAction sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep, DefenderBelief dBelief, RandomGenerator rng) {
		// True game state
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		
		// Compute value of each candidate action for the defender
		double[] candidateValues = new double[dBelief.getGameStateMap().size()];
		DefenderAction[] candidates = new DefenderAction[dBelief.getGameStateMap().size()];
		
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, curTimeStep, numTimeStep);
		
		int idx = 0;
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over all possible game states
		{
			GameState gameState = entry.getKey();
			Double gameStateProb = entry.getValue();
			depGraph.setState(gameState);
			
			List<RandomWalkTuple[]> rwTuplesList = new ArrayList<RandomWalkTuple[]>(); // list of all random walk tuples sampled
			List<AttackCandidate> attCandidateList = new ArrayList<AttackCandidate>(); // corresponding list of attack candidates
			double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for(int i = 0; i < this.numRWSample; i++)
			{
				RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(depGraph, curTimeStep, numTimeStep, rng); // sample random walk
				AttackCandidate attCandidate = new AttackCandidate();
				attValue[i] = RandomWalkAttacker.greedyCandidate(depGraph // greedy attack
						, rwTuples, attCandidate
						, numTimeStep, this.discFact); 
				rwTuplesList.add(rwTuples);
				attCandidateList.add(attCandidate);
			}
			DefenderAction defAction = new DefenderAction();
			double[] attProb = computecandidateProb(this.numRWSample, attValue, this.qrParam); // attack probability
			double dValue = greedyCandidate(depGraph, rwAttacker // greedy defense with respect to each possible game state
					, rwTuplesList, attCandidateList, attProb
					, defAction // this is outcome
					, curTimeStep, numTimeStep
					, this.discFact);
		
			candidateValues[idx] = dValue * gameStateProb ;
			candidates[idx] = defAction;
			idx++;
		}
		// set back to the true game state
		depGraph.setState(savedGameState);
		
		// probability for each possible candidate action for the defender
		
		double[] probabilities = computecandidateProb(dBelief.getGameStateMap().size(), candidateValues, this.logisParam);
		if(dBelief.getGameStateMap().size() == 0)
			System.out.println("Belief is empty???");
		
		// Start sampling
		int[] nodeIndexes = new int[dBelief.getGameStateMap().size()];
		for(int i = 0; i < dBelief.getGameStateMap().size(); i++)
			nodeIndexes[i] = i;
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		int sampleIdx = rnd.sample();
//		candidates[sampleIdx].print();
		return candidates[sampleIdx];
	}
	@Override
	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph with true game state
	 * @param dBelief: belief of the defender over states
	 * @param dAction: action of the defender
	 * @param dObservation: observations of the defender
	 * @param curTimeStep: current time step
	 * @param numTimeStep: total number of time steps
	 * @param rng: Random Generator
	 * @return new belief of the defender
	 *****************************************************************************************/
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
		
		Attacker attacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over current belief of the defender
		{
			GameState gameState = (GameState) entry.getKey(); // one of possible game state
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
//		System.out.println("Revised  belief size: " + revisedBelief.getGameStateMap().size());
//		for(Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet())
//		{
//			System.out.println(entry.getValue());
//		}
		return revisedBelief;
	}
	
	/*****************************************************************************************
	 * Note: defCandidate: outcome of greedy, need to be pre-initialized*
	 * @param depGraph: dependency graph with game state which is being examined by the defender
	 * @param rwAttacker: random walk attacker
	 * @param rwTuplesList: list of random walk tuples 
	 * @param attCandidateList: corresponding list of attack candidates
	 * @param defAction: defender action
	 * @param numTimeStep: total number of time step
	 * @param discFact: reward discount factor
	 * @return
	 *****************************************************************************************/
	public double greedyCandidate(DependencyGraph depGraph // depGraph has current game state the defender is examining
			, RandomWalkAttacker rwAttacker
			, List<RandomWalkTuple[]> rwTuplesList
			, List<AttackCandidate> attCandidateList
			, double[] attProb
			, DefenderAction defAction
			, int curTimeStep, int numTimeStep
			, double discFact)
	{
		double value = 0.0;
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for(Node node : depGraph.vertexSet())
			topoOrder[node.getTopoPosition()] = node;
		
		DefenderCandidate defCandidateAll = new DefenderCandidate(); // all candidate nodes for the defender
		for(AttackCandidate attCandidate : attCandidateList) // all these are inactive
		{
			for(Node node : attCandidate.getNodeCandidateSet())
				defCandidateAll.addNodeCandidate(node);
			for(Edge edge : attCandidate.getEdgeCandidateSet())
				defCandidateAll.addNodeCandidate(edge.gettarget());
		}
		for(Node target : depGraph.getTargetSet()) // active targets
		{
			if(target.getState() == NODE_STATE.ACTIVE)
				defCandidateAll.addNodeCandidate(target);
		}
		
		List<Node> defCandidateListAll = new ArrayList<Node>(defCandidateAll.getNodeCandidateSet());
		boolean[] isChosen = new boolean[defCandidateListAll.size()];
		for(int i = 0; i < defCandidateListAll.size(); i++)
			isChosen[i] = false;
		if(attCandidateList.isEmpty())
		{
			System.out.println("Attacker candidate is empty");
		}
		boolean[][] isInQueue = new boolean[attCandidateList.size()][depGraph.vertexSet().size()];
		// Initialize queue, this queue is used for checking if any node is still in the queue of activating
		// when the defender starts disabling nodes
		for(int i = 0; i < attCandidateList.size(); i++)
		{
			for(int j = 0; j < depGraph.vertexSet().size(); j++)
			{
				isInQueue[i][j] = false;
			}
		}
		
		int idx = 0;
		for(AttackCandidate attCandidate : attCandidateList)
		{
			for(Node node : attCandidate.getNodeCandidateSet())
				isInQueue[idx][node.getId() - 1] = true;
			for(Edge edge : attCandidate.getEdgeCandidateSet())
				isInQueue[idx][edge.gettarget().getId() - 1] = true;
			idx++;
		}
		for(idx = 0; idx < attCandidateList.size(); idx++)
		{
			for(int i = 0; i < depGraph.vertexSet().size(); i++)
			{
				Node node = topoOrder[i];
				if(!isInQueue[idx][node.getId() - 1] && depGraph.inDegreeOf(node) > 0) // if not set in queue yet and not root node
				{
					if(node.getActivationType() == NODE_ACTIVATION_TYPE.OR) // if OR node
					{
						Node preNode = rwTuplesList.get(idx)[node.getId() - 1].getPreAct().get(0).getsource();
						if(isInQueue[idx][preNode.getId() - 1])
							isInQueue[idx][node.getId() - 1] = true;
					}
					else
					{
						boolean temp = true;
						for(Edge preEdge : rwTuplesList.get(idx)[node.getId() - 1].getPreAct())
						{
							if(!isInQueue[idx][preEdge.getsource().getId() - 1])
							{
								temp = false;
								break;
							}
						}
						if(temp)
							isInQueue[idx][node.getId() - 1] = true;
					}
				}
			}
		}
		
		// Start greedy process
		boolean isStop = false;
		while(!isStop) // only stop when no new candidate node can increase the defender's utility
		{
			isStop = true;
			int candidateIdx = 0;
			double maxValue = Double.NEGATIVE_INFINITY; // best value of the defender
			boolean[][] maxQueue = null; // corresponding queue
			Node maxCandidateNode = null; // best node to add
			int maxIdx = -1;
			for(Node dCandidateNode : defCandidateListAll) // iterate over candidate nodes of the defender
			{
				double curValue = 0.0;
				if(!isChosen[candidateIdx]) // if not chosen yet, start examining
				{
					boolean[][] isInCurrentQueue = isInQueue.clone(); // used to examine new node
					idx = 0;
					for(RandomWalkTuple[] rwTuples : rwTuplesList) // iterate over tuples
					{
						List<Node> queue = new ArrayList<Node>();
						queue.add(dCandidateNode);
						isInCurrentQueue[idx][dCandidateNode.getId() - 1] = false;
						while(!queue.isEmpty()) // forward tracking
						{
							Node node = queue.remove(0);
							for(Edge postEdge : depGraph.outgoingEdgesOf(node))
							{
								Node postNode = postEdge.gettarget();
								if(isInCurrentQueue[idx][postNode.getId() - 1]) // if this postNode is in the current queue
								{
									if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
									{
										Node preNode = rwTuples[postNode.getId() - 1].getPreAct().get(0).getsource();
										if(!isInCurrentQueue[idx][preNode.getId() - 1])
										{
											isInCurrentQueue[idx][postNode.getId() - 1] = false;
											queue.add(postNode);
										}
									}
									else
									{
										boolean temp = true;
										for(Edge edge : rwTuples[postNode.getId() - 1].getPreAct())
										{
											Node preNode = edge.getsource();
											if(!isInCurrentQueue[idx][preNode.getId() - 1])
											{
												temp = false;
												break;
											}
										}
										if(!temp)
										{
											isInCurrentQueue[idx][postNode.getId() - 1] = false;
											queue.add(postNode);
										}
									}
								}
							}
						}
						for(Node target : depGraph.getTargetSet())
						{
							int actTime = rwTuples[target.getId() - 1].getTAct();
							if(actTime <= numTimeStep && isInCurrentQueue[idx][target.getId() - 1])
								curValue += attProb[idx] * rwTuples[target.getId() - 1].getPAct() * target.getDPenalty() 
												* Math.pow(discFact, actTime - 1);
						}
						idx++;
					}
					double tempValue = curValue + dCandidateNode.getDCost() * Math.pow(discFact, curTimeStep - 1);
//					System.out.println("Temp value: " + tempValue);
					if(maxValue < tempValue)
					{
						maxValue = tempValue;
						maxQueue = isInCurrentQueue;
						maxCandidateNode = dCandidateNode;
						maxIdx = candidateIdx;
						isStop = false;
					}
				}
				candidateIdx++;
			}
			if(!isStop)
			{
				isInQueue = maxQueue;
				defAction.addNodetoProtect(maxCandidateNode);
				value = maxValue;
				isChosen[maxIdx] = true;
			}
		}
		
		return value;
	}
	public static double[] computecandidateProb(int totalNumCandidate, double[] candidateValue, double logisParam)
	{
		//Normalize candidate value
		double minValue = Double.POSITIVE_INFINITY;
		double maxValue = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < totalNumCandidate; i++)
		{
			if(minValue > candidateValue[i])
				minValue = candidateValue[i];
			if(maxValue < candidateValue[i])
				maxValue = candidateValue[i];
		}
		if(maxValue > minValue)
		{
			for(int i = 0; i < totalNumCandidate; i++)
				candidateValue[i] = (candidateValue[i] - minValue) / (maxValue - minValue);
		}
		else 
		{
			for(int i = 0; i < totalNumCandidate; i++)
				candidateValue[i] = 0.0;
		}
		
		// Compute probability
		double[] probabilities = new double[totalNumCandidate];
		int[] nodeList = new int[totalNumCandidate];
		double sumProb = 0.0;
		for(int i = 0; i < totalNumCandidate; i++)
		{
			nodeList[i] = i;
			probabilities[i] = Math.exp(logisParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for(int i = 0; i < totalNumCandidate; i++)
			probabilities[i] /= sumProb;
		
		return probabilities;
	}
	
}
