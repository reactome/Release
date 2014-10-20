package org.reactome.server.analysis.core.model;

import org.reactome.server.analysis.core.model.resource.MainResource;
import org.reactome.server.analysis.core.util.MapSet;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class HierarchiesData {
    //A double link hierarchy tree with the pathways for each species
    private Map<SpeciesNode, PathwayHierarchy> pathwayHierarchies;
    //A map between pathways identifier and their locations in the pathway hierarchy
    private MapSet<Long, PathwayNode> pathwayLocation;

    Set<AnalysisIdentifier> notFound = new HashSet<AnalysisIdentifier>();

    public HierarchiesData(Map<SpeciesNode, PathwayHierarchy> pathwayHierarchies, MapSet<Long, PathwayNode> pathwayLocation) {
        this.pathwayHierarchies = pathwayHierarchies;
        this.pathwayLocation = pathwayLocation;
    }

    public void addNotFound(AnalysisIdentifier identifier){
        this.notFound.add(identifier);
    }

    public List<PathwayNode> getUniqueHitPathways(SpeciesNode species){
        Set<SpeciesPathway> found = new HashSet<SpeciesPathway>();
        List<PathwayNode> rtn = new LinkedList<PathwayNode>();
        for (PathwayNode pathwayNode : this.getHitPathways()) {
            if(species==null || pathwayNode.getSpecies().equals(species)){
                SpeciesPathway sp = new SpeciesPathway(pathwayNode);
                if(!found.contains(sp)){
                    rtn.add(pathwayNode);
                    found.add(sp);
                }
            }
        }
        Collections.sort(rtn);
        return rtn;
    }

    public Map<SpeciesNode, PathwayHierarchy> getPathwayHierarchies() {
        return pathwayHierarchies;
    }

    private Set<PathwayNode> getHitPathways(){
        Set<PathwayNode> rtn = new HashSet<PathwayNode>();
        for (SpeciesNode species : pathwayHierarchies.keySet()) {
            rtn.addAll(pathwayHierarchies.get(species).getHitPathways());
        }
        return rtn;
    }

    public Set<AnalysisIdentifier> getNotFound() {
        return notFound;
    }

    public MapSet<Long, PathwayNode> getPathwayLocation() {
        return pathwayLocation;
    }

    @SuppressWarnings("ConstantConditions")
    public void setResultStatistics(Map<MainResource, Integer> sampleSizePerResource, Integer notFound){
        for (SpeciesNode species : this.pathwayHierarchies.keySet()) {
            PathwayHierarchy hierarchy = this.pathwayHierarchies.get(species);
            for (PathwayRoot node : hierarchy.getChildren()) {
                node.setResultStatistics(sampleSizePerResource, notFound);
            }
            /*
            FDR has to be calculated after the pValues for each pathway because it uses all the pValues.
            How to do it? Easy :) first we create a list of PathwayStatic objects (class that I defined here as an innerClass
            because it is only used here and it does not make any sense for me to create it in a different class).
            The list will contain all the nodes information but take care of a the following detail:
            In the present implementation we have results split by main resources and we also have the "all together" result.
            That explains what it looks like a mess in the next bit of code, but take it easy and keep reading.
             */
            //Contains several sets of PathwayStatistic objects depending on the main resource (this one is used to calculate
            //the entities FDR result based on the entities pValues
            MapSet<MainResource, PathwayStatistic> pathwayResourceEntityPValue = new MapSet<MainResource, PathwayStatistic>();

            //This one does not depend on main resource because is for the combined result of the entities FDR based in their pValues
            List<PathwayStatistic> pathwayEntityPValue = new LinkedList<PathwayStatistic>();

            //First thing we have to do, is iterate over the hit pathways and populate the lists (and MapSet) defined above
            for (PathwayNode node : hierarchy.getHitPathways()) {
                PathwayNodeData nodeData = node.getPathwayNodeData();

                for (MainResource resource : nodeData.getResources()) {
                    Double pValue = nodeData.getEntitiesPValue(resource);
                    if(pValue!=null)
                        pathwayResourceEntityPValue.add(resource, new PathwayStatistic(node, pValue));
                }
                Double pValue = nodeData.getEntitiesPValue();
                pathwayEntityPValue.add(new PathwayStatistic(node, pValue));
            }
            /*
            Here we have to iterate over the different resources where the "individual" results have been found
            and is when the funny stuff begins, so let's go for it
             */
            for (MainResource resource : pathwayResourceEntityPValue.keySet()) {
                //MapSep contains data in Set associated to the left side of the map to avoid duplication
                Set<PathwayStatistic> set = pathwayResourceEntityPValue.getElements(resource);
                //But we need a list
                List<PathwayStatistic> list = new LinkedList<PathwayStatistic>(set);
                //And now go to see the comments in the method
                this.setFDRWithBenjaminiHochberg(list);
                //When the method finishes, we only need to take the results and assign to the node in question
                for (PathwayStatistic pathwayStatistic : list) {
                    PathwayNodeData nodeData = pathwayStatistic.getPathwayNode().getPathwayNodeData();
                    nodeData.setEntitiesFDR(resource, pathwayStatistic.getFDR());
                }
            }
            //You know what the comment here is... the same than before but for the combined result
            this.setFDRWithBenjaminiHochberg(pathwayEntityPValue);
            for (PathwayStatistic pathwayStatistic : pathwayEntityPValue) {
                PathwayNodeData nodeData = pathwayStatistic.getPathwayNode().getPathwayNodeData();
                nodeData.setEntitiesFDR(pathwayStatistic.getFDR());
            }
        }
    }

    /**
     * Use this method to calculate FDR from a list of pvalues using Benjamini-Hochberg
     * method. The implementation of this method is based on the source code for MEMo
     * (http://cbio.mskcc.org/tools/memo/).
     * @param list a list of PathwayStatic objects representing the hit pathways and their pValue
     */
    private void setFDRWithBenjaminiHochberg(List<PathwayStatistic> list) {
        // Make sure the parsed list if sorted.
        Collections.sort(list); //We sort the guys in the list based on the pValue
        int size = list.size();
        // The last p-value (biggest) should be the same as FDR.
        for (int i = size - 2; i >= 0; i--) {
            double right = list.get(i + 1).getFDR();
            double pValue = list.get(i).getPValue();
            double left = pValue * (size / (i + 1));
            double fdr = Math.min(left, right);
            list.get(i).setFdr(fdr);
        }
    }

    private class PathwayStatistic implements Comparable<PathwayStatistic>{
        private PathwayNode pathwayNode;
        private Double pValue;
        private Double fdr;

        private PathwayStatistic(PathwayNode pathwayNode, Double pValue) {
            this.pathwayNode = pathwayNode;
            this.pValue = pValue;
            this.fdr = pValue; //This is like having a copy itself (used in the Benjamini-Hochberg method)
        }

        private PathwayNode getPathwayNode() {
            return pathwayNode;
        }

        private Double getPValue() {
            return pValue;
        }

        private Double getFDR() {
            return fdr;
        }

        private void setFdr(Double fdr) {
            this.fdr = fdr;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(PathwayStatistic o) {
            return pValue.compareTo(o.pValue);
        }
    }
}
