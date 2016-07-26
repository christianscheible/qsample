# Introduction #

This project, the Joint Fine-Grained Sentiment Analysis Tool (JFSA), contains the source code used to perform experiments with probabilistic models for aspect and subjective phrase detection. We plan to make this project to be an easy to use toolkit, however, in the moment, this is not optimized for end users. For instance, the persistance layer for trained models is quite experimental. In addition, the source code is really not very nice. We are working on improving these things, however, please be warned.

If you still want to go into depth, have a look at the bottom of class `sc.rk.targsubj.TargSubjSpanNER.scala`.
At the bottom, there is the main method from which you can follow what is happening. If you want to make changes to the features, have a look at TSData.scala. If you would like to contribute, send me a mail.

# Content of Repository #
The dictionary structure is as follows:

* src/ includes all source files in some subdirectories.
* bin/ includes some helping scripts. It assumes that the target of your compilation is in target/
* 3rdparty/ contains ark-tweet-nlp-0.3.2.jar which seems not to be available via Maven
* data/ contains example data und should contain other corpora which you use. It contains some third party dictionaries and models as well.
* ini/ contains example initialization files and should contain your new ini files as well
* models/ will contain pretrained models (some preliminary models might already be in).

# Installing #
After cloning the whole repository, you should call the following from the main directory of the project (in which all the folders mentioned above are). I did that on a Mac OS X 10.9.5 computer with Java 1.7 and Maven 2.0.

Install ark-tweet in your Maven directory.

`source bin/install-ark-tweet-nlp.sh`

Compile everything. Warnings are expected. Errors not.

`mvn compile`

Make one jar.

`mvn package`

That results in a jar file in the target directory (jfsa-0.1-jar-with-dependencies.jar) with all dependencies and a jar file with the classes of JSFA (jfsa-0.1.jar)

If you have problems compiling, you can (later, when things get stable) get both jar files (hopefully always in the latest version as in this repository) from

[https://drive.google.com/open?id=0BxoLZ-rhm8qzcVJLLXJxREpPUlk&authuser=0](`https://drive.google.com/open?id=0BxoLZ-rhm8qzcVJLLXJxREpPUlk&authuser=0`)

# Data #
This package does not come with any really useful annotated corpus. However, an example annotation taken from the USAGE corpus (see references below) of some texts is available in `data/en-coffeemachine-a1.txt`. The aspect and subjective phrase annotations are in `data/en-coffeemachine-a1.csv` and the relations are in `data/en-coffeemachine-a1.rel`. The file format should be self-explanatory. The `.txt. file consists of three columns. The first is an internal ID, the second is not used currently (but was thought of expressing global subjectivity, you can use it, it increases performance a bit). Remaining is the text.

The `csv` file consists of lines for subjective phrases and aspects (denoted with "target" at the beginning of the line). The second column is the internal ID.

# Running the system on pre-trained models #

When you have pretrained models at hand (trained as described in the next section) or you want to use the pretrained models delivered as in the folder `models/`, you should run

`java -Xmx2g -cp target/jfsa-0.1.jar:target/jfsa-0.1-jar-with-dependencies.jar sc.rk.targsubj.TargSubjSpanNER modelfile.jfsa inputdata.txt outputdata.txt`

or

`./bin/run.sh modelfile.jfsa inputdata.txt outputdata.txt`

The input data can have `csv` and `rel` at the same place, then an evaluation is performed. In any case, just the `csv` data is written to `outputdata.csv`.
Running pipeline models like that currently does not work; I hope to find the time to implement that soon. The output for the joint models is also not complete (everything is predicted, but the output methods are just depricated, that is simple to be solved, I hope to do that soon).



# Starting your first experiment and training models #

You can just start everything by calling
`./bin/run.sh PATHTOINIFILE`
This is using the compiled classes and the jar with all libraries.

The ini/ directory contains some example ini files using the example data. For actually using the method for something meaningful, you clearly need a bigger corpus. You could for instance use JPDA or USAGE. I can help with conversion to the format needed for JFSA, send me a mail. The ini-files can contain a parameter `modelFileName` in which the trained models are stored.

# Current Limitations #
The main limitation is, that the system is not optimized for and tested when using pretrained models.

In addition, experiments showed that for aspect recognition, the joint model is better than the pipeline model. For detecting subjective phrases which are not targeting a specific aspect, the pipeline model might be better. Joining these two approaches in one workflow is a to-do as well.

# More information #

Several papers are explaining the content of the system in this repository. You probably want to read

* Roman Klinger and Philipp Cimiano. Bi-directional Inter-dependencies of Subjective Expressions and Targets and their Value for a Joint Model. In Proceedings of the 51st Annual Meeting of the Association for Computational Linguistics, pages 848-854, Sofia. Bulgaria. 2013 [http://aclweb.org/anthology/P/P13/P13-2147.pdf](http://aclweb.org/anthology/P/P13/P13-2147.pdf)


and

* Roman Klinger and Philipp Cimiano. Joint and Pipeline Probabilistic Models for Fine-Grained Sentiment Analysis: Extracting Aspects, Subjective Phrases and their Relations. In Sentiment Elicitation from Natural Text for Information Retrieval and Extraction (SENTIRE), IEEE International Conference on Data Mining Workshops (ICDMW),  Dallas. USA. 2013 [http://www.roman-klinger.de/publications/joint-aspect-subjectivity-with-reference.pdf](http://www.roman-klinger.de/publications/joint-aspect-subjectivity-with-reference.pdf)

Some experiments are described in this paper:

* Roman Klinger and Philipp Cimiano. The USAGE review corpus for fine-grained, multi-lingual opinion analysis. In Language Resources and Evaluation Conference,  Reykjavík. Iceland. May 2014 [http://www.lrec-conf.org/proceedings/lrec2014/pdf/85_Paper.pdf](http://www.lrec-conf.org/proceedings/lrec2014/pdf/85_Paper.pdf)

The model in this repository has been used to generated the baseline for the shared task on fine-grained sentiment analysis in German:

* Josef Ruppenhofer, Roman Klinger, Julia Maria Struß, Jonathan Sonntag, and Michael Wiegand. IGGSA Shared Tasks on German Sentiment Analysis. In Workshop Proceedings of the 12th Edition of the KONVENS Conference. Hildesheim. Germany. October 2014 [http://opus.bsz-bw.de/ubhi/volltexte/2014/319/pdf/04_01.pdf](http://opus.bsz-bw.de/ubhi/volltexte/2014/319/pdf/04_01.pdf)

# Acknowledgements #
This work has been supported by 

* CITEC, Bielefeld University (project the MMI project in It's OWL): 01/2013-09/2014
* IESL, University of Massachusetts Amherst: 10/2013-12/2013
* IMS, University of Stuttgart: 10/2014-09/2015 


# 3rd party libraries and resources #

The file data/dic/subjclueslen1-HLTEMNLP05.tff is from http://mpqa.cs.pitt.edu/lexicons/subj_lexicon/
Thanks to Jan Wiebe and her colleagues for making it available under GPL.

The file 3rdparty/ark-tweet-nlp/ark-tweet-nlp-0.3.2.jar is from https://github.com/brendano/ark-tweet-nlp
Thanks to Brendan O'Connor and Kevin Gimpel for making it available under GPL 2.

The files data/dic/sentiwordnet3.txt and data/dic/SentiWordNet_3.0.0.txt are from http://sentiwordnet.isti.cnr.it/
Thanks to Andrea Esuli and Fabrizio Sebastiani for making it available under the Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0) license.