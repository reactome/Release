package org.reactome.server.fireworks.output;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Edge {

    private Long from;
    private Long to;

    public Edge(Long from, Long to) {
        this.from = from;
        this.to = to;
    }

    public Long getFrom() {
        return from;
    }

    public Long getTo() {
        return to;
    }
}
