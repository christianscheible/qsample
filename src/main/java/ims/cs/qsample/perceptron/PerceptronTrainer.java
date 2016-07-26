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

import ims.cs.lingdata.Document;
import ims.cs.qsample.evaluate.EvaluateClassifier;
import ims.cs.qsample.models.QuotationPerceptrons;

import java.util.List;

/**
 * Trains perceptrons for boundary detection
 * Created by scheibcn on 3/3/16.
 */
public abstract class PerceptronTrainer {


    /**
     * Run full training pipeline for cue perceptron. Afterwards, make predictions on the specified data.
     * @param perceptrons perceptrons to be trained
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param numIterations number of epochs to train
     * @param transferLabelOnTraining should predictions also be made on the training data?
     */
    public static void trainCuePerceptronAndLabel(QuotationPerceptrons perceptrons, List<Document> trainDocs,
                                                  List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                                  int numIterations, boolean transferLabelOnTraining) {

        // training
        perceptrons.trainCuePerceptron(trainDocs, numIterations);

        // predict
        System.out.println("Predicting cues");
        if (transferLabelOnTraining) perceptrons.predictCues(trainDocs);
        if (testDocs != null) perceptrons.predictCues(testDocs);
        if (valDocs != null) perceptrons.predictCues(valDocs);
        if (resDocs != null) perceptrons.predictCues(resDocs);

        // add labels
        System.out.println("Adding labels");
        if (transferLabelOnTraining) perceptrons.cueScoreToLabel(trainDocs);
        if (testDocs != null) perceptrons.cueScoreToLabel(testDocs);
        if (valDocs != null) perceptrons.cueScoreToLabel(valDocs);
        if (resDocs != null) perceptrons.cueScoreToLabel(resDocs);
    }

    /**
     * Run full training pipeline for begin and end perceptron. Afterwards, make predictions on the specified data.
     * @param perceptrons perceptrons to be trained
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param train if false, training step may be bypassed (for debugging reasons)
     * @param numIterations number of epochs to train
     */
    public static void trainBoundaryPerceptronsAndLabel(QuotationPerceptrons perceptrons, List<Document> trainDocs,
                                                        List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                                        boolean train, int numIterations) {

        // train B/E perceptrons
        if (train) perceptrons.trainBeginEndPerceptrons(trainDocs, numIterations);

        // predict
        System.out.println("Predicting begin and end");
        perceptrons.predictBeginEnd(trainDocs);
        if (testDocs != null) perceptrons.predictBeginEnd(testDocs);
        if (valDocs != null) perceptrons.predictBeginEnd(valDocs);
        if (resDocs != null) perceptrons.predictBeginEnd(resDocs);
    }


    /**
     * Run full training pipeline for cue, begin, and end perceptron. Afterwards, make predictions on the specified data.
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param beginMargin positive margin for begin perceptron
     * @param endMargin positive margin for end perceptron
     * @param cueMargin positive margin for cue perceptron
     * @param cueOnly if true, do not train begin and end perceptrons
     * @param numIterationsCue number of epochs for cue training
     * @param numIterationsBoundary number of epochs for begin/end training
     * @return
     */
    public static QuotationPerceptrons trainAllPerceptronsAndApply(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                                                   double beginMargin, double endMargin, double cueMargin,
                                                                   boolean cueOnly, int numIterationsCue, int numIterationsBoundary) {
        // set up perceptrons
        Perceptron beginPerceptron = new Perceptron();
        Perceptron endPerceptron = new Perceptron();
        Perceptron cuePerceptron = new Perceptron();
        QuotationPerceptrons perceptrons = new QuotationPerceptrons(beginPerceptron, endPerceptron, cuePerceptron);

        // biasing
        beginPerceptron.marginPositive = beginMargin;
        endPerceptron.marginPositive = endMargin;
        cuePerceptron.marginPositive = cueMargin;

        // train cue perceptron
        trainCuePerceptronAndLabel(perceptrons, trainDocs, testDocs, valDocs, resDocs, numIterationsCue, true);

        // train boundary perceptron
        QuotationPerceptrons.extractFeaturesAboutCue(trainDocs, testDocs, valDocs, resDocs);
        trainBoundaryPerceptronsAndLabel(perceptrons, trainDocs, testDocs, valDocs, resDocs, !cueOnly, numIterationsBoundary);

        // evaluate perceptrons
        EvaluateClassifier.evaluateAndPrint(trainDocs, testDocs, valDocs, resDocs, "FINAL");

        return perceptrons;
    }

    /**
     * Run full training pipeline for cue, begin, and end perceptron. Use jackknifing to train cue model.
     * Afterwards, make predictions on the specified data.
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param beginMargin positive margin for begin perceptron
     * @param endMargin positive margin for end perceptron
     * @param cueMargin positive margin for cue perceptron
     * @param cueOnly if true, do not train begin and end perceptrons
     * @param numIterationsCue number of epochs for cue training
     * @param numIterationsBoundary number of epochs for begin/end training
     * @param numFoldJackknifing number of folds for jackknifing
     * @return
     */
    public static QuotationPerceptrons trainAllPerceptronsAndApplyWithJackknifing(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                                                                  double beginMargin, double endMargin, double cueMargin,
                                                                                  boolean cueOnly, int numIterationsCue, int numIterationsBoundary,
                                                                                  int numFoldJackknifing) {
        // set up perceptrons for jackknifing
        Perceptron cuePerceptronTrain = new Perceptron();
        cuePerceptronTrain.marginPositive = cueMargin;
        QuotationPerceptrons perceptronsForTrain = new QuotationPerceptrons(null, null, null);

        // jackknife cue model
        perceptronsForTrain.jackknifeCue(trainDocs, numFoldJackknifing, numIterationsCue, cueMargin);

        // set up perceptrons for testing
        Perceptron beginPerceptronTest = new Perceptron();
        Perceptron endPerceptronTest = new Perceptron();
        Perceptron cuePerceptronTest = new Perceptron();
        QuotationPerceptrons perceptronsForTest = new QuotationPerceptrons(beginPerceptronTest, endPerceptronTest, cuePerceptronTest);

        // biasing
        beginPerceptronTest.marginPositive = beginMargin;
        endPerceptronTest.marginPositive = endMargin;
        cuePerceptronTest.marginPositive = cueMargin;

        // train cue perceptrons
        trainCuePerceptronAndLabel(perceptronsForTest, trainDocs, testDocs, valDocs, resDocs, numIterationsCue, false);

        // train boundary perceptrons
        QuotationPerceptrons.extractFeaturesAboutCue(trainDocs, testDocs, valDocs, resDocs);
        trainBoundaryPerceptronsAndLabel(perceptronsForTest, trainDocs, testDocs, valDocs, resDocs, !cueOnly, numIterationsBoundary);

        // evaluate perceptrons
        EvaluateClassifier.evaluateAndPrint(trainDocs, testDocs, valDocs, resDocs, "FINAL");

        return perceptronsForTest;
    }
}
