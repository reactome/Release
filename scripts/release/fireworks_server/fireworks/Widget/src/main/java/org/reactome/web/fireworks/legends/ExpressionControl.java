package org.reactome.web.fireworks.legends;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.InlineLabel;
import org.reactome.web.fireworks.analysis.AnalysisType;
import org.reactome.web.fireworks.analysis.ExpressionSummary;
import org.reactome.web.fireworks.events.AnalysisPerformedEvent;
import org.reactome.web.fireworks.events.AnalysisResetEvent;
import org.reactome.web.fireworks.events.ExpressionColumnChangedEvent;
import org.reactome.web.fireworks.handlers.AnalysisPerformedHandler;
import org.reactome.web.fireworks.handlers.AnalysisResetHandler;
import org.reactome.web.fireworks.util.slider.Slider;
import org.reactome.web.fireworks.util.slider.SliderValueChangedEvent;
import org.reactome.web.fireworks.util.slider.SliderValueChangedHandler;

import java.util.List;


/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExpressionControl extends LegendPanel implements ClickHandler, SliderValueChangedHandler,
        AnalysisPerformedHandler, AnalysisResetHandler, ValueChangeHandler<Boolean> {

    private static Integer MIN_SPEED = 3000;
    private static Integer MAX_SPEED = 500;
    private Integer speed = MIN_SPEED / 2 + MAX_SPEED;

    private InlineLabel message;
    private ExpressionSummary expressionSummary;
    private int currentCol = 0;
    private Timer timer;

    private ControlButton rewindBtn;
    private ControlButton playBtn;
    private ControlButton pauseBtn;
    private ControlButton forwardBtn;
    private SpeedButton speedBtn;
    private ControlButton closeBtn;
    private Slider slider;

    public ExpressionControl(EventBus eventBus) {
        super(eventBus);

        //Setting the legend style
        LegendPanelCSS css = RESOURCES.getCSS();
        addStyleName(css.analysisControl());
        addStyleName(css.expressionControl());

        this.rewindBtn = new ControlButton("Show previous", css.rewind(), this);
        this.add(this.rewindBtn);

        this.playBtn = new ControlButton("Play", css.play(), this);
        this.add(this.playBtn);

        this.pauseBtn = new ControlButton("Pause", css.pause(), this);
        this.pauseBtn.setVisible(false);
        this.add(this.pauseBtn);

        this.forwardBtn = new ControlButton("Show next", css.forward(), this);
        this.add(this.forwardBtn);

        this.speedBtn = new SpeedButton(RESOURCES);
        this.speedBtn.setStyleName(css.speed());
        this.speedBtn.addValueChangeHandler(this);
        this.speedBtn.disable();
        this.add(this.speedBtn);

        this.slider = new Slider(100, 24, 0.5);
        this.slider.addSliderValueChangedHandler(this);
        this.slider.setVisible(false);
        this.slider.setStyleName(css.slide());
        this.add(this.slider);

        this.message = new InlineLabel();
        this.add(this.message);

        this.closeBtn = new ControlButton("Close", css.close(), this);
        this.add(this.closeBtn);

        this.initHandlers();
        this.initTimer();
        this.setVisible(false);
    }

    @Override
    public void onAnalysisPerformed(AnalysisPerformedEvent e) {
        if(e.getAnalysisType().equals(AnalysisType.EXPRESSION)) {
            this.currentCol = 0;
            this.eventBus.fireEventFromSource(new ExpressionColumnChangedEvent(this.currentCol), this);
            this.expressionSummary = e.getExpressionSummary();
            this.setName();
            this.setVisible(true);
        }else{
            this.setVisible(false);
        }
    }

    @Override
    public void onAnalysisReset() {
        if(this.isVisible()) {
            if(this.timer.isRunning()) pause();
            this.message.setText("");
            this.expressionSummary = null;
            this.setVisible(false);
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        Object source = event.getSource();

        if(source.equals(this.closeBtn))
            eventBus.fireEventFromSource(new AnalysisResetEvent(), this);
        else if(source.equals(this.rewindBtn))
            this.moveBackward();
        else if(source.equals(this.playBtn))
            this.play();
        else if(source.equals(this.pauseBtn))
            this.pause();
        else if(source.equals(this.forwardBtn))
            this.moveForward();
    }

    private void initHandlers() {
        this.eventBus.addHandler(AnalysisPerformedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
    }

    @Override
    public void onValueChange(ValueChangeEvent<Boolean> event) {
        SpeedButton source = (SpeedButton) event.getSource();
        if(source.equals(this.speedBtn)){
            this.slider.setVisible(source.isDown());
        }
    }

    @Override
    public void onSliderValueChanged(SliderValueChangedEvent event) {
        this.speed = (int) ((MAX_SPEED - MIN_SPEED) * event.getPercentage()) + MIN_SPEED;
        if(this.timer.isRunning()) {
            this.timer.cancel();
            this.timer.scheduleRepeating(this.speed);
        }
    }

    private void initTimer() {
        this.timer = new Timer() {
            @Override
            public void run() {
                moveForward();
            }
        };
    }

    private void moveBackward(){
        if(this.currentCol == 0){
            this.currentCol = this.expressionSummary.getColumnNames().size() - 1;
        }else{
            this.currentCol -= 1;
        }
        this.setName();
        this.eventBus.fireEventFromSource(new ExpressionColumnChangedEvent(this.currentCol), this);
    }

    private void moveForward(){
        if(this.currentCol == this.expressionSummary.getColumnNames().size() - 1){
            this.currentCol = 0;
        }else{
            this.currentCol += 1;
        }
        this.setName();
        this.eventBus.fireEventFromSource(new ExpressionColumnChangedEvent(this.currentCol), this);
    }

    private void pause(){
        this.rewindBtn.setEnabled(true);
        this.forwardBtn.setEnabled(true);
        this.speedBtn.setDown(false);
        this.speedBtn.disable();
        this.slider.setVisible(false);

        this.pauseBtn.setVisible(false);
        this.playBtn.setVisible(true);

        if(this.timer.isRunning()) {
            this.timer.cancel();
        }
    }

    private void play(){
        this.rewindBtn.setEnabled(false);
        this.forwardBtn.setEnabled(false);
        this.speedBtn.enable();

        this.playBtn.setVisible(false);
        this.pauseBtn.setVisible(true);

        this.moveForward();
        this.timer.scheduleRepeating(this.speed);
    }

    private void setName(){
        List<String> cols = this.expressionSummary.getColumnNames();
        String pos = (this.currentCol + 1) + "/" + cols.size();
        String name = cols.get(this.currentCol);
        this.message.setText(pos + " :: " + name);
    }
}
