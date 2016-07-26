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


package ims.cs.parc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ims.cs.lingdata.Document;
import ims.cs.lingdata.Token;
import ims.cs.parc.PARCAttribution.Role;
import ims.cs.parc.PARCAttribution.Type;
import ims.cs.corenlp.Helper;
import ims.cs.util.StaticConfig;

/**
 * Takes attribution annotations and generates a token-level labeling
 * This class produces BIOE labels, which we use mostly for legacy reasons. We will convert them into spans later.
 */
public class SpanLabelExtractor implements Serializable {

	private static final long serialVersionUID = 4268827398247608074L;

	// default BIOE labels
	public static final String BEGIN_TAG = "B";
	public static final String INSIDE_TAG = "I";
	public static final String OUTSIDE_TAG = "O";
	public static final String END_TAG = "E";

	// actual BIOE labels to be used
	public String beginTag;
	public String insideTag = INSIDE_TAG;
	public String outsideTag = OUTSIDE_TAG;
	public String endTag;

	// settings for labeling behavior
	//   use a begin tag?
	private boolean useBegin = true;
	//   use an end tag?
	private boolean useEnd = true;
	//   split adjacent spans?
	private boolean useBTagForce = true;

	// set of quotation types to be considered
	private Set<PARCAttribution.Type> typeSet;


	/**
	 * Sets tag strings depending on the useBegin and useEnd settings
	 */
	private void setBioLabel() {
		// use B or I for beginning?
		if (useBegin)
			beginTag = BEGIN_TAG;
		else
			beginTag = INSIDE_TAG;

		// use E or I for end?
		if (useEnd)
			endTag = END_TAG;
		else
			endTag = INSIDE_TAG;
	}


	/**
	 * Parse configuration string into a set of allowed types
	 * @param typeSetStr
	 * @return
	 */
	private static Set<PARCAttribution.Type> getTypeSet (String typeSetStr) {
		Set<PARCAttribution.Type> typeSet = new HashSet<>();

		if (typeSetStr.contains("I"))
			typeSet.add(Type.INDIRECT);

		if (typeSetStr.contains("D"))
			typeSet.add(Type.DIRECT);

		if (typeSetStr.contains("M"))
			typeSet.add(Type.MIXED);

		return typeSet;
	}

	/**
	 * Set up extractor
	 */
	public SpanLabelExtractor() {
		boolean useEnd = StaticConfig.useBioeTags;
		Set<Type> typeSet = getTypeSet(StaticConfig.quotationTypes);

		setUp(useBegin, useEnd, typeSet);
	}

	/**
	 * Make initial settings based on external parameters or use defaults if unspecified
	 * @param useBegin
	 * @param useEnd
	 * @param typeSet
	 */
	private void setUp (boolean useBegin, boolean useEnd, Set<PARCAttribution.Type> typeSet) {
		if (StaticConfig.verbose && useBTagForce)
			System.out.println("NOTE: separating adjacent spans");

		this.useBegin = useBegin;
		this.useEnd = useEnd;
		setBioLabel();

		if (typeSet == null) {
			this.typeSet = new HashSet<>();
			this.typeSet.add(Type.DIRECT);
			this.typeSet.add(Type.INDIRECT);
			this.typeSet.add(Type.MIXED);
		} else {
			this.typeSet = typeSet;
		}

	}
	

