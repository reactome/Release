package org.reactome.server.analysis.core.model;

import org.reactome.server.analysis.core.util.MapSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple approach to take into account the number of reactions where a physical entity
 * have been seen while building the analysis structure
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class EntityPathwayReactionMap {

    private Map<Long, MapSet<Long, AnalysisReaction>> entityPathwayReactionMap;

    public EntityPathwayReactionMap() {
        this.entityPathwayReactionMap = new HashMap<Long, MapSet<Long, AnalysisReaction>>();
    }

    public void add(Long physicalEntityId, Long pathwayId, AnalysisReaction reaction){
        MapSet<Long, AnalysisReaction> map = this.getOrCreatePathwayCounter(physicalEntityId);
        map.add(pathwayId, reaction);
    }

    public void add(Long physicalEntity, Set<Long> pathwayIds, AnalysisReaction reaction){
        for (Long pathwayId : pathwayIds) {
            this.add(physicalEntity, pathwayId, reaction);
        }
    }

    public MapSet<Long, AnalysisReaction> getPathwaysReactions(Long physicalEntityId){
        return this.entityPathwayReactionMap.get(physicalEntityId);
    }

    public Set<Long> keySet(){
        return this.entityPathwayReactionMap.keySet();
    }

    private MapSet<Long, AnalysisReaction> getOrCreatePathwayCounter(Long physicalEntityId){
        MapSet<Long, AnalysisReaction> rtn = this.entityPathwayReactionMap.get(physicalEntityId);
        if(rtn==null){
            rtn = new MapSet<Long, AnalysisReaction>();
            this.entityPathwayReactionMap.put(physicalEntityId, rtn);
        }
        return rtn;
    }
}
