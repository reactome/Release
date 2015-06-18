package org.reactome.server.fireworks.layout;

import org.reactome.server.fireworks.model.GraphNode;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksLayout {

    private static double MIN_RADIUS_STEP = Branch.RADIUS_DELTA;
    private GraphNode graph;
    private Bursts bursts;

    public FireworksLayout(Bursts bursts, GraphNode graph) {
        this.bursts = bursts;
        this.graph = graph;
    }

    public void doLayout(){
        long start = System.currentTimeMillis();
        for (GraphNode node : this.graph.getChildren()) {
            Burst burst = this.bursts.getBurst(node.getDbId());
            if(burst!=null) {
                node.setLayoutParameters(burst.getCenterX(), burst.getCenterY(), 0, MIN_RADIUS_STEP, 0, 2 * Math.PI);
                double startAngle = Math.toRadians(burst.getStartAngle());
                double endAngle = Math.toRadians(burst.getEndAngle());

                //For "Mycobacterium tuberculosis" there is only one burst covering the 100% of the species
                //so the first node is so big that we need 3 times the MIN_RADIUS_STEP to make it look nice.
                double minRadius = node.getRatio() < 1.0 ? MIN_RADIUS_STEP : MIN_RADIUS_STEP * 3;

                Branch branch = new Branch(burst, minRadius, startAngle, endAngle, node.getChildren());
                branch.setNodesPosition();
                for (GraphNode child : node.getChildren()) {
                    fillBranch(burst, branch.getRadius() + minRadius, child);
                }
            }else if(!node.hasLayoutData()){
//                System.err.println(getClass().getSimpleName() + " >> " + node.getDbId() + " - " + node.getName() + " not found in the bursts config file.");
            }
        }
        System.out.println("Layout execution time: " + (System.currentTimeMillis() - start));
    }

    private void fillBranch(Burst burst, double minRadius, GraphNode node){
        Branch branch = new Branch(burst, minRadius, node.getMinAngle(), node.getMaxAngle(), node.getChildren());
        branch.setNodesPosition();
        double radius = branch.getRadius() + MIN_RADIUS_STEP;
        for (GraphNode child : node.getChildren()) {
            fillBranch(burst, radius, child);
        }
    }
}
