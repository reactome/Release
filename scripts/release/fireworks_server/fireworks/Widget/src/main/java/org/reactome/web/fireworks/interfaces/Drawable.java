package org.reactome.web.fireworks.interfaces;

import com.google.gwt.canvas.dom.client.Context2d;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface Drawable {

    public void draw(Context2d ctx);

    public void drawText(Context2d ctx, double fontSize, double space, boolean selected);

    public void drawThumbnail(Context2d ctx, double factor);

    public void highlight(Context2d ctx, double auraSize);
}
