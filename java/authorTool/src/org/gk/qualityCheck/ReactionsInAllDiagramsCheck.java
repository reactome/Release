/*
 * Created on Mar 11, 2011
 *
 */
package org.gk.qualityCheck;



/**
 * This QA check calls a program in the server-side to check the usage of all reactions in all diagrams
 * in order to find if any reaction has not been placed in any PathwayDiagram.
 * @author wgm
 *
 */
public class ReactionsInAllDiagramsCheck extends ServletBasedQACheck {
    public ReactionsInAllDiagramsCheck() {
        actionName = "ReactionELVCheck";
        resultTitle = "Reactions in Diagrams QA Results";
    }
    
}
