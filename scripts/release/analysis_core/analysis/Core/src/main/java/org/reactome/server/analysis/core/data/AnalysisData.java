package org.reactome.server.analysis.core.data;

import org.apache.log4j.Logger;
import org.reactome.server.analysis.core.model.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * Used to load the DataContainer from the file only ONCE when the application is loaded
 * (either in a local execution or deployed in a server). The default constructor uses
 * the Kryo deserializer in AnalysisDataUtils to create the DataContainer and from this
 * moment on, the AnalysisData object is a PROXY to the DataContainer.
 *
 * *** IMPORTANT ***
 * Please note that every time that getHierarchiesData method is called, it retrieves a
 * clone of the HierarchiesData contained in DataContainer so the changes in it doesn't
 * happen to other instances of HierarchiesData
 *
 * *** NOTE ****
 * AnalysisData is the object to be used in the code, PLEASE avoid using the DataContainer
 * directly in the analysis (unless there is a good reason).
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Component
@Scope("singleton")
public class AnalysisData {
    private static Logger logger = Logger.getLogger(AnalysisData.class.getName());

    private static DataContainer container = null;

    public AnalysisData() {
    }

    private DataContainer getContainer(){
        if(container==null){
            String msg = "DataContainer has not been initialized.";
            logger.error(msg, new NullPointerException(msg));
            System.err.println(getClass().getName() + " [ERROR] : " + msg);
            System.exit( 1 );
        }
        return container;
    }

    /**
     * Returns the physical entity graph with all the complexes, sets and proteins in Reactome
     * @return the physical entity graph with all the complexes, sets and proteins in Reactome
     */
    public PhysicalEntityGraph getPhysicalEntityGraph() {
        return this.getContainer().getPhysicalEntityGraph();
    }

    /**
     * Returns a map from identifier to [resource, [PhysicalEntityNode]]
     * @return a map from identifier to [resource, [PhysicalEntityNode]]
     */
    public IdentifiersMap getIdentifiersMap() {
        return this.getContainer().getIdentifiersMap();
    }

    public Map<SpeciesNode, PathwayHierarchy> getPathwayHierarchies(){
        return this.getContainer().getHierarchiesData().getPathwayHierarchies();
    }

    /**
     * Initialise the AnalysisData object loading the content of the file into the container
     * @param fileName the binary file containing the data structures for the analysis
     */
    public void setFileName(String fileName){
        if(container!=null){
            logger.warn("Attempt to load the content file when previously loaded");
            return;
        }
        try {
            InputStream file = new FileInputStream(fileName);
            container = AnalysisDataUtils.getDataContainer(file);
            HierarchiesDataProducer.initializeProducer(container);
        }catch (FileNotFoundException e){
            String msg = String.format("%s has not been found. Please check the settings", fileName);
            logger.fatal(msg, e);
            System.err.println(msg);
            System.exit( 1 );
        }
    }
}
