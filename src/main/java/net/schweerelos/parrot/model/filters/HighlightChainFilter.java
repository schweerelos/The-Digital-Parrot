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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public class HighlightChainFilter extends Filter {

	private RestrictToChainFilter baseFilter;
	private Set<NodeWrapper> matching;

	public HighlightChainFilter(RestrictToChainFilter baseFilter) {
		this.baseFilter = baseFilter;
		super.setMode(Mode.HIGHLIGHT);
	}

	@Override
	public Set<NodeWrapper> getMatching(ParrotModel parrotModel) {
		if (matching == null) {
			matching = calculateMatching(parrotModel);
		}
		return matching;
	}

	
	
	@Override
	public void setMode(Mode mode) {
		// don't do anything
	}

	private Set<NodeWrapper> calculateMatching(ParrotModel parrotModel) {
		List<ChainLink> highlightChain = new ArrayList<ChainLink>();

		List<ChainLink> links = baseFilter.getLinks();
		// don't highlight anything if the baseFilter's chain is empty
		if (links.isEmpty()) {
			return Collections.emptySet();
		}

		highlightChain.addAll(links);

		// don't highlight the last link if it's any/any
		int lastIndex = highlightChain.size() - 1;
		ChainLink lastLink = highlightChain.get(lastIndex);
		if (!lastLink.hasInstance() && !lastLink.hasType()) {
			highlightChain.remove(lastIndex);
		}

		// the only way this can happen is if the baseFilter's chain had two
		// any/any links and nothing else, which should never happen -- but
		// checking just in case, because calling getNodeWrappersOnChain for a
		// chain with only any/any might be expensive
		if (highlightChain.isEmpty()) {
			return Collections.emptySet();
		}

		return parrotModel.getNodeWrappersOnChain(highlightChain);
	}

}
