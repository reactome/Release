package org.reactome.web.fireworks.profiles.factory;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import org.reactome.web.fireworks.profiles.model.Profile;
import org.reactome.web.fireworks.profiles.model.ProfileColour;
import org.reactome.web.fireworks.profiles.model.ProfileGradient;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ProfileFactory {

    @SuppressWarnings("UnusedDeclaration")
    interface ModelAutoBeanFactory extends AutoBeanFactory {
        AutoBean<Profile> profile();
        AutoBean<ProfileColour> profileColour();
        AutoBean<ProfileGradient> profileGradient();
    }

    public static <T> T getModelObject(Class<T> cls, String json) throws ProfileModelException {
        try{
            AutoBeanFactory factory = GWT.create(ModelAutoBeanFactory.class);
            AutoBean<T> bean = AutoBeanCodex.decode(factory, cls, json);
            return bean.as();
        }catch (Throwable e){
            throw new ProfileModelException("Error mapping json string for [" + cls + "]: " + json, e);
        }
    }
}
