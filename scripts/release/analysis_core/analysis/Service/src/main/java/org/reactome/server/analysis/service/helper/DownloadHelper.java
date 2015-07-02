package org.reactome.server.analysis.service.helper;

import org.reactome.server.analysis.core.model.AnalysisIdentifier;
import org.reactome.server.analysis.core.model.AnalysisReaction;
import org.reactome.server.analysis.core.model.PathwayNodeData;
import org.reactome.server.analysis.core.model.identifier.Identifier;
import org.reactome.server.analysis.core.model.identifier.MainIdentifier;
import org.reactome.server.analysis.core.model.resource.MainResource;
import org.reactome.server.analysis.core.model.resource.Resource;
import org.reactome.server.analysis.core.model.resource.ResourceFactory;
import org.reactome.server.analysis.core.util.MapSet;
import org.reactome.server.analysis.service.report.AnalysisReport;
import org.reactome.server.analysis.service.report.ReportParameters;
import org.reactome.server.analysis.service.result.AnalysisSortType;
import org.reactome.server.analysis.service.result.AnalysisStoredResult;
import org.reactome.server.analysis.service.result.ComparatorFactory;
import org.reactome.server.analysis.service.result.PathwayNodeSummary;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DownloadHelper {

    private static final String DELIMITER = ",";

    public static FileSystemResource getHitPathwaysCVS(String filename, AnalysisStoredResult asr, String resource) throws IOException {
        long start = System.currentTimeMillis();
        File f = File.createTempFile(filename, "csv");
        FileWriter fw = new FileWriter(f);

        List<PathwayNodeSummary> pathways = filterPathwaysByResource(asr.getPathways(), resource);
        Collections.sort(pathways, getComparator("ENTITIES_PVALUE", "ASC", resource));
        fw.write(getAnalysisResultHeader(asr));
        if(resource.toUpperCase().equals("TOTAL")){
            for (PathwayNodeSummary summary : pathways) {
                fw.write(getPathwayNodeSummaryTotalRow(summary));
            }
        }else{
            Resource r = ResourceFactory.getResource(resource);
            if(r instanceof MainResource){
                MainResource mainResource = (MainResource) r;
                for (PathwayNodeSummary summary : pathways) {
                    fw.write(getPathwayNodeSummaryResourceRow(summary, mainResource));
                }
            }
        }
        fw.flush(); fw.close();

        ReportParameters reportParams = new ReportParameters(asr);
        reportParams.setMilliseconds(System.currentTimeMillis() - start);
        AnalysisReport.reportResultDownload(reportParams);

        return new FileSystemResource(f);
    }

    public static FileSystemResource getIdentifiersFoundMappingCVS(String filename, AnalysisStoredResult asr, String resource) throws IOException {
        long start = System.currentTimeMillis();
        File f = File.createTempFile(filename, "csv");
        FileWriter fw = new FileWriter(f);
        StringBuilder sb = new StringBuilder();

        MapSet<String, MainIdentifier> projection = new MapSet<String, MainIdentifier>();
        if(resource.toUpperCase().equals("TOTAL")){
            sb.append("Submitted identifier").append(DELIMITER).append("Found identifier").append(DELIMITER).append("Resource\n");
            fw.write(sb.toString());

            for (Identifier identifier : asr.getFoundEntitiesMap().keySet()) {
                projection.add(identifier.getValue().getId(), asr.getFoundEntitiesMap().getElements(identifier));
            }
            for (String identifier : projection.keySet()) {
                for (MainIdentifier mainIdentifier : projection.getElements(identifier)) {
                    //noinspection StringBufferReplaceableByString
                    StringBuilder line = new StringBuilder(identifier);
                    line.append(DELIMITER).append(mainIdentifier.getValue().getId());
                    line.append(DELIMITER).append(mainIdentifier.getResource().getName());
                    line.append("\n");
                    fw.write(line.toString());
                }
            }
        }else{
            sb.append("Submitted identifier").append(DELIMITER).append("Found identifier\n");
            fw.write(sb.toString());
            Resource r = ResourceFactory.getResource(resource);
            if(r instanceof MainResource){
                MainResource mainResource = (MainResource) r;
                MapSet<Identifier, MainIdentifier> aux = asr.getFoundEntitiesMap(mainResource);
                for (Identifier identifier : aux.keySet()) {
                    projection.add(identifier.getValue().getId(), aux.getElements(identifier));
                }
                for (String identifier : projection.keySet()) {
                    for (MainIdentifier mainIdentifier : projection.getElements(identifier)) {
                        //noinspection StringBufferReplaceableByString
                        StringBuilder line = new StringBuilder(identifier);
                        line.append(DELIMITER).append(mainIdentifier.getValue().getId());
                        line.append("\n");
                        fw.write(line.toString());
                    }
                }
            }
        }
        fw.flush(); fw.close();

        ReportParameters reportParams = new ReportParameters(asr);
        reportParams.setMilliseconds(System.currentTimeMillis() - start);
        AnalysisReport.reportMappingDownload(reportParams);

        return new FileSystemResource(f);
    }

    public static FileSystemResource getNotFoundIdentifiers(String filename, AnalysisStoredResult asr) throws IOException {
        long start = System.currentTimeMillis();
        File f = File.createTempFile(filename, "csv");
        FileWriter fw = new FileWriter(f);

        StringBuilder sb = new StringBuilder("Not found");
        for (String col : asr.getExpressionSummary().getColumnNames()) {
            sb.append(DELIMITER).append(col);
        }
        sb.append("\n");
        fw.write(sb.toString());

        for (AnalysisIdentifier analysisIdentifier : asr.getNotFound()) {
            fw.write(analysisIdentifier.getId());
            for (Double val : analysisIdentifier.getExp()) {
                fw.write(DELIMITER);fw.write(val.toString());
            }
            fw.write("\n");
        }
        fw.flush(); fw.close();

        ReportParameters reportParams = new ReportParameters(asr);
        reportParams.setMilliseconds(System.currentTimeMillis() - start);
        AnalysisReport.reportNotFoundDownload(reportParams);

        return new FileSystemResource(f);
    }

    private static String getAnalysisResultHeader(AnalysisStoredResult asr){
        StringBuilder line = new StringBuilder("Pathway identifier");
        line.append(DELIMITER).append("Pathway name");

        line.append(DELIMITER).append("# Entities found");
        line.append(DELIMITER).append("# Entities total");
        line.append(DELIMITER).append("Entities ratio");
        line.append(DELIMITER).append("Entities pValue");
        line.append(DELIMITER).append("Entities FDR");
        line.append(DELIMITER).append("# Reactions found");
        line.append(DELIMITER).append("# Reactions total");
        line.append(DELIMITER).append("Reactions ratio");
        for (String colName : asr.getExpressionSummary().getColumnNames()) {
            line.append(DELIMITER).append(colName);
        }

        line.append(DELIMITER).append("Species identifier");
        line.append(DELIMITER).append("Species name");

        line.append(DELIMITER).append("Submitted entities found");
        line.append(DELIMITER).append("Mapped entities");

        line.append(DELIMITER).append("Found reaction identifiers");
        return line.append("\n").toString();
    }

    private static String getPathwayNodeSummaryTotalRow(PathwayNodeSummary summary){
        String stId = summary.getStId();
        String id = (stId!=null && !summary.getStId().isEmpty()) ? stId : summary.getPathwayId().toString();
        StringBuilder line = new StringBuilder(id);
        line.append(DELIMITER).append("\"").append(summary.getName()).append("\"");

        PathwayNodeData data = summary.getData();
        line.append(DELIMITER).append(data.getEntitiesFound());
        line.append(DELIMITER).append(data.getEntitiesCount());
        line.append(DELIMITER).append(data.getEntitiesRatio());
        line.append(DELIMITER).append(data.getEntitiesPValue());
        line.append(DELIMITER).append(data.getEntitiesFDR());
        line.append(DELIMITER).append(data.getReactionsFound());
        line.append(DELIMITER).append(data.getReactionsCount());
        line.append(DELIMITER).append(data.getReactionsRatio());
        for (Double aDouble : data.getExpressionValuesAvg()) {
            line.append(DELIMITER).append(aDouble);
        }

        line.append(DELIMITER).append(summary.getSpecies().getSpeciesID());
        line.append(DELIMITER).append(summary.getSpecies().getName());

        StringBuilder submited = new StringBuilder();
        for (Identifier identifier : summary.getData().getIdentifierMap().keySet()) {
            submited.append(identifier.getValue()).append(";");
        }
        if(submited.length()>0){
            submited.delete(submited.length()-1, submited.length());
        }
        line.append(DELIMITER).append("\"").append(submited.toString()).append("\"");

        StringBuilder entities = new StringBuilder();
        for (AnalysisIdentifier identifier : summary.getData().getEntities()) {
            entities.append(identifier.getId()).append(";");
        }
        if(entities.length()>0){
            entities.delete(entities.length()-1, entities.length());
        }
        line.append(DELIMITER).append("\"").append(entities.toString()).append("\"");

        StringBuilder reactions = new StringBuilder();
        for (AnalysisReaction reaction : summary.getData().getReactions()) {
            reactions.append(reaction.toString()).append(";");
        }
        if(reactions.length()>0){
            reactions.delete(reactions.length()-1, reactions.length());
        }
        line.append(DELIMITER).append("\"").append(reactions.toString()).append("\"");

        return line.append("\n").toString();
    }

    private static String getPathwayNodeSummaryResourceRow(PathwayNodeSummary summary, MainResource resource){
        String stId = summary.getStId();
        String id = (stId!=null && !summary.getStId().isEmpty()) ? stId : summary.getPathwayId().toString();
        StringBuilder line = new StringBuilder(id);
        line.append(DELIMITER).append("\"").append(summary.getName()).append("\"");

        PathwayNodeData data = summary.getData();
        line.append(DELIMITER).append(data.getEntitiesFound(resource));
        line.append(DELIMITER).append(data.getEntitiesCount(resource));
        line.append(DELIMITER).append(data.getEntitiesRatio(resource));
        line.append(DELIMITER).append(data.getEntitiesPValue(resource));
        line.append(DELIMITER).append(data.getEntitiesFDR(resource));
        line.append(DELIMITER).append(data.getReactionsFound(resource));
        line.append(DELIMITER).append(data.getReactionsCount(resource));
        line.append(DELIMITER).append(data.getReactionsRatio(resource));
        for (Double aDouble : data.getExpressionValuesAvg(resource)) {
            line.append(DELIMITER).append(aDouble);
        }

        line.append(DELIMITER).append(summary.getSpecies().getSpeciesID());
        line.append(DELIMITER).append(summary.getSpecies().getName());

        //NOTE: We have to ensure we only add a submitted identifier once per row
        Set<String> uniqueSubmitted = new HashSet<String>();
        for (Identifier identifier : summary.getData().getIdentifierMap().keySet()) {
            uniqueSubmitted.add(identifier.getValue().getId());
        }
        StringBuilder submitted = new StringBuilder();
        for (String s : uniqueSubmitted) {
            submitted.append(s).append(";");
        }
        if(submitted.length()>0){
            submitted.delete(submitted.length()-1, submitted.length());
        }
        line.append(DELIMITER).append("\"").append(submitted.toString()).append("\"");

        StringBuilder entities = new StringBuilder();
        for (AnalysisIdentifier identifier : summary.getData().getEntities(resource)) {
            entities.append(identifier.getId()).append(";");
        }
        if(entities.length()>0){
            entities.delete(entities.length()-1, entities.length());
        }
        line.append(DELIMITER).append("\"").append(entities.toString()).append("\"");

        StringBuilder reactions = new StringBuilder();
        for (AnalysisReaction reaction : summary.getData().getReactions(resource)) {
            reactions.append(reaction.toString()).append(";");
        }
        if(reactions.length()>0){
            reactions.delete(reactions.length()-1, reactions.length());
        }
        line.append(DELIMITER).append("\"").append(reactions.toString()).append("\"");

        return line.append("\n").toString();
    }

    private static Comparator<PathwayNodeSummary> getComparator(String sortBy, String order, String resource){
        AnalysisSortType sortType = AnalysisSortType.getSortType(sortBy);
        if(resource!=null){
            Resource r = ResourceFactory.getResource(resource);
            if(r!=null && r instanceof MainResource){
                MainResource mr = (MainResource) r;
                if(order!=null && order.toUpperCase().equals("DESC")){
                    return Collections.reverseOrder(ComparatorFactory.getComparator(sortType, mr));
                }else{
                    return ComparatorFactory.getComparator(sortType, mr);
                }
            }
        }
        if(order!=null && order.toUpperCase().equals("DESC")){
            return Collections.reverseOrder(ComparatorFactory.getComparator(sortType));
        }else{
            return ComparatorFactory.getComparator(sortType);
        }
    }

    private static List<PathwayNodeSummary> filterPathwaysByResource(List<PathwayNodeSummary> pathways, String resource){
        List<PathwayNodeSummary> rtn;
        if(resource.toUpperCase().equals("TOTAL")){
            rtn = pathways;
        }else{
            rtn = new LinkedList<PathwayNodeSummary>();
            Resource r = ResourceFactory.getResource(resource);
            if(r instanceof MainResource){
                MainResource mr = (MainResource) r;
                for (PathwayNodeSummary pathway : pathways) {
                    if(pathway.getData().getEntitiesFound(mr)>0){
                        rtn.add(pathway);
                    }
                }
            }
        }
        return rtn;
    }
}
