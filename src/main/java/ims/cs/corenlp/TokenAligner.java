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

import java.util.*;

import ims.cs.lingdata.ByteCount;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.lingdata.SentenceId;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import ims.cs.parc.ParcUtils;
import ims.cs.util.StaticConfig;

/**
 * Aligns the tokens of a sentence with their CoreNLP versions
 */
public class TokenAligner {

	enum SplitType {NONE_CORENLP, NONE_GOLD, SPLIT};
	SplitType splitType = SplitType.NONE_GOLD;

	private HashMap<IndexedWord, Token> indexedWord2CoreLabel;
	private HashMap<Tree, Token> tree2CoreLabel;
	private List<Token> pcTokenList;
	private CoreMap originalSentence;
	
	private boolean useCoreNlpQuoteCompletion = true;
	private List<Token> pTokens;


	public TokenAligner(List<Token> pTokens, CoreMap cSentence) {
		this.originalSentence = cSentence;
		this.pTokens = pTokens;
	}


	/**
	 * Returns the result of the alignment process.
	 * @return
	 */
	public Sentence getCombinedSentence() {
		alignTokensStrict(pTokens, originalSentence);

		if (pcTokenList.size() == 0) {
			return null;
		}
		
		Sentence combinedSentence = new Sentence();
		combinedSentence.tokenList = pcTokenList;
		
		
		for (Token token: pcTokenList) {
			token.sentence = combinedSentence;
		}
		
		// there is no true mapping between the sentences. we assign the id of the
		// first token for debugging purposes 
		SentenceId sentenceId = pcTokenList.get(0).sentence.sentenceId;
		combinedSentence.sentenceId = sentenceId;

		// add necessary CoreNLP information
		combinedSentence.indexedWordLookup = indexedWord2CoreLabel;
		combinedSentence.treeLookup = tree2CoreLabel;
		combinedSentence.tree = originalSentence.get(TreeAnnotation.class);
		combinedSentence.dependencyGraph = originalSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

		// compute all shortest paths between nodes in the sentence
		combinedSentence.fw = ParcUtils.computeFloydWarshallSGE(combinedSentence.dependencyGraph.edgeListSorted());

		return combinedSentence;
	}

	/**
	 * Add the newly generated token to the token list.
	 * @param combinedToken
	 * @param prevWord
	 */
	private void addNewWord(Token combinedToken, Token prevWord) {
		pcTokenList.add(combinedToken);

		// bookkeeping for CoreNLP information
		indexedWord2CoreLabel.put(combinedToken.dependencyBackpointer, combinedToken);
		tree2CoreLabel.put(combinedToken.treeBackpointer, combinedToken);

		// recovery for badly tokenized punctuation
		if (combinedToken.predText.length() == 0) {
			// this happens when the split is so messed up that a token becomes empty. happens with ellipsis.
			if (StaticConfig.verbose) System.out.println("Missing text, inserting lemma");
			combinedToken.predText = combinedToken.predLemma;
		}

		// link words together
		if (prevWord != null) {
			combinedToken.previousToken = prevWord;
			prevWord.nextToken = combinedToken;
		}
	}

	
	/**
	 * Splits a CoreNLP token based on a position. We split only the word form as we don't have sufficient information
	 * to split the lemma.
	 * @param token
	 * @param absPosition
	 * @return
	 */
	private CoreLabel[] splitToken (CoreLabel token, int absPosition) {
		String word = token.word();
		String origText = token.originalText();

		// initialize parts
		CoreLabel[] splitting = new CoreLabel[2];
		splitting[0] = new CoreLabel(token);
		splitting[1] = new CoreLabel(token);

		// calculate split position
		int relPosition = absPosition - token.beginPosition();
		

		// cut up original text
		if (origText.length() >= relPosition) {
			String origText1 = origText.substring(0, relPosition);
			String origText2 = origText.substring(relPosition);
			
			splitting[0].setOriginalText(origText1);
			splitting[1].setOriginalText(origText2);
		}

		// cut up predicted text
		if (word.length() >= relPosition) {
			String word1 = word.substring(0, relPosition);
			String word2 = word.substring(relPosition);
			
			splitting[0].setWord(word1);
			splitting[1].setWord(word2);
		}
		
		// we could do the same with POS and lemma, but that would be complicated ...
		splitting[0].setEndPosition(absPosition);     /* set a new end as we just shortened this token */
		splitting[1].setBeginPosition(absPosition);   /* set a new position as we just shortened this token */
		
		// copy lemmas
		splitting[0].setLemma(token.lemma());
		splitting[1].setLemma(token.lemma());

		return splitting;
	}

