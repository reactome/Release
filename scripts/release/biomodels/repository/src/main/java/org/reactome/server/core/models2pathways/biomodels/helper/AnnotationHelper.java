package org.reactome.server.core.models2pathways.biomodels.helper;

import org.reactome.server.core.models2pathways.biomodels.model.Annotation;
import org.reactome.server.core.models2pathways.core.helper.NameSpaceHelper;
import org.reactome.server.core.models2pathways.core.helper.TrivialChemicalHelper;
import org.reactome.server.core.models2pathways.core.model.Namespace;
import org.reactome.server.core.models2pathways.core.model.TrivialChemical;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class AnnotationHelper {

    //TODO Question: Do we need this if we don't use the webservice? 
//    public static String toAnalysisFormat(String model, Set<Annotation> annotations) {
//        StringBuilder annotationsInAnalysisFormat = new StringBuilder();
//        //Adding the name of the model to the sample data for a better identification in the PathwayBrowser result
//        annotationsInAnalysisFormat.append("#").append(model).append(System.getProperty("line.separator"));
//        for (Annotation annotation : annotations) {
//            annotationsInAnalysisFormat.append(annotation.getEntityId()).append(System.getProperty("line.separator"));
//        }
//        return String.valueOf(annotationsInAnalysisFormat);
//    }

    public static Set<Annotation> getAnnotationsWithTrivialChemicals() {
        Set<Annotation> annotations = new HashSet<>();
        for (Namespace namespace : NameSpaceHelper.getInstance().getNamespacesWithTrivialChemicals()) {
            for (TrivialChemical trivialChemical : TrivialChemicalHelper.getInstance().getTrivialChemicals()) {
                annotations.add(new Annotation(namespace, trivialChemical.getId().toString()));
            }
        }
        return annotations;
    }
}
