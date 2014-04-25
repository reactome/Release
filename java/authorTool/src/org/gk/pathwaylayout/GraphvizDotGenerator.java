/*
 * This class is used to create a Graphviz dot file for a Pathway class.
 * Created on Nov 18, 2005
 *
 */
package org.gk.pathwaylayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
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
    // This map is used to map the label in svg back to Reactome links
    private Map nameToIdMap;
    // flag to control if URL should be added to nodes. A URL is needed
    // to be output as cmap
    private boolean useURL;
    
    private static final String SMALLMOL_COLOR = "FFFFFF";
    private static final String CANDIDATESET_COLOR = "0064C8";
    private static final String OPENSET_COLOR = "00C864";
    private static final String GEE_COLOR = "C86464";
    private static final String DEFINEDSET_COLOR = "00C8C8";
    private static final String PROTEIN_COLOR = "B0C4DE";
    private static final String COMPLEX_COLOR = "FF8247";
    private static final String SEQUENCE_COLOR = "DEB0C4";
    private static final String OTHER_COLOR = "C8C8C8";
    
    // Entities which will be drawn as a separate copy for each occurrance in pathway (e.g. H20, ATP, etc)
    private Set entitiesDisplayedAsMultipleNodes = new HashSet();
    private Map entitiesToNodeIdListMap = new HashMap();
    private static final int SKY_COORDINATE_SCALING_FACTOR = 10;
     
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
        if (reactions.size() == 0) {
        		System.err.println("No reactions in " + pathway);
            return null;
        }
        return convertToDotGraph3(reactions);
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
        buffer.append("\t node [shape=rectangle, fontsize=10, width=0.1, height=0.1, fontname=Helvetica, style=\"filled,rounded\"];\n");
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

    private String convertToDotGraph2(Set reactions) throws Exception {
        // Convert to dot-recognizable graph
        StringBuffer buffer = new StringBuffer();
        buffer.append("digraph test {");
        buffer.append("\n");
        // The following three properties are for dot
        buffer.append("\t nodesep=\"" + nodeSep + "\";\n"); 
        buffer.append("\t ranksep=\"" + rankSep + "\";\n");
        // Make width and height small enough so that the minimum size can be picked up.
        //buffer.append("\t node [shape=rectangle, ?fontsize=10, width=0.1, height=0.1];\n");
        buffer.append("\t node [shape=rectangle, fontsize=10, width=0.1, height=0.1, fontname=Helvetica, style=\"filled,rounded\"];\n");
        //buffer.append("\t node [shape=rectangle, ?style=\"rounded,filled\", fontsize=10, width=0.1, height=0.1, fontname=Helvetica];\n");
        // The following property is for neato
        buffer.append("\t edge [len=\"" + edgeLen + "\"];\n");
        Map entityToNameMap = new HashMap();
        Map nameToEntityMap = new HashMap();
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
            Map inputId2count = generateNodes(inputs, buffer);
            List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
            Map outputId2count = generateNodes(outputs, buffer);
            List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            List catalysts = new ArrayList();
            Map catalystId2count = null;
            if (cas != null && cas.size() > 0) {
                for (Iterator it1 = cas.iterator(); it1.hasNext();) {
                    GKInstance ca = (GKInstance) it1.next();
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    if (catalyst != null)
                        catalysts.add(catalyst);
                }
                catalystId2count = generateNodes(catalysts, buffer);
            }
            List regulators = new ArrayList();
            List posRegulators = new ArrayList();
            List negRegulators = new ArrayList();
            List reqRegulators = new ArrayList();
            Map regulatorId2count = null;
            Map posRegulatorId2count = null;
            Map negRegulatorId2count = null;
            Map reqRegulatorId2count = null;
            Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulations != null && regulations.size() > 0) {
                for (Iterator it1 = regulations.iterator(); it1.hasNext();) {
                    GKInstance regulation = (GKInstance) it1.next();
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                        if (regulation.getSchemClass().isa(ReactomeJavaConstants.Requirement)) {
                        		reqRegulators.add(regulator);
                        		regulators.add(regulator);
                        } else if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                    			posRegulators.add(regulator);
                    			regulators.add(regulator);
                        } else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                        		negRegulators.add(regulator);
                        		regulators.add(regulator);
                        }
                    }
                }
                regulatorId2count = generateNodes(regulators, buffer);
                StringBuffer junk = new StringBuffer();
                posRegulatorId2count = generateNodes(posRegulators, junk);
                negRegulatorId2count = generateNodes(negRegulators, junk);
                reqRegulatorId2count = generateNodes(reqRegulators, junk);
            }
            // Check if any accessory nodes are needed
            if ((catalystId2count == null) && (regulatorId2count == null)) {
                if (inputId2count == null) {
                    if (outputId2count != null) {
                        if (outputId2count.keySet().size() == 1) {
                            String output = (String) outputId2count.keySet().iterator().next();
                            // Need an extra node
                            addReactionNode(reaction.getDBID() + "", buffer);
                            // Add an edge to output
                            addEdge(reaction.getDBID() + "", output, true, buffer);
                        }
                        else if (outputId2count.keySet().size() > 1) {
                            // Need two extra nodes
                            String name1 = reaction.getDBID() + "_1";
                            addReactionNode(name1, buffer);
                            String name2 = reaction.getDBID() + "_2";
                            addReactionNode(name2, buffer);
                            // Add a link between these two edges
                            addEdge(name1, name2, false, buffer);
                            // Create new edge
                            for (Iterator iterator = outputId2count.keySet().iterator(); iterator.hasNext();) {
                            		String output = (String) iterator.next();
                            		addEdge(name2, output, true, buffer);
                            }
                        }
                    }
                }
                else if (inputId2count.keySet().size() == 1) {
                    String input = (String) inputId2count.keySet().iterator().next();
                    if (outputId2count == null) {
                        addReactionNode(reaction.getDBID() + "", buffer);
                        addEdge(input, reaction.getDBID() + "", true, buffer);
                    }
                    else if (inputId2count.keySet().size() == 1) {
                        String output = (String) outputId2count.keySet().iterator().next();
                        addEdge(input, output, reaction.getDBID(), true, buffer);
                    }
                    else if (outputId2count.keySet().size() > 1) {
                        String rxtNodeName = reaction.getDBID() + "";
                        addReactionNode(rxtNodeName, buffer);
                        addEdge(input, rxtNodeName, false, buffer);
                        for (Iterator it1 = outputId2count.keySet().iterator(); it1.hasNext();) {
                            String output = (String) it1.next();
                            addEdge(rxtNodeName, output, true, buffer);
                        }
                    }
                }
                else if (inputId2count.keySet().size() > 1) {
                    if (outputId2count == null) {
                        String name1 = reaction.getDBID() + "_1";
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name1, buffer);
                        addReactionNode(name2, buffer);
                        addEdge(name1, name2, false, buffer);
                        for (Iterator it1 = inputId2count.keySet().iterator(); it1.hasNext();) {
                            String input = (String) it1.next();
                            addEdge(input, name1, false, buffer);
                        }
                    }
                    else if (outputId2count.keySet().size() == 1) {
                        String name = reaction.getDBID() + "";
                        addReactionNode(name, buffer);
                        for (Iterator it1 = inputId2count.keySet().iterator(); it1.hasNext();)
                            addEdge((String)it1.next(), name, false, buffer);
                        addEdge(name, (String)outputId2count.keySet().iterator().next(), true, buffer);
                    }
                    else if (outputId2count.keySet().size() > 1) {
                        String name1 = reaction.getDBID() + "_1";
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name1, buffer);
                        addReactionNode(name2, buffer);
                        addEdge(name1, name2, false, buffer);
                        for (Iterator it1 = inputId2count.keySet().iterator(); it1.hasNext();)
                            addEdge((String)it1.next(), name1, false, buffer);
                        for (Iterator it1 = outputId2count.keySet().iterator(); it1.hasNext();)
                            addEdge(name2, (String)it1.next(), true, buffer);
                    }
                }
            }
            else { // Need to add the central point for the Reaction edge
                String reactionName = reaction.getDBID() + "";
                addReactionNode(reactionName, buffer);
                for (Iterator it1 = catalystId2count.keySet().iterator(); it1.hasNext();)
                    addCatalystEdge((String)it1.next(), reactionName, buffer);
                // A little weird here: Have to use regulation to get the type of Regulation
                if (regulatorId2count != null && regulatorId2count.keySet().size() > 0) { // Regulations should NOT be null in this case
                		if (posRegulatorId2count != null) {
                			for (Iterator it1 = posRegulatorId2count.keySet().iterator(); it1.hasNext();)
                                addPosRegulatorEdge((String)it1.next(), reactionName, buffer);
                		}
                		if (negRegulatorId2count != null) {
                			for (Iterator it1 = negRegulatorId2count.keySet().iterator(); it1.hasNext();)
                                addNegRegulatorEdge((String)it1.next(), reactionName, buffer);
                		}
                		if (reqRegulatorId2count != null) {
                			for (Iterator it1 = reqRegulatorId2count.keySet().iterator(); it1.hasNext();)
                                addReqRegulatorEdge((String)it1.next(), reactionName, buffer);
                		}
                }
                if (inputId2count == null) {
                    String name1 = reaction.getDBID() + "_1";
                    addReactionNode(name1, buffer);
                    addEdge(name1, reactionName, false, buffer);
                    if (outputId2count == null) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, true, buffer);
                    }
                    else if (outputId2count.keySet().size() == 1) {
                        addEdge(reactionName, (String)outputId2count.keySet().iterator().next(), true, buffer);
                    }
                    else {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, false, buffer);
                        for (Iterator it1 = outputId2count.keySet().iterator(); it1.hasNext();)
                            addEdge(name2, (String)it1.next(), true, buffer);
                    }
                }
                else if (inputId2count.keySet().size() == 1) {
                	   String input = (String) inputId2count.keySet().iterator().next();
                    addEdge(input, reactionName, false, buffer);
                    if (outputId2count == null) {
                        String name1 = reaction.getDBID() + "_1";
                        addReactionNode(name1, buffer);
                        addEdge(reactionName, name1, true, buffer);
                    }
                    else if (outputId2count.keySet().size() == 1) {
                        String output = (String) outputId2count.keySet().iterator().next();
                        addEdge(reactionName, output, true, buffer);
                    }
                    else if (outputId2count.keySet().size() > 1) {
                        String name1 = reaction.getDBID() + "_1";
                        addReactionNode(name1, buffer);
                        addEdge(reactionName, name1, false, buffer);
                        for (Iterator it1 = outputId2count.keySet().iterator(); it1.hasNext();) 
                            addEdge(name1, (String)it1.next(), true, buffer);
                    }
                }
                else if (inputId2count.keySet().size() > 1) {
                    String name1 = reaction.getDBID() + "_1";
                    addReactionNode(name1, buffer);
                    addEdge(name1, reactionName, false, buffer);
                    for (Iterator it1 = inputId2count.keySet().iterator(); it1.hasNext();)
                        addEdge((String)it1.next(), name1, false, buffer);
                    if (outputId2count == null) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, true, buffer);
                    }
                    else if (outputId2count.keySet().size() == 1) {
                        String output = (String) outputId2count.keySet().iterator().next();
                        addEdge(reactionName, output, true, buffer);
                    }
                    else if (outputId2count.keySet().size() > 1) {
                        String name2 = reaction.getDBID() + "_2";
                        addReactionNode(name2, buffer);
                        addEdge(reactionName, name2, false, buffer);
                        for (Iterator it1 = outputId2count.keySet().iterator(); it1.hasNext();)
                            addEdge(name2, (String)it1.next(), true, buffer);
                    }              
                }
            }
        }
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private String convertToDotGraph3(Set reactions) throws Exception {
        // Convert to dot-recognizable graph
        StringBuffer buffer = new StringBuffer();
        buffer.append("digraph test {");
        buffer.append("\n");
        // The following three properties are for dot
        buffer.append("\t nodesep=\"" + nodeSep + "\";\n"); 
        buffer.append("\t ranksep=\"" + rankSep + "\";\n");
        // Make width and height small enough so that the minimum size can be picked up.
        buffer.append("\t node [shape=rectangle, fontsize=10, width=0.1, height=0.1, fontname=Helvetica, style=\"filled,rounded\"];\n");
        // The following property is for neato
        buffer.append("\t edge [len=\"" + edgeLen + "\" fontname=Helvetica fontsize=10" + "];\n");
//        Map entityToNameMap = new HashMap();
//        Map nameToEntityMap = new HashMap();
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
            Map inputId2count = generateNodes(inputs, buffer);
            List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
            Map outputId2count = generateNodes(outputs, buffer);
            List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            List catalysts = new ArrayList();
            Map catalystId2count = null;
            if (cas != null && cas.size() > 0) {
                for (Iterator it1 = cas.iterator(); it1.hasNext();) {
                    GKInstance ca = (GKInstance) it1.next();
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    if (catalyst != null)
                        catalysts.add(catalyst);
                }
                catalystId2count = generateNodes(catalysts, buffer);
            }
            List regulators = new ArrayList();
            List posRegulators = new ArrayList();
            List negRegulators = new ArrayList();
            List reqRegulators = new ArrayList();
            Map regulatorId2count = null;
            Map posRegulatorId2count = null;
            Map negRegulatorId2count = null;
            Map reqRegulatorId2count = null;
            Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulations != null && regulations.size() > 0) {
                for (Iterator it1 = regulations.iterator(); it1.hasNext();) {
                    GKInstance regulation = (GKInstance) it1.next();
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                        if (regulation.getSchemClass().isa(ReactomeJavaConstants.Requirement)) {
                        		reqRegulators.add(regulator);
                        		regulators.add(regulator);
                        } else if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                    			posRegulators.add(regulator);
                    			regulators.add(regulator);
                        } else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                        		negRegulators.add(regulator);
                        		regulators.add(regulator);
                        }
                    }
                }
                regulatorId2count = generateNodes(regulators, buffer);
                StringBuffer junk = new StringBuffer();
                posRegulatorId2count = generateNodes(posRegulators, junk);
                negRegulatorId2count = generateNodes(negRegulators, junk);
                reqRegulatorId2count = generateNodes(reqRegulators, junk);
            }
            String rnid = reaction.getDBID().toString();
            addReactionNode(reaction, rnid, buffer);
            if (inputId2count != null) {
            		for (Iterator i = inputId2count.keySet().iterator(); i.hasNext();) {
            			String enid = (String)i.next();
            			Integer count = (Integer)inputId2count.get(enid);
            			if (count.intValue() > 1) {
            				addEdge(enid, rnid, false, buffer, count.toString());
            			} else {
            				addEdge(enid, rnid, false, buffer);
            			}
            		}
            }
            if (outputId2count != null) {
        			for (Iterator i = outputId2count.keySet().iterator(); i.hasNext();) {
        				String enid = (String)i.next();
        				Integer count = (Integer)outputId2count.get(enid);
        				if (count.intValue() > 1) {
        					addEdge(rnid, enid, true, buffer, count.toString());
        				} else {
        					addEdge(rnid, enid, true, buffer);
        				}
        			}
            }
            if (catalystId2count != null) {
        			for (Iterator i = catalystId2count.keySet().iterator(); i.hasNext();) {
        				String enid = (String)i.next();
        				addCatalystEdge(enid, rnid, buffer);
        			}
            }
            if (posRegulatorId2count != null) {
    				for (Iterator i = posRegulatorId2count.keySet().iterator(); i.hasNext();) {
    					String enid = (String)i.next();
    					addPosRegulatorEdge(enid, rnid, buffer);
    				}
            }
            if (negRegulatorId2count != null) {
				for (Iterator i = negRegulatorId2count.keySet().iterator(); i.hasNext();) {
					String enid = (String)i.next();
					addNegRegulatorEdge(enid, rnid, buffer);
				}
            }
            if (reqRegulatorId2count != null) {
				for (Iterator i = reqRegulatorId2count.keySet().iterator(); i.hasNext();) {
					String enid = (String)i.next();
					addReqRegulatorEdge(enid, rnid, buffer);
				}
            }
        }
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private void addReactionNode(String nodeName, StringBuffer buffer) {
        buffer.append("\t\"");
        buffer.append(nodeName);
        buffer.append("\" [label=\"\" URL=\" ");
        buffer.append("\" shape=circle style=filled color=black];\n");
        nameToIdMap.put(nodeName, extractIdFromName(nodeName));
    }
    
    private void addReactionNode(GKInstance r, String nodeName, StringBuffer buffer) throws Exception {
        buffer.append("\t\"");
        buffer.append(nodeName);
        buffer.append("\" [label=\"\"");
        buffer.append(getURLString(r));
        buffer.append(reactionPositionString(r));
        buffer.append(" shape=circle style=filled color=black];\n");
    }
    
    private String reactionPositionString(GKInstance reaction) throws Exception {
    		Collection reactionCoordinatesC = reaction.getReferers(ReactomeJavaConstants.locatedEvent);
    		if (reactionCoordinatesC == null || reactionCoordinatesC.isEmpty())
    			return "";
    		GKInstance reactionCoordinates = (GKInstance)reactionCoordinatesC.iterator().next();
    		int sx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceX)).intValue();
    		int sy = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceY)).intValue();
    		int tx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetX)).intValue();
    		int ty = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetY)).intValue();
    		return " pos=\"" + (int)((sx + tx) / 2 * SKY_COORDINATE_SCALING_FACTOR) + "," + (int)((sy + ty) / 2 * SKY_COORDINATE_SCALING_FACTOR) + "!\"";
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
    }
    
    private void addEdge(String name1, String name2, boolean needArrow, StringBuffer buffer, String label) {
        buffer.append("\t\"");
        buffer.append(name1);
        buffer.append("\" -> \"");
        buffer.append(name2);
        buffer.append("\" [label=\"" + label + "\"");
        if (needArrow)
            buffer.append("];\n");
        else
            buffer.append(" dir=none];\n");
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
 
    private void addCatalystEdge(String entityNodeId,String rxtNodeName,StringBuffer buffer) {
    		buffer.append("\t\"");
    		buffer.append(entityNodeId);
    		buffer.append("\" -> \"");
    		buffer.append(rxtNodeName);
    		buffer.append("\" [dir=none style=dashed];\n");
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

    private void addPosRegulatorEdge(String regulator, String rxtNodeName, StringBuffer buffer) {
    		buffer.append("\t\"");
    		buffer.append(regulator);
    		buffer.append("\" -> \"");
    		buffer.append(rxtNodeName);
    		buffer.append("\" [dir=none style=dotted");
    		buffer.append(" arrowhead=dot];\n");
}

    private void addNegRegulatorEdge(String regulator, String rxtNodeName, StringBuffer buffer) {
		buffer.append("\t\"");
		buffer.append(regulator);
		buffer.append("\" -> \"");
		buffer.append(rxtNodeName);
		buffer.append("\" [dir=none style=dotted");
		buffer.append(" arrowhead=odot];\n");
}
    
    private void addReqRegulatorEdge(String regulator, String rxtNodeName, StringBuffer buffer) {
		buffer.append("\t\"");
		buffer.append(regulator);
		buffer.append("\" -> \"");
		buffer.append(rxtNodeName);
		buffer.append("\" [dir=none style=dotted");
		buffer.append(" arrowhead=invdot];\n");
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
    
    private void addEdge(String node1,String node2, Long reactionId, boolean needArrow, StringBuffer buffer) {
    		buffer.append("\t\"");
    		buffer.append(node1);
    		buffer.append("\" -> \"");
    		buffer.append(node2);
    		if (needArrow)
    			buffer.append("\";\n");
    		else
    			buffer.append("\" [dir=none];\n");
    		// No quotation marks in svg output
    		String edgeName = node1 + "->" + node2;
    		nameToIdMap.put(edgeName,reactionId);
    }
    
    private void generateNodes(List entities,
                               Map entityToNameMap,
                               Map nameToEntityMap,
                               StringBuffer buffer) throws Exception {
        if (entities == null || entities.size() == 0)
            return;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            GKInstance entity = (GKInstance) it.next();
            String name = generateUniqueName(entity, nameToEntityMap, entityToNameMap);
            if (name == null)
                continue; // entity has been added
            buffer.append("\t\"");
            buffer.append(name);
            String label = generateLabel(entity.getAttributeValue("name").toString());
            buffer.append("\" [label=\"");
            buffer.append(label);
            buffer.append("\" URL=\" \"");
            buffer.append(getNodeRenderingParamsString(entity));
            buffer.append("];\n");
        }
    }

    private Map generateNodes(Collection entities, StringBuffer buffer) throws Exception {
    		if (entities == null || entities.size() == 0)
    			return null;
    		HashMap entityToCountMap = new HashMap();
    		HashMap entityToNodeIdMap = new HashMap();
    		for (Iterator it = entities.iterator(); it.hasNext();) {
    			GKInstance entity = (GKInstance) it.next();
    			Integer count;
    			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
    				entityToCountMap.put(entity,new Integer(1));
    			} else {
    				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
    				continue;
    			}
    			String nodeId = generateNodeUniqueId(entity);
    			if (nodeId == null) {
    				entityToNodeIdMap.put(entity, entity.getDBID().toString());
    				continue; // entity has been added
    			}
    			entityToNodeIdMap.put(entity, nodeId);
    			buffer.append("\t\"");
    			buffer.append(nodeId);
    			//String label = generateLabel(entity.getAttributeValue("name").toString());
    			String label = generateLabel(Utils.findShortestName(entity));
    			buffer.append("\" [label=\"");
    			buffer.append(label + "\"");
    			buffer.append(getURLString(entity));
    			buffer.append(getNodeRenderingParamsString(entity));
    			buffer.append("];\n");
    		}
    		HashMap nodeIdToCountMap = new HashMap();
    		for (Iterator i = entityToNodeIdMap.keySet().iterator(); i.hasNext();) {
    			GKInstance e = (GKInstance) i.next();
    			nodeIdToCountMap.put(entityToNodeIdMap.get(e), entityToCountMap.get(e));
    		}
    		return nodeIdToCountMap;
    }
    
    private String getURLString(GKInstance i) {
    		StringBuffer out = new StringBuffer();
    		out.append(" URL=\"/cgi-bin/eventbrowser?ID=" + i.getDBID());
    		if (i.getDbAdaptor() instanceof MySQLAdaptor) {
				out.append("&DB=" + ((MySQLAdaptor)i.getDbAdaptor()).getDBName());
			}
    		out.append("\"");
    		return out.toString();
    }
    
    public String findShortestName(GKInstance entity) throws Exception {
    		String shortest = entity.getDisplayName();
    		for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.name).iterator(); i.hasNext();) {
    			String n = (String) i.next();
    			if (n.length() < shortest.length())
    				shortest = n;
    		}
    		return shortest;
    }
    
    // Returns null if node exists already
    private String generateNodeUniqueId(GKInstance entity) {
    		List idList;
    		if ((idList = (List) entitiesToNodeIdListMap.get(entity)) == null) {
    			idList = new ArrayList();
    			String id = entity.getDBID().toString();
    			idList.add(id);
    			entitiesToNodeIdListMap.put(entity,idList);
    			return id;
    		} else {
    			if (entitiesDisplayedAsMultipleNodes.contains(entity)) {
    				String id = entity.getDBID() + "_" + (idList.size() + 1);
    				idList.add(id);
    				return id;
    			} else {
    				return null;
    			}
    		}
    }
    
    private String getNodeRenderingParamsString(GKInstance entity) throws Exception {
    		String clsName = entity.getSchemClass().getName();
    		if (clsName.equals(ReactomeJavaConstants.Complex)) {
    			return " color=\"#" + COMPLEX_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.CandidateSet)) {
    			return " color=\"#" + CANDIDATESET_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.DefinedSet)) {
    			return " color=\"#" + DEFINEDSET_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.OpenSet)) {
    			return " color=\"#" + OPENSET_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.GenomeEncodedEntity)) {
    			return " color=\"#" + GEE_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
    			GKInstance re;
    			if (((re = (GKInstance) entity.getAttributeValue(ReactomeJavaConstants.referenceEntity)) != null) 
    					&& re.getSchemClass().getName().equals(ReactomeJavaConstants.ReferencePeptideSequence)) {
    				return " color=\"#" + PROTEIN_COLOR + "\"";
    			}
    			return " color=\"#" + SEQUENCE_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.OtherEntity)) {
    			return " color=\"#" + OTHER_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.Polymer)) {
    			return " color=\"#" + COMPLEX_COLOR + "\"";
    		} else if (clsName.equals(ReactomeJavaConstants.SimpleEntity)) {
    			return " fillcolor=\"#" + SMALLMOL_COLOR + "\"";
    		}
    		return "";
    }
    
    private String generateLabel(String name) {
        char[] chars = name.toCharArray();
        StringBuffer builder = new StringBuffer();
        int TOTAL_LINE_LENGTH = 10;
        int lineLength = 0;
        for (int i = 0; i < chars.length; i++) {
            if (lineLength < TOTAL_LINE_LENGTH) {
                builder.append(chars[i]);
                lineLength ++;
            }
            else if ((Character.isWhitespace(chars[i]) ||
            		chars[i] == ':' ||
				chars[i] == '-' ||
                 chars[i] == '_') &&
                 i <= chars.length-3) {
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
        String name = entity.getDBID().toString();
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
        List components = null;
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) 
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm))
            components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            if (comp.getSchemClass().isa(ReactomeJavaConstants.Reaction))
                reactions.add(comp);
            else
                listReactions(comp, reactions);
        }
    }
    
    public String generatePathwayDiagramInFormat(
    		GKInstance pathway, 
		String dotPath,
		String format) throws Exception {
    		if (dotPath == null)
    			throw new IllegalStateException("dot path is not set!");
    		// Check if dot is there
    		File file = new File(dotPath);
    		if (!file.exists())
    		throw new IllegalStateException("dot cannot be found: " + dotPath);
    		// Load the Pathway first
    		String exeName = dotPath + " -T" + format;
    		Process process = Runtime.getRuntime().exec(exeName);
    		OutputStream os = process.getOutputStream();
    		String dotGraph = generateDot(pathway);
    		if (dotGraph == null)
    			return null;
    		//System.out.println(dotGraph);
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
    		return builder.toString();
    }
    
    public void outputPathwayDiagramInFormat(
    		GKInstance pathway, 
		String dotPath,
		String format) throws Exception {
    		if (dotPath == null)
    			throw new IllegalStateException("dot path is not set!");
    		// Check if dot is there
    		File file = new File(dotPath);
    		if (!file.exists())
    		throw new IllegalStateException("dot cannot be found: " + dotPath);
    		// Load the Pathway first
    		String exeName = dotPath + " -T" + format;
    		Process process = Runtime.getRuntime().exec(exeName);
    		OutputStream os = process.getOutputStream();
    		String dotGraph = generateDot(pathway);
    		if (dotGraph == null) {
    			return;
    		}
    		os.write(dotGraph.getBytes());
    		os.flush();
    		os.close();
   		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		String line = null;
    		int i;
    		while ((i = reader.read()) != -1) {
    			System.out.write(i);
    		}
    		reader.close();
    		process.destroy();
    }
    
    public void createAndStorePathwayDiagram(GKInstance pathway,
                                             MySQLAdaptor dba, 
                                             String dotString) throws Exception {    		
//    		Map nodeId2Node = new HashMap();
//    		Schema schema = dba.getSchema();
//    		GKInstance pathwayDiagram = new GKInstance(schema.getClassByName(ReactomeJavaConstants.PathwayDiagram));
//    		pathwayDiagram.setAttributeValue(ReactomeJavaConstants.representedInstance,pathway);
//    		pathwayDiagram.setIsInflated(true);
//    		StringBuffer graphAttributes = new StringBuffer();
//    		BufferedReader reader = new BufferedReader(new StringReader(dotString));
//    		String lineIn = null;
//    		String line = "";
//    		while ((lineIn = reader.readLine()) != null) {
//    			//System.out.println(lineIn);
//    			line += lineIn.trim();
//    			//System.out.println(line);
//        		if (line.endsWith("{") || line.startsWith("}")) {
//        			line = "";
//        			continue;
//        		}
//        		if (line.endsWith("\\")) {
//        			line = line.substring(0, line.length()-1);
//        			continue;
//        		}
//        		int i = line.indexOf("[");
//        		String renderingInfo = line.substring(i);
//        		String rest = line.substring(0,i-1);
//        		rest = rest.replaceAll("\"","");
//        		String[] ids = rest.split(" -> ");
//        		if (ids.length == 2) {
//        			// Edge
//        			// NOTE: the following code assumes that all Nodes are defined before edges
//        			GKInstance edge = new GKInstance(schema.getClassByName(ReactomeJavaConstants.DiagramEdge));
//        			edge.setIsInflated(true);
//        			GKInstance fromNode = (GKInstance) nodeId2Node.get(ids[0]);
//        			GKInstance toNode = (GKInstance) nodeId2Node.get(ids[1]);
//        			edge.setAttributeValue(ReactomeJavaConstants.fromNode,fromNode);
//        			edge.setAttributeValue(ReactomeJavaConstants.toNode,toNode);
//        			edge.setAttributeValue(ReactomeJavaConstants.dotString,line);
//        			pathwayDiagram.addAttributeValue(ReactomeJavaConstants.edges,edge);
//        		} else {
//        			if (ids[0].equals("graph") || ids[0].equals("edge") || ids[0].equals("node")) {
//        				// Graph attributes
//        				graphAttributes.append(line + "\n");
//        				if (renderingInfo.matches("bb=")) {
//        					renderingInfo = renderingInfo.replaceAll("]","").replaceAll("[","").replaceAll("\"","");
//        					String[] tmp = renderingInfo.split(",");
//        					pathwayDiagram.setAttributeValue(ReactomeJavaConstants.width, Integer.getInteger(tmp[2]));
//        					pathwayDiagram.setAttributeValue(ReactomeJavaConstants.height, Integer.getInteger(tmp[3]));
//        				}
//        			} else {
//        				// Node
//        				Long dbId = extractIdFromName(ids[0]);
//        				GKInstance representedInstance = dba.fetchInstance(dbId);
//        				GKInstance node = new GKInstance(schema.getClassByName(ReactomeJavaConstants.DiagramNode));
//        				node.setIsInflated(true);
//        				node.setAttributeValue(ReactomeJavaConstants.representedInstance,representedInstance);
//        				node.setAttributeValue(ReactomeJavaConstants.dotString,line);
//        				pathwayDiagram.addAttributeValue(ReactomeJavaConstants.nodes,node);
//        				nodeId2Node.put(ids[0],node);
//        			}
//        		}
//        		line = "";
//        	}
//    		pathwayDiagram.setAttributeValue(ReactomeJavaConstants.dotString,graphAttributes.toString());
//    		dba.storeInstance(pathwayDiagram);
    }

    public void setEntitiesDisplayedAsMultipleNodes(MySQLAdaptor dba, String[] names) throws Exception {
    		if (names == null) {
    			names = new String[]{"H+","ATP","ADP","CO2","CoA","AMP","orthophosphate","NAD+","NADH","NADP+","NADPH","FAD","FADH2","H2O","GTP","GDP","UTP","TPP"};
    		}
    		if (names.length == 0) return;
    		Collection instances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.SimpleEntity,ReactomeJavaConstants.name,"=",Arrays.asList(names));
    		for (Iterator ii = instances.iterator(); ii.hasNext(); ) {
    			entitiesDisplayedAsMultipleNodes.add(ii.next());
    		}
    		//System.out.println(entitiesDisplayedAsMultipleNodes);
    }
    
    public void reset() {
    		entitiesToNodeIdListMap.clear();
    }
    
    public void layoutReaction(GKInstance reaction) throws InvalidAttributeException, Exception {
    		//handle input,output and catalyst
    		//if input entity is output of a precedingevent, add edge between precedingevent and the entity
    		//if output entity is input of a following event, add edge between following event and the entity
		//if output entity is catalyst of a following event, add edge between following event and the entity
    		Map entityToCountMap = new HashMap();
    		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
    			GKInstance entity = (GKInstance) it.next();
    			Integer count;
    			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
    				entityToCountMap.put(entity,new Integer(1));
    			} else {
    				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
    				continue;
    			}
    			//make like between entity and current reaction
    			for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
    				GKInstance event = (GKInstance)it2.next();
    				if(!event.getSchemClass().getName().equals(ReactomeJavaConstants.Reaction))
    					continue;
    				if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
    					//make link between entity and preceding reaction
    				}
    			}
    		}
    }
    
    public static void main(String[] args) {
    		if (args.length < 7) {
    			System.out.println("Usage java org.gk.pathwaylayout.GraphvizDotGenerator " +
    			"dbHost dbName dbUser dbPwd dbPort eventID dotPath");
    			System.exit(0);
    		}
    		try {
    			GraphvizDotGenerator generator = new GraphvizDotGenerator();
    			MySQLAdaptor adaptor = new MySQLAdaptor(
    					args[0],
					args[1],
					args[2],
					args[3],
					Integer.parseInt(args[4]));
    			GKInstance pathway = adaptor.fetchInstance(new Long(args[5]));
    			if (pathway != null) {
    				generator.setEntitiesDisplayedAsMultipleNodes(adaptor,null);
    				String dotString = generator.generatePathwayDiagramInFormat(pathway, args[6], "dot");
    				if (dotString == null) {
    					
    				} else {
    					System.out.println(dotString);
    					generator.createAndStorePathwayDiagram(pathway, adaptor, dotString);
    				}
    			}
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    		}
    }
}
