package agent;

import graph.Edge;
import graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.AttackCandidate;
import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public final class UniformAttacker extends Attacker {
	private int maxNumSelectCandidate;
	private int minNumSelectCandidate;
	private double numSelectCandidateRatio;
	private double numCandStdev;
	
	public UniformAttacker(final double aMaxNumSelectCandidate,
		final double aMinNumSelectCandidate, 
		final double aNumSelectCandidateRatio,
		final double aNumCandStdev) {
		super(AttackerType.UNIFORM);
		if (aMinNumSelectCandidate < 0
			|| aMaxNumSelectCandidate < aMinNumSelectCandidate
			|| aNumSelectCandidateRatio < 0.0 || aNumSelectCandidateRatio > 1.0
			|| aNumCandStdev < 0.0) {
			throw new IllegalArgumentException();
		}
		this.maxNumSelectCandidate = (int) aMaxNumSelectCandidate;
		this.minNumSelectCandidate = (int) aMinNumSelectCandidate;
		this.numSelectCandidateRatio = aNumSelectCandidateRatio;
		this.numCandStdev = aNumCandStdev;
	}
	
	/***************************************
	 * @param depGraph: dependency graph with current game state
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @return type of Attacker Action: an attack action
	 ***************************************/
	@Override
	public AttackerAction sampleAction(
		final DependencyGraph depGraph, 
		final int curTimeStep,
		final int numTimeStep, 
		final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0
			|| numTimeStep < curTimeStep || rng == null) {
			throw new IllegalArgumentException();
		}
		// Select candidate for the attacker
		final AttackCandidate attackCandidate = getAttackCandidate(depGraph);
		
		// Sample number of AND-nodes and OR-edges in attackCandidate sets
		final int totalNumCandidate =
			attackCandidate.getEdgeCandidateSet().size()
			+ attackCandidate.getNodeCandidateSet().size();
		// Compute number of candidates to select
		final int goalCount = 
			(int) (totalNumCandidate * this.numSelectCandidateRatio
				+ rng.nextGaussian() * this.numCandStdev);
		final int numSelectCandidate =
			getActionCount(this.minNumSelectCandidate,
				this.maxNumSelectCandidate, totalNumCandidate, goalCount);

		if (numSelectCandidate == 0) { // if there is no candidate
			return new AttackerAction();
		}
		// Sample nodes
		// NB: lower and upper bounds of UniformIntegerDistribution
		// constructor are both inclusive.
		// therefore, this distribution will select a random 0-based index.
		UniformIntegerDistribution rnd =
			new UniformIntegerDistribution(rng, 0, totalNumCandidate - 1);
		return sampleAction(depGraph, attackCandidate, numSelectCandidate, rnd);
	}
	
	@Override
	/***************************************
	 * @param depGraph: dependency graph with current game state
	 * @param curTimeStep: current time step 
	 * @param numTimeStep: total number of time step
	 * @param rng: random generator
	 * @param numSample: number of actions to sample
	 * @param isReplacement: sampling with replacement or not
	 * @return type of Attacker Action: list of attack actions
	 ***************************************/
	public List<AttackerAction> sampleAction(
		final DependencyGraph depGraph,
		final int curTimeStep, 
		final int numTimeStep, 
		final RandomGenerator rng,
		final int numSample, 
		final boolean isReplacement) {
		if (depGraph == null || curTimeStep < 0
			|| numTimeStep < curTimeStep || rng == null
			|| numSample < 1) {
			throw new IllegalArgumentException();
		}
		// this is currently not used, 
		// need to check if the isAdded works properly
		if (isReplacement) {
			Set<AttackerAction> attActionSet = new HashSet<AttackerAction>();
			int i = 0;
			while (i < numSample) {
				AttackerAction attAction =
					sampleAction(depGraph, curTimeStep, numTimeStep, rng);
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
			AttackerAction attAction =
				sampleAction(depGraph, curTimeStep, numTimeStep, rng);
			attActionList.add(attAction);
		}
		return attActionList;
	}
	
	/***************************************
	* @param depGraph dependency graph
	* @param attackCandidate candidate set
	* @param numSelectCandidate number of candidates to select
	* @param rnd integer distribution randomizer
	* @return type of AttackerAction: an action for the attacker
	 ***************************************/
	private static AttackerAction sampleAction(
		final DependencyGraph depGraph,
		final AttackCandidate attackCandidate, 
		final int numSelectCandidate, 
		final AbstractIntegerDistribution rnd) {
		if (depGraph == null || numSelectCandidate < 0
			|| rnd == null || attackCandidate == null) {
			throw new IllegalArgumentException();
		}
		AttackerAction action = new AttackerAction();
		List<Edge> edgeCandidateList =
			new ArrayList<Edge>(attackCandidate.getEdgeCandidateSet());
		List<Node> nodeCandidateList =
			new ArrayList<Node>(attackCandidate.getNodeCandidateSet());
		int totalNumCandidate =
			edgeCandidateList.size() + nodeCandidateList.size();

		// check if this candidate is already chosen
		boolean[] isChosen = new boolean[totalNumCandidate];
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
					Node selectNode =
						nodeCandidateList.get(idx - edgeCandidateList.size());
					action.addAndNodeAttack(
						selectNode, depGraph.incomingEdgesOf(selectNode));
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
	
	public double getNumCandStdev() {
		return this.numCandStdev;
	}

	@Override
	public String toString() {
		return "UniformAttacker [maxNumSelectCandidate="
			+ this.maxNumSelectCandidate
			+ ", minNumSelectCandidate=" + this.minNumSelectCandidate
			+ ", numSelectCandidateRatio=" + this.numSelectCandidateRatio
			+ ", numCandStdev=" + this.numCandStdev + "]";
	}
}
