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

package ims.cs.qsample.greedysample;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.qsample.features.SpanFeatures;
import ims.cs.qsample.models.HigherSpanModel;
import ims.cs.qsample.models.QuotationPerceptrons;
import ims.cs.qsample.spans.Span;
import ims.cs.qsample.spans.SpanBegin;
import ims.cs.qsample.spans.SpanEnd;
import ims.cs.util.StaticConfig;

import java.util.*;

/**
 * A sampler for content spans based on boundary prediction made by token-level classifiers
 * Created by scheibcn on 3/5/16.
 */
public class PerceptronSampler {

    // random generators for shuffling and picking a direction
    Random shufRandom = new Random(171789909);
    Random directionRandom = new Random(171789909);

    // models
    QuotationPerceptrons proposalPerceptrons;
    HigherSpanModel spanModel;

    // samplers
    Sampling beginSampling = new Sampling(new Random(123));
    Sampling endSampling = new Sampling(new Random(313));

    // parameters
    double learningRate = 0.1;
    boolean linearSampling = false;
    boolean updateForGoldSpan = false;
    enum OverlappingSpanCriterion {MAX, SUM, MEAN};
    OverlappingSpanCriterion overlappingSpanCriterion = OverlappingSpanCriterion.SUM;

    /**
     * Set up a new sampler based on the pre-trained proposal perceptrons
     * @param proposalPerceptrons
     */
    public PerceptronSampler(QuotationPerceptrons proposalPerceptrons) {
        this.proposalPerceptrons = proposalPerceptrons;
        this.spanModel = proposalPerceptrons.associatedSpanModel;
    }

    /**
     * Sample an end token according to perceptron scores from the tokens of a document
     * @param document
     * @return
     */
    public int sampleEnd(Document document) {
        // set up list of scored end positions
        int numTokens = document.tokenList.size();
        List<HasScore> endList = new ArrayList<>(numTokens);

        for (int position = 0; position < numTokens; position++) {
            endList.add(new SpanEnd(position, document.tokenList.get(position).perceptronEndScore));
        }

        // sample a token from this list
        int listPosition = endSampling.sampleOne(endList, StaticConfig.endTemperature, 0);
        int position = ((SpanEnd) endList.get(listPosition)).position;

        // statistics
        document.tokenList.get(position).numTimesSampledEnd++;

        return position;
    }

    /**
     * Sample a begin token according to perceptron scores from the tokens of a document
     * @param document
     * @return
     */
    public int sampleBegin(Document document) {
        // set up list of scored begin positions
        int numTokens = document.tokenList.size();
        List<HasScore> beginList = new ArrayList<>(numTokens);

        for (int position = 0; position < numTokens; position++) {
            beginList.add(new SpanBegin(position, document.tokenList.get(position).perceptronBeginScore));
        }

        // sample a token from this list
        int listPosition = beginSampling.sampleOne(beginList, StaticConfig.beginTemperature, 0);
        int position = ((SpanBegin) beginList.get(listPosition)).position;

        // statistics
        document.tokenList.get(position).numTimesSampledBegin++;

        return position;
    }

    /**
     * Sample a begin token left of a given end token according to perceptron scores from the tokens of a document
     * @param document
     * @return
     */
    public int sampleBegin(Document document, int endPosition) {
        // set up list of scored begin positions
        int numTokens = document.tokenList.size();
        List<HasScore> beginList = new ArrayList<>();

        // determine the leftmost possible position according to maxLengthSampling
        int maxBeginPosition = Math.max(0, endPosition - StaticConfig.maxLengthSampling);

        for (int position = endPosition; position >= maxBeginPosition; position--) {
            beginList.add(new SpanBegin(position, document.tokenList.get(position).perceptronBeginScore));
        }

        // abort if there is no possible position
        if (beginList.isEmpty()) return -1;

        // draw a position
        int listPosition = beginSampling.sampleOne(beginList, StaticConfig.beginTemperature, 0);
        int position = ((SpanBegin) beginList.get(listPosition)).position;

        // statistics
        document.tokenList.get(position).numTimesSampledBegin++;


        return position;
    }


