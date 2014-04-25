/*
 * Created on Mar 31, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;

import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.jgraph.graph.GraphConstants;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Utils {

	public static Point getReactionMidPoint(GKInstance reaction) throws Exception {
		Collection reactionCoordinatesC = reaction.getReferers(ReactomeJavaConstants.locatedEvent);
		if (reactionCoordinatesC == null || reactionCoordinatesC.isEmpty())
			return null;
		GKInstance reactionCoordinates = (GKInstance)reactionCoordinatesC.iterator().next();
		int sx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceX)).intValue();
		int sy = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceY)).intValue();
		int tx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetX)).intValue();
		int ty = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetY)).intValue();
		return new Point((int)((sx + tx) / 2), (int)((sy + ty) / 2 ));
	}
	
	public static PolarCoordinates getReactionLengthAndBearing(GKInstance reaction) {
		try {
			Collection reactionCoordinatesC = reaction.getReferers(ReactomeJavaConstants.locatedEvent);
			if (reactionCoordinatesC != null && !reactionCoordinatesC.isEmpty()) {
				GKInstance reactionCoordinates = (GKInstance)reactionCoordinatesC.iterator().next();
				int sx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceX)).intValue();
				int sy = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.sourceY)).intValue();
				int tx = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetX)).intValue();
				int ty = ((Integer)reactionCoordinates.getAttributeValue(ReactomeJavaConstants.targetY)).intValue();
				return new PolarCoordinates(new Point.Double(tx - sx, ty - sy));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Collection getLocatedReactionsForSpecies(MySQLAdaptor dba,String speciesName) throws Exception {
		GKInstance species = (GKInstance)dba.fetchInstanceByAttribute(ReactomeJavaConstants.Species,ReactomeJavaConstants.name,"=",speciesName).iterator().next();
		MySQLAdaptor.AttributeQueryRequest aqr1 = dba.createAttributeQueryRequest(ReactomeJavaConstants.ReactionlikeEvent,ReactomeJavaConstants.species,"=",species);
		MySQLAdaptor.ReverseAttributeQueryRequest aqr2 = 
			dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.ReactionCoordinates,ReactomeJavaConstants.locatedEvent,"IS NOT NULL",null);
		List aqrList = new ArrayList();
		aqrList.add(aqr1);
		aqrList.add(aqr2);
		Collection out = dba.fetchInstance(aqrList);
		//System.out.println("Got " + out.size() + " reactions.");
		return out;
		//return dba.fetchInstance(aqrList);
	}
	
	public static Collection getLocatedReactionsForSpecies(MySQLAdaptor dba, GKInstance species) throws Exception {
		MySQLAdaptor.AttributeQueryRequest aqr1 = dba.createAttributeQueryRequest(ReactomeJavaConstants.ReactionlikeEvent,ReactomeJavaConstants.species,"=",species);
		MySQLAdaptor.ReverseAttributeQueryRequest aqr2 = 
			dba.createReverseAttributeQueryRequest(
					dba.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent),
					dba.getSchema().getClassByName(ReactomeJavaConstants.ReactionCoordinates).getAttribute(ReactomeJavaConstants.locatedEvent),
					"IS NOT NULL",null);
		List aqrList = new ArrayList();
		aqrList.add(aqr1);
		aqrList.add(aqr2);
		Collection out = dba.fetchInstance(aqrList);
		//System.out.println("Got " + out.size() + " reactions.");
		return out;
		//return dba.fetchInstance(aqrList);
	}

	public static Collection getSampleReactions(MySQLAdaptor dba) throws Exception {
		return dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,ReactomeJavaConstants.DB_ID,"=",Arrays.asList(new String[]{"83660","83656","75146","114365","139908","114264","140216","75244","139913","114254","139917","141310","114306","139897","114361","114256","83656","114284","140978","75187","139898","139920","139903","114252","114307","140230","114354","114261","114259","83586","114276","114279","114263","114419","114278","114352","139918","83650","141156","139895","139905","114275","140217","75238","140232","139952","141159","139919","139899","73945"}));
		//return dba.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction,ReactomeJavaConstants.DB_ID,"=",Arrays.asList(new String[]{"73945","141159","141156","139952"}));
		//return dba.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction,ReactomeJavaConstants.DB_ID,"=",Arrays.asList(new String[]{"73945"}));
		//return dba.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction,ReactomeJavaConstants.DB_ID,"=",Arrays.asList(new String[]{"76397","176059"}));
/*		return dba.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction,ReactomeJavaConstants.DB_ID,"=",Arrays.asList(new String[]{"140355","140359","174931",
				"174916","158468","158860","158849","159358","176474","176585","176646","176669","176609","176494","176517","176664","176631","176521","176588",
				"176604","174959","158832","174963","174967","174391","158609","175976","175987","175983","174401","174374","176059","176054","71707","71708",
				"71709","71710","71711","71712","71717","71763","71764","71691","71723","71735","76397","76496","76500","114721","139970","139964","141186",
				"141200","141202","141341","141348","141351","76431","143468","70286","173597","174368","174367","174394","76472","76354","156526","76386",
				"76426","76456","76517","76461","76464","76373","76416","76453","76466","76434","76475","76518","76521","159443","159566","177157","177160",
				"159567","159574","176606","174389","176041"}));*/
	}
	
	public static Set<GKInstance> fetchHiddenEntities(MySQLAdaptor dba) throws Exception {
		return (Set<GKInstance>) dba.fetchInstanceByAttribute(ReactomeJavaConstants.PhysicalEntity,ReactomeJavaConstants.name,"=",Arrays.asList(new String[]{"ATP","ADP",
				"H2O","H+","O2","electron","H2","NAD","NADH"}));
	}
	
	public static Set<GKInstance> getFollowingEventsConnectedOverInputEntity(GKInstance reaction, GKInstance entity) throws Exception {
		Collection followingEvents = reaction.getReferers(ReactomeJavaConstants.precedingEvent);
		Set<GKInstance> out = new HashSet<GKInstance>();
		if (followingEvents != null) {
			for (Iterator it = followingEvents.iterator(); it.hasNext();) {
				GKInstance followingEvent = (GKInstance)it.next();
				if (!followingEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				if(followingEvent.getAttributeValuesList(ReactomeJavaConstants.input).contains(entity)) {
					out.add(followingEvent);
					continue;
				}
			}
		}
		return out;
	}
	
	public static Set<GKInstance> getFollowingEventsConnectedOverCayalyst(GKInstance reaction, GKInstance entity) throws Exception {
		Collection followingEvents = reaction.getReferers(ReactomeJavaConstants.precedingEvent);
		List instructions1 = new ArrayList();
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReactionlikeEvent, new String[]{ReactomeJavaConstants.catalystActivity}, new String[]{}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CatalystActivity, new String[]{ReactomeJavaConstants.physicalEntity}, new String[]{}));
		String[] outClasses = new String[]{ReactomeJavaConstants.PhysicalEntity};
		Set<GKInstance> out = new HashSet<GKInstance>();
		if (followingEvents != null) {
			for (Iterator it = followingEvents.iterator(); it.hasNext();) {
				GKInstance followingEvent = (GKInstance)it.next();
				if (!followingEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				Set s = InstanceUtilities.followInstanceAttributes(followingEvent, instructions1, outClasses);
				if (s.contains(entity)) {
					out.add(followingEvent);
					continue;
				}					
			}
		}
		return out;
	}
	
	public static Set<GKInstance> getFollowingEventsConnectedOverRequirement(GKInstance reaction, GKInstance entity) throws Exception {
		Collection followingEvents = reaction.getReferers(ReactomeJavaConstants.precedingEvent);
		List instructions1 = new ArrayList();
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReactionlikeEvent, new String[]{ReactomeJavaConstants.catalystActivity}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CatalystActivity, new String[]{}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Requirement, new String[]{ReactomeJavaConstants.regulator}, new String[]{}));
		String[] outClasses = new String[]{ReactomeJavaConstants.PhysicalEntity};
		Set<GKInstance> out = new HashSet<GKInstance>();
		if (followingEvents != null) {
			for (Iterator it = followingEvents.iterator(); it.hasNext();) {
				GKInstance followingEvent = (GKInstance)it.next();
				if (!followingEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				Set s = InstanceUtilities.followInstanceAttributes(followingEvent, instructions1, outClasses);
				if (s.contains(entity)) {
					out.add(followingEvent);
					continue;
				}					
			}
		}
		return out;
	}
	
	public static Set<GKInstance> getFollowingEventsConnectedOverNegativeRegulation(GKInstance reaction, GKInstance entity) throws Exception {
		Collection followingEvents = reaction.getReferers(ReactomeJavaConstants.precedingEvent);
		List instructions1 = new ArrayList();
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReactionlikeEvent, new String[]{ReactomeJavaConstants.catalystActivity}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CatalystActivity, new String[]{}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.NegativeRegulation, new String[]{ReactomeJavaConstants.regulator}, new String[]{}));
		String[] outClasses = new String[]{ReactomeJavaConstants.PhysicalEntity};
		Set<GKInstance> out = new HashSet<GKInstance>();
		if (followingEvents != null) {
			for (Iterator it = followingEvents.iterator(); it.hasNext();) {
				GKInstance followingEvent = (GKInstance)it.next();
				if (!followingEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				Set s = InstanceUtilities.followInstanceAttributes(followingEvent, instructions1, outClasses);
				if (s.contains(entity)) {
					out.add(followingEvent);
					continue;
				}					
			}
		}
		return out;
	}

	public static Set<GKInstance> getFollowingEventsConnectedOverPositiveRegulation(GKInstance reaction, GKInstance entity) throws Exception {
		Collection followingEvents = reaction.getReferers(ReactomeJavaConstants.precedingEvent);
		List instructions1 = new ArrayList();
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReactionlikeEvent, new String[]{ReactomeJavaConstants.catalystActivity}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CatalystActivity, new String[]{}, new String[]{ReactomeJavaConstants.regulatedEntity}));
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.PositiveRegulation, new String[]{ReactomeJavaConstants.regulator}, new String[]{}));
		String[] outClasses = new String[]{ReactomeJavaConstants.PhysicalEntity};
		Set<GKInstance> out = new HashSet<GKInstance>();
		if (followingEvents != null) {
			for (Iterator it = followingEvents.iterator(); it.hasNext();) {
				GKInstance followingEvent = (GKInstance)it.next();
				if (!followingEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				Set s = InstanceUtilities.followInstanceAttributesStrictly(followingEvent, instructions1, outClasses);
				if (s.contains(entity)) {
					out.add(followingEvent);
					continue;
				}					
			}
		}
		return out;
	}
	
	public static String findShortestName(GKInstance entity) throws Exception {
		if (entity.getSchemClass().isValidAttribute(ReactomeJavaConstants.shortName)) {
			String sn = (String)entity.getAttributeValue(ReactomeJavaConstants.shortName);
			if (sn != null) return sn; 
		}
		String shortest = entity.getDisplayName();
		for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.name).iterator(); i.hasNext();) {
			String n = (String) i.next();
			if (n.length() < shortest.length())
				shortest = n;
		}
		return shortest;
	}
	
	
	public static GKInstance grepCollectionForSetMemberOrContainingSets (Collection<GKInstance> entityCollection, GKInstance entity) throws Exception {
		List<ClassAttributeFollowingInstruction> instructions = new ArrayList<ClassAttributeFollowingInstruction>();
		instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.PhysicalEntity, new String[]{}, new String[]{ReactomeJavaConstants.hasMember, ReactomeJavaConstants.hasCandidate}));
		instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.EntitySet, new String[]{ReactomeJavaConstants.hasMember}, new String[]{}));
		instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CandidateSet, new String[]{ReactomeJavaConstants.hasCandidate}, new String[]{}));
		Set<GKInstance> extendedEntities = InstanceUtilities.followInstanceAttributes(entity, instructions, null);
		for (GKInstance e : entityCollection) {
			if (extendedEntities.contains(e))
				return e;
		}
		return null;
	}
	
	public static int getCountInCollection (Object o, Collection c) {
		int count = 0;
		for (Iterator i = c.iterator(); i.hasNext();) {
			Object n = i.next();
			if (o == n)
				count++;
		}
		return count;
	}
	
	public static int[] calculateEntityNodeDimensions (GKInstance entity) throws InvalidAttributeException, Exception {
		int[] dimensions; //0 - members, 1 - components
		SchemaClass cls = entity.getSchemClass();
		if (cls.isa(ReactomeJavaConstants.Complex)) {
			int[] t = new int[]{0,0};
			Set seen = new HashSet();
			for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.hasComponent).iterator(); i.hasNext();) {
				GKInstance e = (GKInstance) i.next();
				if (!seen.contains(e)) {
					seen.add(e);
					int[] dims = calculateEntityNodeDimensions(e);
					if (dims[0] > t[0]) t[0] = dims[0];
					t[1] += dims[1];
				}
			}
			dimensions = t;
		} else if (cls.isa(ReactomeJavaConstants.Polymer)) {
			int[] t = new int[]{0,0};
			for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.repeatedUnit).iterator(); i.hasNext();) {
				GKInstance e = (GKInstance) i.next();
				int[] dims = calculateEntityNodeDimensions(e);
				if (dims[0] > t[0]) t[0] = dims[0];
				t[1] += dims[1];
			}
			dimensions = t;
		} else if (cls.isa(ReactomeJavaConstants.DefinedSet)) {
			int[] t = new int[]{0,0};
			for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.hasMember).iterator(); i.hasNext();) {
				GKInstance e = (GKInstance) i.next();
				int[] dims = calculateEntityNodeDimensions(e);
				t[0] += dims[0];
				if (dims[1] > t[1]) t[1] = dims[1];
			}
			dimensions = t;
		} else if (cls.isa(ReactomeJavaConstants.CandidateSet)) {
			int[] t = new int[]{0,0};
			for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.hasMember).iterator(); i.hasNext();) {
				GKInstance e = (GKInstance) i.next();
				int[] dims = calculateEntityNodeDimensions(e);
				t[0] += dims[0];
				if (dims[1] > t[1]) t[1] = dims[1];
			}
			for (Iterator i = entity.getAttributeValuesList(ReactomeJavaConstants.hasCandidate).iterator(); i.hasNext();) {
				GKInstance e = (GKInstance) i.next();
				int[] dims = calculateEntityNodeDimensions(e);
				t[0] += dims[0];
				if (dims[1] > t[1]) t[1] = dims[1];
			}
			dimensions = t;
		} else {
			dimensions = new int[]{1,1};
		}
		return dimensions;
	}
		
	/*
	 * Returns the mean distance and bearing of nodes in Set verteces with respect
	 * to the pole vertex.
	 */
	public static PolarCoordinates getMeanPolarCoordinates (Vertex pole, Set<Vertex> verteces) {
		PolarCoordinates polarCoordinates = new PolarCoordinates();
		double minPhi = Math.PI;
		double maxPhi = -Math.PI;
		for (Vertex v : verteces) {
			PolarCoordinates pc = getPolarCoordinates(pole, v);
			polarCoordinates.r += pc.r;
			polarCoordinates.phi += pc.phi;
			if (pc.phi > maxPhi) maxPhi = pc.phi;
			if (pc.phi < minPhi) minPhi = pc.phi;
		}
		polarCoordinates.r /= verteces.size();
		polarCoordinates.phi /= verteces.size();
		if ((maxPhi - minPhi) > Math.PI) polarCoordinates.phi -= Math.PI;
		return polarCoordinates;
	}
	
	public static Set<PolarCoordinates> getPolarCoordinates (Vertex pole, Set<Vertex> verteces) {
		Set <PolarCoordinates> polarCoordinates = new HashSet<PolarCoordinates>();
		for (Vertex v : verteces) {
			polarCoordinates.add(getPolarCoordinates(pole, v));
		}
		return polarCoordinates;
	}
	
	public static PolarCoordinates getPolarCoordinates (Vertex pole, Vertex vertex) {
		Rectangle pBounds = pole.getBounds();
		Rectangle vBounds = vertex.getBounds();
		return PolarCoordinates.cartesian2polar(vBounds.getCenterX() - pBounds.getCenterX(), vBounds.getCenterY() - pBounds.getCenterY());
	}
	
	public static boolean phiEqualToPhiInSet (double phi, Set<PolarCoordinates> pcSet) {
		int i = (int)(Math.asin(Math.sin(phi)) * 10);
		for (PolarCoordinates pc : pcSet) {
			int j = (int)(Math.asin(Math.sin(pc.phi)) * 10);
			if (i == j)
				return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static void sortByToString(java.util.List objects) {
		Collections.sort(objects, new Comparator() {
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
	}
	
	/*
	 * Calculates the distance from the centre of a rectangle with given witdh and height to a
	 * point at the boundary at given bearing phi.
	 */
	public static double distanceFromCentreToBoundaryAtBearing(double width, double height, double phi) {
		width /= 2;
		height /= 2;
		double t = Math.abs(phi);
		if (t > Math.PI/2)
			t -= t - Math.PI/2;
		if (Math.atan(height / width) < t) {
			return height / Math.sin(t);
		} else {
			return width / Math.cos(t);
		}
	}
	
	public static Vertex largestNode(Collection<Vertex> nodes) {
		Vertex largest = nodes.iterator().next();
		for (Vertex v : nodes) {
			
		}
		return null;
	}

	public static Dimension smallestDistance (Rectangle r1, Rectangle r2) {
		Rectangle union = r1.union(r2);
		int w = union.width - r1.width - r2.width;
		w = (w < 0) ? 0 : w;
		if (r1.x < r2.x) {
			w = -w;
		}
		int h = union.height - r1.height - r2.height;
		h = (h < 0) ? 0 : h;
		if (r1.y < r2.y) {
			h = -h;
		}
		return new Dimension(w,h);
	}

	public static Dimension smallestDistance (Rectangle2D r1, Rectangle2D r2) {
		Rectangle2D.Double union = new Rectangle2D.Double();
		Rectangle2D.union(r1,r2,union);
		double w = union.width - r1.getWidth() - r2.getWidth();
		w = (w < 0) ? 0 : w;
		if (r1.getX() < r2.getX()) {
			w = -w;
		}
		double h = union.height - r1.getHeight() - r2.getHeight();
		h = (h < 0) ? 0 : h;
		if (r1.getY() < r2.getY()) {
			h = -h;
		}
		return new Dimension((int)w,(int)h);
	}
	
	public static Map<GKInstance, List<Vertex>> getInstance2VertexMap (List<Vertex> verteces) {
		Map<GKInstance,List<Vertex>> m = new HashMap<GKInstance,List<Vertex>>();
		for (Vertex v : verteces) {
			GKInstance i = (GKInstance) v.getUserObject();
			List l = m.get(i);
			if (l == null) {
				l = new ArrayList<Vertex>();
				m.put(i, l);
			}
			l.add(v);
		}
		return m;
	}
	
	public static Point.Double getMeanCentrePointCoordinates (Collection<Vertex> verteces) {
		double x = 0;
		double y = 0;
		for (Vertex v : verteces) {
			x  += GraphConstants.getBounds(v.getAttributes()).getCenterX();
			y += GraphConstants.getBounds(v.getAttributes()).getCenterY();
		}
		x /= verteces.size();
		y /= verteces.size();
		return new Point.Double(x, y);
	}
	
	public static Vertex vertexClosestToMeanCentrePoint (Collection<Vertex> verteces) {
		return vertexClosestToPoint(verteces, getMeanCentrePointCoordinates(verteces));
	}
	
	public static Vertex vertexClosestToPoint (Collection<Vertex> verteces, Point.Double p) {
		double shortestD = Integer.MAX_VALUE;
		Vertex closest = null;
		for (Vertex v : verteces) {
			double d = v.centreDistanceTo(p);
			if (d < shortestD) {
				shortestD = d;
				closest = v;
			}
		}
		return closest;
	}
	
	public static Collection<GKInstance> getTopLevelPathwaysForSpecies(MySQLAdaptor dba, GKInstance species) throws Exception {
		ArrayList qr = new ArrayList();
		qr.add(dba.createAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.species, "=", species));
		qr.add(dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.frontPageItem, "IS NOT NULL", null));
		HashSet<GKInstance> out = new HashSet<GKInstance>();
		out.addAll(dba.fetchInstance(qr));
		MySQLAdaptor.QueryRequestList subquery = dba.new QueryRequestList();
		subquery.add(dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.frontPageItem, "IS NOT NULL", null));
		qr.set(1, dba.createAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.inferredFrom, "=", subquery));
		out.addAll(dba.fetchInstance(qr));
		return out;
		//return dba.fetchInstance(qr);
	}

	public static Collection<GKInstance> getTopLevelPathways(MySQLAdaptor dba) throws Exception {
		ArrayList qr = new ArrayList();
		qr.add(dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.frontPageItem, "IS NOT NULL", null));
		HashSet<GKInstance> out = new HashSet<GKInstance>();
		out.addAll(dba.fetchInstance(qr));
		MySQLAdaptor.QueryRequestList subquery = dba.new QueryRequestList();
		subquery.add(dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.frontPageItem, "IS NOT NULL", null));
		qr.set(0, dba.createAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.inferredFrom, "=", subquery));
		out.addAll(dba.fetchInstance(qr));
		return out;
	}
	
	public static Set<GKInstance> getComponentReactions(GKInstance pathway) throws Exception {
		List instructions1 = new ArrayList();
		instructions1.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Pathway, new String[]{ReactomeJavaConstants.hasEvent}, new String[]{}));
		String[] outClasses = new String[]{ReactomeJavaConstants.ReactionlikeEvent};
		Set<GKInstance> s = InstanceUtilities.followInstanceAttributes(pathway, instructions1, outClasses);
		Set<GKInstance> out = new HashSet<GKInstance>();
		for (GKInstance r : s) {
			if (r.getReferers(ReactomeJavaConstants.locatedEvent) != null) {
				out.add(r);
			}
		}
		return out;
	}
	
/*	public static void fixPrecedingEvents (Set<GKInstance> reactions) throws Exception {
		for (GKInstance reaction : reactions) {
			Collection<GKInstance> precedingEvents = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
			Set<GKInstance> precedingEventsInSet = new HashSet<GKInstance>();
			for (GKInstance precedingEvent : precedingEvents) {
				if (reactions.contains(precedingEvent)) {
					precedingEventsInSet.add(precedingEvent);
				}
			}
			if (precedingEventsInSet.isEmpty()) {
				Collection<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
				for (GKInstance entity : inputs) {
					
				}
			}
		}
	}*/
}	
