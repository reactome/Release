/*
 * Created on Aug 3, 2004
 */
package org.gk.persistence;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.gk.model.Bookmark;
import org.gk.model.GKInstance;

/**
 * A wrap class for a list of Bookmark objects.
 * @author wugm
 */
public class Bookmarks {
    private java.util.List bookmarks = new ArrayList();
    private String sortingKey = "displayName"; // Default value
    // For model change listener
    private PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
    
    public Bookmarks() {
    }
    
    public Bookmarks(String sortingKey, java.util.List bookmarks) {
        setSortingKey(sortingKey);
        setBookmarks(bookmarks);
    }

    /**
     * @return Returns the bookmarks.
     */
    public java.util.List getBookmarks() {
        return bookmarks;
    }
    
    public int size() {
        return bookmarks == null ? 0 : bookmarks.size();
    }
    
    public void clear() {
    	if (bookmarks != null)
    		bookmarks.clear();
    }
    
    /**
     * @param bookmarks The bookmarks to set.
     */
    public void setBookmarks(java.util.List bookmarks) {
        this.bookmarks = bookmarks;
    }
    /**
     * @return Returns the sortingKey.
     */
    public String getSortingKey() {
        return sortingKey;
    }
    /**
     * @param sortingKey The sortingKey to set.
     */
    public void setSortingKey(String sortingKey) {
        this.sortingKey = sortingKey;
    }
    
    public Bookmark getBookmark(int index) {
    	if (bookmarks == null)
    		return null;
    	if (index < 0 || index > bookmarks.size() - 1)
    		return null;
    	return (Bookmark) bookmarks.get(index);
    }
    
    public void removeBookmark(int index) {
    	if (bookmarks == null)
    		return;
    	if (index < 0 || index > bookmarks.size() - 1)
    		return;
    	bookmarks.remove(index);
    	propSupport.firePropertyChange("delete", -1, index);
    }
    
	public int addBookmark(GKInstance instance) {
		if (bookmarks.contains(instance.getDBID()))
			return -1;
		if (bookmarks == null)
			bookmarks = new ArrayList();
		Bookmark bookmark = new Bookmark(instance.getDisplayName(),
		                        instance.getDBID(),
		                        instance.getSchemClass().getName());
		// Need to find the inserted index
		int index = 0;
		Bookmark mark = null;
		if (sortingKey.equals("displayName")) {
			for (Iterator it = bookmarks.iterator(); it.hasNext();) {
				mark = (Bookmark) it.next();
				if (mark.getDisplayName().compareTo(bookmark.getDisplayName()) > 0) {
					break; 
				}
				index ++;
			}
		}
		else if (sortingKey.equals("DB_ID")) {
			for (Iterator it = bookmarks.iterator(); it.hasNext();) {
				mark = (Bookmark) it.next();
				if (mark.getDbID().compareTo(bookmark.getDbID()) > 0)
					break;
				index ++;
			}
		}
		else if (sortingKey.equals("type")) {
			for (Iterator it = bookmarks.iterator(); it.hasNext();) {
				mark = (Bookmark) it.next();
				if (mark.getType().compareTo(bookmark.getType()) > 0)
					break;
				index ++;
			}
		}
		else
			index = bookmarks.size();
		bookmarks.add(index, bookmark);
		propSupport.firePropertyChange("add", -1, index);
		return index;
	}

	/**
	 * Delete a Bookmark for the specified GKInstance object.
	 * @param instance
	 */
	public int deleteBookmark(GKInstance instance) {
		Bookmark bookmark = null;
		int index = 0;
		for (Iterator it = bookmarks.iterator(); it.hasNext();) {
			bookmark = (Bookmark) it.next();
			if (bookmark.getDbID().equals(instance.getDBID())) {
				it.remove();
				propSupport.firePropertyChange("delete", -1, index);
				return index;
			}
			index ++;
		}
		return -1; // Nothing deleted
	}
    
    public boolean contains(Long dbID) {
    	if (bookmarks == null)
    		return false;
		Bookmark bookmark = null;
    	for (Iterator it = bookmarks.iterator(); it.hasNext();) {
			bookmark = (Bookmark) it.next();
			if (bookmark.getDbID().equals(dbID))
				return true;
		}
    	return false;
    }
    
    public void sort(Comparator sorter) {
    	if (bookmarks == null)
    		return;
    	Collections.sort(bookmarks, sorter);
    	propSupport.firePropertyChange("sort", -1, 0);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
    	propSupport.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
    	propSupport.removePropertyChangeListener(l);
    }
}
