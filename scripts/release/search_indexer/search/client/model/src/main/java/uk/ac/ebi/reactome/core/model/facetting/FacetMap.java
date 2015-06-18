package uk.ac.ebi.reactome.core.model.facetting;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class FacetMap {

    private long totalNumFount;
    private FacetList speciesFacet;
    private FacetList typeFacet;
    private FacetList keywordFacet;
    private FacetList compartmentFacet;

    public long getTotalNumFount() {
        return totalNumFount;
    }

    public void setTotalNumFount(long totalNumFount) {
        this.totalNumFount = totalNumFount;
    }

    public FacetList getSpeciesFacet() {
        return speciesFacet;
    }

    public void setSpeciesFacet(FacetList speciesFacet) {
        this.speciesFacet = speciesFacet;
    }

    public FacetList getTypeFacet() {
        return typeFacet;
    }

    public void setTypeFacet(FacetList typeFacet) {
        this.typeFacet = typeFacet;
    }

    public FacetList getKeywordFacet() {
        return keywordFacet;
    }

    public void setKeywordFacet(FacetList keywordFacet) {
        this.keywordFacet = keywordFacet;
    }

    public FacetList getCompartmentFacet() {
        return compartmentFacet;
    }

    public void setCompartmentFacet(FacetList compartmentFacet) {
        this.compartmentFacet = compartmentFacet;
    }
}
