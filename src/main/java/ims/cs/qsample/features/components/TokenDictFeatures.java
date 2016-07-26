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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Feature extractor that extracts information about a token from a dictionary (read from a tab-separated file)
 */
public class TokenDictFeatures {

	private String featureName = "VERBDICT";
	private String listFileName;
	private Map<String, Set<String>> wordMap;
	public String posStart = null;


	/**
	 * Set up the feature extractor
	 * @param listFileName name of the dictionary file (tab-separated)
	 * @param featureName name of the feature that will be extracted
	 * @throws IOException
	 */
	public TokenDictFeatures(String listFileName, String featureName) throws IOException {
		this.listFileName = listFileName;
		this.featureName = featureName;
		loadDictionary();
	}

	/**
	 * Extract dictionary information for the token t
	 * @param t
	 */
	public void extract(Token t) {
		// check if the token's lemma is in the dictionary
		if (wordMap.containsKey(t.predLemma)) {
			// check for POS restriction if necessary
			if (posStart == null || t.predPosTag.startsWith(posStart)) {
				for (String vclass : wordMap.get(t.predLemma))
					t.boundaryFeatureSet.add(featureName + "=" + vclass);
			}
		}
	}

	/**
	 * Load dictionary from a tab-separated file
	 * @throws IOException
	 */
	private void loadDictionary() throws IOException {
		wordMap = new HashMap<>();
		
	    BufferedReader br = new BufferedReader(new FileReader(listFileName));
	    String line;
	    
	    while ((line = br.readLine()) != null) {
	    	line = line.trim();
			String[] tokens = line.split("\\s+");
			String word = tokens[0];
			String wordClass = tokens[1];

			if (!wordMap.containsKey(word)) {
				wordMap.put(word, new HashSet<String>());
			}

	    	wordMap.get(word).add(wordClass);
	    }
	    
	    br.close();

	}

	
}
