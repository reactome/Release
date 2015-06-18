package org.reactome.server.fireworks;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Main {

    public static enum Tool {
        GRAPH,
        LAYOUT;

        public static String getOptions(){
            StringBuilder sb = new StringBuilder();
            for (Tool tool : values()) {
                sb.append(tool).append(", ");
            }
            sb.delete(sb.length()-2, sb.length()-1);
            return sb.toString();
        }

        public static Tool getTool(String str){
            String aux = str.toUpperCase();
            for (Tool tool : values()) {
                if(tool.toString().equals(aux)) return tool;
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                Main.class.getName(),
                "Provides a set of tools for the Fireworks building and layout",
                new Parameter[] {
                        new UnflaggedOption( "tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                                "The tool to use. Options: " + Tool.getOptions())
                }
        );

        if(args.length==0){
            System.err.println("Please specify the tool to use. Options: " + Tool.getOptions());
            System.exit( 1 );
        }

        Tool tool = Tool.getTool(args[0]);
        if(tool!=null) {
            switch (Tool.getTool(args[0])) {
                case GRAPH:
                    GraphCreation.main(args);
                    break;
                case LAYOUT:
                    Layout.main(args);
                    break;
            }
        }else{
            jsap.parse(args[0]);
            if( jsap.messagePrinted() ) System.exit( 1 );

            System.err.println("Sorry, the tool " + args[0] + " is not available." );
            System.err.println("\nUsage:" );
            System.err.println(Main.class.getName() + " tool [options]");
            System.err.println("\nThe tools are: " + Tool.getOptions());
            System.exit( 1 );
        }
    }
}
