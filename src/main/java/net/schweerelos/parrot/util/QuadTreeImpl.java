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


package net.schweerelos.parrot.util;

import java.util.List;


public class QuadTreeImpl<T> implements QuadTree<T> {

	private com.bbn.openmap.util.quadtree.QuadTree delegate;
	
	public QuadTreeImpl() {
		delegate = new com.bbn.openmap.util.quadtree.QuadTree();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<T> get(LatLonBounds bounds) {
		return (List<T>) delegate.get(bounds.topLeftLat, bounds.topLeftLon, bounds.bottomRightLat, bounds.bottomRightLon);
	}

	@Override
	public List<T> getAll() {
		return get(new LatLonBounds(90,	-180, -90, 180));
	}

	@Override
	public void put(float lat, float lon, T value) {
		delegate.put(lat, lon, value);
	}

}
