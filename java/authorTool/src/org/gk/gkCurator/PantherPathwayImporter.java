/*
 * Created on Dec 4, 2006
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.database.CheckOutProgressDialog;
import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * This helper class is used to import panther pathways from a converted panther 
 * databases. The panther pathways to be imported should have been converted into
 * the Reactome data model. All instances converted from Panther are labelled with
 * data source, "pantherdb". All shell instances have flagged in the created InstanceEdit
 * with "proxy" InstanceEdit.
 * @author guanming
 *
 */
public class PantherPathwayImporter {
    private Long dataSourceId;
    private GKInstance dataSource;
    // Used to control check out progress
    private CheckOutProgressDialog progressDialog;
    // Want to keep this for the whole session
    private static String dbUser;
    private static String dbPwd;
    
    public PantherPathwayImporter() {
    }
    
    public void setDataSourceID(Long dbId) {
        this.dataSourceId = dbId;
    }
    
    public void doImport(Component parentComp) {
        try {
            MySQLAdaptor dbAdaptor = getDbAdaptorForImportedPathway(parentComp);
            if (dbAdaptor == null)
                return;
            Collection pathways = getPathwayList(dbAdaptor);
            if (pathways == null || pathways.size() == 0) {
                JOptionPane.showMessageDialog(parentComp, 
                                              "Cannot find any pathways converted from Panther in the database.",
                                              "No Panther Pathway Found",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List selectedPathway = selectPathways(pathways, parentComp);
            if (selectedPathway != null) {
                doImport(selectedPathway, parentComp);
            }
            dbAdaptor.getConnection().close();
        }
        catch(Exception e) {
            System.err.println("PantherPathwayImport.doImport(): " + e);
            JOptionPane.showMessageDialog(parentComp,
                                          "Error in importing: " + e.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void doImport(final List pathways, Component comp) {
        JFrame parentFrame = (JFrame) SwingUtilities.getRoot(comp);
        progressDialog = new CheckOutProgressDialog(parentFrame,
                             "Importing External Pathway");
        Thread t = new Thread() {
            public void run() {
                try {
                    yield(); // Make sure progDialog.setVisible is called first.
                    importPathways(pathways);
                }
                catch(Exception e) {
                    System.err.println("PantherPathwayImporter.doImport(): " + e);
                    e.printStackTrace();
                    progressDialog.setIsWrong();
                }
            }
        };
        t.start();
        progressDialog.setVisible(true);
    }
    
    private void importPathways(List pathways) throws Exception {
        // This importing is different from checking out from the database
        // event browser. All instances will be checked in the selected pathway.
        // No shell instances will be used. Instances from panther will be
        // assigned negative DB_IDs, and instances labeled with proxy will be
        // reset as shell instances.
        MySQLAdaptor dba = null;
        Map clsMap = new HashMap();
        for (Iterator it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            if (dba == null)
                dba = (MySQLAdaptor) pathway.getDbAdaptor();
            addToClsMap(pathway, clsMap);
        }
        Map touchedMap = new HashMap();
        progressDialog.setText("Pulling instances out of the database...");
        pullOutInstances(dba, 
                         clsMap, 
                         touchedMap);
        // This is a special case: check regulation
        if (progressDialog.isCancelClicked())
            return;
        progressDialog.setText("Checking regulations...");
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Regulation,
                                                             ReactomeJavaConstants.dataSource,
                                                             "=",
                                                             dataSource);
        if (collection != null) {
            SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
            dba.loadInstanceAttributeValues(collection, cls.getAttributes());
            clsMap.clear();
            for (Iterator it = collection.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                GKInstance regulated = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                if (regulated == null)
                    continue;
                if (touchedMap.containsKey(regulated.getDBID())) {
                    addToClsMap(regulation, clsMap);
                }
            }
            pullOutInstances(dba, clsMap, touchedMap);
        }
        if (progressDialog.isCancelClicked())
            return;
        progressDialog.setText("Pushing instances into the local project...");
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        List newIds = new ArrayList();
        Map instanceMap = generateInstanceMap(touchedMap, newIds);
        // Interaction is not in the database used for curating. Converte
        // Interaction into Reaction by placing interactors as inputs.
        // Instances in this map should never be used again and abosultely
        // not written back to the database. So some attributes can be manipulated
        // here.
        convertInteractionToReaction(dba, instanceMap);
        fileAdaptor.store(instanceMap);
        // reset ids
        long newId = -1;
        for (Iterator it = newIds.iterator(); it.hasNext();) {
            Long dbId = (Long) it.next();
            GKInstance instance = fileAdaptor.fetchInstance(dbId);
            instance.setDBID(new Long(newId));
            fileAdaptor.dbIDUpdated(dbId, instance);
            fileAdaptor.markAsDirty(instance);
            newId --;
        }
        progressDialog.setIsDone();
    }
    
    private void convertInteractionToReaction(MySQLAdaptor dba,
                                              Map instanceMap) throws Exception {
        SchemaClass interactionCls = dba.getSchema().getClassByName(ReactomeJavaConstants.Interaction);
        if (interactionCls == null)
            return;
        List interactions = (List) instanceMap.get(interactionCls);
        if (interactions == null || interactions.size() == 0)
            return; // Don't need
        SchemaClass reactionCls = dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        List reactions = (List) instanceMap.get(reactionCls);
        if (reactions == null) {
            reactions = new ArrayList();
            instanceMap.put(reactionCls, reactions);
        }
        for (Iterator it = interactions.iterator(); it.hasNext();) {
            GKInstance i = (GKInstance) it.next();
            List list = i.getAttributeValuesList(ReactomeJavaConstants.interactor);
            i.setSchemaClass(reactionCls);
            if (list != null)
                i.setAttributeValue(ReactomeJavaConstants.input, list);
            reactions.add(i);
        }
        instanceMap.remove(interactionCls);
    }
    
    private void pullOutInstances(MySQLAdaptor dba, 
                                  Map clsMap, 
                                  Map touchedMap) throws Exception {
        GKSchemaAttribute att = null;
        java.util.List atts = new ArrayList();
        Set next = new HashSet();
        while (true && !progressDialog.isCancelClicked()) {
            for (Iterator it = clsMap.keySet().iterator(); it.hasNext();) {
                GKSchemaClass cls = (GKSchemaClass) it.next();
                Set list = (Set) clsMap.get(cls);
                atts.clear();
                for (Iterator it1 = cls.getAttributes().iterator(); it1.hasNext();) {
                    att = (GKSchemaAttribute) it1.next();
                    if (att.getName().equals("DB_ID") || att.getName().equals("_displayName"))
                        continue; // Escape them
                    atts.add(att);
                }
                dba.loadInstanceAttributeValues(list, atts);
                // register these fetched instances
                for (Iterator it1 = list.iterator(); it1.hasNext();) {
                    GKInstance i = (GKInstance) it1.next();
                    touchedMap.put(i.getDBID(), i);
                    for (Iterator it2 = cls.getAttributes().iterator(); it2.hasNext();) {
                        att = (GKSchemaAttribute) it2.next();
                        if (!att.isInstanceTypeAttribute())
                            continue;
                        List values = i.getAttributeValuesList(att);
                        if (values == null || values.size() == 0)
                            continue;
                        for (Iterator it3 = values.iterator(); it3.hasNext();) {
                            GKInstance i3 = (GKInstance) it3.next();
                            if (touchedMap.containsKey(i3.getDBID()))
                                continue;
                            // Check database
                            GKInstance ds = (GKInstance) i3.getAttributeValue(ReactomeJavaConstants.dataSource);
                            if (ds == dataSource)
                                next.add(i3);
                            else {
                                i3.setIsShell(true);
                                touchedMap.put(i3.getDBID(), i3);
                            }
                        }
                    }
                }
            }
            if (next.size() == 0)
                break;
            clsMap.clear();
            for (Iterator it = next.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                addToClsMap(instance, clsMap);
            }
            next.clear();
        }
    }
    
    private Map generateInstanceMap(Map touchedMap, List newIds) throws Exception {
        Map instanceMap = new HashMap();
        // Make sure instances converted from Panther having negative DB_IDs
        for (Iterator it = touchedMap.keySet().iterator(); it.hasNext();) {
            Long id = (Long) it.next();
            GKInstance instance = (GKInstance) touchedMap.get(id);
            GKInstance ds = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.dataSource);
            if (ds == dataSource) {
                newIds.add(id);
            }
            SchemaClass cls = instance.getSchemClass();
            List list = (List) instanceMap.get(cls);
            if (list == null) {
                list = new ArrayList();
                instanceMap.put(cls, list);
            }
            list.add(instance);
        }
        return instanceMap;
    }
    
    private void addToClsMap(GKInstance instance,
                             Map clsMap) {
        Set set = (Set) clsMap.get(instance.getSchemClass());
        if (set == null) {
            set = new HashSet();
            clsMap.put(instance.getSchemClass(), set);
        }
        set.add(instance);
    }
    
    private List selectPathways(Collection pathways,
                                Component parentComp) {
        Frame parentFrame = (Frame) SwingUtilities.getRoot(parentComp);
        List pathwayList = new ArrayList(pathways);
        PathwaySelectDialog dialog = new PathwaySelectDialog(pathwayList,
                                                             parentFrame);
        dialog.setVisible(true);
        if (dialog.isOKClicked())
            return dialog.getSelectedPathways();
        return null;
    }
    
    private Collection getPathwayList(MySQLAdaptor dbAdaptor) throws Exception {
        // There are no hierarchy in the panther pathways. Just list all pathways
        // Get the data source
        if (dataSourceId == null)
            throw new IllegalStateException("PantherPathwayImporter.getPathwayList(): dataSourceId is not specified.");
        dataSource = dbAdaptor.fetchInstance(dataSourceId);
        Collection pathways = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway, 
                                                                 ReactomeJavaConstants.dataSource, 
                                                                 "=",
                                                                 dataSource);
        return pathways;
    }
    
    private boolean initDataSources(Element elm,
                                    Component parentComp) {
        List dataSourceElmList = elm.getChildren("dataSource");
        if (dataSourceElmList == null || dataSourceElmList.size() == 0)
            throw new IllegalStateException("No data sources specified in the config file, curator.xml!");
        List names = new ArrayList();
        Map nameToId = new HashMap();
        for (Iterator it = dataSourceElmList.iterator(); it.hasNext();) {
            Element tmp = (Element) it.next();
            String dbId = tmp.getAttributeValue("dbId");
            String name = tmp.getAttributeValue("name");
            nameToId.put(name, dbId);
            names.add(name);
        }
        // Use a GUI to let the user to choose one data source
        Frame parentFrame = (Frame) SwingUtilities.getRoot(parentComp);
        ChooseDataSourceDialog dsDialog = new ChooseDataSourceDialog(names,
                                                                     parentFrame);
        dsDialog.setSize(360, 225);
        dsDialog.setModal(true);
        dsDialog.setVisible(true);
        if (dsDialog.isOkClicked()) {
            String name = dsDialog.getSelectedName();
            dataSourceId = new Long(nameToId.get(name).toString());
            return true;
        }
        return false;
    }
    
    private MySQLAdaptor getDbAdaptorForImportedPathway(Component comp) throws Exception {
        InputStream metaConfig = GKApplicationUtilities.getConfig("curator.xml");
        if (metaConfig == null)
            throw new IllegalStateException("PantherPathwayImporter." +
            "getDbAdaptorForImportedPathway: cannot find curator.xml.");
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(metaConfig);
        Element elm = (Element) XPath.selectSingleNode(doc.getRootElement(), "importedPathwayDb");
        if (!initDataSources(elm,
                             comp))
            return null;
        String dbHost = elm.getChildText("dbHost");
        String dbName = elm.getChildText("dbName");
        String dbPort = elm.getChildText("dbPort");
        if (dbPort == null || dbPort.length() == 0)
            dbPort = "3306";
        if (dbUser == null || dbPwd == null) {
            // Try to get these information from the active MySQLAdaptor
            MySQLAdaptor mainDBA = PersistenceManager.getManager().getActiveMySQLAdaptor(comp);
            if (mainDBA == null)
                return null;
            // User input might be only the machine name.
            // However, configured dbHost has the full name.
            if (dbHost.startsWith(mainDBA.getDBHost())) {
                dbUser = mainDBA.getDBUser();
                dbPwd = mainDBA.getDBPwd();
            }
            else {
                JOptionPane.showMessageDialog(comp, 
                                              "To import panther pathways, please enter the user name and password\n" +
                                              "for the database gk_central@brie8.cshl.edu.",
                                              "Database Info",
                                              JOptionPane.INFORMATION_MESSAGE);
                Properties prop = new Properties();
                prop.setProperty("dbHost", dbHost);
                prop.setProperty("dbName", dbName);
                prop.setProperty("dbPort", "3306");
                DBConnectionPane connectionPane = new DBConnectionPane();
                connectionPane.setValues(prop);
                connectionPane.showUserAndNameOnly();
                if(connectionPane.showInDialog(comp)) {
                    dbUser = prop.getProperty("dbUser");
                    dbPwd = prop.getProperty("dbPwd");
                }
            }
        }
        if (dbUser != null && dbPwd != null) {
            MySQLAdaptor dbAdaptor = new MySQLAdaptor(dbHost,
                                                      dbName,
                                                      dbUser,
                                                      dbPwd,
                                                      Integer.parseInt(dbPort));
            return dbAdaptor;
        }
        return null;
    }
    
    private class ChooseDataSourceDialog extends JDialog {
        
        private boolean isOkClicked = false;
        private JButton okBtn = null;
        private List buttonList;
        
        public ChooseDataSourceDialog(List dataSources,
                                      Frame parentFrame) {
            super(parentFrame);
            init(dataSources);
        }
        
        private void init(List dataSources) {
            setTitle("Choose Data Source");
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel("Please choose one data source:");
            contentPane.add(label, constraints);
            buttonList = new ArrayList(dataSources.size());
            ButtonGroup bg = new ButtonGroup();
            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (Iterator it = buttonList.iterator(); it.hasNext();) {
                        JRadioButton btn = (JRadioButton) it.next();
                        if (btn.isSelected()) {
                            okBtn.setEnabled(true);
                            return;
                        }
                    }
                    okBtn.setEnabled(false);
                }
            };
            for (int i = 0; i < dataSources.size(); i++) {
                JRadioButton btn = new JRadioButton(dataSources.get(i).toString());
                bg.add(btn);
                buttonList.add(btn);
                btn.addActionListener(l);
                constraints.gridy = i + 1;
                contentPane.add(btn, constraints);
            }
            getContentPane().add(contentPane, BorderLayout.CENTER);
            // Create control pane
            DialogControlPane controlPane = new DialogControlPane();
            okBtn = controlPane.getOKBtn();
            okBtn.setEnabled(false);
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    ChooseDataSourceDialog.this.dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    ChooseDataSourceDialog.this.dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            setLocationRelativeTo(getOwner());
        }
        
        public String getSelectedName() {
            for (Iterator it = buttonList.iterator(); it.hasNext();) {
                JRadioButton btn = (JRadioButton) it.next();
                if (btn.isSelected())
                    return btn.getText();
            }
            return null;
        }
        
        public boolean isOkClicked() {
            return this.isOkClicked;
        }
    }
    
