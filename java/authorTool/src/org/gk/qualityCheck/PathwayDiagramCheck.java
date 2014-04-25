/*
 * Created on Sep 29, 2010
 *
 */
package org.gk.qualityCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.util.StringUtils;


public class PathwayDiagramCheck extends SingleAttributeClassBasedCheck {

    public PathwayDiagramCheck() {
        checkClsName = ReactomeJavaConstants.PathwayDiagram;
        checkAttribute = ""; // For progress pane to avoid null 
    }
    
    

    @Override
    protected Set<GKInstance> getAllContainedEntities(GKInstance container) throws Exception {
        return new HashSet<GKInstance>();
    }



    @Override
    protected void checkOutSelectedInstances(JFrame frame) {
        int reply = JOptionPane.showConfirmDialog(frame, 
                                                  "To fix a problem in a pathway diagram, please use the database event view to\n" +
                                                  "check out the pathway represented by the diagram. But you can still check out\n" + 
                                                  "the selected PathwayDiagram instance. Do you want to continue checking out?",
                                                  "Checking Out?",
                                                  JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.NO_OPTION)
            return;
        super.checkOutSelectedInstances(frame);
    }

    @Override
    protected boolean checkInstance(GKInstance instance) throws Exception {
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram))
            throw new IllegalArgumentException(instance + " is not a PathwayDiagram instance!");
        Set<Long> dbIds = extractReactomeIds(instance);
        if (dbIds == null || dbIds.size() == 0)
            return true;
        if (dataSource instanceof MySQLAdaptor) {
            MySQLAdaptor dba = (MySQLAdaptor) dataSource;
            boolean rtn = dba.exist(new ArrayList<Long>(dbIds));
            return rtn;
        }
        else {
            Collection<?> instances = dataSource.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject,
                                                                          ReactomeJavaConstants.DB_ID,
                                                                          "=", 
                                                                          dbIds);
            return instances.size() == dbIds.size();
        }
    }

    protected Set<Long> extractReactomeIds(GKInstance instance) throws InvalidAttributeException, Exception {
        String xml = (String) instance.getAttributeValue(ReactomeJavaConstants.storedATXML);
        if (xml == null || xml.length() == 0)
            return null;
        // Don't try to load the diagram, which is very slow!!!
        //RenderablePathway diagram = reader.openDiagram(instance);
//        List<Renderable> components = diagram.getComponents();
//        if (components == null || components.size() == 0)
//            return true;  // Just ignore it
        Set<Long> dbIds = new HashSet<Long>();
        // Use this REGEXP to find all reactomeIds
        String regexp = "reactomeId=\"((\\d)+)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(xml);
        int start = 0;
        while (matcher.find(start)) {
            start = matcher.end();
            String dbId = matcher.group(1); 
            dbIds.add(new Long(dbId));
        }
        return dbIds;
    }

    @Override
    protected Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances) {
        return filterInstancesForProject(instances, 
                                         ReactomeJavaConstants.PathwayDiagram);
    }

    @Override
    protected ResultTableModel getResultTableModel() throws Exception {
        return new PathwayDiagramTableModel();
    }

    @Override
    protected void loadAttributes(Collection<GKInstance> instances) throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) dataSource;
        // Need to load all complexes in case some complexes are used by complexes for checking
        progressPane.setText("Load PathwayDiagram attributes ...");
        loadAttributes(ReactomeJavaConstants.PathwayDiagram, 
                       ReactomeJavaConstants.storedATXML, 
                       dba);
    }
    
    private class PathwayDiagramTableModel extends ResultTableModel {
        private String[] values = null;
        
        public PathwayDiagramTableModel() {
            setColNames(new String[]{"Pathway", "DB_IDs without Instances"});
        }

        @Override
        public void setInstance(GKInstance instance) {
            if (values == null)
                values = new String[2];
            try {
                GKInstance pathway = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.representedPathway);
                values[0] = pathway.getDisplayName() + " [" + pathway.getDBID() + "]";
                // Get DB_IDs that are not in the database
                Set<Long> reactomeIds = extractReactomeIds(instance);
                Collection<?> instances = dataSource.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject,
                                                                     ReactomeJavaConstants.DB_ID,
                                                                     "=", 
                                                                     reactomeIds);
                Set<Long> found = new HashSet<Long>();
                for (Object obj : instances) {
                    found.add(((GKInstance)obj).getDBID());
                }
                reactomeIds.removeAll(found);
                values[1] = StringUtils.join(", ", new ArrayList<Long>(reactomeIds));
                fireTableStructureChanged();
            }
            catch(Exception e) {
                System.err.println("PathwayDiagramTableModel.setInstance(): " + e);
                e.printStackTrace();
            }
        }

        public int getRowCount() {
            return values == null ? 0 : 1;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (values == null)
                return null;
            return values[columnIndex];
        }
        
    }
}
