For all reaction like events in a release database (i.e. test_reactome_XX), Gene Ontology (GO) annotations are created for cellular components (C), molecular functions (F), and biological processes (P) using the GO Annotation File (GAF) format. The format is described here http://www.geneontology.org/page/go-annotation-file-gaf-format-21
An example of each annotation is shown here.

UniProtKB	P00813	ADA_HUMAN	GO:0005829	REACTOME:R-HSA-74241	TAS	C	protein	taxon:9606 20130528	Reactome

UniProtKB	P00813	ADA_HUMAN	GO:0004000	PMID:3182793	EXP	F	protein	taxon:9606 20150207	Reactome

UniProtKB	P00813	ADA_HUMAN	GO:0043101	REACTOME:R-HSA-74217	TAS	P	protein	taxon:9606 20171121	Reactome

For cellular components, a GAF entry is generated for each protein associated with an event. The protein’s “compartment” attribute is used to make this annotation, except that if a protein derives from a symbiotic relationship between a human host cell and the organism that provides the protein (including disease-associated symbioses) the GO_CellularComponent is used instead. Currently, species handled in this way are taxons 11676 (HIV), 211044 (Influenza A virus), 1491 (Clostridium botulinum), and 1392 (Bacillus anthracis). The evidence code used for all annotations is “TAS” and the reference is the Reactome reaction like event.

For biological processes, proteins are obtained from a reaction-like event in two ways. If the event has one or more catalyst activities, the proteins are taken only from the physical entity of each catalyst activity. If there is no catalyst, all proteins from the event are used. The proteins are then annotated with the event’s biological process attribute. If the event has no biological process, the parent and “grandparent” pathways are checked for a biological process to use. If, again, no biological process is found, no annotation is made. The evidence code used for all annotations is “TAS” and the reference is the Reactome reaction like event.

For molecular functions, only proteins associated with catalyst activity instances are considered. Specifically, proteins are obtained from the active unit (or physical entity if active unit is not specified) of each catalyst activity for an event. No annotation is made, however, if any of the following are true: no physical entity, no compartment for physical entity, no active unit and physical entity is a complex with multiple and distinct proteins, active unit is a complex, or multiple active units. The evidence code used is “EXP” when PubMed references are attached to the catalyst activity and “TAS” otherwise using the Reactome reaction like event as a reference.
