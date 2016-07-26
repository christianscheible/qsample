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


package ims.cs.qsample.features;

import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.corenlp.Helper;
import edu.stanford.nlp.trees.Constituent;
import ims.cs.qsample.spans.Span;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by scheibcn on 11/5/15.
 */
public class SpanFeatures {

    /**
     * Pastes text in front of all feature names
     * @param fs
     * @param text
     * @return
     */
    public static FeatureSet indexBy(FeatureSet fs, String text) {
        FeatureSet fsIndex = new FeatureIntSet();
        for (String feature: fs) {
            fsIndex.add(text + "-" + feature);
        }

        return fsIndex;
    }

    /**
     * Add features about the type of the span (direct, indirect, mixed)
     * @param span
     * @param fs
     */
    public static void spanTypeFeatures(Span span, FeatureSet fs) {
        String spanType = "SPANTYPE-INDIRECT";

        if (Helper.isQuote(span.first()) && Helper.isQuote(span.last())) {
            spanType = "SPANTYPE-DIRECT";

        } else {
            for (int i = span.begin.position; i <= span.end.position; i++) {
                if (Helper.isQuote(span.tokenAt(i))) spanType = "SPANTYPE-MIXED";
            }
        }

        fs.add(spanType);

        // CONJUNCTION
//        List<String> conjunctionFeatures = new ArrayList<>(fs.size());
//        for (String feature : fs) {
//            if (!feature.startsWith("SPANTYPE-")) {
//                conjunctionFeatures.add(spanType + "_&_" + feature);
//            }
//        }
//        fs.addAll(conjunctionFeatures);
    }

    /**
     * Add features related to constituent matches of the span
     * @param span
     * @param fs
     */
    public static void matchesConstituent(Span span, FeatureSet fs) {
        Sentence sentence = span.first().sentence;
        if (sentence != span.last().sentence) return;

        // CONSTITUENT SEQUENCE MATCH
        if (span.first().boundaryFeatureSet.contains("HAS-STARTING-CONSTITUENT") &&
                span.last().boundaryFeatureSet.contains("HAS-ENDING-CONSTITUENT")) {
            fs.add("MATCHES-AT-LEAST-ONE-CONSTITUENT");
        }

        // CONSTITUENT EXACT MATCH
        Set<Constituent> constituents = sentence.tree.constituents();
        for (Constituent constituent : constituents) {
            int begin = constituent.start();
            int end = constituent.end();

            // TODO POST-ACL: end - begin > 0 would be correct. change later.
            if (end - begin > 1 && begin == span.first().predSentencePosition && end == span.last().predSentencePosition) {
                fs.add("MATCHES-CONSTITUENT");
            }
        }
    }

    /**
     * Add features about the relation of the span to the sentences of the document (number of sentences matched, ...)
     * @param span
     * @param fs
     */
    public static void sentenceStructureFeature(Span span, FeatureSet fs) {
        Sentence sentence = null;
        int numSentences = 0;

        for (int i = span.begin.position; i <= span.end.position; i++) {
            Token token = span.tokenAt(i);
            Sentence newSentence = token.sentence;
            if (newSentence != sentence) {
                numSentences++;
                sentence = newSentence;
            }
        }

        fs.add("NUMBER-OF-SENTENCES=" + numSentences);

        if (numSentences == 1) {
            if (span.begin.position == 0 && span.end.position == sentence.tokenList.size() - 1) {
                fs.add("MATCHES-SINGLE-SENTENCE");
            }
            if (span.begin.position == 0 && span.end.position == sentence.tokenList.size() - 2) {
                fs.add("MATCHES-SINGLE-SENTENCE-EXCEPT-FOR-LAST-TOKEN");
            }

        }
    }

    /**
     * Adds features about the number of tokens matched
     * @param span
     * @param fs
     */
    public static void numTokensFeature(Span span, FeatureSet fs) {
        int spanLength = span.length();
        List<String> features = Binning.distanceBins1to100(spanLength, "SPAN-LENGTH");
        fs.addAll(features);
        if (spanLength <= 5) fs.add("SPAN-LENGTH=" + span.length());
    }

