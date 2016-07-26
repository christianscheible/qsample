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

import java.util.List;

/**
 * Evaluation functions for single-token classifiers
 * Created by scheibcn on 3/2/16.
 */
public class EvaluateClassifier {

    /**
     * Container class for quotation classifier results, i.e., begin, end, and cue F1
     */
    public static class ClassifierResults {
        F1.Stats beginStats;
        F1.Stats endStats;
        F1.Stats cueStats;

        public String toString() {
            return String.format("Pb=%1.3f Rb=%1.3f Fb=%1.3f     Pe=%1.3f Re=%1.3f Fe=%1.3f     Pc=%1.3f Rc=%1.3f Fc=%1.3f",
                    beginStats.precision, beginStats.recall, beginStats.f1,
                    endStats.precision, endStats.recall, endStats.f1,
                    cueStats.precision, cueStats.recall, cueStats.f1);

        }
    }

    /**
     * Evaluate begin, end, and cue classifier output over all tokens in the specified documents
     * @param trainDocs
     * @return
     */
    public static ClassifierResults evaluateClassifier (List<Document> trainDocs) {
        if (trainDocs == null) return null;
        ClassifierResults results = new ClassifierResults();

        results.beginStats = F1.evalPerceptron(trainDocs, "begin");
        results.endStats = F1.evalPerceptron(trainDocs, "end");
        results.cueStats = F1.evalPerceptron(trainDocs, "cue");

        return results;
    }


    /**
     * Print begin, end, and cue classifier evaluations over all tokens in the specified training, test, val, and
     * resubstitution documents
     * @param trainDocs
     * @param testDocs
     * @param valDocs
     * @param resDocs
     * @param prefix
     */
    public static void evaluateAndPrint(List<Document> trainDocs, List<Document> testDocs, List<Document> valDocs, List<Document> resDocs, String prefix) {
        ClassifierResults trainResults = evaluateClassifier(trainDocs);
        ClassifierResults testResults = evaluateClassifier(testDocs);
        ClassifierResults valResults = evaluateClassifier(valDocs);
        ClassifierResults resResults = evaluateClassifier(resDocs);

        if (trainResults != null) System.out.println(prefix + " TRAIN   " + trainResults.toString());
        if (testResults != null) System.out.println(prefix + " TEST    " + testResults.toString());
        if (valResults != null) System.out.println(prefix + " VAL     " + valResults.toString());
        if (resResults != null) System.out.println(prefix + " RES     " + resResults.toString());
    }

}
