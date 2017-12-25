package connectfourrl;

import java.util.Arrays;

import connectfourdomain.C4Board;

/**
 * Holds the result of one observation-action-reward
 * tuple in a game of Connect Four.
 */
public final class C4Episode {

	/**
	 * The board state, as a vector of 0.0f or 1.0f
	 * indicating if a red piece or a black piece is present.
	 */
	private final float[] input;
	/**
	 * The discounted reward for the learner, based on -1 for
	 * a loss, 0 for a draw, 1 for a win.
	 */
	private double discReward;
	/**
	 * The advantage for the learner, which will be the 
	 * discReward normalized over an epoch of training games
	 * to zero mean and unit variance.
	 */
	private double advantage;
	/**
	 * The action that was taken, in {0, . . ., C4Board.WIDTH - 1}.
	 */
	private final int column;
	/**
	 * The index of the play epoch in which this episode was
	 * created, for data management purposes.
	 */
	private final int epoch;
	/**
	 * The search depth of the minimax opponent played against.
	 */
	private final int opponentLevel;
	
	/**
	 * Constructor.
	 * @param aInput the board state
	 * @param aDiscReward the discounted reward
	 * @param aColumn the action
	 * @param aEpoch the epoch
	 * @param aOpponentLevel the opponent depth
	 */
	public C4Episode(
		final float[] aInput,
		final double aDiscReward,
		final int aColumn,
		final int aEpoch,
		final int aOpponentLevel
	) {
		assert aInput != null && aInput.length == C4SimpleNNPlayer.NUM_INPUTS;
		assert aColumn >= 0 && aColumn < C4Board.WIDTH;
		assert aEpoch >= 0;
		assert aOpponentLevel >= 0;
		this.input = aInput;
		this.discReward = aDiscReward;
		this.advantage = 0.0;
		this.column = aColumn;
		this.epoch = aEpoch;
		this.opponentLevel = aOpponentLevel;
	}
	
	/**
	 * Copy constructor.
	 * @param toCopy episode to copy
	 */
	public C4Episode(final C4Episode toCopy) {
		this(toCopy.input, toCopy.discReward,
			toCopy.column, toCopy.epoch, toCopy.opponentLevel);
	}

	/**
	 * @return the board state
	 */
	public float[] getInput() {
		return this.input;
	}
	
	/**
	 * @return the discounted future reward, normalized over the
	 * epoch to 0 mean and unit variance
	 */
	public double getAdvantage() {
		return this.advantage;
	}
	
	/**
	 * @param aAdvantage update advantage to the given value.
	 * Called after epoch is completed.
	 */
	public void setAdvantage(final double aAdvantage) {
		this.advantage = aAdvantage;
	}

	/**
	 * @return the discounted reward
	 */
	public double getDiscReward() {
		return this.discReward;
	}
	
	/**
	 * @param aReward set the discounted future reward.
	 * Called after the game is over.
	 */
	public void setDiscReward(final double aReward) {
		this.discReward = aReward;
	}

	/**
	 * @return get the action choice
	 */
	public int getColumn() {
		return this.column;
	}
	
	/**
	 * @return the epoch of training
	 */
	public int getEpoch() {
		return this.epoch;
	}
	
	/**
	 * @return the opponent search depth
	 */
	public int getOpponentLevel() {
		return this.opponentLevel;
	}

	@Override
	public String toString() {
		return "C4Episode [\ninput=" + Arrays.toString(this.input)
			+ ",\ndiscReward=" + this.discReward 
			+ ",\nadvantage=" + this.advantage 
			+ ", \ncolumn=" + this.column
			+ ", \nepoch=" + this.epoch
			+ ", \nopponentLevel=" + this.opponentLevel
			+ "]";
	}
}
