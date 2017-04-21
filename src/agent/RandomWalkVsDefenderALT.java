package agent;

import game.GameOracle;
import graph.Edge;
import graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderCandidate;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import agent.RandomWalkAttacker.RandomWalkTuple;
import graph.INode.*;

public class RandomWalkVsDefenderALT extends Defender{
	double logisParam; // Logistic parameter to randomize defense strategies
	double discFact; // reward discount factor
	double thres; // to remove game state from belief
	
	double qrParam; // for the attacker
	int numRWSample = 50; // number of random walks for the attacker
	
	// number of samples to update the defender's belief.
	int numStateSample = 30; // number of states to sample
	int numAttActionSample = 30; // number of attack actions to sample
	
	/*****************************************************************************************
	 * Initialization 
	 * @param logisParam: defense parameter for randomizing defenses
	 * @param discFact: reward discount factor
	 * @param thres: threshold used to limit defender's belief
	 * @param qrParam: attack parameter for randomizing attacks
	 * @param numRWSample: number of random walk samples
	 * @param numStateSample: number of game state samples
	 * @param numAttActionSample: number of attack action samples
	 *****************************************************************************************/
	public RandomWalkVsDefenderALT(
			double logisParam, 
			double discFact, 
			double thres, 
			double qrParam, 
			int numRWSample, 
			int numStateSample, 
			int numAttActionSample) {
		this(logisParam, discFact, thres
				, qrParam, numRWSample);
		this.numStateSample = numStateSample;
		this.numAttActionSample = numAttActionSample;
	}
	
	/*****************************************************************************************
	 * Initialization
	 * @param logisParam: defense parameter for randomizing defenses
	 * @param discFact: reward discount factor
	 * @param thres: threshold used to limit defender's belief
	 * @param qrParam: attack parameter for randomizing attacks
	 * @param numRWSample: number of random walk samples
	 *****************************************************************************************/
	public RandomWalkVsDefenderALT(
			double logisParam, 
			double discFact, 
			double thres, 
			double qrParam, 
			double numRWSample) {
		super(DefenderType.vsRANDOM_WALK);
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.numRWSample = (int)numRWSample;
	}

	@Override
	public DefenderAction sampleAction(
			DependencyGraph depGraph,
			int curTimeStep, 
			int numTimeStep, 
			DefenderBelief dBelief,
			RandomGenerator rng) {
		// TODO Auto-generated method stub
		DefenderAction defAction = new DefenderAction();
		greedyAction(depGraph, curTimeStep, numTimeStep, dBelief, rng, defAction);
		return defAction;
	}

