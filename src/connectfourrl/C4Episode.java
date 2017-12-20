package connectfourrl;

import java.util.Arrays;

import connectfourdomain.C4Board;

public class C4Episode {

	private final float[] input;
	private double discReward;
	private final int column;
	private final int epoch;
	
	public C4Episode(
		final float[] aInput,
		final double aDiscReward,
		final int aColumn,
		final int aEpoch
	) {
		assert aInput != null && aInput.length == C4SimpleNNPlayer.NUM_INPUTS;
		assert aColumn >= 0 && aColumn < C4Board.WIDTH;
		assert aEpoch >= 0;
		this.input = aInput;
		this.discReward = aDiscReward;
		this.column = aColumn;
		this.epoch = aEpoch;
	}

	public float[] getInput() {
		return this.input;
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

	@Override
	public String toString() {
		return "C4Episode [\ninput=" + Arrays.toString(this.input)
			+ ",\ndiscReward=" + this.discReward 
			+ ", \ncolumn=" + this.column
			+ ", \nepoch=" + this.epoch
			+ "]";
	}
}
