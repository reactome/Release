/*
 * Created on Aug 17, 2009
 *
 */
package org.gk.qualityCheck;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to organize QAs that can be run based on classes, e.g., ImbalanceChecker, CompartmentChecker, etc.
 * @author wgm
 *
 */
public abstract class ClassBasedQualityCheck extends AbstractQualityCheck {
    
    protected final int SIZE_TO_LOAD_ATTS = 10;

    /**
     * Check if a shell instance is contained in the passed instances.
     * @param instances
     * @return
     */
    protected boolean containShellInstances(Collection instances) {
        GKInstance instance = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.isShell()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * A way to dispaly error message regarding the checking.
     */
    protected abstract void showErrorMessage();
    
    protected void loadAttributes(String clsName,
                                  String attName,
                                  MySQLAdaptor dba) throws Exception {
        Collection instances = dba.fetchInstancesByClass(clsName);
        loadAttributes(instances, clsName, attName, dba);
    }
    
    protected void loadAttributes(Collection instances,
                                  String clsName,
                                  String attName,
                                  MySQLAdaptor dba) throws Exception {
        SchemaAttribute att = dba.getSchema().getClassByName(clsName).getAttribute(attName);
        dba.loadInstanceAttributeValues(instances, att);
    }
    
    protected ListSelectionListener generateListSelectionListener(final ResultPane resultPane, 
                                                                  final JSplitPane jsp,
                                                                  final JButton checkOutBtn, 
                                                                  final String titlePrefix) {
        ListSelectionListener l = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                List selected = getDisplayedList().getSelection();
                if (selected.size() > 0)
                    checkOutBtn.setEnabled(true);
                else
                    checkOutBtn.setEnabled(false);
                if (selected.size() != 1)
                    return;
                if (!resultPane.isVisible()) {
                    resultPane.setVisible(true);
                    jsp.setDividerLocation((int)(jsp.getHeight() * 0.75));
                }
                GKInstance reaction = (GKInstance) selected.get(0);
                resultPane.setInstance(reaction);
                resultPane.setText(titlePrefix + " for \"" + reaction.getDisplayName() + " [" + reaction.getDBID() + "]\"");
            }
        };
        return l;
    }


    protected class ResultPane extends JPanel {
        
        private JTable resultTable;
        private JLabel resultLabel;
        
        public ResultPane() {
            init();
        }
        
        private void init() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEtchedBorder());
            resultLabel = GKApplicationUtilities.createTitleLabel("Imbalance");
            resultTable = new JTable();
            add(new JScrollPane(resultTable), BorderLayout.CENTER);
            add(resultLabel, BorderLayout.NORTH);
        }
        
        public void setTableModel(ResultTableModel model) {
            resultTable.setModel(model);
        }
        
        public void setInstance(GKInstance instance) {
            ResultTableModel model = (ResultTableModel) resultTable.getModel();
            model.setInstance(instance);
        }
        
        public void setText(String text) {
            resultLabel.setText(text);
        }
    }

}
