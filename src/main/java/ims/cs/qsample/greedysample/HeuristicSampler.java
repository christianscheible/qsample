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
import ims.cs.qsample.spans.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A "sampler" for content spans that actually works heuristically.
 * Created by scheibcn on 3/3/16.
 */
public class HeuristicSampler {

    // whether to randomize the order in which tokens are processed
    public boolean doShuffleTokens = false;
    Random shufRandom = new Random(181178);


    /**
     * Finds the next token right of the given cue position that was predicted to be a begin token
     * @param document
     * @param cuePosition
     * @param maxDist maximum distance of the token, returns -1 if exceeded
     * @return
     */
    public static int findNextBeginFromCue(Document document, int cuePosition, int maxDist) {
        List<Token> tokenList = document.tokenList;
        for (int i = cuePosition + 1; i <= Math.min(cuePosition + maxDist, tokenList.size() - 1); i++) {
            Token token = tokenList.get(i);
            if (token.perceptronBeginScore > 0) return i;
        }
        return -1;
    }

    /**
     * Finds the next token left of the given cue position that was predicted to be an end token
     * @param document
     * @param cuePosition
     * @param maxDist maximum distance of the token, returns -1 if exceeded
     * @return
     */
    public static int findPrevEndFromCue(Document document, int cuePosition, int maxDist) {
        List<Token> tokenList = document.tokenList;
        for (int i = cuePosition - 1; i >= Math.max(cuePosition - maxDist, 0); i--) {
            Token token = tokenList.get(i);
            if (token.perceptronEndScore > 0) return i;
        }
        return -1;
    }

    /**
     * Finds the next token right of the given begin position that was predicted to be an end token
     * @param document
     * @param beginPosition
     * @param maxDist maximum distance of the token, returns -1 if exceeded
     * @return
     */
    public static int findNextEndFromBegin(Document document, int beginPosition, int maxDist) {
        List<Token> tokenList = document.tokenList;
        for (int i = beginPosition + 1; i <= Math.min(beginPosition + maxDist, tokenList.size() - 1); i++) {
            Token token = tokenList.get(i);
            if (token.perceptronEndScore > 0) return i;
        }
        return -1;
    }

    /**
     * Finds the next token left of the given end position that was predicted to be a begin token
     * @param document
     * @param endPosition
     * @param maxDist maximum distance of the token, returns -1 if exceeded
     * @return
     */
    public static int findPrevBeginFromEnd(Document document, int endPosition, int maxDist) {
        List<Token> tokenList = document.tokenList;
        for (int i = endPosition - 1; i >= Math.max(endPosition - maxDist, 0); i--) {
            Token token = tokenList.get(i);
            if (token.perceptronBeginScore > 0) return i;
        }
        return -1;
    }


    /**
     * Predict content spans for a document using the greedy sampling heuristic
     * @param document
     * @param maxDistFromCue
     * @param maxSpanLength
     */
    public void sampleGreedy(Document document, int maxDistFromCue, int maxSpanLength) {

        // shuffle tokens?
        List<Token> tokenList = new ArrayList<>(document.tokenList);
        if (doShuffleTokens) Collections.shuffle(tokenList, shufRandom);


        // go through tokens in pre-defined order
        for (Token token : tokenList) {
            // if the token is a cue ...
            if (token.isPredictedCue) {
                // find a begin token to the right
                int nextBegin = findNextBeginFromCue(document, token.predPosition, maxDistFromCue);
                if (nextBegin != -1) {
                    // make sure there is no span yet
                    Token beginToken = document.tokenList.get(nextBegin);
                    if (beginToken.isInPredictedContentSpan()) continue;

                    // find an end token
                    int nextEnd = findNextEndFromBegin(document, nextBegin, maxSpanLength);
                    if (nextEnd != -1) {
                        // make sure there is no span yet
                        Token endToken = document.tokenList.get(nextEnd);
                        if (endToken.isInPredictedContentSpan()) continue;

                        // add a new span
                        document.predictedSpanSet.add(new Span(document, nextBegin, nextEnd, "content"));
                    }
                }

                // also go backwards: find an end token to the left
                int prevEnd = findPrevEndFromCue(document, token.predPosition, maxDistFromCue);
                if (prevEnd != -1) {
                    // make sure there is no span yet
                    Token endToken = document.tokenList.get(prevEnd);
                    if (endToken.isInPredictedContentSpan()) continue;

                    // find a begin token
                    int prevBegin = findPrevBeginFromEnd(document, prevEnd, maxSpanLength);
                    if (prevBegin != -1) {
                        // make sure there is no span yet
                        Token beginToken = document.tokenList.get(prevBegin);
                        if (beginToken.isInPredictedContentSpan()) continue;

                        // add span
                        document.predictedSpanSet.add(new Span(document, prevBegin, prevEnd, "content"));
                    }
                }
            }
        }
    }

    /**
     * Predict content spans for all documents using the greedy sampling heuristic
     * @param documents
     * @param maxDistFromCue
     * @param maxSpanLength
     */
    public void sampleGreedy(List<Document> documents, int maxDistFromCue, int maxSpanLength) {
        System.out.println("Sampling ");
        int i = 0;
        for (Document document : documents) {
            if (i++ % 50 == 0) System.out.print(" " + i);
            sampleGreedy(document, maxDistFromCue, maxSpanLength);
        }
        System.out.println();
    }
}
