/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.Shortcut;

public class ComplexHandler extends RenderableHandler {
    
    protected GKInstance convertChanged(Renderable r) throws Exception {
        Long dbId = getDbId(r);
        if (dbId == null)
            return null; // Just in case. Should not occur.
        GKInstance local = createNewWithID(ReactomeJavaConstants.Complex, dbId);
        return local;
    }
    
    public GKInstance createNew(Renderable r) throws Exception {
        GKInstance local = fileAdaptor.createNewInstance(ReactomeJavaConstants.Complex);
        return local;
    }

    protected void convertPropertiesForNew(GKInstance iComplex, 
                                           Renderable rComplex,
                                           Map rToIMap) throws Exception {
        extractComponents(iComplex, rComplex, rToIMap);
        extractNames(rComplex, iComplex);
        extractTaxon(rComplex, iComplex);
        extractLocalization(rComplex, iComplex);
        extractSummation(rComplex, iComplex);
    }
    
    protected void extractComponents(GKInstance iComplex,
                                     Renderable rComplex,
                                     Map rToIMap) throws Exception {
        // hasComponent
        java.util.List components = rComplex.getComponents();
        if (components == null || components.size() == 0) {
            iComplex.setAttributeValue(ReactomeJavaConstants.hasComponent, null);
            return ;
        }
        java.util.List hasComponent = new ArrayList(components.size());
        RenderableComplex renderableComplex = (RenderableComplex) rComplex;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable comp = (Renderable) it.next();
            if (comp instanceof Shortcut)
                comp = ((Shortcut)comp).getTarget();
            GKInstance compInstance = (GKInstance) rToIMap.get(comp);
            int stoi = renderableComplex.getStoichiometry(comp);
            if (stoi == 0)
                stoi = 1;
            for (int i = 0; i < stoi; i++)
                hasComponent.add(compInstance);
        }
        iComplex.setAttributeValue(ReactomeJavaConstants.hasComponent, hasComponent);
    }

    @Override
    public void convertPropertiesForNew(GKInstance iComplex, 
                                        Renderable rComplex) throws Exception {
        // hasComponent
        java.util.List components = rComplex.getComponents();
        if (components == null || components.size() == 0) {
            return ;
        }
        java.util.List hasComponent = new ArrayList(components.size());
        RenderableComplex renderableComplex = (RenderableComplex) rComplex;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable comp = (Renderable) it.next();
            GKInstance instance = fileAdaptor.fetchInstance(comp.getReactomeId());
            if (instance == null)
                continue;
            hasComponent.add(instance);
        }
        iComplex.setAttributeValue(ReactomeJavaConstants.hasComponent, hasComponent);
    }
    
}
