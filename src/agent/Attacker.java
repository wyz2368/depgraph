package agent;

import java.util.List;

import model.AttackCandidate;
import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomGenerator;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

public abstract class Attacker {
	public enum AttackerType {
		UNIFORM, VALUE_PROPAGATION, RANDOM_WALK;
		
		@Override
		public String toString() {
			switch(this) {
			case UNIFORM: return "UN";
			case VALUE_PROPAGATION: return "VP";
			case RANDOM_WALK: return "RW";
			default: return "";
			}
		}
	}
	
	public enum AttackerParam {
		MAX_NUM_SELECT_CAND, MIN_NUM_SELECT_CAND,
		NUM_SELECT_CAND_RATIO, QR_PARAM, NUM_RW_SAMPLE, STDEV;
		
		@Override
		public String toString() {
			switch(this) {
			case MAX_NUM_SELECT_CAND: return "maxNumSelectCandidate";
			case MIN_NUM_SELECT_CAND: return "minNumSelectCandidate";
			case NUM_SELECT_CAND_RATIO: return "numSelectCandidateRatio";
			case QR_PARAM: return "qrParam";
			case NUM_RW_SAMPLE: return "numRWSample";
			case STDEV: return "stdev";
			default: return "";
			}	
		}
	}
	
	private AttackerType attType;

	public Attacker(final AttackerType aAttType) {
		if (aAttType == null) {
			throw new IllegalArgumentException();
		}
		this.attType = aAttType;
	}

	public final AttackerType getAType() {
		return this.attType;
	}

	public abstract AttackerAction sampleAction(
		DependencyGraph depGraph, 
		int curTimeStep, 
		int numTimeStep, 
		RandomGenerator rng);
	
	public abstract List<AttackerAction> sampleAction(
		DependencyGraph depGraph, 
		int curTimeStep,
		int numTimeStep, 
		RandomGenerator rng, 
		int numSample, 
		boolean isReplacement);
	
	private static boolean hasInactiveTarget(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		for (final Node target : depGraph.getTargetSet()) {
			if (target.getState() == NodeState.INACTIVE) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isCandidate(final Node node, final DependencyGraph depGraph) {
		if (node == null || depGraph == null) {
			throw new IllegalArgumentException();
		}
		if (node.getActivationType() == NodeActivationType.AND) {
			// this node is AND type
			for (Edge inEdge : depGraph.incomingEdgesOf(node)) {
				if (inEdge.getsource().getState() == NodeState.INACTIVE) {
					return false;
				}
			}
			return true;
		}
		// this node is OR type
		for (Edge inEdge : depGraph.incomingEdgesOf(node)) {
			if (inEdge.getsource().getState() == NodeState.ACTIVE) {
				return true;
			}
		}
		return false;
	}
	
	/*****************************************************************************************
	* @param depGraph: dependency graph
	* @param curTimeStep: current time step 
	* @param numTimeStep: total number of time step
	* @return type of AttackCandidate: candidate set for the attacker
	*****************************************************************************************/
	public static final AttackCandidate getAttackCandidate(final DependencyGraph depGraph) {
		if (depGraph == null) {
			throw new IllegalArgumentException();
		}
		final AttackCandidate result = new AttackCandidate();
		if (!hasInactiveTarget(depGraph)) {
			return result;
		}
		
		for (final Node node : depGraph.vertexSet()) {
			if (node.getState() == NodeState.INACTIVE && isCandidate(node, depGraph)) {
				// only add inactive candidate nodes
				if (node.getActivationType() == NodeActivationType.AND) {
					// if AND node, then add node to the candidate set
					result.addNodeCandidate(node);
				} else {
					// if OR node, then add all in-edges to the candidate set
					for (final Edge inEdge: depGraph.incomingEdgesOf(node)) {
						if (inEdge.getsource().getState() == NodeState.ACTIVE) {
							result.addEdgeCandidate(inEdge);
						}
					}
				}
			}
		}
		return result;
	}
	
	public static int getActionCount(
		final int strategyMin,
		final int strategyMax,
		final int availableMax,
		final int idealCount
	) {
		if (
			strategyMin > strategyMax
			|| strategyMin < 0
			|| strategyMax < 0
			|| availableMax < 0
		) {
			throw new IllegalArgumentException();
		}
		
		if (availableMax < strategyMin) {
			// not enough available to even have strategyMin.
			return availableMax;
		}
		int result = idealCount;
		// result can't be less than strategyMin.
		result = Math.max(result, strategyMin);
		// result can't be more than strategyMax.
		result = Math.min(result, strategyMax);
		// result can't be more than availableMax.
		result = Math.min(result, availableMax);
		return result;
	}
	
	public static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}
