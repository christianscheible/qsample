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


package ims.cs.mallet;


import cc.mallet.types.Instance;
import ims.cs.lingdata.Document;

/**
 * Mallet "Instance" wrapper class for documents
 */
public class PARCDocumentInstance extends Instance {

	private static final long serialVersionUID = -6933321582801583924L;

	public transient Document document;
	
	private PARCDocumentInstance() {
		super(null, null, null, null);
	};
	
	public PARCDocumentInstance(Document document) {
		super(document, null, document.docId, document);
		this.document = document;
	}

	
	public Document getDocument() {
		return document;
	}
	
}
