/*
 * Created on Aug 4, 2005
 *
 */
package org.reactome.test;

import org.gk.database.util.ReferencePeptideSequenceAutoFiller;
import org.gk.model.GKInstance;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;

import junit.framework.TestCase;

public class ReferencePeptideSequenceAutoFillerUnitTest extends TestCase {
    
    public ReferencePeptideSequenceAutoFillerUnitTest() {
    }
    
    public void testProcess() {
        try {
            ReferencePeptideSequenceAutoFiller filler = new ReferencePeptideSequenceAutoFiller();
            GKInstance instance = new GKInstance();
            XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
            SchemaClass cls = fileAdaptor.getSchema().getClassByName("ReferencePeptideSequence");
            instance.setSchemaClass(cls);
            instance.setAttributeValue("identifier", "O00255");
            filler.process(instance, null);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

}
