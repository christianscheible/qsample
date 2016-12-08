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


package ims.cs.parc;

import ims.cs.lingdata.Corpus;
import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.corenlp.PARCCoreNlpPipeline;
import ims.cs.qsample.features.FeatureExtraction;

import ims.cs.util.NewStaticPrinter;
import ims.cs.qsample.spans.Span;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A corpus with a built-in pre-processing pipeline
 * Created by scheibcn on 11/5/15.
 */
public class ProcessedCorpus {

    public Corpus corpus;
    SpanLabelExtractor labelExtractor = new SpanLabelExtractor();
    FeatureExtraction featureExtractor;

    public ProcessedCorpus(Corpus corpus) throws IOException, SAXException, ParserConfigurationException, ClassNotFoundException {
        this.corpus = corpus;
        featureExtractor = new FeatureExtraction();
    }

    /**
     * Takes a document list and runs the following steps:<br/>
     *   - CoreNLP pipeline<br/>
     *   - feature extraction<br/>
     *   - label extraction<br/>
     * @param originalDocs list of documents to be processed
     * @return new list of documents after processing
     */
    public List<Document> transformDocumentList(List<Document> originalDocs) {
        PARCCoreNlpPipeline coreNlpPipeline = new PARCCoreNlpPipeline(originalDocs);
        List<Document> processedDocuments = new ArrayList<Document>(originalDocs.size());

        Iterator<Document> docIter = coreNlpPipeline.iterator();

        while (docIter.hasNext()) {
            Document doc = docIter.next();
            processedDocuments.add(doc);
            featureExtractor.extractAllFeatures(doc);
            labelExtractor.label(doc);
            bioToSpan(doc, null, true);
        }

        return processedDocuments;
    }


    public List<Document> getTrain() {
        return transformDocumentList(corpus.getTrain().docList);
    }

    public List<Document> getDev() {
        return transformDocumentList(corpus.getDev().docList);
    }

    public List<Document> getTest() {
        return transformDocumentList(corpus.getTest().docList);
    }

    /**
     * Returns a sample of training documents (first n)
     * @param size
     * @return
     */
    public List<Document> getTrainSample(int size) {
        return transformDocumentList(corpus.getTrain().docList.subList(0, size));
    }

    /**
     * Returns a sample of test documents (first n)
     * @param size
     * @return
     */
    public List<Document> getTestSample(int size) {
        return transformDocumentList(corpus.getTest().docList.subList(0, size));
    }

    /**
     * Returns a sample of dev documents (first n)
     * @param size
     * @return
     */
    public List<Document> getDevSample(int size) {
        return transformDocumentList(corpus.getDev().docList.subList(0, size));
    }


    /**
     * Stores document predictions in a file. Format is one word per line, BIOE annotations.
     * @param documents
     * @param experimentId
     * @param newLineAtSentenceEnd
     * @param writeCues
     * @throws IOException
     */
    public static void savePredictionsToFile(List<Document> documents, String experimentId, boolean newLineAtSentenceEnd, boolean writeCues) throws IOException {
        String fileName = NewStaticPrinter.fileRoot + ".predictions-" + experimentId + ".txt.gz";

        // initialize writer if necessary
        PrintWriter writer = null;
        if (!StaticConfig.oneFilePerInput)
            writer = new PrintWriter(new GZIPOutputStream(new FileOutputStream(fileName)));


        for (Document document : documents) {
            boolean inSpan = false;

            // set up a new writer if requested
            if (StaticConfig.oneFilePerInput) {
                if (writer != null) writer.close();
                writer = new PrintWriter(new GZIPOutputStream(new FileOutputStream(new File(StaticConfig.outputDirectory, document.docId.toString() + ".quotations.gz"))));
            }

            for (Token token : document.tokenList) {
                String bioLabelPred = "O";
                boolean spanStarts = token.startsPredictedContentSpan();
                boolean spanEnds = token.endsPredictedContentSpan();

                if (spanStarts) {
                    inSpan = true;
                    bioLabelPred = "B";
                } else if (spanEnds) {
                    inSpan = false;
                    bioLabelPred = "E";
                } else if (inSpan) {
                    bioLabelPred = "I";
                } else if (writeCues && token.isPredictedCue) {
                    bioLabelPred = "C";
                }

                String text = token.originalPredText != null ? token.originalPredText : token.predText;

                writer.println(text + "\t" + token.predByteCount.begin + "\t" + token.predByteCount.end + "\t"
                        + token.contentBIOAnnotationGold + "\t" + bioLabelPred);
                if (newLineAtSentenceEnd && token.endsSentence()) writer.println();
            }
        }
        writer.close();

        if (StaticConfig.oneFilePerInput) {
            System.out.println("Wrote predictions to " + StaticConfig.outputDirectory);
        } else {
            System.out.println("Wrote predictions to " + fileName);
        }
    }

    /**
     * Restores predictions from a file.
     * @param documents
     * @param fileName
     * @throws IOException
     */
    public static void loadPredictionsFromFile(List<Document> documents, String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));

        for (Document document : documents) {
            for (Token token : document.tokenList) {
                String line = reader.readLine();
                String[] tokens = line.split("\\t");
                if (token.contentBIOAnnotationPred == null) token.contentBIOAnnotationPred = new HashMap<>();
                token.contentBIOAnnotationPred.put(fileName, tokens[2]);
            }
        }
    }

    /**
     * Converts BIO labels to span annotations
     * @param document
     * @param slot
     * @param isGold
     */
    public static void bioToSpan(Document document, String slot, boolean isGold) {
        String prevLabel = "O";
        int currentBegin = -1;

        // select span set
        Set<Span> spanSet;

        if (isGold) spanSet = document.goldSpanSet;
        else spanSet = document.predictedSpanSet;

        spanSet.clear();

        // copy annotations
        for (int i = 0; i < document.tokenList.size(); i++) {
            Token token = document.tokenList.get(i);
            String label;

            // select label
            if (isGold) label = token.contentBIOAnnotationGold;
            else label = token.contentBIOAnnotationPred.get(slot);


            if ((label.equals("O") || label.equals("B")) && !prevLabel.equals("O")) {
                Span span = new Span(document, currentBegin, i - 1, "content");
                if (span.length() > 1) spanSet.add(span);
                currentBegin = -1;
            }
            if (label.equals("B") || (label.equals("I") && prevLabel.equals("O"))) {
                currentBegin = i;
            }
            prevLabel = label;
        }

        if (currentBegin != -1) {
            Span span = new Span(document, currentBegin, document.tokenList.size() - 1, "content");
            if (span.length() > 1) spanSet.add(span);
        }

    }


    public static void main (String[] args) throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        ProcessedCorpus pc = new ProcessedCorpus(PARCCorpus.getInstance());
        List<Document> trainDocs = pc.getTrainSample(3);
    }

}
