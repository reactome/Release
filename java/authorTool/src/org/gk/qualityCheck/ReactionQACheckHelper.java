/*
 * Created on Aug 18, 2009
 *
 */
package org.gk.qualityCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.ProgressPane;

/**
 * This helper class is used to help QA for reactions. Using this class to avoid
 * a complicated class hierarchical structure.
 * @author wgm
 */
@SuppressWarnings("unchecked")
public class ReactionQACheckHelper {
    private Collection regulations;
    
    public ReactionQACheckHelper() {
    }
    
    public void setCachedRegulations(Collection c) {
        this.regulations = c;
    }
    
    public Collection getCachedRegulations() {
        return this.regulations;
    }

    public void loadRegulations(MySQLAdaptor dba,
                                ProgressPane progressPane) throws Exception {
        String[] attNames = new String[] {
                ReactomeJavaConstants.regulator,
                ReactomeJavaConstants.regulatedEntity
        };
        regulations = dba.fetchInstancesByClass(ReactomeJavaConstants.Regulation);
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
        loadAttributes(regulations,
                       cls,
                       attNames,
                       dba, 
                       progressPane);
    }
    
    protected void loadAttributes(Collection instances,
                                  SchemaClass cls,
                                  String[] attNames,
                                  MySQLAdaptor dba,
                                  ProgressPane progressPane) throws Exception {
        for (int i = 0; i < attNames.length; i++) {
            SchemaAttribute att = cls.getAttribute(attNames[i]);
            progressPane.setText("Load " + cls.getName() + " " + attNames[i] + "...");
            dba.loadInstanceAttributeValues(instances, att);
            if (progressPane.isCancelled())
                return;
        }
    }
    protected Set<GKInstance> getAllContainedEntities(GKInstance reaction) throws Exception {
        Map<String, List<GKInstance>> roleToEntities = getReactionParticipants(reaction);
        Set<GKInstance> entities = new HashSet<GKInstance>();
        for (String role : roleToEntities.keySet()) {
            List<GKInstance> list = roleToEntities.get(role);
            entities.addAll(list);
        }
        return entities;
    }
    
    /**
     * This helper method is used to grep reaction participants from a passed reaction.
     * @param reaction
     * @return a map with role names (input, output, catalyst, regulator) as keys, and participants are values
     * @throws Exception
     */
    public Map<String, List<GKInstance>> getReactionParticipants(GKInstance reaction) throws Exception {
        Map<String, List<GKInstance>> roleToEntities = new HashMap<String, List<GKInstance>>();
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        if (inputs != null && inputs.size() > 0)
            roleToEntities.put("input",
                               inputs);
        List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs != null && outputs.size() > 0)
            roleToEntities.put("output",
                               outputs);
        // Need to check cataylystActivities
        List cases = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cases != null && cases.size() > 0) {
            List<GKInstance> catalysts = new ArrayList<GKInstance>();
            for (Iterator it = cases.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (catalyst != null && catalyst.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                    catalysts.add(catalyst);
            }
            if (catalysts.size() > 0)
                roleToEntities.put("catalyst",
                                   catalysts);
        }
        // Need to check regulation. To avoid database access, we do a reverse lookup here.
        List<GKInstance> regulators = new ArrayList<GKInstance>();
        if (regulations == null) { // Avoid repeating database query.
            PersistenceAdaptor dataSource = reaction.getDbAdaptor();
            SchemaClass cls = dataSource.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
            regulations = dataSource.fetchInstancesByClass(cls);
        }
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            GKInstance regulation = (GKInstance) it.next();
            GKInstance regulated = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
            if (regulated == reaction) {
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                    regulators.add(regulator);
            }
        }
        if (regulators.size() >0)
            roleToEntities.put("regulator",
                               regulators);
        return roleToEntities;
    }
    
    public ResultTableModel getResultTableModel(String[] colNames,
                                                String checkAttribute) {
        ReactionResultTableModel tableModel = new ReactionResultTableModel();
        tableModel.setColNames(colNames);
        tableModel.setCheckAttribute(checkAttribute);
        return tableModel;
    }
    
    class ReactionResultTableModel extends ResultTableModel {
        private String checkAttribute;
        
        class RowData {
            String role;
            GKInstance entity; 
            String values;
        }
        
        private Map<String, List<GKInstance>> roleToEntities;
        private List<RowData> rowData;
        
        public ReactionResultTableModel() {
            rowData = new ArrayList();
        }
        
        public void setCheckAttribute(String attribute) {
            this.checkAttribute = attribute;
        }

        public void setInstance(GKInstance instance) {
            try {
                roleToEntities = getReactionParticipants(instance);
                rowData.clear();
                for (Iterator it = roleToEntities.keySet().iterator(); it.hasNext();) {
                    String role = (String) it.next();
                    List entities = (List) roleToEntities.get(role);
                    InstanceUtilities.sortInstances(entities);
                    for (Iterator it1 = entities.iterator(); it1.hasNext();) {
                        GKInstance entity = (GKInstance) it1.next();
                        RowData data = new RowData();
                        data.entity = entity;
                        data.role = role;
                        if (entity.getSchemClass().isValidAttribute(checkAttribute)) {
                            List values = entity.getAttributeValuesList(checkAttribute);
                            data.values = convertInstancesToString(values);
                        }
                        else
                            data.values = "";
                        rowData.add(data);
                    }
                }
                fireTableStructureChanged();
            }
            catch(Exception e) {
                System.err.println("ReactionResultTableModel.setInstance(): " + e);
                e.printStackTrace();
            }
        }
        
        private String convertInstancesToString(List values) {
            StringBuilder builder = new StringBuilder();
            if (values != null && values.size() > 0) {
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance value = (GKInstance) it.next();
                    builder.append(value.getDisplayName());
                    if (it.hasNext())
                        builder.append(", ");
                }
            }
            return builder.toString();
        }

        public int getRowCount() {
            return rowData.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            RowData data = (RowData) rowData.get(rowIndex);
            switch (columnIndex) {
                case 0 :
                    return data.role;
                case 1 :
                    return data.entity.getDisplayName() + " [" + data.entity.getDBID() + "]";
                case 2 :
                    return data.values;
            }
            return null;
        }
        
    }
    
}
