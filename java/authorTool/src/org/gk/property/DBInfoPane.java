/*
 * Created on Jul 1, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.ReferenceDatabase;
import org.gk.render.Renderable;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableProtein;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPanel is used for editing database related info.
 * @author wgm
 */
public class DBInfoPane extends RenderablePropertyPane implements ActionListener {
    private final String UNIPROT_SEARCH_URL = "http://www.pir.uniprot.org/search/textSearch.shtml";
    private final String UNIPROT_VIEW_URL = "http://www.pir.uniprot.org/cgi-bin/upEntry?id=";
    private final String UNIPROT_BROWSER_NAME = "UniProt";
    private final String CHEBI_SEARCH_URL = "http://www.ebi.ac.uk/chebi/advancedSearchForward.do";
    // Example: http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI%3A15422 (for ATP)
    private final String CHEBI_VIEW_URL = "http://www.ebi.ac.uk/chebi/searchId.do?chebiId=";
    private final String CHEBI_BROWSER_NAME = "ChEBI";
	// GUIs
	private JTextField acTF;
    private JRadioButton uniProtBtn;
    private JRadioButton chEBIBtn;
    private JRadioButton otherBtn;
    private JLabel otherLabel;
    private JTextField otherTF;
    private JButton searchBtn;
    private JButton viewBtn;
    // a note
    private JLabel noteLabel;
	
	public DBInfoPane() {
		init();
	}

	private void init() {
		setLayout(new GridBagLayout());
		// Set up an internal db info pane
		GridBagConstraints constraints = new GridBagConstraints();
		JLabel refDBLabel = new JLabel("Database:");
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(4, 4, 4, 4);
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
		add(refDBLabel, constraints);
		// Add these database choices
        uniProtBtn = new JRadioButton("UniProt");
        chEBIBtn = new JRadioButton("ChEBI");
        otherBtn = new JRadioButton("Other");
        ButtonGroup group = new ButtonGroup();
        group.add(uniProtBtn);
        group.add(chEBIBtn);
        group.add(otherBtn);
        uniProtBtn.setSelected(true); // Default
        // add these buttons
        constraints.gridx = 1;
		add(uniProtBtn, constraints);
        constraints.gridx = 2;
        add(chEBIBtn, constraints);
        constraints.gridx = 3;
        add(otherBtn, constraints);
        // Add a line for other: this should be disable as default
        otherLabel = new JLabel("Name for other database:");
        otherTF = new JTextField();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        add(otherLabel, constraints);
        constraints.gridx = 2;
        constraints.gridwidth = 2;
        add(otherTF, constraints);
        // This line is for accession: add a JPanel to make layout looks nice
        JLabel acNo = new JLabel("Accession:");
		constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        add(acNo, constraints);
		acTF = new JTextField();
		//acTF.setColumns(10);
        constraints.gridx = 1;
        constraints.gridwidth = 1;
		add(acTF, constraints);
		// Add two buttons
        searchBtn = new JButton("Search",
                                AuthorToolAppletUtilities.createImageIcon("Search16.gif"));
        constraints.gridx = 2;
        constraints.gridwidth = 1;
        add(searchBtn, constraints);
        viewBtn = new JButton("View",
                              AuthorToolAppletUtilities.createImageIcon("Abstract.gif"));
        constraints.gridx = 3;
        add(viewBtn, constraints);
        // A checkbox to enable multiple database entries
        noteLabel = new JLabel("<html><u>Note</u>: Use \",\" to separate multiple accessions, e.g., Q01094, Q14209.</html>");
        Font font = noteLabel.getFont();
        noteLabel.setFont(font.deriveFont(font.getSize2D() - 1.0f));
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 4;
        add(noteLabel, constraints);
        setOtherDbInfoEnabled(false);
        installListeners();
	}
    
