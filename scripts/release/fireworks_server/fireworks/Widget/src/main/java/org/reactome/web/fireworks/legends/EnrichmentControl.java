package org.reactome.web.fireworks.legends;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.InlineLabel;
import org.reactome.web.fireworks.events.AnalysisPerformedEvent;
import org.reactome.web.fireworks.events.AnalysisResetEvent;
import org.reactome.web.fireworks.handlers.AnalysisPerformedHandler;
import org.reactome.web.fireworks.handlers.AnalysisResetHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class EnrichmentControl extends LegendPanel implements ClickHandler,
        AnalysisPerformedHandler, AnalysisResetHandler {

    private InlineLabel message;
    private ControlButton closeBtn;

    public EnrichmentControl(final EventBus eventBus) {
        super(eventBus);

        LegendPanelCSS css = RESOURCES.getCSS();
        //Setting the legend style
        addStyleName(css.analysisControl());
        addStyleName(css.enrichmentControl());

        this.message = new InlineLabel();
        this.add(this.message);

        this.closeBtn = new ControlButton("Close", css.close(), this);
        this.add(this.closeBtn );

        this.initHandlers();
        this.setVisible(false);
    }

    @Override
    public void onAnalysisPerformed(AnalysisPerformedEvent e){
        switch (e.getAnalysisType()){
            case OVERREPRESENTATION:
            case SPECIES_COMPARISON:
                String message = e.getAnalysisType().name().replaceAll("_", " ");
                this.message.setText(message.toUpperCase());
                this.setVisible(true);
                break;
            default:
                this.setVisible(false);
        }
    }

    @Override
    public void onAnalysisReset() {
        if(this.isVisible()) {
            this.message.setText("");
            this.setVisible(false);
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        if(event.getSource().equals(this.closeBtn)){
            eventBus.fireEventFromSource(new AnalysisResetEvent(), this);
        }
    }

    private void initHandlers() {
        this.eventBus.addHandler(AnalysisPerformedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
    }

}
