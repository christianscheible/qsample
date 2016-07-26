QSample
=======

QSample is a natural language processing tool for automatically
detecting quotations in text.


Requirements
------------

Java JVM (>= 1.7) and Maven (>= 3.0.0) need to be installed. All other
dependencies will be downloaded automatically.


Usage
-----

Install the tool by running the following commands:

	git clone https://github.com/christianscheible/qsample.git
	cd qsample
	mvn compile
	mvn package
	
If the build was successful, you will find two .jar files in `target/`
(with and without dependencies, respectively).

The following line runs the tool on some example documents (plain text,
one document per file):

	java -jar target/qsample-0.1-jar-with-dependencies.jar --sample example/documents/ output

The result of this command is a BIOE-style labeling which you can find
in the `output` directory. For example, in the following snippet,

	Witnesses       O       O
	said            O       C
	that            O       B
	several         O       I
	passengers      O       I
	have            O       I
	broken          O       I
	bones           O       E
	.               O       O
	
the label `C` marks the occurrence of a *cue*, and all words between the
`B` and `E` tag are the *content* of the quotation.


Running an experiment
---------------------

To run an experiment on annotated data, you need to obtain several
resources:

* Penn Attribution Relations Corpus (PARC3, http://homepages.inf.ed.ac.uk/s1052974/resources.php)
* Penn Treebank 2 (https://catalog.ldc.upenn.edu/LDC95T7)
* BBN Pronoun Coreference and Entity Type Corpus (https://catalog.ldc.upenn.edu/LDC2005T33)

Afterwards, you can run experiments based on the configuration files in
`resources/PARC/configs/`. To test the pre-trained models, you need to
adapt the paths in the configuration files. To train a model, you can
simply switch from `TEST` to `TRAIN` mode in the configuration.


More information
----------------

For more information, refer to our paper

	@InProceedings{scheibleklingerpado2016,
		author    = {Scheible, Christian and Klinger, Roman and Pad\'{o}, Sebastian},
		title     = {Model Architectures for Quotation Detection},
		booktitle = {Proceedings of the 54th Annual Meeting of the Association for Computational Linguistics},
		year      = {2016}
	}

	
or have a look at the website at http://www.ims.uni-stuttgart.de/data/qsample


License
-------

Please see `licenses/LICENSE.md`.
