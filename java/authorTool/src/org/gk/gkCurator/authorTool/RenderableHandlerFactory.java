/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;

public class RenderableHandlerFactory {
    private Map typeToHandlers;
    
    private static RenderableHandlerFactory factory;
    
    private RenderableHandlerFactory() {
        initHandlers();
    }
    
    private void initHandlers() {
        typeToHandlers = new HashMap();
        RenderableHandler entityHandler = new EntityHandler();
        RenderableHandler complexHandler = new ComplexHandler();
        RenderableHandler reactionHandler = new ReactionHandler();
        RenderableHandler pathwayHandler = new PathwayHandler();
        RenderableHandler interactionHandler = new InteractionHandler();
        typeToHandlers.put(RenderableInteraction.class,
                           interactionHandler);
        typeToHandlers.put(RenderablePathway.class,
                           pathwayHandler);
        typeToHandlers.put(RenderableComplex.class,
                           complexHandler);
        typeToHandlers.put(RenderableReaction.class,
                           reactionHandler);
        typeToHandlers.put(ReactionNode.class,
                           reactionHandler);
        typeToHandlers.put(RenderableEntity.class,
                           entityHandler);
    }
    
    public static RenderableHandlerFactory getFactory() {
        if (factory == null)
            factory = new RenderableHandlerFactory();
        return factory;
    }
    
    public void setFileAdaptor(XMLFileAdaptor fileAdaptor) {
        Collection values = typeToHandlers.values();
        for (Iterator it = values.iterator(); it.hasNext();) {
            RenderableHandler handler = (RenderableHandler) it.next();
            handler.setFileAdaptor(fileAdaptor);
        }
    }
    
    public void setDBAdapptor(MySQLAdaptor dbAdaptor) {
        Collection values = typeToHandlers.values();
        for (Iterator it = values.iterator(); it.hasNext();) {
            RenderableHandler handler = (RenderableHandler) it.next();
            handler.setDbAdaptor(dbAdaptor);
        }
    }

    public RenderableHandler getHandler(Renderable r) {
        RenderableHandler handler = (RenderableHandler) typeToHandlers.get(r.getClass());
        if (handler != null)
            return handler;
        // as default
        handler = (RenderableHandler) typeToHandlers.get(RenderableEntity.class);
        return handler;
    }
    
}
