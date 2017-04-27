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
	
	/*****************************************************************************************
	* @param depGraph: dependency graph
	* @param curTimeStep: current time step 
	* @param numTimeStep: total number of time step
	* @return type of AttackCandidate: candidate set for the attacker
	*****************************************************************************************/
	public static final AttackCandidate selectCandidate(final DependencyGraph depGraph) {
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
						} else { // if OR node, then add edges to the candidate set
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
			return availableMax;
		}
		int result = idealCount;
		// can't be less than strategyMin
		result = Math.max(result, strategyMin);
		// can't choose more than are available
		result = Math.min(result, strategyMax);
		// can't choose more than are available
		result = Math.min(result, availableMax);
		return result;
	}
}
