package org.reactome.server.core.models2pathways.biomodels.helper;

import org.reactome.server.core.models2pathways.biomodels.model.Annotation;
import org.reactome.server.core.models2pathways.biomodels.model.BioModel;
import org.reactome.server.core.models2pathways.core.helper.SpeciesHelper;
import org.reactome.server.core.models2pathways.core.model.Specie;
import org.sbml.jsbml.Model;
import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class BioModelHelper {
    final static Logger logger = Logger.getLogger(BioModelHelper.class.getName());

    private static final BioModelsWSClient client = new BioModelsWSClient();

    public static Set<String> getAllSignificantBioModelIds() {
        Set<String> allBioMdIds = new HashSet<>();
        for (Specie specie : SpeciesHelper.getInstance().getSpecies()) {
            try {
                String[] tempBioMdIds = client.getModelsIdByTaxonomyId(specie.getBioMdId().toString());
                Collections.addAll(allBioMdIds, tempBioMdIds);
            } catch (BioModelsWSException e) {
                logger.info(
                        "Error on using BioModels-WebService, to retrieve BioModels Id by Taxonomy Id\n" +
                                "Species Name: \t" + specie.getName() + "\n" +
                                "Species BioModels Id: \t" + specie.getBioMdId() + "\n" +
                                "Species Reactome Id: \t" + specie.getReactId() + "\n");
                e.printStackTrace();
            }
        }
        return allBioMdIds;
    }

    public static BioModel getBioModelByBioModelId(String bioMdId) {
        Model model = convertBioModelSBMLToObject(getBioModelSBMLByBioModelId(bioMdId));
        String bioMdName = model.getName();
        Specie specie = getBioModelTaxonomy(model);
        Set<Annotation> annotations = getBioModelAnnotations(model);
        return new BioModel(bioMdName, bioMdId, specie, annotations);
    }

    /**
     * Returns the sbml-Model based on the given sbml-Model-Name
     *
     * @param bioMdId
     * @return
     */
    private static String getBioModelSBMLByBioModelId(String bioMdId) {
        String bioMdSBML = null;
        try {
            bioMdSBML = client.getModelSBMLById(bioMdId);
            System.out.println(bioMdSBML);
        } catch (BioModelsWSException e) {
            logger.info("Error on retrieving SBML-Files on " + bioMdId + ".\n Please restart the process");
            e.printStackTrace();
            System.exit(1);
        }
        if (bioMdSBML == null) {
            logger.info("Retrieved BioModel-SBML, for " + bioMdId + " is 'null' \n Please restart the process");
            System.exit(1);
        }
        return bioMdSBML;
    }

    /**
     * Getting information out of the SBML-Model/XML
     */
    private static Model convertBioModelSBMLToObject(String bioMdSBML) {
        return ExtractInformationFromSBMLModel.convertBioModelSBMLString(bioMdSBML);
    }

    private static Specie getBioModelTaxonomy(Model model) {
        return ExtractInformationFromSBMLModel.getModelTaxonomy(model);
    }

    private static Set<Annotation> getBioModelAnnotations(Model model) {
        return ExtractInformationFromSBMLModel.extractAnnotation(model);
    }
}
