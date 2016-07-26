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

import java.util.Iterator;

import edu.stanford.nlp.ling.IndexedWord;

/**
 * Iterates over all indexed words safely -- this is useful as punctuation may not have an associated indexed word
 */
public class IndexedWordIterator implements Iterator<IndexedWord> {

	Iterator<IndexedWord> iter;
	IndexedWord currentWord;
	int index = 1;
	
	private void fetch() {
		if (iter.hasNext()) {
			currentWord = iter.next();
		} else {
			currentWord = null;			
		}
	}
	
	public IndexedWordIterator(Iterator<IndexedWord> iter) {
		this.iter = iter;
		fetch();
	}
	
	public boolean hasNext() {
		return true;
	}

	public IndexedWord next() {
		IndexedWord returnVal;
		
		if (currentWord == null) {
			returnVal = null;
		} else if (currentWord.index() == index) {
			returnVal = currentWord;
			fetch();
		} else {
			returnVal = null;
		}
		
		index++;
		return returnVal;
	}

	public void remove() {
		throw new UnsupportedOperationException("no remove allowed");
	}

}