	/**
	 * Takes a PARC token and a CoreNLP token and combines them into a single token
	 * @param tok
	 * @param cl
	 * @param currentCoreNlpSentenceIndex
	 * @return
	 */
	public Token combineTokens(Token tok, CoreLabel cl, int currentCoreNlpSentenceIndex) {
		// check whether we use gold information or CoreNLP information
		if (StaticConfig.useGoldPreprocessing) {
			return combineTokensGold(tok, cl, currentCoreNlpSentenceIndex);
		} else {
			return combineTokensPred(tok, cl, currentCoreNlpSentenceIndex);
		}
	}

	/**
	 * Combines my token and a CoreNlp token using predicted information
	 * @param tok
	 * @param cl
	 * @param currentCoreNlpSentenceIndex
	 * @return
	 */
	public static Token combineTokensPred(Token tok, CoreLabel cl, int currentCoreNlpSentenceIndex) {
		Token combined = new Token(tok);
		combined.predText = cl.word();
		combined.predLemma = cl.lemma();
		combined.predPosition = -1;   /* will be determined by document aligner */
		combined.predPosTag = cl.tag();
		combined.predSentencePosition = currentCoreNlpSentenceIndex;
		combined.predNer = Helper.translateNer(cl.ner());
		combined.predByteCount = new ByteCount(cl.beginPosition(), cl.endPosition());
		return combined;
	}

	/**
	 * Combines my token and a CoreNlp token using gold tokens, lemmas, and POS tags (as in Pareti et al.)
	 * @param tok
	 * @param cl
	 * @param currentCoreNlpSentenceIndex
	 * @return
	 */
	public static Token combineTokensGold(Token tok, CoreLabel cl, int currentCoreNlpSentenceIndex) {
		Token combined = new Token(tok);
		combined.predText = tok.goldText;
		combined.predLemma = tok.goldLemma;
		combined.predPosition = -1;   /* will be determined by document aligner */
		combined.predPosTag = tok.goldPosTag;
		combined.predNer = tok.goldNer;
		combined.predSentencePosition = currentCoreNlpSentenceIndex;
		combined.predByteCount = new ByteCount(cl.beginPosition(), cl.endPosition());
		return combined;
	}

