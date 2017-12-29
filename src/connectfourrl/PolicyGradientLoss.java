package connectfourrl;

import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import connectfourdomain.C4Board;

/**
 * The loss function for policy gradient learning.
 * 
 * The loss is defined as the negative log likelihood of the
 * action actually sampled (i.e., taken) by the agent,
 * (i.e., the softmax output layer's value for the taken action),
 * multiplied by the advantage of the result (i.e.,
 * the discounted reward, after normalization over an
 * epoch to be zero-mean and unit variance).
 */
public final class PolicyGradientLoss implements ILossFunction {
	
	/**
	 * If true, make debugging checks.
	 */
	private static final boolean DEBUG = true;

	/**
	 * For serialization.
	 */
	private static final long serialVersionUID = -1914178080356540759L;
	
	/**
	 * Logger included to match example code.
	 */
	@SuppressWarnings("unused")
	private static Logger logger =
		LoggerFactory.getLogger(PolicyGradientLoss.class);

	/**
	 * @param labels the output labels. will be all ones.
	 * @param preOutput input to the softmax activation of last layer
	 * of network
	 * @param activationFn the softmax over network outputs
	 * @param mask normally used to mask (set to 0) the loss for
	 * ignored elements, but we use to multiply loss by the
	 * activation of an episode
	 * @return the score of each episode
	 */
    private static INDArray scoreArray(
		final INDArray labels,
		final INDArray preOutput, 
		final IActivation activationFn,
		final INDArray mask) {
        if (labels.size(1) != preOutput.size(1)) {
            throw new IllegalArgumentException(
                "Labels array numColumns (size(1) = " 
        		+ labels.size(1) + ") does not match output layer"
                + " number of outputs (nOut = " + preOutput.size(1) + ") ");
        }
        
        final int myDebugRow = 7;
        
        // preOutput has already been multiplied by the mask,
        // which holds 1.0 for entries to be ignored (masked),
        // and x for the entry to be used, where x is the activation.
        
        // divide preOutput by the mask, to undo the effect of the mask.
        // the result, preOuputUnmask, is the output of the neural net
        // before softmax activation.
        final INDArray preOutputUnmask = preOutput.div(mask);
        
        if (DEBUG) {
        	double total = 0.0;
        	for (int i = 0; i < C4Board.WIDTH; i++) {
        		total += preOutputUnmask.getDouble(myDebugRow, i);
        	}
        	final double tolerance = 0.0001;
        	if (Math.abs(1.0 - total) < tolerance) {
        		throw new IllegalArgumentException(
    				"preOutput has already had softmax: " + preOutputUnmask);
        	} else {
        		System.out.println(
    				"output before softmax: "
						+ preOutputUnmask.getRow(myDebugRow));
        	}
        }
        
        // get the softmax over the output neuron values,
        // in the array "output".
        INDArray output = activationFn.getActivation(preOutputUnmask, true);
        if (DEBUG) {
        	// should sum to 1
        	System.out.println(
    			"\toutput after softmax: " + output.getRow(myDebugRow));
        }
        
        // fixedMask will hold 0.0 for all entries in mask that equal 1.0,
        // and 1.0 for all entries in mask that do not equal 1.0.
        // the purpose is to mask entries to ignore (moves not chosen).
        final INDArray fixedMask = mask.dup();
        final double tol = 0.0001;
        for (int i = 0; i < C4Memory.DATASET_SIZE; i++) {
        	for (int j = 0; j < C4Board.WIDTH; j++) {
        		if (Math.abs(fixedMask.getDouble(i, j) - 1.0) < tol) {
        			// entry had value 1.0 -> mask it with 0.0
        			fixedMask.putScalar(new int[]{i, j}, 0.0f);
        		} else {
        			// entry had the activation level -> leave unmasked,
        			// with 1.0
        			fixedMask.putScalar(new int[]{i, j}, 1.0f);
        		}
        	}
        }
        
        // take the negative log of each probability from the
        // softmax activations.
        INDArray scoreArr = Transforms.log(output, false).mul(-1.0);
        
        // multiply each output entry by its fixedMask,
        // which will be zero unless it's
        // the output actually used, else 1.
        scoreArr = scoreArr.muli(fixedMask).muli(mask);
        if (DEBUG) {
        	System.out.println("\t\tmask: " + mask.getRow(myDebugRow));
        	System.out.println(
    			"\t\tfixedMask: " + fixedMask.getRow(myDebugRow));
        	System.out.println("\t\tscores: " + scoreArr.getRow(myDebugRow));
        }
        return scoreArr;
    }

