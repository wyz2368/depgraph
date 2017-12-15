package rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.DefenderObservation;
import model.SecurityAlert;

public final class RLDefenderRawObservation {

	private final List<Integer> activeObservedIds = new ArrayList<Integer>();
	
	public RLDefenderRawObservation(final DefenderObservation defObs) {
		assert defObs != null;
		for (final SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				this.activeObservedIds.add(alert.getNode().getId());
			}
		}
		Collections.sort(this.activeObservedIds);
	}

	public List<Integer> getActiveObservedIds() {
		return this.activeObservedIds;
	}

	@Override
	public String toString() {
		return "RLDefenderRawObservation [activeObservedIds="
			+ this.activeObservedIds + "]";
	}
}
