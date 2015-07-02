package org.reactome.server.analysis.service.helper;

import org.apache.log4j.Logger;
import org.reactome.server.analysis.service.utils.Tokenizer;
import org.reactome.server.lru.LruFolderContentChecker;
import org.reactome.server.lru.LruFolderContentCheckerFileDeletedHandler;
import org.springframework.context.annotation.Scope;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Scope("singleton")
public class FileCheckerController implements LruFolderContentCheckerFileDeletedHandler {
    private static Logger logger = Logger.getLogger(FileCheckerController.class.getName());

    private static Thread checker = null;

    private String pathDirectory;
    private Long maxSize;
    private Long threshold;
    private Long time;
    private Long ttl;

    public FileCheckerController() {
    }

    public void setPathDirectory(String pathDirectory) {
        this.pathDirectory = pathDirectory;
        this.initialize();
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
        this.initialize();
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
        this.initialize();
    }

    public void setTime(Long time) {
        this.time = time;
        this.initialize();
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
        this.initialize();
    }

    public void initialize(){
        if(checker!=null){
            //We ensure only one thread will be created
            logger.warn("Attempt to initialise the file checker when initialised before");
            return;
        }
        if(pathDirectory!=null && maxSize!=null && threshold!=null && time!=null && ttl!=null){
            LruFolderContentChecker folderContentChecker = new LruFolderContentChecker(pathDirectory, maxSize, threshold, time, ttl);
            folderContentChecker.addCheckerFileDeletedHandler(this);
            checker = new Thread(folderContentChecker);
            try{
                checker.setName("FileCheckerController");
            }catch (SecurityException e){
                logger.warn("FileCheckerController thread renaming failed!");
            }
            checker.start();
            logger.info("FileCheckerController started...");
        }
    }

    @Override
    public void onLruFolderContentCheckerFileDeleted(String fileName) {
        Tokenizer.removeAssociatedToken(fileName);
    }
}
