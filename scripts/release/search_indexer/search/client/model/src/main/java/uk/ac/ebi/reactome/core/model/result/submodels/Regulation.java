package uk.ac.ebi.reactome.core.model.result.submodels;

/**
 * Created by flo on 7/4/14.
 */
public class Regulation {

    private EntityReference regulator;
    private EntityReference regulatedEntity;
    private String regulationType;

    public String getRegulationType() {
        return regulationType;
    }

    public void setRegulationType(String regulationType) {
        this.regulationType = regulationType;
    }

    public EntityReference getRegulator() {
        return regulator;
    }

    public void setRegulator(EntityReference regulator) {
        this.regulator = regulator;
    }

    public EntityReference getRegulatedEntity() {
        return regulatedEntity;
    }

    public void setRegulatedEntity(EntityReference regulatedEntity) {
        this.regulatedEntity = regulatedEntity;
    }
}
