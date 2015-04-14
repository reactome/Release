# Models2Pathways
Automatically maps models from BioModels to Reactome pathways based on analysis results.

## Purpose
Models2Pahtways is a tool to find programaticly relation between Reactome pathways and models of BioModels.

## How does it work?

### BioModels
To get the significant information of a BioModel, it is requestion all BioModels containing a Reactome species. Each BioModel is
represented in a SBML-Format and using an extraction algorithm and the BioModels-WS, it is getting all the the important data out
of it, like: 

- Name
- BioModel Id
- Species
- All annotations

The set of annotations is extracted on a base of namespaces, which are shared between Reactome and BioModels: 

- uniprot
- chebi
- obo.chebi
- ensembl

###Reactome
To find the relations between Reactome pathways and models of BioModels, the excrated BioModels annotations have to be analysed.
For analysing the annotation, it is using the core of the Reactome Analysis Tool. This core allows to analysis a set of annotations
and returns statictical result of pathways which are containg this annotations and so it answer the question, does a BioModel, based
on it annotations, have an overlap with one or more Reactome pathways.

###Core
The core is managing and using the Reactome and BioModels part. 





##Parameter
Usage: java org.reactome.server.models2pathways.core.entrypoint.Models2Pathways
                [(-s|--significantFDR) <significantFDR>] [(-e|--extendedFDR) <extendedFDR>] (-o|--output) <output> [(-c|--coverage) <coverage>] [(-b|--biomodels) <biomodels>] (-r|--reactome) <reactome>

  [(-s|--significantFDR) <significantFDR>]
        Value of the FDR for significant results (default: 0.005)

  [(-e|--extendedFDR) <extendedFDR>]
        Value of the FDR for possible results

  (-o|--output) <output>
        Path to output tsv

  [(-c|--coverage) <coverage>]
        minimum pathway reaction coverage (default: 0.5)

  [(-b|--biomodels) <biomodels>]
        Path to folder of BioModels files. ALTERNATIVE TO BioModels-Webservice!

  (-r|--reactome) <reactome>
        Path to Reactome intermediate file, containing preprocessed to for the
        analysis
