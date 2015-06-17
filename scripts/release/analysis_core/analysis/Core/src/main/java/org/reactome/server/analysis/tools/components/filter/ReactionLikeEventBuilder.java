package org.reactome.server.analysis.tools.components.filter;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.mapper.EventMapper;
import org.reactome.core.model.*;
import org.reactome.server.analysis.core.model.AnalysisReaction;
import org.reactome.server.analysis.core.model.EntityPathwayReactionMap;
import org.reactome.server.analysis.core.model.PathwayNode;
import org.reactome.server.analysis.core.util.MapSet;
import org.reactome.server.analysis.tools.BuilderTool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Component
public class ReactionLikeEventBuilder {

    private EntityPathwayReactionMap entityPathwayReaction = new EntityPathwayReactionMap();

    /**
     * The approach in this method is to check all the reactionlikeEvent objects, find their locations in the
     * hierarchies data object (using the pathwayLocation map) and then find out the Physical Entities involved
     * in each one of them, so they will be assigned to the location.
     * The MapSet ensures NOT DUPLICATION either for location or entities
     *
     * @param dba database connexion adaptor
     * @param pathwayLocation mapping from pathway to locations of the reachable pathways from the hierarchy root
     */
    public void build(MySQLAdaptor dba, MapSet<Long, PathwayNode> pathwayLocation){
        //There is a factory, but we know that this is always gonna be the same mapper in every iteration, so
        //it is quicker to set it up once and use it every time
        EventMapper mapper = new EventMapper();
        mapper.setClassName(ReactomeJavaConstants.Event);

        Collection<?> instances;
        try {
            instances = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int i=0; int tot=instances.size();
        for (Object instance : instances) {
            GKInstance inst = (GKInstance) instance;
            if(BuilderTool.VERBOSE) {
                System.out.print("\rProcessing ReactionlikeEvent -> " + inst.getDBID() + " >> " + ++i + "/" + tot + "   ");
            }
            if(!inst.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)){
                continue;
            }
            ReactionlikeEvent reactionLikeEvent = DatabaseObjectFactory.getDatabaseObject(inst).loadDetails();
            if(reactionLikeEvent==null) continue;
            //referrers will contain only those that are "reachable" in the pathway location map
            //NOTE: It means that the orphan reactionLikeEvent are removed at this point
            Set<Long> referrers = this.getReferrers(inst, pathwayLocation);
            if(!referrers.isEmpty()){
                AnalysisReaction ar = new AnalysisReaction(inst.getDBID(), getStableIdentifier(inst));
                for (PhysicalEntity pe : this.getInvolvedPhysicalEntities(reactionLikeEvent)) {
                    if(pe!=null){
                        Long physicalEntityId = pe.getDbId();
                        entityPathwayReaction.add(physicalEntityId, referrers, ar);
                    }
                }
            }
        }
        if(BuilderTool.VERBOSE) {
            System.out.println("\rReactionlikeEvent processed");
        }
    }

    public EntityPathwayReactionMap getEntityPathwayReaction() {
        return this.entityPathwayReaction;
    }

    /**
     * For a given reactionlikeEvent, it checks the previously loaded inputs, outputs, regulators and
     * catalysts and retrieve a set of Physical Entities that are involved in that reaction
     *
     * @param reactionlikeEvent the reaction like event object
     * @return a set of Physical Entities involved in the reaction
     */
    private Set<PhysicalEntity> getInvolvedPhysicalEntities(ReactionlikeEvent reactionlikeEvent){
        Set<PhysicalEntity> pes = new HashSet<PhysicalEntity>();

        //INPUTS
        if(reactionlikeEvent.getInput()!=null){
            pes.addAll(reactionlikeEvent.getInput());
        }
        //OUTPUTS
        if(reactionlikeEvent.getOutput()!=null){
            pes.addAll(reactionlikeEvent.getOutput());
        }
        //REGULATORS
        List<DatabaseObject> regulators = new LinkedList<DatabaseObject>();
        if(reactionlikeEvent.getNegativeRegulators()!=null){
            regulators.addAll(reactionlikeEvent.getNegativeRegulators());
        }
        if(reactionlikeEvent.getPositiveRegulators()!=null){
            regulators.addAll(reactionlikeEvent.getPositiveRegulators());
        }
        for (DatabaseObject regulator : regulators) {
            if(regulator instanceof PhysicalEntity){
                pes.add((PhysicalEntity) regulator);
            }
        }
        //CATALYST ACTIVITIES
        if(reactionlikeEvent.getCatalystActivity()!=null) {
            for (CatalystActivity catalystActivity : reactionlikeEvent.getCatalystActivity()) {
                if (catalystActivity != null){
                    catalystActivity.load();
                    if(catalystActivity.getPhysicalEntity() != null) {
                        pes.add(catalystActivity.getPhysicalEntity());
                    }
                }
            }
        }
        //ENTITY FUNCTIONAL STATUS
        if(reactionlikeEvent.getEntityFunctionalStatus()!=null){
            for (EntityFunctionalStatus efs : reactionlikeEvent.getEntityFunctionalStatus()) {
                if(efs!=null) {
                    efs.load();
                    if (efs.getPhysicalEntity() != null) {
                        pes.add(efs.getPhysicalEntity());
                    }
                }
            }
        }

        return pes;
    }

    /**
     * For a given reactionlikeEvent, it retrieves the referrers (which can be Pathways/Subpathways) and look for the
     * locations where those referrers are in the hierarchies data structure (using the pathwaysLocation map set up
     * while construction the first one)
     *
     * @param inst the GKInstance representing the reactionlikeEvent
     * @param pathwayLocation mapping from pathway to locations of the reachable pathways from the hierarchy root
     * @return the set of locations corresponding to Pathways/Subpathways where the event is referred
     */
    private Set<Long> getReferrers(GKInstance inst, MapSet<Long, PathwayNode> pathwayLocation){
        if(!inst.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)){
            return new HashSet<Long>();
        }

        Collection<?> parents;
        try {
            parents = inst.getReferers(ReactomeJavaConstants.hasEvent);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<Long>();
        }

        Set<Long> referrers = new HashSet<Long>();
        if(parents!=null && !parents.isEmpty()){
            for (Object parent : parents) {
                try {
                    GKInstance p = (GKInstance) parent;
                    //Pathways location object is used because we do NOT want to include
                    //pathways (referrers) that are not reachable from the hierarchy root
                    Set<PathwayNode> locations = pathwayLocation.getElements(p.getDBID());
                    if(locations!=null){
                        referrers.add(p.getDBID());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return referrers;
    }

    private String getStableIdentifier(GKInstance reaction){
        try {
            GKInstance stId = (GKInstance) reaction.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            return (String) stId.getAttributeValue(ReactomeJavaConstants.identifier);
        } catch (Exception e) {
            return "";
        }
    }
}
