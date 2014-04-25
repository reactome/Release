/*
 * Created on May 3, 2004
 */
package launcher;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gk.util.BrowserLauncher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A class used to launch the application.
 * @author wugm
 */
public class Launcher {
    // The update file
    private final String UPDATE_FILE_NAME = "resources" + File.separator + "update.xml";
    // The launched application
    private Launchable app;
    private String appClsName;
    // cache remote resource for updating
    private Map remoteResources;
    // uri
    private String uri;
    // The singleton instance
    private static Launcher launcher;
    
	private Launcher() {
	}
	
	public static Launcher getLauncher() {
	    if (launcher == null)
	        launcher = new Launcher();
	    return launcher;
	}
	
	public void launch(String appClsName) {
		try {
		    this.appClsName = appClsName;
			Class appCls = Class.forName(appClsName);
			app = (Launchable) appCls.newInstance();
             app.addUpdateSchemaAction(createUpdateSchemaAction());
             // This method should be called after the above line to add correct 
             // menu separator
			app.addCheckUpdateAction(createCheckUpdateAction());
			app.launch();
			// Create a new thread to check if there are any new update available.
		    Thread t = new Thread() {
			    public void run() {
			        boolean needUpdate = checkUpdate(false);
			        if (needUpdate) {
			            app.showUpdateAvailable(createUpdateAction());
			        }
			    }
			};
			t.start();
		}
		catch(Exception e) {
			System.out.println("Launcher.launch(): " + e.getMessage());
			e.printStackTrace();
		}		
	}

	public static void main(String[] args) {
		// Default for curator tool
		String clsName = null;
		if (args.length > 0)
			clsName = args[0];
		else
			clsName = "org.gk.gkCurator.GKCuratorFrame";
		getLauncher().launch(clsName);
	}
	
