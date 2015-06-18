package org.reactome.server.fireworks;

import com.martiansoftware.jsap.*;
import org.reactome.server.analysis.core.data.AnalysisData;
import org.reactome.server.analysis.core.model.PathwayHierarchy;
import org.reactome.server.analysis.core.model.SpeciesNode;
import org.reactome.server.analysis.core.model.SpeciesNodeFactory;
import org.reactome.server.fireworks.factory.ReactomeGraphNodeFactory;
import org.reactome.server.fireworks.model.Graphs;
import org.reactome.server.fireworks.util.GraphUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class GraphCreation {
    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                Main.class.getName(),
                "Creates a minimised version of the analysis intermediate file which loads faster for Fireworks customisation",
                new Parameter[] {
                    new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The tool to use. Options: " + org.reactome.server.Main.Tool.getOptions()) //WE DO NOT TAKE INTO ACCOUNT TOOL HERE ANY MORE
                    ,new FlaggedOption( "structure", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "structure",
                        "The file containing the data structure for the analysis." )
                    ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                        "The file where the results are written to." )
                    ,new QualifiedSwitch( "verbose", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output." )
                }
        );

        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

        ApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
        //Initializing Analysis Data  *** IMPORTANT ***
        String structure = config.getString("structure");
        AnalysisData analysisData = context.getBean(AnalysisData.class);
        analysisData.setFileName(structure);

        Graphs  graphs = new Graphs();
        SpeciesNode humanNode = SpeciesNodeFactory.getHumanNode();
        PathwayHierarchy hierarchy = analysisData.getPathwayHierarchies().get(humanNode);
        ReactomeGraphNodeFactory reactomeGraphFactory = new ReactomeGraphNodeFactory(hierarchy);
        graphs.addGraphNode(reactomeGraphFactory.getGraphNode());

        for (SpeciesNode node : analysisData.getPathwayHierarchies().keySet()) {
            if(!node.equals(humanNode)){
                hierarchy = analysisData.getPathwayHierarchies().get(node);
                reactomeGraphFactory = new ReactomeGraphNodeFactory(hierarchy);
                graphs.addGraphNode(reactomeGraphFactory.getGraphNode());
            }
        }
        GraphUtils.save(graphs, config.getString("output"));

        if(config.getBoolean("verbose")){
            System.out.println(config.getString("output") + " has been created");
        }

        System.exit( 0 );
    }
}
