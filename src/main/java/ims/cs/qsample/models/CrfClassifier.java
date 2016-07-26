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

import cc.mallet.fst.*;
import cc.mallet.optimize.Optimizable;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.types.TokenSequence;
import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.mallet.PARCDocumentInstance;
import ims.cs.mallet.DocumentFeatureSet2TokenSequence;
import ims.cs.parc.ProcessedCorpus;
import ims.cs.util.NewStaticPrinter;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Wrapper to Mallet CRF for processing our data structures
 * Created by scheibcn on 3/3/16.
 */
public class CrfClassifier {

    CRF crf;
    Pipe pipe;
    TransducerTrainer trainer;

    // training parameters
    enum TrainMode {INCREASING, STANDARD, THREADED, THREADED2};
    public TrainMode trainMode = TrainMode.THREADED2;

    public int numIter;
    public int l2reg = 10;


    /**
     * Converts a list of documents to a Mallet InstanceList
     * @param documents
     * @return
     */
    public InstanceList documentsToInstances(List<Document> documents) {
        InstanceList instanceList = new InstanceList(pipe, documents.size());
        for (Document document : documents) {
            // make a new instance
            PARCDocumentInstance inst = new PARCDocumentInstance(document);
            List<Token> tokenList = document.tokenList;

            // copy labels into mallet instance
            List<Object> labelList = new ArrayList<Object>(tokenList.size());

            for (Token token : tokenList) {
                String label = token.contentBIOAnnotationGold;
                labelList.add(label);
            }

            inst.setTarget(new TokenSequence(labelList.toArray(new Object[labelList.size()])));

            // add instance to instance list
            instanceList.addThruPipe(inst);
        }

        return instanceList;
    }

