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

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.qsample.evaluate.EvaluateClassifier;
import ims.cs.qsample.features.BoundaryFeatures;
import ims.cs.qsample.perceptron.Perceptron;
import ims.cs.util.NewStaticPrinter;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bundle of three perceptrons required for boundary detection.
 * Created by scheibcn on 3/2/16.
 */
public class QuotationPerceptrons implements Serializable {

    private static final long serialVersionUID = 57993770070357620L;

    public Perceptron beginPerceptron;
    public Perceptron endPerceptron;
    public Perceptron cuePerceptron;

    public HigherSpanModel associatedSpanModel;

    Random shufRandom = new Random(123121);

    public QuotationPerceptrons(Perceptron beginPerceptron, Perceptron endPerceptron, Perceptron cuePerceptron) {
        this.beginPerceptron = beginPerceptron;
        this.endPerceptron = endPerceptron;
        this.cuePerceptron = cuePerceptron;
    }

    /**
     * Extracts cue features for all tokens in all documents.
     * This has to happen in a separate step as we first have to predict cues using other features which we then re-use.
     * @param trainDocs
     * @param testDocs
     * @param valDocs
     * @param resDocs
     */
    public static void extractFeaturesAboutCue(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs) {
        if (trainDocs != null) BoundaryFeatures.additionalBoundaryFeaturesFromCue(trainDocs);
        if (testDocs != null) BoundaryFeatures.additionalBoundaryFeaturesFromCue(testDocs);
        if (valDocs != null) BoundaryFeatures.additionalBoundaryFeaturesFromCue(valDocs);
        if (resDocs != null) BoundaryFeatures.additionalBoundaryFeaturesFromCue(resDocs);
    }

    /**
     * Performs one iteration of cue perceptron training for the given document and stores the scores (after the update)
     * in the respective instances.
     * @param document
     * @param isTraining
     */
    public void scoreAndUpdateCuePerceptron(Document document, boolean isTraining) {
        // shuffle tokens
        List<Token> tokens = new ArrayList<Token>(document.getTokenList());
        Collections.shuffle(tokens, shufRandom);

        // perform training step
        if (isTraining) {
            for (Token t : tokens) {
                cuePerceptron.train(t.boundaryFeatureSet, t.isGoldCue(), 0.1);
            }
        }

        // compute new score
        for (Token t : tokens)
            t.perceptronCueScore = cuePerceptron.score(t.boundaryFeatureSet, true);
    }


    /**
     * Performs one iteration of begin and end perceptron training for the given document and stores the scores
     * (after the update) in the respective instances.
     * @param document
     * @param isTraining
     */
    public void scoreAndUpdatePerceptron(Document document, boolean isTraining) {
        // shuffle tokens
        List<Token> tokens = new ArrayList<Token>(document.getTokenList());
        Collections.shuffle(tokens, shufRandom);

        // perform training step
        if (isTraining) {
            for (Token t : tokens) {
                boolean isStart = t.startsGoldContentSpan();
                boolean isEnd = t.endsGoldContentSpan();

                beginPerceptron.train(t.boundaryFeatureSet, isStart, 0.1);
                endPerceptron.train(t.boundaryFeatureSet, isEnd, 0.1);
            }
        }

        // compute new score
        for (Token t : tokens) {
            t.perceptronBeginScore = beginPerceptron.score(t.boundaryFeatureSet, true);
            t.perceptronEndScore = endPerceptron.score(t.boundaryFeatureSet, true);
        }
    }


    /**
     * Train cue perceptron for nEpochs
     * @param trainDocuments
     * @param nEpochs
     */
    public void trainCuePerceptron(List<Document> trainDocuments, int nEpochs) {
        System.out.println("Training cue perceptron ");

        // shuffle documents before training
        List<Document> shuffledDocuments = new ArrayList<>(trainDocuments);
        Collections.shuffle(shuffledDocuments, new Random(123));

        // train for n epochs
        for (int i = 0; i < nEpochs; i++) {
            System.out.print(i+1 + " ");
            for (Document document : shuffledDocuments) {
                scoreAndUpdateCuePerceptron(document, true);
            }
        }
        System.out.println();

        // print weights
        try {
            cuePerceptron.printWeights(NewStaticPrinter.fileName + ".weightsCue" + nEpochs);
        } catch (FileNotFoundException e) {
            System.out.println("Could not print perceptron weights");
        }
    }

    /**
     * Interval data structure for storing cross-validation offsets
     */
    public static class Interval {
        public int begin; public int end;
        public Interval(int begin, int end) {this.begin = begin; this.end = end;}
    }

    /**
     * Compute n-fold cross-validation offsets for given data set size and fold count
     * @param nDocs size of data set
     * @param nFolds number of folds
     * @return
     */
    public static List<Interval> getCVTestOffsets(int nDocs, int nFolds) {
        List<Interval> testOffsets = new ArrayList<>(nFolds);

        int foldSize = nDocs/nFolds;

        for (int i = 0; i < nFolds; i++) {
            testOffsets.add(new Interval(i * foldSize, (i+1) * foldSize));
        }
        return testOffsets;
    }

