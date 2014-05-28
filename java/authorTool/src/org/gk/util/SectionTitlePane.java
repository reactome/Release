/*
 * Created on Jan 22, 2007
 *
 */
package org.gk.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This customized JPanel is used as a title for a content pane. By click the right
 * arrow and label, the content pane should be toggled between closed and open.
 * @author guanming
 *
 */
public class SectionTitlePane extends JPanel implements MouseListener {
    
    private JComponent sectionPane;
    private JLabel label;
    // cached these image icons
    private ImageIcon widgetOpenIcon;
    private ImageIcon widgetOpenHovIcon;
    private ImageIcon widgetClosedIcon;
    private ImageIcon widgetClosedHovIcon;
    // A flag
    private boolean isClosed;
    
    public SectionTitlePane() {
        this("Title");
    }
    
    public SectionTitlePane(String title) {
        init();
        label.setText(title);
    }
    
    public void setTitle(String title) {
        label.setText(title);
    }
    
    public String getTitle() {
        return label.getText();
    }
    
    private void init() {
        // Initialize all icons
        widgetOpenIcon = AuthorToolAppletUtilities.createImageIcon("widget_open.gif");
        widgetOpenHovIcon = AuthorToolAppletUtilities.createImageIcon("widget_open_hov.gif");
        widgetClosedIcon = AuthorToolAppletUtilities.createImageIcon("widget_closed.gif");
        widgetClosedHovIcon = AuthorToolAppletUtilities.createImageIcon("widget_closed_hov.gif");
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        label = new JLabel();
        add(label);
        label.setIcon(widgetOpenIcon);
        Font font = getFont();
        label.setFont(font.deriveFont(Font.BOLD));
        addMouseListener(this);
        label.addMouseListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    public void mouseClicked(MouseEvent e) {
        toggleState();
    }

    public void mouseEntered(MouseEvent e) {
        if (isClosed)
            label.setIcon(widgetClosedHovIcon);
        else
            label.setIcon(widgetOpenHovIcon);
        label.setForeground(Color.BLUE);
    }
    
    public void mouseExited(MouseEvent e) {
        if (isClosed)
            label.setIcon(widgetClosedIcon);
        else
            label.setIcon(widgetOpenIcon);
        label.setForeground(getForeground());
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void setSectionPane(JComponent contentPane) {
        this.sectionPane = contentPane;
    }
    
    public JComponent getSectionPane() {
        return this.sectionPane;
    }
    
    public boolean isClosed() {
        return this.isClosed;
    }
    
    public void setIsClosed(boolean isClosed) {
        if (this.isClosed == isClosed)
            return;
        this.isClosed = isClosed;
        // Should use hov since the mouse should be in
        if (isClosed) 
            label.setIcon(widgetClosedHovIcon);
        else
            label.setIcon(widgetOpenHovIcon);
        if (sectionPane != null) {
            sectionPane.setVisible(!isClosed);
            JComponent parentComp = (JComponent) sectionPane.getParent();
            if (parentComp != null) {
                parentComp.revalidate();
                parentComp.repaint();
            }
        }
    }
    
    private void toggleState() {
        setIsClosed(!isClosed);
        firePropertyChange("isClosed", !isClosed, isClosed);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Paint old = g2.getPaint();
        int w = getWidth();
        int h = getHeight();
        Paint gradient = new GradientPaint(0, 0,
                                           getBackground(),
                                           0, h,
                                           Color.WHITE);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(old);
    }
        
}
