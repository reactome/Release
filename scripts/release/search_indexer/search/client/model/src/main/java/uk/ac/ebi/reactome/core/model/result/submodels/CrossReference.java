package uk.ac.ebi.reactome.core.model.result.submodels;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class CrossReference {

    private String identifier;
    private Database database;

    public CrossReference(String identifier, Database database) {
        this.identifier = identifier;
        this.database = database;
    }


    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}
