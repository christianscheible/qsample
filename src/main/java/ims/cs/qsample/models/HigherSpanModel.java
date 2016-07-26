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


package ims.cs.qsample.models;

import ims.cs.qsample.features.FeatureSet;
import ims.cs.qsample.perceptron.Perceptron;
import ims.cs.qsample.spans.Span;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;

/**
 * A model for scoring a whole span (rather than just begin and end information)
 * Created by scheibcn on 3/5/16.
 */
public class HigherSpanModel implements Serializable {

    private static final long serialVersionUID = 3509778136938744648L;

    // We actually make separate models for begin, end, and span-level information.
    // This makes feature management easier, among other things.
    Perceptron beginPerceptron;
    Perceptron endPerceptron;
    Perceptron higherOrderPerceptron;

    public HigherSpanModel() {
        this.beginPerceptron = new Perceptron();
        this.endPerceptron = new Perceptron();
        this.higherOrderPerceptron = new Perceptron();
    }

    /**
     * Computes the current score of a span according to the model
     * @param span
     * @param average use averaged perceptron?
     * @return
     */
    public double score(Span span, boolean average) {
        // we handle the begin, end, and span features separately
        FeatureSet beginFeatures = span.first().boundaryFeatureSet;
        FeatureSet endFeatures = span.last().boundaryFeatureSet;
        FeatureSet spanFeatures = span.featureSet;

        // ... then, we can compute three individual scores
        double score = 0;
        score += beginPerceptron.score(beginFeatures, average);
        score += endPerceptron.score(endFeatures, average);
        score += higherOrderPerceptron.score(spanFeatures, average);

        return score;
    }

    /**
     * Train the model using a given span, updating with a specified learning rate
     * @param span
     * @param isPositive Has the example been correctly classified?
     * @param rate learning rate
     */
    public void train(Span span, boolean isPositive, double rate) {
        FeatureSet leftFeatures = span.first().boundaryFeatureSet;
        FeatureSet rightFeatures = span.last().boundaryFeatureSet;
        FeatureSet spanFeatures = span.featureSet;

        // negate the learning rate if the example was wrong
        double effectiveRate = rate;
        if (!isPositive) effectiveRate = -effectiveRate;

        // update the three models separately
        //   (use the update function directly as the train function would first check the score, which is nonsensical
        //   for the individual models)
        beginPerceptron.update(leftFeatures, effectiveRate);
        endPerceptron.update(rightFeatures, effectiveRate);
        higherOrderPerceptron.update(spanFeatures, effectiveRate);
    }


    /**
     * Writes the current feature weights to a file
     * @param fileName
     * @throws FileNotFoundException
     */
    public void printWeights(String fileName) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(fileName);
        PrintStream ps = new PrintStream(fos);
        beginPerceptron.printWeights(ps, "BEGIN");
        endPerceptron.printWeights(ps, "END");
        higherOrderPerceptron.printWeights(ps, "HIGHER");
        ps.close();
    }

}
