/*
 * Created on Dec 16, 2008
 *
 */
package org.gk.elv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.database.AttributeEditEvent;
import org.gk.gkCurator.authorTool.ModifiedResidueHandler;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Node;
import org.gk.render.NodeAttachment;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableFeature;

/**
 * This helper class is used to handle attribute edit from EWAS instances, which should have
 * effects in the entity level view.
 * @author wgm
 *
 */
public class ElvEWASEditHandler extends ElvPhysicalEntityEditHandler {
    
    public ElvEWASEditHandler() {
    }
    
    public void ewasEdit(AttributeEditEvent event) {
        GKInstance instance = event.getEditingInstance();
        // Just in case
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
            return;
        String attName = event.getAttributeName();
        if (attName.equals(ReactomeJavaConstants.hasModifiedResidue))
            hadModifiedResidueEdit(instance);
    }
    
    public void modifiedResidueEdit(AttributeEditEvent event) {
        GKInstance modifiedResidue = event.getEditingInstance();
        if (!modifiedResidue.getSchemClass().isa(ReactomeJavaConstants.ModifiedResidue))
            return;
        // Get EWAS referring to this instance
        XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
        try {
            Map map = fileAdaptor.getReferrersMap(modifiedResidue);
            List pes = (List) map.get(ReactomeJavaConstants.hasModifiedResidue);
            if (pes == null || pes.size() == 0)
                return;
            // Need to check the change
            for (Iterator it = pes.iterator(); it.hasNext();) {
                GKInstance pe = (GKInstance) it.next();
                modifiedResidueEdit(pe, modifiedResidue);
            }
        }
        catch(Exception e) {
            System.err.println("EWASEditHandler.modifiedResidueEdit(): " + e);
            e.printStackTrace();
        }
    }
    
    private void modifiedResidueEdit(GKInstance pe,
                                     GKInstance modifiedResidue) throws Exception {
        List<Renderable> displayed = zoomableEditor.searchConvertedRenderables(pe);
        if (displayed == null || displayed.size() == 0)
            return;
        for (Renderable r : displayed) {
            if (!(r instanceof Node))
                continue;
            Node node = (Node) r;
            modifiedResidueEdit(node, modifiedResidue);
        }
        PathwayEditor editor = zoomableEditor.getPathwayEditor();
        editor.repaint(editor.getVisibleRect());
    }
    
    private void modifiedResidueEdit(Node node,
                                     GKInstance modifiedResidue) throws Exception {
        List<NodeAttachment> attachments = node.getNodeAttachments();
        if (attachments == null || attachments.size() == 0)
            return; // For some reason, it is not displayed.
        // Need to find which NodeAttachment 
        NodeAttachment found = null;
        for (NodeAttachment attachment : attachments) {
            if (attachment.getReactomeId().equals(modifiedResidue.getDBID())) {
                found = attachment;
                break;
            }
        }
        if (found != null)
            node.removeNodeAttachment(found);
        // Just create a new one
        ModifiedResidueHandler handler = new ModifiedResidueHandler();
        RenderableFeature newAttachment = handler.convertModifiedResidue(modifiedResidue);
        // Use the same position to avoid a surprise
        newAttachment.setRelativePosition(found.getRelativeX(), 
                                          found.getRelativeY());
        node.addFeatureLocally(newAttachment);
    }
    
    private void hadModifiedResidueEdit(GKInstance instance) {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return;
        List<Renderable> displayed = zoomableEditor.searchConvertedRenderables(instance);
        if (displayed == null || displayed.size() == 0)
            return;
        // Check if any change in the hasModifiedResidue attribute
        try {
            List list = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            for (Renderable r : displayed) {
                validateNodeAttachments(r, list);
            }
        }
        catch(Exception e) {
            System.err.println("EWASEditHandler.hasModifieedResidueEdit(): " + e);
            e.printStackTrace();
        }
    }
    
    public void validateDisplayedNodeAttachments(Node node) {
        if (node.getReactomeId() == null || 
            node instanceof RenderableCompartment)
            return;
        XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
        GKInstance instance = (GKInstance) fileAdaptor.fetchInstance(node.getReactomeId());
        if (instance == null)
            return;
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return;
        try {
            List list = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            validateNodeAttachments(node, list);
        }
        catch(Exception e) {
            System.err.println("ElvEWASEditHandler.validateDisplayedNodeAttachments(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Make sure the node attachments are consistent with the hasModifiedResidue values.
     * @param r
     * @param hasModifiedResidues
     */
    private void validateNodeAttachments(Renderable r,
                                        List hasModifiedResidues) {
        if (!(r instanceof Node))
            return;
        Node node = (Node) r;
        List<NodeAttachment> nodeAttachments = node.getNodeAttachments();
        if ((nodeAttachments == null || nodeAttachments.size() == 0) &&
            (hasModifiedResidues == null || hasModifiedResidues.size() == 0))
            return;
        List copy = null;
        if (hasModifiedResidues == null)
            copy = new ArrayList();
        else
            copy = new ArrayList(hasModifiedResidues);
        if (nodeAttachments != null && nodeAttachments.size() > 0) {
            List<NodeAttachment> deleted = new ArrayList<NodeAttachment>();
            boolean isFound = false;
            for (NodeAttachment attachment : nodeAttachments) {
                if (attachment.getReactomeId() == null)
                    continue; // Don't care. Let it be!
                isFound = false;
                // Check if this attachment is still there
                for (Iterator it = copy.iterator(); it.hasNext();) {
                    GKInstance instance = (GKInstance) it.next();
                    if (instance.getDBID().equals(attachment.getReactomeId())) {
                        isFound = true;
                        it.remove();
                        break;
                    }
                }
                if (!isFound)
                    deleted.add(attachment);
            }
            for (NodeAttachment attachment : deleted)
                node.removeNodeAttachment(attachment);
        }
        // Check if new node attachment should be added
        if (copy.size() > 0) {
            ModifiedResidueHandler handler = new ModifiedResidueHandler();
            for (Iterator it = copy.iterator(); it.hasNext();) {
                GKInstance hasModifiedResidue = (GKInstance) it.next();
                RenderableFeature feature = handler.convertModifiedResidue(hasModifiedResidue);
                node.addFeatureLocally(feature);
            }
        }
    }
    
}
