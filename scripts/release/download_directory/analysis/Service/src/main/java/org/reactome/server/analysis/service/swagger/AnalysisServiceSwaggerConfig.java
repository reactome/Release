package org.reactome.server.analysis.service.swagger;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.wordnik.swagger.model.ApiInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Configuration
@EnableSwagger
public class AnalysisServiceSwaggerConfig { // extends SpringSwaggerConfig {

    private SpringSwaggerConfig springSwaggerConfig;

    @Autowired
    public void setSpringSwaggerConfig(SpringSwaggerConfig springSwaggerConfig) {
        this.springSwaggerConfig = springSwaggerConfig;
    }

    @Bean //Don't forget the @Bean annotation
    public SwaggerSpringMvcPlugin customImplementation(){
        return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
                .apiInfo(apiInfo())
                .includePatterns(".*");
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "Pathway Analysis Service",
                "Provides an API for pathway over-representation and expression analysis as well as species comparison tool",
                "/pages/analysisservice/terms", //Not showing the link (deleted by hand, please edit swagger-ui.js to take it back)
                "help@reactome.org",
                "Creative Commons Attribution 3.0 Unported License",
                "http://creativecommons.org/licenses/by/3.0/legalcode"
        );
        return apiInfo;
    }
}