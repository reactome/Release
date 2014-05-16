/*
 * Created on Feb 4, 2004
 */
package org.gk.gkCurator;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.gk.gkCurator.authorTool.AuthorToolToCuratorToolConverter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.util.GKFileFilter;
import org.gk.util.XMLFileFilter;

/**
 * This is a class used to import an authoring tool file to the local repository.
 * @author wugm
 */
public class ImportAuthoringFileEngine {
	
	public ImportAuthoringFileEngine() {
	}
	
	public File importFile(String defaultDir, Component parentComp, boolean isForVer2) {
        // Make sure database is connected
        if(!ensureDatabaseConnection(parentComp))
            return null;
		// Get a selected file
		JFileChooser fileChooser = new JFileChooser();
		if (defaultDir != null) {
			File dir = new File(defaultDir);
			if (dir.exists() && dir.isDirectory())
				fileChooser.setCurrentDirectory(dir);
		}
		GKFileFilter gkFilter = new GKFileFilter();
		fileChooser.addChoosableFileFilter(gkFilter);
		fileChooser.addChoosableFileFilter(new XMLFileFilter());
		fileChooser.setFileFilter(gkFilter);
		fileChooser.setDialogTitle("Choose an Authoring File...");
		int reply = fileChooser.showOpenDialog(parentComp);
		if (reply == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file == null)
				return null;
			try {
			    importFile(file, isForVer2);
                return file;
			}
            catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parentComp,
                                              "Cannot import the author tool file, " + file.getName() + ":" +
                                              e.getMessage(),
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
		}
        return null;
	}
    
    private boolean ensureDatabaseConnection(Component parentComp) {
        MySQLAdaptor dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(parentComp);
        if (dbAdaptor == null) {
            int reply = JOptionPane.showConfirmDialog(parentComp,
                                                      "Cannot connect to a valid database. Some of repository instances\n" +
                                                      "may not be in your local project. Since no active database \n" +
                                                      "connection is available, these instances will be recreated. \n" +
                                                      "Do you wish to continue without database connection?",
                                                      "Continuing importing without db connection?",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.NO_OPTION)
                return false;
        }
        return true;
    }
	
    private void importFile(File file, boolean isForVer2) throws Exception{
        AuthorToolToCuratorToolConverter converter = new AuthorToolToCuratorToolConverter();
        converter.convert(file, isForVer2);
    }
}
