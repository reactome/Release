package uk.ac.ebi.reactome.core.controller;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.reactome.core.businesslogic.service.SearchService;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.facetting.FacetMap;
import uk.ac.ebi.reactome.core.model.query.Query;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.GroupedResult;
import uk.ac.ebi.reactome.core.model.result.Result;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring WEB Controller
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
@SuppressWarnings("SameReturnValue")
@Controller
@RequestMapping("")
class WebController {

    private final SearchService searchService;
    private static final int rowCount = 30;

    private static final String SPECIES_FACET         =  "species_facet";
    private static final String TYPES_FACET           =  "type_facet";
    private static final String KEYWORDS_FACET        =  "keyword_facet";
    private static final String COMPARTMENTS_FACET    =  "compartment_facet";

    private static final String Q               =  "q";
    private static final String SPECIES         =  "species";
    private static final String TYPES           =  "types";
    private static final String KEYWORDS        =  "keywords";
    private static final String COMPARTMENTS    =  "compartments";

    private static final String ENTRY           =  "entry";
    private static final String GROUPED_RESULT  =  "groupedResult";
    private static final String SUGGESTIONS     =  "suggestions";

    private static final String PAGE            =  "page";
    private static final String MAX_PAGE        =  "maxpage";
    private static final String CLUSTER         =  "cluster";

    private static final String TITLE =  "title";

    // Autowired annotation not necessary for Constructor injection
    public WebController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * entry point (for testing)
     * @return empty page
     */
    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public String entryPoint () {
        return "ebistart";
    }

    /**
     * Method for autocompletion
     * @param tagName query snippet to be autocompleted
     * @return List of Suggestions
     * @throws SolrSearcherException
     */
    @RequestMapping(value = "/getTags", method = RequestMethod.GET)
    public @ResponseBody List<String> getTags(@RequestParam String tagName) throws SolrSearcherException {
        return searchService.getAutocompleteSuggestions(tagName);
    }

    /**
     * Loads data for advanced view and displays advanced view
     * @param model SpringModel
     * @return Advanced view
     * @throws SolrSearcherException
     */
    @RequestMapping(value = "/advanced", method = RequestMethod.GET)
    public String gotoAdv (ModelMap model) throws SolrSearcherException {
        FacetMap facetMap = searchService.getTotalFacetingInformation();
        model.addAttribute(SPECIES_FACET,         facetMap.getSpeciesFacet());
        model.addAttribute(TYPES_FACET,           facetMap.getTypeFacet());
        model.addAttribute(KEYWORDS_FACET,        facetMap.getKeywordFacet());
        model.addAttribute(COMPARTMENTS_FACET,    facetMap.getCompartmentFacet());
        model.addAttribute(TITLE,    "advanced Search");
        return "ebiadvanced";
    }

    /**
     * Shows detailed information of an entry
     * @param id StId or DbId
     * @param version Reactome Database version
//     * @param q,species,types,compartments,keywords parameters to save existing query and facets
     * @param model SpringModel
     * @return Detailed page
     * @throws EnricherException
     * @throws SolrSearcherException
     */
    @RequestMapping(value = "/detail/v{version}/{id:.*}", method = RequestMethod.GET)
    public String detailVersion (@PathVariable String id,
                                 @PathVariable Integer version,
//                                 @RequestParam ( required = false ) String q,
//                                 @RequestParam ( required = false ) String species,
//                                 @RequestParam ( required = false ) String types,
//                                 @RequestParam ( required = false ) String compartments,
//                                 @RequestParam ( required = false ) String keywords,
                                 ModelMap model) throws EnricherException, SolrSearcherException {
//        model.addAttribute(Q, checkOutputIntegrity(q));
//        model.addAttribute(SPECIES, checkOutputIntegrity(species));
//        model.addAttribute(TYPES, checkOutputIntegrity(types));
//        model.addAttribute(COMPARTMENTS, checkOutputIntegrity(compartments));
//        model.addAttribute(KEYWORDS, checkOutputIntegrity(keywords));
        EnrichedEntry entry = searchService.getEntryById(version, id);
        if (entry!= null) {
        model.addAttribute(ENTRY, entry);
        model.addAttribute(TITLE, entry.getName() + " (" + entry.getSpecies() + ")" );
        return "detail";
    } else {
        return "noresultsfound";
    }
    }

