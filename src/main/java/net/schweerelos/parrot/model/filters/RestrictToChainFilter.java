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

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public class RestrictToChainFilter extends Filter {

	private List<ChainLink> links;
	
	public RestrictToChainFilter(Chain chain) {
		links = chain.getLinksSnapshot();
		super.setMode(Mode.RESTRICT);
	}

	@Override
	public Set<NodeWrapper> getMatching(ParrotModel parrotModel) {
		Set<NodeWrapper> chainWrappers = parrotModel.getNodeWrappersOnChain(getLinks());
		Set<NodeWrapper> wrappers = parrotModel.getAllNodeWrappers();
		wrappers.removeAll(chainWrappers);
		return wrappers;
	}

	@Override
	public String toString() {
		return String.format("[Connections filter, chain %s]\n", links);
	}

	public List<ChainLink> getLinks() {
		return links;
	}

	@Override
	public void setMode(Mode mode) {
		// don't do anything
	}

	public boolean sameChain(Chain chain) {
		List<ChainLink> chainLinks = chain.getLinks();
		if (chainLinks.size() != links.size()) {
			return false;
		}
		Comparator<ChainLink> comp = ChainLink.getCloneComparator();
		for (int i = 0; i < links.size(); i++) {
			ChainLink filterLink = links.get(i);
			ChainLink chainLink = chainLinks.get(i);
			if (comp.compare(filterLink, chainLink) != 0) {
				// found a pair that's not same
				return false;
			}
		}
		// if we get here, we never found one pair of links that doesn't match
		// -> they all match
		return true;
	}	
	
}
