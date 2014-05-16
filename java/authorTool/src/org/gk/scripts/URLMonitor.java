/*
 * Created on Dec 19, 2012
 *
 */
package org.gk.scripts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

//import javax.mail.Message;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * A class that is used to monitor a http URL.
 * @author gwu
 *
 */
public class URLMonitor {
    private final static Logger logger = Logger.getLogger(URLMonitor.class);
    private Set<String> downURLs;
    
    public URLMonitor() {
        downURLs = new HashSet<String>();
    }
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Run this application using three parameters: a file containing a list of URLs, a file containing a list of emails, and time interval");
            System.exit(1);
        }
        PropertyConfigurator.configure("resources/log4j.properties");
        URLMonitor monitor = new URLMonitor();
        monitor.monitor(args[0], 
                        args[1],
                        args[2]);
    }
    
    public void monitor(String urlListFile,
                        String emailFile,
                        String time) {
        logger.info("Start monitoring...");
        try {
            List<String> urls = loadURLs(urlListFile);
            while (true) {
                for (String text : urls) {
                    URL url = new URL(text);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // Peek the status first. If it is not connected, an exception will be thrown
                    try {
                        int code = connection.getResponseCode();
                    }
                    catch(Exception e) {
                        if (!downURLs.contains(text)) {
                            downURLs.add(text);
                            sendMail(text, false, emailFile);
                        }
                        continue;
                    }
                    if (connection.getResponseCode() == 200) {
                        if (downURLs.contains(text)) {
                            sendMail(text, 
                                     true,
                                     emailFile);
                            downURLs.remove(text);
                        }
                        continue; // Work fine
                    }
                    else if (!downURLs.contains(text)) {
                        downURLs.add(text);
                        sendMail(text, 
                                 false,
                                 emailFile);
                    }
                }
                Thread.sleep(Long.parseLong(time));
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private void sendMail(String url,
                          boolean isRecoverd,
                          String emailFile) throws Exception {
//        String from = "guanmingwu@yahoo.com";
//        String host = "localhost";
//        Properties properties = System.getProperties();
//        properties.setProperty("mail.smpt.host", host);
//        Session session = Session.getDefaultInstance(properties);
//        MimeMessage message = new MimeMessage(session);
//        message.setFrom(new InternetAddress(from));
//        String status = isRecoverd ? "recovered" : "down";
//        message.setSubject(url + " is " + status);
//        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//        Date date = new Date();
//        message.setText(url + " is " + status + " at " + dateFormat.format(date));
//        FileReader fr = new FileReader(emailFile);
//        BufferedReader br = new BufferedReader(fr);
//        String line = null;
//        while ((line = br.readLine()) != null) {
//            message.addRecipients(Message.RecipientType.TO,
//                                  line);
//        }
//        br.close();
//        fr.close();
//        Transport.send(message);
//        logger.info("Sending out email for " + url + " is " + status);
    }
    
    private List<String> loadURLs(String fileName) throws IOException {
        FileReader reader = new FileReader(fileName);
        BufferedReader br = new BufferedReader(reader);
        List<String> url = new ArrayList<String>();
        String line = null;
        while ((line = br.readLine()) != null)
            url.add(line);
        br.close();
        reader.close();
        return url;
    }
    
}
