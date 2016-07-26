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

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import edu.stanford.nlp.ling.IndexedWord;
import ims.cs.parc.ParcUtils;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;

import java.util.*;

/**
 * A collection of static features for recognizing span boundaries
 * Created by scheibcn on 3/2/16.
 */
public class BoundaryFeatures {

    /**
     * Returns the previous word safely
     * @param token
     * @return
     */
    public static String prevWord(Token token) {
        Token previousToken = token.previousToken;
        if (previousToken == null) {
            return "NONE";
        } else {
            return previousToken.predText;
        }
    }

    /**
     * Returns the next word safely
     * @param token
     * @return
     */
    public static String nextWord(Token token) {
        Token nextToken = token.nextToken;
        if (nextToken == null) {
            return "NONE";
        } else {
            return nextToken.predText;
        }
    }


    /**
     * Returns the previous lemma safely
     * @param token
     * @return
     */
    public static String prevLemma(Token token) {
        Token previousToken = token.previousToken;
        if (previousToken == null) {
            return "NONE";
        } else {
            return previousToken.predLemma;
        }
    }

    /**
     * Returns the next lemma safely
     * @param token
     * @return
     */
    public static String nextLemma(Token token) {
        Token nextToken = token.nextToken;
        if (nextToken == null) {
            return "NONE";
        } else {
            return nextToken.predLemma;
        }
    }

    /**
     * Returns the previous POS tag safely
     * @param token
     * @return
     */
    public static String prevPosTag(Token token) {
        Token previousToken = token.previousToken;
        if (previousToken == null) {
            return "NONE";
        } else {
            return previousToken.predPosTag;
        }
    }

    /**
     * Returns the next POS tag safely
     * @param token
     * @return
     */
    public static String nextPosTag(Token token) {
        Token nextToken = token.nextToken;
        if (nextToken == null) {
            return "NONE";
        } else {
            return nextToken.predPosTag;
        }
    }

    /**
     * Shape builder, re-implemented from FACTORIE
     * @param text
     * @return
     */
    public static String shape(String text) {
        int maxRepetitions = 3;
        StringBuilder sb = new StringBuilder();
        char prevChar = 0;
        char currentChar = 0;
        int repetitions = 0;

        for (int i = 0; i < text.length(); i++) {
            char actualChar = text.charAt(i);
            if (Character.isUpperCase(actualChar)) currentChar = 'X';
            else if (Character.isLowerCase(actualChar)) currentChar = 'x';
            else if (Character.isDigit(actualChar)) currentChar = '0';
            else if (Character.isWhitespace(actualChar)) currentChar = '_';
            else currentChar = actualChar;
            if (currentChar == prevChar) repetitions += 1;
            else {prevChar = currentChar; repetitions = 0;}

            if (repetitions < maxRepetitions) sb.append(currentChar);
        }

        return sb.toString();
    }

