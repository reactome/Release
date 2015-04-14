Tool to extracts some useful annotation from all the SBML files (coming from BioModels Database) present in a given folder.
The aim is to later find relevant pathway from Reactome matching individual models, and therefore increase the linkings between the 2 resources.

How to run the tool:
* edit variable MODELS_FOLDER_PATH in ExtractAnnoFromFile.java to point to the folder containing the SBML files
* run: ant


Current shortcomings:
---------------------
* Extracts all UniProt identifiers for a given SBML species; if no UniProt, extracts all ChEBI; if no, extracts all ChEMBL Compound; if no, extracts all Ensembl identifiers.
* Does not consider qualifiers.
* Code could be better written and optimised.


Open questions:
---------------
* handling on non human related models?
* handling of SBML species annotated with several identifiers:
  complexes using hasPart (see BIOMD0000000015)
  element not found in public database described using hasVersion (see BIOMD0000000004)
* usage of qualifiers (often not a simple 'is', but can be 'hasVersion', 'isHomologTo', ...)
 
