/*
 * Created on Oct 30, 2006
 *
 */
package org.gk.util;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.ImageIcon;
import javax.swing.JApplet;

/**
 * This class is dedicated for the author tool applets.
 * @author guanming
 *
 */
public class AuthorToolAppletUtilities {
    public static String REACTOME_INSTANCE_URL = "http://brie8.cshl.edu/cgi-bin/eventbrowser?DB=gk_central&ID=";
    public static final String REACTOME_BROWSER_NAME = "reactome";
    public static final String PUBMED_BROWSER_NAME = "pubmed";
    private static final String IMAGE_DIR = "images";
    private static final String RESOURCE_DIR = "resources";
    private static final String DOCS_DIR = "docs";
    private static String SCHEMA_FILE_NAME = "schema";
    // A flag 
    public static boolean isInApplet;
    private static JApplet applet;
    
    /**
     * Set the file name for the local schema. This file should be in the local
     * resources folder. Otherwise, it will not be loaded.
     * @param fileName
     */
    public static void setSchemaFileName(String fileName) {
        SCHEMA_FILE_NAME = fileName;
    }
    
    public static String getSchemaFileName() {
        return SCHEMA_FILE_NAME;
    }
    
    public static ImageIcon createImageIcon(String file) {
        // Have to use "/" instead of File.separator. Otherwise, it cannot work under windows
        URL url = AuthorToolAppletUtilities.class.getResource(IMAGE_DIR + "/" + file);
        if (url == null) {
            //System.err.println("Cannot generate a url: " + path);
            return createImageIconFromFile(file);
        }
        return new ImageIcon(url);
    }
    
    public static URL getCodeBase() {
        return applet.getCodeBase();
    }
    
    public static void setApplet(JApplet a) {
        applet = a;
    }
    
    public static void displayURL(String urlName, String browserName) {
        if (applet == null) {
            System.err.println("Cannot launch an url: applet has not specified!");
            return;
        }
        try {
            applet.getAppletContext().showDocument(new URL(urlName), 
                                                   browserName);
        }
        catch(MalformedURLException e) {
            applet.showStatus("Cannot open url: " + e.getMessage());
        }
    }
    
    private static void displayHelpForWebStart(String fileName) throws Exception {
        BasicService service = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
        URL codebase = service.getCodeBase();
        String helpUrl = codebase + DOCS_DIR + "/" + fileName;
        service.showDocument(new URL(helpUrl));
    }
    
    public static void displayHelp(String fileName,
                                   Component toolPane) throws Exception {
        URL url = AuthorToolAppletUtilities.getDocumentResource(fileName);
        if (url == null) {
            // Maybe in a web start environment
            AuthorToolAppletUtilities.displayHelpForWebStart(fileName);
        }
        else {
            BrowserLauncher.displayURL(url.toString(), toolPane);
        }
    }
    
    /**
     * This will make the applet runner happy during developing.
     * @param path
     * @return
     */
    private static ImageIcon createImageIconFromFile(String fileName) {
        return new ImageIcon(IMAGE_DIR + File.separator + fileName);
    }
    
    public static InputStream getResourceAsStream(String fileName) throws IOException {
        // Don't use File.separator in a jar file. It cannot work under windows
        InputStream is = AuthorToolAppletUtilities.class.getResourceAsStream(RESOURCE_DIR + "/" + fileName);
        if (is == null)
            is = new FileInputStream(RESOURCE_DIR + File.separator + fileName);
        return is;
    }
    
    public static URL getDocumentResource(String fileName) throws IOException {
        // Don't use File.seperator. It cannot work for jar under windows.
        URL url = AuthorToolAppletUtilities.class.getResource(DOCS_DIR + "/" + fileName);
        if (url == null) {
            File file = new File(DOCS_DIR + File.separator + fileName);
            if (file.exists())
                url = file.toURL();
        }
        return url;
    }
    
    public static Properties loadProperties(String fileName) throws IOException {
        Properties prop = new Properties();
        InputStream is = getResourceAsStream(fileName);
        prop.load(is);
        // Have to decode dbPwd if any
        String dbPwd = prop.getProperty("dbPwd");
        if (dbPwd != null) {
            dbPwd = decrypt(dbPwd);
            prop.setProperty("dbPwd", dbPwd);
        }
        return prop;
    }
    
    // There are three places a schema object can be found:
    // local resources folder, .reactome folder, and JWS 
    // cached resources folder that is in a jar. So the
    // schema class will be searched according the above order.
    /**
     *  There are three places a schema object can be found:
     *  local resources folder, .reactome folder, and JWS 
     *  cached resources folder that is in a jar. So the
     *  schema class will be searched according the above order.
     *  @return a loaded object. The client should cast it to a schema.
     */
    public static Object fetchLocalSchema() throws Exception {
        InputStream io = null;
        File file = new File(RESOURCE_DIR + File.separator + SCHEMA_FILE_NAME);
        if (file.exists())
            io = new FileInputStream(file);
        if (io == null) {
            file = new File(GKApplicationUtilities.getReactomeDir() + File.separator + SCHEMA_FILE_NAME);
            if (file.exists())
                io = new FileInputStream(file);
        }
        if (io == null) 
            io = AuthorToolAppletUtilities.class.getResourceAsStream(RESOURCE_DIR + "/" + SCHEMA_FILE_NAME);
        if (io == null)
            throw new IllegalStateException("Cannot find a local schema object!");
        ObjectInputStream ois = new ObjectInputStream(io);
        Object schema = ois.readObject();
        // Remember to close these two streams
        ois.close();
        io.close();
        return schema;
    }
    
    /**
     * The passed schema object will be tried to save into resources folder, 
     * if there is no resource folder as in JWS, .reactome will be used.
     * @param schema
     * @throws Exception
     */
    public static void saveLocalSchema(Object schema) throws IOException {
        // Check if resources folder existing
        File resourcesFilder = new File(RESOURCE_DIR);
        File schemaFile = null;
        if (resourcesFilder.exists()) {
            // Back up first
            schemaFile = new File(RESOURCE_DIR + File.separator + SCHEMA_FILE_NAME);
            if (schemaFile.exists()) {
                GKApplicationUtilities.copy(schemaFile, new File(RESOURCE_DIR + File.separator + "schema.bak"));
            }
        }
        else {
            // Try to save to .reactome. No need to backup since this folder is
            // hidden from the user.
            schemaFile = new File(GKApplicationUtilities.getReactomeDir() + File.separator + SCHEMA_FILE_NAME);
        }
        FileOutputStream fos = new FileOutputStream(schemaFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(schema);
        oos.close();
        fos.close();
    }
    
    /**
     * This method is copied from {@link GKApplicationUtilities}.
     * @param value
     * @return
     */
    public static String decrypt(String value) {
        java.util.List list = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(value, ", []");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            list.add(new Integer(token));
        }
        char[] array = new char[list.size()];
        int tmp;
        for (int i = 0; i < list.size(); i++) {
            tmp = ((Integer) list.get(i)).intValue();
            tmp -= i * i;
            array[i] = (char) tmp;
        }
        return new String(array);
    }
    
}
