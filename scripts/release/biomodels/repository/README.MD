# Models2Pathways

Models2Pathways is a commandline-tool to find cross references between BioModels and Reactome.

### Version

1.0

### Installation

* To start with it, clone the project to your local machine
* Build a package out of it to get a executable jar-file:
```
mvn clean package
```
* jar-file will be in the target-folder


### Usage

```sh
Usage: java -jar Models2Pathways-1.0-jar-with-dependencies.jar
                (-o|--output) <output> (-b|--biomodels) <biomodels> (-r|--reactome) <reactome> [(-s|--significantFDR) <significantFDR>] [(-e|--extendedFDR) <extendedFDR>] [(-c|--coverage) <coverage>]

  (-o|--output) <output>
        Path to output tsv (MANDATORY)

  (-b|--biomodels) <biomodels>
        Path to folder of BioModels files. (MANDATORY)

  (-r|--reactome) <reactome>
        Path to Reactome intermediate file, containing preprocessed to for the
        analysis (MANDATORY)

  [(-s|--significantFDR) <significantFDR>]
        Value of the FDR for significant results (default: 0.005)

  [(-e|--extendedFDR) <extendedFDR>]
        Value of the FDR for possible results

  [(-c|--coverage) <coverage>]
        minimum pathway reaction coverage (default: 0.6)

```

### Maven-Dependencies

Models2Pathways includes a few thirparty dependencies:

* jsbml 1.0
* analysis-core 1.4.1
* httpclient 4.3.5
* jackson-annotations 2.4.3
* jackson-databind 2.4.3
* junit 4.8.2
* jcl-over-slf4j 1.7.0
* slf4j-api 1.7.0
* slf4j-log4j12 1.7.0
* log4j 1.2.14
* javassist 3.12.1.GA
* jsap 2.1
* spring-core 4.1.5.RELEASE
* spring-context 4.1.5.RELEASE

### Filtered namespaces

To take care, that we only extract annotations, which are possibly in Reactome, we only extract those who are having one of the following namesapces:
* uniprot
* chebi
* obo.chebi
* chembl.compound
* ensembl

### Filtered species

To take care, that we only use BioModels with a species, which are possibly in Reactome, we only take those BioModels, who are having one of the following species:

* HOMO SAPIENS
* ARABIDOPSIS THALIANA
* BOS TAURUS
* DICTYOSTELIUM DISCOIDEUM
* DROSOPHILA MELANOGASTE
* ESCHERICHIA COLI
* MUS MUSCULUS
* MYCOBACTERIUM TUBERCULOSIS
* RATTUS NORVEGICUS
* SACCHAROMYCES CEREVISIAE
* SCHIZOSACCHAROMYCES POMBE


### How to change filtered species?
Go to src/main/resources and adopt species.txt

### How to change trivial chemicals?
Go to src/main/resources and adopt trivialchemicals.txt

### How to change namspaces?
Go to src/main/resources and adopt namespaces.txt

### How to change the default FDR?

### How to change the default reactions coverage?

### How to change the format of the result/output file?

### What is the format of the result/output file?

### What is the 'extended' FDR?

### What is the structure in src/main/resources/species.txt?

### What is the structure in src/main/resources/namespaces.txt?

### What is the structure in src/main/resources/trivialchemicals.txt?
