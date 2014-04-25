/*
 * GKEditorFrame.java
 *
 * Created on June 16, 2003, 1:34 PM
 */

package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ReactionNodeGraphEditor;
import org.gk.osxAdapter.OSXApplication;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.property.PropertyManager;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.util.AboutGKPane;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.HierarchicalLayout;
import org.gk.util.RecentProjectHelper;

/**
 * The application frame. This is the entry point for the whole application.
 * @author  wgm
 */
public class GKEditorFrame extends JFrame implements OSXApplication {
	// Fixed name
	public static final String GK_EDITOR_NAME = "Reactome Author Tool";
	public static final String VERSION = "4.0";
	public static final int BUILD_NUMBER = 30;
    // A list of all actions
    AuthorToolActionCollection actionCollection;
    // for text and reference annotation
    private GKEditorManager editorManager;
    private JMenu fileMenu;
    //private JMenuItem closeItem;
    // For property setting
    private Properties properties;
    // A flag to check if this application is standalone to used with
    // other application
    boolean isForCuratorTool = false;
    // For recent project menus
    private RecentProjectHelper recentPrjHelper;
    private final int totalProjectNumber = 4;
    // Wrapped AuthorToolApplet
    AuthorToolPanel toolPane;
    
    /** Creates a new instance of GKEditorFrame */
    public GKEditorFrame() {
    	init();
    }
    
    public GKEditorFrame(boolean isForCuratorTool) {
    	this.isForCuratorTool = isForCuratorTool;
    	init();
    }
    