    private void installListeners() {
        uniProtBtn.addActionListener(this);
        chEBIBtn.addActionListener(this);
        otherBtn.addActionListener(this);
        searchBtn.addActionListener(this);
        viewBtn.addActionListener(this);
        DocumentListener docListerner = new DocumentListener(){
            public void changedUpdate(DocumentEvent e) {
                // Do nothing: just format changes
            }
            public void insertUpdate(DocumentEvent e) {
                doDBInfoChange();
            }
            public void removeUpdate(DocumentEvent e) {
                doDBInfoChange();
            }
        };
        acTF.getDocument().addDocumentListener(docListerner);
        otherTF.getDocument().addDocumentListener(docListerner);
    }
    
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == uniProtBtn ||
            src == chEBIBtn) {
            setOtherDbInfoEnabled(false);
            doDBInfoChange();
        }
        else if (src == otherBtn) {
            // Give an information to the user
            JOptionPane.showMessageDialog(this, 
                                          "Please enter the database name in the text box\n" +
                                          "\"Name for other database\".",
                                          "Reminding",
                                          JOptionPane.INFORMATION_MESSAGE);
            setOtherDbInfoEnabled(true);
            doDBInfoChange();
        }
        else if (src == searchBtn)
            search();
        else if (src == viewBtn) 
            view();
    }
    
    private void search() {
        if (otherBtn.isSelected()) {
            JOptionPane.showMessageDialog(this,
                                          "Please go to the web site for your database, search\n" +
                                          "your entity, and enter database identifier here.",
                                          "Search Other Database",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final DBInfoSearchPane searchPane = new DBInfoSearchPane();
        searchPane.setDbName(getDbName());
        searchPane.setSpeciesName((String)r.getAttributeValue(RenderablePropertyNames.TAXON));
        final JDialog dialog = GKApplicationUtilities.createDialog(this, 
                                                                  "Database Access Search");
        dialog.getContentPane().add(searchPane, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(searchPane.getOKBtn());
        searchPane.addCancelAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        searchPane.addOKAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String access = searchPane.getAccess();
                if (access != null)
                    acTF.setText(access);
                else
                    acTF.setText("");
                dialog.dispose();
            }
        });
        searchPane.addBrowseAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { 
                dialog.dispose();
                browse();
            }
        });
        if (!searchPane.initDatabaseConnection()) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot initialize a database connection.",
                                          "Error in Search Database",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Focus the name text field when the dialog is opened
        dialog.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                searchPane.focusTextField();
            }
        });
        searchPane.smartSearch(r);
        dialog.setModal(true);
        GKApplicationUtilities.center(dialog);
        dialog.setVisible(true);
    }
    
    private void browse() {
        String dbName = getDbName();
        String url = null;
        String browserName = null;
        if (dbName.equals("UniProt")) {
            url = UNIPROT_SEARCH_URL;
            browserName = UNIPROT_BROWSER_NAME;
        }
        else if (dbName.equals("ChEBI")) {
            url = CHEBI_SEARCH_URL;
            browserName = CHEBI_BROWSER_NAME;
        }
        if (url == null)
            return;
        if (AuthorToolAppletUtilities.isInApplet) {
            AuthorToolAppletUtilities.displayURL(url, browserName); 
        }
        else {
            try {
                BrowserLauncher.displayURL(url, this);
            }
            catch(IOException e) {
                JOptionPane.showMessageDialog(this, 
                                              "Cannot launch the web browser: " + e.getMessage(),
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();                              
            }
        }
    }
    
    private void view() {
        if (otherBtn.isSelected()) {
            JOptionPane.showMessageDialog(this,
                                          "Please go to the web site for your database and view\nyour entity there.",
                                          "View Other Database",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!uniProtBtn.isSelected() && !chEBIBtn.isSelected()) {
            JOptionPane.showMessageDialog(this,
                                          "No database is selected. Please choose one.",
                                          "Error in View",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        String ac = acTF.getText().trim();
        if (ac.length() == 0) {
            JOptionPane.showMessageDialog(this, 
                                          "Please enter database accession or id to view",
                                          "Empty Accession",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Get the selected text
        String query = acTF.getSelectedText();
        if (query == null || query.trim().length() == 0)
            query = acTF.getText();
        String[] tokens = query.split(",");
        query = tokens[0].trim();
        // in case it is a variant identifier from UniProt IDs
        int index = query.indexOf("-");
        if (index > 0)
            query = query.substring(0, index);
        String url = null;
        if (uniProtBtn.isSelected()) {
            url = UNIPROT_VIEW_URL + query;
            if (AuthorToolAppletUtilities.isInApplet) 
                AuthorToolAppletUtilities.displayURL(url, UNIPROT_BROWSER_NAME);
            else {
                try {
                    BrowserLauncher.displayURL(url, this);
                }
                catch(IOException e) {
                    JOptionPane.showMessageDialog(this,
                                            "Cannot view the record in UniProt: " + e.getMessage(),
                                            "Error in View",
                                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        else if (chEBIBtn.isSelected()) {
            ac = query;
            if (ac.indexOf("ChEBI") < 0) {
                ac = "CHEBI%3A" + ac; // ChEBI is used directly
            }
            else
                ac = ac.replaceAll(":", "%3A"); // Replace ":" by "%3A"
            url = CHEBI_VIEW_URL + ac;
            if (AuthorToolAppletUtilities.isInApplet)
                AuthorToolAppletUtilities.displayURL(url, CHEBI_BROWSER_NAME);
            else {
                try {
                    BrowserLauncher.displayURL(url, this);
                }
                catch(IOException e) {
                    JOptionPane.showMessageDialog(this,
                                            "Cannot view the record in ChEBI: " + e.getMessage(),
                                            "Error in View",
                                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void setOtherDbInfoEnabled(boolean enabled) {
        otherLabel.setEnabled(enabled);
        otherTF.setEnabled(enabled);
    }
	
	private void doDBInfoChange() {
        if (duringSetting)
            return;
		// For reference db
		DatabaseIdentifier oldValue = (DatabaseIdentifier) r.getAttributeValue("databaseIdentifier");
		DatabaseIdentifier newValue = new DatabaseIdentifier();
		String database = getDbName();
		newValue.setDbName(database);
		newValue.setAccessNo(getAccessNo());
		if (oldValue != null)
			newValue.setDB_ID(oldValue.getDB_ID());
		if (!newValue.equals(oldValue)) {
            r.setAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER, 
                                newValue);
			fireRenderablePropertyChange(r, 
                                         RenderablePropertyNames.DATABASE_IDENTIFIER,
		                                 oldValue, 
                                         newValue);
        }
	}
	
	public void setRenderable(Renderable renderable) {
		super.setRenderable(renderable);
        duringSetting = true;
        if (renderable != null) {
            // Set the ReferenceDatabase
            DatabaseIdentifier dbIdentifier = (DatabaseIdentifier) renderable.getAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER);
            if (dbIdentifier != null && dbIdentifier.getDbName() != null) {
                String dbName = dbIdentifier.getDbName();
                if (dbName.equals("UniProt")) {
                    uniProtBtn.setSelected(true);
                    setOtherDbInfoEnabled(false);
                    otherTF.setText("");
                }
                else if (dbName.equals("ChEBI")) {
                    chEBIBtn.setSelected(true);
                    setOtherDbInfoEnabled(false);
                    otherTF.setText("");
                }
                else if (dbName != null) {
                    otherBtn.setSelected(true);
                    setOtherDbInfoEnabled(true);
                    otherTF.setText(dbName);
                }
                String ac = dbIdentifier.getAccessNo();
                if (ac == null)
                    ac = "";
                acTF.setText(ac);
            }
            else {
                // Default database
                if (renderable instanceof RenderableProtein) {
                    uniProtBtn.setSelected(true);
                    setOtherDbInfoEnabled(false);
                }
                else if (renderable instanceof RenderableChemical) {
                    chEBIBtn.setSelected(true);
                    setOtherDbInfoEnabled(false);
                }
                else {
                    otherBtn.setSelected(true);
                    setOtherDbInfoEnabled(true);
                }
                otherTF.setText("");
                String ac = "";
                if (dbIdentifier != null && dbIdentifier.getAccessNo() != null)
                    ac = dbIdentifier.getAccessNo();
                acTF.setText(ac);
            }
        }
        duringSetting = false;
	}
	
	public ReferenceDatabase getSelectedDB() {
        String dbName = getDbName();
        if (dbName != null && dbName.length() > 0) {
            ReferenceDatabase ref = new ReferenceDatabase();
            ref.setName(dbName);
            return ref;
        }
        return null;
    }
    
    private String getDbName() {
        String dbName = null;
        if (uniProtBtn.isSelected())
            dbName = uniProtBtn.getText();
        else if (chEBIBtn.isSelected())
            dbName = chEBIBtn.getText();
        else if (otherBtn.isSelected()) {
            dbName = otherTF.getText().trim();
        }
        return dbName;
    }
	
	public String getAccessNo() {
		String accessNo = acTF.getText().trim();
		if (accessNo.length() == 0)
			return null;
		return accessNo;
	}
}
