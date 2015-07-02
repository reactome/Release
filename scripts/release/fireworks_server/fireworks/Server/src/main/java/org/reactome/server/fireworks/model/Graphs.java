package org.reactome.server.fireworks.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This is a dummy class just used to serialise its content to a file.
 * The aim is to reduce the file size from the AnalysisData to this one
 * so loading time gets reduced in case the user wants to play with the
 * bursts configuration files in order to get better Fireworks display,
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Graphs {

    List<GraphNode> graphNodes;

    public Graphs() {
        this.graphNodes = new LinkedList<GraphNode>();
    }

    public boolean addGraphNode(GraphNode graphNode){
        return this.graphNodes.add(graphNode);
    }

    public List<GraphNode> getGraphNodes() {
        return graphNodes;
    }
}
