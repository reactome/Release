package uk.ac.ebi.reactome.solr.core.impl;

import org.apache.solr.client.solrj.response.*;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.model.facetting.FacetContainer;
import uk.ac.ebi.reactome.core.model.facetting.FacetList;
import uk.ac.ebi.reactome.core.model.facetting.FacetMap;
import uk.ac.ebi.reactome.core.model.query.Query;
import uk.ac.ebi.reactome.core.model.result.Entry;
import uk.ac.ebi.reactome.core.model.result.GroupedResult;
import uk.ac.ebi.reactome.core.model.result.Result;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;
import uk.ac.ebi.reactome.solr.core.io.ISolrConverter;

import java.util.*;

/**
 * Converts a Solr QueryResponse into Objects provided by Project Models
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class SolrConverter implements ISolrConverter {

    private static final Logger logger = LoggerFactory.getLogger(SolrConverter.class);
    private final SolrSearcher solrSearcher;


    private static final String DB_ID                 =  "dbId";
    private static final String ST_ID                 =  "stId";
    private static final String NAME                  =  "name";

    private static final String SPECIES               =  "species";
    private static final String SPECIES_FACET         =  "species_facet";
    private static final String TYPES                 =  "type_facet";
    private static final String KEYWORDS              =  "keywords_facet";
    private static final String COMPARTMENTS          =  "compartment_facet";

    private static final String SUMMATION             =  "summation";
    private static final String INFERRED_SUMMATION    =  "inferredSummation";
    private static final String REFERENCE_NAME        =  "referenceName";
    private static final String REFERENCE_IDENTIFIERS =  "referenceIdentifiers";

    private static final String IS_DISEASE            =  "isDisease";
    private static final String EXACT_TYPE            =  "exactType";

    private static final String DATABASE_NAME         =  "databaseName";
    private static final String REFERENCE_URL         =  "referenceURL";

    private static final String REGULATOR             =  "regulator";
    private static final String REGULATED_ENTITY      =  "regulatedEntity";
    private static final String REGULATOR_ID          =  "regulatorId";
    private static final String REGULATED_ENTITY_ID   =  "regulatedEntityId";

    /**
     * Constructor for Dependency Injection
     * @param solrSearcher Parameters set in spring xml
     */
    public SolrConverter(SolrSearcher solrSearcher) {
        this.solrSearcher = solrSearcher;
    }

    /**
     * Method for autocompletion
     * @param query String of the query parameter given
     * @return List(String) of Suggestions
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public List<String> getAutocompleteSuggestions(String query) throws SolrSearcherException {
        List<String> aux = new LinkedList<String>();
        if (query!= null && !query.isEmpty()) {
            aux = suggestionHelper(solrSearcher.getAutocompleteSuggestions(query));
        }

        List<String> rtn = new LinkedList<String>();
        if(aux!=null) {
            for (String q : aux) {
                if (solrSearcher.existsQuery(q)) {
                    rtn.add(q);
                }
            }
        }
        return rtn;
    }

    /**
     * Method for spellcheck and suggestions
     * @param query String of the query parameter given
     * @return List(String) of Suggestions
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public List<String> getSpellcheckSuggestions(String query) throws SolrSearcherException {
        List<String> aux = new LinkedList<String>();
        if (query!= null && !query.isEmpty()) {
            aux = suggestionHelper(solrSearcher.getSpellcheckSuggestions(query));
        }

        List<String> rtn = new LinkedList<String>();
        if(aux!=null) {
            for (String q : aux) {
                if (solrSearcher.existsQuery(q)) {
                    rtn.add(q);
                }
            }
        }
        return rtn;
    }

    /**
     * Method gets all faceting information for the fields: species, types, compartments, keywords
     * @return FacetMap
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public FacetMap getFacetingInformation () throws SolrSearcherException {
        return getFacetMap(solrSearcher.getFacetingInformation());
    }

    /**
     * Method gets Faceting Info considering Filter of other possible FacetFields
     * @param queryObject QueryObject (query, types, species, keywords, compartments)
     * @return FacetMap
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public FacetMap getFacetingInformation(Query queryObject) throws SolrSearcherException {
        return getFacetMap(solrSearcher.getFacetingInformation(queryObject), queryObject);
    }

    /**
     * Converts single SolrResponseEntry to Object Entry (Model)
     * @param id can be dbId of stId
     * @return Entry Object
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public Entry getEntryById(String id) throws SolrSearcherException {
        if (id != null && !id.isEmpty()) {
            QueryResponse response = solrSearcher.searchById(id);
            List<SolrDocument> solrDocuments = response.getResults();
            if (solrDocuments != null && !solrDocuments.isEmpty() && solrDocuments.get(0)!=null){
                return buildEntry(solrDocuments.get(0), null);
            }
        }
        logger.warn("no Entry found for this id" + id);
        return null;
    }

    /**
     * Converts Solr QueryResponse to GroupedResult
     * @param queryObject QueryObject (query, types, species, keywords, compartments, start, rows)
     * @return GroupedResponse
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public GroupedResult getClusteredEntries(Query queryObject) throws SolrSearcherException {

        if (queryObject != null && queryObject.getQuery()!= null && !queryObject.getQuery().isEmpty()) {
            QueryResponse queryResponse = solrSearcher.searchCluster(queryObject);
            if (queryResponse!= null) {
                return parseClusteredResponse(queryResponse);
            }
        }
        return null;
    }

    /**
     * Converts Solr QueryResponse to GroupedResult
     * @param queryObject QueryObject (query, types, species, keywords, compartments, start, rows)
     * @return GroupedResponse
     * @throws uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException
     */
    public GroupedResult getEntries(Query queryObject) throws SolrSearcherException {

        if (queryObject != null && queryObject.getQuery()!= null && !queryObject.getQuery().isEmpty()) {
            QueryResponse queryResponse = solrSearcher.search(queryObject);
            if (queryResponse!= null) {
                return parseResponse(queryResponse);
            }
        }
        return null;
    }

    private GroupedResult parseResponse(QueryResponse queryResponse){
        if (queryResponse!= null) {
            List<SolrDocument> solrDocuments = queryResponse.getResults();
            Map<String, Map<String, List<String>>> highlighting =  queryResponse.getHighlighting();
            List<Result> resultList = new ArrayList<Result>();
            List<Entry> entries = new ArrayList<Entry>();

            for (SolrDocument solrDocument : solrDocuments) {
                Entry entry = buildEntry(solrDocument, highlighting);
                entries.add(entry);
            }
            resultList.add(new Result(entries, "Results", queryResponse.getResults().getNumFound(), entries.size()));
            return new GroupedResult(resultList,solrDocuments.size(),1, (int) queryResponse.getResults().getNumFound());

        }
        return null;
    }

    /**
     * Helper Method to convert Solr faceting information to FacetMap considering the selected Filtering Options
     * @param response Solr QueryResponse
     * @param queryObject considers species, types keywords compartments
     * @return FacetMap
     */
    private FacetMap getFacetMap(QueryResponse response, Query queryObject)  {

        if (response!= null && queryObject!=null) {
            FacetMap facetMap = new FacetMap();
            facetMap.setTotalNumFount(response.getResults().getNumFound());
            facetMap.setSpeciesFacet(getFacets(response.getFacetField(SPECIES_FACET), queryObject.getSpecies()));
            facetMap.setTypeFacet(getFacets(response.getFacetField(TYPES), queryObject.getTypes()));
            facetMap.setKeywordFacet(getFacets(response.getFacetField(KEYWORDS), queryObject.getKeywords()));
            facetMap.setCompartmentFacet(getFacets(response.getFacetField(COMPARTMENTS), queryObject.getCompartment()));
            return facetMap;
        }
        return null;
    }

    /**
     * Helper Method separates Faceting information into selected and available facets
     * @param facetField Solr FacetField (of a selected field)
     * @param selectedItems List of selected Strings of the queryObject
     * @return FacetList
     */
    private FacetList getFacets (FacetField facetField, List<String> selectedItems) {
        if (facetField!=null) {
            List<FacetContainer> selected = new ArrayList<FacetContainer>();
            List<FacetContainer> available = new ArrayList<FacetContainer>();
            for (FacetField.Count field : facetField.getValues()) {
                if (selectedItems != null && selectedItems.contains(field.getName())) {
                    selected.add(new FacetContainer(field.getName(), field.getCount()));
                } else {
                    available.add(new FacetContainer(field.getName(), field.getCount()));
                }
            }
            return new FacetList(selected, available);
        }
        return null;
    }

    /**
     * Helper Method to convert Solr faceting information to FacetMap
     * @param queryResponse Solr QueryResponse
     * @return FacetMap
     */
    private FacetMap getFacetMap(QueryResponse queryResponse) {

        if (queryResponse!=null && queryResponse.getFacetFields()!=null && !queryResponse.getFacetFields().isEmpty()) {
            FacetMap facetMap = new FacetMap();
            List<FacetField> facetFields = queryResponse.getFacetFields();
            for (FacetField facetField : facetFields) {
                List<FacetContainer> available = new ArrayList<FacetContainer>();
                List<FacetField.Count> fields = facetField.getValues();
                for (FacetField.Count field : fields) {
                    available.add(new FacetContainer(field.getName(), field.getCount()));
                }
                if (facetField.getName().equals(SPECIES_FACET)) {
                    facetMap.setSpeciesFacet(new FacetList(available));
                } else if (facetField.getName().equals(TYPES)) {
                    facetMap.setTypeFacet(new FacetList(available));
                } else if (facetField.getName().equals(KEYWORDS)) {
                    facetMap.setKeywordFacet(new FacetList(available));
                } else if (facetField.getName().equals(COMPARTMENTS)) {
                    facetMap.setCompartmentFacet(new FacetList(available));
                }
            }
            return facetMap;
        }
        return null;
    }

    /**
     * Method for the construction of an entry from a SolrDocument, taking into account available highlighting information
     * @param solrDocument Solr Document
     * @param highlighting SOLRJ Object Map<String,Map<String, List<String>>> used for highlighting
     * @return Entry
     */
    private Entry buildEntry(SolrDocument solrDocument, Map<String,Map<String,List<String>>> highlighting) {
        if (solrDocument != null && !solrDocument.isEmpty()) {
            Entry entry = new Entry();
            entry.setDbId((String) solrDocument.getFieldValue(DB_ID));

            if (solrDocument.containsKey(ST_ID)) {
                entry.setStId((String) solrDocument.getFieldValue(ST_ID));
                entry.setId((String) solrDocument.getFieldValue(ST_ID));
            } else {
                entry.setId((String) solrDocument.getFieldValue(DB_ID));
            }

            entry.setExactType((String) solrDocument.getFieldValue(EXACT_TYPE));
            entry.setIsDisease((Boolean) solrDocument.getFieldValue(IS_DISEASE));
            //Only the first species is taken into account
            Collection species = solrDocument.getFieldValues(SPECIES);
            if(species!=null) {
                entry.setSpecies((String) species.toArray()[0]);
            }
            entry.setDatabaseName((String) solrDocument.getFieldValue(DATABASE_NAME));
            entry.setReferenceURL((String) solrDocument.getFieldValue(REFERENCE_URL));
            entry.setRegulatorId((String) solrDocument.getFieldValue(REGULATOR_ID));
            entry.setRegulatedEntityId((String) solrDocument.getFieldValue(REGULATED_ENTITY_ID));
            if (solrDocument.containsKey(COMPARTMENTS)) {
                Collection<Object> compartments = solrDocument.getFieldValues(COMPARTMENTS);
                List<String> list = new ArrayList<String>();
                for (Object compartment : compartments) {
                    list.add(compartment.toString());
                }
                entry.setCompartmentNames(list);
            }

            if (highlighting != null  && highlighting.containsKey(solrDocument.getFieldValue(DB_ID))) {
                setHighlighting(entry, solrDocument, highlighting.get(solrDocument.getFieldValue(DB_ID)));
            } else {
                entry.setName((String) solrDocument.getFieldValue(NAME));
                entry.setSummation((String) solrDocument.getFieldValue(SUMMATION));
                entry.setReferenceName((String) solrDocument.getFieldValue(REFERENCE_NAME));
                entry.setReferenceIdentifier(selectRightReferenceIdentifier(solrDocument));
                entry.setRegulator((String) solrDocument.getFieldValue(REGULATOR));
                entry.setRegulatedEntity((String) solrDocument.getFieldValue(REGULATED_ENTITY));
            }
            if (solrDocument.containsKey(INFERRED_SUMMATION)) {
                entry.setSummation((String) solrDocument.getFieldValue(INFERRED_SUMMATION));
            }

            return entry;
        }
        return null;
    }

    private String selectRightReferenceIdentifier(SolrDocument solrDocument){
        Collection<Object> list = solrDocument.getFieldValues(REFERENCE_IDENTIFIERS);
        if(list!=null) {
            String candidate = null;
            for (Object obj : list) {
                String str = (String) obj;
                candidate = candidate==null?str:candidate;
                if (!str.contains(":")) {
                    return str;
                }
            }
            return candidate;
        }
        return null;
    }

    /**
     * Helper Method that sets Highlighted snippets if they are available
     * @param entry Entry Object
     * @param solrDocument Solr Document used when there are no highlighting Values
     * @param snippets Map containing the Highlighted Strings
     */
    private void setHighlighting(Entry entry, SolrDocument solrDocument, Map<String, List<String>> snippets) {

        if (snippets.containsKey(NAME) && snippets.get(NAME) != null && !snippets.get(NAME).isEmpty()) {
            entry.setName(snippets.get(NAME).get(0));
        } else {
            entry.setName((String) solrDocument.getFieldValue(NAME));
        }
        if (snippets.containsKey(SUMMATION) && snippets.get(SUMMATION) != null && !snippets.get(SUMMATION).isEmpty()) {
            entry.setSummation(snippets.get(SUMMATION).get(0));
        } else {
            entry.setSummation((String) solrDocument.getFieldValue(SUMMATION));
        }
        if (snippets.containsKey(REFERENCE_NAME) && snippets.get(REFERENCE_NAME) != null && !snippets.get(REFERENCE_NAME).isEmpty()) {
            entry.setReferenceName(snippets.get(REFERENCE_NAME).get(0));
        } else {
            entry.setReferenceName((String) solrDocument.getFieldValue(REFERENCE_NAME));
        }
        entry.setReferenceIdentifier(selectRightHighlightingForReferenceIdentifiers(solrDocument, snippets));
        if (snippets.containsKey(REGULATOR) && snippets.get(REGULATOR) != null && !snippets.get(REGULATOR).isEmpty()) {
            entry.setRegulator(snippets.get(REGULATOR).get(0));
        } else {
            entry.setRegulator((String) solrDocument.getFieldValue(REGULATOR));
        }
        if (snippets.containsKey(REGULATED_ENTITY) && snippets.get(REGULATED_ENTITY) != null && !snippets.get(REGULATED_ENTITY).isEmpty()) {
            entry.setRegulatedEntity(snippets.get(REGULATED_ENTITY).get(0));
        } else {
            entry.setRegulatedEntity((String) solrDocument.getFieldValue(REGULATED_ENTITY));
        }
    }

    private String selectRightHighlightingForReferenceIdentifiers(SolrDocument solrDocument,  Map<String, List<String>> snippets){
        String candidate = null;
        if (snippets.containsKey(REFERENCE_IDENTIFIERS) && snippets.get(REFERENCE_IDENTIFIERS) != null && !snippets.get(REFERENCE_IDENTIFIERS).isEmpty()) {
            for (String snippet : snippets.get(REFERENCE_IDENTIFIERS)) {
                if(snippet.contains("highlighting")){
                    return snippet;
                }else{
                    candidate = candidate==null?snippet:candidate;
                }
            }
        } else {
            return selectRightReferenceIdentifier(solrDocument);
        }
        return candidate;
    }

    /**
     * Helper Method for converting a Solr Grouped Response into a Grouped Result (Model)
     * @param queryResponse Solr QueryResponse
     * @return GroupedResult
     */
    private GroupedResult parseClusteredResponse (QueryResponse queryResponse) {
        if (queryResponse!= null) {
            if (queryResponse.getGroupResponse() != null) {
                if (queryResponse.getGroupResponse().getValues() != null && !queryResponse.getGroupResponse().getValues().isEmpty()) {
                    if (queryResponse.getGroupResponse().getValues().get(0) != null) {
                        GroupCommand groupCommand = queryResponse.getGroupResponse().getValues().get(0);
                        List<Group> groups = groupCommand.getValues();
                        Map<String, Map<String, List<String>>> highlighting =  queryResponse.getHighlighting();
                        int rowCounter = 0;
                        List<Result> resultList = new ArrayList<Result>();
                        for (Group group : groups) {
                            SolrDocumentList solrDocumentList = group.getResult();
                            List<Entry> entries = new ArrayList<Entry>();
                            for (SolrDocument solrDocument : solrDocumentList) {
                                Entry entry = buildEntry(solrDocument, highlighting);
                                entries.add(entry);
                            }
                            resultList.add(new Result(entries, group.getGroupValue(), solrDocumentList.getNumFound(), entries.size()));
                            rowCounter += entries.size();
                        }
                        return new GroupedResult(resultList,rowCounter,groupCommand.getNGroups(), groupCommand.getMatches());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper Function for converting SolrCollatedResults
     * @param response Solr QueryResponse
     * @return List(String) of Suggestions
     */
    private List<String> suggestionHelper(QueryResponse response) {
        if (response!= null && response.getSpellCheckResponse() != null) {
            List<String> list = new ArrayList<String>();
            List<SpellCheckResponse.Collation> suggestions = response.getSpellCheckResponse().getCollatedResults();
            if (suggestions != null && !suggestions.isEmpty()) {
                for (SpellCheckResponse.Collation suggestion : suggestions) {
                    list.add(suggestion.getCollationQueryString());
                }
                return list;
            }
        }
        return null;
    }

}
