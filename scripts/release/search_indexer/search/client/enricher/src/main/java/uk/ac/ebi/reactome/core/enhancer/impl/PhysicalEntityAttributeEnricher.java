package uk.ac.ebi.reactome.core.enhancer.impl;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.submodels.*;

import java.util.*;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class PhysicalEntityAttributeEnricher extends Enricher{

    private static final Logger logger = LoggerFactory.getLogger(PhysicalEntityAttributeEnricher.class);
    private static final String ENTITY_ON_OTHER_CELL = "entityOnOtherCell";

    public void setPhysicalEntityAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {
            try {

                setGeneralPhysicalEntityAttributes(instance, enrichedEntry);
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    enrichComplex(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.Polymer)) {
                    enrichPolymer(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
                    enrichSimpleEntity(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                    enrichEntitySet(instance, enrichedEntry);
                } else if (instance.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                    enrichEntityWithAccessionSequence(instance, enrichedEntry);
                }
            } catch (Exception e) {
                logger.error("Error occurred when trying to set event attributes", e);
                throw new EnricherException("Error occurred when trying to set event attributes", e);
            }
        }
    }

    private void setGeneralPhysicalEntityAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {

            enrichedEntry.setReferedEntities(getPhysicalEntityReferers(instance)); // Change if Prune tree is in place for everything

            enrichedEntry.setInferredTo(getEntityReferences(instance, ReactomeJavaConstants.inferredTo));
            enrichedEntry.setGoMolecularComponent(getGoTerms(instance, ReactomeJavaConstants.goCellularComponent));
            enrichedEntry.setRegulatingEntities(getRegulations(instance, ReactomeJavaConstants.regulator));

        }
    }














    private void enrichComplex(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {
            enrichedEntry.setComponents(getEntityReferences(instance, ReactomeJavaConstants.hasComponent));
            enrichedEntry.setEntityOnOtherCell(getEntityReferences(instance, ENTITY_ON_OTHER_CELL));
        }
    }

    private void enrichPolymer(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {
            enrichedEntry.setRepeatedUnits(getEntityReferences(instance, ReactomeJavaConstants.repeatedUnit));
        }
    }

    private void enrichSimpleEntity(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {
            enrichedEntry.setReferenceEntity(getReferenceEntity(instance,enrichedEntry));
        }
    }

    private void enrichEntityWithAccessionSequence(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {
            enrichedEntry.setReferenceEntity(getReferenceEntity(instance,enrichedEntry));
            enrichedEntry.setModifiedResidues(getModifiedResidue(instance));
        }
    }

    private void enrichEntitySet(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null && enrichedEntry != null) {

            enrichedEntry.setMember(getEntityReferences(instance, ReactomeJavaConstants.hasMember));
            if (instance.getSchemClass().isa(ReactomeJavaConstants.CandidateSet)) {
                enrichedEntry.setCandidates(getEntityReferences(instance, ReactomeJavaConstants.hasCandidate));
            } else if (instance.getSchemClass().isa(ReactomeJavaConstants.OpenSet)) {
                enrichedEntry.setReferenceEntity(getReferenceEntity(instance,enrichedEntry));
            }
        }
    }

    private Map<String,List<EntityReference>> getPhysicalEntityReferers( GKInstance instance) throws EnricherException {
        try {
            Map<String, List<EntityReference>> referers = new HashMap<String, List<EntityReference>>();

            List<EntityReference> entityReferences = getReferers(instance, ReactomeJavaConstants.hasComponent);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.hasComponent, entityReferences);
            }
            entityReferences = getReferers(instance, ReactomeJavaConstants.repeatedUnit);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.repeatedUnit, entityReferences);
            }
            entityReferences = getReferers(instance, ReactomeJavaConstants.hasCandidate);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.hasCandidate, entityReferences);
            }
            entityReferences = getReferers(instance, ReactomeJavaConstants.hasMember);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.hasMember, entityReferences);
            }
            entityReferences = getReferers(instance, ReactomeJavaConstants.input);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.input, entityReferences);
            }
            entityReferences = getReferers(instance, ReactomeJavaConstants.output);
            if (entityReferences != null) {
                referers.put(ReactomeJavaConstants.output, entityReferences);
            }
            return referers;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }

    }

    private ReferenceEntity getReferenceEntity(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        if (instance != null) {
            if (hasValue(instance, ReactomeJavaConstants.referenceEntity)) {
                try {
                    GKInstance referenceEntityInstance = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                    if (enrichedEntry.getCrossReferences()==null) {
                        enrichedEntry.setCrossReferences(getCrossReferences(referenceEntityInstance, ReactomeJavaConstants.crossReference, null));
                    } else {
                        enrichedEntry.setCrossReferences(getCrossReferences(referenceEntityInstance, ReactomeJavaConstants.crossReference, enrichedEntry.getCrossReferences()));
                    }

                    ReferenceEntity referenceEntity = new ReferenceEntity();
                    setGeneralReferenceEntityAttributes(instance.getDBID(), referenceEntityInstance, referenceEntity);

                    if (referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceMolecule) || referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGroup)) {
                        referenceEntity.setFormula(getAttributeString(referenceEntityInstance, ReactomeJavaConstants.formula));
                        referenceEntity.setAtomicConnectivity(getAttributeString(referenceEntityInstance, ReactomeJavaConstants.atomicConnectivity));

                        return referenceEntity;
                    } else if (referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceSequence)) {

                        referenceEntity.setSecondaryIdentifier(getAttributes(referenceEntityInstance, ReactomeJavaConstants.secondaryIdentifier));
                        referenceEntity.setGeneNames(getAttributes(referenceEntityInstance, ReactomeJavaConstants.geneName));
                        referenceEntity.setDescription(getAttributes(referenceEntityInstance, ReactomeJavaConstants.description));


                        if (referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct) || referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceRNASequence)) {
                            referenceEntity.setReferenceGenes(getCrossReferences(referenceEntityInstance, ReactomeJavaConstants.referenceGene, null));

                            if (referenceEntityInstance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct)) {
                                referenceEntity.setChain(getAttributesDisplayNames(referenceEntityInstance, "chain"));
                                referenceEntity.setReferenceTranscript(getCrossReferences(referenceEntityInstance, ReactomeJavaConstants.referenceTranscript, null));

                                if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform)) {
//                                    referenceEntity.setIsoformParent(getCrossReferences(referenceEntityInstance, ReactomeJavaConstants.isoformParent, null));

                                }
                            }
                        }

                        return referenceEntity;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new EnricherException(e.getMessage() , e);
                }
            }
        }
        return null;
    }



    private void setGeneralReferenceEntityAttributes(Long dbId, GKInstance instance, ReferenceEntity referenceEntity) throws EnricherException {

        List<String> names = getAttributes(instance, ReactomeJavaConstants.name);
        if (names!= null && !names.isEmpty()) {
            if (names.size() >= 1) {
                referenceEntity.setReferenceName(names.get(0));
                if (names.size()>1) {
                    referenceEntity.setReferenceSynonyms(names.subList(1, names.size() - 1));
                }
            } else {
                referenceEntity.setReferenceName(instance.getDisplayName());
            }
        }
        referenceEntity.setReferenceIdentifier(getAttributeString(instance, ReactomeJavaConstants.identifier));
        referenceEntity.setOtherIdentifier(getAttributes(instance, ReactomeJavaConstants.otherIdentifier));



        referenceEntity.setDerivedEwas(getReferedEntityReferences(dbId, instance));
        referenceEntity.setDatabase(getDatabase(instance, referenceEntity.getReferenceIdentifier()));

    }




    private List<EntityReference> getReferers(GKInstance instance, String fieldName) throws EnricherException {
        if (instance!=null && fieldName!=null) {
            try {
                Collection<?> collection = instance.getReferers(fieldName);
                if (collection != null && !collection.isEmpty()) {
                    List<EntityReference> entityReferences = new ArrayList<EntityReference>();
                    for (Object gkInstance : collection) {
                        entityReferences.add(getEntityReferenceHelper((GKInstance) gkInstance));
                    }
                    return entityReferences;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }

        }return null;
    }

    private List<ModifiedResidue> getModifiedResidue(GKInstance instance) throws EnricherException {
        if(hasValues(instance, ReactomeJavaConstants.hasModifiedResidue)) {
            try {
                List<?> modifiedResidueInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
                List<ModifiedResidue> modifiedResidues = new ArrayList<ModifiedResidue>();
                for (Object modifiedResidueObject : modifiedResidueInstanceList) {
                    GKInstance modifiedResidueInstance = (GKInstance) modifiedResidueObject;
                    ModifiedResidue modifiedResidue = new ModifiedResidue();
                    modifiedResidue.setName(modifiedResidueInstance.getDisplayName());
                    modifiedResidue.setCoordinate(getAttributeInteger(modifiedResidueInstance, ReactomeJavaConstants.coordinate));
                    modifiedResidue.setModification(getEntityReference(modifiedResidueInstance, ReactomeJavaConstants.modification));
                    modifiedResidue.setPsiMod(getPsiMod(modifiedResidueInstance));
                    modifiedResidues.add(modifiedResidue);
                }
                return modifiedResidues;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }

    private PsiMod getPsiMod(GKInstance instance) throws EnricherException {
        if (hasValue(instance, ReactomeJavaConstants.psiMod)) {
            try {
                GKInstance psiModInstance = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.psiMod);
                PsiMod psiMod = new PsiMod();
                psiMod.setName(getAttributeString(psiModInstance, ReactomeJavaConstants.name));
                psiMod.setIdentifier(getAttributeString(psiModInstance, ReactomeJavaConstants.identifier));
                psiMod.setDefinition(getAttributeString(psiModInstance, ReactomeJavaConstants.definition));
                psiMod.setDatabase(getDatabase(psiModInstance, psiMod.getIdentifier()));
                return psiMod;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }


}
