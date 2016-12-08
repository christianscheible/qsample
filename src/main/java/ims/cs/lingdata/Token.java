
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


package ims.cs.lingdata;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ims.cs.parc.PARCAttribution;
import ims.cs.corenlp.Helper;
import ims.cs.qsample.features.FeatureSet;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import ims.cs.qsample.spans.Span;

/**
 * Representation of a token.
 * Tokens hold lexical information, positional information, and references to their representations in parse trees.
 */
public class Token {
	public enum Property {POS, TEXT, LEMMA, GOLDLABEL};
	

	// gold information
	public String goldLemma;
	public String goldText;
	public String goldPosTag;
	public String goldNer;
	public int goldPosition;
	public int goldSentencePosition;
	public ByteCount goldByteCount;


	// predicted information
	public String predLemma;
	public String predText;
	public String originalPredText;
	public String predPosTag;
	public int predPosition;
	public int predSentencePosition;
	public String predNer;
	public boolean isPredictedCue;
	public ByteCount predByteCount;

	// features
	public FeatureSet featureSet;
	public FeatureSet boundaryFeatureSet;

	
	// structural properties
	public Sentence sentence;
	public boolean paragraphBegins;
	public boolean ignoreQuote;
	public Token previousToken;
	public Token nextToken;
	public boolean isHeadVerb;
	
	// PARC annotations
	public List<PARCAttribution> attributionList;

	// BIO data
	public Map<String,String> contentBIOAnnotationPred;
	public String contentBIOAnnotationGold;

	// STOP annotations
	public String stopFineAnnotation;
	
	// CoreNlp compatibility
	public IndexedWord dependencyBackpointer;
	public Tree treeBackpointer;
	public TreeGraphNode tgn;

	// perceptron stuff
	public double perceptronBeginScore;
	public double perceptronEndScore;
	public double perceptronCueScore;

	// stats
	public int numTimesSampledBegin;
	public int numTimesSampledEnd;
	public int numTimesSampledCue;

	
	// Some basic property stuff, emulating CoreMap behavior.
	// This is handy because we can use variables to refer to properties of the token
	public String getProperty(Property p) {
		switch (p) {
		case POS:
			return predPosTag;
		case LEMMA:
			return predLemma;
		case TEXT:
			return predText;
		case GOLDLABEL:
			return contentBIOAnnotationGold;
		default:
			return null;
		}
	}

	public Token() {}

	// crude copy constructor
	public Token(Token other) {
		goldByteCount = other.goldByteCount;
		goldLemma = other.goldLemma;
		goldText = other.goldText;
		goldPosTag = other.goldPosTag;
		goldPosition = other.goldPosition;
		goldSentencePosition = other.goldSentencePosition;
		predLemma = other.predLemma;
		predText = other.predText;
		predPosTag = other.predPosTag;
		predPosition = other.predPosition;
		predSentencePosition = other.predSentencePosition;
		predNer = other.predNer;
		featureSet = other.featureSet;
		sentence = other.sentence;
		paragraphBegins = other.paragraphBegins;
		ignoreQuote = other.ignoreQuote;
		previousToken = other.previousToken;
		nextToken = other.nextToken;
		attributionList = other.attributionList;
		contentBIOAnnotationPred = other.contentBIOAnnotationPred;
		contentBIOAnnotationGold = other.contentBIOAnnotationGold;
		stopFineAnnotation = other.stopFineAnnotation;
		dependencyBackpointer = other.dependencyBackpointer;
		treeBackpointer = other.treeBackpointer;
	}
	
	@Override
	public String toString() {
		return this.predText + "(G:" + this.goldText + ")@" + this.predPosition;
	}

	/**
	 * String representation of the token in a context of n neighboring tokens.
	 * @param n
	 * @return
	 */
	public String getContextString(int n) {
		StringBuilder sb = new StringBuilder();
		Token currentWord = this;
		int rollback = 0;
		
		for (int i = 0; i < n; i++) {
			Token prevWord = currentWord.previousToken;
			if (prevWord != null) {
				currentWord = prevWord;
			} else {
				break;
			}
			rollback ++;
		}
		
		for (int i = 0; i < rollback + n; i++) {
			sb.append(currentWord);
			sb.append(" ");
			currentWord = currentWord.nextToken;
			if (currentWord == null)
				break;
		}

		return sb.toString();
	}

