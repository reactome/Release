package org.reactome.server.analysis.core.model.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class ResourceFactory {
    //DEFINES the main resources (and those to use as main resources in case the first ones are not found.
    public static enum MAIN { UNIPROT, ENSEMBL, CHEBI }
    //NOTE: The idea is to get rid of the auxMainResources by being in touch with curators and find out whether a
    //main resource identifier can be used in the PhysicalEntities where no main resource was found
    public static enum AUX { NCBI_PROTEIN, EMBL, COMPOUND } //TODO: Add miRBase as AUX resource

    //Some resources use the same identifiers than other resources so in these cases we do not take into account
    //the equivalents. To do so we convert the equivalent ones to the main resource in the 'getResource' method
    private static Map<String, String> equivalences = new HashMap<String, String>();
    static {
        equivalences.put("GENECARDS",   MAIN.UNIPROT.name());
        equivalences.put("UCSC_HUMAN",  MAIN.UNIPROT.name());

        equivalences.put("BIOGPS_GENE", "NCBI_GENE");
        equivalences.put("CTD_GENE",    "NCBI_GENE");
        equivalences.put("DBSNP_GENE",  "NCBI_GENE");

        equivalences.put("DOCK_BLASTER","PROTEIN_DATA_BANK");
    }

    //Cache containing the previously created resource for a given name
    private static Map<String, Resource> resourceMap = new HashMap<String, Resource>();

    public static Resource getResource(String name){
        name = name.toUpperCase().replaceAll("\\s", "_").trim();
        if(equivalences.containsKey(name)){
            name = equivalences.get(name);
        }
        Resource resource = resourceMap.get(name);
        if(resource==null){
            for (MAIN main : MAIN.values()) {
                if(name.equals(main.name())){
                    resource = new MainResource(name);
                }
            }
            for (AUX aux : AUX.values()) {
                if(name.equals(aux.name())){
                    resource = new MainResource(name, true);
                }
            }
            if(resource==null){
                resource = new Resource(name);
            }
            resourceMap.put(name, resource);
        }
        return resource;
    }

    public static MAIN getMainResource(String name){
        name = name.toUpperCase().replaceAll("\\s", "_").trim();
        for (MAIN main : MAIN.values()) {
            if(main.toString().equals(name)){
                return main;
            }
        }
        return null;
    }
}
