/*
 * Created on Aug 11, 2003
 */
package org.gk.persistence;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.InstanceEdit;
import org.gk.model.Modification;
import org.gk.model.Reference;
import org.gk.model.Summation;
import org.gk.render.*;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This Persistence implementaion is used to persist Renderables to the local 
 * file system.
 * @author wgm
 */
public class FilePersistence {
	
	private static final String POST_FIX_TO_ENTITY_LIST = "ac";
	
	public FilePersistence() {
	}
	
	private void save(Renderable renderable,
					  Element nodeElm,
		              Element edgeElm) {
		Document doc = nodeElm.getOwnerDocument();
		List components = renderable.getComponents();
		for (Iterator it = components.iterator(); it.hasNext();) {
			Renderable tmp = (Renderable)it.next();
            if (tmp instanceof Shortcut) {
				Element elm = doc.createElement("shortcut");
				String type = null;
				if (tmp instanceof RenderableEntity)
					type = "entity";
				else if (tmp instanceof RenderableComplex)
					type = "complex";
				else if (tmp instanceof RenderableReaction)
					type = "reaction";
				else if (tmp instanceof RenderablePathway)
					type = "pathway";			
				else if (tmp instanceof ReactionNode)
					type = "reactionNode";		
				elm.setAttribute("type", type);
				// Set target
				Renderable target = ((Shortcut)tmp).getTarget();
				elm.setAttribute("target", target.getDisplayName());
				elm.setAttribute("id", tmp.getID() + "");
				saveNodeDisplay(elm, tmp);
				nodeElm.appendChild(elm);
			}
			else if (tmp instanceof RenderableEntity) {
				saveRenderableEntity(tmp, doc, nodeElm);
			}
			else if (tmp instanceof RenderableComplex) {
				saveRenderableComplex(tmp, doc, nodeElm);
			}
			else if (tmp instanceof RenderablePathway) {
				Element elm = createElement(doc, "pathway", tmp);
				saveNodeDisplay(elm, tmp);
				nodeElm.appendChild(elm);
				saveEvent(elm, tmp);
				saveEditInfo(elm, tmp);
				if (tmp.getComponents() != null
					&& tmp.getComponents().size() > 0) {
					Element nodeElm1 = doc.createElement("nodes");
					Element edgeElm1 = doc.createElement("edges");
					List list = tmp.getComponents();
					if (list != null && list.size() > 0)
						save(tmp, nodeElm1, edgeElm1);
					if (nodeElm1.getChildNodes().getLength() > 0)
						elm.appendChild(nodeElm1);
					if (edgeElm1.getChildNodes().getLength() > 0)
						elm.appendChild(edgeElm1);
				}
			}
			else if (tmp instanceof ReactionNode) {
				Element elm = createElement(doc, "reaction", tmp);
				saveNodeDisplay(elm, tmp);
				nodeElm.appendChild(elm);
				Element nodeElm1 = doc.createElement("nodes");
				Element edgeElm1 = doc.createElement("edges");
				List list = tmp.getComponents();
				if (list != null && list.size() > 0)
					save(tmp, nodeElm1, edgeElm1);
				if (nodeElm1.getChildNodes().getLength() > 0)
					elm.appendChild(nodeElm1);
				RenderableReaction reaction = ((ReactionNode)tmp).getReaction();
				if (reaction == null) 
					System.err.println("FilePersistence.save(): Empty Reaction: " + tmp.getDisplayName() + ", " + tmp.getID());
				Element elm1 = doc.createElement("reaction");
				elm1.setAttribute("displayName", tmp.getDisplayName());
				elm1.setAttribute("id", tmp.getID() + "");
                 if (reaction != null) {
                     saveEvent(elm1, reaction);
                     saveEditInfo(elm1, reaction);
                     createEdgeElement(reaction, elm1);
                 }
				edgeElm1.appendChild(elm1);
				elm.appendChild(edgeElm1);
			}
			else if (tmp instanceof RenderableReaction) {
                Element elm = createElement(doc, "reaction", tmp);
				saveEvent(elm, tmp);
				saveEditInfo(elm, tmp);
				createEdgeElement((RenderableReaction)tmp, elm);
				edgeElm.appendChild(elm);
			}
			else if (tmp instanceof FlowLine) {
				Element elm = doc.createElement("flowLine");
				elm.setAttribute("id", tmp.getID() + "");
				createEdgeElement((FlowLine)tmp, elm);
				edgeElm.appendChild(elm);
			}
		}
	}
	
	private void saveRenderableEntity(Renderable entity, Document doc, Element nodeElm) {
		Element elm = createElement(doc, "entity", entity);
		saveNodeDisplay(elm, entity);
		saveEntity(elm, entity);
		saveModification(elm, entity);
		saveEditInfo(elm, entity);
		saveDatabaseIdentifier(elm, entity);
		nodeElm.appendChild(elm);
	}
    
    private Element createElement(Document doc,
                                  String type,
                                  Renderable r) {
        Element elm = doc.createElement(type);
        elm.setAttribute("displayName", r.getDisplayName());
        elm.setAttribute("id", r.getID() + "");
        elm.setAttribute("isChanged", r.isChanged() + "");
        return elm;
    }
	
	private void saveRenderableComplex(Renderable complex, Document doc, Element nodeElm) {
		Element elm = createElement(doc, "complex", complex);
		saveNodeDisplay(elm, complex);
		nodeElm.appendChild(elm);
		saveEntity(elm, complex);
		saveEditInfo(elm, complex);
		// Need save summation and references
		appendSummationElm(complex, elm, doc);
		appendReferenceElm(complex, elm, doc);
		appendAttachmentElm(complex, elm, doc);
		List list = complex.getComponents();
		if (list != null && list.size() > 0) {
			Element nodeElm1 = doc.createElement("nodes");
			save(complex, nodeElm1, null);
            NodeList childNodes = nodeElm1.getChildNodes();
			if (childNodes != null && childNodes.getLength() > 0) {
				elm.appendChild(nodeElm1);
                // Append stoichiometries for subunits if needed
                RenderableComplex complex1 = (RenderableComplex) complex;
                for (Iterator it = list.iterator(); it.hasNext();) {
                    Renderable r= (Renderable) it.next();
                    int stoi = complex1.getStoichiometry(r);
                    if (stoi > 1) {
                        // Have to find the element
                        for (int i = 0; i < childNodes.getLength(); i++) {
                            Element elm1 = (Element) childNodes.item(i);
                            String target = null;
                            if (elm1.getNodeName().equals("shortcut")) 
                                target = elm1.getAttribute("target");
                            else
                                target = elm1.getAttribute("displayName");
                            if (target.equals(r.getDisplayName())) {
                                elm1.setAttribute("stoichiometry", stoi + "");
                                break;
                            }
                        }
                    }
                }
			}
		}
	}
	
	private void saveNodeDisplay(Element elm, Renderable renderable) {
		// Set position
		Point pos = renderable.getPosition();
		elm.setAttribute("position", pos.x + " " + pos.y);
		// Set bgColor
		if (renderable.getBackgroundColor() != null)
			elm.setAttribute("bgColor", convertToString(renderable.getBackgroundColor()));
		// Set fgColor
		if (renderable.getForegroundColor() != null)
			elm.setAttribute("fgColor", convertToString(renderable.getForegroundColor()));
	}
	
	private String convertToString(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		return r + " " + g + " " + b;
	}
	
	private Color convertToColor(String text) {
		StringTokenizer tokenizer = new StringTokenizer(text);
		int r = Integer.parseInt(tokenizer.nextToken());
		int g = Integer.parseInt(tokenizer.nextToken());
		int b = Integer.parseInt(tokenizer.nextToken());
		Color color = new Color(r, g, b);
		return color;
	}
	
