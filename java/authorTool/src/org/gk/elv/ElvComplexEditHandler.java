/*
 * Created on Dec 19, 2008
 *
 */
package org.gk.elv;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.database.AttributeEditEvent;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;

/**
 * This helper class is used to handle attribute edit for complex.
 * @author wgm
 *
 */
public class ElvComplexEditHandler extends ElvPhysicalEntityEditHandler {
    
    public ElvComplexEditHandler() {
    }
    
    public void complexEdit(AttributeEditEvent e) {
        String attName = e.getAttributeName();
        if (!attName.equals(ReactomeJavaConstants.hasComponent)) {
            return;
        }
        GKInstance instance = e.getEditingInstance();
        List<Renderable> list = zoomableEditor.searchConvertedRenderables(instance);
        if (list == null || list.size() == 0)
            return;
        // Only complex having subunits displayed need to be checked
        for (Renderable r : list) {
            // For sure it should be a node
            if (!(r instanceof Node))
                continue; // There is a bug somewhere
            complexEdit((Node)r, e);
        }
    }
    
    public void validateDisplayedComplex(RenderableComplex complex) {
        XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
        GKInstance instance = fileAdaptor.fetchInstance(complex.getReactomeId());
        if (instance == null || instance.isShell())
            return; // Don't bother if it is shell.
//        Map<GKInstance, Integer> compMap = getCompMap(complex);
//        if (compMap.size() == 1) {
//            // Make sure it is displayed as multimer
//            Integer stoi = compMap.get(compMap.keySet().iterator().next());
//            if (stoi > 1) {
//                // Should be displayed as multimer
//                if (complex.getMultimerMonomerNumber() != stoi)
//                    zoomableEditor.reInsertInstance(instance);
////                else {
////                    // Just check the display name
////                    if (!complex.getDisplayName().equals(compMap.keySet().iterator().next().getDisplayName()))
////                        zoomableEditor.reInsertInstance(instance);
////                }
//            }
//        }
//        else {
            if (complex.isComponentsHidden())
                return; // don't bother
            // Check if any component has been changed
            validateComplexComponents(complex, instance);
        //}
    }
    
    private void validateComplexComponents(RenderableComplex complex,
                                           GKInstance instance) {
        try {
            // instance maybe an EntitySet since EntitySet can be converted to Complex
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                return;
            List hasComponents = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            List<GKInstance> iCopy = null;
            if (hasComponents == null)
                iCopy = new ArrayList<GKInstance>();
            else
                iCopy = new ArrayList<GKInstance>(hasComponents);
            List<Renderable> components = complex.getComponents();
            List<Renderable> rCopy = null;
            if (components == null)
                rCopy = new ArrayList<Renderable>();
            else
                rCopy = new ArrayList<Renderable>(components);
            List<Renderable> deleted = new ArrayList<Renderable>();
            XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
            for (Renderable r : rCopy) {
               GKInstance i = fileAdaptor.fetchInstance(r.getReactomeId());
               if (!iCopy.remove(i))
                   deleted.add(r);
            }
            for (Renderable r : deleted) {
                deleteComplexComponent(complex, r);
            }
            // New components
            for (GKInstance i : iCopy) {
                addComplexComponent(complex, i);
            }
        }
        catch(Exception e) {
            System.err.println("ElvComplexEditHandler.validateComplexComponents(): " + e);
            e.printStackTrace();
        }
    }
    
    protected Renderable addComplexComponent(RenderableComplex complex,
                                             GKInstance comp) {
        Renderable r = zoomableEditor.insertInstance(comp);
        if (r instanceof RenderableComplex)
            ((RenderableComplex)r).hideComponents(true);
        r.setPosition(new Point(complex.getPosition()));
        // Use the complex's bounds: smaller
        Rectangle bounds = new Rectangle(complex.getBounds());
        // Use half of the bounds
        bounds.x += bounds.width / 4;
        bounds.y += bounds.height / 4;
        bounds.width /= 2;
        bounds.height /= 2;
        r.setBounds(bounds);
        complex.addComponent(r);
        r.setContainer(complex);
        return r;
    }
    
