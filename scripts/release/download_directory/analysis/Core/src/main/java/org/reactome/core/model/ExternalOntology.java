/*
 * Created on Jun 7, 2013
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author gwu
 *
 */
@XmlRootElement
public class ExternalOntology extends DatabaseObject {
    private String definition;
    private String identifier;
    private List<ExternalOntology> instanceOf;
    private List<String> name;
    private ReferenceDatabase referenceDatabase;
    private List<String> synonym;
    
    public ExternalOntology() {
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<ExternalOntology> getInstanceOf() {
        return instanceOf;
    }

    public void setInstanceOf(List<ExternalOntology> instanceOf) {
        this.instanceOf = instanceOf;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public ReferenceDatabase getReferenceDatabase() {
        return referenceDatabase;
    }

    public void setReferenceDatabase(ReferenceDatabase referenceDatabase) {
        this.referenceDatabase = referenceDatabase;
    }

    public List<String> getSynonym() {
        return synonym;
    }

    public void setSynonym(List<String> synonym) {
        this.synonym = synonym;
    }
    
}
