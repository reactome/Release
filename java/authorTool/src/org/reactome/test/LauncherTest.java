/*
 * Created on Aug 13, 2004
 */
package org.reactome.test;

import java.io.File;

import junit.framework.TestCase;
import launcher.Launcher;

/**
 * 
 * @author wgm
 */
public class LauncherTest extends TestCase {
    
    public LauncherTest(String name) {
        super(name);
    }
    
    public void testChecking() {
        Launcher launcher = Launcher.getLauncher();
        boolean needUpdate = launcher.checkUpdate(true);
        assertTrue(needUpdate);
    }
    
    public void testUpdate() {
        Launcher launcher = Launcher.getLauncher();
        boolean needUpdate = launcher.checkUpdate(true);
        if (needUpdate)
            launcher.update();
        File org = new File("org");
        assertTrue(org.exists());        
    }
    
}
