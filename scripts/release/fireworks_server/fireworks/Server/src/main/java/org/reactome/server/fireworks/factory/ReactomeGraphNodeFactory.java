package org.reactome.server.fireworks.factory;

import org.reactome.server.analysis.core.model.PathwayHierarchy;
import org.reactome.server.analysis.core.model.PathwayNode;
import org.reactome.server.analysis.core.model.PathwayRoot;
import org.reactome.server.fireworks.model.GraphNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ReactomeGraphNodeFactory {

    private Map<Long, GraphNode> map = new HashMap<Long, GraphNode>();

    private GraphNode graph;

    public ReactomeGraphNodeFactory(PathwayHierarchy hierarchy) {
        this.graph = new GraphNode(hierarchy.getSpecies().getSpeciesID(), hierarchy.getSpecies().getName());
        for (PathwayRoot pNode : hierarchy.getChildren()) {
            GraphNode gNode = getOrCreateGraphNode(pNode);
            this.graph.addChild(gNode);
            buildBranch(pNode, gNode);
        }
    }

    public GraphNode getGraphNode() {
        return graph;
    }

    private void buildBranch(PathwayNode pathwayNode, GraphNode graphNode) {
        for (PathwayNode pNode : pathwayNode.getChildren()) {
            GraphNode gNode = getOrCreateGraphNode(pNode);
            graphNode.addChild(gNode);
            buildBranch(pNode, gNode);
        }
    }

    private GraphNode getOrCreateGraphNode(PathwayNode node) {
        GraphNode rtn = this.map.get(node.getPathwayId());
        if (rtn == null) {
            rtn = new GraphNode(node);
            this.map.put(node.getPathwayId(), rtn);
        }
        return rtn;
    }
}