    private class PathwaySelectDialog extends JDialog {
        private InstanceListPane listPane;
        private JTextArea summationText;
        private JLabel summationLabel;
        private DialogControlPane controlPane;
        private boolean isOKClicked = false;
        
        public PathwaySelectDialog(List pathways,
                                   Frame parentFrame) {
            super(parentFrame);
            init(pathways);
        }
        
        private void init(List pathways) {
            setTitle("Choose Pathways");
            JLabel captionLabel = GKApplicationUtilities.createTitleLabel("Please choose one or more panther pathways from the following list:");
            captionLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            listPane = new InstanceListPane();
            InstanceUtilities.sortInstances(pathways);
            listPane.setDisplayedInstances(pathways);
            String title = "Pathways in " + dataSource.getDisplayName()  + " (" + pathways.size() + "):";
            listPane.setTitle(title);
            JPanel summationPane = new JPanel();
            summationPane.setLayout(new BorderLayout());
            summationLabel = new JLabel("Summation");
            summationLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            summationText = new JTextArea();
            summationText.setLineWrap(true);
            summationText.setWrapStyleWord(true);
            summationText.setEditable(false);
            summationPane.add(summationLabel, BorderLayout.NORTH);
            JScrollPane scrollPane = new JScrollPane(summationText);
            scrollPane.setMinimumSize(new Dimension(50, 50));
            summationPane.add(scrollPane, BorderLayout.CENTER);
            JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                            listPane,
                                            summationPane);
            jsp.setDividerLocation(360);
            jsp.setResizeWeight(0.6d);
            controlPane = new DialogControlPane();
            JButton okBtn = controlPane.getOKBtn();
            okBtn.setText("Import");
            okBtn.setMnemonic('I');
            getContentPane().add(captionLabel, BorderLayout.NORTH);
            getContentPane().add(jsp, BorderLayout.CENTER);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            installListeners();
            setModal(true);
            setSize(600, 500);
            GKApplicationUtilities.center(this);
        }
        
