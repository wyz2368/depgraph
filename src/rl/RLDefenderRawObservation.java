package rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.DefenderObservation;
import model.SecurityAlert;

public final class RLDefenderRawObservation {

	private final List<Integer> activeObservedIds = new ArrayList<Integer>();
	
	private final Set<Integer> activeObservedIdSet;
	
	private final int timeStepsLeft;
	
	public RLDefenderRawObservation(final DefenderObservation defObs) {
		assert defObs != null;
		for (final SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				this.activeObservedIds.add(alert.getNode().getId());
			}
		}
		Collections.sort(this.activeObservedIds);
		this.activeObservedIdSet = new HashSet<Integer>(this.activeObservedIds);
		this.timeStepsLeft = defObs.getTimeStepsLeft();
	}

	public List<Integer> getActiveObservedIds() {
		return this.activeObservedIds;
	}
	
	public Set<Integer> activeObservedIdSet() {
		return this.activeObservedIdSet;
	}
	
	public int getTimeStepsLeft() {
		return this.timeStepsLeft;
	}

	@Override
	public String toString() {
		return "RLDefenderRawObservation [activeObservedIds="
			+ this.activeObservedIds + ", timeStepsLeft="
			+ this.timeStepsLeft + "]";
	}
}
