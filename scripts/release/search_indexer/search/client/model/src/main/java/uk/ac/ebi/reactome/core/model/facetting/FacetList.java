package uk.ac.ebi.reactome.core.model.facetting;

import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class FacetList {

    private List<FacetContainer> selected;
    private List<FacetContainer> available;

    public FacetList(List<FacetContainer> available) {
        this.available = available;
    }

    public FacetList(List<FacetContainer> selected, List<FacetContainer> available) {
        this.selected = selected;
        this.available = available;
    }


    public List<FacetContainer> getSelected() {
        return selected;
    }

    public void setSelected(List<FacetContainer> selected) {
        this.selected = selected;
    }

    public List<FacetContainer> getAvailable() {
        return available;
    }

    public void setAvailable(List<FacetContainer> available) {
        this.available = available;
    }
}
