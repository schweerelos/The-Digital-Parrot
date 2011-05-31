/*
 * Copyright (C) 2011 Andrea Schweer
 *
 * This file is part of the Digital Parrot. 
 *
 * The Digital Parrot is free software; you can redistribute it and/or modify
 * it under the terms of the Eclipse Public License as published by the Eclipse
 * Foundation or its Agreement Steward, either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * The Digital Parrot is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the Eclipse Public License for
 * more details.
 *
 * You should have received a copy of the Eclipse Public License along with the
 * Digital Parrot. If not, see http://www.eclipse.org/legal/epl-v10.html. 
 *
 */


package net.schweerelos.parrot.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import net.schweerelos.parrot.model.NodeWrapper;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.BasicRenderer;

public class ParrotGraphRenderer extends
		BasicRenderer<NodeWrapper, NodeWrapper> {

	@Override
	public void render(RenderContext<NodeWrapper, NodeWrapper> renderContext,
			Layout<NodeWrapper, NodeWrapper> layout) {
		// paint all the edges
		try {
			for (NodeWrapper edge : getGraphEdges(renderContext, layout)) {
				renderEdge(renderContext, layout, edge);
				renderEdgeLabel(renderContext, layout, edge);
			}
		} catch (ConcurrentModificationException cme) {
			renderContext.getScreenDevice().repaint();
		}

		// paint all the vertices
		try {
			for (NodeWrapper vertex : getGraphVertices(renderContext, layout)) {
				renderVertex(renderContext, layout, vertex);
				renderVertexLabel(renderContext, layout, vertex);
			}
		} catch (ConcurrentModificationException cme) {
			renderContext.getScreenDevice().repaint();
		}
	}

	public Collection<NodeWrapper> getGraphVertices(
			RenderContext<NodeWrapper, NodeWrapper> renderContext,
			Layout<NodeWrapper, NodeWrapper> layout) {
		PickedState<NodeWrapper> vertexPickInfo = renderContext
				.getPickedVertexState();
		PickedState<NodeWrapper> edgePickInfo = renderContext
				.getPickedEdgeState();

		Graph<NodeWrapper, NodeWrapper> graph = layout.getGraph();

		List<NodeWrapper> picked = new ArrayList<NodeWrapper>();
		List<NodeWrapper> adjacentEdgePicked = new ArrayList<NodeWrapper>();
		List<NodeWrapper> neighbourPicked = new ArrayList<NodeWrapper>();
		List<NodeWrapper> highlighted = new ArrayList<NodeWrapper>();
		List<NodeWrapper> adjacentEdgeHighlighted = new ArrayList<NodeWrapper>();
		List<NodeWrapper> neighbourHighlighted = new ArrayList<NodeWrapper>();
		List<NodeWrapper> others = new ArrayList<NodeWrapper>();

		Collection<NodeWrapper> vertices = graph.getVertices();

		// separate these vertices into different groups
		// depending on whether they (or neighbour vertices 
		// or adjacent edges) are highlighted/picked
		for (NodeWrapper vertex : vertices) {
			if (vertexPickInfo.isPicked(vertex)) {
				// picked vertices are more important than unpicked ones
				picked.add(vertex);
			} else if (GraphViewHelper.hasPickedAdjacentEdge(vertex,
					edgePickInfo, graph)) {
				// source and target vertices of picked edges are more important
				// than others
				adjacentEdgePicked.add(vertex);
			} else if (GraphViewHelper.hasPickedNeighbour(vertex,
					vertexPickInfo, graph)) {
				// neighbours of picked vertices are more important than others
				neighbourPicked.add(vertex);
			} else if (vertex.isHighlighted()) {
				// highlighted vertices are more important than others
				highlighted.add(vertex);
			} else if (GraphViewHelper
					.hasHighlightedAdjacentEdge(vertex, graph)) {
				// source and target vertices of highlighted edges are more
				// important than others
				adjacentEdgeHighlighted.add(vertex);
			} else if (GraphViewHelper.hasHighlightedNeighbour(vertex, graph)) {
				// neighbours of highlighted vertices are more important than
				// others
				neighbourHighlighted.add(vertex);
			} else {
				others.add(vertex);
			}
		}

		List<NodeWrapper> result = new ArrayList<NodeWrapper>();

		// make sure that result has all vertices, least important first
		result.addAll(others);
		result.addAll(neighbourHighlighted);
		result.addAll(adjacentEdgeHighlighted);
		result.addAll(highlighted);
		result.addAll(neighbourPicked);
		result.addAll(adjacentEdgePicked);
		result.addAll(picked);

		return result;
	}

	public Collection<NodeWrapper> getGraphEdges(
			RenderContext<NodeWrapper, NodeWrapper> renderContext,
			Layout<NodeWrapper, NodeWrapper> layout) {
		PickedState<NodeWrapper> edgePickInfo = renderContext
				.getPickedEdgeState();

		Graph<NodeWrapper, NodeWrapper> graph = layout.getGraph();

		Collection<NodeWrapper> edges = graph.getEdges();

		List<NodeWrapper> picked = new ArrayList<NodeWrapper>();
		List<NodeWrapper> adjacentVertexPicked = new ArrayList<NodeWrapper>();
		List<NodeWrapper> highlighted = new ArrayList<NodeWrapper>();
		List<NodeWrapper> adjacentVertexHighlighted = new ArrayList<NodeWrapper>();
		List<NodeWrapper> others = new ArrayList<NodeWrapper>();

		for (NodeWrapper edge : edges) {
			if (edgePickInfo.isPicked(edge)) {
				// picked edges are more important than unpicked ones
				picked.add(edge);
			} else if (GraphViewHelper.hasPickedAdjacentVertex(edge,
					edgePickInfo, graph)) {
				// outgoing edges of picked nodes are more important than others
				adjacentVertexPicked.add(edge);
			} else if (edge.isHighlighted()) {
				// highlighted edges are more important than others
				highlighted.add(edge);
			} else if (GraphViewHelper
					.hasHighlightedAdjacentVertex(edge, graph)) {
				// outgoing edges of highlighted nodes are more important than
				// others
				adjacentVertexHighlighted.add(edge);
			} else {
				others.add(edge);
			}
		}

		List<NodeWrapper> result = new ArrayList<NodeWrapper>();

		result.addAll(others);
		result.addAll(adjacentVertexHighlighted);
		result.addAll(highlighted);
		result.addAll(adjacentVertexPicked);
		result.addAll(picked);

		return result;
	}
}
