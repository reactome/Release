package org.reactome.server.models2pathways.core.utils;

import org.reactome.server.models2pathways.biomodels.model.BioModel;
import org.reactome.server.models2pathways.reactome.model.PathwaySummary;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FileExporter {
    final static Logger logger = Logger.getLogger(FileExporter.class.getName());

    private static final String TAB = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String FILE_NAME = "models2pathways";
    private static final String PATHWAY_BROWSER_BASE_URL = "http://www.reactome.org/PathwayBrowser/#";
    private static final String GO_EVIDENCE_CODE = "IEA";
    private static String locationPath;
    private static FileWriter fileWriter;

    public static boolean createFile() {
        String locationPath = getLocationPath();
        removeFile();
        try {
            fileWriter = new FileWriter(locationPath + ".tsv", true);
        } catch (IOException e) {
            logger.info("Error on creating output file at: \n" +
                    locationPath + "tsv");
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("A tsv file has been created: " + locationPath);
        return true;
    }

    public static void addRow(PathwaySummary pathwaySummary, BioModel bioModel) {
        try {
            fileWriter.append(bioModel.getBioMdId());
            fileWriter.append(TAB);
            fileWriter.append(pathwaySummary.getStId());
            fileWriter.append(TAB);
            fileWriter.append(pathwaySummary.getEntities().getFdr().toString());
            fileWriter.append(TAB);
            fileWriter.append(PATHWAY_BROWSER_BASE_URL).append(pathwaySummary.getStId());
            fileWriter.append(TAB);
            fileWriter.append(pathwaySummary.getName());
            fileWriter.append(TAB);
            fileWriter.append(GO_EVIDENCE_CODE);
            fileWriter.append(TAB);
            fileWriter.append(bioModel.getSpecie().getName());
            fileWriter.append(NEW_LINE);
            fileWriter.flush();
        } catch (IOException e) {
            logger.info("Error on witting in file");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void closeFile() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            logger.info("Error on closing output file.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getLocationPath() {
        return locationPath + FILE_NAME;
    }

    public static void setLocationPath(String locationPath) {
        FileExporter.locationPath = locationPath;
    }

    private static void removeFile() {
        try {
            Files.deleteIfExists(Paths.get(getLocationPath() + ".tsv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
