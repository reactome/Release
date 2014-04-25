package org.gk.pathwaylayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class VertexClusterer {
	
	public int min_distance = PathwayLayoutConstants.NEIGHBOURHOOD_RADIUS;
	List<List<Vertex>> clusters;
	//List<VertexCluster> clusters;
	
	public VertexClusterer () {
		
	}
	
	public List<List<Vertex>> cluster (List<Vertex> nodes) {
		clusters = new ArrayList<List<Vertex>>();
		for (int i = 0; i < nodes.size(); ++i) {
			Vertex v1 = nodes.get(i);
			List<Vertex> cluster = null;
			for (int j = i + 1; j < nodes.size(); ++j) {
				Vertex v2 = nodes.get(j);
				if (v1.centreDistanceTo(v2) <= min_distance) {
					if (cluster == null) {
						cluster = new ArrayList<Vertex>();
						clusters.add(cluster);
						cluster.add(v1);
					}
					cluster.add(v2);
				}
			}
		}
		// The following can surely be optimised
		boolean cont = true;
		while (cont) {
			cont = false;
			OUTER: for (int i = 0; i < clusters.size(); ++i) {
				List<Vertex> cluster1 = clusters.get(i);
				List<List<Vertex>> include = new ArrayList<List<Vertex>>();
				INNER: for (int j = i + 1; j < clusters.size(); ++j) {
					List<Vertex> cluster2 = clusters.get(j);
					for (Vertex v1 : cluster1) {
						for (Vertex v2 : cluster2) {
							if (v1.centreDistanceTo(v2) <= min_distance) {
								include.add(cluster2);
								continue INNER;
							}
						}
					}
				}
				if (!include.isEmpty()) {
					for (List<Vertex> cluster : include) {
						for (Vertex v : cluster) {
							if (!cluster1.contains(v)) {
								cluster1.add(v);
							}
						}
						clusters.remove(cluster);
					}
					cont = true;
				}
			}
		}
		if (clusters.isEmpty()) {
			return null;
		}
		return clusters;
	}
	
/*	public List<List<Vertex>> cluster (List<Vertex> nodes) {
		clusters = new ArrayList<VertexCluster>();
		for (int i = 0; i < nodes.size(); ++i) {
			Vertex v1 = nodes.get(i);
			VertexCluster cluster = null;
			for (int j = i + 1; j < nodes.size(); ++j) {
				Vertex v2 = nodes.get(j);
				if (v1.distanceTo(v2) <= min_distance) {
					if (cluster == null) {
						cluster = new VertexCluster();
						clusters.add(cluster);
						cluster.add(v1);
					}
					cluster.add(v2);
				}
			}
		}
		// The following can surely be optimised
		boolean cont = true;
		while (cont) {
			cont = false;
			OUTER: for (int i = 0; i < clusters.size(); ++i) {
				VertexCluster cluster1 = clusters.get(i);
				List<VertexCluster> include = new ArrayList<VertexCluster>();
				INNER: for (int j = i + 1; j < clusters.size(); ++j) {
					VertexCluster cluster2 = clusters.get(j);
					for (Vertex v1 : cluster1) {
						for (Vertex v2 : cluster2) {
							if (v1.distanceTo(v2) <= min_distance) {
								include.add(cluster2);
								continue INNER;
							}
						}
					}
				}
				if (!include.isEmpty()) {
					for (List<Vertex> cluster : include) {
						for (Vertex v : cluster) {
							cluster1.add(v);
						}
						clusters.remove(cluster);
					}
					cont = true;
				}
			}
		}
		if (clusters.isEmpty()) {
			return null;
		}
		return (List<List<Vertex>>)clusters;
	}
	
	class VertexCluster extends ArrayList<Vertex> {
		
		public VertexCluster () {
			super();
		}
		
		public VertexCluster (Collection<Vertex> c) {
			super(c);
		}
		
		public boolean add (Vertex v) {
			if (contains(v)) {
				return false;
			}
			add(v);
			return true;
		}
		
		public Vertex get (int i) {
			return (Vertex) super.get(i);
		}
		
		public Vertex[] toArray () {
			return (Vertex[]) super.toArray();
		}
	}*/
	
}