    private void init() {
        loadProperties();
    	JWindow window = null;
    	// If this GKEditorFrame is used in the Curator Tool or
    	// deployed in an Applet, don't show the splash window.
		if (!isForCuratorTool && 
		    !AuthorToolAppletUtilities.isInApplet) {
			if (isMac()) {
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("apple.awt.showGrowBox", "true");
			}
			// Display a launch window
			AboutGKPane aboutPane = new AboutGKPane(true, false);
			aboutPane.setApplicationTitle(GK_EDITOR_NAME);
			aboutPane.setVersion(VERSION);
			aboutPane.setBuildNumber(BUILD_NUMBER);
			aboutPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.lightGray, Color.black));
			window = new JWindow();
			window.getContentPane().add(aboutPane, BorderLayout.CENTER);
			window.setSize(378, 320);
			GKApplicationUtilities.center(window);
			window.setVisible(true);
		}
        // Start initialization.
        setTitle(GK_EDITOR_NAME);
        // Set Image Icon
        ImageIcon icon = AuthorToolAppletUtilities.createImageIcon("R-small.png");
        setIconImage(icon.getImage());
        toolPane = new AuthorToolPanel(this);
        AuthorToolAppletUtilities.isInApplet = false;
        actionCollection = toolPane.getActionCollection();
        editorManager = new GKEditorManager(this);
        setJMenuBar(createMenuBar());
        // Have to take content out. Otherwise, Menus cannot be displayed correctly under Windows
        getContentPane().add(toolPane, BorderLayout.CENTER);
        // Open last opened project.
    	if (!isForCuratorTool) {
    		openLastProject();
    		try {
    			Thread.sleep(500);
    		}
    		catch(InterruptedException e) {}
    		if (window != null) {
    		    window.setVisible(false);
    		    window.dispose();
    		}
    	}
    	// Set the main window size based on properties
    	initMainFrame();
    	String toolbarText = properties.getProperty("toolbarText");
    	if (toolbarText == null || toolbarText.equals("true"))
    	    toolPane.setTextInToolbarVisible(true);
    	else
    	    toolPane.setTextInToolbarVisible(false);
    }
    
	protected void validateAddingNodesAction(java.util.List importNodes, Renderable parent) {
		for (Iterator it = importNodes.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
		}
		// Update display
		GraphEditorPane graphPane = getDisplayedGraphPane();
		if (graphPane instanceof PathwayEditor)
			graphPane.repaint(graphPane.getVisibleRect());
		else if (graphPane instanceof ReactionNodeGraphEditor) {
			//((ReactionEditor)graphPane).getEntitiesPane().refresh();
			graphPane.repaint(graphPane.getVisibleRect());
		}
		enableSaveAction(true);
	}
    
    private void initMainFrame() {
    	String boundsStr = properties.getProperty("windowBounds");
    	if (boundsStr != null && boundsStr.length() > 0) {
    		StringTokenizer tokenizer = new StringTokenizer(boundsStr);
    		try {
    			int x = Integer.parseInt(tokenizer.nextToken());
    			int y = Integer.parseInt(tokenizer.nextToken());
    			int w = Integer.parseInt(tokenizer.nextToken());
    			int h = Integer.parseInt(tokenizer.nextToken());
    			setLocation(x, y);
    			setSize(w, h);
    		}
    		catch(NumberFormatException e) {
    			boundsStr = null;
    		}
    	}
    	// In case there is something is wrong or nothing is saved.
		if (boundsStr == null || boundsStr.length() == 0) {
			int w = 950;
			int h = 750;
			setSize(w, h);
			// Center the edtiroFrame
			GKApplicationUtilities.center(this);
		}
        String jspDividerPosition = properties.getProperty("jsp");
        if (jspDividerPosition != null) {
            toolPane.setJScrollPaneDividerPosition(Integer.parseInt(jspDividerPosition));
        }
        String leftJsp = properties.getProperty("leftJsp");
        if (leftJsp != null) {
            int pos = Integer.parseInt(leftJsp);
            toolPane.getGraphicView().setLeftScrollPaneDividerPos(pos);
        }
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
//        addComponentListener(new ComponentAdapter() {
//            public void componentResized(ComponentEvent e) {
//                System.out.println("Size: " + getSize());
//            }
//        });
    }
    
    private Properties loadProperties() {
        properties = new Properties();
        try {
            File file = GKApplicationUtilities.getPropertyFile("gkEditor.prop");
            if (file.exists()) {
                FileInputStream input = new FileInputStream(file);
                properties.load(input);
            }
        }
        catch(IOException e) {
            System.err.println("GKEditorFrame.loadProperties(): " + e);
            e.printStackTrace();
        }
        // Initialize based on properties
        if (!isForCuratorTool) // Don't set the LF. It should be determined by the curator tool.
            GKApplicationUtilities.setLookAndFeel(properties.getProperty("lookAndFeel"));
        // Set node width
        String nodeWidth = properties.getProperty("nodeWidth");
        if (nodeWidth != null && nodeWidth.length() > 0) {
            int w = Integer.parseInt(nodeWidth);
            Node.setNodeWidth(w);
        }
        String dist = properties.getProperty("layoutNodeDist");
        if (dist != null && dist.length() > 0) {
            HierarchicalLayout.setNodeDistance(Integer.parseInt(dist));
        }
        dist = properties.getProperty("layoutLayerDist");
        if (dist != null && dist.length() > 0)
            HierarchicalLayout.setLayerDistance(Integer.parseInt(dist)); 
        // Have to descypt the pwd
        String dbPwd = properties.getProperty("dbPwd");
        if (dbPwd != null) {
            dbPwd = GKApplicationUtilities.decrypt(dbPwd);
            properties.setProperty("dbPwd", dbPwd);
        }
        if (PersistenceManager.getManager().getDBConnectInfo() == null)
            PersistenceManager.getManager().setDBConnectInfo(properties);
        PropertyManager.getManager().setSystemProperties(properties);
        return properties;
    }
    
    private void openLastProject() {
    	String prjSrcName = properties.getProperty("lastOpenedProject");
    	if (prjSrcName == null || prjSrcName.length() == 0) {
            // create a new project
            editorManager.createNewProject();
    	}
        else if (!editorManager.open(prjSrcName)) {
            // If the last project cannot be opened, create a new project.
            // The last object may be deleted. If no new project is created,
            // errors will occur.
            editorManager.createNewProject(); 
        }
    }
    
    private JMenuBar createMenuBar() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        boolean isMac = isMac();
        if (isMac)
            GKApplicationUtilities.macOSXRegistration(this);
        JMenuBar menuBar= new JMenuBar();
        fileMenu = new JMenu("File");
        JMenuItem newProjectItem = fileMenu.add(actionCollection.getNewProjectAction());
        newProjectItem.setAccelerator(KeyStroke.getKeyStroke('N', shortcutMask));
        JMenuItem openPathwayItem = fileMenu.add(actionCollection.getOpenProjectAction());
        openPathwayItem.setAccelerator(KeyStroke.getKeyStroke('O', shortcutMask));
        fileMenu.addSeparator();
        JMenuItem saveProjectItem = fileMenu.add(actionCollection.getSaveProjectAction());
        saveProjectItem.setAccelerator(KeyStroke.getKeyStroke('S', shortcutMask));
        JMenuItem saveAsItem = fileMenu.add(actionCollection.getSaveAsAction());
        fileMenu.addSeparator();
        // Add export diagrams
        JMenuItem exportDiagramItem = fileMenu.add(actionCollection.getExportDiagramAction());
        fileMenu.add(exportDiagramItem);
        JMenuItem dumpCoordinatesItem = fileMenu.add(actionCollection.getSaveCoordinatesToDbAction());
        fileMenu.add(dumpCoordinatesItem);
        recentPrjHelper = new RecentProjectHelper();
        recentPrjHelper.setTotalProjectNumber(totalProjectNumber);
        recentPrjHelper.setTopAnchorItem(dumpCoordinatesItem);
        recentPrjHelper.setFileMenu(fileMenu);
        recentPrjHelper.loadProjects(properties);
        ActionListener recentPrjAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JMenuItem item = (JMenuItem) e.getSource();
                String text = item.getText();
                int index = text.indexOf(" ");
                int i = Integer.parseInt(text.substring(0, index));
                String projectSourceName = recentPrjHelper.getRecentProject(i - 1);
                if(!editorManager.close())
                    return;
                if (!editorManager.open(projectSourceName)) {
                    recentPrjHelper.removeProject(i - 1);
                }
                else if (i > 1) { // Switch the opened project to the top of the list
                    recentPrjHelper.switchToTop(i - 1);
                }
            }
        };
        recentPrjHelper.setProjectActionListener(recentPrjAction);
        List prjMenus = recentPrjHelper.getProjectMenus();
        if (prjMenus.size() > 0) {
            fileMenu.addSeparator();
            for (int i = 0; i < prjMenus.size(); i++) {
                JMenuItem item = (JMenuItem) prjMenus.get(i);
                fileMenu.add(item);
            }
        }
        if (!isMac) {
            fileMenu.addSeparator();
            JMenuItem exitMenu = new JMenuItem("Exit");
            if (isForCuratorTool)
                exitMenu.setText("Close");
            exitMenu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    exit();
                }
            });
            fileMenu.add(exitMenu);
            recentPrjHelper.setBottomAnchorItem(exitMenu);
        }
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        JMenuItem cutItem = editMenu.add(actionCollection.getCutAction());
        cutItem.setAccelerator(KeyStroke.getKeyStroke('X', shortcutMask));
        JMenuItem copyItem = editMenu.add(actionCollection.getCopyAction());
        copyItem.setAccelerator(KeyStroke.getKeyStroke('C', shortcutMask));
        JMenuItem pasteItem = editMenu.add(actionCollection.getPasteAsAliasAction());
        pasteItem.setAccelerator(KeyStroke.getKeyStroke('V', shortcutMask));
        JMenuItem item = editMenu.add(actionCollection.getCloneAction());
        item.setAccelerator(KeyStroke.getKeyStroke('V', shortcutMask | KeyEvent.SHIFT_MASK));
        JMenuItem deleteItem = editMenu.add(actionCollection.getDeleteAction());
        if (isMac)
            deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        else
            deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        // Add undo/redo support
        editMenu.addSeparator();
        JMenuItem undo = editMenu.add(actionCollection.getUndoAction());
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask));
        JMenuItem redo = editMenu.add(actionCollection.getRedoAction());
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask));
        editMenu.addSeparator();
        JMenuItem selectAll = editMenu.add(actionCollection.getSelectAllAction());
        selectAll.setAccelerator(KeyStroke.getKeyStroke('A', shortcutMask));
        editMenu.addSeparator();
        // Disable this: as of Dec 22, 2006. Delete in text editing conflicts with this function.
        // This is more like a bug in MacOS 1.4.2. MacOS 1.5.0 doesn't have this problem.
