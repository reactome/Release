package org.reactome.server.models2pathways.reactome.helper;

import org.reactome.server.analysis.core.components.EnrichmentAnalysis;
import org.reactome.server.analysis.core.data.AnalysisData;
import org.reactome.server.analysis.core.model.*;
import org.reactome.server.models2pathways.biomodels.model.Annotation;
import org.reactome.server.models2pathways.biomodels.model.BioModel;
import org.reactome.server.models2pathways.core.helper.SpeciesHelper;
import org.reactome.server.models2pathways.core.model.Specie;
import org.reactome.server.models2pathways.reactome.model.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class AnalysisCoreHelper {
    private static EnrichmentAnalysis enrichmentAnalysis;
    private static String structure;

    public AnalysisCoreHelper() {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");
        AnalysisData analysisData = context.getBean(AnalysisData.class);
        analysisData.setFileName(structure);
        enrichmentAnalysis = context.getBean(EnrichmentAnalysis.class);
        System.out.println();
    }

    public static String getStructure() {
        return structure;
    }

    public static void setStructure(String structure) {
        AnalysisCoreHelper.structure = structure;
    }

    public AnalysisResult getAnalysisResult(AnalysisStoredResult analysisStoredResult, List<PathwaySummary> pathwaySummaryList) {
        return new AnalysisResult(analysisStoredResult, pathwaySummaryList);
    }

    public SpeciesNode getSpeciesNode(Specie specie) {
        return SpeciesNodeFactory.getSpeciesNode(specie.getReactId(), specie.getName());
    }

    public UserData getUserData(String bioModelsName, Set<Annotation> annotations) {
        List<String> columnNames = new ArrayList<>();
        Set<AnalysisIdentifier> analysisIdentifiers = new HashSet<>();
        for (Annotation annotation : annotations) {
            analysisIdentifiers.add(new AnalysisIdentifier(annotation.getEntityId()));
        }
        columnNames.add(bioModelsName);
        return new UserData(columnNames, analysisIdentifiers, null);
    }

    public HierarchiesData getHierarchiesData(UserData userData, SpeciesNode species) throws NullPointerException {
        return enrichmentAnalysis.overRepresentation(userData.getIdentifiers(), species);
    }

    public List<PathwaySummary> getPathwaySummaryList(List<PathwayNode> pathwayNodeList, String resource) {
        List<PathwaySummary> pathwaySummaryList = new ArrayList<>();
        for (PathwayNode pathwayNode : pathwayNodeList) {
            pathwaySummaryList.add(new PathwaySummary(new PathwayNodeSummary(pathwayNode), resource));
        }
        return pathwaySummaryList;
    }

    public AnalysisStoredResult getAnalysisStoredResult(UserData userData, HierarchiesData hierarchiesData) {
        return new AnalysisStoredResult(userData, hierarchiesData);
    }

    public List<PathwaySummary> getReliablePathways(List<PathwaySummary> pathways, Double customFDR, BioModel bioModel, Double reactionCoverage) {
        List<PathwaySummary> reliablePathways = new ArrayList<>();
        for (PathwaySummary pathway : pathways) {
            try {
                if (pathway.isLlp() && pathway.getEntities().getFdr() <= customFDR) {
                    try {
                        if (Objects.equals(pathway.getSpecies().getDbId(), SpeciesHelper.getInstance().getSpecieByBioMdSpecieId(bioModel.getSpecie().getBioMdId()).getReactId())
                                && ((double) pathway.getReactions().getFound() / (double) pathway.getReactions().getTotal()) >= reactionCoverage) {
                            reliablePathways.add(pathway);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return reliablePathways;
    }

    public String getResource(List<ResourceSummary> resourceSummary) {
        ResourceSummary rs = resourceSummary.size() == 2 ? resourceSummary.get(1) : resourceSummary.get(0);
        return rs.getResource();
    }

    public SpeciesNode convertToSpeciesNode(Specie specie) {
        return SpeciesNodeFactory.getSpeciesNode(specie.getReactId(), specie.getName());
    }

    @Scope("singleton")
    public enum Type {
        SPECIES_COMPARISON,
        OVERREPRESENTATION,
        EXPRESSION;

        public static Type getType(String type) {
            for (Type t : values()) {
                if (t.toString().toLowerCase().equals(type.toLowerCase())) {
                    return t;
                }
            }
            return null;
        }
    }
}