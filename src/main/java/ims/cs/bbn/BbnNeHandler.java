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


package ims.cs.bbn;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML handler to process named entity information from the BBN dataset.
 */
public class BbnNeHandler extends DefaultHandler {

	StringBuffer accumulator = new StringBuffer();   /* Accumulate parsed text */
	List<String> tags;
	Map<String, List<String>> tagMap = new HashMap<>();
	String currentTag;
	String fileNo;
	boolean tagPreceded = false;
	boolean disableNextTag = false;
	private String docNo;


	public void characters(char[] buffer, int start, int length) {
		accumulator.append(buffer, start, length);
	}


	@Override
    public void startDocument() throws SAXException {
	}

	@Override
    public void endDocument() throws SAXException {
    }

	/**
	 * Returns all currently unprocessed text read so far
	 * @return
	 */
	public String popText() {
		String text = accumulator.toString();
		accumulator.setLength(0);
		return text;
	}

	/**
	 * Counts number of spaces. Double spaces are conflated.
	 * @param s
	 * @return
	 */
	public int numSpaces(String s) {
		int numSpaces = 0;
		boolean prevIsWhitespace = false;

		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				if (!prevIsWhitespace)
					numSpaces++;
				prevIsWhitespace = true;
			} else {
				prevIsWhitespace = false;
			}
		}

		return numSpaces;
	}

	/**
	 * Counts number of words.
	 * @param s
	 * @return
	 */
	public int numWords(String s) {
		int numWords;
		if (s.equals("")) {
			numWords = 0;
		} else {
			numWords = numSpaces(s) + 1;
		}
		return numWords;
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {

		if (qName.equals("DOC")) {    /* document starts, reset accumulator */
			accumulator.setLength(0);
			tags = new ArrayList<>();
		} else if (qName.endsWith("EX")) {  /* NE tag starts */
			String text = popText();
			String trimText = text.trim();

			// count words to align with the tokenized text
			int numWords = numWords(trimText);


			// adjust word counters in case of mid-word tags
			if (tagPreceded && (text.length() == 0 || !Character.isWhitespace(text.charAt(0)))) numWords--;
			if (text.length() == 0 || !Character.isWhitespace(text.charAt(text.length()-1))) numWords--;

			if (trimText.length() > 0 && numWords < 0) {
				disableNextTag = true;
			}

			// pad with outside tags
			for (int i = 0; i < numWords; i++) tags.add("O");
			currentTag = atts.getValue("TYPE");
		} else if (qName.equals("DOCNO")) {   /* new document, reset accumulator (to be sure) */
			accumulator.setLength(0);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("DOC")) {   //* document ends */
			String text = popText();
			String trimText = text.trim();
			int numWords = numWords(trimText);

			// adjust word counters in case of mid-word tags
			if (tagPreceded && (text.length() == 0 || !Character.isWhitespace(text.charAt(0)))) numWords--;

			// pad with outside tags
			for (int i = 0; i < numWords; i++) tags.add("O");

			// store annotation
			tagMap.put(fileNo, tags);
			tagPreceded = false;
		} else if (qName.endsWith("EX")) {   /* NE tag ends */
			if (disableNextTag) {
				disableNextTag = false;
				return;
			}

			String text = popText();
			String trimText = text.trim();
			int numWords = numWords(trimText);
			for (int i = 0; i < numWords; i++) tags.add(currentTag);
			tagPreceded = true;
		} else if (qName.equals("DOCNO")) {  /* document number ends, parse document number */
			docNo = popText();
			fileNo = docNo.trim().substring(5);
			tagPreceded = false;
		}
	}

	/**
	 * Returns the NE annotations for a given file ID
	 * @param fileId
	 * @return
	 */
	public List<String> getTags(String fileId) {
		return tagMap.get(fileId);
	}


}
