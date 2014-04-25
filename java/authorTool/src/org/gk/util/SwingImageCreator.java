/*
 * Created on Jan 16, 2007
 *
 */
package org.gk.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;


/**
 * This class is copied from http://today.java.net/pub/a/today/2006/04/20/bringing-swing-to-the-web.html.
 * It is placed in the top-level package since I have to scale PathwayEditor drawing back to the original
 * in cases there are some zooming is done.
 */
public class SwingImageCreator {
    /**
     * Creates a buffered image of type TYPE_INT_RGB 
     * from the supplied component. This method will 
     * use the preferred size of the component as the 
     * image's size.
     * @param component the component to draw
     * @return an image of the component
     */
    public static BufferedImage createImage(JComponent component){
       return createImage(component, BufferedImage.TYPE_INT_RGB);
    }
    
    /**
     * Creates a buffered image (of the specified type) 
     * from the supplied component. This method will use 
     * the preferred size of the component as the image's size
     * @param component the component to draw
     * @param imageType the type of buffered image to draw
     * 
     * @return an image of the component
     */
    public static BufferedImage createImage(JComponent component, 
                                            int imageType) {
//        PathwayEditor pathwayEditor = (PathwayEditor) component;
//        double scaleX = pathwayEditor.getScaleX();
//        double scaleY = pathwayEditor.getScaleY();
//        pathwayEditor.setScale(1.0, 1.0);
        Dimension componentSize = component.getPreferredSize();
        component.setSize(componentSize); //Make sure these are the same!!!
        BufferedImage img = new BufferedImage(componentSize.width,
                                              componentSize.height,
                                              imageType);
        Graphics2D grap = img.createGraphics();
        grap.setFont(component.getFont());
        // Need to set clip with the whole size so that everything can be drawn
        Rectangle clip = new Rectangle(componentSize);
        grap.setClip(clip);
        grap.fillRect(0,0,img.getWidth(),img.getHeight());
        component.paint(grap);
//        pathwayEditor.setScale(scaleX, scaleY);
        return img;
    }
    
    /**
     * Export anything in the component. Note: the scale is not done here.
     * @param comp
     * @param fileChooser
     * @throws IOException
     */
    public static void exportImage(ZoomableJPanel comp,
                                   JFileChooser fileChooser) throws IOException {
        FileFilter pngFilter = new ImageFileFilter(".png", "PNG Image File (*.png)");
        FileFilter jpgFilter = new ImageFileFilter(".jpg", "JPEG Image File (*.jpg, *.jpeg)");
        FileFilter pdfFilter = new ImageFileFilter(".pdf", "PDF File (*.pdf)");
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(jpgFilter);
        fileChooser.setFileFilter(pdfFilter); 
        File selectedFile = GKApplicationUtilities.chooseSaveFile(fileChooser, 
                                                                  comp);
        if (selectedFile == null)
            return;
        FileFilter filter = fileChooser.getFileFilter();
        // Something special
        if (filter == pdfFilter) {
            exportImageInPDF(comp, selectedFile);
            return;
        }
        // Have to figure format name
        String formatName = null;
        if (filter == pngFilter)
            formatName = "png";
        else if (filter == jpgFilter)
            formatName = "jpg";
        else
            formatName = guessFormatFromFileName(selectedFile);
        exportImage(comp, selectedFile, formatName);
    }
    
    public static void exportImageInPDF(JComponent comp,
                                        File file) throws IOException {
        Dimension size = comp.getPreferredSize();
        com.lowagie.text.Rectangle pageSize = new com.lowagie.text.Rectangle(size.width,
                                                                             size.height);
        try {
            Document document = new Document(pageSize);
            FileOutputStream fos = new FileOutputStream(file);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            Graphics2D g = cb.createGraphics(pageSize.getWidth(), pageSize.getHeight(), new DefaultFontMapper());
            g.setFont(comp.getFont());
            comp.paint(g);
            g.dispose();
            if (document != null) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        catch (DocumentException exp)
        {
            throw new IOException(exp.getMessage());
        }
    }
    
    /**
     * Export a buffered image to an external file. 
     * @param image
     * @param fileChooser
     * @param parentComp
     * @throws IOException
     */
    public static void exportImage(BufferedImage image,
                                   JFileChooser fileChooser,
                                   Component parentComp) throws IOException {
        FileFilter pngFilter = new ImageFileFilter(".png", "PNG Image File (*.png)");
        FileFilter jpgFilter = new ImageFileFilter(".jpg", "JPEG Image File (*.jpg, *.jpeg)");
        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(jpgFilter);
        fileChooser.setFileFilter(pngFilter); 
        File selectedFile = GKApplicationUtilities.chooseSaveFile(fileChooser, 
                                                                  parentComp);
        if (selectedFile == null)
            return;
        FileFilter filter = fileChooser.getFileFilter();
        // Have to figure format name
        String formatName = null;
        if (filter == pngFilter)
            formatName = "png";
        else if (filter == jpgFilter)
            formatName = "jpg";
        else
            formatName = guessFormatFromFileName(selectedFile);
        ImageIO.write(image, formatName, selectedFile);
    }

    /**
     * Export an image from a JComponent.
     * @param comp
     * @param selectedFile
     * @param formatName
     * @throws IOException
     */
    private static void exportImage(ZoomableJPanel comp, 
                                    File selectedFile,
                                    String formatName) throws IOException {
        double oldScaleX = comp.getScaleX();
        double oldScaleY = comp.getScaleY();
        comp.setScale(1.0d, 1.0d);
        BufferedImage image = createImage(comp);
        comp.setScale(oldScaleX,
                      oldScaleY);
        ImageIO.write(image, formatName, selectedFile);
    }
    
    private static String guessFormatFromFileName(File file) {
        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            String ext = fileName.substring(index + 1);
            if (ext.equalsIgnoreCase("png"))
                return "png";
            if (ext.equalsIgnoreCase("jpg") ||
                ext.equalsIgnoreCase("jpeg"))
                return "jpg";
        }
        return "png"; // as default
    }
    
    /**
     * Extends GKFileFilter so that extenstion name can be used.
     * @author guanming
     *
     */
    private static class ImageFileFilter extends GKFileFilter {
        private String extName;
        private String desc;
        
        public ImageFileFilter(String ext,
                             String desc) {
            this.extName = ext;
            this.desc = desc;
        }
        
        public boolean accept(File file) {
            if (file.isDirectory())
                return true;
            String fileName = file.getName();
            int index = fileName.lastIndexOf(".");
            if (index == -1)
                return false; // No ext not accepted
            String ext = fileName.substring(index);
            if (extName.equalsIgnoreCase(ext))
                return true;
            if (extName.equals(".jpg") &&
                ".JPEG".equalsIgnoreCase(ext))
                return true;
            return false;
        }
        
        public String getDescription() {
            return desc;
        }
        
        public String getExtName() {
            return extName;
        }
    }
 }
