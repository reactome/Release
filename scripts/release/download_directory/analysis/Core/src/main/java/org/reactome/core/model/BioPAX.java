package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Author: Stanislav Palatnik
 * Date: 8/10/11
 */
@XmlRootElement
class  BioPAX {
    private long dbID;
    private String biopaxxml;

    public BioPAX() {
    }


    public long getDbID() {
        return dbID;
    }

    public void setDbID(long dbID) {
        this.dbID = dbID;
    }

    public String getBiopaxxml() {
        return biopaxxml;
    }

    public void setBiopaxxml(String biopaxxml) {
        this.biopaxxml = biopaxxml;
    }


}
