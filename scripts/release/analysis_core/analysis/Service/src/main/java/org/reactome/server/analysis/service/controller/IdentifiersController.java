package org.reactome.server.analysis.service.controller;

import com.wordnik.swagger.annotations.*;
import org.reactome.server.analysis.core.model.UserData;
import org.reactome.server.analysis.core.util.InputUtils;
import org.reactome.server.analysis.service.exception.BadRequestException;
import org.reactome.server.analysis.service.helper.AnalysisHelper;
import org.reactome.server.analysis.service.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Controller
@Api(value = "identifiers", description = "Queries for multiple identifiers", position = 1)
@RequestMapping(value = "/identifiers")
public class IdentifiersController {

    @Autowired
    private AnalysisHelper controller;

    @ApiOperation(value = "Analyse the post identifiers over the different species and projects the result to Homo Sapiens",
                  notes = "The projection is calculated by the orthologous slot in the Reactome database. Use page and pageSize " +
                          "to reduce the amount of data retrieved. Use sortBy and order to sort the result by your preferred option. " +
                          "The resource field will filter the results to show only those corresponding to the preferred molecule type " +
                          "(TOTAL includes all the different molecules type)")
    @ApiResponses({@ApiResponse( code = 400, message = "Bad request" )})
    @RequestMapping(value = "/projection", method = RequestMethod.POST, consumes = "text/plain",  produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostTextToHuman( @ApiParam(name = "input", required = true, value = "Identifiers to analyse followed by their expression (when applies)")
                                             @RequestBody String input,
                                              @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                             @RequestParam(required = false) Integer pageSize,
                                              @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                             @RequestParam(required = false) Integer page,
                                              @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                             @RequestParam(required = false) String sortBy,
                                              @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                             @RequestParam(required = false) String order,
                                              @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                             @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        if(input==null || input.isEmpty()) throw (new BadRequestException());
        UserData ud = InputUtils.getUserData(input);
        return controller.analyse(ud, true).getResultSummary(sortBy, order, resource, pageSize, page);
    }

    @ApiOperation(value = "Analyse the post identifiers over the different species",
                  notes = "Use page and pageSize to reduce the amount of data retrieved. Use sortBy and order to sort the result by your " +
                          "preferred option. The resource field will filter the results to show only those corresponding to the preferred " +
                          "molecule type (TOTAL includes all the different molecules type)")
    @ApiResponses({@ApiResponse( code = 400, message = "Bad request" )})
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "text/plain", produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostText( @ApiParam(name = "input", required = true, value = "Identifiers to analyse followed by their expression (when applies)")
                                      @RequestBody String input,
                                       @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                      @RequestParam(required = false) Integer pageSize,
                                       @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                      @RequestParam(required = false) Integer page,
                                       @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                      @RequestParam(required = false) String sortBy,
                                       @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                      @RequestParam(required = false) String order,
                                       @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                      @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        if(input==null || input.isEmpty()) throw (new BadRequestException());
        UserData ud = InputUtils.getUserData(input);
        return controller.analyse(ud, false).getResultSummary(sortBy, order, resource, pageSize, page);
    }

    @ApiOperation(value = "Analyse the identifiers in the file over the different species and projects the result to Homo Sapiens",
                  notes = "The projection is calculated by the orthologous slot in the Reactome database. Use page and pageSize " +
                          "to reduce the amount of data retrieved. Use sortBy and order to sort the result by your preferred option. " +
                          "The resource field will filter the results to show only those corresponding to the preferred molecule type " +
                          "(TOTAL includes all the different molecules type)")
    @ApiResponses({
            @ApiResponse( code = 413, message = "The file size is larger than the maximum configured size (10MB)"  ),
            @ApiResponse( code = 415, message = "Unsupported Media Type (only 'text/plain')" )})
    @RequestMapping(value = "/form/projection", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostFileToHuman( @ApiParam(name = "file", required = true, value = "A file with the data to be analysed")
                                             @RequestParam(required = true) MultipartFile file,
                                              @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                             @RequestParam(required = false) Integer pageSize,
                                              @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                             @RequestParam(required = false) Integer page,
                                              @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                             @RequestParam(required = false) String sortBy,
                                              @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                             @RequestParam(required = false) String order,
                                              @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                             @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        UserData ud = controller.getUserData(file);
        return controller.analyse(ud, true, file.getOriginalFilename()).getResultSummary(sortBy, order, resource, pageSize, page);
    }

    @ApiOperation(value = "Analyse the identifiers in the file over the different species",
                  notes = "Use page and pageSize to reduce the amount of data retrieved. Use sortBy and order to sort the result by your " +
                          "preferred option. The resource field will filter the results to show only those corresponding to the preferred " +
                          "molecule type (TOTAL includes all the different molecules type)")
    @ApiResponses({
            @ApiResponse( code = 413, message = "The file size is larger than the maximum configured size (10MB)"  ),
            @ApiResponse( code = 415, message = "Unsupported Media Type (only 'text/plain')" )})
    @RequestMapping(value = "/form", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostFile( @ApiParam(name = "file", required = true, value = "A file with the data to be analysed")
                                      @RequestParam(required = true) MultipartFile file,
                                       @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                      @RequestParam(required = false) Integer pageSize,
                                       @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                      @RequestParam(required = false) Integer page,
                                       @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                      @RequestParam(required = false) String sortBy,
                                       @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                      @RequestParam(required = false) String order,
                                       @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                      @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        UserData ud = controller.getUserData(file);
        return controller.analyse(ud, false, file.getOriginalFilename()).getResultSummary(sortBy, order, resource, pageSize, page);
    }

    @ApiOperation(value = "Analyse the identifiers contained in the provided url over the different species and projects the result to Homo Sapiens",
                  notes = "The projection is calculated by the orthologous slot in the Reactome database. Use page and pageSize " +
                          "to reduce the amount of data retrieved. Use sortBy and order to sort the result by your preferred option. " +
                          "The resource field will filter the results to show only those corresponding to the preferred molecule type " +
                          "(TOTAL includes all the different molecules type)")
    @ApiResponses({
            @ApiResponse( code = 413, message = "The file size is larger than the maximum configured size (10MB)"  ),
            @ApiResponse( code = 415, message = "Unsupported Media Type (only 'text/plain')" ),
            @ApiResponse( code = 422, message = "The provided URL is not processable" )})
    @RequestMapping(value = "/url/projection", method = RequestMethod.POST, consumes = "text/plain", produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostURLToHuman( @ApiParam(name = "url", required = true, value = "A URL pointing to the data to be analysed")
                                            @RequestBody String url,
                                             @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                            @RequestParam(required = false) Integer pageSize,
                                             @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                            @RequestParam(required = false) Integer page,
                                             @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                            @RequestParam(required = false) String sortBy,
                                             @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                            @RequestParam(required = false) String order,
                                             @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                            @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        UserData ud = controller.getUserDataFromURL(url);
        String fileName = controller.getFileNameFromURL(url);
        return controller.analyse(ud, true, fileName).getResultSummary(sortBy, order, resource, pageSize, page);
    }

    @ApiOperation(value = "Analyse the identifiers contained in the provided url over the different species",
                  notes = "Use page and pageSize to reduce the amount of data retrieved. Use sortBy and order to sort the result by your " +
                          "preferred option. The resource field will filter the results to show only those corresponding to the preferred " +
                          "molecule type (TOTAL includes all the different molecules type)")
    @ApiResponses({
            @ApiResponse( code = 413, message = "The file size is larger than the maximum configured size (10MB)"  ),
            @ApiResponse( code = 415, message = "Unsupported Media Type (only 'text/plain')" ),
            @ApiResponse( code = 422, message = "The provided URL is not processable" )})
    @RequestMapping(value = "/url", method = RequestMethod.POST, consumes = "text/plain", produces = "application/json")
    @ResponseBody
    public AnalysisResult getPostURL( @ApiParam(name = "url", required = true, value = "A URL pointing to the data to be analysed")
                                     @RequestBody String url,
                                      @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                     @RequestParam(required = false) Integer pageSize,
                                      @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                     @RequestParam(required = false) Integer page,
                                      @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                     @RequestParam(required = false) String sortBy,
                                      @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                     @RequestParam(required = false) String order,
                                      @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,MIRBASE,NCBI_PROTEIN,EMBL,COMPOUND")
                                     @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        UserData ud = controller.getUserDataFromURL(url);
        String fileName = controller.getFileNameFromURL(url);
        return controller.analyse(ud, false, fileName).getResultSummary(sortBy, order, resource, pageSize, page);
    }
}
