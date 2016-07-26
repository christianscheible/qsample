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
import ims.cs.qsample.models.CrfClassifier;
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
 * Run an experiment with a CRF model
 * Created by scheibcn on 3/3/16.
 */
public class RunCrf {


    /**
     * Run the full CRF training and testing pipeline
     * @param trainDocs training documents
     * @param testDocs test documents (may be null)
     * @param valDocs validation documents (may be null)
     * @param resDocs resubstitution documents (may be null)
     * @param beginMargin positive margin for begin perceptron
     * @param endMargin positive margin for end perceptron
     * @param cueMargin positive margin for cue perceptron
     * @param numIter number of epochs for training
     * @param perceptrons optionally: specify some pre-trained perceptrons
     * @param crfClassifier optionally: specify a pre-trained CRF
     * @return final CRF model
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static CrfClassifier runCrfPipeline(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs,
                                            double beginMargin, double endMargin, double cueMargin,
                                            int numIter, QuotationPerceptrons perceptrons, CrfClassifier crfClassifier) throws IOException, ClassNotFoundException {

        // train a cue model if necessary, then predict
        if (perceptrons == null) {
            PerceptronTrainer.trainAllPerceptronsAndApply(trainDocs, testDocs, valDocs, resDocs, beginMargin, endMargin, cueMargin, true, 10, 10);
        } else {
            perceptrons.predictionPipelineCue(trainDocs, testDocs, valDocs, resDocs);
            perceptrons.predictionPipelineBoundary(trainDocs, testDocs, valDocs, resDocs);
        }

        // train CRF
        if (crfClassifier == null) {
            crfClassifier = new CrfClassifier();
            crfClassifier.numIter = numIter;
            crfClassifier.train(trainDocs, testDocs, valDocs, resDocs);
        }

        // apply CRF
        System.out.println("Applying CRF to test data");
        crfClassifier.test(trainDocs, testDocs, valDocs, resDocs);

        // evaluate
        EvaluateSpan.evaluateAndPrint("", "|", trainDocs, testDocs, valDocs, resDocs);

        // save predictions
        Common.writePredictionsToFile(trainDocs, testDocs, valDocs, resDocs);

        // output feature weights
        // this takes a lot of time, so it's deactivated right now
        if (false) crfClassifier.print();

        return crfClassifier;
    }

    /**
     * This runs the full experimental pipeline w/ training and testing
     * @return
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static CrfClassifier fullExperiment() throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        ProcessedCorpus pc = new ProcessedCorpus(PARCCorpus.getInstance());
        List<Document> trainDocs = pc.getTrain();
        List<Document> testDocs = pc.getTest();
        List<Document> valDocs = pc.getDev();
        List<Document> resDocs = pc.getTrainSample(10);

        return runCrfPipeline(trainDocs, testDocs, valDocs, resDocs, StaticConfig.beginMargin, StaticConfig.endMargin, StaticConfig.cueMargin, 500, null, null);
    }

    /**
     * Running this program will train the CRF model as described in the paper
     * @param args
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static void main(String[] args) throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        String logFileName = NewStaticPrinter.getLogFileName(StaticConfig.outputDirectory + "/crf-");
        NewStaticPrinter.init(logFileName);
        MultiOutputStream.init(logFileName);

        CrfClassifier crf = fullExperiment();
        crf.saveCrf(logFileName + ".crfmodel");
    }
}
