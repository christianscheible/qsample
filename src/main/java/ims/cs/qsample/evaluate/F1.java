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
import ims.cs.lingdata.Token;
import ims.cs.parc.PARCAttribution;
import ims.cs.qsample.spans.Span;

import java.util.*;

/**
 * F1-measure for classifier evaluation
 * Created by scheibcn on 3/2/16.
 */
public class F1 {

    /**
     * Container for F1 statistics
     */
    public static class Stats {
        public List<Stats> localStats;

        public int trueCount;
        public int predictedCount;
        // Store two double correct counts for precision and recall. These are necessary for partial match.
        public double correctCountP;
        public double correctCountR;

        public double precision;
        public double recall;
        public double f1;

        /**
         * Add in all statistics from o
         * @param o
         */
        public void accumulate(Stats o) {
            trueCount += o.trueCount;
            predictedCount += o.predictedCount;
            correctCountP += o.correctCountP;
            correctCountR += o.correctCountR;
        }

        /**
         * Compute precision, recall, and F1 from the currently stored statistics
         */
        public void computePRF() {
            if (predictedCount == 0) {
                precision = 1;
            } else {
                precision = correctCountP/(double) predictedCount;
            }

            if (trueCount == 0) {
                recall = 1;
            } else {
                recall = correctCountR/(double) trueCount;
            }

            if (recall + precision == 0) {
                f1 = 0;
            } else {
                f1 = 2.0 * precision * recall / (precision + recall);
            }
        }

        @Override
        public String toString() {
            return toString(2);
        }

        public String toString(int sigDigits) {
            return String.format(" %1."+ sigDigits + "f %1." + sigDigits + "f %1." + sigDigits + "f ", precision, recall, f1);
        }
    }


    /**
     * Evaluate F1 for all spans in the documentList
     * @param documentList documents for evaluation
     * @param label label to be evaluated
     * @param partial give credit for partial match?
     * @param typeRestriction type to be evaluated on (pass null for all types)
     * @return
     */
    public static Stats evalSpans(List<Document> documentList, String label, boolean partial, PARCAttribution.Type typeRestriction) {
        Stats stats = new Stats();
        stats.localStats = new ArrayList<>(documentList.size());

        for (Document document: documentList) {
            Stats localStats = evalSpans(document, label, partial, typeRestriction);
            stats.accumulate(localStats);
            stats.localStats.add(localStats);
        }

        stats.computePRF();

        return stats;
    }


