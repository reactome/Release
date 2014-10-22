/*
 * Created on Jul 24, 2012
 *
 */
package org.reactome.core.mapper;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.core.controller.ReactomeModelPostMapper;
import org.reactome.core.controller.ReactomeToRESTfulAPIConverter;
import org.reactome.core.controller.ReflectionUtility;
import org.reactome.core.model.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class PhysicalEntityMapper extends ReactomeModelPostMapper {
    
    public PhysicalEntityMapper() {
        
    }

    @Override
    public void postProcess(GKInstance inst, DatabaseObject obj,
                            ReactomeToRESTfulAPIConverter converter)
            throws Exception {
    }

    @Override
    public void fillDetailedView(GKInstance inst,
                                 DatabaseObject obj,
                                 ReactomeToRESTfulAPIConverter converter) throws Exception {
        if (!(validParameters(inst, obj)))
            return;
        PhysicalEntity pe = (PhysicalEntity) obj;
        fillCrossReferences(inst, pe, converter);
        handleCatalysedEvents(inst, converter, pe);
        handleRegulatedEvents(inst, converter, pe);
        handleProducedByEvents(inst, converter, pe);
        handleConsumedByEvents(inst, converter, pe);
        handleInferredTo(inst, converter, pe);
    }

    public void fillCrossReferences(GKInstance inst,
                                    PhysicalEntity pe,
                                    ReactomeToRESTfulAPIConverter converter) throws Exception {
        PersistenceAdaptor dba = inst.getDbAdaptor();
        // Check its cross-reference first
        List<DatabaseIdentifier> dbiList = pe.getCrossReference();
        if (dbiList != null && dbiList.size() > 0) {
            for (DatabaseIdentifier dbi : dbiList) {
                assignValidURL(dba, dbi);
            }
        }
        // Get from its referenceEntity
        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            // There is only one referenceEntity
            GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity == null)
                return;
            processReferenceEntity(refEntity, converter, pe);
            // Check if there is any ReferenceGene attribute
            if (refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceGene)) {
                List<GKInstance> refGeneList = refEntity.getAttributeValuesList(ReactomeJavaConstants.referenceGene);
                for (GKInstance refGene : refGeneList)
                    processReferenceEntity(refGene, converter, pe);
            }
        }
    }
    
    private void handleInferredTo(GKInstance instance,
                                  ReactomeToRESTfulAPIConverter converter,
                                  PhysicalEntity pe) throws Exception {
        List<PhysicalEntity> inferredTo = pe.getInferredTo();
        if (inferredTo == null || inferredTo.size() == 0)
            return;
        //TODO: The use of string here should be changed after updating ReactomeJavaConstant class.
        List<GKInstance> inferredToList = instance.getAttributeValuesList("inferredTo");
        if (inferredToList == null || inferredToList.size() == 0)
            return;
        // Create a map for searching
        Map<Long, PhysicalEntity> idToPe = new HashMap<Long, PhysicalEntity>();
        for (PhysicalEntity pe1 : inferredTo) {
            idToPe.put(pe1.getDbId(), pe1);
        }
        for (GKInstance inst1 : inferredToList) {
            if (!inst1.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
                continue;
            PhysicalEntity pe1 = idToPe.get(inst1.getDBID());
            if (pe1 == null)
                continue; // This should not occur
            Method setSpeciesMethod = ReflectionUtility.getNamedMethod(pe1,
                    "setSpecies");
            if (setSpeciesMethod == null)
                continue;
            List<GKInstance> speciesList = inst1.getAttributeValuesList(ReactomeJavaConstants.species);
            List<Species> species = new ArrayList<Species>(speciesList.size());
            for (GKInstance speciesInst : speciesList) {
                Species species1 = (Species) converter.createObject(speciesInst);
                species.add(species1);
            }
            // The method argument may be an array of species or just a species
            if (setSpeciesMethod.getParameterTypes()[0] == Species.class) {
                setSpeciesMethod.invoke(pe1, species.get(0));
            }
            else // Should be a list
                setSpeciesMethod.invoke(pe1, species);
        }
    }
    
    private void handleRegulatedEvents(GKInstance inst,
                                       ReactomeToRESTfulAPIConverter converter,
                                       PhysicalEntity pe) throws Exception {
        Collection<GKInstance> regulations = inst.getReferers(ReactomeJavaConstants.regulator);
        if (regulations == null || regulations.size() == 0)
            return;
        List<Event> inhibitedEvents = null;
        List<Event> activatedEvents = null;
        List<Event> requiredEvents = null;
        for (GKInstance regulation : regulations) {
            GKInstance event = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
            if (event == null || !event.getSchemClass().isa(ReactomeJavaConstants.Event))
                continue;
            Event eventObj = (Event) converter.createObject(event);
            // Note: a regulated CA has not been handled here.
            if (regulation.getSchemClass().isa(ReactomeJavaConstants.Requirement)) {
                if (requiredEvents == null)
                    requiredEvents = new ArrayList<Event>();
                requiredEvents.add(eventObj);
            }
            else if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                if (activatedEvents == null)
                    activatedEvents = new ArrayList<Event>();
                activatedEvents.add(eventObj);
            }
            else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                if (inhibitedEvents == null)
                    inhibitedEvents = new ArrayList<Event>();
                inhibitedEvents.add(eventObj);
            }
        }
        pe.setRequiredEvent(requiredEvents);
        pe.setInhibitedEvent(inhibitedEvents);
        pe.setActivatedEvent(activatedEvents);
    }
    
    private void handleProducedByEvents(GKInstance inst,
                                        ReactomeToRESTfulAPIConverter converter,
                                        PhysicalEntity pe) throws Exception {
        Collection<GKInstance> producedByEventInsts = inst.getReferers(ReactomeJavaConstants.output);
        if (producedByEventInsts == null || producedByEventInsts.size() == 0)
            return;
        List<Event> producedByEvents = new ArrayList<Event>();
        for (GKInstance eventInst : producedByEventInsts) {
            Object obj = converter.createObject(eventInst);
            if (obj instanceof Event)
                producedByEvents.add((Event)obj);
        }
        pe.setProducedByEvent(producedByEvents);
    }
    
    private void handleConsumedByEvents(GKInstance inst,
                                        ReactomeToRESTfulAPIConverter converter,
                                        PhysicalEntity pe) throws Exception {
        Collection<GKInstance> consumedByInsts = inst.getReferers(ReactomeJavaConstants.input);
        if (consumedByInsts == null || consumedByInsts.size() == 0)
            return;
        List<Event> consumedByEvents = new ArrayList<Event>();
        for (GKInstance eventInst : consumedByInsts) {
            DatabaseObject obj = converter.createObject(eventInst);
            if (obj instanceof Event) 
                consumedByEvents.add((Event)obj);
        }
        pe.setConsumedByEvent(consumedByEvents);
    }

    private void handleCatalysedEvents(GKInstance inst,
                                      ReactomeToRESTfulAPIConverter converter,
                                      PhysicalEntity pe) throws Exception,
            InvalidAttributeException {
        // List of CatalystActivities involved with this PE
        Collection<GKInstance> cas = inst.getReferers(ReactomeJavaConstants.physicalEntity);
        if (cas != null && cas.size() > 0) {
            Set<GKInstance> goMF = new HashSet<GKInstance>();
            Set<GKInstance> events = new HashSet<GKInstance>();
            for (GKInstance ca : cas) {
                if (ca.getSchemClass().isa(ReactomeJavaConstants.CatalystActivity)) {
                    List<GKInstance> values = ca.getAttributeValuesList(ReactomeJavaConstants.activity);
                    if (values != null)
                        goMF.addAll(values);
                    Collection<GKInstance> referrers = ca.getReferers(ReactomeJavaConstants.catalystActivity);
                    if (referrers != null)
                        events.addAll(referrers);
                }
            }
            if (goMF.size() > 0) {
                List<GO_MolecularFunction> activities = new ArrayList<GO_MolecularFunction>();
                for (GKInstance mf : goMF) {
                    GO_MolecularFunction tmp = (GO_MolecularFunction) converter.createObject(mf);
                    activities.add(tmp);
                }
                pe.setGoActivity(activities);
            }
            if (events.size() > 0) {
                List<Event> catalyzedEvents = new ArrayList<Event>();
                for (GKInstance event : events) {
                    Event tmp = (Event) converter.createObject(event);
                    catalyzedEvents.add(tmp);
                }
                pe.setCatalyzedEvent(catalyzedEvents);
            }
        }
    }

    private void processReferenceEntity(GKInstance refEntity,
                                        ReactomeToRESTfulAPIConverter converter,
                                        PhysicalEntity pe)  throws Exception, InvalidAttributeException {
        // Fake DatabaseIdentifier for ReferenceEntity for easy process
        DatabaseIdentifier dbi = new DatabaseIdentifier();
        dbi.setDbId(refEntity.getDBID());
        dbi.setDisplayName(refEntity.getDisplayName());

        PersistenceAdaptor dba = refEntity.getDbAdaptor();
        assignValidURL(dba, dbi);
        pe.addCrossReference(dbi);
        List<GKInstance> xrefList = refEntity.getAttributeValuesList(ReactomeJavaConstants.crossReference);
        if (xrefList != null && xrefList.size() > 0) {
            for (GKInstance xref : xrefList) {
                dbi = (DatabaseIdentifier) converter.createObject(xref);
                assignValidURL(dba, dbi);
                pe.addCrossReference(dbi);
            }
        }
    }
                                        
    private void assignValidURL(PersistenceAdaptor dba,
                                DatabaseIdentifier dbi) throws Exception {
        assignValidURLToDatabaseIdentifier(dba, dbi);
    }
    
    @Override
    public void postShellProcess(GKInstance inst, DatabaseObject obj)
            throws Exception {
    }

    @Override
    protected boolean isValidObject(DatabaseObject obj) {
//        if (obj instanceof EntityWithAccessionedSequence ||
//            obj instanceof SimpleEntity) // Only these two classes have referenceEntity attribute.
//            return true;
//        return false;
        return obj instanceof PhysicalEntity;
    }
    
    
    
}
