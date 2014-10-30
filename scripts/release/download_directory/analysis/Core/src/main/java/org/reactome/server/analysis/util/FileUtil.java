package org.reactome.server.analysis.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class FileUtil {

    private static Logger logger = Logger.getLogger(FileUtil.class.getName());

    public static void checkFileName(String fileName){
        File file = new File(fileName);

        if(file.isDirectory()){
            String msg = fileName + " is a folder. Please specify a valid file name.";
            System.err.println(msg);
            logger.fatal(msg);
            System.exit( 1 );
        }

        if(file.getParent()==null){
            file = new File("./" + fileName);
        }
        Path parent = Paths.get(file.getParent());
        if(!Files.exists(parent)){
            String msg = parent + " does not exist.";
            System.err.println(msg);
            logger.fatal(msg);
            System.exit( 1 );
        }

        if(!file.getParentFile().canWrite()){
            String msg = "No write access in " + file.getParentFile();
            System.err.println(msg);
            logger.fatal(msg);
            System.exit( 1 );
        }

        logger.trace(fileName + " is a valid file name");
    }
}
