package org.reactome.server.analysis.service.result;

import org.reactome.server.analysis.core.model.PathwayNodeData;
import org.reactome.server.analysis.core.model.resource.MainResource;

import java.util.Comparator;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class ComparatorFactory {

    public static Comparator<PathwayNodeSummary> getComparator(AnalysisSortType type){
        if(type==null) type = AnalysisSortType.ENTITIES_PVALUE;
        switch (type){
            case NAME:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        int rtn = compareTo(o1.getName(), o2.getName());
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;
                    }
                };
            case TOTAL_ENTITIES:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesCount():null;
                        Comparable t2 = d2!=null?d2.getEntitiesCount():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case TOTAL_REACTIONS:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsCount():null;
                        Comparable t2 = d2!=null?d2.getReactionsCount():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case FOUND_ENTITIES:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesFound():null;
                        Comparable t2 = d2!=null?d2.getEntitiesFound():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case FOUND_REACTIONS:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsFound():null;
                        Comparable t2 = d2!=null?d2.getReactionsFound():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case ENTITIES_RATIO:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesRatio():null;
                        Comparable t2 = d2!=null?d2.getEntitiesRatio():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case ENTITIES_FDR:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesFDR():null;
                        Comparable t2 = d2!=null?d2.getEntitiesFDR():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case REACTIONS_RATIO:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsRatio():null;
                        Comparable t2 = d2!=null?d2.getReactionsRatio():null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;                    }
                };
            case ENTITIES_PVALUE:
            default:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        Comparable t1 = o1.getData().getEntitiesPValue();
                        Comparable t2 = o2.getData().getEntitiesPValue();
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2);
                        }
                        return rtn;
                    }
                };
        }
    }

    public static Comparator<PathwayNodeSummary> getComparator(AnalysisSortType type, final MainResource r){
        if(r ==null){
            return ComparatorFactory.getComparator(type);
        }
        if(type==null) type = AnalysisSortType.ENTITIES_PVALUE;
        switch (type){
            case NAME:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        int rtn =  compareTo(o1.getName(), o2.getName());
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case TOTAL_ENTITIES:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesCount(r):null;
                        Comparable t2 = d2!=null?d2.getEntitiesCount(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case TOTAL_REACTIONS:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsCount(r):null;
                        Comparable t2 = d2!=null?d2.getReactionsCount(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case FOUND_ENTITIES:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesFound(r):null;
                        Comparable t2 = d2!=null?d2.getEntitiesFound(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case FOUND_REACTIONS:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsFound(r):null;
                        Comparable t2 = d2!=null?d2.getReactionsFound(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case ENTITIES_RATIO:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesRatio(r):null;
                        Comparable t2 = d2!=null?d2.getEntitiesRatio(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case ENTITIES_FDR:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getEntitiesFDR(r):null;
                        Comparable t2 = d2!=null?d2.getEntitiesFDR(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case REACTIONS_RATIO:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        PathwayNodeData d1 = o1.getData(); PathwayNodeData d2 = o2.getData();
                        Comparable t1 = d1!=null?d1.getReactionsRatio(r):null;
                        Comparable t2 = d2!=null?d2.getReactionsRatio(r):null;
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
            case ENTITIES_PVALUE:
            default:
                return new Comparator<PathwayNodeSummary>() {
                    @Override
                    public int compare(PathwayNodeSummary o1, PathwayNodeSummary o2) {
                        Comparable t1 = o1.getData().getEntitiesPValue(r);
                        Comparable t2 = o2.getData().getEntitiesPValue(r);
                        int rtn = compareTo(t1, t2);
                        if(rtn==0){
                            return genericCompare(o1, o2, r);
                        }
                        return rtn;
                    }
                };
           }
    }

    static int compareTo(Comparable c1, Comparable c2){
        if(c1 == null){
            if(c2 == null){
                return 0;
            }else{
                return 1;
            }
        }
        if(c2 == null){
            return -1;
        }else{
            //noinspection unchecked
            return c1.compareTo(c2);
        }
    }

    static int genericCompare(PathwayNodeSummary o1, PathwayNodeSummary o2){
        int rtn = getReactionsPercentage(o2).compareTo(getReactionsPercentage(o1));
        if(rtn==0){
            rtn = getEntitiesPercentage(o2).compareTo(getEntitiesPercentage(o1));
            if(rtn==0) {
                rtn = o2.getData().getEntitiesCount().compareTo(o1.getData().getEntitiesCount());
                if(rtn==0){
                    rtn = o2.getData().getReactionsCount().compareTo(o1.getData().getReactionsCount());
                }
            }
        }
        return rtn;
    }

    static int genericCompare(PathwayNodeSummary o1, PathwayNodeSummary o2, MainResource r){
        int rtn = getReactionsPercentage(o2, r).compareTo(getReactionsPercentage(o1, r));
        if(rtn==0){
            rtn = getEntitiesPercentage(o2, r).compareTo(getEntitiesPercentage(o1, r));
            if(rtn==0) {
                rtn = o2.getData().getEntitiesCount(r).compareTo(o1.getData().getEntitiesCount(r));
                if(rtn==0){
                    rtn = o2.getData().getReactionsCount(r).compareTo(o1.getData().getReactionsCount(r));
                }
            }
        }
        return rtn;
    }

    static Double getEntitiesPercentage(PathwayNodeSummary node) {
        return node.getData().getEntitiesFound() / node.getData().getEntitiesCount().doubleValue();
    }

    static Double getEntitiesPercentage(PathwayNodeSummary node, MainResource r) {
        return node.getData().getEntitiesFound(r) / node.getData().getEntitiesCount(r).doubleValue();
    }

    static Double getReactionsPercentage(PathwayNodeSummary node) {
        return node.getData().getReactionsFound() / node.getData().getReactionsCount().doubleValue();
    }

    static Double getReactionsPercentage(PathwayNodeSummary node, MainResource r) {
        return node.getData().getReactionsFound(r) / node.getData().getReactionsCount(r).doubleValue();
    }
}
