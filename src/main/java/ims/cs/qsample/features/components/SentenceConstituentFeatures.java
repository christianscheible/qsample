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

import java.util.LinkedList;
import java.util.List;

import ims.cs.lingdata.Sentence;
import ims.cs.lingdata.Token;
import ims.cs.qsample.features.FeatureSet;
import edu.stanford.nlp.trees.Tree;
import ims.cs.util.StaticConfig;

/**
 * Token features derived from the constituency parse of a sentence
 */
public abstract class SentenceConstituentFeatures {

	// feature names
	private static final String LEVEL_FEATURE = "LVL";
	private static final String LEFTMOST_FEATURE = "IS-LEFTMOST";
	private static final String GOV_FEATURE = "GOV:";
	private static final String AL_FEATURE = "AL:";
	private static final String PARENT_FEATURE = "PARENT:";

	public static void extract(Sentence s) {
		addTreeFeatures(s, s.tree);
	}

	/**
	 * Class for keeping track of node-level pairs
	 */
	private static class NodeFeatures {
		String label;
		Integer level;
		
		public NodeFeatures(String label, int depth) {
			this.label = label;
			this.level = depth;
		}
	}

	/**
	 * Add tree features recursively
	 * @param s
	 * @param t
	 */
	private static void addTreeFeatures(Sentence s, Tree t) {
		addTreeFeatures(s, t, 0, new LinkedList<NodeFeatures>(), null, true, null);
	}

	/**
	 * Recursion step for tree featues
	 * @param sentence
	 * @param t complete tree
	 * @param level current level
	 * @param governingLabels list of governing labels
	 * @param parent information about direct parent
	 * @param isLeftmost is the node the leftmost one in the constituent specified by ancestorWhereLeftmost
	 * @param ancestorWhereLeftmost
	 */
	private static void addTreeFeatures(Sentence sentence, Tree t, int level, List<NodeFeatures> governingLabels, NodeFeatures parent, boolean isLeftmost, NodeFeatures ancestorWhereLeftmost) {


		if (t.isLeaf()) {   /* terminal nodes */
			// get the current token represented by this subtree
			Token pToken = sentence.treeLookup.get(t);

			// check if token is null. this can happen if the token was unaligned previously (e.g., because of
			// a parser error)
			if (pToken == null) {
				if (StaticConfig.verbose)
					System.err.println(sentence.sentenceId + " Dropping tree without associated token: " + t + " ");
				return;
			}

			FeatureSet fs = pToken.boundaryFeatureSet;

			// leftmost feature (see Pareti paper for description)
			if (StaticConfig.constituentLeftmost && isLeftmost)
				fs.add(LEFTMOST_FEATURE);

			// level in tree
			if (StaticConfig.constituentLevel) {
				fs.add(LEVEL_FEATURE + level);
				addLevelBinHeuristic(pToken, LEVEL_FEATURE, level);
			}

			// leftmost feature label
			if (StaticConfig.constituentAncestorL) {
				fs.add(AL_FEATURE + "LBL:" + ancestorWhereLeftmost.label);
				fs.add(AL_FEATURE + "LVL:" + ancestorWhereLeftmost.level);
				
				addLevelBinHeuristic(pToken, AL_FEATURE + "LVL", ancestorWhereLeftmost.level);
			}

			// parent in constituent tree
			if (StaticConfig.constituentParent) {
				fs.add(PARENT_FEATURE + "LBL:" + parent.label);
			}

			// labels of all ancestors
			if (StaticConfig.constituentGoverning) {   /* "Ancestor" features in the paper */
				for (NodeFeatures nf: governingLabels) {
					// label with and without depth
					fs.add(GOV_FEATURE + nf.label + "@" + nf.level);   /* ambiguous in paper */
					fs.add(GOV_FEATURE + nf.label);
					fs.add(GOV_FEATURE + nf.label + "@-" + (level - nf.level));   /* ambiguous in paper */

					addLevelBinHeuristic(pToken, GOV_FEATURE + nf.label + "@", nf.level);
					addLevelBinHeuristic(pToken, GOV_FEATURE + nf.label + "@-", (level - nf.level));
				}
			}
		} else {  // non-terminal node
			List<Tree> childList = t.getChildrenAsList();
			String label = t.label().toString();

			// copy governing node features for next recursion step
			List<NodeFeatures> governingLabelsUpdate = new LinkedList<NodeFeatures>(governingLabels);
			governingLabelsUpdate.add(new NodeFeatures(label, level));

			// set leftmost ancestor
			if (ancestorWhereLeftmost == null) {
				ancestorWhereLeftmost = new NodeFeatures(label, level);
			}

			// check for pre-terminals -- otherwise, set the leftmost flag for the first constituent
			if (childList.size() > 1) {
				isLeftmost = true;
			}

			// call function for all children
			for (Tree child : childList) {
				addTreeFeatures(sentence, child, level + 1, governingLabelsUpdate, new NodeFeatures(label, level), isLeftmost, ancestorWhereLeftmost);
				isLeftmost = false;
				ancestorWhereLeftmost = null;
			}
		}
	}

	/**
	 * Binning for levels
	 * @param mToken
	 * @param feature
	 * @param value
	 */
	private static void addLevelBinHeuristic(Token mToken, String feature, int value) {
		if (!StaticConfig.constituentBinning) return;
		
		FeatureSet fs = mToken.boundaryFeatureSet;

		int[] bins = new int[] {0, 1, 2, 3, 5, 7, 10, 13, 16, 20, 25, 40, 1000  };
		
		for (int i=0; i < bins.length - 1; i++) {
			int threshLower = bins[i];
			int threshUpper = bins[i + 1];

			// threshold satisfied? add bin feature!
			if (value <= threshUpper) {
				if (StaticConfig.constituentBinningStacked) {
					fs.add(feature + "(<=)" + threshLower);
					if (value >= threshLower)
						fs.add(feature + "(>=)" + threshLower);
				} else if (value > threshLower) {
					fs.add(feature + "(EXACT)" + threshLower);
				}
			}
		}
	}

	
}
