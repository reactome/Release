package org.reactome.server.analysis.core.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;
import org.apache.log4j.Logger;
import org.reactome.server.analysis.core.model.DataContainer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisDataUtils {
    private static Logger logger = Logger.getLogger(AnalysisDataUtils.class.getName());

    public static DataContainer getDataContainer(InputStream file){
        logger.info(String.format("Loading %s file...", DataContainer.class.getSimpleName()));
        long start = System.currentTimeMillis();
        DataContainer container = (DataContainer) AnalysisDataUtils.read(file);
        container.initialize();
        long end = System.currentTimeMillis();
        logger.info(String.format("%s file loaded in %d ms", DataContainer.class.getSimpleName() , end-start));
        return container;
    }

    public static <T> T kryoCopy(T object){
        long start = System.currentTimeMillis();
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        T rtn = kryo.copy(object);
        long end = System.currentTimeMillis();
        logger.trace(String.format("%s cloned in %d ms", object.getClass().getSimpleName() , end-start));
        return rtn;
    }

    public static void kryoSerialisation(DataContainer container, String fileName){
        logger.trace(String.format("Saving %s data into file %s", container.getClass().getSimpleName(), fileName));
        try {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            OutputStream file = new FileOutputStream(fileName);
            Output output = new Output(file);
            container.prepareToSerialise(); //Getting the structure ready for serialisation
            kryo.writeClassAndObject(output, container);
            output.close();
            container.initialize(); //At the end the data structure remains the same
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private static Object read(InputStream file){
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        Input input = new Input(file);
        Object obj = kryo.readClassAndObject(input);
        input.close();
        return obj;
    }
}
