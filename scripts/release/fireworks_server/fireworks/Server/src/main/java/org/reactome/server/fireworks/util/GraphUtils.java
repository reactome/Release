package org.reactome.server.fireworks.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;
import org.reactome.server.fireworks.model.GraphNode;
import org.reactome.server.fireworks.model.Graphs;

import java.io.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class GraphUtils {

    public static Graphs getReactomeGraphs(String fileName) throws FileNotFoundException {
        InputStream file = new FileInputStream(fileName);
        return (Graphs) GraphUtils.read(file);
    }

//    public static <T> T kryoCopy(T object){
//        Kryo kryo = new Kryo();
//        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
//        T rtn = kryo.copy(object);
//        return rtn;
//    }

    public static void save(Graphs graphs, String fileName){
        try {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            OutputStream file = new FileOutputStream(fileName);
            Output output = new Output(file);
            kryo.writeClassAndObject(output, graphs);
            output.close();
        } catch (FileNotFoundException e) {
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
