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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import ims.cs.lingdata.Document;

/**
 * XML parser for the PARC corpus
 */
public class PARCParser {

	private static PARCParser instance;
	private static SAXParser saxParser;
	private static XMLReader xmlReader;
	private static PARCHandler handler;
	
	private PARCParser () throws ParserConfigurationException, SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    saxParser = spf.newSAXParser();
	    xmlReader = saxParser.getXMLReader();
	    handler = new PARCHandler();
	    xmlReader.setContentHandler(handler);
	}
	
	public Document parseFile(File xmlFile) throws IOException, SAXException {
		xmlReader.parse(new InputSource(xmlFile.getPath()));
	    return handler.getDocument();

	}
	
	public static PARCParser getInstance() throws ParserConfigurationException, SAXException {
		if (instance == null) {
			instance = new PARCParser();
		}
		return instance;
	}
}
