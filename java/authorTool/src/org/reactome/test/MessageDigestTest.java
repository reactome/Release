/*
 * Created on Mar 3, 2009
 *
 */
package org.reactome.test;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class MessageDigestTest {
    
    @Test
    public void testMD5() throws NoSuchAlgorithmException {
        String input = "This is a test";
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes());
        byte[] bytes = md.digest();
        String message = new BigInteger(bytes).toString(16);
        System.out.println("After digest: " + message);
    }
    
}
