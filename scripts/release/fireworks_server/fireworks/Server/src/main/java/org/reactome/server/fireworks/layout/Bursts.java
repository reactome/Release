package org.reactome.server.fireworks.layout;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "bursts"
})
public class Bursts {

    @JsonProperty("bursts")
    private Map<Long, Burst> bursts = new HashMap<Long, Burst>();

    public Bursts() {}

    public Bursts(@JsonProperty("bursts") Map<Long, Burst> bursts) {
        this.bursts = bursts;
    }

    public Burst addBurst(Burst burst){
        if(burst.getDbId().equals(1643685L)) return null; //No Disease
        return this.bursts.put(burst.getDbId(), burst);
    }

    public Burst getBurst(Long dbId){
        Burst burst = this.bursts.get(dbId);
        if(burst!=null) {
            burst.setDbId(dbId);
        }
        return burst;
    }

    /**
     *
     * @return
     * The burstList
     */
    @JsonProperty("bursts")
    public Map<Long, Burst> getBursts() {
        return bursts;
    }

    /**
     *
     * @param burstList
     * The burstList
     */
    @JsonProperty("bursts")
    public void setBurstList(Map<Long, Burst> bursts) {
        this.bursts = bursts;
    }

}
