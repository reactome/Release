/*
 * Created on Aug 11, 2003
 */
package org.gk.persistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;

/**
 * An object to organize a set of objects to be persisted.
 * @author wgm
 */
public class Project implements Serializable {
	private RenderablePathway process;
	private Map removedTasks; // Key: Renderable, Value: a list of GKTask.label
	private Map definedTasks; // Key: Renderable, Value: a list of GKTask
	private String sourceName;
	private Map listedEntities; // Key: "Small Molecules", "Proteins", "Complexes"
	                            // Value: A list of Renderable objects
	private boolean isDirty;

	public Project() {
	}
	
	public Project(RenderablePathway process) {
		this.process = process;
	}
	
	public RenderablePathway getProcess() {
		return process;
	}

	public void setProcess(RenderablePathway renderable) {
		process = renderable;
	}
	
	public void setSourceName(String name) {
		this.sourceName = name;
	}
	
	public String getSourceName() {
		return this.sourceName;
	}
	
	public void setIsDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}
	
	public boolean isDirty() {
		return this.isDirty;
	}
	
	public String getName() {
		return process.getDisplayName();
	}
	
	/**
	 * Return the user defined tasks. Keys are Renderable objects while
	 * values are lists of GKTasks.
	 * @return an empty HashMap if nothing is in.
	 */
	public Map getDefinedTasks() {
		if (definedTasks == null)
			return new HashMap();
		return definedTasks;
	}

	/**
	 * Return the user removed tasks. Keys are Renderable objects while
	 * values are lists of GKTask labels.
	 * @return an empty HashMap if nothing is in.
	 */
	public Map getRemovedTasks() {
		if (removedTasks == null)
			return new HashMap();
		return removedTasks;
	}

	public void setDefinedTasks(Map map) {
		definedTasks = map;
	}

	public void setRemovedTasks(Map map) {
		removedTasks = map;
	}
	
	public void setListedEntities(Map map) {
		listedEntities = map;
	}
	
	public Map getListedEntities() {
		return listedEntities;
	}
    
    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeObject(process);
        stream.writeUTF(sourceName);
    }
    
    private void readObject(java.io.ObjectInputStream stream) 
            throws IOException, ClassNotFoundException {
        // Want to figure out container properties for Renderable. container
        // is transient in order to support DnD.
        process = (RenderablePathway) stream.readObject();
        sourceName = (String) stream.readUTF();
        setContainer(process);
    }
    
    private void setContainer(Renderable container) {
        List children = container.getComponents();
        if (children == null || children.size() == 0)
            return;
        // Do a width first
        for (Iterator it = children.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            r.setContainer(container);
        }
        for (Iterator it = children.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            setContainer(r);
        }
    }
}
