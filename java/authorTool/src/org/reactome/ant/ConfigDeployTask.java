/*
 * Created on Dec 8, 2005
 *
 */
package org.reactome.ant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ConfigDeployTask extends Task {
    // the root directory
    private String wsdlFileName;
    private String jdbcFileName;
    // URL for the wsdl
    private String targetNameSpace;
    // These properties are used to set database connection information
    private String dbName;
    private String dbHost;
    private String dbUser;
    private String dbPwd;
    private int dbPort;
    private String reactomeUrl;
    // For pathway layout
    private String dotPath;
    
    public ConfigDeployTask() {
    }
    
    
    public void setJdbcFileName(String jdbcFileName) {
        this.jdbcFileName = jdbcFileName;
    }


    public void setWsdlFileName(String wsdlFileName) {
        this.wsdlFileName = wsdlFileName;
    }
    
    public void setDotPath(String path) {
        this.dotPath = path;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public void setDbPwd(String dbPwd) {
        this.dbPwd = dbPwd;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public void setTargetNameSpace(String targetNameSpace) {
        this.targetNameSpace = targetNameSpace;
    }
    
    public void setReactomeUrl(String reactomeUrl) {
        this.reactomeUrl = reactomeUrl;
    }
    
    public void execute() throws BuildException {
        processWSDLFile();
        processConfigFile();
    }
    
    private void processWSDLFile() throws BuildException {
        try {
            String localhost = "localhost";
            FileReader fileReader = new FileReader(wsdlFileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            StringBuffer builder = new StringBuffer();
            while((line = bufferedReader.readLine()) != null) {
                line = line.replaceAll("localhost", targetNameSpace);
                builder.append(line).append("\n");
            }
            bufferedReader.close();
            fileReader.close();
            FileWriter fileWriter = new FileWriter(wsdlFileName);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.write(builder.toString());
            printWriter.flush();
            printWriter.close();
            fileWriter.close();
            System.out.println("WSDL file is processed.");
        }
        catch(IOException e) {
            throw new BuildException(e);
        }
    }
    
    private void processConfigFile() throws BuildException {
        try {
            FileWriter writer = new FileWriter(jdbcFileName);
            PrintWriter printWriter = new PrintWriter(writer);
            StringBuffer builder = new StringBuffer();
            builder.append("jdbc.dbHost=").append(dbHost).append("\n");
            builder.append("jdbc.dbName=").append(dbName).append("\n");
            builder.append("jdbc.dbUser=").append(dbUser).append("\n");
            builder.append("jdbc.dbPwd=").append(dbPwd).append("\n");
            builder.append("dot.path=").append(dotPath).append("\n");
            if (dbPort == 0)
                dbPort = 3306; // Default
            builder.append("jdbc.dbPort=").append(dbPort).append("\n");
            if (reactomeUrl != null)
                builder.append("reactome.link=").append(reactomeUrl).append("\n");
            printWriter.write(builder.toString());
            printWriter.flush();
            printWriter.close();
            writer.close();
            System.out.println("config.properties is processed.");
        }
        catch(IOException e) {
            throw new BuildException(e);
        }
    }

}
