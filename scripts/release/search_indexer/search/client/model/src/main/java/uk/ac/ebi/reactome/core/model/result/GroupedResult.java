package uk.ac.ebi.reactome.core.model.result;

import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class GroupedResult {

    private List<Result> results;
    private int rowCount;
    private int numberOfGroups;
    private int numberOfMatches;


    public GroupedResult(List<Result> results, int rowCount, Integer numberOfGroups, int numberOfMatches) {
        this.results = results;
        this.numberOfGroups = numberOfGroups;
        this.numberOfMatches = numberOfMatches;
        this.rowCount = rowCount;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