	@Override
	/*****************************************************************************************
	 * Update defender's belief 
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
			if(node.getState() == NodeState.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		
		DefenderBelief newBelief = new DefenderBelief(); // new belief of the defender
		Map<GameState, Double> observationProbMap = new HashMap<GameState, Double>(); // probability of observation given game state
		
		Attacker attacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over current belief of the defender
		{
			GameState gameState = entry.getKey(); // one of possible game state
			Double curStateProb = entry.getValue(); // probability of the game state
		
			depGraph.setState(gameState); // for each possible state
			
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // sample attacker actions
			
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
						observationProb = observationProbMap.get(newGameState);
					
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
	
	public double greedyAction(
			DependencyGraph depGraph, 
			int curTimeStep, 
			int numTimeStep, 
			DefenderBelief dBelief, 
			RandomGenerator rng, 
			DefenderAction defAction){ // this is outcome
		double greedyValue = 0.0;
		RandomWalkAttacker rwAttacker = new RandomWalkAttacker(this.numRWSample, this.qrParam, this.discFact);
		// True game state
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NodeState.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for(Node node : depGraph.vertexSet())
			topoOrder[node.getTopoPosition()] = node;
		
		int beliefSize = dBelief.getGameStateMap().size();
		RandomWalkTuple[][][] rwTuplesAll = new RandomWalkTuple[beliefSize][][];
		AttackerAction[][] rwAttActionAll = new AttackerAction[beliefSize][];
		double[][] rwAttProbAll = new double[beliefSize][];
		int bIdx = 0;
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()){ // iterate over all possible game states
			GameState gameState = entry.getKey(); // a possible game state
			depGraph.setState(gameState); // temporarily set game state to the graph
			
			RandomWalkTuple[][] rwTuplesList = new RandomWalkTuple[this.numRWSample][]; // list of all random walk tuples sampled
			AttackerAction[] attActionList = new AttackerAction[this.numRWSample]; // corresponding list of attack candidates
			double[] attValue = new double[this.numRWSample]; // values of corresponding action of the attacker
			for(int i = 0; i < this.numRWSample; i++){
				RandomWalkTuple[] rwTuples = rwAttacker.randomWalk(depGraph, curTimeStep, rng); // sample random walk
				AttackerAction attAction = new AttackerAction();
				attValue[i] = RandomWalkAttacker.greedyAction(
									depGraph, // greedy attack
									rwTuples, 
									attAction, // attAction is an outcome as well
									numTimeStep, 
									this.discFact); 
				rwTuplesList[i]= rwTuples;
				attActionList[i] = attAction;
			}
			double[] attProb = RandomWalkAttacker.computeCandidateProb(this.numRWSample, attValue, this.qrParam); // attack probability
			
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
		for(bIdx = 0; bIdx < beliefSize; bIdx++) { // iterate over all possible game states
			for(int j = 0; j < this.numRWSample; j++) {
				for(int k = 0; k < depGraph.vertexSet().size(); k++){
					isBlock[bIdx][j][k] = true;
				}
				AttackerAction attAction = rwAttActionAll[bIdx][j];
				for(Entry<Node, Set<Edge>> entry : attAction.getAction().entrySet())
					isBlock[bIdx][j][entry.getKey().getId() - 1] = false;
				for(int k = 0; k < depGraph.vertexSet().size(); k++) {
					Node node = topoOrder[k];
					if(rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct() != null && isBlock[bIdx][j][node.getId() - 1]
							&& rwTuplesAll[bIdx][j][node.getId() - 1].getTAct() <= numTimeStep) {
						if(node.getActivationType() == NodeActivationType.OR) {
							Node preNode = rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct().get(0).getsource();
							if(!isBlock[bIdx][j][preNode.getId() - 1])
								isBlock[bIdx][j][node.getId() - 1] = false;
						}
						else {
							boolean tempBlock = false;
							for(Edge edge : rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct()) {
								if(isBlock[bIdx][j][edge.getsource().getId() - 1]) {
									tempBlock = true;
									break;
								}
							}
							if(!tempBlock) {
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
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) {
			for(int j = 0; j < this.numRWSample; j++) {
				for(Node target : depGraph.getTargetSet())
				if(!isBlock[bIdx][j][target.getId() - 1]) {
					greedyValue += entry.getValue() 
							* rwAttProbAll[bIdx][j]
							* target.getDPenalty() 
							* Math.pow(this.discFact, rwTuplesAll[bIdx][j][target.getId() - 1].getTAct() - 1);
				}
			}
			for(Node node : entry.getKey().getEnabledNodeSet()) {
				if(node.getType() == NodeType.TARGET) {
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
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over all possible game states
		{
			GameState gameState = entry.getKey(); // a possible game state
			for(int j = 0; j < this.numRWSample; j++)
			{
				for(Node node : gameState.getEnabledNodeSet())
				{
					if(node.getType() == NodeType.TARGET)
						defCandidate.addNodeCandidate(node);
				}
				
				AttackerAction attAction = rwAttActionAll[bIdx][j];
				for(Entry<Node, Set<Edge>> attActionEntry : attAction.getAction().entrySet())
					defCandidate.addNodeCandidate(attActionEntry.getKey());
			}
			bIdx++;
		}
//		defCandidate.print();
		List<Node> defCandidateList = new ArrayList<Node>(defCandidate.getNodeCandidateSet());
		boolean[] isGreedyChosen = new boolean[defCandidateList.size()];
		for(int i = 0; i < defCandidateList.size(); i++)
			isGreedyChosen[i] = false;
		
		// Start greedy
		boolean isStop = false;
		while(!isStop)
		{
			System.out.println("Greedy process....");
			int dCandidateIdx = 0;
			double maxValue = 0.0;
			int maxNodeIdx = -1;
			boolean[][][] maxIsBlock = null;
			for(Node dCandidateNode : defCandidateList)
			{
				if(!isGreedyChosen[dCandidateIdx]) // Examining this node
				{
					double curValue = dCandidateNode.getDCost() * Math.pow(this.discFact, curTimeStep - 1);
					boolean[][][] isBlockClone = new boolean[dBelief.getGameStateMap().size()][this.numRWSample][depGraph.vertexSet().size()];
					for(int i = 0; i < dBelief.getGameStateMap().size(); i++)
						for(int j = 0; j < this.numRWSample; j++)
							for(int k = 0; k < depGraph.vertexSet().size(); k++)
								isBlockClone[i][j][k] = isBlock[i][j][k];
					bIdx = 0;
					for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over all possible game states
					{
						GameState gameState = entry.getKey();
						Double gameStateProb = entry.getValue();
						if(dCandidateNode.getType() == NodeType.TARGET && gameState.containsNode(dCandidateNode))
							curValue -= dCandidateNode.getDPenalty() 
								* Math.pow(this.discFact, curTimeStep - 1)
								* gameStateProb;
						for(int j = 0; j < this.numRWSample; j++)
						{
							isBlockClone[bIdx][j][dCandidateNode.getId() - 1] = true;
							for(int k = 0; k < depGraph.vertexSet().size(); k++)
							{
								Node node = topoOrder[k];
								if(rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct() != null && !isBlockClone[bIdx][j][node.getId() - 1]
										&& rwTuplesAll[bIdx][j][node.getId() - 1].getTAct() <= numTimeStep)
								{
									if(node.getActivationType() == NodeActivationType.OR) // OR nodes
									{
										Node preNode = rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct().get(0).getsource();
										if(isBlockClone[bIdx][j][preNode.getId() - 1])
											isBlockClone[bIdx][j][node.getId() - 1] = true;
									}
									else // AND nodes
									{
										for(Edge edge : rwTuplesAll[bIdx][j][node.getId() - 1].getPreAct())
										{
											if(isBlockClone[bIdx][j][edge.getsource().getId() - 1])
											{
												isBlockClone[bIdx][j][node.getId() - 1] = true;
												break;
											}
										}
									}
								}
							}
							
							for(Node target : depGraph.getTargetSet())
							{
								RandomWalkTuple rwTuple = rwTuplesAll[bIdx][j][target.getId() - 1]; 
								if(isBlockClone[bIdx][j][target.getId() - 1] 
										&& !isBlock[bIdx][j][target.getId() - 1]
												&& rwTuple.getTAct() <= numTimeStep)
								{
									curValue -= gameStateProb * rwAttProbAll[bIdx][j]
											* rwTuple.getPAct() 
											* Math.pow(this.discFact, rwTuple.getTAct() - 1)
											* target.getDPenalty();
								}
							}
						}
						
						bIdx++;
					}
					System.out.println(curValue);
					if(curValue > maxValue)
					{
						maxValue = curValue;
						maxNodeIdx = dCandidateIdx;
						maxIsBlock = isBlockClone;
					}
					
				}
				dCandidateIdx++;
			}
			if(maxNodeIdx != -1)
			{
				greedyValue += maxValue;
				isGreedyChosen[maxNodeIdx] = true;
				isBlock = maxIsBlock;
				defAction.addNodetoProtect(defCandidateList.get(maxNodeIdx));
			}
			else
				isStop = true;
		}
		
		return greedyValue;
	}

}
