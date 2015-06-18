package org.reactome.web.fireworks.menu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import org.reactome.web.fireworks.events.ProfileChangedEvent;
import org.reactome.web.fireworks.menu.submenu.ProfileMenuBar;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.profiles.model.Profile;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SettingsMenuPanel extends AbsolutePanel implements ClickHandler,
        ProfileMenuBar.ProfileColourChangedHandler,
        AboutMenuItem.AboutMenuItemSelectedHandler {

    private EventBus eventBus;

    public static final String MENU_ID = DOM.createUniqueId();
    private final SettingsMenuResources resources;
    private final PopupPanel popupPanel;

    public SettingsMenuPanel(EventBus eventBus) {
        this.eventBus = eventBus;

        //Setting the legend style
        this.resources = GWT.create(SettingsMenuResources.class);
        resources.getCSS().ensureInjected();
        getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
        setStyleName(resources.getCSS().menuPanel());

        String settings = resources.getCSS().settings();
        add(new SettingsButton("Settings", settings, this));

        this.popupPanel = new PopupPanel(true, false);
        this.popupPanel.getElement().setId(MENU_ID);
        this.popupPanel.addStyleName(resources.getCSS().dropDownMenu());
//        this.popupPanel.setAnimationEnabled(true);
        initMenu();
    }

    private void initMenu(){
        MenuBar menu = new MenuBar(true);
        menu.setAutoOpen(true); menu.setAnimationEnabled(true);
//        menu.setAppearToLeft();

        menu.addItem("Colour profiles", new ProfileMenuBar(this));
        menu.addItem(new AboutMenuItem(this.resources.aboutFireworks(), this));
        this.popupPanel.add(menu);
    }

    @Override
    public void onClick(ClickEvent event) {
        this.popupPanel.showRelativeTo(this);
    }

    @Override
    public void onAboutMenuItemSelected() {
        this.popupPanel.hide();
    }

    @Override
    public void onProfileColourChanged(Profile profile) {
        this.popupPanel.hide();
        FireworksColours.setProfile(profile);
        this.eventBus.fireEventFromSource(new ProfileChangedEvent(profile), this);
    }

    /**
     * A ClientBundle of resources used by this widget.
     */
    public interface SettingsMenuResources extends ClientBundle {
        /**
         * The styles used in this widget.
         */
        @Source(SettingsMenuPanelCSS.CSS)
        SettingsMenuPanelCSS getCSS();

        @Source("images/settings_clicked.png")
        ImageResource settingsClicked();

        @Source("images/settings_disabled.png")
        ImageResource settingsDisabled();

        @Source("images/settings_hovered.png")
        ImageResource settingsHovered();

        @Source("images/settings_normal.png")
        ImageResource settingsNormal();

        @Source("aboutFireworks.html")
        TextResource aboutFireworks();

    }

    /**
     * Styles used by this widget.
     */
    @CssResource.ImportedWithPrefix("fireworks-SettingsMenuPanel")
    public interface SettingsMenuPanelCSS extends CssResource {
        /**
         * The path to the default CSS styles used by this resource.
         */
        String CSS = "org/reactome/web/fireworks/menu/MenuPanel.css";

        String menuPanel();

        String dropDownMenu();

        String settings();
    }
}
