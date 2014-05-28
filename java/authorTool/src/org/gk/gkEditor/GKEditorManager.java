/*
 * Created on Jul 2, 2003
 */
package org.gk.gkEditor;

import java.io.File;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.gk.database.GKDBBrowserPopupManager;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.persistence.GKBReader;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.Project;
import org.gk.render.Node;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableRegistry;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.GKFileFilter;
import org.gk.util.XMLFileFilter;

/**
 * Use this class to do some house-keeping work.
 * @author wgm
 */
public class GKEditorManager {
	// The opened Project
	private Project openedProject;
    private GKEditorFrame editorFrame;

	public GKEditorManager(GKEditorFrame frame) {
		this.editorFrame = frame;
	}

	/**
	 * Close the current opened project.
	 * @return true for close it successfully while false for not closing.
	 */
	public boolean close() {
		if (openedProject == null)
			return true;
		if (openedProject.isDirty()) {
			int reply =
				JOptionPane.showConfirmDialog(
					editorFrame,
					"Do you want to save the changes you made to " + openedProject.getName(),
					"Save Changes?",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (reply == JOptionPane.YES_OPTION)
				save();
			else if (reply == JOptionPane.CANCEL_OPTION)
				return false;
		}
		close(openedProject);
		return true;
	}

	private void close(Project project) {
		Renderable process = project.getProcess();
        //TODO: Need to close open project in the both tree
		//editorFrame.getProcessPane().remove(process);
		editorFrame.setTitle(GKEditorFrame.GK_EDITOR_NAME);
		openedProject = null;
		editorFrame.actionCollection.updateActions();
        //editorFrame.updateMessage();
		GKDBBrowserPopupManager.getManager().setOpenedProject(null);
        // Refresh the registry
        RenderableRegistry.getRegistry().clear();
	}

	public void save() {
		if (openedProject == null || !openedProject.isDirty())
			return;
		String sourceName = openedProject.getSourceName();
		if (sourceName == null) {
			saveAs();
		}
		else {
			try {
                GKBWriter writer = new GKBWriter();
                writer.save(openedProject, sourceName);
			}
			catch(Exception e) {
				System.err.println("GKEditorManager.save(): " + e);
				e.printStackTrace();
				JOptionPane.showMessageDialog(editorFrame,
				                              "Cannot save the project: " + e.getMessage(),
				                              "Error in Saving",
				                              JOptionPane.ERROR_MESSAGE);
			}
			editorFrame.enableSaveAction(false);
		}
	}

	public void saveAs() {
		if (openedProject == null)
			return;
		JFileChooser fileChooser = new JFileChooser();
		String currentDir = editorFrame.getProperties().getProperty("currentDir");
		if (currentDir != null)
			fileChooser.setCurrentDirectory(new File(currentDir));
		GKFileFilter gkFilter = new GKFileFilter();
		fileChooser.addChoosableFileFilter(gkFilter);
		fileChooser.addChoosableFileFilter(new XMLFileFilter());
		fileChooser.setFileFilter(gkFilter);
		fileChooser.setDialogTitle("Save A Project...");
		File selectedFile = GKApplicationUtilities.chooseSaveFile(fileChooser, 
		                                                          editorFrame);
		if (selectedFile == null)
			return;
		try {
            GKBWriter writer = new GKBWriter();
            writer.save(openedProject, selectedFile.toString());
		}
		catch(Exception e) {
			System.err.println("GKEditorManager.saveAs(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(editorFrame,
			                              "Cannot save the project: " + e.getMessage(),
			                              "Error in Saving",
			                              JOptionPane.ERROR_MESSAGE);
		}
		editorFrame.addRecentProject(openedProject);
		setFrameTitle(openedProject);
		editorFrame.enableSaveAction(false);
		editorFrame.getProperties().setProperty("currentDir", selectedFile.getParent());
	}

	public boolean checkOpenedProject() {
		// Call close first
		if (openedProject != null) {
			if (openedProject.isDirty()) {
				int reply =
					JOptionPane.showConfirmDialog(
						editorFrame,
						"Do you want to save the changes you made to "
							+ openedProject.getName(),
						"Save Changes?",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (reply == JOptionPane.YES_OPTION)
					save();
				else if (reply == JOptionPane.CANCEL_OPTION)
					return false;
			}
			close(openedProject);
		}
		return true;
	}

	public void open() {
		JFileChooser fileChooser = new JFileChooser();
		String currentDir = editorFrame.getProperties().getProperty("currentDir");
		if (currentDir != null)
			fileChooser.setCurrentDirectory(new File(currentDir));
		GKFileFilter fileFilter = new GKFileFilter();
		fileChooser.addChoosableFileFilter(fileFilter);
		fileChooser.addChoosableFileFilter(new XMLFileFilter());
		fileChooser.setFileFilter(fileFilter);
		fileChooser.setDialogTitle("Open A Project...");
		int reply = fileChooser.showOpenDialog(editorFrame);
		if (reply == JFileChooser.APPROVE_OPTION) {
			if (!checkOpenedProject())
				return;
			File selectedFile = fileChooser.getSelectedFile();
			if(open(selectedFile.toString())) {
				editorFrame.getProperties().setProperty("currentDir", selectedFile.getParent());
			}
		}
	}

	public boolean open(String sourceName) {
	    GKBReader persistence = new GKBReader();
	    try {
	        Project project = persistence.open(sourceName);
	        open(project);
	        editorFrame.addRecentProject(project);
	        return true;
	    }
	    catch(Exception e) {
	        JOptionPane.showMessageDialog(editorFrame,
	                                      "Cannot Open the specified file:\n" + e,
	                                      "Error in Opening Project",
	                                      JOptionPane.ERROR_MESSAGE);
	        e.printStackTrace();
	        return false;
	    }
	}

	public void open(Project project) {
		this.openedProject = project;
		editorFrame.toolPane.openProject(project);
        setFrameTitle(project);
		editorFrame.updateActions();
		editorFrame.enableSaveAction(false);
	}
	
	private void setFrameTitle(Project project) {
		RenderablePathway pathway = project.getProcess();
		// Set title
		StringBuffer title = new StringBuffer(pathway.getDisplayName());
		if (project.getSourceName() != null)
			title.append(" [" + editorFrame.generateMenuLabel(project.getSourceName()) + "]");
		title.append(" - " + GKEditorFrame.GK_EDITOR_NAME);
		editorFrame.setTitle(title.toString());
	}

	public void createNewProject() {
        if (!checkOpenedProject())
            return;
		// A Project is a Pathway without parent pathways specified.
		RenderablePathway pathway = new RenderablePathway();
		pathway.setDisplayName("New Project");
        Project project = new Project();
		project.setProcess(pathway);
		open(project);
		// Don't try to save a new project. Nothing there.
        // editorFrame.enableSaveAction(true);
		// Have to register this new pathway
		RenderableRegistry.getRegistry().add(pathway);
	}
	
	public void stopNameEditing() {
		GraphEditorPane graphPane = editorFrame.getDisplayedGraphPane();
		if (graphPane != null)
			graphPane.stopEditing();
	}
	
	/**
	 * Update a display name for a Renderable object.
	 * @param renderable
	 */
	public void updateName(Renderable renderable) {
		//editorFrame.getProcessPane().refresh(renderable);
	}
    
    public void updateIsChanged(Renderable r) {
        //editorFrame.getProcessPane().refresh(r);
    }

	public Project getOpenedProject() {
		return this.openedProject;
	}
	
	public void validateNodeWidth() {
		if (openedProject == null)
			return;
		// Need to invalidate all node bounds
		java.util.List nodes = RenderUtility.getAllDescendents(openedProject.getProcess());
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof Node) {
				Node node = (Node) obj;
				node.invalidateBounds();
				node.invalidateConnectWidgets();
			}
		}
		GraphEditorPane graphPane = editorFrame.getDisplayedGraphPane();
		if (graphPane != null)
			graphPane.repaint();
	}
}
