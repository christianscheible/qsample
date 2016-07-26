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


package ims.cs.qsample.spans;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.parc.PARCAttribution;
import ims.cs.qsample.features.FeatureSet;
import ims.cs.qsample.greedysample.HasScore;

import java.util.*;

/**
 * Implementation of spans over documents
 * Created by scheibcn on 11/5/15.
 */
public class Span implements HasScore {

    public Document document;

    // begin and end point
    public SpanBegin begin;  /* begin marks the first token of the span (inclusive) */
    public SpanEnd end;      /* end marks the last token of the span (also inclusive!) */

    // modeling data
    public double score;
    public double objective;
    public String label;
    public FeatureSet featureSet;


    /**
     * Create a span wrt a sentence. beginInSentence and endInSentence are positions within this sentence.
     * Internally, the sentence positions will be translated into document positions.
     * @param sentence
     * @param beginInSentence
     * @param endInSentence
     * @param label
     */
    public Span(Sentence sentence, int beginInSentence, int endInSentence, String label) {
        this(sentence.document,
                sentence.first().goldPosition + beginInSentence,
                sentence.first().goldPosition + endInSentence,
                label);
    }

    /**
     * Create a span wrt a document given integer positions.
     * @param document
     * @param begin
     * @param end
     * @param label
     */
    public Span(Document document, int begin, int end, String label) {
        this(document, new SpanBegin(begin), new SpanEnd(end), label);
    }

    /**
     * Create a span wrt a document given begin and end objects.
     * @param document
     * @param begin
     * @param end
     * @param label
     */
    public Span(Document document, SpanBegin begin, SpanEnd end, String label) {
        this.document = document;
        this.begin = begin;
        this.end = end;
        this.label = label;

        // range check begin
        if (begin.position < 0 || begin.position >= document.tokenList.size()) {
            throw new IndexOutOfBoundsException("Begin out of bounds");
        }

        // range check end
        if (end.position < 0 || end.position >= document.tokenList.size()) {
            throw new IndexOutOfBoundsException("End out of bounds");
        }



    }

    /**
     * Checks whether the span overlaps any other from a list
     * @param others
     * @return
     */
    public boolean overlaps(Collection<Span> others) {
        for (Span other : others) {
            if (this.overlaps(other)) return true;
        }

        return false;
    }


    /**
     * Returns whether this span matches the other exactly.
     * An exact match occurs if the begin and end positions of both spans are equal
     * @param goldSpan
     * @return
     */
    public boolean matches(Span goldSpan) {
        return this.begin.position == goldSpan.begin.position && this.end.position == goldSpan.end.position;
    }

    /**
     * Returns whether there is a semi match between this span and the other.
     * A semi match is given if at least one of the boundaries of the spans are identical.
     * @param goldSpan
     * @return
     */
    public boolean semiMatches(Span goldSpan) {
        return this.begin.position == goldSpan.begin.position || this.end.position == goldSpan.end.position;
    }


    /**
     * Filter all matching spans from the given collection
     * @param otherSpans
     * @return
     */
    public List<Span> matchingSpans(Collection<Span> otherSpans) {
        List<Span> matchingList = new ArrayList<>();
        for (Span goldSpan : otherSpans) {
            if (this.matches(goldSpan)) {
                matchingList.add(goldSpan);
            }
        }

        return matchingList;
    }

    /**
     * Filter all semi-matching spans from the given collection
     * @param goldSpans
     * @return
     */
    public List<Span> semiMatchingSpans(Collection<Span> goldSpans) {
        List<Span> matchingList = new ArrayList<>();
        for (Span goldSpan : goldSpans) {
            if (this.semiMatches(goldSpan)) {
                matchingList.add(goldSpan);
            }
        }

        return matchingList;
    }


    /**
     * Filter all spans in the list that have an overlap with this one
     * @param others
     * @return
     */
    public List<Span> overlappingSpans(Collection<Span> others) {
        List<Span> result = new ArrayList<>();
        for (Span other : others) {
            if (this.overlaps(other)) result.add(other);
        }

        return result;
    }

