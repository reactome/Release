package org.reactome.server.analysis.service.result;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public enum AnalysisSortType {
    NAME,
    TOTAL_ENTITIES,
    TOTAL_REACTIONS,
    FOUND_ENTITIES,
    FOUND_REACTIONS,
    ENTITIES_RATIO,
    ENTITIES_PVALUE,
    ENTITIES_FDR,
    REACTIONS_RATIO;

    public static AnalysisSortType getSortType(String type){
        if(type!=null){
            for (AnalysisSortType sortType : values()) {
                if(sortType.toString().equals(type.toUpperCase())){
                    return sortType;
                }
            }
        }
        return ENTITIES_PVALUE;
    }
}
