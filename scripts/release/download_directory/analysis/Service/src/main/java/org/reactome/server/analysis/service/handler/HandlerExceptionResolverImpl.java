package org.reactome.server.analysis.service.handler;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * By implementing the HandlerExceptionResolver we can check for different exceptions happened
 * during handler mapping or execution. Different actions can be developed depending of the
 * exception type.
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class HandlerExceptionResolverImpl implements org.springframework.web.servlet.HandlerExceptionResolver{

    private static Logger logger = Logger.getLogger(HandlerExceptionResolverImpl.class.getName());

    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {

        if(ex instanceof MaxUploadSizeExceededException) {
            response.setContentType("text/html");
            response.setStatus(HttpStatus.REQUEST_ENTITY_TOO_LARGE.value());

            try {
                Long maxSizeInBytes = ((MaxUploadSizeExceededException) ex).getMaxUploadSize();
                Integer mb = (int) Math.floor(maxSizeInBytes / 1024 / 1024);

                PrintWriter out = response.getWriter();
                out.println(
                        "Error: " + HttpStatus.REQUEST_ENTITY_TOO_LARGE.value() + "\n " +
                        "Message: Maximum upload size of " + mb + " MB per attachment exceeded"
                );
                return new ModelAndView();
            } catch (IOException e) {
                logger.error("Error writing to output stream", e);
            }
        }

        if(ex instanceof MultipartException){
            response.setContentType("text/html");
            response.setStatus(HttpStatus.BAD_REQUEST.value());

            try {
                PrintWriter out = response.getWriter();
                out.println(
                        "Error: " + HttpStatus.BAD_REQUEST.value() + "\n " +
                        "Message: " + ex.getMessage()
                );
                out.println(ex.getMessage());

                return new ModelAndView();
            } catch (IOException e) {
                logger.error("Error writing to output stream", e);
            }
        }

        ex.printStackTrace();

        return null;
    }
}