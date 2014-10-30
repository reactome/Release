/*
 * Created on Jun 7, 2013
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author gwu
 *
 */
@XmlRootElement
public class FunctionalStatus extends DatabaseObject {
    private FunctionalStatusType functionalStatusType;
    private SequenceOntology structuralVariant;
    
    public FunctionalStatus() {
        
    }

    public FunctionalStatusType getFunctionalStatusType() {
        return functionalStatusType;
    }

    public void setFunctionalStatusType(FunctionalStatusType functionalStatusType) {
        this.functionalStatusType = functionalStatusType;
    }

    public SequenceOntology getStructuralVariant() {
        return structuralVariant;
    }

    public void setStructuralVariant(SequenceOntology structuralVariant) {
        this.structuralVariant = structuralVariant;
    }
    
    
}
