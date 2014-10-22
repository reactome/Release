/*
 * Created on Jun 25, 2012
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author gwu
 *
 */
@XmlRootElement
public class ReferenceDatabase extends DatabaseObject {
    private String accessUrl;
    private List<String> name;
    private String url;
    
    public ReferenceDatabase() {
        
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessurl) {
        this.accessUrl = accessurl;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
}
