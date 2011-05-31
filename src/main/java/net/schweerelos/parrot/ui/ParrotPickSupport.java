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

import java.awt.Shape;
import java.util.Collection;
import java.util.LinkedHashSet;

import net.schweerelos.parrot.model.NodeWrapper;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.picking.ShapePickSupport;
import edu.uci.ics.jung.visualization.renderers.Renderer;

public class ParrotPickSupport extends ShapePickSupport<NodeWrapper, NodeWrapper> implements
		GraphElementAccessor<NodeWrapper, NodeWrapper> {

	public ParrotPickSupport(VisualizationServer<NodeWrapper, NodeWrapper> vv) {
		super(vv);
		super.style = ShapePickSupport.Style.HIGHEST;
	}

	@Override
	public NodeWrapper getEdge(Layout<NodeWrapper, NodeWrapper> layout,
			double x, double y) {
		return super.getEdge(layout, x, y);
	}

	@Override
	public NodeWrapper getVertex(Layout<NodeWrapper, NodeWrapper> layout,
			double x, double y) {
		return super.getVertex(layout, x, y);
	}

	@Override
	public Collection<NodeWrapper> getVertices(
			Layout<NodeWrapper, NodeWrapper> layout, Shape rectangle) {
		return super.getVertices(layout, rectangle);
	}

	@Override
	protected Collection<NodeWrapper> getFilteredVertices(
			Layout<NodeWrapper, NodeWrapper> layout) {
		Collection<NodeWrapper> graphVertices;
		Renderer<NodeWrapper, NodeWrapper> renderer = super.vv.getRenderer();
		if (renderer instanceof ParrotGraphRenderer) {
			graphVertices = ((ParrotGraphRenderer) renderer).getGraphVertices(super.vv.getRenderContext(), layout);
		} else {
			graphVertices = layout.getGraph().getVertices();
		}
		if(verticesAreFiltered()) {
    		Collection<NodeWrapper> unfiltered = graphVertices;
    		Collection<NodeWrapper> filtered = new LinkedHashSet<NodeWrapper>();
    		for(NodeWrapper v : unfiltered) {
    			if(isVertexRendered(Context.<Graph<NodeWrapper,NodeWrapper>,NodeWrapper>getInstance(layout.getGraph(),v))) {
    				filtered.add(v);
    			}
    		}
    		return filtered;
    	} else {
    		return graphVertices;
    	}
	}

	
	
}
