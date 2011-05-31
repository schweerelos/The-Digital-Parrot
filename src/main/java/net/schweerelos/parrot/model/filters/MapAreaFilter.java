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

package net.schweerelos.parrot.model.filters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.model.CenteredThing;
import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.util.LatLonBounds;
import net.schweerelos.parrot.util.QuadTree;

public class MapAreaFilter extends Filter {

	private LatLonBounds bounds;

	public MapAreaFilter(LatLonBounds newBounds) {
		this.bounds = newBounds;
	}

	@Override
	/*
	 * Get all NodeWrappers that are outside the currently visible area
	 */
	public Set<NodeWrapper> getMatching(ParrotModel parrotModel) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		QuadTree<CenteredThing<NodeWrapper>> locatedThings = parrotModel.getLocatedThings();
		
		// get all things
		List<CenteredThing<NodeWrapper>> allThings = locatedThings.getAll();

		// get things *inside* the currently visible area
		List<CenteredThing<NodeWrapper>> withinBoundsThings = locatedThings.get(bounds);
		// subtract visible things from all things
		// to get all things *outside* the visible area
		allThings.removeAll(withinBoundsThings);

		for (CenteredThing<NodeWrapper> centeredThing : allThings) {
			result.add(centeredThing.getValue());
		}
		return result;
	}

}