	private void saveEntity(Element elm, Renderable entity) {
		// Save Properties
		Document doc = elm.getOwnerDocument();
		Element propsElm = doc.createElement("properties");
		saveNames(propsElm, doc, entity);
		appendPropElm("definition", entity, propsElm, doc);
		appendPropElm("taxon", entity, propsElm, doc);
        appendPropElm(RenderablePropertyNames.LOCALIZATION, 
                      entity, 
                      propsElm, 
                      doc);
		appendPropElm("DB_ID", 
                       entity,  
                       propsElm, 
                       doc);
		if (propsElm.getChildNodes().getLength() > 0)
			elm.appendChild(propsElm);		
	}
	
	private void saveDatabaseIdentifier(Element entityElm, Renderable entity) {
		DatabaseIdentifier identifier = (DatabaseIdentifier) entity.getAttributeValue("databaseIdentifier");
		if (identifier != null) {
			Document doc = entityElm.getOwnerDocument();
			Element identifierElm = doc.createElement("databaseIdentifier");
			if (identifier.getDB_ID() != null)
				identifierElm.setAttribute("DB_ID", identifier.getDB_ID() + "");
			if (identifier.getDbName() != null)
				identifierElm.setAttribute("referenceDB", identifier.getDbName());
			if (identifier.getAccessNo() != null)
				identifierElm.setAttribute("accessNo", identifier.getAccessNo());
			entityElm.appendChild(identifierElm);
		}
	}
	
	private void saveModification(Element entityElm, Renderable entity) {
		List modifications = (List) entity.getAttributeValue("modifications");
		if (modifications != null && modifications.size() > 0) {
			Document doc = entityElm.getOwnerDocument();
			Element mdfsElm = doc.createElement("modifications");
			Element mdfElm = null;
			Modification mdf = null;
			for (Iterator it = modifications.iterator(); it.hasNext();) {
				mdf = (Modification) it.next();
				mdfElm = doc.createElement("modification");
				if (mdf.getCoordinate() > -1) 
					mdfElm.setAttribute("coordinate", mdf.getCoordinate() + "");
				if (mdf.getResidue() != null)
					mdfElm.setAttribute("residue", mdf.getResidue());
				if (mdf.getModification() != null)
					mdfElm.setAttribute("modification", mdf.getModification());
				if (mdf.getModificationDbID() != null)
					mdfElm.setAttribute("dbID", mdf.getModificationDbID());
				if (mdf.getDB_ID() != null)
					mdfElm.setAttribute("DB_ID", mdf.getDB_ID() + "");
				mdfsElm.appendChild(mdfElm);
			}
			entityElm.appendChild(mdfsElm);
		}
	}
	
	private void saveEditInfo(Element elm, Renderable renderable) {
		Document doc = elm.getOwnerDocument();
		Element editInfoElm = doc.createElement("editInfo");
		InstanceEdit edit = (InstanceEdit) renderable.getAttributeValue("created");
		if (edit != null) {
			Element createdElm = doc.createElement("edit");
			createdElm.setAttribute("type", "created");
			boolean needAdded = false;
			if (edit.getAuthorName() != null) {
				needAdded = true;
				createdElm.setAttribute("author", edit.getAuthorName());
			}
			if (edit.getDate() != null) {
				needAdded = true;
				createdElm.setAttribute("date", edit.getDate());
			}
			if (needAdded) {
				editInfoElm.appendChild(createdElm);
			}
		}
		edit = (InstanceEdit) renderable.getAttributeValue("modified");
		if (edit != null) {
			Element modifiedElm = doc.createElement("edit");
			modifiedElm.setAttribute("type", "modified");
			boolean needAdded = false;
			if (edit.getAuthorName() != null) {
				needAdded = true;
				modifiedElm.setAttribute("author", edit.getAuthorName());
			}
			if (edit.getDate() != null) {
				needAdded = true;
				modifiedElm.setAttribute("date", edit.getDate());
			}
			if (needAdded) {
				editInfoElm.appendChild(modifiedElm);
			}
		}
		if (editInfoElm.getChildNodes().getLength() > 0)
			elm.appendChild(editInfoElm);
	}
	
	private void saveNames(Element propsElm, Document doc, Renderable renderable) {
		List names = (List) renderable.getAttributeValue("names");
		if (names != null && names.size() > 0) {
			StringBuffer buffer = new StringBuffer();
			int size = names.size(); 
			for (int i = 0; i < size; i++) {
				String name = (String) names.get(i);
				buffer.append(name);
				if (i < size - 1)
					buffer.append("; "); // Delimiter
			}
			Element propElm = doc.createElement("property");
			propElm.setAttribute("name", "names");
			propElm.setAttribute("value", buffer.toString());
			propsElm.appendChild(propElm);
		}		
	}
	
	private void saveEvent(Element elm, Renderable event) {
	    // Save Properties
	    Document doc = elm.getOwnerDocument(); // For create elements
	    Element propsElm = doc.createElement("properties");
	    saveNames(propsElm, doc, event);
	    appendPropElm("definition", event, propsElm, doc);
	    appendPropElm("goTerm", event, propsElm, doc);
	    appendPropElm("taxon", event, propsElm, doc);
	    appendPropElm("localization", event, propsElm, doc);
	    appendPropElm("isReversible", event, propsElm, doc);
	    appendPropElm("DB_ID", event, propsElm, doc);
	    if (propsElm.getChildNodes().getLength() > 0)
	        elm.appendChild(propsElm);
	    appendSummationElm(event, elm, doc);
	    appendReferenceElm(event, elm, doc);
	    appendAttachmentElm(event, elm, doc);
	    appendPrecedingEventElm(event, elm, doc);
	}
	
	private void appendPrecedingEventElm(Renderable event, Element parentElm, Document doc) {
		List precedingEvents = (List) event.getAttributeValue("precedingEvent");
		if (precedingEvents == null || precedingEvents.size() == 0)
			return;
		Element preEvtElm = doc.createElement("precedingEvents");
		for (Iterator it = precedingEvents.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			Element evtElm = doc.createElement("precedingEvent");
			evtElm.setAttribute("name", r.getDisplayName());
			preEvtElm.appendChild(evtElm);
		}
		parentElm.appendChild(preEvtElm);
	}
	
	private void appendReferenceElm(Renderable renderable, Element parentElm, Document doc) {
		List references = (List) renderable.getAttributeValue("references");
		if (references != null && references.size() > 0) {
			appendReferenceElm(references, parentElm, doc);
		}
	}
	
	private void appendReferenceElm(java.util.List references, Element parentElm, Document doc) {
		Element referencesElm = doc.createElement("references");
		parentElm.appendChild(referencesElm);
		Element elm;
		Reference reference;
		for (Iterator it = references.iterator(); it.hasNext();) {
			reference = (Reference) it.next();
			elm = doc.createElement("reference");
			if (reference.getDB_ID() != null)
				elm.setAttribute("DB_ID", reference.getDB_ID() + "");
			elm.setAttribute("PMID", reference.getPmid() + "");
			if (reference.getAuthor() != null)
				elm.setAttribute("author", reference.getAuthor());
			if (reference.getJournal() != null)
				elm.setAttribute("journal", reference.getJournal());
			elm.setAttribute("year", reference.getYear() + "");
			if (reference.getVolume() != null)
				elm.setAttribute("volume", reference.getVolume());
			if (reference.getPage() != null)
				elm.setAttribute("page", reference.getPage());
			if (reference.getTitle() != null)
				elm.setAttribute("title", reference.getTitle());
			referencesElm.appendChild(elm);
		}
	}
	
