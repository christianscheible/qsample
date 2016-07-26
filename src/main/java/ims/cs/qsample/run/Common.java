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
import ims.cs.parc.ProcessedCorpus;
import ims.cs.qsample.features.SpanFeatures;
import ims.cs.qsample.models.QuotationPerceptrons;
import ims.cs.qsample.spans.Span;
import ims.cs.util.StaticConfig;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Some common functions
 * Created by scheibcn on 3/5/16.
 */
public abstract class Common {
    /**
     * Writes the predictions to a file in BIO format
     * @param trainDocs
     * @param testDocs
     * @param valDocs
     * @param resDocs
     */
    public static void writePredictionsToFile(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs) {
        // if in text mode, write empty line after sentence ends and write cues
        boolean writeNewLineAfterSentence = StaticConfig.cliMode == StaticConfig.CliMode.TEXT;
        boolean writeCues = StaticConfig.cliMode == StaticConfig.CliMode.TEXT;

        // try to write predictions
        try {
            if (trainDocs != null) ProcessedCorpus.savePredictionsToFile(trainDocs, "train-final", writeNewLineAfterSentence, writeCues);
            if (testDocs != null) ProcessedCorpus.savePredictionsToFile(testDocs, "test-final", writeNewLineAfterSentence, writeCues);
            if (valDocs != null) ProcessedCorpus.savePredictionsToFile(valDocs, "val-final", writeNewLineAfterSentence, writeCues);
            if (resDocs != null) ProcessedCorpus.savePredictionsToFile(resDocs, "res-final", writeNewLineAfterSentence, writeCues);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to write results to file");
        }


    }

    /**
     * Writes out all perceptron models
     * @param perceptrons
     * @param fileName
     * @throws IOException
     */
    public static void serializeModels(QuotationPerceptrons perceptrons, String fileName) throws IOException {
        System.out.println("Writing perceptron model to " + fileName);
        ObjectOutputStream outputStream = new ObjectOutputStream (new GZIPOutputStream(new FileOutputStream(fileName)));
        outputStream.writeObject(perceptrons);
    }

    /**
     * Reads all perceptron models from a file
     * @param fileName
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static QuotationPerceptrons deserializeModels(String fileName) throws IOException, ClassNotFoundException {
        System.out.println("Loading perceptron model from " + fileName);
        ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(fileName)));
        return (QuotationPerceptrons) inputStream.readObject();
    }

    /**
     * Adds features to gold spans
     * @param documents
     */
    public static void addFeaturesToGoldSpans(List<Document> documents) {
        for (Document document : documents) {
            for (Span goldSpan : document.goldSpanSet) {
                SpanFeatures.addAllSpanFeatures(goldSpan);
            }
        }
    }

}