    /**
     * Sample an end token right of a given begin token according to perceptron scores from the tokens of a document
     * @param document
     * @return
     */
    public int sampleEnd(Document document, int beginPosition) {
        // set up list of scored end positions
        int numTokens = document.tokenList.size();
        List<HasScore> endList = new ArrayList<>(numTokens - beginPosition);

        // determine the rightmost possible position according to maxLengthSampling
        int maxEndPosition = Math.min(numTokens - 1, beginPosition + StaticConfig.maxLengthSampling);

        for (int position = beginPosition; position <= maxEndPosition; position++) {
            endList.add(new SpanEnd(position, document.tokenList.get(position).perceptronEndScore));
        }

        // abort if there is no possible position
        if (endList.isEmpty()) return -1;

        // draw a position
        int listPosition = endSampling.sampleOne(endList, StaticConfig.endTemperature, 0);
        int position = ((SpanEnd) endList.get(listPosition)).position;

        // statistics
        document.tokenList.get(position).numTimesSampledEnd++;

        return position;
    }

    /**
     * Generate a number of span candidates for a given document
     * @param document
     * @return
     */
    public List<Span> sampleBeginEndRandomly(Document document) {
        List<Span> candidates = new ArrayList<>(1);

        int beginPosition = -1;
        int endPosition = -1;

        int numTrials = 0;

        // try to find a consistent configuration of begin and end tokens
        // draw at most maxNumTrials spans
        while(numTrials < StaticConfig.maxNumTrials && (beginPosition == -1 || endPosition == -1)) {
            // sample whether to start with the begin or end token
            boolean goForward = directionRandom.nextBoolean();
            if (goForward) {   /* begin first */
                beginPosition = sampleBegin(document);
                endPosition = sampleEnd(document, beginPosition);
                numTrials++;
            } else {   /* end first */
                endPosition = sampleEnd(document);
                beginPosition = sampleBegin(document, endPosition);
                numTrials++;
            }
        }

        candidates.add(new Span(document, beginPosition, endPosition, "content"));
        return candidates;
    }


    /**
     * Generate a number of span candidates for a given document while considering cue positions
     * @param document
     * @return
     */
    public List<Span> sampleBeginEndCueLinear(Document document) {

        List<Span> candidateList = new ArrayList<>();

        int beginPosition = -1;
        int endPosition = -1;

        // test al tokens
        for (int cuePosition = 0; cuePosition < document.tokenList.size(); cuePosition++) {
            // skip non-cues
            Token cue = document.tokenList.get(cuePosition);
            if (!cue.isPredictedCue) continue;

            // sample a direction, then sample a begin and end position
            boolean goForward = directionRandom.nextBoolean();
            if (goForward) {
                beginPosition = HeuristicSampler.findNextBeginFromCue(document, cue.predPosition, StaticConfig.maxCueDistanceSampling);
                if (beginPosition != -1) endPosition = sampleEnd(document, beginPosition);
            } else {
                endPosition = HeuristicSampler.findPrevEndFromCue(document, cue.predPosition, StaticConfig.maxCueDistanceSampling);
                if (endPosition != -1) beginPosition = sampleBegin(document, endPosition);
            }
            if (beginPosition != -1 && endPosition != -1)
                candidateList.add(new Span(document, beginPosition, endPosition, "content"));

        }

        return candidateList;
    }


    /**
     * Update perceptron weights based on
     * @param document
     * @param candidateSpan
     */
    public void updateAgainstGold (Document document, Span candidateSpan) {
        // find gold spans that match the candidate
        List<Span> matchingGoldSpans = candidateSpan.matchingSpans(document.goldSpanSet);

        // if there is none, the span is not correct
        boolean isCorrect = !matchingGoldSpans.isEmpty();

        // if the span is incorrect and there is a margin violation, perform an update
        if (!isCorrect && candidateSpan.score > -StaticConfig.samplerMarginNegative) {
            spanModel.train(candidateSpan, false, learningRate);

            if (updateForGoldSpan) {
                // Also update weights for missed gold spans?
                // TODO: ideally, you want to check for score violations here

                List<Span> overlappingGoldSpans = candidateSpan.overlappingSpans(document.goldSpanSet);
                for (Span goldSpan : overlappingGoldSpans) {
                    spanModel.train(goldSpan, true, learningRate);
                }
            }
        } else if (isCorrect && candidateSpan.score <= StaticConfig.samplerMarginPositive) {
            // also update correct spans if the score is not large enough yet
            spanModel.train(candidateSpan, true, learningRate);
        }
    }

