/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.FilePersistence;
import org.gk.persistence.GKBReader;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.property.PropertyManager;
import org.gk.render.*;
import org.gk.schema.SchemaAttribute;

public class AuthorToolToCuratorToolConverter {
    
    public AuthorToolToCuratorToolConverter() {
        init();
    }
    
    private void init() {
        // call this method to initialize HandlerFactory
        RenderableHandlerFactory factory = RenderableHandlerFactory.getFactory();
        factory.setDBAdapptor(PersistenceManager.getManager().getActiveMySQLAdaptor());
        factory.setFileAdaptor(PersistenceManager.getManager().getActiveFileAdaptor());
    }
    
    public void convert(File file, boolean isForVer2) throws Exception {
        RenderablePathway process = null;
        if (isForVer2)
            process = loadAuthorToolV2File(file);
        else
            process = loadAuthorToolFile(file);
        computePrcedingEvents(process); 
        Set renderables = null;
        if (isForVer2)
            renderables = grepConvertablesForVer2(process);
        else {
            renderables = grepConvertables(process);
            preprocessCompartments(renderables,
                                   process);
        }
        Map renderable2InstanceMap = new HashMap();
        Renderable r = null;
        RenderableHandler handler = null;
        RenderableHandlerFactory factory = RenderableHandlerFactory.getFactory();
        // Special for process
        handler = factory.getHandler(process);
        GKInstance converted = handler.convert(process, renderable2InstanceMap);
        // First step convert all types
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            handler = factory.getHandler(r);
            handler.convert(r, renderable2InstanceMap);
        }
        // Second step convert properties
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            handler = factory.getHandler(r);
            handler.convertProperties(r, renderable2InstanceMap);
        }
        // Special treatment for process
        // handle components
        handleProcessComponents(process, converted, renderable2InstanceMap);
        handler = factory.getHandler(process);
        handler.convertProperties(process, renderable2InstanceMap);
    }
    
    private void preprocessCompartments(Set renderables,
                                        RenderablePathway process) {
        // Check if there is any compartment setting. Try to make it work
        // backward compatibilty
        boolean hasCompartment = false;
        boolean hasCytosol = false;
        for (Iterator it = process.getComponents().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableCompartment) {
                hasCompartment = true;
                String name = r.getDisplayName();
                if (name.equals("cytosol")) {
                    hasCytosol = true;
                    break;
                }
            }
        }
        if (!hasCompartment)
            return; // It may be an old version file or the user doesn't care about compartment.
        // For objects directly contained by compartment
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            Renderable container = r.getContainer();
            if (container instanceof RenderablePathway) {
                // No compartment assigned, extracellular should be right
                if (hasCytosol) {
                    r.setLocalization("extracellular");
                }
                continue;
            }
            if (!(container instanceof RenderableCompartment))
                continue;
            String localization = PropertyManager.getManager().
                                        getLocalizationFromContainer((RenderableCompartment)container, r);
            r.setLocalization(localization);
        }
        copyLocalizationToComplexComps(renderables);
    }
    
    private void copyLocalizationToComplexComps(Set renderables) {
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableComplex &&
                r.getLocalization() != null &&
                !(r.getContainer() instanceof RenderableComplex)) {
                Set<Renderable> comps = RenderUtility.getAllContainedComponents(r);
                if (comps == null)
                    continue;
                String loc = r.getLocalization();
                for (Renderable comp : comps) {
                    comp.setLocalization(loc);
                }
            }
        }
    }
    
    private void handleProcessComponents(RenderablePathway process,
                                         GKInstance converted,
                                         Map renderable2InstanceMap) throws Exception {
        List components = process.getComponents();
        if (components == null || components.size() == 0)
            return;
        List compInstances = new ArrayList();
        SchemaAttribute hasCompAtt = converted.getSchemClass().getAttribute(ReactomeJavaConstants.hasEvent);
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderablePathway ||
                r instanceof RenderableReaction ||
                r instanceof RenderableInteraction) {
                RenderablePathway pathwayContainer = getPathwayContainer(r);
                if (pathwayContainer == process) {
                    GKInstance inst = (GKInstance) renderable2InstanceMap.get(r);
                    if (inst != null && hasCompAtt.isValidValue(inst))
                        compInstances.add(inst);
                }
            }
            it.remove(); // Empty components list
        }
        converted.setAttributeValue(ReactomeJavaConstants.hasEvent,
                                    compInstances);
    }
    
    /**
     * This helper method is used to get a up-level pathway container. A reaction
     * may be contained by a compartment. In this case, compartment's container will
     * be checked until a pathway is found.
     * @param r
     * @return
     */
    private RenderablePathway getPathwayContainer(Renderable r) {
        Renderable container = r.getContainer();
        while (true) {
            if (container == null ||
                container instanceof RenderablePathway)
                break;
            container = container.getContainer();
        }
        return (RenderablePathway) container;
    }
    
    private void computePrcedingEvents(RenderablePathway pathway) throws Exception {
        List components = pathway.getComponents();
        if (components == null)
            return;
        // Check reactions first
        List reactions = new ArrayList();
        List flowLines = new ArrayList();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableReaction ||
                r instanceof RenderableInteraction) {
                reactions.add(r);
            }
            else if (r instanceof FlowLine)
                flowLines.add(r);
        }
        // Computer precedingEvents
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            HyperEdge edge = (HyperEdge) it.next();
            List precedingEvents = null;
            for (Iterator it1 = reactions.iterator(); it1.hasNext();) {
                HyperEdge edge1 = (HyperEdge) it1.next();
                if (edge1 == edge)
                    continue;
                if (isPrecedingEvent(edge1, edge)) {
                    if (precedingEvents == null)
                        precedingEvents = new ArrayList();
                    precedingEvents.add(edge1);
                }
            }
            if (precedingEvents != null)
                edge.setAttributeValue(RenderablePropertyNames.PRECEDING_EVENT, 
                                       precedingEvents);
        }
        // Check FlowLine
        for (Iterator it = flowLines.iterator(); it.hasNext();) {
            FlowLine fl = (FlowLine) it.next();
            Node input = fl.getInputNode(0);
            Node output = fl.getOutputNode(0);
            if (input == null || output == null)
                continue;
            List precedingEvents = (List) output.getAttributeValue(RenderablePropertyNames.PRECEDING_EVENT);
            if (precedingEvents == null) {
                precedingEvents = new ArrayList();
                output.setAttributeValue(RenderablePropertyNames.PRECEDING_EVENT,
                                         precedingEvents);
            }
            precedingEvents.add(input);
        }
    }
    
    private boolean isPrecedingEvent(HyperEdge pre,
                                     HyperEdge edge) throws Exception {
        // one of inputs is an output of pre
        List inputs = edge.getInputNodes();
        List preOutputs = pre.getOutputNodes();
        if (inputs != null && preOutputs != null) {
            for (Iterator it = inputs.iterator(); it.hasNext();) {
                Renderable input = (Renderable) it.next();
                if (contains(preOutputs, input))
                    return true;
            }
        }
        // one of catalysts is an output of pre
        List catalysts = edge.getHelperNodes();
        if (catalysts != null && preOutputs != null) {
            for (Iterator it = catalysts.iterator(); it.hasNext();) {
                Renderable catalyst = (Renderable) it.next();
                if (contains(preOutputs, catalyst))
                    return true;
            }
        }
        return false;
    }
    
    private boolean contains(List list,
                             Renderable r) {
        String name = r.getDisplayName();
        // Check based on name
        Renderable r1 = null;
        for (Iterator it = list.iterator(); it.hasNext();) {
            r1 = (Renderable) it.next();
            if (r1.getDisplayName().equals(name))
                return true;
        }
        return false;
    }
    
    private RenderablePathway loadAuthorToolFile(File file) throws Exception {
        GKBReader reader = new GKBReader();
        Project project = reader.open(file.getAbsolutePath());
        RenderablePathway process = project.getProcess();
        return process;
    }
    
    private RenderablePathway loadAuthorToolV2File(File file) throws Exception {
        FilePersistence reader = new FilePersistence();
        Project project = reader.open(file.getAbsolutePath());
        RenderablePathway pathway = project.getProcess();
        return pathway;
    }
    
    private Set grepConvertables(RenderablePathway process) throws Exception {
        Set set = new HashSet();
        if (process.getComponents() != null) {
            for (Iterator it = process.getComponents().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof FlowLine &&
                    !(r instanceof RenderableInteraction))
                    continue; // Escape FlowLines
                if (r instanceof Note || r instanceof RenderableCompartment)
                    continue;
                if (r instanceof Shortcut)
                    set.add(((Shortcut)r).getTarget());
                else {
                    set.add(r);
                    if (r instanceof RenderableComplex)
                        grepComplexComponents((RenderableComplex)r, set);
                    // All pathways are contained by process
                }
            }
        }
        return set;
    }
    
    private Set grepConvertablesForVer2(RenderablePathway process) throws Exception {
        Set set = new HashSet();
        List renderables = RenderUtility.getAllDescendents(process);
        set.addAll(renderables);
        // Remove flow line. It is not useful
        for (Iterator it = set.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof FlowLine || 
                obj instanceof Shortcut)
                it.remove();
        }
        return set;
    }
    
    private void grepComplexComponents(RenderableComplex complex,
                                       Set set) {
        List components = complex.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable comp = (Renderable) it.next();
            if (comp instanceof Shortcut)
                set.add(((Shortcut)comp).getTarget());
            else {
                set.add(comp);
                if (comp instanceof RenderableComplex)
                    grepComplexComponents((RenderableComplex)comp, set);
            }
        }
    }    
}
