package connectfourrl;

import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.activations.impl.ActivationSoftmax;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.nd4j.linalg.lossfunctions.LossUtil;

/**
 * The loss function for policy gradient learning.
 * 
 * The loss is defined as the log likelihood of the
 * action actually sampled (i.e., taken) by the agent,
 * after softmax activation of the output layer,
 * multiplied by the advantage of the result (i.e.,
 * the discounted reward, after normalization over an
 * epoch to be zero-mean and unit variance).
 */
public final class PolicyGradientLoss implements ILossFunction {

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
	 * @param labels the output labels
	 * @param preOutput (not sure what this does)
	 * @param activationFn the loss function
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

        INDArray output = activationFn.getActivation(preOutput.dup(), true);
        INDArray scoreArr = Transforms.log(output, false).muli(labels);

        // Weighted loss function
        if (mask != null) {
            scoreArr = scoreArr.muli(mask);
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

        return score;
    }

    @Override
    public INDArray computeScoreArray(
		final INDArray labels, final INDArray preOutput, 
		final IActivation activationFn, final INDArray mask) {
        INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
        return scoreArr.sum(1);
    }

    @Override
    public INDArray computeGradient(
		final INDArray labels, final INDArray preOutput, 
		final IActivation activationFn, final INDArray mask) {
        if (labels.size(1) != preOutput.size(1)) {
            throw new IllegalArgumentException(
                "Labels array numColumns (size(1) = " 
        		+ labels.size(1) + ") does not match output layer"
                + " number of outputs (nOut = " + preOutput.size(1) + ") ");

        }
        INDArray grad;
        INDArray output = activationFn.getActivation(preOutput.dup(), true);

        if (activationFn instanceof ActivationSoftmax) {

            // Weighted loss function
            if (mask != null) {
                INDArray temp = labels.muli(mask);
                INDArray col = temp.sum(1);
                grad = output.mulColumnVector(col).sub(temp);
            } else {
                grad = output.subi(labels);
            }
        } else {
            INDArray dLda = output.rdivi(labels).negi();

            grad = activationFn.backprop(preOutput, dLda).getFirst(); 

            // Weighted loss function
            if (mask != null) {
                if (mask.length() != output.size(1)) {
                    throw new IllegalStateException(
                		"Weights vector (length " + mask.length()
                        + ") does not match output.size(1)=" + output.size(1));
                }
                grad.muliRowVector(mask);
            }
        }

        // Loss function with masking
        if (mask != null) {
            LossUtil.applyMask(grad, mask);
        }

        return grad;
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
