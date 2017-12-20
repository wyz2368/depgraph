package connectfourrl;

import java.util.Arrays;

import connectfourdomain.C4Board;

public final class C4Episode {

	private final float[] input;
	private double discReward;
	private double advantage;
	private final int column;
	private final int epoch;
	private final int opponentLevel;
	
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

	public float[] getInput() {
		return this.input;
	}
	
	public double getAdvantage() {
		return this.advantage;
	}
	
	public void setAdvantage(final double aAdvantage) {
		this.advantage = aAdvantage;
	}

	public double getDiscReward() {
		return this.discReward;
	}
	
	public void setDiscReward(final double aReward) {
		this.discReward = aReward;
	}

	public int getColumn() {
		return this.column;
	}
	
	public int getEpoch() {
		return this.epoch;
	}
	
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
