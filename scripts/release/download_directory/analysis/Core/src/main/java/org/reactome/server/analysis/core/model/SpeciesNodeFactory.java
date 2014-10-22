package org.reactome.server.analysis.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class SpeciesNodeFactory {

    public static long HUMAN_DB_ID = 48887L;
    public static String HUMAN_STR = "Homo sapiens";

    private static Map<Long, SpeciesNode> speciesMap = new HashMap<Long, SpeciesNode>();

    public static SpeciesNode getSpeciesNode(Long speciesID, String name){
        SpeciesNode speciesNode = speciesMap.get(speciesID);
        if(speciesNode==null){
            speciesNode = new SpeciesNode(speciesID, name);
            speciesMap.put(speciesID, speciesNode);
        }
        return speciesNode;
    }

    public static SpeciesNode getHumanNode(){
        return new SpeciesNode(HUMAN_DB_ID, HUMAN_STR);
    }
}
