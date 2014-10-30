package org.reactome.server.analysis.core.util;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Pair<S,T> {
    private S fst;
    private T snd;

    public Pair(S fst, T snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public S getFst() {
        return fst;
    }

    public T getSnd() {
        return snd;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (fst != null ? !fst.equals(pair.fst) : pair.fst != null) return false;
        if (snd != null ? !snd.equals(pair.snd) : pair.snd != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fst != null ? fst.hashCode() : 0;
        result = 31 * result + (snd != null ? snd.hashCode() : 0);
        return result;
    }
}