	/**
	 * Check if there is an update availabe.
	 * @return true if there are updates available.
	 */
	public boolean checkUpdate(boolean needErrorDisplay) {
		File file = new File(UPDATE_FILE_NAME);
		if (!file.exists())
			return false; // No update file. Don't need to check updates.
	    InputStream localStream = null;
	    try {
	        localStream = new FileInputStream(file);
	    }
	    catch(IOException e) {
	        System.err.println("Laucher.checkUpdate(): " + e);
	        e.printStackTrace();
	    }
	    if (localStream == null)
	        return false; // No update file. Don't need to check updates.
		/**
		 * The update file format:
		 * <?xml version="1.0" encoding="UTF-8" ?> 
		 * <resources uri="http://localhost:8080/gkb/update">
		 * <resource name="org.jar" timestamp="200405040142p" deleteDir="true" />
		 * <resource name="lib.jar" timestamp="200405040142p" deleteDir="true" />
		 * <resource name="resource.jar" timestamp="200405040142p" deleteDir="false" />
		 * <resource name="doc.jar" timestamp="200405040142p" deleteDir="true" />
		 * <resource name="image.jar" timestamp="200405040142p" deleteDir="true" />
		 * <resource name="src.jar" timestamp="1176821531595" deleteDir="false" /> 
		 * </resources>
		 */
		Map localResources = new HashMap();
		if (remoteResources != null)
		    remoteResources.clear();
		else
		    remoteResources = new HashMap();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(localStream);
			Element root = doc.getDocumentElement();
			uri = root.getAttribute("uri");
			// Check the localMap
			NodeList resources = root.getElementsByTagName("resource");
			for (int i = 0; i < resources.getLength(); i++) {
				Element resource = (Element) resources.item(i);
				String name = resource.getAttribute("name");
				String timestamp = resource.getAttribute("timestamp");
				String deleteDir = resource.getAttribute("deleteDir");
				Resource r = new Resource();
				r.setName(name);
				r.setTimeStamp(timestamp);
				r.setDeleteDir(new Boolean(deleteDir).booleanValue());
				localResources.put(name, r);
			}
			// Load the remote meta.xml file
			URL url = new URL(uri + "/update.xml");
			InputStream inputStream = url.openStream();
			// Create document for remote xml file
			InputSource remoteSource = new InputSource(inputStream);
			doc = builder.parse(remoteSource);
			root = doc.getDocumentElement();
			// Get the resouces
			resources = root.getElementsByTagName("resource");
			for (int i = 0; i < resources.getLength(); i++) {
				Element resource = (Element) resources.item(i);
				String name = resource.getAttribute("name");
				String date = resource.getAttribute("timestamp");
				String deleteDir = resource.getAttribute("deleteDir");
				Resource r = new Resource();
				r.setName(name);
				r.setTimeStamp(date);
				r.setDeleteDir(new Boolean(deleteDir).booleanValue());
				remoteResources.put(name, r);
			}
			inputStream.close();
			return compareResources(localResources, remoteResources);
		}
		catch(Exception e) {
			System.out.println("Launcher.checkUpdate(): " + e);
			//e.printStackTrace();
			if (needErrorDisplay && (app != null)) {
			    JOptionPane.showMessageDialog(app.getUserFrame(),
			                                  "Update checking failed: " + e.getMessage(),
			                                  "Error in Updating Checking",
			                                  JOptionPane.ERROR_MESSAGE);
			}
		}
		return false;
	}
	
	private boolean compareResources(Map localMap, Map remoteMap) {
	    if (remoteMap == null || remoteMap.size() == 0)
	        return false;
	    String name;
	    Resource remoteR;
	    Resource localR;
	    boolean rtn = false;
	    for (Iterator it = remoteMap.keySet().iterator(); it.hasNext();) {
	        name = (String) it.next();
	        remoteR = (Resource) remoteMap.get(name);
	        localR = (Resource) localMap.get(name);
	        if (localR == null) {
	            remoteR.setNeedUpdate(true);
	            rtn = true;
	        }
	        else {
	            if (remoteR.isNewerThan(localR)) {
	                remoteR.setNeedUpdate(true);
	                rtn = true;
	            }
	        }
	    }
	    return rtn;
	}
	
	public void update(Component parentComp) {
	    // Just use a very simple approach
	    try {
            BrowserLauncher.displayURL("http://reactomedev.oicr.on.ca/download/tools/curatorTool/install.htm",
                                       parentComp);
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(parentComp,
                                          "Error in displaying the download page: " + e,
                                          "Page Display Error",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
//        int reply = JOptionPane.showConfirmDialog(parentComp, 
//                                                  "New update is available. Do you want to update now? ",
//                                                  //"The application will be\nautomatically restarted after updated.",
//                                                  "Update Available", 
//                                                  JOptionPane.YES_NO_OPTION);
//        if (reply == JOptionPane.YES_OPTION) {
//    	    // Check if it is under Mac
//    	    String os = System.getProperty("os.name").toLowerCase();
//            if (os.indexOf("mac") > -1) {
//    	    	// Block it
//    	    	JOptionPane.showMessageDialog(parentComp,
//    	    			                      "Automatic update has not been implemented for Mac OS X now. Please install " +
//    	    			                      "the tool manually from\nhttp://brie8.cshl.edu/download/tools/curatorTool/install.htm",
//    	    			                      "Mac OS X Note",
//    	    			                      JOptionPane.INFORMATION_MESSAGE);
//    	    	return;
//    	    }
//            //app.close();
//            //app = null;
//            update();
//            restart();
//        }
    }
	
	/**
	 * Update the files.
	 */
	public void update() {
	    // No need to update
	    if (remoteResources == null || remoteResources.size() == 0)
	        return;
	    // Starting updating
	    try {
            Resource r = null;
            for (Iterator it = remoteResources.keySet().iterator(); it.hasNext();) {
                r = (Resource) remoteResources.get(it.next());
                if (r.needUpdate)
                    update(r);
            }
            updateFile();
        }
        catch (Exception e) {
            System.err.println("Launcher.update(): " + e);
            e.printStackTrace();
        }
	}
	
	private void updateFile() {
	    try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			// Create the xml document
			Document document = builder.newDocument();
			Element resourcesElm = document.createElement("resources");
			resourcesElm.setAttribute("uri", uri);
			document.appendChild(resourcesElm);
			for (Iterator it = remoteResources.keySet().iterator(); it.hasNext();) {
			    Resource r = (Resource) remoteResources.get(it.next());
			    Element resourceElm = document.createElement("resource");
			    resourceElm.setAttribute("name", r.getName());
			    resourceElm.setAttribute("timestamp", r.getTimeStamp());
			    resourceElm.setAttribute("deletedir", r.isDeleteDir() + "");
			    resourcesElm.appendChild(resourceElm);
			}
			// export the xml document
			File file = new File(UPDATE_FILE_NAME);
			FileOutputStream xmlOut = new FileOutputStream(file);
			TransformerFactory tffactory = TransformerFactory.newInstance();
			Transformer transformer = tffactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(xmlOut);
			transformer.transform(source, result);
	    }
	    catch(Exception e) {
	        System.err.println("Launcher.updateFile(): " + e);
	        e.printStackTrace();
	    }
	}
    
    private void copy(String resourceName) throws Exception {
        File file = new File(resourceName);
        if (file.exists()) {
        	//TODO: This may not work. Have to try other way.
            file.renameTo(new File(resourceName + ".bak"));
        }
        try {
            URL url = new URL(uri + "/" + resourceName);
            InputStream is = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            FileOutputStream fos = new FileOutputStream(resourceName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int length = 10240;
            byte[] in = new byte[length];
            int read = bis.read(in, 0, length);
            while (read > 0) {
                bos.write(in, 0, read);
                read = bis.read(in, 0, length);
            }
            bos.close();
            fos.close();
            bis.close();
            is.close();
        }
        catch(Exception e) {
            if (file.exists()) {
                file.renameTo(new File(resourceName));
            }
            throw e; // rethrow this exception
        }
    }
	
	private void update(Resource r) throws Exception {
	    String name = r.getName();
        if (name.equals("src.jar")) {
            copy(name);
            return;
        }
        int index = name.indexOf(".");
        String dirName = name.substring(0, index); 
	    File dir = new File(dirName);
        if (r.isDeleteDir()) {
            // back up the original folder
            if(!dir.renameTo(new File(dirName + "bak"))) {
                JOptionPane.showMessageDialog(app != null ? app.getUserFrame() : null,
                                              "Cannot backup files. The automatically update will abort.\n" +
                                              "You can update your tool via manual downloading.",
                                              "Error in Updating",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        // Maybe have to backup the directory for no deleting dir
        // Open connection to the jar file
        JarFile jarFile = null;
        try {
            URL url = new URL("jar:" + uri + "/" + name + "!/");
            JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
            jarFile = urlConnection.getJarFile();
        }
        catch (Exception e) {
            System.err.println("Launcher.update(): " + e);
            e.printStackTrace();
        }
        if (jarFile == null) {
            if (r.isDeleteDir())
                dir.renameTo(new File(dirName));
            return;
        }
        // Create a dir first
        dir.mkdir();
        extractJar(jarFile, dirName);
        dir = new File(dirName + "bak");
        delete(dir); // Delete the backup file
    }
	
	private void delete(File file) {
	    if (!file.isDirectory())
	        file.delete();
	    else {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                file.delete();
                return;
            }
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
            file.delete();
        }
	}
	
	private void extractJar(JarFile jarFile, String dirName) throws Exception {
        Enumeration entries = jarFile.entries();
        // Make directories first
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().startsWith("META-INF"))
                continue;
            if (entry.isDirectory()) {
                String fileName = entry.getName();
                // Get rid of the last "/"
                fileName = fileName.substring(0, fileName.length() - 1);
                File dirFile = new File(dirName + File.separator + fileName);
                if (!dirFile.exists())
                    dirFile.mkdir();
            }
        }
        // Extract file contents
        entries = jarFile.entries();
        // Make directories first
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().startsWith("META-INF"))
                continue;
            if (!entry.isDirectory())
                extract(jarFile, entry, dirName);
        }        
	}
	
	private void extract(JarFile jarFile, ZipEntry entry, String dirName) throws Exception {
        InputStream input = jarFile.getInputStream(entry);
        File outputFile = new File(dirName + File.separator + entry.getName());
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedInputStream bis = new BufferedInputStream(input);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        byte[] buffer = new byte[1024 * 10]; // 10 k
        int c = 0;
        while ((c = bis.read(buffer)) > 0) {
            bos.write(buffer, 0, c);
        }
        bis.close();
        input.close();
        bos.close();
        fos.close();
	}
	
	private Action createCheckUpdateAction() {
	    Action updateAction = new AbstractAction("Check for Software Update") {
	        public void actionPerformed(ActionEvent e) {
	            boolean needUpdate = checkUpdate(true);
	            if (!needUpdate) {
	                JOptionPane.showMessageDialog(app.getUserFrame(),
	                                              "No new update available.",
	                                              "No Update",
	                                              JOptionPane.INFORMATION_MESSAGE);
	                return;
	            }
	            update(app.getUserFrame());
	        }
	    };
	    return updateAction;
	}
	
	private Action createUpdateAction() {
	    Action updateAction = new AbstractAction("New update is available. Click to update the tool.") {
	        public void actionPerformed(ActionEvent e) {
	            update(app.getUserFrame());
	        }
	    };
	    return updateAction;
	}
    
    private Action createUpdateSchemaAction() {
        Action updateSchemaAction = new AbstractAction("Update Schema From DB") {
            public void actionPerformed(ActionEvent e) {
                if (app != null) {
                    if (app.updateSchema()) {
                        if (app.close()) {
                            app = null;
                            // Restart the application
                            launch(appClsName);
                        }
                    }
                }
            }
        };
        return updateSchemaAction;
    }
	
	public void restart() {
//	    if (app != null) {
//	        app.close();
//	        app = null;
//	    }
//	    launch(appClsName);
		int reply = JOptionPane.showConfirmDialog(app.getUserFrame(),
					                  "Sorry! Cannot restart the application automatically. Please restart it manually.\n" +
					                  "Do you want to exit the current session now?",
									  "Restarting?",
									  JOptionPane.YES_NO_OPTION);										  
		if (reply == JOptionPane.YES_OPTION)
			System.exit(0);
	}
	
	/**
	 * To cache the resource information from the update.xml file.
	 * 
	 */
	class Resource {
	    private String name;
	    private String timeStamp;
	    private boolean deleteDir;
	    private boolean needUpdate;
	    
	    public Resource() {
	    }
	    
	    public Resource(String name, String timeStamp, boolean deleteDir) {
	    }
        /**
         * @return Returns the deleteDir.
         */
        public boolean isDeleteDir() {
            return deleteDir;
        }
        /**
         * @param deleteDir The deleteDir to set.
         */
        public void setDeleteDir(boolean deleteDir) {
            this.deleteDir = deleteDir;
        }
        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }
        /**
         * @param name The name to set.
         */
        public void setName(String name) {
            this.name = name;
        }
        /**
         * @return Returns the timeStamp.
         */
        public String getTimeStamp() {
            return timeStamp;
        }
        /**
         * @param timeStamp The timeStamp to set.
         */
        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }
        
        public boolean isNewerThan(Resource r) {
            Date time = new Date(Long.parseLong(timeStamp));
            Date otherTime = new Date(Long.parseLong(r.getTimeStamp()));
            return time.after(otherTime);
        }
        
        public void setNeedUpdate(boolean needUpdate) {
            this.needUpdate = needUpdate;
        }
        
        public boolean isNeedUpdate() {
            return this.needUpdate;
        }
	}
}

