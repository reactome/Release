package org.reactome.server.analysis.service.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PathwayIdentifier {

    private String identifier;
    private List<Double> exp;
    private Set<IdentifierMap> mapsTo;

    public PathwayIdentifier(IdentifierSummary is, Set<IdentifierMap> mapsTo) {
        this.identifier = is.getId();
        this.exp = is.getExp();
        this.mapsTo = mapsTo;
    }

    public PathwayIdentifier(PathwayIdentifier pi, String resource){
        this.identifier = pi.identifier;
        this.exp = pi.exp;
        this.mapsTo = new HashSet<>();
        for (IdentifierMap identifierMap : pi.mapsTo) {
            if(identifierMap.getResource().equals(resource)){
                this.mapsTo.add(identifierMap);
            }
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Double> getExp() {
        return exp;
    }

    public Set<IdentifierMap> getMapsTo() {
        return mapsTo;
    }

    protected void merge(Set<IdentifierMap> mapsTo){
        for (IdentifierMap aux : mapsTo) {
            boolean added = false;
            for (IdentifierMap mt : this.mapsTo) {
                if(mt.getResource().equals(aux.getResource())){
                    mt.addAll(aux.getIds());
                    added = true;
                    break;
                }
            }
            if(!added){
                this.mapsTo.add(aux);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PathwayIdentifier that = (PathwayIdentifier) o;

        //noinspection RedundantIfStatement
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }
}
