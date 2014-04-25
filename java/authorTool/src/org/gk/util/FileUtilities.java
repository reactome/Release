/*
 * Created on Jun 18, 2009
 *
 */
package org.gk.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A group of utiltiy methods related to File I/O.
 * @author wgm
 *
 */
public class FileUtilities {
    // For input
    private FileReader fileReader;
    private BufferedReader bufferedReader;
    // For output
    private FileWriter fileWriter;
    private PrintWriter printWriter;
    
    public FileUtilities() {
    }
    
    public void setInput(String fileName) throws IOException {
        fileReader = new FileReader(fileName);
        bufferedReader = new BufferedReader(fileReader);
    }
    
    public String readLine() throws IOException {
        return bufferedReader.readLine();
    }
    
    public void close() throws IOException {
        if (bufferedReader != null) {
            bufferedReader.close();
            fileReader.close();
            bufferedReader = null;
            fileReader = null;
        }
        if (printWriter != null) {
            printWriter.close();
            printWriter = null;
            fileWriter.close();
            fileWriter = null;
        }
    }
    
    public void setOutput(String fileName) throws IOException {
        fileWriter = new FileWriter(fileName);
        printWriter = new PrintWriter(fileWriter);
    }
    
    public void printLine(String line) throws IOException {
        printWriter.println(line);
    }
}
