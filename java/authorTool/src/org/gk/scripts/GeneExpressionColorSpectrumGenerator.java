/*
 * Created on Feb 8, 2013
 *
 */
package org.gk.scripts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * This class is used to generate color spectrum used in the web site as a background image.
 * @author gwu
 *
 */
public class GeneExpressionColorSpectrumGenerator {
    
    public GeneExpressionColorSpectrumGenerator() {
        
    }
    
    @Test
    public void testColor() {
        System.out.println("Color: " + new Color(65280));
        System.out.println("Yellow: " + Color.yellow.toString());
        System.out.println("Blue: " + Color.BLUE.toString());
    }
    
    @Test
    public void generateSpectrum() throws Exception {
        int width = 36;
        int height = 325;
        //        height = 1200;
        BufferedImage image = new BufferedImage(width, 
                                                height, 
                                                BufferedImage.TYPE_INT_RGB);
        //        Paint color = new LinearGradientPaint(new Point(0, 0), 
        //                                              new Point(0, height), 
        //                                              new float[]{0.0f, 0.5f, 1.0f}, 
        //                                              new Color[]{Color.red, Color.green, Color.blue});
        
        Paint color = new LinearGradientPaint(new Point(0, 0), 
                                              new Point(0, height), 
                                              new float[]{0.0f, 1.0f}, 
                                              new Color[]{Color.yellow, Color.blue});
        
        //        Paint color = new LinearGradientPaint(new Point(0, 0),
        //                                              new Point(0, height),
        //                                              new float[]{0.0f, 1.0f},
        //                                              new Color[]{Color.red, Color.green});
        
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.setPaint(color);
        g2.fill(new Rectangle(0, 0, width, height));
        ImageIO.write(image, "png", new File("tmp/ColorSpectrum.png"));
    }
    
}
