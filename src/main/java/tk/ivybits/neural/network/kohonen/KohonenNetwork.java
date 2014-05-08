package tk.ivybits.neural.network.kohonen;

import lombok.Data;
import lombok.Getter;

import java.util.*;

import static java.lang.Math.*;
import static tk.ivybits.neural.network.kohonen.KohonenNetwork.LearnMethod.*;
import static tk.ivybits.neural.network.VecMath.*;

// Based off Jeff Heaton's neural network examples - heatonresearch.org
@Data
public class KohonenNetwork<T> {
    public enum LearnMethod {
        ADDITIVE, SUBTRACTIVE
    }

    protected double[][] outputWeights;
    protected double error;
    @Getter
    protected final int inputNeuronCount, outputNeuronCount;
    @Getter
    protected double[] output;
    protected HashMap<T, double[]> samples = new HashMap<>();
    protected T[] _neuronMap;

    public KohonenNetwork(int in, int out) {
        this.inputNeuronCount = in;
        this.outputNeuronCount = out;
        this.output = new double[out];
        outputWeights = new double[outputNeuronCount][inputNeuronCount + 1];
    }

    public T[] getNeuronMap() {
        if (_neuronMap != null) return _neuronMap;
        Object map[] = new Object[samples.size()];

        for (Map.Entry<T, double[]> ds : samples.entrySet()) {
            int best = winner(ds.getValue());
            map[best] = ds.getKey();
        }
        return _neuronMap = (T[]) map;
    }

    public T recall(double[] input) {
        return getNeuronMap()[winner(input)];
    }

    /**
     * Presents an input vector to the network.
     *
     * @param input The input vector to present to the network.
     * @return The neuron which fired.
     */
    public int winner(double input[]) {
        double biggest = Double.MIN_VALUE;
        double normalizationFactor = 1.0 / magnitudeOf(input);
        // The winning neuron
        int winning = 0;
        for (int i = 0; i < outputNeuronCount; i++) {
            // To calculate the weight of an output neuron,
            // 1. get the dot product of the input and weight vectors
            // 2. multiply by the normalization factor
            // 3. map to bipolar (add 1 and / by 2)
            double weight = (dot(input, outputWeights[i]) * normalizationFactor + 1) * 0.5;
            // Clamp to 0, 1
            output[i] = clamp(weight, 0, 1);
            if (weight > biggest) {
                biggest = weight;
                winning = i;
            }
        }

        return winning;
    }

    /**
     * Evaluates errors in the network and fills a matrix of correction values to adjustWeights on.
     *
     * @param rate        The rate at which to adjust weights, a real number less than 1.
     * @param method      The method to use to learn.
     * @param won         A container to store how many times each neuron won.
     * @param corrections A container to store corrections.
     * @return The total error of this network.
     */
    protected double evaluateErrors(double rate, LearnMethod method, int won[], double corrections[][]) {
        double[] weights = method == ADDITIVE ? new double[inputNeuronCount + 1] : null;

        double largestError = 0.0;

        // loop through all training sets to determine correction
        for (double[] set : samples.values()) {
            int best = winner(set);
            won[best]++;
            double[] output = outputWeights[best];
            double[] correction = corrections[best];
            double length = 0.0;

            double normalizationFactor = 1.0 / magnitudeOf(set);

            for (int i = 0; i < inputNeuronCount; i++) {
                double diff = set[i] * normalizationFactor - output[i];
                length += diff * diff;
                if (method == SUBTRACTIVE)
                    correction[i] += diff;
                else
                    weights[i] = rate * set[i] * normalizationFactor + output[i];
            }
            double diff = output[inputNeuronCount];
            length += diff * diff;
            if (method == SUBTRACTIVE)
                correction[inputNeuronCount] = diff;
            else
                weights[inputNeuronCount] = output[inputNeuronCount];

            if (length > largestError)
                largestError = length;

            if (method == ADDITIVE) {
                normalize(weights);
                for (int i = 0; i <= inputNeuronCount; i++)
                    correction[i] += weights[i] - output[i];
            }
        }
        return sqrt(largestError);
    }


    /**
     * Adjusts the network weights based on the given corrections.
     *
     * @param rate        The rate at which to adjust weights, a real number less than 1.
     * @param method      The method to use to learn.
     * @param won         A container to store how many times each neuron won.
     * @param corrections A container containing connections.
     * @return The largest correction made.
     */
    protected double adjustWeights(double rate, LearnMethod method, int won[], double corrections[][]) {
        double largestCorrection = 0.0;

        for (int i = 0; i < outputNeuronCount; i++) {
            if (won[i] == 0)
                continue;

            double f = 1.0 / (double) won[i];
            if (method == SUBTRACTIVE)
                f *= rate;

            double length = 0.0;

            for (int j = 0; j <= inputNeuronCount; j++) {
                double corr = f * corrections[i][j];
                outputWeights[i][j] += corr;
                length += corr * corr;
            }

            if (length > largestCorrection)
                largestCorrection = length;
        }
        // Scale correction by learning rate
        return sqrt(largestCorrection) / rate;
    }