        private String getSummationText(GKInstance pathway) throws Exception {
            List summations = pathway.getAttributeValuesList(ReactomeJavaConstants.summation);
            if (summations == null || summations.size() == 0)
                return "No summation available.";
            StringBuffer builder = new StringBuffer();
            for (Iterator it = summations.iterator(); it.hasNext();) {
                GKInstance sum = (GKInstance) it.next();
                String text = (String) sum.getAttributeValue(ReactomeJavaConstants.text);
                if (text != null) {
                    builder.append(text);
                    // add a line
                    builder.append("\n\n");
                }
            }
            return builder.toString();
        }
        
        private void installListeners() {
            listPane.addSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    List selected = listPane.getSelection();
                    if (selected == null || selected.size() != 1) {
                        summationText.setText("");
                    }
                    else if (selected.size() == 1) {
                        GKInstance pathway = (GKInstance) selected.get(0);
                        try {
                            String text = getSummationText(pathway);
                            summationText.setText(text);
                        }
                        catch(Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (selected == null || selected.size() == 0)
                        controlPane.getOKBtn().setEnabled(false);
                    else
                        controlPane.getOKBtn().setEnabled(true);
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    dispose();
                }
            });
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    dispose();
                }
            });
            controlPane.getOKBtn().setEnabled(false);
        }
        
        public boolean isOKClicked() {
            return this.isOKClicked;
        }
        
        public List getSelectedPathways() {
            return listPane.getSelection();
        }
        
    }
    
    
}
