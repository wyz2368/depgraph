package connectfourrl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
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

/**
 * Learns to play Connect Four against minimax opponent.
 * Learns by stochastic gradient descent with policy
 * gradient objective (i.e., the advantage-weighted
 * log likelihood of the selected move).
 */
public final class C4SimpleNNPlayer {
	
	/**
	 * Number of input neurons.
	 * One neuron per cell in board, for each of
	 * red and black.
	 */
	public static final int NUM_INPUTS = 84;
	/**
	 * Learning rate.
	 */
	private static final double LEARNING_RATE = 0.01;
	/**
	 * Games per epoch of play vs. opponent.
	 */
	private static final int GAMES_PER_EPOCH = 1000;
	/**
	 * Discount factor for future rewards in game.
	 */
	private static final double DISC_FACTOR = 0.95;
	/**
	 * L2 regularizer for network weights.
	 */
	private static final double REGULARIZER = 0.01;
	/**
	 * Stores episodes from recent games.
	 */
	private static final C4Memory MEMORY = new C4Memory();
	/**
	 * The neural network, a multilayer perceptron.
	 */
	private static MultiLayerNetwork net;
	
	/**
	 * Can indicate how verbosely the output should be printed.
	 */
	public static enum Verbosity {
		/**
		 * Print most verbose output.
		 */
		HIGH,
		/**
		 * Print somewhat verbose output.
		 */
		MEDIUM,
		/**
		 * Print minimal output.
		 */
		LOW
	}
	
	/**
	 * The level of verbosity to use in output.
	 */
	public static final Verbosity PRINT_LEVEL = Verbosity.LOW;
	/**
	 * Main method.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		final boolean isConv = true;
		final int roundCount = 100;
		 trainRounds(roundCount, 1, 0, isConv);
		
		/*
		final int maxEpochsPerRound = 10;
		final double targetWinRate = 0.3;
		final int maxOpponentLevel = 2;
		final boolean success = 
			trainUntilDone(maxEpochsPerRound, targetWinRate, maxOpponentLevel);
		System.out.println("Training result success: " + success);
		*/
		
