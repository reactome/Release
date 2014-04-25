/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;

/**
 * This factory class is used to generate InstanceHandler for class CuratorToolToAuthorToolConverter.
 * @author guanming
 *
 */
public class InstanceHandlerFactory {
    // Preconfigured handlers
    private InstanceHandler pathwayHandler;
    private InstanceHandler reactionHandler;
    private InstanceHandler entityHandler;
    private InstanceHandler complexHandler;
    private InstanceHandler interactionHandler;
    
    private static InstanceHandlerFactory factory;
    
    public static InstanceHandlerFactory getFactory() {
        if (factory == null)
            factory = new InstanceHandlerFactory();
        return factory;
    }
    
    private InstanceHandlerFactory() {
        pathwayHandler = new PathwayInstanceHandler();
        reactionHandler = new ReactionInstanceHandler();
        complexHandler = new ComplexInstanceHandler();
        entityHandler = new EntityInstanceHandler();
        interactionHandler = new InteractionInstanceHandler();
    }
    
    public InstanceHandler getHandler(GKInstance instance) {
        SchemaClass cls = instance.getSchemClass();
        if (cls.isa(ReactomeJavaConstants.Pathway))
            return pathwayHandler;
        if (cls.isa(ReactomeJavaConstants.ReactionlikeEvent))
            return reactionHandler;
        if (cls.isa(ReactomeJavaConstants.Complex))
            return complexHandler;
        if (cls.isa(ReactomeJavaConstants.PhysicalEntity))
            return entityHandler;
        if (cls.isa(ReactomeJavaConstants.Interaction))
            return interactionHandler;
        return null;
    }
}
