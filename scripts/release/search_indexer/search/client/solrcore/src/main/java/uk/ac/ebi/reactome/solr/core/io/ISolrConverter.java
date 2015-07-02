package uk.ac.ebi.reactome.solr.core.io;

import uk.ac.ebi.reactome.core.model.facetting.FacetMap;
import uk.ac.ebi.reactome.core.model.query.Query;
import uk.ac.ebi.reactome.core.model.result.Entry;
import uk.ac.ebi.reactome.core.model.result.GroupedResult;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;

import java.util.List;

/**
 * Converts a Solr QueryResponse into Objects provided by Project Models
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public interface ISolrConverter {

    public FacetMap getFacetingInformation(Query query) throws SolrSearcherException;
    public FacetMap getFacetingInformation () throws SolrSearcherException;

    public List<String> getAutocompleteSuggestions(String query) throws SolrSearcherException;
    public List<String> getSpellcheckSuggestions(String query) throws SolrSearcherException;

    public Entry getEntryById(String id) throws SolrSearcherException;
    public GroupedResult getEntries(Query query) throws SolrSearcherException;
    public GroupedResult getClusteredEntries(Query query) throws SolrSearcherException;
}


