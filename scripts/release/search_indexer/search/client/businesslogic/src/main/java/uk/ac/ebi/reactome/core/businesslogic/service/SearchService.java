package uk.ac.ebi.reactome.core.businesslogic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.businesslogic.exception.SearchServiceException;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.enhancer.impl.Enricher;
import uk.ac.ebi.reactome.core.enhancer.io.IEnricher;
import uk.ac.ebi.reactome.core.model.facetting.FacetContainer;
import uk.ac.ebi.reactome.core.model.facetting.FacetMap;
import uk.ac.ebi.reactome.core.model.query.Query;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.Entry;
import uk.ac.ebi.reactome.core.model.result.GroupedResult;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;
import uk.ac.ebi.reactome.solr.core.io.ISolrConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Search Service acts as api between the Controller and Solr / Database
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final ISolrConverter solrConverter;

    private static String host;
    private static String database;
    private static String currentDatabase;
    private static String user;
    private static String password;
    private static Integer port;

    /**
     * Constructor for Spring Dependency Injection and loading MavenProperties
     * @param solrConverter Parameters are set in spring mvc-dispatcher-servlet
     * @throws SearchServiceException
     */
    public SearchService(ISolrConverter solrConverter) throws SearchServiceException {
        this.solrConverter = solrConverter;
        loadProperties();
    }

    private void loadProperties () throws SearchServiceException {

        try {
            Properties databaseProperties = new Properties();
            databaseProperties.load(getClass().getResourceAsStream("/web.properties"));
            host = databaseProperties.getProperty("database_host");
            database = databaseProperties.getProperty("database_name");
            currentDatabase = databaseProperties.getProperty("database_currentDatabase");
            user = databaseProperties.getProperty("database_user");
            password = databaseProperties.getProperty("database_password");
            port = Integer.valueOf(databaseProperties.getProperty("database_port"));
        } catch (IOException e) {
            logger.error("Error when loading Database Properties ", e);
            throw new SearchServiceException("Error when loading Database Properties ", e);
        }
    }

    /**
     * Gets Faceting information for a specific query + filters.
     * This Method will query solr once again if the number of selected filters and found facets differ
     * (this will help preventing false faceting information when filter are contradictory to each other)
     * @param queryObject query and filter (species types keywords compartments)
     * @return FacetMap
     * @throws SolrSearcherException
     */
    public FacetMap getFacetingInformation (Query queryObject) throws SolrSearcherException {
        if (queryObject != null && queryObject.getQuery()!=null && !queryObject.getQuery().isEmpty()) {

            FacetMap facetMap = solrConverter.getFacetingInformation(queryObject);
            boolean correctFacets = true;
            // Each faceting group(species,types,keywords,compartments) is dependent from all selected filters of other faceting groups
            // This brings the risk of having filters that contradict each other. To avoid having selected facets that will cause problems
            // with the next filtering or querying it is necessary to remove those from the filtering process and repeat the faceting step
            if (queryObject.getSpecies() != null && facetMap.getSpeciesFacet().getSelected().size() != queryObject.getSpecies().size()) {
                correctFacets = false;
                List<String> species = new ArrayList<String>();
                for (FacetContainer container : facetMap.getSpeciesFacet().getSelected()) {
                    species.add(container.getName());
                }
                queryObject.setSpecies(species);
            }
            if (queryObject.getTypes() != null && facetMap.getTypeFacet().getSelected().size() != queryObject.getTypes().size()) {
                correctFacets = false;
                List<String> types = new ArrayList<String>();
                for (FacetContainer container : facetMap.getTypeFacet().getSelected()) {
                    types.add(container.getName());
                }
                queryObject.setTypes(types);
            }
            if (queryObject.getKeywords() != null && facetMap.getKeywordFacet().getSelected().size() != queryObject.getKeywords().size()) {
                correctFacets = false;
                List<String> keywords = new ArrayList<String>();
                for (FacetContainer container : facetMap.getKeywordFacet().getSelected()) {
                    keywords.add(container.getName());
                }
                queryObject.setKeywords(keywords);
            }
            if (queryObject.getCompartment() != null && facetMap.getCompartmentFacet().getSelected().size() != queryObject.getCompartment().size()) {
                correctFacets = false;
                List<String> compartments = new ArrayList<String>();
                for (FacetContainer container : facetMap.getCompartmentFacet().getSelected()) {
                    compartments.add(container.getName());
                }
                queryObject.setCompartment(compartments);
            }
            if (correctFacets) {
                return facetMap;
            } else {
                return solrConverter.getFacetingInformation(queryObject);
            }
        }
        return null;
    }

    /**
     * Method for providing Faceting information for Species,Types,Keywords and Compartments
     * @return FacetMap
     * @throws SolrSearcherException
     */
    public FacetMap getTotalFacetingInformation () throws SolrSearcherException {
        return solrConverter.getFacetingInformation();
    }

    /**
     * Method for providing autocomplete suggestions
     * @param query Term (Snippet) you want to have autocompleted
     * @return  List(String) of suggestions if solr is able to provide some
     * @throws SolrSearcherException
     */
    public List<String> getAutocompleteSuggestions(String query) throws SolrSearcherException {
        if (query!= null && !query.isEmpty()) {
            return solrConverter.getAutocompleteSuggestions(query);
        }
        return null;
    }

    /**
     * Method for supplying spellcheck suggestions
     * @param query Term you searched for
     * @return List(String) of suggestions if solr is able to provide some
     * @throws SolrSearcherException
     */
    public List<String> getSpellcheckSuggestions(String query) throws SolrSearcherException {
        if (query!= null && !query.isEmpty()) {
            return solrConverter.getSpellcheckSuggestions(query);
        }
        return null;
    }

    /**
     * Returns one specific Entry by DbId
     * @param id StId or DbId
     * @return Entry Object
     */
    public EnrichedEntry getEntryById(String id) throws EnricherException, SolrSearcherException {
        if (id != null && !id.isEmpty()) {
            IEnricher enricher = new Enricher(host, currentDatabase, user, password, port);
            if (id.toUpperCase().contains("REACT_")) {
                Entry entry = solrConverter.getEntryById(id.split("\\.")[0]);
                if (entry!= null) {
                    return enricher.enrichEntry(entry.getDbId());
                }
            } else {
                return enricher.enrichEntry(id);
            }
        }
        return null;
    }

    /**
     * Returns one specific Entry by DbId
     * @param id StId or DbId
     * @return Entry Object
     */
    public EnrichedEntry getEntryById(Integer version, String id) throws EnricherException, SolrSearcherException {

        IEnricher enricher = new Enricher(host,  database + version, user, password, port);
        if (id.toUpperCase().contains("REACT_")) {
            Entry entry = solrConverter.getEntryById(id.split("\\.")[0]);
            if (entry!=null) {
                return enricher.enrichEntry(entry.getDbId());
            }
        } else {
            return enricher.enrichEntry(id);
        }
        return null;
    }

    /**
     * This Method gets multiple entries for a specific query while considering the filter information
     * the entries will be returned grouped into types and sorted by relevance (depending on the chosen solr properties)
     * @param queryObject QueryObject (query, species, types, keywords, compartments, start, rows)
     *                    start specifies the starting point (offset) and rows the amount of entries returned in total
     * @return GroupedResult
     * @throws SolrSearcherException
     */
    public GroupedResult getEntries(Query queryObject, Boolean cluster) throws SolrSearcherException {
        if (cluster) {
            return solrConverter.getClusteredEntries(queryObject);
        } else {
            return solrConverter.getEntries(queryObject);
        }
    }
}