/*
 * Created on May 14, 2012
 *
 */
package org.gk.database.util;


/**
 * @author gwu
 *
 */
public class SOAttributeAutoFiller extends DiseaseAttributeAutoFiller {
    
    public SOAttributeAutoFiller() {
        ONTOLOGY_NAME = "SO";
        displayOntologyName = "SequenceOntology";
    }
    
    /* (non-Javadoc)
     * @see org.gk.database.util.AbstractAttributeAutoFiller#getConfirmationMessage()
//     */
//    @Override
//    protected String getConfirmationMessage() {
//        return "Do you want the tool to fetch information from the EBI ontology database \n" +
//               "for this SequenceOntology instance?";
//    }

//    @Override
//    protected Object getRequiredAttribute(GKInstance instance) throws Exception {
//        return instance.getAttributeValue(ReactomeJavaConstants.accession);
//    }
//
//    /**
//     * The implementation of the SO is the simpliest one. Nothing needs to be done here!
//     */
//    @Override
//    protected void mapMetaToAttributes(GKInstance instance, String termId,
//                                       Query service) throws Exception {
//        
//    }
//
//    @Override
//    protected String getIdentifierAttributeName() {
//        return ReactomeJavaConstants.accession;
//    }
    
}
