/*
 * Created on Jun 26, 2012
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
public class InterChainCrosslinkedResidue extends CrosslinkedResidue {
    private List<InterChainCrosslinkedResidue> equivalentTo;
    private List<ReferenceSequence> secondReferenceSequence;
    
    public InterChainCrosslinkedResidue() {
        
    }

    public List<InterChainCrosslinkedResidue> getEquivalentTo() {
        return equivalentTo;
    }

    public void setEquivalentTo(List<InterChainCrosslinkedResidue> equivalentTo) {
        this.equivalentTo = equivalentTo;
    }

    public List<ReferenceSequence> getSecondReferenceSequence() {
        return secondReferenceSequence;
    }

    public void setSecondReferenceSequence(List<ReferenceSequence> secondReferenceSequence) {
        this.secondReferenceSequence = secondReferenceSequence;
    }
    
}
