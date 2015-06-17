package org.reactome.server.statistics.entrypoint;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This controller has been created to serve the landing page either in "/" or "/index.html"
 * We need to use the jsp parser because header and footer are dynamically generated by
 * HeaderFooterCacher class and it includes jsp directives in those files.
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Controller
public class EntryPointController {

    @RequestMapping(value = {"/", "/index.html"}, method = RequestMethod.GET)
//    @ApiIgnore //Swagger will NOT include this method in the documentation
    public String entryPoint() {
        return "index";
    }
}
