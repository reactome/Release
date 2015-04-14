package org.reactome.server.core.models2pathways.core;

import org.reactome.server.core.models2pathways.biomodels.helper.BioModelHelper;
import org.reactome.server.core.models2pathways.biomodels.model.BioModel;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Producer implements Runnable {
    final static Logger logger = Logger.getLogger(Producer.class.getName());

    private BlockingQueue<BioModel> bioModelBlockingQueue;

    public Producer(BlockingQueue<BioModel> bioModelBlockingQueue) {
        this.bioModelBlockingQueue = bioModelBlockingQueue;
    }

    @Override
    public void run() {
        try {
            for (String bioMdId : BioModelHelper.getAllSignificantBioModelIds()) {
                BioModel bioModel = BioModelHelper.getBioModelByBioModelId(bioMdId);
                if (bioModel.getAnnotations().isEmpty()) {
                    //There is no point to continue for model without annotations
                    continue;
                }
                System.out.println("Ready");
                try {
                    bioModelBlockingQueue.put(bioModel);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("Error on Thread (" + Thread.currentThread().getName() + ") sleeping process");
                    e.printStackTrace();
                }
            }
        } finally {
            Thread.currentThread().interrupt();
            logger.info("Producer has finished");
        }
    }
}
