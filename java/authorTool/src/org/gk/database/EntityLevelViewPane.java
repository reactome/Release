/*
 * Created on Nov 10, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.gk.graphEditor.EntityLevelView;
import org.gk.model.GKInstance;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.RenderUtility;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchemaClass;

/**
 * A customized JPanel for holding a hierarchical view of events.
 * @author wugm
 */
public class EntityLevelViewPane extends JPanel {
	private JPanel pathwaySelector;
	private EntityLevelView entityLevelView;
	// Mark the change
	private boolean isDirty = true;

	public EntityLevelViewPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		pathwaySelector = new JPanel();
//		initPathwaySelector();
		
		entityLevelView = new EntityLevelView();
		entityLevelView.setMinimumSize(new Dimension(20, 100));
		
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pathwaySelector, entityLevelView);
		add(jsp, BorderLayout.CENTER);
		jsp.setDividerLocation(250);
		jsp.setResizeWeight(0.2);
		jsp.setOneTouchExpandable(true);
	}
	
	private void initPathwaySelector() {
		Collection instances = deriveInstancesFromSchemaClass("Reaction");
		if (instances!=null) {
			JList pathwayList = new JList(instances.toArray());
			pathwayList.requestFocus();
			
			JScrollPane pathwayListScrollPane = new JScrollPane(pathwayList);
			
			pathwaySelector.add(pathwayListScrollPane);
		} 
	}
	
	public void setEditable(boolean editable) {
		entityLevelView.setEditable(editable);
	}
	
	public boolean isEditable() {
		return entityLevelView.isEditable();
	}
	
	public EntityLevelView getEntityLevelView() {
		return entityLevelView;
	}
	
	public void setInstance(GKInstance instance) {
		try {
			RenderablePathway pathway = (RenderablePathway) RenderUtility.convertToNode(instance, false);
			entityLevelView.setPathway(pathway, false);
		} catch(Exception e) {
			System.err.println("EntityLevelViewPane.setInstance: could not add pathway to entity level view");
			e.printStackTrace();
		}
	}
	
	private List deriveInstancesFromSchemaClass(String className) {
		if (className==null)
			return null;
		
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		
		if (adaptor==null) {
			System.err.println("deriveInstancesFromSchemaClass: WARNING - XMLFileAdaptor was null, giving up!");
			return null;
		}
		
		ArrayList instances = new ArrayList();
		Collection schemaClassInstances;
		try {
			// Add the instances found for the current schema
			// class, if any.
			schemaClassInstances = adaptor.fetchInstancesByClass(className);
			if (schemaClassInstances!=null)
				addAllNonRedundant(instances, schemaClassInstances);
		} catch (Exception e) {
			System.err.println("AttributePane.deriveInstancesFromSchemaClass(): problem while trying to get some instances for the schema class "+className);
			e.printStackTrace();
		}
		
		// Add the instances found for each subclass.  If there
		// are no further subclasses, then the recursion is
		// terminated.
		Object glopsi = adaptor.fetchSchemaClass(className);
		Collection subclasses = ((GKSchemaClass)(adaptor.fetchSchemaClass(className))).getSubClasses();
		schemaClassInstances = null;
		if (subclasses!=null) {
			GKSchemaClass attributeSchemaClass;
			for (Iterator it = subclasses.iterator(); it.hasNext();) {
				attributeSchemaClass = (GKSchemaClass)it.next();
				schemaClassInstances = deriveInstancesFromSchemaClass(attributeSchemaClass.getName());
				if (schemaClassInstances!=null)
					addAllNonRedundant(instances, schemaClassInstances);
			}
		}
		
		return instances;
	}
	
	private void addAllNonRedundant(ArrayList baseList, Collection stuffToAdd) {
		Object addMe;
		for (Iterator it = stuffToAdd.iterator(); it.hasNext();) {
			addMe = it.next();
			if (addMe!=null && !baseList.contains(addMe))
				baseList.add(addMe);
		}
	}
	
	public java.util.List getSelection() {
		return entityLevelView.getSelection();
	}
	
	public boolean isDirty() {
		return this.isDirty;
	}
	
	public void setUpLocalView() {
		initPathwaySelector();
	}
}
