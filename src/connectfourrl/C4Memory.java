package connectfourrl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class C4Memory {

	public static final int MAX_EPOCHS = 5;
	
	private final List<C4Episode> episodes = new ArrayList<C4Episode>();

	public C4Memory() {
		// do nothing
	}
	
	public void addEpoch(final List<C4Episode> epoch) {
		assert epoch != null && !epoch.isEmpty();
		if (epoch.get(0).getEpoch() <= maxEpoch()) {
			throw new IllegalArgumentException("duplicate addition");
		}
		if (maxEpoch() - minEpoch() == MAX_EPOCHS - 1) {
			dropOldestEpoch();
		}
		
		setAdvantages(epoch);
		this.episodes.addAll(epoch);
	}
	
	private static void setAdvantages(final List<C4Episode> epoch) {
		final double meanReward = meanReward(epoch);
		final double stdReward = stdReward(epoch);
		for (final C4Episode episode: epoch) {
			final double reward = episode.getDiscReward();
			final double advantage = (reward - meanReward) / stdReward;
			episode.setAdvantage(advantage);
		}
	}
	
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
	
	private static double meanReward(final List<C4Episode> epoch) {
		double totalReward = 0.0;
		for (final C4Episode episode: epoch) {
			totalReward += episode.getDiscReward();
		}
		return totalReward / epoch.size();
	}
	
	private void dropOldestEpoch() {
		final int oldMinEpoch = minEpoch();
		for (int i = this.episodes.size() - 1; i >= 0; i--) {
			if (this.episodes.get(i).getEpoch() == oldMinEpoch) {
				this.episodes.remove(i);
			}
		}
	}

	public List<C4Episode> getEpisodes() {
		return this.episodes;
	}
	
	public int minEpoch() {
		if (this.episodes.isEmpty()) {
			return -1;
		}
		return this.episodes.get(0).getEpoch();
	}
	
	public int maxEpoch() {
		if (this.episodes.isEmpty()) {
			return -1;
		}
		return this.episodes.get(this.episodes.size() - 1).getEpoch();
	}
	
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
			+ "\n]";
	}
}
