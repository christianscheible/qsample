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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import ims.cs.lingdata.Token;


/**
 * Feature extractor that check whether a token is in a list (specified in a file)
 */
public class TokenListFeatures {

	private String featureName = "VERBLIST";
	private String listFileName;
	private Set<String> wordSet;
	private int window = 5;
	public String posStart = null;


	/**
	 * Set up the feature extractor
	 * @param listFileName list of words (one word per line)
	 * @param featureName
	 * @throws IOException
	 */
	public TokenListFeatures(String listFileName, String featureName) throws IOException {
		this.listFileName = listFileName;
		this.featureName = featureName;
		loadWordList();
	}

	/**
	 * Extract list feature for the token t
	 * @param t
	 */
	public void extract(Token t) {
		// current token
		if ((posStart == null || t.predPosTag.startsWith(posStart)) && wordSet.contains(t.predLemma)) {
				t.boundaryFeatureSet.add(featureName);
		}

		// window before the token
		Token prevToken = t;
		for (int i = 0; i < window; i++) {
			prevToken = prevToken.previousToken;
			if (prevToken == null) break;
			if (wordSet.contains(prevToken.predLemma)) {
				t.boundaryFeatureSet.add("WIN_-" + (i+1) + "-" + featureName);
			}
		}

		// window after the token
		Token nextToken = t;
		for (int i = 0; i < window; i++) {
			nextToken = nextToken.nextToken;
			if (nextToken == null) break;
			if (wordSet.contains(nextToken.predLemma)) {
				t.boundaryFeatureSet.add("WIN_+" + (i+1) + "-" + featureName);
			}
		}
	}

	/**
	 * Loads the word list (one word per line)
	 * @throws IOException
	 */
	private void loadWordList() throws IOException {
		wordSet = new HashSet<>();
		
	    BufferedReader br = new BufferedReader(new FileReader(listFileName));
	    String line;

	    while ((line = br.readLine()) != null) {
	    	line = line.trim();
	    	wordSet.add(line);
	    }
	    
	    br.close();

	}

	
}
