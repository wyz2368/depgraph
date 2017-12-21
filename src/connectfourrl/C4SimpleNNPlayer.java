package connectfourrl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import connectfourdomain.C4Board;
import connectfourdomain.C4Player;
import connectfourdomain.C4Board.Winner;

public final class C4SimpleNNPlayer {
	
	public static final int NUM_INPUTS = 84;
	
	private static final int NUM_HIDDEN_NODES = 200;
	
	private static final double LEARNING_RATE = 0.1;
	
	private static final int GAMES_PER_EPOCH = 600;
	
	private static final double DISC_FACTOR = 0.99;
	
	private static final double REGULARIZER = 0.01;
	
	private static final C4Memory memory = new C4Memory();
	
	private static MultiLayerNetwork net;
	
	public static void main(final String[] args) {
		trainRounds(15, 1);
		
		// playGameVsComputer(0);
		// final String outFileName = "epochData.csv";
		// memory.recordToFile(outFileName);
	}
	
	public static void trainRounds(
		final int rounds,
		final int epochsPerRound
	) {
		if (rounds < 1 || epochsPerRound < 1) {
			throw new IllegalArgumentException();
		}

		setupNet();
		for (int i = 0; i < rounds; i++) {
			for (int j = 0; j < epochsPerRound; j++) {
				addMemoryEpoch(0);
			}
			trainFromMemory();
		}
	}
	
	private C4SimpleNNPlayer() {
		// not called
	}
	
	public static void setupNet() {        
        final MultiLayerConfiguration conf =
    		new NeuralNetConfiguration.Builder()
            .iterations(1)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(LEARNING_RATE)
            .updater(Updater.NESTEROVS)
            .regularization(true).l2(REGULARIZER)
            .list()
            .layer(0, new DenseLayer.Builder().nIn(NUM_INPUTS).
        		nOut(NUM_HIDDEN_NODES)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(1, new OutputLayer
        		.Builder(new PolicyGradientLoss())
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                .nIn(NUM_HIDDEN_NODES).nOut(C4Board.WIDTH).build())
            .pretrain(false).backprop(true).build();
        
        net = new MultiLayerNetwork(conf);
        net.init();
	}
	
	public static void trainFromMemory() {
		if (memory.isEmpty()) {
			throw new IllegalStateException();
		}
		
		/*
		 * Loss function:
		 * sum over episodes: advantage * log-likelihood of action taken
		 */
		final long startTime = System.currentTimeMillis();
		
		final DataSet ds = memory.getDataSetWithMasks();
		net.fit(ds);
		
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		System.out.println("Time taken for training: "
			+ durationInSec + " seconds");
	}
	
	public static void addMemoryEpoch(
		final int opponentLevel
	) {
		final long startTime = System.currentTimeMillis();
		
		final int curEpoch = memory.maxEpoch() + 1;
		final List<C4Episode> localMemory = new ArrayList<C4Episode>();
		int wins = 0;
		for (int game = 0; game < GAMES_PER_EPOCH; game++) {
			localMemory.addAll(playGameForLearning(curEpoch, opponentLevel));
			final double curReward =
				localMemory.get(localMemory.size() - 1).getDiscReward();
			if (curReward > 0.0) {
				wins++;
			} else if (curReward < 0.0) {
				wins--;
			}
			final int printFrequency = 100;
			if (game % printFrequency == 0) {
				System.out.println(game);
			}
		}
		memory.addEpoch(localMemory);
		
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		final DecimalFormat fmt = new DecimalFormat("#.###"); 
		System.out.println("Time taken for epoch: "
			+ fmt.format(durationInSec) + " seconds");
		System.out.println(
			"Sec per game: " + fmt.format((durationInSec / GAMES_PER_EPOCH)));
		System.out.println("Win rate: "
			+ fmt.format((wins * 1.0 / GAMES_PER_EPOCH)));
	}
	
	public static List<C4Episode> playGameForLearning(
		final int epoch,
		final int opponentSearchDepth
	) {
		final List<C4Episode> result = new ArrayList<C4Episode>();
		
		final C4Board board = new C4Board();
		C4Player.setSearchDepth(opponentSearchDepth);
		while (board.getWinner() == Winner.NONE) {
			if (board.isBlackTurn()) {
				// NN will play next.
				final float[] nnInput = board.getAsFloatArray();
				
				int col = getNNMove(board);
				while (!board.isLegalMove(col)) {
					col = getNNMove(board);
				}
				
				// initialize all episodes with 0 reward
				final C4Episode episode = new C4Episode(
					nnInput, 0.0, col, epoch, C4Player.getSearchDepth());
				result.add(episode);
				
				board.makeMove(col);
			} else {
				board.makeMove(C4Player.getRedMove(board));
			}
		}
		
		// set the correct discounted rewards.
		// 0 reward for draws
		double reward = 0.0;
		if (board.getWinner() == Winner.BLACK) {
			// +1 reward for winning
			reward = 1.0;
		} else if (board.getWinner() == Winner.RED) {
			// -1 reward for losing
			reward = -1.0;
		}
		for (int i = result.size() - 1; i >= 0; i--) {
			// discount rewards exponentially for
			// previous time steps.
			result.get(i).setDiscReward(reward);
			reward *= DISC_FACTOR;
		}
		
		return result;
	}
	
	public static void playGameVsComputer(
		final int opponentSearchDepth
	) {
		final C4Board board = new C4Board();
		C4Player.setSearchDepth(opponentSearchDepth);
		while (board.getWinner() == Winner.NONE) {
			System.out.println("\n" + board + "\n");
			if (board.isBlackTurn()) {
				int col = getNNMove(board);
				while (!board.isLegalMove(col)) {
					System.out.println("Invalid move.");
					col = getNNMove(board);
				}
				board.makeMove(col);
			} else {
				board.makeMove(C4Player.getRedMove(board));
			}
		}
		
		System.out.println(board);
		System.out.println("Winner: " + board.getWinner());
		if (board.getWinner() == Winner.RED) {
			System.out.println("Computer wins, puny human.");
		} else if (board.getWinner() == Winner.BLACK) {
			System.out.println("You have defeated the computer!");
		}
	}

	public static int randomChoice(final INDArray probs) {
		final double draw = Math.random();
		float sum = 0.0f;
		for (int i = 0; i < probs.length(); i++) {
			sum += probs.getFloat(i);
			if (draw < sum) {
				return i;
			}
		}
		final double tol = 0.0001;
		assert Math.abs(sum - 1.0) < tol;
		
		return probs.length() - 1;
	}
	
	public static int getNNMove(final C4Board board) {
		final INDArray features = Nd4j.create(board.getAsFloatArray());
		// not training input, so false
        final INDArray predicted = net.output(features, false);
        return randomChoice(predicted);
	}
}
