package agent;

import game.GameOracle;
import graph.Edge;
import graph.INode.NODE_TYPE;
import graph.Node;
import graph.INode.NODE_ACTIVATION_TYPE;
import graph.INode.NODE_STATE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import model.AttackCandidate;
import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderCandidate;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameState;

public class ValuePropagationvsDefender extends Defender{
	public ValuePropagationvsDefender(int maxNumRes, int minNumRes, double numResRatio
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
	public ValuePropagationvsDefender(int maxNumRes, int minNumRes, double numResRatio
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
	
	@Override
	public DefenderAction sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep, DefenderBelief dBelief, RandomGenerator rng) {
		
		// Compute value of each candidate node for the defender
		Map<Node, Double> dCandidateValueMap = computeCandidateValueTopo(depGraph, dBelief
				, curTimeStep, numTimeStep, this.discFact, this.propagationParam);
		
		List<Node> dCandidateNodeList = new ArrayList<Node>();
		double[] candidateValue = new double[dCandidateValueMap.size()];
		
		// Get candidate list with values for sampling
		int idx = 0;
		for(Entry<Node, Double> entry : dCandidateValueMap.entrySet())
		{
			dCandidateNodeList.add(entry.getKey());
			candidateValue[idx] = entry.getValue();
			idx++;
		}
		
		int totalNumCandidate = dCandidateValueMap.size();
		
		
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

		return sampleAction(depGraph, dCandidateNodeList, numNodetoProtect, rnd);
	}

	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph 
	 * @param dBelief: belief of the defender over states
	 * @param dAction: action of the defender
	 * @param dObservation: observation of the defender
	 * @param curTimeStep: current time step
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @return type of DefenderBelief: new belief of the defender
	 *****************************************************************************************/
	public DefenderBelief updateBelief(DependencyGraph depGraph, DefenderBelief dBelief
			, DefenderAction dAction, DefenderObservation dObservation
			, int curTimeStep, int numTimeStep
			, RandomGenerator rng)
	{
		RandomDataGenerator rnd = new RandomDataGenerator(rng);
		
		// Used for storing true game state of the game
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				savedGameState.addEnabledNode(node);
		}
		
		DefenderBelief newBelief = new DefenderBelief(); // new belief of the defender
		Map<GameState, Double> observationProbMap = new HashMap<GameState, Double>(); // observation prob given game state
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
				, this.numAttCandidateRatio, this.qrParam, this.discFact); // assumption about the attacker
		
		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet()) // iterate over current belief of the defender
		{
			GameState gameState = entry.getKey();
			Double curStateProb = entry.getValue();
			
			depGraph.setState(gameState); // for each possible state
			List<AttackerAction> attActionList = attacker.sampleAction(depGraph, curTimeStep, numTimeStep
					, rng, this.numAttActionSample, false); // Sample attacker actions
			
			for(int attActionSample = 0; attActionSample < this.numAttActionSample; attActionSample++)
			{
				AttackerAction attAction = attActionList.get(attActionSample);
//				attAction.print();
				// Sample states
				List<GameState> gameStateList = GameOracle.generateStateSample(gameState, attAction, dAction
						, rnd, this.numStateSample, true); // s' <- s, a, d
				int curNumStateSample = gameStateList.size();
				for(int stateSample = 0; stateSample < curNumStateSample; stateSample++)
				{
					GameState newGameState = gameStateList.get(stateSample);
					Double curProb = newBelief.getProbability(newGameState);
					double observationProb = 0.0;
					if(curProb == null) // new game state
					{
						observationProb = GameOracle.computeObservationProb(newGameState, dObservation);
						observationProbMap.put(newGameState, observationProb);
						curProb = 0.0;
					}
					else
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
		DefenderBelief revisedBelief = new DefenderBelief();
		for(Entry<GameState, Double> entry : newBelief.getGameStateMap().entrySet())
		{
//			System.out.println(entry.getValue());
			if(entry.getValue() > this.thres)
				revisedBelief.addState(entry.getKey(), entry.getValue());
		}
//		
//		System.out.println("Testing belief size");
//		System.out.println(dBelief.getGameStateMap().size());
//		System.out.println(newBelief.getGameStateMap().size());
//		System.out.println(revisedBelief.getGameStateMap().size());
//		System.out.println("End testing belief size");
		newBelief.clear();
		// Revision
		sumProb = 0.0;
		for(Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet())
		{
			sumProb += entry.getValue();
		}
		for(Entry<GameState, Double> entry : revisedBelief.getGameStateMap().entrySet())
		{
			entry.setValue(entry.getValue() / sumProb); 
//			System.out.println(entry.getValue());
		}
//		if(sumProb == 0.0)
//			System.out.println("Wrong in belief");
//		depGraph.setState(savedGameState);
//		if(revisedBelief.getGameStateMap().isEmpty())
//		{
//			System.out.println("Something is wrong");
//			revisedBelief.addState(new GameState(), 1.0);
//		}
		return revisedBelief;
	}

	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph
	 * @param dBelief: belief of the defender
	 * @param curTimeStep: current time step
	 * @param numTimeStep: total number of time step
	 * @param discountFactor: reward discount factor
	 * @param propagationParam: for the AND node
	 * @return: nodes and corresponding values
	 *****************************************************************************************/
	public Map<Node, Double> computeCandidateValueTopo(DependencyGraph depGraph
			, DefenderBelief dBelief
			, int curTimeStep, int numTimeStep, double discountFactor
			, double propagationParam)
	{
		Map<Node, Double> dCandidateMap = new HashMap<Node, Double>();
		
		GameState savedGameState = new GameState();
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				savedGameState.addEnabledNode(node);
		}

		for(Entry<GameState, Double> entry : dBelief.getGameStateMap().entrySet())
		{
			GameState curGameState = entry.getKey();
			double stateProb = entry.getValue();
			depGraph.setState(curGameState);
			
			AttackCandidate curAttCandidate = ValuePropagationAttacker.selectCandidate(depGraph, curTimeStep, numTimeStep);
			
			double[] curACandidateProb = ValuePropagationAttacker.computecandidateProb(depGraph, curAttCandidate
					, curTimeStep, numTimeStep, this.qrParam, discountFactor, propagationParam
					, this.maxNumAttCandidate, this.minNumAttCandidate, this.numAttCandidateRatio);
//			for(int i = 0; i < curACandidateProb.length; i++)
//				System.out.println(curACandidateProb[i]);
			
			DefenderCandidate curDefCandidate = selectDCandidate(curGameState, curAttCandidate);
			double[] curDCandidateValue = computeCandidateValueTopo(depGraph
					, curAttCandidate, curDefCandidate
					, curTimeStep, numTimeStep
					, discountFactor, propagationParam);
//			for(int i = 0; i < curDCandidateValue.length; i++)
//				System.out.println(curDCandidateValue[i]);
			
			List<Node> curDefCandidateList = new ArrayList<Node>(curDefCandidate.getNodeCandidateSet());
			List<Edge> curEdgeACandidateList = new ArrayList<Edge>(curAttCandidate.getEdgeCandidateSet());
			List<Node> curNodeACandidateList = new ArrayList<Node>(curAttCandidate.getNodeCandidateSet());
			int dIdx = 0;
			for(Node node : curDefCandidateList)
			{
				double tempValue = 0.0;
				double tempAttProb = 1.0;
				if(node.getType() != NODE_TYPE.TARGET || node.getState() != NODE_STATE.ACTIVE)
				{
					if(node.getActivationType() == NODE_ACTIVATION_TYPE.AND)
					{
						int idx = curNodeACandidateList.indexOf(node);
						tempAttProb = curACandidateProb[idx + curEdgeACandidateList.size()];
						tempValue += tempAttProb * curDCandidateValue[dIdx];
//						tempValue += tempAttProb;
					}
					else // OR candidate
					{
						int idx = 0;
						for(Edge edge : curEdgeACandidateList)
						{
							if(edge.gettarget().getId() == node.getId())
							{
								tempAttProb = curACandidateProb[idx];
								tempValue += tempAttProb * curDCandidateValue[dIdx];
//								tempValue += tempAttProb;
							}
							idx++;
						}
					}
				}
				else
				{
					tempValue += curDCandidateValue[dIdx];
				}
				Double curValue = dCandidateMap.get(node);
				if(curValue == null) // if this is a new candidate
				{
					 dCandidateMap.put(node, tempValue * stateProb);
				}
				else
				{
					dCandidateMap.replace(node, curValue + tempValue * stateProb);
				}
				dIdx++;
			}
		}
		for(Entry<Node, Double> entry : dCandidateMap.entrySet())
		{
			Node node = entry.getKey();
			Double value = entry.getValue();
			entry.setValue(value + node.getDCost() * Math.pow(discountFactor, curTimeStep - 1));
		}
		depGraph.setState(savedGameState);
		
		return dCandidateMap;
	}
	
	/*****************************************************************************************
	 * 
	 * @param gameState: game state
	 * @param attCandidate: attack candidate
	 * @return defender candidate
	 *****************************************************************************************/
	public static DefenderCandidate selectDCandidate(GameState gameState, AttackCandidate attCandidate)
	{
		DefenderCandidate dCandidate = new DefenderCandidate();
		for(Edge edge : attCandidate.getEdgeCandidateSet()) // post-conditions of OR nodes
		{
			dCandidate.addNodeCandidate(edge.gettarget());
//			if(edge.gettarget().getType() == NODE_TYPE.TARGET)
//				System.out.println("Candidate has targets");
		}
		for(Node node : attCandidate.getNodeCandidateSet()) // AND nodes
		{
			dCandidate.addNodeCandidate(node);
//			if(node.getType() == NODE_TYPE.TARGET)
//				System.out.println("Candidate has targets");
		}
		for(Node node : gameState.getEnabledNodeSet()) // active target nodes
			if(node.getType() == NODE_TYPE.TARGET)
			{
				dCandidate.addNodeCandidate(node);
//				System.out.println("Candidate has active target");
			}
		return dCandidate;
	}
	
	/*****************************************************************************************
	 * 
	 * @param depGraph: dependency graph with current game state the defender is examining 
	 * @param attackCandidate: candidate of the attacker
	 * @param dCandidate: candidate of the defender
	 * @param curTimeStep: current time step
	 * @param numTimeStep: total number of time steps
	 * @param discountFactor: reward discount factor
	 * @param propagationParam: for propagating value over AND nodes
	 * @return value for each candidate of the defender
	 *****************************************************************************************/
	public double[] computeCandidateValueTopo(DependencyGraph depGraph
			, AttackCandidate attackCandidate, DefenderCandidate dCandidate
			, int curTimeStep, int numTimeStep, double discountFactor
			, double propagationParam)
	{
		
		List<Node> dCandidateList = new ArrayList<Node>(dCandidate.getNodeCandidateSet());
		
		double[] dCandidateValue = new double[dCandidateList.size()];
		for(int i = 0; i < dCandidateList.size(); i++)
			dCandidateValue[i] = 0.0; // initialize value of candidate nodes for the defender

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
//					r[i][j][k] = 0.0;
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
							for(int j = 1; j <= numTimeStep - curTimeStep; j++)
							{
//								double r_hat = 0.0;
//								if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
//								{
//									// Check active nodes
//									double prob = 1.0;
//									for(Edge postEdge : depGraph.incomingEdgesOf(postNode))
//									{
//										Node srcNode = postEdge.getsource();
//										if(srcNode.getState() == NODE_STATE.ACTIVE)
//											prob *= (1 - postEdge.getActProb());
//									}
//									prob = 1 - prob;
//									double newProb = 1.0;
//									for(Edge postEdge : depGraph.incomingEdgesOf(postNode))
//									{
//										Node srcNode = postEdge.getsource();
//										if(srcNode.getId() != node.getId() && srcNode.getState() == NODE_STATE.ACTIVE)
//											newProb *= (1 - postEdge.getActProb());
//									}
//									newProb = 1 - newProb;
//									
//									double reducedProb = prob - newProb;
//									
//									r_hat = r[i][j - 1][postNode.getId() - 1] * reducedProb; 
//								}
//								else
//								{
//									r_hat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
//								}
								
								double r_hat = 0.0;
								if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
								{
									r_hat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
								}
								else
								{
									r_hat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
//									int degree = depGraph.inDegreeOf(postNode);
//									for(Edge postEdge : depGraph.incomingEdgesOf(postNode))
//									{
//										if(postEdge.getsource().getState() == NODE_STATE.ACTIVE)
//											degree--;
//									}
//									r_hat = r_hat / Math.pow(degree, propagationParam);
								}
//								r[i][j][node.getId() - 1] += discountFactor * r_hat;
								if(r[i][j][node.getId() - 1] > discountFactor * r_hat)
									r[i][j][node.getId() - 1] = discountFactor * r_hat;
							}
						}
					}
				}
			}
		
		}
		
		/*****************************************************************************************/
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
//					rSum[k] += r[i][j][k];
			}
		}
		int idx = 0;
		for(Node node : dCandidateList)
		{
//			dCandidateValue[idx] += node.getDCost();
			if(node.getState() != NODE_STATE.ACTIVE) // not active targets, then belonging to attack candidate set
			{
				if(node.getActivationType() == NODE_ACTIVATION_TYPE.OR) // OR nodes, then belong to attack edge set
				{
					double prob = 1.0;
					for(Edge edge : attackCandidate.getEdgeCandidateSet())
					{
						if(edge.gettarget().getId() == node.getId())
						{
							prob *= (1 - edge.getActProb());
						}
					}
					prob = 1.0 - prob;
					dCandidateValue[idx] -= prob * rSum[node.getId() - 1];
				}
				else  // AND nodes, then belong to attack node set
				{
					dCandidateValue[idx] -= node.getActProb() * rSum[node.getId() - 1];
				}
			}
			else // if this is active target
			{
				System.out.println("Active targets");
				dCandidateValue[idx] -= node.getDPenalty();
				if(rSum[node.getId() - 1] != Double.POSITIVE_INFINITY)
					dCandidateValue[idx] -= rSum[node.getId() - 1];
			}
			dCandidateValue[idx] *= Math.pow(discountFactor, curTimeStep - 1); 
			idx++;
		}
		dCandidateList.clear();
		targetList.clear();
		return dCandidateValue;
	}
	/*****************************************************************************************
	 * 
	 * @param totalNumCandidate
	 * @param candidateValue
	 * @param logisParam
	 * @return
	 *****************************************************************************************/
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
	public DefenderAction sampleAction(DependencyGraph dependencyGraph, List<Node> dCandidateNodeList, int numNodetoProtect,
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
	
}