	/**
	 * Print token in context of 5 (for debugging purposes)
	 */
	public void printContext() {
		printContext(5);
	}

	/**
	 * Print token in context (for debugging purposes)
	 */
	public void printContext(int n) {
		
		System.out.println(getContextString(n));
	}

	/**
	 * Returns the first attribution of the requested role associated with the token
	 * @param role
	 * @return
	 */
	public PARCAttribution getAttributionWithRole(PARCAttribution.Role role) {
		if (attributionList != null) {
			for (PARCAttribution att : attributionList) {
				if (att.role == role) {
					return att;
				}
			}
		}
		return null;
	}


	/**
	 * Determines whether a constituent starts at this token
	 * @return
	 */
	public boolean hasStartingConstituents() {
		Tree tree = sentence.tree;
		Set<Constituent> constituents = tree.constituents();

		for (Constituent c : constituents) {
			if (c.end() - c.start() > 1 &&
					c.start() == predSentencePosition)
				return true;
		}

		return false;
	}

	/**
	 * Determines whether a constituent ends at this token
	 * @return
	 */
	public boolean hasEndingConstituents() {
		Tree tree = sentence.tree;
		Set<Constituent> constituents = tree.constituents();


		for (Constituent c : constituents) {
			if (c.end() - c.start() > 1 &&
					c.end() == predSentencePosition)
				return true;
		}

		return false;
	}


	/**
	 * Determines if the token has a Cue annotation
	 * @return
	 */
	public boolean isGoldCue() {
		boolean hasCue = getAttributionWithRole(PARCAttribution.Role.CUE) != null;
		return hasCue && isHeadVerb;
	}

	/**
	 * Determines whether the token is part of any predicted content span
	 * @return
	 */
	public boolean isInPredictedContentSpan() {
		for (Span span : sentence.document.predictedSpanSet) {
			if (span.begin.position <= this.predPosition &&
					span.end.position >= this.predPosition)
				return true;
		}
		return false;
	}


	/**
	 * Determines whether the the token is the first in any gold content span
	 * @return
	 */
	public boolean startsGoldContentSpan() {
		for (Span span : sentence.document.goldSpanSet) {
			if (span.begin.position == this.predPosition) return true;
		}
		return false;
	}


	/**
	 * Determines whether the the token is the last in any gold content span
	 * @return
	 */
	public boolean endsGoldContentSpan() {
		for (Span span : sentence.document.goldSpanSet) {
			if (span.end.position == this.predPosition) return true;
		}
		return false;
	}

	/**
	 * Determines whether the the token is the first in any predicted content span
	 * @return
	 */
	public boolean startsPredictedContentSpan() {
		for (Span span : sentence.document.predictedSpanSet) {
			if (span.begin.position == this.predPosition) return true;
		}
		return false;
	}

	/**
	 * Determines whether the the token is the last in any predicted content span
	 * @return
	 */
	public boolean endsPredictedContentSpan() {
		for (Span span : sentence.document.predictedSpanSet) {
			if (span.end.position == this.predPosition) return true;
		}
		return false;
	}

	public boolean endsSentence() {
		return sentence.last() == this;
	}


	/**
	 * Determines whether the token is a quotation mark
	 * @return
	 */
	public boolean isQuote() {
		return Helper.isQuote(this);
	}

	/**
	 * Determines whether the token starts with a lowercase symbol (i.e., whether it is not title case)
	 * @return
	 */
	public boolean predTextIsLower() {
		return Character.isLowerCase(predText.charAt(0));
	}

	/**
	 * Returns the final characters of the (predicted) text
	 * @param len length of the suffix
	 * @return
	 */
	public String suffix(int len) {
		if (predText.length() >= len) {
			return predText.substring(predText.length() - len);
		} else {
			return predText;
		}
	}

}
