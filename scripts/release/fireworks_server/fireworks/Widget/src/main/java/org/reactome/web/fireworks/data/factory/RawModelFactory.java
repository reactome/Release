package org.reactome.web.fireworks.data.factory;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import org.reactome.web.fireworks.data.RawEdge;
import org.reactome.web.fireworks.data.RawGraph;
import org.reactome.web.fireworks.data.RawNode;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class RawModelFactory {

    @SuppressWarnings("UnusedDeclaration")
    interface ModelAutoBeanFactory extends AutoBeanFactory {
        AutoBean<RawNode> node();
        AutoBean<RawEdge> edge();
        AutoBean<RawGraph> graph();
    }

    public static <T> T getModelObject(Class<T> cls, String json) throws RawModelException {
        try{
            AutoBeanFactory factory = GWT.create(ModelAutoBeanFactory.class);
            AutoBean<T> bean = AutoBeanCodex.decode(factory, cls, json);
            return bean.as();
        }catch (Throwable e){
            throw new RawModelException("Error mapping json string for [" + cls + "]: " + json, e);
        }
    }
}