    @Override
    public double computeScore(
		final INDArray labels, final INDArray preOutput, 
		final IActivation activationFn, final INDArray mask, 
		final boolean average) {
        INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);

        double score = scoreArr.sumNumber().doubleValue();

        if (average) {
            score /= scoreArr.size(0);
        }

        if (DEBUG) {
            System.out.println("score: " + score);
        }
        return score;
    }

    @Override
    public INDArray computeScoreArray(
		final INDArray labels, final INDArray preOutput, 
		final IActivation activationFn, final INDArray mask) {
        INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
        INDArray result = scoreArr.sum(1);
        return result;
    }
    
    @Override
    public INDArray computeGradient(
		final INDArray labels,
		final INDArray preOutput, 
		final IActivation activationFn,
		final INDArray mask
	) {
        final INDArray preOutputUnmask = preOutput.div(mask);
        INDArray output = activationFn.getActivation(preOutputUnmask, true);
        final int myDebugRow = 7;
        if (DEBUG) {
        	// should sum to 1
        	System.out.println(
    			"\tgradient output after softmax: "
					+ output.getRow(myDebugRow));
        }
        
        final INDArray fixedMask = mask.dup();
        final double tol = 0.0001;
        for (int i = 0; i < C4Memory.DATASET_SIZE; i++) {
        	for (int j = 0; j < C4Board.WIDTH; j++) {
        		if (Math.abs(fixedMask.getDouble(i, j) - 1.0) < tol) {
        			// entry had value 1.0 -> mask it with 0.0
        			fixedMask.putScalar(new int[]{i, j}, 0.0f);
        		} else {
        			// entry had the activation level -> leave unmasked,
        			// with 1.0
        			fixedMask.putScalar(new int[]{i, j}, 1.0f);
        		}
        	}
        }
        INDArray scoreArr = output.muli(fixedMask).muli(mask);
        if (DEBUG) {
        	final int myRow = 7;
        	System.out.println("\t\tgradient mask: " + mask.getRow(myDebugRow));
        	System.out.println(
    			"\t\tgradient fixedMask: " + fixedMask.getRow(myDebugRow));
        	System.out.println("gradient: " + scoreArr.getRow(myRow));
        }
        return scoreArr;
    }

    @Override
    public Pair<Double, INDArray> computeGradientAndScore(
		final INDArray labels, final INDArray preOutput, 
		final IActivation activationFn, 
		final INDArray mask, final boolean average) {
        return new Pair<>(
            computeScore(labels, preOutput, activationFn, mask, average),
            computeGradient(labels, preOutput, activationFn, mask));
    }

    @Override
    public String name() {
        return "PolicyGradientLoss";
    }


    @Override
    public String toString() {
        return "PolicyGradientLoss()";
    }

    @Override
	public boolean equals(final Object o) {
        if (o == this) {
        	return true;
        }
        if (!(o instanceof PolicyGradientLoss)) {
        	return false;
        }
        final PolicyGradientLoss other = (PolicyGradientLoss) o;
        if (!other.canEqual((Object) this)) {
        	return false;
        }
        return true;
    }

    @Override
	public int hashCode() {
        int result = 1;
        return result;
    }

    /**
     * @param other test object
     * @return true if the object is a PolicyGradientLoss
     */
    @SuppressWarnings("static-method")
	protected boolean canEqual(final Object other) {
        return other instanceof PolicyGradientLoss;
    }
}
