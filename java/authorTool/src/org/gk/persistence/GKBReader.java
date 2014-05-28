/*
 * Created on Jan 5, 2007
 *
 */
package org.gk.persistence;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.Modification;
import org.gk.model.Reference;
import org.gk.model.Summation;
import org.gk.render.*;
import org.gk.render.RenderableFeature.FeatureType;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

/**
 * This class is used to read gkb files from a file system.
 * @author guanming
 *
 */
public class GKBReader implements RenderablePropertyNames {
    
    public GKBReader() {
    }
    
    private Color convertToColor(String text) {
        StringTokenizer tokenizer = new StringTokenizer(text);
        int r = Integer.parseInt(tokenizer.nextToken());
        int g = Integer.parseInt(tokenizer.nextToken());
        int b = Integer.parseInt(tokenizer.nextToken());
        Color color = new Color(r, g, b);
        return color;
    }
    
    private void parseAttachmentElm(org.jdom.Element xmlNode, 
                                    Renderable renderable) {
        List list = xmlNode.getChildren();
        int size = list.size();
        java.util.List attachments = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            org.jdom.Element elm = (org.jdom.Element) list.get(i);
            String name = elm.getText();
            attachments.add(name);
        }
        if (attachments.size() > 0)
            renderable.setAttributeValue(ATTACHMENT, attachments);
    }
    
    private void parseSummationElm(org.jdom.Element summationElm,
                                   Renderable renderable) {
        Summation summation = new Summation();
        String dbID = summationElm.getAttributeValue(DB_ID);
        if (dbID != null && dbID.length() > 0)
            summation.setDB_ID(new Long(dbID));
        String isChangedStr = summationElm.getAttributeValue("isChanged");
        if (isChangedStr != null && isChangedStr.length() > 0)
            summation.setIsChanged(Boolean.valueOf(isChangedStr).booleanValue());
        List children = summationElm.getChildren();
        for (int i = 0; i < children.size(); i++) {
            org.jdom.Element child = (org.jdom.Element) children.get(i);
            String elmName = child.getName();
            if (elmName.equals("Text"))
                summation.setText(child.getText());
            else if (elmName.equals(REFERENCE)) {
                List references = parseReferencesElm(child);
                if (references != null)
                    summation.setReferences(references);
            }
        }
        if (!summation.isEmpty()) {
            renderable.setAttributeValue("summation", summation);
        }
    }
    
    private java.util.List parseReferencesElm(org.jdom.Element refsElm) {
        List list = refsElm.getChildren();
        int size = list.size(); 
        List references = new ArrayList(size);
        Reference reference = null;
        org.jdom.Element refElm;
        String value;
        for (int i = 0; i < size; i++) {
            refElm = (org.jdom.Element) list.get(i);
            reference = new Reference();
            value = refElm.getAttributeValue("DB_ID");
            if (value != null && value.length() > 0)
                reference.setDB_ID(new Long(value));
            value = refElm.getAttributeValue("PMID");
            if (value != null && value.length() > 0) 
                reference.setPmid(Integer.parseInt(value)); 
            value = refElm.getAttributeValue("author");
            if (value != null && value.length() > 0)
                reference.setAuthor(value);
            references.add(reference);
            value = refElm.getAttributeValue("journal");
            if (value != null && value.length() > 0)
                reference.setJournal(value);
            value = refElm.getAttributeValue("year");
            if (value != null && value.length() > 0)
                reference.setYear(Integer.parseInt(value));
            value = refElm.getAttributeValue("volume");
            if (value != null && value.length() > 0)
                reference.setVolume(value);
            value = refElm.getAttributeValue("page");
            if (value != null && value.length() > 0)
                reference.setPage(value);
            value = refElm.getAttributeValue("title");
            if (value != null && value.length() > 0)
                reference.setTitle(value);
        }
        return references;
    }
    
    private void parseNodeDisplayInfo(Renderable node,
                                      org.jdom.Element nodeElm) {
        node.setPosition(parsePosition(nodeElm.getAttributeValue("position")));
        String text = nodeElm.getAttributeValue("bgColor");
        if (text != null && text.length() > 0)
            node.setBackgroundColor(convertToColor(text));
        else
            node.setBackgroundColor(null); // In case shortcuts, which may take value from target
        text = nodeElm.getAttributeValue("fgColor");
        if (text != null && text.length() > 0)
            node.setForegroundColor(convertToColor(text));
        else
            node.setForegroundColor(null);
        text = nodeElm.getAttributeValue("lineColor");
        if (text != null && text.length() > 0) 
            node.setLineColor(convertToColor(text));
        else
            node.setLineColor(null);
        text = nodeElm.getAttributeValue("lineWidth");
        if (text != null && text.length() > 0)
            node.setLineWidth(new Float(text));
        else
            node.setLineWidth(null);
        if (node instanceof Node) {
            text = nodeElm.getAttributeValue("needDashedBorder");
            if (text != null && text.length() > 0)
                ((Node)node).setNeedDashedBorder(new Boolean(text)); // Should be used only for node
        }
        // Get the bounds
        String boundsStr = nodeElm.getAttributeValue("bounds");
        if (boundsStr != null) {
            Rectangle bounds = parseBoundsInfo(boundsStr);
            node.setBounds(bounds);
        }
        String textPos = nodeElm.getAttributeValue("textPosition");
        if (textPos != null) {
            String[] tokens = textPos.split(" ");
            node.setTextPosition(Integer.parseInt(tokens[0]),
                                 Integer.parseInt(tokens[1]));
        }
        String numberInfo = nodeElm.getAttributeValue("multimerMonomerNumber");
        if (numberInfo != null && numberInfo.length() > 0) {
            int number = Integer.parseInt(numberInfo);
            ((Node)node).setMultimerMonomerNumber(number);
        }
        String isPrivate = nodeElm.getAttributeValue("isPrivate");
        if (isPrivate != null && isPrivate.length() > 0) {
            if (node instanceof Note) {
                Note note = (Note) node;
                note.setPrivate(new Boolean(isPrivate));
            }
        }
        if (node instanceof RenderableCompartment) {
            String insets = nodeElm.getAttributeValue("insets");
            if (insets != null && insets.length() > 0) {
                RenderableCompartment compartment = (RenderableCompartment) node;
                Rectangle insetBounds = parseBoundsInfo(insets);
                compartment.setInsets(insetBounds);
            }
        }
    }

    private Rectangle parseBoundsInfo(String boundsStr) {
        String[] tokens = boundsStr.split(" ");
        Rectangle bounds = new Rectangle();
        bounds.x = Integer.parseInt(tokens[0]);
        bounds.y = Integer.parseInt(tokens[1]);
        bounds.width = Integer.parseInt(tokens[2]);
        bounds.height = Integer.parseInt(tokens[3]);
        return bounds;
    }
    
    private void parseEdgeDisplayInfo(HyperEdge edge,
                                      org.jdom.Element edgeElm) {
        // Get the backbone points
        String positionStr = edgeElm.getAttributeValue("position");
        String text = edgeElm.getAttributeValue("lineColor");
        if (text != null && text.length() > 0)
            edge.setLineColor(convertToColor(text));
        text = edgeElm.getAttributeValue("lineWidth");
        if (text != null && text.length() > 0)
            edge.setLineWidth(Float.parseFloat(text));
        String pointsStr = edgeElm.getAttributeValue("points");
        StringTokenizer tokenizer = new StringTokenizer(pointsStr, ",");
        java.util.List points = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            String pointStr = tokenizer.nextToken().trim();
            Point point = parsePosition(pointStr);
            points.add(parsePosition(pointStr));
            if (pointStr.equals(positionStr))
                edge.setPosition(point);
        }
        edge.setBackbonePoints(points);
        if (edge instanceof RenderableReaction) {
            RenderableReaction reaction = (RenderableReaction) edge;
            String rxtTypeInfo = edgeElm.getAttributeValue("reactionType");
            if (rxtTypeInfo != null) {
                String type = rxtTypeInfo.toUpperCase().replaceAll(" ", "_");
                reaction.setReactionType(ReactionType.valueOf(type));
            }
        }
    }
    
    private List<Point> parsePointsValue(String points) {
        String[] tokens = points.split(", ");
        List<Point> list = new ArrayList<Point>(tokens.length);
        for (String token : tokens) {
            Point p = parsePosition(token);
            list.add(p);
        }
        return list;
    }
    
    private void parseDatabaseIdentifierElm(org.jdom.Element dbIdentifierElm, Renderable entity) {
        DatabaseIdentifier identifier = new DatabaseIdentifier();
        String value = dbIdentifierElm.getAttributeValue("DB_ID");
        if (value != null && value.length() > 0)
            identifier.setDB_ID(new Long(value));
        value = dbIdentifierElm.getAttributeValue("referenceDB");
        if (value != null && value.length() > 0)
            identifier.setDbName(value);
        value = dbIdentifierElm.getAttributeValue("accessNo");
        if (value != null && value.length() > 0)
            identifier.setAccessNo(value);
        entity.setAttributeValue("databaseIdentifier", identifier);
    }
    
    private void parseModificationsElm(org.jdom.Element mdfsElm, 
                                       Renderable entity) {
        List list = mdfsElm.getChildren();
        if (list == null || list.size() == 0)
            return;
        int size = list.size();
        List modifications = new ArrayList(size);
        Modification modification = null;
        org.jdom.Element mdfElm = null;
        String value = null;
        for (int i = 0; i < size; i++) {
            mdfElm = (org.jdom.Element) list.get(i);
            modification = new Modification();
            value = mdfElm.getAttributeValue("coordinate");
            if (value != null && value.length() > 0) 
                modification.setCoordinate(Integer.parseInt(value));
            value = mdfElm.getAttributeValue("residue");
            if (value != null && value.length() > 0)
                modification.setResidue(value);
            value = mdfElm.getAttributeValue("modification");
            if (value != null && value.length() > 0)
                modification.setModification(value);
            value = mdfElm.getAttributeValue("dbID");
            if (value != null && value.length() > 0)
                modification.setModificationDbID(value);
            value = mdfElm.getAttributeValue("DB_ID");
            if (value != null && value.length() > 0)
                modification.setDB_ID(new Long(value));
            modifications.add(modification);
        }
        entity.setAttributeValue("modifications", modifications);
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
    
    /**
     * Open a RenderablePathway from a passed process element.
     * @param root
     * @return
     */
    public RenderablePathway openProcess(org.jdom.Element root) {
        RenderablePathway pathway = new RenderablePathway();
        String reactomeDiagramId = root.getAttributeValue("reactomeDiagramId");
        if (reactomeDiagramId != null && reactomeDiagramId.length() > 0) {
            pathway.setReactomeDiagramId(new Long(reactomeDiagramId));
        }
        else { // Used as the second choice
            String reactomeId = root.getAttributeValue("reactomeId");
            if (reactomeId != null && reactomeId.length() > 0)
                pathway.setReactomeId(Long.parseLong(reactomeId));
        }
        parseProperties(pathway, root);
        openProcess(root, pathway);
        return pathway;
    }
    
    protected void parseProperties(Renderable r,
                                 org.jdom.Element parentElm) {
        // Get the properties element
        org.jdom.Element propsElm = parentElm.getChild("Properties");
        if (propsElm == null)
            return; // Nothing stored
        List children = propsElm.getChildren();
        for (Iterator it = children.iterator(); it.hasNext();) {
            org.jdom.Element propElm = (org.jdom.Element) it.next();
            String attName = propElm.getName();
            if (attName.equals(SUMMATION)) {
                parseSummationElm(propElm, r);
            }
            else if (attName.equals(REFERENCE)) {
                List references = parseReferencesElm(propElm);
                if (references != null)
                    r.setAttributeValue(REFERENCE, references);
            }
            else if (attName.equals(ATTACHMENT)) {
                parseAttachmentElm(propElm, r);
            }
            else if (attName.equals(DATABASE_IDENTIFIER)) {
                parseDatabaseIdentifierElm(propElm, r);
            }
            else if (attName.equals(MODIFICATION)) {
                parseModificationsElm(propElm, r);
            }
            else if (attName.equals(ALIAS)) {
                parseAliases(propElm, r);
            }
            else {
                parseProperty(attName, propElm, r);
            }
        }
    }
    
    private void parseProperty(String propName,
                               org.jdom.Element propElm,
                               Renderable r) {
        String attValue = propElm.getText();
        r.setAttributeValue(propName, attValue);
    }
    
    private void parseAliases(org.jdom.Element propElm,
                              Renderable r) {
        String text = propElm.getText();
        StringTokenizer tokenizer = new StringTokenizer(text, ";");
        List aliases = new ArrayList();
        while (tokenizer.hasMoreTokens()) 
            aliases.add(tokenizer.nextToken());
        if (aliases.size() > 0)
            r.setAttributeValue(ALIAS, aliases);
    }
    
    public Project open(String source) throws Exception {
        Project project = open(new InputSource(source));
        project.setSourceName(source);
        return project;
    }
    
    public Project open(InputSource reader) throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        org.jdom.Document doc = saxBuilder.build(reader);
        org.jdom.Element root = doc.getRootElement();
        RenderablePathway process = openProcess(root);
        // Create two branches: One for nodes, another for edges
        Project project = new Project();
        project.setProcess(process);
        // Register all Renderable objects in the process.
        RenderableRegistry.getRegistry().registerAll(process);
        // Need to reset nextId
        String nextId = root.getAttributeValue("nextId");
        RenderableRegistry.getRegistry().resetNextId(Integer.parseInt(nextId));
        return project;
    }
    
    private void openProcess(org.jdom.Element root,
                             RenderablePathway process) {
        // Recorder all opened nodes
        Map id2Object = new HashMap();
        // Nodes first
        org.jdom.Element nodesElm = root.getChild("Nodes");
        openNodes(nodesElm, process, id2Object);
        // Edges second
        org.jdom.Element edgesElm = root.getChild("Edges");
        openEdges(edgesElm, process, id2Object);
        // Another pass to make compartment components correct
        parseComponentsForContainers(nodesElm, 
                                     id2Object);
        // Make sure all shortcuts relationships are correct
        // Note: Shortcuts have been used in the AT tool extensively. This should not 
        // be deleted.
        parseShortcuts(nodesElm,
                       id2Object);
        // Pathway components
        Element pathwaysElm = root.getChild("Pathways");
        openPathwayInfo(pathwaysElm, id2Object);
    }
    
    private void parseShortcuts(Element nodesElm,
                                Map id2Object) {
        List children = nodesElm.getChildren();
        if (children == null)
            return;
        for (Iterator it = children.iterator(); it.hasNext();) {
            Element nodeElm = (Element) it.next();
            Element shortcutsElm = nodeElm.getChild("Shortcuts");
            if (shortcutsElm == null)
                continue;
            String id = nodeElm.getAttributeValue("id");
            Node target = (Node) id2Object.get(id);
            List shortcutsChildren = shortcutsElm.getChildren("Shortcut");
            List<Renderable> shortcuts = new ArrayList<Renderable>();
            // The following loop should include this target node too.
            for (Iterator it1 = shortcutsChildren.iterator(); it1.hasNext();) {
                Element shortcutElm = (Element) it1.next();
                id = shortcutElm.getAttributeValue("id");
                Node shortcut = (Node) id2Object.get(id);
                shortcut.setShortcuts(shortcuts);
                shortcuts.add(shortcut);
                shortcut.setAttributes(target.getAttributes());
            }
        }

    }
    
    private void parseComponentsForContainers(Element nodesElm,
                                              Map id2Object) {
        List children = nodesElm.getChildren();
        if (children == null)
            return;
        List<RenderableComplex> complexes = new ArrayList<RenderableComplex>();
        for (Iterator it = children.iterator(); it.hasNext();) {
            Element nodeElm = (Element) it.next();
            String id = nodeElm.getAttributeValue("id");
            Node node = (Node) id2Object.get(id);
            if (node instanceof RenderableCompartment ||
                node instanceof RenderableComplex) {
                parseComponents((ContainerNode)node, 
                                nodeElm, 
                                id2Object);
                if (node instanceof RenderableComplex)
                    complexes.add((RenderableComplex)node);
            }
        }
        for (RenderableComplex complex : complexes)
            complex.rebuildHierarchy();
    }
    
    private void openNodes(org.jdom.Element nodesElm,
                           ContainerNode container,
                           Map id2Object) {
        List children = nodesElm.getChildren();
        if (children == null)
            return;
        for (Iterator it = children.iterator(); it.hasNext();) {
            org.jdom.Element nodeElm = (org.jdom.Element) it.next();
            String id = nodeElm.getAttributeValue("id");
            Renderable r = createRenderableFromElement(nodeElm,
                                                       id2Object);
            container.addComponent(r);
            r.setContainer(container);
            parseNodeDisplayInfo(r, nodeElm);
            parseProperties(r, nodeElm);
            parseNodeAttachments(r, nodeElm);
            if (r instanceof RenderableComplex)
                parseComplexInfo((RenderableComplex)r,
                                 nodeElm,
                                 id2Object);
        }
    }
    
    private void parseNodeAttachments(Renderable r,
                                      Element elm) {
        if (!(r instanceof Node))
            return;
        Element attachmentsElm = elm.getChild("NodeAttachments");
        if (attachmentsElm == null)
            return;
        Node node = (Node) r;
        List children = attachmentsElm.getChildren();
        try {
            List<NodeAttachment> attachments = new ArrayList<NodeAttachment>();
            for (Iterator it = children.iterator(); it.hasNext();) {
                Element attachElm = (Element) it.next();
                String name = attachElm.getName();
                NodeAttachment attachment = (NodeAttachment) Class.forName(name).newInstance();
                double relativeX = Double.parseDouble(attachElm.getAttributeValue("relativeX"));
                double relativeY = Double.parseDouble(attachElm.getAttributeValue("relativeY"));
                attachment.setRelativePosition(relativeX, relativeY);
                if (attachment instanceof RenderableFeature) {
                    RenderableFeature feature = (RenderableFeature) attachment;
                    String type = attachElm.getAttributeValue("type");
                    if (type != null && type.length() > 0)
                        feature.setFeatureType(FeatureType.valueOf(type));
                    else {
                        // Try label
                        String label = attachElm.getAttributeValue("label");
                        if (label != null && label.length() > 0)
                            feature.setLabel(label);
                    }
                }
                String desc = attachElm.getAttributeValue("description");
                attachment.setDescription(desc);
                String trackId = attachElm.getAttributeValue("trackId");
                attachment.setTrackId(Integer.parseInt(trackId));
                String reactomeId = attachElm.getAttributeValue("reactomeId");
                if (reactomeId != null && reactomeId.length() > 0)
                    attachment.setReactomeId(new Long(reactomeId));
                attachments.add(attachment);
            }
            node.setNodeAttachmentsLocally(attachments);
        }
        catch(Exception e) {
            System.err.println("GKBReader.parseNodeAttachments(): " + e);
            e.printStackTrace();
        }
    }
    
    private void parseComplexInfo(RenderableComplex complex,
                                  org.jdom.Element complexElm,
                                  Map id2Object) {
        String hideComponents = complexElm.getAttributeValue("hideComponents");
        if (hideComponents != null)
            complex.hideComponents(Boolean.valueOf(hideComponents).booleanValue());
        if (complex.isComponentsHidden()) {
            // Parse old bounds information
            parseComplexOldBoundsInfo(complex, complexElm);
        }
    }
    
    private void parseComplexOldBoundsInfo(RenderableComplex complex,
                                           Element complexElm) {
        Element oldBoundsElm = complexElm.getChild("OldBounds");
        if (oldBoundsElm == null)
            return;
        List children = oldBoundsElm.getChildren("Bounds");
        Map<Integer, Rectangle> oldIdToBounds = new HashMap<Integer, Rectangle>();
        for (Iterator it = children.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String id = elm.getAttributeValue("id");
            String boundsStr = elm.getAttributeValue("bounds");
            Rectangle bounds = parseBoundsInfo(boundsStr);
            oldIdToBounds.put(new Integer(id),
                              bounds);
        }
        complex.setOldBounds(oldIdToBounds);
    }
    
    private void openPathwayInfo(Element pathwaysElm,
                                 Map id2Object) {
        List list = pathwaysElm.getChildren();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Element pathwayElm = (Element) it.next();
            String id = pathwayElm.getAttributeValue("id");
            RenderablePathway pathway = (RenderablePathway) id2Object.get(id);
            openPathwayInfo(pathway, pathwayElm, id2Object);
        }
    }
    
    private void openPathwayInfo(RenderablePathway pathway,
                                 Element pathwayElm,
                                 Map id2Object) {
        List compElmList = pathwayElm.getChildren();
        for (Iterator it = compElmList.iterator(); it.hasNext();) {
            Element cmpElm = (Element) it.next();
            String id = cmpElm.getAttributeValue("id");
            Renderable comp = (Renderable) id2Object.get(id);
            if (comp == null)
                continue;
            pathway.addComponent(comp);
            // Don't set the container to the pathway
            //comp.setContainer(pathway);
        }
    }
    
    private void parseComponents(ContainerNode container,
                                 Element containerElm,
                                 Map id2Object) {
        Element components = containerElm.getChild("Components");
        if (components == null)
            return;
        List compElmList = components.getChildren("Component");
        for (Iterator it = compElmList.iterator(); it.hasNext();) {
            Element cmpElm = (Element) it.next();
            String id = cmpElm.getAttributeValue("id");
            Renderable comp = (Renderable) id2Object.get(id);
            if (comp == null) {
                //throw new IllegalStateException("GKBReader.parseComponent(): cannot find component " + id + " for " + container.getID());
                continue;
            }
            container.addComponent(comp);
            comp.setContainer(container);
        }
    }
    
    protected Renderable createRenderableFromElement(org.jdom.Element elm,
                                                     Map id2Object) {
        String id = elm.getAttributeValue("id");
        Renderable r = (Renderable) id2Object.get(id);
        if (r != null)
            return r;
        String elmName = elm.getName();
        if (elmName.equals("Shortcut")) {
            String targetId = elm.getAttributeValue("target");
            Renderable target = (Renderable) id2Object.get(targetId);
            if (target == null) {
                String type = elm.getAttributeValue("type");
                target = createRenderableFromType(type);
                id2Object.put(targetId, target);
            }
            r = (Renderable) target.generateShortcut();
            // Want to generate bounds based on position instead of target
            r.setBounds(null);
        }
        else {
            r = createRenderableFromType(elmName);
        }
        r.setID(Integer.parseInt(id));
        String reactomeId = elm.getAttributeValue("reactomeId");
        if (reactomeId != null && reactomeId.length() > 0)
            r.setReactomeId(Long.parseLong(reactomeId));
        id2Object.put(id, r);
        return r;
    }
    
    protected Renderable createRenderableFromType(String type) {
        Renderable r = null;
        try {
            // To support old version of block
            if (type.equals("org.gk.render.Block"))
                type = "org.gk.render.RenderableCompartment";
            r = (Renderable) Class.forName(type).newInstance();
        }
        catch(Exception e) {
            System.err.println("GKBReader.createRenderableFromType(): " + e);
            e.printStackTrace();
        }
        return r;
    }
    
    private void openEdges(org.jdom.Element edgesElm,
                           RenderablePathway process,
                           Map id2Object) {
        List children = edgesElm.getChildren();
        if (children == null)
            return;
        for (Iterator it = children.iterator(); it.hasNext();) {
            org.jdom.Element edgeElm = (org.jdom.Element) it.next();
            Renderable r = createRenderableFromElement(edgeElm,
                                                       id2Object);
            if (r instanceof RenderableInteraction) {
                // Have to set type
                String typeName = edgeElm.getAttributeValue("interactionType");
                InteractionType type = InteractionType.getType(typeName);
                ((RenderableInteraction)r).setInteractionType(type);
            }
            process.addComponent(r);
            r.setContainer(process);
            parseEdgeDisplayInfo((HyperEdge)r, edgeElm);
            parseEdgeInfo((HyperEdge)r, edgeElm, id2Object);
            parseProperties(r, edgeElm);
            // special case for reaction
            if (r instanceof RenderableReaction) {
                String isReversible = (String) r.getAttributeValue(IS_REVERSIBLE);
                if (isReversible != null && isReversible.equals("true"))
                    ((RenderableReaction)r).setNeedInputArrow(true);
            }
        }
    }
    
    private void parseEdgeInfo(HyperEdge edge,
                               org.jdom.Element edgeElm,
                               Map id2Object) {
        // Attach inputs, outputs and helpers
        List list = edgeElm.getChildren();
        for (int i = 0; i < list.size(); i++) {
            org.jdom.Element roleElm = (org.jdom.Element) list.get(i);
            String nodeName = roleElm.getName();
            if (nodeName.equals("Inputs")) {
                parseEdgeInputOutputElm(roleElm, edge, id2Object, HyperEdge.INPUT);
            }
            else if (nodeName.equals("Outputs")) {
                parseEdgeInputOutputElm(roleElm, edge, id2Object, HyperEdge.OUTPUT);
            }
            else if (nodeName.equals("Catalysts")) {
                List catalystPoints = parseEdgeHelpersElm(roleElm, 
                                                          edge, 
                                                          id2Object,
                                                          HyperEdge.CATALYST);
                edge.setHelperPoints(catalystPoints);
            }
            else if (nodeName.equals("Inhibitors")) {
                List inhibitorPoints = parseEdgeHelpersElm(roleElm, 
                                                           edge, 
                                                           id2Object, 
                                                           HyperEdge.INHIBITOR);
                edge.setInhibitorPoints(inhibitorPoints);
            }
            else if (nodeName.equals("Activators")) {
                List activatorPoints = parseEdgeHelpersElm(roleElm, 
                                                           edge, 
                                                           id2Object, 
                                                           HyperEdge.ACTIVATOR);
                edge.setActivatorPoints(activatorPoints);
            }
        }
    }
    
    /**
     * Have to use this method to load inputs, outputs. Otherwise, inputHub
     * or outputHub will not be correct by calling addInput or addOutput only.
     * @param roleElm
     * @param edge
     * @param id2Object
     * @param connectRole
     */
    private void parseEdgeInputOutputElm(Element roleElm,
                                         HyperEdge edge,
                                         Map id2Object,
                                         int connectRole) {
        List list = roleElm.getChildren();
        int length = list.size();
        List points = edge.getBackbonePoints();
        if (length == 1) { // The minimum should be 1
            Element elm = (Element) list.get(0);
            String id = elm.getAttributeValue("id");
            Node node = (Node) id2Object.get(id);
            if (node != null) {
                Point pos = null;
                Point controlP = null;
                if (connectRole == HyperEdge.INPUT) {
                    pos = (Point) points.get(0);
                    controlP = (Point) points.get(1);
                }
                else if (connectRole == HyperEdge.OUTPUT) {
                    pos = (Point) points.get(points.size() - 1);
                    controlP = (Point) points.get(points.size() - 2);
                }
                ConnectWidget widget = createConnectWidget(pos, controlP, connectRole, 0, edge, node);
                String stoi = elm.getAttributeValue("stoichiometry");
                if (stoi != null && stoi.length() > 0)
                    widget.setStoichiometry(Integer.parseInt(stoi));
            }
        }
        else if (length > 1) {
            java.util.List connectPoints = new ArrayList();
            Point controlP = null;
            if (connectRole == HyperEdge.INPUT) {
                controlP = edge.getInputHub();
                edge.setInputPoints(connectPoints);
            }
            else if (connectRole == HyperEdge.OUTPUT) {
                controlP = edge.getOutputHub();
                edge.setOutputPoints(connectPoints);
            }
            for (int j = 0; j < length; j++) {
                Element elm = (Element) list.get(j);
                String id = elm.getAttributeValue("id");
                Node node = (Node) id2Object.get(id);
                if (node != null) {
                    List<Point> branch = null;
//                    Point pos = (Point) node.getPosition().clone();
                    Point pos = null;
                    String pointsValue = elm.getAttributeValue("points");
                    Point controlPi = controlP;
                    if (pointsValue == null || pointsValue.length() == 0) {
                        branch = new ArrayList<Point>();
                        pos = (Point) node.getPosition().clone();
                        branch.add(pos);
                    }
                    else {
                        branch = parsePointsValue(pointsValue);
                        // The following is not necessary any more!
//                        branch.set(0, pos); // Replace the first point with the point from the node that is linked to reaction.
//                                            // Use this way to control the linkage.
                        pos = branch.get(0);
                        if (branch.size() > 1)
                            controlPi = branch.get(1);
                    }
                    connectPoints.add(branch);
                    ConnectWidget widget = createConnectWidget(pos, 
                                                               controlPi,
                                                               connectRole, 
                                                               j, 
                                                               edge, 
                                                               node);
                    String stoi = elm.getAttributeValue("stoichiometry");
                    if (stoi != null && stoi.length() > 0)
                        widget.setStoichiometry(Integer.parseInt(stoi));
                }
            }
        }
    }
    
    private List parseEdgeHelpersElm(Element helpersElm,
                                     HyperEdge edge,
                                     Map id2Object,
                                     int connectRole) {
        java.util.List helperList = helpersElm.getChildren();
        int length = helperList.size();
        java.util.List helperPoints = new ArrayList();
        for (int j = 0; j < length; j++) {
            Element helperElm = (Element) helperList.get(j);
            String id = helperElm.getAttributeValue("id");
            Renderable helper = (Renderable) id2Object.get(id);
            if (helper != null) {
//                Point pos = (Point) helper.getPosition().clone();
                Point pos = null;
                java.util.List<Point> helperbranch = null;
                String pointsValue = helperElm.getAttributeValue("points");
                Point controlP = edge.getPosition();
                if (pointsValue != null && pointsValue.length() > 0) {
                    helperbranch = parsePointsValue(pointsValue);
//                    helperbranch.set(0, pos);
                    pos = helperbranch.get(0);
                    if (helperbranch.size() > 1)
                        controlP = helperbranch.get(1);
                }
                else {
                    helperbranch = new ArrayList<Point>();
                    pos = (Point) helper.getPosition().clone();
                    helperbranch.add(pos);
                }
                helperPoints.add(helperbranch);
                createConnectWidget(pos, 
                                    controlP, 
                                    connectRole, 
                                    j, 
                                    edge, 
                                    (Node)helper);
            }
        }
        return helperPoints;
    }
    
    private ConnectWidget createConnectWidget(Point pos,
                                              Point controlP,
                                              int connectRole,
                                              int index,
                                              HyperEdge edge,
                                              Node node) {
        ConnectWidget widget = new ConnectWidget(pos, controlP, connectRole, index);
        widget.setEdge(edge);
        widget.setConnectedNode(node);
        widget.connect();
        widget.invalidate();
        return widget;
    }
}
