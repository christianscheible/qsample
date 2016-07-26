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

import java.io.Serializable;

/**
 * Document ID as used in the Wall Street Journal corpus.
 * Each document is part of a section and is stored in a file, each of which have an integral ID.
 */
public class WSJId implements Serializable, DocumentId {

	private static final long serialVersionUID = 4443044961863001270L;
	
	private Integer section;
	private Integer file;

	
	public WSJId (Integer section) {
		this(section, null);
	}

	public WSJId (String section, String file) {
		this(Integer.parseInt(section), Integer.parseInt(file));
	}

	public WSJId (String section) {
		this(Integer.parseInt(section));
	}
	
	public WSJId (Integer section, Integer file) {
		this.section = section;
		this.file = file;
	}
	
	public int getSectionInt() {
		return section;
	}

	public int getFileInt() {
		return file;
	}
	
	private static String addOffset(int i) {
		if (i < 10) {
			return "0" + i;
		} else {
			return "" + i;
		}
	}
	
	public String getSectionStr() {
		return addOffset(section);
	}
	
	public String getFileStr() {
		return addOffset(file);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof WSJId) {
			WSJId otherId = (WSJId) other; 
			return (this.section == otherId.section) && (this.file == otherId.file);
		} else {
			return false;
		}
	}

	public boolean sectionEquals(WSJId other) {
		return this.section == other.section;
	}
	
	@Override
	public String toString() {
		return getSectionStr() + getFileStr();
	}
}