    /**
     * Creates the default Mallet pipeline for processing documents
     * @return
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Pipe newDocumentPipe() throws FileNotFoundException, ClassNotFoundException, IOException {
        List<Pipe> pipeList = new ArrayList<Pipe>(10);

        // Convert our own feature sets to mallet feature "sets"
        pipeList.add(new DocumentFeatureSet2TokenSequence());

        // Convert mallet feature "sets" to feature vectors
        pipeList.add(new TokenSequence2FeatureVectorSequence(true, true));

        // Convert label strings to mallet internal format
        pipeList.add(new Target2LabelSequence());

        return new SerialPipes(pipeList);
    }

    /**
     * Trains a new CRF using the trainDocuments. The other documents are currently not used but may be necessary for
     * monitoring purposes in the future.
     * @param trainDocuments
     * @param testDocuments
     * @param valDocuments
     * @param resDocuments
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void train (List<Document> trainDocuments, List<Document> testDocuments, List<Document> valDocuments, List<Document> resDocuments) throws IOException, ClassNotFoundException {
        pipe = newDocumentPipe();

        // convert documents to instances
        InstanceList trainInstanceList = documentsToInstances(trainDocuments);
        InstanceList valInstanceList = null;

        // set up CRF infrastructure
        crf = new CRF(pipe, null);
        crf.addFullyConnectedStatesForLabels();
        crf.addStartState();
        crf.setWeightsDimensionAsIn(trainInstanceList, false);


        // select machine
        int nThreads;

        // some IMS-specific heuristics for choosing the number of threads
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            if (hostName.contains("buntfalke")) {
                nThreads = 20;
            } else if (hostName.contains("flamingo")) {
                nThreads = 20;
            } else if (hostName.contains("falke")) {
                nThreads = 24;
            } else if (hostName.contains("habicht")) {
                nThreads = 12;
            } else {
                nThreads = 1;
            }
        } catch (UnknownHostException e) {
            System.out.println("Could not get host name, falling back to 1 threads");
            nThreads = 1;
        }

        System.out.println("Using " + nThreads + " threads for training");

        System.out.println("Training mode: " + trainMode.name());

        if (trainMode == TrainMode.STANDARD || trainMode == TrainMode.INCREASING) { // non-parallelized training
            CRFTrainerByLabelLikelihood trainerNt = new CRFTrainerByLabelLikelihood(crf);
            trainerNt.setUseSomeUnsupportedTrick(false); // don't do non-deterministic stuff!
            trainerNt.setGaussianPriorVariance(l2reg);   //orig: 10
            trainer = trainerNt;

            if (trainMode == TrainMode.STANDARD) {
                ((CRFTrainerByLabelLikelihood)trainer).train(trainInstanceList,  numIter);
            } else if (trainMode == TrainMode.INCREASING) {
                // train model incrementally with samples of the data of increasing size
                ((CRFTrainerByLabelLikelihood)trainer).train(trainInstanceList,  numIter, new double[] {0.01, 0.1, 0.2, 0.5, 1.0});
            }

        } else if (trainMode == TrainMode.THREADED) {   // parallel training, old version
            // http://mallet.cs.umass.edu/fst.php
            CRFOptimizableByBatchLabelLikelihood batchOptLabel =
                    new CRFOptimizableByBatchLabelLikelihood(crf, trainInstanceList, nThreads);

            ThreadedOptimizable optLabel = new ThreadedOptimizable(
                    batchOptLabel, trainInstanceList, crf.getParameters().getNumFactors(),
                    new CRFCacheStaleIndicator(crf));

            Optimizable.ByGradientValue[] opts =
                    new Optimizable.ByGradientValue[]{optLabel};

            // by default, use L-BFGS as the optimizer
            CRFTrainerByValueGradients crfTrainer =
                    new CRFTrainerByValueGradients(crf, opts);
            trainer = crfTrainer;
            crfTrainer.setMaxResets(0);

            // now train the model
            crfTrainer.train(trainInstanceList, Integer.MAX_VALUE);

            // turn off the optimizer, otherwise the program will not terminate in the end
            optLabel.shutdown();

        } else if (trainMode == TrainMode.THREADED2) {   // parallel training, new version
            CRFTrainerByThreadedLabelLikelihood trainerT = new CRFTrainerByThreadedLabelLikelihood(crf, nThreads);
            trainerT.setGaussianPriorVariance(l2reg); //orig: 10
            trainerT.setUseSomeUnsupportedTrick(false); // don't do non-deterministic stuff!
            trainer = trainerT;
            trainerT.train(trainInstanceList, numIter);
            trainerT.shutdown();
        } else {
            throw new Error("Invalid train mode. This shouldn't happen.");
        }
    }

    /**
     * Loads a previously serialized CRF from file
     * @param serializedFile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void loadCrf(String serializedFile) throws IOException, ClassNotFoundException {
        System.out.println("Reading CRF from file " + serializedFile);
        ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(serializedFile)));
        crf = (CRF) ois.readObject();
        pipe = crf.getInputPipe();
        ois.close();
    }


    /**
     * Serializes the CRF model to a file
     * @param fileName
     * @throws IOException
     */
    public void saveCrf(String fileName) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(fileName)));
        oos.writeObject(crf);
        oos.close();
    }


    /**
     * Predict spans for a given document
     * @param documents
     */
    public void predictSpans(List<Document> documents) {
        InstanceList instances = documentsToInstances(documents);
        Transducer model = crf;

        Iterator<Instance> instanceIterator = instances.iterator();

        for (Document document : documents) {
            Instance instance = instanceIterator.next();
            Sequence data = (Sequence) instance.getData();
            Sequence predOutput = model.transduce(data);

            // transfer labels to token
            for (int i = 0; i < document.tokenList.size(); i++) {
                String label = predOutput.get(i).toString();
                Token token = document.tokenList.get(i);

                if (token.contentBIOAnnotationPred == null) token.contentBIOAnnotationPred = new HashMap<>();
                token.contentBIOAnnotationPred.put("CRF", label);
            }

            // convert to spans
            ProcessedCorpus.bioToSpan(document, "CRF", false);
        }

    }

    /**
     * Predict spans for all sets of documents
     * @param trainDocuments
     * @param testDocuments
     * @param valDocuments
     * @param resDocuments
     */
    public void test(List<Document> trainDocuments, List<Document> testDocuments, List<Document> valDocuments, List<Document> resDocuments) {
        if (trainDocuments != null) predictSpans(trainDocuments);
        if (testDocuments != null) predictSpans(testDocuments);
        if (valDocuments != null) predictSpans(valDocuments);
        if (resDocuments != null) predictSpans(resDocuments);
    }

    /**
     * Prints the feature weights to a file specified by our logger
     */
    public void print() {
        System.out.println("Printing feature weights");

        try {
            PrintWriter writer = new PrintWriter(new GZIPOutputStream(new FileOutputStream(NewStaticPrinter.fileName + ".crf-weights.txt.gz")));
            crf.print(writer);
            writer.flush();
            writer.close();

            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to print feature weights");
        }

    }


}
