package org.reactome.server.models2pathways.core.helper;

import org.reactome.server.analysis.core.model.HierarchiesData;
import org.reactome.server.analysis.core.model.SpeciesNode;
import org.reactome.server.analysis.core.model.UserData;
import org.reactome.server.models2pathways.biomodels.helper.AnnotationHelper;
import org.reactome.server.models2pathways.biomodels.model.BioModel;
import org.reactome.server.models2pathways.core.entrypoint.Models2Pathways;
import org.reactome.server.models2pathways.core.utils.FileExporter;
import org.reactome.server.models2pathways.reactome.helper.AnalysisCoreHelper;
import org.reactome.server.models2pathways.reactome.model.AnalysisResult;
import org.reactome.server.models2pathways.reactome.model.AnalysisStoredResult;
import org.reactome.server.models2pathways.reactome.model.PathwaySummary;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Consumer implements Runnable {
    final static Logger logger = Logger.getLogger(Consumer.class.getName());

    private Double significantFDR;
    private Double extendedFDR;
    private Double reactionCoverage;
    private AnalysisCoreHelper analysisCoreHelper;
    private BlockingQueue<BioModel> bioModelBlockingQueue;

    public Consumer(BlockingQueue<BioModel> bioModelBlockingQueue, double significantFDR, double reactionCoverage) {
        this.bioModelBlockingQueue = bioModelBlockingQueue;
        this.significantFDR = significantFDR;
        this.reactionCoverage = reactionCoverage;
        this.analysisCoreHelper = new AnalysisCoreHelper();
    }

    public Consumer(BlockingQueue<BioModel> bioModelBlockingQueue, double significantFDR, double extendedFDR, String reactionCoverage) {
        this.bioModelBlockingQueue = bioModelBlockingQueue;
        this.significantFDR = significantFDR;
        this.extendedFDR = extendedFDR;
        this.reactionCoverage = Double.valueOf(reactionCoverage);
        this.analysisCoreHelper = new AnalysisCoreHelper();
    }

    @Override
    public void run() {
        while (Models2Pathways.isProducerAlive()) {
            BioModel bioModel;
            try {
                bioModel = bioModelBlockingQueue.take();
            } catch (InterruptedException e) {
                logger.info("Error on process BioModels");
                e.printStackTrace();
                continue;
            }

            //Remove all trivial chemicals
            bioModel.getAnnotations().removeAll(AnnotationHelper.getAnnotationsWithTrivialChemicals());
            //On my way to get the AnalysisResult Object!!!
            //TODO: clean up, this is not necessary!
            UserData userData;
            SpeciesNode speciesNode;
            HierarchiesData hierarchiesData;
            AnalysisStoredResult analysisStoredResult;
            List<PathwaySummary> pathwaySummaryList;
            AnalysisResult analysisResult;
            try {
                userData = analysisCoreHelper.getUserData(bioModel.getName(), bioModel.getAnnotations());
                speciesNode = analysisCoreHelper.convertToSpeciesNode(SpeciesHelper.getInstance().getSpecieByBioMdSpecieId(bioModel.getSpecie().getBioMdId()));
                hierarchiesData = analysisCoreHelper.getHierarchiesData(userData, speciesNode);
                analysisStoredResult = new AnalysisStoredResult(userData, hierarchiesData);
                analysisStoredResult.setHitPathways(hierarchiesData.getUniqueHitPathways(speciesNode));
                pathwaySummaryList = analysisCoreHelper.getPathwaySummaryList(hierarchiesData.getUniqueHitPathways(speciesNode), "TOTAL");
                analysisResult = analysisCoreHelper.getAnalysisResult(analysisStoredResult, pathwaySummaryList);
            } catch (NullPointerException e) {
                logger.info("NullPointerException on creating analysisResult object");
                e.printStackTrace();
                continue;
            }
            //Check resource
            String resourceSummary = analysisCoreHelper.getResource(analysisResult.getResourceSummary());
            //If other resource 
            if (!resourceSummary.equals("TOTAL")) {
                pathwaySummaryList = analysisCoreHelper.getPathwaySummaryList(hierarchiesData.getUniqueHitPathways(speciesNode), resourceSummary);
                analysisResult = analysisCoreHelper.getAnalysisResult(analysisStoredResult, pathwaySummaryList);
            }
            //remove not significant pathways
            analysisResult.setReliablePathways(analysisCoreHelper.getReliablePathways(analysisResult.getPathways(), significantFDR, bioModel, reactionCoverage));
            if (analysisResult.getReliablePathways().isEmpty() && extendedFDR != null) {
                analysisResult.setReliablePathways(analysisCoreHelper.getReliablePathways(analysisResult.getPathways(), extendedFDR, bioModel, reactionCoverage));
            }
            for (PathwaySummary pathwaySummary : analysisResult.getReliablePathways()) {
                FileExporter.addRow(pathwaySummary, bioModel);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Error on Thread (" + Thread.currentThread().getName() + ") sleeping process");
                e.printStackTrace();
            }
        }
        logger.info("Consumer process has been finished.");
        FileExporter.closeFile();
        logger.info("\nProcess has been finished");
        System.exit(1);
    }
}