    /**
     * Shows detailed information of an entry
     * @param id StId or DbId
//     * @param q,species,types,compartments,keywords parameters to save existing query and facets
     * @param model SpringModel
     * @return Detailed page
     * @throws EnricherException
     * @throws SolrSearcherException
     */
    @RequestMapping(value = "/detail/{id:.*}", method = RequestMethod.GET)
    public String detail(@PathVariable String id,
//                         @RequestParam ( required = false ) String q,
//                         @RequestParam ( required = false ) String species,
//                         @RequestParam ( required = false ) String types,
//                         @RequestParam ( required = false ) String compartments,
//                         @RequestParam ( required = false ) String keywords,
                         ModelMap model)  throws EnricherException, SolrSearcherException {
//        model.addAttribute(Q, checkOutputIntegrity(q));
//        model.addAttribute(SPECIES, checkOutputIntegrity(species));
//        model.addAttribute(TYPES, checkOutputIntegrity(types));
//        model.addAttribute(COMPARTMENTS, checkOutputIntegrity(compartments));
//        model.addAttribute(KEYWORDS, checkOutputIntegrity(keywords));
        EnrichedEntry entry = searchService.getEntryById(id);
        if (entry!= null) {
            model.addAttribute(ENTRY, entry);
            model.addAttribute(TITLE, entry.getName());
            return "detail";
        } else {
            return "noresultsfound";
        }
    }

//    quick and ugly fix
@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="IOException occurred")
    @RequestMapping(value = "/query/", method = RequestMethod.GET)
    public void error () {
//        return "../../resources/404.jas";
    }

    /**
     * spellcheck has to be applied after faceting search because dictionary can not contain 100% all index info
     * @param q,species,types,compartments,keywords parameters to save existing query and facets
     * @param page page number
     * @param model SpringModel
     * @return main search result page
     * @throws SolrSearcherException
     */
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public String search (@RequestParam ( required = true ) String q,
                          @RequestParam ( required = false ) List<String> species,
                          @RequestParam ( required = false ) List<String> types,
                          @RequestParam ( required = false ) List<String> keywords,
                          @RequestParam ( required = false ) List<String> compartments,
                          @RequestParam ( required = false ) Boolean cluster,
                          @RequestParam ( required = false ) Integer page,
                          ModelMap model) throws SolrSearcherException {
        if (q!= null && !q.isEmpty()) {
            q = filterQuery(q);
            if (cluster == null) {
                cluster = false;
            }
            if (page==null || page == 0) {
                page = 1;
            }

            model.addAttribute(Q, checkOutputIntegrity(q));
            model.addAttribute(TITLE, "Search results for " + q);
            model.addAttribute(SPECIES, checkOutputListIntegrity(species));
            model.addAttribute(TYPES, checkOutputListIntegrity(types));
            model.addAttribute(COMPARTMENTS, checkOutputListIntegrity(compartments));
            model.addAttribute(KEYWORDS, checkOutputListIntegrity(keywords));
            model.addAttribute(CLUSTER, cluster);
            Query queryObject = new Query(q, species,types,compartments,keywords);
            model.addAttribute(PAGE, page);
            FacetMap facetMap = searchService.getFacetingInformation(queryObject);
            // Faceting information is used to determine if the query with the currently selected filters
            // will return any results. If nothing is found, all the selected filters will be removed and
            // another query will be sent to Solr
            if (facetMap != null && facetMap.getTotalNumFount()>0){
                model.addAttribute(SPECIES_FACET,         facetMap.getSpeciesFacet());
                model.addAttribute(TYPES_FACET,           facetMap.getTypeFacet());
                model.addAttribute(KEYWORDS_FACET,        facetMap.getKeywordFacet());
                model.addAttribute(COMPARTMENTS_FACET,    facetMap.getCompartmentFacet());
                Integer typeCount = getTypeCount(types, facetMap);
                if (typeCount!=0) {
                    Integer rows = rowCount;
                    if (cluster) {
                        rows = rowCount / typeCount;
                    }
                    queryObject.setStart(rows * (page - 1));
                    queryObject.setRows(rows);
                    GroupedResult groupedResult = searchService.getEntries(queryObject, cluster);
                    double resultCount = getHighestResultCount(groupedResult);
                    model.addAttribute(MAX_PAGE, (int) Math.ceil(resultCount / rows));
                    model.addAttribute(GROUPED_RESULT, groupedResult);
                }
                return "ebisearcher";
            } else {
                facetMap = searchService.getFacetingInformation(new Query(q,null,null,null,null));
                if (facetMap != null && facetMap.getTotalNumFount()>0){
                    model.addAttribute(SPECIES_FACET,         facetMap.getSpeciesFacet());
                    model.addAttribute(TYPES_FACET,           facetMap.getTypeFacet());
                    model.addAttribute(KEYWORDS_FACET,        facetMap.getKeywordFacet());
                    model.addAttribute(COMPARTMENTS_FACET,    facetMap.getCompartmentFacet());
                    Integer typeCount = getTypeCount(types, facetMap);
                    if (typeCount!=0) {
                        Integer rows = rowCount;
                        if (cluster) {
                            rows = rowCount / typeCount;
                        }
                        queryObject.setStart(rows * (page - 1));
                        queryObject.setRows(rows);
                        GroupedResult groupedResult = searchService.getEntries(queryObject,cluster);
                        double resultCount = getHighestResultCount(groupedResult);
                        model.addAttribute(MAX_PAGE, (int) Math.ceil(resultCount / rows));
                        model.addAttribute(GROUPED_RESULT, groupedResult);
                    }
                    return "ebisearcher";
                } else {
                    // Generating spellcheck suggestions if no faceting informatioon was found, while using no filters
                    model.addAttribute(SUGGESTIONS, searchService.getSpellcheckSuggestions(q));
                }
            }
        }
        return "noresultsfound";
    }

    /**
     * Returns the highest result number for the different groups
     * @param groupedResult result set
     * @return double highest result number
     */
    private double getHighestResultCount(GroupedResult groupedResult){
        double max = 0;
        for (Result result : groupedResult.getResults()) {
            if (max<result.getEntriesCount()) {
                max = result.getEntriesCount();
            }
        }
        return max;
    }

    /**
     * Returns either selected types size or available types size
     * @param types selected types
     * @param facetMap available types
     * @return integer
     */
    private int getTypeCount (List<String>types, FacetMap facetMap){
        if (types!= null) {
            return types.size();
        } else {
            return facetMap.getTypeFacet().getAvailable().size();
        }
    }

    private String checkOutputIntegrity (String output) {
        if (output != null && !output.isEmpty()) {
            output = output.replaceAll("<", "<").replaceAll(">", ">");
            output = output.replaceAll("eval\\((.*)\\)", "");
            output = output.replaceAll("[\\\"\\\'][\\s]*((?i)javascript):(.*)[\\\"\\\']", "\"\"");
            output = output.replaceAll("((?i)script)", "");
            return Jsoup.clean(output, Whitelist.basic());
        }
        return null;
    }
    private List<String> checkOutputListIntegrity(List<String> list) {
        if (list!=null && !list.isEmpty()) {
            List<String> checkedList = new ArrayList<String>();
            for (String output : list) {
                checkedList.add(checkOutputIntegrity(output));
            }
            return checkedList;
        }
        return null;
    }
    private String filterQuery(String q){
        //Massaging the query parameter to remove dots in Reactome stable identifiers
        StringBuilder sb = new StringBuilder();
        for (String token : q.split("\\s+")) {
            if(token.toUpperCase().contains("REACT_")){
                sb.append(token.split("\\.")[0]);
            }else{
                sb.append(token);
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}