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

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.NotTimedThingException;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.TimedThingsHelper;
import net.schweerelos.timeline.model.IntervalChain;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.hp.hpl.jena.ontology.OntResource;

public class IntervalChainBasedFilter extends SimpleNodeFilter {
	private IntervalChain<NodeWrapper> intervals;
	private ParrotModel model;
	
	public IntervalChainBasedFilter(IntervalChain<NodeWrapper> intervals, ParrotModel model) {
		this.intervals = intervals;	
		this.model = model;
	}
	
	protected boolean matches(NodeWrapper nodeWrapper) {
		if (intervals == null) {
			Logger logger = Logger.getLogger(IntervalChainBasedFilter.class);
			logger.warn("no intervals!");
			return false;
		}
		if (!nodeWrapper.isOntResource()) {
			return false;
		}
		OntResource node = nodeWrapper.getOntResource();
		if (!TimedThingsHelper.isTimedThing(node, model)) {
			return false;
		}
		try {
			DateTime startsAt = TimedThingsHelper.extractStartDate(node, model);
			DateTime endsAt = TimedThingsHelper.extractEndDate(node, model);
			return intervals.contains(new Interval(startsAt, endsAt));
		} catch (NotTimedThingException ntte) {
			return false;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		switch (getMode()) {
		case HIGHLIGHT:
			result.append("Highlight");
			break;
		case RESTRICT:
			result.append("Restrict to");
			break;
		}
		result.append(" everything");
		if (intervals != null) {
			result.append(" in ");
			result.append(intervals.toString());
		}
		return result.toString();
	}

}
