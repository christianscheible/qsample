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

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import ims.cs.qsample.features.FeatureSet;

/**
 * Pipe to convert our internal feature set to mallet token feature entries
 * @author scheibcn
 */
public class DocumentFeatureSet2TokenSequence extends Pipe {

	private static final long serialVersionUID = 3218174517742238232L;

	@Override
	public Instance pipe(Instance inst) {

		// ensure that the instance is of the right type
		if (!(inst instanceof PARCDocumentInstance)) {
			throw new UnsupportedOperationException("Expected CoreMap, got " + inst.getClass());
		}


		List<ims.cs.lingdata.Token> tokenList = ((PARCDocumentInstance) inst).document.getTokenList();
		TokenSequence ts = new TokenSequence();

		// iterate over tokens and convert their internal feature sets into Mallet feature sets
		for (ims.cs.lingdata.Token cToken : tokenList) {
			FeatureSet fs = cToken.boundaryFeatureSet;
			Token mToken = new Token(cToken.predText);

			// copy each feature
			for (Object entry : fs) {
				mToken.setFeatureValue(entry.toString(), 1);
			}
			
			ts.add(mToken);
		}
		
		inst.setData(ts);
		
		return inst;
	}
	
}
