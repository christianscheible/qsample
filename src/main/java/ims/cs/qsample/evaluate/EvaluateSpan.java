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


package ims.cs.qsample.evaluate;

import ims.cs.lingdata.Document;
import ims.cs.parc.PARCAttribution;

import java.util.List;

/**
 * Evaluation functions for span prediction models.
 * Created by scheibcn on 3/2/16.
 */
public class EvaluateSpan {

    /**
     * Container class for all necessary F1 statistics to do Pareti-style quotation evaluation
     */
    public static class SpanResults {
        public F1.Stats strictCue;
        public F1.Stats strictContent;
        public F1.Stats partialContent;
        public F1.Stats strictContentDirect;
        public F1.Stats partialContentDirect;
        public F1.Stats strictContentIndirect;
        public F1.Stats partialContentIndirect;
        public F1.Stats strictContentMixed;
        public F1.Stats partialContentMixed;

        public String toString(String sep) {
            return strictContent.toString() + sep
                    + strictContentDirect + sep
                    + strictContentIndirect + sep
                    + strictContentMixed + sep
                    + strictCue + sep + sep
                    + partialContent + sep
                    + partialContentDirect + sep
                    + partialContentIndirect + sep
                    + partialContentMixed;
        }
    }

    /**
     * SpanResults for training, test, validation, and resubstitution data
     */
    public static class ResultSet {
        public SpanResults trainResults;
        public SpanResults testResults;
        public SpanResults valResults;
        public SpanResults resResults;
    }


    /**
     * Evaluate cue and content span models
     * @param documentList
     * @return
     */
    public static SpanResults cueContentEvaluation (List<Document> documentList) {
        SpanResults evaluation = new SpanResults();
        evaluation.strictCue = F1.evalSpans(documentList, "cue", false, null);
        evaluation.strictContent = F1.evalSpans(documentList, "content", false, null);
        evaluation.partialContent = F1.evalSpans(documentList, "content", true, null);
        evaluation.strictContentDirect = F1.evalSpans(documentList, "content", false, PARCAttribution.Type.DIRECT);
        evaluation.partialContentDirect = F1.evalSpans(documentList, "content", true, PARCAttribution.Type.DIRECT);
        evaluation.strictContentIndirect = F1.evalSpans(documentList, "content", false, PARCAttribution.Type.INDIRECT);
        evaluation.partialContentIndirect = F1.evalSpans(documentList, "content", true, PARCAttribution.Type.INDIRECT);
        evaluation.strictContentMixed = F1.evalSpans(documentList, "content", false, PARCAttribution.Type.MIXED);
        evaluation.partialContentMixed = F1.evalSpans(documentList, "content", true, PARCAttribution.Type.MIXED);

        return evaluation;
    }

    /**
     * Returns a string where the input s is repeated n times
     * @param s
     * @param n
     * @return
     */
    private static String generateN(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }

        return sb.toString();
    }

    private static void printHeader(String sep, int offset) {
        System.out.println(generateN("-", offset) + "--------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(generateN(" ", offset) + "        exact                                                                              "+sep+""+sep+" partial");
        System.out.println(generateN(" ", offset) + "        ALL            "+sep+" DIRECT         "+sep+" INDIRECT       "+sep+" MIXED          "+sep+" cue            "+sep+""+sep+" ALL            "+sep+" DIRECT         "+sep+" INDIRECT       "+sep+" MIXED         ");
        System.out.println(generateN(" ", offset) + "        P    R    F    "+sep+" P    R    F    "+sep+" P    R    F    "+sep+" P    R    F    "+sep+" P    R    F    "+sep+""+sep+" P    R    F    "+sep+" P    R    F    "+sep+" P    R    F    "+sep+" P    R    F   ");

    }

    private static void printFooter(int offset) {
        System.out.println(generateN("-", offset) + "--------------------------------------------------------------------------------------------------------------------------------------------------------");
    }


    private static void printResults(String prefix, String sep, SpanResults trainingEval, SpanResults testEval, SpanResults valEval, SpanResults resEval) {
        printHeader(sep, prefix.length() + 1);

        if (trainingEval != null) System.out.println(prefix + " TRAIN  " + trainingEval.toString(sep));
        if (testEval != null) System.out.println(prefix + " TEST   " + testEval.toString(sep));
        if (valEval != null) System.out.println(prefix + " VAL    " + valEval.toString(sep));
        if (resEval != null) System.out.println(prefix + " RES    " + resEval.toString(sep));

        printFooter(prefix.length() + 1);
    }

    public static ResultSet evaluateAndPrint(String prefix, String sep, List<Document> trainingDocuments, List<Document> testDocuments, List<Document> valDocuments, List<Document> resDocuments) {
        ResultSet resultSet = new ResultSet();

        if (trainingDocuments != null) resultSet.trainResults = cueContentEvaluation(trainingDocuments);
        if (testDocuments != null) resultSet.testResults = cueContentEvaluation(testDocuments);
        if (valDocuments != null) resultSet.valResults = cueContentEvaluation(valDocuments);
        if (resDocuments != null) resultSet.resResults = cueContentEvaluation(resDocuments);

        printResults(prefix, sep, resultSet.trainResults, resultSet.testResults,
                resultSet.valResults, resultSet.resResults);

        return resultSet;
    }


}
