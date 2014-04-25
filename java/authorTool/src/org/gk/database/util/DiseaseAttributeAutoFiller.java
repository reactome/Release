/*
 * Created on Apr 1, 2011
 *
 */
package org.gk.database.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author wgm
 *
 */
public class DiseaseAttributeAutoFiller extends PsiModAttributeAutoFiller {
    
    public DiseaseAttributeAutoFiller() {
        ONTOLOGY_NAME = "DOID";
        displayOntologyName = "Disease";
    }
    
    @Override
    protected List<String> extractSynonym(Map<String, String> meta) {
        List<String> synonyms = new ArrayList<String>();
        for (String key : meta.keySet()) {
            String value = meta.get(key);
            if (key == null)
                continue; // Sometime there is a null as a key, which may not be correct!
            if (key.startsWith("exact_synonym")) { // Based on the format used in the web service. I guess this may not be true in the future.
                synonyms.add(value);
            }
        }
        return synonyms;
    }
}
