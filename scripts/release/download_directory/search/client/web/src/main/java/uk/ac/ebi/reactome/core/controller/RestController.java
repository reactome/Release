package uk.ac.ebi.reactome.core.controller;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.reactome.core.businesslogic.exception.SearchServiceException;
import uk.ac.ebi.reactome.core.businesslogic.service.SearchService;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.exception.ErrorInfo;
import uk.ac.ebi.reactome.core.model.facetting.FacetMap;
import uk.ac.ebi.reactome.core.model.query.Query;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.GroupedResult;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * Converts a Solr QueryResponse into Objects provided by Project Models
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
@Controller
@RequestMapping("/json")
class RestController {

    private static final Logger logger = Logger.getLogger(WebController.class);
    private final SearchService searchService;

    public RestController(SearchService searchService) {
        this.searchService = searchService;

    }

    //    http://localhost:8080/search_service/detail/REACT_578.2
    @RequestMapping(value="/detail/{id:.*}", method = RequestMethod.GET)
    public @ResponseBody EnrichedEntry getEntry(@PathVariable String id) throws Exception {
         return searchService.getEntryById(id);
    }

    @RequestMapping(value="/detail/v{version}/{id:.*}", method = RequestMethod.GET)
    public @ResponseBody EnrichedEntry getEntryByVersion(@PathVariable String id,
                                                         @PathVariable Integer version) throws Exception {
        return searchService.getEntryById(version, id);
    }

    @RequestMapping(value = "/spellcheck", method = RequestMethod.GET)
    public  @ResponseBody List<String> spellcheckSuggestions(@RequestParam ( required = true ) String query) throws SolrSearcherException {
        return searchService.getSpellcheckSuggestions(query);
    }

    // http://localhost:8080/json/suggester?query=apop
    @RequestMapping(value = "/suggest", method = RequestMethod.GET)
    public  @ResponseBody List<String> suggesterSuggestions(@RequestParam ( required = true ) String query) throws SolrSearcherException {
        return searchService.getAutocompleteSuggestions(query);
    }

    //localhost:8080/json/facet
    @RequestMapping(value = "/facet", method = RequestMethod.GET)
    public  @ResponseBody FacetMap facet() throws SolrSearcherException {
            return searchService.getTotalFacetingInformation();
    }

    //http://localhost:8080/json/facet_query?query=apoptosis&species=%22Homo%20sapiens%22&species=%22Bos%20taurus%22
    @RequestMapping(value = "/facet_query", method = RequestMethod.GET)
    public  @ResponseBody FacetMap facet_type(@RequestParam ( required = true ) String query,
                                              @RequestParam ( required = false ) List<String> species,
                                              @RequestParam ( required = false ) List<String> types,
                                              @RequestParam ( required = false ) List<String> compartments,
                                              @RequestParam ( required = false ) List<String> keywords ) throws SolrSearcherException {
            Query queryObject = new Query(query, species, types, compartments, keywords);
            return searchService.getFacetingInformation(queryObject);
    }

    //http://localhost:8080/json/search?query=apoptosis&species=Homo%20sapiens,%20Bos%20taurus
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public @ResponseBody GroupedResult getResult (@RequestParam( required = true ) String query,
                                            @RequestParam ( required = false ) List<String> species,
                                            @RequestParam ( required = false ) List<String> types,
                                            @RequestParam ( required = false ) List<String> compartments,
                                            @RequestParam ( required = false ) List<String> keywords,
                                            @RequestParam ( required = false ) Boolean cluster,
                                            @RequestParam ( required = false ) Integer page,
                                            @RequestParam ( required = false ) Integer rows ) throws SolrSearcherException {
        Query queryObject = new Query(query, species,types,compartments,keywords,page, rows);
        return searchService.getEntries(queryObject, cluster);
    }

    /**
     * Overwrites the Global Exception Handler
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SolrSearcherException.class)
    @ResponseBody
    ErrorInfo handleSolrException(HttpServletRequest req, SolrSearcherException e) {
        logger.error(e);
        return new ErrorInfo("SolrService Exception occurred", req.getRequestURL(), e);
    }
    /**
     * Overwrites the Global Exception Handler
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SearchServiceException.class)
    @ResponseBody
    ErrorInfo handleServiceException(HttpServletRequest req, SearchServiceException e) {
        logger.error(e);
        return new ErrorInfo("SearchService Exception occurred", req.getRequestURL(), e);
    }
    /**
     * Overwrites the Global Exception Handler
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(EnricherException.class)
    @ResponseBody
    ErrorInfo handleEnricherException(HttpServletRequest req, EnricherException e) {
        logger.error(e);
        return new ErrorInfo("Enricher Exception occurred", req.getRequestURL(), e);
    }
}