    /**
     * Adds features about whether the span overlaps any cues
     * @param span
     * @param fs
     */
    public static void overlapsCueFeature(Span span, FeatureSet fs) {
        int numOverlapped = 0;

        for (int i = span.begin.position; i <= span.end.position; i++) {
            Token token = span.tokenAt(i);
            if (token.isPredictedCue) {
                numOverlapped++;
            }
        }

        if (numOverlapped > 0) {
            fs.add("OVERLAPS-CUE");
            fs.add("OVERLAPS-CUE,NUMBER=" + numOverlapped);
        }
    }

    /**
     * Adds some features on the tokens the span contains that are implemented in the boundary feature extractor
     * @param span
     * @param fs
     */
    public static void simpleTokenFeatures(Span span, FeatureSet fs) {
        for (int i = span.begin.position+1; i <= span.end.position-1; i++) {
            Token iToken = span.tokenAt(i);
            List<String> insideFeatures = BoundaryFeatures.tokenFeatures(iToken, false);
            fs.addAll(insideFeatures);
        }

    }


    /**
     * Adds some count features about the tokens of a span. These are mostly counts (e.g., how many NE's are in the span?)
     * @param span
     * @param fs
     */
    public static void tokenFeatures(Span span, FeatureSet fs) {
        int numNe = 0;
        int numLowerCase = 0;
        int numPronoun = 0;
        int numComma = 0;

        for (int i = span.begin.position+1; i <= span.end.position-1; i++) {
            Token iToken = span.tokenAt(i);
            if (fs.contains("SHAPE=[XX]")) numNe ++;
            if (iToken.predTextIsLower()) numLowerCase++;
            if (iToken.predPosTag.startsWith("PR")) numPronoun++;
            if (iToken.predText.equals(",")) numComma++;
        }

        fs.add("NUMBER-OF-NE=" + numNe);
        fs.add("NUMBER-OF-PRO=" + numPronoun);
        fs.add("NUMBER-OF-LOWERCASE=" + numLowerCase);
        fs.add("NUMBER-OF-COMMA=" + numLowerCase);


        for (int i = span.begin.position; i <= span.end.position; i++) {
            Token token = span.tokenAt(i);
            if (token.predPosTag.startsWith("V")) fs.add("CONTAINS-VERB");
        }

    }

    /**
     * Find the closest cue to the left within the same sentence
     * @param sentence
     * @param token
     * @return
     */
    public static Token seekCueLeft(Sentence sentence, Token token) {
        for (int i = token.predSentencePosition - 1; i >= 0; i--) {
            Token prevToken = sentence.tokenList.get(i);
            if (prevToken.isPredictedCue) return prevToken;
        }
        return null;
    }

    /**
     * Find the closest cue to the right within the same sentence
     * @param sentence
     * @param token
     * @return
     */
    public static Token seekCueRight(Sentence sentence, Token token) {
        for (int i = token.predSentencePosition + 1; i < sentence.tokenList.size(); i++) {
            Token prevToken = sentence.tokenList.get(i);
            if (prevToken.isPredictedCue) return prevToken;
        }
        return null;
    }

    /**
     * Add gold information. This is only used for debugging and is not part of the production model.
     * @param span
     * @param fs
     */
    public static void goldFeatures(Span span, FeatureSet fs) {
        boolean correct = !span.matchingSpans(span.document.goldSpanSet).isEmpty();
        if (correct) fs.add("!!!!!!!!!!!!!!!!!!!!!!!!!!!GOLD!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!IS-CORRECT");
        else fs.add("!!!!!!!!!!!!!!!!!!!!!!!!!!!GOLD!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!IS-WRONG");
    }


