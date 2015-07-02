package uk.ac.ebi.reactome.core.enhancer.impl;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.submodels.CatalystActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class EventAttributeEnricher extends Enricher {

    private static final Logger logger = LoggerFactory.getLogger(EventAttributeEnricher.class);

    public void setEventAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {
        try {

            if (instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                enrichedEntry.setCatalystActivities(getCatalystActivities(instance));
                enrichedEntry.setInput(getEntityReferences(instance, ReactomeJavaConstants.input));
                enrichedEntry.setOutput(getEntityReferences(instance, ReactomeJavaConstants.output));
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
                    enrichedEntry.setReverseReaction(getEntityReference(instance, ReactomeJavaConstants.reverseReaction));

                }
            }
            if (hasValue(instance, ReactomeJavaConstants.goBiologicalProcess)){
                GKInstance goBiologicalProcess = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.goBiologicalProcess);
                enrichedEntry.setGoBiologicalProcess(getGoTerm(goBiologicalProcess));
            }
            enrichedEntry.setRegulatedEvents(getRegulations(instance, ReactomeJavaConstants.regulatedEntity));

        } catch (Exception e) {
            logger.error("Error occurred when trying to set event attributes", e);
            throw new EnricherException("Error occurred when trying to set event attributes", e);
        }
    }

    /**
     * Returns a list of catalyst activities of a instance
     * @param instance GkInstance
     * @return List of CatalystActivities
     * @throws EnricherException
     */
    private List<CatalystActivity> getCatalystActivities(GKInstance instance) throws EnricherException{
        if (hasValues(instance, ReactomeJavaConstants.catalystActivity)){
            try {
                List<CatalystActivity> catalystActivityList = new ArrayList<CatalystActivity>();
                List<?> catalystActivityInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);

                for (Object catalystActivityObject : catalystActivityInstanceList) {
                    GKInstance catalystActivityInstance = (GKInstance) catalystActivityObject;
                    CatalystActivity catalystActivity = new CatalystActivity();
                    catalystActivity.setPhysicalEntity(getEntityReference(catalystActivityInstance, ReactomeJavaConstants.physicalEntity));
                    catalystActivity.setActiveUnit(getEntityReferences(catalystActivityInstance, ReactomeJavaConstants.activeUnit));
                    if (hasValue(catalystActivityInstance, ReactomeJavaConstants.activity)) {
                        GKInstance activityInstance = (GKInstance) catalystActivityInstance.getAttributeValue(ReactomeJavaConstants.activity);
                        catalystActivity.setActivity(getGoTerm(activityInstance));
                    }
                    catalystActivityList.add(catalystActivity);
                }
                return catalystActivityList;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }
}
