package org.reactome.core.controller;


public class InstanceNotFoundException extends ReactomeRemoteException {

    private String clsName;
    private long dbId;
    private String propertyValue;

    public InstanceNotFoundException() {

    }

    public InstanceNotFoundException(long dbId) {
        this.dbId = dbId;
    }

    public InstanceNotFoundException(String clsName, long dbId) {
        this.clsName = clsName;
        this.dbId = dbId;
    }

    public InstanceNotFoundException(String clsName, String value) {
        this.clsName = clsName;
        this.propertyValue = value;
    }

    public String getClsName() {
        return clsName;
    }

    public void setClsName(String clsName) {
        this.clsName = clsName;
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public String toString() {
        if (clsName != null && dbId != 0)
            return super.toString() + ": " + clsName + ": " + dbId;
        if (dbId != 0)
            return super.toString() + ": " + dbId;
        return super.toString();
    }

}