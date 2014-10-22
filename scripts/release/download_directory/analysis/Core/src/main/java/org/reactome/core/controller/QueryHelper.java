/*
 * Created on Nov 10, 2005
 *
 */
package org.reactome.core.controller;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.reactome.core.model.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * This class is used to handle some complicated query for CaBioDomainService class.
 *
 * @author guanming
 */
@SuppressWarnings("unchecked")
public class QueryHelper {

    private MySQLAdaptor dba;

    @Autowired
    private ReactomeToRESTfulAPIConverter converter;

    public QueryHelper() {
    }

    public void setDba(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    public void setConverter(ReactomeToRESTfulAPIConverter converter) {
        this.converter = converter;
    }
    
    /**
     * Query Event instances by names and species. This method uses a "LIKE" search.
     * @param pattern
     * @return
     * @throws Exception
     */
    public List<GKInstance> queryByNameAndSpecies(String clsName,
                                                  String pattern,
                                                  String speciesName) throws Exception {
        SchemaClass cls = dba.getSchema().getClassByName(clsName);
        // If name is a valid attribute, use name. Otherwise, use _displayName
        String att = null;
        if (cls.isValidAttribute(ReactomeJavaConstants.name))
            att = ReactomeJavaConstants.name;
        else
            att = ReactomeJavaConstants._displayName;
        Collection<GKInstance> instances = dba.fetchInstanceByAttribute(clsName, 
                                                                        att, 
                                                                        "like", 
                                                                        "%" + pattern + "%");
        if (instances == null)
            instances = new ArrayList<GKInstance>();
        // Do a filter if speciesName is provided and species is a valid name
        if (speciesName != null && speciesName.length() > 0 && cls.isValidAttribute(ReactomeJavaConstants.species)) {
            boolean remove = true;
            for (Iterator<GKInstance> it = instances.iterator(); it.hasNext();) {
                GKInstance inst = it.next();
                remove = true;
                List<GKInstance> species = inst.getAttributeValuesList(ReactomeJavaConstants.species);
                if (species != null && species.size() > 0) {
                    for (GKInstance tmp : species) {
                        if (tmp.getDisplayName().equalsIgnoreCase(speciesName)) {
                            remove = false;
                            break;
                        }
                    }
                }
                if (remove)
                    it.remove();
            }
        }
        return (instances instanceof List) ? (List<GKInstance>)instances : new ArrayList<GKInstance>(instances);
    }

    public List<GKInstance> query(String className,
                                  String caBioPropName,
                                  String caBioPropValue) throws Exception {
        Set<GKInstance> set = new HashSet<GKInstance>();
        // Handle special cases first
        
        SchemaClass cls = dba.getSchema().getClassByName(className);
        if (!cls.isValidAttribute(caBioPropName)) {
//            System.out.print(caBioPropName +"invalid");
            return new ArrayList<GKInstance>();
        }
        if (!cls.getAttribute(caBioPropName).isInstanceTypeAttribute()) {
//            System.out.println(caBioPropName +"notinstance");
            Collection instances = dba.fetchInstanceByAttribute(className, 
                                                                caBioPropName, 
                                                                "=", 
                                                                caBioPropValue);
            if (instances == null || instances.size() == 0)
                return new ArrayList<GKInstance>();
            set.addAll(instances);
        } 
        else {
//            System.out.println(caBioPropName +"isinstance");
            // Have to construct GKInstance from value for query
            //Set<GKInstance> criteria = buildQueryCriteria(caBioPropValue);
            GKInstance instance =  dba.fetchInstance(Long.parseLong(caBioPropValue));
            Collection instances = dba.fetchInstanceByAttribute(className,
                                                                caBioPropName,
                                                                "=",
                                                                instance);
            if (instances != null)
                set.addAll(instances);
        }
        List<GKInstance> gkInstances = new ArrayList<GKInstance>(set);
        InstanceUtilities.sortInstances(gkInstances);
        return gkInstances;
    }

    /**
     * Query ancestors for an Event.
     * @return
     * @throws Exception
     */
    public List<List<Event>> queryAncestors(GKInstance event) throws Exception {
        List<List<Event>> ancestors = new ArrayList<List<Event>>();
        List<Event> branch = new ArrayList<Event>();
        ancestors.add(branch);
        queryAncestors(ancestors, branch, event);
        return ancestors;
    }
    
    /**
     * A resurve way to get ancestors into a List<ShellInstance>. An event can have more than
     * one parent.
     * @param ancestors
     * @param branch
     * @param event
     * @throws Exception
     */
    private void queryAncestors(List<List<Event>> ancestors, 
                                List<Event> branch,
                                GKInstance event) throws Exception {
        Event convertedEvent = convertToEvent(event);
        if (convertedEvent != null)
            branch.add(0, convertedEvent);
        Collection<?> parents = event.getReferers(ReactomeJavaConstants.hasEvent);
        if (parents == null || parents.size() == 0)
            return;
        if (parents.size() == 1) {
            GKInstance parent = (GKInstance) parents.iterator().next();
            queryAncestors(ancestors, branch, parent);
        }
        else {
            // Need to make a copy first to avoid any overriding
            List<Event> copy = new ArrayList<Event>(branch);
            int index = 0;
            for (Iterator<?> it = parents.iterator(); it.hasNext();) {
                GKInstance parent = (GKInstance) it.next();
                if (index == 0) {
                    queryAncestors(ancestors, branch, parent);
                }
                else {
                    List<Event> newBranch = new ArrayList<Event>(copy);
                    ancestors.add(newBranch);
                    queryAncestors(ancestors, newBranch, parent);
                }
                index ++;
            }
        }
    }
    
    private Event convertToEvent(GKInstance instance) throws Exception {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
            Event event = (Event) converter.createObject(instance);
            return event;
        }
        return null;
    }
    
//    @Test
//    public void testQueryAncestors() throws Exception {
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_current_ver42",
//                                            "root",
//                                            "macmysql01");
//        setDba(dba);
//        ReactomeToRESTfulAPIConverter converter = new ReactomeToRESTfulAPIConverter();
//        converter.setMapper(new ReactomeToRESTfulAPIMapper());
//        converter.setPostMapperFactory(new ReactomeModelPostMapperFactory());
//        setConverter(converter);
//        // The following event should have four branches
//        GKInstance event = dba.fetchInstance(69019L);
//        List<List<Event>> ancestors = queryAncestors(event);
//        System.out.println("Total branches: " + ancestors.size());
//        for (List<Event> branch : ancestors) {
//            for (Event inst : branch)
//                System.out.print(inst.getDisplayName() + "||");
//            System.out.println();
//        }
//    }
//
//    @Test
//    public void testQueryByNameAndSpecies() throws Exception {
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_current_ver42",
//                                            "root",
//                                            "macmysql01");
//        setDba(dba);
//        Collection<GKInstance> c = queryByNameAndSpecies(ReactomeJavaConstants.Event,
//                                                         "Apoptosis",
//                                                         "Homo sapiens");
//        System.out.println("Total instances: " + c.size());
//        for (GKInstance inst : c)
//            System.out.println(inst);
//    }

}
