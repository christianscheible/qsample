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

import ims.cs.lingdata.Document;
import ims.cs.lingdata.DocumentId;
import ims.cs.lingdata.Token;
import ims.cs.util.StaticConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * XML parser for BBN named entity dataset
 */
public class BbnNeParser {


	private static BbnNeParser instance;
	private static SAXParser saxParser;
	private static XMLReader xmlReader;
	private static BbnNeHandler handler;

	public String currentBbnFile;


	private BbnNeParser() throws ParserConfigurationException, SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		saxParser = spf.newSAXParser();
		xmlReader = saxParser.getXMLReader();
		handler = new BbnNeHandler();
		xmlReader.setContentHandler(handler);
	}

	/**
	 * BBN splits each section in up to 4 files. This function determines which one contains the document in question.
	 * @param document
	 * @return
	 */
	public String getBbnFileName(Document document) {
		DocumentId id = document.docId;
		String sectionStr = id.getSectionStr();
		String fileStr = id.getFileStr();
		int num = Integer.parseInt(fileStr);
		char partitionChar;

		// BBN partition rule
		if (num < 25) {
			partitionChar = 'a';
		} else if (num < 50) {
			partitionChar = 'b';
		} else if (num < 75) {
			partitionChar = 'c';
		} else {
			partitionChar = 'd';
		}

		String fileName = "wsj" + sectionStr + partitionChar + ".qa";

		return fileName;
	}


	/**
	 * Takes a previously loaded WSJ document and adds BBN named entities.
	 * This function does some rudimentary caching, which requires the WSJ documents to be parsed in order to stay fast.
	 * @param document
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public Document augmentDocumentXml(Document document) throws IOException, SAXException {
		String fileName = getBbnFileName(document);

		// move to the next BBN file if necessary
		// this will be efficient if the documents are passed in WSJ order as it avoids reloading the same file
		if (!fileName.equals(currentBbnFile)) {
			File xmlFile = new File(StaticConfig.bbnPath + fileName);
			xmlReader.parse(new InputSource(xmlFile.getPath()));
			currentBbnFile = fileName;
		}

		List<String> tags = handler.getTags(document.docId.getFileStr());
		List<Token> tokenList = document.tokenList;

		// sanity check: same number of tokens?
		if (tags.size() != tokenList.size()) {
			throw new Error("Tag and token counts differ");
		}

		// align tags and tokens
		for (int i = 0; i < tokenList.size(); i++) {
			Token token = tokenList.get(i);
			String neTag = tags.get(i);
			token.goldNer = neTag;
		}

		return document;
	}


	public static BbnNeParser getInstance() throws ParserConfigurationException, SAXException {
		if (instance == null) {
			instance = new BbnNeParser();
		}
		return instance;
	}

}
