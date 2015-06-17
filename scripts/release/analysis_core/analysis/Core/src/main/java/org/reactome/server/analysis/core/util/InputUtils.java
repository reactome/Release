package org.reactome.server.analysis.core.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.reactome.server.analysis.core.model.AnalysisIdentifier;
import org.reactome.server.analysis.core.model.UserData;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Parses the input to create a UserData object with the data to be analysed
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class InputUtils {
    private static Logger logger = Logger.getLogger(InputUtils.class.getName());

    @SuppressWarnings("UnusedDeclaration")
    public static UserData getUserData(String input){
        logger.trace("Loading identifiers...");
        long start = System.currentTimeMillis();
        String md5 = DigestUtils.md5DigestAsHex(input.getBytes());
        UserData ud = InputUtils.processData(input, md5);
        long end = System.currentTimeMillis();
        logger.trace(String.format("%d loaded in %d ms", ud.getIdentifiers().size(), end-start));
        return ud;
    }

    public static UserData getUserData(InputStream is) throws IOException {
        logger.trace("Loading identifiers...");
        long start = System.currentTimeMillis();
        String input = IOUtils.toString(is);
        String md5 = DigestUtils.md5DigestAsHex(input.getBytes());
        UserData ud = InputUtils.processData(input, md5);
        long end = System.currentTimeMillis();
        logger.trace(String.format("%d loaded in %d ms", ud.getIdentifiers().size(), end-start));
        return ud;
    }

    private static boolean isComment(String line){
        return line.startsWith("#") || line.startsWith("//");
    }

    private static List<String> getHeaderColumnNames(String line){
        line = line.replaceAll("^(#|//)", "");
        List<String> columnNames = new LinkedList<String>();
        for (String columnName : line.split("\\t")) {
            columnNames.add(StringEscapeUtils.escapeJava(columnName.trim())); //Kryo serialization error instead :(
        }
        return columnNames; // Arrays.asList(line.split("\\s"));
    }

    private static AnalysisIdentifier getAnalysisIdentifier(String line){
        AnalysisIdentifier rtn = null;
        if(line==null || line.isEmpty()) return rtn;
        String[] data = line.split("\\t");
        if(data.length>0){
            rtn = new AnalysisIdentifier(data[0].trim());
            for(int i=1; i<data.length; ++i){
                try{
                    rtn.add(Double.valueOf(data[i].trim()));
                }catch (NumberFormatException nfe){
                    rtn.add(null); //null won't be taken into account for the AVG
                }
            }
        }
        return rtn;
    }

    private static UserData processData(String data, String md5){
        List<String> headerColumnNames = new LinkedList<>();
        Set<AnalysisIdentifier> expressionData = new HashSet<AnalysisIdentifier>();
        String[] lines = data.split("\n");
        if(lines.length>0){ //FIRST LINE treated separately to avoid checking the others (speed purposes)
            String line = lines[0].trim();
            if(isComment(line)){
                headerColumnNames = getHeaderColumnNames(line);
            }else{
                AnalysisIdentifier ai = getAnalysisIdentifier(line);
                if(ai!=null) expressionData.add(ai);
            }
        }
        for (int i=1; i<lines.length; ++i) {
            AnalysisIdentifier ai = getAnalysisIdentifier(lines[i].trim());
            if(ai!=null) expressionData.add(ai);
        }

        return new UserData(headerColumnNames, expressionData, md5);
    }
}
