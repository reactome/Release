/*
 * Created on Oct 19, 2012
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
public class ReplacedResidue extends GeneticallyModifiedResidue {
    
    private Integer coordinate;
    private List<PsiMod> psiMod;
    
    public ReplacedResidue() {
        
    }

    public Integer getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Integer coordinate) {
        this.coordinate = coordinate;
    }

    public List<PsiMod> getPsiMod() {
        return psiMod;
    }

    public void setPsiMod(List<PsiMod> psiMod) {
        this.psiMod = psiMod;
    }
    
    
}
