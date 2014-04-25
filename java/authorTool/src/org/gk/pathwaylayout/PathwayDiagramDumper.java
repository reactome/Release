/*
 * Created on Oct 7, 2010
 *
 */
package org.gk.pathwaylayout;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.Node;
import org.gk.render.RenderablePathway;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.SwingImageCreator;
import org.junit.Test;

/**
 * This class is used to create a list of dump files for pathway diagrams for specific database.
 * @author wgm
 *
 */
public class PathwayDiagramDumper {
    
    public PathwayDiagramDumper() {
    }
 
    /**
     * The entry point to output diagrams.
     * @param dba
     * @param outputDir
     * @throws Exception
     */
    public void dumpDiagrams(MySQLAdaptor dba, 
                             File outputDir) throws Exception {
        // Make sure if these static variable values are used
        Node.setWidthRatioOfBoundsToText(1.0d);
        Node.setHeightRatioOfBoundsToText(1.0d);
        
        outputDir = ensureDir(outputDir);
        Collection<?> diagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        PathwayEditor editor = new PathwayEditor();
        editor.setHidePrivateNote(true);
        DiagramGKBReader reader = new DiagramGKBReader();
        // Used for some preprocessing
        PredictedPathwayDiagramGeneratorFromDB helper = new PredictedPathwayDiagramGeneratorFromDB();
        PathwayDiagramGeneratorViaAT generator = new PathwayDiagramGeneratorViaAT();
        File pdfDir = new File(outputDir, "PDF");
        File pngDir = new File(outputDir, "PNG");
        for (Iterator<?> it = diagrams.iterator(); it.hasNext();) {
            GKInstance diagramInst = (GKInstance) it.next();
//            if (!diagramInst.getDisplayName().equals("Diagram of Abnormal metabolism in phenylketonuria"))
//                continue;
//            if (!diagramInst.getDBID().equals(500219L))
//                continue;
//            // Test code
//            if (!diagramInst.getDisplayName().equals("Diagram of Mitotic M-M/G1 phases"))
//                continue;
            if (!shouldExport(diagramInst))
                continue;
            RenderablePathway pathway = reader.openDiagram(diagramInst);
            if (pathway.getComponents() == null || pathway.getComponents().size() == 0)
                continue; // No need to output
            helper.fineTuneDiagram(pathway);
            editor.setRenderable(pathway);
            editor.setHidePrivateNote(true);
            // Just to make the tightNodes() work, have to do an extra paint
            // to make textBounds correct
            generator.paintOnImage(editor);
            editor.tightNodes(true);
            // Output PDF
            String fileName = diagramInst.getDisplayName();
            fileName = fileName.replaceAll("(\\\\|/)", "-");
            // Make sure the file name is not too long
            if (fileName.length() > 255 - 4) // 4 is for .png or .pdf
                fileName = fileName.substring(0, 255 - 4);
            // Note: It seems there is a bug in the PDF exporter to set correct FontRenderContext.
            // Have to call PNG export first to make some rectangles correct.
            File pngFileName = new File(pngDir, fileName + ".png");
            BufferedImage image = SwingImageCreator.createImage(editor);
            ImageIO.write(image, "png", pngFileName);
            File pdfFileName = new File(pdfDir, fileName + ".pdf");
            SwingImageCreator.exportImageInPDF(editor, pdfFileName);
        }
    }
    
    @Test
    public void testDumpDiagrams() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver43", 
                                            "root",
                                            "macmysql01");
        File file = new File("tmp");
        dumpDiagrams(dba, file);
    }
    
    /**
     * In this implementation, only human related diagrams should be exported.
     * @param diagram
     * @return
     * @throws Exception
     */
    private boolean shouldExport(GKInstance diagram) throws Exception {
       GKInstance pathway = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
       if (pathway == null)
           return false;
       List<?> values = pathway.getAttributeValuesList(ReactomeJavaConstants.species);
       if (values == null || values.size() == 0)
           return true; // Treat the default as human
       // Check if human is a species
       for (Iterator<?> it = values.iterator(); it.hasNext();) {
           GKInstance species = (GKInstance) it.next();
           if (species.getDBID().equals(GKApplicationUtilities.HOMO_SAPIENS_DB_ID))
               return true;
       }
       return false;
    }
    
    private File ensureDir(File outputDir) throws IOException {
        if (outputDir == null) {
            outputDir = new File("diagram_output");
        }
        if (outputDir.exists()) {
            if (outputDir.isDirectory()) {
                // Ensure two sub folders existing
                File pdfDir = new File(outputDir, "PDF");
                if (pdfDir.exists()) 
                    pdfDir.delete();
                pdfDir.mkdir();
                File pngDir = new File(outputDir, "PNG");
                if (pngDir.exists())
                    pngDir.delete();
                pngDir.mkdir();
                return outputDir;
            }
            // This is a file
            outputDir.mkdir();
            // Create two sub-folders
            File pdfDir = new File(outputDir, "PDF");
            pdfDir.mkdir();
            File pngDir = new File(outputDir, "PNG");
            pngDir.mkdir();
            return outputDir;
        }
        else {
            outputDir.mkdir();
            // Create two sub-folders
            File pdfDir = new File(outputDir, "PDF");
            pdfDir.mkdir();
            File pngDir = new File(outputDir, "PNG");
            pngDir.mkdir();
            return outputDir;
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 5) {
            String message = "Usage java -Xmx1024m org.gk.pathwaylayout.PathwayDiagramDumper dbHost dbName dbUser dbPwd dbPort (output_dir)\n" +
            		         "Note: the output_dir is optional. Two sub-directories will be created: one for PDF files, and another for PNG files.";
            System.err.println(message);
            return;
        }
        try {
            MySQLAdaptor dba = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3],
                                                new Integer(args[4]));
            File dir = null;
            if (args.length > 5)
                dir = new File(args[5]);
            PathwayDiagramDumper dumper = new PathwayDiagramDumper();
            dumper.dumpDiagrams(dba, dir);
        }
        catch(Exception e) {
            
        }
    }
    
}
