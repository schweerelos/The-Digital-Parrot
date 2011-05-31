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
import java.util.Set;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public abstract class SimpleFilter extends Filter {

	@Override
	public Set<NodeWrapper> getMatching(ParrotModel parrotModel) {
		Set<NodeWrapper> applicableNodeWrappers = extractApplicableNodeWrappers(parrotModel);
		
		Set<NodeWrapper> matching = new HashSet<NodeWrapper>();
		for (NodeWrapper nodeWrapper : applicableNodeWrappers) {
			if (matches(nodeWrapper)) {
				matching.add(nodeWrapper);
			}
		}
		return matching;
	}

	protected abstract boolean matches(NodeWrapper nodeWrapper);

	protected abstract Set<NodeWrapper> extractApplicableNodeWrappers(
			ParrotModel parrotModel);

}
