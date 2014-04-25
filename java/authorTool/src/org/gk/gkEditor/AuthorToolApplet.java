/*
 * Created on Feb 1, 2007
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JApplet;
import javax.swing.JOptionPane;

import org.gk.persistence.Project;
import org.gk.util.AuthorToolAppletUtilities;

public class AuthorToolApplet extends JApplet {
    private GKEditorFrame frame = null;
    
    public void init() {
        AuthorToolAppletUtilities.isInApplet = true;
        frame = new GKEditorFrame();
        frame.setVisible(false);
        Component toolPane = frame.getContentPane().getComponent(0);
        getContentPane().add(toolPane, BorderLayout.CENTER);
    }

    @Override
    public void stop() {
        createSaveWarning();
    }

    /**
     * A helper method to generate a warning in case there is any unsaved change.
     */
    private void createSaveWarning() {
        Project project = frame.getEditorManager().getOpenedProject();
        if (project.isDirty()) {
            int reply =
                JOptionPane.showConfirmDialog(
                    this,
                    "Do you want to save the changes you made to " + project.getName(),
                    "Save Changes?",
                    JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
                frame.getEditorManager().save();
        }
    }
    
}
