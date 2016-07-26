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


package ims.cs.qsample.features;

import java.io.IOException;


import ims.cs.qsample.features.components.SentenceConstituentFeatures;
import ims.cs.qsample.features.components.SentenceDependencyFeatures;
import ims.cs.qsample.features.components.SentenceFeaturesDerivedFromListCue;
import ims.cs.qsample.features.components.SentenceIndicatorFeatures;
import ims.cs.qsample.features.components.TokenDictFeatures;
import ims.cs.qsample.features.components.TokenLexicalFeatures;
import ims.cs.qsample.features.components.TokenListFeatures;
import ims.cs.lingdata.Document;
import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.qsample.features.components.DocumentOffsetConjunction;
import ims.cs.qsample.features.components.DocumentQuotationFeature;
import ims.cs.util.StaticConfig;

/**
 * Feature extractor class for (mostly) those features that require non-static code.
 */
public class FeatureExtraction {

	private TokenListFeatures tokenPersonFeatures;
	private TokenListFeatures tokenOrganizationFeatures;
	private TokenListFeatures tokenTitleFeatures;
	private TokenListFeatures tokenListFeatures;
	private TokenListFeatures tokenNounListFeatures;
	private TokenDictFeatures verbNetFeatures;
	private DocumentOffsetConjunction documentOffsetConjunction;


	public FeatureExtraction () throws IOException, ClassNotFoundException {
		// non-static extractors
		tokenPersonFeatures = new TokenListFeatures("resources/PARC/listfeatures/person.hyponyms.txt", "EK:PER");
		tokenOrganizationFeatures = new TokenListFeatures("resources/PARC/listfeatures/organization.hyponyms.txt", "EK:ORG");
		tokenTitleFeatures = new TokenListFeatures("resources/PARC/listfeatures/titles.txt", "EK:TITLE");
		tokenListFeatures = new TokenListFeatures("resources/PARC/listfeatures/krestel_verbs.txt", "CUELIST");
		tokenNounListFeatures = new TokenListFeatures("resources/PARC/listfeatures/attribution_nouns.txt", "NOUNCUELIST");
		verbNetFeatures = new TokenDictFeatures("resources/PARC/listfeatures/verbnet.txt", "VERBNET");

		// restrict extractors to certain pos tags
		tokenNounListFeatures.posStart = "N";
		tokenListFeatures.posStart = "V";
		verbNetFeatures.posStart = "V";

		// Offset conjunction on non-static features
		documentOffsetConjunction = new DocumentOffsetConjunction();
	}

	
	/**
	 * Runs token-level feature extraction on the tokens in the document
	 * @param document
	 */
	public void extractTokenFeatures(Document document) {
		for (Token token : document.tokenList) {
			tokenPersonFeatures.extract(token);
			tokenOrganizationFeatures.extract(token);
			tokenTitleFeatures.extract(token);
			TokenLexicalFeatures.extract(token);

			tokenListFeatures.extract(token);
			tokenNounListFeatures.extract(token);
			verbNetFeatures.extract(token);
		}
	}
	

	/**
	 * Runs sentence-level feature extraction on the sentences in the document
	 * @param document
	 */
	public void extractSentenceFeatures (Document document) {
		for (Sentence sentence : document.sentenceList) {
			SentenceIndicatorFeatures.extract(sentence);
			if (StaticConfig.dependencyFeatures) SentenceDependencyFeatures.extract(sentence);
			if (StaticConfig.constituentFeatures) SentenceConstituentFeatures.extract(sentence);
			SentenceFeaturesDerivedFromListCue.extract(sentence);
		}
	}


	public void setUpFeatureSets(Document doc) {
		for (Token token : doc.tokenList)
			if (token.boundaryFeatureSet == null)
				token.boundaryFeatureSet = new FeatureIntSet();
	}


	/**
	 * Runs feature extraction on a single document
	 * @param document
	 */
	public void extractAllFeatures (Document document) {
		// initialize empty feature sets
		setUpFeatureSets(document);
		
		// Token features & sentence features
		extractTokenFeatures(document);
		extractSentenceFeatures(document);

		// quotation mark features
		if (StaticConfig.documentQuotationFeature)
			DocumentQuotationFeature.extract(document);

		// offset conjunction
		if (StaticConfig.documentOffsetConjunction)
			documentOffsetConjunction.extract(document);

		// additional features
		BoundaryFeatures.additionalBoundaryFeatures(document);
	}


}
