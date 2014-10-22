/*
 * Created on Jun 26, 2012
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author gwu
 *
 */
@XmlRootElement
public class TranslationalModification extends AbstractModifiedResidue {
    private Integer coordinate;
    private PsiMod psiMod;
    
    public TranslationalModification() {
        
    }

    public Integer getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Integer coordinate) {
        this.coordinate = coordinate;
    }

    public PsiMod getPsiMod() {
        return psiMod;
    }

    public void setPsiMod(PsiMod psiMod) {
        this.psiMod = psiMod;
    }
    
}
