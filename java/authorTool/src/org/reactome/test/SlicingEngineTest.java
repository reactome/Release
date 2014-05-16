/*
 * Created on Feb 2, 2006
 *
 */
package org.reactome.test;

import java.util.List;

import junit.framework.TestCase;

import org.gk.slicing.SlicingEngine;

/**
 * This TestCase is used to check some functions in SlicingEngine class.
 * @author guanming
 *
 */
public class SlicingEngineTest extends TestCase {

    public SlicingEngineTest() {
        
    }
    
    public void testGetSpecies() throws Exception {
        SlicingEngine engine = new SlicingEngine();
        engine.setSpeciesFileName("Species.txt");
        List speciesIDs = engine.getSpeciesIDs();
        System.out.println("Species: " + speciesIDs);
    }
}
