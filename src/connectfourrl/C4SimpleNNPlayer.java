package connectfourrl;

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
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import connectfourdomain.C4Board;
import connectfourdomain.C4Player;
import connectfourdomain.C4Board.Winner;

public final class C4SimpleNNPlayer {
	
	private static MultiLayerNetwork net;

	public static void main(final String[] args) {
		setupNet();
		playGameVsComputer();
	}
	
	private C4SimpleNNPlayer() {
		// not called
	}
	
	public static void setupNet() {
		final double learningRate = 0.0001;
        final int numInputs = 84;
        final int numHiddenNodes = 200;
        final int numOutputs = 7;
        
        final MultiLayerConfiguration conf =
    		new NeuralNetConfiguration.Builder()
            .iterations(1)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(learningRate)
            .updater(Updater.NESTEROVS)
            .list()
            .layer(0, new DenseLayer.Builder().nIn(numInputs).
        		nOut(numHiddenNodes)
                .weightInit(WeightInit.XAVIER)
                .build())
            .layer(1, new OutputLayer
        		.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                .nIn(numHiddenNodes).nOut(numOutputs).build())
            .pretrain(false).backprop(true).build();
        
        net = new MultiLayerNetwork(conf);
        net.init();
	}
	
	public static void playGameVsComputer() {
		final C4Board board = new C4Board();
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
