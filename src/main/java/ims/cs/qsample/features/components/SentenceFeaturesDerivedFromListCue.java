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
import ims.cs.util.StaticConfig;

import java.util.LinkedList;
import java.util.List;

/**
 * Token features based on cue information from the noun cue list.
 */
public abstract class SentenceFeaturesDerivedFromListCue {

	private static final String CUE_DEP_PREFIX = "CUE-DEP:NOUNCUE";
	private static final String CUE_PREFIX = "SENT:HASCUE:NOUNCUE";

	/**
	 * Extract features for all tokens in the sentence
	 * @param sentence
	 */
	public static void extract (Sentence sentence) {
		boolean sentenceHasCueFeature = sentenceHasCue(sentence.tokenList);

		// check each token for noun-cue-ness, push features to its dependents (transitively)
		for (Token pToken : sentence.tokenList) {
			if (StaticConfig.dependencyCueDependent) {
				// token is in noun cue list
				if (pToken.boundaryFeatureSet.contains("NOUNCUELIST"))
					addCueDependentFeature("LIST", pToken, sentence);

				// token is "according to"
				if (pToken.predText.toLowerCase().equals("according")
						&& pToken.nextToken != null
						&& pToken.nextToken.predText.equals("to"))
					addCueDependentFeature("ACCORDINGTO", pToken, sentence);
			}

			SentenceIndicatorFeatures.addFeaturePositiveAndNegative(CUE_PREFIX, sentenceHasCueFeature, pToken);
		}
	}

	/**
	 * Push features to all dependents of a cue
	 * @param type
	 * @param token
	 * @param sentence
	 */
	private static void addCueDependentFeature(String type, Token token, Sentence sentence) {
		List<Token> stack = new LinkedList<Token>();
		stack.add(token);

		// recursively iterate over all children (and their children ...)
		while (stack.size() > 0) {
			Token current = stack.remove(0);
			current.boundaryFeatureSet.add(CUE_DEP_PREFIX + "-" + type);
			
			List<Token> children = Helper.getDependencyChildren(current);
			
			if (children == null) continue;

			for (Token c : children) {
				if (c != null)	stack.add(c);
			}
		}
	}

	/**
	 * Check whether the sentence has any noun cues
	 * @param data
	 * @return
	 */
	private static boolean sentenceHasCue(List<Token> data) {
		for (Token token: data) {
			if (token.boundaryFeatureSet.contains("NOUNCUELIST")) {
				return true;
			}
		}
		return false;
	}

}
