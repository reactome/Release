package org.reactome.server.core.models2pathways.biomodels.helper;


import org.reactome.server.core.models2pathways.biomodels.model.Annotation;
import org.reactome.server.core.models2pathways.biomodels.model.Bag;
import org.reactome.server.core.models2pathways.biomodels.model.ModelElement;
import org.reactome.server.core.models2pathways.core.helper.NameSpaceHelper;
import org.reactome.server.core.models2pathways.core.helper.SpeciesHelper;
import org.reactome.server.core.models2pathways.core.model.Namespace;
import org.reactome.server.core.models2pathways.core.model.Specie;
import org.sbml.jsbml.*;

import javax.xml.stream.XMLStreamException;
import java.util.HashSet;
import java.util.Set;


/**
 * Extracts some model annotations (mainly from species) with the objective to use those in the reactome Analysis tool.
 *
 * @author Camille Laibe and Maximilian Koch
 * @version 20140704
 */
public class ExtractInformationFromSBMLModel {

    /**
     * Reads SBML-Model string and converts it to an Model-object
     *
     * @param bioMdSBML
     * @return
     */
    public static Model convertBioModelSBMLString(String bioMdSBML) {
        SBMLReader reader = new SBMLReader();
        SBMLDocument model = null;
        try {
            model = reader.readSBMLFromString(bioMdSBML);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return model != null ? model.getModel() : null;
    }

    /**
     * Extracts all the necessary annotations.
     */
    public static Set<Annotation> extractAnnotation(Model bioMdSBML) {
        Set<Annotation> annotations = new HashSet<>();
        for (Species species : bioMdSBML.getListOfSpecies()) {
            ModelElement component = extractComponentAnnotation(species);
            annotations.addAll(displayRelevantAnnotation(component));
        }
        return annotations;
    }

    /**
     * Extracts the taxonomical annotation from the model object.
     * Only considers "bqbiol:occursIn" or "bqbiol:hasTaxon" qualifiers.
     */
    public static Specie getModelTaxonomy(Model bioMdSBML) {
        Specie specie = null;
        org.sbml.jsbml.Annotation modelAnnotation = bioMdSBML.getAnnotation();
        for (CVTerm cvTerm : modelAnnotation.getListOfCVTerms()) {
            String qualifier = getQualifier(cvTerm);
            // retrieves all the URIs
            for (String uri : cvTerm.getResources()) {
                if ((qualifier.equalsIgnoreCase("bqbiol:occursIn") || qualifier.equalsIgnoreCase("bqbiol:hasTaxon"))
                        && (uri.contains("taxonomy"))) {
                    specie = SpeciesHelper.getInstance().getSpecieByBioMdSpecieId(Long.valueOf(extractIdFromURI(uri)));
                }
            }
        }
        return specie;
    }

    /**
     * Extract the necessary annotation from a model component.
     *
     * @param //species
     */
    private static ModelElement extractComponentAnnotation(SBase component) {
        ModelElement element = new ModelElement();
        for (CVTerm cvTerm : component.getCVTerms()) {
            Bag bag = new Bag();
            bag.setQualifier(getQualifier(cvTerm));
            // retrieves all the URIs
            for (String uri : cvTerm.getResources()) {
                Annotation annotation = new Annotation(NameSpaceHelper.getInstance().getNamespace(extractNamespaceFromURI(uri)),
                        extractIdFromURI(uri), uri);
                if (annotation.getNamespace() == null) {
                    continue;
                }
                bag.addAnnotation(annotation);
            }
            if (bag.getAnnotations() != null) {
                element.addBag(bag);
            }
        }
        return element;
    }

    /**
     * Get the qualifier's name from a CVTerm.
     */
    private static String getQualifier(CVTerm cvTerm) {
        String qualifier;
        String namespacePrefix = cvTerm.isModelQualifier() ? "bqmodel" : "bqbiol";
        String qualifierName;
        if (cvTerm.isModelQualifier()) {
            qualifierName = cvTerm.getModelQualifierType().getElementNameEquivalent();
        } else {
            qualifierName = cvTerm.getBiologicalQualifierType().getElementNameEquivalent();
        }
        qualifier = namespacePrefix + ":" + qualifierName;

        return qualifier;
    }

    /**
     * Displays all annotations of a given sbml element which are relevant for the reactome data analyis tool.
     */
    private static Set<Annotation> displayRelevantAnnotation(ModelElement component) {
        Integer counterTmp;
        Set<Annotation> annotations = new HashSet<>();
        for (Namespace namespace : NameSpaceHelper.getInstance().getNamespaces()) {
            counterTmp = findAllAnnotationFromDataCollection(component, namespace);
            if (counterTmp > 0) {
                annotations.addAll(getAllAnnotationFromDataCollection(component, namespace));
            }
        }
        return annotations;
    }

    /**
     * Finds and counts all the annotation of a sbml component from a given data collection.
     */
    private static Integer findAllAnnotationFromDataCollection(ModelElement component, Namespace namespace) {
        Integer counter = 0;
        if (component.getBags() != null) {
            for (Bag bag : component.getBags()) {
                for (Annotation annotation : bag.getAnnotations()) {
                    try {
                        if (annotation.getNamespace().getName().equals(namespace.getName())) {
                            counter++;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
        return counter;
    }

    /**
     * Prints all the annotation of a sbml component from a given data collection.
     */
    private static Set<Annotation> getAllAnnotationFromDataCollection(ModelElement component, Namespace namespace) {
        Set<Annotation> annotations = new HashSet<>();
        for (Bag bag : component.getBags()) {
            for (Annotation annotation : bag.getAnnotations()) {
                if (annotation.getNamespace().getName().equalsIgnoreCase(namespace.getName())) {
                    if (annotation.getEntityId().contains(":")) {
                        annotation.setEntityId(annotation.getEntityId().split(":")[1]);
                    }
                    annotations.add(annotation);
                }
            }
        }
        return annotations;
    }

    /**
     * Extracts the entity identifier from an Identifiers.org URI.
     * E.g. http://identifiers.org/taxonomy/8292   >>>  8292
     */
    private static String extractIdFromURI(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
    /**
     * Extracts the namespace from an Identifiers.org URI.
     * E.g. http://identifiers.org/taxonomy/8292   >>>  taxonomy
     */
    //TODO: Got: "StringIndexOutOfBoundsException"
    private static String extractNamespaceFromURI(String uri) {
        try {
            return uri.substring(23, uri.indexOf("/", 24));
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("StringIndexOutOfBoundsException on " + uri);
        }
        return null;
    }
}