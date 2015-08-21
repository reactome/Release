# Orthoinference

## Scripts

### wrapper_orthoinference.pl
- main wrapper script
- takes arguments
	- user *db_user*
	- pass *db_pass*
	- host *db_host* (default: localhost)
	- port *db_port* (default: 3306)
	- source_db *source_database* (default: test_slice_xx_myisam)
	- from *source_species* (default: hsap)
	- r *release_version*
- calls other scripts
	- [tweak_data_model.pl](#tweak_data_model.pl)
	- [infer_events.pl](#infer_events.pl) (run for all target species)
	- [remove_unused_PE.pl](#remove_unused_PE.pl)
	- [updateDisplayName.pl](#updateDisplayName.pl)
- output
	- new database with inference (default: test_reactome_xx)
	- if a source database is specified, its name is used
	  as the base for the database with inference that is
	  produced; the source and target species are appended
	  when specified

<a name="tweak_data_model.pl"></a>	  
#### tweak_data_model.pl
- changes made to the data model of the newly created
  database to house computationally inferred events and
  entities
	- attributes 'TotalProt', 'InferredProt', and
	  'MaxHomologues' added to 'EntitySet', 'Complex',
	  'Polymer', 'Reaction' classes
	- defining attributes changed
		- OpenSet has name and species changed to 'any'
		  and 'all', respectively.
		- BlackBoxEvent and DefinedSet have species
		  changed to 'all'
		- ReferenceDatabase has accessUrl changed to 'all'		

<a name="infer_events.pl"></a>
#### infer_events.pl
- for each species inferred, the species gene to protein
  mapping file and source species (human by default) protein
  to species protein mapping files are used (generated from
  the orthopair release step)
- inference is done at the level of Reaction like events
- Reaction like events are excluded from inference if they:
	- are part of the Influenza, HIV, or Amyloid pathways
	- are on the skip list file normal_event_skip_list.txt
	  (normal events in disease pathways)
	- are chimeric
	- have a related species tag
	- have more than one species tag
	- have entities that collectively have more than one
	  species
	- have a disease tag
	- are manually inferred
	- have a reverse attribute of 'has member' (inferred as
	  part of the higher level event)
	- exist already as in the target species
- hierarchy for the species constructed using the human
  hierarchy's connections between pathways and events
  as a model
- preceding events that were inferred are linked to the
  events they precede allowing events not already in the
  hierarchy to be included
  
##### inferring events
- starts at [infer_event](#infer_events.pl) subroutine
- source event used to create a new inferred event instance
- generic summation and evidence type added
- copied from source event:
	- compartment
	- species
	- name
	- GO Biological Process
- a count of distinct proteins in the source event
  ensures only events with proteins are inferred
- event attributes that are inferred (failure to
  infer these aborts inference of the event unless
  otherwise noted):
	- inputs
	- outputs
	- catalyst activity
		- new catalyst activity created and the
		  following are inferred, except where noted,
		  and added:
			- activities (not inferred; points to
			  source instances)
			- physical entity (catalyst activity
			  inference fails if unsuccessful)
			- active units (unless source is a
			  domain instance)
			- regulations (catalyst activity
			  inference fails if unsuccessful and
			  the regulation is required by the
			  catalyst activity)
	- regulations (failure to infer only aborts
	  inference of the event if the regulation
	  instance is required by the event)
		- new regulation instance created and
		  regulator inferred and added
- count of total and inferred proteins from
  source event added to inferred event
- inferred from and orthologous event slots
  on the inferred event are filled pointing
  to the source event
- orthologous event slot on the source event
  is filled pointing to the inferrred event
- inferred event is cached to prevent
  duplications
- source event is stored to later build hierarchy
  of target species

##### inferring entities
- starts at [orthologous_entity](#orthologous_entity) subroutine

<a name="orthologous_entity"></a>
###### orthologous_entity
- determines type/class of physical entity
  and sends it to the appropriate subroutine
  for inference and returns the resulting
  inferred entity:
	- Genome Encoded Entity/EWAS sent to [create_homol_gee](#create_homol_gee)
	- Complex/Polymer sent to [infer_complex_polymer](#infer_complex_polymer)
	- Set sent to [infer_gse](#infer_gse)
	- Simple Entity returned as is (no new instance inferred;
	  inferred event will point to the original entity)
	- Other Entity returned as is (no new instance inferred;
      inferred event will point to the original entity)
	  NOTE: For prokaryotic species, the entity is cloned
	  and the compartment is changed to intracellular if
	  needed for organelle compartments in the source species
	  that don't exist in the target species

<a name="create_homol_gee"></a>
###### create_homol_gee (Genome Encoded Entity(GEE)/EWAS inference)
- GEEs (that are not EWASs) are not inferred; if inference
  forced by the script (this is the case with polymer and complex
  components/repeated units), a ghost GEE instance is created
  using the compartment, species and name of source GEE
- Each EWAS is inferred to its homologue(s) (uses [infer_ewas](#infer_ewas)
  subroutine)
	- Defined Set inferred if multiple homologues
	- EWAS inferred if one homologue
	- Ghost GEE inferred if no homologue

<a name="infer_ewas"></a>
###### infer_ewas (EWAS inference)
- each homologue of the source EWAS is inferred creating a new
  EWAS instance
- a Reference Gene Product (RGP) is created for the inferred EWAS
  (either ENSEMBL or UniProt) along with a Reference DNA Sequence
  for the RGP 'Reference Gene' slot
- if start and/or end co-ordinates for the source EWAS are greater
  than 1, the text "the co-ordinates are copied over from Homo
  Sapiens" is appended
- modified residues are also inferred and attached to the
  inferred EWAS

<a name="infer_complex_polymer"></a>  
###### infer_complex_polymer (Complex/Polymer inference)
- first, components are tested to ensure at least a certain
  percentage (75% by default) are inferrable or the inference
  of the complex/polymer is aborted (unless forced by the
  script; this is the case with polymer and complex components/
  repeated units)
- the complex/polymer instance is inferred
- components/repeated units inferred using the orthologous_entity
  subroutine
- attributes 'TotalProt', 'InferredProt', and 'MaxHomologues'
  filled
- source complex/polymer used instead of newly inferred complex/polymer
  instance if their components/repeated units are identical (i.e. the
  same instances are used in the source and inferred complex/polymer;
  happens when components/repeated units are only simple or other
  entities)

<a name="infer_gse"></a>
###### infer_gse (Set inference)
- set instance is inferred
- members inferred using the orthologous_entity subroutine
- for open sets, the 'Reference Entity' slot filled
- attributes 'TotalProt', 'InferredProt', and 'MaxHomologues'
  filled
- no inference of the set occurs if proteins are present
  but none are inferrable (unless forced by the script in
  which case a ghost GEE is created)
- defined sets with only one member only have that member
  inferred and returned -- i.e. not contained within an
  inferred set instance
- candidate sets have only members inferred but if no members
  exist in the source set then candidates are inferred only
  if ALL candidates can be successfully inferred
- at least one member/candidate must be successfully inferred
  or the inference of the set is aborted (unless forced by the
  script in which case a ghost GEE is created)
- source set used instead of newly inferred set instance if their
  member/candidate instances are identical (i.e. the same instances
  are used in the source and inferred set; happens when members/
  candidates are only simple or other entities)

<a name="remove_unused_PE.pl"></a>
#### remove_unused_PE.pl
- removes physical entities without referrers (except for simple
  entities and other entities)

<a name="updateDisplayName.pl"></a>
#### updateDisplayName.pl
- gives instances of a specified class proper display names and
  adds a modified instance edit to any changed instance
