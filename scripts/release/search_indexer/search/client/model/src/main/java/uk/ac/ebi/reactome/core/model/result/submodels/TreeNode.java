package uk.ac.ebi.reactome.core.model.result.submodels;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by flo on 8/14/14.
 */
public class TreeNode {
    private String name;
    private String species;
    private String url;
    private Set<TreeNode> children;

    public void addChild(TreeNode node) {
        if (children == null) {
            children = new HashSet<TreeNode>();
        }
            children.add(node);
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(Set<TreeNode> children) {
        this.children = children;
    }
}
