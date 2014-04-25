/*
 * Created on Dec 1, 2004
 *
 */
package org.gk.pathView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * This model class is used to help process data for comparing reactions in the sky.
 * @author wgm
 */
public class ComparingReactionsInSkyModel {
    private MySQLAdaptor dba;

    public ComparingReactionsInSkyModel() {
    }

    public ComparingReactionsInSkyModel(MySQLAdaptor dba) {
        this.dba = dba;
    }

    public void setMySQLAdapter(MySQLAdaptor adapter) {
        this.dba = adapter;
    }

    /**
     * Get the list of reactions that should be in the sky.
     * @param releasedPathways a list of top-level pathway instances.
     * @return
     */
    public List getReactionsInSky() {
        if (dba == null)
            return new ArrayList();
        try {
            Collection c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                                        ReactomeJavaConstants._doRelease,
                                                        "=",
                                                        "true");
            return new ArrayList(c);
        }
        catch(Exception e) {
            System.err.println("ComparingReactionsInSkyModel.getReactionsInSky(): " + e);
            e.printStackTrace();
        }
        return new ArrayList();
    }
}