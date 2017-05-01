package agent;

import graph.Edge;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;
import graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class RandomWalkAttacker extends Attacker {
	
	public final class RandomWalkTuple {
		// time of activation in random walk
		private int tAct = 0;
		// probability of activation in random walk
		private double pAct = 0.0;
		// parent nodes used for activation in random walk
		private List<Edge> preAct;
		
		public RandomWalkTuple(final int tAct, final double pAct, final List<Edge> preAct) {
			if (tAct < 0 || !isProb(pAct)) {
				throw new IllegalArgumentException(tAct + "\t" + pAct + "\t" + preAct);
			}
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

		@Override
		public String toString() {
			return "RandomWalkTuple [tAct=" + this.tAct + ", pAct=" + this.pAct
				+ ", preAct=" + this.preAct + "]";
		}
	}
	
	private double qrParam;
	private double discFact;
	private static final int DEFAULT_NUM_RW_SAMPLE = 100;
	private int numRWSample = DEFAULT_NUM_RW_SAMPLE;
	
	public RandomWalkAttacker(final double numRWSample, final double qrParam, final double discFact) {
		super(AttackerType.RANDOM_WALK);
		if (numRWSample < 1.0 || !isProb(discFact) || qrParam < 0.0) {
			throw new IllegalArgumentException(numRWSample + "\t" + discFact + "\t" + discFact);
		}
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
	public AttackerAction sampleAction(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || rng == null) {
			throw new IllegalArgumentException();
		}
		// Compute the greedy action values
		final double[] actionValues = new double[this.numRWSample];
		final AttackerAction[] actions = new AttackerAction[this.numRWSample];
		for (int i = 0; i < this.numRWSample; i++) {
			final AttackerAction attAction = new AttackerAction();
			actions[i] = attAction;
			final RandomWalkTuple[] rwSample = randomWalk(depGraph, curTimeStep, rng);
			actionValues[i] = greedyAction(depGraph, rwSample, attAction, numTimeStep, this.discFact);
		}
		
		// Compute probability to choose each rwSample's action
		final double[] probabilities = computeCandidateProb(actionValues, this.qrParam);
		
		// array of [0, 1, . . ., this.numRWSample - 1]
		final int[] rwSampleIndexes = getIndexArray(this.numRWSample);
		// a probability mass function with integer values
		final EnumeratedIntegerDistribution rnd =
			new EnumeratedIntegerDistribution(rng, rwSampleIndexes, probabilities);
		
		final int sampleIdx = rnd.sample();
		return actions[sampleIdx];
	}

	@Override
	public List<AttackerAction> sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep, final RandomGenerator rng,
		final int numSample, final boolean isReplacement) {
		if (depGraph == null || curTimeStep < 0 || numTimeStep < curTimeStep || rng == null || numSample < 1) {
			throw new IllegalArgumentException();
		}
		if (isReplacement) { // this is currently not used, need to check if the isAdded works properly
			final Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
			while (attActionSet.size() < numSample) {
				final AttackerAction attAction = sampleAction(depGraph, curTimeStep, numTimeStep, rng);
				attActionSet.add(attAction);
			}
			return new ArrayList<AttackerAction>(attActionSet);
		}
		// this is currently used, correct
		final List<AttackerAction> attActionList = new ArrayList<AttackerAction>();
		while (attActionList.size() < numSample) {
			final AttackerAction attAction = sampleAction(depGraph, curTimeStep, numTimeStep, rng);
			attActionList.add(attAction);
		}
		return attActionList;
	}
	
	/**
	* @param depGraph dependency graph
	* @param curTimeStep current time step
	* @param rng random generator
	* @return random walk tuple for all nodes
	*/
	public RandomWalkTuple[] randomWalk(final DependencyGraph depGraph,
		final int curTimeStep, final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0 || rng == null) {
			throw new IllegalArgumentException();
		}
		// has a RandomWalkTuple for each node in depGraph
		final RandomWalkTuple[] rwTuples = new RandomWalkTuple[depGraph.vertexSet().size()];

		// get (forward) topographical order over nodes, from roots to leaves.
		final Node[] topoOrder = getTopoOrder(depGraph);

		// Start random walk, from root nodes
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			final Node topoNode = topoOrder[i];
			final RandomWalkTuple newTuple = getRWTuple(topoNode, depGraph, curTimeStep, rwTuples, rng);
			rwTuples[topoNode.getId() - 1] = newTuple;
		}
		return rwTuples;
	}
	
	private RandomWalkTuple getRWTuple(
		final Node node,
		final DependencyGraph depGraph,
		final int curTimeStep,
		final RandomWalkTuple[] rwTuples,
		final RandomGenerator rng
	) {
		if (node == null || depGraph == null || curTimeStep < 0
			|| rwTuples == null || rng == null) {
			throw new IllegalArgumentException();
		}
		if (node.getState() == NodeState.ACTIVE) {
			return getRWTupleActive(node, curTimeStep);
		}
		if (depGraph.inDegreeOf(node) == 0) {
			return getRWTupleInactiveRoot(node, curTimeStep, depGraph);
		}
		if (node.getActivationType() == NodeActivationType.OR) {
			return getRWTupleInactiveNonRootOr(node, depGraph, rwTuples, rng);
		}
		return getRWTupleInactiveNonRootAnd(node, depGraph, rwTuples);
	}
	
	// pAct = 1, tAct = curTimeStep - 1, preAct = null
	private RandomWalkTuple getRWTupleActive(
		final Node node,
		final int curTimeStep
	) {
		if (node == null || node.getState() != NodeState.ACTIVE || curTimeStep < 0) {
			throw new IllegalArgumentException();
		}
		final double pAct = 1.0;
		final int tAct = curTimeStep - 1;
		final List<Edge> preAct = null;
		return new RandomWalkTuple(tAct, pAct, preAct);
 	}
	
	// pAct = p(v), tAct = curTimeStep, preAct = null
	private RandomWalkTuple getRWTupleInactiveRoot(
		final Node node,
		final int curTimeStep,
		final DependencyGraph depGraph
	) {
		if (node == null || node.getState() != NodeState.INACTIVE
			|| curTimeStep < 0 || depGraph.inDegreeOf(node) != 0) {
			throw new IllegalArgumentException();
		}
		final double pAct = node.getActProb();
		final int tAct = curTimeStep;
		final List<Edge> preAct = null;
		return new RandomWalkTuple(tAct, pAct, preAct);
	}
	
	private RandomWalkTuple getRWTupleInactiveNonRootOr(
		final Node node,
		final DependencyGraph depGraph,
		final RandomWalkTuple[] rwTuples,
		final RandomGenerator rng
	) {
		if (node == null || node.getState() != NodeState.INACTIVE
			|| depGraph.inDegreeOf(node) == 0 || node.getActivationType() != NodeActivationType.OR
			|| rwTuples == null || rng == null
		) {
			throw new IllegalArgumentException();
		}
		final List<Edge> inEdges = new ArrayList<Edge>(depGraph.incomingEdgesOf(node));
		final double[] probabilities = new double[inEdges.size()];
		double totalProb = 0.0;
		for (int inEdgeIndex = 0; inEdgeIndex < inEdges.size(); inEdgeIndex++) {
			final Node parent = inEdges.get(inEdgeIndex).getsource();
			// p^{rw}(u, v) \prop pAct(u) * p(u, v)
			probabilities[inEdgeIndex] = rwTuples[parent.getId() - 1].getPAct() * inEdges.get(inEdgeIndex).getActProb();
			totalProb += probabilities[inEdgeIndex];
		}
		
		// normalize probabilities, to sum to 1.0
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] /= totalProb;
		}
		totalProb = 0.0;
		for (int i = 0; i < probabilities.length; i++) {
			if (!isProb(probabilities[i])) {
				// result values must be in [0, 1]
				throw new IllegalStateException();
			}
			totalProb += probabilities[i];
		}
		final double tolerance = 0.0001;
		if (Math.abs(totalProb - 1.0) > tolerance) {
			// total of result must equal 1.0
			throw new IllegalStateException();
		}
		
		// select parent u of node v.
		final int[] nodeIndexes = getIndexArray(inEdges.size());
		final EnumeratedIntegerDistribution rnd = new EnumeratedIntegerDistribution(rng, nodeIndexes, probabilities);
		final int chosenPreIndex = rnd.sample();
		final Edge chosenInEdge = inEdges.get(chosenPreIndex);
		final Node chosenParent = chosenInEdge.getsource();
		
		// pAct(v) = pAct(u) * p(u, v), for edge (u, v)
		final double pAct = rwTuples[chosenParent.getId() - 1].getPAct() * chosenInEdge.getActProb();
		// tAct(v) = tAct(u) + 1
		final int tAct = rwTuples[chosenParent.getId() - 1].getTAct() + 1;
		// preAct(v) = [u]
		final List<Edge> preAct = new ArrayList<Edge>();
		preAct.add(chosenInEdge);
		return new RandomWalkTuple(tAct, pAct, preAct);
	}
	
	private RandomWalkTuple getRWTupleInactiveNonRootAnd(
		final Node node,
		final DependencyGraph depGraph,
		final RandomWalkTuple[] rwTuples
	) {
		if (node == null || node.getState() != NodeState.INACTIVE
			|| depGraph.inDegreeOf(node) == 0 || node.getActivationType() != NodeActivationType.AND
			|| rwTuples == null
		) {
			throw new IllegalArgumentException();
		}
		
		// tAct = [max_{u in pre(v)} tAct(u)] + 1
		// preAct = [all incoming edges of node]
		int tAct = 0;
		final List<Edge> preAct = new ArrayList<Edge>();
		for (final Edge inEdge : depGraph.incomingEdgesOf(node)) {
			preAct.add(inEdge);
			
			final Node parent = inEdge.getsource();
			tAct = Math.max(tAct, rwTuples[parent.getId() - 1].getTAct());
		}
		tAct++;
		
		final boolean[] alreadyInSeq = new boolean[depGraph.vertexSet().size()];
		alreadyInSeq[node.getId() - 1] = true; // node is in the sequence
		final List<Node> sequenceList = new ArrayList<Node>(); 
		for (final Edge inEdge: preAct) {
			final Node parent = inEdge.getsource();
			sequenceList.add(parent);
			alreadyInSeq[parent.getId() - 1] = true;
		}
		// initially, sequenceList contains every parent of node.
		// alreadyInSeq has true for every node that has been in sequenceList, false for all others.
		
		// pAct starts with node's pAct(v), because node is an AND-type node
		double pAct = node.getActProb();
		while (!sequenceList.isEmpty()) {
			final Node curNode = sequenceList.remove(0);
			final RandomWalkTuple curRWTuple = rwTuples[curNode.getId() - 1];
			
			if (curNode.getState() == NodeState.ACTIVE) {
				continue;
			}
			// node is INACTIVE
			if (curNode.getActivationType() == NodeActivationType.AND) { // inactive AND node
				// multiply pAct by p(u) for an AND-type ancestor.
				pAct *= curNode.getActProb();
				
				if (curRWTuple.getPreAct() != null) { // not the root node, has parents
					for (final Edge inEdge: curRWTuple.getPreAct()) {
						final Node parent = inEdge.getsource();
						if (!alreadyInSeq[parent.getId() - 1]) {
							// not already processed.
							// will proceed to parents of the AND-type node.
							sequenceList.add(parent);
							alreadyInSeq[parent.getId() - 1] = true;
						}
					}
				}
			} else {  // inactive OR node. cannot be a root node because OR-type.
				final Edge inEdge = curRWTuple.getPreAct().get(0);
				
				if (curRWTuple.getPreAct().size() != 1) {
					throw new IllegalStateException();
				}
				
				// multiply pAct by p(u, v) for the preAct in-edge of this node
				pAct *= inEdge.getActProb();
				
				final Node parent = inEdge.getsource();
				if (!alreadyInSeq[parent.getId() - 1]) {
					// not already processed.
					// will proceed to parent of the OR-type node.
					sequenceList.add(parent);
					alreadyInSeq[parent.getId() - 1] = true;
				}
			}
		}
		return new RandomWalkTuple(tAct, pAct, preAct);
	}
	
	static double greedyAction(
		final DependencyGraph depGraph, 
		final RandomWalkTuple[] rwTuples, 
		final AttackerAction attAction, 
		final int numTimeStep, 
		final double discFact) {
		if (depGraph == null || rwTuples == null || attAction == null
			|| numTimeStep < 0 || !isProb(discFact)) {
			throw new IllegalArgumentException();
		}
		// list of all target nodes
		final List<Node> targetList = new ArrayList<Node>(depGraph.getTargetSet());
		// sequence to reach greedy target subset
		final boolean[] isInSequence = new boolean[depGraph.vertexSet().size()];
		// will contain the set of targets greedily chosen to attempt to activate
		final Set<Node> greedyTargetSet = new HashSet<Node>();
		// value of the chosen target subset
		double totalValue = 0.0;
		while (true) {
			// Start searching for next best target
			Node targetToAdd = null;
			final boolean[] chosenIsInSequence = new boolean[depGraph.vertexSet().size()];
			final boolean[] isInCurSequence = new boolean[depGraph.vertexSet().size()];
			for (int targetIdx = 0; targetIdx < targetList.size(); targetIdx++) {
				final Node target = targetList.get(targetIdx);
				final RandomWalkTuple targetRWTuple = rwTuples[target.getId() - 1];
				if (target.getState() == NodeState.ACTIVE
					|| greedyTargetSet.contains(target)
					|| targetRWTuple.getTAct() > numTimeStep) {
					continue;
				}
				// Value of target
				// add to total value of previously selected goal targets:
				// pAct(v) * r^a(v) + discFact^{tAct(v) - 1}
				double targetValue = totalValue
					+ targetRWTuple.getPAct() * target.getAReward() * Math.pow(discFact, targetRWTuple.getTAct() - 1);
				for (int j = 0; j < depGraph.vertexSet().size(); j++) {
					isInCurSequence[j] = isInSequence[j];
				}
				if (!isInCurSequence[target.getId() - 1]) { // target node is not in sequence so far
					// Cost of activating the target
					isInCurSequence[target.getId() - 1] = true;
					if (target.getActivationType() == NodeActivationType.AND) {
						targetValue += targetRWTuple.getPAct() / target.getActProb()  
							* target.getACost() * Math.pow(discFact, targetRWTuple.getTAct() - 1);
					} else {
						final Edge chosenEdge = targetRWTuple.getPreAct().get(0);
						targetValue += targetRWTuple.getPAct() / chosenEdge.getActProb() 
							* chosenEdge.getACost() * Math.pow(discFact, targetRWTuple.getTAct() - 1);
					}

					// Start finding sequence of the target
					final List<Node> sequence = new ArrayList<Node>();
					if (targetRWTuple.getPreAct() != null) { // this target is not a root node
						for (final Edge edge : targetRWTuple.getPreAct()) {
							if (edge.getsource().getState() == NodeState.INACTIVE) {
								sequence.add(edge.getsource());
							}
						}
					}
					while (!sequence.isEmpty()) {
						final Node curNode = sequence.remove(0);
						final RandomWalkTuple curRwTuple = rwTuples[curNode.getId() - 1];
						if (!isInCurSequence[curNode.getId() - 1]) {
							isInCurSequence[curNode.getId() - 1] = true;
							if (curNode.getActivationType() == NodeActivationType.AND) { // AND node
								targetValue += curRwTuple.getPAct() / curNode.getActProb()  
									* curNode.getACost() * Math.pow(discFact, curRwTuple.getTAct() - 1);
								if (curRwTuple.getPreAct() != null) { // not root node
									for (final Edge inEdge: curRwTuple.getPreAct()) {
										final Node parent = inEdge.getsource();
										if (!isInCurSequence[parent.getId() - 1] 
												&& parent.getState() == NodeState.INACTIVE) {
											isInCurSequence[parent.getId() - 1] = true;
											sequence.add(parent);
										}
									}
								}
							} else { // OR node
								if (curRwTuple.getPreAct() != null) {
									final Edge chosenEdge = curRwTuple.getPreAct().get(0);
									targetValue += curRwTuple.getPAct() / chosenEdge.getActProb() 
										* chosenEdge.getACost() * Math.pow(discFact, curRwTuple.getTAct() - 1);
									if (!isInCurSequence[chosenEdge.getsource().getId() - 1]
										&& chosenEdge.getsource().getState() == NodeState.INACTIVE) {
										isInCurSequence[chosenEdge.getsource().getId() - 1] = true;
										sequence.add(chosenEdge.getsource());
									}
								}
							}
						}
					}
				}
				if (targetValue > totalValue) {
					// including target goal node would increase total value of set,
					// by at least as much as any previously considered target to add
					// to the current greedy set of targets.
					totalValue = targetValue;
					targetToAdd = target;
					for (int j = 0; j < depGraph.vertexSet().size(); j++) {
						chosenIsInSequence[j] = isInCurSequence[j];
					}
				}
			}
			if (targetToAdd == null) {
				// no target was chosen this iteration. stop iterating.
				break;
			}
			
			// some target was chosen. continue iterating.
			for (int j = 0; j < depGraph.vertexSet().size(); j++) {
				isInSequence[j] = chosenIsInSequence[j];
			}
			greedyTargetSet.add(targetToAdd);
		}

		// Find corresponding candidate set for chosen subset of targets
		if (!greedyTargetSet.isEmpty()) {
			addAncestorsToAttackSet(attAction, depGraph, isInSequence, rwTuples);
		}
		if (Double.isNaN(totalValue)) {
			throw new IllegalStateException();
		}
		return totalValue;
	}
	
	private static void addAncestorsToAttackSet(
		final AttackerAction attAction,
		final DependencyGraph depGraph,
		final boolean[] isInSequence,
		final RandomWalkTuple[] rwTuples
	) {
		if (attAction == null || depGraph == null || isInSequence == null || rwTuples == null
			|| !attAction.getActionCopy().keySet().isEmpty()) {
			throw new IllegalArgumentException();
		}
		for (final Node node: depGraph.vertexSet()) {
			if (isInSequence[node.getId() - 1] && node.getState() == NodeState.INACTIVE) {
				// only add inactive nodes that are in the activation sequence of a selected
				// target node.
				if (node.getActivationType() == NodeActivationType.AND) {
					boolean isCandidate = true;
					for (final Edge inEdge : depGraph.incomingEdgesOf(node)) {
						if (inEdge.getsource().getState() == NodeState.INACTIVE) {
							// some parent of the AND-type node is not active.
							// will not add this node.
							isCandidate = false;
							break;
						}
					}
					if (isCandidate) {
						// and the AND-type node and its in-edges to attack set
						attAction.addAndNodeAttack(node, depGraph.incomingEdgesOf(node));
					}
				} else {
					// FIXME shouldn't we include an OR-type node even if its selected parent is INACTIVE,
					// if it is in the candidate set (i.e., any parent is ACTIVE), and in isInSequence?
					final RandomWalkTuple rwTuple = rwTuples[node.getId() - 1];
					final Edge inEdge = rwTuple.getPreAct().get(0);
					if (inEdge.getsource().getState() == NodeState.ACTIVE) {
						// the selected parent of the OR-type node is active. will add this node.
						// and the OR-type node and its chosen in-edge to attack set
						attAction.addOrNodeAttack(node, inEdge);
					}
				}
			}
		}
	}
	
	public static double computeAttackerValue(
		final DependencyGraph depGraph,
		final AttackerAction attAction,
		final RandomWalkTuple[] rwTuples,
		final double aDiscFact,
		final int curTimeStep,
		final int numTimeStep) {
		if (depGraph == null || attAction == null || rwTuples == null
			|| !isProb(aDiscFact) || curTimeStep < 0 || numTimeStep < curTimeStep) {
			throw new IllegalArgumentException();
		}
		double value = 0.0;
		final boolean[] isInQueue = new boolean[depGraph.vertexSet().size()];

		for (final Entry<Node, Set<Edge>> entry: attAction.getActionCopy().entrySet()) {
			final Node node = entry.getKey();
			isInQueue[node.getId() - 1] = true; 
			final double discValue = Math.pow(aDiscFact, curTimeStep - 1);
			if (node.getActivationType() == NodeActivationType.AND) {
				value += node.getACost() * discValue;
			} else {
				for (final Edge edge : entry.getValue()) {
					value += edge.getACost() * discValue;
				}
			}
		}
		final Node[] topoOrder = getTopoOrder(depGraph);
		for (int i = 0; i < depGraph.vertexSet().size(); i++) {
			final Node topoNode = topoOrder[i];
			final RandomWalkTuple rwTuple = rwTuples[topoNode.getId() - 1];
			final List<Edge> preEdgeList = rwTuple.getPreAct();
			if (preEdgeList != null) { // not active and not root nodes
				isInQueue[topoNode.getId() - 1] = false;
				if (topoNode.getActivationType() == NodeActivationType.AND) {
					isInQueue[topoNode.getId() - 1] = true;
					for (final Edge inEdge : preEdgeList) {
						if (!isInQueue[inEdge.getsource().getId() - 1]) {
							isInQueue[topoNode.getId() - 1] = false;
							break;
						}
					}
				} else {
					final Node parent = preEdgeList.get(0).getsource();
					if (isInQueue[parent.getId() - 1]) {
						isInQueue[topoNode.getId() - 1] = true;
					}
				}
			}
		}
		for (final Node target : depGraph.getTargetSet()) {
			final RandomWalkTuple rwTuple = rwTuples[target.getId() - 1];
			final int actTime = rwTuple.getTAct();
			if (isInQueue[target.getId() - 1] && actTime <= numTimeStep) {
				value += rwTuple.getPAct() * Math.pow(aDiscFact, actTime - 1) * target.getAReward();
			}
		}
		return value;
	}

	public double getQrParam() {
		return this.qrParam;
	}

	public double getDiscFact() {
		return this.discFact;
	}

	public int getNumRWSample() {
		return this.numRWSample;
	}

	@Override
	public String toString() {
		return "RandomWalkAttacker [qrParam=" + this.qrParam + ", discFact="
			+ this.discFact + ", numRWSample=" + this.numRWSample + "]";
	}
}
