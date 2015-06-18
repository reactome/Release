package uk.ac.ebi.reactome.core.model.result.submodels;

import java.util.List;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class CatalystActivity {
    private GoTerm activity;
    private EntityReference physicalEntity;
    private List<EntityReference> activeUnit;

    public GoTerm getActivity() {
        return activity;
    }

    public void setActivity(GoTerm activity) {
        this.activity = activity;
    }

    public EntityReference getPhysicalEntity() {
        return physicalEntity;
    }

    public void setPhysicalEntity(EntityReference physicalEntity) {
        this.physicalEntity = physicalEntity;
    }

    public List<EntityReference> getActiveUnit() {
        return activeUnit;
    }

    public void setActiveUnit(List<EntityReference> activeUnit) {
        this.activeUnit = activeUnit;
    }
}
