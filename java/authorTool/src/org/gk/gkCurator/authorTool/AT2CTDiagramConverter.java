/*
 * Created on Jan 13, 2009
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.GKBReader;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.Project;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;

/**
 * This application is used to convert a AT project into a CT diagram so that it can be used
 * directly for display pathway.
 * @author wgm
 *
 */
public class AT2CTDiagramConverter {
    
    public static void main(String[] args) {
        String source = "/Users/wgm/Documents/wgm/work/reactome/SignalingByEGFRNoComp.gkb";
        GKBReader reader = new GKBReader();
        try {
            Project project = reader.open(source);
            RenderablePathway pathway = project.getProcess();
            // Remove complex components that have been hidden
            List copy = new ArrayList(pathway.getComponents());
            for (Iterator it = copy.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.isVisible())
                    continue;
                Renderable container = r.getContainer();
                container.removeComponent(r);
                r.setContainer(null);
                pathway.removeComponent(r);
            }
            // Remove shortcuts
            copy = new ArrayList(pathway.getComponents());
            for (Iterator it = copy.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.getShortcuts() != null)
                    r.setShortcuts(null);
            }
            // Save it
            String target = "/Users/wgm/Documents/wgm/work/reactome/SignalingByEGFRNoComp_InCT.gkb";
            GKBWriter writer = new DiagramGKBWriter();
            writer.save(project, target);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