    /**
     * Get training data for given test fold
     * @param documents entire data set
     * @param interval test fold
     * @return
     */
    public static List<Document> getTrainData(List<Document> documents, Interval interval) {
        List<Document> trainList = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            // get all documents outside the interval
            if (i < interval.begin || i > interval.end) trainList.add(documents.get(i));
        }
        return trainList;
    }

    /**
     * Get test data for given test fold
     * @param documents entire data set
     * @param interval test fold
     * @return
     */
    public static List<Document> getTestData(List<Document> documents, Interval interval) {
        List<Document> testList = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            // get all documents inside the interval
            if (i >= interval.begin && i <= interval.end) testList.add(documents.get(i));
        }
        return testList;
    }


    /**
     * Train a cue model by jackknifing. This is to prevent over-performance of the cue model on the training data.
     * We accomplish this by doing n-fold cross validation.
     * @param trainDocuments
     * @param nFolds
     * @param nEpochs
     * @param marginCue
     */
    public void jackknifeCue(List<Document> trainDocuments, int nFolds, int nEpochs, double marginCue) {
        // generate folds
        List<Interval> testOffsets = getCVTestOffsets(trainDocuments.size(), nFolds);
        System.out.println("Jackknifing training cue ");

        // for each fold, train a classifier and apply
        for (int fold = 0; fold < nFolds; fold++) {
            System.out.print(fold + ": ");

            // split data
            Interval testInterval = testOffsets.get(fold);
            List<Document> trainDocsFold = getTrainData(trainDocuments, testInterval);
            List<Document> testDocsFold = getTestData(trainDocuments, testInterval);

            // train model
            cuePerceptron = new Perceptron();
            cuePerceptron.marginPositive = marginCue;
            trainCuePerceptron(trainDocsFold, nEpochs);

            // apply model and evaluate
            predictCues(testDocsFold);
            cueScoreToLabel(testDocsFold);

            EvaluateClassifier.evaluateAndPrint(testDocsFold, null, null, null, " JK" + fold);
        }

        EvaluateClassifier.evaluateAndPrint(trainDocuments, null, null, null, "OVERALL");
    }

    /**
     * Trains the begin and end perceptron for n epochs
     * @param trainDocuments
     * @param nEpochs number of training epochs
     */
    public void trainBeginEndPerceptrons(List<Document> trainDocuments, int nEpochs) {
        System.out.println("Training B/E perceptrons ");

        // shuffle data
        List<Document> shuffledDocuments = new ArrayList<>(trainDocuments);
        Collections.shuffle(shuffledDocuments, new Random(123));

        // train perceptrons for n epochs
        for (int i = 0; i < nEpochs; i++) {
            System.out.print(i+1 + " ");
            for (Document document : shuffledDocuments) {
                scoreAndUpdatePerceptron(document, true);
            }
        }
        System.out.println();

        // print weights
        try {
            beginPerceptron.printWeights(NewStaticPrinter.fileName + ".weightsBegin" + nEpochs);
            endPerceptron.printWeights(NewStaticPrinter.fileName + ".weightsEnd" + nEpochs);
        } catch (FileNotFoundException e) {
            System.out.println("Could not print perceptron weights");
        }

    }

    /**
     * Predict cues for all documents (only scores are set here)
     * @param documents
     */
    public void predictCues(List<Document> documents) {
        for (Document document : documents) {
            scoreAndUpdateCuePerceptron(document, false);
        }
    }

    /**
     * Predict begin and end for all documents (only scores are set here)
     * @param documents
     */
    public void predictBeginEnd(List<Document> documents) {
        for (Document document : documents) {
            scoreAndUpdatePerceptron(document, false);
        }
    }

    /**
     * Make hard cue labels from scores
     * @param documents
     */
    public void cueScoreToLabel(List<Document> documents) {
        for (Document document: documents) {
            for (Token token : document.tokenList) {
                if (token.perceptronCueScore > 0) {
                    token.isPredictedCue = true;
                } else {
                    token.isPredictedCue = false;
                }
            }
        }
    }

    /**
     * Pipeline for predicting cues over all specified datasets
     * @param trainList
     * @param testList
     * @param devList
     * @param resList
     */
    public void predictionPipelineCue(List<Document> trainList, List<Document> testList, List<Document> devList, List<Document> resList) {
        // predict scores
        if (trainList != null) this.predictCues(trainList);
        if (testList != null) this.predictCues(testList);
        if (devList != null) this.predictCues(devList);
        if (resList != null) this.predictCues(resList);

        // make actual labels
        if (trainList != null) this.cueScoreToLabel(trainList);
        if (testList != null) this.cueScoreToLabel(testList);
        if (devList != null) this.cueScoreToLabel(devList);
        if (resList != null) this.cueScoreToLabel(resList);
    }


    /**
     * Pipeline for predicting begin and end over all specified datasets
     * @param trainList
     * @param testList
     * @param devList
     * @param resList
     */
    public void predictionPipelineBoundary(List<Document> trainList, List<Document> testList, List<Document> devList, List<Document> resList) {
        // extract cue features
        QuotationPerceptrons.extractFeaturesAboutCue(trainList, testList, devList, resList);

        // predict
        if (trainList != null) this.predictBeginEnd(trainList);
        if (testList != null) this.predictBeginEnd(testList);
        if (devList != null) this.predictBeginEnd(devList);
        if (resList != null) this.predictBeginEnd(resList);
    }

}
