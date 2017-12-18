package rl;

import java.util.ArrayList;
import java.util.List;

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
