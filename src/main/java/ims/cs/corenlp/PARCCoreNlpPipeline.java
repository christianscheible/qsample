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


package ims.cs.corenlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import ims.cs.lingdata.*;
import ims.cs.parc.ParcUtils;
import edu.stanford.nlp.pipeline.CustomAnnotationSerializer;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import ims.cs.parc.PARCCorpus;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

/**
 * A pipeline for processing our documents with CoreNLP
 */
public class PARCCoreNlpPipeline implements Iterable<Document>{

	private StanfordCoreNLP pipeline;
	private List<Document> documentList;

	private static CustomAnnotationSerializer ser = new CustomAnnotationSerializer(false, false);


	/**
	 * An iterator over documents after CoreNLP processing
	 */
	class PARCCoreNlpDocumentIterator implements Iterator<Document> {

		private Iterator<Document> documentIterator;

		public PARCCoreNlpDocumentIterator(List<Document> documentList) {
			this.documentIterator = documentList.iterator();
		}
		
		public boolean hasNext() {
			return this.documentIterator.hasNext();
		}

		public Document next() {
			Document pDocument = documentIterator.next();
			
			// check if this document has been processed before
			if (pDocument.isCoreNlpProcessed)
				return pDocument;
			
			DocumentId id = pDocument.docId;
			File processedFile = getParsedFileName(id);

			Annotation annotation = null;
			boolean failedToLoad = false;

			// try to load the cached file
			if (processedFile.exists()) {
				try {
					annotation = deserializeAnnotation(processedFile);
				} catch (IOException e) {
					// in case of failure, trigger parser in the next step
					System.err.println("Failed to load " + processedFile + ", falling back to parser");
					failedToLoad = true;
				}
			} 
			
			// Parse the document in case no serialized file was available or readable
			if (!processedFile.exists() || failedToLoad) {
				annotation = parseDocumentFromRaw(pDocument);
				try {
					serializeAnnotation(annotation, processedFile);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Unable to store annotation in file");
				}
			}		    
			
			// go through all tokens and flatten the quotes
			List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);

			if (StaticConfig.flattenQuotes) {
				for (CoreMap sentence : sentences) {
					List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);

					if (tokens == null)
						throw new Error("Document does not have TokensAnnotation");

					for (CoreLabel cl : tokens)
						Helper.flattenQuotes(cl);
				}
			}

			// align gold and predicted tokens
			DocumentAligner da = new DocumentAligner(pDocument, annotation);
			Document document = da.getDocument();
			document.isCoreNlpProcessed = true;

			// post-processing
			ParcUtils.sanitizeQuotationMarks(document);
			ParcUtils.anonymizeNamedEntities(document);
			ParcUtils.doParagraphAnnotation(document);
			ParcUtils.markParagraphQuotes(document);

			// find head verbs
			for (Sentence sentence : document.sentenceList)
				ParcUtils.markHeadVerbs(sentence);

			return document;
		}

		public void remove() { throw new UnsupportedOperationException("Not implemented"); }
	}


	/**
	 * basic setup, but do not allow instantiation without providing data
	 */
	private PARCCoreNlpPipeline() { }

	public PARCCoreNlpPipeline(List<Document> documentList) {
		this();
		this.documentList = documentList;
	}


	/**
	 * Determines the location of the cached parse
	 * @param id
	 * @return
	 */
	public static File getParsedFileName(DocumentId id) {
		File dirFile = new File(StaticConfig.coreNlpOutputDirectory, id.getSectionStr());
		File sentenceFile = new File(dirFile, id + ".cSer.gz");
		
		return sentenceFile;
	}

	/**
	 * Stores CoreNlp output in a file
	 * @param a
	 * @param file
	 * @throws IOException
	 */
	public static void serializeAnnotation(Annotation a, File file) throws IOException {
		File parentFile = file.getParentFile();
		if (!parentFile.exists()) parentFile.mkdirs();

		if (StaticConfig.verbose) System.out.println("(CAS) Writing to file " + file);

		// remove these to try to save memory
		a.remove(CollapsedDependenciesAnnotation.class);
		a.remove(BasicDependenciesAnnotation.class);

		if (StaticConfig.cacheParses) {
			OutputStream ret = ser.write(a, new FileOutputStream(file));
			ret.close();
		}
	}

	/**
	 * Reads CoreNlp output from a file
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Annotation deserializeAnnotation(File file) throws IOException {
		if (StaticConfig.verbose) System.out.println("(CAS) Reading from file " + file);
		Pair<Annotation, InputStream> ret = ser.read(new FileInputStream(file));
		ret.second.close();
		Annotation annotation = ret.first();

		return annotation;
	}


	/**
	 * Initializes the local pipeline dynamically
	 */
	private void setUpPipeline() {
		System.out.println("Need CoreNLP pipeline, initializing ...\n");
		Properties props = new Properties();
		props.put("annotators", "tokenize,ssplit,pos,lemma,parse,ner");
		pipeline = new StanfordCoreNLP(props);
		System.out.println("\n ... done with CoreNLP initialization.");
	}

	/**
	 * Parse document based on raw PTB text (rather than PARC annotation file)
	 * @param document
	 * @return
	 */
	private Annotation parseDocumentFromRaw (Document document) {
		// dynamically set up the parsing pipeline if needed
		// (this could be done in the constructor, but it actually takes some time
		// and is unnecessary once the parses are cached)
		if (pipeline == null) setUpPipeline();

		Annotation coreNlpDocument = new Annotation(document.text);
		pipeline.annotate(coreNlpDocument);
		return coreNlpDocument;		
	}

	
	public Iterator<Document> iterator() { return new PARCCoreNlpDocumentIterator(documentList); }


	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		PARCCorpus c = PARCCorpus.getInstance();

		List<Document> documentList = c.getDocumentList();

		PARCCoreNlpPipeline pipeline = new PARCCoreNlpPipeline(documentList);
		Iterator<Document> cDocumentIterator = pipeline.iterator();
		
		while (cDocumentIterator.hasNext()) {
			Document document = cDocumentIterator.next();
		}

		System.gc();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("No sleep");
		}
		
		Runtime instance = Runtime.getRuntime();
		int mb = 1024 * 1024; 

		System.out.println("Used Memory (MB): " 	+ (instance.totalMemory() - instance.freeMemory()) / mb);

	}

}
