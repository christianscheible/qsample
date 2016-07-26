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


package ims.cs.util;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.qsample.spans.Span;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A static printer to easily log output. Used mostly for debugging purposes.
 * Created by scheibcn on 3/3/16.
 */
public class NewStaticPrinter {
    // printer may be turned off
    public static boolean isOn = true;

    public static String fileRoot;
    public static String fileName;
    static PrintWriter writer;

    /**
     * Pass function to do nothing.
     */
    public static void pass() {}

    /**
     * Sets a log file name from the specified log file root
     * @param logFileName
     * @throws FileNotFoundException
     */
    public static void init(String logFileName) throws FileNotFoundException {
        fileRoot = logFileName;
        fileName = logFileName + ".debug";
        if (isOn) writer = new PrintWriter(fileName);
    }


    /**
     * Generates a log file name from the specified log file root
     * @param prefix
     * @return
     */
    public static String getLogFileName (String prefix) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        Date date = new Date();

        return prefix + dateFormat.format(date) + ".log";
    }

    /**
     * Print line to log file
     * @param s
     */
    public static void println(String s) {
        if (isOn) {
            writer.write(s);
            writer.write("\n");
        }
    }

    /**
     * Print to log file
     * @param s
     */
    public static void print(String s) {
        if (isOn) {
            writer.write(s);
        }
    }

    /**
     * Print n copies of s to the log file
     * @param s
     * @param n
     */
    public static void printN(String s, int n) {
        for (int i = 0; i < n; i++) print(s);
        println("");
    }


    /**
     * Print the perceptron predictions for the given document to the log file
     * @param document
     * @param prefix string to prepend for each line
     */
    public static void printPerceptronPrediction (Document document, String prefix) {
        for (Token token : document.getTokenList()) {
            StringBuilder line = new StringBuilder();

            // prefix
            line.append(prefix);
            line.append("\t");

            // add token information
            line.append(token.predText);
            line.append("\t");


            // gold information
            boolean goldBegin = token.startsGoldContentSpan();
            boolean goldEnd = token.endsGoldContentSpan();
            boolean goldCue = token.isGoldCue();

            if (goldBegin) line.append('B');
            else line.append('_');

            if (goldEnd) line.append('E');
            else line.append('_');

            if (goldCue) line.append('C');
            else line.append('_');

            line.append('\t');

            // predicted information
            if (token.perceptronBeginScore > 0) line.append('B');
            else line.append('_');

            if (token.perceptronEndScore > 0) line.append('E');
            else line.append('_');

            if (token.isPredictedCue) line.append('C');
            else line.append('_');

            line.append('\t');

            // scores
            line.append(token.perceptronBeginScore); line.append('\t');
            line.append(token.perceptronEndScore); line.append('\t');
            line.append(token.perceptronCueScore); line.append('\t');
            line.append('\t');

            // scores
            line.append(token.numTimesSampledBegin); line.append('\t');
            line.append(token.numTimesSampledEnd); line.append('\t');
            line.append(token.numTimesSampledCue); line.append('\t');


            println(line.toString());
        }
    }

    /**
     * Print document predictions and gold information using SGML-style tags
     * @param doc
     */
    public static void printAnnotatedDocument(Document doc) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < doc.tokenList.size(); i++) {
            if (Span.anyBeginsAt(doc.goldSpanSet, i)) sb.append("<GOLD>");
            if (Span.anyBeginsAt(doc.predictedSpanSet, i)) sb.append("<pred>");
            sb.append(doc.tokenList.get(i).predText);
            if (Span.anyEndsAt(doc.predictedSpanSet, i)) sb.append("</pred>");
            if (Span.anyEndsAt(doc.goldSpanSet, i)) sb.append("</GOLD>");
            sb.append(" ");
        }

        println(sb.toString());
    }

}
