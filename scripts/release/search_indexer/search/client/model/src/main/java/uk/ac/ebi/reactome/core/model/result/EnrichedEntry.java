package uk.ac.ebi.reactome.core.model.result;

import uk.ac.ebi.reactome.core.model.result.submodels.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class EnrichedEntry extends Entry {

    private Set<TreeNode> locationsPathwayBrowser;

    private List<String> summations;
    private List<String> synonyms;

    private List<Literature> literature;
    private List<EntityReference> orthologousEvents;
    private List<Disease> diseases;

    private Map<String, List<CrossReference>>  crossReferences;
    private Map<String, List<EntityReference>> referedEntities; // this can be refered Inputs Outputs ... but also from a ReferenceEntity to a Ewaas

    private EntityReference reverseReaction;

    private List<EntityReference> inferredFrom;
    private List<EntityReference> inferredTo;

    private List<GoTerm> compartments;
    private List<GoTerm> goMolecularComponent;
    private GoTerm goBiologicalProcess;


    private List<EntityReference> input;
    private List<EntityReference> output;
    private List<EntityReference> candidates;
    private List<EntityReference> member;
    private List<EntityReference> repeatedUnits;
    private List<EntityReference> components;
    private List<EntityReference> entityOnOtherCell;

    private ReferenceEntity referenceEntity;

    private List<CatalystActivity> catalystActivities;
    private Regulation regulation;
    private Map<String, List<Regulation>> regulatedEvents;
    private Map<String, List<Regulation>> regulatingEntities;

    private List<ModifiedResidue> modifiedResidues;

    public Set<TreeNode> getLocationsPathwayBrowser() {
        return locationsPathwayBrowser;
    }

    public void setLocationsPathwayBrowser(Set<TreeNode> locationsPathwayBrowser) {
        this.locationsPathwayBrowser = locationsPathwayBrowser;
    }

    public List<String> getSummations() {
        return summations;
    }

    public void setSummations(List<String> summations) {
        this.summations = summations;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public List<Literature> getLiterature() {
        return literature;
    }

    public void setLiterature(List<Literature> literature) {
        this.literature = literature;
    }

    public List<EntityReference> getOrthologousEvents() {
        return orthologousEvents;
    }

    public void setOrthologousEvents(List<EntityReference> orthologousEvents) {
        this.orthologousEvents = orthologousEvents;
    }

    public List<Disease> getDiseases() {
        return diseases;
    }

    public void setDiseases(List<Disease> diseases) {
        this.diseases = diseases;
    }

    public Map<String, List<CrossReference>> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(Map<String, List<CrossReference>> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public Map<String, List<EntityReference>> getReferedEntities() {
        return referedEntities;
    }

    public void setReferedEntities(Map<String, List<EntityReference>> referedEntities) {
        this.referedEntities = referedEntities;
    }

    public EntityReference getReverseReaction() {
        return reverseReaction;
    }

    public void setReverseReaction(EntityReference reverseReaction) {
        this.reverseReaction = reverseReaction;
    }

    public List<EntityReference> getInferredFrom() {
        return inferredFrom;
    }

    public void setInferredFrom(List<EntityReference> inferredFrom) {
        this.inferredFrom = inferredFrom;
    }

    public List<EntityReference> getInferredTo() {
        return inferredTo;
    }

    public void setInferredTo(List<EntityReference> inferredTo) {
        this.inferredTo = inferredTo;
    }

    public List<GoTerm> getCompartments() {
        return compartments;
    }

    public void setCompartments(List<GoTerm> compartments) {
        this.compartments = compartments;
    }

    public List<GoTerm> getGoMolecularComponent() {
        return goMolecularComponent;
    }

    public void setGoMolecularComponent(List<GoTerm> goMolecularComponent) {
        this.goMolecularComponent = goMolecularComponent;
    }

    public GoTerm getGoBiologicalProcess() {
        return goBiologicalProcess;
    }

    public void setGoBiologicalProcess(GoTerm goBiologicalProcess) {
        this.goBiologicalProcess = goBiologicalProcess;
    }

    public List<EntityReference> getInput() {
        return input;
    }

    public void setInput(List<EntityReference> input) {
        this.input = input;
    }

    public List<EntityReference> getOutput() {
        return output;
    }

    public void setOutput(List<EntityReference> output) {
        this.output = output;
    }

    public List<EntityReference> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<EntityReference> candidates) {
        this.candidates = candidates;
    }

    public List<EntityReference> getMember() {
        return member;
    }

    public void setMember(List<EntityReference> member) {
        this.member = member;
    }

    public List<EntityReference> getRepeatedUnits() {
        return repeatedUnits;
    }

    public void setRepeatedUnits(List<EntityReference> repeatedUnits) {
        this.repeatedUnits = repeatedUnits;
    }

    public List<EntityReference> getComponents() {
        return components;
    }

    public void setComponents(List<EntityReference> components) {
        this.components = components;
    }

    public List<EntityReference> getEntityOnOtherCell() {
        return entityOnOtherCell;
    }

    public void setEntityOnOtherCell(List<EntityReference> entityOnOtherCell) {
        this.entityOnOtherCell = entityOnOtherCell;
    }

    public ReferenceEntity getReferenceEntity() {
        return referenceEntity;
    }

    public void setReferenceEntity(ReferenceEntity referenceEntity) {
        this.referenceEntity = referenceEntity;
    }

    public List<CatalystActivity> getCatalystActivities() {
        return catalystActivities;
    }

    public void setCatalystActivities(List<CatalystActivity> catalystActivities) {
        this.catalystActivities = catalystActivities;
    }

    public Regulation getRegulation() {
        return regulation;
    }

    public void setRegulation(Regulation regulation) {
        this.regulation = regulation;
    }

    public Map<String, List<Regulation>> getRegulatedEvents() {
        return regulatedEvents;
    }

    public void setRegulatedEvents(Map<String, List<Regulation>> regulatedEvents) {
        this.regulatedEvents = regulatedEvents;
    }

    public Map<String, List<Regulation>> getRegulatingEntities() {
        return regulatingEntities;
    }

    public void setRegulatingEntities(Map<String, List<Regulation>> regulatingEntities) {
        this.regulatingEntities = regulatingEntities;
    }

    public List<ModifiedResidue> getModifiedResidues() {
        return modifiedResidues;
    }

    public void setModifiedResidues(List<ModifiedResidue> modifiedResidues) {
        this.modifiedResidues = modifiedResidues;
    }
}
