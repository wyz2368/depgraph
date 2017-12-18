package rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.Node;
import model.DefenderAction;

public final class RLDefenderAction {

	private final List<Integer> nodeIds = new ArrayList<Integer>();
	
	private final Set<Integer> nodeIdSet;

	public RLDefenderAction(final DefenderAction action) {
		for (final Node node: action.getAction()) {
			this.nodeIds.add(node.getId());
		}
		Collections.sort(this.nodeIds);
		this.nodeIdSet = new HashSet<Integer>(this.nodeIds);
	}

	public List<Integer> getNodeIds() {
		return this.nodeIds;
	}
	
	public Set<Integer> nodeIdSet() {
		return this.nodeIdSet;
	}

	@Override
	public String toString() {
		return "RLDefenderAction [nodeIds=" + this.nodeIds + "]";
	}
}
