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

package net.schweerelos.timeline.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;

public class IntervalChain<T extends Object> implements Iterable<PayloadInterval<T>> {
	private List<PayloadInterval<T>> intervals;
	private DateTime firstStart;
	private DateTime lastEnd;

	public IntervalChain() {
		intervals = new ArrayList<PayloadInterval<T>>();
	}
	
	public List<PayloadInterval<T>> getIntervals() {
		return intervals;
	}
	
	public DateTime getFirstStart() {
		return firstStart;
	}
	
	public DateTime getLastEnd() {
		return lastEnd;
	}
	
	public void add(PayloadInterval<T> interval) {
		intervals.add(interval);
		if (firstStart == null || interval.getStart().isBefore(firstStart)) {
			firstStart = interval.getStart();
		}
		if (lastEnd == null || interval.getEnd().isAfter(lastEnd)) {
			lastEnd = interval.getEnd();
		}
	}

	public boolean contains(Interval interval) {
		if (interval.getStart().isBefore(firstStart) || interval.getEnd().isAfter(lastEnd)) {
			return false;
		}
		for (PayloadInterval<T> chainInterval : intervals) {
			if (chainInterval.contains(interval)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<PayloadInterval<T>> iterator() {
		return intervals.iterator();
	}
	
	@Override 
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("interval between ");
		result.append(firstStart);
		result.append(" and ");
		result.append(lastEnd);
		result.append(": ");
		boolean first = true;
		for (PayloadInterval<T> interval : intervals) {
			if (!first) {
				result.append(" and ");
			}
			result.append(interval.getStart());
			result.append(" to ");
			result.append(interval.getEnd());
			first = false;
		}
		return result.toString();
	}
}
