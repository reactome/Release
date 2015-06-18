package org.reactome.web.fireworks.profiles;

import org.reactome.web.fireworks.profiles.model.Profile;
import org.reactome.web.fireworks.util.gradient.ThreeColorGradient;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public final class FireworksProfile {

    private static final Double THRESHOLD = 0.05;
    private final Profile profile;

    ThreeColorGradient nodeEnrichment;
    ThreeColorGradient nodeExpression;

    ThreeColorGradient edgeEnrichment;
    ThreeColorGradient edgeExpression;

    FireworksProfile(Profile profile) {
        this.profile = profile;
        try {
            this.nodeEnrichment = new ThreeColorGradient(profile.getNode().getEnrichment());
            this.nodeExpression = new ThreeColorGradient(profile.getNode().getExpression());

            this.edgeEnrichment = new ThreeColorGradient(profile.getEdge().getEnrichment());
            this.edgeExpression = new ThreeColorGradient(profile.getEdge().getExpression());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*#################### NODE ####################*/
    public final String getNodeInitialColour(){
        return this.profile.getNode().getInitial();
    }
    public final String getNodeHighlightColour(){
        return this.profile.getNode().getHighlight();
    }
    public final String getNodeSelectionColour(){
        return this.profile.getNode().getSelection();
    }
    public final String getNodeFadeoutColour(){
        return this.profile.getNode().getFadeout();
    }
    public final String getNodeEnrichmentColour(double p) {
        if(p<=THRESHOLD) {
            return this.nodeEnrichment.getColor(p/THRESHOLD);
        }else{
            return this.profile.getNode().getHit();
        }
    }
    public final String getNodeExpressionColour(double p) {
        return this.nodeExpression.getColor(p);
    }
    public final String getNodeExpressionColour(double p, double expression, double min, double max) {
        if(p<=THRESHOLD) {
            return this.nodeExpression.getColor(expression, min, max);
        }else{
            return this.profile.getNode().getHit();
        }
    }


    /*#################### EDGE ####################*/
    public final String getEdgeInitialColour(){
        return this.profile.getEdge().getInitial();
    }
    public final String getEdgeHighlightColour(){
        return this.profile.getEdge().getHighlight();
    }
    public final String getEdgeSelectionColour(){
        return this.profile.getEdge().getSelection();
    }
    public final String getEdgeFadeoutColour(){
        return this.profile.getEdge().getFadeout();
    }
    public final String getEdgeEnrichmentColour(double p) {
        if(p<=THRESHOLD) {
            return this.edgeEnrichment.getColor(p/THRESHOLD);
        }else{
            return this.profile.getEdge().getHit();
        }
    }
    public final String getEdgeExpressionColour(double p, double expression, double min, double max) {
        if(p<=THRESHOLD) {
            return this.edgeExpression.getColor(expression, min, max);
        }else{
            return this.profile.getEdge().getHit();
        }
    }


    /*################# THUMBNAIL ##################*/
    public final String getThumbnailInitialColour(){
        return this.profile.getThumbnail().getInitial();
    }
    public final String getThumbnailHighlightColour(){
        return this.profile.getThumbnail().getHighlight();
    }
    public final String getThumbnailSelectionColour(){
        return this.profile.getThumbnail().getSelection();
    }

}
