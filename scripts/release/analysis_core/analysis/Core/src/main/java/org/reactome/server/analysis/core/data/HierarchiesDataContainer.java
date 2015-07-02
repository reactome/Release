package org.reactome.server.analysis.core.data;

import org.apache.log4j.Logger;
import org.reactome.server.analysis.core.model.HierarchiesData;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class HierarchiesDataContainer {

    private static Logger logger = Logger.getLogger(HierarchiesDataContainer.class.getName());

    private static final Object READER_SEMAPHORE = new Object();

    public static int POOL_SIZE = 5;
    private static HierarchiesData[] pool = new HierarchiesData[POOL_SIZE];

    private static int read_pos = 0;
    private static int write_pos = 0;

    public static boolean put(HierarchiesData data){
        synchronized (READER_SEMAPHORE){
            if(!isFull()){
                pool[write_pos] = data;
                write_pos = next(write_pos);
            }
            logger.trace(String.format("%s written in the pool", HierarchiesData.class.getSimpleName()));
            READER_SEMAPHORE.notify();
        }
        return true;
    }

    public static HierarchiesData take(){
        if(isEmpty()){
            return HierarchiesDataProducer.getHierarchiesData();
        }

        HierarchiesData data;
        synchronized (READER_SEMAPHORE) {
            data = pool[read_pos];
            pool[read_pos] = null; //Garbage collector purposes
            read_pos = next(read_pos);
            logger.trace(String.format("%s taken from the pool", HierarchiesData.class.getSimpleName()));
        }

        return data;
    }

    public static synchronized boolean isEmpty(){
        return size()==0;
    }

    public static synchronized boolean isFull(){
        return size()==POOL_SIZE;
    }

    private static int next(int pos){
        return (pos + 1) % POOL_SIZE;
    }

    private static int prev(int pos){
        return pos == 0? POOL_SIZE-1 : pos-1;
    }

    private static synchronized int size(){
        if(read_pos==write_pos){
            if(pool[prev(write_pos)]==null){
                return 0;
            }
            return POOL_SIZE;
        }
        if(read_pos<write_pos){
            return write_pos - read_pos;
        }
        return POOL_SIZE - (read_pos - write_pos);
    }
}
