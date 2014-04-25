/*
 * Created on Aug 1, 2003
 */
package org.gk.property;

import java.util.ArrayList;

/**
 * This class describes a GO term.
 * @author wgm
 */
public class GOTerm {
	
	public static final int IS_A = 0;
	public static final int PART_OF = 1;
	
	private int type;	
	private java.util.List children;
	private java.util.List synonyms;
	// The first parent
	private GOTerm firstParent;
	private String name;
	private String id;
	
	public GOTerm() {
	}
	
	public GOTerm(String name) {
		this.name = name;
	}
	
	public void addChild(GOTerm child) {
		if (children == null)
			children = new ArrayList();
		if (!(children.contains(child)))
			children.add(child);
	}
	
	public java.util.List getChildren() {
		return this.children;
	}
	
	public void addSynonym(String synonym) {
		if (synonyms == null)
			synonyms = new ArrayList();
		synonyms.add(synonym);
	}
	
	public java.util.List getSynonyms() {
		return this.synonyms;
	}

	// The following methods are used as delegating methods for TreeModel.
	
	public GOTerm getChild(int index) {
		// Should do these checking. However, it is not necessary when
		// GOTerm is only used in JTree, since getChildCound() should be
		// called first.
		/*
		if (children == null)
			return null;
		if (index < 0 || index > children.size() - 1)
			return null;
		*/
		return (GOTerm) children.get(index);
	}
	
	public int getIndexOfChild(GOTerm child) {
		return children.indexOf(child);
	}
	
	public int getChildCount() {
		if (children == null)
			return 0;
		return children.size();		
	}
	
	public boolean isLeaf() {
		if (children == null || children.size() == 0)
			return true;
		return false;
	}
	
	public String toString() {
		if (name == null)
			return "";
		return name;
	}
	
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(String string) {
		id = string;
	}

	public void setName(String string) {
		name = string;
	}
	
	/**
	 * Set the first parent for this GOTerm.
	 * @param parent
	 */
	public void setParent(GOTerm parent) {
		this.firstParent = parent;
	}
	
	/**
	 * Get the first parent for this GOTerm.
	 * @return the first parent.
	 */
	public GOTerm getParent() {
		return this.firstParent;
	}
	
	/**
	 * Do a shallow clone.
	 */
	public Object clone() {
		GOTerm clone = new GOTerm();
		clone.setName(name);
		clone.setId(id);
		clone.setType(type);
		return clone;
	}

	/**
	 * @return the type of this GOTerm.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set the type. Type is one of GOTerm.IS_A or GOTerm.PART_OF.
	 */
	public void setType(int type) {
		this.type = type;
	}

}