    /**
     * Returns true if the other span lies fully within the boundaries of this one, false otherwise
     * @param other
     * @return
     */
    public boolean contains(Span other) {
        return this.begin.position <= other.begin.position && this.end.position >= other.end.position;
    }

    /**
     * Checks whether this span overlaps another one
     * @param other
     * @return
     */
    public boolean overlaps(Span other) {
        if (this.begin.position >= other.begin.position && this.begin.position <= other.end.position) return true;
        if (this.end.position >= other.begin.position && this.end.position <= other.end.position) return true;
        if (this.end.position >= other.end.position && this.begin.position <= other.begin.position) return true;
        return false;
    }

    public double getScore() {
        return score;
    }


    public int length() {
        return end.position - begin.position + 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Span( ");
        for (int i = begin.position; i <= end.position; i++) {
            sb.append(tokenAt(i));
            if (i != end.position) sb.append(" ");
        }
        sb.append(")");

        return sb.toString();
    }


    /**
     * Checks whether there is a span beginning here
     * @param predictedSpans
     * @param position
     * @return
     */
    public static boolean anyBeginsAt(Collection<Span> predictedSpans, int position) {
        for (Span span : predictedSpans) {
            if (span.begin.position == position) return true;
        }
        return false;
    }

    /**
     * Checks whether there is a span ending here
     * @param predictedSpans
     * @param position
     * @return
     */
    public static boolean anyEndsAt(Collection<Span> predictedSpans, int position) {
        for (Span span : predictedSpans) {
            if (span.end.position == position) return true;
        }
        return false;
    }

    /**
     * Number of tokens shared between the two spans.
     * @param other
     * @return
     */
    public int computeOverlap(Span other) {
        int maxBegin = Math.max(this.begin.position, other.begin.position);
        int minEnd = Math.min(this.end.position, other.end.position);

        return Math.max(0, minEnd - maxBegin + 1);
    }

    /**
     * Determine the quotation type (direct, indirect, or mixed) of a span according to rules by Pareti (2015)
     * @return
     */
    public PARCAttribution.Type getType() {
        if (this.first().isQuote() && this.last().isQuote()) {
            return PARCAttribution.Type.DIRECT;
        } else {
            for (int i = begin.position; i <= end.position; i++) {
                if (tokenAt(i).isQuote()) return PARCAttribution.Type.MIXED;
            }
        }
        return PARCAttribution.Type.INDIRECT;
    }

    /**
     * Get all spans whose type is equal to the specified one
     * @param spans
     * @param type
     * @return
     */
    public static Set<Span> getSpansOfType(Collection<Span> spans, PARCAttribution.Type type) {
        Set<Span> result = new HashSet<>();
        for (Span span : spans) {
            if (span.getType() == type) result.add(span);
        }
        return result;
    }

    /**
     * Calculates the begin position of the span relative to its sentence
     * @return
     */
    public int getSentenceBeginPosition () {
        Token first = first();
        int offset = first.goldPosition - first.goldSentencePosition;

        return begin.position - offset;

    }

    /**
     * Calculates the end position of the span relative to its sentence
     * @return
     */
    public int getSentenceEndPosition () {
        Token first = first();
        int offset = first.goldPosition - first.goldSentencePosition;

        return end.position - offset;

    }


    /**
     * Returns the token at position i in the document. Note that we do not perform range checks here, so i can be
     * outside this span.
     * @param i
     * @return
     */
    public Token tokenAt(int i) {
        return document.tokenList.get(i);
    }

    public Token first() {
        return document.tokenList.get(begin.position);
    }

    public Token last() {
        return document.tokenList.get(end.position);
    }


    /**
     * Checks for equality. Two spans are equal if their positions match. Object equality is not required.
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        Span other = (Span) obj;

        return matches(other);
    }

    /**
     * Simple hash code computation. If you have documents with more than 10001 tokens, this could fail.
     * @return
     */
    @Override
    public int hashCode() {
        return 100001 * begin.position + end.position;
    }

}
