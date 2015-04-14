package org.reactome.server.models2pathways.biomodels.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Stores the information in an rdf:bag inside an sbml file.
 *
 * @author Camille Laibe
 * @version 20140703
 */
public class Bag {
    private String qualifier;
    private List<Annotation> annotations;

    public Bag() {
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public List<Annotation> getAnnotations() {
        return this.annotations;
    }

    public void addAnnotation(Annotation annotation) {
        if (null == this.annotations) {
            this.annotations = new ArrayList<>();
        }
        this.annotations.add(annotation);
    }

    @Override
    public String toString() {
        return "Bag{" +
                "qualifier='" + qualifier + '\'' +
                ", annotations=" + annotations +
                '}';
    }
}
