package org.reactome.server.fireworks.util;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.GKInstance2ModelObject;
import org.reactome.core.model.Species;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Get the species list for pathways listed in the front page items and their
     * orthologous events.
     * @return
     */
    public static List<Species> getSpeciesList(MySQLAdaptor dba, GKInstance2ModelObject converter) {
        try {
            Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
            if (c == null || c.size() == 0)
                return null;
            GKInstance frontPage = (GKInstance) c.iterator().next();
            List<GKInstance> values = frontPage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
            Set<GKInstance> species = new HashSet<GKInstance>();
            for (GKInstance pathway : values) {
                List<GKInstance> speciesList = pathway.getAttributeValuesList(ReactomeJavaConstants.species);
                if (speciesList != null)
                    species.addAll(speciesList);
                List<GKInstance> orEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
                if (orEvents == null)
                    continue;
                for (GKInstance orEvent : orEvents) {
                    speciesList = orEvent.getAttributeValuesList(ReactomeJavaConstants.species);
                    if (speciesList != null)
                        species.addAll(speciesList);
                }
            }
            // In case of not using cache in the server-side, using HashSet will create duplicated entries
            // So using a HashMap keyed by DB_IDs should avoid this problem
            Map<Long, GKInstance> dbIdToInst = new HashMap<Long, GKInstance>();
            for (GKInstance inst : species) {
                dbIdToInst.put(inst.getDBID(), inst);
            }
            List<Species> rtn = new ArrayList<Species>();
            // Place the human in the top: this is special
            Species human = null;
            for (GKInstance s : dbIdToInst.values()) {
                Species converted = (Species) converter.createObject(s);
                if (s.getDBID().equals(48887L))
                    human = converted;
                else
                    rtn.add(converted);
            }
            Collections.sort(rtn, new Comparator<Species>() {
                public int compare(Species s1, Species s2) {
                    return s1.getDisplayName().compareTo(s2.getDisplayName());
                }
            });
            rtn.add(0, human);
            return rtn;
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
