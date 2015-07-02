package org.reactome.web.fireworks.model.factory;

import org.reactome.web.fireworks.data.RawEdge;
import org.reactome.web.fireworks.data.RawGraph;
import org.reactome.web.fireworks.data.RawNode;
import org.reactome.web.fireworks.data.factory.RawModelException;
import org.reactome.web.fireworks.data.factory.RawModelFactory;
import org.reactome.web.fireworks.model.Edge;
import org.reactome.web.fireworks.model.Graph;
import org.reactome.web.fireworks.model.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class ModelFactory {

    public static Graph getGraph(String json) throws ModelException {
        RawGraph graph;
        try {
            graph = RawModelFactory.getModelObject(RawGraph.class, json);
        } catch (RawModelException e) {
            throw new ModelException(e.getMessage(), e);
        }

        Set<Node> nodes = new HashSet<Node>();
        Map<Long, Node> map = new HashMap<Long, Node>();
        for (RawNode rNode : graph.getNodes()) {
            Node node = new Node(rNode);
            //TODO: Fix the bug on the server side and remove the condition here :)
            if(!map.keySet().contains(rNode.getDbId())){
                map.put(rNode.getDbId(), node);
                nodes.add(node);
            }

        }

        Set<Edge> edges = new HashSet<Edge>();
        for (RawEdge rEdge : graph.getEdges()) {
            Node from = map.get(rEdge.getFrom());
            Node to = map.get(rEdge.getTo());
            edges.add(from.addChild(to));
        }

        return new Graph(graph.getSpeciesId(), nodes, edges);
    }

}
