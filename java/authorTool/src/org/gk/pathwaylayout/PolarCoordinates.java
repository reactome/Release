package org.gk.pathwaylayout;

import java.awt.Point;

public class PolarCoordinates {
	public double r;
	public double phi;
	
	public PolarCoordinates () {
		this(0,0);
	}
	
	public PolarCoordinates (double r, double phi) {
		this.r = r;
		this.phi = phi;
	}
	
	public PolarCoordinates (Point.Double point) {
		this(Math.sqrt(point.x*point.x + point.y*point.y), Math.atan2(point.y, point.x));
	}
	
	public Point.Double toCartesianDouble () {
		return new Point.Double(r * Math.cos(phi), r * Math.sin(phi));
	}
	
	public Point toCartesian () {
		return new Point((int)(r * Math.cos(phi)), (int)(r * Math.sin(phi)));
	}
	
	public String toString () {
		return "[" + r + ", " + phi + "]";
	}
	
	public static PolarCoordinates cartesian2polar (double x, double y) {
		return new PolarCoordinates(new Point.Double(x,y));
	}
	
	public static double getBearing (double x, double y) {
		return Math.atan2(y, x);
	}
}
