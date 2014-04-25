/*
 * Created on Mar 1, 2004
 */
package org.gk.graphEditor;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.gk.render.*;

/**
 * A GraphEditorAction for LinkWidget related actions.
 * @author wugm
 */
public class LinkWidgetAction implements GraphEditorAction {
	protected final int NEW_NODE_DISTANCE = 100;
	// The parent GraphEditorPane this action will be working on.
	private GraphEditorPane graphPane;
	private Point pressPoint;

	public LinkWidgetAction(GraphEditorPane graphPane) {
		this.graphPane = graphPane;
	}
	
	public void doAction(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			pressPoint = e.getPoint();
		}
		else if (e.getID() == MouseEvent.MOUSE_CLICKED) {
			insertNode(e);
		}
		else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			dragFlowLine(e);
		}
	}
	
	private void dragFlowLine(MouseEvent e) {
        // The graphPane might be scaled
        double x = e.getX() / graphPane.getScaleX();
        double y = e.getY() / graphPane.getScaleY();
		int selectionDirection = graphPane.isLinkWidgetPicked((int)x, (int)y);
		if (selectionDirection == GraphEditorPane.LINK_WIDGET_NONE)
			return; // Guide condition
		Node node = (Node) graphPane.getSelection().get(0);
		FlowLine flowLine = new FlowLine();
		Rectangle rect = node.getBounds();
		Point selectPoint = null;
		switch(selectionDirection) {
			case GraphEditorPane.LINK_WIDGET_EAST :
				// Have to set the positions first before attaching the node.
				selectPoint = new Point(rect.x + rect.width + DefaultRenderConstants.LINK_WIDGET_WIDTH, 
				                        rect.y + rect.height / 2);
				break;
			case GraphEditorPane.LINK_WIDGET_SOUTH :
				selectPoint = new Point(rect.x + rect.width / 2,
				                        rect.y + rect.height + DefaultRenderConstants.LINK_WIDGET_WIDTH);
				break;
			case GraphEditorPane.LINK_WIDGET_WEST :
				selectPoint = new Point(rect.x - DefaultRenderConstants.LINK_WIDGET_WIDTH,
				                        rect.y + rect.height / 2);
				break;
			case GraphEditorPane.LINK_WIDGET_NORTH :
				selectPoint = new Point(rect.x + rect.width / 2,
				                        rect.y - DefaultRenderConstants.LINK_WIDGET_WIDTH);
				break;
		}
        // Changed to be used as output no matter the direction as of Dec 11, 2006.
        flowLine.setInputHub(node.getLinkPoint());
        // Fixed input hub for gene
        flowLine.setOutputHub(selectPoint);
        flowLine.addInput(node);
		ConnectWidget inputWidget = ((HyperEdgeConnectInfo)flowLine.getConnectInfo()).getConnectWidget(node);
        graphPane.insertEdge(flowLine, false);
		if (flowLine.isPicked(selectPoint)) { // Call this method to set up the correct connect widget.
			                                  // Cannot fail.
			graphPane.setShouldDrawLinkWidgets(false);
			graphPane.getSelectionModel().removeSelected(node);
			graphPane.getSelectionModel().addSelected(flowLine);
			graphPane.currentAction = graphPane.connectAction;
			graphPane.connectAction.prevPoint = pressPoint;
			graphPane.connectAction.setConnectWidget(flowLine.getConnectWidget());
			graphPane.connectAction.doAction(e);
		}
	}
	
	protected void insertNode(MouseEvent e) {
		int selectedDirection = graphPane.isLinkWidgetPicked(e.getX(), e.getY());		
		Node node = (Node) graphPane.getSelection().get(0);
		Node newNode = null;
		if (node instanceof RenderablePathway) {
			newNode = new RenderablePathway();
			newNode.setDisplayName("Pathway");
			String displayName = RenderableRegistry.getRegistry().generateUniqueName(newNode);
			newNode.setDisplayName(displayName);
		}
		else if (node instanceof ReactionNode) {
			RenderableReaction reaction = new RenderableReaction();
			reaction.initPosition(new Point(100, 100)); // Default position.
			reaction.setDisplayName("Reaction");
			String displayName = RenderableRegistry.getRegistry().generateUniqueName(reaction);
			reaction.setDisplayName(displayName);
			newNode = new ReactionNode(reaction);                                                      
		}
		if (newNode == null)
			return;
		// Need to register it
		RenderableRegistry.getRegistry().add(newNode);
		Point position1 = node.getPosition();
		Dimension size = node.getBounds().getSize();
		Point position = new Point();
		// link
		FlowLine flowLine = new FlowLine();
		boolean isInput = true;
		switch (selectedDirection) {
			case GraphEditorPane.LINK_WIDGET_EAST :
				position.x = position1.x + size.width / 2 + NEW_NODE_DISTANCE;
				position.y = position1.y;
				isInput = false;
				break;
			case GraphEditorPane.LINK_WIDGET_SOUTH :
				position.x = position1.x;
				position.y = position1.y + size.height / 2 + NEW_NODE_DISTANCE;
				isInput = false;
				break;
			case GraphEditorPane.LINK_WIDGET_WEST :
				position.x = position1.x - size.width / 2 - NEW_NODE_DISTANCE;
				if (position.x < 10)
					position.x = 10;
				position.y = position1.y;
				isInput = true;
				break;
			case GraphEditorPane.LINK_WIDGET_NORTH :
				position.x = position1.x;
				position.y = position1.y - size.height / 2 - NEW_NODE_DISTANCE;
				if (position.y < 10)
					position.y = 10;
				isInput = true;
				break;
		}
		newNode.setPosition(position);
		graphPane.insertNode(newNode);
		// Initialize the positions and attach nodes
		if (isInput) {
			flowLine.setInputHub(newNode.getPosition());
			flowLine.setOutputHub(node.getPosition());
			flowLine.addInput(newNode);
			flowLine.addOutput(node);
		}
		else {
			flowLine.setOutputHub(newNode.getPosition());
			flowLine.setInputHub(node.getPosition());
			flowLine.addOutput(newNode);
			flowLine.addInput(node);
		}
		graphPane.insertEdge(flowLine, false);
		// Select the newNode
		graphPane.getSelectionModel().removeSelected(node);
		graphPane.getSelectionModel().addSelected(newNode);
		graphPane.setShouldDrawLinkWidgets(false);
		graphPane.repaint(); //TODO: Have to minimize the repaint area.
		final Renderable newNode1 = newNode;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				graphPane.revalidate();
				// It might be not initialized because of synchronization issue.
				if (newNode1.getBounds() != null)
					graphPane.scrollRectToVisible(newNode1.getBounds());
			}
		});
	}

}