	/**
	 * Conditionally add labels from the buffer to the sequence depending on the quotation type
	 * @param tagBuffer
	 * @param tagSequence
	 * @param tokenBuffer
	 */
	private void insertBuffer(List<String> tagBuffer, List<String> tagSequence, List<Token> tokenBuffer) {
		// do nothing if buffer is empty
		if (tagBuffer.size() == 0) return;


		// get the text last token
		String tokenText = tokenBuffer.get(tagBuffer.size() - 1).predText;
		int offset = 1;


		// span cannot end in these punctuation symbols according to Pareti -- exclude if this is the case
		if (tokenText.equals(",") || tokenText.equals(".") || tokenText.equals(":") || tokenText.equals("?")) {
			offset++;
			tagBuffer.set(tagBuffer.size() - 1, outsideTag);
		}

		// if using end and last tag was not B, set an end tag
		if (useEnd && offset <= tagBuffer.size() &&  !tagBuffer.get(tagBuffer.size()-offset).equals(beginTag)) {
			tagBuffer.set(tagBuffer.size() - offset, endTag);
		}

		// determine the type of the quotation in the buffer
		// TODO: if this is used again, check whether the above modifications cause any problems
		Type type;
		
		Token firstToken = tokenBuffer.get(0);
		Token lastToken = tokenBuffer.get(tokenBuffer.size()-1);

		boolean lastTokenEndsDocument = lastToken.predPosition == lastToken.sentence.document.getTokenList().size() -1;

		if (Helper.isQuote(firstToken) && (Helper.isQuote(lastToken) || lastTokenEndsDocument)) {
			type = Type.DIRECT;
		} else if (Helper.hasInnerQM(tokenBuffer)) {
			type = Type.MIXED;
		} else {
			type = Type.INDIRECT;
		}

		// Write the tags into the tag sequence. If we're excluding this quotation type, write outside tokens instead
		if (typeSet.contains(type)) {
			tagSequence.addAll(tagBuffer);
		} else {
			for (int i = 0; i < tagBuffer.size(); i++) {
				tagSequence.add(outsideTag);
			}
		}
		
		tagBuffer.clear();
		tokenBuffer.clear();
	}

	/**
	 * Annotates all tokens in a document with BIO(E) labels
	 * @param document
	 * @return
	 */
	public Document label(Document document) {

		// Since we may want to filter some quotation types, we first buffer the tags and then check whether they make a valid annotation
		List<String> tagBuffer = new LinkedList<String>();
		List<Token> tokenBuffer = new LinkedList<Token>();
		List<String> labelList = new ArrayList<String>();

		
		Object prevLabel = outsideTag;
		String prevAttributionId = null;

		// check for attribution labels at each token
		for (Token token : document.tokenList) {
			PARCAttribution contentAtt = token.getAttributionWithRole(Role.CONTENT);

			String currentAttributionId;
			if (contentAtt == null)
				currentAttributionId = null;
			else
				currentAttributionId = contentAtt.id;


			// Special rule for adjacent attributions. If attribution tags change, force
			// a B tag. Otherwise, we would merge attributions.
			boolean bTagForced = false;
			if ((currentAttributionId != null) && (prevAttributionId != null)
					&& !currentAttributionId.equals(prevAttributionId))
				bTagForced = true;
			
			prevAttributionId = currentAttributionId;

			// determine next tag
			String nextTag;

			if (contentAtt != null) {
				// we actually found a CONTENT annotation here, so try to set a B tag
				if (useBTagForce && bTagForced) {
					nextTag = beginTag;
					insertBuffer(tagBuffer, labelList, tokenBuffer);
				} else if (prevLabel.equals(outsideTag)) {
					nextTag = beginTag;
				} else {
					nextTag = insideTag;
				}
				
				tagBuffer.add(nextTag);
				tokenBuffer.add(token);
			} else {   /* no annotation, so process the current buffer contents and set an O tag */
				insertBuffer(tagBuffer, labelList, tokenBuffer);
				labelList.add(outsideTag);
				nextTag = outsideTag;
			}
			
			prevLabel = nextTag;

		}

		// final insert
		insertBuffer(tagBuffer, labelList, tokenBuffer);

		// now copy the labels into the annotation
		Iterator<Token> tokenIter = document.tokenList.iterator();
		Iterator<String> labelIter = labelList.iterator();
		
		while (tokenIter.hasNext()) {
			Token token = tokenIter.next();

			// check if token is already annotated; we'll leave it alone then
			if (token.contentBIOAnnotationGold != null)
				continue;

			String label = labelIter.next();
			token.contentBIOAnnotationGold = label;
		}
		
		return document;
	}


	public static void convertGoldToBio (List<Document> corpus) {
		for (Document doc : corpus) {
			for (Token token : doc.tokenList) {
				String label = token.contentBIOAnnotationGold;
				if (label.equals(END_TAG))
					token.contentBIOAnnotationGold = INSIDE_TAG;
			}
		}
	}
	
}
