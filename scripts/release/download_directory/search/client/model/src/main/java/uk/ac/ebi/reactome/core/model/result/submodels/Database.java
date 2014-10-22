package uk.ac.ebi.reactome.core.model.result.submodels;

/**
 * Created by flo on 7/2/14.
 */
public class Database {
    private String name;
    private String url;

    public Database(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
