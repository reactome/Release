package uk.ac.ebi.reactome.solr.indexer.model;

import org.apache.solr.client.solrj.beans.Field;

import java.util.List;

/**
 * JAVA BEAN representing the Document that is going to be Indexed into Solr
 * @Field marks a Solr Field
 * Created by flo on 4/29/14.
 */
public class IndexDocument {

    @Field
    private Long dbId;
    @Field
    private String stId;
    @Field
    private String name;
    @Field
    private String type;
    @Field
    private String exactType;
    @Field
    private Boolean isDisease;

    @Field
    private List<String> diseaseId;
    @Field
    private List<String> diseaseName;
    @Field
    private List<String> diseaseSynonyms;

    @Field
    private String species;
    @Field
    private List<String> relatedSpecies;

    private String taxId;

    @Field
    private List<String> synonyms;
    @Field
    private String summation;

    @Field
    private String inferredSummation;
    @Field
    private String regulatorId;
    @Field
    private String regulatedEntityId;
    @Field
    private String regulator;
    @Field
    private String regulatedEntity;
    @Field
    private List<String> compartmentName;
    @Field
    private List<String> compartmentAccession;

    @Field
    private List<String> literatureReferenceTitle;
    @Field
    private List<String> literatureReferenceAuthor;
    @Field
    private List<String> literatureReferencePubMedId;
    @Field
    private List<String> literatureReferenceIsbn;

    @Field
    private String goBiologicalProcessName;
    @Field
    private List<String> goBiologicalProcessAccessions;

    @Field
    private String goCellularComponentName;
    @Field
    private List<String> goCellularComponentAccessions;

    @Field
    private List<String> goMolecularFunctionName;
    @Field
    private List<String> goMolecularFunctionAccession;

    // Reference Entity
    @Field
    private List<String> keywords;

    @Field
    private List<String> crossReferences;


    @Field
    private List<String> referenceCrossReferences;

    private List<CrossReference> allCrossReferences;

    @Field
    private String referenceName;
    @Field
    private List<String> referenceIdentifiers;
    @Field
    private String referenceURL;
    @Field
    private String databaseName;

    @Field
    private List<String> referenceSynonyms;

    @Field
    private List<String> referenceOtherIdentifier;
    @Field
    private List<String> referenceGeneNames;


    // Auto Generated Getters Setters


    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public List<String> getGoBiologicalProcessAccessions() {
        return goBiologicalProcessAccessions;
    }

    public List<String> getGoCellularComponentAccessions() {
        return goCellularComponentAccessions;
    }

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

    public Boolean getIsDisease() {
        return isDisease;
    }

    public void setIsDisease(Boolean isDisease) {
        this.isDisease = isDisease;
    }

    public List<String> getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(List<String> diseaseId) {
        this.diseaseId = diseaseId;
    }

    public List<String> getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(List<String> diseaseName) {
        this.diseaseName = diseaseName;
    }

    public List<String> getDiseaseSynonyms() {
        return diseaseSynonyms;
    }

    public void setDiseaseSynonyms(List<String> diseaseSynonyms) {
        this.diseaseSynonyms = diseaseSynonyms;
    }

    public String getExactType() {
        return exactType;
    }

    public void setExactType(String exactType) {
        this.exactType = exactType;
    }

    public String getInferredSummation() {
        return inferredSummation;
    }

    public void setInferredSummation(String inferredSummation) {
        this.inferredSummation = inferredSummation;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }



    public List<String> getReferenceCrossReferences() {
        return referenceCrossReferences;
    }

    public void setReferenceCrossReferences(List<String> referenceCrossReferences) {
        this.referenceCrossReferences = referenceCrossReferences;
    }

