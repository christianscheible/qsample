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

import java.util.List;
import java.util.Map;

import ims.cs.lingdata.Types.PartitionName;

/**
 * Abstract Corpus class.
 * A corpus has training, dev, and test partitions as well as a document list
 */
public abstract class Corpus {
	
	List<Document> docList;
	private Map<PartitionName, Partition> partitionMap;
	
	public abstract Partition getTrain();
	public abstract Partition getDev();
	public abstract Partition getTest();
	
	public List<Document> getDocumentList() {
		return docList;
	}
	
	public void setDocumentList(List<Document> docList) {
		this.docList = docList;
	}
	public Map<PartitionName, Partition> getPartitionMap() {
		return partitionMap;
	}
	public void setPartitionMap(Map<PartitionName, Partition> partitionMap) {
		this.partitionMap = partitionMap;
	}

}
