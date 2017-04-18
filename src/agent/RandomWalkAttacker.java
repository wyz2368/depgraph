package agent;

import graph.Edge;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.AttackCandidate;
import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class RandomWalkAttacker extends Attacker {
	public final class RandomWalkTuple {
		private int tAct = 0;
		private double pAct = 0.0;
		private List<Edge> preAct;
		public RandomWalkTuple(final int tAct, final double pAct, final List<Edge> preAct) {
			this.tAct = tAct;
			this.pAct = pAct;
			this.preAct = preAct;
		}
		public int getTAct() {
			return this.tAct;
		}
		public double getPAct() {
			return this.pAct;
		}
		public List<Edge> getPreAct() {
			return this.preAct;
		}
	}
	private double qrParam;
	private double discFact;
	private static final int DEFAULT_NUM_RW_SAMPLE = 200;
	private int numRWSample = DEFAULT_NUM_RW_SAMPLE;
	
	public RandomWalkAttacker(final double numRWSample, final double qrParam, final double discFact) {
		super(AttackerType.RANDOM_WALK);
		this.numRWSample = (int) numRWSample;
		this.qrParam = qrParam;
		this.discFact = discFact;
	}
	@Override
	/**
	 * @param depGraph: dependency graph
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @return type of Attacker Action: an attack action
	 */
	public AttackerAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep, final RandomGenerator rng) {
		// Compute the candidate values, each candidate is a set
		double[] candidateValues = new double[this.numRWSample];
		AttackCandidate[] candidates = new AttackCandidate[this.numRWSample];
		for (int i = 0; i < this.numRWSample; i++) {
			RandomWalkTuple[] rwSample = randomWalk(depGraph, curTimeStep, rng);
			AttackCandidate attCandidate = new AttackCandidate();
			candidateValues[i] = greedyCandidate(depGraph, rwSample, attCandidate, numTimeStep, this.discFact);
//			System.out.println(candidateValues[i]);
			candidates[i] = attCandidate;
		}
		
		// Compute the candidate probability
		double[] probabilities = computecandidateProb(this.numRWSample, candidateValues, this.qrParam);
		
		// Start sampling
		int[] nodeIndexes = new int[this.numRWSample];
		for (int i = 0; i < this.numRWSample; i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		int sampleIdx = rnd.sample();
		AttackerAction attAction = new AttackerAction();
		for (Node node : candidates[sampleIdx].getNodeCandidateSet()) {
			attAction.addNodetoActive(node, depGraph.incomingEdgesOf(node));
		}
		for (Edge edge : candidates[sampleIdx].getEdgeCandidateSet()) {
			Set<Edge> edgeSet = new HashSet<Edge>();
			edgeSet.add(edge);
			attAction.addNodetoActive(edge.gettarget(), edgeSet);
		}
		
		return attAction;
	}
	
	/**
	 * @param depGraph dependency graph
	 * @param curTimeStep current time step
	 * @param rng random generator
	 * @return random walk tuple for all nodes
	 */
	public RandomWalkTuple[] randomWalk(final DependencyGraph depGraph,
			final int curTimeStep, final RandomGenerator rng) {
		RandomWalkTuple[] rwTuples = new RandomWalkTuple[depGraph.vertexSet().size()];
		/*****************************************************************************************/
		// Get topological order, starting from zero
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];
		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		/*****************************************************************************************/
		boolean[] isInSequence = new boolean[depGraph.vertexSet().size()];
		// Start random walk, from root nodes
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			Node node = topoOrder[i];
			int tAct = 0;
			double pAct = 0.0;
			List<Edge> preAct = null;
			/*****************************************************************************************/
			if (node.getState() == NodeState.ACTIVE) { // active nodes
				tAct = curTimeStep - 1;
				pAct = 1.0;
			} else if (depGraph.inDegreeOf(node) == 0) { // root nodes
				tAct = curTimeStep;
				pAct = node.getActProb();
			} else if (node.getActivationType() == NodeActivationType.AND) { // inactive and non-root and AND-type
				preAct = new ArrayList<Edge>();
				for (Edge edge : depGraph.incomingEdgesOf(node)) {
					Node preNode = edge.getsource();
					tAct = Math.max(tAct, rwTuples[preNode.getId() - 1].getTAct());
					preAct.add(edge);
				}
				tAct = tAct + 1;
				// Activation probability
				pAct = node.getActProb();
				List<Node> sequenceList = new ArrayList<Node>();
				for (int j = 0; j < depGraph.vertexSet().size(); j++) {
					isInSequence[i] = false;
				}
				for (Edge edge : preAct) {
					if (!isInSequence[edge.getsource().getId() - 1]) {
						sequenceList.add(edge.getsource());
						isInSequence[edge.getsource().getId() - 1] = true;
					}
//					sequenceSet.add(edge.getsource());
				}
				// Start backtracking
				while (!sequenceList.isEmpty()) {
					Node curNode = sequenceList.remove(0);
					RandomWalkTuple rwTuple = rwTuples[curNode.getId() - 1];
					if (curNode.getActivationType() == NodeActivationType.AND) { // OR node
						pAct *= curNode.getActProb();
						if (rwTuple.getPreAct() != null) {
							for (Edge edge : rwTuple.getPreAct()) {
								Node preNode = edge.getsource();
//								if(sequenceSet.add(preNode))
//									sequenceList.add(preNode);
								if (!isInSequence[preNode.getId() - 1]) {
									sequenceList.add(preNode);
									isInSequence[preNode.getId() - 1] = true;
								}
							}
						}
					} else { // OR node
						if (rwTuple.getPreAct() != null) {
							Edge edge = rwTuple.getPreAct().get(0);
							pAct *= edge.getActProb();
							Node preNode = edge.getsource();
//							if(sequenceSet.add(preNode))
//								sequenceList.add(preNode);
							if (!isInSequence[preNode.getId() - 1]) {
								sequenceList.add(preNode);
								isInSequence[preNode.getId() - 1] = true;
							}
						}
					}
				}
			} else { // inactive and non-root and OR-type --- random walk 
				List<Edge> edgeList = new ArrayList<Edge>(depGraph.incomingEdgesOf(node));
				int[] nodeIndexes = new int[edgeList.size()];
				double[] probabilities = new double[edgeList.size()];
				double sumProb = 0.0;
				for (int j = 0; j < edgeList.size(); j++) {
					nodeIndexes[j] = j;
					Node curNode = edgeList.get(j).getsource();
					probabilities[j] = rwTuples[curNode.getId() - 1].getPAct();
					sumProb += probabilities[j];
				}
				for (int j = 0; j < edgeList.size(); j++) {
					probabilities[j] /= sumProb;
				}
				EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
				int chosenPreIndex = rnd.sample();
				Edge chosenEdge = edgeList.get(chosenPreIndex);
				Node chosenPreNode = chosenEdge.getsource();
				
				tAct = rwTuples[chosenPreNode.getId() - 1].getTAct() + 1;
				pAct = rwTuples[chosenPreNode.getId() - 1].getPAct() * chosenEdge.getActProb();
				preAct = new ArrayList<Edge>();
				preAct.add(chosenEdge);
			}
			rwTuples[node.getId() - 1] = new RandomWalkTuple(tAct, pAct, preAct);
		}
		return rwTuples;
	}
	
	/*
	attCandidate: outcome of greedy, initialized already*
	*****************************************************************************************/
	static double greedyCandidate(final DependencyGraph depGraph, final RandomWalkTuple[] rwTuples
		, final AttackCandidate attCandidate, final int numTimeStep, final double discFact) {
		Set<Node> greedyTargetSet = new HashSet<Node>();
		List<Node> targetList = new ArrayList<Node>(depGraph.getTargetSet());
		boolean[] isChosen = new boolean[targetList.size()];
		for (int i = 0; i < targetList.size(); i++) {
			isChosen[i] = false;
		}
		double value = 0.0; // value of the chosen target subset
		boolean isStop = false; // greedy stop
		boolean[] isInSequence = new boolean[depGraph.vertexSet().size()]; // sequence to reach greedy target subset
//		Set<Node> sequenceSet = new HashSet<Node>();
		for (int j = 0; j < depGraph.vertexSet().size(); j++) {
			isInSequence[j] = false;
		}
		boolean[] chosenIsInSequence = new boolean[depGraph.vertexSet().size()]; // keep track for the searching
		boolean[] isInCurSequence = new boolean[depGraph.vertexSet().size()]; // for each iteration of greedy
		while (!isStop) {
			/*****************************************************************************************/
			// Start searching for next best target
			int targetIdx = 0; // for searching over target list
			int chosenIdx = -1; // target index which is chosen
			
			for (Node target : targetList) {
				RandomWalkTuple rwTuple = rwTuples[target.getId() - 1];
				if (target.getState() != NodeState.ACTIVE && !isChosen[targetIdx] && rwTuple.getTAct() <= numTimeStep) {
					/*****************************************************************************************/
					// Value of target
					double curValue = value + rwTuple.getPAct() * target.getAReward() * Math.pow(discFact, rwTuple.getTAct() - 1);
					/*****************************************************************************************/
					for (int j = 0; j < depGraph.vertexSet().size(); j++) {
						isInCurSequence[j] = isInSequence[j];
					}
					if (!isInCurSequence[target.getId() - 1]) { // target node is not in sequence so far
						/*****************************************************************************************/
						// Cost of activating the target
						isInCurSequence[target.getId() - 1] = true;
						if (target.getActivationType() == NodeActivationType.AND) {
							curValue += rwTuple.getPAct() / target.getActProb()  
								* target.getACost() * Math.pow(discFact, rwTuple.getTAct() - 1);
						} else {
							Edge chosenEdge = rwTuple.getPreAct().get(0);
							curValue += rwTuple.getPAct() / chosenEdge.getActProb() 
								* chosenEdge.getACost() * Math.pow(discFact, rwTuple.getTAct() - 1);
						}
						/*****************************************************************************************/
						// Start finding sequence of the target
						List<Node> sequence = new ArrayList<Node>();
						if (rwTuple.getPreAct() != null) {
							for (Edge edge : rwTuple.getPreAct()) {
								if (edge.getsource().getState() != NodeState.ACTIVE) {
									sequence.add(edge.getsource());
								}
							}
						}
						while (!sequence.isEmpty()) {
							Node curNode = sequence.remove(0);
							RandomWalkTuple curRwTuple = rwTuples[curNode.getId() - 1];
							if (!isInCurSequence[curNode.getId() - 1]) {
								isInCurSequence[curNode.getId() - 1] = true;
								if (curNode.getActivationType() == NodeActivationType.AND) {
									curValue += curRwTuple.getPAct() / curNode.getActProb()  
											* curNode.getACost() * Math.pow(discFact, curRwTuple.getTAct() - 1);
									if (curRwTuple.getPreAct() != null) {
										for (Edge edge : curRwTuple.getPreAct()) {
											Node preNode = edge.getsource();
											if (!isInCurSequence[preNode.getId() - 1] 
													&& preNode.getState() != NodeState.ACTIVE) {
												isInCurSequence[preNode.getId() - 1] = true;
												sequence.add(preNode);
											}
										}
									}
								} else {
									if (curRwTuple.getPreAct() != null) {
										Edge chosenEdge = curRwTuple.getPreAct().get(0);
										curValue += curRwTuple.getPAct() / chosenEdge.getActProb() 
											* chosenEdge.getACost() * Math.pow(discFact, curRwTuple.getTAct() - 1);
										if (!isInCurSequence[chosenEdge.getsource().getId() - 1]
											&& chosenEdge.getsource().getState() != NodeState.ACTIVE) {
											isInCurSequence[chosenEdge.getsource().getId() - 1] = true;
											sequence.add(chosenEdge.getsource());
										}
									}
								}
								
							}
						}
						/*****************************************************************************************/
					}
					if (curValue > value) {
						value = curValue;
						chosenIdx = targetIdx;
						for (int j = 0; j < depGraph.vertexSet().size(); j++) {
							chosenIsInSequence[j] = isInCurSequence[j];
						}
					}
				}
				targetIdx++;
			}
			if (chosenIdx != -1) {
				isChosen[chosenIdx] = true;
				for (int j = 0; j < depGraph.vertexSet().size(); j++) {
					isInSequence[j] = chosenIsInSequence[j];
				}
				greedyTargetSet.add(targetList.get(chosenIdx));
			} else {
				isStop = true;
			}
			/*****************************************************************************************/
		}
		/*****************************************************************************************/
		// Find corresponding candidate set for chosen subset of targets
		if (!greedyTargetSet.isEmpty()) {
			for (Node node : depGraph.vertexSet()) {
				if (isInSequence[node.getId() - 1] && node.getState() != NodeState.ACTIVE) {
					if (node.getActivationType() == NodeActivationType.AND) {
						boolean isCandidate = true;
						for (Edge edge : depGraph.incomingEdgesOf(node)) {
							if (edge.getsource().getState() != NodeState.ACTIVE) {
								isCandidate = false;
								break;
							}
						}
						if (isCandidate) {
							attCandidate.addNodeCandidate(node);
						}
					} else {
						RandomWalkTuple rwTuple = rwTuples[node.getId() - 1];
						Edge edge = rwTuple.getPreAct().get(0);
						if (edge.getsource().getState() == NodeState.ACTIVE) {
							attCandidate.addEdgeCandidate(edge);
						}
					}
				}
			}
		}
		if (Double.isNaN(value)) {
			System.out.println("Wrong");
		}
		return value;
	}
	
	/*****************************************************************************************
	 * @param totalNumCandidate total number of candidates
	 * @param candidateValue corresponding candidate values
	 * @param qrParam
	 * @return QR distribution over candidates
	 *****************************************************************************************/
	static double[] computecandidateProb(final int totalNumCandidate, final double[] candidateValue, final double qrParam) {
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
			probabilities[i] = Math.exp(qrParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for (int i = 0; i < totalNumCandidate; i++) {
			probabilities[i] /= sumProb;
		}
		
		return probabilities;
	}
	
	@Override
	public List<AttackerAction> sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep, final RandomGenerator rng,
		final int numSample, final boolean isReplacement) {
		if (isReplacement) { // this is currently not used, need to check if the isAdded works properly
			Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
			int i = 0;
			while (i < numSample) {
				AttackerAction attAction = sampleAction(depGraph, curTimeStep, numTimeStep, rng);
				boolean isAdded = attActionSet.add(attAction);
				if (isAdded) {
					i++;
				}
			}
			return new ArrayList<AttackerAction>(attActionSet);
		}
		// this is currently used, correct
		List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
		for (int i = 0; i < numSample; i++) {
			AttackerAction attAction = sampleAction(depGraph, curTimeStep, numTimeStep, rng);
			attActionList.add(attAction);
		}
		return attActionList;
	}
}
