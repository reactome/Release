/*
 * This class is used to create a Graphviz dot file for a Pathway class.
 * Created on Nov 18, 2005
 *
 */
package org.gk.gkCurator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class GraphvizDotGenerator {
    // To control layout graph
    private static double nodeSep = 0.3d;
    private static double rankSep = 0.4d;
    private static double edgeLen = 2.8d;
    private static int FILE_SIZE_LIMIT = 100; // (100k)
    // This map is used to map the label in svg back to Reactome links
    private Map nameToIdMap;
    // flag to control if URL should be added to nodes. A URL is needed
    // to be output as cmap
    private boolean useURL;
    
    public GraphvizDotGenerator() {
        // Used to add links
        nameToIdMap = new HashMap();
    }
    
    public void setUseURL(boolean useURL) {
        this.useURL = useURL;
    }
    
    /**
     * This method is used to add xlinks to a svg graph so that
     * the reactome web site can be displayed.
     * @param svg
     * @return
     */
    public String addLinksToSvg(String svg, String reactomeUrl) throws IOException {
        if (nameToIdMap == null || nameToIdMap.size() == 0)
            return svg; // Cannot do anything since there is no map existing
        // Convert to JDOM Document
        try {
            SAXBuilder saxbuilder = new SAXBuilder();
            StringReader reader = new StringReader(svg);
            Document document = saxbuilder.build(reader);
            // Root element is svg
            Element root = document.getRootElement();
            // Add namespace defintion to xlink
            Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
            root.addNamespaceDeclaration(xlinkNS);
            // The first element is root g
            Element rootG = root.getChild("g", root.getNamespace());
            // A list of g elements
            List gList = rootG.getChildren("g", rootG.getNamespace());
            // To avoid the error of checkConcurrentModification
            gList = new ArrayList(gList);
            for (Iterator it = gList.iterator(); it.hasNext();) {
                Element groupElm = (Element) it.next();
                // Need to get String label for it
                String title = groupElm.getChild("title", groupElm.getNamespace()).getText();
                Long reactomeId = (Long) nameToIdMap.get(title);
                if (reactomeId != null) // It might be just an accessory node
                    addLink(groupElm, reactomeId, reactomeUrl, xlinkNS);
            }
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            StringWriter writer = new StringWriter();
            outputter.output(document, writer);
            return writer.toString();
        }
        catch(JDOMException e) {
            System.err.println("GraphvizDotGenerator.addLinksToSvg(): " + e);
            e.printStackTrace();
        }
        return svg;
    }
    
    private void addLink(Element groupElm, 
                         Long id, 
                         String reactomeUrl,
                         Namespace xlinkNS) {
        // Create a link node
        Element linkNode = new Element("a", groupElm.getNamespace());
        linkNode.setAttribute("href", reactomeUrl + "&ID=" + id, xlinkNS);
        Element parent = groupElm.getParentElement();
        groupElm.detach();
        linkNode.addContent(groupElm);
        parent.addContent(linkNode);
    }
    
    /**
     * Convert a Pathway object into a dot file format.
     * @param pathway
     * @return
     */
    public String generateDot(GKInstance pathway) throws Exception {
        // Get all contained Reactions recursively
        Set reactions = new HashSet();
        listReactions(pathway, reactions);
        // Generate nodes from inputs, outputs, catalyst and modulator
        if (reactions.size() == 0)
            return null;
        return convertToDotGraph(reactions);
    }
    
    private String convertToDotGraph(Set reactions) throws Exception {
        // Convert to dot-recognizable graph
        StringBuffer buffer = new StringBuffer();
        buffer.append("digraph test {");
        buffer.append("\n");
        // The following three properties are for dot
        buffer.append("\t nodesep=\"" + nodeSep + "\";\n"); 
        buffer.append("\t ranksep=\"" + rankSep + "\";\n");
        // Make width and height small enough so that the minimum size can be picked up.
        //buffer.append("\t node [shape=rectangle, fontsize=8, width=0.1, height=0.1];\n");
        // Add the fontname so that the generated svg can be opened in firefox though the performance to
        // view SVG is very bad (almost not usable). This fontname should be kept when this class is used
        // in a perl script. This change is due to the latest version of Graphiviz (2.12).
        buffer.append("\t node [shape=rectangle, fontsize=8, width=0.01, height=0.01];\n");
        // The following property is for neato
        buffer.append("\t edge [len=\"" + edgeLen + "\"];\n");
        Map entityToNameMap = new HashMap();
        Map nameToEntityMap = new HashMap();
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
            generateNodes(inputs, entityToNameMap, nameToEntityMap, buffer);
            List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
            generateNodes(outputs, entityToNameMap, nameToEntityMap, buffer);
            List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            List catalysts = new ArrayList();
            if (cas != null && cas.size() > 0) {
                for (Iterator it1 = cas.iterator(); it1.hasNext();) {
                    GKInstance ca = (GKInstance) it1.next();
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    if (catalyst != null)
                        catalysts.add(catalyst);
                }
                generateNodes(catalysts, entityToNameMap, nameToEntityMap, buffer);
            }
            List regulators = new ArrayList();
            Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulations != null && regulations.size() > 0) {
                for (Iterator it1 = regulations.iterator(); it1.hasNext();) {
                    GKInstance regulation = (GKInstance) it1.next();
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                        regulators.add(regulator);
                }
                generateNodes(regulators, entityToNameMap, nameToEntityMap, buffer);
            }
            // Check if any accessory nodes are needed
            if ((catalysts.size() == 0) && (regulators.size() == 0)) {
                if (inputs == null || inputs.size() == 0) {
                    if (outputs != null) {
                        if (outputs.size() == 1) {
                            GKInstance output = (GKInstance) outputs.get(0);
                            // Need an extra node
                            addReactionNode(reaction.getDBID() + "", buffer);
                            // Add an edge to output
                            addEdge(reaction.getDBID() + "", output, entityToNameMap, true, buffer);
                        }
                        else if (outputs.size() > 1) {
                            // Need two extra nodes
                            String name1 = reaction.getDBID() + "_1";
                            addReactionNode(name1, buffer);
                            String name2 = reaction.getDBID() + "_2";
                            addReactionNode(name2, buffer);
                            // Add a link between these two edges
                            addEdge(name1, name2, false, buffer);
                            // Create new edge
                            for (Iterator iterator = outputs.iterator(); iterator.hasNext();) {
                                GKInstance output = (GKInstance) iterator.next();
                                addEdge(name2, output, entityToNameMap, true, buffer);
                            }
                        }
                    }
                }
                else if (inputs.size() == 1) {
                    GKInstance input = (GKInstance) inputs.get(0);
                    if (outputs == null || outputs.size() == 0) {
                        addReactionNode(reaction.getDBID() + "", buffer);
                        addEdge(input, reaction.getDBID() + "", entityToNameMap, true, buffer);
                    }
                    else if (outputs.size() == 1) {
                        GKInstance output = (GKInstance) outputs.get(0);
                        addEdge(input, output, reaction.getDBID(), entityToNameMap, true, buffer);
                    }
                    else if (outputs.size() > 1) {
                        String rxtNodeName = reaction.getDBID() + "";
                        addReactionNode(rxtNodeName, buffer);
                        addEdge(input, rxtNodeName, entityToNameMap, false, buffer);
                        for (Iterator it1 = outputs.iterator(); it1.hasNext();) {
                            GKInstance output = (GKInstance) it1.next();
                            addEdge(rxtNodeName, output, entityToNameMap, true, buffer);
                        }
                    }
                }
                else if (inputs.size() > 1) {
                    if (outputs == null || outputs.size() == 0) {
                        String name1 = reaction.getDBID() + "_1";
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name1, buffer);
                        addReactionNode(name2, buffer);
                        addEdge(name1, name2, false, buffer);
                        for (Iterator it1 = inputs.iterator(); it1.hasNext();) {
                            GKInstance input = (GKInstance) it1.next();
                            addEdge(input, name1, entityToNameMap, false, buffer);
                        }
                    }
                    else if (outputs.size() == 1) {
                        String name = reaction.getDBID() + "";
                        addReactionNode(name, buffer);
                        for (Iterator it1 = inputs.iterator(); it1.hasNext();)
                            addEdge((GKInstance)it1.next(), name, entityToNameMap, false, buffer);
                        addEdge(name, (GKInstance)outputs.get(0), entityToNameMap, true, buffer);
                    }
                    else if (outputs.size() > 1) {
                        String name1 = reaction.getDBID() + "_1";
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name1, buffer);
                        addReactionNode(name2, buffer);
                        addEdge(name1, name2, false, buffer);
                        for (Iterator it1 = inputs.iterator(); it1.hasNext();)
                            addEdge((GKInstance)it1.next(), name1, entityToNameMap, false, buffer);
                        for (Iterator it1 = outputs.iterator(); it1.hasNext();)
                            addEdge(name2, (GKInstance)it1.next(), entityToNameMap, true, buffer);
                    }
                }
            }
            else { // Need to add the central point for the Reaction edge
                String reactionName = reaction.getDBID() + "";
                addReactionNode(reactionName, buffer);
                for (Iterator it1 = catalysts.iterator(); it1.hasNext();)
                    addCatalystEdge((GKInstance)it1.next(), reactionName, entityToNameMap, buffer);
                // A little weird here: Have to use regulation to get the type of Regulation
                if (regulators.size() > 0) { // Regulations should NOT be null in this case
                    for (Iterator it1 = regulations.iterator(); it1.hasNext();)
                        addRegulatorEdge((GKInstance)it1.next(), reactionName, entityToNameMap, buffer);
                }
                if (inputs == null || inputs.size() == 0) {
                    String name1 = reaction.getDBID() + "_1";
                    addReactionNode(name1, buffer);
                    addEdge(name1, reactionName, false, buffer);
                    if (outputs == null || outputs.size() == 0) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, true, buffer);
                    }
                    else if (outputs.size() == 1) {
                        addEdge(reactionName, (GKInstance)outputs.get(0), entityToNameMap, true, buffer);
                    }
                    else {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, false, buffer);
                        for (Iterator it1 = outputs.iterator(); it1.hasNext();)
                            addEdge(name2, (GKInstance)it1.next(), entityToNameMap, true, buffer);
                    }
                }
                else if (inputs.size() == 1) {
                    GKInstance input = (GKInstance) inputs.get(0);
                    addEdge(input, reactionName, entityToNameMap, false, buffer);
                    if (outputs == null || outputs.size() == 0) {
                        String name1 = reaction.getDBID() + "_1";
                        addReactionNode(name1, buffer);
                        addEdge(reactionName, name1, true, buffer);
                    }
                    else if (outputs.size() == 1) {
                        GKInstance output = (GKInstance) outputs.get(0);
                        addEdge(reactionName, output, entityToNameMap, true, buffer);
                    }
                    else if (outputs.size() > 1) {
                        String name1 = reaction.getDBID() + "_1";
                        addReactionNode(name1, buffer);
                        addEdge(reactionName, name1, false, buffer);
                        for (Iterator it1 = outputs.iterator(); it1.hasNext();) 
                            addEdge(name1, (GKInstance)it1.next(), entityToNameMap, true, buffer);
                    }
                }
                else if (inputs.size() > 1) {
                    String name1 = reaction.getDBID() + "_1";
                    addReactionNode(name1, buffer);
                    addEdge(name1, reactionName, false, buffer);
                    for (Iterator it1 = inputs.iterator(); it1.hasNext();)
                        addEdge((GKInstance)it1.next(), name1, entityToNameMap, false, buffer);
                    if (outputs == null || outputs.size() == 0) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, true, buffer);
                    }
                    else if (outputs.size() == 1) {
                        GKInstance output = (GKInstance) outputs.get(0);
                        addEdge(reactionName, output, entityToNameMap, true, buffer);
                    }
                    else if (outputs.size() > 1) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, false, buffer);
                        for (Iterator it1 = outputs.iterator(); it1.hasNext();)
                            addEdge(name2, (GKInstance)it1.next(), entityToNameMap, true, buffer);
                    }              
                }
            }
        }
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private void addReactionNode(String nodeName, StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(nodeName);
        if (useURL) {
            buffer.append("\" [label=\"\" URL=\"::");
            buffer.append(nodeName);
            buffer.append("\" shape=circle style=filled];\n");
        }
        else {
            buffer.append("\" [shape=point];\n");
        }
        nameToIdMap.put(nodeName, extractIdFromName(nodeName));
    }
    
    private void addEdge(String name1, String name2, boolean needArrow, StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(name1);
        buffer.append("\" -> \"");
        buffer.append(name2);
        if (needArrow)
            buffer.append("\";\n");
        else
            buffer.append("\" [dir=none];\n");
        nameToIdMap.put(name1 + "->" + name2, extractIdFromName(name1));
    }
    
    private void addEdge(String name1, 
                         GKInstance entity, 
                         Map entityToNameMap,
                         boolean needArrow,
                         StringBuffer buffer) {
        // Add an edge to output
        buffer.append("\t\"");
        buffer.append(name1);
        buffer.append("\" -> \"");
        buffer.append(entityToNameMap.get(entity));
        if (needArrow)
            buffer.append("\";\n");
        else
            buffer.append("\" [dir=none]\";\n");
        nameToIdMap.put(name1 + "->" + entityToNameMap.get(entity), extractIdFromName(name1));
    }
    
    private void addEdge(GKInstance entity,
                         String name1, 
                         Map entityToNameMap, 
                         boolean needArrow,
                         StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(entityToNameMap.get(entity));
        buffer.append("\" -> \"");
        buffer.append(name1);
        if (needArrow)
            buffer.append("\";\n");
        else
            buffer.append("\" [dir=none]");
        nameToIdMap.put(entityToNameMap.get(entity) + "->" + name1,
                        extractIdFromName(name1));
    }
    
    private Long extractIdFromName(String name) {
        int index = name.indexOf("_");
        if (index == -1)
            return new Long(name);
        else {
            return new Long(name.substring(0, index));
        }
    }
    
    private void addCatalystEdge(GKInstance entity,
                                 String rxtNodeName,
                                 Map entityToNameMap,
                                 StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(entityToNameMap.get(entity));
        buffer.append("\" -> \"");
        buffer.append(rxtNodeName);
        buffer.append("\" [dir=none style=dotted];\n");
        nameToIdMap.put(entityToNameMap.get(entity) + "->" + rxtNodeName,
                        extractIdFromName(rxtNodeName));
    }
    
    private void addRegulatorEdge(GKInstance regulation,
                                  String rxtNodeName,
                                  Map entityToNameMap,
                                  StringBuffer buffer) throws Exception {
        GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
        if (!regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
            return;
        buffer.append("\t\"");
        buffer.append(entityToNameMap.get(regulator));
        buffer.append("\" -> \"");
        buffer.append(rxtNodeName);
        buffer.append("\" [dir=none style=dotted");
        SchemaClass schemaCls = regulation.getSchemClass();
        if (schemaCls.isa(ReactomeJavaConstants.NegativeRegulation))
            buffer.append(" arrowhead=odot");
        else if (schemaCls.isa(ReactomeJavaConstants.Requirement))
            buffer.append(" arrowhead=invdot");
        else
            buffer.append(" arrowhead=dot");
        buffer.append("];\n");
        nameToIdMap.put(entityToNameMap.get(regulator) + "->" + rxtNodeName,
                        extractIdFromName(rxtNodeName));
    }
    
    private void addEdge(GKInstance entity1,
                         GKInstance entity2, 
                         Long reactionId, 
                         Map entityToNameMap,
                         boolean needArrow, StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(entityToNameMap.get(entity1));
        buffer.append("\" -> \"");
        buffer.append(entityToNameMap.get(entity2));
        if (needArrow)
            buffer.append("\";\n");
        else
            buffer.append("\" [dir=none];\n");
        // No quotation marks in svg output
        String edgeName = entityToNameMap.get(entity1) + "->" + entityToNameMap.get(entity2);
        nameToIdMap.put(edgeName,
                        reactionId);
    }
    
    private void generateNodes(List entities,
                               Map entityToNameMap,
                               Map nameToEntityMap,
                               StringBuffer buffer) {
        if (entities == null || entities.size() == 0)
            return;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            GKInstance entity = (GKInstance) it.next();
            String name = generateUniqueName(entity, nameToEntityMap, entityToNameMap);
            if (name == null)
                continue; // entity has been added
            buffer.append("\t\"");
            buffer.append(name);
            String label = generateLabel(name);
            buffer.append("\" [label=\"");
            buffer.append(label);
            if (useURL) {
                buffer.append("\" URL=\"");
                buffer.append(entity.getDBID());
                buffer.append("::"); // Used as a delimiter
                buffer.append(label);
            }
            if (entity.getSchemClass().isa(ReactomeJavaConstants.Complex))
                buffer.append("\" shape=octagon];\n");
            else
                buffer.append("\"];\n");
        }
    }
    
    private String generateLabel(String name) {
        char[] chars = name.toCharArray();
        StringBuffer builder = new StringBuffer();
        int TOTAL_LINE_LENGTH = 20;
        int lineLength = 0;
        for (int i = 0; i < chars.length; i++) {
            if (lineLength < TOTAL_LINE_LENGTH) {
                builder.append(chars[i]);
                lineLength ++;
            }
            else if (Character.isWhitespace(chars[i]) || 
                    chars[i] == '-' ||
                    chars[i] == '_') {
                builder.append(chars[i]).append("\\n");
                lineLength = 0;
            }
            else {
                builder.append(chars[i]);
                lineLength ++;
            }
        }
        return builder.toString();
    }
    
    private String generateUniqueName(GKInstance entity, 
                                      Map nameToEntityMap,
                                      Map entityToNameMap) {
        String name = entity.getDisplayName();
        GKInstance entityInMap = (GKInstance) nameToEntityMap.get(name);
        if (entityInMap == null) {
            nameToEntityMap.put(name, entity);
            entityToNameMap.put(entity, name);
            nameToIdMap.put(name, entity.getDBID());
            return name;
        }
        else {
            if (entityInMap == entity)
                return null;
            else {
                // Another entity has the same name as this entity
                int c = 1;
                while (true) {
                    name += c;
                    entityInMap = (GKInstance) nameToEntityMap.get(name);
                    if (entityInMap == null) {
                        nameToEntityMap.put(name, entity);
                        entityToNameMap.put(entity, name);
                        nameToIdMap.put(name, entity.getDBID());
                        return name;
                    }
                    else if (entityInMap == entity) {
                        return null;
                    }
                    c ++;
                }
            }
        }
    }
    
    private void listReactions(GKInstance pathway, Set reactions) throws Exception {
        // Do a check in case a call starts with a Reaction
        if (pathway.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            reactions.add(pathway);
            return;
        }
        List components = null;
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) 
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) 
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm))
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            if (comp.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                reactions.add(comp);
            else
                listReactions(comp, reactions);
        }
    }
    
    public String generatePathwayDiagramInSVGForId(GKInstance pathway, 
                                                   String reactomeUrl,
                                                   String dotPath) throws Exception {
        if (dotPath == null)
            throw new IllegalStateException("dot path is not set!");
        // Check if dot is there
        File file = new File(dotPath);
        if (!file.exists())
            throw new IllegalStateException("dot cannot be found: " + dotPath);
        // Load the Pathway first
        String exeName = dotPath + " -Tsvg";
        Process process = Runtime.getRuntime().exec(exeName);
        OutputStream os = process.getOutputStream();
        String dotGraph = generateDot(pathway);
        if (dotGraph == null)
            return generateEmptySVG();
        // Check its size. If it is too big, just generate a too big string
        if (dotGraph.length() / 1024 > FILE_SIZE_LIMIT)
            return generateFileTooBigSVG(reactomeUrl, pathway);
        os.write(dotGraph.getBytes());
        os.flush();
        os.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuffer builder = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        process.destroy();
        String svgString = builder.toString();
        if (reactomeUrl != null)
            return addLinksToSvg(svgString, reactomeUrl);
        else
            return svgString;
    }
    
    public void generatePathwayDiagramInPNG(GKInstance pathway,
                                            String dotPath,
                                            File output) throws Exception {
        if (dotPath == null)
            throw new IllegalStateException("dot path is not set!");
        // Check if dot is there
        File file = new File(dotPath);
        if (!file.exists())
            throw new IllegalStateException("dot cannot be found: " + dotPath);
        // Load the Pathway first
        String exeName = dotPath + " -Tpng";
        Process process = Runtime.getRuntime().exec(exeName);
        OutputStream os = process.getOutputStream();
        String dotGraph = generateDot(pathway);
//        if (dotGraph == null)
//            return generateEmptySVG();
//        // Check its size. If it is too big, just generate a too big string
//        if (dotGraph.length() / 1024 > FILE_SIZE_LIMIT)
//            return generateFileTooBigSVG(reactomeUrl, pathway);
        os.write(dotGraph.getBytes());
        os.flush();
        os.close();
        byte[] c = new byte[1024 * 10];
        int read = 0;
        FileOutputStream fos = new FileOutputStream(output);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = process.getInputStream();
        while ((read = is.read(c)) > 0) {
            bos.write(c, 0, read);
        }
        bos.close();
        fos.close();
        is.close();
        process.destroy();
    }
    
    private String generateEmptySVG() {
        String emptyString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                             "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n" + 
                             "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" contentStyleType=\"text/css\">\n" +
                             "<text x=\"20\" y=\"50\" style=\"font-size:24.00;\">No SVG generated: probably entities cannot be found in the selected pathway.</text>\n" +
                             "</svg>";      
        return emptyString;
    }
    
    public String generateFileTooBigSVG(String reactomeUrl, GKInstance pathway) {
        String tooBigString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                              "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n" + 
                              "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" contentStyleType=\"text/css\">\n" +
                              "<text x=\"20\" y=\"50\" style=\"font-size:24.00;\">The selected pathway is too big for a SVG view.</text>\n" +
                              "<text x=\"20\" y=\"100\" style=\"font-size:24.00;\">Please use its child pathways.</text>\n" +
                              "<a xlink:href=\"" + reactomeUrl + "&amp;ID=" + pathway.getDBID() + "\">" + 
                              "<text x=\"20\" y=\"150\" style=\"font-size:24.00;stroke:navy\">" + pathway.getDisplayName() + ": " + pathway.getDBID() + "</text>\n" +
                              "</a>\n</svg>"; 
        return tooBigString;
    }
    
    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println("Usage java org.reactome.gkCurator.GraphvizDotGenerator " +
                               "dbHost dbName dbUser dbPwd dbPort eventID dotPath reactomeUrl");
            System.exit(0);
        }
        try {
            GraphvizDotGenerator generator = new GraphvizDotGenerator();
            MySQLAdaptor adaptor = new MySQLAdaptor(args[0],
                    args[1],
                    args[2],
                    args[3],
                    Integer.parseInt(args[4]));
            GKInstance pathway = adaptor.fetchInstance(new Long(args[5]));
            if (pathway != null) {
                String svg = generator.generatePathwayDiagramInSVGForId(pathway, args[7], args[6]);
                System.out.println(svg);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