	private void appendSummationElm(Renderable renderable, Element parentElm, Document doc) {
		Summation summation = (Summation)renderable.getAttributeValue("summation");
		if (summation != null) {
			Element summationElm = doc.createElement("summation");
			if (summation.getDB_ID() != null) {
				summationElm.setAttribute("DB_ID", summation.getDB_ID() + "");
			}
            summationElm.setAttribute("isChanged", summation.isChanged() + "");
			if (summation.getText() != null && summation.getText().length() > 0) {
				Node textNode = doc.createTextNode(summation.getText());
                // Wrap with a text element: New with JDOM output
                Element textElm = doc.createElement("text");
                textElm.appendChild(textNode);
				summationElm.appendChild(textElm);
			}
			parentElm.appendChild(summationElm);
			// Need to append references
			if (summation.getReferences() != null)
				appendReferenceElm(summation.getReferences(), summationElm, doc);
		}
	}
	
	private void appendAttachmentElm(Renderable renderable, Element parentElm, Document doc) {
		java.util.List attachments = (java.util.List) renderable.getAttributeValue("attachments");
		if (attachments != null && attachments.size() > 0) {
			Element elm = doc.createElement("attachments");
			for (Iterator it = attachments.iterator(); it.hasNext();) {
				String name = (String) it.next();
				Element elm1 = doc.createElement("attachment");
				elm1.setAttribute("name", name);
				elm.appendChild(elm1);
			}
			parentElm.appendChild(elm);
		}
	}
	
	private void appendPropElm(String propName, Renderable renderable, Element propsElm, Document doc) {
		Object value = renderable.getAttributeValue(propName);
		if (value != null) {
			Element propElm = doc.createElement("property");
			propElm.setAttribute("name", propName);
			propElm.setAttribute("value", value.toString());
			propsElm.appendChild(propElm);
		}
	}
	
	private void createEdgeElement(HyperEdge reaction, Element elm) {
		boolean needStoi = false;
		if (reaction instanceof RenderableReaction)
			needStoi = true;
		// Backbone Points
		java.util.List points = reaction.getBackbonePoints();		
		StringBuffer buffer = new StringBuffer();
		Point p = null;
		for (int i = 0; i < points.size(); i++) {
			p = (Point) points.get(i);
			buffer.append(p.x + " " + p.y);
			if (i < points.size() - 1)
				buffer.append(", ");
		}	
		elm.setAttribute("points", buffer.toString());
		Point pos = reaction.getPosition();
		elm.setAttribute("position", pos.x + " " + pos.y);	
		elm.setAttribute("lineWidth", reaction.getLineWidth() + "");
		if (reaction.getForegroundColor() != null)
			elm.setAttribute("lineColor", convertToString(reaction.getForegroundColor()));
		// For inputs
		Document ownerDoc = elm.getOwnerDocument();
		java.util.List inputs = reaction.getInputNodes();
		if (inputs != null && inputs.size() > 0) {
			Element inputsElm = ownerDoc.createElement("inputs");
			for (Iterator it = inputs.iterator(); it.hasNext();) {
				Element inputElm = ownerDoc.createElement("input");
				Renderable input = (Renderable) it.next();
				inputElm.setAttribute("nodeName", input.getDisplayName());
				inputElm.setAttribute("id", input.getID() + "");
				if (needStoi) {
					int stoi = ((RenderableReaction)reaction).getInputStoichiometry(input);
					if (stoi != 1)
						inputElm.setAttribute("stoichiometry", stoi + "");
				}
				inputsElm.appendChild(inputElm);
			}
			elm.appendChild(inputsElm);
		}	
		java.util.List outputs = reaction.getOutputNodes();
		if (outputs != null && outputs.size() > 0) {
			Element outputsElm = ownerDoc.createElement("outputs");
			for (Iterator it = outputs.iterator(); it.hasNext();) {
				Element outputElm = ownerDoc.createElement("output");
				Renderable output = (Renderable) it.next();
				outputElm.setAttribute("nodeName", output.getDisplayName());
				outputElm.setAttribute("id", output.getID() + "");
				if (needStoi) {
					int stoi = ((RenderableReaction)reaction).getOutputStoichiometry(output);
					if (stoi != 1)
						outputElm.setAttribute("stoichiometry", stoi + "");
				}
				outputsElm.appendChild(outputElm);
			}
			elm.appendChild(outputsElm);
		}
		// Save catalysts
		java.util.List helpers = reaction.getHelperNodes();
		if (helpers != null && helpers.size() > 0) {
			Element helpersElm = ownerDoc.createElement("helpers");
			for (Iterator it = helpers.iterator(); it.hasNext();) {
				Element helperElm = ownerDoc.createElement("helper");
				Renderable helper = (Renderable) it.next();
				helperElm.setAttribute("nodeName", helper.getDisplayName());
				helperElm.setAttribute("id", helper.getID() + "");
				helpersElm.appendChild(helperElm);
			}
			elm.appendChild(helpersElm);
		}
		// Save inhibitors
		java.util.List inhibitors = reaction.getInhibitorNodes();
		if (inhibitors != null && inhibitors.size() > 0) {
			Element inhibitorsElm = ownerDoc.createElement("inhibitors");
			for (Iterator it = inhibitors.iterator(); it.hasNext();) {
				Element inhibitorElm = ownerDoc.createElement("inhibitor");
				Renderable inhibitor = (Renderable) it.next();
				inhibitorElm.setAttribute("nodeName", inhibitor.getDisplayName());
				inhibitorElm.setAttribute("id", inhibitor.getID() + "");
				inhibitorsElm.appendChild(inhibitorElm);
			}
			elm.appendChild(inhibitorsElm);
		}
		// Save activators
		java.util.List activators = reaction.getActivatorNodes();
		if (activators != null && activators.size() > 0) {
			Element activatorsElm = ownerDoc.createElement("activators");
			for (Iterator it = activators.iterator(); it.hasNext();) {
				Element activatorElm = ownerDoc.createElement("activator");
				Renderable activator = (Renderable) it.next();
				activatorElm.setAttribute("nodeName", activator.getDisplayName());
				activatorElm.setAttribute("id", activator.getID() + "");
				activatorsElm.appendChild(activatorElm);
			}
			elm.appendChild(activatorsElm);
		}
	}

	private void parseEventElm(Element eventElm, Renderable event) {
		NodeList list = eventElm.getChildNodes();
		int size = list.getLength(); 
		Node xmlNode = null;
		String nodeName = null;
		for (int i = 0; i < size; i++) {
			xmlNode = list.item(i);
			nodeName = xmlNode.getNodeName();
			if (nodeName.equals("properties")) {
				parsePropertiesElm((Element)xmlNode, event);
			}
			else if (nodeName.equals("summation")) 
				parseSummationElm((Element)xmlNode, event);
			else if (nodeName.equals("references")) 
				parseReferencesElm((Element)xmlNode, event);
			else if (nodeName.equals("editInfo"))
				parseEditInfoElm((Element)xmlNode, event);
			else if (nodeName.equals("attachments"))
				parseAttachmentElm((Element)xmlNode, event);
			else if (nodeName.equals("precedingEvents"))
				parsePrecedingEventsElm((Element)xmlNode, event);
		}
	}
	
	private void parsePrecedingEventsElm(Element xmlNode, Renderable event) {
		java.util.List list = getChildElements(xmlNode, "precedingEvent");
		if (list == null || list.size() == 0)
			return;
		java.util.List events = new ArrayList(list.size());
		for (int i = 0; i < list.size(); i++) {
			Element preEvtElm = (Element) list.get(i);
			String name = preEvtElm.getAttribute("name");
			events.add(name);
		}
		event.setAttributeValue("precedingEvent", events);
	}
	
