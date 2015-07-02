package uk.ac.ebi.reactome.core.model.result.submodels;

import java.util.List;
import java.util.Map;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class ReferenceEntity {

    private String referenceName;
    private String referenceIdentifier;
    private String variantIdentifier;
    private Database database;

    private List<String> referenceSynonyms;
    private List<String> otherIdentifier;
    private List<String> secondaryIdentifier;
    private List<String> geneNames;
    private List<String> description;

    private List<EntityReference> derivedEwas;

    //Gene Product
    private List<String> chain;
    private Map<String, List<CrossReference>> referenceGenes;
    private Map<String, List<CrossReference>>  referenceTranscript;

    //Isoform
    private List<CrossReference> isoformParent;

    // Reference Molecule

    private String formula;
    private String atomicConnectivity;




    public List<String> getSecondaryIdentifier() {
        return secondaryIdentifier;
    }

    public void setSecondaryIdentifier(List<String> secondaryIdentifier) {
        this.secondaryIdentifier = secondaryIdentifier;
    }

    public List<String> getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(List<String> geneNames) {
        this.geneNames = geneNames;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }


    public List<EntityReference> getDerivedEwas() {
        return derivedEwas;
    }

    public Map<String, List<CrossReference>> getReferenceTranscript() {
        return referenceTranscript;
    }

    public void setReferenceTranscript(Map<String, List<CrossReference>> referenceTranscript) {
        this.referenceTranscript = referenceTranscript;
    }

    public Map<String, List<CrossReference>> getReferenceGenes() {
        return referenceGenes;
    }

    public void setReferenceGenes(Map<String, List<CrossReference>> referenceGenes) {
        this.referenceGenes = referenceGenes;
    }

    public List<CrossReference> getIsoformParent() {
        return isoformParent;
    }

    public void setIsoformParent(List<CrossReference> isoformParent) {
        this.isoformParent = isoformParent;
    }

    public String getVariantIdentifier() {
        return variantIdentifier;
    }

    public void setVariantIdentifier(String variantIdentifier) {
        this.variantIdentifier = variantIdentifier;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getAtomicConnectivity() {
        return atomicConnectivity;
    }

    public void setAtomicConnectivity(String atomicConnectivity) {
        this.atomicConnectivity = atomicConnectivity;
    }

    public void setDerivedEwas(List<EntityReference> derivedEwas) {
        this.derivedEwas = derivedEwas;
    }

    public List<String> getReferenceSynonyms() {
        return referenceSynonyms;
    }

    public void setReferenceSynonyms(List<String> referenceSynonyms) {
        this.referenceSynonyms = referenceSynonyms;
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


    public List<String> getOtherIdentifier() {
        return otherIdentifier;
    }

    public void setOtherIdentifier(List<String> otherIdentifier) {
        this.otherIdentifier = otherIdentifier;
    }



    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}
