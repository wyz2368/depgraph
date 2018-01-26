package rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.Node;
import model.DefenderAction;
import model.DefenderObservation;
import model.SecurityAlert;

/**
 * A "raw" or unprocessed representation of the current game state.
 */
public final class RLDefenderRawObservation {

	/**
	 * A sorted list of the node IDs that the defender observed as apparently
	 * active in the most recent time step.
	 */
	private final List<Integer> activeObservedIds = new ArrayList<Integer>();
	
	/**
	 * The set of node IDs that the defender observed as apparently
	 * active in the most recent time step.
	 */
	private final Set<Integer> activeObservedIdSet;
	
	/**
	 * The set of node IDs that the defender protected in the most recent
	 * time step.
	 */
	private final Set<Integer> defendedIds;
	
	/**
	 * How many time steps remain in the simulation.
	 */
	private final int timeStepsLeft;
	
	/**
	 * Constructor.
	 * @param defObs the defender's observation
	 * @param defAct the defender's action
	 */
	public RLDefenderRawObservation(
		final DefenderObservation defObs,
		final DefenderAction defAct
	) {
		assert defObs != null && defAct != null;
		for (final SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				this.activeObservedIds.add(alert.getNode().getId());
			}
		}
		Collections.sort(this.activeObservedIds);
		this.activeObservedIdSet = new HashSet<Integer>(this.activeObservedIds);
		this.timeStepsLeft = defObs.getTimeStepsLeft();
		this.defendedIds = new HashSet<Integer>();
		if (defAct != null) {
			for (final Node node: defAct.getAction()) {
				this.defendedIds.add(node.getId());
			}
		}
	}

	/**
	 * Get the sorted list of node IDs that appeared active
	 * in the most recent time step.
	 * @return the list of apparently active node IDs
	 */
	public List<Integer> getActiveObservedIds() {
		return this.activeObservedIds;
	}
	
	/**
	 * Get the set of node IDs that appeared active
	 * in the most recent time step.
	 * @return the set of apparently active node IDs
	 */
	public Set<Integer> activeObservedIdSet() {
		return this.activeObservedIdSet;
	}
	
	/**
	 * Get the number of time steps remaining.
	 * @return the number of time steps left
	 */
	public int getTimeStepsLeft() {
		return this.timeStepsLeft;
	}
	
	/**
	 * Get the set of node IDs that the defender
	 * protected in the latest time step.
	 * @return the set of protected node IDs
	 */
	public Set<Integer> getDefendedIds() {
		return this.defendedIds;
	}

	@Override
	public String toString() {
		return "RLDefenderRawObservation [activeObservedIds="
			+ this.activeObservedIds + ", timeStepsLeft="
			+ this.timeStepsLeft + ", defendedIds="
			+ this.defendedIds + "]";
	}
}
