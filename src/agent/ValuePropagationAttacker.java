	package agent;

import graph.Edge;
import graph.INode.NODE_ACTIVATION_TYPE;
import graph.Node;
import graph.INode.NODE_STATE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import model.AttackCandidate;
import model.AttackerAction;
import model.DependencyGraph;

public class ValuePropagationAttacker extends Attacker{
	int maxNumSelectCandidate;
	int minNumSelectCandidate;
	double numSelectCandidateRatio;
	double qrParam;
	double discFact;
	
	double propagationParam = 0.5;
	public ValuePropagationAttacker(double maxNumSelectCandidate, double minNumSelectCandidate, double numSelectCandidateRatio
			, double qrParam, double discFact)
	{
		super(ATTACKER_TYPE.VALUE_PROPAGATION);
		this.maxNumSelectCandidate =(int) maxNumSelectCandidate;
		this.minNumSelectCandidate = (int) minNumSelectCandidate;
		this.numSelectCandidateRatio = numSelectCandidateRatio;
		this.qrParam = qrParam;
		this.discFact = discFact;
	}
	@Override
	/*****************************************************************************************
	 * @param depGraph: dependency graph
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @return type of Attacker Action: an attack action
	 *****************************************************************************************/
	public AttackerAction sampleAction(DependencyGraph depGraph, int curTimeStep, int numTimeStep, RandomGenerator rng)
	{	
		// Find candidate
		AttackCandidate attackCandidate = selectCandidate(depGraph, curTimeStep, numTimeStep);
		// Compute candidate value
//		double[] candidateValue = computeCandidateValue(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
//				, this.propagationParam);
//		double[] candidateValue = computeCandidateValueTime(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
//				, this.propagationParam);
		double[] candidateValue = computeCandidateValueTopoBest(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
				, this.propagationParam);
		int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if(totalNumCandidate < this.minNumSelectCandidate)
			numSelectCandidate = totalNumCandidate;
		else 
		{
			numSelectCandidate = Math.max(this.minNumSelectCandidate, (int)(totalNumCandidate * this.numSelectCandidateRatio));
			numSelectCandidate = Math.min(this.maxNumSelectCandidate, numSelectCandidate);
		}
		if(numSelectCandidate == 0) // if there is no candidate
			return new AttackerAction();
		
		// Compute probability to choose each node
		double[] probabilities = computecandidateProb(totalNumCandidate, candidateValue, this.qrParam);
		
		// Sampling
		int[] nodeIndexes = new int[totalNumCandidate];
		for(int i = 0; i < totalNumCandidate; i++)
			nodeIndexes[i] = i;
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		return sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
	}
	public static double[] computecandidateProb(DependencyGraph depGraph, AttackCandidate attackCandidate
			, int curTimeStep, int numTimeStep
			, double qrParam, double discFact, double propagationParam
			, int maxNumSelectCandidate, int minNumSelectCandidate, double numSelectCandidateRatio)
	{
		double[] candidateValue = computeCandidateValueTopoBest(depGraph, attackCandidate, curTimeStep, numTimeStep, discFact
				, propagationParam);
		int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if(totalNumCandidate < minNumSelectCandidate)
			numSelectCandidate = totalNumCandidate;
		else 
		{
			numSelectCandidate = Math.max(minNumSelectCandidate, (int)(totalNumCandidate * numSelectCandidateRatio));
			numSelectCandidate = Math.min(maxNumSelectCandidate, numSelectCandidate);
		}
		if(numSelectCandidate == 0) // if there is no candidate
			return null;
		
		// Compute probability to choose each node
		return computecandidateProb(totalNumCandidate, candidateValue, qrParam);
	}
	