	/**
	 * Aligns the tokens of a sentence
	 * @param pTokens
	 * @param cSentence
	 */
	private void alignTokensStrict(List<Token> pTokens, CoreMap cSentence) {

        Tree tree = cSentence.get(TreeAnnotation.class);
		SemanticGraph dependencies = cSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<CoreLabel> cTokens = cSentence.get(CoreAnnotations.TokensAnnotation.class);


		Iterator<IndexedWord> depIterator = new IndexedWordIterator(dependencies.vertexListSorted().iterator());
		pcTokenList = new ArrayList<Token>(cTokens.size());
		List<Tree> leaves = tree.getLeaves();
		Iterator<Tree> leafIterator = leaves.iterator();
        
		indexedWord2CoreLabel = new HashMap<IndexedWord, Token>();
		tree2CoreLabel = new HashMap<Tree, Token>();

		// state variables
		Token prevCombinedToken = null;
		
		Iterator<CoreLabel> cTokenIter = cTokens.iterator();
		Iterator<Token> pTokenIter = pTokens.iterator();
		int currentCoreNlpSentenceIndex = 0;
		
		CoreLabel cToken = cTokenIter.next();
		Token pToken = pTokenIter.next();
		Token prevPToken = null;

		int pFinal = pTokens.get(pTokens.size()-1).goldByteCount.getEnd();
		int cFinal = cTokens.get(cTokens.size()-1).endPosition();
		
		int pBegin = pToken.goldByteCount.getBegin();
		int pEnd = pToken.goldByteCount.getEnd();
		
		int cBegin = cToken.beginPosition();
		int cEnd = cToken.endPosition();

		// for compatibility: TreeGraphNode bookkeeping
		Collection<TypedDependency> dependencyEdges = dependencies.typedDependencies();
		List<IndexedWord> tgnList = new ArrayList<>(cTokens.size());

		for (int i = 0; i < cTokens.size()+1; i++)
			tgnList.add(null);


		for (TypedDependency edge : dependencyEdges) {
			tgnList.set(edge.gov().index(), edge.gov());
			tgnList.set(edge.dep().index(), edge.dep());
		}

		Iterator<IndexedWord> tgnIterator = tgnList.iterator();


		IndexedWord dep = null;
		Tree leaf = null;
		IndexedWord tgn = null;



		// move dep and tree iterators forward by 1
		if (depIterator.hasNext()) dep = depIterator.next();
		if (leafIterator.hasNext()) leaf = leafIterator.next();
		if (tgnIterator.hasNext()) tgn = tgnIterator.next();


		// guess a pSentence for debug messages -- may be null if there is no sentence annotation
		Sentence pSentence = pTokens.get(pTokens.size()-1).sentence;
		String pSentenceId;
		
		if (pSentence != null) {
			SentenceId id = pSentence.sentenceId;
			pSentenceId = id == null ? "null" : id.toString();
		} else {
			pSentenceId = null;
		}
		
		boolean usedPToken = false;


		// loop until we reach the end of either sentence
		while ((pFinal != pEnd) || (cFinal != cEnd)) {
			// Check for unwanted conditions:

			//   1. No PARC tokens left?
			//      this happens when the raw text contained tokens that are missing in the PARC data. these are mostly
			//      sentence-final punctuation marks.
			if (pToken == null) {
				// try to recover here for final quotes that the parser predicted. This may be good or bad.
				if (useCoreNlpQuoteCompletion && Helper.isQuote(cToken)) {
					Token combinedToken = combineTokens(prevPToken, cToken, currentCoreNlpSentenceIndex);

					prevCombinedToken.dependencyBackpointer = dep;
					prevCombinedToken.treeBackpointer =  leaf;


					// bookkeeping with new token
					if (usedPToken) {
						// avoid making subsequent tokens start tokens!
						combinedToken.paragraphBegins = false;
					}

					addNewWord(combinedToken, prevCombinedToken);
				} else {
					if (StaticConfig.verbose) System.out.println(pSentenceId +
							" Dropping unmatched " + cToken + " " + "(PARC tokens: " + pTokens + " )");
				}

				// stop processing this sentence, drop remaining CoreNLP data -- in practice, these will never be needed
				break;
			}

			//   2. No CoreNLP tokens left
			if (cToken == null) {
				if (StaticConfig.verbose)
					System.out.println("Unaligned Token(s) in "  + pSentenceId + " " + pToken);

				break;
			}


			// check whether tokens at least overlap before continuing processing ...
			pBegin = pToken.goldByteCount.getBegin();
			pEnd = pToken.goldByteCount.getEnd();
			
			cBegin = cToken.beginPosition();
			cEnd = cToken.endPosition();

			// ... if they don't, try to recover by syncing up
			if (cBegin > pEnd) {
				if (usedPToken) {
					if (StaticConfig.verbose) System.out.println(pSentenceId + " out of sync "  +
							pToken + " " + cToken + " -- trying to fix");
					
					if (pTokenIter.hasNext()) {
						prevPToken = pToken;
						pToken = pTokenIter.next();
						continue; // restart the iteration
					} else {
						if (StaticConfig.verbose) System.out.println(pSentenceId +
								" Dropping unmatched " + cToken + " " + "(PARC tokens: " + pTokens + " )");
						break;
					}
				} else {   /* this may happen when tokens from previous iterations have a wrong byte count -- skip */
					if (StaticConfig.verbose) System.out.println(pSentenceId + " Dropping unmatched "
							+ cToken + " " + "(PARC tokens: " + pTokens + " )");
					break;
				}
			}


			// Now the main part. There are three conditions which could occur.
			if (pEnd == cEnd) {
				// 1. Tokens have identical end points
				//    In this case, just combine the tokens and move on
				Token combinedToken = combineTokens(pToken, cToken, currentCoreNlpSentenceIndex);
				
				combinedToken.dependencyBackpointer = dep;
				combinedToken.treeBackpointer = leaf;
				combinedToken.tgn = tgn;

				
				// bookkeeping with new token
				if (usedPToken) { // avoid making subsequent tokens start tokens!
					combinedToken.paragraphBegins = false;
				}


				addNewWord(combinedToken, prevCombinedToken);
				prevCombinedToken = combinedToken;
				
				// move iterators
				if (cTokenIter.hasNext()) {
					cToken = cTokenIter.next();
					currentCoreNlpSentenceIndex++;
				} else {
					cToken = null;
				}
				if (pTokenIter.hasNext()) {
					prevPToken = pToken;
					pToken = pTokenIter.next();
				} else {
					pToken = null;
				}
				usedPToken = false;

				// add parse information
				if (depIterator.hasNext()) dep = depIterator.next();
				if (leafIterator.hasNext()) leaf = leafIterator.next();
				if (tgnIterator.hasNext()) tgn = tgnIterator.next();


				
			} else if (cEnd > pEnd) {
				// 2. The CoreNLP token is longer than the PARC token
				//    split the CoreNLP token into two parts

				Token combinedToken;
				CoreLabel[] splitCToken = null;

				if (splitType == SplitType.SPLIT) {
					splitCToken = splitToken(cToken, pEnd);

					combinedToken = combineTokens(pToken, splitCToken[0], currentCoreNlpSentenceIndex);
				} else if (splitType == SplitType.NONE_CORENLP) {
					throw new Error();
				} else {
					combinedToken = combineTokens(pToken, cToken, currentCoreNlpSentenceIndex);
				}

				combinedToken.dependencyBackpointer = dep;
				combinedToken.treeBackpointer = leaf;
				combinedToken.tgn = tgn;

				// bookkeeping with new token
				if (usedPToken) { // avoid making subsequent tokens start tokens!
					combinedToken.paragraphBegins = false;
				}

				addNewWord(combinedToken, prevCombinedToken);
				prevCombinedToken = combinedToken;
				
				// get new pToken to match the remaining bit
				if (pTokenIter.hasNext()) {
					prevPToken = pToken;
					pToken = pTokenIter.next();
				} else {
					pToken = null;
				}

				if (splitType == SplitType.SPLIT)
					cToken = splitCToken[1];

				usedPToken = false;

			} else { // cEnd < pEnd
				// 3. The PARC token is longer than the CoreNLP token
				//    Attach the PARC token to multiple CoreNLP tokens

				Token combinedToken = combineTokens(pToken, cToken, currentCoreNlpSentenceIndex);
				
				combinedToken.dependencyBackpointer = dep;
				combinedToken.treeBackpointer = leaf;
				combinedToken.tgn = tgn;
				
				// bookkeeping with new token
				if (usedPToken) { // avoid making subsequent tokens start tokens!
					combinedToken.paragraphBegins = false;
				}

				addNewWord(combinedToken, prevCombinedToken);
				prevCombinedToken = combinedToken;

				// get new cToken and other CoreNLP data
				if (cTokenIter.hasNext()) {
					cToken = cTokenIter.next();
					currentCoreNlpSentenceIndex++;
				} else {
					cToken = null;
				}
				usedPToken = true;
				
				if (depIterator.hasNext()) dep = depIterator.next();
				if (leafIterator.hasNext()) leaf = leafIterator.next();
				if (tgnIterator.hasNext()) tgn = tgnIterator.next();
			}
		}
	}


	public void setUseCoreNlpQuoteCompletion(boolean useCoreNlpQuoteCompletion) {
		this.useCoreNlpQuoteCompletion = useCoreNlpQuoteCompletion;
	}

}
