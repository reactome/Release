/*
 * Created on Jul 11, 2003
 */
package org.gk.util;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * This class is used to launch an external web browser. The user should
 * specify the path for the web browser before launch.
 * @author wgm
 */
public class BrowserLauncher {
	// Property Path
	//private static final String PROP_PATH = "resources" + File.separator + "browser.prop";
	private static final String PROP_FILE = "browser.prop";
	
	private static String browserPath;
	
	public static void displayURL(String url, Component comp) throws IOException {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("mac") > -1) {
			String[] cmdArray = new String[]{"open", url};			
			Runtime.getRuntime().exec(cmdArray);
			return;
		}
		if (browserPath == null)
			locateBrowser(comp);
		if (browserPath == null) {
			JOptionPane.showMessageDialog(comp, "No Browser Specified!", 
			                              "Error In Launching Browser", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String[] cmdArray = new String[]{browserPath, url};
		Process process = Runtime.getRuntime().exec(cmdArray);
		// This avoids a memory leak on some versions of Java on Windows.
		// That's hinted at in <http://developer.java.sun.com/developer/qow/archive/68/>.
		/* Should not do this. This will hang on the Java thread that launch this process.
		try {
			process.waitFor();
		}
		catch (InterruptedException ie) {
			throw new IOException("InterruptedException while launching browser: "
					+ ie.getMessage());
		}
		int exitCode = process.exitValue();
		if (exitCode != 0) {
			System.err.println("Error in launching browser.");
			throw new IOException("BrowserLaunch: Error in launching browser.");
		}
		*/
	}	
    
    public static void setBrowser(String path) {
		browserPath = path;
		Properties prop = new Properties();
		prop.setProperty("browser", browserPath);
		try {
			File file = getPropertyFile();
		    FileOutputStream output = new FileOutputStream(file);
			prop.store(output, "Browser Setting");
			output.close();
		}
		catch(IOException e) {
			System.err.println("BrowserLauncher.setBrowser(): " + e);
			e.printStackTrace();
		}
	}
	
	public static String getBrowser() {
		String browserPath = null;
		// Check if browser properties is set.
		File file = getPropertyFile();
		//File file = new File(BrowserLauncher.PROP_PATH);
		if (file != null && file.exists()) {
			Properties properties = new Properties();
			try {
				FileInputStream input = new FileInputStream(file);
				properties.load(input);
				input.close();
			}
			catch(IOException e) {
				System.err.println("BrowserLauncher.locateBrowser(): " + e);
			}
			browserPath = properties.getProperty("browser");
		}
		return browserPath;
	}
	
	private static File getPropertyFile() {
	    File file = null;
	    try {
	        file = GKApplicationUtilities.getPropertyFile(PROP_FILE);
	    }
	    catch(IOException e) {
	        System.err.println("BrowserLauncher.getPropertyFile(): " + e);
	        e.printStackTrace();
	    }
	    return file;
	}
	
	private static void locateBrowser(Component comp) {
		// Check if browser properties is set.
		//File file = new File(PROP_PATH);
		File file = getPropertyFile();
	    if (file != null && file.exists()) {
			Properties properties = new Properties();
			try {
				FileInputStream input = new FileInputStream(file);
				properties.load(input);
				input.close();
			}
			catch(IOException e) {
				System.err.println("BrowserLauncher.locateBrowser(): " + e);
				e.printStackTrace();
			}
			browserPath = properties.getProperty("browser");
		}
		if (browserPath == null) {
			// Launch a GUI
			JFileChooser fileChooser = new JFileChooser();
		    fileChooser.setDialogTitle("Choose A Web Browser");
			int rtn = fileChooser.showOpenDialog(comp);
			if (rtn == JFileChooser.APPROVE_OPTION) {
				browserPath = fileChooser.getSelectedFile().getAbsolutePath();
				// Save to properties.
				Properties prop = new Properties();
				try {
				    File file1 = getPropertyFile();
					FileOutputStream output = new FileOutputStream(file1);
					prop.setProperty("browser", browserPath);
					prop.store(output, "Browser Setting");
					output.close();
				}
				catch(IOException e) {
					System.err.println("BrowserLauncher.locateBrowser() 1: " + e);
				}
			}
		}
	}
}
