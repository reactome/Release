/*
 * Created on Mar 12, 2007
 *
 */
package org.reactome.celldesigner;

import jp.sbi.celldesigner.plugin.CellDesignerPlugin;
import jp.sbi.celldesigner.plugin.PluginMenu;
import jp.sbi.celldesigner.plugin.PluginMenuItem;
import jp.sbi.celldesigner.plugin.PluginSBase;

public class ReactomePlugin extends CellDesignerPlugin {

    public ReactomePlugin() {
        PluginMenu menu     = new PluginMenu("Reactome Plug-in");
        ImportAuthorToolProjectPlugIn action = new ImportAuthorToolProjectPlugIn(this);
        PluginMenuItem item = new PluginMenuItem("Import Author Tool Project", action);
        menu.add(item);
        ParseCoordinatesPlugIn parseCoordinates = new ParseCoordinatesPlugIn(this);
        PluginMenuItem parseCoordinatesItem = new PluginMenuItem("Parse Coordinates", parseCoordinates);
        menu.add(parseCoordinatesItem);
        addCellDesignerPluginMenu(menu);
    }
    
    public void addPluginMenu() {
    }

    public void modelClosed(PluginSBase arg0) {
    }

    public void modelOpened(PluginSBase arg0) {
    }

    public void modelSelectChanged(PluginSBase arg0) {
    }

    public void SBaseAdded(PluginSBase arg0) {
    }

    public void SBaseChanged(PluginSBase arg0) {
    }

    public void SBaseDeleted(PluginSBase arg0) {
    }
    
}
