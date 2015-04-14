package org.reactome.server.models2pathways.reactome.model;

import org.reactome.server.analysis.core.model.PathwayNodeData;
import org.reactome.server.analysis.core.model.resource.MainResource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PathwaySummary {
    private String stId;
    private Long dbId;
    private String name;
    private SpeciesSummary species;
    private boolean llp; //lower level pathway
    private EntityStatistics entities;
    private ReactionStatistics reactions;

    public PathwaySummary(PathwayNodeSummary node, String resource) {
        this.stId = node.getStId();
        this.dbId = node.getPathwayId();
        this.name = node.getName();
        this.species = new SpeciesSummary(node.getSpecies().getSpeciesID(), node.getSpecies().getName());
        this.llp = node.isLlp();
        initialize(node.getData(), resource);
    }

    private void initialize(PathwayNodeData d, String resource) {
        if (resource.equals("TOTAL")) {
            this.entities = new EntityStatistics(
                    "TOTAL",
                    d.getEntitiesCount(),
                    d.getEntitiesFound(),
                    d.getEntitiesRatio(),
                    d.getEntitiesPValue(),
                    d.getEntitiesFDR(),
                    d.getExpressionValuesAvg()
            );
            this.reactions = new ReactionStatistics(
                    "TOTAL",
                    d.getReactionsCount(),
                    d.getReactionsFound(),
                    d.getReactionsRatio()
            );
        } else {
            for (MainResource mr : d.getResources()) {
                if (mr.getName().equals(resource)) {
                    this.entities = new EntityStatistics(
                            mr.getName(),
                            d.getEntitiesCount(mr),
                            d.getEntitiesFound(mr),
                            d.getEntitiesRatio(mr),
                            d.getEntitiesPValue(mr),
                            d.getEntitiesFDR(mr),
                            d.getExpressionValuesAvg(mr)
                    );
                    this.reactions = new ReactionStatistics(
                            mr.getName(),
                            d.getReactionsCount(mr),
                            d.getReactionsFound(mr),
                            d.getReactionsRatio(mr)
                    );
                    break;
                }
            }
        }
    }

    public String getStId() {
        return stId;
    }

    public Long getDbId() {
        return dbId;
    }

    public String getName() {
        return name;
    }

    public SpeciesSummary getSpecies() {
        return species;
    }

    public boolean isLlp() {
        return llp;
    }

    public EntityStatistics getEntities() {
        return entities;
    }

    public ReactionStatistics getReactions() {
        return reactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PathwaySummary that = (PathwaySummary) o;

        if (dbId != null ? !dbId.equals(that.dbId) : that.dbId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dbId != null ? dbId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PathwaySummary{" +
                "stId='" + stId + '\'' +
                ", dbId=" + dbId +
                ", name='" + name + '\'' +
                ", species=" + species +
                ", llp=" + llp +
                ", entities=" + entities +
                ", reactions=" + reactions +
                '}';
    }
}