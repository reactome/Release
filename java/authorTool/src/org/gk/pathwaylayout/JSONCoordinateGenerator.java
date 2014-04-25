/*
 * Created on Feb 17, 2009
 *
 */
package org.gk.pathwaylayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * This class is used to help generate JSON coordinate. This is basically proted from
 * Imre's perl module, Coordinate.pm.
 * @author wgm
 *
 */
public class JSONCoordinateGenerator {
 
    public JSONCoordinateGenerator() {
    }
    
    public String generateCoordinateJSON(GKInstance diagram,
                                         MySQLAdaptor dba) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("({");
        int height = (Integer) diagram.getAttributeValue(ReactomeJavaConstants.height);
        int width = (Integer) diagram.getAttributeValue(ReactomeJavaConstants.width);
        builder.append("\"height\": ").append(height).append(",\n");
        builder.append("\"width\": ").append(width).append(",\n");
        builder.append("\"coordinates\": [\n");
        // Generate the actual coordinates
        generateJSON(builder, diagram, dba);
        builder.append("]\n").append("})");
        return builder.toString();
    }
    
    private void generateJSON(StringBuilder builder,
                              GKInstance diagram,
                              MySQLAdaptor dba) throws Exception {
        Collection vertices = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex, 
                                                           ReactomeJavaConstants.pathwayDiagram,
                                                           "=",
                                                           diagram);
        if (vertices == null || vertices.size() == 0)
            return;
        // Create a map
        final Map<GKInstance, List<GKInstance>> rToVMap = new HashMap<GKInstance, List<GKInstance>>();
        for (Iterator it = vertices.iterator(); it.hasNext();) {
            GKInstance v = (GKInstance) it.next();
            // Gets db_id for entity in corresponding table (i.e. from EWAS table)
            GKInstance r = (GKInstance) v.getAttributeValue(ReactomeJavaConstants.representedInstance);
            if (r == null)
                continue; // Just in case
            List<GKInstance> list = rToVMap.get(r);
            if (list == null) {
                list = new ArrayList<GKInstance>();
                rToVMap.put(r, list);
            }
            list.add(v);
        }
        // Need to sort all vertices
        List<GKInstance> keys = new ArrayList<GKInstance>(rToVMap.keySet());
        Collections.sort(keys, new Comparator<GKInstance>() {
            public int compare(GKInstance r1, 
                               GKInstance r2) {
                try {
                    List<GKInstance> list1 = rToVMap.get(r1);
                    List<GKInstance> list2 = rToVMap.get(r2);
                    GKInstance inst1 = list1.get(0);
                    GKInstance inst2 = list2.get(0);
                    int x1 = (Integer) inst1.getAttributeValue(ReactomeJavaConstants.x);
                    int x2 = (Integer) inst2.getAttributeValue(ReactomeJavaConstants.x);
                    return x1 - x2;
                }
                catch(Exception e) {}
                return 0;
            }
        });
        for (Iterator<GKInstance> it = keys.iterator(); it.hasNext();) {
            GKInstance instance = it.next();
            builder.append("{\"cls\": \"").append(instance.getSchemClass().getName()).append("\", ");
            builder.append("\"id\": ").append(instance.getDBID()).append(", ");
            if(instance.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)){            	
//                Collection ewasC = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
//                                                                ReactomeJavaConstants.DB_ID,
//                                                                "=",
//                                                                instance.getDBID());
//                GKInstance v = (GKInstance) ewasC.iterator().next();
                GKInstance refEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (refEntity != null)
                    builder.append("\"refid\": ").append(refEntity.getDBID()).append(", ");
            }
            String name = instance.getDisplayName();
            name = StringEscapeUtils.escapeHtml(name);
            builder.append("\"name\": \"").append(name).append("\", ");
            builder.append("\"coords\": [");
            List<GKInstance> list = rToVMap.get(instance);
            generateCoordinateJSON(builder, list);
            builder.append("], ");
            String type = null;
            if (instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                type = "Reaction";
            else if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex))
                type = "Complex";
            else
                type = "Entity";
            builder.append("\"icon\": \"").append("/icons/dtree/").append(type).append(".gif\"");
            builder.append("}");
            if (it.hasNext())
                builder.append(",");
            builder.append("\n");
        }
    }
     
    private void generateCoordinateJSON(StringBuilder builder,
                                        List<GKInstance> vertices) throws Exception {
        for (Iterator<GKInstance> it = vertices.iterator(); it.hasNext();) {
            GKInstance v = it.next();
            builder.append("{");
            if (v.getSchemClass().isa(ReactomeJavaConstants.EntityVertex) ||
                v.getSchemClass().isa(ReactomeJavaConstants.PathwayVertex)) {
                int x = (Integer) v.getAttributeValue(ReactomeJavaConstants.x);
                int y = (Integer) v.getAttributeValue(ReactomeJavaConstants.y);
                int w = (Integer) v.getAttributeValue(ReactomeJavaConstants.width);
                int h = (Integer) v.getAttributeValue(ReactomeJavaConstants.height);
                builder.append("\"x\": ").append(x).append(", ");
                builder.append("\"y\": ").append(y).append(", ");
                builder.append("\"w\": ").append(w).append(", ");
                builder.append("\"h\": ").append(h).append(", ");
                builder.append("\"id\": ").append(v.getDBID()).append(", ");
                builder.append("\"cls\": \"EntityVertex\"");
            }
            else if (v.getSchemClass().isa(ReactomeJavaConstants.ReactionVertex)) {
                int x = (Integer) v.getAttributeValue(ReactomeJavaConstants.x);
                int y = (Integer) v.getAttributeValue(ReactomeJavaConstants.y);
                builder.append("\"x\": ").append(x).append(", ");
                builder.append("\"y\": ").append(y).append(", ");
                builder.append("\"w\": 0, ");
                builder.append("\"h\": 0, ");
                builder.append("\"id\": ").append(v.getDBID()).append(", ");
                builder.append("\"cls\": \"ReactionVertex\"");
            }
            builder.append("}");
            if (it.hasNext())
                builder.append(", ");
        }
    }
    
}
