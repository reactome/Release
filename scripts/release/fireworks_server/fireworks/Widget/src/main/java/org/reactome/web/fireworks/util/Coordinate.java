package org.reactome.web.fireworks.util;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Coordinate {
    private double x;
    private double y;

    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Coordinate add(Coordinate coordinate){
        return new Coordinate(x + coordinate.x, y + coordinate.y);
    }

    public double distance(Coordinate coordinate) {
        Coordinate diff = this.minus(coordinate);
        return Math.sqrt(diff.getX()*diff.getX() + diff.getY()*diff.getY());
    }

    public Coordinate divide(double f){
        return new Coordinate(this.x/f, this.y/f);
    }

    public Coordinate minus(Coordinate coordinate){
        return new Coordinate(x - coordinate.x, y - coordinate.y);
    }

    public Coordinate multiply(double f){
        return new Coordinate(this.x * f , this.y * f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coordinate that = (Coordinate) o;

        if (Double.compare(that.x, x) != 0) return false;
        if (Double.compare(that.y, y) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
