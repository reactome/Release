package org.reactome.server.fireworks.factory;

import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.Event;
import org.reactome.core.model.Pathway;
import org.reactome.core.model.Species;
import org.reactome.server.analysis.core.model.SpeciesNodeFactory;
import org.reactome.server.fireworks.model.GraphNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class OrthologyGraphFactory {

    private GraphNode specieGraph;

    private Map<Long, GraphNode> dbId2HumanNode = new HashMap<Long, GraphNode>();
    private Map<Long, GraphNode> dbId2SpeciesNode = new HashMap<Long, GraphNode>();

    public OrthologyGraphFactory(GraphNode humanGraph, GraphNode specieGraph) {
        this.specieGraph = specieGraph;

        initMap(dbId2HumanNode, humanGraph);
        initMap(dbId2SpeciesNode, specieGraph);

        setLayout();

        GraphNode aux = new GraphNode(this.specieGraph.getDbId(), this.specieGraph.getName());
        for (GraphNode child : this.specieGraph.children) {
            if(child.hasLayoutData()){
                aux.addChild(child);
            }
        }
        this.specieGraph = aux;
    }

    public GraphNode getGraph() {
        return specieGraph;
    }

    private void setLayout() {
        for (GraphNode speciesNode : dbId2SpeciesNode.values()) {
            GraphNode hsNode = getHumanGraphNode(speciesNode);
            if (hsNode != null && !speciesNode.hasLayoutData()) {
                speciesNode.setLayoutParameters(hsNode);
            } // else {
//                System.err.println(speciesNode + " does not have orth to human");
//            }
        }
    }

    private GraphNode getHumanGraphNode(GraphNode node) {
        if (node.getStId().equals("TLP")) return null;
        Pathway pathway = DatabaseObjectFactory.getDatabaseObject(node.getDbId()).load();
        if (pathway.getInferredFrom() != null) {
            for (Event event : pathway.getInferredFrom()) {
                event.load();
                for (Species s : event.getSpecies()) {
                    if (s.getDbId().equals(SpeciesNodeFactory.HUMAN_DB_ID)) {
                        return dbId2HumanNode.get(event.getDbId());
                    }
                }
            }
        }
        if (pathway.getOrthologousEvent() != null) {
            for (Event event : pathway.getOrthologousEvent()) {
                event.load();
                for (Species s : event.getSpecies()) {
                    if (s.getDbId().equals(SpeciesNodeFactory.HUMAN_DB_ID)) {
                        return dbId2HumanNode.get(event.getDbId());
                    }
                }
            }
        }
        return null;
    }

    private void initMap(Map<Long, GraphNode> map, GraphNode node) {
        map.put(node.getDbId(), node);
        for (GraphNode gNode : node.getChildren()) {
            initMap(map, gNode);
        }
    }
}
