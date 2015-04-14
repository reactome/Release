package org.reactome.server.core.models2pathways.core;

//import org.reactome.server.core.models2pathways.biomodels.helper.BioModelHelper;
import org.reactome.server.core.models2pathways.biomodels.helper.BioModelHelper;
import org.reactome.server.core.models2pathways.biomodels.helper.ExtractInformationFromSBMLModel;
import org.reactome.server.core.models2pathways.biomodels.model.BioModel;
import org.reactome.server.core.models2pathways.core.helper.SpeciesHelper;
import org.reactome.server.core.models2pathways.core.helper.TrivialChemicalHelper;
import org.sbml.jsbml.Model;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Producer implements Runnable {
    final static Logger logger = Logger.getLogger(Producer.class.getName());

    private BlockingQueue<BioModel> bioModelBlockingQueue;
    private String path = "/Users/maximiliankoch/Documents/Reactome/BioModels/curated/";

    public Producer(BlockingQueue<BioModel> bioModelBlockingQueue) {
        this.bioModelBlockingQueue = bioModelBlockingQueue;
    }

    @Override
    public void run() {
        try {
            for(File file : new File(path).listFiles()){
                if(file.getName().contains("BIOMD")){
                    BioModel bioModel = BioModelHelper.getBioModelByBioModelId(file);
                    if(bioModel.getSpecie() != null && SpeciesHelper.getInstance().getSpecies().contains(bioModel.getSpecie())){
                        bioModelBlockingQueue.put(BioModelHelper.getBioModelByBioModelId(file));
                    }
                }
            }
//            for (String bioMdId : BioModelHelper.getAllSignificantBioModelIds()) {
//                BioModel bioModel = BioModelHelper.getBioModelByBioModelId(bioMdId);
//                if (bioModel.getAnnotations().isEmpty()) {
//                    //There is no point to continue for model without annotations
//                    continue;
//                }
//                System.out.println("Ready");
//                try {
//                    bioModelBlockingQueue.put(bioModel);
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    logger.info("Error on Thread (" + Thread.currentThread().getName() + ") sleeping process");
//                    e.printStackTrace();
//                }
//            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
            logger.info("Producer has finished");
        }
    }
}
