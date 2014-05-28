/*
 * Created on Sep 22, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;

import org.gk.database.util.DBTool;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.schema.GKSchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JFrame to browse the GK database.
 * @author wgm
 */
public class GKDatabaseBrowser extends JFrame {
	// Connection to the database
	private MySQLAdaptor dba;
	// GUIs
	private SchemaViewPane schemaView;
	// A flag
	private boolean isDisplayable = true;
	// To control menus
	private boolean useShortMenu = false;
	// To control JPopupMenu
	private int popupType = GKDBBrowserPopupManager.STANDALONE_TYPE; // Default
	// For event view
	private Action dbEventViewAction;
	
	public GKDatabaseBrowser() {
		init();
	}
	
	public GKDatabaseBrowser(String host, String dbName, String user, String pwd) {
	    this(host, dbName, "3306", user, pwd);
	}
	
	public GKDatabaseBrowser(String host, String dbName, String port, String user, String pwd) {
		init();
		MySQLAdaptor dba = (MySQLAdaptor)PersistenceManager.getManager().getMySQLAdaptor(
		                         host, dbName, user, pwd, Integer.parseInt(port));
		if (dba != null) {
			try {
				setMySQLAdaptor(dba);
				if (dbName != null && host != null)
					setTitle(dbName + "@" + host + " - Schema View");
				else
					setTitle("Schema View");
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(this, "Cannot get the schema from the database",
											  "Error in Fetching Schema", JOptionPane.ERROR_MESSAGE);
			}
		}
		else {
			JOptionPane.showMessageDialog(this, "Cannot connect to the database",
			                              "Error in Connection", JOptionPane.ERROR_MESSAGE);
			isDisplayable = false;
		}		
	}
	
	public GKDatabaseBrowser(MySQLAdaptor adaptor) {
		this(adaptor, false, null);
	}

	public GKDatabaseBrowser(MySQLAdaptor adaptor, boolean useShortMenu, Action dbEventViewAction) {
		this.useShortMenu = useShortMenu;
		this.dbEventViewAction = dbEventViewAction;
		init();
		try {
			setMySQLAdaptor(adaptor);
			setTitle(adaptor.getDBName() + "@" + adaptor.getDBHost() + " - Schema View");
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(
				this,
				"Cannot get the schema from the database",
				"Error in Fetching Schema",
				JOptionPane.ERROR_MESSAGE);
			isDisplayable = false;
		}
	}
	
	private void setMySQLAdaptor(MySQLAdaptor adaptor) throws Exception {
		dba = adaptor;
		schemaView.setPersistenceAdaptor(dba);
	}
	
	public MySQLAdaptor getMySQLAdaptor() {
		return this.dba;
	}
	
	private void getClassCounters(GKSchemaClass schemaClass, Map counterMap, MySQLAdaptor dba)
	             throws Exception {
		long counter = dba.getClassInstanceCount(schemaClass);
		counterMap.put(schemaClass, new Long(counter));
		if (schemaClass.getSubClasses() != null && schemaClass.getSubClasses().size() > 0) {
			for (Iterator it = schemaClass.getSubClasses().iterator(); it.hasNext();) {
				GKSchemaClass subClass = (GKSchemaClass) it.next();
				getClassCounters(subClass, counterMap, dba);
			}
		}
	}
	
	public boolean isDisplayable() {
		return isDisplayable;
	}
	
