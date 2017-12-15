package rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import graph.Node;
import model.DefenderAction;

public final class RLDefenderAction {

	private List<Integer> nodeIds = new ArrayList<Integer>();

	public RLDefenderAction(final DefenderAction action) {
		for (final Node node: action.getAction()) {
			this.nodeIds.add(node.getId());
		}
		Collections.sort(this.nodeIds);
	}

	public List<Integer> getNodeIds() {
		return this.nodeIds;
	}

	@Override
	public String toString() {
		return "RLDefenderAction [nodeIds=" + this.nodeIds + "]";
	}
}
