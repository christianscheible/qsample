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


package ims.cs.qsample.features.components;

import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.corenlp.Helper;
import ims.cs.qsample.features.FeatureSet;
import ims.cs.util.StaticConfig;

/**
 * Add sentence-level indicator features to each token
 */
public abstract class SentenceIndicatorFeatures {

	// feature names
	private static final String QUOT_PREFIX = "SENT:QUOT";
	private static final String NE_PREFIX = "SENT:NE";
	private static final String PRO_PREFIX = "SENT:PRO";
	private static final String SL_PREFIX = "SL=";
	private static final String SL_LT_PREFIX = "SL<=";
	private static final String SL_GT_PREFIX = "SL>=";
	private static final String SL_EXACT_PREFIX = "SL-EXACT-BIN=";
	private static final String SENT_BEGIN_WINDOW = "SENT-BEGIN-WIN";
	private static final String SENT_END_WINDOW = "SENT-END-WIN";
	private static final String INVERT_PREFIX = "NOT:";

	/**
	 * Extract indicator features for all tokens in this sentence
	 * @param sentence
	 */
	public static void extract (Sentence sentence) {
		// pre-compute features
		boolean sentenceHasQuotFeature = sentenceHasQuotationMark(sentence);
		boolean sentenceHasProFeature = sentenceHasPro(sentence);
		boolean sentenceHasNeFeature = sentenceHasNe(sentence);
		int sentenceLength = sentence.tokenList.size();

		// distance to sentence boundaries
		sentenceBoundDistance(sentence);

		// now add pre-computed features to token list
		for (Token mToken : sentence.tokenList) {
			if (StaticConfig.sentenceHasQuote) addFeaturePositiveAndNegative(QUOT_PREFIX, sentenceHasQuotFeature, mToken);
			if (StaticConfig.sentenceHasPronoun) addFeaturePositiveAndNegative(PRO_PREFIX, sentenceHasProFeature, mToken);

			if (StaticConfig.sentenceHasNe) addFeaturePositiveAndNegative(NE_PREFIX, sentenceHasNeFeature, mToken);
			if (StaticConfig.sentenceLength) {
				addLengthLogBinHeuristic(mToken, sentenceLength);
				mToken.boundaryFeatureSet.add(SL_PREFIX + sentenceLength);
			}
		}
	}

	/**
	 * Add positive or negative version of a feature (i.e., also explicitly mark the absence of a feature)
	 * @param featureName
	 * @param featureOn
	 * @param token
	 */
	public static void addFeaturePositiveAndNegative(String featureName, boolean featureOn, Token token) {
		if (featureOn)
			token.boundaryFeatureSet.add(featureName);
		else
			token.boundaryFeatureSet.add(INVERT_PREFIX + featureName);
	}

	/**
	 * Binning for lengths, exponential bin spacing
	 * @param pToken
	 * @param length
	 */
	private static void addLengthLogBinHeuristic(Token pToken, int length) {
		if (!StaticConfig.sentenceLengthBinning) return;
		
		FeatureSet fs = pToken.boundaryFeatureSet;
		
		int[] bins = new int[] {0, 2, 4, 8, 16, 32, 64, 1000};
		
		for (int i=0; i < bins.length - 1; i++) {
			int threshLower = bins[i];
			int threshUpper = bins[i+1];
			
			if (length <= threshUpper) {
				if (StaticConfig.sentenceLengthBinningStacked) {
					fs.add(SL_LT_PREFIX + "STACKED-" + threshLower);
				} else if (length > threshLower) {
					fs.add(SL_EXACT_PREFIX + threshLower);
				}
			} 
			
			if ((length >= threshLower) && StaticConfig.sentenceLengthBinningStacked) {
				fs.add(SL_GT_PREFIX + threshLower);
			}
		}
		
	}

	/**
	 * Add features about the distance of each token to the sentence boundary
	 * @param sentence
	 */
	private static void sentenceBoundDistance(Sentence sentence) {
		int pos = 0;
		int sl = sentence.tokenList.size();

		for (Token token : sentence.tokenList) {
			// compute distance to end
			int endDist = sl - pos - 1;

			// if distance to either boundary is within a window of 5, add respective feature
			if (pos < 5) token.boundaryFeatureSet.add(SENT_BEGIN_WINDOW);
			if (endDist < 5) token.boundaryFeatureSet.add(SENT_END_WINDOW);

			pos++;
		}
	}

	/**
	 * Determines whether a sentence contains a quotation mark
	 * @param sentence
	 * @return
	 */
	private static boolean sentenceHasQuotationMark(Sentence sentence) {
		for (Token token: sentence.tokenList) {
			if (Helper.isQuote(token)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a sentence contains a pronoun
	 * @param sentence
	 * @return
	 */
	private static boolean sentenceHasPro(Sentence sentence) {
		for (Token token: sentence.tokenList) {
			if (token.predPosTag.startsWith("PR")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a sentence contains a named entity
	 * @param sentence
	 * @return
	 */
	private static boolean sentenceHasNe(Sentence sentence) {
		for (Token token: sentence.tokenList) {
			if ((token.predNer.startsWith("PERSON")) || (token.predNer.startsWith("ORGANIZATION"))) {
				return true;
			}
		}
		return false;
	}

	


}
