/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.render;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.GKInstance;
import org.gk.model.Modification;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.Reference;
import org.gk.model.Summation;
import org.gk.schema.GKSchemaClass;
import org.gk.util.GraphLayoutEngine;

/**
 * This utility is used to convert GKInstsances to Renderable objects.
 * @author guanming
 *
 */
public class InstanceToRenderableConverter {
    
    /**
     * Use this method to convert a GKInstance to a ReactionNode.
     * @param instance an Instance of Reaction whose attribute values should be filled.
     * @return a ReactionNode that can be displayed.
     */
    private static ReactionNode convertToReactionNode(GKInstance instance, 
                                                      boolean needProp,
                                                      Map convertedMap) throws Exception {
        ReactionNode node = (ReactionNode) convertedMap.get(instance);
        if (node != null) {
            ReactionNode shortcut = (ReactionNode) node.generateShortcut();
            return shortcut;
        }
        RenderableReaction reaction = new RenderableReaction();
        reaction.setDisplayName(instance.getDisplayName());
        reaction.initPosition(new Point(50, 50)); // Randomly assign position.
        node = new ReactionNode(reaction);
        List inputs = instance.getAttributeValuesList("input");
        Map nodeMap = new HashMap();
        Map stoiMap = new HashMap();
        if (inputs != null) {
            for (Iterator it = inputs.iterator(); it.hasNext();) {
                GKInstance inputInstance = (GKInstance) it.next();
                String name = inputInstance.getDisplayName();
                if (nodeMap.containsKey(name)) {
                    // Update stoiMap
                    Integer value = (Integer) stoiMap.get(name);
                    stoiMap.put(name, new Integer(value.intValue() + 1));
                }
                else {
                    Node entity = convertToNode(inputInstance, needProp, convertedMap);
                    reaction.addInput(entity);
                    node.addComponent(entity);
                    entity.setContainer(node);
                    nodeMap.put(name, entity);
                    stoiMap.put(name, new Integer(1));
                }
            }
            // Set the input stoichiometries
            for (Iterator it = stoiMap.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                Integer value = (Integer) stoiMap.get(name);
                if (value.intValue() > 1) {
                    Renderable entity = (Renderable) nodeMap.get(name);
                    reaction.setInputStoichiometry(entity, value.intValue());
                }
            }
        }
        List outputs = instance.getAttributeValuesList("output");
        if (outputs != null) {
            nodeMap.clear();
            stoiMap.clear();
            for (Iterator it = outputs.iterator(); it.hasNext();) {
                GKInstance outputInstance = (GKInstance) it.next();
                String name = outputInstance.getDisplayName();
                if (nodeMap.containsKey(name)) {
                    // Update stoiMap
                    Integer value = (Integer) stoiMap.get(name);
                    stoiMap.put(name, new Integer(value.intValue() + 1));
                }
                else {
                    Node entity = convertToNode(outputInstance, needProp, convertedMap);
                    reaction.addOutput(entity);
                    node.addComponent(entity);
                    entity.setContainer(node);
                    nodeMap.put(name, entity);
                    stoiMap.put(name, new Integer(1));
                }
            }
            // Set output stoichiometries
            for (Iterator it = stoiMap.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                Integer value = (Integer) stoiMap.get(name);
                if (value.intValue() > 1) {
                    Renderable entity = (Renderable) nodeMap.get(name);
                    reaction.setOutputStoichiometry(entity, value.intValue());
                }
            }
        }
        List ca = instance.getAttributeValuesList("catalystActivity");
        if (ca != null && ca.size() > 0) {
            java.util.List helpers = new ArrayList(ca.size());
            for (Iterator it = ca.iterator(); it.hasNext();) {
                GKInstance activity = (GKInstance) it.next();
                java.util.List catalysts = activity.getAttributeValuesList("physicalEntity");
                if (catalysts != null && catalysts.size() > 0) {
                    for (Iterator it1 = catalysts.iterator(); it1.hasNext();) {
                        GKInstance catalyst = (GKInstance) it1.next();
                        Renderable helper = convertToNode(catalyst, needProp, convertedMap);
                        helpers.add(helper);
                    }
                }
            }
            if (helpers.size() > 0) {
                for (Iterator it = helpers.iterator(); it.hasNext();) {
                    Node helper = (Node) it.next();
                    reaction.addHelper(helper);
                    node.addComponent(helper);
                    helper.setContainer(node);
                }
            }
        }
        //TODO: How to extract regulation information from Regulation.
        // This information cannot be extracted directly from Reaction
        // right now.
        //if (instance.getSchemClass().isValidAttribute("regulation")) {
        //  List regulation = instance.getAttributeValuesList("regulation");
        // Use referrers to get the regulated entity.
        Collection regulation = instance.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulation != null && regulation.size() > 0) {
                java.util.List inhibitors = new ArrayList();
                java.util.List activators = new ArrayList();
                for (Iterator it = regulation.iterator(); it.hasNext();) {
                    GKInstance instance1 = (GKInstance)it.next();
                    GKInstance regulator = (GKInstance)instance1.getAttributeValue("regulator");
                    if (regulator == null || !regulator.getSchemClass().isa("PhysicalEntity"))
                        continue;
                    Renderable modulator = convertToNode(regulator, needProp, convertedMap);
                    if (instance1.getSchemClass().isa("NegativeRegulation")) { // Inhibitor
                        inhibitors.add(modulator);
                    }
                    else if (instance1.getSchemClass().isa("PositiveRegulation")) { // Activator
                        activators.add(modulator);
                    }
                }
                // Add to reaction
                if (inhibitors.size() > 0) {
                    for (Iterator it = inhibitors.iterator(); it.hasNext();) {
                        Node inhibitor = (Node)it.next();
                        reaction.addInhibitor(inhibitor);
                        node.addComponent(inhibitor);
                        inhibitor.setContainer(node);
                    }
                }
                if (activators.size() > 0) {
                    for (Iterator it = activators.iterator(); it.hasNext();) {
                        Node activator = (Node)it.next();
                        reaction.addActivator(activator);
                        node.addComponent(activator);
                        activator.setContainer(node);
                    }
                }
            }
        //}
        // The point is set arbitrarily
        reaction.layout(new Point(250, 200));
        convertedMap.put(instance, node);
        return node;
    }
    
    /**
     * Convert a GKInstance object to a Renderable object. The contained instances will be handled recursively.
     * @param instance
     * @param needProp true if the properties in GKInstance should be transferred to Renderable object. False for no
     * transferring. However, the specified instance will be used as model instance for the converted renderable object.
     * @return
     * @throws Exception
     */
    public static Renderable convertToNode(GKInstance instance, 
                                           boolean needProp) throws Exception {
        // To keep track the converted instances
        Map map = new HashMap();
        Renderable node = convertToNode(instance, needProp, map);
        if (needProp) { // The following can only work under needProp condition since
                        // a new GKInstance as Model is needed
            // Have to convert GKInstance in precedingEvent to Node
            List allEvents = RenderUtility.getAllEvents(node);
            Renderable event = null;
            List preEvts = null;
            for (Iterator it = allEvents.iterator(); it.hasNext();) {
                event = (Renderable)it.next();
                preEvts = (List)event.getAttributeValue("precedingEventFake"); // A fake key
                if (preEvts == null || preEvts.size() == 0)
                    continue;
                java.util.List precedingEvents = new ArrayList(preEvts.size());
                for (Iterator it1 = preEvts.iterator(); it1.hasNext();) {
                    GKInstance instEvt = (GKInstance)it1.next();
                    Renderable node1 = (Renderable)map.get(instEvt);
                    if (node1 != null)
                        precedingEvents.add(node1);
                }
                if (precedingEvents.size() > 0)
                    event.setAttributeValue("precedingEvent", precedingEvents);
            }
        }
        return node;
    }
    
    private static Node convertToNode(GKInstance instance, 
                                      boolean needProp, 
                                      Map map) throws Exception {
        GKSchemaClass schemaClass = (GKSchemaClass)instance.getSchemClass();
        Node node = null;
        if (schemaClass.isa("Complex")) {
            node = convertToComplex(instance, needProp, map);
        }
        else if (schemaClass.isa("PhysicalEntity")) {
            node = convertToEntity(instance, needProp, map);
        }
        else if (schemaClass.isa("Reaction")) {
            node = convertToReactionNode(instance, needProp, map);
        }
        else if (schemaClass.isa("Pathway")) {
            node = convertToPathway(instance, needProp, map);
        }
        else if (schemaClass.isa(ReactomeJavaConstants.ConceptualEvent))
            node = convertToPathway(instance, needProp, map);
        else if (schemaClass.isa(ReactomeJavaConstants.EquivalentEventSet))
            node = convertToPathway(instance, needProp, map);
        else if (schemaClass.isa("Event")) {
            //"Event" is an abstract class, there is no need to consider this.
            node = convertToEntity(instance, needProp, map);
        }
        if (node != null) {
            node.setPosition(new Point(50, 50)); // Default position
            if (!(node instanceof Shortcut)) {
                if (needProp) {
                    //node.setModelInstance(new GKInstance());
                    getPropertiesForRenderable(instance, node);
                }
                //else
                //  node.setModelInstance(instance);
            }
        }
        return node;
    }
    
    private static RenderableEntity convertToEntity(GKInstance instance, boolean needProp, Map convertedMap) {
        RenderableEntity entity = (RenderableEntity) convertedMap.get(instance);
        if (entity != null) {
            RenderableEntity shortcut = (RenderableEntity) entity.generateShortcut();
            return shortcut;
        }
        entity = new RenderableEntity(instance.getDisplayName());
        convertedMap.put(instance, entity);
        return entity;
    }
    
    private static RenderableComplex convertToComplex(GKInstance instance, boolean needProp, Map convertedMap) throws Exception {
        RenderableComplex complex = (RenderableComplex) convertedMap.get(instance);
        if (complex != null) {
            // Create a shortcut
            RenderableComplex shortcut = (RenderableComplex) complex.generateShortcut();
            return shortcut;
        }
        complex = new RenderableComplex(instance.getDisplayName());
        java.util.List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                GKInstance tmp = (GKInstance) it.next();
                Renderable node = convertToNode(tmp, needProp, convertedMap);
                if (node != null) {
                    complex.addComponent(node);
                    node.setContainer(complex);
                }
            }
        }
        if (complex.getPosition() == null)
            complex.setPosition(50, 50); // Assign an arbitrary position
        complex.layout();
        // Move a little bit
        if (complex.getComponents() != null) {
            for (Iterator it = complex.getComponents().iterator(); it.hasNext();) {
                Renderable node = (Renderable) it.next();
                node.move(150, 100); // These two values are arbitrary
            }
        }
        convertedMap.put(instance, complex);
        return complex;
    }
    
    /**
     * Use this method to convert a GK Pathway to a RenderablePathway. This method is
     * used to convert three GKInstances, Pathway, ConceptualEvent, EquivalentEventSet
     * into Pathway. So correct attributes should be checked.
     */
    private static RenderablePathway convertToPathway(GKInstance instance, 
                                                      boolean needProp,
                                                      Map convertedMap) throws Exception {
        RenderablePathway pathway = (RenderablePathway) convertedMap.get(instance);
        if (pathway != null) {
            PathwayShortcut shortcut = (PathwayShortcut) pathway.generateShortcut();
            return shortcut;
        }
        pathway = new RenderablePathway(instance.getDisplayName());
        List components = null;
        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) // Pathway
            components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) // Pathway
            components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm)) // ConceptualEvent
            components = instance.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
        else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) // EquivalentEventSet
            components = instance.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        if (components != null && components.size() > 0) {
            Map nodeMap = new HashMap();
            for (Iterator it = components.iterator(); it.hasNext();) {
                GKInstance tmp = (GKInstance) it.next();
                if (tmp == null)
                    continue;
                Renderable node = convertToNode(tmp, needProp, convertedMap);
                nodeMap.put(tmp, node);
                pathway.addComponent(node);
                node.setContainer(pathway);
            }
            // Have to figure out the dependencies among all nodes
            for (Iterator it = nodeMap.keySet().iterator(); it.hasNext();) {
                GKInstance tmp = (GKInstance) it.next();
                Node node = (Node) nodeMap.get(tmp);
                // Check the preceding event
                java.util.List precedingEvent = tmp.getAttributeValuesList("precedingEvent");
                if (precedingEvent != null && precedingEvent.size() > 0) {
                    GKInstance eventInst = null;
                    java.util.List unconnected = new ArrayList();
                    for (Iterator it1 = precedingEvent.iterator(); it1.hasNext();) {
                        eventInst = (GKInstance) it1.next();
                        if (nodeMap.containsKey(eventInst)) { // Create a link
                            FlowLine flowLine = new FlowLine();
                            flowLine.addInput((Node)nodeMap.get(eventInst));
                            flowLine.addOutput(node);
                            flowLine.initPosition(new Point(50, 50));
                            pathway.addComponent(flowLine);
                            flowLine.setContainer(pathway);
                        }
                        else {
                            unconnected.add(eventInst);
                        }
                    }
                    if (unconnected.size() > 0) 
                        // Use a fake name to avoid overwriting the actual values
                        node.setAttributeValue("precedingEventFake", unconnected);
                }
            }
        }
        // Do an automatic layout
        if (pathway.getComponents() != null) {
            pathway.layout(GraphLayoutEngine.HIERARCHICAL_LAYOUT);
            for (Iterator it = pathway.getComponents().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                r.move(150, 50); // Give some extra space
            }
        }
        convertedMap.put(instance, pathway);
        return pathway;
    }
    
    /**
     * Extract properties that should be used in a Renderable object from
     * a specified instance.
     * @param instance
     * @param renderable
     */
    public static void getPropertiesForRenderable(GKInstance instance, Renderable renderable) {
        try {
            //renderable.setAttributeValue("DB_ID", instance.getDBID());
            // Should do for all shortcuts
            List<Renderable> shortcuts = renderable.getShortcuts();
            if (shortcuts == null || shortcuts.size() == 0)
                renderable.setReactomeId(instance.getDBID());
            else {
                for (Renderable r : shortcuts) {
                    r.setReactomeId(instance.getDBID());
                }
            }
            //getEditProperty("created", instance, renderable);
            //getEditProperty("modified", instance, renderable);
            getProperty("species", "taxon", instance, renderable);
            // compartment is also needed for Entity as of build 23.
            getProperty("compartment", "localization", instance, renderable);
            java.util.List values = instance.getAttributeValuesList("name");
            if (values != null && values.size() > 0) {
                java.util.List names = new ArrayList(values); // Have to make a clone to avoid
                // changes in the orignal list.
                names.remove(renderable.getDisplayName());
                if (names.size() > 0)
                    renderable.setAttributeValue("names", names);
                else
                    renderable.setAttributeValue("names", null);
            }
            else
                renderable.setAttributeValue("names", null);
            // For databaseIdentifier
            GKInstance gkDatabaseIdentifier = null;
            if (instance.getSchemClass().isValidAttribute("referenceEntity")) {
                gkDatabaseIdentifier = (GKInstance) instance.getAttributeValue("referenceEntity");
            }
            if (gkDatabaseIdentifier == null &&
                    instance.getSchemClass().isValidAttribute("crossReference")) {
                // Check crossReference
                gkDatabaseIdentifier = (GKInstance) instance.getAttributeValue("crossReference");
            }
            if (gkDatabaseIdentifier != null) {
                // Both ReferenceEntity and DatabaseReference have identifier and referenceDatabase attributes
                String identifier = null;
                if (gkDatabaseIdentifier.getSchemClass().isValidAttribute("identifier"))
                    identifier = (String) gkDatabaseIdentifier.getAttributeValue("identifier");
                GKInstance referenceDatabase = null;
                if (gkDatabaseIdentifier.getSchemClass().isValidAttribute("referenceDatabase"))
                    referenceDatabase = (GKInstance) gkDatabaseIdentifier.getAttributeValue("referenceDatabase");
                if (identifier != null || referenceDatabase != null) {
                    DatabaseIdentifier dbIdentifier = new DatabaseIdentifier();
                    dbIdentifier.setAccessNo(identifier);
                    if (referenceDatabase != null)
                        dbIdentifier.setDbName(referenceDatabase.getDisplayName());
                    renderable.setAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER, 
                                                 dbIdentifier);
                }
            }
            // For modification for Entity
            if (instance.getSchemClass().isValidAttribute("hasModifiedResidue")) {
                values = instance.getAttributeValuesList("hasModifiedResidue");
                List modifications = convertModifications(values);
                if (modifications != null)
                    renderable.setAttributeValue("modifications", modifications);
            }
            if (instance.getSchemClass().isa("Complex") || instance.getSchemClass().isa("Event")) {
                values = instance.getAttributeValuesList("summation");
                if (values != null && values.size() > 0) {
                    // One value only
                    Summation summation = null;
                    Object obj = values.get(0);
                    if (obj instanceof GKInstance) {
                        summation = convertLiteratureReferenceToSummation((GKInstance)obj);
                    }
                    else if (obj instanceof String) {
                        summation = new Summation();
                        summation.setText(obj.toString());
                    }
                    renderable.setAttributeValue("summation", summation);
                }
                else
                    renderable.setAttributeValue("summation", null);
                values = instance.getAttributeValuesList("literatureReference");
                java.util.List references = extractReferences(values);
                renderable.setAttributeValue("references", references);
            }
        }
        catch (Exception e) {
            System.err.println("RenderUtility.getPropertiesForRenderable(): " + e);
            e.printStackTrace();
        }
    }
    
    public static Summation convertLiteratureReferenceToSummation(GKInstance literatureReference) throws Exception {
        Summation summation = new Summation();
        summation.setText((String)literatureReference.getAttributeValue("text"));
        summation.setDB_ID(literatureReference.getDBID());
        // Check literature
        java.util.List refInstances = literatureReference.getAttributeValuesList("literatureReference");
        summation.setReferences(extractReferences(refInstances));
        return summation;
    }
    
    /**
     * Convert a list of GKInstance for hasModifiedResidue to Modifcation instances.
     * @param hasModifiedResidue
     * @return
     * @throws Exception
     */
    public static List convertModifications(List hasModifiedResidue) throws Exception {
        if (hasModifiedResidue != null && hasModifiedResidue.size() > 0) {
            java.util.List modifications = new ArrayList(hasModifiedResidue.size());
            for (Iterator it = hasModifiedResidue.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof GKInstance) {
                    GKInstance gi = (GKInstance) obj;
                    Modification mod = new Modification();
                    mod.setDB_ID(gi.getDBID());
                    Object obj1 = gi.getAttributeValue("coordinate");
                    if (obj1 != null)
                        mod.setCoordinate(Integer.parseInt(obj1.toString()));
                    if (gi.getSchemClass().isValidAttribute(ReactomeJavaConstants.residue)) {
                        obj1 = gi.getAttributeValue(ReactomeJavaConstants.residue);
                        if (obj1 instanceof GKInstance)
                            mod.setResidue(((GKInstance)obj1).getDisplayName());
                        else if (obj1 instanceof String)
                            mod.setResidue(obj1.toString());
                    }
                    // Not support yet
//                    else if (gi.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
//                        
//                    }
                    if (gi.getSchemClass().isValidAttribute(ReactomeJavaConstants.modification)) {
                        obj1 = gi.getAttributeValue(ReactomeJavaConstants.modification);
                        if (obj1 instanceof GKInstance)
                            mod.setModification(((GKInstance)obj1).getDisplayName());
                        else if (obj1 instanceof String)
                            mod.setModification(obj1.toString());
                    }
                    //obj1 = gi.getAttributeValue("databaseIdentifier");
                    //if (obj1 instanceof GKInstance)
                    //  mod.setModificationDbID(((GKInstance)obj1).getDisplayName());
                    //else if (obj1 instanceof String)
                    //  mod.setModificationDbID(obj1.toString());   
                    modifications.add(mod);
                }
            }
            return modifications;
        }
        return null;
    }
    
    /**
     * Concever a list of LiteratureReference GKInstance to a list of Reference objects.
     * @param values
     * @return
     * @throws Exception
     */
    public static java.util.List extractReferences(java.util.List values) throws Exception{
        if (values == null || values.size() == 0)
            return null;
        java.util.List references = new ArrayList(values.size());
        GKInstance ref = null;
        for (Iterator it = values.iterator(); it.hasNext();) {
            ref = (GKInstance)it.next();
            // Escape a shell instance. There is no meaning to export a shell instance to the author tool.
            if (ref.isShell())
                continue;
            Reference refObj = new Reference();
            refObj.setDB_ID(ref.getDBID());
            // Set author
            java.util.List authors = ref.getAttributeValuesList("author");
            if (authors != null && authors.size() > 0) {
                StringBuffer b = new StringBuffer();
                for (Iterator it1 = authors.iterator(); it1.hasNext();) {
                    GKInstance author = (GKInstance)it1.next();
                    String displayName = author.getDisplayName();
                    displayName = displayName.replace(',', ' ');
                    b.append(displayName);
                    if (it1.hasNext())
                        b.append(", ");
                }
                refObj.setAuthor(b.toString());
            }
            // Set pubmed
            Object pubMedId = ref.getAttributeValue("pubMedIdentifier");
            if (pubMedId != null)
                refObj.setPmid(Long.parseLong(pubMedId.toString()));
            // Set title
            Object title = ref.getAttributeValue("title");
            if (title instanceof String)
                refObj.setTitle(title.toString());
            // set Journal
            Object journal = ref.getAttributeValue("journal");
            if (journal instanceof String)
                refObj.setJournal(journal.toString());
            // Set year
            Object year = ref.getAttributeValue("year");
            if (year != null)
                refObj.setYear(Integer.parseInt(year.toString()));
            // Set volume
            Object vol = ref.getAttributeValue("volume");
            if (vol != null)
                refObj.setVolume(vol.toString());
            // Set pages
            Object pages = ref.getAttributeValue("pages");
            if (pages instanceof String) {
                refObj.setPage(pages.toString());
            }
            references.add(refObj);
        }
        return references;
    }
    
    private static void getProperty(String dbPropName, String renderPropName, GKInstance instance, Renderable r) 
                        throws Exception {
        // Do a check to avoid throwing exceptions
        if (!instance.getSchemClass().isValidAttribute(dbPropName))
            return;
        java.util.List values = instance.getAttributeValuesList(dbPropName);
        if (values != null && values.size() > 0) {
            Object obj = values.get(0);
            String value = null;
            if (obj instanceof GKInstance) {
                value = ((GKInstance)obj).getDisplayName();
            }
            else if (obj instanceof String){
                value = (String) obj;
            }
            r.setAttributeValue(renderPropName, value);
        }
    }
    
}
