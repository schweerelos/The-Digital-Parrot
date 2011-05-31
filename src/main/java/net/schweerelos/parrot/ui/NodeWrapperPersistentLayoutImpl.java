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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.schweerelos.parrot.model.NodeWrapper;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.layout.ObservableCachingLayout;
import edu.uci.ics.jung.visualization.layout.PersistentLayout;
import edu.uci.ics.jung.visualization.util.Caching;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;

/**
 * Implementation of PersistentLayout for NodeWrappers -- which means it deals with the value of each vertex rather than the vertex itself.
 * Defers to another layout until 'restore' is called,
 * then it uses the saved vertex locations.
 * 
 * @author Andrea Schweer <schweer@cs.waikato.ac.nz>
 * 
 * @see edu.uci.ics.jung.visualization.layout.PersistentLayoutImpl<V, E>
 */
public class NodeWrapperPersistentLayoutImpl extends ObservableCachingLayout<NodeWrapper, NodeWrapper>
implements PersistentLayout<NodeWrapper, NodeWrapper>,  ChangeEventSupport, Caching {

	/** Maps NodeWrapper URIs to the location of their respective vertex. */
	protected Map<String, Point> uriToNodeLocation;
	/** Holds all vertices whose position has been locked. */
	protected Set<NodeWrapper> lockedVertices;
	/** Whether the graph is locked (stops the VisualizationViewer rendering thread). */
	protected boolean locked;
	
	public NodeWrapperPersistentLayoutImpl(
			Layout<NodeWrapper, NodeWrapper> delegate) {
		super(delegate);
		
		uriToNodeLocation = LazyMap.decorate(new HashMap<String, Point>(), new RandomPointFactory(getSize()));
		lockedVertices = new HashSet<NodeWrapper>();
		locked = false;
	}

	@Override
	public synchronized void persist(String fileName) throws IOException {
		uriToNodeLocation.clear();
		
        for(NodeWrapper vertex : getGraph().getVertices()) {
            Point p = new Point(transform(vertex));
            uriToNodeLocation.put(vertex.getOntResource().getURI(), p);
        }
        
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                fileName));
        oos.writeObject(uriToNodeLocation);
        oos.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void restore(String fileName) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                fileName));
        uriToNodeLocation = (Map<String, Point>) ois.readObject();
        ois.close();
        
        initializeLocations();
        
        locked = true;
        fireStateChanged();
	}

	protected void initializeLocations() {
        for(NodeWrapper vertex : getGraph().getVertices()) {
            Point2D coord = delegate.transform(vertex);
            if (!lockedVertices.contains(vertex))
                initializeLocation(vertex, coord, getSize());
        }
	}

	protected void initializeLocation(NodeWrapper vertex, Point2D coord,
			Dimension size) {
        String uri = vertex.getOntResource().getURI();
		if (uri != null && !uri.isEmpty() && uriToNodeLocation.containsKey(uri)) {
			Point point = uriToNodeLocation.get(uri);
			coord.setLocation(point.x, point.y);
		} else {
			// TODO
		}
	}

	@Override
    public void lock(boolean locked) {
        this.locked = locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.Layout#incrementsAreDone()
     */
	@Override
    public boolean done() {
        return locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.Layout#lockVertex(edu.uci.ics.jung.graph.Vertex)
     */
	@Override
    public void lock(NodeWrapper vertex, boolean state) {
        lockedVertices.add(vertex);
        delegate.lock(vertex, state);
    }

	@SuppressWarnings("serial")
	public static class RandomPointFactory implements Factory<Point>, Serializable {

    	Dimension d;
    	public RandomPointFactory(Dimension d) {
    		this.d = d;
    	}
		public edu.uci.ics.jung.visualization.layout.PersistentLayout.Point create() {
	            double x = Math.random() * d.width;
	            double y = Math.random() * d.height;
				return new Point(x,y);
		}
    }
}
