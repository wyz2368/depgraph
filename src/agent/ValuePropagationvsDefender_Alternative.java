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
import java.util.Set;
import java.util.Map.Entry;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class ValuePropagationvsDefender_Alternative extends Defender{
	int maxNumRes;
	int minNumRes;
	double numResRatio;
	double logisParam;
	double discFact;
	double thres; // to remove game state from belief
	
	// defender's assumption abt attacker
	double qrParam; 
	int maxNumAttCandidate; 
	int minNumAttCandidate;
	double numAttCandidateRatio;
	double propagationParam = 0.5;
	
	// number of simulation to approximate update
	int numStateSample = 50;
	int numAttActionSample = 50;
	
	/*****************************************************************************************
	 * 
	 * @param maxNumRes
	 * @param minNumRes
	 * @param numResRatio
	 * @param logisParam
	 * @param discFact
	 * @param thres
	 * @param qrParam
	 * @param maxNumAttCandidate
	 * @param minNumAttCandidate
	 * @param numAttCandidateRatio
	 *****************************************************************************************/
	public ValuePropagationvsDefender_Alternative(int maxNumRes, int minNumRes, double numResRatio
			, double logisParam, double discFact, double thres
			, double qrParam, int maxNumAttCandidate, int minNumAttCandidate, double numAttCandidateRatio) {
		super(DEFENDER_TYPE.vsVALUE_PROPAGATION);
		
		this.maxNumRes = maxNumRes;
		this.minNumRes = minNumRes;
		this.numResRatio = numResRatio;
		this.logisParam = logisParam;
		this.discFact = discFact;
		this.thres = thres;
		
		this.qrParam = qrParam;
		this.maxNumAttCandidate = maxNumAttCandidate;
		this.minNumAttCandidate = minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
		// TODO Auto-generated constructor stub
	}
	
	/*****************************************************************************************
	 * 
	 * @param maxNumRes
	 * @param minNumRes
	 * @param numResRatio
	 * @param logisParam
	 * @param discFact
	 * @param thres
	 * @param qrParam
	 * @param maxNumAttCandidate
	 * @param minNumAttCandidate
	 * @param numAttCandidateRatio
	 * @param numStateSample
	 * @param numAttActionSample
	 *****************************************************************************************/
	public ValuePropagationvsDefender_Alternative(int maxNumRes, int minNumRes, double numResRatio
			, double logisParam, double discFact, double thres
			, double qrParam, int maxNumAttCandidate, int minNumAttCandidate, double numAttCandidateRatio
			, int numStateSample, int numAttActionSample) {
		this(maxNumRes, minNumRes, numResRatio
				, logisParam, discFact, thres
				, qrParam, maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio);
		// TODO Auto-generated constructor stub
		this.numStateSample = numStateSample;
		this.numAttActionSample = numAttActionSample;
	}

	@Override
	public DefenderAction sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep
			, DefenderBelief dBelief
			, RandomGenerator rng) {
		// TODO Auto-generated method stub
		Map<Node, Double> dValueMap = computeCandidateValueTopo(depGraph, dBelief, curTimeStep, numTimeStep
				, this.discFact, rng);
		List<Node> dCandidateNodeList = new ArrayList<Node>();
		double[] candidateValue = new double[dValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for(Entry<Node, Double> entry : dValueMap.entrySet())
		{
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		int totalNumCandidate = dValueMap.size();
		
		
		// Compute probability to choose each node
		double[] probabilities = computecandidateProb(totalNumCandidate, candidateValue, this.logisParam);

		// Only keep candidates with high probability
		int numGoodCandidate = 0;
		for(int i = 0; i < totalNumCandidate; i++)
			if(probabilities[i] >= this.thres)
				numGoodCandidate++;
		// Compute number of candidates to select
		int numNodetoProtect = 0;
		if(dCandidateNodeList.size() < this.minNumRes)
			numNodetoProtect = dCandidateNodeList.size();
		else 
		{
			numNodetoProtect = Math.max(this.minNumRes, (int)(this.numResRatio * dCandidateNodeList.size()));
			numNodetoProtect = Math.min(this.maxNumRes, numNodetoProtect);
		}
		if(numNodetoProtect > numGoodCandidate)
			numNodetoProtect = numGoodCandidate;
		
		if(numNodetoProtect == 0) // if there is no candidate
			return new DefenderAction();
		
		// Sampling
		int[] nodeIndexes = new int[dCandidateNodeList.size()];
		for(int i = 0; i < dCandidateNodeList.size(); i++)
			nodeIndexes[i] = i;
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return sampleAction(dCandidateNodeList, numNodetoProtect, rnd);
	}
	@Override
	public DefenderBelief updateBelief(DependencyGraph depGraph,
			DefenderBelief dBelief, DefenderAction dAction,
			DefenderObservation dObservation, int curTimeStep, int numTimeStep,
			RandomGenerator rng) {
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
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
				, this.numAttCandidateRatio, this.qrParam, this.discFact);
		
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
//				attAction.print();
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
						, rnd, this.numStateSample, true); // s' <- s, a, d, // Sample new game states
				int curNumStateSample = gameStateList.size();
				for(int stateSample = 0; stateSample < curNumStateSample; stateSample++)
				{
					GameState newGameState = gameStateList.get(stateSample);
//					System.out.println("New game state");
//					newGameState.print();
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
//			System.out.println(entry.getValue());
			if(entry.getValue() > this.thres)
				revisedBelief.addState(entry.getKey(), entry.getValue());
		}
//		newBelief.clear();
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
	
	public Map<Node, Double> computeCandidateValueTopo(DependencyGraph depGraph
			, DefenderBelief dBelief
			, int curTimeStep, int numTimeStep, double discountFactor
			, RandomGenerator rng)
	{
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
				, this.numAttCandidateRatio, this.qrParam, this.discFact); // assumption about the attacker
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		System.out.println("Start defender belief:........");
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over current belief of the defender
		{
			GameState gameState = entry.getKey();
			gameState.print();
			System.out.println("Prob: " + entry.getValue());
			Double curStateProb = entry.getValue();
			
			depGraph.setState(gameState); // for each possible state
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // Sample attacker actions
			Map<Node, Double> curDValueMap = computeCandidateValueTopo(depGraph, attActionList
					, curTimeStep, numTimeStep, discountFactor);
			for(Entry<Node, Double> dEntry : curDValueMap.entrySet())
			{
				Node node = dEntry.getKey();
				Double value = dEntry.getValue();
				
				Double curDValue = dValueMap.get(node);
				if(curDValue == null)
				{
					curDValue = value * curStateProb;
				}
				else
				{
					curDValue += value * curStateProb;
				}
				dValueMap.put(node, curDValue);
			}
		}
		System.out.println("End defender belief:........");
		for(Entry<Node, Double> entry : dValueMap.entrySet())
		{
			Node node = entry.getKey();
			Double value = entry.getValue();
			if(value == Double.POSITIVE_INFINITY)
				value = 0.0;
			entry.setValue((-value + node.getDCost()) * Math.pow(discountFactor, curTimeStep - 1));
		}
		depGraph.setState(savedGameState);
		return dValueMap;
	}
	
	public static Map<Node, Double> computeCandidateValueTopo(DependencyGraph depGraph
			, List<AttackerAction> attActionList
			, int curTimeStep, int numTimeStep, double discountFactor
			)
	{
//		System.out.println("Defender compute candidate value");
		Map<Node, Double> dValueMap = new HashMap<Node, Double>();
		
		// Compute values for each node in the graph
		List<Node> targetList = new ArrayList<Node>(depGraph.getTargetSet()); // list of targets
		Node[] topoOrder = new Node[depGraph.vertexSet().size()]; // topological order of nodes in the graph

		for(Node node : depGraph.vertexSet())
		{
			topoOrder[node.getTopoPosition()] = node;
		}
		
		double[][][] r = new double[targetList.size()][numTimeStep - curTimeStep + 1][depGraph.vertexSet().size()];
		for(int i = 0; i < targetList.size(); i++)
		{
			for(int j = 0; j <= numTimeStep - curTimeStep; j++)
			{
				for(int k = 0; k < depGraph.vertexSet().size(); k++)
				{
					r[i][j][k] = Double.POSITIVE_INFINITY;
				}
			}
		}
		for(int i = 0; i < targetList.size(); i++)
		{
			Node node = targetList.get(i);
			if(node.getState() != NODE_STATE.ACTIVE) // for non-active targets only
				r[i][0][node.getId() - 1] = node.getDPenalty();
		}
		for(int k = depGraph.vertexSet().size() - 1; k >= 0; k--) // starting propagate values for the defender 
		{
			Node node = topoOrder[k];

			Set<Edge> edgeSet = depGraph.outgoingEdgesOf(node);
			if(edgeSet != null && !edgeSet.isEmpty()) // if non-leaf
			{
				for(Edge edge : edgeSet)
				{
					Node postNode = edge.gettarget();
					if(postNode.getState() != NODE_STATE.ACTIVE)
					{
						for(int i = 0; i < targetList.size(); i++)
						{
							if(targetList.get(i).getState() != NODE_STATE.ACTIVE)
							for(int j = 1; j <= numTimeStep - curTimeStep; j++)
							{
								double r_hat = 0.0;
								if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
								{
									r_hat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
								}
								else
								{
									r_hat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
								}
								if(r[i][j][node.getId() - 1] > discountFactor * r_hat)
									r[i][j][node.getId() - 1] = discountFactor * r_hat;
							}
						}
					}
				}
			}
		
		}
		// Sum of value for candidates
		double[] rSum = new double[depGraph.vertexSet().size()];
		for(int i = 0; i < depGraph.vertexSet().size(); i++)
			rSum[i] = Double.POSITIVE_INFINITY;
		for(int i = 0; i < targetList.size(); i++)
		{
			if(targetList.get(i).getState() != NODE_STATE.ACTIVE)
			for(int j = 0; j <= numTimeStep - curTimeStep; j++)
			{
				for(int k = 0; k < depGraph.vertexSet().size(); k++)
					if(rSum[k] > r[i][j][k])
						rSum[k] = r[i][j][k];
			}
		}
		
		/*****************************************************************************************/
		for(AttackerAction attAction : attActionList)
		{
//			attAction.print();
			for(Entry<Node, Set<Edge>> attEntry : attAction.getAction().entrySet())
			{
				Node node = attEntry.getKey();
				Set<Edge> edgeSet = attEntry.getValue();
				
				double addedDValue = rSum[node.getId() - 1];
				double actProb = 1.0;
				if(node.getActivationType() == NODE_ACTIVATION_TYPE.OR)
				{ 
					for(Edge edge : edgeSet)
						actProb *= (1 - edge.getActProb());
					actProb = 1 - actProb;
				}
				else
					actProb *= node.getActProb();
				addedDValue *= actProb;
				
				Double curDValue = dValueMap.get(node);
				if(curDValue == null) // if this is new
					curDValue = addedDValue;
				else
					curDValue += addedDValue;
				dValueMap.put(node, curDValue);
			}
		}
		for(Entry<Node, Double> entry : dValueMap.entrySet())
		{
			double value = entry.getValue();
			entry.setValue(value / attActionList.size());
		}
		
		for(Node target : depGraph.getTargetSet())
		{
			if(target.getState() == NODE_STATE.ACTIVE)
			{
//				System.out.println("This is active target...........");
				double dValue = target.getDPenalty();
				if(rSum[target.getId() - 1] != Double.POSITIVE_INFINITY)
					dValue += rSum[target.getId() - 1];
				dValueMap.put(target, dValue);
			}
		}
		return dValueMap;
	}
	
	public static DefenderAction sampleAction(List<Node> dCandidateNodeList, int numNodetoProtect,
			AbstractIntegerDistribution rnd)
	{
		DefenderAction action = new DefenderAction();
		
		boolean[] isChosen = new boolean[dCandidateNodeList.size()];
		for(int i = 0; i < dCandidateNodeList.size(); i++)
			isChosen[i] = false;
		int count = 0;
		while(count < numNodetoProtect)
		{
			int idx = rnd.sample();
			if(!isChosen[idx])
			{
				action.addNodetoProtect(dCandidateNodeList.get(idx));
				isChosen[idx] = true;
				count++;
			}
				
		}
		return action;
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
