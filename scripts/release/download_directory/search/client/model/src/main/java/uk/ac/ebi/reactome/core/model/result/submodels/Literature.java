package uk.ac.ebi.reactome.core.model.result.submodels;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Literature {

    private String title;
    private Integer year;

    private String journal;
    private String pubMedIdentifier;

    private String url;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getPubMedIdentifier() {
        return pubMedIdentifier;
    }

    public void setPubMedIdentifier(String pubMedIdentifier) {
        this.pubMedIdentifier = pubMedIdentifier;
    }
}
