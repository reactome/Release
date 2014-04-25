/*
 * Created on Mar 12, 2007
 *
 */
package org.reactome.celldesigner;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

import jp.sbi.celldesigner.plugin.PluginAction;
import jp.sbi.celldesigner.plugin.PluginModel;
import jp.sbi.celldesigner.plugin.PluginModifierSpeciesReference;
import jp.sbi.celldesigner.plugin.PluginReaction;
import jp.sbi.celldesigner.plugin.PluginSpecies;
import jp.sbi.celldesigner.plugin.PluginSpeciesAlias;
import jp.sbi.celldesigner.plugin.PluginSpeciesReference;
import jp.sbi.celldesigner.plugin.util.PluginReactionSymbolType;
import jp.sbi.celldesigner.plugin.util.PluginSpeciesSymbolType;

import org.gk.persistence.GKBReader;
import org.gk.persistence.Project;
import org.gk.render.ContainerNode;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableProtein;

public class ImportAuthorToolProjectPlugIn extends PluginAction {
    private ReactomePlugin plug;
    
    public ImportAuthorToolProjectPlugIn(ReactomePlugin plug) {
        this.plug = plug;
    }
    
    public void myActionPerformed(ActionEvent arg0) {
        try {
            Project project = loadProject();
            if (project == null)
                return;
            convert(project.getProcess());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void convert(ContainerNode pathway) {
        PluginModel model = plug.getSelectedModel();
        Map rToCMap = new HashMap();
        List comps = pathway.getComponents();
        // First for species
        for (Iterator it = comps.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof HyperEdge)
                continue;
            PluginSpecies species = convertToSpecies((Renderable)obj,
                                                     model);
            rToCMap.put(obj, species);
        }
        // For reactions
        for (Iterator it = comps.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof HyperEdge) {
                convertToReaction((HyperEdge)obj,
                                  rToCMap,
                                  model);
            }
        }
    }
    
    private PluginSpecies convertToSpecies(Renderable r,
                                           PluginModel model) {
        PluginSpecies species = null;
        if (r instanceof RenderableProtein)
            species = new PluginSpecies(PluginSpeciesSymbolType.PROTEIN_GENERIC,
                                        r.getDisplayName());
        else if (r instanceof RenderableComplex)
            species = new PluginSpecies(PluginSpeciesSymbolType.COMPLEX,
                                        r.getDisplayName());
        else if (r instanceof RenderableChemical)
            species = new PluginSpecies(PluginSpeciesSymbolType.SIMPLE_MOLECULE,
                                        r.getDisplayName());
        else
            species = new PluginSpecies(PluginSpeciesSymbolType.UNKNOWN,
                                        r.getDisplayName());
        PluginSpeciesAlias psa = species.getSpeciesAlias(0); 
        psa.setFramePosition(r.getPosition().x,
                             r.getPosition().y); 
        // How to calculate size of an alias??
        model.addSpecies(species); 
        plug.notifySBaseAdded(species); 
        return species;
    }
    
    private Project loadProject() throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        int reply = fileChooser.showOpenDialog(null);
        if (reply != JFileChooser.APPROVE_OPTION)
            return null;
        File file = fileChooser.getSelectedFile();
        GKBReader reader = new GKBReader();
        Project process = reader.open(file.getAbsolutePath());
        return process;
    }
    
    private PluginReaction convertToReaction(HyperEdge edge,
                                             Map rToCMap,
                                             PluginModel  model) {
        PluginReaction reaction = new PluginReaction();
        reaction.setReactionType(PluginReactionSymbolType.STATE_TRANSITION);
        List inputs = edge.getInputNodes();
        for (Iterator it = inputs.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            PluginSpeciesReference ref = createReference(r, rToCMap, reaction);
            reaction.addReactant(ref);
        }
        List outputs = edge.getOutputNodes();
        for (Iterator it = outputs.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            PluginSpeciesReference ref = createReference(r, rToCMap, reaction);
            reaction.addProduct(ref);
        }
        List catalysts = edge.getHelperNodes();
        for (Iterator it = catalysts.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            PluginModifierSpeciesReference ref = createModifierReference(r, 
                                                                 rToCMap, 
                                                                 reaction,
                                                                 PluginReactionSymbolType.CATALYSIS);
            reaction.addModifier(ref);
        }
        List activators = edge.getActivatorNodes();
        for (Iterator it = activators.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            // Type is not correct
            PluginModifierSpeciesReference ref = createModifierReference(r, 
                                                                         rToCMap, 
                                                                         reaction, 
                                                                         PluginReactionSymbolType.UNKNOWN_CATALYSIS);
            reaction.addModifier(ref);
        }
        List inhibitors = edge.getInhibitorNodes();
        for (Iterator it = inhibitors.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            // Type is not correct
            PluginModifierSpeciesReference ref = createModifierReference(r, 
                                                                         rToCMap, 
                                                                         reaction, 
                                                                         PluginReactionSymbolType.INHIBITION);
            reaction.addModifier(ref);            
        }
        model.addReaction(reaction);
        plug.notifySBaseAdded(reaction);
        return reaction;
    }
    
    private PluginModifierSpeciesReference createModifierReference(Renderable r,
                                                                       Map rToCMap,
                                                                       PluginReaction reaction,
                                                                       String type) {
        PluginSpecies species = (PluginSpecies) rToCMap.get(r);
        PluginSpeciesAlias alias = species.getSpeciesAlias(0);
        PluginModifierSpeciesReference ref = new PluginModifierSpeciesReference(reaction,
                                                                                alias);
        ref.setModificationType(type);
        return ref; 
    }
    
    private PluginSpeciesReference createReference(Renderable node,
                                                   Map rToCMap,
                                                   PluginReaction reaction) {
        PluginSpecies species = (PluginSpecies) rToCMap.get(node);
        PluginSpeciesAlias alias = species.getSpeciesAlias(0);
        PluginSpeciesReference ref = new PluginSpeciesReference(reaction,
                                                                alias);
        return ref;
    }
}