    public List<String> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(List<String> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public List<CrossReference> getAllCrossReferences() {
        return allCrossReferences;
    }

    public void setAllCrossReferences(List<CrossReference> allCrossReferences) {
        if (this.allCrossReferences == null) {
            this.allCrossReferences = allCrossReferences;
        } else {
            this.allCrossReferences.addAll(allCrossReferences);
        }

    }

    public String getRegulatedEntity() {
        return regulatedEntity;
    }

    public void setRegulatedEntity(String regulatedEntity) {
        this.regulatedEntity = regulatedEntity;
    }

    public String getRegulator() {
        return regulator;
    }

    public void setRegulator(String regulator) {
        this.regulator = regulator;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public List<String> getRelatedSpecies() {
        return relatedSpecies;
    }

    public void setRelatedSpecies(List<String> relatedSpecies) {
        this.relatedSpecies = relatedSpecies;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getSummation() {
        return summation;
    }

    public void setSummation(String summation) {
        this.summation = summation;
    }

    public List<String> getCompartmentName() {
        return compartmentName;
    }

    public void setCompartmentName(List<String> compartmentName) {
        this.compartmentName = compartmentName;
    }

    public List<String> getCompartmentAccession() {
        return compartmentAccession;
    }

    public void setCompartmentAccession(List<String> compartmentAccession) {
        this.compartmentAccession = compartmentAccession;
    }

    public List<String> getLiteratureReferenceTitle() {
        return literatureReferenceTitle;
    }

    public void setLiteratureReferenceTitle(List<String> literatureReferenceTitle) {
        this.literatureReferenceTitle = literatureReferenceTitle;
    }

    public List<String> getLiteratureReferenceAuthor() {
        return literatureReferenceAuthor;
    }

    public void setLiteratureReferenceAuthor(List<String> literatureReferenceAuthor) {
        this.literatureReferenceAuthor = literatureReferenceAuthor;
    }

    public List<String> getLiteratureReferencePubMedId() {
        return literatureReferencePubMedId;
    }

    public void setLiteratureReferencePubMedId(List<String> literatureReferencePubMedId) {
        this.literatureReferencePubMedId = literatureReferencePubMedId;
    }

    public List<String> getLiteratureReferenceIsbn() {
        return literatureReferenceIsbn;
    }

    public void setLiteratureReferenceIsbn(List<String> literatureReferenceIsbn) {
        this.literatureReferenceIsbn = literatureReferenceIsbn;
    }

    public String getGoBiologicalProcessName() {
        return goBiologicalProcessName;
    }

    public void setGoBiologicalProcessName(String goBiologicalProcessName) {
        this.goBiologicalProcessName = goBiologicalProcessName;
    }

    public List<String> getGoBiologicalProcessAccession() {
        return goBiologicalProcessAccessions;
    }

    public void setGoBiologicalProcessAccessions(List<String> goBiologicalProcessAccessions) {
        this.goBiologicalProcessAccessions = goBiologicalProcessAccessions;
    }

    public String getGoCellularComponentName() {
        return goCellularComponentName;
    }

    public void setGoCellularComponentName(String goCellularComponentName) {
        this.goCellularComponentName = goCellularComponentName;
    }

    public List<String> getGoCellularComponentAccession() {
        return goCellularComponentAccessions;
    }

    public void setGoCellularComponentAccessions(List<String> goCellularComponentAccessions) {
        this.goCellularComponentAccessions = goCellularComponentAccessions;
    }

    public List<String> getGoMolecularFunctionName() {
        return goMolecularFunctionName;
    }

    public void setGoMolecularFunctionName(List<String> goMolecularFunctionName) {
        this.goMolecularFunctionName = goMolecularFunctionName;
    }

    public List<String> getGoMolecularFunctionAccession() {
        return goMolecularFunctionAccession;
    }

    public void setGoMolecularFunctionAccession(List<String> goMolecularFunctionAccession) {
        this.goMolecularFunctionAccession = goMolecularFunctionAccession;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public List<String> getReferenceSynonyms() {
        return referenceSynonyms;
    }

    public void setReferenceSynonyms(List<String> referenceSynonyms) {
        this.referenceSynonyms = referenceSynonyms;
    }

    public List<String> getReferenceIdentifiers() {
        return referenceIdentifiers;
    }

    public void setReferenceIdentifiers(List<String> referenceIdentifiers) {
        this.referenceIdentifiers = referenceIdentifiers;
    }

    public List<String> getReferenceOtherIdentifier() {
        return referenceOtherIdentifier;
    }

    public void setReferenceOtherIdentifier(List<String> referenceOtherIdentifier) {
        this.referenceOtherIdentifier = referenceOtherIdentifier;
    }

    public List<String> getReferenceGeneNames() {
        return referenceGeneNames;
    }

    public void setReferenceGeneNames(List<String> referenceGeneNames) {
        this.referenceGeneNames = referenceGeneNames;
    }

    public String getReferenceURL() {
        return referenceURL;
    }

    public void setReferenceURL(String referenceURL) {
        this.referenceURL = referenceURL;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}