	@Override
	/*****************************************************************************************
	 * @param depGraph: dependency graph
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @param numSample: number of samples to generate
	 * @param isReplacement: whether sampling with replacement or not
	 * @return Samples of attacker action
	 *****************************************************************************************/
	public List<AttackerAction> sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep, RandomGenerator rng,
			int numSample, boolean isReplacement) {
		// Find candidate
		AttackCandidate attackCandidate = selectCandidate(depGraph, curTimeStep, numTimeStep);
		// Compute candidate value
//		double[] candidateValue = computeCandidateValue(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
//				, this.propagationParam);
//		double[] candidateValue = computeCandidateValueTime(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
//				, this.propagationParam);
		double[] candidateValue = computeCandidateValueTopoBest(depGraph, attackCandidate, curTimeStep, numTimeStep, this.discFact
				, this.propagationParam);
		int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if(totalNumCandidate < this.minNumSelectCandidate)
			numSelectCandidate = totalNumCandidate;
		else 
		{
			numSelectCandidate = Math.max(this.minNumSelectCandidate, (int)(totalNumCandidate * this.numSelectCandidateRatio));
			numSelectCandidate = Math.min(this.maxNumSelectCandidate, numSelectCandidate);
		}
		if(numSelectCandidate == 0) // if there is no candidate
		{
			List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
			attActionList.add(new AttackerAction());
			return attActionList;
		}
		
		// Compute probability to choose each node
		double[] probabilities = computecandidateProb(totalNumCandidate, candidateValue, this.qrParam);
		
		// Sampling
		int[] nodeIndexes = new int[totalNumCandidate];
		for(int i = 0; i < totalNumCandidate; i++)
			nodeIndexes[i] = i;
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		if(isReplacement) // this is currently not used, need to check if the isAdded works properly
		{
			Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
			int i = 0;
			while(i < numSample)
			{
				AttackerAction attAction = sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
				boolean isAdded = attActionSet.add(attAction);
				if(isAdded)
					i++;
			}
			return new ArrayList<AttackerAction>(attActionSet);
		}
		else // this is currently used, correct
		{
			List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
			for(int i = 0; i < numSample; i++)
			{
				AttackerAction attAction = sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
				attActionList.add(attAction);
			}
			return attActionList;
		}
	}
	/*****************************************************************************************
	 * @param totalNumCandidate: total number of candidates
	 * @param candidateValue: corresponding candidate values
	 * @return QR distribution over candidates
	 *****************************************************************************************/
	public static double[] computecandidateProb(int totalNumCandidate, double[] candidateValue, double qrParam)
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
			probabilities[i] = Math.exp(qrParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for(int i = 0; i < totalNumCandidate; i++)
			probabilities[i] /= sumProb;
		
		return probabilities;
	}
	/*****************************************************************************************
	 * @param depGraph: dependency graph
	 * @param attackCandidate: candidate lists 
	 * @param curTimeStep: current time step
	 * @param numTimeStep: total number of time step
	 * @param discountFactor: reward discount factor
	 * @return values of candidates: sorted from edge candidate list to node candidate list
	 *****************************************************************************************/
	public static double[] computeCandidateValue(DependencyGraph depGraph, AttackCandidate attackCandidate
			, int curTimeStep, int numTimeStep, double discountFactor
			, double propagationParam)
	{
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();
		double[] candidateValue = new double[totalNumCandidate];
		for(int i = 0; i < totalNumCandidate; i++)
			candidateValue[i] = 0.0;
		
		List<Node> targetList = new ArrayList<Node>(); // list of inactive targets
		for(Node target : depGraph.getTargetSet())
		{
			if(target.getState() != NODE_STATE.ACTIVE)
				targetList.add(target);
		}
		if(targetList.isEmpty()) // if all targets are already enabled
			return candidateValue;
		/*****************************************************************************************/
		// Initialize node propagation value with respect to inactive targets
		double[][] r = new double[targetList.size()][depGraph.vertexSet().size()];
		for(int i = 0; i < targetList.size(); i++)
		{
			for(int j = 0; j < depGraph.vertexSet().size(); j++)
			{
				r[i][j] = 0;
			}
		}
		for(int i = 0; i < targetList.size(); i++)
		{
			r[i][targetList.get(i).getId() - 1] = targetList.get(i).getAReward();
		}
		List<Node> nodeList = new ArrayList<Node>(); // node queue for back tracking 
		List<Integer> timeList = new ArrayList<Integer>(); // corresponding time step of the nodelist
		for(Node target : targetList)
		{
			nodeList.add(target);
			timeList.add(curTimeStep);
		}
		/*****************************************************************************************/
		// Start backtracking
		while(!nodeList.isEmpty())
		{
			Node node = nodeList.remove(0);
			int timeStep = timeList.remove(0);
			Set<Edge> edgeSet = depGraph.incomingEdgesOf(node);
			if(edgeSet != null)
			for(Edge edge : edgeSet) // searching over incoming edges of the current node
			{
				Node srcNode = edge.getsource();
				if(srcNode.getState() != NODE_STATE.ACTIVE) // only consider inactive nodes
				{
					double r_hat = 0.0;
					for(int i = 0; i < targetList.size(); i++)
					{
						if(node.getActivationType() == NODE_ACTIVATION_TYPE.AND) // marginal increment for AND nodes
						{
							r_hat = r[i][node.getId() - 1] * node.getActProb();
							r_hat += node.getACost();
							r_hat = r_hat / Math.pow(depGraph.inDegreeOf(node), propagationParam);
						}
						else // OR nodes
						{
							r_hat = r[i][node.getId() - 1] * edge.getActProb(); 
							r_hat += edge.getACost();
						}
						
						if(r[i][srcNode.getId() - 1] < discountFactor * r_hat)
						{
							r[i][srcNode.getId() - 1] = discountFactor * r_hat;
						}
//						if(r_hat > 0)
//							r[i][srcNode.getId() - 1] += discountFactor * r_hat;
					}
					// if the new time step doesn't exceed the total number of time step, then add new node to the queue
					if(timeStep + 1 < numTimeStep)
					{
						nodeList.add(srcNode);
						timeList.add(timeStep + 1);
					}
				}
			}
		}
		// sum of values over target sets 
		double[] rSum = new double[depGraph.vertexSet().size()];
		for(int i = 0; i < depGraph.vertexSet().size(); i++)
			rSum[i] = 0.0;
		for(int i = 0; i < depGraph.vertexSet().size(); i++)
		{
			for(int j = 0; j < targetList.size(); j++)
				rSum[i] += r[j][i];
//				rSum[i] = Math.max(rSum[i], r[j][i]) ;
		}
//		for(int i = 0; i < depGraph.vertexSet().size(); i++)
//			rSum[i] /= targetList.size();
		/*****************************************************************************************/
		// Sum of value for candidates
		int idx = 0;
		for(Edge edge : edgeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (edge.getACost() + edge.getActProb() * rSum[edge.gettarget().getId() - 1]);
			idx++;
		}
		for(Node node : nodeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (node.getACost() + node.getActProb() * rSum[node.getId() - 1]);
			idx++;
		}
		
		edgeCandidateList.clear();
		nodeCandidateList.clear();
		return candidateValue;
	}
	
	public static double[] computeCandidateValueTopoSum(DependencyGraph depGraph, AttackCandidate attackCandidate
			, int curTimeStep, int numTimeStep, double discountFactor
			, double propagationParam)
	{
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();
		double[] candidateValue = new double[totalNumCandidate];
		for(int i = 0; i < totalNumCandidate; i++)
			candidateValue[i] = 0.0;
		
		List<Node> targetList = new ArrayList<Node>(); // list of inactive targets
		for(Node target : depGraph.getTargetSet())
		{
			if(target.getState() != NODE_STATE.ACTIVE)
				targetList.add(target);
		}
		if(targetList.isEmpty()) // if all targets are already enabled
			return candidateValue;
		
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];

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
					r[i][j][k] = 0.0;
				}
			}
		}
		for(int i = 0; i < targetList.size(); i++)
		{
			Node node = targetList.get(i);
			r[i][0][node.getId() - 1] = node.getAReward();
		}
		for(int k = depGraph.vertexSet().size() - 1; k >= 0; k--)
		{
			Node node = topoOrder[k];
			if(node.getState() != NODE_STATE.ACTIVE)
			{
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
									double r_hat = 0.0;
									if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
									{
										r_hat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
										r_hat += edge.getACost();
									}
									else
									{
										r_hat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
										r_hat += postNode.getACost();
										int degree = depGraph.inDegreeOf(postNode);
										for(Edge postEdge : depGraph.incomingEdgesOf(postNode))
										{
											if(postEdge.getsource().getState() == NODE_STATE.ACTIVE)
												degree--;
										}
										r_hat = r_hat / Math.pow(degree, propagationParam);
									}
									if(r[i][j][node.getId() - 1] < discountFactor * r_hat)
//									r[i][j][node.getId() - 1] += discountFactor * r_hat;
										r[i][j][node.getId() - 1] = discountFactor * r_hat;
								}
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
			rSum[i] = 0;
		for(int i = 0; i < targetList.size(); i++)
		{
			for(int j = 0; j <= numTimeStep - curTimeStep; j++)
			{
				for(int k = 0; k < depGraph.vertexSet().size(); k++)
					rSum[k] += r[i][j][k] * Math.pow(discountFactor, j);
			}
		}
		int idx = 0;
		for(Edge edge : edgeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (edge.getACost() + edge.getActProb() * rSum[edge.gettarget().getId() - 1]);
			idx++;
		}
		for(Node node : nodeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (node.getACost() + node.getActProb() * rSum[node.getId() - 1]);
			idx++;
		}
		
		edgeCandidateList.clear();
		nodeCandidateList.clear();
		return candidateValue;
	}
	
	public static double[] computeCandidateValueTopoBest(DependencyGraph depGraph, AttackCandidate attackCandidate
			, int curTimeStep, int numTimeStep, double discountFactor
			, double propagationParam)
	{
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();
		double[] candidateValue = new double[totalNumCandidate];
		for(int i = 0; i < totalNumCandidate; i++)
			candidateValue[i] = 0.0;
		
		List<Node> targetList = new ArrayList<Node>(); // list of inactive targets
		for(Node target : depGraph.getTargetSet())
		{
			if(target.getState() != NODE_STATE.ACTIVE)
				targetList.add(target);
		}
		if(targetList.isEmpty()) // if all targets are already enabled
			return candidateValue;
		
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];

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
					r[i][j][k] = 0.0;
				}
			}
		}
		for(int i = 0; i < targetList.size(); i++)
		{
			Node node = targetList.get(i);
			r[i][0][node.getId() - 1] = node.getAReward();
		}
		for(int k = depGraph.vertexSet().size() - 1; k >= 0; k--)
		{
			Node node = topoOrder[k];
			if(node.getState() != NODE_STATE.ACTIVE)
			{
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
									double r_hat = 0.0;
									if(postNode.getActivationType() == NODE_ACTIVATION_TYPE.OR)
									{
										r_hat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
										r_hat += edge.getACost();
									}
									else
									{
										r_hat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
										r_hat += postNode.getACost();
										int degree = depGraph.inDegreeOf(postNode);
										for(Edge postEdge : depGraph.incomingEdgesOf(postNode))
										{
											if(postEdge.getsource().getState() == NODE_STATE.ACTIVE)
												degree--;
										}
										r_hat = r_hat / Math.pow(degree, propagationParam);
									}
									if(r[i][j][node.getId() - 1] < discountFactor * r_hat)
//									r[i][j][node.getId() - 1] += discountFactor * r_hat;
										r[i][j][node.getId() - 1] = discountFactor * r_hat;
								}
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
			rSum[i] = 0;
		for(int i = 0; i < targetList.size(); i++)
		{
			for(int j = 0; j <= numTimeStep - curTimeStep; j++)
			{
				for(int k = 0; k < depGraph.vertexSet().size(); k++)
//					rSum[k] += r[i][j][k] * Math.pow(discountFactor, j);
					if(rSum[k] < r[i][j][k])
						rSum[k] = r[i][j][k];
			}
		}
		int idx = 0;
		for(Edge edge : edgeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (edge.getACost() + edge.getActProb() * rSum[edge.gettarget().getId() - 1]);
			idx++;
		}
		for(Node node : nodeCandidateList)
		{
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
					* (node.getACost() + node.getActProb() * rSum[node.getId() - 1]);
			idx++;
		}
		
		edgeCandidateList.clear();
		nodeCandidateList.clear();
		return candidateValue;
	}
	/*****************************************************************************************
	 * @param depGraph: dependency graph
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @return type of AttackCandidate: candidate set for the attacker
	 *****************************************************************************************/
	public static AttackCandidate selectCandidate(DependencyGraph depGraph, int curTimeStep, int numTimeStep)
	{
		AttackCandidate aCandidate = new AttackCandidate();
		
		// Check if all targets are already active, then the attacker doesn't need to do anything
		boolean isAllTargetActive = true;
		for(Node target : depGraph.getTargetSet())
		{
			if(target.getState() != NODE_STATE.ACTIVE)
			{
				isAllTargetActive = false;
				break;
			}
		}
		// Start selecting candidate when some targets are inactive
		if(!isAllTargetActive)
		for(Node node : depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.INACTIVE) // only check inactive nodes
			{
				boolean isCandidate = false;
				if(node.getActivationType() == NODE_ACTIVATION_TYPE.AND){// if this node is AND type
					isCandidate = true;
					for(Edge inEdge : depGraph.incomingEdgesOf(node))
					{
						if(inEdge.getsource().getState() == NODE_STATE.INACTIVE)
						{
							isCandidate = false;
							break;
						}
					}
				}
				else // if this node is OR type
				{
					for(Edge inEdge : depGraph.incomingEdgesOf(node))
					{
						if(inEdge.getsource().getState() != NODE_STATE.INACTIVE)
						{
							isCandidate = true;
							break;
						}
					}
				}
				
				if(isCandidate) // if this node is a candidate
				{
					if(node.getActivationType() == NODE_ACTIVATION_TYPE.AND) // if AND node, then add node to the candidate set
					{
						aCandidate.addNodeCandidate(node);
					}
					else // if OR node, then add edges to the  candidate set
					{
						for(Edge inEdge : depGraph.incomingEdgesOf(node))
						{
							if(inEdge.getsource().getState() == NODE_STATE.ACTIVE)
								aCandidate.addEdgeCandidate(inEdge);
						}
					}
				}
				
			}
		}
		return aCandidate;
	}
	/*****************************************************************************************
	 * @param depGraph: dependency graph
	 * @param attackCandidate: candidate set
	 * @param numSelectCandidate: number of candidates to select
	 * @param rnd: integer distribution randomizer
	 * @return type of AttackerAction: an action for the attacker
	 *****************************************************************************************/
	public static AttackerAction sampleAction(DependencyGraph depGraph, AttackCandidate attackCandidate, int numSelectCandidate
			, AbstractIntegerDistribution rnd)
	{
		AttackerAction action = new AttackerAction();
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();

		boolean[] isChosen = new boolean[totalNumCandidate]; // check if this candidate is already chosen
		for(int i = 0; i < totalNumCandidate; i++)
			isChosen[i] = false;
		int count = 0;
		while(count < numSelectCandidate)
		{
			int idx = rnd.sample(); // randomly chooses a candidate
			if(!isChosen[idx]) // if this candidate is not chosen
			{
				if(idx < edgeCandidateList.size()) // select edge
				{
					Edge selectEdge = edgeCandidateList.get(idx);
					Set<Edge> edgeSet = action.getAction().get(selectEdge.gettarget()); //find the current edge candidates w.r.t. the OR node
					if(edgeSet != null) // if this OR node is included in the attacker action, add new edge to the edge set associated with this node
					{
						edgeSet.add(selectEdge);
					}
					else // if this OR node is node included in the attacker action, create a new one
					{
						edgeSet = new HashSet<Edge>();
						edgeSet.add(selectEdge);
						action.getAction().put(selectEdge.gettarget(), edgeSet);
					}
						
				}
				else // select node, this is for AND node only
				{
					Node selectNode = nodeCandidateList.get(idx - edgeCandidateList.size());
					action.getAction().put(selectNode, depGraph.incomingEdgesOf(selectNode));
				}
				
				isChosen[idx] = true; // set chosen to be true
				count++;
			}	
		}
		edgeCandidateList.clear();
		nodeCandidateList.clear();
		return action;
	}
}
