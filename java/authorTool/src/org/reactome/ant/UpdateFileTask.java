package org.reactome.ant;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author wugm
 */
public class UpdateFileTask extends Task {
    
    private File dir;
    private String uri;
    private List resources;

    public UpdateFileTask() {
        resources = new ArrayList();
    }
    
    public void setDir(File dir) {
        this.dir = dir;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public Resource createResource() {
        Resource r = new Resource();
        resources.add(r);
        return r;
    }
    
    public void execute() throws BuildException {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<resources uri=\"");
        buffer.append(uri);
        buffer.append("\">\n");
        Resource r = null;
        for (Iterator it = resources.iterator(); it.hasNext();) {
            r = (Resource) it.next();
            File file = new File(dir + File.separator + r.name);
            if (!file.exists())
                throw new BuildException("UpdateFileTask(): cannot find jar file \"" + r.name + "\".");
            buffer.append("<resource name=\"");
            buffer.append(r.name);
            buffer.append("\" ");
            buffer.append("timestamp=\"");
            if (r.timestamp == null)
                buffer.append(file.lastModified());
            else
                buffer.append(r.timestamp);
            buffer.append("\" ");
            buffer.append("deleteDir=\"");
            buffer.append(r.deleteDir);
            buffer.append("\" />");
            buffer.append("\n");
        }
        buffer.append("</resources>");
        File file = new File(dir + File.separator + "update.xml");
        try {
            FileWriter fos = new FileWriter(file);
            fos.write(buffer.toString());
            fos.flush();
            fos.close();
        }
        catch(IOException e) {
            throw new BuildException(e.getMessage());
        }
    }
    
    public class Resource {
        private String name;
        private boolean deleteDir;
        private String timestamp;
        
        public Resource() {
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setDeleteDir(boolean deleteDir) {
            this.deleteDir = deleteDir;
        }
        
        public void setTimestamp(String ts) {
            this.timestamp = ts;
        }
        
    }
    
}
