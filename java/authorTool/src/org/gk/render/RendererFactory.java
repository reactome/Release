/*
 * Created on Dec 12, 2006
 *
 */
package org.gk.render;

import java.util.HashMap;
import java.util.Map;



/**
 * This class is refactored from PathwayEditor, and used to help renderering and editing
 * for in PathwayEditor.
 * @author guanming
 *
 */
public class RendererFactory {
    // Cached renderer
    private Map<Class<? extends Renderable>, Renderer> clsToRenderer;
    // For editing display name. This editor can be used for all kinds of node.
    private Editor nodeEditor;
    // Singleton
    private static RendererFactory factory;
    
    public static RendererFactory getFactory() {
        if (factory == null)
            factory = new RendererFactory();
        return factory;
    }
    
    RendererFactory() {
        init();
    }
    
    private void init() {
        clsToRenderer = new HashMap<Class<? extends Renderable>, Renderer>();
        Renderer entityRenderer = new DefaultEntityRenderer();
        clsToRenderer.put(RenderableEntity.class,
                          entityRenderer);
        Renderer reactionRenderer = new DefaultReactionRenderer();
        clsToRenderer.put(RenderableReaction.class,
                          reactionRenderer);
        Renderer complexRenderer = new DefaultComplexRenderer();
        clsToRenderer.put(RenderableComplex.class,
                          complexRenderer);
        Renderer pathwayRenderer = new DefaultPathwayRenderer();
        clsToRenderer.put(RenderablePathway.class,
                          pathwayRenderer);
        Renderer reactionNodeRenderer = new DefaultReactionNodeRenderer();
        clsToRenderer.put(ReactionNode.class,
                          reactionNodeRenderer);
        Renderer flowLineRenderer = new DefaultFlowLineRenderer();
        clsToRenderer.put(FlowLine.class,
                          flowLineRenderer);
        clsToRenderer.put(RenderableInteraction.class,
                          flowLineRenderer);
        Renderer setAndMemberLinkRenderer = new DefaultEntitySetAndMemberLinkRenderer();
        clsToRenderer.put(EntitySetAndMemberLink.class,
                          setAndMemberLinkRenderer);
        DefaultEntitySetAndMemberLinkRenderer setAndSetlinkRenderer = new DefaultEntitySetAndMemberLinkRenderer();
        setAndSetlinkRenderer.setHideOutput(true);
        setAndSetlinkRenderer.setDashPattern(new float[]{10.0f, 20.0f});
        clsToRenderer.put(EntitySetAndEntitySetLink.class,
                          setAndSetlinkRenderer);
        Renderer geneRenderer = new DefaultGeneRenderer();
        clsToRenderer.put(RenderableGene.class,
                          geneRenderer);
        Renderer proteinRenderer = new DefaultProteinRenderer();
        clsToRenderer.put(RenderableProtein.class,
                          proteinRenderer);
        Renderer entitySetRenderer = new DefaultEntitySetRenderer();
        clsToRenderer.put(RenderableEntitySet.class,
                          entitySetRenderer);
        Renderer renderer = new DefaultChemicalRenderer();
        clsToRenderer.put(RenderableChemical.class,
                          renderer);
        renderer = new DefaultRNARenderer();
        clsToRenderer.put(RenderableRNA.class,
                          renderer);
        renderer = new DefaultCompartmentRenderer();
        clsToRenderer.put(RenderableCompartment.class,
                          renderer);
        renderer = new DefaultNoteRenderer();
        clsToRenderer.put(Note.class,
                          renderer);
        renderer = new SourceOrSinkRenderer();
        clsToRenderer.put(SourceOrSink.class,
                          renderer);
        renderer = new DefaultProcessNodeRenderer();
        clsToRenderer.put(ProcessNode.class,
                          renderer);
        nodeEditor = new DefaultNodeEditor();
    }
    
    public Renderer getRenderer(Renderable r) {
        Renderer renderer = clsToRenderer.get(r.getClass());
        return renderer;
    }
    
    public Renderer getRenderer(Class<? extends Renderable> cls) {
        return clsToRenderer.get(cls);
    }
    
    public Editor getEditor(Renderable r) {
        return nodeEditor;
    }
    
}
