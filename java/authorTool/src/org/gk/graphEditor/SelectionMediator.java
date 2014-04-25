/*
 * Created on Jan 4, 2007
 *
 */
package org.gk.graphEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Combined with interface Selectable, this class is used to synchronize selection among
 * several selectable components.
 * @author guanming
 *
 */
public class SelectionMediator {
    private List selectables;
    // A flag to block circular calling
    private boolean isLocked;
    
    public SelectionMediator() {
    }
    
    public void addSelectable(Selectable s) {
        if (selectables == null)
            selectables = new ArrayList();
        selectables.add(s);
    }
    
    public synchronized void fireSelectionEvent(Selectable source) {
        if (selectables == null || selectables.size() == 0)
            return;
        if (isLocked) // It has been locked by other source
            return; 
        isLocked = true;
        Selectable s = null;
        List selection = source.getSelection();
        for (Iterator it = selectables.iterator(); it.hasNext();) {
            s = (Selectable) it.next();
            if (s == source)
                continue;
            // In case this list is changed 
            s.setSelection(new ArrayList(selection));
        }
        isLocked = false;
    }
    
}
