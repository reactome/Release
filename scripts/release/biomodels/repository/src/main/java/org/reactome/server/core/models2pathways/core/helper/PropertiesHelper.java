package org.reactome.server.core.models2pathways.core.helper;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class PropertiesHelper {

    public Properties getDBProperties() {
        Properties properties = new Properties();
        try {
            properties.load(PropertiesHelper.class.getClassLoader().getResourceAsStream("db.properties"));
        } catch (IOException e) {
            //logger.error("Can't load properties file", e);
        }
        return properties;
    }

    public Properties getAnalysisURLProperties() {
        Properties properties = new Properties();
        try {
            properties.load(PropertiesHelper.class.getClassLoader().getResourceAsStream("url.properties"));
        } catch (IOException e) {
            //logger.error("Can't load properties file", e);
        }
        return properties;
    }
}
