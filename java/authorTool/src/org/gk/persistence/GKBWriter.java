/*
 * Created on Jan 5, 2007
 *
 */
package org.gk.persistence;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.Modification;
import org.gk.model.Reference;
import org.gk.model.Summation;
import org.gk.render.*;
import org.gk.render.RenderableFeature.FeatureType;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class is used to write .gkb file to a file system.
 * @author guanming
 *
 */
public class GKBWriter implements RenderablePropertyNames {
    // A flag to control is properties should be exported.
    // If this value is true and an object is not registered,
    // its properties will not be exported into XML file.
    // Otherwise, all objects, no matter if it is registered, should be
    // export with objects.
    private boolean needRegistryCheck = true;
    
    public GKBWriter() {
    }
    
    public void setNeedRegistryCheck(boolean check) {
        this.needRegistryCheck = check;
    }
    
    public String generateXMLString(Project project) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        save(project, bos);
        return bos.toString("UTF-8");
    }
    
    protected org.jdom.Element createElementForRenderable(Renderable r) {
        org.jdom.Element elm = new org.jdom.Element(r.getClass().getName());
        elm.setAttribute("id", r.getID() + "");
        Long reactomeId = r.getReactomeId();
        if (reactomeId != null)
            elm.setAttribute("reactomeId", reactomeId + "");
        return elm;
    }
    
    private String createBoundsText(Rectangle bounds) {
        String text = bounds.x + " " + bounds.y + " " + 
                      bounds.width + " " + bounds.height;
        return text;
    }
    
    private void appendNodeDisplayInfo(Renderable renderable, 
                                       Element parentElm) {
        // Set position
        Point pos = renderable.getPosition();
        parentElm.setAttribute("position", pos.x + " " + pos.y);
        // bounds
        Rectangle bounds = renderable.getBounds();
        if (bounds != null)
            parentElm.setAttribute("bounds", createBoundsText(bounds));
        Rectangle textBounds = renderable.getTextBounds();
        if (textBounds != null)
            parentElm.setAttribute("textPosition",
                                   textBounds.x + " " + textBounds.y);
        // Set bgColor
        if (renderable.getBackgroundColor() != null)
            parentElm.setAttribute("bgColor", convertToString(renderable.getBackgroundColor()));
        if (renderable.getForegroundColor() != null)
            parentElm.setAttribute("fgColor",
                                   convertToString(renderable.getForegroundColor()));
        if (renderable.getLineColor() != null)
            parentElm.setAttribute("lineColor",
                                   convertToString(renderable.getLineColor()));
        if (renderable.getLineWidth() != null)
            parentElm.setAttribute("lineWidth", renderable.getLineWidth() + "");
        if (renderable instanceof Node) {
            Node node = (Node) renderable;
            if (node.isNeedDashedBorder()) {
                parentElm.setAttribute("needDashedBorder", node.isNeedDashedBorder() + "");
            }
        }
        // Check if it is a multimer
        if (renderable instanceof Node) {
            Node node = (Node) renderable;
            int number = node.getMultimerMonomerNumber();
            if (number >= 2) {
                parentElm.setAttribute("multimerMonomerNumber", number + "");
            }
            if (renderable instanceof Note) {
                boolean isPrivate = ((Note)renderable).isPrivate();
                if (isPrivate)
                    parentElm.setAttribute("isPrivate", isPrivate + "");
            }
        }
    }
    
    private String convertToString(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return r + " " + g + " " + b;
    }
    
    private void appendDatabaseIdentifier(DatabaseIdentifier identifier,
                                          org.jdom.Element parentElm) {
        if (identifier != null) {
            org.jdom.Element identifierElm = new org.jdom.Element(DATABASE_IDENTIFIER);
            if (identifier.getDB_ID() != null)
                identifierElm.setAttribute("DB_ID", identifier.getDB_ID() + "");
            if (identifier.getDbName() != null)
                identifierElm.setAttribute("referenceDB", identifier.getDbName());
            if (identifier.getAccessNo() != null)
                identifierElm.setAttribute("accessNo", identifier.getAccessNo());
            parentElm.addContent(identifierElm);
        }
    }
    
    private void appendModification(List modifications,
                                    org.jdom.Element parentElm) {
        if (modifications != null && modifications.size() > 0) {
            org.jdom.Element mdfsElm = new org.jdom.Element(MODIFICATION);
            org.jdom.Element mdfElm = null;
            Modification mdf = null;
            for (Iterator it = modifications.iterator(); it.hasNext();) {
                mdf = (Modification) it.next();
                mdfElm = new org.jdom.Element(mdf.getClass().getName());
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
                mdfsElm.addContent(mdfElm);
            }
            parentElm.addContent(mdfsElm);
        }
    }
    
    private void appendAliases(List names,
                             org.jdom.Element parentElm) {
        if (names != null && names.size() > 0) {
            StringBuffer buffer = new StringBuffer();
            int size = names.size(); 
            for (int i = 0; i < size; i++) {
                String name = (String) names.get(i);
                buffer.append(name);
                if (i < size - 1)
                    buffer.append(";"); // Delimiter
            }
            org.jdom.Element propElm = new org.jdom.Element(ALIAS);
            propElm.setText(buffer.toString());
            parentElm.addContent(propElm);
        }       
    }
    
    private void appendReferences(java.util.List references, 
                                  org.jdom.Element parentElm) {
        if (references == null || references.size() == 0)
            return;
        org.jdom.Element referencesElm = new org.jdom.Element(REFERENCE);
        parentElm.addContent(referencesElm);
        org.jdom.Element elm;
        Reference reference;
        for (Iterator it = references.iterator(); it.hasNext();) {
            reference = (Reference) it.next();
            elm = new org.jdom.Element(reference.getClass().getName());
            if (reference.getDB_ID() != null)
                elm.setAttribute("DB_ID", reference.getDB_ID() + "");
            elm.setAttribute("PMID", reference.getPmid() + "");
            if (reference.getAuthor() != null)
                elm.setAttribute("author", reference.getAuthor());
            if (reference.getJournal() != null)
                elm.setAttribute("journal", reference.getJournal());
            if (reference.getYear() > 0)
                elm.setAttribute("year", reference.getYear() + "");
            if (reference.getVolume() != null)
                elm.setAttribute("volume", reference.getVolume());
            if (reference.getPage() != null)
                elm.setAttribute("page", reference.getPage());
            if (reference.getTitle() != null)
                elm.setAttribute("title", reference.getTitle());
            referencesElm.addContent(elm);
        }
    }
    
    private void appendSummationElm(Summation summation,
                                    org.jdom.Element parentElm) {
        org.jdom.Element summationElm = new org.jdom.Element(SUMMATION);
        if (summation.getDB_ID() != null) {
            summationElm.setAttribute("DB_ID", summation.getDB_ID() + "");
        }
        summationElm.setAttribute("isChanged", summation.isChanged() + "");
        if (summation.getText() != null && summation.getText().length() > 0) {
            // create a text element
            org.jdom.Element textElm = new org.jdom.Element("Text");
            summationElm.addContent(textElm);
            textElm.setText(summation.getText());
        }
        parentElm.addContent(summationElm);
        // Need to append references
        appendReferences(summation.getReferences(), summationElm);
    }
    
    private void appendAttachmentElm(List attachments,
                                     org.jdom.Element parentElm) {
        if (attachments != null && attachments.size() > 0) {
            org.jdom.Element listElm = new org.jdom.Element(ATTACHMENT);
            for (Iterator it = attachments.iterator(); it.hasNext();) {
                String name = (String) it.next();
                org.jdom.Element elm = new org.jdom.Element("name");
                elm.setText(name);
                listElm.addContent(elm);
            }
            parentElm.addContent(listElm);
        }
    }
    
    public boolean save(Project project, 
                        String dest) throws Exception {
        project.setSourceName(dest);
        FileOutputStream fos = new FileOutputStream(dest);
        return save(project, fos);
    }
    
    public boolean save(Project project,
                        OutputStream os) throws Exception {
        org.jdom.Document document = new org.jdom.Document();
        org.jdom.Element root = createRootElement(project.getProcess());
        document.setRootElement(root);
        outputDocument(document, os);
        return true;
    }
    
    public Element createRootElement(RenderablePathway process) {
        org.jdom.Element root = createProcessNode(process);
        org.jdom.Element nodesElm = new org.jdom.Element("Nodes");
        org.jdom.Element edgesElm = new org.jdom.Element("Edges");
        Element pathwaysElm = new Element("Pathways");
        save(process,
             nodesElm,
             edgesElm, 
             pathwaysElm);
        if (nodesElm.getChildren() != null)
            root.addContent(nodesElm);
        if (edgesElm.getChildren() != null)
            root.addContent(edgesElm);
        if (pathwaysElm.getChildren() != null)
            root.addContent(pathwaysElm);
        return root;
    }
    
    private void save(RenderablePathway process,
                      org.jdom.Element nodesElm,
                      org.jdom.Element edgesElm,
                      Element pathwaysElm) {
        List components = process.getComponents();
        if (components == null || components.size() == 0)
            return;
        Renderable r = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (r instanceof Shortcut) {
                saveShortcut(r, nodesElm);
            }
            else if (r instanceof RenderableCompartment)
                saveCompartment((RenderableCompartment)r, nodesElm);
            else if (r instanceof Node) 
                saveNode((Node)r, nodesElm, pathwaysElm);
            else if (r instanceof HyperEdge)
                saveEdge((HyperEdge)r, edgesElm);
        }
    }
    
    private void saveShortcut(Renderable shortcut,
                              org.jdom.Element nodesElm) {
        org.jdom.Element shortcutElm = new org.jdom.Element("Shortcut");
        // id should be used still
        shortcutElm.setAttribute("id", shortcut.getID() + "");
        Renderable target = ((Shortcut)shortcut).getTarget();
        shortcutElm.setAttribute("target", target.getID() + "");
        shortcutElm.setAttribute("type", target.getClass().getName());
        appendNodeDisplayInfo(shortcut, shortcutElm);
        if (shortcut instanceof RenderableComplex)
            appendComplexInfo((RenderableComplex)shortcut, shortcutElm);
        nodesElm.addContent(shortcutElm);
        if (shortcut instanceof Node)
            saveNodeAttachments(shortcutElm, (Node)shortcut);
    }
    
    private void saveNode(Node node,
                          Element nodesElm,
                          Element pathwaysElm) {
        org.jdom.Element nodeElm = createElementForRenderable(node);
        appendNodeDisplayInfo(node, nodeElm);
        saveProperties(nodeElm, node);
        saveShortcuts(nodeElm, node);
        saveNodeAttachments(nodeElm, node);
        if (node instanceof RenderableComplex)
            appendComplexInfo((RenderableComplex)node,
                              nodeElm);
        else if (node instanceof RenderablePathway) {
            appendPathwayInfo((RenderablePathway)node,
                              pathwaysElm);
        }
        nodesElm.addContent(nodeElm);
    }
    
    private void saveShortcuts(Element nodeElm,
                               Node node) {
        if (node.getShortcuts() == null || 
            node.getShortcuts().size() < 2 || // No need if there is only one shortcut. It should be itself.
            !RenderableRegistry.getRegistry().isRegistered(node))
            return;
        List<Renderable> shortcuts = node.getShortcuts();
        Element shortcutsElm = new Element("Shortcuts");
        nodeElm.addContent(shortcutsElm);
        for (Renderable r : shortcuts) {
            Element shortcutElm = new Element("Shortcut");
            shortcutElm.setAttribute("id", r.getID() + "");
            shortcutsElm.addContent(shortcutElm);
        }
    }
    
    private void saveNodeAttachments(Element nodeElm,
                                     Node node) {
        List<NodeAttachment> attachments = node.getNodeAttachments();
        if (attachments == null || attachments.size() == 0)
            return;
        Element attachmentsElm = new Element("NodeAttachments");
        RenderableFeature numberFeature = null;
        for (NodeAttachment attachment : attachments) {
            Element attachElm = new Element(attachment.getClass().getName());
            if (attachment == numberFeature)
                attachElm.setAttribute("isNumber", attachment.getLabel());
            attachElm.setAttribute("relativeX", attachment.getRelativeX() + "");
            attachElm.setAttribute("relativeY", attachment.getRelativeY() + "");
            if (attachment instanceof RenderableFeature) {
                RenderableFeature feature = (RenderableFeature) attachment;
                FeatureType type = feature.getFeatureType();
                if (type != null)
                    attachElm.setAttribute("type", type.getOriginalName());
                else {
                    String label = attachment.getLabel();
                    if (label != null && label.length() > 0)
                        attachElm.setAttribute("label", label);
                }
            }
            // An optional property
            String desc = attachment.getDescription();
            if (desc != null)
                attachElm.setAttribute("description", desc);
            // This is required
            int trackId = attachment.getTrackId();
            attachElm.setAttribute("trackId", trackId + "");
            // From the database
            if (attachment.getReactomeId() != null) {
                attachElm.setAttribute("reactomeId", 
                                       attachment.getReactomeId() + "");
            }
            attachmentsElm.addContent(attachElm);
        }
        nodeElm.addContent(attachmentsElm);
    }
    
    private void appendComplexInfo(RenderableComplex complex,
                                   org.jdom.Element complexElm) {
        // components
        if (complex.isComponentsHidden()) {
            complexElm.setAttribute("hideComponents",
                                    "true");
            // Need to save the old bounds information
            appendComplexOldBounds(complex, complexElm);
        }
        List components = complex.getComponents();
        if (components == null || components.size() == 0)
            return;
        saveComponents(complex, complexElm);
    }
    
    private void appendComplexOldBounds(RenderableComplex complex,
                                        Element complexElm) {
        Map<Integer, Rectangle> oldBounds = complex.getOldBounds();
        if (oldBounds == null || oldBounds.size() == 0)
            return;
        Element oldBoundsElm = new Element("OldBounds");
        complexElm.addContent(oldBoundsElm);
        for (Integer id : oldBounds.keySet()) {
            Rectangle bounds = oldBounds.get(id);
            Element elm = new Element("Bounds");
            elm.setAttribute("id", id + "");
            if (bounds != null)
                elm.setAttribute("bounds",
                                 createBoundsText(bounds));
            oldBoundsElm.addContent(elm);
        }
    }
    
    private void appendPathwayInfo(RenderablePathway pathway,
                                   Element pathwaysElm) {
        List components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return;
        Element pathwayElm = new Element("Pathway");
        pathwayElm.setAttribute("id", pathway.getID() + "");
        pathwaysElm.addContent(pathwayElm);
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            Element compElm = new Element("Component");
            compElm.setAttribute("id", r.getID() + "");
            pathwayElm.addContent(compElm);
        }
    }
    
    private void saveCompartment(RenderableCompartment compartment,
                                 org.jdom.Element nodesElm) {
        Element compartmentElm = createElementForRenderable(compartment);
        appendNodeDisplayInfo(compartment, compartmentElm);
        Rectangle insets = compartment.getInsets();
        if (insets != null && !insets.isEmpty()) {
            // Save insets
            String insetsText = createBoundsText(insets);
            compartmentElm.setAttribute("insets", insetsText);
        }
        saveProperties(compartmentElm, compartment);
        // Need to process compartment information
        saveComponents(compartment, compartmentElm);
        nodesElm.addContent(compartmentElm);
    }
    
    private void saveComponents(ContainerNode container,
                                Element containerElm) {
        List components = container.getComponents();
        if (components == null || components.size() == 0)
            return;
        Element componentsElm = new Element("Components");
        containerElm.addContent(componentsElm);
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            Element compElm = new Element("Component");
            compElm.setAttribute("id", r.getID() + "");
            componentsElm.addContent(compElm);
        }
    }
    
    private void saveEdge(HyperEdge edge,
                          org.jdom.Element edgesElm) {
        org.jdom.Element edgeElm = createElementForRenderable(edge);
        if (edge instanceof RenderableInteraction &&
            ((RenderableInteraction)edge).getInteractionType() != null) {
            edgeElm.setAttribute("interactionType",
                                 ((RenderableInteraction)edge).getInteractionType().getTypeName());
        }
        appendEdgeDisplayInfo(edge, edgeElm);
        appendEdgeInfo(edge, edgeElm);
        saveProperties(edgeElm, edge);
        edgesElm.addContent(edgeElm);
    }
    
    private void appendEdgeDisplayInfo(HyperEdge reaction,
                                       org.jdom.Element elm) {
        // Backbone Points
        String points = convertPointsToString(reaction.getBackbonePoints());   
        elm.setAttribute("points", points);
        Point pos = reaction.getPosition();
        elm.setAttribute("position", pos.x + " " + pos.y);  
        elm.setAttribute("lineWidth", reaction.getLineWidth() + "");
        if (reaction.getLineColor() != null)
            elm.setAttribute("lineColor", convertToString(reaction.getLineColor()));
        // Type for reaction
        if (reaction instanceof RenderableReaction) {
            RenderableReaction rxt = (RenderableReaction) reaction;
            ReactionType type = rxt.getReactionType();
            if (type != null) {
                elm.setAttribute("reactionType", type.toString());
            }
        }
    }

    private String convertPointsToString(List points) {
        StringBuilder buffer = new StringBuilder();
        Point p = null;
        for (int i = 0; i < points.size(); i++) {
            p = (Point) points.get(i);
            buffer.append(p.x + " " + p.y);
            if (i < points.size() - 1)
                buffer.append(", ");
        }
        return buffer.toString();
    }
    
    private List getBranch(int index,
                           List branches) {
        if (branches == null || branches.size() == 0)
            return null;
        if (index > branches.size() - 1)
            return null;
        return (List) branches.get(index);
    }
    
    private void appendEdgeInfo(HyperEdge edge,
                                org.jdom.Element edgeElm) {
        boolean needStoi = false;
        if (edge instanceof RenderableReaction)
            needStoi = true;
        // For inputs
        java.util.List inputs = edge.getInputNodes();
        if (inputs != null && inputs.size() > 0) {
            List inputBranches = edge.getInputPoints();
            org.jdom.Element inputsElm = new org.jdom.Element("Inputs");
            for (int i = 0; i < inputs.size(); i++) {
                org.jdom.Element inputElm = new org.jdom.Element("Input");
                Renderable input = (Renderable) inputs.get(i);
                inputElm.setAttribute("id", input.getID() + "");
                if (needStoi) {
                    int stoi = ((RenderableReaction)edge).getInputStoichiometry(input);
                    if (stoi != 1)
                        inputElm.setAttribute("stoichiometry", stoi + "");
                }
                inputsElm.addContent(inputElm);
                List branch = getBranch(i, inputBranches);
                if (branch != null) {
                    String points = convertPointsToString(branch);
                    inputElm.setAttribute("points", points);
                }
            }
            edgeElm.addContent(inputsElm);
        }   
        java.util.List outputs = edge.getOutputNodes();
        if (outputs != null && outputs.size() > 0) {
            org.jdom.Element outputsElm = new org.jdom.Element("Outputs");
            List outputBranches = edge.getOutputPoints();
            for (int i = 0; i < outputs.size(); i++) {
                org.jdom.Element outputElm = new org.jdom.Element("Output");
                Renderable output = (Renderable) outputs.get(i);
                outputElm.setAttribute("id", output.getID() + "");
                if (needStoi) {
                    int stoi = ((RenderableReaction)edge).getOutputStoichiometry(output);
                    if (stoi != 1)
                        outputElm.setAttribute("stoichiometry", stoi + "");
                }
                outputsElm.addContent(outputElm);
                List branch = getBranch(i, outputBranches);
                if (branch != null) {
                    String points = convertPointsToString(branch);
                    outputElm.setAttribute("points", points);
                }
            }
            edgeElm.addContent(outputsElm);
        }
        // Save catalysts
        java.util.List helpers = edge.getHelperNodes();
        List helperPoints = edge.getHelperPoints();
        appendEdgeHelperNodes(helpers, 
                              helperPoints,
                              "Catalyst", 
                              edgeElm);
        java.util.List inhibitors = edge.getInhibitorNodes();
        List inhibitorPoints = edge.getInhibitorPoints();
        appendEdgeHelperNodes(inhibitors, 
                              inhibitorPoints,
                              "Inhibitor",
                              edgeElm);
        java.util.List activators = edge.getActivatorNodes();
        List activatorPoints = edge.getActivatorPoints();
        appendEdgeHelperNodes(activators,
                              activatorPoints,
                              "Activator",
                              edgeElm);
    }
    
    private void appendEdgeHelperNodes(List nodes,
                                       List branches,
                                       String name,
                                       org.jdom.Element edgeElm) {
        if (nodes != null && nodes.size() > 0) {
            org.jdom.Element helpersElm = new org.jdom.Element(name + "s");
            for (int i = 0; i < nodes.size(); i++) {
                org.jdom.Element helperElm = new org.jdom.Element(name);
                Renderable helper = (Renderable) nodes.get(i);
                helperElm.setAttribute("id", helper.getID() + "");
                helpersElm.addContent(helperElm);
                List branch = getBranch(i, branches);
                if (branch != null) {
                    String points = convertPointsToString(branch);
                    helperElm.setAttribute("points", points);
                }
            }
            edgeElm.addContent(helpersElm);
        }
    }
    
    protected org.jdom.Element createProcessNode(RenderablePathway pathway) {
        org.jdom.Element processElm = new org.jdom.Element("Process");
        // Move back one position
        processElm.setAttribute("nextId", (RenderableRegistry.getRegistry().nextId() - 1) + "");
        if (pathway.getReactomeDiagramId() != null)
            processElm.setAttribute("reactomeDiagramId", pathway.getReactomeDiagramId().toString());
        else if (pathway.getReactomeId() != null) // Used as the second choice
            processElm.setAttribute("reactomeId", pathway.getReactomeId().toString());
        saveProperties(processElm, pathway);
        return processElm;
    }
    
    protected void saveProperties(org.jdom.Element elm,
                                  Renderable r) {
        // Attach the properties to the registered objects only.
        if (needRegistryCheck &&
            !(r instanceof Note) &&
            !RenderableRegistry.getRegistry().isRegistered(r))
            return;
        Map attributes = r.getAttributes();
        if (attributes == null || attributes.size() == 0)
            return;
        org.jdom.Element propsElm = new org.jdom.Element("Properties");
        elm.addContent(propsElm);
        for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
            // Handle special cases
            String attName = (String) it.next();
            Object attValue = attributes.get(attName);
            if (attValue == null)
                continue;
            if (attName.equals(SUMMATION)) {
                appendSummationElm((Summation)attValue, propsElm);
            }
            else if (attName.equals(REFERENCE)) {
                appendReferences((List)attValue, propsElm);
            }
            else if (attName.equals(ATTACHMENT)) {
                appendAttachmentElm((List)attValue, propsElm);
            }
            else if (attName.equals(DATABASE_IDENTIFIER)) {
                appendDatabaseIdentifier((DatabaseIdentifier)attValue,
                                         propsElm);
            }
            else if (attName.equals(MODIFICATION)) {
                appendModification((List)attValue,
                                   propsElm);
            }
            else if (attName.equals(ALIAS)) {
                appendAliases((List)attValue, propsElm);
            }
            else {
                saveProperty(attName, attValue, propsElm);
            }
        }
    }
    
    protected void saveProperty(String attName,
                                Object attValue,
                                org.jdom.Element propertiesElm) {
        org.jdom.Element propElm = new org.jdom.Element(attName);
        propElm.setText(attValue.toString());
        propertiesElm.addContent(propElm);
    }
    
    private void outputDocument(org.jdom.Document document,
                                OutputStream os) throws Exception {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(document, os);
    }
}
