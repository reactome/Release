package org.reactome.server.models2pathways.core.utils;

import org.reactome.server.models2pathways.core.model.Namespace;
import org.reactome.server.models2pathways.core.model.Specie;
import org.reactome.server.models2pathways.core.model.TrivialChemical;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class PropertiesLoader {
    private static final String SEPARATOR = "\t";
    private static final String SPECIES_FILENAME = "species.txt";
    private static final String NAMESPACE_FILENAME = "namespaces.txt";
    private static final String TRIVIALCHEMICALS_FILENAME = "trivialchemicals.txt";

    /**
     *  
     * @return A Set of Species, based on resources/species.txt
     * @throws IOException
     */
    public Set<Specie> getSpecies() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SPECIES_FILENAME);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Set<Specie> species = new HashSet<>();
        String line;
        while ((line = br.readLine()) != null) {
            String[] content = line.split(SEPARATOR);
            species.add(new Specie(Long.valueOf(content[0]), Long.valueOf(content[1]), content[2]));
        }
        return species;
    }

    /**
     *
     * @return A Set of Namespaces, based on resources/namespaces.txt
     * @throws IOException
     */
    public Set<Namespace> getNamespaces() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(NAMESPACE_FILENAME);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Set<Namespace> namespaces = new HashSet<>();
        String line;
        while ((line = br.readLine()) != null) {
            String[] content = line.split(SEPARATOR);
            namespaces.add(new Namespace(content[0], Boolean.valueOf(content[1])));
        }
        return namespaces;
    }

    /**
     *
     * @return A Set of Trivial Chemicals, based on resources/trivialchemicals.txt
     * @throws IOException
     */
    public Set<TrivialChemical> getTrivialChemicals() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TRIVIALCHEMICALS_FILENAME);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Set<TrivialChemical> trivialChemicals = new HashSet<>();
        String line;
        while ((line = br.readLine()) != null) {
            String[] content = line.split(SEPARATOR);
            trivialChemicals.add(new TrivialChemical(Long.valueOf(content[0]), content[1]));
        }
        return trivialChemicals;
    }
}
