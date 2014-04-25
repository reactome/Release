/*
 * Created on Aug 1, 2003
 */
package org.gk.property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * This is the model class for GODisplayPane.
 * @author wgm
 */
public class GODisplayModel {
	private TreeModel treeModel;
	private Map nodeMap; // Keep a record of terms with the same ID.
	
	public GODisplayModel() {
		load();
	}
	
	private void load() {
		GOTerm rootTerm = null;
		try {
			FileReader reader = new FileReader("resources" + File.separator + "process.ontology");
			BufferedReader bfr = new BufferedReader(reader);
			String line = null;
			StringTokenizer tokenizer = null;
			while ((line = bfr.readLine()) != null) {
				if (line.startsWith("!"))
					continue;
				if (line.startsWith("$")) { // For Gene_Ontology
					// This is the rootTerm
					rootTerm = new GOTerm();
					int index = line.indexOf(";");
					rootTerm.setName(line.substring(1, index).trim());
					rootTerm.setId(line.substring(index + 1).trim());
					break;
				}
			}
			Pattern pattern = Pattern.compile("[<%]");
			Matcher matcher = null;
			int layer;
			int prevLayer = 0;
			int diffLayer = 0;
			GOTerm prevTerm = rootTerm;
			String[] terms = null;
			nodeMap = new HashMap();
			Pattern escapePtn = Pattern.compile("\\\\,");
			Pattern tokenPtn = Pattern.compile(";");
			while ((line = bfr.readLine()) != null) {
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					layer = matcher.start();
					diffLayer = layer - prevLayer;
					terms = pattern.split(line.subSequence(matcher.end(), line.length()));
					String[] tokens = tokenPtn.split(terms[0]);
					String termName = tokens[0].trim();
					termName = escapePtn.matcher(termName).replaceAll(",");
					String termID = tokens[1].trim();
					GOTerm term = new GOTerm();
					term.setName(termName);
					term.setId(termID);
					// Check type
					String typeStr = matcher.group();
					if (typeStr.equals("<"))
						term.setType(GOTerm.PART_OF);
					else // There are only two types.
						term.setType(GOTerm.IS_A);
					// Parse synonyms
					for (int i = 2; i < tokens.length; i++) {
						int index2 = tokens[i].indexOf(":");
						String synonym = tokens[i].substring(index2 + 1).trim();
						term.addSynonym(synonym);
					}
					if (diffLayer == 1) {
						prevTerm.addChild(term);
						term.setParent(prevTerm);
					}
					else {
						GOTerm parentTerm = prevTerm.getParent();
						for (int i = diffLayer; i < 0; i++) {
							parentTerm = parentTerm.getParent();
						}
						parentTerm.addChild(term);
						term.setParent(parentTerm);
					}
					prevTerm = term;
					prevLayer = layer;
					// Check Node Map
					Object obj = nodeMap.get(termID);
					if (obj == null) {
						nodeMap.put(termID, term);
					}
					else if (obj instanceof GOTerm) {
						GOTerm term1 = (GOTerm) obj;
						java.util.List list = new ArrayList();
						list.add(term1);
						list.add(term);
						nodeMap.put(termID, list);
					}
					else if (obj instanceof java.util.List) {
						java.util.List list = (java.util.List) obj;
						list.add(term);
					}
				}
			}
			// Purge NodeMap to keep the memory usage as small as possible
			for (Iterator it = nodeMap.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				Object value = nodeMap.get(key);
				if (value instanceof GOTerm)
					it.remove();
			}
			reader.close();
			bfr.close();
		}
		catch(IOException e) {
			System.err.println("GODisplayModel.load(): " + e);
		}
		treeModel = new GOTreeModel(rootTerm);
	}
	
	public TreeModel getTreeModel() {
		return treeModel;
	}
	
	public TreeModel buildModeForTerm(GOTerm term) {
		Object obj = nodeMap.get(term.getId());
		GOTerm root = null;
		if (obj == null) { // Sinle parent
			GOTerm child = term;
			GOTerm parent = child.getParent();
			GOTerm childClone = (GOTerm) child.clone();
			while (parent != null) {
				GOTerm parentClone = (GOTerm) parent.clone();
				// Create linking
				childClone.setParent(parentClone);
				parentClone.addChild(childClone);
				// GO up
				child = parent;
				parent = parent.getParent();
				childClone = parentClone;
			}
			root = childClone;
		}
		else {
			java.util.List list = (java.util.List) obj;
			Map map = new HashMap();
			GOTerm termClone = (GOTerm) term.clone();
			GOTerm childClone = null;
			// GO through other tree paths.	
			for (int i = 0; i < list.size(); i++) {
				GOTerm child = (GOTerm) list.get(i);
				GOTerm parent = child.getParent();
				childClone = termClone;
				while (parent != null) {
					GOTerm parentClone = (GOTerm) map.get(parent.getId());
					if (parentClone == null) {
						parentClone = (GOTerm) parent.clone();
						map.put(parentClone.getId(), parentClone);
					}
					parentClone.addChild(childClone);
					child = parent;
					parent = parent.getParent();
					childClone = parentClone;
				}
			}
			// Convert the map to tree
			root = (GOTerm) childClone.clone();
			convertMapToTree(childClone, root);
		}
		GOTreeModel model = new GOTreeModel(root);
		return model;
	}
	
	/**
	 * A recursive method to convert a map to tree.
	 */
	private void convertMapToTree(GOTerm mapNode, GOTerm treeNode) {
		java.util.List children = mapNode.getChildren();
		if (children == null || children.size() == 0)
			return;
		for (Iterator it = children.iterator(); it.hasNext();) {
			GOTerm mapNode1 = (GOTerm) it.next();
			GOTerm mapNode1Clone = (GOTerm) mapNode1.clone();
			treeNode.addChild(mapNode1Clone);
			convertMapToTree(mapNode1, mapNode1Clone);
		}
	}
	
	class GOTreeModel implements TreeModel {
		private GOTerm rootTerm;
		
		public GOTreeModel(GOTerm root) {
			this.rootTerm = root;
		}
		
		public void addTreeModelListener(TreeModelListener l) {
			// Do nothing here.
		}

		public Object getChild(Object parent, int index) {
			GOTerm parentTerm = (GOTerm) parent;
			return parentTerm.getChild(index);
		}

		public int getChildCount(Object parent) {
			GOTerm parentTerm = (GOTerm) parent;
			return parentTerm.getChildCount();
		}

		public int getIndexOfChild(Object parent, Object child) {
			GOTerm parentTerm = (GOTerm) parent;
			GOTerm childTerm = (GOTerm) child;
			return parentTerm.getIndexOfChild(childTerm);
		}

		public Object getRoot() {
			return this.rootTerm;
		}

		public boolean isLeaf(Object node) {
			GOTerm term = (GOTerm)node;
			return term.isLeaf();
		}

		public void removeTreeModelListener(TreeModelListener l) {
			// Do nothing here.
		}

		public void valueForPathChanged(TreePath path, Object newValue) {
			// Not editable.
		}
	}
}
