package org.reactome.server.models2pathways.core.utils;


import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class JSAPHandler {
    public static JSAPResult ArgumentHandler(String[] args) {
        JSAP jsap = new JSAP();
        FlaggedOption opt1 = new FlaggedOption("output")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('o')
                .setLongFlag("output");
        opt1.setHelp("Path to output tsv (MANDATORY)");

        FlaggedOption opt2 = new FlaggedOption("biomodels")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('b')
                .setLongFlag("biomodels");
        opt2.setHelp("Path to folder of BioModels files. (MANDATORY");

        FlaggedOption opt3 = new FlaggedOption("reactome")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('r')
                .setLongFlag("reactome");
        opt3.setHelp("Path to Reactome intermediate file, containing preprocessed to for the analysis (MANDATORY)");

        FlaggedOption opt4 = new FlaggedOption("significantFDR")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setRequired(false)
                .setShortFlag('s')
                .setDefault(String.valueOf(0.005))
                .setLongFlag(String.valueOf("significantFDR"));
        opt4.setHelp("Value of the FDR for significant results");

        FlaggedOption opt5 = new FlaggedOption("extendedFDR")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setRequired(false)
                .setShortFlag('e')
                .setLongFlag("extendedFDR");
        opt5.setHelp("Value of the FDR for possible results");

        FlaggedOption opt6 = new FlaggedOption("coverage")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setRequired(false)
                .setShortFlag('c')
                .setDefault(String.valueOf(0.6))
                .setLongFlag("coverage");
        opt6.setHelp("minimum pathway reaction coverage");

        try {
            jsap.registerParameter(opt1);
            jsap.registerParameter(opt2);
            jsap.registerParameter(opt3);
            jsap.registerParameter(opt4);
            jsap.registerParameter(opt5);
            jsap.registerParameter(opt6);
        } catch (JSAPException e) {
            e.printStackTrace();
        }

        JSAPResult jsapResult = jsap.parse(args);

        if (!jsapResult.success()) {
            System.err.println();
            System.err.println("Usage: java -jar Models2Pathways-1.0-jar-with-dependencies.jar");
            System.err.println("                "
                    + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);
        }
        return jsapResult;
    }
}