    /**
     * Forces the least active neuron to win.
     *
     * @param won         A container which stores how many times each neuron won.
     */
    protected void forceWin(int[] won) {
        double dist = Double.MAX_VALUE;

        // Iterate over all training sets and find the one which produces the least output
        double[] worstSet = samples.values().iterator().next();
        for (double[] inputSet : samples.values()) {
            int n = winner(inputSet);
            if (output[n] < dist) {
                dist = output[n];
                worstSet = inputSet;
            }
        }

        // Iterate over all output neurons to find one with most potential that wasn't activated
        // If we activate such a neuron, we help balance the network
        dist = Double.MIN_VALUE;
        double[] outputs = outputWeights[0];
        for (int n = 0; n != outputNeuronCount; n++) {
            // Find one which wasn't activated
            if (won[n] != 0) continue;
            // Check if its weight is greater than any we have so far
            if (output[n] > dist) {
                // If it is, we should activate it
                dist = output[n];
                outputs = outputWeights[n];
            }
        }
        System.arraycopy(worstSet, 0, outputs, 0, worstSet.length);

        outputs[inputNeuronCount] = 0;
        normalize(outputs);
    }

    public void queueData(T t, double[] data) {
        _neuronMap = null;
        samples.put(t, data);
    }

    /**
     * Trains the network using the current training sets.
     *
     */
    public void learn() {
        // The additive method was the one originally proposed by Kohonen, and tends
        // to give better results than the subtractive method
        learn(ADDITIVE);
    }

    /**
     * Trains the network using the current training sets.
     *
     * @param method      The method to use to learn.
     */
    public void learn(LearnMethod method) {
        learn(0.4, 0.1, 0.99, 10000, method);
    }

    /**
     * Trains the network using the current training sets.
     *
     * @param learnRate       The weight adjustment factor, from 0..1. The higher this value is, the faster the network learns
     *                        but the more inaccurate its results may be.
     * @param quitError       The acceptable error range. Once, the network's error falls below this value training is completed
     *                        and this method returns.
     * @param reductionFactor The value to multiply the learn rate by each epoch. Allows the learn rate to decrease over
     *                        time and the network to be "polished".
     * @param retries         How many epochs to simulate.
     * @param method          The method to use to learn.
     */
    public void learn(double learnRate, double quitError, double reductionFactor, int retries, LearnMethod method) {
        KohonenNetwork best = new KohonenNetwork(inputNeuronCount, outputNeuronCount);

        // Keep track of the current learning rate
        double rate = learnRate;

        // Initialize the weight matrix to normalized random values
        init();

        double bestErr = Double.MAX_VALUE;

        for (int epoch = 0; ; ) {
            // Keep track of how many times each neuron won
            int[] won = new int[outputNeuronCount];


            // Container for neuron corrections
            double[][] corrections = new double[outputNeuronCount][inputNeuronCount + 1];

            double totalError = evaluateErrors(rate, method, won, corrections);

            // A better matrix has been found
            if (totalError < bestErr) {
                bestErr = totalError;
                // Copy to our matrix
                best.outputWeights = outputWeights;
            }

            // A suitable matrix has been found; exit training
            if (totalError < quitError) break;

            // Some neurons might not have won at all, meaning some neurons won more than once
            int winners = 0;
            for (int i = 0; i != won.length; winners += (won[i++] != 0 ? 1 : 0)) ;
            if (winners < max(outputNeuronCount, samples.size())) {
                // In that case, force them to win to offload
                forceWin(won);
                continue;
            }

            double correction = adjustWeights(rate, method, won, corrections);
            // If the correction is too low, there is no point continuing with this current
            // configuration, so reset our parent weight matrix and restart
            if (correction < 1E-5) {
                if (++epoch > retries) break;
                // Start a new cycle
                init();
                rate = learnRate;
                continue;
            }

            // As training progresses decrease the learning rate
            // This results in the network starting to learn very quickly, then slowing down
            // to "polish" the weights
            if (rate > 0.1) {
                rate *= reductionFactor;
            }
        }

        error = bestErr;

        // Copy weights to our network
        outputWeights = best.outputWeights;
        // And normalize
        for (double[] weight : outputWeights) {
            normalize(weight);
        }
    }

    /**
     * Initializes the output weight matrix to random normalized values.
     */
    protected void init() {
        // Initialize a randomized weight matrix - helps the network converge faster
        for (int x = 0; x < outputWeights.length; x++) {
            for (int y = 0; y < outputWeights[0].length; y++) {
                outputWeights[x][y] = Math.random() * 10 - 5;
            }
        }
        // Weights must be normalized
        for (int n = 0; n != outputWeights.length; normalize(outputWeights[n++])) ;
    }
}
