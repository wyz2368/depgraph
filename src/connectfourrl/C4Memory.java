package connectfourrl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class C4Memory {

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
		this.episodes.addAll(epoch);
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
		    	builder.append(episode.getDiscReward()).append('\n');
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
