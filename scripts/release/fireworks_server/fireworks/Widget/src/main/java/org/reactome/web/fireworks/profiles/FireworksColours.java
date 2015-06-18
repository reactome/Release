package org.reactome.web.fireworks.profiles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Cookies;
import org.reactome.web.fireworks.profiles.factory.ProfileFactory;
import org.reactome.web.fireworks.profiles.factory.ProfileModelException;
import org.reactome.web.fireworks.profiles.model.Profile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class FireworksColours {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String PROFILE_COOKIE = "pathwaybrowser_fireworks_colour";
    public static FireworksProfile PROFILE;

    /**
     * When Fireworks is loaded, we first try to load the profile indicated
     * in the cookie. If there is not match then we load the default one.
     */
    static {
        String profileName = Cookies.getCookie(PROFILE_COOKIE);
        ProfileType type = ProfileType.getByName(profileName);
        setProfile(type.getProfile());
    }

    public static void setProfile(Profile profile){
        PROFILE = new FireworksProfile(profile);

        //The strategy is to remove the cookie when the standard is selected so in case
        //we decide to change the standard profile in the future, that will propagate
        //automatically for those who have not changed to a different profile
        if(ProfileType.getStandard().getProfile().equals(profile)){
            Cookies.removeCookie(PROFILE_COOKIE);
        }else {
            Date expires = new Date();
            Long nowLong = expires.getTime();
            nowLong = nowLong + (1000 * 60 * 60 * 24 * 365L); //One year time
            expires.setTime(nowLong);
            Cookies.setCookie(PROFILE_COOKIE, profile.getName(), expires);
        }
    }

    public static String getSelectedProfileName(){
        return Cookies.getCookie(PROFILE_COOKIE);
    }

    /**
     * To add a profile first please add the ProfileSource interface
     * and then add the corresponding entry in this enumeration.
     */
    @SuppressWarnings("UnusedDeclaration")
    public enum ProfileType {
        PROFILE_01(ProfileSource.SOURCE.profile01()),
        PROFILE_02(ProfileSource.SOURCE.profile02()),
        PROFILE_03(ProfileSource.SOURCE.profile03()),
        PROFILE_04(ProfileSource.SOURCE.profile04());

        Profile profile;

        ProfileType(TextResource resource) {
            try {
                profile = ProfileFactory.getModelObject(Profile.class, resource.getText());
            } catch (ProfileModelException e) {
                GWT.log(e.getMessage());
                profile = null;
            }
        }

        public Profile getProfile() {
            return profile;
        }

        public static List<String> getProfiles() {
            List<String> rtn = new ArrayList<String>();
            for (ProfileType value : values()) {
                rtn.add(value.profile.getName());
            }
            return rtn;
        }

        public static ProfileType getByName(String name){
            for (ProfileType value : values()) {
                if(value.profile.getName().equals(name)){
                    return value;
                }
            }
            return getStandard();
        }

        public static ProfileType getStandard(){
            return PROFILE_01;
        }
    }

    interface ProfileSource extends ClientBundle {

        static final ProfileSource SOURCE = GWT.create(ProfileSource.class);

        @Source("profile_01.json")
        TextResource profile01();

        @Source("profile_02.json")
        TextResource profile02();

        @Source("profile_03.json")
        TextResource profile03();

        @Source("profile_04.json")
        TextResource profile04();
    }
}