package org.reactome.server.analysis.core.model.resource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Resource {

    private String name;

    protected Resource(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean is(ResourceFactory.MAIN type){
        return this.name.equals(type.name());
    }

    public boolean is(String name){
        return this.name.equals(name.toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        //noinspection RedundantIfStatement
        if (name != null ? !name.equals(resource.name) : resource.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
