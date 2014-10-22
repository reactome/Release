/*
 * Created on Feb 28, 2012
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is simply used to make JAXB happy.
 * @author gwu
 */
@XmlRootElement(name="InstanceList")
public class ListOfShellInstances {
    
    private List<ShellInstance> instances;
    
    public ListOfShellInstances() {
    }
    
    public List<ShellInstance> getInstance() {
        return instances;
    }
    
    public void setInstance(List<ShellInstance> list) {
        this.instances = list;
    }
    
    public void addInstance(ShellInstance instance) {
        if (instances == null)
            instances = new ArrayList<ShellInstance>();
        instances.add(instance);
    }
    
    public void addInstance(int index, ShellInstance instance) {
        if (instances == null)
            instances = new ArrayList<ShellInstance>();
        instances.add(index, instance);
    }
    
    public ListOfShellInstances copy() {
        ListOfShellInstances copy = new ListOfShellInstances();
        if (instances != null)
            copy.instances = new ArrayList<ShellInstance>(instances);
        return copy;
    }
    
}
