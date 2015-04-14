package org.reactome.server.core.models2pathways.core.utils;

import org.reactome.server.core.models2pathways.core.helper.PropertiesHelper;

import java.net.URI;
import java.util.Properties;

/**
 * @author Maximilian Koch <mkoch@ebi.ac.uk>
 */
public class URLBuilder {

    private static final PropertiesHelper propertiesHelper = new PropertiesHelper();


    public static URI getIdentifiersURL() {
        Properties properties = getProperties();
        return URI.create(properties.getProperty("analysis.service.url"));
    }

    public static URI getTokenURL(String token, String resource) {
        Properties properties = getProperties();

        return URI.create(properties.getProperty("analysis.service.url.token") + token +
                properties.getProperty("analysis.service.url.token.extension") + resource);
    }

    private static Properties getProperties() {
        return propertiesHelper.getAnalysisURLProperties();
    }
}
