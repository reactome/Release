package uk.ac.ebi.reactome.core.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.reactome.core.businesslogic.exception.SearchServiceException;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.solr.core.exception.SolrSearcherException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Global exception handel controller
 * This controller will deal with all exceptions thrown by the other controllers if they don't treat them individually
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
@SuppressWarnings("SameReturnValue")
@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String EXCEPTION = "exception";
    private static final String URL = "url";

//    @ResponseStatus(value= HttpStatus.INTERNAL_SERVER_ERROR, reason="EnricherException occurred")
    @ExceptionHandler(EnricherException.class)
    public ModelAndView handleOtherExceptions(HttpServletRequest request, EnricherException e){
       ModelAndView model = new ModelAndView("error/generic_error");
        logger.info("Exception occurred:: URL="+request.getRequestURL());
        model.addObject(EXCEPTION, e);
        model.addObject(URL, request.getRequestURL());
        return model;
    }

//    @ResponseStatus(value= HttpStatus.INTERNAL_SERVER_ERROR, reason="SolrSearcherException occurred")
    @ExceptionHandler(SolrSearcherException.class)
    public ModelAndView handleSolrSearcherException(HttpServletRequest request, SolrSearcherException e){
        ModelAndView model = new ModelAndView("error/generic_error");
        logger.info("SolrSearcherException occurred:: URL="+request.getRequestURL());
        model.addObject(EXCEPTION, e);
        model.addObject(URL, request.getRequestURL());
        return model;
    }

//    @ResponseStatus(value= HttpStatus.INTERNAL_SERVER_ERROR, reason="SearchServiceException occurred")
    @ExceptionHandler(SearchServiceException.class)
    public ModelAndView handleSQLException(HttpServletRequest request, SearchServiceException e){
        ModelAndView model = new ModelAndView("error/generic_error");
        logger.info("SQLException occurred:: URL="+request.getRequestURL());
        model.addObject(EXCEPTION, e);
        model.addObject(URL, request.getRequestURL());
        return model;
    }

    @ResponseStatus(value= HttpStatus.NOT_FOUND, reason="IOException occurred")
    @ExceptionHandler(IOException.class)
    public void handleIOException(){
        logger.error("IOException handler executed");  //returning 404 error code
    }
}