//        if (isMac)
//            deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
//        else // Delete key cannot work under MacOS X
//            deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        JMenuItem findItem = editMenu.add(actionCollection.getSearchTreeAction());
        findItem.setAccelerator(KeyStroke.getKeyStroke('F', shortcutMask));
        menuBar.add(editMenu);
//        // View Menu For future implementation
//        JMenu viewMenu = new JMenu("View");
//        viewMenu.setMnemonic('V');
//        JMenuItem backItem = viewMenu.add(actionCollection.getBackAction());
//        backItem.setAccelerator(KeyStroke.getKeyStroke('[', shortcutMask));
//        JMenuItem forwardItem = viewMenu.add(actionCollection.getForwardAction());
//        forwardItem.setAccelerator(KeyStroke.getKeyStroke(']', shortcutMask));
//        menuBar.add(viewMenu);
        
        // Tool menu
        JMenu toolMenu = new JMenu("Tools");
        // Text is changed
        //TODO: have to test if text for this menuitem is changed under Windows, and Linux.
        JMenuItem flagChangedItem = toolMenu.add(actionCollection.getFlagChangedAction());
        JMenuItem removeComptFromNameItem = toolMenu.add(actionCollection.getRemoveCompartFromNameAction());
        JMenuItem tightNodeBoundsItem = toolMenu.add(actionCollection.getTightBoundsAction());
        JMenuItem switchViewItem = toolMenu.add(actionCollection.getSwitchViewAction());
        switchViewItem.setAccelerator(KeyStroke.getKeyStroke("F3"));
        JMenuItem layoutItem = toolMenu.add(actionCollection.getLayoutAction());
        layoutItem.setAccelerator(KeyStroke.getKeyStroke("F4"));
        if (!isMac) {
            toolMenu.addSeparator();
            JMenuItem optionsItem = toolMenu.add(actionCollection.getOptionAction());
        }
        menuBar.add(toolMenu);
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = helpMenu.add(actionCollection.getHelpAction());
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        JMenuItem tutorialItem = new JMenuItem("Tutorial");
        tutorialItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String fileName = "tutorial.html";
                try {
                    AuthorToolAppletUtilities.displayHelp(fileName, GKEditorFrame.this);
                }
                catch(Exception e1) {
                    System.err.println("GKEditorFrame.createMenuBar(): " + e1);
                    e1.printStackTrace();
                }
            }
        });
        helpMenu.add(tutorialItem);
        if (!isMac) {
            helpMenu.addSeparator();
            JMenuItem aboutItem = helpMenu.add(actionCollection.getAboutAction());
        }
        menuBar.add(helpMenu);
        if (!isMac) {
            fileMenu.setMnemonic('F');
            editMenu.setMnemonic('E');
            toolMenu.setMnemonic('T');
            helpMenu.setMnemonic('H');
        }
        return menuBar;
    }
    
    protected boolean isMac() {
    	String osName = System.getProperty("os.name").toLowerCase();
    	if (osName.indexOf("mac") > -1)
    		return true;
    	return false;
    }
    
	public GraphEditorPane getDisplayedGraphPane() {
		return toolPane.getDisplayedGraphPane();
	}
	
    public GKEditorManager getEditorManager() {
    	return this.editorManager;
    }
    
    /**
     * Open a Renderable object in a new GraphEditorPane.
     * @param renderable
     */
    public void open(Renderable renderable) {
        toolPane.open(renderable);
    }
	
    protected void addRecentProject(Project project) {
        recentPrjHelper.addRecentProject(project.getSourceName());
    }
    
    protected String generateMenuLabel(String projectName) {
    	int max = 25;
    	if (projectName.length() < max)
    		return projectName;
    	int index = projectName.indexOf(File.separator);
    	String driverLabel = projectName.substring(0, index);
    	String rtn = null;
    	int totalLength = projectName.length();
    	index = totalLength;
    	int prevIndex = index;
    	int c = 0;
    	while (index >= 0) {
    		index = projectName.lastIndexOf(File.separator, index - 1);
			if (totalLength - index > max) {
				String tmp = null;
				if (c == 0) // Use the file name only.
					tmp = projectName.substring(index + 1, index + max) + "...";
				else
					tmp = projectName.substring(prevIndex + 1);
				rtn = driverLabel + File.separator + "..." + File.separator + tmp;
				break;
			}
			prevIndex = index;
			c ++;
		}
    	return rtn;
    }
    
    public void exit() {
        // Save the last opened project info
        String lastOpenedProject = null;
        if (editorManager.getOpenedProject() != null)
            lastOpenedProject = editorManager.getOpenedProject().getSourceName();
        if (lastOpenedProject == null)
            lastOpenedProject = "";
        properties.setProperty("lastOpenedProject", lastOpenedProject);
        if (!editorManager.close()) {
            return;
        }
        // Should empty them first in case anything changed
        recentPrjHelper.storeProjects(properties);
        // Save window size and position
        Rectangle bounds = getBounds();
        properties.setProperty("windowBounds", 
                bounds.x + " " + bounds.y + " " +
                bounds.width + " " + bounds.height);
        properties.setProperty("jsp", toolPane.getJScrollPaneDividerPosition() + "");
        properties.setProperty("leftJsp", toolPane.getGraphicView().getLeftScrollPaneDividerPos() + "");
        // Do a really simple encryption
        String pwd = properties.getProperty("dbPwd");
        if (pwd != null) {
            //properties.setProperty("dbPwd", GKApplicationUtilities.encrypt(pwd));
            properties.remove("dbPwd");
        }
        try {
            File file = GKApplicationUtilities.getPropertyFile("gkEditor.prop");
            if (!file.exists()) {
                boolean canCreate = file.createNewFile();
                if (!canCreate) {
                    System.err.println("GKEditor.exit(): Cannot create a property file.");
                    return;
                }
            }
            FileOutputStream output = new FileOutputStream(file);
            properties.store(output, "GK Editor Properties");
            output.close();
        }
        catch (IOException e) {
            System.err.println("GKEditorFrame.exit() " + e);
            e.printStackTrace();
        }
        if (isForCuratorTool)
            dispose();
        else
            System.exit(0);
    }
    
    public void updateActions() {
        actionCollection.updateActions();
    }
    
    /**
   	 * Call this method if the preferred width of Node is changed to
   	 * re-calculate the bounds of Nodes.
   	 */
    public void validateNodeWidth() {
    	editorManager.validateNodeWidth();
    }
    
    public void enableSaveAction(boolean enabled) {
    	toolPane.enableSaveAction(enabled);
    }
    
    public Properties getProperties() {
    	return this.properties;
    }
    
    // The following three methods are used to hook to MacOS X's application menu.
    public void about() {
    	ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "exit");
    	actionCollection.getAboutAction().actionPerformed(e);
    }
    
    public void preferences() {
    	ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "options");
    	actionCollection.getOptionAction().actionPerformed(e);
    }
    
    public void quit() {
    	exit();
    }
    
    public static void main(String[] args) {
		GKEditorFrame editorFrame = new GKEditorFrame();
		editorFrame.setVisible(true);
	}
}
