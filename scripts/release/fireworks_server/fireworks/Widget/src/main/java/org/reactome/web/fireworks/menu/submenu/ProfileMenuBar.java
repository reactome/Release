package org.reactome.web.fireworks.menu.submenu;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MenuItem;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.profiles.model.Profile;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ProfileMenuBar extends SubMenuBar{

    public interface ProfileColourChangedHandler {
        void onProfileColourChanged(Profile profile);
    }

    public ProfileMenuBar(final ProfileColourChangedHandler handler) {
        super(true);
        setAnimationEnabled(true);

        String selected = FireworksColours.getSelectedProfileName();
        for (final String name : FireworksColours.ProfileType.getProfiles()) {
            final MenuItem item = new MenuItem(new SafeHtmlBuilder().appendEscaped(name).toSafeHtml());

            if(name.equals(selected)){
                flagItemAsSelected(item);
            }else{
                flagItemAsNormal(item);
            }

            item.setScheduledCommand(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    if (handler != null) {
                        for (MenuItem itemTemp : getItems()) {
                            flagItemAsNormal(itemTemp);
                        }
                        flagItemAsSelected(item);

                        Profile p = FireworksColours.ProfileType.getByName(name).getProfile();
                        handler.onProfileColourChanged(p);
                    }
                }
            });
            addItem(item);
        }
    }

    private void flagItemAsSelected(MenuItem item){
        Style style = item.getElement().getStyle();
        style.setFontWeight(Style.FontWeight.BOLDER);
        style.setTextDecoration(Style.TextDecoration.UNDERLINE);
    }

    private void flagItemAsNormal(MenuItem item){
        Style style = item.getElement().getStyle();
        style.setFontWeight(Style.FontWeight.LIGHTER);
        style.setTextDecoration(Style.TextDecoration.NONE);
    }
}