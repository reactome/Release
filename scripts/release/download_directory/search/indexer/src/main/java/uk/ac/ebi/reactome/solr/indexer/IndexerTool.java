package uk.ac.ebi.reactome.solr.indexer;

import com.martiansoftware.jsap.*;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.gk.persistence.MySQLAdaptor;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.impl.Indexer;

import java.io.File;
import java.sql.SQLException;

/**
 * Creates the Solr documents and the ebeye.xml file
 *
 * Created by flo on 4/30/14.
 */
public class IndexerTool {

    public static void main(String[] args) throws JSAPException {
        long startTime = System.currentTimeMillis();

        SimpleJSAP jsap = new SimpleJSAP(
                IndexerTool.class.getName(),
                "A tool for generating a Solr Index", //TODO
                new Parameter[] {
                        new FlaggedOption( "host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                                "The database host")
                        ,new FlaggedOption( "database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                                "The reactome database name to connect to")
                        ,new FlaggedOption( "username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                                "The database user")
                        ,new FlaggedOption( "password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                                "The password to connect to the database")
                        ,new FlaggedOption( "solrurl", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "solrurl",
                                "Url of the running Solr server")
                        ,new FlaggedOption( "input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'c', "cv",
                                "CSV input file specifying the controlled vocabulary terms that should appear as keywords" )
                        ,new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "output",
                                "XML output file for the EBeye" )
                        ,new FlaggedOption( "release", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'r', "release",
                                "Release version number" )
                        ,new QualifiedSwitch( "verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                                "Requests verbose output." )
                }
        );
        JSAPResult config = jsap.parse(args);
        if( jsap.messagePrinted() ) System.exit( 1 );

        try {
            MySQLAdaptor dba = new MySQLAdaptor(
                    config.getString("host"),
                    config.getString("database"),
                    config.getString("username"),
                    config.getString("password")
            );
            SolrServer solrServer = new HttpSolrServer(
                    config.getString("solrurl")
            );

            File output = new File(config.getString("output"));
            String release = config.getString("release");
            File controlledVocabulary = new File(config.getString("input"));
            Boolean verbose = config.getBoolean("verbose");
            Indexer indexer = new Indexer(dba, solrServer, controlledVocabulary, output, release, verbose);

            indexer.index();
            long stopTime = System.currentTimeMillis();
            long ms =  stopTime-startTime;
            int seconds = (int) (ms / 1000) % 60 ;
            int minutes = (int) ((ms / (1000*60)) % 60);

            if (verbose) {
                System.out.println("Indexing was successful within: " + minutes + "minutes " + seconds + "seconds ");
            }
        }
        catch (SQLException e) {
            System.out.println("Could not initiate MySQLAdapter");
            e.printStackTrace();
        } catch (IndexerException e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }
}
