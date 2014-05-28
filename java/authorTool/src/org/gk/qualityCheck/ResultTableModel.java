/*
 * Created on Aug 18, 2009
 *
 */
package org.gk.qualityCheck;

import javax.swing.table.AbstractTableModel;

import org.gk.model.GKInstance;


public abstract class ResultTableModel extends AbstractTableModel {

        private String[] colNames = new String[] {""};
        
        public ResultTableModel() {
        }
        
        public void setColNames(String[] names) {
            this.colNames = names;
        }

        public abstract void setInstance(GKInstance instance); 
        
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        public String getColumnName(int column) {
            return colNames[column];
        }

        public int getColumnCount() {
            return colNames.length;
        }
    }
    
