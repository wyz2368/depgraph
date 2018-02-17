package rl;

import java.util.ArrayList;
import java.util.List;

/**
 * A raw representation of game state from the attacker's perspective.
 */
public final class RLAttackerRawObservation {

	/**
	 * IDs in {1, . . ., NodeCount} of nodes that were attacked
	 * in the most recent time step.
	 * IDs are not duplicated, and are in ascending order.
	 */
	private final List<Integer> attackedNodeIds =
		new ArrayList<Integer>();
	
	/**
	 * IDs in {1, . . ., EdgeCount} of edges to OR nodes that were attacked
	 * in the most recent time step.
	 * IDs are not duplicated, and are in ascending order.
	 */
	private final List<Integer> attackedEdgeIds =
		new ArrayList<Integer>();
	
	/**
	 * IDs in {1, . . ., NodeCount} or AND nodes that are legal
	 * to attack next (i.e., in the candidate set).
	 * IDs are not duplicated, and are in ascending order.
	 */
	private final List<Integer> legalToAttackNodeIds =
		new ArrayList<Integer>();
	
	/**
	 * IDs in {1, . . ., EdgeCount} or edges to OR nodes that are legal
	 * to attack next (i.e., in the candidate set).
	 * IDs are not duplicated, and are in ascending order.
	 */
	private final List<Integer> legalToAttackEdgeIds =
			new ArrayList<Integer>();
	
	/**
	 * A list of the past ATTACKER_OBS_LENGTH time steps, of
	 * the node IDs in {1, . . ., NodeCount} that were active
	 * at that time step. If there are not enough time steps
	 * elapsed yet, earlier time steps' lists will be empty.
	 * Within an inner list, IDs are not duplicated, and are in ascending order.
	 */
	private final List<List<Integer>> activeNodeIdsHistory =
		new ArrayList<List<Integer>>();
	
	/**
	 * Number of time steps left in the game.
	 */
	private final int timeStepsLeft;
	
	/**
	 * How many time steps of activeNodeIds will be kept.
	 */
	public static final int ATTACKER_OBS_LENGTH = 1;
	
	/**
	 * @param aAttackedNodeIds the list of AND node IDs that were most
	 * recently attacked, strictly increasing.
	 * @param aAttackedEdgeIds the list of IDs of edges to OR nodes
	 * that were most recently attacked, strictly increasing. 
	 * @param aLegalToAttackNodeIds the list of AND node IDs that it is
	 * legal to attack currently, strictly increasing.
	 * @param aLegalToAttackEdgeIds the list of IDs of edges to OR nodes
	 * that it is legal to attack currently, strictly increasing.
	 * @param aActiveNodeIdsHistory a list for each previous time step
	 * in order from earliest to most recent, of the list of node IDs that
	 * were active (enabled by attacker) at that time, strictly increasing.
	 * @param aTimeStepsLeft how many time steps are left
	 * @param aNodeCount how many nodes are in the network
	 * @param andNodeIds the ID of every AND node
	 * @param edgeToOrIds the ID of every edge to an OR node
	 */
	public RLAttackerRawObservation(
		final List<Integer> aAttackedNodeIds,
		final List<Integer> aAttackedEdgeIds,
		final List<Integer> aLegalToAttackNodeIds,
		final List<Integer> aLegalToAttackEdgeIds,
		final List<List<Integer>> aActiveNodeIdsHistory,
		final int aTimeStepsLeft,
		final int aNodeCount,
		final List<Integer> andNodeIds,
		final List<Integer> edgeToOrIds
	) {
		if (aAttackedNodeIds == null
			|| aAttackedEdgeIds == null
			|| aLegalToAttackNodeIds == null
			|| aLegalToAttackEdgeIds == null
			|| aActiveNodeIdsHistory == null
			|| andNodeIds == null
			|| edgeToOrIds == null
		) {
			throw new IllegalArgumentException();
		}
		if (!validateIdsList(aAttackedNodeIds, andNodeIds)
			|| !validateIdsList(aAttackedEdgeIds, edgeToOrIds)
			|| !validateIdsList(aLegalToAttackNodeIds, andNodeIds)
			|| !validateIdsList(aLegalToAttackEdgeIds, edgeToOrIds)
		) {
			throw new IllegalArgumentException();
		}
		this.attackedNodeIds.addAll(aAttackedNodeIds);
		this.attackedEdgeIds.addAll(aAttackedEdgeIds);

		this.legalToAttackNodeIds.addAll(aLegalToAttackNodeIds);
		this.legalToAttackEdgeIds.addAll(aLegalToAttackEdgeIds);
		
		for (int t = aActiveNodeIdsHistory.size() - ATTACKER_OBS_LENGTH;
				t < aActiveNodeIdsHistory.size(); t++) {
				final List<Integer> curActiveNodeIds = new ArrayList<Integer>();
				if (t >= 0) {
					if (!validateIdsList(
						aActiveNodeIdsHistory.get(t), aNodeCount)) {
						throw new IllegalArgumentException();
					}
					curActiveNodeIds.addAll(aActiveNodeIdsHistory.get(t));
				}
				this.activeNodeIdsHistory.add(curActiveNodeIds);
			}
		
		this.timeStepsLeft = aTimeStepsLeft;
	}
	