    /**
     * Sample new spans for a given document. If isTraining is set, we also perform updates.
     * @param document
     * @param isTraining
     * @param numIter
     */
    public void sampleAndScoreBeginEnd(Document document, boolean isTraining, int numIter) {
        // use the averaged perceptron if we're not in training
        boolean doAverage = !isTraining;

        // container for span proposals
        Set<Span> proposedSpanSet = new HashSet<>();

        for (int iter = 0; iter < numIter; iter++) {
            // sample a new span
            List<Span> candidateSpanList;

            // sample some candidates
            if (linearSampling) {
                candidateSpanList = sampleBeginEndCueLinear(document);
            } else {
                candidateSpanList = sampleBeginEndRandomly(document);
            }

            for (Span candidateSpan : candidateSpanList) {
                if (candidateSpan == null) continue;

                // skip span if it's already there
                if (document.predictedSpanSet.contains(candidateSpan)) continue;

                // also skip spans that we already checked (we sampled without replacement)
                if (proposedSpanSet.contains(candidateSpan)) {
                    continue;
                } else {
                    proposedSpanSet.add(candidateSpan);
                }

                // extract features for the span
                SpanFeatures.addAllSpanFeatures(candidateSpan);

                // score the span
                candidateSpan.score = spanModel.score(candidateSpan, doAverage);

                // now try to add the span
                // we can only accept the span if its score is > 0
                if (candidateSpan.score > 0) {
                    double existingScores = 0;

                    // find spans at the same position
                    List<Span> existingSpans = candidateSpan.overlappingSpans(document.predictedSpanSet);

                    // if there are other spans, check whether removing them is justified
                    // score other spans and average
                    for (Span existingSpan : existingSpans) {
                        // extract features if necessary
                        if (existingSpan.featureSet == null)
                            SpanFeatures.addAllSpanFeatures(existingSpan);

                        // score the span
                        existingSpan.score = spanModel.score(existingSpan, doAverage);

                        if (overlappingSpanCriterion == OverlappingSpanCriterion.SUM ||
                                overlappingSpanCriterion == OverlappingSpanCriterion.MEAN) {   /* score sum or mean */
                            existingScores += existingSpan.score;
                        } else {   /* maximum score */
                            if (existingSpan.score > existingScores)
                                existingScores = existingSpan.score;
                        }
                    }

                    // normalize if method is mean
                    if (overlappingSpanCriterion == OverlappingSpanCriterion.MEAN &&
                            !existingSpans.isEmpty())
                        existingScores /= existingSpans.size();

                    // check if the new span is better than the overlapping spans
                    if (candidateSpan.score > existingScores) {
                        document.predictedSpanSet.add(candidateSpan);

                        // remove all overlapping spans
                        for (Span existingSpan : existingSpans) {
                            boolean success = document.predictedSpanSet.remove(existingSpan);
                            if (!success) throw new Error("Remove failed!");
                        }

                        // optional sanity check for removal debugging, which is slow, so turned off by default
                        if (false) {
                            existingSpans = candidateSpan.overlappingSpans(document.predictedSpanSet);
                            for (Span other : existingSpans) {
                                if (other != candidateSpan) {
                                    System.out.print("");
                                    throw new Error("Remove failed!");
                                }
                            }
                        }
                    }
                }

                // update perceptron weights
                if (isTraining) updateAgainstGold(document, candidateSpan);
            }
        }
    }

    /**
     * Go through the spans of a document and removes all with a score smaller than 0
     * @param document
     * @param isTraining
     */
    public void removeBadSpans(Document document, boolean isTraining) {
        boolean doAverage = !isTraining;

        Iterator<Span> iterator = document.predictedSpanSet.iterator();

        while (iterator.hasNext()) {
            Span span = iterator.next();
            if (span.featureSet == null) SpanFeatures.addAllSpanFeatures(span);
            span.score = spanModel.score(span, doAverage);

            if (span.score <= 0)
                iterator.remove();

            // update some more
            if (isTraining) updateAgainstGold(document, span);
        }
    }

    /**
     * Sample new spans for a given list of documents. If isTraining is set, we also perform updates.
     * @param documentList
     * @param isTraining
     * @param numIter
     */
    public void sampleAndScoreBeginEnd(List<Document> documentList, boolean isTraining, int numIter) {
        List<Document> shuffledDocumentList = new ArrayList<>(documentList);
        Collections.shuffle(shuffledDocumentList, shufRandom);


        for (Document document : shuffledDocumentList) {
            removeBadSpans(document, isTraining);
            sampleAndScoreBeginEnd(document, isTraining, numIter);
        }
    }


}
