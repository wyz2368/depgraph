package connectfourrl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import connectfourdomain.C4Board;

/**
 * Stores episodes from recent training epochs.
 * Has methods to generate training DataSets for
 * the neural net, by drawing with replacement (bootstrap)
 * from the stored episodes.
 */
public final class C4Memory {

	/**
	 * Maximum number of epochs of play data to store.
	 * When new data is added from the next epoch,
	 * the oldest epoch's data is dropped.
	 */
	public static final int MAX_EPOCHS = 10;
	/**
	 * How many episodes to include in datasets for training
	 * of neural net.
	 */
	public static final int DATASET_SIZE = 500;
	/**
	 * Used to store the episodes from play.
	 */
	private final List<C4Episode> episodes = new ArrayList<C4Episode>();
	/**
	 * Used to select uniform random episodes from memory for
	 * training.
	 */
	private static final Random RAND = new Random();

	/**
	 * Constructor.
	 */
	public C4Memory() {
		// do nothing
	}
	
	/**
	 * Adds the given epoch's episodes to memory.
	 * If there are too many epochs in memory, the oldest
	 * epoch's data is dropped.
	 * @param epoch the data to add
	 */
	public void addEpoch(final List<C4Episode> epoch) {
		assert epoch != null && !epoch.isEmpty();
		if (epoch.get(0).getEpoch() <= maxEpoch()) {
			throw new IllegalArgumentException("duplicate addition");
		}
		if (!this.isEmpty()
			&& (epoch.get(0).getOpponentLevel() != opponentLevel())) {
			throw new IllegalStateException("mixing opponent level data");
		}
		if (maxEpoch() - minEpoch() == MAX_EPOCHS - 1) {
			dropOldestEpoch();
		}
		
		// balanceEpoch(epoch);
		setAdvantages(epoch);
		this.episodes.addAll(epoch);
	}
	
	/**
	 * Update the epoch by resampling results that are 
	 * not losses, to equal the number of losses (if
	 * too many losses).
	 * (Or of wins, if too may wins.)
	 * @param epoch the epoch to balance
	 */
	@SuppressWarnings("unused")
	private static void balanceEpoch(final List<C4Episode> epoch) {
		int lossEpisodes = 0;
		int tieEpisodes = 0;
		int winEpisodes = 0;
		for (C4Episode episode: epoch) {
			if (episode.getDiscReward() < 0.0) {
				lossEpisodes++;
			} else if (episode.getDiscReward() > 0.0) {
				winEpisodes++;
			} else {
				tieEpisodes++;
			}
		}
		
		if (lossEpisodes > tieEpisodes + winEpisodes
			&& lossEpisodes < epoch.size()) {
			// too many losses.
			final double fractionToAdd =
				(lossEpisodes - tieEpisodes - winEpisodes) * 1.0
				/ (tieEpisodes + winEpisodes);
			final List<C4Episode> toAdd = new ArrayList<C4Episode>();
			for (C4Episode episode: epoch) {
				if (episode.getDiscReward() >= 0.0) {
					if (Math.random() < fractionToAdd) {
						toAdd.add(new C4Episode(episode));
					}
				}
			}
			epoch.addAll(toAdd);
		} else if (winEpisodes > lossEpisodes + tieEpisodes
			&& winEpisodes < epoch.size()) {
			// too many wins.
			final double fractionToAdd =
				(winEpisodes - tieEpisodes - lossEpisodes) * 1.0
				/ (tieEpisodes + lossEpisodes);
			final List<C4Episode> toAdd = new ArrayList<C4Episode>();
			for (C4Episode episode: epoch) {
				if (episode.getDiscReward() <= 0.0) {
					if (Math.random() < fractionToAdd) {
						toAdd.add(new C4Episode(episode));
					}
				}
			}
			epoch.addAll(toAdd);
		}
	}
	
	/**
	 * @return the opponent level played against in the stored
	 * data.
	 */
	public int opponentLevel() {
		if (this.isEmpty()) {
			return -1;
		}
		return this.episodes.get(0).getOpponentLevel();
	}
	
	/**
	 * Empties the memory of data.
	 */
	public void clear() {
		this.episodes.clear();
	}
	
	// features will be an N * 84 INDArray,
	// where N is the number of training examples
	// labels will be an N * 7 INDArray,
	// where N is the number of training examples
	/**
	 * Use bootstrapping to sample a dataset for 
	 * training in a format suitable for
	 * DeepLearning4J.
	 * 
	 * A set of DATASET_SIZE episodes will be sampled
	 * with replacement from the memory, in arbitrary
	 * order.
	 * 
	 * The "output mask" field of the DataSet is
	 * overloaded with a an INDArray of arbitrary
	 * real numbers, containing the advantage value
	 * or each episode, which will be multiplied by
	 * the log likelihood when the gradient is
	 * taken.
	 * 
	 * @param isConv if true, return as 6 * 7 * 2 tensor
	 * for convolutional net. If false, return as 84-item
	 * vector for fully-connected net.
	 * 
	 * @return a DataSet for DeepLearning4J to use
	 * for training.
	 */
	public DataSet getDataSetWithMasks(final boolean isConv) {
		assert !isEmpty();
		final float[][] inputs =
			new float[DATASET_SIZE][C4SimpleNNPlayer.NUM_INPUTS];
		final float[][] labels =
			new float[DATASET_SIZE][C4Board.WIDTH];
		final float[][] labelsMask =
			new float[DATASET_SIZE][C4Board.WIDTH];

		for (int i = 0; i < DATASET_SIZE; i++) {
			// select dataset of size DATASET_SIZE,
			// using bootstrap (sampling with replacement)
			// from this.episodes.
			final C4Episode episode = this.episodes.get(
				RAND.nextInt(this.episodes.size()));
			inputs[i] = episode.getInput();
			labels[i][episode.getColumn()] = 1.0f;
			labelsMask[i][episode.getColumn()] =
				(float) episode.getAdvantage();
		}


		INDArray inputsIND = null;
		if (isConv) {
			final float[] inputsFlat =
				new float[DATASET_SIZE * C4SimpleNNPlayer.NUM_INPUTS];
			for (int i = 0; i < DATASET_SIZE; i++) {
				for (int j = 0; j < C4SimpleNNPlayer.NUM_INPUTS; j++) {
					inputsFlat[i * C4SimpleNNPlayer.NUM_INPUTS + j] =
						inputs[i][j];
				}
			}
			inputsIND = Nd4j.create(inputsFlat,
				new int[]{DATASET_SIZE, 2, C4Board.HEIGHT, C4Board.WIDTH});
		} else {
			inputsIND = Nd4j.create(inputs);
		}
		final INDArray labelsIND = Nd4j.create(labels);
		final INDArray labelsMaskIND = Nd4j.create(labelsMask);
		
		return new DataSet(
			inputsIND, labelsIND, null, labelsMaskIND);
	}
	