    private void multimerEdit(Node complex,
                              AttributeEditEvent edit) {
        // Just do a reinsert
        GKInstance instance = zoomableEditor.getXMLFileAdaptor().fetchInstance(complex.getReactomeId());
        if (instance == null)
            return;
        zoomableEditor.reInsertInstance(instance);
    }
    
//    private Map<GKInstance, Integer> getCompMap(Node complex) {
//        GKInstance instance = zoomableEditor.getXMLFileAdaptor().fetchInstance(complex.getReactomeId());
//        Map<GKInstance, Integer> compMap = new HashMap<GKInstance, Integer>();
//        if (instance.isShell())
//            return compMap;
//        try {
//            List list = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
//            if (list != null) {
//                for (Object obj : list) {
//                    GKInstance comp = (GKInstance) obj;
//                    Integer stoi = compMap.get(comp);
//                    if (stoi == null)
//                        compMap.put(comp, 1);
//                    else
//                        compMap.put(comp, ++stoi);
//                }
//            }
//        }
//        catch(Exception e) {
//            System.err.println("ElvComplexEditHandler.getCompMap(): " + e);
//            e.printStackTrace();
//        }
//        return compMap;
//    }
    
    private void complexEdit(Node complex,
                             AttributeEditEvent edit) {
        // Disable multimer display: cannot figure out a good way to solve 
        // _displayName problem. Should it be a subunit, how to save it?
//        if (complex.getMultimerMonomerNumber() > 1) {
//            multimerEdit(complex, edit);
//            return;
//        }
        // The node should be a complex if it is not displayed as multimer
        if (!(complex instanceof RenderableComplex))
            return;
        boolean needRepaint = false;
        if (edit.getEditingType() == AttributeEditEvent.REMOVING) {
            // Check if multimer should be used
//            Map<GKInstance, Integer> compMap = getCompMap(complex);
//            if (compMap.size() == 1) {
//                // Should be displayed as multimer
//                multimerEdit(complex, edit);
//                return;
//            }
            RenderableComplex complex1 = (RenderableComplex) complex;
            if (complex1.isComponentsHidden())
                return;
            // It is possible the complex can be displayed as 
            List removed = edit.getRemovedInstances();
            List components = complex.getComponents();
            // Just in case
            if (removed != null && components != null) {
                for (Iterator it = removed.iterator(); it.hasNext();) {
                    GKInstance instance = (GKInstance) it.next();
                    Renderable foundComp = null;
                    components = complex.getComponents();
                    for (Iterator it1 = components.iterator(); it1.hasNext();) {
                        Renderable r = (Renderable) it1.next();
                        if (r.getReactomeId().equals(instance.getDBID())) {
                            foundComp = r;
                            break;
                        }
                    }
                    if (foundComp != null) {
                        deleteComplexComponent(complex, foundComp);
                        needRepaint = true;
                    }
                }
            }
        }
        else if (edit.getEditingType() == AttributeEditEvent.ADDING) {
            RenderableComplex complex1 = (RenderableComplex) complex;
            if (complex1.isComponentsHidden())
                return;
            List added = edit.getAddedInstances();
            if (added != null && added.size() > 0) {
                for (Iterator it = added.iterator(); it.hasNext();) {
                    GKInstance instance = (GKInstance) it.next();
                    // The complex should be RenderableComplex
                    if (complex instanceof RenderableComplex) {
                        addComplexComponent((RenderableComplex)complex, 
                                            instance);
                        needRepaint = true;
                    }
                }
            }
        }
        if (needRepaint) {
            PathwayEditor pathwayEditor = zoomableEditor.getPathwayEditor();
            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        }
    }

    private void deleteComplexComponent(Node complex, Renderable foundComp) {
        complex.removeComponent(foundComp);
        foundComp.setContainer(null);
        zoomableEditor.getPathwayEditor().delete(foundComp);
    }
    
}
