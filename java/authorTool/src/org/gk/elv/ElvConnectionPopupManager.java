/*
 * Created on Dec 16, 2008
 *
 */
package org.gk.elv;

import org.gk.graphEditor.ConnectionPopupManager;

/**
 * This customized ConnectionPopupManager is used to handle popup action for the ELV.
 * @author wgm
 *
 */
public class ElvConnectionPopupManager extends ConnectionPopupManager {
    
    public ElvConnectionPopupManager() {
    }

    @Override
    protected void setRolesForNodeToNode() {
        roleList.add("Produce");
        roleList.add("Associate");
    }
    
}
