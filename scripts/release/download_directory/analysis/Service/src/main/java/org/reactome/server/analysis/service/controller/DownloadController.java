package org.reactome.server.analysis.service.controller;

import com.wordnik.swagger.annotations.*;
import org.reactome.server.analysis.service.helper.AnalysisHelper;
import org.reactome.server.analysis.service.helper.DownloadHelper;
import org.reactome.server.analysis.service.result.AnalysisStoredResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Controller
@Api(value = "download", description = "Retrieve downloadable files in CSV format", position = 4)
@RequestMapping(value = "/download")
public class DownloadController {
    
    @Autowired
    private AnalysisHelper controller;

    @ApiOperation(value = "Downloads all hit pathways for a given analysis",
                  notes = "The results are filtered by the selected resource. The filename is the one to be suggested in the download window.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No result corresponding to the token was found"),
            @ApiResponse(code = 410, message = "Result deleted due to a new data release")})
    @RequestMapping(value = "/{token}/pathways/{resource}/{filename}.csv", method = RequestMethod.GET, produces = "text/csv" )
    @ResponseBody
    public FileSystemResource downloadHitPathways( @ApiParam(name = "token", required = true, value = "The token associated with the data to download")
                                                  @PathVariable String token,
                                                   @ApiParam(name = "resource", value = "the preferred resource", required = true, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,NCBI_PROTEIN,EMBL,COMPOUND")
                                                  @PathVariable String resource,
                                                   @ApiParam(name = "filename", value = "the file name for the downloaded information", required = true, defaultValue = "result")
                                                  @PathVariable String filename) throws IOException {
        AnalysisStoredResult asr = this.controller.getFromToken(token);
        return DownloadHelper.getHitPathwaysCVS(filename, asr, resource);
    }

    @ApiOperation(value = "Downloads those identifiers found for a given analysis and a certain resource",
                  notes = "The identifiers are filtered by the selected resource. The filename is the one to be suggested in the download window.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No result corresponding to the token was found"),
            @ApiResponse(code = 410, message = "Result deleted due to a new data release")})
    @RequestMapping(value = "/{token}/entities/found/{resource}/{filename}.csv", method = RequestMethod.GET, produces = "text/csv" )
    @ResponseBody
    public FileSystemResource downloadMappingResult( @ApiParam(name = "token", required = true, value = "The token associated with the data to download")
                                                    @PathVariable String token,
                                                     @ApiParam(name = "resource", value = "the preferred resource", required = true, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,NCBI_PROTEIN,EMBL,COMPOUND")
                                                    @PathVariable String resource,
                                                     @ApiParam(name = "filename", value = "the file name for the downloaded information", required = true, defaultValue = "result")
                                                    @PathVariable String filename) throws IOException {
        AnalysisStoredResult asr = this.controller.getFromToken(token);
        return DownloadHelper.getIdentifiersFoundMappingCVS(filename, asr, resource);
    }

    @ApiOperation(value = "Downloads a list of the not found identifiers",
                  notes = "Those identifiers from the user sample that are not present up to the current data version. The filename is the one to be suggested in the download window.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No result corresponding to the token was found"),
            @ApiResponse(code = 410, message = "Result deleted due to a new data release")})
    @RequestMapping(value = "/{token}/entities/notfound/{filename}.csv", method = RequestMethod.GET, produces = "text/csv" )
    @ResponseBody
    public FileSystemResource downloadNotFound( @ApiParam(name = "token", required = true, value = "The token associated with the data to download")
                                   @PathVariable String token,
                                    @ApiParam(name = "filename", value = "the file name for the downloaded information", required = true, defaultValue = "result")
                                   @PathVariable String filename) throws IOException {
        AnalysisStoredResult asr = this.controller.getFromToken(token);
        return DownloadHelper.getNotFoundIdentifiers(filename, asr);
    }

}
