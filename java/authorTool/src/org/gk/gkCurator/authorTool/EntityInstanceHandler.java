/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.property.SearchDBTypeHelper;
import org.gk.render.Node;
import org.gk.render.NodeAttachment;
import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderableFeature;

public class EntityInstanceHandler extends InstanceHandler {
    private SearchDBTypeHelper typeHelper;
    
    public EntityInstanceHandler() {
        typeHelper = new SearchDBTypeHelper();
    }
    
    protected Renderable convertToRenderable(GKInstance instance) throws Exception {
        // Have to find the type for instance
        Class type = typeHelper.guessNodeType(instance);
        Renderable r = RenderableFactory.generateRenderable(type, container);
        return r;
    }

    protected void convertProperties(GKInstance instance, 
                                     Renderable r, 
                                     Map iToRMap) throws Exception {
    }

    @Override
    public void simpleConvertProperties(GKInstance instance, 
                                        Renderable r,
                                        Map<GKInstance, Renderable> toRMap) throws Exception {
        handleModifiedResidues(instance, r);
    }

    private void handleModifiedResidues(GKInstance instance,
                                        Renderable r) throws Exception {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return;
        // Just in case
        if (!(r instanceof Node))
            return;
        // Want to convert ModifieredResidue
        List list = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (list == null || list.size() == 0)
            return;
        // Need to convert to attachments
        List<NodeAttachment> features = new ArrayList<NodeAttachment>();
        ModifiedResidueHandler handler = new ModifiedResidueHandler();
        for (Iterator it = list.iterator(); it.hasNext();) {
            GKInstance modifiedResidue = (GKInstance) it.next();
            RenderableFeature feature = handler.convertModifiedResidue(modifiedResidue);
            features.add(feature);
        }
        Node node = (Node) r;
        node.setNodeAttachmentsLocally(features);
    }
}
