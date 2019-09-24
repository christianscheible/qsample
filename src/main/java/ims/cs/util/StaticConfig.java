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


package ims.cs.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * A static configuration class
 * Created by scheibcn on 5/30/16.
 */
public abstract class StaticConfig {
    public enum Model {CRF, GREEDY, SAMPLE}
    public enum CliMode {TRAIN, TEST, TEXT}

    // FEATURES
    // - syntactic
    public static boolean dependencyFeatures = true;
    public static boolean dependencyParentRel = true;
    public static boolean dependencyParentRelHead = true;
    public static boolean dependencyChildRel = true;
    public static boolean dependencyChildRelHead = true;

    public static boolean constituentFeatures = true;
    public static boolean constituentLevel = true;
    public static boolean constituentLeftmost = true;
    public static boolean constituentGoverning = true;
    public static boolean constituentAncestorL = true;
    public static boolean constituentParent = true;
    public static boolean constituentBinning = false;
    public static boolean constituentBinningStacked = false;

    public static boolean dependencyCueDependent = true;
    public static boolean sentenceHasCue = false;

    // - lexical
    public static boolean lexicalToken = false;
    public static boolean lexicalLemma = false;
    public static boolean lexicalPos = false;
    public static boolean lexicalBigram = false;
    public static int lexicalWindowSize = 5;

    public static boolean sentenceHasQuote = true;
    public static boolean sentenceHasNe = true;
    public static boolean sentenceHasPronoun = true;
    public static boolean sentenceLength = true;
    public static boolean sentenceLengthBinning = false;
    public static boolean sentenceLengthBinningStacked = false;

    public static boolean documentQuotationFeature = true;
    public static boolean documentOffsetConjunction = true;


    // PREPROCESSING
    public static boolean useGoldPreprocessing = true;
    public static boolean useBioeTags = true;
    public static String quotationTypes = "DIM";
    public static boolean flattenQuotes = true;


    // HYPERPARAMETERS
    // - sampling iteration
    public static int outerIter = 30;  // best 30
    public static int innerIter = 50;  // best 50
    public static int predictionIter = 1000;  // best 1000
    public static int predictEvery = 10;  // best 10
    public static int maxNumTrials = 10;  // best 10

    // - span length
    public static int maxCueDistanceHeuristic = 30;  // best 30
    public static int maxLengthHeuristic = 50;  // best 50

    public static int maxCueDistanceSampling = 30;  // best 30
    public static int maxLengthSampling = 75;  // best 75


    // - margins
    public static double beginMargin = 25;  // best 25
    public static double endMargin = 25;    // best 25
    public static double cueMargin = 25;    // best 25

    public static int samplerMarginPositive = 15;  // best 15
    public static int samplerMarginNegative = 1;   // best 1


    // -  temperature
    public static double beginTemperature = 10;  // best 10
    public static double endTemperature = 10;    // best 10
    public static double cueTemperature = 10;    // best 10


    // - training options
    public static boolean jackknifing = false;  // best false


    // PATHS
    // - input data location
    public static String parcRoot = "/mount/corpora11/d7/Users/scheibcn/quotations/Data/PARC3_complete";
    public static String pdtbWsjRawDirectory = "/mount/corpora11/d7/Users/scheibcn/quotations/Data/PTB/treebank2/raw/wsj/";
    public static String bbnPath = "/mount/corpora11/d7/Users/scheibcn/quotations/Data/BBN/bbn-pcet/data/WSJtypes-subtypes-fixed/";

    // - output locations
    public static String coreNlpOutputDirectory = "/mount/corpora11/d7/Users/scheibcn/quotations/Data/WSJ_corenlp";
    public static String outputDirectory = "/home/users1/scheibcn/quotations/results/txt/joint-first-run";

    // BEHAVIOR / COMMAND LINE OPTIONS
    public static boolean verbose = true;
    public static CliMode cliMode = CliMode.TEST;
    public static String inputDirectory = "";
    public static Model modelForTextFileMode = Model.SAMPLE;
    public static boolean cacheParses = true;
    public static boolean oneFilePerInput = false;

    public static String crfModelFile = "resources/PARC/models/acl2016.goldtok.crfmodel";
    public static String perceptronModelFile = "resources/PARC/models/acl2016.goldtok.models";


    /**
     * Load configuration from file
     * @param fileName
     * @throws IOException
     */
    public static void loadConfig(String fileName) throws IOException {
        System.out.println("Loading configuration from file " + fileName);
        Properties properties = new Properties();
        properties.load(new FileInputStream(fileName));

        // some reflection hacking to load the properties
        Field[] fields = StaticConfig.class.getDeclaredFields();
        for(Field f: fields) {
            f.setAccessible(true);
            String name = f.getName();
            Object value = properties.get(name);

            try {
                if (f.getType() == double.class) {
                    f.set(null, Double.parseDouble((String) value));
                } else if (f.getType() == boolean.class) {
                    f.set(null, Boolean.parseBoolean((String) value));
                } else if (f.getType() == int.class) {
                    f.set(null, Integer.parseInt((String) value));
                } else if (f.getType() == CliMode.class) {
                    f.set(null, CliMode.valueOf((String) value));
                } else if (f.getType() == String.class) {
                    f.set(null, value);
                } else if (f.getType() == Model.class) {
                    f.set(null, Model.valueOf((String) value));
                } else {
                    System.out.println(f.getName() + " = " + value + " " + f.getType());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            f.setAccessible(false);
        }

    }

    /**
     * Save configuration to file
     * @param fileName
     * @throws IOException
     */
    public static void saveConfig(String fileName) throws IOException {
        Properties properties = new Properties();

        // use reflection to iterate over all fields
        Field[] fields = StaticConfig.class.getDeclaredFields();
        for(Field f: fields) {
            f.setAccessible(true);
            try {
                properties.put(f.getName(), f.get(null).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        properties.store(new FileOutputStream(fileName), "");
    }


}
