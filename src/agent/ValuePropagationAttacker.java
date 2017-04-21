package agent;

import graph.Edge;
import graph.INode.NodeActivationType;
import graph.Node;
import graph.INode.NodeState;

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

public final class ValuePropagationAttacker extends Attacker {
	private int maxNumSelectCandidate;
	private int minNumSelectCandidate;
	private double numSelectCandidateRatio;
	private double qrParam;
	private double discFact;
	
	private final double propagationParam = 0.5;
	private final boolean isBest = true;
	
	public ValuePropagationAttacker(
		final double maxNumSelectCandidate, 
		final double minNumSelectCandidate,
		final double numSelectCandidateRatio,
		final double qrParam, final double discFact) {
		super(AttackerType.VALUE_PROPAGATION);
		if (minNumSelectCandidate < 1 || maxNumSelectCandidate < minNumSelectCandidate
			|| !isProb(numSelectCandidateRatio)) {
			throw new IllegalArgumentException();
		}
		this.maxNumSelectCandidate = (int) maxNumSelectCandidate;
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
	public AttackerAction sampleAction(
			final DependencyGraph depGraph, 
			final int curTimeStep,
			final int numTimeStep, 
			final RandomGenerator rng) {	
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || rng == null) {
			throw new IllegalArgumentException();
		}
		// Find candidate
		AttackCandidate attackCandidate = selectCandidate(depGraph);
		// Compute candidate value
		double[] candidateValue = computeCandidateValueTopo(
									depGraph, 
									attackCandidate, 
									curTimeStep, 
									numTimeStep, 
									this.discFact, 
									this.propagationParam,
									this.isBest);
		int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() 
			+ attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if (totalNumCandidate < this.minNumSelectCandidate) {
			numSelectCandidate = totalNumCandidate;
		} else {
			numSelectCandidate = Math.max(this.minNumSelectCandidate,
				(int) (totalNumCandidate * this.numSelectCandidateRatio));
			numSelectCandidate = Math.min(this.maxNumSelectCandidate, numSelectCandidate);
		}
		if (numSelectCandidate == 0) { // if there is no candidate
			return new AttackerAction();
		}
		
		// Compute probability to choose each node
		double[] probabilities = computeCandidateProb(totalNumCandidate, candidateValue, this.qrParam);
		
		// Sampling
		int[] nodeIndexes = new int[totalNumCandidate];
		for (int i = 0; i < totalNumCandidate; i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		
		return sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
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
	public List<AttackerAction> sampleAction(
			final DependencyGraph depGraph,
			final int curTimeStep, 
			final int numTimeStep, 
			final RandomGenerator rng,
			final int numSample, 
			final boolean isReplacement) {
		if (depGraph == null || numTimeStep < 0 || rng == null || numSample < 1) {
			throw new IllegalArgumentException();
		}
		// Find candidate
		AttackCandidate attackCandidate = selectCandidate(depGraph);
		// Compute candidate value
		double[] candidateValue = computeCandidateValueTopo(
										depGraph, 
										attackCandidate, 
										curTimeStep, 
										numTimeStep, 
										this.discFact, 
										this.propagationParam,
										this.isBest);
		int totalNumCandidate =
			attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if (totalNumCandidate < this.minNumSelectCandidate) {
			numSelectCandidate = totalNumCandidate;
		} else {
			numSelectCandidate = Math.max(this.minNumSelectCandidate,
				(int) (totalNumCandidate * this.numSelectCandidateRatio));
			numSelectCandidate = Math.min(this.maxNumSelectCandidate, numSelectCandidate);
		}
		if (numSelectCandidate == 0) { // if there is no candidate
			List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
			attActionList.add(new AttackerAction());
			return attActionList;
		}
		
		// Compute probability to choose each node
		double[] probabilities = computeCandidateProb(totalNumCandidate, candidateValue, this.qrParam);
		
		// Sampling
		int[] nodeIndexes = new int[totalNumCandidate];
		for (int i = 0; i < totalNumCandidate; i++) {
			nodeIndexes[i] = i;
		}
		EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		if (isReplacement) { // this is currently not used, need to check if the isAdded works properly
			Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
			int i = 0;
			while (i < numSample) {
				AttackerAction attAction = sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
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
			AttackerAction attAction = sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
			attActionList.add(attAction);
		}
		return attActionList;
	}
	
	static double[] computeCandidateProb(
		final DependencyGraph depGraph, 
		final AttackCandidate attackCandidate,
		final int curTimeStep, 
		final int numTimeStep,
		final double qrParam, 
		final double discFact,
		final double propagationParam, 
		final int maxNumSelectCandidate,
		final int minNumSelectCandidate, 
		final double numSelectCandidateRatio,
		final boolean isBest) {
		if (depGraph == null || attackCandidate == null
			|| curTimeStep < 0 || numTimeStep < curTimeStep
			|| discFact < 0.0 || discFact > 1.0 || minNumSelectCandidate < 1
			|| maxNumSelectCandidate < minNumSelectCandidate
			|| !isProb(numSelectCandidateRatio)
		) {
			throw new IllegalArgumentException();
		}
		double[] candidateValue = computeCandidateValueTopo(
			depGraph, 
			attackCandidate, 
			curTimeStep, 
			numTimeStep,
			discFact, 
			propagationParam,
			isBest);
		int totalNumCandidate =
			attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		int numSelectCandidate = 0;
		if (totalNumCandidate < minNumSelectCandidate) {
			numSelectCandidate = totalNumCandidate;
		} else {
			numSelectCandidate = Math.max(minNumSelectCandidate, (int) (totalNumCandidate * numSelectCandidateRatio));
			numSelectCandidate = Math.min(maxNumSelectCandidate, numSelectCandidate);
		}
		if (numSelectCandidate == 0) { // if there is no candidate
			return null;
		}
		
		// Compute probability to choose each node
		return computeCandidateProb(totalNumCandidate, candidateValue, qrParam);
	}
	
	/*****************************************************************************************
	* @param depGraph: dependency graph
	* @param curTimeStep: current time step 
	* @param numTimeStep: total number of time step
	* @return type of AttackCandidate: candidate set for the attacker
	*****************************************************************************************/
	static AttackCandidate selectCandidate(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		AttackCandidate aCandidate = new AttackCandidate();
		
		// Check if all targets are already active, then the attacker doesn't need to do anything
		boolean isAllTargetActive = true;
		for (Node target : depGraph.getTargetSet()) {
			if (target.getState() != NodeState.ACTIVE) {
				isAllTargetActive = false;
				break;
			}
		}
		// Start selecting candidate when some targets are inactive
		if (!isAllTargetActive) {
			for (Node node : depGraph.vertexSet()) {
				if (node.getState() == NodeState.INACTIVE) { // only check inactive nodes
					boolean isCandidate = false;
					if (node.getActivationType() == NodeActivationType.AND) { // if this node is AND type
						isCandidate = true;
						for (Edge inEdge : depGraph.incomingEdgesOf(node)) {
							if (inEdge.getsource().getState() == NodeState.INACTIVE) {
								isCandidate = false;
								break;
							}
						}
					} else { // if this node is OR type
						for (Edge inEdge : depGraph.incomingEdgesOf(node)) {
							if (inEdge.getsource().getState() != NodeState.INACTIVE) {
								isCandidate = true;
								break;
							}
						}
					}
					
					if (isCandidate) { // if this node is a candidate
						// if AND node, then add node to the candidate set
						if (node.getActivationType() == NodeActivationType.AND) {
							aCandidate.addNodeCandidate(node);
						} else { // if OR node, then add edges to the  candidate set
							for (Edge inEdge : depGraph.incomingEdgesOf(node)) {
								if (inEdge.getsource().getState() == NodeState.ACTIVE) {
									aCandidate.addEdgeCandidate(inEdge);
								}
							}
						}
					}
				}
			}
		}
		return aCandidate;
	}
	
	/*****************************************************************************************
	 * @param totalNumCandidate: total number of candidates
	 * @param candidateValue: corresponding candidate values
	 * @return QR distribution over candidates
	 *****************************************************************************************/
	private static double[] computeCandidateProb(
		final int totalNumCandidate, final double[] candidateValue, final double qrParam) {
		if (totalNumCandidate < 0 || candidateValue == null) {
			throw new IllegalArgumentException();
		}
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
			probabilities[i] = Math.exp(qrParam * candidateValue[i]);
			sumProb += probabilities[i];
		}
		for (int i = 0; i < totalNumCandidate; i++) {
			probabilities[i] /= sumProb;
		}
		
		return probabilities;
	}
	
	private static double[] computeCandidateValueTopo(
		final DependencyGraph depGraph, 
		final AttackCandidate attackCandidate, 
		final int curTimeStep, 
		final int numTimeStep, 
		final double discountFactor, 
		final double propagationParam,
		final boolean isBest
	) {
		if (depGraph == null || attackCandidate == null
			|| curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
		) {
			throw new IllegalArgumentException();
		}
		// if there is no candidate, then no point to compute attack probability
		if (attackCandidate.isEmpty()) {
			return null;
		}
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();
		double[] candidateValue = new double[totalNumCandidate];
		for (int i = 0; i < totalNumCandidate; i++) {
			candidateValue[i] = 0.0;
		}
		
		List<Node> targetList = new ArrayList<Node>(); // list of inactive targets
		for (Node target : depGraph.getTargetSet()) {
			if (target.getState() != NodeState.ACTIVE) {
				targetList.add(target);
			}
		}
		
		// Get topological order of the vertices in graph
		Node[] topoOrder = new Node[depGraph.vertexSet().size()];

		for (Node node : depGraph.vertexSet()) {
			topoOrder[node.getTopoPosition()] = node;
		}
		
		// Value propagation of each node with respect to each
		// inactive target and propagation time (>= curTimeStep & <= numTimeStep).
		double[][][] r = new double[targetList.size()][numTimeStep - curTimeStep + 1][depGraph.vertexSet().size()];
		for (int i = 0; i < targetList.size(); i++) {
			for (int j = 0; j <= numTimeStep - curTimeStep; j++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					r[i][j][k] = 0.0;
				}
			}
		}
		
		// For inactive targets first
		for (int i = 0; i < targetList.size(); i++) {
			Node node = targetList.get(i);
			r[i][0][node.getId() - 1] = node.getAReward();
		}
		// Start examining nodes in inverse topological order (leaf nodes first)
		for (int k = depGraph.vertexSet().size() - 1; k >= 0; k--) {
			Node node = topoOrder[k];
			// checking inactive nodes only since no need to examine active nodes
			if (node.getState() != NodeState.ACTIVE) {
				Set<Edge> edgeSet = depGraph.outgoingEdgesOf(node);
				if (edgeSet != null && !edgeSet.isEmpty()) { // if non-leaf 
					for (Edge edge : edgeSet) { // examining each outgoing edge of this node
						Node postNode = edge.gettarget();
						// if this postcondition is not active, then propagate value from this node
						if (postNode.getState() != NodeState.ACTIVE) {
							for (int i = 0; i < targetList.size(); i++) {
								for (int j = 1; j <= numTimeStep - curTimeStep; j++) {
									double rHat = 0.0;
									// postNode is of type OR
									if (postNode.getActivationType() == NodeActivationType.OR) {
										rHat = r[i][j - 1][postNode.getId() - 1] * edge.getActProb(); 
										rHat += edge.getACost();
									} else { // postNode is of type AND
										rHat = r[i][j - 1][postNode.getId() - 1] * postNode.getActProb();
										rHat += postNode.getACost();
										int degree = depGraph.inDegreeOf(postNode);
										for (Edge postEdge : depGraph.incomingEdgesOf(postNode)) {
											if (postEdge.getsource().getState() == NodeState.ACTIVE) {
												degree--;
											}
										}
										// Some normalization with respect to # inactive precondition nodes
										rHat = rHat / Math.pow(degree, propagationParam);
									}
									if (isBest) {
										// only keep maximum propagation value
										if (r[i][j][node.getId() - 1] < discountFactor * rHat) {
											r[i][j][node.getId() - 1] = discountFactor * rHat;
										}
									} else { // sum
										if (rHat > 0) {
											r[i][j][node.getId() - 1] += discountFactor * rHat;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		/*****************************************************************************************/
		// Now, only keep maximum propagation values over all propagation paths
		double[] rSum = new double[depGraph.vertexSet().size()];
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			rSum[i] = 0;
		}
		for (int i = 0; i < targetList.size(); i++) {
			for (int j = 0; j <= numTimeStep - curTimeStep; j++) {
				for (int k = 0; k < depGraph.vertexSet().size(); k++) {
					if (isBest) {
						if (rSum[k] < r[i][j][k]) {
							rSum[k] = r[i][j][k];
						}
					} else { // sum
						if (r[i][j][k] > 0) {
							rSum[k] += r[i][j][k];
						}
					}
				}
			}
		}
		
		// Now compute final propagation value for each candidate, considering cost and activation probs
		int idx = 0;
		for (Edge edge : edgeCandidateList) {
			candidateValue[idx] = Math.pow(discountFactor, curTimeStep - 1) 
				* (edge.getACost() + edge.getActProb() * rSum[edge.gettarget().getId() - 1]);
			idx++;
		}
		for (Node node : nodeCandidateList) {
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
	 * @param attackCandidate: candidate set
	 * @param numSelectCandidate: number of candidates to select
	 * @param rnd: integer distribution randomizer
	 * @return type of AttackerAction: an action for the attacker
	 *****************************************************************************************/
	private static AttackerAction sampleAction(
			final DependencyGraph depGraph,
			final AttackCandidate attackCandidate, 
			final int numSelectCandidate, 
			final AbstractIntegerDistribution rnd) {
		if (depGraph == null || numSelectCandidate < 0 || rnd == null || attackCandidate == null) {
			throw new IllegalArgumentException();
		}
		AttackerAction action = new AttackerAction();
		List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();

		boolean[] isChosen = new boolean[totalNumCandidate]; // check if this candidate is already chosen
		for (int i = 0; i < totalNumCandidate; i++) {
			isChosen[i] = false;
		}
		int count = 0;
		while (count < numSelectCandidate) {
			int idx = rnd.sample(); // randomly chooses a candidate
			if (!isChosen[idx]) { // if this candidate is not chosen
				if (idx < edgeCandidateList.size()) { // select edge
					Edge selectEdge = edgeCandidateList.get(idx);
					action.addOrNodeAttack(selectEdge.gettarget(), selectEdge);
				} else { // select node, this is for AND node only
					Node selectNode = nodeCandidateList.get(idx - edgeCandidateList.size());
					action.addAndNodeAttack(selectNode, depGraph.incomingEdgesOf(selectNode));
				}
				
				isChosen[idx] = true; // set chosen to be true
				count++;
			}	
		}
		edgeCandidateList.clear();
		nodeCandidateList.clear();
		return action;
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
