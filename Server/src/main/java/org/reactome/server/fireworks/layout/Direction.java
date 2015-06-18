package org.reactome.server.fireworks.layout;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
enum Direction {

    CLOCKWISE,
    ANTICLOCKWISE;

    public static Direction getDirection(String direction){
        for (Direction d : values()) {
            if(d.toString().toLowerCase().equals(direction.toLowerCase())){
                return d;
            }
        }
        return CLOCKWISE;
    }

}
