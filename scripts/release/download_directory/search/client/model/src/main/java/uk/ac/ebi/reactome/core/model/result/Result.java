package uk.ac.ebi.reactome.core.model.result;

import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Result {

    private List<Entry> entries;
    private String typeName;
    private long entriesCount;
    private int rowCount;

    public Result(List<Entry> entries, String typeName, long entriesCount, int rowCount) {
        this.entries = entries;
        this.typeName = typeName;
        this.entriesCount = entriesCount;
        this.rowCount = rowCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public long getEntriesCount() {
        return entriesCount;
    }

    public void setEntriesCount(long entriesCount) {
        this.entriesCount = entriesCount;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
