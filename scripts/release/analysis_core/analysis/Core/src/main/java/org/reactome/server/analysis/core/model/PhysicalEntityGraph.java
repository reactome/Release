package org.reactome.server.analysis.core.model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PhysicalEntityGraph {
    private Set<PhysicalEntityNode> nodes;

    public PhysicalEntityGraph() {
        this.nodes = new HashSet<PhysicalEntityNode>();
    }

    public void addRoot(PhysicalEntityNode node){
        this.nodes.add(node);
    }

    public Set<PhysicalEntityNode> getRootNodes() {
        return nodes;
    }

    public Set<PhysicalEntityNode> getAllNodes(){
        Set<PhysicalEntityNode> rtn = new HashSet<PhysicalEntityNode>();
        for (PhysicalEntityNode node : this.nodes) {
            rtn.addAll(node.getAllNodes());
        }
        return rtn;
    }

//    public MapSet<Resource, String> getMainResourceIdentifiers(){
//        MapSet<Resource, String> rtn = new MapSet<Resource, String>();
//        for (PhysicalEntityNode node : this.nodes) {
//            rtn.addAll(node.getMainResourcesIdentifiers());
//        }
//        return rtn;
//    }

    public void setLinkToParent(){
        for (PhysicalEntityNode node : nodes) {
            node.setLinkToParent(null);
        }
    }

    public void setOrthologiesCrossLinks(){
        for (PhysicalEntityNode node : getAllNodes()) {
            node.setOrthologiesCrossLinks();
        }
    }

    public void prepareToSerialise(){
        for (PhysicalEntityNode node : nodes) {
            node.removeLinkToParent();
        }
    }

}