    /**
     * Checks whether any digit occurs in the input string
     * @param s
     * @return
     */
    public static boolean containsDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) return true;
        }
        return false;
    }

    /**
     * Some information about the current tokens and its neighbors
     * @param token
     * @param doLexical
     * @return
     */
    public static List<String> tokenFeatures(Token token, boolean doLexical) {
        List<String> fs = new ArrayList<>();
        if (doLexical) {
            fs.add("TOKEN=" + token.predText);
            fs.add("LEMMA=" + token.predLemma);
            fs.add("POS=" + token.predPosTag);

            // LEMMA BIGRAM
            fs.add("BIGRAM-WORD-L=" + prevWord(token) + "_" + token.predText);
            fs.add("BIGRAM-WORD-R=" + token.predText + "_" + nextWord(token));

            // LEMMA BIGRAM
            fs.add("BIGRAM-LEMMA-L=" + prevLemma(token) + "_" + token.predLemma);
            fs.add("BIGRAM-LEMMA-R=" + token.predLemma + "_" + nextLemma(token));

            // POS BIGRAM
            fs.add("BIGRAM-POS-L=" + prevPosTag(token) + "_" + token.predPosTag);
            fs.add("BIGRAM-POS-R=" + token.predPosTag + "_" + nextPosTag(token));

            // WINDOW
            for (int i = 1; i <= 5; i++) {
                Token prevToken = token.sentence.document.getPrevToken(token, i);
                String modifierPrev = sameSentenceModifier(token, prevToken);


                if (prevToken == null) {
                    fs.add("PREV-WORD-" + modifierPrev + "-" + i + "=NONE");
                    fs.add("PREV-LEMMA-"+ modifierPrev + "-" + i + "=NONE");
                    fs.add("PREV-POS-"+ modifierPrev + "-" + i + "=NONE");
                } else {
                    fs.add("PREV-WORD-"+ modifierPrev + "-" + i + "=" + prevToken.predText);
                    fs.add("PREV-LEMMA-"+ modifierPrev + "-" + i + "=" + prevToken.predLemma);
                    fs.add("PREV-POS-"+ modifierPrev + "-" + i + "=" + prevToken.predPosTag);
                }

                Token nextToken = token.sentence.document.getNextToken(token, i);
                String modifierNext = sameSentenceModifier(token, nextToken);

                if (nextToken == null) {
                    fs.add("NEXT-WORD+" + modifierNext + "-"  + i + "=NONE");
                    fs.add("NEXT-LEMMA+" + modifierNext + "-"   + i + "=NONE");
                    fs.add("NEXT-POS+" + modifierNext + "-"   + i + "=NONE");
                } else {
                    fs.add("NEXT-WORD+" + modifierNext + "-"   + i + "=" + nextToken.predText);
                    fs.add("NEXT-LEMMA+" + modifierNext + "-"   + i + "=" + nextToken.predLemma);
                    fs.add("NEXT-POS+" + modifierNext + "-"   + i + "=" + nextToken.predPosTag);
                }
            }
        }

            // SHAPE
            fs.add("SHAPE=" + shape(token.predText));

            // CAPITALIZATION
            if (!token.predTextIsLower()) fs.add("CAPITALIZED");

            // NUMBERS AND PUNCTUATION
            if (containsDigit(token.predText)) fs.add("NUMERIC");
            if (token.predText.matches("\\{Punct}")) fs.add("IS-PUNCTUATION");

            // POSITION IN DOCUMENT
            if (token.predPosition == token.sentence.document.tokenList.size() - 1)
                fs.add("END-OF-DOCUMENT");

            // CONSTITUENT STRUCTURE
            if (token.hasStartingConstituents())
                fs.add("HAS-STARTING-CONSTITUENT");
            else
                fs.add("HAS-STARTING-CONSTITUENT-NOT");

            if (token.hasEndingConstituents())
                fs.add("HAS-ENDING-CONSTITUENT");
            else
                fs.add("HAS-ENDING-CONSTITUENT-NOT");

            // CASE OF IMMEDIATE NEIGHBORS
            if (token.previousToken != null && token.previousToken.predTextIsLower()) fs.add("PREV-IS-LOWER");
            if (token.nextToken != null && token.nextToken.predTextIsLower()) fs.add("NEXT-IS-LOWER");


        return fs;
    }


    /**
     * Extracts features not contained in the (historically) original feature extractor
     * @param document
     */
    public static void additionalBoundaryFeatures(Document document) {
        // features that are (roughly) about single tokens
        for (Token token : document.getTokenList()) {
            List<String> tokenFeatures = tokenFeatures(token, true);
            token.boundaryFeatureSet.addAll(tokenFeatures);

        }

        // features on the sentence or document level
        addSentenceAndDocumentFeatures(document);
    }

    /**
     * Checks whether a path contains a given relation
     * @param path
     * @param relation
     * @return
     */
    public static boolean pathContainsRelation(GraphPath<IndexedWord, ParcUtils.IndexedEdge> path, String relation) {
        for (ParcUtils.IndexedEdge edge : path.getEdgeList())
            if (edge.rel.getShortName().equals(relation))
                return true;

        return false;
    }

    /**
     * Performs additional conjunctions with pre-existing ones.
     * This is needed after cues have been detected.
     * @param token
     * @param feature
     */
    public static void addConjunction(Token token, String feature) {
        List<String> newFeatures = new ArrayList<>();
        Iterator<String> iter = token.boundaryFeatureSet.iterator();

        while (iter.hasNext()) {
            String oldFeature = iter.next();
            if (oldFeature.contains("CONJUNCTION"))
                newFeatures.add("BOUND:CUE:" + oldFeature + ":" + feature);
        }

        token.boundaryFeatureSet.addAll(newFeatures);
    }

    /**
     * Add cue-dependent features to all documents
     * @param documents
     */
    public static void additionalBoundaryFeaturesFromCue(List<Document> documents) {
        for (Document document : documents) additionalBoundaryFeaturesFromCue(document);
    }

    /**
     * Returns a feature string indicating whether two tokens are in the same sentence
     * @param t1
     * @param t2
     * @return
     */
    public static String sameSentenceModifier(Token t1, Token t2) {
        if (t1 != null && t2 != null && t1.sentence == t2.sentence) return "SAME-SENTENCE";
        else return "DIFFERENT-SENTENCE";
    }

    /**
     * Returns the (directed) dependency path from startToken to endToken
     * @param startToken
     * @param endToken
     * @return
     */
    public static GraphPath pathFromTo(Token startToken, Token endToken) {
        Sentence sentence = startToken.sentence;
        if (sentence != endToken.sentence) return null;

        Graph graph = sentence.fw.getGraph();

        if (graph.containsVertex(startToken.dependencyBackpointer) && graph.containsVertex(endToken.dependencyBackpointer)) {
            return sentence.fw.getShortestPath(startToken.dependencyBackpointer, endToken.dependencyBackpointer);
        } else {
            return null;
        }
    }

    /**
     * Cue-dependent features that can only be added after cues are detected
     * @param document
     */
    public static void additionalBoundaryFeaturesFromCue(Document document) {
        // keep note of whether a token depends on a cue
        boolean[] isCueDep = new boolean[document.tokenList.size()];
        boolean[] singleEdgeL = new boolean[document.tokenList.size()];
        boolean[] singleEdgeR = new boolean[document.tokenList.size()];
        boolean[] pathL = new boolean[document.tokenList.size()];
        boolean[] pathR = new boolean[document.tokenList.size()];


        for (Token potentialCue : document.tokenList) {
            if (!potentialCue.isPredictedCue) continue;
            int position = potentialCue.predPosition;

            // WINDOW FEATURES
            for (int i = 1; i <= 5; i++) {
                // LEFT
                int newPositionLeft = position - i;
                if (newPositionLeft >= 0) {
                    Token adjacentToken = document.tokenList.get(newPositionLeft);
                    FeatureSet fs = adjacentToken.boundaryFeatureSet;
                    String modifier = sameSentenceModifier(potentialCue, adjacentToken);

                    fs.add("CUE-COMES-RIGHT-WIN-" + modifier);
                    fs.add("CUE-COMES-RIGHT-WIN-" + modifier + "-" + i);
                }

                // RIGHT
                int newPositionRight = position + i;
                if (newPositionRight < document.tokenList.size()) {
                    Token adjacentToken = document.tokenList.get(newPositionRight);
                    FeatureSet fs = adjacentToken.boundaryFeatureSet;

                    String modifier = sameSentenceModifier(potentialCue, adjacentToken);

                    fs.add("CUE-COMES-LEFT-WIN-" + modifier);
                    fs.add("CUE-COMES-LEFT-WIN-" + modifier + "-" + i);
                }
            }

            // DISTANCE FEATURES
            for (int i = 1; i < 50; i++) {
                Token prevToken = potentialCue.sentence.document.getNextToken(potentialCue, i);
                if (prevToken != null) {
                    List<String> featuresPrev = Binning.distanceBinsAll(i, "DISTANCE-TO-NEXT-CUE-");
                    prevToken.boundaryFeatureSet.addAll(featuresPrev);
                }

                Token nextToken = potentialCue.sentence.document.getNextToken(potentialCue, i);
                if (nextToken != null) {
                    List<String> featuresNext = Binning.distanceBinsAll(i, "DISTANCE-TO-PREV-CUE-");
                    nextToken.boundaryFeatureSet.addAll(featuresNext);
                }
            }

            // DEPENDENCY FEATURES
            Sentence sentence = potentialCue.sentence;
            for (Token token : sentence.tokenList) {
                if (token == potentialCue) continue;
                FeatureSet fs = token.boundaryFeatureSet;
                Graph graph = sentence.fw.getGraph();

                // SINGLE EDGE
                if (graph.containsEdge(potentialCue.dependencyBackpointer, token.dependencyBackpointer)) {
                    fs.add("BOUND:CUE:SINGLE-EDGE->");
                    singleEdgeR[token.predPosition] = true;
                }
                else {
                    fs.add("BOUND:CUE:SINGLE-EDGE-NOT-LOC->");
                }
                if (graph.containsEdge(token.dependencyBackpointer, potentialCue.dependencyBackpointer)) {
                    fs.add("BOUND:CUE:SINGLE-EDGE<-");
                    singleEdgeL[token.predPosition] = true;
                }
                else {
                    fs.add("BOUND:CUE:SINGLE-EDGE-NOT-LOC<-");
                }

                // PATHS
                if (graph.containsVertex(token.dependencyBackpointer) && graph.containsVertex(potentialCue.dependencyBackpointer)) {

                    GraphPath spRight = pathFromTo(potentialCue, token);
                    if (spRight != null) {
                        fs.add("BOUND:CUE:PATH->");
                        fs.add("BOUND:CUE:PATH->,LEN=" + spRight.getEdgeList().size());
                        if (pathContainsRelation(spRight, "ccomp")) {
                            fs.add("BOUND:CUE:PATH-HAS-CCOMP->");
                        }
                        isCueDep[token.predPosition] = true;

                    } else {
                        fs.add("BOUND:CUE:PATH-NOT-LOC->");
                    }

                    GraphPath spLeft = pathFromTo(token, potentialCue);
                    if (spLeft != null) {
                        fs.add("BOUND:CUE:PATH<-");
                        fs.add("BOUND:CUE:PATH<-,LEN=" + spLeft.getEdgeList().size());
                        if (pathContainsRelation(spLeft, "ccomp")) {
                            fs.add("BOUND:CUE:PATH-HAS-CCOMP<-");
                        }
                        pathL[token.predPosition] = true;
                    } else {
                        fs.add("BOUND:CUE:PATH-NOT-LOC<-");
                    }

                }

                // SENTENCE: HAS CUE
                token.boundaryFeatureSet.add("SENT:HASCUE");
                addConjunction(token, "SENT-HASCUE");
            }
        }

        // checks are done, now add cue dependent information
        for (Token token: document.tokenList) {
            FeatureSet fs = token.boundaryFeatureSet;
            if (isCueDep[token.predPosition]) {
                fs.add("BOUND:CUE:IS-CUE-DEP");
                addConjunction(token, "CUE-DEP");
            } else {
                fs.add("BOUND:CUE:IS-CUE-DEP-NOT");
                addConjunction(token, "CUE-DEP-NOT");
            }
        }
    }

    /**
     * Checks whether a token is a person or organization entity
     * @param token
     * @return
     */
    public static boolean isRelevantNe(Token token) {
        return token.predNer.startsWith("PERSON") || token.predNer.startsWith("ORGANIZATION");
    }

    public static boolean documentHasCue(Document document) {
        for (Token token : document.tokenList) {
            if (token.isPredictedCue) return true;
        }

        return false;
    }

    /**
     * Features involving the document or sentence level
     * @param document
     */
    public static void addSentenceAndDocumentFeatures(Document document) {
        boolean documentHasCue = documentHasCue(document);

        for (Token token : document.tokenList) {
            FeatureSet fs = token.boundaryFeatureSet;

            // document has cue?
            if (documentHasCue) fs.add("DOCUMENT-HAS-CUE");

            // SENTENCE BEGIN/END DISTANCE
            fs.addAll(Binning.distanceBins1to100(token.predSentencePosition, "DISTANCE-TO-SENTENCE-BEGIN"));
            fs.addAll(Binning.distanceBins1to100(token.sentence.tokenList.size() - token.predSentencePosition - 1, "DISTANCE-TO-SENTENCE-END"));

            // SENTENCE BEGIN
            int sentenceLength = token.sentence.tokenList.size();
            if (token.predSentencePosition == 0) {
                fs.add("STARTS-SENTENCE");
            }
            // SENTENCE END
            if (token.predSentencePosition + 1 == sentenceLength) {
                fs.add("ENDS-SENTENCE");
            }
            if (token.predSentencePosition + 2 == sentenceLength) {
                fs.add("NEXT-ENDS-SENTENCE");
            }

            // NE WINDOW
            for (int i = 1; i <= 5; i++) {
                Token prevToken = token.sentence.document.getPrevToken(token, i);
                if (prevToken != null && isRelevantNe(prevToken)) {
                    fs.add("PREV-IS-NE");
                }

                Token nextToken = token.sentence.document.getNextToken(token, i);
                if (nextToken != null && isRelevantNe(nextToken)) {
                    fs.add("NEXT-IS-NE");
                }
            }
        }
    }

}
