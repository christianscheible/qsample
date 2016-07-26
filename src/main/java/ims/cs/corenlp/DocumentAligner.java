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
import java.util.Iterator;
import java.util.List;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import ims.cs.util.StaticConfig;

/**
 * Aligns CoreNLP parser output with the original document. This is necessary since CoreNLP may produce a
 * tokenization that deviates from the input.
 */
public class DocumentAligner {

	private Document pDocument;
	private List<Sentence> pcSentenceList;
	private boolean useCoreNlpQuoteCompletion = true; 
	
	
	public DocumentAligner(Document pDocument, CoreMap cDocument) {
		this.pDocument = pDocument;
		alignSentences(pDocument, cDocument);
	}

	/**
	 * Aligns original document and CoreNLP processed document.
	 * @param pDocument
	 * @param cDocument
	 */
	private void alignSentences(Document pDocument, CoreMap cDocument) {

		// get sentences
	    List<CoreMap> cSentenceList = cDocument.get(SentencesAnnotation.class);

		// state variables
		pcSentenceList = new ArrayList<>();
		Iterator<CoreMap> cSentenceIter = cSentenceList.iterator();
	    Iterator<Token> pTokenIter = pDocument.tokenList.iterator();
	    Token nextPToken = pTokenIter.next(); 

		// now iterate over CoreNLP sentences
	    while (cSentenceIter.hasNext()) {
			// get sentence tokens
	    	CoreMap cSentence = cSentenceIter.next();
			List<CoreLabel> cTokens = cSentence.get(CoreAnnotations.TokensAnnotation.class);
	    	List<Token> currentSentencePTokens = new ArrayList<>(cTokens.size());


			// identify last token
	    	CoreLabel finalToken = cTokens.get(cTokens.size()-1);
			int endPosition = finalToken.endPosition();
			
			// align tokens by byte count until the end of the sentence
			while (nextPToken.goldByteCount.getBegin() <= endPosition) {
				currentSentencePTokens.add(nextPToken);
				if (nextPToken.goldByteCount.getEnd() <= endPosition) {
					if (pTokenIter.hasNext()) {
						nextPToken = pTokenIter.next();
					} else {
						break;
					}
				} else {
					break;
				}
			}
			

			// check if any tokens need to be aligned at all
			if (currentSentencePTokens.size() > 0) {
				TokenAligner ta = new TokenAligner(currentSentencePTokens, cSentence);
				ta.setUseCoreNlpQuoteCompletion(useCoreNlpQuoteCompletion);
				Sentence combinedSentence = ta.getCombinedSentence();

				if (combinedSentence == null) {
					if (StaticConfig.verbose)
						System.out.println("Discarding empty combined sentence: " +
								cSentence.toString() + currentSentencePTokens.toString());
				} else {
					pcSentenceList.add(combinedSentence);
				}
			} else {   /* sentence may be empty if CoreNLP produced spurious tokens */
				if (StaticConfig.verbose)
					System.out.println("Discarding empty PARC sentence: " +
						cSentence.toString() + currentSentencePTokens.toString());
			}

	    }	    

	}

	/**
	 * Returns the aligned document
	 * @return
	 */
	public Document getDocument() {
		Document combinedDocument = new Document(pDocument);
		
		combinedDocument.sentenceList = pcSentenceList;
		
		List<Token> documentTokenList = new ArrayList<Token>(pcSentenceList.size() * 5);
		
		for (Sentence sentence: pcSentenceList) {
			sentence.document = combinedDocument;
			documentTokenList.addAll(sentence.tokenList);
		}
		
		combinedDocument.tokenList = documentTokenList;

		// set token positions in the new document
		for (int i = 0; i < combinedDocument.tokenList.size(); i++) {
			combinedDocument.tokenList.get(i).predPosition = i;
		}

		return combinedDocument;

	}

}
