package org.reactome.server.analysis.service.utils;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;
import org.apache.log4j.Logger;
import org.reactome.server.analysis.service.report.AnalysisReport;
import org.reactome.server.analysis.service.report.ReportParameters;
import org.reactome.server.analysis.service.result.AnalysisStoredResult;

import java.io.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class ResultDataUtils {

    private static Logger logger = Logger.getLogger(ResultDataUtils.class.getName());

    public static AnalysisStoredResult getAnalysisResult(String fileName) throws FileNotFoundException {
        return getAnalysisResult(fileName, null);
    }

    public static AnalysisStoredResult getAnalysisResult(String fileName, ReportParameters report) throws FileNotFoundException {
        long start = -1;
        if(report!=null) {
            start = System.currentTimeMillis();
        }
        AnalysisStoredResult rtn = retrieveAnalysisResult(fileName);
        if(report!=null){
            report.setAnalysisStoredResult(rtn);
            report.setMilliseconds(System.currentTimeMillis()-start);
            AnalysisReport.reportCachedAnalysis(report);
        }
        return rtn;
    }

    public static void kryoSerialisation(AnalysisStoredResult result, String fileName){
        long start = System.currentTimeMillis();
        try {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            OutputStream file = new FileOutputStream(fileName);
            Output output = new Output(file);
            kryo.writeClassAndObject(output, result);

            output.close();
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("%s saved in %d ms", result.getClass().getSimpleName(), end - start));
    }

    private static AnalysisStoredResult retrieveAnalysisResult(String fileName) throws FileNotFoundException {
        InputStream file = new FileInputStream(fileName);
        AnalysisStoredResult rtn = (AnalysisStoredResult) ResultDataUtils.read(file);
        logger.info(fileName + " retrieved");
        return rtn;
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
