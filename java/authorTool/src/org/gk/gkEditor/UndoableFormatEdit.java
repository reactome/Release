/*
 * Created on Aug 25, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.UndoableGraphEdit;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;

public class UndoableFormatEdit extends UndoableGraphEdit {
    
    private Map<Renderable, Color> oldBgMap;
    private Map<Renderable, Color> oldFgMap;
    private Map<Renderable, Color> oldLineColorMap;
    private Map<Renderable, Float> oldLineWidthMap;
    private Map<Renderable, Color> newBgMap;
    private Map<Renderable, Color> newFgMap;
    private Map<Renderable, Color> newLineColorMap;
    private Map<Renderable, Float> newLineWidthMap;
    private List<Renderable> objects;
    
    public UndoableFormatEdit(List<Renderable> objects,
                              GraphEditorPane graphPane) {
        this.graphPane = graphPane;
        this.objects = new ArrayList<Renderable>(objects);
        storeOldFormatInfo();
    }
    
    private void storeOldFormatInfo() {
        oldBgMap = new HashMap<Renderable, Color>();
        oldFgMap = new HashMap<Renderable, Color>();
        oldLineColorMap = new HashMap<Renderable, Color>();
        oldLineWidthMap = new HashMap<Renderable, Float>();
        storeFormatInfo(oldBgMap,
                        oldFgMap,
                        oldLineColorMap,
                        oldLineWidthMap);
    }
    
    private void storeFormatInfo(Map<Renderable, Color> bgMap,
                                 Map<Renderable, Color> fgMap,
                                 Map<Renderable, Color> lineColorMap,
                                 Map<Renderable, Float> lineWidthMap) {
        for (Renderable r : objects) {
            bgMap.put(r, r.getBackgroundColor());
            fgMap.put(r, r.getForegroundColor());
            lineColorMap.put(r, r.getLineColor());
            if (r instanceof HyperEdge)
                lineWidthMap.put(r, ((HyperEdge)r).getLineWidth());
        }
    }
    
    public void undo() {
        super.undo();
        if (newBgMap == null) {
            newBgMap = new HashMap<Renderable, Color>();
            newFgMap = new HashMap<Renderable, Color>();
            newLineWidthMap = new HashMap<Renderable, Float>();
            newLineColorMap = new HashMap<Renderable, Color>();
            storeFormatInfo(newBgMap,
                            newFgMap,
                            newLineColorMap,
                            newLineWidthMap);
        }
        format(oldBgMap,
               oldFgMap,
               oldLineColorMap,
               oldLineWidthMap);
    }
    
    private void format(Map<Renderable, Color> bgMap,
                        Map<Renderable, Color> fgMap,
                        Map<Renderable, Color> lineColorMap,
                        Map<Renderable, Float> lineWidthMap) {
        for (Renderable r : objects) {
            r.setBackgroundColor(bgMap.get(r));
            r.setForegroundColor(fgMap.get(r));
            r.setLineColor(lineColorMap.get(r));
            if (r instanceof HyperEdge) {
                HyperEdge edge = (HyperEdge) r;
                edge.setLineWidth(lineWidthMap.get(r));
            }
        }
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
    public void redo() {
        super.redo();
        format(newBgMap,
               newFgMap,
               newLineColorMap,
               newLineWidthMap);
    }
    
}
