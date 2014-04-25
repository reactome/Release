/*
 * Created on Dec 15, 2006
 *
 */
package org.gk.render;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import org.gk.util.AuthorToolAppletUtilities;

/**
 * This factory class is used to generate Renderable objects.
 * @author guanming
 *
 */
public class RenderableFactory {
    
    private static RenderableFactory factory;
    // Icons for different types of Renderable classes.
    private Map<Class, Icon> clsToIconMap;
    private Icon projectIcon;
    
    private RenderableFactory() {
        initIcons();
    }
    
    private void initIcons() {
        clsToIconMap = new HashMap<Class, Icon>();
        Icon icon = AuthorToolAppletUtilities.createImageIcon("Entity.gif");
        clsToIconMap.put(RenderableEntity.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Complex.gif");
        clsToIconMap.put(RenderableComplex.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Reaction.gif");
        clsToIconMap.put(RenderableReaction.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Pathway.gif");
        clsToIconMap.put(ProcessNode.class,
                         icon);
        clsToIconMap.put(RenderablePathway.class,
                         icon);
        projectIcon = AuthorToolAppletUtilities.createImageIcon("Project.gif");
        icon = AuthorToolAppletUtilities.createImageIcon("Gene.gif");
        clsToIconMap.put(RenderableGene.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Interaction.gif");
        clsToIconMap.put(RenderableInteraction.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("RNA.gif");
        clsToIconMap.put(RenderableRNA.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Protein.gif");
        clsToIconMap.put(RenderableProtein.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Chemical.gif");
        clsToIconMap.put(RenderableChemical.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Block.gif");
        clsToIconMap.put(RenderableCompartment.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("Text.gif");
        clsToIconMap.put(Note.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("SourceSink.png");
        clsToIconMap.put(SourceOrSink.class,
                         icon);
        icon = AuthorToolAppletUtilities.createImageIcon("FlowLine.gif");
        clsToIconMap.put(FlowLine.class,
                         icon);
    }
    
    public static RenderableFactory getFactory() {
        if (factory == null)
            factory = new RenderableFactory();
        return factory;
    }
    
    public Icon getIcon(Renderable renderable) {
        if (renderable instanceof Shortcut)
            renderable = (Renderable) ((Shortcut)renderable).getTarget();
        if (renderable instanceof RenderablePathway &&
            renderable.getContainer() == null) {
    		return projectIcon;
        }
    	return (Icon) clsToIconMap.get(renderable.getClass());
    }
    
    public Icon getIcon(Class rCls) {
        return (Icon) clsToIconMap.get(rCls);
    }

    public static Renderable generateReactionNode(Renderable container) {
        RenderableReaction reaction = new RenderableReaction();
        reaction.initPosition(new Point(150, 100));
        reaction.setDisplayName("Reaction");
        reaction.setDisplayName(RenderableRegistry.getRegistry().generateUniqueName(reaction));
        reaction.setContainer(container);
        reaction.setDisplayAsNode(true);
        Renderable rtn = reaction.generateReactionNode();
        rtn.setIsChanged(true);
        return rtn;
    }

    public static Renderable generateRenderable(Class type, Renderable container) {
        try {
            Renderable rtn = (Renderable) type.newInstance();
            rtn.setDisplayName(rtn.getType());
            if (container instanceof Shortcut)
                container = ((Shortcut)container).getTarget();
            rtn.setContainer(container);
            rtn.setDisplayName(RenderableRegistry.getRegistry().generateUniqueName(rtn));
            return rtn;
        }
        catch(Exception e) {
            System.err.println("RenderUtility.generateRenderable(): " + e);
            e.printStackTrace();
        }
        return null;
    }
    
}
