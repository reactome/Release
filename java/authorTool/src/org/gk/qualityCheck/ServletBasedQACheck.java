/*
 * Created on Mar 11, 2011
 *
 */
package org.gk.qualityCheck;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.gk.database.AttributeEditConfig;
import org.gk.database.InstanceListPane;
import org.gk.database.WSInfoHelper;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.util.FileUtilities;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TextFileFilter;

/**
 * This QA check is based on a server-side program for performance reason. The returned
 * results from the server-side should be a tab-delimited file with the first line
 * as the table headers.
 * @author wgm
 *
 */
public class ServletBasedQACheck extends AbstractQualityCheck {
    
    private String qaUrl;
    protected String actionName;
    protected String resultTitle;
    private JTable resultTable;

    public String getQaUrl() {
        return qaUrl;
    }

    public void setQaUrl(String qaUrl) {
        this.qaUrl = qaUrl;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public void check(GKSchemaClass cls) {
    }

    @Override
    public void check() {
            Thread t = new Thread() {
                public void run() {
                    initProgressPane("Check Reactions in All Pathway Diagrams");
                    try {
                        String urlName = constructWSURL();
    //                    System.out.println(urlName);
                        if (urlName == null) {
                            hideProgressPane();
                            return;
                        }
                        URL url = new URL(urlName);
                        InputStream is = url.openStream();
                        displayResults(is);
                    }
                    catch(Exception e) {
                        JOptionPane.showMessageDialog(parentComp, 
                                                      "Error in checking all reactions in all pathway diagrams: \n" + e,
                                                      "Error in QA",
                                                      JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                    hideProgressPane();
                }
            };
            t.start();
        }

    private void displayResults(InputStream is) throws IOException {
        TabTextTableModel tableModel = new TabTextTableModel();
        tableModel.readContent(is);
        resultTable = new JTable();
        resultTable.setModel(tableModel);
        resultTable.getTableHeader().setReorderingAllowed(false);
        TableRowSorter<TabTextTableModel> sorter = new TableRowSorter<TabTextTableModel>(tableModel);
        resultTable.setRowSorter(sorter);
        sorter.toggleSortOrder(0); // Sort based on the first column
        // Set up a JFrame to display this table
        JFrame frame = new JFrame(resultTitle);
        frame.getContentPane().add(new JScrollPane(resultTable), BorderLayout.CENTER);
        CheckOutControlPane controlPane = new CheckOutControlPane(frame);
        controlPane.setCheckOutVisiable(false);
        JTextField filter = createFilterText(sorter);
        controlPane.add(new JLabel("Filter:"));
        controlPane.add(filter);
        frame.getContentPane().add(controlPane, BorderLayout.SOUTH);
        // Set image icon
        if (parentComp instanceof JFrame) {
            frame.setIconImage(((JFrame)parentComp).getIconImage());
        }
        frame.setSize(650, 525);
        frame.setLocationRelativeTo(parentComp);
        frame.setVisible(true);
    }
    
    private JTextField createFilterText(final TableRowSorter<TabTextTableModel> sorter) {
        final JTextField tf = new JTextField();
        tf.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                setUpFilter(sorter, tf);
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                setUpFilter(sorter, tf);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                setUpFilter(sorter, tf);
            }
        });
        tf.setColumns(30);
        return tf;
    }
    
    private void setUpFilter(TableRowSorter<TabTextTableModel> sorter,
                             JTextField tf) {
        String text = tf.getText().trim();
        if (text.length() == 0) {
            sorter.setRowFilter(null);
            return;
        }
        // Do some parsing
        String[] tokens = text.split(";");
        // Get the table header
        TabTextTableModel model = (TabTextTableModel) resultTable.getModel();
        List<String> headers = model.headers;
        List<RowFilter<TabTextTableModel, Object>> filters = new ArrayList<RowFilter<TabTextTableModel,Object>>();
        for (String token : tokens) {
            token = token.trim();
            int index = token.indexOf(":");
            if (index > 0) {
                String header = token.substring(0, index).trim();
                String pattern = token.substring(index + 1).trim();
                int col = headers.indexOf(header);
                if (col >= 0) {
                    RowFilter<TabTextTableModel, Object> rf = RowFilter.regexFilter(pattern, 
                                                                                    col);
                    filters.add(rf);
                }
                else {
                    RowFilter<TabTextTableModel, Object> rf = RowFilter.regexFilter(pattern);
                    filters.add(rf);
                }
            }
            else {
                RowFilter<TabTextTableModel, Object> rf = RowFilter.regexFilter(token);
                filters.add(rf);
            }
        }
        RowFilter<TabTextTableModel, Object> rf = RowFilter.andFilter(filters);
        sorter.setRowFilter(rf);
    }
    
