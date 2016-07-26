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

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import ims.cs.bbn.BbnNeParser;
import ims.cs.lingdata.*;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import ims.cs.lingdata.Types.Genre;
import ims.cs.lingdata.Types.PartitionName;
import ims.cs.parc.xml.PARCParser;

/**
 * Corpus implementation for PARC data
 */
public class PARCCorpus extends Corpus {

	private File directory;
	private File ptbNewsDocumentFile = new File("resources/PARC/news.txt");
	
	private final String TRAIN_DIR = "train";
	private final String DEV_DIR = "dev";
	private final String TEST_DIR = "test";
	
	private PARCParser parser;

	Set<String> newsIdSet = new HashSet<>();
	

	private static PARCCorpus instance;
	
	public PARCCorpus () throws ParserConfigurationException, SAXException, IOException {
		this(StaticConfig.parcRoot);
	}

	public void readNewsIds() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(ptbNewsDocumentFile));
		String[] newsIds = reader.readLine().split(", ");
		for (String id : newsIds) newsIdSet.add(id);
	}

	/**
	 * Reads the PARC data from the given directory
	 * @param corpusDirectory
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public PARCCorpus (String corpusDirectory) throws ParserConfigurationException, SAXException, IOException {
		if (corpusDirectory != null) {
			directory = new File(corpusDirectory);
		}

		// find out which documents are actually annotated (only the news ones!)
		readNewsIds();

		// initialize parser and read the three parts
		parser = PARCParser.getInstance();

		System.out.println("PARC corpus directory: " + directory);
		System.out.println("  Reading training data");
		Partition train = readSections(new File(directory, TRAIN_DIR));
		System.out.println("  Reading dev data");
		Partition dev = readSections(new File(directory, DEV_DIR));
		System.out.println("  Reading test data");
		Partition test = readSections(new File(directory, TEST_DIR));

		// store the three partitions
		Map<PartitionName,Partition> partitionMap = new HashMap<Types.PartitionName, Partition>();
		partitionMap.put(PartitionName.TRAIN, train);
		partitionMap.put(PartitionName.DEV, dev);
		partitionMap.put(PartitionName.TEST, test);
		setPartitionMap(partitionMap);

		// add extra annotations
		makeDocumentsAnnotation();
		System.out.println("Adding gold NEs");
		annotateBbnNes();
	}

	/**
	 * Loads named entity annotations from the BBN dataset
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void annotateBbnNes() throws ParserConfigurationException, SAXException, IOException {
		BbnNeParser neParser = BbnNeParser.getInstance();

		for (Document document : getTest().docList) {
			neParser.augmentDocumentXml(document);
		}
		for (Document document : getDev().docList) {
			neParser.augmentDocumentXml(document);
		}

		for (Document document : getTrain().docList) {
			neParser.augmentDocumentXml(document);
		}
	}


	/**
	 * Loads the raw text file for the given document ID
	 * @param wsjId
	 * @return
	 */
	private static File getRawFile(WSJId wsjId) {
		String sectionId = wsjId.getSectionStr();
		String fileId = wsjId.getFileStr();
		
		return new File(new File(StaticConfig.pdtbWsjRawDirectory, sectionId), String.format("wsj_%s%s", sectionId, fileId));
	}

	/**
	 * Some internal bookkeeping of document lists
	 */
	private void makeDocumentsAnnotation() {
		List<Document> documentList = new ArrayList<Document>();
		Map<PartitionName, Partition> partitionMap = getPartitionMap();
		for (Partition partition : partitionMap.values()) {
			List<Document> partitionDocumentList = partition.docList;
			documentList.addAll(partitionDocumentList);
		}
		
		setDocumentList(documentList);
	}

	/**
	 * Adds the raw text to a document (by loading it from a file)
	 * @param wsjId
	 * @return
	 * @throws IOException
	 */
	private static String readRawText(WSJId wsjId) throws IOException {
		File rawFile = getRawFile(wsjId);
		String documentText = new String(Files.readAllBytes(rawFile.toPath()));
		documentText = documentText.replaceFirst("\\.START", "      ");

		return documentText;
	}

	/**
	 * Reads all WSJ sections in the given directory
	 * @param partDirectory
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	private Partition readSections (File partDirectory) throws IOException, SAXException {
		File[] sectionDirList = partDirectory.listFiles();

		Map<String, List<Document>> sectionMap = new LinkedHashMap<String, List<Document>>();
		List<Document> partitionDocumentList = new ArrayList<Document>(sectionDirList.length * 100);
		Arrays.sort(sectionDirList);

		// iterate over the sections found in the directory
		for (File sectionDir: sectionDirList) {
			String sectionId = sectionDir.getName();
			List<Document> sectionDocumentList = new ArrayList<Document>(100);

			File[] fileList = sectionDir.listFiles();
			Arrays.sort(fileList);

			// load each file in the current section
			for (File f: fileList) {
				String fileId = f.getName().substring(6, 8);
				if (StaticConfig.verbose) System.out.println("Reading " + f);

				// make document ID
				WSJId fileWsjId = new WSJId(sectionId, fileId);
				String idString = fileWsjId.getSectionStr() + fileWsjId.getFileStr();

				// check if we have a news document, skip otherwise
				if (!newsIdSet.contains("wsj_" + idString)) {
					if (StaticConfig.verbose) System.out.println("Skipping " + idString + " because it's not news.");
					continue;
				}

				// parse document and do bookkeeping
				Document document = parser.parseFile(f);
				document.docId = fileWsjId;
				document.genre = Genre.NEWS;
				
				addSentenceIds(document.sentenceList, fileWsjId);
				sectionDocumentList.add(document);
				partitionDocumentList.add(document);
				
				// load raw text
				String rawText = readRawText(fileWsjId);
				document.text = rawText;
				
			}			

			// store document
			sectionMap.put(sectionId, sectionDocumentList);
		}
		
		Partition partition = new Partition();
		partition.sectionMap = sectionMap;
		partition.docList = partitionDocumentList;
		
		return partition;
	}

	/**
	 * Generates sentence IDs for all sentences in the given list
	 * @param list
	 * @param wsjId
	 */
	private static void addSentenceIds(List<Sentence> list, WSJId wsjId) {
		for (Sentence sentence : list) {
			GornAddressList gorn = sentence.gorn;
			SentenceId id = new SentenceId(wsjId, gorn);
			sentence.sentenceId = id;
		}
	}


	public static PARCCorpus getInstance() throws ParserConfigurationException, SAXException, IOException {
		if (instance == null) {
			instance = new PARCCorpus();
		}
		return instance;
	}
	

	public Partition getTrain() {
		return getPartitionMap().get(PartitionName.TRAIN);
	}

	public Partition getDev() {
		return getPartitionMap().get(PartitionName.DEV);
	}

	public Partition getTest() {
		return getPartitionMap().get(PartitionName.TEST);
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		PARCCorpus c = getInstance();
	}

}
