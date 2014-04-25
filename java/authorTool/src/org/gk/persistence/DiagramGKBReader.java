/*
 * Created on Nov 18, 2008
 *
 */
package org.gk.persistence;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderablePathway;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


/**
 * This subclass to GKBReader is used to read in diagram information for 
 * a Curator Tool project.
 * @author wgm
 *
 */
public class DiagramGKBReader extends GKBReader {
    // Used to correct something
    private PersistenceAdaptor persistenceAdaptor;
    
    public DiagramGKBReader() {
    }
    
    public void setPersistenceAdaptor(PersistenceAdaptor adaptor) {
        this.persistenceAdaptor = adaptor;
    }
    
    public PersistenceAdaptor getPersistenceAdaptor() {
        return this.persistenceAdaptor;
    }
    
    /**
     * Generate a displayable diagram from a PathwayDiagram instance.
     * @param pathwayDiagram
     * @return
     * @throws Exception
     */
    public RenderablePathway openDiagram(GKInstance pathwayDiagram) throws Exception {
        if (!pathwayDiagram.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
            throw new IllegalArgumentException(pathwayDiagram + " is not a PathwayDiagram instance!");
        }
        String xml = (String) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.storedATXML);
//        System.out.println(xml);
        if (xml == null)
            return null;
        RenderablePathway rPathway = openDiagram(xml);
        setDisplayNames(rPathway,
                        pathwayDiagram.getDbAdaptor());
        // For some old pathway diagram, there is no PathwayDiagram id
        if (rPathway.getReactomeDiagramId() == null)
            rPathway.setReactomeDiagramId(pathwayDiagram.getDBID());
        return rPathway;
    }
    
    /**
     * Open a diagram from an XML String.
     * @param xml
     * @return
     * @throws Exception
     */
    public RenderablePathway openDiagram(String xml) throws Exception {
        if (xml == null)
            return null;
        Reader sReader = new StringReader(xml);
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document document = builder.build(sReader);
        org.jdom.Element root = document.getRootElement();
        RenderablePathway pathway = openProcess(root);
        return pathway;
    }
    
    @Override
    public RenderablePathway openProcess(Element root) {
        RenderablePathway pathway = super.openProcess(root);
        // Check if hideCompartmentInNode value
        String hideComptInNode = root.getAttributeValue("hideCompartmentInName");
        if (hideComptInNode != null)
            pathway.setHideCompartmentInNode(new Boolean(hideComptInNode));
        else // Default should hide display name if not specified.
            pathway.setHideCompartmentInNode(true);
        return pathway;
    }

    /**
     * Use this method to set the display name for components in a diagram displayed
     * by a RenderablePathway object.
     * @param pathway
     * @param fileAdaptor
     */
    public void setDisplayNames(RenderablePathway pathway,
                                PersistenceAdaptor fileAdaptor) throws Exception {
        List<?> components = pathway.getComponents();
        if (components != null) {
            for (Iterator<?> it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                Long reactomeId = r.getReactomeId();
                if (reactomeId == null)
                    continue; // In case for notes
                GKInstance instance = fileAdaptor.fetchInstance(reactomeId);
                if (instance == null)
                    continue;
                String displayName = instance.getDisplayName();
//                String displayName = getShortestDisplayName(instance);
                r.setDisplayName(displayName);
            }
        }
        // Don't forget the top-level pathway
        boolean topNameIsSet = false;
        if (pathway.getReactomeDiagramId() != null) {
            GKInstance pdInst = fileAdaptor.fetchInstance(pathway.getReactomeDiagramId());
            if (pdInst != null) {
                pathway.setDisplayName(pdInst.getDisplayName());
                topNameIsSet = true;
            }
        }
        if (!topNameIsSet) {
            // A second choice
            GKInstance top = fileAdaptor.fetchInstance(pathway.getReactomeId());
            if (top == null) // Just in case there is an error.
                System.err.println("No Pathway instance existing for diagram: " + pathway.getDisplayName() + 
                                   " (" + pathway.getReactomeId() + ")");
            else
                pathway.setDisplayName(top.getDisplayName());
        }
        if (pathway.getHideCompartmentInNode())
            RenderUtility.hideCompartmentInNodeName(pathway);
        // Test code should be removed
//        removeSmallMolecules(pathway);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Renderable createRenderableFromElement(Element elm, 
                                                     Map id2Object) {
        Renderable r = super.createRenderableFromElement(elm, id2Object);
        if (r.getReactomeId() != null && persistenceAdaptor != null) {
            try {
                GKInstance inst = persistenceAdaptor.fetchInstance(r.getReactomeId());
                if (inst != null) {
                    if (inst.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                        if (r.getClass() != RenderableEntitySet.class) {
                            // Old format. Needs to be updated!
                            Renderable r1 = new RenderableEntitySet();
                            r1.setID(r.getID());
                            r1.setReactomeId(r.getReactomeId());
                            r = r1;
                            // Replace the old registered object
                            id2Object.put(r.getID() + "", r);
                        }
                    }
                }
            }
            catch(Exception e) {
                // Just show nothing here
            }
        }
        return r;
    }
    
//    
//    /**
//     * This is a test method and should not be in the released code.
//     * @param inst
//     * @return
//     * @throws Exception
//     */
//    private String getShortestDisplayName(GKInstance inst) throws Exception {
//        return inst.getDisplayName();
        // Want to get a list of usable names and pick up the shortest one
//        Set<String> names = new HashSet<String>();
//        names.add(inst.getDisplayName());
//        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.name)) {
//            List<?> list = inst.getAttributeValuesList(ReactomeJavaConstants.name);
//            if (list != null)
//                for (Object obj : list)
//                    names.add(obj.toString());
//        }
//        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
//            GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
//            if (refEntity != null) {
//                List<?> list = refEntity.getAttributeValuesList(ReactomeJavaConstants.name);
//                if (list != null)
//                    for (Object obj : list)
//                        names.add(obj.toString());
//            }
//        }
//        // Get the shortest name
//        String rtn = null;
//        for (String name : names) {
//            if (rtn == null)
//                rtn = name;
//            else if (name.length() < rtn.length())
//                rtn = name;
//        }
//        return rtn;
//    }
    
//    /**
//     * Another test method to hide small molecules
//     * @param pathway
//     * @throws Exception
//     */
//    private void removeSmallMolecules(RenderablePathway pathway) throws Exception {
//        Set<String> toBeHidden = new HashSet<String>();
//        toBeHidden.add("ATP");
//        toBeHidden.add("ADP");
//        toBeHidden.add("H2O");
//        List<?> components = pathway.getComponents();
//        Set<Renderable> set = new HashSet<Renderable>();
//        for (Iterator<?> it = components.iterator(); it.hasNext();) {
//            Renderable r= (Renderable) it.next();
//            if (r instanceof Node && toBeHidden.contains(r.getDisplayName())) {
//                r.clearConnectWidgets();
//                it.remove();
//                r.setContainer(null);
//            }
//        }
//    }
    
}
