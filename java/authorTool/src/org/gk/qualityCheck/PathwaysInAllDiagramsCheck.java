/*
 * Created on Mar 11, 2011
 *
 */
package org.gk.qualityCheck;


/**
 * @author wgm
 *
 */
public class PathwaysInAllDiagramsCheck extends ServletBasedQACheck {
    
    public PathwaysInAllDiagramsCheck() {
        actionName = "PathwayELVCheck";
        resultTitle = "Pathways in Diagrams QA Results";
    }

    /**
     * A bug in the output for pathways. Right now it use "reactions".
     */
    @Override
    protected String fixHeader(String line) {
        line = line.replaceAll("Reaction", "Pathway");
        return line;
    }
    
    
}