	private void init() {
		initMenus();
		schemaView = new SchemaViewPane();
		schemaView.hideBookmarkView(); // Don't need bookmarks for the time being.
        //TODO: the settings in this view should be the same as in the local project.
        // However, it is not for the time being.
        schemaView.getInstancePane().showViewSettings();
		// For delete an Instance object in the db
		schemaView.getInstancePane().getInstanceList().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doPopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doPopup(e);
			}
		});
		schemaView.getSchemaPane().getClassTree().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					doSchemaPanePopup(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					doSchemaPanePopup(e);
				}
			}
		});
        getContentPane().add(schemaView, BorderLayout.CENTER);
		initTools();
		// Size, position and title
		setSize(900, 800);
		GKApplicationUtilities.center(this);
	}
	
	@Override
	public void setBounds(java.awt.Rectangle rect) {
	    super.setBounds(rect);
	    schemaView.distributeWidthEvenly((int)rect.getWidth());
	}
	
	private void doSchemaPanePopup(MouseEvent e) {
		JPopupMenu popup = GKDBBrowserPopupManager.getSchemaPanePopup(this);
		if (popup != null) {
			JComponent source = (JComponent) e.getSource();
			popup.show(source, e.getX(), e.getY());
		}
	}
	
	private void doPopup(MouseEvent e) {
		JPopupMenu popup = GKDBBrowserPopupManager.getInstanceListPanePopupMenu(popupType, this);
		if (popup != null) {
			JComponent comp = (JComponent) e.getSource();
			popup.show(comp, e.getX(), e.getY());
		}
	}
	
	private void initTools() {
		if (useShortMenu)
			return;
		// Find the resource package path
		URL url = getClass().getResource("util");
		if (url == null) // No folder
		    return;
		// Go to the package and find all classes that implements DBTool
		//String path = "org" + File.separator + "gk" + File.separator + "database" +
		//              "util";
		String packageName = "org.gk.database.util";
		String fileName = url.getFile().replaceAll("%20", " "); // Use space
		File file = new File(fileName);
		if (file.exists()) {
			String[] files = file.list();
			JMenu tools = new JMenu("Tools");
			for (int i = 0; i < files.length; i++) {
				try {
					int index = files[i].lastIndexOf(".class");
					if (index <= 0)
						continue;
					String clsName = files[i].substring(0, index);
					Class clazz = Class.forName(packageName + "." + clsName);
					Class[] interfaces = clazz.getInterfaces();
					for (int j = 0; j < interfaces.length; j++) {
						if(interfaces[j].getName().equals("org.gk.database.util.DBTool")) {
							final DBTool dbTool = (DBTool) clazz.newInstance();
							JMenuItem item = new JMenuItem(dbTool.getTitle());
							tools.add(item);
							item.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									dbTool.doAction();	
								}
							});
							break;
						}
					}
				}
				catch(Exception e) {
				}
			}
			if (tools.getMenuComponentCount() > 0) {
				JMenuBar bar = getJMenuBar();
				tools.setMnemonic('T');
				bar.add(tools, 1);
			}
		}
	}
	
	private void refresh() {
		try {
			dba.refresh();
			schemaView.refresh(dba);
		}
		catch(Exception e) {
			System.err.println("GKDatabaseBrowser.refresh(): " + e);
			e.printStackTrace();
		}
	}
	
	private void initMenus() {
		// Action Listener for all menu items
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				if (command.equals("close")) {
					if (useShortMenu)
						FrameManager.getManager().closeSchemaBrowser();
					else
						FrameManager.getManager().closeBrowser();
				}
				else if (command.equals("closeAll")) {
					FrameManager.getManager().closeAll();
				}
				else if (command.equals("arrangeAll")) {
					FrameManager.getManager().arrangeAll();
				}
//				else if (command.equals("eventView")) {
//					MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(GKDatabaseBrowser.this);
//					FrameManager.getManager().showEventView(adaptor, useShortMenu, popupType);
//				}
				else if (command.equals("refresh")) {
					refresh();
				}
				else if (command.equals("schema")) {
					exportSchema();
				}
			}
		};
		JMenuBar menubar = new JMenuBar();
		JMenu file = new JMenu("File");
		file.setMnemonic('F');
		if (dbEventViewAction != null) {
		    file.add(dbEventViewAction);
		    file.addSeparator();
		}
		JMenuItem refreshItem = new JMenuItem("Refresh");
		refreshItem.setActionCommand("refresh");
		refreshItem.addActionListener(l);
		file.add(refreshItem);
		file.addSeparator();
		JMenuItem exportSchemaItem = new JMenuItem("Export Schema");
		exportSchemaItem.setActionCommand("schema");
		exportSchemaItem.addActionListener(l);
		file.add(exportSchemaItem);
		file.addSeparator();
		JMenuItem close = new JMenuItem("Close");
		close.setActionCommand("close");
		close.addActionListener(l);
		close.setMnemonic('C');
		file.add(close);
		menubar.add(file);
		if (useShortMenu) {
			setJMenuBar(menubar);
			return;
		}
		JMenu windows = FrameManager.getManager().generateWindowMenu();
		menubar.add(windows);
		setJMenuBar(menubar);
	}
	
	private void exportSchema() {
		if (dba == null || dba.getSchema() == null) {
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save Schema...");
		chooser.setSelectedFile(new File("schema"));
		int reply = JFileChooser.CANCEL_OPTION;
		while ((reply = chooser.showSaveDialog(this)) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			if (file != null) {
				if (file.exists()) {
					reply = JOptionPane.showConfirmDialog(this,
					                                      "Do you want to overwrite the selected file?",
					                                      "Overwrite?",
					                                      JOptionPane.YES_NO_CANCEL_OPTION);
					if (reply == JOptionPane.NO_OPTION) {
						continue;
					}
					else if (reply == JOptionPane.CANCEL_OPTION)
						break;
				}
				FileOutputStream fos = null;
				ObjectOutputStream ois = null;
				try {
					fos = new FileOutputStream(file);
					ois = new ObjectOutputStream(fos);
					ois.writeObject(dba.getSchema());
				}
				catch(Exception e) {
					System.err.println("GKDatabaseBrowser.exportSchema(): " + e);
					e.printStackTrace();
				}
				finally {
					try {
						if (fos != null)
							fos.close();
						if (ois != null)
							ois.close();
					}
					catch(IOException e) {}
				}
				break;
			}
		}
	}
	
	public SchemaViewPane getSchemaView() {
		return schemaView;
	}
	
	/**
	 * Set the popup type. The type should be one of STANDALONE_TYPE, CURATOR_TOOL_TYPE
	 * and AUTHOR_TOOL_TYPE in GKDBBrowserPopupManager.
	 * @param type
	 */
	public void setPopupType(int type) {
		this.popupType = type;
	}
	
	public static void main(String[] args) {
		GKApplicationUtilities.setLookAndFeel("aqua");
		Properties prop = new Properties();
		if (args.length == 5) {
			prop.setProperty("dbHost", args[0]);
			prop.setProperty("dbName", args[1]);
			prop.setProperty("dbPort", args[2]);
			prop.setProperty("dbUser", args[3]);
			prop.setProperty("dbPwd", args[4]);
		}
		else {
			File file = new File("resources" + File.separator + "curator.prop");
			if (file.exists()) {
				try {
					FileInputStream fis = new FileInputStream(file);
					prop.load(fis);
					fis.close();
				}
				catch(IOException e) {
					System.err.println("GKDatabaseBrowser.loadProp(): " + e);
					e.printStackTrace();
				}
			}
		}
		DBConnectionPane connectionPane = new DBConnectionPane();
		connectionPane.setValues(prop);
		if (!connectionPane.showInDialog(new JFrame())) {
			System.exit(0);
			return;
		}
		PersistenceManager.getManager().setDBConnectInfo(prop);
		MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(new JFrame());
		if (adaptor == null) {
			JOptionPane.showMessageDialog(null, "Cannot connect to the database.",
			                              "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GKDatabaseBrowser browser = FrameManager.getManager().openBrowser(adaptor, GKDBBrowserPopupManager.STANDALONE_TYPE);
		if (browser != null) {
			browser.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
				    // Call to exit after all AWT events are finished. Otherwise,
				    // there will be an exception thrown from the awt event queue.
				    SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				            System.exit(0);
				        }
				    });
				}
			});
		}
	}
}
