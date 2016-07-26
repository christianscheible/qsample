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


package ims.cs.lingdata;

/**
 * Holds an ID for a sentence.
 * Can be calculated from the document's ID together with the Gorn address of the sentence.
 */
public class SentenceId {

	private WSJId wsjId;
	private GornAddressList gorn;
	
	public SentenceId(WSJId wsdId, GornAddressList gorn) {
		this.gorn = gorn;
		this.wsjId = wsdId;
	}

	public WSJId getWsjId () {
		return wsjId;
	}

	public GornAddressList getGorn() {
		return gorn;
	}

	@Override
	public String toString() {
		return "" + wsjId + ":" + gorn;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SentenceId) {
			SentenceId objId = (SentenceId) obj;
			return this.wsjId.equals(objId.wsjId) && this.gorn.equals(objId.gorn);
			//FIXME: maybe the gorn thing doesn't work
		} else {
			return false;
		}
	}
	
}
