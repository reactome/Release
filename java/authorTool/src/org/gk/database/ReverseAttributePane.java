/*
 * Created on Jan 24, 2006
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPanel is used to display Referrers.
 * @author guanming
 *
 */
public class ReverseAttributePane extends JPanel {
    //GUIs
    private JLabel titleLabel;
    private AttributeTable propTable;
    // To control if closable panel is needed
    private JPanel closePane;
    // To control editablity of attribute property dialogs
    private boolean isEditable;
    // Used to detect the change in a popup property dialog
    private AttributeEditListener editListener;
    // Record this GKInstance so that a refresh can be done
    private GKInstance instance;
    // For generating dialog
    private Component parentComp;
    // For display popup menu
    private List<Action> popupActions;
    
    public ReverseAttributePane() {
        init();
    }
    
    public void setParentComponent(Component comp) {
        this.parentComp = comp;
    }
    
    public void showInDialog() {
        if (isEmpty()) {
            JOptionPane.showMessageDialog(parentComp,
                    "There are no instances referring to the instance: \n" +
                    instance.toString(),
                    "Instance Referers",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JDialog dialog = GKApplicationUtilities.createDialog(parentComp, "Referrers Dialog");
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.setSize(500, 400);
        //Give the second column more space
        propTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        propTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        dialog.setLocationRelativeTo(dialog.getOwner());
        initEditListener();
        // Need to track changes for an editable display
        if (isEditable) {
            AttributeEditManager.getManager().addAttributeEditListener(editListener);
            // Remove to remove this editListener when the dialog is closed. Otherwise, it
            // will be stuck in memory
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    AttributeEditManager.getManager().removeAttributeEditListener(editListener);
                }
            });
        }
        dialog.setVisible(true);
    }
    
    /**
     * Display referrers for a specified GKInstance object.
     * @param instance
     */
    public void displayReferrersWithCallback(GKInstance instance,
                                             final Component parentComp) {
        setGKInstance(instance);
        setParentComponent(parentComp);
        // Add a popup menu so that another panel can be displayed
        // Add a referrer action
        Action displayReferersAction = new AbstractAction("Display Referrers") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                List selection = getSelectedInstances();
                GKInstance selected = (GKInstance) selection.get(0);
                ReverseAttributePane newPane = new ReverseAttributePane();
                newPane.displayReferrersWithCallback(selected, parentComp);
            }
        };
        addPopupAction(displayReferersAction);
        showInDialog();
    }
    
    private void initEditListener() {
        if (editListener != null)
            return;
        editListener = new AttributeEditListener() {
            public void attributeEdit(AttributeEditEvent e) {
                String attName = e.getAttributeName();
                if (attName != null && isAttDisplayed(attName)) {
                    resetReferrersMap();
                }
                else if (attName == null)
                    resetReferrersMap(); // Have to refresh the whole thing
            }
        };
    }
    
    private boolean isAttDisplayed(String attName) {
        int rowCount = propTable.getRowCount();
        String key = null;
        for (int i = 0; i < rowCount; i++) {
            key = (String) propTable.getValueAt(i, 0);
            if (key != null && key.equals(attName))
                return true;
        }
        return false;
    }
    
    private void init() {
        setLayout(new BorderLayout());
        
        titleLabel = new JLabel();
        titleLabel.setBorder(GKApplicationUtilities.getTitleBorder());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        add(titleLabel, BorderLayout.NORTH);
        
        propTable = new AttributeTable();
        ReverseAttributeModel model = new ReverseAttributeModel();
        propTable.setModel(model);
        // To popup a new AttributePane
        propTable.addMouseListener(createTableCellListener());
        add(new JScrollPane(propTable), BorderLayout.CENTER);
        closePane = new JPanel();
        closePane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Window root = SwingUtilities.getWindowAncestor(ReverseAttributePane.this);
                if (root != null)
                    root.dispose();
            }
        });
        closePane.add(closeBtn);
        add(closePane, BorderLayout.SOUTH);
    }
    
    public void addPopupAction(Action action) {
        if (popupActions == null)
            popupActions = new ArrayList<Action>();
        popupActions.add(action);
    }
    
    private void doPopup(MouseEvent e) {
        if (popupActions == null || popupActions.size() == 0)
            return ;
        JPopupMenu popup = new JPopupMenu();
        for (Action action : popupActions) {
            popup.add(action);
        }
        popup.show(propTable, e.getX(), e.getY());
    }
    
    public void hideClosePane() {
        closePane.setVisible(false);
    }
    
    private MouseListener createTableCellListener() {
        MouseAdapter adapter = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2  &&
                    !e.isPopupTrigger()) { // To avoid chasing: popup a dialog and display Popup menu
                    // Check the selected instance
                    // Only a single selection should popup a dialog
                    int rowCount = propTable.getSelectedRowCount();
                    int colCount = propTable.getSelectedColumnCount();
                    if (rowCount > 1 || colCount > 1)
                        return;
                    // Work only for values
                    int selectedRow = propTable.getSelectedRow();
                    int selectedCol = propTable.getSelectedColumn();
                    if ((selectedCol == 1) && (selectedRow > -1)) {
                        GKInstance selectedInstance = (GKInstance) propTable.getValueAt(selectedRow, selectedCol);
                        //This can only work for a dialog
                        Window parentWindow = SwingUtilities.getWindowAncestor(ReverseAttributePane.this);
                        if (parentWindow instanceof JDialog)
                            FrameManager.getManager().showInstance(selectedInstance, (JDialog)parentWindow, isEditable);
                        else
                            FrameManager.getManager().showInstance(selectedInstance, isEditable);
                    }
                }
                else if (e.isPopupTrigger())
                    doPopup(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }
        };
        return adapter;
    }
    
    public void setGKInstance(GKInstance instance) {
        this.instance = instance;
        // Only local GKInstance can be edited.
        isEditable = instance.getDbAdaptor() instanceof XMLFileAdaptor;
        titleLabel.setText(instance.toString() + "'s Referrers:");
        resetReferrersMap();
    }
    
    private void resetReferrersMap() {
        if (instance == null)
            return; // In case
        PersistenceAdaptor adaptor = instance.getDbAdaptor();
        try {
            Map map = null;
            if (adaptor instanceof XMLFileAdaptor) { // For a local GKInstance
                map = ((XMLFileAdaptor)adaptor).getReferrersMap(instance);
            }
            else if (adaptor instanceof MySQLAdaptor) {
                map = getReferrersMapForDBInstance(instance);
            }
            if (map == null)
                map = new HashMap();
            ReverseAttributeModel model = (ReverseAttributeModel) propTable.getModel();
            model.setReferrersMap(map);
        }
        catch(Exception e) {
            System.err.println("ReverseAttributePane.resetReferrersMap(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * A helper method to fetch all referrers for a database GKInstance.
     * @param instance
     * @return
     * @throws Exception
     */
    public Map<String, List<GKInstance>> getReferrersMapForDBInstance(GKInstance instance) throws Exception {
        Map<String, Set<GKInstance>> map = new HashMap<String, Set<GKInstance>>();
        for (Iterator rai = instance.getSchemClass().getReferers().iterator(); rai.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute)rai.next();
            Collection r = instance.getReferers(att);
            // It is possible two attributes have same name but from different classe as in
            // pathwayDiagram in Edge and Vertex.
            if (r == null || r.size() == 0)
                continue;
            Set<GKInstance> set = map.get(att.getName());
            if (set == null) {
                set = new HashSet<GKInstance>();
                map.put(att.getName(), set);
            }
            set.addAll(r);
        }
        // Do a sort
        Map<String, List<GKInstance>> rtnMap = new HashMap<String, List<GKInstance>>();
        for (String attName : map.keySet()) {
            Set<GKInstance> set = map.get(attName);
            List<GKInstance> list = new ArrayList<GKInstance>(set);
            InstanceUtilities.sortInstances(list);
            rtnMap.put(attName, list);
        }
        return rtnMap;
    }
    
    /**
     * Check if there is any referrer available.
     * @return true for no referrer available.
     */
    private boolean isEmpty() {
        ReverseAttributeModel model = (ReverseAttributeModel) propTable.getModel();
        // Check if any values in the model value column
        int rowCount = model.getRowCount();
        for (int i = 0; i < rowCount; i ++) {
            if (model.getValueAt(i, 1) != null)
                return false;
        }
        return true;
    }
    
    /**
     * Expose the internal AttributeTable so that the client can do some customized adding.
     * @return
     */
    public AttributeTable getAttributeTable() {
        return this.propTable;
    }
    
    /**
     * Return the selected GKInstance objects. 
     * @return an emptty List will be returned if nothing is selected.
     */
    public List getSelectedInstances() {
        Set rtn = new HashSet();
        if (!propTable.isColumnSelected(1))
            return new ArrayList(); // No GKInstance is selected
        int rowCount = propTable.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            if (propTable.isCellSelected(i, 1))
                rtn.add(propTable.getValueAt(i, 1));
        }
        return new ArrayList(rtn);
    }
    
    class ReverseAttributeModel extends AttributeTableModel {
        private String[] headers = new String[] {"Referrer Property Name", "Referrer"};
        private Object[][] values;
        private CellSpan cellSpan;
        
        public ReverseAttributeModel() {
            cellSpan = new CellSpan();
            values = new Object[1][1];
        }
        
        public String getColumnName(int col) {
            return headers[col];
        }
        
        public CellSpan getCellSpan() {
            return cellSpan;
        }
        
        public int getColumnCount() {
            return headers.length;
        }
        
        public int getRowCount() {
            return values.length;
        }
        
        public Object getValueAt(int row, int col) {
            if (cellSpan.isVisible(row, col)) {
                return values[row][col];
            }
            return null;
        }
        
        public boolean isCellEditable(int row, int col) {
            return false; // This is not editable table
        }
        
        public void setReferrersMap(Map refererMap) {
            List keys = new ArrayList(refererMap.keySet());
            Collections.sort(keys);
            int col = 2;
            int row = 0;
            // Calculate the total row number
            for (Iterator it = keys.iterator(); it.hasNext();) {
                Object key = it.next();
                List valueList = (List) refererMap.get(key);
                if (valueList != null) 
                    row += (valueList.size() == 0 ? 1 : valueList.size());
                else
                    row++;
            }
            values = new Object[row][col];
            row = 0;
            for (Iterator it = keys.iterator(); it.hasNext();) {
                Object key = it.next();
                List referers = (List) refererMap.get(key);
                if (referers == null || referers.size() == 0) {
                    values[row][0] = key;
                    values[row][1] = null;
                    row++;
                }
                else {
                    int size = referers.size() == 0 ? 1 : referers.size();
                    values[row][0] = key;
                    int i = 0;
                    // Have to encode line separators
                    for (Iterator it1 = referers.iterator(); it1.hasNext();) {
                        values[row + i][1] = it1.next();
                        i++;
                    }
                    row += size;
                }
            }
            cellSpan.initSpans(refererMap, keys);
            fireTableDataChanged();
        }
        
        public SchemaClass getSchemaClass() {
            return null;
        }
    }
}
