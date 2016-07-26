/*
 * This file is part of QSample.
 * QSample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QSample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QSample.  If not, see <http://www.gnu.org/licenses/>.
 */


package ims.cs.qsample.perceptron;

import ims.cs.qsample.features.FeatureSet;
import ims.cs.qsample.spans.Span;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Implementation of perceptron model
 * Created by scheibcn on 11/5/15.
 */
public class Perceptron  implements Serializable {
    private static final long serialVersionUID = 3436601656314837271L;

    // this model can actually also do logistic regression
    public enum UpdateType {PERCEPTRON, LR};

    // .. default is perceptron
    public UpdateType updateType = UpdateType.PERCEPTRON;

    public Weights weights = new Weights();

    // parameters
    public double fixedBias = 0;       /* optional bias that can be manually adjusted */
    public double marginPositive = 1;  /* margin for positive class */
    public double marginNegative = 1;  /* margin for negative class */

    // some debugging data
    public int numUpdates = 0;


    public Perceptron() {
        weights.weightMap.put("BIAS", 0.0);
    }


    /**
     * Score a feature set
     * @param featureSet
     * @return
     */
    public double score(FeatureSet featureSet, boolean average) {
        double score = 0;

        // first, add bias
        if (average) {
            score += weights.getAvg("BIAS");
            score += fixedBias;
        } else {
            score += weights.get("BIAS");
        }

        // then, score all features in the data
        for (String feature: featureSet) {
            if (average) {
                score += weights.getAvg(feature);
            } else {
                score += weights.get(feature);
            }
        }

        return score;
    }

    /**
     * Perform an update with a given training example
     * @param featureSet
     * @param isPositive is this example a positive one?
     * @param rate
     */
    public void train(FeatureSet featureSet, boolean isPositive, double rate) {
        if (updateType == UpdateType.PERCEPTRON)
            trainPerceptron(featureSet, isPositive, rate);   /* perceptron update */
        else if (updateType == UpdateType.LR)
            trainLr(featureSet, isPositive, rate);           /* logistic regression update */
    }

    /**
     * Perform a perceptron-style update
     * @param featureSet
     * @param isPositive
     * @param rate
     */
    public void trainPerceptron(FeatureSet featureSet, boolean isPositive, double rate) {
        double predScore = score(featureSet, false);

        if (isPositive && predScore - marginPositive <= 0) {  /* positive example and negative margin violation */
            update(featureSet, rate);
        } else if (!isPositive && predScore + marginNegative > 0) { /* negative example and positive margin violation */
            update(featureSet, -rate);
        }
    }

    /**
     * Perform a logistic regression update
     * @param featureSet
     * @param isPositive
     * @param rate
     */
    public void trainLr(FeatureSet featureSet, boolean isPositive, double rate) {
        double predScore = score(featureSet, false);

        // true probability of the example?
        int trueProb;
        if (isPositive) trueProb = 1;
        else trueProb = 0;

        // learning rate times LR gradient
        double step = rate * (trueProb - sigmoid(predScore));

        update(featureSet, step);
    }


    /**
     * Update the weights for each feature by the given rate
     * @param featureSet
     * @param rate
     */
    public void update(FeatureSet featureSet, double rate) {
        // bias
        weights.update("BIAS", rate);

        // features
        for (String feature : featureSet) {
            weights.update(feature, rate);
        }

        numUpdates++;
    }

    /**
     * Print the weights for the features of the span to debug
     * @param span
     * @param prefix
     */
    public void printInfo(Span span, String prefix) {
        for (String feature: span.featureSet) {
            double weight = weights.get(feature);
            System.out.println(prefix + feature + "   " + weight);
        }
    }

    /**
     * Write the current feature weights to a file
     * @param fileName
     * @throws FileNotFoundException
     */
    public void printWeights(String fileName) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(fileName);
        PrintStream ps = new PrintStream(fos);
        printWeights(ps, "");
    }

    /**
     * Print the current feature weights to stdout
     */
    public void printWeights() {
        printWeights(System.out, "");
    }

    /**
     * Write the current feature weights to a stream, prepend each line with the specified prefix
     * @param out
     * @param prefix
     */
    public void printWeights(PrintStream out, String prefix) {
        for (Map.Entry entry : weights.weightMap.entrySet()) {
            out.println(prefix + "-->" + entry.getKey() + "\t" + entry.getValue());
        }
    }


    /**
     * Calculate the sigmoid of x
     * @param x
     * @return
     */
    public static double sigmoid(double x) {
        return 1/(1+Math.exp(-x));
    }

}