    protected String fixHeader(String line) {
        return line;
    }

    /**
     * A helper method to create an action for dump to file button.
     * @return
     */
    protected Action createDumpToFileAction() {
        Action action = new AbstractAction("Dump to File") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = GKApplicationUtilities.createFileChooser(GKApplicationUtilities.getApplicationProperties());
                FileFilter txtFilter = new TextFileFilter();
                fileChooser.addChoosableFileFilter(txtFilter);
                JButton btn = (JButton) e.getSource();
                File file = GKApplicationUtilities.chooseSaveFile(fileChooser, 
                                                                  ".txt", 
                                                                  btn == null ? parentComp : btn);
                if (file == null)
                    return;
                saveTableToFile(file);
            }
        };
        return action;
    }

    /**
     * Helper method to save the table into a tab-delimited file.
     * @param file
     */
    private void saveTableToFile(File file) {
        FileUtilities fu = new FileUtilities();
        try {
            fu.setOutput(file.getAbsolutePath());
            StringBuilder builder = new StringBuilder();
            TabTextTableModel model = (TabTextTableModel) resultTable.getModel();
            for (Iterator<String> it = model.headers.iterator(); it.hasNext();) {
                String header = it.next();
                builder.append(header);
                if (it.hasNext())
                    builder.append("\t");
            }
            fu.printLine(builder.toString());
            builder.setLength(0);
            for (int row = 0; row < resultTable.getRowCount(); row++) {
                for (int col = 0; col < resultTable.getColumnCount(); col++) {
                    Object value = resultTable.getValueAt(row, col);
                    builder.append(value == null ? "" : value.toString());
                    if (col < resultTable.getColumnCount() - 1)
                        builder.append("\t");
                }
                fu.printLine(builder.toString());
                builder.setLength(0);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentComp,
                                          "Eror in dumping the table content into a file: " + e,
                                          "Error in Dumping",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private String constructWSURL() throws UnsupportedEncodingException {
        String[] connectInfo = new WSInfoHelper().getWSInfo(parentComp);
        if (connectInfo == null) {
            JOptionPane.showMessageDialog(parentComp,
                                    "No connecting information to the server side program is provided!",
                                    "Error in QA Check",
                                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String user = connectInfo[0];
        String key = connectInfo[1];
        if (qaUrl == null)
            qaUrl = AttributeEditConfig.getConfig().getPDUrl();
        // dataSource should be MySQLAdaptor only
        String dbName = ((MySQLAdaptor)dataSource).getDBName();
        String dbHost = ((MySQLAdaptor)dataSource).getDBHost();
        return qaUrl + "?action=" + actionName + 
                "&dbHost=" + dbHost + 
                "&dbName=" + dbName + 
                "&user=" + user + 
                "&key=" + key;
    }

    @Override
    public void check(GKInstance instance) {
    }

    @Override
    public void check(List<GKInstance> instances) {
    }

    @Override
    public void checkProject(GKInstance event) {
    }

    @Override
    protected InstanceListPane getDisplayedList() {
        return null;
    }
    
    /**
     * The output returned from the server should a tab-delimited file with the first line as a table header.
     * The following customized TableModel is used to construct a table for displaying the results.
     */
    class TabTextTableModel extends AbstractTableModel {
        List<String> headers;
        List<List<String>> contents;
        
        public TabTextTableModel() {
            super();
        }
        
        public void readContent(InputStream is) throws IOException {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            line = fixHeader(line);
            String[] tokens = line.split("\t");
            // First line is for the headers
            headers = Arrays.asList(tokens);
            contents = new ArrayList<List<String>>();
            while ((line = br.readLine()) != null) {
                tokens = line.split("\t");
                contents.add(Arrays.asList(tokens));
            }
            br.close();
            isr.close();
            is.close();
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Long.class;
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            return headers.get(column);
        }

        @Override
        public int getRowCount() {
            if (contents == null)
                return 0;
            return contents.size();
        }

        @Override
        public int getColumnCount() {
            if (headers == null)
                return 0;
            return headers.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (contents == null)
                return null;
            List<String> values = contents.get(rowIndex);
            if (columnIndex < values.size()) {
                Object value = values.get(columnIndex);
                if (columnIndex == 0)
                    return new Long(value.toString());
                return value;
            }
            else
                return null;
        }
        
    }
}
