package org.reactome.server.models2pathways.biomodels.helper;

import org.reactome.server.models2pathways.biomodels.model.Annotation;
import org.reactome.server.models2pathways.core.helper.NameSpaceHelper;
import org.reactome.server.models2pathways.core.helper.TrivialChemicalHelper;
import org.reactome.server.models2pathways.core.model.Namespace;
import org.reactome.server.models2pathways.core.model.TrivialChemical;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class AnnotationHelper {

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
