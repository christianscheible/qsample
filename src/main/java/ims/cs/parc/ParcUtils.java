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


package ims.cs.parc;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import ims.cs.corenlp.Helper;
import ims.cs.lingdata.ByteCount;
import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import edu.stanford.nlp.trees.Tree;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Iterator;
import java.util.List;

/**
 * Collection of Utility functions
 */
public abstract class ParcUtils {

	/**
	 * Find all head verbs in the corpus. The algorithm is taken from Pareti (2015).
	 * @param sentence
	 */
	public static void markHeadVerbs (Sentence sentence) {

		for (Tree tree : sentence.tree.preOrderNodeList()) {
			if (tree.label().value().equals("VP")) {
				boolean valid = true;
				for (Tree child : tree.children()) {
					if (child.label().value().equals("VP")) {
						valid = false;
						break;
					}
				}

				if (valid) {
					for (Tree child : tree.children()) {
						if (child.firstChild().isLeaf() && child.label().value().startsWith("V")) {
							Token token = sentence.treeLookup.get(child.firstChild());
							if (token != null)
								token.isHeadVerb = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Annotates paragraph-continuing quotation marks. doParagraphAnnotation() needs to be called before this.
	 * @param document
	 */
	public static void markParagraphQuotes(Document document) {
		int quoteIndex = 1;

		for (Token token: document.tokenList) {
			if (Helper.isQuote(token)) {
				// ignore even quotes at paragraph begins
				if (token.paragraphBegins && quoteIndex % 2 == 0)
					token.ignoreQuote = true;
				else
					quoteIndex++;
			}
		}
	}

	/**
	 * Annotates for each token whether it starts a paragraph by its raw text
	 * @param document
	 */
	public static void doParagraphAnnotation (Document document) {
		String documentText = document.text;
		Iterator<Token> tokenIter = document.tokenList.iterator();

		Token token = tokenIter.next();
		ByteCount bc = token.goldByteCount;

		// iterate over all character positions in the text
		char prevC = 0;

	    for (int i = 0; i < documentText.length(); i++) {
	    	if (i > bc.getEnd()) {
	    		if (!tokenIter.hasNext()) break;   /* reached the last token */

	    		token = tokenIter.next();
	    		bc = token.goldByteCount;
	    	}

	    	char c = documentText.charAt(i);

			// two consecutive newlines indicate a paragraph
	    	if (prevC == '\n' &&  c == '\n') {
	    		token.paragraphBegins = true;
	    	}

	    	prevC = c;
	    }
	}

	/**
	 * Anonymizes certain named entities in the text
	 * @param document
	 */
	public static void anonymizeNamedEntities (Document document) {
		for (Token token: document.getTokenList()) {
			if (token.predNer.startsWith("ORGANIZATION") || token.predNer.startsWith("PERSON")) {
				String substText = "[NE]";

				token.predLemma = substText;
				token.predText = substText;
				token.goldLemma = substText;
				token.goldText = substText;
			}
		}
	}

	/**
	 * CoreNLP tries to predict opening and closing quotation marks.
	 * This method maps the variation back to one symbol.
	 * @param document
	 */
	public static void sanitizeQuotationMarks (Document document) {
		for (Token token : document.getTokenList()) {
			// double quotes
			if (token.predLemma.equals("``") || token.predLemma.equals("\"") || token.predLemma.equals("''")) {
				token.predLemma = "\"";
				token.predPosTag = "\"";
				token.predText = "\"";
				token.goldPosTag = "\"";
				token.goldLemma = "\"";
				token.goldText = "\"";
			}

			// single quotes
			if (token.predLemma.equals("`") || token.predLemma.equals("''")) {
				token.predLemma = "'";
				token.predPosTag = "'";
				token.predText = "'";
				token.goldLemma = "'";
				token.goldPosTag = "'";
				token.goldText = "'";
			}

		}
	}

	/**
	 * The FW implementation needs distinct objects as edges, which this class accomplishes.
	 * CoreNLP seems to optimize storage by caching strings, so different edges have identical label strings.
	 */
	public static class IndexedEdge {
		public GrammaticalRelation rel;
		public int index;

		public IndexedEdge(GrammaticalRelation rel, int index) {
			this.rel = rel;
			this.index = index;
		}
	}

	/**
	 * Compute cached dependency paths using Floyd Warshall
	 * @param dependencies
	 * @return
	 */
	public static FloydWarshallShortestPaths computeFloydWarshallSGE(List<SemanticGraphEdge> dependencies) {
		SimpleDirectedGraph<IndexedWord, IndexedEdge> graph = new SimpleDirectedGraph<IndexedWord, IndexedEdge>(IndexedEdge.class);
		int edgeId = 0;
		for (SemanticGraphEdge dep : dependencies) {
			graph.addVertex(dep.getGovernor());
			graph.addVertex(dep.getDependent());
			graph.addEdge(dep.getGovernor(), dep.getDependent(), new IndexedEdge(dep.getRelation(), edgeId));
		}
		return new FloydWarshallShortestPaths(graph);
	}

}
