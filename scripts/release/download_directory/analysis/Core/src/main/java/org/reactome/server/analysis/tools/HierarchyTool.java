package org.reactome.server.analysis.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.Main;
import org.reactome.server.analysis.core.data.AnalysisData;
import org.reactome.server.analysis.tools.components.exporter.HierarchyExporter;
import org.reactome.server.analysis.util.FileUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class HierarchyTool {

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                BuilderTool.class.getName(),
                "Provides a set of tools for the pathway analysis and species comparison",
                new Parameter[] {
                        new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                            "The tool to use. Options: " + Main.Tool.getOptions()) //WE DO NOT TAKE INTO ACCOUNT TOOL HERE ANY MORE
                        ,new FlaggedOption( "type", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 't', "type",
                            "Type of export [DETAILS, RELATIONSHIP]")
                        ,new FlaggedOption( "input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'i', "input",
                            "The file containing the data structure for the analysis.")
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                            "The file where the results are written to." )
                }
        );
        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

        String fileName = config.getString("output");
        FileUtil.checkFileName(fileName);

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");

        AnalysisData analysisData = context.getBean(AnalysisData.class);
        HierarchyExporter exporter = context.getBean(HierarchyExporter.class);
        String type = config.getString("type").toUpperCase();
        switch (type) {
            case "RELATIONSHIP":
                //Initializing Analysis Data  *** IMPORTANT ***
                analysisData.setFileName(config.getString("input"));
                exporter.exportParentship(fileName);
                break;
            case "DETAILS":
                //Initializing Analysis Data  *** IMPORTANT ***
                analysisData.setFileName(config.getString("input"));
                exporter.exportDetails(fileName);
                break;
            default:
                System.err.println("Wrong export type, please use either DETAILS or RELATIONSHIP");
        }
    }

}
