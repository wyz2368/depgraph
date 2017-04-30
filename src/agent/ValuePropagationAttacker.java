package agent;

import graph.Edge;
import graph.INode.NodeActivationType;
import graph.Node;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	// TODO make propagationParam a parameter from simulation_spec
	private final double propagationParam = 0.5;
	// TODO make useMaxOnly a parameter from simulation_spec
	private final boolean useMaxOnly = true;
	
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
		final AttackCandidate attackCandidate = getAttackCandidate(depGraph);
		if (attackCandidate.isEmpty()) {
			return new AttackerAction();
		}

		// Compute candidate values
		final double[] candidateVals = computeCandidateValueTopo(
			depGraph, 
			attackCandidate, 
			curTimeStep, 
			numTimeStep, 
			this.discFact, 
			this.propagationParam,
			this.useMaxOnly);
		
		// Compute number of candidates to select
		final int totalNumCandidate = attackCandidate.getEdgeCandidateSet().size() 
			+ attackCandidate.getNodeCandidateSet().size();
		final int goalCount = 
			(int) (totalNumCandidate * this.numSelectCandidateRatio + rng.nextGaussian() * this.numCandStdev);
		final int numToSelect =
			getActionCount(this.minNumSelectCandidate, this.maxNumSelectCandidate, totalNumCandidate, goalCount);
		if (numToSelect == 0) { // if there is no candidate to select
			return new AttackerAction();
		}
		
		// Compute probability to choose each node
		final double[] probabilities = computeCandidateProb(candidateVals, this.qrParam);
		
		// array of [0, 1, . . ., totalNumCandidate - 1]
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
		final AttackCandidate attackCandidate = getAttackCandidate(depGraph);
		if (attackCandidate.isEmpty()) {
			List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
			attActionList.add(new AttackerAction());
			return attActionList;
		}
		// Compute candidate value
		final double[] candidateVals = computeCandidateValueTopo(
			depGraph, 
			attackCandidate, 
			curTimeStep, 
			numTimeStep, 
			this.discFact, 
			this.propagationParam,
			this.useMaxOnly);
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
			final double range = maxVal - minVal;
			for (int i = 0; i < vals.length; i++) {
				vals[i] = (vals[i] - minVal) / range;
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
			if (!isProb(normalVals[i])) {
				// normalized values must be in [0, 1]
				throw new IllegalArgumentException();
			}
		}
		
		final double[] result = new double[normalVals.length];
		double totalProb = 0.0;
		for (int i = 0; i < result.length; i++) {
			result[i] = Math.exp(qrParam * normalVals[i]);
			totalProb += result[i];
		}
		for (int i = 0; i < result.length; i++) {
			result[i] /= totalProb;
		}
		
		totalProb = 0.0;
		for (int i = 0; i < result.length; i++) {
			if (!isProb(result[i])) {
				// result values must be in [0, 1]
				throw new IllegalStateException();
			}
			totalProb += result[i];
		}
		final double tolerance = 0.0001;
		if (Math.abs(totalProb - 1.0) > tolerance) {
			// total of result must equal 1.0
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

		// Normalize each candidate value: map to [0, 1] as (val - min) / (max - min).
		normalize(candidateVals);
		
		// Compute probability, using quantal response distribution.
		return getProbsFromNormalizedVals(candidateVals, qrParam);
	}
	
	// get target nodes (i.e., goal nodes) that are in the INACTIVE state, as a list
	private static List<Node> getInactiveTargets(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		final List<Node> result = new ArrayList<Node>();
		for (final Node node: depGraph.getTargetSet()) {
			if (node.getState() == NodeState.INACTIVE) {
				result.add(node);
			}
		}
		return result;
	}
	
	private static Node[] getTopoOrder(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		final Node[] result = new Node[depGraph.vertexSet().size()];
		for (final Node node : depGraph.vertexSet()) {
			result[node.getTopoPosition()] = node;
		}
		if (!isValidTopoOrder(result, depGraph)) {
			throw new IllegalStateException();
		}
		return result;
	}
	
	public static boolean isValidTopoOrder(final Node[] input, final DependencyGraph depGraph) {
		if (input == null || depGraph == null || depGraph.vertexSet().size() != input.length) {
			throw new IllegalArgumentException();
		}
		
		final List<Node> inputList = Arrays.asList(input);
		final Set<Integer> idSet = new HashSet<Integer>();
		for (final Node node: inputList) {
			idSet.add(node.getId());
		}
		for (int i = 1; i <= input.length; i++) {
			if (!idSet.contains(i)) {
				System.out.println("Missing value: " + i);
				System.out.println(idSet);
				return false;
			}
		}
		// all Ids in {1, 2, . . ., input.length} are present

		for (final Node node: input) {
			final Set<Edge> outEdges = depGraph.outgoingEdgesOf(node);
			for (final Edge outEdge: outEdges) {
				if (inputList.indexOf(outEdge.gettarget()) < inputList.indexOf(node)) {
					System.out.println("Out-of-order pair:");
					System.out.println("Edge: " + outEdge);
					return false;
				}
			}
		}
		// every Node's children are after that Node.
		return true;
	}
	
	private static int inactiveInEdgeCount(
		final Node node,
		final DependencyGraph depGraph
	) {
		if (node == null || depGraph == null) {
			throw new IllegalArgumentException();
		}
		int result = 0;
		for (final Edge nodeInEdge: depGraph.incomingEdgesOf(node)) {
			if (nodeInEdge.getsource().getState() == NodeState.INACTIVE) {
				result++;
			}
		}
		return result;
	}
	
	private static double[] computeCandidateValueTopo(
		final DependencyGraph depGraph, 
		final AttackCandidate attackCand, 
		final int curTimeStep, 
		final int numTimeStep, 
		final double discountFactor, 
		final double propagationParam,
		final boolean useMaxOnly
	) {
		if (depGraph == null || attackCand == null || attackCand.isEmpty()
			|| curTimeStep < 0 || numTimeStep < curTimeStep
			|| discountFactor < 0.0 || discountFactor > 1.0
			|| propagationParam < 0.0
		) {
			throw new IllegalArgumentException();
		}
		
		// target nodes (i.e., goal nodes) that are in the INACTIVE state, as a list
		final List<Node> inactiveTargets = getInactiveTargets(depGraph);

		// Value propagation of each node with respect to each
		// inactive target and propagation time (>= curTimeStep & <= numTimeStep).
		// maps inactiveTargetIndex, to future time step index, to node index, to value.
		final double[][][] r =
			new double[inactiveTargets.size()][numTimeStep - curTimeStep + 1][depGraph.vertexSet().size()];
		
		// r^w(w, t) = r^a(w), for all w that are inactive target (goal) nodes.
		for (int inactIndex = 0; inactIndex < inactiveTargets.size(); inactIndex++) {
			final Node inactiveTarget = inactiveTargets.get(inactIndex);
			// value of each inactive target, and current time step index (i.e., 0), at its own ID,
			// is the attacker reward of the target.
			// current time step t maps to 0 in the array.
			r[inactIndex][0][inactiveTarget.getId() - 1] = inactiveTarget.getAReward();
		}

		// get (forward) topographical order over nodes, from roots to leaves.
		final Node[] topoOrder = getTopoOrder(depGraph);
		
		// iterate over nodes in reverse topological order (i.e., leaf node first)
		for (int topoIndex = depGraph.vertexSet().size() - 1; topoIndex >= 0; topoIndex--) {
			final Node curNode = topoOrder[topoIndex];
			if (curNode.getState() == NodeState.ACTIVE) {
				// skip active nodes
				continue;
			}
			final Set<Edge> curOutEdges = depGraph.outgoingEdgesOf(curNode);
			for (final Edge outEdge: curOutEdges) { // examine each outgoing edge of curNode
				final Node childNode = outEdge.gettarget();
				if (childNode.getState() == NodeState.ACTIVE) {
					// skip active child nodes
					continue;
				}
				// curNode and childNode are both INACTIVE.
				// propagate value from childNode to curNode
				for (int inactIndex = 0; inactIndex < inactiveTargets.size(); inactIndex++) {
					// iterating over inactive goal nodes
					for (int timeIndex = 1; timeIndex <= numTimeStep - curTimeStep; timeIndex++) {
						// iterating over future times
						double rHat = 0.0;
						
						if (childNode.getActivationType() == NodeActivationType.AND) {
							// childNode is of type AND.
							// rHat = [c^a(v) + p(v) * r^w(v, t - 1)] / inactiveInEdgeCount^{propagationParam}
							
							// start with the cost to act on childNode.
							rHat = childNode.getACost();
							
							// add childNode's previous time step value, times childNode's activation
							// probability.
							rHat += childNode.getActProb() * r[inactIndex][timeIndex - 1][childNode.getId() - 1];

							// divide by count of inactive in-edges of childNode, to power of propagationParam.
							final int inactiveInEdgeCount =
								inactiveInEdgeCount(childNode, depGraph);							
							rHat /= Math.pow(inactiveInEdgeCount, propagationParam);
						} else  {
							// childNode is of type OR.
							// rHat = c^a(u, v) + p(u, v) * r^w(v, t - 1)
							
							// start with the cost to act on outEdge.
							rHat = outEdge.getACost();
							
							// add childNode's previous time step value, times outEdge's activation
							// probability.
							rHat += outEdge.getActProb() * r[inactIndex][timeIndex - 1][childNode.getId() - 1];
						}
						
						if (useMaxOnly) {
							// only keep maximum propagation value
							if (r[inactIndex][timeIndex][curNode.getId() - 1] < discountFactor * rHat) {
								// r^w(u, t) < discountFactor * rHat
									// r^w(u, t) <- discountFactor * rHat
								r[inactIndex][timeIndex][curNode.getId() - 1] = discountFactor * rHat;
							}
						} else { // sum
							// TODO this version is not documented in Overleaf doc (algorithm 2)
							if (rHat > 0) {
								r[inactIndex][timeIndex][curNode.getId() - 1] += discountFactor * rHat;
							}
						}
					}
				}
			}
		}
		
		// get aggregate propagation values over all inactive nodes.
		final double[] rAggregate =
			aggregateValues(r, depGraph, inactiveTargets.size(), numTimeStep, curTimeStep, useMaxOnly);
		if (rAggregate.length != depGraph.vertexSet().size()) {
			throw new IllegalStateException();
		}
		
		// compute final propagation value for each candidate, considering cost and activation probability
		final double[] result = getCandVals(attackCand, rAggregate);
		return result;
	}
	
	// TODO: change Overleaf doc: does not make sense to multiply each by timeStep^discountFactor,
	// because effect on probability of selection cancels out across different candidates.
	private static double[] getCandVals(
		final AttackCandidate attackCand,
		final double[] rAggregate
	) {
		if (attackCand == null || rAggregate == null) {
			throw new IllegalArgumentException();
		}
		final List<Edge> edgeCands = new ArrayList<Edge>(attackCand.getEdgeCandidateSet());
		final List<Node> nodeCands = new ArrayList<Node>(attackCand.getNodeCandidateSet());
		final int candCount = edgeCands.size() + nodeCands.size();
		
		final double[] result = new double[candCount];
		int i = 0;
		for (final Edge edgeCand: edgeCands) {
			// r(e = v, u) <- c^a(e) + p(e) * rHat(u), where u is the end node of the edge.
			result[i] = (edgeCand.getACost() + edgeCand.getActProb() * rAggregate[edgeCand.gettarget().getId() - 1]);
			i++;
		}
		for (final Node nodeCand : nodeCands) {
			// r(u) <- c^a(u) + p(u) * rHat(u)
			result[i] = (nodeCand.getACost() + nodeCand.getActProb() * rAggregate[nodeCand.getId() - 1]);
			i++;
		}
		return result;
	}
	
	private static double[] aggregateValues(
		final double[][][] r,
		final DependencyGraph depGraph,
		final int inactiveTargetCount,
		final int numTimeStep,
		final int curTimeStep,
		final boolean useMaxOnly
	) {
		if (r == null || depGraph == null || inactiveTargetCount < 1 || curTimeStep < 0 || numTimeStep < curTimeStep) {
			throw new IllegalArgumentException();
		}
		
		// result will have an aggregate value for every node in depGraph
		final double[] result = new double[depGraph.vertexSet().size()];
		
		// iterate over inactive target (goal) nodes
		for (int inactIndex = 0; inactIndex < inactiveTargetCount; inactIndex++) {
			// iterate over future time steps
			for (int timeIndex = 0; timeIndex <= numTimeStep - curTimeStep; timeIndex++) {
				// iterate over all nodes
				for (int nodeIndex = 0; nodeIndex < depGraph.vertexSet().size(); nodeIndex++) {
					if (useMaxOnly) { // max
						// TODO this is not documented in Overleaf doc (algorithm 2)
						if (result[nodeIndex] < r[inactIndex][timeIndex][nodeIndex]) {
							result[nodeIndex] = r[inactIndex][timeIndex][nodeIndex];
						}
					} else { // sum
						if (r[inactIndex][timeIndex][nodeIndex] > 0) {
							result[nodeIndex] += r[inactIndex][timeIndex][nodeIndex];
						}
					}
				}
			}
		}
		return result;
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

	public boolean useMaxOnly() {
		return this.useMaxOnly;
	}
	
	@Override
	public String toString() {
		return "ValuePropagationAttacker [maxNumSelectCandidate="
			+ this.maxNumSelectCandidate + ", minNumSelectCandidate="
			+ this.minNumSelectCandidate + ", numSelectCandidateRatio="
			+ this.numSelectCandidateRatio + ", qrParam=" + this.qrParam
			+ ", discFact=" + this.discFact + ", numCandStdev=" + this.numCandStdev
			+ ", propagationParam=" + this.propagationParam + ", useMaxOnly="
			+ this.useMaxOnly + "]";
	}

	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
