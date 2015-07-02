package org.reactome.server.analysis.core.util;

import cern.jet.random.Binomial;
import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class MathUtilities {

    private static RandomEngine randomEngine = new DRand();

    public static Double calculatePValue(double ratio, int sampleSize, int success) {
        if(ratio==1.d) return 0d;
        Binomial binomial = new Binomial(sampleSize, ratio, randomEngine);
        if(success == 0){ // To avoid unreasonable value
            success = 1;
        }
        return 1.0d - binomial.cdf(success - 1);
    }

}
