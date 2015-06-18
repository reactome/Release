package uk.ac.ebi.reactome.core.model.query;

import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Query {

    private String query;
    private List<String> species;
    private List<String> types;
    private List<String> keywords;
    private List<String> compartment;
    private Integer start;
    private Integer rows;

    public Query() {

    }

    public Query(String query, List<String> species, List<String> types, List<String> keywords, List<String> compartment, Integer start, Integer rows) {
        this.query = query;
        this.species = species;
        this.types = types;
        this.keywords = keywords;
        this.compartment = compartment;
        this.start = start;
        this.rows = rows;
    }

    public Query(String query, List<String> species, List<String> types, List<String> compartment, List<String> keywords) {
        this.query = query;
        this.species = species;
        this.types = types;
        this.keywords = keywords;
        this.compartment = compartment;
    }



    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getSpecies() {
        return species;
    }

    public void setSpecies(List<String> species) {
        this.species = species;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getCompartment() {
        return compartment;
    }

    public void setCompartment(List<String> compartment) {
        this.compartment = compartment;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }
}
