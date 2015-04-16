package org.reactome.server.models2pathways.core.helper;

//import BioModelHelper;

import org.reactome.server.models2pathways.biomodels.helper.BioModelHelper;
import org.reactome.server.models2pathways.biomodels.model.BioModel;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Producer implements Runnable {
    final static Logger logger = Logger.getLogger(Producer.class.getName());
    private static String path;
    private BlockingQueue<BioModel> bioModelBlockingQueue;

    public Producer(BlockingQueue<BioModel> bioModelBlockingQueue) {
        this.bioModelBlockingQueue = bioModelBlockingQueue;
    }

    public static void setPath(String path) {
        Producer.path = path;
    }

    @Override
    public void run() {
        try {
            File files[] = new File(path).listFiles();
            if (files == null) {
                logger.info("No files in BioModels-Folder");
                Thread.currentThread().interrupt();
                System.exit(1);
            }
            for (File file : files) {
                if (file.getName().startsWith("BIOMD")) {
                    BioModel bioModel = BioModelHelper.getBioModelByBioModelId(file);
                    if (bioModel.getSpecie() != null && SpeciesHelper.getInstance().getSpecies().contains(bioModel.getSpecie())) {
                        bioModelBlockingQueue.put(BioModelHelper.getBioModelByBioModelId(file));
                    }
                }
            }
        } catch (InterruptedException | NullPointerException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
            logger.info("Producer has finished");
        }
    }
}
