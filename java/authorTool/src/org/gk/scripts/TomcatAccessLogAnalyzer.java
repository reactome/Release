/*
 * Created on Jan 25, 2012
 *
 */
package org.gk.scripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * @author gwu
 *
 */
public class TomcatAccessLogAnalyzer {
    
    public TomcatAccessLogAnalyzer() {
        
    }
    
    @Test
    public void test() {
        String line = "reactome_tomcat_55.2012-01-25.txt:193.63.58.71 - - [25/Jan/2012:13:37:36 -0500]";
        Pattern pattern = Pattern.compile("(\\d)+\\.(\\d)+\\.(\\d)+\\.(\\d)+");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
            System.out.println(matcher.group());
    }
    
    @Test
    public void process() throws IOException {
        String fileName = "tmp/soap.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        Map<String, Integer> ipToCount = new HashMap<String, Integer>();
        Pattern pattern = Pattern.compile("(\\d)+\\.(\\d)+\\.(\\d)+\\.(\\d)+");
        while ((line = fu.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if(!matcher.find()) {
                System.out.println("Cannot find id: " + line);
                continue;
            }
            String ip = matcher.group();
            Integer count = ipToCount.get(ip);
            if (count == null)
                ipToCount.put(ip, 1);
            else
                ipToCount.put(ip, ++count);
        }
        fu.close();
        System.out.println("Total IPs: " + ipToCount.size());
        System.out.println("\nIP\tAccess");
        for (String ip : ipToCount.keySet()) {
            System.out.println(ip + "\t" + ipToCount.get(ip));
        }
    }
    
}