    /**
     * Adds conjunctions on the begin and end tokens of a span
     * @param span
     * @param fs
     */
    public static void beginEndConjunction(Span span, FeatureSet fs) {
        Token firstToken = span.first();
        Token lastToken = span.last();

        fs.add("BE-CONJUNCTION-WORD-WORD=" + firstToken.predText + "_" + lastToken.predText);
        fs.add("BE-CONJUNCTION-LEMMA-LEMMA=" + firstToken.predLemma + "_" + lastToken.predLemma);
        fs.add("BE-CONJUNCTION-POS-POS=" + firstToken.predPosTag + "_" + lastToken.predPosTag);

        fs.add("BE-CONJUNCTION-WORD-1-WORD+1=" + BoundaryFeatures.prevWord(firstToken) + "_" + BoundaryFeatures.nextWord(lastToken));
        fs.add("BE-CONJUNCTION-LEMMA-1-LEMMA+1=" + BoundaryFeatures.prevLemma(firstToken) + "_" + BoundaryFeatures.nextLemma(lastToken));
        fs.add("BE-CONJUNCTION-POS-1-POS+1=" + BoundaryFeatures.prevPosTag(firstToken) + "_" + BoundaryFeatures.nextPosTag(lastToken));

        fs.add("BE-CONJUNCTION-WORD-WORD+1=" + firstToken.predText + "_" + BoundaryFeatures.nextWord(lastToken));
        fs.add("BE-CONJUNCTION-LEMMA-LEMMA+1=" + firstToken.predLemma + "_" + BoundaryFeatures.nextLemma(lastToken));
        fs.add("BE-CONJUNCTION-POS-POS+1=" + firstToken.predPosTag + "_" + BoundaryFeatures.nextPosTag(lastToken));

        fs.add("BE-CONJUNCTION-WORD-1-WORD=" + BoundaryFeatures.prevWord(firstToken) + "_" + lastToken.predText);
        fs.add("BE-CONJUNCTION-LEMMA-1-LEMMA=" + BoundaryFeatures.prevLemma(firstToken) + "_" + lastToken.predLemma);
        fs.add("BE-CONJUNCTION-POS-1-POS=" + BoundaryFeatures.prevPosTag(firstToken) + "_" + lastToken.predPosTag);
    }

    /**
     * Adds features about the cue structure in relation to the span
     * @param span
     * @param fs
     */
    public static void cueStructure(Span span, FeatureSet fs) {
        // find next cue in first sentence
        Token firstToken = span.first();
        Sentence firstSentence = firstToken.sentence;
        Token prevCue = seekCueLeft(firstSentence, firstToken);

        // find next cue in last sentence
        Token lastToken = span.last();
        Sentence lastSentence = lastToken.sentence;
        Token nextCue = seekCueRight(lastSentence, lastToken);

        // CUE DEP STRUCTURE
        if (span.first().boundaryFeatureSet.contains("BOUND:CUE:IS-CUE-DEP")
                && span.last().boundaryFeatureSet.contains("BOUND:CUE:IS-CUE-DEP")) {
            fs.add("BOTH-CUE-DEP");
        } else {
            fs.add("BOTH-CUE-DEP-NOT");
        }

        // NUMBER OF CUE DEPENDENT
        int numCueDep = 0;
        for (int i = span.begin.position; i <= span.end.position; i++) {
            if (span.tokenAt(i).boundaryFeatureSet.contains("BOUND:CUE:IS-CUE-DEP")) {
                numCueDep++;
            }
        }
        double cueRatio = numCueDep/(double) span.length();
        List cueRatioBins = Binning.distanceBins1to100((int) (cueRatio * 100), "CUE-DEP-PERCENTAGE");
        fs.addAll(cueRatioBins);

        // SURROUNDINGS?
        if (prevCue != null) {
            fs.add("CUE-PRECEDES-FIRST-TOKEN");
        }

        if (nextCue != null) {
            fs.add("CUE-SUCCEEDS-LAST-TOKEN");
        }

        boolean bothSentCues = false;
        if (prevCue != null && nextCue != null) {
            fs.add("BOTH-SENTENCES-HAVE-CUES");
            bothSentCues = true;
        } else if (prevCue == null && nextCue == null) {
            fs.add("NO-SENTENCE-HAS-CUE");
        }

        // CLOSEST
        if (prevCue != null || nextCue != null) {
            int distToPrev = 1000;
            if (prevCue != null) distToPrev = Math.abs(prevCue.predPosition - firstToken.predPosition);

            int distToNext = 1000;
            if (nextCue != null) distToNext = Math.abs(nextCue.predPosition - lastToken.predPosition);

            Token closestCue;
            boolean closestIsLeft = distToNext > distToPrev;
            if (closestIsLeft) {
                closestCue = prevCue;
            } else {
                closestCue = nextCue;
            }

            if (closestIsLeft) fs.add("LEFT-CUE-IS-CLOSEST");
            else fs.add("RIGHT-CUE-IS-CLOSEST");

            // SYNTACTIC INFO
            GraphPath closestCueToBegin = BoundaryFeatures.pathFromTo(closestCue, firstToken);
            GraphPath closestCueToEnd = BoundaryFeatures.pathFromTo(closestCue, lastToken);

            if (closestCueToBegin != null) fs.add("BEGIN-DEPENDS-ON-CLOSEST-CUE");
            if (closestCueToEnd != null) fs.add("END-DEPENDS-ON-CLOSEST-CUE");
            if (closestCueToBegin != null && closestCueToEnd != null) fs.add("BEGIN-AND-END-DEPEND-ON-CLOSEST-CUE");

            int numClosestCueDep = 0;
            for (int i = span.begin.position; i <= span.end.position; i++) {
                Token innerToken = span.tokenAt(i);
                GraphPath closestCueToInner = BoundaryFeatures.pathFromTo(closestCue, firstToken);
                if (closestCueToInner != null) numClosestCueDep++;
            }

            // CLOSEST CUE DEP RATIO
            double closestCueRatio = numClosestCueDep/(double) span.length();
            List closestCueRatioBins = Binning.distanceBins1to100((int) (closestCueRatio * 100), "CLOSEST-CUE-DEP-PERCENTAGE");
            fs.addAll(closestCueRatioBins);

            // CLOSEST CUE: WORDS BETWEEN
            if (closestIsLeft) {
                for (int betweenPos = closestCue.predPosition+1; betweenPos < firstToken.predPosition; betweenPos++) {
                    Token betweenToken = span.document.tokenList.get(betweenPos);
                    fs.add("WORD-BETWEEN-CUE-AND-CONTENT-LEFT-" + betweenToken.predText);
                    fs.add("POS-BETWEEN-CUE-AND-CONTENT-LEFT-" + betweenToken.predPosTag);
                }
            } else {
                for (int betweenPos = lastToken.predPosition+1; betweenPos < closestCue.predPosition; betweenPos++) {
                    Token betweenToken = span.document.tokenList.get(betweenPos);
                    fs.add("WORD-BETWEEN-CUE-AND-CONTENT-RIGHT-" + betweenToken.predText);
                    fs.add("POS-BETWEEN-CUE-AND-CONTENT-RIGHT-" + betweenToken.predPosTag);
                }
            }
        }


        // CONJUNCTION
        List<String> conjunctionFeatures = new ArrayList<>(fs.size());
        for (String feature : fs) {
            if (feature.contains("CONJUNCTION")) {
                conjunctionFeatures.add("BOTH-SENTENCES-HAVE-CUES=" + bothSentCues + "_&_" + feature);
            }
        }
        fs.addAll(conjunctionFeatures);


    }

