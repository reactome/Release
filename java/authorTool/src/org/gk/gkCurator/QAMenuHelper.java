/*
 * Created on May 25, 2010
 *
 */
package org.gk.gkCurator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.gk.database.EventCentricViewPane;
import org.gk.database.SchemaViewPane;
import org.gk.model.PersistenceAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.qualityCheck.AbstractQualityCheck;
import org.gk.util.GKApplicationUtilities;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This helper class is used to set up QA menus.
 * @author wgm
 *
 */
public class QAMenuHelper {
   
    public QAMenuHelper() {
    }
    
    public JMenu createQAMenu(SchemaViewPane schemaView,
                              PersistenceAdaptor dataSource) {
        try {
            List<?> children = loadQACheckers();
            return initQAActions(children, schemaView, dataSource);
        }
        catch(Exception e) {
            System.err.println("GKCuratorFrame.createQAMenuForBrowser(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    private List<?> loadQACheckers() throws IOException, JDOMException {
        InputStream metaConfig = GKApplicationUtilities.getConfig("curator.xml");
        if (metaConfig == null)
            return null;
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document document = builder.build(metaConfig);
        org.jdom.Element qaCheckers = document.getRootElement().getChild("QACheckers");
        List<?> children = qaCheckers.getChildren();
        return children;
    }
    
    public JMenu createQAMenu(EventCentricViewPane eventView,
                              PersistenceAdaptor dataSource) {
        try {
            List children = loadQACheckers();
            return initQAActions(children, 
                                 eventView, 
                                 dataSource);
        }
        catch(Exception e) {
            System.err.println("GKCuratorFrame.createQAMenuForBrowser(): " + e);
            e.printStackTrace();
        }
        return null;
    }
    
    private JMenu initQAActions(List checkers,
                                EventCentricViewPane eventView,
                                PersistenceAdaptor dataSource) {
        if (checkers == null || checkers.size() == 0)
            return null;
        JMenu qaMenu = new JMenu("QA Check");
        createQAMenuItem(qaMenu, checkers, null, eventView, dataSource);
        return qaMenu;
    }
    
    private JMenu initQAActions(List<?> checkers, 
                                SchemaViewPane schemaView,
                                PersistenceAdaptor dataSource) {
        if (checkers == null || checkers.size() == 0)
            return null;
        JMenu qaMenu = new JMenu("QA Check");
        qaMenu.setMnemonic('Q');
        createQAMenuItem(qaMenu, checkers, schemaView, null, dataSource);
        return qaMenu;
    }
    
    private void createQAMenuItem(JMenu menu,
                                  List<?> checkers,
                                  final SchemaViewPane schemaView,
                                  final EventCentricViewPane eventView,
                                  final PersistenceAdaptor dataSource) {
        org.jdom.Element checkerElm = null;
        for (Iterator<?> it = checkers.iterator(); it.hasNext();) {
            checkerElm = (org.jdom.Element) it.next();
            String dbOnly = checkerElm.getAttributeValue("dbOnly");
            if (dbOnly != null && dbOnly.equals("true") && dataSource instanceof XMLFileAdaptor)
                continue;
            if (checkerElm.getName().equals("QAChecker")) {
                String name = checkerElm.getAttributeValue("name");
                final String clsName = checkerElm.getAttributeValue("class");
                JMenuItem checkItem = new JMenuItem(name);
                menu.add(checkItem);
                checkItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        AbstractQualityCheck.doCheck(clsName, 
                                                     dataSource,
                                                     schemaView,
                                                     eventView,
                                                     GKApplicationUtilities.getApplicationProperties());
                    }
                });
            }
            else if (checkerElm.getName().equals("Group")) {
                // This should be a JMenu
                String name = checkerElm.getAttributeValue("name");
                JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                // No other type elements are allowed. One layer only!
                List children = checkerElm.getChildren("QAChecker");
                createQAMenuItem(subMenu, 
                                 children, 
                                 schemaView,
                                 eventView,
                                 dataSource);
            }
        }
    }
    
}
