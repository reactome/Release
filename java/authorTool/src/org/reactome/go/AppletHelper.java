/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JApplet;

/**
 * A utility class for doing Applet specific actions.
 * @author wugm
 */
public class AppletHelper {
	private static AppletHelper helper;
	// Context
	private JApplet applet;
	private ImageIcon isAIcon;
	private ImageIcon isPartOfIcon;
	
	public AppletHelper(JApplet applet) {
		this.applet = applet;
	}

	public static AppletHelper getHelper() {
		return helper;
	}
	
	public static void setHelper(AppletHelper helper1) {
		helper = helper1;
	}
	
	public ImageIcon getIcon(String imageFileName) {
		Image image = applet.getImage(applet.getCodeBase(), "images/" + imageFileName);
		return new ImageIcon(image);
	}
	
	public ImageIcon getIsAIcon() {
		if (isAIcon == null) {
			Image image = applet.getImage(applet.getCodeBase(), "images/IsA.gif");
			isAIcon = new ImageIcon(image);
		}
		return isAIcon;
	}
	
	public ImageIcon getIsPartOfIcon() {
		if (isPartOfIcon == null) {
			Image image = applet.getImage(applet.getCodeBase(), "images/PartOf.gif");
			isPartOfIcon = new ImageIcon(image);
		}
		return isPartOfIcon;
	}
	
	public void showDocument(String url, String windowName) throws Exception{
		applet.getAppletContext().showDocument(new URL(url), windowName);
	}

}
