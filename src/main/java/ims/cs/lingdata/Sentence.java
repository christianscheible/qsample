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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import org.jgrapht.alg.FloydWarshallShortestPaths;

/**
 * Representation of a sentence.
 * Is part of a document; contains a list of tokens; may have a constituency and a dependency tree.
 */
public class Sentence  {

	public List<Token> tokenList;
	public GornAddressList gorn;
	public SentenceId sentenceId;
	public int positionInDocument;
	public Document document;
	
	// CoreLabel backwards lookup
	public Map<IndexedWord, Token> indexedWordLookup;
	public HashMap<Tree, Token> treeLookup;

	// CoreNLP output
	public Tree tree;
	public SemanticGraph dependencyGraph;
	public FloydWarshallShortestPaths fw;


	public Sentence () {}
	public Sentence (Document d) {
		document = d;
	}

	public List<Token> getTokenList() {
		return tokenList;
	}

	public Token first() { return tokenList.get(0); }
	public Token last() { return tokenList.get(tokenList.size()-1); }

	@Override
	public String toString() {
		return tokenList.toString();
	}
}
