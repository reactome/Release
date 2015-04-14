//package org.reactome.server.core.models2pathways.helper;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.util.EntityUtils;
//import org.reactome.server.core.models2pathways.core.enums.Species;
//import AnalysisResult;
//import PathwaySummary;
//import ResourceSummary;
//import org.reactome.server.core.models2pathways.biomodels.model.SBMLModel;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Logger;
//
///**
//* @author Maximilian Koch <mkoch@ebi.ac.uk>
//*/
//public class AnalysisServiceHandler {
//    final static Logger logger = Logger.getLogger(AnalysisServiceHandler.class.getName());
//
//    private static final String DEFAULT_CHARSET = "UTF-8";
//
//    public static AnalysisResult getReactomeAnalysisResultBySBMLModel(SBMLModel sbmlModel, Double customFDR, Double reactionCoverage) {
//        HttpResponse httpResponse = AnalysisServiceRequest.requestByModel(sbmlModel);
//        String token = null;
//        String jsonResult = null;
//        if (httpResponse.getStatusLine().getStatusCode() == 200) {
//            try {
//                jsonResult = EntityUtils.toString(httpResponse.getEntity(), DEFAULT_CHARSET);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            AnalysisResult analysisResult = getAnalysisResultObject(jsonResult);
//            if (analysisResult.getPathwaysFound() != 0) {
//                token = analysisResult.getSummary().getToken();
//                String resourceSummary = getResource(analysisResult.getResourceSummary());
//                httpResponse = AnalysisServiceRequest.requestByToken(token, resourceSummary);
//                try {
//                    HttpEntity entity = httpResponse.getEntity();
//                    jsonResult = EntityUtils.toString(entity, DEFAULT_CHARSET);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                analysisResult = getAnalysisResultObject(jsonResult);
//            }
////            analysisResult.setReliablePathways(getReliablePathways(analysisResult.getPathways(), customFDR, sbmlModel, reactionCoverage));
////            analysisResult.setToken(token);
//            return analysisResult;
//        }
//        return null;
//    }
//
//    private static List<PathwaySummary> getReliablePathways(List<PathwaySummary> pathways, Double customFDR, SBMLModel sbmlModel, Double reactionCoverage) {
//        List<PathwaySummary> reliablePathways = new ArrayList<>();
//        for (PathwaySummary pathway : pathways) {
//            if (pathway.isLlp() && pathway.getEntities().getFdr() <= customFDR) {
//                try {
//                    if (pathway.getSpecies().getDbId().toString().equals(
//                            Species.getSpeciesByBioModelsTaxonomyid(sbmlModel.getBioModelsTaxonomyId().getBioModelsTaxonomyId()).getReactomeTaxonomyId())
//                            && ((double) pathway.getEntities().getFound() / (double) pathway.getEntities().getTotal()) >= reactionCoverage) {
//                        reliablePathways.add(pathway);
//                    }
//                } catch (NullPointerException ignored) {
//                }
//            }
//        }
//        return reliablePathways;
//    }
//
//    public static String getResource(List<ResourceSummary> resourceSummary) {
//        ResourceSummary rs = resourceSummary.size() == 2 ? resourceSummary.get(1) : resourceSummary.get(0);
//        return rs.getResource();
//    }
//
//    private static AnalysisResult getAnalysisResultObject(String jsonString) {
//        AnalysisResult analysisResult = null;
//        ObjectMapper mapper = new ObjectMapper();
//
//        try {
//            analysisResult = mapper.readValue(jsonString, AnalysisResult.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return analysisResult;
//    }
//}
