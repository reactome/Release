/*
 * Created on Feb 28, 2008
 *
 */
package org.reactome.celldesigner;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import jp.sbi.celldesigner.plugin.PluginAction;
import jp.sbi.celldesigner.plugin.PluginListOf;
import jp.sbi.celldesigner.plugin.PluginModel;
import jp.sbi.celldesigner.plugin.PluginReaction;
import jp.sbi.celldesigner.plugin.PluginSpeciesAlias;

/**
 * This plugin is used to extract coordinates information from a CellDesginer model.
 * @author wgm
 *
 */
public class ParseCoordinatesPlugIn extends PluginAction {
    private ReactomePlugin plugin;
    
    public ParseCoordinatesPlugIn(ReactomePlugin plugin) {
        this.plugin = plugin;
    }
    
    public void myActionPerformed(ActionEvent action) {
        // Temp test
        try {
            File tmpFile = new File("/Users/wgm/Desktop/CellDesignerOutput.txt");
            FileWriter fileWriter = new FileWriter(tmpFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            PluginModel model = plugin.getSelectedModel();
            PluginListOf list = model.getListOfAllSpeciesAlias();
            List speciesList = list.getArray();
            for (Iterator it = speciesList.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof PluginSpeciesAlias) {
                    PluginSpeciesAlias alias = (PluginSpeciesAlias) obj;
                    double x = alias.getX();
                    double y = alias.getY();
                    double width = alias.getWidth();
                    double height = alias.getHeight();
                    printWriter.println(alias.getName() + ": " + x + ", " + y + ", " + width + ", " + height);
                }
            }
            list = model.getListOfReactions();
            List reactionList = list.getArray();
            for (Iterator it = reactionList.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof PluginReaction) {
                    PluginReaction reaction = (PluginReaction) obj;
                    String sbml = reaction.toSBML();
                    printWriter.println(reaction.getId() + ": " + sbml + ": " + reaction.toString());
                }
            }
            printWriter.close();
            fileWriter.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    
}
