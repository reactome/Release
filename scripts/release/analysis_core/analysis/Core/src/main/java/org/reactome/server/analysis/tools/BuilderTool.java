package org.reactome.server.analysis.tools;

import com.martiansoftware.jsap.*;
import org.apache.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.GKInstance2ModelObject;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.server.Main;
import org.reactome.server.analysis.tools.components.filter.AnalysisBuilder;
import org.reactome.server.analysis.util.FileUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BuilderTool {

    private static Logger logger = Logger.getLogger(BuilderTool.class.getName());
    public static boolean VERBOSE;


    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                BuilderTool.class.getName(),
                "Provides a set of tools for the pathway analysis and species comparison",
                new Parameter[] {
                        new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                                "The tool to use. Options: " + Main.Tool.getOptions()) //WE DO NOT TAKE INTO ACCOUNT TOOL HERE ANY MORE
                        ,new FlaggedOption( "host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                                "The database host")
                        ,new FlaggedOption( "database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                                "The reactome database name to connect to")
                        ,new FlaggedOption( "username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                                "The database user")
                        ,new FlaggedOption( "password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                                "The password to connect to the database")
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                                "The file where the results are written to." )
                        ,new QualifiedSwitch( "verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                                "Requests verbose output." )
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

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");
        GKInstance2ModelObject converter = context.getBean(GKInstance2ModelObject.class);
        DatabaseObjectFactory.initializeFactory(dba, converter);

        String fileName = config.getString("output");
        FileUtil.checkFileName(fileName);

        VERBOSE = config.getBoolean("verbose");

        logger.trace("Starting the data container creation...");
        long start = System.currentTimeMillis();
        AnalysisBuilder builder = context.getBean(AnalysisBuilder.class);
        builder.build(dba, fileName);
        long end = System.currentTimeMillis();
        logger.trace(String.format("Data container creation finished in %d minutes", Math.round((end - start) / 60000L)));
    }


}
