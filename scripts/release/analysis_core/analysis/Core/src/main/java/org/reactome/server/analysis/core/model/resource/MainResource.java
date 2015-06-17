package org.reactome.server.analysis.core.model.resource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class MainResource extends Resource {

    private boolean auxMainResource;

    protected MainResource(String name) {
        this(name, false);
    }

    protected MainResource(String name, boolean auxMainResource) {
        super(name);
        setAuxMainResource(auxMainResource);
    }

    public boolean isAuxMainResource() {
        return auxMainResource;
    }

    protected void setAuxMainResource(boolean auxMainResource){
        this.auxMainResource = auxMainResource;
    }
}
