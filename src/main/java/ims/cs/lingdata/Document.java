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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ims.cs.lingdata.Types.Genre;
import ims.cs.qsample.spans.Span;

/**
 * Representation of a document.
 * Has a list of sentences and a list of tokens; holds span predictions.
 */
public class Document  {

	public List<Sentence> sentenceList;
	public List<Token> tokenList;
	public DocumentId docId;
	public Genre genre;
	public String text;
	public String sourceCorpusName;

	// span predictions
	public Set<Span> predictedSpanSet;
	public Set<Span> goldSpanSet;


	// CoreNLP flag to avoid multiple processing
	public boolean isCoreNlpProcessed;

	public Document(Document pDocument) {
		this.docId = pDocument.docId;
		this.genre = pDocument.genre;
		this.text = pDocument.text;
		this.sourceCorpusName = pDocument.sourceCorpusName;

		this.predictedSpanSet = new HashSet<Span>();
		this.goldSpanSet = new HashSet<Span>();
	}


	public Document() { }


	public List<Token> getTokenList() {
		return tokenList;
	}
	
    public Set<Span> goldSpansOfLabel(String label) {
		Set<Span> selectedGoldSpans = new HashSet<>();
		for (Span gs : goldSpanSet) {
			if (gs.label.equals(label)) {
				selectedGoldSpans.add(gs);
			}
		}
		return selectedGoldSpans;
	}

	public Set<Span> predictedSpansOfLabel(String label) {
		Set<Span> predGoldSpans = new HashSet<>();
		for (Span ps : predictedSpanSet) {
			if (ps.label.equals(label)) {
				predGoldSpans.add(ps);
			}
		}
		return predGoldSpans;
	}

	public Token getPrevToken(Token t) {
		return getPrevToken(t, 1);
	}

	public Token getNextToken(Token t) {
		return getNextToken(t, 1);
	}

	public Token getPrevToken(Token t, int dist) {
		if (t.predPosition - dist >= 0) return tokenList.get(t.predPosition-dist);
		else return null;
	}

	public Token getNextToken(Token t, int dist) {
		if (t.predPosition < tokenList.size()-dist) return tokenList.get(t.predPosition+dist);
		else return null;
	}

}