    /**
     * Calculates F1 statistics for a single document
     * @param document
     * @param label label to be evaluated
     * @param partial give credit for partial match?
     * @param typeRestriction type to be evaluated on (pass null for all types)
     * @return
     */
    public static Stats evalSpans(Document document, String label, boolean partial, PARCAttribution.Type typeRestriction) {
        Stats stats = new Stats();

        Set<Span> usedGoldSpans = new HashSet<>();
        Set<Span> predictedSpans = document.predictedSpansOfLabel(label);
        Set<Span> goldSpans = document.goldSpansOfLabel(label);

        Set<Span> predictedSpansOfInterest;
        Set<Span> goldSpansOfInterest;

        if (typeRestriction != null) {
            predictedSpansOfInterest = Span.getSpansOfType(predictedSpans, typeRestriction);
            goldSpansOfInterest = Span.getSpansOfType(goldSpans, typeRestriction);
        } else {
            predictedSpansOfInterest = predictedSpans;
            goldSpansOfInterest = goldSpans;
        }

        // DENOMINATORS
        if (partial) {
            stats.trueCount = goldSpansOfInterest.size();
            stats.predictedCount = predictedSpansOfInterest.size();

        } else {
            stats.trueCount = goldSpansOfInterest.size();
            stats.predictedCount = predictedSpansOfInterest.size();
        }


        // PRECISION
        if (partial) {
            for (Span predSpan : predictedSpansOfInterest)  {
                List<Span> matchedGoldSpans = predSpan.overlappingSpans(goldSpans);

                for (Span goldSpan : matchedGoldSpans) {
                    int overlap = predSpan.computeOverlap(goldSpan);
                    stats.correctCountP += overlap/(double) predSpan.length();
                }
            }
        } else {
            for (Span predSpan : predictedSpansOfInterest) {
                List<Span> matchedGoldSpans = predSpan.matchingSpans(goldSpans);
                if (matchedGoldSpans.size() > 1) throw new Error("More than one gold span matching!");
                if (!matchedGoldSpans.isEmpty()) {
                    Span matchedSpan = matchedGoldSpans.get(0);
                    if (usedGoldSpans.contains(matchedSpan)) throw new Error("Already used that gold span!");
                    stats.correctCountP += 1;
                }
            }
        }

        // RECALL
        if (partial) {
            for (Span predSpan : predictedSpans) {
                Collection<Span> matchedGoldSpans = predSpan.overlappingSpans(goldSpans);
                if (typeRestriction != null) {
                    matchedGoldSpans = Span.getSpansOfType(matchedGoldSpans, typeRestriction);
                }
                for (Span goldSpan : matchedGoldSpans) {
                    int overlap = predSpan.computeOverlap(goldSpan);
                    stats.correctCountR += overlap/(double) goldSpan.length();
                }
            }
        } else {
            for (Span predSpan : predictedSpans) {
                Collection<Span> matchedGoldSpans = predSpan.matchingSpans(goldSpans);
                if (typeRestriction != null) {
                    matchedGoldSpans = Span.getSpansOfType(matchedGoldSpans, typeRestriction);
                }
                if (matchedGoldSpans.size() > 1) throw new Error("More than one gold span matching!");
                if (!matchedGoldSpans.isEmpty()) {
                    Span matchedGoldSpan = matchedGoldSpans.iterator().next();
                    if (usedGoldSpans.contains(matchedGoldSpan)) throw new Error("Already used that gold span!");
                    stats.correctCountR += 1;
                }
            }
        }

        // sanity checks: do we get more correct answers than predicted?
        if (((int) stats.correctCountP) > stats.predictedCount) {
            System.out.println(((int) stats.correctCountP) + " > " + stats.predictedCount);
            throw new Error("Too many spans");
        }

        // sanity checks: do we get more correct answers than true?
        if (((int) stats.correctCountR) > stats.trueCount) {
            System.out.println(((int) stats.correctCountR) + " > " + stats.trueCount);
            throw new Error("Too many spans");
        }

        // P, R, F
        stats.computePRF();

        return stats;
    }


    /**
     * Classifier evaluation
     * @param document document with begin, end, cue predictions
     * @param position class to be evaluated (begin, end, or cue)
     * @return
     */
    public static Stats evalPerceptron(Document document, String position) {
        Stats stats = new Stats();

        for (Token token : document.tokenList) {
            boolean trueBoundary = false;
            boolean predBoundary = false;

            if (position.equals("begin") && token.startsGoldContentSpan()) trueBoundary = true;
            if (position.equals("end") && token.endsGoldContentSpan()) trueBoundary = true;
            if (position.equals("cue") && token.isGoldCue()) {
                trueBoundary = true;
            }

            if (position.equals("begin") && token.perceptronBeginScore > 0) predBoundary = true;
            if (position.equals("end") && token.perceptronEndScore > 0) predBoundary = true;
            if (position.equals("cue") && token.isPredictedCue) {
                predBoundary = true;
            }

            if (trueBoundary) stats.trueCount ++;
            if (predBoundary) stats.predictedCount ++;
            if (predBoundary && trueBoundary) {
                stats.correctCountP ++;
                stats.correctCountR ++;
            }
        }

        return stats;
    }

    public static Stats evalPerceptron(List<Document> documents, String position) {
        Stats stats = new Stats();

        for (Document document: documents) {
            Stats localStats = evalPerceptron(document, position);
            stats.accumulate(localStats);
        }

        stats.computePRF();

        return stats;
    }

}
