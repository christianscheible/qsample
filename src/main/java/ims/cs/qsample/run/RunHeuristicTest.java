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
import ims.cs.qsample.models.QuotationPerceptrons;
import ims.cs.qsample.perceptron.PerceptronTrainer;
import ims.cs.util.MultiOutputStream;
import ims.cs.util.NewStaticPrinter;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * Run an experiment with the greedy heuristic model
 * Created by scheibcn on 11/5/15.
 */
public class RunHeuristicTest {

    // whether to shuffle tokens during prediction
    static boolean doShuffleTokens = false;
    static boolean incrementalPrediction = false;

    /**
     * Run the full greedy heuristic training and testing pipeline
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param beginMargin positive margin for begin perceptron
     * @param endMargin positive margin for end perceptron
     * @param cueMargin positive margin for cue perceptron
     * @param model optionally: specify some pre-trained perceptrons
     * @return final perceptron models
     */
    public static QuotationPerceptrons runHeuristicPipeline(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                            double beginMargin, double endMargin, double cueMargin, QuotationPerceptrons model) {

        // train model or predict
        if (model == null) {
            model = PerceptronTrainer.trainAllPerceptronsAndApply(trainDocs, testDocs, valDocs, resDocs, beginMargin, endMargin, cueMargin, false, 10, 10);
        } else {
            model.predictionPipelineCue(trainDocs, testDocs, valDocs, resDocs);
            model.predictionPipelineBoundary(trainDocs, testDocs, valDocs, resDocs);
        }


        // debug output
        for (Document document: testDocs) NewStaticPrinter.printPerceptronPrediction(document, "PP");
        NewStaticPrinter.printN("-", 80);

        // SAMPLING
        HeuristicSampler sampler = new HeuristicSampler();
        sampler.doShuffleTokens = doShuffleTokens;


        int[] maxDistances;
        int[] maxLengths;

        if (incrementalPrediction) {   /* version 1: incremental prediction -- performs slightly worse */
            maxDistances = new int[]{5, 10, 20, 30};
            maxLengths = new int[]{50, 50, 50, 50};
        } else {                       /* version 2: full prediction immediately */
            maxDistances = new int[]{30};
            maxLengths = new int[]{50};
        }

        for (int i = 0; i < maxDistances.length; i++) {
            // sample
            int maxDistance = maxDistances[i];
            int maxLength = maxLengths[i];

            if (trainDocs != null) sampler.sampleGreedy(trainDocs, maxDistance, maxLength);
            if (testDocs != null) sampler.sampleGreedy(testDocs, maxDistance, maxLength);
            if (valDocs != null) sampler.sampleGreedy(valDocs, maxDistance, maxLength);
            if (resDocs != null) sampler.sampleGreedy(resDocs, maxDistance, maxLength);

            // evaluate
            EvaluateSpan.evaluateAndPrint("" + maxDistance + " ", "|", trainDocs, testDocs, valDocs, resDocs);
        }

        // save predictions
        Common.writePredictionsToFile(trainDocs, testDocs, valDocs, resDocs);

        return model;
    }

    /**
     * This runs the full experimental pipeline w/ training and testing
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static void fullExperiment() throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        ProcessedCorpus pc = new ProcessedCorpus(PARCCorpus.getInstance());
        List<Document> trainDocs = pc.getTrain();
        List<Document> testDocs = pc.getTest();
        List<Document> valDocs = pc.getDev();
        List<Document> resDocs = pc.getTrainSample(10);

        runHeuristicPipeline(trainDocs, testDocs, valDocs, resDocs, StaticConfig.beginMargin, StaticConfig.endMargin, StaticConfig.cueMargin, null);
    }

    /**
     * Run this to train a model without going through QSample.main()
     * @param args
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static void main (String[] args) throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        String logFileName = NewStaticPrinter.getLogFileName("/home/users1/scheibcn/quotations/results/txt/joint-first-run/heuristic-");
        MultiOutputStream.init(logFileName);
        NewStaticPrinter.init(logFileName);

        fullExperiment();
    }
}
