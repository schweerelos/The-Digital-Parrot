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

import java.util.Collection;

import net.schweerelos.parrot.model.NodeWrapper;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.picking.PickedState;

public abstract class GraphViewHelper {

	public static boolean hasHighlightedAdjacentEdge(NodeWrapper vertex,
			Graph<NodeWrapper, NodeWrapper> graph) {
		Collection<NodeWrapper> edges = graph.getIncidentEdges(vertex);
		for (NodeWrapper edge : edges) {
			if (edge.isHighlighted()) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPickedAdjacentEdge(NodeWrapper vertex,
			PickedState<NodeWrapper> edgePickInfo, Graph<NodeWrapper, NodeWrapper> graph) {
		Collection<NodeWrapper> edges = graph.getIncidentEdges(vertex);
		for (NodeWrapper edge : edges) {
			if (edgePickInfo.isPicked(edge)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasHighlightedNeighbour(NodeWrapper vertex, Graph<NodeWrapper, NodeWrapper> graph) {
		Collection<NodeWrapper> neighbours = graph.getNeighbors(vertex);
		for (NodeWrapper neighbour : neighbours) {
			if (neighbour.isHighlighted()) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPickedNeighbour(NodeWrapper vertex,
			PickedInfo<NodeWrapper> vertexPickInfo, Graph<NodeWrapper, NodeWrapper> graph) {
		Collection<NodeWrapper> neighbours = graph.getNeighbors(vertex);
		for (NodeWrapper neighbour : neighbours) {
			if (vertexPickInfo.isPicked(neighbour)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPickedAdjacentVertex(NodeWrapper edge, PickedInfo<NodeWrapper> vertexPickInfo, Graph<NodeWrapper, NodeWrapper> graph) {
		Pair<NodeWrapper> endpoints = graph.getEndpoints(edge);
		return vertexPickInfo.isPicked(endpoints.getFirst()) || vertexPickInfo.isPicked(endpoints.getSecond());
	}

	public static boolean hasHighlightedAdjacentVertex(NodeWrapper edge,
			Graph<NodeWrapper, NodeWrapper> graph) {
		Pair<NodeWrapper> endpoints = graph.getEndpoints(edge);
		return endpoints.getFirst().isHighlighted() || endpoints.getSecond().isHighlighted();
	}

}
