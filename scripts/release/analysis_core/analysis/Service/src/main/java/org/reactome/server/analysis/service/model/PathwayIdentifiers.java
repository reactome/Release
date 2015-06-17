package org.reactome.server.analysis.service.model;

import org.reactome.server.analysis.core.model.identifier.Identifier;
import org.reactome.server.analysis.core.model.identifier.MainIdentifier;
import org.reactome.server.analysis.core.util.MapSet;
import org.reactome.server.analysis.service.result.PathwayNodeSummary;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PathwayIdentifiers {

    private List<PathwayIdentifier> identifiers;
    private Set<String> resources;
    private List<String> expNames;
    private Integer found;

    private PathwayIdentifiers(List<PathwayIdentifier> identifiers, Set<String> resources, List<String> expNames, Integer found) {
        this.identifiers = identifiers;
        this.resources = resources;
        this.expNames = expNames;
        this.found = found;
    }

    public PathwayIdentifiers(PathwayNodeSummary nodeSummary, List<String> expNames) {
        this.expNames = expNames;

        this.resources = new HashSet<String>();
        this.identifiers = new LinkedList<PathwayIdentifier>();
        MapSet<Identifier, MainIdentifier> identifierMap = nodeSummary.getData().getIdentifierMap();
        for (Identifier identifier : identifierMap.keySet()) {

            MapSet<String, String> mapsTo = new MapSet<>();
            for (MainIdentifier mainIdentifier : identifierMap.getElements(identifier)) {
                String name = mainIdentifier.getResource().getName();
                this.resources.add(name);
                mapsTo.add(name, mainIdentifier.getValue().getId());
            }

            IdentifierSummary is = new IdentifierSummary(identifier.getValue());
            Set<IdentifierMap> maps = new HashSet<>();
            for (String resource : mapsTo.keySet()) {
                maps.add(new IdentifierMap(resource, mapsTo.getElements(resource)));
            }

            boolean added = false;
            for (PathwayIdentifier pathwayIdentifier : this.identifiers) {
                if(pathwayIdentifier.getIdentifier().equals(is.getId())){
                    pathwayIdentifier.merge(maps);
                    added = true;
                    break;
                }
            }
            if(!added){
                this.identifiers.add(new PathwayIdentifier(is, maps));
            }
        }
        //IMPORTANT TO BE HERE!
        this.found = this.identifiers.size();
    }

    public List<PathwayIdentifier> getIdentifiers() {
        return identifiers;
    }

    public Set<String> getResources() {
        return resources;
    }

    public List<String> getExpNames() {
        return expNames;
    }

    public Integer getFound() {
        return found;
    }

    private List<PathwayIdentifier> filterByResource(String resource){
        List<PathwayIdentifier> rtn = new LinkedList<PathwayIdentifier>();
        for (PathwayIdentifier identifier : identifiers) {
            for (IdentifierMap identifierMap : identifier.getMapsTo()) {
                if(identifierMap.getResource().equals(resource)){
                    rtn.add(new PathwayIdentifier(identifier, resource));
                }
            }
        }
        return rtn;
    }

    public PathwayIdentifiers filter(String resource, Integer pageSize, Integer page) {
        resource = resource.toUpperCase();

        List<PathwayIdentifier> aux;
        if(resource.equals("TOTAL")){
            aux = this.identifiers;
        }else{
            aux = filterByResource(resource.toUpperCase());
        }

        pageSize = (pageSize==null) ? aux.size() : pageSize ;
        pageSize = pageSize < 0 ? 0 : pageSize;

        page = (page==null) ? 1 : page;
        page = page < 0 ? 0 : page;

        int from = pageSize * (page - 1);
        if(from < aux.size() && from > -1){
            int to = from + pageSize;
            to = to > aux.size() ? aux.size() : to;
            Set<String> resources = resource.equals("TOTAL") ? this.resources : new HashSet<String>(Arrays.asList(resource));
            return new PathwayIdentifiers(aux.subList(from, to), resources, this.expNames, aux.size());
        }else{
            return null;
        }
    }
}
