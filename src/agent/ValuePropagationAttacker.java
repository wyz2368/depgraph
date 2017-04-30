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
	private double numCandStdev;
	
	private final double propagationParam = 0.5;
	private final boolean isBest = true;
	
	public ValuePropagationAttacker(
		final double maxNumSelectCandidate, 
		final double minNumSelectCandidate,
		final double numSelectCandidateRatio,
		final double qrParam,
		final double discFact,
		final double numCandStdev) {
		super(AttackerType.VALUE_PROPAGATION);
		if (minNumSelectCandidate < 1 || maxNumSelectCandidate < minNumSelectCandidate
			|| !isProb(numSelectCandidateRatio)
			|| numCandStdev < 0.0 || qrParam < 0.0) {
			throw new IllegalArgumentException();
		}
		this.maxNumSelectCandidate = (int) maxNumSelectCandidate;
		this.minNumSelectCandidate = (int) minNumSelectCandidate;
		this.numSelectCandidateRatio = numSelectCandidateRatio;
		this.qrParam = qrParam;
		this.discFact = discFact;
		this.numCandStdev = numCandStdev;
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
		final AttackCandidate attackCandidate = selectCandidate(depGraph);
		// Compute candidate value
		final double[] candidateVals = computeCandidateValueTopo(
			depGraph, 
			attackCandidate, 
			curTimeStep, 
			numTimeStep, 
			this.discFact, 
			this.propagationParam,
			this.isBest);
		final int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() 
			+ attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		final int goalCount = 
			(int) (totalNumCandidate * this.numSelectCandidateRatio + rng.nextGaussian() * this.numCandStdev);
		final int numToSelect =
			getActionCount(this.minNumSelectCandidate, this.maxNumSelectCandidate, totalNumCandidate, goalCount);
		if (numToSelect == 0) { // if there is no candidate to select
			return new AttackerAction();
		}
		
		// Compute probability to choose each node
		final double[] probabilities = computeCandidateProb(candidateVals, this.qrParam);
		
		// Sampling
		final int[] nodeIndexes = getNodeIndexes(totalNumCandidate);
		// a probability mass function with integer values
		final EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);

		return sampleAction(depGraph, attackCandidate, numToSelect, rnd);
	}
	
	// get list of length count, with values {0, 1, . . ., count - 1}
	private static int[] getNodeIndexes(final int count) {
		if (count < 1) {
			throw new IllegalArgumentException();
		}
		final int[] result = new int[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = i;
		}
		return result;
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
		final AttackCandidate attackCandidate = selectCandidate(depGraph);
		// Compute candidate value
		final double[] candidateVals = computeCandidateValueTopo(
			depGraph, 
			attackCandidate, 
			curTimeStep, 
			numTimeStep, 
			this.discFact, 
			this.propagationParam,
			this.isBest);
		final int totalNumCandidate =
			attackCandidate.getEdgeCandidateSet().size() + attackCandidate.getNodeCandidateSet().size();
		
		// Compute number of candidates to select
		final int goalCount = 
			(int) (totalNumCandidate * this.numSelectCandidateRatio + rng.nextGaussian() * this.numCandStdev);
		final int numSelectCandidate =
			getActionCount(this.minNumSelectCandidate, this.maxNumSelectCandidate, totalNumCandidate, goalCount);
		if (numSelectCandidate == 0) { // if there is no candidate
			List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
			attActionList.add(new AttackerAction());
			return attActionList;
		}
		
		// Compute probability to choose each node
		final double[] probabilities = computeCandidateProb(candidateVals, this.qrParam);
		
		// Sampling
		final int[] nodeIndexes = getNodeIndexes(totalNumCandidate);
		
		final EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		if (isReplacement) { // this is currently not used, need to check if the isAdded works properly
			final Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
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
		final List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
		for (int i = 0; i < numSample; i++) {
			final AttackerAction attAction = sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
			attActionList.add(attAction);
		}
		return attActionList;
	}
	
	private static double max(final double[] input) {
		if (input == null || input.length == 0) {
			throw new IllegalArgumentException();
		}
		double result = Double.MIN_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] > result) {
				result = input[i];
			}
		}
		return result;
	}
	
	private static double min(final double[] input) {
		if (input == null || input.length == 0) {
			throw new IllegalArgumentException();
		}
		double result = Double.MAX_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] < result) {
				result = input[i];
			}
		}
		return result;
	}
	
	// for each value in vals,
	// replace with (val - min) / (max - min).
	private static void normalize(final double[] vals) {
		if (vals == null || vals.length == 0) {
			throw new IllegalArgumentException();
		}
		final double minVal = min(vals);
		final double maxVal = max(vals);
		if (minVal == maxVal) {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = 0.0;
			}
		} else {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = (vals[i] - minVal) / (maxVal - minVal);
			}
		}
	}
	
	// given values in normalVals in [0, 1] and qrParam.
	// first map each val to exp(val * qrParam).
	// then normalize to a probability vector, by dividing
	// each result by the total (so all will sum to 1.0).
	private static double[] getProbsFromNormalizedVals(
		final double[] normalVals,
		final double qrParam
	) {
		if (normalVals == null || normalVals.length == 0 || qrParam < 0.0) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < normalVals.length; i++) {
			if (normalVals[i] < 0.0 || normalVals[0] > 1.0) {
				throw new IllegalArgumentException();
			}
		}
		final double[] result = new double[normalVals.length];
		double totalProb = 0.0;
		for (int i = 0; i < normalVals.length; i++) {
			result[i] = Math.exp(qrParam * normalVals[i]);
			totalProb += result[i];
		}
		for (int i = 0; i < result.length; i++) {
			result[i] /= totalProb;
		}
		totalProb = 0.0;
		for (int i = 0; i < result.length; i++) {
			if (result[i] < 0.0 || result[i] > 1.0) {
				throw new IllegalStateException();
			}
			totalProb += result[i];
		}
		final double tolerance = 0.0001;
		if (Math.abs(totalProb - 1.0) > tolerance) {
			throw new IllegalStateException();
		}
		return result;
	}
	
	/*****************************************************************************************
	 * @param candidateValue: candidate values
	 * @return QR distribution over candidates
	 *****************************************************************************************/
	private static double[] computeCandidateProb(
		final double[] candidateVals,
		final double qrParam) {
		if (candidateVals == null || candidateVals.length == 0 || qrParam < 0.0) {
			throw new IllegalArgumentException();
		}

		//Normalize candidate value: map each to [0, 1] as (val - min) / (max - min).
		normalize(candidateVals);
		
		// Compute probability, using quantal response distribution.
		return getProbsFromNormalizedVals(candidateVals, qrParam);
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
		final AttackerAction action = new AttackerAction();
		final List<Edge> edgeCandidateList = new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		final List<Node> nodeCandidateList = new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		final int totalNumCandidate = edgeCandidateList.size() + nodeCandidateList.size();

		// check if this candidate is already chosen
		final boolean[] isChosen = new boolean[totalNumCandidate];
		for (int i = 0; i < totalNumCandidate; i++) {
			isChosen[i] = false;
		}
		int count = 0;
		while (count < numSelectCandidate) {
			int idx = rnd.sample(); // randomly chooses a candidate
			if (!isChosen[idx]) { // if this candidate is not chosen
				if (idx < edgeCandidateList.size()) { // select edge
					final Edge selectEdge = edgeCandidateList.get(idx);
					action.addOrNodeAttack(selectEdge.gettarget(), selectEdge);
				} else { // select node, this is for AND node only
					final Node selectNode = nodeCandidateList.get(idx - edgeCandidateList.size());
					action.addAndNodeAttack(selectNode, depGraph.incomingEdgesOf(selectNode));
				}
				
				isChosen[idx] = true; // set chosen to be true
				count++;
			}	
		}
		return action;
	}
	
	public int getMaxNumSelectCandidate() {
		return this.maxNumSelectCandidate;
	}

	public int getMinNumSelectCandidate() {
		return this.minNumSelectCandidate;
	}

	public double getNumSelectCandidateRatio() {
		return this.numSelectCandidateRatio;
	}

	public double getQrParam() {
		return this.qrParam;
	}

	public double getDiscFact() {
		return this.discFact;
	}

	public double getNumCandStdev() {
		return this.numCandStdev;
	}

	public double getPropagationParam() {
		return this.propagationParam;
	}

	public boolean isBest() {
		return this.isBest;
	}
	
	@Override
	public String toString() {
		return "ValuePropagationAttacker [maxNumSelectCandidate="
			+ this.maxNumSelectCandidate + ", minNumSelectCandidate="
			+ this.minNumSelectCandidate + ", numSelectCandidateRatio="
			+ this.numSelectCandidateRatio + ", qrParam=" + this.qrParam
			+ ", discFact=" + this.discFact + ", numCandStdev=" + this.numCandStdev
			+ ", propagationParam=" + this.propagationParam + ", isBest="
			+ this.isBest + "]";
	}

	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
