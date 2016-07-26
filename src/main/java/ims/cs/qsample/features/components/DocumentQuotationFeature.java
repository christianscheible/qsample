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


package ims.cs.qsample.features.components;

import java.util.List;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.corenlp.Helper;

/**
 * Check for each token whether it is enclosed in quotation marks
 */
public abstract class DocumentQuotationFeature {
	
	public static final String INQ_PREFIX = "DOC:INQ";
	public static final String NOTINQ_PREFIX = "DOC:NOTINQ";
	public static final String OPEN_PREFIX = "DOC:Q-OPENS";
	public static final String CLOSE_PREFIX = "DOC:Q-CLOSES";

	public static void extract(Document document) {
		boolean inQuote = false;

		List<Token> tokenList = document.getTokenList();
			
		for (Token token : tokenList) {
			// check if token is a quotation mark and is not to be ignored
			// (paragraph-initial tokens may be marked to be ignored)
			if (Helper.isQuote(token) && !token.ignoreQuote) {

				// add respective feature ...
				if (inQuote)
					token.boundaryFeatureSet.add(CLOSE_PREFIX);
				else
					token.boundaryFeatureSet.add(OPEN_PREFIX);

				// toggle in-quote state
				inQuote = !inQuote;
				token.boundaryFeatureSet.add(INQ_PREFIX);
			} else if (inQuote) {   /* currently in quote */
				token.boundaryFeatureSet.add(INQ_PREFIX);
			} else {   /* currently not in quote */
				token.boundaryFeatureSet.add(NOTINQ_PREFIX);
			}
		}
	}

}
