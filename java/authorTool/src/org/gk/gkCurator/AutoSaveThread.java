/*
 * Created on Nov 20, 2007
 *
 */
package org.gk.gkCurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.GKApplicationUtilities;

public class AutoSaveThread extends Thread {
    // In miliseconds
    private long preTime;
    private long period;
    private boolean shouldStop = false;
    
    public AutoSaveThread() {
    }
    
    public void setSavePeriod(int min) {
        period = min * 60 * 1000;
    }
    
    public void initialize() {
        preTime = System.currentTimeMillis();
    }
    
    public void setIsStop(boolean stop) {
        shouldStop = stop;
    }
    
    /**
     * Use this method so that at most five back-up can be created in order to avoid any possible
     * data loss.
     * @return
     */
    private String getTempFileName() {
        String fileName = "AutoSaved_";
        List<File> files = new ArrayList<File>();
        for (int i = 1; i <= 5; i++) {
            String temp = fileName + i + ".rtpj";
            File tempFile = GKApplicationUtilities.getTempFile(temp);
            if (!tempFile.exists())
                return temp;
            files.add(tempFile);
        }
        // All five files have been used. Find the oldest file, and replace it.
        int index = files.size() - 1;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            long time = file.lastModified();
            if (time < oldest) {
                oldest = time;
                index = i;
            }
        }
        index ++; // Need step up one.
        return fileName + index + ".rtpj";
    }
    
    public void run() {
        while (!shouldStop) {
            // When the program starts, preTime is 0, there is always a saving
            // doing if there is any changes.
            long diff = System.currentTimeMillis() - preTime;
            if (diff > period) {
                XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
                try {
                    //TODO: Need to check syncrhonization issue regarding adaptor.
                    if (adaptor.isDirty()) {
                        // Need to get the temp file name
                        String tempFileName = getTempFileName();
                        File tmpFile = GKApplicationUtilities.createTempFile(tempFileName);
                        adaptor.saveAsTemp(tmpFile.getAbsolutePath());
                    }
                }
                catch(Exception e) {
                    System.err.println("Cannot do auto-saving: " + e);
                    e.printStackTrace();
                }
                // Move from the try block to avoid an infinity loop.
                initialize();
                try {
                    // Put thread into sleep
                    Thread.sleep(period);
                }
                catch(InterruptedException e) {
                    System.err.println("AutoSaveThread.run(): " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
}
