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


package ims.cs.qsample.run;

import ims.cs.lingdata.Document;
import ims.cs.parc.PARCCorpus;
import ims.cs.parc.ProcessedCorpus;
import ims.cs.qsample.evaluate.EvaluateSpan;
import ims.cs.qsample.greedysample.HeuristicSampler;
import ims.cs.qsample.greedysample.PerceptronSampler;
import ims.cs.qsample.models.QuotationPerceptrons;
import ims.cs.qsample.models.HigherSpanModel;
import ims.cs.qsample.perceptron.PerceptronTrainer;
import ims.cs.util.MultiOutputStream;
import ims.cs.util.NewStaticPrinter;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.List;

/**
 * Run an experiment with the sampling model
 * Created by scheibcn on 3/5/16.
 */
public class RunPerceptronSampler {


    /**
     * Predict spans for the given documents
     * @param documents
     * @param perceptronSampler
     * @param heuristicSampler
     */
    public static void predict(List<Document> documents, PerceptronSampler perceptronSampler, HeuristicSampler heuristicSampler)  {
        for (Document document : documents) document.predictedSpanSet.clear();
        if (documents != null) heuristicSampler.sampleGreedy(documents, StaticConfig.maxCueDistanceHeuristic, StaticConfig.maxLengthHeuristic);
        perceptronSampler.sampleAndScoreBeginEnd(documents, false, StaticConfig.predictionIter);
    }

    /**
     * Run the full sampling training and testing pipeline
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param beginMargin positive margin for begin perceptron
     * @param endMargin positive margin for end perceptron
     * @param cueMargin positive margin for cue perceptron
     * @param quotationPerceptrons optionally: specify some pre-trained perceptrons
     * @return
     */
    public static QuotationPerceptrons runPsPipeline(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                                     double beginMargin, double endMargin, double cueMargin, QuotationPerceptrons quotationPerceptrons) {

        // train the model unless we were passed a trained one
        boolean doTraining = quotationPerceptrons == null;

        // set up models, pre-train, predict
        if (quotationPerceptrons == null) {
            if (StaticConfig.jackknifing) {
                quotationPerceptrons = PerceptronTrainer.trainAllPerceptronsAndApplyWithJackknifing(trainDocs, testDocs, valDocs, resDocs,
                        beginMargin, endMargin, cueMargin, false, 10, 10, 10);
            } else {
                quotationPerceptrons = PerceptronTrainer.trainAllPerceptronsAndApply(trainDocs, testDocs, valDocs, resDocs,
                        beginMargin, endMargin, cueMargin, false, 10, 10);
            }

            quotationPerceptrons.associatedSpanModel = new HigherSpanModel();;
        } else {
            quotationPerceptrons.predictionPipelineCue(trainDocs, testDocs, valDocs, resDocs);
            quotationPerceptrons.predictionPipelineBoundary(trainDocs, testDocs, valDocs, resDocs);
        }


        // INITIALIZE W/ HEURISTICS
        HeuristicSampler heuristicSampler = new HeuristicSampler();
        if (trainDocs != null) heuristicSampler.sampleGreedy(trainDocs, StaticConfig.maxCueDistanceHeuristic, StaticConfig.maxLengthHeuristic);
        if (testDocs != null) heuristicSampler.sampleGreedy(testDocs, StaticConfig.maxCueDistanceHeuristic, StaticConfig.maxLengthHeuristic);
        if (valDocs != null) heuristicSampler.sampleGreedy(valDocs, StaticConfig.maxCueDistanceHeuristic, StaticConfig.maxLengthHeuristic);
        if (resDocs != null) heuristicSampler.sampleGreedy(resDocs, StaticConfig.maxCueDistanceHeuristic, StaticConfig.maxLengthHeuristic);

        // evaluate
        EvaluateSpan.evaluateAndPrint("INIT", "|", trainDocs, testDocs, valDocs, resDocs);

        // SAMPLING
        PerceptronSampler perceptronSampler = new PerceptronSampler(quotationPerceptrons);

        // train if necessary
        if (doTraining) {
            if (trainDocs != null) Common.addFeaturesToGoldSpans(trainDocs);

            for (int i = 0; i < StaticConfig.outerIter; i++) {
                perceptronSampler.sampleAndScoreBeginEnd(trainDocs, true, StaticConfig.innerIter);

                // predict periodically
                if (i != 0 && i % StaticConfig.predictEvery == 0) {
                    if (testDocs != null) predict(testDocs, perceptronSampler, heuristicSampler);
                    if (valDocs != null) predict(valDocs, perceptronSampler, heuristicSampler);
                    if (resDocs != null) predict(resDocs, perceptronSampler, heuristicSampler);
                }

                // evaluate
                EvaluateSpan.evaluateAndPrint("" + i + " ", "|", trainDocs, testDocs, valDocs, resDocs);
            }
        }

        // predict on test
        System.out.println("Predicting");
        if (!doTraining && trainDocs != null) predict(trainDocs, perceptronSampler, heuristicSampler);
        if (testDocs != null) predict(testDocs, perceptronSampler, heuristicSampler);
        if (valDocs != null) predict(valDocs, perceptronSampler, heuristicSampler);
        if (resDocs != null) predict(resDocs, perceptronSampler, heuristicSampler);
        EvaluateSpan.evaluateAndPrint("FINAL" + " ", "|", trainDocs, testDocs, valDocs, resDocs);

        // save predictions
        Common.writePredictionsToFile(trainDocs, testDocs, valDocs, resDocs);

        // output peceptron predictions once more
        for (Document document: testDocs) NewStaticPrinter.printPerceptronPrediction(document, "PP");
        NewStaticPrinter.printN("-", 80);

        if (doTraining) {
            // save feature weights
            System.out.println("Printing perceptron features");
            try {
                quotationPerceptrons.associatedSpanModel.printWeights(NewStaticPrinter.fileName + ".weightsSpan" + StaticConfig.outerIter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("Failed to print perceptron features");
            }
        }

        return quotationPerceptrons;
    }


    /**
     * The full experiment from the paper to train and test a model
     * @return
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static QuotationPerceptrons fullExperiment() throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        ProcessedCorpus pc = new ProcessedCorpus(PARCCorpus.getInstance());
        List<Document> trainDocs = pc.getTrain();
        List<Document> testDocs = pc.getTest();
        List<Document> valDocs = pc.getDev();
        List<Document> resDocs = pc.getTrainSample(10);

        NewStaticPrinter.isOn = true;

        return runPsPipeline(trainDocs, testDocs, valDocs, resDocs,
                StaticConfig.beginMargin, StaticConfig.endMargin, StaticConfig.cueMargin, null);
    }

    /**
     * Run this to train a model without going through QSample.main()
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, SAXException, ParserConfigurationException {
        String logFileName = NewStaticPrinter.getLogFileName(Common.pathConcat(StaticConfig.outputDirectory, "perceptronsampler-"));
        MultiOutputStream.init(logFileName);
        NewStaticPrinter.init(logFileName);

        QuotationPerceptrons perceptrons = fullExperiment();

        // serialization
        Common.serializeModels(perceptrons, logFileName + ".models");
    }
}
