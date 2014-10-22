/*
 * Created on Jun 30, 2005
 *
 */
package org.reactome.core.controller;

/**
 * This JavaBean class is used to hold paramters passed from http request.
 *
 * @author guanming
 */
public class BioPAXExporterCommand {

    private String dbName;
    private Long dbID;

    public BioPAXExporterCommand() {

    }

    public void setDbName(String name) {
        this.dbName = name;
    }

    /**
     * @return Returns the dbID.
     */
    public Long getDbID() {
        return dbID;
    }

    /**
     * @param dbID The dbID to set.
     */
    public void setDbID(Long dbID) {
        this.dbID = dbID;
    }

    /**
     * @return Returns the dbName.
     */
    public String getDbName() {
        return dbName;
    }
}
