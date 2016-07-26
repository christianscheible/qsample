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
 * A document ID for plain text documents.
 * Since we require a WSJ-like directory structure, we can generate IDs from that.
 */
public class PlainTextDocId implements DocumentId {

    String sectionStr;
    String fileStr;

    public PlainTextDocId (String section, String file) {
        sectionStr = section;
        fileStr = file;
    }

    @Override
    public String getSectionStr() {
        return sectionStr;
    }

    @Override
    public String getFileStr() {
        return fileStr;
    }

    @Override
    public String toString() {
        return sectionStr + "," + fileStr;
    }
}