	/**
	 * @return true if no episodes are in memory
	 */
	public boolean isEmpty() {
		return this.episodes.isEmpty();
	}
	
	/**
	 * Set the advantage of each episode to the correct value, based on
	 * the distribution of rewards in the epoch.
	 * The advantage of an episode is:
	 * 		a_i <- (r_i - \mu) \sigma,
	 * where a_i is the episode's advantage, r_i is the episode's
	 * discounted reward, \mu is the sample mean reward of the epoch,
	 * and \sigma is the sample standard deviation of rewards in the epoch.
	 * @param epoch the epoch to use for normalization. its advantage
	 * values will be updated.
	 */
	private static void setAdvantages(final List<C4Episode> epoch) {
		final double meanReward = meanReward(epoch);
		final double stdReward = stdReward(epoch);
		for (final C4Episode episode: epoch) {
			final double reward = episode.getDiscReward();
			final double advantage = (reward - meanReward) / stdReward;
			episode.setAdvantage(advantage);
		}
	}
	
	/**
	 * @param epoch an epoch of episodes
	 * @return the sample standard deviation of rewards in the
	 * epoch
	 */
	private static double stdReward(final List<C4Episode> epoch) {
		final double meanReward = meanReward(epoch);
		double sumSquaredDiff = 0.0;
		for (final C4Episode episode: epoch) {
			sumSquaredDiff +=
				Math.pow(episode.getDiscReward() - meanReward, 2.0);
		}
		final double meanSquaredDiff = sumSquaredDiff / epoch.size();
		return Math.sqrt(meanSquaredDiff);
	}
	
	/**
	 * @param epoch an epoch of episodes
	 * @return the sample mean discounted reward in the epoch
	 */
	private static double meanReward(final List<C4Episode> epoch) {
		double totalReward = 0.0;
		for (final C4Episode episode: epoch) {
			totalReward += episode.getDiscReward();
		}
		return totalReward / epoch.size();
	}
	
	/**
	 * Delete all episodes from the earliest epoch in memory.
	 */
	private void dropOldestEpoch() {
		final int oldMinEpoch = minEpoch();
		for (int i = this.episodes.size() - 1; i >= 0; i--) {
			if (this.episodes.get(i).getEpoch() == oldMinEpoch) {
				this.episodes.remove(i);
			}
		}
	}

	/**
	 * @return the list of episodes currently in memory
	 */
	public List<C4Episode> getEpisodes() {
		return this.episodes;
	}
	
	/**
	 * @return the epoch of the earliest episode in memory,
	 * or -1 if none
	 */
	public int minEpoch() {
		if (this.episodes.isEmpty()) {
			return -1;
		}
		return this.episodes.get(0).getEpoch();
	}
	
	/**
	 * @return the epoch of the latest episode in memory,
	 * or -1 if none
	 */
	public int maxEpoch() {
		if (this.episodes.isEmpty()) {
			return -1;
		}
		return this.episodes.get(this.episodes.size() - 1).getEpoch();
	}
	
	/**
	 * Record all episodes in memory in CSV format,
	 * with one episode per line,
	 * at the given file name.
	 * 
	 * @param fileName the file name to store under
	 */
	public void recordToFile(final String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		// up to 4 decimals in reward
		final DecimalFormat fmt = new DecimalFormat("#.####"); 
	    try {
		    final File file = new File(fileName);
			file.createNewFile();
		    final BufferedWriter writer =
	    		new BufferedWriter(new FileWriter(file));
		    
		    final double half = 0.5;
		    for (final C4Episode episode: this.episodes) {
		    	StringBuilder builder = new StringBuilder();
		    	for (float f: episode.getInput()) {
		    		if (f > half) {
		    			builder.append(1).append(',');
		    		} else {
		    			builder.append(0).append(',');
		    		}
		    	}
		    	builder.append(episode.getColumn()).append(',');
		    	builder.append(fmt.format(episode.getDiscReward())).append(',');
		    	builder.append(fmt.format(episode.getAdvantage())).append(',');
		    	builder.append(episode.getOpponentLevel()).append('\n');
		    	writer.write(builder.toString());
		    }
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "C4Memory [" 
			+ "\nepisodeCount = " + this.episodes.size()
			+ "\nminEpoch = " + minEpoch()
			+ "\nmaxEpoch = " + maxEpoch()
			+ "\nopponentLevel = " + opponentLevel()
			+ "\n]";
	}
}