	private void parseAttachmentElm(Element xmlNode, Renderable renderable) {
		NodeList list = xmlNode.getChildNodes();
		java.util.List attachments = new ArrayList();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeName().equals("attachment")) {
				Element elm = (Element) node;
				String name = elm.getAttribute("name");
				if (name != null && name.length() > 0)
					attachments.add(name);
			}
		}
		if (attachments.size() > 0)
			renderable.setAttributeValue("attachments", attachments);
	}
	
	private void parseEditInfoElm(Element xmlNode, Renderable renderable) {
		Node node = null;
		NodeList nodeList = xmlNode.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			node = nodeList.item(i);
			if (node.getNodeName().equals("edit")) {
				Element editElm = (Element) node;
				InstanceEdit edit = new InstanceEdit();
				String type = editElm.getAttribute("type");
				String author = editElm.getAttribute("author");
				String date = editElm.getAttribute("date");
				if (type.equals("created")) {
					if (author != null && author.length() > 0)
						edit.setAuthorName(author);
					if (date != null && date.length() > 0)
						edit.setDate(date);
					renderable.setAttributeValue("created", edit);
				}
				else if (type.equals("modified")) {
					if (author != null && author.length() > 0)
						edit.setAuthorName(author);
					if (date != null && date.length() > 0)
						edit.setDate(date);
					renderable.setAttributeValue("modified", edit);
				}
			}
		}
	}
	
	private void parsePropertiesElm(Element propsElm, Renderable renderable) {
		java.util.List propList = getChildElements(propsElm, "property");
		int size= propList.size();
		Element propElm = null;
		String propName;
		String value;
		for (int i = 0; i < size; i++) {
			propElm = (Element) propList.get(i);
			propName = propElm.getAttribute("name");
			value = propElm.getAttribute("value");
			if (propName.equals("names")) { // Need to parse the name list
				List nameList = new ArrayList();
				StringTokenizer tokenizer = new StringTokenizer(value, ";");
				while (tokenizer.hasMoreTokens()) {
					String name = tokenizer.nextToken().trim();
					nameList.add(name);
				}
				renderable.setAttributeValue("names", nameList);
			}
			else if (propName.equals("DB_ID")) {
				renderable.setAttributeValue("DB_ID", new Long(value));
			}
			else 
				renderable.setAttributeValue(propName, value);
		}
	}
	
	private void parseSummationElm(Element summationElm, Renderable renderable) {
		NodeList list = summationElm.getChildNodes();
		Summation summation = new Summation();
		String dbID = summationElm.getAttribute("DB_ID");
		if (dbID != null && dbID.length() > 0)
			summation.setDB_ID(new Long(dbID));
        String isChangedStr = summationElm.getAttribute("isChanged");
        if (isChangedStr != null && isChangedStr.length() > 0)
            summation.setIsChanged(Boolean.valueOf(isChangedStr).booleanValue());
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getNodeValue().trim();
                if (text.length() > 0)
                    // This is for old, non-formatted XML output
                    summation.setText(node.getNodeValue());
			}
            else if (node.getNodeName().equals("text")) {
                // This is for new, JDOM based XML output
                summation.setText(getText(node));
            }
			else if (node.getNodeName().equals("references")) {
				java.util.List references = parseReferencesElm((Element)node);
				if (references.size() > 0)
					summation.setReferences(references);
			}
		}
		if (!summation.isEmpty()) {
			renderable.setAttributeValue("summation", summation);
		}
	}
    
    private String getText(Node textNode) {
        NodeList list = textNode.getChildNodes();
        if (list == null)
            return null;
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.TEXT_NODE)
                return node.getNodeValue();
        }
        return null;
    }
	
	private java.util.List parseReferencesElm(Element refsElm) {
		NodeList list = refsElm.getChildNodes();
		int size = list.getLength(); 
		List references = new ArrayList();
		Reference reference = null;
		Element refElm;
		Node node;
        String value;
		for (int i = 0; i < size; i++) {
            node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
               continue;     
            refElm = (Element) list.item(i);
			reference = new Reference();
			value = refElm.getAttribute("DB_ID");
			if (value.length() > 0)
				reference.setDB_ID(new Long(value));
			value = refElm.getAttribute("PMID");
			if (value.length() > 0) 
				reference.setPmid(Integer.parseInt(value));	
			value = refElm.getAttribute("author");
			if (value.length() > 0)
				reference.setAuthor(value);
			references.add(reference);
			value = refElm.getAttribute("journal");
			if (value.length() > 0)
				reference.setJournal(value);
			value = refElm.getAttribute("year");
			if (value.length() > 0)
				reference.setYear(Integer.parseInt(value));
			value = refElm.getAttribute("volume");
			if (value.length() > 0)
				reference.setVolume(value);
			value = refElm.getAttribute("page");
			if (value.length() > 0)
			reference.setPage(value);
			value = refElm.getAttribute("title");
			if (value.length() > 0)
				reference.setTitle(value);
		}
		return references;
	}
	
	private void parseReferencesElm(Element refsElm, Renderable renderable) {
		java.util.List references = parseReferencesElm(refsElm);
		if (references.size() > 0)
			renderable.setAttributeValue("references", references);
	}
	
	private void openEdges(Node elm, Renderable renderable) {
		NodeList list = elm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getNodeName();
			if (name.equals("reaction")) {
				Element elm1 = (Element) node;
				RenderableReaction reaction = new RenderableReaction();
				String id = elm1.getAttribute("id");
				reaction.setID(Integer.parseInt(id));
				reaction.setContainer(renderable);
				reaction.setDisplayName(elm1.getAttribute("displayName"));
				parseEventElm(elm1, reaction);
				parseEdge(elm1, reaction, renderable);
				renderable.addComponent(reaction);
				// Have to check isReversible
				String isReversible = (String) reaction.getAttributeValue("isReversible");
				if (isReversible != null && isReversible.equals("true"))
					reaction.setNeedInputArrow(true);
			}
			else if (name.equals("flowLine")) {
				Element elm1 = (Element) node;
				FlowLine flowLine = new FlowLine();
				String id = elm1.getAttribute("id");
				flowLine.setID(Integer.parseInt(id));
				flowLine.setContainer(renderable);
				parseEdge(elm1, flowLine, renderable);
				renderable.addComponent(flowLine);
			}
		}
	}
	
	private void openEdgesFromNode(Node nodesElm, Renderable container) {
		NodeList list = nodesElm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getNodeName();
			if (name.equals("pathway")) {
				String cName = ((Element)node).getAttribute("displayName");
				Renderable container1 = RenderUtility.getComponentByName(container, cName);
				if (container1 instanceof Shortcut)
					container1 = ((Shortcut)container1).getTarget();
				NodeList list1 = node.getChildNodes();
				for (int j = 0; j < list1.getLength(); j++) {
					Node node1 = list1.item(j);
					if (node1.getNodeName().equals("nodes"))
						openEdgesFromNode(node1, container1);
					else if (node1.getNodeName().equals("edges"))
						openEdges(node1, container1);
				}
			}
			else if (name.equals("reaction")) {
				String cName = ((Element)node).getAttribute("displayName");
				Renderable container1 = RenderUtility.getComponentByName(container, cName);
				if (container1 instanceof Shortcut)
					container1 = ((Shortcut)container1).getTarget();
				NodeList list1 = node.getChildNodes();
				for (int j = 0; j < list1.getLength(); j++) {
					Node node1 = list1.item(j);
					if (node1.getNodeName().equals("edges")) {
						openEdges(node1, container1);
					}
				}
			}	
		}
	}
	
	/**
	 * A simplified openNode method for RenderableComplex. Here the stoichiometries
	 * should be checked for nodes.
	 */
	private void openNodeForComplex(Element elm, 
	                                RenderableComplex parent,
	                                Map shortcutMap) {
		NodeList list = elm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getNodeName();
			if (name.equals("entity")) {
				Element elm1 = (Element) node;
				Renderable entity = new RenderableEntity(elm1.getAttribute("displayName"));
				openNodeDisplay(elm1, entity);
				parseEntityElm(elm1, entity);
				parent.addComponent(entity);
                 String stoi = elm1.getAttribute("stoichiometry");
                 if (stoi != null && stoi.length() > 0)
                     parent.setStoichiometry(entity, Integer.parseInt(stoi));
				entity.setContainer(parent);
			}
			else if (name.equals("complex")) {
				Element elm1 = (Element) node;
				RenderableComplex complex = new RenderableComplex();
				complex.setDisplayName(elm1.getAttribute("displayName"));
				openNodeDisplay(elm1, complex);
				parseEventElm(elm1, complex);// Complex has similar information to event.
				parent.addComponent(complex);
                String stoi = elm1.getAttribute("stoichiometry");
                if (stoi != null && stoi.length() > 0)
                    parent.setStoichiometry(complex, Integer.parseInt(stoi));
                complex.setContainer(parent);
				java.util.List childNodes = getChildElements(elm1, "nodes");
				if (childNodes.size() > 0) {
					Element nodesElm = (Element) childNodes.get(0);
					openNodeForComplex(nodesElm, complex, shortcutMap);
				}
			}
			else if (name.equals("shortcut")) { 
				shortcutMap.put(node, parent);
			}
		}	
	}
	
	private void openNodeDisplay(Element elm, Renderable node) {
		node.setPosition(parsePosition(elm.getAttribute("position")));
		String text = elm.getAttribute("bgColor");
		if (text.length() > 0)
			node.setBackgroundColor(convertToColor(text));
		text = elm.getAttribute("fgColor");
		if (text.length() > 0)
			node.setForegroundColor(convertToColor(text));
	}
	
	private void openNode(Element elm, Renderable container, Map shortcutMap) {
		NodeList list = elm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getNodeName();
			if (name.equals("entity")) {
				Element elm1 = (Element) node;
				RenderableEntity entity = new RenderableEntity();
                openNode(elm1, entity, container);
				parseEntityElm(elm1, entity);
			}
			else if (name.equals("reaction")) {
				Element elm1 = (Element) node;
				ReactionNode reactionNode = new ReactionNode();
                openNode(elm1, reactionNode, container);
				parseReactionNode(elm1, reactionNode, shortcutMap);
			}
			else if (name.equals("complex")) {
				Element elm1 = (Element) node;
				RenderableComplex complex = new RenderableComplex();
				openNode(elm1, complex, container);
				parseEventElm(elm1, complex);// Complex has similar information to event.
				java.util.List childNodes = getChildElements(elm1, "nodes");
				if (childNodes.size() > 0) {
					Element nodesElm = (Element) childNodes.get(0);
					openNodeForComplex(nodesElm, complex, shortcutMap);
				}
			}
			else if (name.equals("pathway")) {
				Element elm1 = (Element) node;
				RenderablePathway pathway = new RenderablePathway();
                openNode(elm1, pathway, container);
				parseEventElm(elm1, pathway);
				parsePathwayNode(elm1, pathway, shortcutMap);
			}
			else if (name.equals("shortcut")) {
				shortcutMap.put(node, container);
			}
		}
	}
    
    private void openNode(Element elm, 
                          Renderable node,
                          Renderable container) {
        node.setDisplayName(elm.getAttribute("displayName"));
        String id = elm.getAttribute("id");
        node.setID(Integer.parseInt(id));
        openNodeDisplay(elm, node);
        loadIsChanged(node, elm);     
        container.addComponent(node);
        node.setContainer(container);
    }
	
	private void parseEntityElm(Element entityElm, Renderable entity) {
		NodeList list = entityElm.getChildNodes();
		int size = list.getLength(); 
		Node node = null;
		String nodeName = null;
		for (int i = 0; i < size; i++) {
			node = list.item(i);
			nodeName = node.getNodeName();
			if (nodeName.equals("properties"))
				parsePropertiesElm((Element)node, entity);
			else if (nodeName.equals("modifications"))
				parseModificationsElm((Element)node, entity);
			else if (nodeName.equals("editInfo"))
				parseEditInfoElm((Element)node, entity);
			else if (nodeName.equals("databaseIdentifier"))
				parseDatabaseIdentifierElm((Element)node, entity);
		}
	}
	
	private void parseDatabaseIdentifierElm(Element dbIdentifierElm, Renderable entity) {
		DatabaseIdentifier identifier = new DatabaseIdentifier();
		String value = dbIdentifierElm.getAttribute("DB_ID");
		if (value != null && value.length() > 0)
			identifier.setDB_ID(new Long(value));
		value = dbIdentifierElm.getAttribute("referenceDB");
		if (value != null && value.length() > 0)
			identifier.setDbName(value);
		value = dbIdentifierElm.getAttribute("accessNo");
		if (value != null && value.length() > 0)
			identifier.setAccessNo(value);
		entity.setAttributeValue("databaseIdentifier", identifier);
	}
	
	private void parseModificationsElm(Element mdfsElm, Renderable entity) {
		java.util.List list = getChildElements(mdfsElm, "modification");
		int size = list.size();
		if (size > 0) {
			List modifications = new ArrayList();
			Modification modification = null;
			Element mdfElm = null;
			String value = null;
			for (int i = 0; i < size; i++) {
				mdfElm = (Element) list.get(i);
				modification = new Modification();
				value = mdfElm.getAttribute("coordinate");
				if (value.length() > 0) 
					modification.setCoordinate(Integer.parseInt(value));
				value = mdfElm.getAttribute("residue");
				if (value.length() > 0)
					modification.setResidue(value);
				value = mdfElm.getAttribute("modification");
				if (value.length() > 0)
					modification.setModification(value);
				value = mdfElm.getAttribute("dbID");
				if (value.length() > 0)
					modification.setModificationDbID(value);
				value = mdfElm.getAttribute("DB_ID");
				if (value.length() > 0)
					modification.setDB_ID(new Long(value));
				modifications.add(modification);
			}
			entity.setAttributeValue("modifications", modifications);
		}
	}
	
	private void parsePathwayNode(Element pathwayElm, 
	                              RenderablePathway pathway,
	                              Map shortcutMap) {
		NodeList list = pathwayElm.getChildNodes();
		int len = list.getLength();
		for (int i = 0; i < len; i++) {
			Node node = list.item(i);
			String nodeName = node.getNodeName();
			if (nodeName.equals("nodes")) {
				openNode((Element)node, pathway, shortcutMap);
			}
		}
	}
	
	private void parseReactionNode(Element nodeElm, 
	                               ReactionNode node,
	                               Map shortcutMap) {
		NodeList list = nodeElm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node xmlNode = list.item(i);
			String nodeName = xmlNode.getNodeName();
			if (nodeName.equals("nodes")) {
				openNode((Element)xmlNode, node, shortcutMap);
			}
		}
	}
	
	private void parseEdge(Element rxtElm, HyperEdge reaction, Renderable container) {
		// Get the backbone points
		String positionStr = rxtElm.getAttribute("position");
		String text = rxtElm.getAttribute("lineColor");
		if (text.length() > 0)
			reaction.setForegroundColor(convertToColor(text));
		text = rxtElm.getAttribute("lineWidth");
		if (text.length() > 0)
			reaction.setLineWidth(Float.parseFloat(text));
		String pointsStr = rxtElm.getAttribute("points");
		StringTokenizer tokenizer = new StringTokenizer(pointsStr, ",");
		java.util.List points = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			String pointStr = tokenizer.nextToken().trim();
			Point point = parsePosition(pointStr);
			points.add(parsePosition(pointStr));
			if (pointStr.equals(positionStr))
				reaction.setPosition(point);
		}
		reaction.setBackbonePoints(points);
		// Attach inputs, outputs and helpers
		NodeList list = rxtElm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String nodeName = node.getNodeName();
			if (nodeName.equals("inputs")) {
				Element inputsElm = (Element) node;
				java.util.List inputList = getChildElements(inputsElm, "input");
				int length = inputList.size();
				if (length == 1) { // The minium should be 1
					Element inputElm = (Element)inputList.get(0);
					String id = inputElm.getAttribute("id");
					Renderable input = container.getComponentByID(Integer.parseInt(id));
					if (input != null) {
						Point pos = (Point) points.get(0);
						Point controlPoint = (Point) points.get(1);
						ConnectWidget widget = new ConnectWidget(pos, controlPoint, HyperEdge.INPUT, 0);
						widget.setEdge(reaction);
						widget.setConnectedNode((org.gk.render.Node)input); // Should be safe to cast to node.
						String stoi = inputElm.getAttribute("stoichiometry");
						if (stoi.length() > 0)
							widget.setStoichiometry(Integer.parseInt(stoi));
						widget.connect();
						widget.invalidate();
					}
				}
				else {
					Point controlP = reaction.getInputHub();
					java.util.List inputPoints = new ArrayList();
					reaction.setInputPoints(inputPoints);
					for (int j = 0; j < length; j++) {
						Element inputElm = (Element) inputList.get(j);
						String id = inputElm.getAttribute("id");
						Renderable input = container.getComponentByID(Integer.parseInt(id));
						if (input != null) {
							Point pos = (Point) input.getPosition().clone();
							java.util.List inputBranch = new ArrayList();
							inputBranch.add(pos);
							inputPoints.add(inputBranch);
							ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.INPUT, j);
							widget.setEdge(reaction);
							widget.setConnectedNode((org.gk.render.Node)input);
							String stoi = inputElm.getAttribute("stoichiometry");
							if (stoi.length() > 0)
								widget.setStoichiometry(Integer.parseInt(stoi));
							widget.connect();
							widget.invalidate();
						}
					}
				}
			}
			else if (nodeName.equals("outputs")) {
				Element outputsElm = (Element) node;
				java.util.List outputList = getChildElements(outputsElm, "output");
				int length = outputList.size();
				if (length == 1) { // The minimum length
					Element outputElm = (Element) outputList.get(0);
					String id = outputElm.getAttribute("id");
					Renderable output = container.getComponentByID(Integer.parseInt(id));
					if (output != null) {
						// Get ConnectWidget position
						int size = points.size();
						Point pos = (Point) points.get(size - 1);
						Point controlP = (Point) points.get(size - 2);
						ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.OUTPUT, 0);
						widget.setEdge(reaction);
						widget.setConnectedNode((org.gk.render.Node)output);
						String stoi = outputElm.getAttribute("stoichiometry");
						if (stoi.length() > 0)
							widget.setStoichiometry(Integer.parseInt(stoi));
						widget.connect();
						widget.invalidate();
					}
				}
				else {
					Point controlP = reaction.getOutputHub();
					java.util.List outputPoints = new ArrayList();
					reaction.setOutputPoints(outputPoints);
					for (int j = 0; j < length; j++) {
						Element outputElm = (Element) outputList.get(j);
						String id = outputElm.getAttribute("id");
						Renderable output = container.getComponentByID(Integer.parseInt(id));
						if (output != null) {
							Point pos = (Point) output.getPosition().clone();
							java.util.List outputBranch = new ArrayList();
							outputBranch.add(pos);
							outputPoints.add(outputBranch);
							ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.OUTPUT, j);
							widget.setEdge(reaction);
							widget.setConnectedNode((org.gk.render.Node)output);
							String stoi = outputElm.getAttribute("stoichiometry");
							if (stoi.length() > 0)
								widget.setStoichiometry(Integer.parseInt(stoi));
							widget.connect();
							widget.invalidate();
						}
					}
				}
			}
			else if (nodeName.equals("helpers")) {
				Element helpersElm = (Element) node;
				java.util.List helperList = getChildElements(helpersElm, "helper");
				int length = helperList.size();
				java.util.List helperPoints = new ArrayList();
				reaction.setHelperPoints(helperPoints);
				Point controlP = reaction.getPosition();
				for (int j = 0; j < length; j++) {
					Element helperElm = (Element) helperList.get(j);
					String id = helperElm.getAttribute("id");
					Renderable helper = container.getComponentByID(Integer.parseInt(id));
					if (helper != null) {
						Point pos = (Point) helper.getPosition().clone();
						java.util.List helperbranch = new ArrayList();
						helperbranch.add(pos);
						helperPoints.add(helperbranch);
						ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.CATALYST, j);
						widget.setEdge(reaction);
						widget.setConnectedNode((org.gk.render.Node)helper);
						widget.connect();
						widget.invalidate();
					}
				}
			}
			else if (nodeName.equals("inhibitors")) {
				Element inhibitorsElm = (Element) node;
				java.util.List inhibitorList = getChildElements(inhibitorsElm, "inhibitor");
				int length = inhibitorList.size();
				java.util.List inhibitorPoints = new ArrayList();
				reaction.setInhibitorPoints(inhibitorPoints);
				Point controlP = reaction.getPosition();
				for (int j = 0; j < length; j++) {
					Element helperElm = (Element) inhibitorList.get(j);
					String id = helperElm.getAttribute("id");
					Renderable inhibitor = container.getComponentByID(Integer.parseInt(id));
					if (inhibitor != null) {
						Point pos = (Point) inhibitor.getPosition().clone();
						java.util.List inhibitorBranch = new ArrayList();
						inhibitorBranch.add(pos);
						inhibitorPoints.add(inhibitorBranch);
						ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.INHIBITOR, j);
						widget.setEdge(reaction);
						widget.setConnectedNode((org.gk.render.Node)inhibitor);
						widget.connect();
						widget.invalidate();
					}
				}				
			}
			else if (nodeName.equals("activators")) {
				Element activatorsElm = (Element) node;
				java.util.List activatorList = getChildElements(activatorsElm, "activator");
				int length = activatorList.size();
				java.util.List activatorPoints = new ArrayList();
				reaction.setActivatorPoints(activatorPoints);
				Point controlP = reaction.getPosition();
				for (int j = 0; j < length; j++) {
					Element activatorElm = (Element) activatorList.get(j);
					String id = activatorElm.getAttribute("id");
					Renderable activator = container.getComponentByID(Integer.parseInt(id));
					if (activator != null) {
						Point pos = (Point) activator.getPosition().clone();
						java.util.List activatorBranch = new ArrayList();
						activatorBranch.add(pos);
						activatorPoints.add(activatorBranch);
						ConnectWidget widget = new ConnectWidget(pos, controlP, HyperEdge.ACTIVATOR, j);
						widget.setEdge(reaction);
						widget.setConnectedNode((org.gk.render.Node)activator);
						widget.connect();
						widget.invalidate();
					}
				}
			}
		}
	}
	
	private Point parsePosition(String position) {
		Point p = new Point();
		int index = position.indexOf(" ");
		String xPos = position.substring(0, index);
		String yPos = position.substring(index + 1);
		p.x = Integer.parseInt(xPos);
		p.y = Integer.parseInt(yPos);
		return p;
	}
	
	public boolean save(Project project, String dest) throws Exception {
		project.setSourceName(dest);
		Renderable process = project.getProcess();
		Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		document = builder.newDocument();
		if (document == null) {
			return false;
		}
		Element root = document.createElement("process");
		root.setAttribute("displayName", process.getDisplayName());
        root.setAttribute("isChanged", process.isChanged() + "");
		document.appendChild(root);
		saveEvent(root, process);
		saveEditInfo(root, process);
		Element nodeElm = document.createElement("nodes");
		Element edgeElm = document.createElement("edges");
		List components = process.getComponents();
		if (components != null && components.size() > 0)
			save(process, nodeElm, edgeElm);
		if (nodeElm.getChildNodes().getLength() > 0)
			root.appendChild(nodeElm);
		if (edgeElm.getChildNodes().getLength() > 0)
			root.appendChild(edgeElm);
		// Handle tasks here.
		Element tasksElm = document.createElement("tasks");
		saveTasks(tasksElm, project);
		if (tasksElm.getChildNodes().getLength() > 0)
			root.appendChild(tasksElm);
        outputDocument(document, dest);
//		FileOutputStream xmlOut = new FileOutputStream(dest);
//		TransformerFactory tffactory = TransformerFactory.newInstance();
//		Transformer transformer = tffactory.newTransformer();
//		DOMSource source = new DOMSource(document);
//		StreamResult result = new StreamResult(xmlOut);
//		transformer.transform(source, result);
		// To save listed entities
		if (project.getListedEntities() == null || project.getListedEntities().size() == 0) {
			String entityFile = dest + POST_FIX_TO_ENTITY_LIST; // For accessory file
			File file = new File(entityFile);
			if (file.exists())
				file.delete();
		}
		else {
			saveListedEntities(project.getListedEntities(), dest);
		}
		return true;
	}
    
    private void outputDocument(Document document,
                                String destFileName) throws Exception {
        DOMBuilder dombuilder = new DOMBuilder();
        org.jdom.Document jdomDoc = dombuilder.build(document);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream xmlOut = new FileOutputStream(destFileName);
        outputter.output(jdomDoc, xmlOut);
    }
	
	private void saveListedEntities(Map listedEntities, String dest) throws Exception {
		Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		document = builder.newDocument();
		Element root = document.createElement("EntityList");
		document.appendChild(root);
		java.util.List list = (java.util.List)listedEntities.get("Complexes");
		if (list != null && list.size() > 0) {
			Element elm = document.createElement("complexes");
			for (Iterator it = list.iterator(); it.hasNext();) {
				Renderable r = (Renderable)it.next();
				saveRenderableComplex(r, document, elm);
			}
			root.appendChild(elm);
		}
		list = (java.util.List)listedEntities.get("Proteins");
		if (list != null && list.size() > 0) {
			Element elm = document.createElement("proteins");
			for (Iterator it = list.iterator(); it.hasNext();) {
				Renderable r = (Renderable)it.next();
				saveRenderableEntity(r, document, elm);
			}
			root.appendChild(elm);
		}
		list = (java.util.List)listedEntities.get("Small Molecules");
		if (list != null && list.size() > 0) {
			Element elm = document.createElement("smallMolecules");
			for (Iterator it = list.iterator(); it.hasNext();) {
				Renderable r = (Renderable)it.next();
				saveRenderableEntity(r, document, elm);
			}
			root.appendChild(elm);
		}
		// Save document
		FileOutputStream xmlOut = new FileOutputStream(dest + POST_FIX_TO_ENTITY_LIST);
		TransformerFactory tffactory = TransformerFactory.newInstance();
		Transformer transformer = tffactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(xmlOut);
		transformer.transform(source, result);
	}
	
	private void saveTasks(Element tasksElm, Project project) {
		Document doc = tasksElm.getOwnerDocument();
		List tasks = null;
		Renderable renderable = null;
		// Save removed tasks.
		Map removedTasks = project.getRemovedTasks();
		if (removedTasks != null && removedTasks.size() > 0) {
			Element rmTaskElm = doc.createElement("removedTasks");
			// Key: Renderable; Value: A list of task labels.
			for (Iterator it = removedTasks.keySet().iterator(); it.hasNext();) {
				renderable = (Renderable) it.next();
				tasks = (List) removedTasks.get(renderable);
				if (tasks != null && tasks.size() > 0) {
					for (Iterator it1 = tasks.iterator(); it1.hasNext();) {
						Element taskElm = doc.createElement("task");
						taskElm.setAttribute("target", renderable.getDisplayName());
						taskElm.setAttribute("label", it1.next().toString());
						rmTaskElm.appendChild(taskElm);
					}
				}
			}
			if (rmTaskElm.getChildNodes().getLength() > 0)
				tasksElm.appendChild(rmTaskElm);
		}
		// Save user defined tasks
		Map definedTasks = project.getDefinedTasks();
		if (definedTasks != null && definedTasks.size() > 0) {
			Element definedTaskElm = doc.createElement("definedTasks");
			// Key: Renderable; Value: A list of task labels
			for (Iterator it = definedTasks.keySet().iterator(); it.hasNext();) {
				renderable = (Renderable) it.next();
				tasks = (List) definedTasks.get(renderable);
				if (tasks != null && tasks.size() > 0) {
					for (Iterator it1 = tasks.iterator(); it1.hasNext();) {
						GKTask task = (GKTask) it1.next();
						Element taskElm = doc.createElement("task");
						taskElm.setAttribute("target", renderable.getDisplayName());
						taskElm.setAttribute("description", task.getDescription());
						definedTaskElm.appendChild(taskElm);
					}
				}
			}
			if (definedTaskElm.getChildNodes().getLength() > 0)
				tasksElm.appendChild(definedTaskElm);
		}
	}
	
    private void loadIsChanged(Renderable r,
                               Element elm) {
        String isChangedStr = elm.getAttribute("isChanged");
        if (isChangedStr == null || isChangedStr.length() == 0)
            return;
        r.setIsChanged(Boolean.valueOf(isChangedStr).booleanValue());
    }
    
	public Project open(String source) {
		RenderablePathway process = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(source);
			Element root = doc.getDocumentElement();
			process = new RenderablePathway(root.getAttribute("displayName"));
            loadIsChanged(process, root);
			parseEventElm(root, process);
			// Create two branches: One for nodes another for edges
			NodeList list = root.getChildNodes();
			// Shortcuts should be opened at last since their targets
			// must be resolved after these targets are loaded. Keep these
			// Shortcuts in a map: Keys Shortcut XML Element, Values: Shortcuts'
			// container Renderable objects.
			Map shortcutMap = new HashMap();
			// First open all nodes
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeName().equals("nodes")) {
					openNode((Element)node, process, shortcutMap);
				}
			}
			// Second shortcuts now
			openShortcuts(shortcutMap);
			// Third open edges
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeName().equals("nodes")) {
					openEdgesFromNode((Element)node, process);
				}
				if (node.getNodeName().equals("edges"))
					openEdges(node, process);
			}
             // Reset the next id for Process
			process.resetNextID();
			Project project = new Project();
			project.setProcess(process);
			project.setSourceName(source);
			// Register all Renderable objects in the process.
            RenderableRegistry.getRegistry().registerAll(process);
			// Convert the names in the precedingEvent list to nodes
			List allEvents = RenderUtility.getAllEvents(process);
			// Conver to map to increase the performance
			Map eventMap = new HashMap();
			for (Iterator it = allEvents.iterator(); it.hasNext();) {
				Renderable event = (Renderable) it.next();
				eventMap.put(event.getDisplayName(), event);
			}
			for (Iterator it = allEvents.iterator(); it.hasNext();) {
				Renderable event = (Renderable) it.next();
				List names = (List) event.getAttributeValue("precedingEvent");
				if (names != null && names.size() > 0) {
					List events = new ArrayList(names.size());
					for (Iterator it1 = names.iterator(); it1.hasNext();) {
						String name = (String) it1.next();
						Renderable event1 = (Renderable) eventMap.get(name);
						events.add(event1);
					}
					event.setAttributeValue("precedingEvent", events);
				}
			}
			// Open tasks
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeName().equals("tasks")) {
					openTasks((Element)node, project);
					break;
				}
			}
			// Read the listed entities
			File file = new File(source + POST_FIX_TO_ENTITY_LIST);
			if (file.exists()) {
				doc = builder.parse(file);
				openListedEntities(project, doc);
			}
			return project;
        }
		catch(Exception e) {
			System.err.println("FilePersistence.open(): " + e);
			e.printStackTrace();
		}
		return null;	
	}
	
	private void openListedEntities(Project project, Document doc) {
		Map map = new HashMap();
		Element root = doc.getDocumentElement();
		NodeList nodeList = root.getChildNodes();
		Map shortcutMap = new HashMap();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeName().equals("complexes")) {
				java.util.List list = new ArrayList();
				java.util.List nodeList1 = getChildElements(node, "complex");
				for (int j = 0; j < nodeList1.size(); j++) {
                    Element complexElm = (Element) nodeList1.get(j);
                    RenderableComplex complex = new RenderableComplex();
                    complex.setDisplayName(complexElm.getAttribute("displayName"));
                    openNodeDisplay(complexElm, complex);
                    parseEventElm(complexElm, complex); // Complex has similar information to event.
                    java.util.List childNodes = getChildElements(complexElm, "nodes");
                    if (childNodes.size() > 0) {
                        Element nodesElm = (Element) childNodes.get(0);
                        shortcutMap.clear();
                        openNodeForComplex(nodesElm, complex, shortcutMap);
                    }
                    complex.setAttributeValue("type", "complex");
                    list.add(complex);
                }
                map.put("Complexes", list);
			}
			else if (node.getNodeName().equals("proteins")) {
				java.util.List list = new ArrayList();
				java.util.List nodeList1 = getChildElements(node, "entity");
				for (int j = 0; j < nodeList1.size(); j++) {
					Element proteinElm = (Element) nodeList1.get(j);
					Renderable protein = new RenderableEntity(proteinElm.getAttribute("displayName"));
					openNodeDisplay(proteinElm, protein);
					parseEntityElm(proteinElm, protein);
					protein.setAttributeValue("type", "protein");
					list.add(protein);
				}
				map.put("Proteins", list);
			}
			else if (node.getNodeName().equals("smallMolecules")) {
				java.util.List list = new ArrayList();
				java.util.List nodeList1 = getChildElements(node, "entity");
				for (int j = 0; j < nodeList1.size(); j++) {
					Element proteinElm = (Element) nodeList1.get(j);
					Renderable protein = new RenderableEntity(proteinElm.getAttribute("displayName"));
					openNodeDisplay(proteinElm, protein);
					parseEntityElm(proteinElm, protein);
					protein.setAttributeValue("type", "smallMolecule");
					list.add(protein);
				}
				map.put("Small Molecules", list);				
			}
		}
		project.setListedEntities(map);
	}
	
	private void openShortcuts(Map shortcutMap) {
		if (shortcutMap.size() == 0)
			return;
		Element shortcutElm = null;
		Renderable container = null;
		Renderable target = null;
        for (Iterator it = shortcutMap.keySet().iterator(); it.hasNext();) {
			shortcutElm = (Element) it.next();
			container = (Renderable) shortcutMap.get(shortcutElm);
			String targetName = shortcutElm.getAttribute("target");
			target = RenderUtility.getComponentByName(container, targetName);
			if (target == null) {
				System.err.println("FilePersistence.openShortcuts(): target not found: " + targetName);
				continue; // It should not happened!
			}
			if (target instanceof Shortcut)
				target = ((Shortcut)target).getTarget();
			Renderable shortcut = (Renderable) target.generateShortcut();
			String id = shortcutElm.getAttribute("id");
			shortcut.setID(Integer.parseInt(id));
			openNodeDisplay(shortcutElm, shortcut);
			if (!checkCircular(container, shortcut)) {
			    container.addComponent(shortcut);
			    if (container instanceof RenderableComplex) {
			        String stoi = shortcutElm.getAttribute("stoichiometry");
			        if (stoi != null && stoi.length() > 0) {
			            RenderableComplex complexContainer = (RenderableComplex) container;
			            complexContainer.setStoichiometry(shortcut, Integer.parseInt(stoi));
			        }
			    }
			    shortcut.setContainer(container);
			}
		}
	}

    private boolean checkCircular(Renderable container, Renderable contained) {
        Renderable r = container;
        while (r != null) {
            String displayName = contained.getDisplayName();
            if (container.getDisplayName().equals(displayName)) {
                System.err.println("found: " + displayName);
                return true;
            }
            r = r.getContainer();
        }
        // Need to check contained
        if (contained.getComponents() != null) {
            for (Iterator it = contained.getComponents().iterator(); it.hasNext();) {
                Renderable tmp = (Renderable) it.next();
                if (tmp instanceof FlowLine ||
                    tmp instanceof RenderableEntity ||
                    tmp instanceof ReactionNode ||
                    tmp instanceof RenderableReaction)
                    continue;
                if(checkCircular(container, tmp))
                    return true;
            }
        }
        return false;
    }
	
	private void openTasks(Element tasksElm, Project project) {
		NodeList list = tasksElm.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeName().equals("removedTasks")) 
				openRemovedTasks((Element)node, project);
			else if (node.getNodeName().equals("definedTasks")) 
				openDefinedTasks((Element)node, project);
		}
	}
	
	private void openDefinedTasks(Element definedTasksElm, Project project) {
		List taskElms = getChildElements(definedTasksElm, "task");
		Map map = new HashMap();
		int size = taskElms.size();
		for (int j = 0; j < size; j++) {
			Element taskElm = (Element) taskElms.get(j);
			String targetName = taskElm.getAttribute("target");
			String desc = taskElm.getAttribute("description");
			List list = (List)map.get(targetName);
			if (list == null) {
				list = new ArrayList();
				map.put(targetName, list);
			}
			list.add(desc);
		}		
		// Change the type of key in the map from String to Renderable
		Renderable process = project.getProcess();
		Map taskMap = new HashMap();
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			String targetName = (String) it.next();
			List list = (List) map.get(targetName);
			Renderable target = RenderUtility.getComponentByName(process, targetName);
			if (target instanceof Shortcut)
				target = ((Shortcut)target).getTarget();
			if (target != null) {
				List taskList = new ArrayList();
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					String desc = (String) it1.next();
					taskList.add(new GKTask(target, null, desc, true));
				}
				if (target instanceof ReactionNode)
					target = ((ReactionNode)target).getReaction();
				taskMap.put(target, taskList);
			}
		}
		project.setDefinedTasks(taskMap);
	}
	
	private void openRemovedTasks(Element rmTasksElm, Project project) {
		java.util.List taskElms = getChildElements(rmTasksElm, "task");
		Map map = new HashMap();
		int size = taskElms.size();
		for (int j = 0; j < size; j++) {
			Element taskElm = (Element) taskElms.get(j);
			String targetName = taskElm.getAttribute("target");
			String label = taskElm.getAttribute("label");
			List list = (List)map.get(targetName);
			if (list == null) {
				list = new ArrayList();
				map.put(targetName, list);
			}
			list.add(label);
		}		
		// Change the type of key in the map from String to Renderable
		Renderable process = project.getProcess();
		Map taskMap = new HashMap();
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			String targetName = (String) it.next();
			List list = (List) map.get(targetName);
			Renderable target = RenderUtility.getComponentByName(process, targetName);
			if (target instanceof Shortcut)
				target = ((Shortcut)target).getTarget();
			if (target != null) {
				if (target instanceof ReactionNode)
					target = ((ReactionNode)target).getReaction();
				taskMap.put(target, list);
			}
		}
		project.setRemovedTasks(taskMap);
	}
	
	private java.util.List getChildElements(Node parentNode, String tagName) {
	    java.util.List list = new ArrayList();
	    NodeList nodeList = parentNode.getChildNodes();
	    for(int i = 0; i < nodeList.getLength(); i++) {
	        Node node = nodeList.item(i);
	        if ((node instanceof Element) && 
	           node.getNodeName().equals(tagName))
	            list.add(node);
	    }
	    return list;
	}
}
