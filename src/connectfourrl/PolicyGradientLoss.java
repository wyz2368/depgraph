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

@SuppressWarnings("serial")
public final class PolicyGradientLoss implements ILossFunction {

    /* This example illustrates how to implements a custom loss function 
     * that can then be applied to training your neural net
       All loss functions have to implement the ILossFunction interface
       The loss function implemented here is:
       L = (y - y_hat)^2 +  |y - y_hat|
        y is the true label, y_hat is the predicted output
     */

    @SuppressWarnings("unused")
	private static Logger logger =
		LoggerFactory.getLogger(PolicyGradientLoss.class);

    /*
    Needs modification depending on your loss function
        scoreArray calculates the loss for a single data point 
        or in other words a batch size of one
        It returns an array the shape and size of the output of the neural net.
        Each element in the array is the loss function applied to 
        the prediction and it's true value
        scoreArray takes in:
        true labels - labels
        the input to the final/output layer of the neural network - preOutput,
        the activation function on the final layer of the 
        neural network - activationFn
        the mask - (if there is a) mask associated with the label
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

        //Weighted loss function
        if (mask != null) {
        	/*
            if (mask.length() != scoreArr.size(1)) {
                throw new IllegalStateException(
        		"Weights vector (length " + mask.length()
                + ") does not match output.size(1)=" + preOutput.size(1));
            }
            */
            scoreArr = scoreArr.muli(mask);
        }

        return scoreArr;
    }

    /*
    Remains the same for all loss functions
    Compute Score computes the average loss function across many datapoints.
    The loss for a single datapoint is summed over all output features.
     */
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

    /*
    Needs modification depending on your loss function
        Compute the gradient wrt to the preout 
        (which is the input to the final layer of the neural net)
    */
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
            /*
        	if (mask != null && LossUtil.isPerOutputMasking(output, mask)) {
                throw new UnsupportedOperationException(
            		"Per output masking for MCXENT + softmax: not supported");
            }
            */

            //Weighted loss function
            if (mask != null) {
            	/*
                if (mask.length() != output.size(1)) {
                    throw new IllegalStateException(
            		"Weights vector (length " + mask.length()
                    + ") does not match output.size(1)=" + output.size(1));
                }
                */
                INDArray temp = labels.muli(mask);
                INDArray col = temp.sum(1);
                grad = output.mulColumnVector(col).sub(temp);
            } else {
                grad = output.subi(labels);
            }
        } else {
            INDArray dLda = output.rdivi(labels).negi();

            grad = activationFn.backprop(preOutput, dLda).getFirst(); 

            //Weighted loss function
            if (mask != null) {
                if (mask.length() != output.size(1)) {
                    throw new IllegalStateException(
                		"Weights vector (length " + mask.length()
                        + ") does not match output.size(1)=" + output.size(1));
                }
                grad.muliRowVector(mask);
            }
        }

        //Loss function with masking
        if (mask != null) {
            LossUtil.applyMask(grad, mask);
        }

        return grad;
    }

    //remains the same for a custom loss function
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

    @SuppressWarnings("static-method")
	protected boolean canEqual(final Object other) {
        return other instanceof PolicyGradientLoss;
    }
}
