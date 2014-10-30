package org.reactome.server.analysis.tools.components.exporter;

import org.reactome.server.analysis.core.data.AnalysisData;
import org.reactome.server.analysis.core.model.PathwayHierarchy;
import org.reactome.server.analysis.core.model.PathwayNode;
import org.reactome.server.analysis.core.model.PathwayRoot;
import org.reactome.server.analysis.core.model.SpeciesNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Component
public class HierarchyExporter {

    @Autowired
    private AnalysisData analysisData;

    public void exportParentship(String fileName) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream(new File(fileName)));
        Map<SpeciesNode, PathwayHierarchy> hierarchies = analysisData.getPathwayHierarchies();
        for (SpeciesNode species : hierarchies.keySet()) {
            PathwayHierarchy hierarchy = hierarchies.get(species);
            for (PathwayRoot root : hierarchy.getChildren()) {
                printNodeParentship(root, ps);
            }
        }
    }

    public void exportDetails(String fileName) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream(new File(fileName)));
        Map<SpeciesNode, PathwayHierarchy> hierarchies = analysisData.getPathwayHierarchies();
        for (SpeciesNode species : hierarchies.keySet()) {
            PathwayHierarchy hierarchy = hierarchies.get(species);
            for (PathwayRoot root : hierarchy.getChildren()) {
                printNodeDetails(root, ps);
            }
        }
    }

    private void printNodeParentship(PathwayNode node, PrintStream ps){
        for (PathwayNode child : node.getChildren()) {
            ps.println(getNodeIdentifier(node) + "\t" + getNodeIdentifier(child));
            printNodeParentship(child, ps);
        }
    }

    private void printNodeDetails(PathwayNode node, PrintStream ps){
        ps.println(getNodeIdentifier(node) + "\t" + node.getName() + "\t" + node.getSpecies().getName());
        for (PathwayNode child : node.getChildren()) {
            printNodeDetails(child, ps);
        }
    }

    private String getNodeIdentifier(PathwayNode node){
        String id = node.getStId();
        return id!=null && !id.isEmpty()? id : node.getPathwayId().toString();
    }
}
