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

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.hp.hpl.jena.ontology.OntResource;

public class TimeBasedFilter extends SimpleNodeFilter {

	private Interval interval;
	private ParrotModel model;

	public TimeBasedFilter(Interval interval, ParrotModel parrotModel) {
		this.interval = interval;
		this.model = parrotModel;
	}

	@Override
	protected boolean matches(NodeWrapper nodeWrapper) {
		boolean timedThing = false;
		boolean inInterval = false;
		
		if (interval == null) {
			Logger logger = Logger.getLogger(TimeBasedFilter.class);
			logger.warn("no interval!");
			inInterval = true;
			return accept(timedThing, inInterval);
		}
		if (!nodeWrapper.isOntResource()) {
			timedThing = false;
			return accept(timedThing, inInterval);
		}
		OntResource node = nodeWrapper.getOntResource();
		if (!TimedThingsHelper.isTimedThing(node, model)) {
			timedThing = false;
			return accept(timedThing, inInterval);
		}

		try {
			timedThing = true;
			DateTime startsAt = TimedThingsHelper.extractStartDate(node, model);
			DateTime endsAt = TimedThingsHelper.extractEndDate(node, model);
			inInterval = interval.contains(new Interval(startsAt, endsAt));
			return accept(timedThing, inInterval);
		} catch (NotTimedThingException ntte) {
			timedThing = false;
			return accept(timedThing, inInterval);
		}
	}

	private boolean accept(boolean timedThing, boolean inInterval) {
		if (getMode() == Mode.HIGHLIGHT) {
			return timedThing && inInterval;
		} else { // mode must be restrict
			return timedThing && !inInterval;
		}
	}

}
