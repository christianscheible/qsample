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


package ims.cs.qsample.run;

import ims.cs.lingdata.*;
import ims.cs.parc.ProcessedCorpus;
import ims.cs.util.StaticConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by scheibcn on 6/1/16.
 */
public class PlainTextCorpusReader {

    /**
     * Read document, one sentence per line
     * @param file
     * @return
     */
    public static Document readDocument(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        // read all text from file
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        // build a document with some bogus structure
        String text = sb.toString();

        Document d = new Document();
        Sentence s = new Sentence();
        Token t = new Token();

        // add text and set byte count
        t.goldText = text;
        t.goldByteCount = new ByteCount(0, t.goldText.length());

        // bookkeeping
        s.tokenList = new ArrayList<>();
        s.tokenList.add(t);

        d.sentenceList = new ArrayList<>();
        d.sentenceList.add(s);

        d.tokenList = new ArrayList<>();
        d.tokenList.add(t);
        d.text = text;

        // build a document id from the file and directory names
        d.docId = new PlainTextDocId(file.getParentFile().getName(), file.getName());

        reader.close();

        return d;
    }

    public static ProcessedCorpus readDocuments(String directory) throws ClassNotFoundException, SAXException, ParserConfigurationException, IOException {
        List<Document> documentList = new ArrayList<>();

        // import all files in the directory
        File dir = new File(directory);
        File[] files = dir.listFiles();
        Arrays.sort(files);

        for (File file : files) {
            if (StaticConfig.verbose) System.out.println(file);
            Document document = readDocument(file);
            documentList.add(document);
        }

        PlainTextCorpus corpus = new PlainTextCorpus(documentList);

        return new ProcessedCorpus(corpus);
    }


    public static void pipeline() {

    }

    public static Document dummyDocument () {
        Document d = new Document();
        Sentence s = new Sentence();
        Token t = new Token();

        t.goldText = "\"I am very disappointed,\" said Dr. Miller.\n Futher, he reported that everything was fine.";
        t.goldByteCount = new ByteCount(0, t.goldText.length());

        s.tokenList = new ArrayList<>();
        s.tokenList.add(t);

        d.sentenceList = new ArrayList<>();
        d.sentenceList.add(s);

        d.tokenList = new ArrayList<>();
        d.tokenList.add(t);
        d.text = t.goldText;

        d.docId = new PlainTextDocId("dummyTestDirectory1", "dummyTestFile1");

        return d;
    }
}
