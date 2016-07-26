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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.qsample.features.FeatureSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Offset conjunction over a selection of features.
 * Idea here: enumerate all possible patterns of feature conjunctions. Then test for each feature set whether it
 * contains each of the conjunctions. If so, add the conjunction.
 */
public class DocumentOffsetConjunction {

	// features subject to conjunction
	private static final String[] features = new String[] {"SENT:QUOT", "SENT:NE", "SENT:PRO", "SENT:HASCUE", "CUE-DEP", "IS-LEFTMOST", "SENT-BEGIN-WIN", "SENT-END-WIN"};

	private List<String[]> patternList;
	

	public DocumentOffsetConjunction() {
		patternList = new LinkedList<>();
		
		// add empty entry to start
		patternList.add(new String[] {});
		
		
		for (String s : features) {
			List<String[]> newPatterns = new LinkedList<String[]>();
			for (String[] pattern : patternList) {
				String[] concat = append(pattern, s);
				newPatterns.add(concat);
			}
			
			patternList.addAll(newPatterns);
		}
		
		// remove the empty entry
		patternList.remove(0);
	}

	/**
	 * Add feature conjunctions to all tokens in the document
	 * @param document
	 */
	public void extract (Document document) {
		List<Token> tokenList = document.getTokenList();
		
		for (Token token : tokenList) {
			FeatureSet fs = token.boundaryFeatureSet;

			// check for each pattern whether the feature set satisfies it
			for (String[] features : patternList) {
				boolean matches = true;
				for (String feature: features) {
					if (!fs.contains(feature)) {
						matches = false;
						break;
					}
				}

				// if the pattern is satisfied, add the conjunction
				if (matches) {
					fs.add("CONJUNCTION:" + StringUtils.join(",", features));
				}
			}
		}
	}
	
	
	public static String[] append (String[] a1, String s) {
		String[] ret = new String[a1.length + 1];
		System.arraycopy(a1, 0, ret, 0, a1.length);
		ret[ret.length-1] = s;
		return ret;
	}
	
	public void printPatterns() {
		for(String[] p : patternList) {
			System.out.println(Arrays.toString(p));
		}
	}


}
