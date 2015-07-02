package uk.ac.ebi.reactome.core.model.result;


import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Entry {
    private String dbId;
    private String stId;
    private String id;
    private String name;
    private String exactType;
    private String species;
    private String summation;
    private String referenceName;
    private String referenceIdentifier;
    private List<String> compartmentNames;
    private Boolean isDisease;
    private String databaseName;
    private String referenceURL;
    private String regulator;
    private String regulatedEntity;
    private String regulatorId;
    private String regulatedEntityId;

    public String getRegulatorId() {
        return regulatorId;
    }

    public void setRegulatorId(String regulatorId) {
        this.regulatorId = regulatorId;
    }

    public String getRegulatedEntityId() {
        return regulatedEntityId;
    }

    public void setRegulatedEntityId(String regulatedEntityId) {
        this.regulatedEntityId = regulatedEntityId;
    }

    public String getRegulator() {
        return regulator;
    }

    public void setRegulator(String regulator) {
        this.regulator = regulator;
    }

    public String getRegulatedEntity() {
        return regulatedEntity;
    }

    public void setRegulatedEntity(String regulatedEntity) {
        this.regulatedEntity = regulatedEntity;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getReferenceURL() {
        return referenceURL;
    }

    public void setReferenceURL(String referenceURL) {
        this.referenceURL = referenceURL;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsDisease() {
        return isDisease;
    }

    public void setIsDisease(Boolean isDisease) {
        this.isDisease = isDisease;
    }

    public String getExactType() {
        return exactType;
    }

    public void setExactType(String exactType) {
        this.exactType = exactType;
    }

    public List<String> getCompartmentNames() {
        return compartmentNames;
    }

    public void setCompartmentNames(List<String> compartmentNames) {
        this.compartmentNames = compartmentNames;
    }

    public String getDbId() {
        return dbId;
    }

    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    public String getStId() {
        return stId;
    }

    public void setStId(String stId) {
        this.stId = stId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSummation() {
        return summation;
    }

    public void setSummation(String summation) {
        this.summation = summation;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }



    public String getReferenceIdentifier() {
        return referenceIdentifier;
    }

    public void setReferenceIdentifier(String referenceIdentifier) {
        this.referenceIdentifier = referenceIdentifier;
    }


}
