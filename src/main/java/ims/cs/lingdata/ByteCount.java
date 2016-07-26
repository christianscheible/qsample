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
 * Byte offset information
 */
public class ByteCount {
	public int begin;
	public int end;
	
	public ByteCount (int begin, int end) {
		this.begin = begin;
		this.end = end;
	}

	public ByteCount(String value) {
		String[] tokens = value.split(",");
		begin = Integer.parseInt(tokens[0]);
		end = Integer.parseInt(tokens[1]);
	}

	public int getBegin() {
		return begin;
	}


	public int getEnd() {
		return end;
	}
	
	@Override
	public String toString() {
		return "" + begin + "," + end;
	}

}
