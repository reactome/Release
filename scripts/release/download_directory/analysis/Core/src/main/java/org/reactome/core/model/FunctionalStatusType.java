/*
 * Created on Jun 7, 2013
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
public class FunctionalStatusType extends DatabaseObject {
    private String defintion;
    private List<String> name;
    
    public FunctionalStatusType() {
        
    }

    public String getDefintion() {
        return defintion;
    }

    public void setDefintion(String defintion) {
        this.defintion = defintion;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }
    
    
    
}