    /**
     * Adds features about the quotation mark structure (e.g., does the span contain an even number of quotation marks?)
     * @param span
     * @param fs
     */
    public static void qmStructure(Span span, FeatureSet fs) {
        int numQm = 0;

        for (int i = span.begin.position; i <= span.end.position; i++) {
            Token iToken = span.tokenAt(i);
            if (Helper.isQuote(iToken)) numQm++;
        }

        boolean lastEndsDoc = span.end.position == span.document.tokenList.size() - 1;
        boolean numQmEven = numQm % 2 == 0;
        boolean lastIsQm = Helper.isQuote(span.last());
        boolean firstIsQm = Helper.isQuote(span.first());

        fs.add("NUM-QM-EVEN=" + numQmEven + "_&_AT-DOC-END=" + lastEndsDoc);
        if (numQm == 1) fs.add("NUM-QM=" + numQm + "_&_" + "LAST-IS-QM=" + lastIsQm);
        if (numQm == 2 && lastIsQm && firstIsQm) fs.add("WELL-FORMED-DIRECT");
    }

    /**
     * Remove cue features from previous extractors
     * @param fs
     */
    public static void filterIncoming(FeatureSet fs) {
        Iterator<String> iterator = fs.iterator();
        while (iterator.hasNext()) {
            String feature = iterator.next();
            if (feature.endsWith("-CUELIST")) {
                iterator.remove();
            }
        }
    }

    /**
     * The main feature extraction function
     * @param span
     * @return
     */
    public static void addAllSpanFeatures(Span span) {
        FeatureSet fs = new FeatureIntSet();

        // full span features
        sentenceStructureFeature(span, fs);
        numTokensFeature(span, fs);
        overlapsCueFeature(span, fs);
        matchesConstituent(span, fs);
        cueStructure(span, fs);
        qmStructure(span, fs);
        simpleTokenFeatures(span, fs);
        tokenFeatures(span, fs);
        beginEndConjunction(span, fs);
        spanTypeFeatures(span, fs);

        // DEBUG: add gold information
//        goldFeatures(span, fs);

        span.featureSet = fs;
    }



}