		// playGameVsComputer(0);
		// final String outFileName = "epochData.csv";
		// memory.recordToFile(outFileName);
	}
	
	/**
	 * Trains against each opponent level from 0 through maxOpponentLevel,
	 * until reaching the targetWinRate in an epoch against the current
	 * level, or trying maxEpochsPerRound times without reaching this
	 * win rate and returning false (failure).
	 * Returns true if target win rate is reached against maxOpponentLevel.
	 * @param maxEpochsPerRound how many epochs of play and training
	 * are allowed against an opponent level before returning false
	 * @param targetWinRate the win rate in [-1, 1] that must be reached
	 * in an epoch of play against an opponent level before proceeding
	 * @param maxOpponentLevel the maximum opponent level train against
	 * @param isConv true if should use tensor version of input data,
	 * instead of vector version
	 * @return  true if maxOpponentLevel was beaten in an epoch with
	 * win rate at least targetWinRate, else false
	 */
	public static boolean trainUntilDone(
		final int maxEpochsPerRound,
		final double targetWinRate,
		final int maxOpponentLevel,
		final boolean isConv
	) {
		if (maxEpochsPerRound < 1
			|| targetWinRate <= -1.0 || targetWinRate > 1.0
			|| maxOpponentLevel > C4Player.MAX_SEARCH_DEPTH
		) {
			throw new IllegalArgumentException();
		}
		
		final long startTime = System.currentTimeMillis();
		if (isConv) {
			setupNetConv();
		} else {
			setupNetSimple();
		}
		for (int opponentLevel = 0;
			opponentLevel <= maxOpponentLevel;
			opponentLevel++) {
			MEMORY.clear();
			System.out.println(
				"Proceeding to opponentLevel: " + opponentLevel);
			boolean reachedTargetWinRate = false;
			for (int epoch = 0; epoch < maxEpochsPerRound; epoch++) {
				final double winRate = addMemoryEpoch(opponentLevel, isConv);
				if (winRate >= targetWinRate) {
					// move on to next opponentLevel
					reachedTargetWinRate = true;
					break;
				}
				
				trainFromMemory(isConv);
			}
			if (!reachedTargetWinRate) {
				System.out.println(
					"Failed to beat opponentLevel: " + opponentLevel);
				System.out.println("Tried " + maxEpochsPerRound + " epochs.");
				return false;
			}
		}
		
		System.out.println(
			"Cleared opponent level: " + maxOpponentLevel);
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		final DecimalFormat fmt = new DecimalFormat("#.###"); 
		System.out.println("Time taken: "
			+ fmt.format(durationInSec) + " seconds");
		return true;
	}
	
	/**
	 * Trains against a fixed opponent for a certain number of rounds.
	 * @param rounds how many rounds of training to do
	 * @param epochsPerRound how many epochs of play against opponent
	 * to do in between each round of training
	 * @param opponentLevel level to play against
	 * @param isConv whether to use convolutional version of training
	 * data (instead of fully-connected, vector version)
	 */
	public static void trainRounds(
		final int rounds,
		final int epochsPerRound,
		final int opponentLevel,
		final boolean isConv
	) {
		if (rounds < 1 || epochsPerRound < 1 || opponentLevel < 0
			|| opponentLevel > C4Player.MAX_SEARCH_DEPTH) {
			throw new IllegalArgumentException();
		}

		final long startTime = System.currentTimeMillis();
		if (isConv) {
			setupNetConv();
		} else {
			setupNetSimple();
		}
		// setupNetTwoLayer();
		for (int i = 0; i < rounds; i++) {
			for (int j = 0; j < epochsPerRound; j++) {
				addMemoryEpoch(opponentLevel, isConv);
			}
			trainFromMemory(isConv);
		}
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		final DecimalFormat fmt = new DecimalFormat("#.###"); 
		System.out.println("Time taken for all rounds: "
			+ fmt.format(durationInSec) + " seconds");
	}
	
	/**
	 * Private constructor for utility class.
	 */
	private C4SimpleNNPlayer() {
		// not called
	}
	
	/**
	 * Initializes the neural net.
	 * 
	 * Note:
	 * We use Nesterov momentum (0.9),
	 * L2 regularization,
	 * Xavier initialization of weights,
	 * softmax output,
	 * PolicyGradientLoss custom loss function.
	 * 
	 * consider:
	 * add RELU activation to dense layer,
	 * with another dense layer after it.
	 * try adding dropout and Adam optimizer.
	 */
	public static void setupNetSimple() {
		final int numHiddenNodes = 200;
        final MultiLayerConfiguration conf =
    		new NeuralNetConfiguration.Builder()
            .iterations(1)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(LEARNING_RATE)
            .updater(Updater.NESTEROVS)
            .regularization(true).l2(REGULARIZER)
            .list()
            .layer(0, new DenseLayer.Builder().nIn(NUM_INPUTS)
        		.nOut(numHiddenNodes)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(1, new OutputLayer
        		.Builder(new PolicyGradientLoss())
                .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                .nIn(numHiddenNodes).nOut(C4Board.WIDTH).build())
            .pretrain(false).backprop(true).build();
        
        net = new MultiLayerNetwork(conf);
        net.init();
	}
	
	/**
	 * Setup neural net with convolutions.
	 */
	public static void setupNetConv() {
		final int kernelWidth = 4;
		final int convFilters = 32;
		final int denseNeurons = 64;
		final double dropout = 0.5;
        final MultiLayerConfiguration conf =
    		new NeuralNetConfiguration.Builder()
            .iterations(1)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(LEARNING_RATE)
            .updater(Updater.NESTEROVS)
            .regularization(true)
            .l2(REGULARIZER)
            .list()
            .layer(0, new ConvolutionLayer.Builder(
        		new int[]{kernelWidth, kernelWidth},
        		new int[]{1, 1})
        		.nOut(convFilters)
        		.weightInit(WeightInit.XAVIER)
        		.convolutionMode(ConvolutionMode.Same)
        		.activation(Activation.RELU)
        		.build())
            .layer(1, new DenseLayer.Builder()
            	.nIn(convFilters * C4Board.WIDTH * C4Board.HEIGHT)
        		.nOut(denseNeurons)
                .weightInit(WeightInit.XAVIER)
                .dropOut(dropout)
                .activation(Activation.RELU)
                .build())
            .layer(2, new OutputLayer.Builder(
        		new PolicyGradientLoss())
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTMAX)
            	.nIn(denseNeurons)
                .nOut(C4Board.WIDTH).build())
            .setInputType(
        		InputType.convolutional(C4Board.HEIGHT, C4Board.WIDTH, 2))
            .pretrain(false).backprop(true).build();
        net = new MultiLayerNetwork(conf);
        net.init();
	}
	
	/*
	public static void setupNetTwoLayer() {
        final MultiLayerConfiguration conf =
    		new NeuralNetConfiguration.Builder()
            .iterations(1)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(LEARNING_RATE)
            .updater(Updater.NESTEROVS)
            .regularization(true).l2(REGULARIZER)
            .list()
            .layer(0, new DenseLayer.Builder().nIn(NUM_INPUTS)
        		.nOut(NUM_HIDDEN_NODES)
                .weightInit(WeightInit.XAVIER)
                //.activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder().nIn(NUM_HIDDEN_NODES)
        		.nOut(NUM_HIDDEN_NODES)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(2, new OutputLayer
        		.Builder(new PolicyGradientLoss())
                .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                .nIn(NUM_HIDDEN_NODES).nOut(C4Board.WIDTH).build())
            .pretrain(false).backprop(true).build();
        
        net = new MultiLayerNetwork(conf);
        net.init();
	}
	*/
	
	/**
	 * Perform a round of neural net training, with data in MEMORY.
	 * MEMORY must have data from play before this method is called.
	 * 
	 * @param isConv whether to use the tensor input format, for convolutional
	 * net.
	 */
	public static void trainFromMemory(final boolean isConv) {
		if (MEMORY.isEmpty()) {
			throw new IllegalStateException();
		}
		
		/*
		 * Loss function:
		 * sum over episodes: advantage * log-likelihood of action taken
		 */
		final long startTime = System.currentTimeMillis();
		
		final DataSet ds = MEMORY.getDataSetWithMasks(isConv);
		net.fit(ds);
		
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		if (PRINT_LEVEL == Verbosity.HIGH) {
			System.out.println("Time taken for training: "
				+ durationInSec + " seconds");
		}
	}
	
	/**
	 * Run GAMES_PER_EPOCH games between the neural network and an
	 * opponent of level oppoenentLevel.
	 * Add the episodes from this play epoch to MEMORY.
	 * @param opponentLevel the level of opponent to play against.
	 * @param isConv true if should use tensor representation of board,
	 * false if should use vector.
	 * @return the win rate of the neural net against the opponent,
	 * based on the mean score, with the score being -1 for a loss,
	 * 1 for a win, 0 for a draw.
	 */
	public static double addMemoryEpoch(
		final int opponentLevel,
		final boolean isConv
	) {
		if (opponentLevel < 0 || opponentLevel > C4Player.MAX_SEARCH_DEPTH) {
			throw new IllegalArgumentException();
		}
		final long startTime = System.currentTimeMillis();
		
		final int curEpoch = MEMORY.maxEpoch() + 1;
		final List<C4Episode> localMemory = new ArrayList<C4Episode>();
		int wins = 0;
		final int printFrequency = 1000;
		for (int game = 0; game < GAMES_PER_EPOCH; game++) {
			localMemory.addAll(playGameForLearning(
				curEpoch, opponentLevel, isConv));
			final double curReward =
				localMemory.get(localMemory.size() - 1).getDiscReward();
			if (curReward > 0.0) {
				wins++;
			} else if (curReward < 0.0) {
				wins--;
			}
			if (game % printFrequency == 0 && game > 0) {
				if (PRINT_LEVEL == Verbosity.HIGH) {
					System.out.println(game);
				}
			}
		}
		MEMORY.addEpoch(localMemory);
		
		long endTime = System.currentTimeMillis();
		final double thousand = 1000.0;
		final double durationInSec = (endTime - startTime) / thousand;
		final DecimalFormat fmt = new DecimalFormat("#.###"); 
		if (PRINT_LEVEL == Verbosity.HIGH) {
			System.out.println("Time taken for epoch: "
				+ fmt.format(durationInSec) + " seconds");
			System.out.println(
				"Sec per game: "
				+ fmt.format((durationInSec / GAMES_PER_EPOCH)));
		}

		final double winRate = wins * 1.0 / GAMES_PER_EPOCH;
		System.out.println("Win rate: " + fmt.format(winRate) 
			+ ", opponentLevel: " + opponentLevel
			+ ", epoch: " + curEpoch);
		return winRate;
	}
	
	/**
	 * Play a game as neural network against the opponent
	 * of level opponentSearchDepth, with episodes marked
	 * with epoch "epoch." Return a list of the episodes
	 * from the game.
	 * @param epoch an integer indicating how many epochs
	 * of play there have been already
	 * @param opponentSearchDepth opponent level to play
	 * against
	 * @param isConv true if should use tensor board
	 * representation, false if should use vector
	 * @return a list of all episodes from the game
	 */
	public static List<C4Episode> playGameForLearning(
		final int epoch,
		final int opponentSearchDepth,
		final boolean isConv
	) {
		final List<C4Episode> result = new ArrayList<C4Episode>();
		
		final C4Board board = new C4Board();
		C4Player.setSearchDepth(opponentSearchDepth);
		while (board.getWinner() == Winner.NONE) {
			if (board.isBlackTurn()) {
				// NN will play next.
				float[] nnInput = null;
				if (isConv) {
					nnInput = board.getAsFloatArray();
				} else {
					nnInput = board.getAsFloatArray();
				}
				
				int col = getNNMove(board, isConv);
				if (!board.isLegalMove(col)) {
					System.out.println(board);
					throw new IllegalStateException(
						"NN move must be legal: " + col);
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
	
	/**
	 * Play a game as neural net against opponent of level
	 * opponentSearchDepth, display the board after each move,
	 * and return nothing.
	 * @param opponentSearchDepth opponent level to play against
	 * @param isConv true if should use tensor board representation,
	 * false if should use vector
	 */
	public static void playGameVsComputer(
		final int opponentSearchDepth,
		final boolean isConv
	) {
		final C4Board board = new C4Board();
		C4Player.setSearchDepth(opponentSearchDepth);
		while (board.getWinner() == Winner.NONE) {
			System.out.println("\n" + board + "\n");
			if (board.isBlackTurn()) {
				int col = getNNMove(board, isConv);
				if (!board.isLegalMove(col)) {
					throw new IllegalStateException("NN move must be legal");
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
	
	/**
	 * Return a legal move, drawn according to the softmax
	 * outputs of the network, setting all illegal move outputs
	 * to 0. If all outputs are 0, return a random legl move
	 * but display a warning message.
	 * @param probs the softmax outputs of the network
	 * @param board the board state, used to check which
	 * moves are legal
	 * @return a choice based on the softmax outputs over
	 * legal moves only, or a random legal move if these
	 * are all zero.
	 */
	public static int constrainedChoice(
		final INDArray probs,
		final C4Board board
	) {
		final float[] vals = new float[C4Board.WIDTH];
		float sum = 0.0f;
		int modeIndex = -1;
		float mode = 0.0f;
		for (int i = 0; i < probs.length(); i++) {
			if (board.isLegalMove(i)) {
				vals[i] = probs.getFloat(i);
				sum += vals[i];
				if (vals[i] > mode) {
					mode = vals[i];
					modeIndex = i;
				}
			}
		}
		for (int i = 0; i < vals.length; i++) {
			vals[i] /= sum;
		}
		
		final double draw = Math.random();
		float total = 0.0f;
		for (int i = 0; i < vals.length; i++) {
			total += vals[i];
			if (draw < total) {
				return i;
			}
		}
		final double tol = 0.0001;
		assert Math.abs(sum - 1.0) < tol;
		
		if (modeIndex != -1) {
			return modeIndex;
		}
		if (PRINT_LEVEL == Verbosity.MEDIUM || PRINT_LEVEL == Verbosity.HIGH) {
			System.err.println("Making random legal move.");
		}
		return board.randomLegalMove();
	}

	/**
	 * Return a random draw from the softmax output of the network,
	 * which may be an illegal move.
	 * @param probs the softmax output of the network
	 * @return an int indicating the move chosen, which may be
	 * illegal.
	 */
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
	
	/**
	 * Pick the random move from the neural net's softmax
	 * activation, over legal moves only.
	 * @param board the board state, to check legality.
	 * @param isConv if true, represent board as tensor.
	 * otherwise, represent as vector
	 * @return a random legal move based on the softmax
	 * activations over legal moves only
	 */
	public static int getNNMove(final C4Board board, final boolean isConv) {
		INDArray features = null;
		if (isConv) {
			features = Nd4j.create(board.getAsFloatArray(),
				new int[]{1, 2, C4Board.HEIGHT, C4Board.WIDTH});
			if (PRINT_LEVEL == Verbosity.HIGH) {
				System.out.println(board);
				System.out.println(features);
			}
		} else {
			features = Nd4j.create(board.getAsFloatArray());
		}
		// not training input, so false
        final INDArray predicted = net.output(features, false);
        int result = constrainedChoice(predicted, board);
        assert board.isLegalMove(result);
        return result;
	}
}
