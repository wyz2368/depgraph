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
	 * A list, for each of OBS_LENGTH time steps, of
	 * a sorted list of the node IDs that the defender observed as apparently
	 * active in the most recent time step.
	 * 
	 * Most recent time step is last.
	 */
	private final List<List<Integer>> activeObservedIds =
		new ArrayList<List<Integer>>();
	
	/**
	 * A list, for each of OBS_LENGTH time steps, of
	 * the set of node IDs that the defender observed as apparently
	 * active in the most recent time step.
	 * 
	 * Most recent time step is last.
	 */
	private final List<Set<Integer>> activeObservedIdSet =
		new ArrayList<Set<Integer>>();
	
	/**
	 * A list, for each of OBS_LENGTH time steps, of
	 * the set of node IDs that the defender protected in the most recent
	 * time step.
	 * 
	 * Most recent time step is last.
	 */
	private final List<Set<Integer>> defendedIds =
		new ArrayList<Set<Integer>>();
	
	/**
	 * How many time steps remain in the simulation.
	 */
	private final int timeStepsLeft;
	
	/**
	 * The number of time steps of recent alert signals
	 * and defense sets that will be included in the observation.
	 */
	public static final int DEFENDER_OBS_LENGTH = 3;
	
	/**
	 * Constructor.
	 * @param defObs a list of the defender's observation
	 * over the previous time steps, most recent last.
	 * @param defAct a list of the defender's action
	 * over the previous time steps, most recent last.
	 * 
	 * Note that defObs and defAct may have lengths less
	 * than OBS_LENGTH if the run just started, but defObs
	 * should not be empty.
	 */
	public RLDefenderRawObservation(
		final List<DefenderObservation> defObs,
		final List<DefenderAction> defAct
	) {
		assert defObs != null && defAct != null && DEFENDER_OBS_LENGTH >= 1;
		/*
		if (defObs.isEmpty() || defObs.size() - defAct.size() != 1) {
			throw new IllegalArgumentException();
		}
		*/
		for (int t = defObs.size() - DEFENDER_OBS_LENGTH;
			t < defObs.size(); t++) {
			DefenderObservation curDefObs = null;
			if (t >= 0) {
				curDefObs = defObs.get(t);
			}
			final List<Integer> curActiveObservedIds = new ArrayList<Integer>();
			if (curDefObs != null) {
				for (final SecurityAlert alert: curDefObs.getAlertSet()) {
					if (alert.isActiveAlert()) {
						curActiveObservedIds.add(alert.getNode().getId());
					}
				}
			}
			Collections.sort(curActiveObservedIds);
			this.activeObservedIds.add(curActiveObservedIds);
			
			final Set<Integer> curActiveObservedIdSet =
				new HashSet<Integer>(curActiveObservedIds);
			this.activeObservedIdSet.add(curActiveObservedIdSet);
		}

		if (defObs.isEmpty()) {
			this.timeStepsLeft = DEFENDER_OBS_LENGTH; // hack
		} else {
			this.timeStepsLeft =
				defObs.get(defObs.size() - 1).getTimeStepsLeft();
		}
		
		for (int t = defAct.size() - DEFENDER_OBS_LENGTH;
			t < defAct.size(); t++) {
			DefenderAction curDefAct = null;
			if (t >= 0) {
				curDefAct = defAct.get(t);
			}
			final Set<Integer> curDefIds = new HashSet<Integer>();
			if (curDefAct != null) {
				for (final Node node: curDefAct.getAction()) {
					curDefIds.add(node.getId());
				}
			}
			this.defendedIds.add(curDefIds);
		}
	}

	/**
	 * Get the sorted list of node IDs that appeared active
	 * in the most recent time step.
	 * @param timeStepsAgo how recent the returned set should be.
	 * 0 means current time step, (OBS_LENGTH - 1) is the earliest
	 * available time step.
	 * @return the list of apparently active node IDs
	 */
	public List<Integer> getActiveObservedIds(final int timeStepsAgo) {
		if (timeStepsAgo < 0 || timeStepsAgo >= DEFENDER_OBS_LENGTH) {
			throw new IllegalArgumentException();
		}
		return this.activeObservedIds.get(
			DEFENDER_OBS_LENGTH - timeStepsAgo - 1);
	}
	
	/**
	 * Get the set of node IDs that appeared active
	 * in the most recent time step.
	 * @param timeStepsAgo how recent the returned set should be.
	 * 0 means current time step, (OBS_LENGTH - 1) is the earliest
	 * available time step.
	 * @return the set of apparently active node IDs
	 */
	public Set<Integer> activeObservedIdSet(final int timeStepsAgo) {
		if (timeStepsAgo < 0 || timeStepsAgo >= DEFENDER_OBS_LENGTH) {
			throw new IllegalArgumentException();
		}
		return this.activeObservedIdSet.get(
			DEFENDER_OBS_LENGTH - timeStepsAgo - 1);
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
	 * @param timeStepsAgo how recent the returned set should be.
	 * 0 means current time step, (OBS_LENGTH - 1) is the earliest
	 * available time step.
	 * @return the set of protected node IDs
	 */
	public Set<Integer> getDefendedIds(final int timeStepsAgo) {
		if (timeStepsAgo < 0 || timeStepsAgo >= DEFENDER_OBS_LENGTH) {
			throw new IllegalArgumentException();
		}
		return this.defendedIds.get(DEFENDER_OBS_LENGTH - timeStepsAgo - 1);
	}

	@Override
	public String toString() {
		return "RLDefenderRawObservation [activeObservedIds="
			+ this.activeObservedIds + ", timeStepsLeft="
			+ this.timeStepsLeft + ", defendedIds="
			+ this.defendedIds + "]";
	}
}
