package org.reactome.server.core.models2pathways.core.helper;

import org.reactome.server.core.models2pathways.core.model.TrivialChemical;

import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class TrivialChemicalHelper {

    private static TrivialChemicalHelper trivialChemicalHelper = new TrivialChemicalHelper();
    private Set<TrivialChemical> trivialChemicals;

    private TrivialChemicalHelper() {
    }

    public static TrivialChemicalHelper getInstance() {
        return trivialChemicalHelper;
    }

    public Set<TrivialChemical> getTrivialChemicals() {
        return trivialChemicals;
    }

    public void setTrivialChemicals(Set<TrivialChemical> trivialChemicals) {
        this.trivialChemicals = trivialChemicals;
    }

    @Override
    public String toString() {
        return "TrivialChemicalHelper{" +
                "trivialChemicals=" + trivialChemicals +
                '}';
    }
}
