/*
 * Created on Nov 18, 2008
 *
 */
package org.gk.persistence;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Note;
import org.gk.render.Renderable;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderablePropertyNames;
import org.jdom.Element;

/**
 * This customized GKBWriter is used to save diagrams generated in the curator tool. For the curator
 * tool diagram, properties are not needed to save. 
 * @author wgm
 *
 */
public class DiagramGKBWriter extends GKBWriter {
    // A flag to control if display names should be written out
    private boolean needDisplayName;
    // Used to generate type of object if needed
    private PersistenceAdaptor persistenceAdaptor;
    
    public DiagramGKBWriter() {
    }
    
    public PersistenceAdaptor getPersistenceAdaptor() {
        return persistenceAdaptor;
    }

    public void setPersistenceAdaptor(PersistenceAdaptor persistenceAdaptor) {
        this.persistenceAdaptor = persistenceAdaptor;
    }

    public void setNeedDisplayName(boolean need) {
        this.needDisplayName = need;
    }
    
    /**
     * Generate XML text for the passed RenderabelPathway object.
     * @param pathway
     * @return
     * @throws Exception
     */
    public String generateXMLString(RenderablePathway pathway) throws Exception {
        Project project = new Project(pathway);
        return generateXMLString(project);
    }
    
    @Override
    protected Element createProcessNode(RenderablePathway pathway) {
        Element element = super.createProcessNode(pathway);
        element.setAttribute("hideCompartmentInName", 
                             pathway.getHideCompartmentInNode() + "");
        return element;
    }

    /**
     * Override the super class method to avoid saving properties.
     */
    @Override
    protected void saveProperties(Element elm, Renderable r) {
        // Don't allow to save properties in this subclass.
        if (r instanceof Note) { // Note is handled here.
            // Force to save.
            //RenderableRegistry.getRegistry().add(r);
            super.saveProperties(elm, r);
        }
        else if (needDisplayName) {
            String displayName = r.getDisplayName();
            if (displayName != null) {
                org.jdom.Element propsElm = new org.jdom.Element("Properties");
                elm.addContent(propsElm);
                saveProperty(RenderablePropertyNames.DISPLAY_NAME,
                             displayName, 
                             propsElm);
            }
        }
    }

    /**
     * Override the method in the superclass method so that schema class can be provided
     * if needed.
     */
    @Override
    protected Element createElementForRenderable(Renderable r) {
        Element elm = super.createElementForRenderable(r);
        if (persistenceAdaptor != null && r.getReactomeId() != null) {
            try {
                GKInstance inst = persistenceAdaptor.fetchInstance(r.getReactomeId());
                if (inst != null) {
                    elm.setAttribute(RenderablePropertyNames.SCHEMA_CLASS, 
                                     inst.getSchemClass().getName());
                    // Do a modification for old PathwayDiagrams
                    if (inst.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                        elm.setName(RenderableEntitySet.class.getName());
                }
            }
            catch (Exception e) {
                // Just do nothing if there is an exception thrown!
//                e.printStackTrace();
            }
        }
        return elm;
    }
    
}
