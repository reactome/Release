/*
 * Created on Dec 19, 2008
 *
 */
package org.gk.elv;

import java.util.List;

import org.gk.model.GKInstance;
import org.gk.render.Renderable;

/**
 * This super class is mainly for the purpose of organization to group all subclasses
 * together.
 * @author wgm
 *
 */
public class ElvInstanceEditHandler {
    
    protected InstanceZoomablePathwayEditor zoomableEditor;
    
    public ElvInstanceEditHandler() {
        
    }

    public InstanceZoomablePathwayEditor getZoomableEditor() {
        return zoomableEditor;
    }

    public void setZoomableEditor(InstanceZoomablePathwayEditor zoomableEditor) {
        this.zoomableEditor = zoomableEditor;
    }
    
    protected void updateDoNotReleaseEventVisible(GKInstance instance) {
        zoomableEditor.updateDoNotReleaseEventVisible(instance);
    }
    
    /**
     * Check if a GKInstance can be inserted into the assigned pathway editor. Default
     * is fine for any GKInstance. 
     * @param instance
     * @return
     * @throws Exception
     */
    public boolean isInsertable(GKInstance instance,
                                Renderable converted) throws Exception {
        if (converted.isTransferrable())
            return true;
        // Means alias is not allowed!
        List<Renderable> list = zoomableEditor.searchConvertedRenderables(instance);
        return !(list != null && list.size() > 0);
    }
    
    /**
     * Do cleaning up job after a GKInstance is inserted. Default action is nothing.
     * @param instance GKInstance object that has been inserted
     * @param renderable Converted GKInstances inserted as is.
     */
    public void postInsert(GKInstance instance,
                           Renderable renderable) throws Exception {
        
    }
    
}
