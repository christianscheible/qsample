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

import ims.cs.lingdata.Token;
import ims.cs.qsample.features.FeatureSet;
import ims.cs.util.StaticConfig;

/**
 * Extracts lexical information about a token (e.g., word, lemma, POS)
 */
public abstract class TokenLexicalFeatures {

	private static final String TOK_PREFIX = "TOK";
	private static final String LEMMA_PREFIX = "LEMMA";
	private static final String POS_PREFIX = "POS";
	private static final String BG_PREFIX = "BG";
	private static final String NE_PREFIX = "NE";
	private static final String PARBEGIN_PREFIX = "PAR-BEGINS";
	private static final String PAREND_PREFIX = "PAR-ENDS";


	/**
	 * Extract lexical features about a single token t
	 * @param t
	 */
	public static void extract(Token t) {

		if (StaticConfig.lexicalPos ||
				StaticConfig.lexicalLemma ||
				StaticConfig.lexicalToken)
			addWindowFeatures(t);

		if (StaticConfig.lexicalBigram) addBigramFeature(t);
		addNeFeature(t);
		addDocStructureFeature(t);
	}

	/**
	 * Adds paragraph begin and end features
	 * @param token
	 */
	private static void addDocStructureFeature(Token token) {
		if (token.paragraphBegins) token.boundaryFeatureSet.add(PARBEGIN_PREFIX);
		if (token.nextToken == null || token.nextToken.paragraphBegins) token.boundaryFeatureSet.add(PAREND_PREFIX);
	}

	/**
	 * Adds features about whether the token is part of a named entity
	 * @param token
	 */
	private static void addNeFeature(Token token) {
		if (!token.predNer.equals("?") && !token.predNer.equals("O")) {
			token.boundaryFeatureSet.add(NE_PREFIX+"-IS-NE");
			token.boundaryFeatureSet.add(NE_PREFIX+"-IS-NE-" + token.predNer);
		}
	}

	/**
	 * Adds bigram features with the previous and next token
	 * @param token
	 */
	private static void addBigramFeature(Token token) {
		String prevWordForm;
		String prevLemma;

		String nextWordForm;
		String nextLemma;

		// find previous token
		if (token.previousToken == null) {
			prevWordForm = "null";
			prevLemma = "null";
		} else {
			Token prevToken = token.previousToken;
			prevWordForm = prevToken.predText;
			prevLemma = prevToken.predLemma;
		}

		// find next token
		if (token.nextToken == null) {
			nextWordForm = "null";
			nextLemma = "null";
		} else {
			Token nextToken = token.nextToken;
			nextWordForm = nextToken.predText;
			nextLemma = nextToken.predLemma;
		}

		// add features of word and lemma bigrams
		FeatureSet fs = token.boundaryFeatureSet;

		fs.add(BG_PREFIX + prevWordForm + "<--" + token.predText);
		fs.add(BG_PREFIX + "(LEMMA)" + prevLemma + "<--" + token.predLemma);

		fs.add(BG_PREFIX + nextWordForm + "-->" + token.predText);
		fs.add(BG_PREFIX + "(LEMMA)" + nextLemma + "-->" + token.predLemma);
	}


	/**
	 * Adds features from other tokens within a window
	 * @param pToken
	 */
	private static void addWindowFeatures(Token pToken) {
		// current POS tag
		FeatureSet fs = pToken.boundaryFeatureSet;
		
		if (StaticConfig.lexicalPos)   fs.add(POS_PREFIX + "-0=" + pToken.predPosTag);
		if (StaticConfig.lexicalToken) fs.add(TOK_PREFIX + "-0=" + pToken.predText);
		if (StaticConfig.lexicalLemma) fs.add(LEMMA_PREFIX + "-0=" + pToken.predLemma);

		
		// previous tokens
		Token currentToken = pToken;
		for (int i = 1; i <= StaticConfig.lexicalWindowSize; i++) {
			String leftPos;
			String leftTok;
			String leftLemma;

			Token prevToken = currentToken.previousToken;
			if (prevToken != null) {
				leftPos = prevToken.predPosTag;
				leftTok = prevToken.predText;
				leftLemma = prevToken.predLemma;
				currentToken = prevToken;
			} else {
				leftPos = "NONE";
				leftLemma = "NONE";
				leftTok = "NONE";
			}

			if (StaticConfig.lexicalPos)	fs.add("WIN_" + POS_PREFIX + "-" + i + "=" + leftPos);
			if (StaticConfig.lexicalToken) fs.add("WIN_" + TOK_PREFIX + "-" + i + "=" + leftTok);
			if (StaticConfig.lexicalLemma) fs.add("WIN_" + LEMMA_PREFIX + "-" + i + "=" + leftLemma);
		}

		// subsequent tokens
		currentToken = pToken;
		for (int i = 1; i <= StaticConfig.lexicalWindowSize; i++) {
			String rightPos;
			String rightTok;
			String rightLemma;

			Token nextToken = currentToken.nextToken;
			if (nextToken != null) {
				rightPos = nextToken.predPosTag;
				rightTok = nextToken.predText;
				rightLemma = nextToken.predLemma;
				currentToken = nextToken;

			} else {
				rightPos = "NONE";
				rightLemma = "NONE";
				rightTok = "NONE";
				
			}

			if (StaticConfig.lexicalPos)   fs.add("WIN_" + POS_PREFIX + "+" + i + "=" + rightPos);
			if (StaticConfig.lexicalToken) fs.add("WIN_" + TOK_PREFIX + "+" + i + "=" + rightTok);
			if (StaticConfig.lexicalLemma) fs.add("WIN_" + LEMMA_PREFIX + "+" + i + "=" + rightLemma);

		}
	}


	
}
