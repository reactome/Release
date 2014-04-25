/*
 * Created on Sep 22, 2005
 *
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.Modification;
import org.gk.model.Reference;
import org.gk.model.ReferenceDatabase;
import org.gk.model.Summation;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.BrowserLauncher;

public abstract class HTMLPropertyPane extends JEditorPane {
    
    public HTMLPropertyPane() {
        init();
    }
    
    public JDialog generateDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog);
        initDialog(dialog);
        return dialog;
    }
    
    public JDialog generateDialog(JFrame parentFrame) {
        JDialog dialog = new JDialog(parentFrame);
        initDialog(dialog);
        return dialog;
    }
    
    protected abstract String getDisplayName();
    
    private void initDialog(final JDialog dialog) {
        dialog.getContentPane().add(new JScrollPane(this), BorderLayout.CENTER);
        dialog.setTitle("Properties for " + getDisplayName());
        // Add a close button
        JPanel southPane = new JPanel();
        southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        closeBtn.setMnemonic('C');
        southPane.add(closeBtn);
        closeBtn.setDefaultCapable(true);
        dialog.getRootPane().setDefaultButton(closeBtn);
        dialog.getContentPane().add(southPane, BorderLayout.SOUTH);    
    }
    
    private void init() {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setEditable(false);
        setContentType("text/html");
        addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String desc = e.getDescription();
                    processLink(desc);
                }
            }
        });     
    }
    
    private void processLink(String desc) {
        if (desc.startsWith("PUBMED")) {
            int index = desc.indexOf(":");
            String pmid = desc.substring(index + 1);
            String url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?" + 
                         "cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=" + pmid;
            if (AuthorToolAppletUtilities.isInApplet) {
                AuthorToolAppletUtilities.displayURL(url, 
                                                     AuthorToolAppletUtilities.PUBMED_BROWSER_NAME);
            }
            else {
                try {
                    BrowserLauncher.displayURL(url, this);
                }
                catch(IOException e) {
                    System.err.println("EntityPropertyDialog.processLink(): " + e);
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                                                  "Exception in displaying pubmed abstract: " + e.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        else if (desc.startsWith("Reactome")) {
            int index = desc.indexOf(":");
            String id = desc.substring(index + 1);
            String url = AuthorToolAppletUtilities.REACTOME_INSTANCE_URL + id;
            if (AuthorToolAppletUtilities.isInApplet) {
                AuthorToolAppletUtilities.displayURL(url,
                                                     AuthorToolAppletUtilities.REACTOME_BROWSER_NAME);
            }
            else {
                try {
                    BrowserLauncher.displayURL(url, this);
                }
                catch(IOException e) {
                    System.err.println("EntityPropertyDialog.processLink(): " + e);
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                                                  "Exception in displaying a Reactome link: " + e.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        else if (desc.startsWith("DBLINK")) {
            StringTokenizer tokenizer = new StringTokenizer(desc, ":");
            tokenizer.nextToken(); // Escape the first DBLINK
            String dbName = tokenizer.nextToken();
            String accessNo = tokenizer.nextToken();
            // Search the database
            java.util.List databases = PropertyManager.getManager().getReferenceDBs();
            ReferenceDatabase refDB = null;
            for (Iterator it = databases.iterator(); it.hasNext();) {
                ReferenceDatabase tmp = (ReferenceDatabase) it.next();
                if (tmp.getName().equals(dbName)) {
                    refDB = tmp;
                    break;
                }
            } 
            if (refDB == null) {
                JOptionPane.showMessageDialog(this,
                                              "The database used is not defined in the author tool.",
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            else {
                String accessURL = refDB.getAccessURL();
                String url = accessURL.replaceAll("###ID###", accessNo);
                if (AuthorToolAppletUtilities.isInApplet) {
                    AuthorToolAppletUtilities.displayURL(url, 
                                                         refDB.getName().toLowerCase());
                }
                else {
                    try {
                        BrowserLauncher.displayURL(url, this);
                    }
                    catch(IOException e1) {
                        System.err.println("EntityListPane.processLink() 1: " + e1);
                        e1.printStackTrace();
                    }
                }
            }
        }
        else if (desc.startsWith("Instance")) {
            int index = desc.indexOf(":");
            processInstanceLink(desc);
        }           
    }
    
    protected abstract void processInstanceLink(String desc);
    protected abstract String getTaxon();
    protected abstract String getLocalization();
    protected abstract Long getDBID();
    protected abstract Summation getSummation();
    protected abstract DatabaseIdentifier getDatabaseIdentifier();
    protected abstract String getType();
    protected abstract List getReferences();
    
    public void clean() {
        setText("");
    }
    
    protected abstract List getAliases();
    
    protected String generateHtml() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html><body>");
        // Common information for all types of Renderable. Use a table
        buffer.append("<table width=\"100%\" border=\"1\">");
        fillTableRow("Name", getDisplayName(), buffer);
        java.util.List aliases = (java.util.List) getAliases();;
        generateHtmlForStringList(buffer, aliases, "Alternative Names");       
        String taxon = getTaxon();
        if (taxon != null) {
            buffer.append("<tr>");
            fillTableRowHeader("Taxon", buffer, 1);
            fillTableRowValue(taxon, buffer);
            buffer.append("</tr>");
        }   
        String localization = getLocalization();;
        if (localization != null) {
            buffer.append("<tr>");
            fillTableRowHeader("Localization", buffer, 1);
            fillTableRowValue(localization, buffer);
            buffer.append("</tr>");
        }
        Long dbID = getDBID();
        if (dbID != null) {
            buffer.append("<tr>");
            fillTableRowHeader("Reactome_ID", buffer, 1);
             String reactomeUrl = "Reactome:" + dbID.toString();
            fillTableRowValue("<a href=\"" + reactomeUrl + "\">" + dbID.toString() + "</a>", buffer);
            buffer.append("</tr>");
        }
        // Description
        Summation summation = getSummation();
        if (summation != null && summation.getText() != null &&
            summation.getText().length() > 0) {
            buffer.append("<tr>");
            fillTableRowHeader("Description", buffer, 1);
            fillTableRowValue(summation.getText(), buffer);
            buffer.append("</tr>");
        }
        DatabaseIdentifier di = getDatabaseIdentifier();
        if (di != null) {
            buffer.append("<tr>");
            fillTableRowHeader("DatabaseIdentifier", buffer, 1);
            String dbLink = di.getDbName() + ":" + di.getAccessNo();
            fillTableRowValue("<a href=\"DBLINK:" + dbLink + "\">" + dbLink + "</a>", buffer);
            buffer.append("</tr>");
        }
        String type = getType();
        if (type.equals("Pathway")) {
            generateHtmlForPathway(buffer);
        }
        else if (type.equals("Reaction")) {
            generateHtmlForReaction(buffer);
        }
        else if (type.equals("Complex")) {
            generateHtmlForComplex(buffer);
        }
        else if (type.equals("Protein")) {
            generateHtmlForProtein(buffer);
        }
        else if (type.equals("Compound")) {
            generateHtmlForCompound(buffer);
        }
        else { // It is OK to call the method if r is a small molecule. There should be nothing there.
            generateHtmlForEntity(buffer);
        }
        // References
        generateHTMLForReferences(buffer);
        buffer.append("</body></html>");
        //System.out.println(buffer.toString());
        return buffer.toString();
    }

    protected void generateHtmlForStringList(StringBuffer buffer, 
                                           java.util.List values,
                                           String title) {
        if (values != null && values.size() > 0) {
            buffer.append("<tr>");
            fillTableRowHeader(title, buffer, values.size());
            for (int i = 0; i < values.size(); i++) {
                if (i > 0)
                    buffer.append("<tr>");
                fillTableRowValue(values.get(i).toString(), buffer);
                buffer.append("</tr>");
            }
        }
    }

    private void generateHTMLForReferences(StringBuffer buffer) {
        java.util.List references = getReferences();
        if (references != null && references.size() > 0) {
            buffer.append("<tr>");
            fillTableRowHeader("Reference", buffer, references.size());
            for (int i = 0; i < references.size(); i++) {
                if (i > 0) 
                    buffer.append("<tr>");
                Reference ref = (Reference) references.get(i);
                StringBuffer buffer1 = new StringBuffer();
                buffer1.append("<a href=\"PUBMED:" + ref.getPmid() + "\">");
                if (ref.getAuthor() != null)
                    buffer1.append(ref.getAuthor() + ". ");
                if (ref.getTitle() != null)
                    buffer1.append(ref.getTitle() + " ");
                if (ref.getJournal() != null)
                    buffer1.append(ref.getJournal() + " ");
                if (ref.getVolume() != null)
                    buffer1.append(ref.getVolume() + ": ");
                if (ref.getPage() != null)
                    buffer1.append(ref.getPage() + " ");
                if (ref.getYear() > 0)
                    buffer1.append("(" + ref.getYear() + ")");
                buffer1.append("</a>");
                fillTableRowValue(buffer1.toString(), buffer);
                buffer.append("</tr>");
            }
        }
    }
    
    protected void fillTableRow(String header,
                                String value,
                                StringBuffer buffer) {
        buffer.append("<tr>");
        fillTableRowHeader(header, buffer, 1);
        fillTableRowValue(value, buffer);
        buffer.append("</tr>");
    }
    
    private void fillTableRowHeader(String header, StringBuffer buffer, int rowSpan) {
        buffer.append("<th align=\"left\" bgcolor=\"#C0C0C0\" rowspan=\"" + rowSpan + "\">" + header + "</th>");
    }
    
    private void fillTableRowValue(String value, StringBuffer buffer) {
        buffer.append("<td>" + value + "</td>");
    }
    
    protected abstract List getModifications();
    
    private void generateHtmlForEntity(StringBuffer buffer) {
        // Get modifications
        java.util.List modifications = getModifications();
        if (modifications != null && modifications.size() > 0) {
            buffer.append("<tr>");
            fillTableRowHeader("Modification", buffer, modifications.size());
            for (int i = 0; i < modifications.size(); i++) {
                if (i > 0) 
                    buffer.append("<tr>");
                Modification modification = (Modification)modifications.get(i);
                StringBuffer buffer1 = new StringBuffer();
                if (modification.getModification() != null) {
                    buffer1.append(modification.getModification());
                    buffer1.append(" ");
                }
                if (modification.getResidue() != null) {
                    buffer1.append("on ");
                    buffer1.append(modification.getResidue());
                    buffer1.append(" ");
                }
                if (modification.getCoordinate() > 0) {
                    buffer1.append("at ");
                    buffer1.append(modification.getCoordinate());
                }
                fillTableRowValue(buffer1.toString(), buffer);
                buffer.append("</tr>");
            }
        }
    }
    
    protected abstract void generateHtmlForComplex(StringBuffer buffer);
    
    protected void generateHTMLForList(List renderables, 
                                     Map stoichiometries, 
                                     StringBuffer buffer, 
                                     String rowTitle) {
        if (renderables == null || renderables.size() == 0)
            return;      
        int index = 0;
        buffer.append("<tr>");
        fillTableRowHeader(rowTitle, buffer, renderables.size());
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            if (index > 0)
                buffer.append("<tr>");
            Object subunit = it.next();
            String displayName = getDisplayName(subunit);
            Integer stoi = (Integer) stoichiometries.get(subunit);
            String value = null;
            if (stoi != null && stoi.intValue() > 1)
                value = stoi + " x <a href=\"Instance:" + displayName + "\">"
                + displayName + "</a>";
            else
                value = "<a href=\"Instance:" + displayName + "\">"
                + displayName + "</a>";
            fillTableRowValue(value, buffer);
            buffer.append("</tr>");
            index++;
        }
    }
    
    protected abstract String getDisplayName(Object component);
    
    protected abstract void generateHtmlForPathway(StringBuffer buffer);
    
    protected abstract void generateHtmlForReaction(StringBuffer buffer); 
    
    protected abstract void generateHtmlForProtein(StringBuffer buffer);
    
    protected abstract void generateHtmlForCompound(StringBuffer buffer);
    
}
