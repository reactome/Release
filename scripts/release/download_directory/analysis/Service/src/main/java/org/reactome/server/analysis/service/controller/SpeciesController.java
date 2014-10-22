package org.reactome.server.analysis.service.controller;

import com.wordnik.swagger.annotations.*;
import org.reactome.server.analysis.core.model.SpeciesNodeFactory;
import org.reactome.server.analysis.service.helper.AnalysisHelper;
import org.reactome.server.analysis.service.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Api(value = "species", description= "Species comparison", position = 3)
@RequestMapping(value = "/species")
public class SpeciesController {

    @Autowired
    private AnalysisHelper controller;

    @ApiOperation(value = "Compares Homo sapiens to the specified species",
                  notes = "Use page and pageSize to reduce the amount of data retrieved. Use sortBy and order to sort the result by your " +
                          "preferred option. The resource field will filter the results to show only those corresponding to the preferred " +
                          "molecule type (TOTAL includes all the different molecules type)")
    @ApiResponses({@ApiResponse( code = 404, message = "Species identifier does not match with any of the species in the current data" )})
    @RequestMapping(value = "/homoSapiens/{species}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public AnalysisResult compareHomoSapiensTo( @ApiParam(name = "species", required = true, value = "The dbId of the species to compare to")
                                              @PathVariable Long species,
                                               @ApiParam(name = "pageSize", value = "pathways per page", defaultValue = "20")
                                              @RequestParam(required = false) Integer pageSize,
                                               @ApiParam(name = "page", value = "page number", defaultValue = "1")
                                              @RequestParam(required = false) Integer page,
                                               @ApiParam(name = "sortBy", value = "how to sort the result", required = false, defaultValue = "ENTITIES_PVALUE", allowableValues = "NAME,TOTAL_ENTITIES,TOTAL_REACTIONS,FOUND_ENTITIES,FOUND_REACTIONS,ENTITIES_RATIO,ENTITIES_PVALUE,ENTITIES_FDR,REACTIONS_RATIO")
                                              @RequestParam(required = false) String sortBy,
                                               @ApiParam(name = "order", value = "specifies the order", required = false, defaultValue = "ASC", allowableValues = "ASC,DESC")
                                              @RequestParam(required = false) String order,
                                               @ApiParam(name = "resource", value = "the resource to sort", required = false, defaultValue = "TOTAL", allowableValues = "TOTAL,UNIPROT,ENSEMBL,CHEBI,NCBI_PROTEIN,EMBL,COMPOUND")
                                              @RequestParam(required = false, defaultValue = "TOTAL") String resource) {
        Long from = SpeciesNodeFactory.getHumanNode().getSpeciesID(); //For the time being let's do only human ;)
        return controller.compareSpecies(from, species).getResultSummary(sortBy, order, resource, pageSize, page);
    }


}

