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


package ims.cs.parc.xml;

import java.util.ArrayList;
import java.util.List;

import ims.cs.parc.PARCAttribution;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.lingdata.Types.Genre;
import ims.cs.lingdata.ByteCount;
import ims.cs.parc.PARCAttribution.Role;

public class PARCHandler extends DefaultHandler {
	
	private Document document;
	private Token currentToken;
	private List<Token> tokenList;
	private List<Token> documentTokenList;


	private List<PARCAttribution> currentAttributionStack;
	private List<Sentence> sentenceList;
	private StringBuilder sentenceStringBuilder;
	private int nestedLevel = 0;
	private int numberOfRoles;

	@Override
    public void startDocument() throws SAXException {
		currentAttributionStack = new ArrayList<>();
		nestedLevel = 0;
		sentenceList = new ArrayList<Sentence>();
		documentTokenList = new ArrayList<Token>();
	}
	
	@Override
    public void endDocument() throws SAXException {
		document = new Document();
		document.sentenceList = sentenceList;
		document.genre = Genre.NEWS;
		document.tokenList = documentTokenList;
		
		document.sourceCorpusName = "PARC";

		for (Sentence sentence: sentenceList) {
			sentence.document = document;
		}

		
    }


	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (qName.equals("SENTENCE")) {   /* new sentence begins, initialize data structures */
			tokenList = new ArrayList<Token>();
			sentenceStringBuilder = new StringBuilder();
		} else if (qName.equals("WORD")) {   /* word read */
			// get information from parser
			ByteCount byteCount = new ByteCount(atts.getValue("ByteCount"));
			String gornStr = atts.getValue("gorn");
			String lemma = atts.getValue("lemma");
			String pos = atts.getValue("pos");
			String wordForm = atts.getValue("text");
			int position = Integer.parseInt(atts.getValue("word"));
			int sentencePosition = Integer.parseInt(atts.getValue("sentenceWord"));

			// generate new token and fill
			Token token = new Token();
			
			token.goldByteCount = byteCount;
			token.goldLemma = lemma;
			token.goldText = wordForm;
			token.goldPosTag = pos;
			token.goldPosition = position;
			token.goldSentencePosition = sentencePosition;
			token.attributionList = new ArrayList<>();

			// token bookkeeping
			tokenList.add(token);
			documentTokenList.add(token);
			currentToken = token;
			sentenceStringBuilder.append(wordForm);
			sentenceStringBuilder.append(" ");

		} else if (qName.equals("attribution")) {   /* attribution annotation (within a word) */
			// initialize an attribution container for the first attribution
			PARCAttribution currentAttribution = new PARCAttribution();
			currentAttributionStack.add(0, currentAttribution);
			currentAttribution.id = atts.getValue("id");
			numberOfRoles = 0;
		} else if (qName.equals("attributionRole")) {   /* new role encountered (within an attribution) */
			String role = atts.getValue("roleValue");
			numberOfRoles ++;

			PARCAttribution currentAttribution = currentAttributionStack.get(0);

			if (currentAttribution.role != null) {
				// this is not the first one, so we need a new container
				PARCAttribution prevAttribution = currentAttribution;
				currentAttribution = new PARCAttribution();
				currentAttribution.id = prevAttribution.id;
				currentAttributionStack.add(0, currentAttribution);
			}

			// set the role
			if (role.equals("content")) {
				currentAttribution.role = Role.CONTENT;
			} else if (role.equals("source")) {
				currentAttribution.role = Role.SOURCE;
			} else if (role.equals("cue")) {
				currentAttribution.role = Role.CUE;
			} else if (role.equals("supplement")) {
				currentAttribution.role = Role.SUPPLEMENT;
			} else {
				throw new UnsupportedOperationException("unexpected role: " + role);
			}
		} else if (!qName.equals("root")) {
			// these would be internal nodes of trees
			// however, we're not reading trees
		} else {
			// root, node
			// ignore, we're not reading trees
		}
	}
	
	@Override
	public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) {
		if (qName.equals("SENTENCE")) {
			// sentence ends, do some bookkeeping
			Sentence sentence = new Sentence();

			sentence.tokenList = tokenList;
			sentenceList.add(sentence);

			for (Token token: tokenList)
				token.sentence = sentence;

		} else if (qName.equals("WORD")) {
			// sanity check: did we use up all attributions?
			if (!currentAttributionStack.isEmpty()) throw new Error("some attributions were not used, stack is not empty");
			currentAttributionStack.clear();
		} else if (qName.equals("attribution")) {   /* attribution ends, associate it with the current token */
			// pop
			if (numberOfRoles == 0) {   /* in this case, just pop because the attribution had no role and is thus garbage anyway */
				currentAttributionStack.remove(0);
			}
			for (int i=0; i < numberOfRoles; i++) {
				PARCAttribution currentAttribution = currentAttributionStack.get(0);
				currentAttributionStack.remove(0);

				if (!currentAttribution.id.toLowerCase().contains("nested")) {
					if (currentAttribution.id == null || currentAttribution.role == null) {
						// this can actually occur in the data. print a message, we can't recover here
						System.out.println("Token " + currentToken + " had an attribution with empty role.");
					}
					currentToken.attributionList.add(currentAttribution);
				}
			}

		} else if (qName.equals("attributionRole")) {
			// already taken care of at beginning, do nothing
		} else if (!qName.equals("root")) {
			// internal node, do nothing
		} else {
			// root, do nothing
		}
	}

	public Document getDocument() {
		return document;
	}
	
	
}
