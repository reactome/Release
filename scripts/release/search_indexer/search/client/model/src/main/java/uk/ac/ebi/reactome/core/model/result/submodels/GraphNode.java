package uk.ac.ebi.reactome.core.model.result.submodels;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class GraphNode {

    private Long dbId;
    private String stId;
    private String name;
    private String species;
    private Long speciesId;
    private String url;

    private Set<GraphNode> children;
    private Set<GraphNode> parent;

    public void addParent(GraphNode node) {
        if (parent==null) {
            parent = new HashSet<GraphNode>();
        }
        parent.add(node);
    }
    public void addChild(GraphNode node) {
        if (children==null) {
            children = new HashSet<GraphNode>();
        }
        children.add(node);
    }

    public Set<GraphNode> getLeafs () {
        Set<GraphNode> leafs = new HashSet<GraphNode>();
        if (this.children == null) {
            leafs.add(this);
        } else {
            for (GraphNode child : this.children) {
                leafs.addAll(child.getLeafs());
            }
        }
        return leafs;
    }


    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getStId() {
        return stId;
    }

    public void setStId(String stId) {
        this.stId = stId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public Long getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(Long speciesId) {
        this.speciesId = speciesId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<GraphNode> getChildren() {
        return children;
    }

    public void setChildren(Set<GraphNode> children) {
        this.children = children;
    }

    public Set<GraphNode> getParent() {
        return parent;
    }

    public void setParent(Set<GraphNode> parent) {
        this.parent = parent;
    }
}
