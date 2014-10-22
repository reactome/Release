package org.reactome.server.analysis.tools;

import com.martiansoftware.jsap.*;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.Main;
import org.reactome.server.analysis.core.data.AnalysisData;
import org.reactome.server.analysis.core.model.resource.ResourceFactory;
import org.reactome.server.analysis.tools.components.exporter.Exporter;
import org.reactome.server.analysis.util.FileUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExporterTool {


    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                BuilderTool.class.getName(),
                "Provides a set of tools for the pathway analysis and species comparison",
                new Parameter[] {
                        new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                            "The tool to use. Options: " + Main.Tool.getOptions()) //WE DO NOT TAKE INTO ACCOUNT TOOL HERE ANY MORE
                        ,new FlaggedOption( "host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                            "The database host")
                        ,new FlaggedOption( "resource", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'r', "resource",
                            "The resource to export data from (Options [ "+ getAvailableResources() + " ]")
                        ,new FlaggedOption( "database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                            "The reactome database name to connect to")
                        ,new FlaggedOption( "username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                            "The database user")
                        ,new FlaggedOption( "password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                            "The password to connect to the database")
                        ,new FlaggedOption( "input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'i', "input",
                            "The file containing the data structure for the analysis." )
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                            "The file where the results are written to.")
                        ,new QualifiedSwitch( "all", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'a', "all",
                            "Exports data for all the levels in the hierarchy." )
                }
        );

        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

        MySQLAdaptor dba = new MySQLAdaptor(
                config.getString("host"),
                config.getString("database"),
                config.getString("username"),
                config.getString("password")
        );

        String fileName = config.getString("output");
        FileUtil.checkFileName(fileName);

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");

        String resource = config.getString("resource");
        ResourceFactory.MAIN mainResource = ResourceFactory.getMainResource(resource);
        if(mainResource!=null){
            //Initializing Analysis Data  *** IMPORTANT ***
            AnalysisData analysisData = context.getBean(AnalysisData.class);
            analysisData.setFileName(config.getString("input"));

            Boolean all = config.getBoolean("all");
            Exporter exporter = context.getBean(Exporter.class);
            exporter.export(dba, mainResource, fileName, all);

        }else{
            System.err.println("Invalid resource " + resource + ". Available options are: " + getAvailableResources());
        }
    }

    private static String getAvailableResources(){
        StringBuilder sb = new StringBuilder();
        for (ResourceFactory.MAIN main : ResourceFactory.MAIN.values()) {
            sb.append(" ").append(main.toString());
        }
        return sb.toString();
    }
}
