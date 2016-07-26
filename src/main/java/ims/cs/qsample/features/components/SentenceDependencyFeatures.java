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

import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.corenlp.Helper;
import ims.cs.qsample.features.FeatureSet;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import ims.cs.util.StaticConfig;

/**
 * Token features derived from the dependency parse of a sentence
 */
public abstract class SentenceDependencyFeatures {

	// feature names
	private static final String PARENT_REL_PREFIX = "PARENT-REL";
	private static final String PARENT_RELHEAD_PREFIX = "PARENT-REL+HD";
	private static final String CHILD_REL_PREFIX = "CHILD-REL";
	private static final String CHILD_RELHEAD_PREFIX = "CHILD-REL+HD";

	/**
	 * Extract dependency features for all tokens in this sentence
	 * @param sentence
	 */
	public static void extract (Sentence sentence) {
		for (Token pToken : sentence.tokenList) {
			if (StaticConfig.dependencyParentRel || StaticConfig.dependencyParentRelHead) addParentFeature(pToken);
			if (StaticConfig.dependencyChildRel || StaticConfig.dependencyChildRelHead) addChildFeatures(pToken);
		}
	}

	/**
	 * Add features about the parent of the token
	 * @param token
	 */
	private static void addParentFeature(Token token) {
		SemanticGraphEdge parentEdge = Helper.getDependencyParentRel(token);
		
		FeatureSet fs = token.boundaryFeatureSet;
		
		if (parentEdge != null) {
			// plain parent
			if (StaticConfig.dependencyParentRel)
				fs.add(PARENT_REL_PREFIX + "=" + parentEdge.getRelation());

			// parent and relation label
			if (StaticConfig.dependencyParentRelHead)
				fs.add(PARENT_RELHEAD_PREFIX + "=" + parentEdge.getRelation() + "," + parentEdge.getGovernor().lemma());
		}
	}

	/**
	 * Add features about the child of a token
	 * @param pcToken
	 */
	private static void addChildFeatures(Token pcToken) {
		List<SemanticGraphEdge> childEdgeList = Helper.getDependencyChildrenRels(pcToken);
		FeatureSet fs = pcToken.boundaryFeatureSet;

		if (childEdgeList != null) {
			for (SemanticGraphEdge childEdge : childEdgeList) {
				// plain child
				if (StaticConfig.dependencyChildRel)
					fs.add(CHILD_REL_PREFIX + "=" + childEdge.getRelation());

				// child and relation label
				if (StaticConfig.dependencyChildRelHead)
					fs.add(CHILD_RELHEAD_PREFIX + "=" + childEdge.getRelation() + "," + childEdge.getDependent().lemma());
			}
		}
	}


}