	/**
	 * Convenience constructor for the empty initial observation,
	 * at the beginning of a game.
	 * @param aLegalToAttackNodeIds the list of AND node IDs that it is
	 * legal to attack currently, strictly increasing. (These are the
	 * root nodes of the graph.)
	 * @param andNodeIds the ID of every AND node
	 * @param aTimeStepsLeft how many time steps in the game
	 */
	public RLAttackerRawObservation(
		final List<Integer> aLegalToAttackNodeIds,
		final List<Integer> andNodeIds,
		final int aTimeStepsLeft
	) {
		if (aLegalToAttackNodeIds == null || andNodeIds == null) {
			throw new IllegalArgumentException();
		}
		if (!validateIdsList(aLegalToAttackNodeIds, andNodeIds)) {
			throw new IllegalArgumentException();
		}
		if (aTimeStepsLeft <= 0) {
			throw new IllegalArgumentException();
		}
		for (int t = 0; t < ATTACKER_OBS_LENGTH; t++) {
			final List<Integer> curActiveNodeIds = new ArrayList<Integer>();
			this.activeNodeIdsHistory.add(curActiveNodeIds);
		}
		this.legalToAttackNodeIds.addAll(aLegalToAttackNodeIds);
		this.timeStepsLeft = aTimeStepsLeft;
	}
	
	/**
	 * @param idsList a list of IDs of nodes or edges to check.
	 * @param maxId the highest legal ID
	 * @return true if the list is valid. the list should
	 * have all entries in {1, . . ., maxId}, and all entries
	 * strictly increasing.
	 */
	private static boolean validateIdsList(
		final List<Integer> idsList,
		final int maxId 
	) {
		if (idsList.isEmpty()) {
			return true;
		}
		for (int i = 0; i < idsList.size() - 1; i++) {
			final int left = idsList.get(i);
			final int right = idsList.get(i + 1);
			if (left >= right) {
				return false;
			}
			if (left < 1 || left > maxId) {
				return false;
			}
		}
		final int last = idsList.get(idsList.size() - 1);
		if (last < 1 || last > maxId) {
			return false;
		}
		return true;
	}
	
	/**
	 * @param idsList a list of IDs of nodes or edges to check.
	 * @param validIdsList the list of legal IDs.
	 * @return true if the list is valid. the list should
	 * have only entries in validIdsList, and all entries
	 * strictly increasing.
	 */
	private static boolean validateIdsList(
		final List<Integer> idsList,
		final List<Integer> validIdsList 
	) {
		if (idsList.isEmpty()) {
			return true;
		}
		if (validIdsList.contains(0)) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < idsList.size() - 1; i++) {
			final int left = idsList.get(i);
			final int right = idsList.get(i + 1);
			if (left >= right) {
				return false;
			}
			if (!validIdsList.contains(left)) {
				return false;
			}
		}
		final int last = idsList.get(idsList.size() - 1);
		if (!validIdsList.contains(last)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the list of node IDs that were attacked
	 * most recently, strictly increasing.
	 */
	public List<Integer> getAttackedNodeIds() {
		return this.attackedNodeIds;
	}

	/**
	 * @return the list of edge IDs that were attacked
	 * most recently, strictly increasing.
	 */
	public List<Integer> getAttackedEdgeIds() {
		return this.attackedEdgeIds;
	}

	/**
	 * @return the list of AND node IDs that are legal to attack,
	 * strictly increasing.
	 */
	public List<Integer> getLegalToAttackNodeIds() {
		return this.legalToAttackNodeIds;
	}

	/**
	 * @return the list of IDs of edges to OR nodes that are legal to attack,
	 * strictly increasing.
	 */
	public List<Integer> getLegalToAttackEdgeIds() {
		return this.legalToAttackEdgeIds;
	}

	/**
	 * @return a list of length ATTACKER_OBS_LENGTH, of the active (i.e.,
	 * enabled by attacker) nodes' IDs, strictly increasing, for each
	 * time step. If not enough time steps have occurred, earlier time
	 * steps will have empty lists.
	 * The earliest time step will be first in the list.
	 */
	public List<List<Integer>> getActiveNodeIdsHistory() {
		return this.activeNodeIdsHistory;
	}

	/**
	 * @return the number of time steps left in the game.
	 */
	public int getTimeStepsLeft() {
		return this.timeStepsLeft;
	}

	@Override
	public String toString() {
		return "RLAttackerRawObservation [attackedNodeIds="
			+ this.attackedNodeIds + ", attackedEdgeIds=" + this.attackedEdgeIds
			+ ", legalToAttackNodeIds=" + this.legalToAttackNodeIds
			+ ", legalToAttackEdgeIds=" + this.legalToAttackEdgeIds
			+ ", activeNodeIdsHistory=" + this.activeNodeIdsHistory
			+ ", timeStepsLeft=" + this.timeStepsLeft + "]";
	}
}
