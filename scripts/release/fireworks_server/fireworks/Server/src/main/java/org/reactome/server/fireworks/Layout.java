package org.reactome.server.fireworks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.GKInstance2ModelObject;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.server.fireworks.factory.OrthologyGraphFactory;
import org.reactome.server.fireworks.layout.Bursts;
import org.reactome.server.fireworks.layout.FireworksLayout;
import org.reactome.server.fireworks.model.GraphNode;
import org.reactome.server.fireworks.model.Graphs;
import org.reactome.server.fireworks.output.Graph;
import org.reactome.server.fireworks.util.GraphUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Layout {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                Main.class.getName(),
                "Provides a set of tools for the pathway analysis and species comparison",
                new Parameter[] {
                    new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                        "The tool to use. Options: " + org.reactome.server.Main.Tool.getOptions()) //WE DO NOT TAKE INTO ACCOUNT TOOL HERE ANY MORE
                    ,new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                        "The database host")
                    , new FlaggedOption("database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                    , new FlaggedOption("username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                        "The database user")
                    , new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                        "The password to connect to the database")
                    ,new FlaggedOption( "graph", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'g', "graph",
                        "The file containing the data structure for the analysis." )
                    ,new FlaggedOption( "folder", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'f', "folder",
                        "The folder where the configuration file are stored." )
                    ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                        "The folder where the results are written to." )
                    ,new FlaggedOption( "species", JSAP.STRING_PARSER, "ALL", JSAP.NOT_REQUIRED, 's', "species",
                        "Species Fireworks to layout (default: all). Use '_' instead of ' ' (example: homo_sapiens)" )
                    ,new QualifiedSwitch( "verbose", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output." )
                }
        );

        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        MySQLAdaptor dba = new MySQLAdaptor(
                config.getString("host"),
                config.getString("database"),
                config.getString("username"),
                config.getString("password")
        );
        ApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
        GKInstance2ModelObject converter = context.getBean(GKInstance2ModelObject.class);
        DatabaseObjectFactory.initializeFactory(dba, converter);

        String outputDir = config.getString("output");

        Graphs graphs = null;
        try {
            graphs = GraphUtils.getReactomeGraphs(config.getString("graph"));
        } catch (FileNotFoundException ex){
            System.err.println("Can not load file " + config.getString("graph"));
            System.exit( 1 );
        }

        File directory = new File(config.getString("folder"));
        if(!directory.exists()){
            System.err.println(config.getString("folder") + " is not a valid folder");
            System.exit(1);
        }

        List<GraphNode> list = graphs.getGraphNodes();
        GraphNode main = list.remove(0);

        String homoSapiens = getBurstsFileName(directory, main.getName());
        if(!(new File(homoSapiens)).exists()){
            System.err.println(homoSapiens + " is MANDATORY");
            System.exit(1);
        }

        System.out.println(main.getName() + "...");
        Bursts bursts = getBurstsConfiguration(directory, main.getName());
        FireworksLayout layout = new FireworksLayout(bursts, main);
        layout.doLayout();
        saveSerialisedGraphNode(main, outputDir);

        boolean speciesSpecified = !config.getString("species").equals("ALL");
        for (GraphNode node : list) {
            if(speciesSpecified) {
                String aux = node.getName().replaceAll(" ", "_").toLowerCase();
                if (!aux.equals(config.getString("species").toLowerCase())) continue;
            }

            System.out.println("\n" + node.getName() + "...");
            bursts = getBurstsConfiguration(directory, node.getName());
            if(bursts!=null){
                layout = new FireworksLayout(bursts, node);
                layout.doLayout();
            }
            OrthologyGraphFactory ogf = new OrthologyGraphFactory(main, node);
            saveSerialisedGraphNode(ogf.getGraph(), outputDir);
        }
        System.out.println("\nFirework layout finished.");

        System.exit( 0 );
    }

    private static Bursts getBurstsConfiguration(File directory, String speciesName){
        String fileName =getBurstsFileName(directory, speciesName);
        try {
            return mapper.readValue(new File(fileName), Bursts.class);
        }catch (Exception e){
            System.out.println(fileName + " could not be found");
        }
        return null;
    }

    private static String getBurstsFileName(File directory, String speciesName){
        String fileName = speciesName.replaceAll(" ", "_")  + "_bursts.json";
        return directory.getAbsolutePath() + File.separator + fileName;
    }

    private static void saveSerialisedGraphNode(GraphNode graphNode, String dir){
        dir = dir.endsWith("/") ? dir : dir + "/";
        String fileName = dir + graphNode.getName().replaceAll(" ", "_") + ".json";
        try {
            mapper.writeValue(new File(fileName), new Graph(graphNode));
            System.out.println(fileName + " written.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
