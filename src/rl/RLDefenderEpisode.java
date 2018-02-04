package rl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RLDefenderEpisode {
	
	/**
	 * How many steps of observation and action should
	 * be present (at maximum, if available) in rawObs
	 * and actions.
	 */
	public static final int RL_MEMORY_LENGTH = 3;
	
	/**
	 * Additional discount factor for future rewards,
	 * used for RL training.
	 */
	public static final double RL_DISCOUNT_FACTOR = 0.9;
	
	/**
	 * A list in reverse order (most recent episode first),
	 * of length up to RL_MEMORY_LENGTH.
	 */
	private final List<RLDefenderRawObservation> rawObs;
	
	/**
	 * A list in reverse order (most recent episode first),
	 * of length up to RL_MEMORY_LENGTH.
	 */
	private final List<RLDefenderAction> actions;
	
	/**
	 * A non-negative integer (can be zero).
	 */
	private final int timeStepsLeft;
	
	/**
	 * How many nodes are in the network.
	 */
	private final int nodeCount;
	
	/**
	 * Discounted reward from current time step through end
	 * of simulation.
	 * This time step's reward is not discounted, but
	 * future time steps' rewards are discounted
	 * exponentially.
	 * Previous time steps' rewards are not incorporated.
	 */
	private final double discountedReward;

	public RLDefenderEpisode(
		final List<RLDefenderRawObservation> aRawObs,
		final List<RLDefenderAction> aActions,
		final double aDiscountedReward,
		final int aTimeStepsLeft,
		final int aNodeCount
	) {
		assert aRawObs != null && aActions != null;
		assert aRawObs.size() <= RL_MEMORY_LENGTH
			&& aActions.size() <= RL_MEMORY_LENGTH;
		assert aTimeStepsLeft >= 0;
		assert aNodeCount > 0;
		
		this.rawObs = new ArrayList<RLDefenderRawObservation>();
		this.rawObs.addAll(aRawObs);
		
		this.actions = new ArrayList<RLDefenderAction>();
		this.actions.addAll(aActions);
		
		this.discountedReward = aDiscountedReward;
		this.timeStepsLeft = aTimeStepsLeft;
		this.nodeCount = aNodeCount;
	}

	public List<RLDefenderRawObservation> getRawObs() {
		return this.rawObs;
	}

	public List<RLDefenderAction> getActions() {
		return this.actions;
	}

	public double getDiscountedReward() {
		return this.discountedReward;
	}
	
	public int getTimeStepsLeft() {
		return this.timeStepsLeft;
	}
	
	public int getNodeCount() {
		return this.nodeCount;
	}
	
	/**
	 * 
	 * @return String encoding of the episode.
	 * 
	 * First line is for observations.
	 * It has length (RL_MEMORY_LENGTH * nodeCount) + 1.
	 * The first item is the number of time steps left.
	 * After that, the first nodeCount are the most recent observation,
	 * and so on.
	 * Missing observations are filled in with all zeros.
	 * A zero represents no alert, 1 an alert.
	 * 
	 * Second line is for actions.
	 * It has length RL_MEMORY_LENGTH * nodeCount.
	 * The first nodeCount are the most recent action, and so on.
	 * Missing actions are filled in with all zeros.
	 * A zero represents not defending a node, 1 defending it.
	 * 
	 * The third line is the discounted payoff, as a double (string encoding).
	 */
	public String getStringForFile() {
		final StringBuilder builder = new StringBuilder();
		
		builder.append(this.timeStepsLeft).append(',');
		for (int obsIndex = 0; obsIndex < RL_MEMORY_LENGTH; obsIndex++) {
			if (obsIndex < this.rawObs.size()) {
				final RLDefenderRawObservation curObs =
					this.rawObs.get(obsIndex);
				final Set<Integer> activeIdsObs = curObs.activeObservedIdSet(0);
				for (int i = 0; i < this.nodeCount; i++) {
					if (activeIdsObs.contains(i + 1)) {
						builder.append(1);
					} else {
						builder.append(0);
					}
					if (i < this.nodeCount - 1) {
						builder.append(',');
					}
				}
			} else {
				for (int i = 0; i < this.nodeCount; i++) {
					builder.append(0);
					if (i < this.nodeCount - 1) {
						builder.append(',');
					}
				}
			}
			if (obsIndex < RL_MEMORY_LENGTH - 1) {
				builder.append(',');
			}
		}
		builder.append('\n');
		
		for (int actIndex = 0; actIndex < RL_MEMORY_LENGTH; actIndex++) {
			if (actIndex < this.actions.size()) {
				final RLDefenderAction curAction =
					this.actions.get(actIndex);
				final Set<Integer> actionNodes = curAction.nodeIdSet();
				for (int i = 0; i < this.nodeCount; i++) {
					if (actionNodes.contains(i + 1)) {
						builder.append(1);
					} else {
						builder.append(0);
					}
					if (i < this.nodeCount - 1) {
						builder.append(',');
					}
				}
			} else {
				for (int i = 0; i < this.nodeCount; i++) {
					builder.append(0);
					if (i < this.nodeCount - 1) {
						builder.append(',');
					}
				}
			}
			if (actIndex < RL_MEMORY_LENGTH - 1) {
				builder.append(',');
			}
		}
		builder.append('\n');

		final DecimalFormat df4 = new DecimalFormat(".####");
		builder.append(df4.format(this.discountedReward));
		return builder.toString();
	}

	@Override
	public String toString() {
		return "RLDefenderEpisode [\nrawObs=" + this.rawObs 
			+ ",\nactions=" + this.actions
			+ ",\ndiscountedReward=" + this.discountedReward
			+ ",\ntimeStepsLeft=" + this.timeStepsLeft
			+ ",\nnodeCount=" + this.nodeCount
			+ "]";
	}
}
