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


package ims.cs.corenlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Partition;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

/**
 * Some helper functions for CoreNLP processing
 */
public abstract class Helper {
	

	/**
	 * Checks whether an internal token is a quote
	 * @param token
	 * @return
	 */
	public static boolean isQuote(Token token) {
		// single quotes are mostly wrong and unhelpful, so ignore!
		if (token.sentence.document.isCoreNlpProcessed) {
			return (token.predPosTag.equals("\"") || token.predPosTag.equals("``") ||
					token.predPosTag.equals("''") || token.predPosTag.equals("QUOT"))
					&& !token.predPosTag.equals("'") && !token.predPosTag.equals("`")
					&& !token.predText.equals("'") && !token.predText.equals("`") ;
		} else {
			return (token.goldPosTag.equals("\"") || token.goldPosTag.equals("``") ||
					token.goldPosTag.equals("''") || token.goldPosTag.equals("QUOT"))
					&& !token.goldPosTag.equals("'") && !token.goldPosTag.equals("`")
					&& !token.predText.equals("'") && !token.predText.equals("`");
		}
	}

	/**
	 * Checks whether a CoreNLP token is a quote
	 * @param token
	 * @return
	 */
	public static boolean isQuote(CoreLabel token) {
		// single quotes are mostly wrong and unhelpful, so ignore!!!
		return (token.tag().equals("\"") || token.tag().equals("``") ||
				token.tag().equals("''") || token.tag().equals("QUOT"))
				&& !token.tag().equals("'") && !token.tag().equals("`") ;
	}


	
	/**
	 * Checks whether the token sequence has an interior quotation mark (i.e., not including the first and last token)
	 * @param tokenBuffer
	 * @return
	 */
	public static boolean hasInnerQM(List<Token> tokenBuffer) {
		for (int i = 1; i<tokenBuffer.size()-1; i++) {
			Token token = tokenBuffer.get(i);
			if (Helper.isQuote(token)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Checks whether a token is punctuation
	 * @param word
	 * @return
	 */
	public static boolean isPunctuation(String word) {
		return word.length() == 1 && word.matches("[.,-;!?]");
	}

	
	/**
	 * get relation to parent in dependency graph
	 * @param token
	 * @return
	 */
	public static SemanticGraphEdge getDependencyParentRel(Token token) {
		IndexedWord indexedWord = token.dependencyBackpointer;
		Sentence sentence = token.sentence;
		
		
		IndexedWord parentWord = getDependencyParentIw(token);
		
		if (parentWord == null) {
			return null;
		} else {
			return sentence.dependencyGraph.getEdge(parentWord, indexedWord);
		}
	}
	
	/**
	 * Get list of all relations to children in the dependency graph
	 * @param token
	 * @return
	 */
	public static List<SemanticGraphEdge> getDependencyChildrenRels(Token token) {

		if (token == null) 
			return null;
		
		Sentence sentence = token.sentence;
		
		IndexedWord indexedWord = token.dependencyBackpointer;
		List<IndexedWord> children = getDependencyChildrenIw(token);

		if (children == null) return null;

		List<SemanticGraphEdge> relList = new ArrayList<SemanticGraphEdge>();

		for (IndexedWord child : children) {
			if (child != null) {
				relList.add(sentence.dependencyGraph.getEdge(indexedWord, child));				
			}
		}
		
		return relList;
	}


	/**
	 * Get parent indexed word in the CoreNLP dependency graph
	 * @param token
	 * @return
	 */
	private static IndexedWord getDependencyParentIw(Token token) {
		IndexedWord indexedWord = token.dependencyBackpointer;
		Sentence sentence = token.sentence;

		if (indexedWord == null) {
			return null;
		} else {
			IndexedWord iw = sentence.dependencyGraph.getParent(indexedWord);
			
			return iw;
		}
	}

	
	
	/**
	 * Get parent in dependency tree
	 * @param token
	 * @return
	 */
	public static Token getDependencyParent(Token token) {
		
		Sentence sentence = token.sentence;

		Map<IndexedWord, Token> lookupMap = sentence.indexedWordLookup;
		IndexedWord iw = getDependencyParentIw(token);
			
		return lookupMap.get(iw);
	}


	/**
	 * Get child indexed word in the CoreNLP dependency graph
	 * @param token
	 * @return
	 */
	public static List<IndexedWord> getDependencyChildrenIw (Token token) {
		
		Sentence sentence = token.sentence;
		IndexedWord indexedWord = token.dependencyBackpointer;
		
		if (indexedWord == null) {
			return null;
		} else {
			List<IndexedWord> iwChildList =  sentence.dependencyGraph.getChildList(indexedWord);

			
			return iwChildList;
		}
	}

	
	/** 
	 * Get list of children in dependency tree
	 * @param token
	 * @return
	 */
	public static List<Token> getDependencyChildren (Token token) {
		
		List<IndexedWord> iwChildList = getDependencyChildrenIw(token);
		
		if (iwChildList == null) {
			return null;
		}
		
		Sentence sentence = token.sentence;

		Map<IndexedWord, Token> lookupMap = sentence.indexedWordLookup;

		List<Token> clList = new ArrayList<Token>(iwChildList.size());
			
		for (IndexedWord c : iwChildList) {
			Token childToken = lookupMap.get(c);

			clList.add(childToken);
		}
			
		return clList;
	}

	/**
	 * Remove begin and end distinction for quotation marks that CoreNLP predicts
	 * @param tok
	 */
	public static void flattenQuotes(CoreLabel tok) {
		String word = tok.word();
		
		if ((word.equals("``") || word.equals("''"))) {
			tok.set(ValueAnnotation.class, "\"");
			tok.set(TextAnnotation.class, "\"");
			tok.set(LemmaAnnotation.class, "\"");
		} 
	}
	
	
	/**
	 * String representation of context. Positive n goes right, negative n goes left.
	 * @param cl
	 * @param n
	 * @return
	 */
	public static String contextString (Token cl, int n) {
		String sb = "";
		
		boolean reverse = false;
		
		if (n < 0) {
			n = -n;
			while (n > 0) {
				n--;
				cl =  cl.previousToken;
				sb = cl + " " + sb;
				if (cl == null) break;
			}		
			
		} else if (n > 0) {
			while (n > 0) {
				n--;
				cl = cl.nextToken;
				sb = sb + " " + cl;
				if (cl == null) break;
			}		
		
		} else {
			return "";
		}

		return sb.toString();
	}

	/**
	 * Returns the first n documents from a partition
	 * @param n
	 * @param partition
	 * @param offsetMultFromBegin
	 * @return
	 */
	public static Partition firstNDocs(int n, Partition partition, int offsetMultFromBegin) {
		List<Document> allDocs = partition.docList;
		
		List<Document> firstNDocs = allDocs.subList(offsetMultFromBegin * n, n * (offsetMultFromBegin+1));
		
		return new Partition(firstNDocs);
	}

	/**
	 * Returns the last n documents from a partition
	 * @param n
	 * @param partition
	 * @param offsetMultFromEnd
	 * @return
	 */
	public static Partition lastNDocs(int n, Partition partition, int offsetMultFromEnd) {
		List<Document> allDocs = partition.docList;
		
		List<Document> lastNDocs = allDocs.subList(allDocs.size()-((offsetMultFromEnd + 1) * n), allDocs.size() - (n * offsetMultFromEnd));
		
		return new Partition(lastNDocs);
	}

	/**
	 * Returns the required section from a partition
	 * @param sectionN
	 * @param partition
	 * @return
	 */
	public static Partition getSection(int sectionN, Partition partition) {
		String sectionStr = String.format("%02d", sectionN);
		List<Document> docs = partition.sectionMap.get(sectionStr);
		return new Partition(docs);
	}


	/**
	 * Sample n documents from a partition, drawing the documents by modulo intervals
	 * @param n
	 * @param partition
	 * @param offsetMultFromBegin
	 * @return
	 */
	public static Partition downSampleInterval(int n, Partition partition, int offsetMultFromBegin) {
		List<Document> sample = new ArrayList<Document>();
		List<Document> allDocs = partition.getDocumentList();
		
		int mod = allDocs.size()/n;
		
		int i = 0;
		for (Document doc: allDocs) {
			if ((i + offsetMultFromBegin) % mod == 0)
				sample.add(doc);
			i++;
		}

		return new Partition(sample);
	}


	/**
	 * Concatenates arbitrarily many partitions
	 * @param corpora
	 * @return
	 */
	public static Partition concatDocumentLists(Partition ... corpora) {
		int len = 0;
		for (int i = 0; i < corpora.length; i++) len += corpora[i].size();
		
		List<Document> result = new ArrayList<Document>(len);
		for (int i = 0; i < corpora.length; i++) result.addAll(corpora[i].docList);

		return new Partition(result);
	}


	/**
	 * Filters a partition by genres
	 * @param partition
	 * @param genreArray
	 * @return
	 */
	public static Partition filterByGenre(Partition partition, String[] genreArray) {
		List<Document> allDocs = partition.docList;
		List<Document> genreDocs = new ArrayList<Document>();

		for (Document doc: allDocs) { 
			for (String genre: genreArray) {
				if (doc.genre.name().contains(genre)) {
					genreDocs.add(doc);
					break;
				}
			}
		}

		return new Partition(genreDocs);
	}

	/**
	 * Translates some Stanford NER labels into BBN-compatible labels
	 * @param stanfordNer
	 * @return
	 */
	public static String translateNer(String stanfordNer) {
		if (stanfordNer.startsWith("PER")) return "PERSON";
		else if (stanfordNer.startsWith("ORG")) return "ORGANIZATION";
		else return stanfordNer;
	}

}
