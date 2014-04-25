/*
 * Created on Jul 2, 2008
 *
 */
package org.gk.render;

/**
 * This enum is used to describe the types of reaction for drawing purposes.
 * @author wgm
 *
 */
public enum ReactionType {
    TRANSITION  {// Default: should be used for types that are not one of the above four.
        public String toString() {return "Transition";}
    },    
    ASSOCIATION {
        public String toString() {return "Association";}
    },
    DISSOCIATION {
        public String toString() {return "Dissociation";}
    },
    OMITTED_PROCESS {
        public String toString() {return "Omitted Process";}
    },
    UNCERTAIN_PROCESS{
        public String toString() {return "Uncertain Process";}
    }
}
