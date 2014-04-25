/*
 * Created on Mar 5, 2009
 *
 */
package org.reactome.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;


/**
 * For cleanup VertexSearchableTerm instances
 * @author wgm
 *
 */
public class CleanUpVertexSearchableTermInstances {
    
    private MySQLAdaptor getMySQLAdaptor() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("brie8.cshl.edu",
                                            "test_gk_central_121108",
                                            "authortool",
                                            "T001test",
                                            3306);
        return dba;
    }
    
    @Test
    public void checkVSTInstances() throws Exception {
        MySQLAdaptor dba = getMySQLAdaptor();
        Collection c = dba.fetchInstancesByClass(ReactomeJavaConstants.VertexSearchableTerm);
        List<GKInstance> list = new ArrayList<GKInstance>(c);
        Collections.sort(list, new Comparator<GKInstance>() {
            public int compare(GKInstance inst1, GKInstance inst2) {
                Long id1 = inst1.getDBID();
                Long id2 = inst2.getDBID();
                return id1.compareTo(id2);
            }
        });
        GKInstance pre = null;
        for (GKInstance inst : list) {
            if (pre == null) {
                pre = inst;
                continue;
            }
            int diff = (int) (inst.getDBID() - pre.getDBID());
            if (diff > 1) {
                System.out.println(pre.getDBID() + " -> " + inst.getDBID());
                break;
            }
            pre = inst;
        }
        System.out.println("Done...");
    }
    
}
