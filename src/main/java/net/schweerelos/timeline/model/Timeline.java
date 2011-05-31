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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.timeline.IntervalListener;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.Weeks;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Timeline<T> {

	private static final String INTERVAL_PROPERTY_KEY = "interval";

	public enum Mode { Years, Months, Weeks,  Days	}
	
	private IntervalChain<T> allIntervals;
	private Set<PayloadInterval<T>> currentlyVisibleIntervals = new HashSet<PayloadInterval<T>>();
	private Set<PayloadInterval<T>> intervalsWithinRange = new HashSet<PayloadInterval<T>>();

	private DateTime start;
	private DateTime end;

	private int numSlices;
	
	private Period increment;
	private Mode incrementMode;
	
	private PropertyChangeSupport changeSupport;
	
	private SliceLabelExtractor sliceLabelExtractor;
	
	private Logger logger;
	
	public Timeline(IntervalChain<T> intervals) {
		logger = Logger.getLogger(Timeline.class);
		
		allIntervals = intervals;
		changeSupport = new PropertyChangeSupport(this);
		if (intervals != null) {
			setInterval(intervals.getFirstStart(), intervals.getLastEnd());
		}
	}
	
	public void setInterval(DateTime start, DateTime end) {
		DateTimeZone defaultTimeZone = DateTimeZone.getDefault();
		
		// make sure we don't start before the start of the intervals (in years)
		DateTime firstStart = allIntervals.getFirstStart();
		firstStart = firstStart.withDayOfYear(firstStart.dayOfYear().getMinimumValue());
		if (start != null && start.toDateTime(defaultTimeZone).isBefore(firstStart)) {
			this.start = firstStart;
		} else {
			this.start = start; 
		}
		
		// make sure we don't end after the end of the intervals (in years)
		DateTime lastEnd = allIntervals.getLastEnd();
		lastEnd = lastEnd.withDayOfYear(lastEnd.dayOfYear().getMaximumValue());
		if (end != null && end.toDateTime(defaultTimeZone).isAfter(lastEnd)) {
			this.end = lastEnd;
		} else {
			this.end = end;
		}
				
		recalculate();
	}

	private void recalculate() {
		if (start == null || end == null) {
			logger.warn("recalculating aborted, start and/or end is null");
			numSlices = 0;
			return;
		}
		Interval interval = new Interval(start, end);

		if (Years.yearsIn(interval).isGreaterThan(Years.ZERO)) {
			// make it start at the start of the current increment mode
			start = start.withDayOfYear(start.dayOfYear().getMinimumValue());
			end = end.withDayOfYear(end.dayOfYear().getMaximumValue());
			interval = new Interval(start, end);
			
			// figure out number of slices
			numSlices = Years.yearsIn(interval).getYears();
			if (start.plusYears(numSlices).isBefore(end)) {
				numSlices += 1;
			}
			
			// update label extractor
			sliceLabelExtractor = new SliceLabelExtractor() {
				@Override
				public String extractLabel(DateTime from) {
					return from.year().getAsShortText();
				}
			};
			
			// update increment
			increment = Years.ONE.toPeriod();
			incrementMode = Mode.Years;
		} else if (Months.monthsIn(interval).isGreaterThan(Months.ZERO)) {
			// make it start at the start of the current increment mode
			start = start.withDayOfMonth(start.dayOfMonth().getMinimumValue());
			end = end.withDayOfMonth(end.dayOfMonth().getMaximumValue());
			interval = new Interval(start, end);
			
			numSlices = Months.monthsIn(interval).getMonths();
			if (start.plusMonths(numSlices).isBefore(end)) {
				numSlices += 1;
			}
			
			sliceLabelExtractor = new SliceLabelExtractor() {
				@Override
				public String extractLabel(DateTime from) {
					return from.monthOfYear().getAsShortText();
				}
			};
			
			increment = Months.ONE.toPeriod();
			incrementMode = Mode.Months;
		} else if (Weeks.weeksIn(interval).isGreaterThan(Weeks.ZERO)) {
			start = start.withDayOfWeek(start.dayOfWeek().getMinimumValue());
			end = end.withDayOfWeek(end.dayOfWeek().getMaximumValue());
			interval = new Interval(start, end);
			
			numSlices = Weeks.weeksIn(interval).getWeeks();
			if (start.plusWeeks(numSlices).isBefore(end)) {
				numSlices += 1;
			}
			
			sliceLabelExtractor = new SliceLabelExtractor() {
				@Override
				public String extractLabel(DateTime from) {
					return "W" + from.weekOfWeekyear().getAsShortText();
				}
			};
			
			increment = Weeks.ONE.toPeriod();
			incrementMode = Mode.Weeks;
		} else {
			numSlices = Days.daysIn(interval).getDays();
			if (start.plusDays(numSlices).isBefore(end)) {
				numSlices += 1;
			}
			if (numSlices == 0) {
				// force at least one day to be drawn
				numSlices = 1;
			}
			
			sliceLabelExtractor = new SliceLabelExtractor() {
				@Override
				public String extractLabel(DateTime from) {
					return from.dayOfMonth().getAsShortText();
				}
			};
			
			increment = Days.ONE.toPeriod();
			incrementMode = Mode.Days;
		}
		
		// reset time of day too
		start = start.withMillisOfDay(start.millisOfDay().getMinimumValue());
		end = end.withMillisOfDay(end.millisOfDay().getMaximumValue());
		
		// recalculate which intervals are within range
		intervalsWithinRange.clear();
		intervalsWithinRange.addAll(calculateIntervalsWithinRange(start, end));
		
		// notify listeners
		changeSupport.firePropertyChange(INTERVAL_PROPERTY_KEY, interval, new Interval(start, end));
	}

	
	
	private Set<PayloadInterval<T>> calculateIntervalsWithinRange(
			DateTime rangeStart, DateTime rangeEnd) {
		Set<PayloadInterval<T>> result = new HashSet<PayloadInterval<T>>(); 
		Interval range = new Interval(rangeStart, rangeEnd);
		for (PayloadInterval<T> interval : allIntervals) {
			if (range.contains(interval.getStart()) && range.contains(interval.getEnd())) {
				result.add(interval);
			}
		}
		return result;
	}
	
	public Set<PayloadInterval<T>> getVisibleIntervals(Duration minLength) {
		Set<PayloadInterval<T>> result = new HashSet<PayloadInterval<T>>();
		List<PayloadInterval<T>> intervals = allIntervals.getIntervals();
		for (PayloadInterval<T> interval : intervals) {
			DateTime intervalStart = interval.getStart();
			DateTime intervalEnd = interval.getEnd();
			
			// if interval is completely outside of time shown in timeline
			// -> skip this interval
			if (intervalEnd.isBefore(start) || intervalStart.isAfter(end)) {
				continue;
			}
			
			// if interval is too short
			// -> skip this interval
			Duration length = interval.toInterval().toDuration();
			if (length.isShorterThan(minLength)) {
				continue;
			}
			
			if (isWithinRange(intervalStart)) {
				if (isWithinRange(intervalEnd)) {
					// interval is completely inside time shown in timeline
					result.add(interval);
				} else {
					// interval starts during timeline but ends later
					result.add(interval);
				}
			} else if (isWithinRange(intervalEnd)) {
				// interval starts before timeline but ends within
				result.add(interval);			
			} else {
				// interval start before timeline and ends later
				result.add(interval);
			}
		}
		return result;
	}

	public boolean isWithinRange(DateTime date) {
		boolean notBeforeStart = date.isAfter(start) || date.isEqual(start);
		boolean notAfterEnd = date.isBefore(end) || date.isEqual(end);
		return notBeforeStart && notAfterEnd;
	}

	public DateTime getStart() {
		return start;
	}

	public DateTime getEnd() {
		return end;
	}

	public Duration getDuration() {
		return new Duration(start, end);
	}

	public boolean isBeforeStart(DateTime date) {
		return date.isBefore(start);
	}

	public void addIntervalListener(IntervalListener listener) {
		changeSupport.addPropertyChangeListener(INTERVAL_PROPERTY_KEY, listener);
	}

	public void removeIntervalListener(IntervalListener listener) {
		changeSupport.removePropertyChangeListener(INTERVAL_PROPERTY_KEY, listener);
	}

	public Set<PayloadInterval<T>> getIntervalsWithinRange(){
		return intervalsWithinRange;
	}
	
	public void clear() {
		PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
		for (int i = 0; i < listeners.length; i++) {
			changeSupport.removePropertyChangeListener(listeners[i]);
		}

		allIntervals = null;
		currentlyVisibleIntervals.clear();
		intervalsWithinRange.clear();
		
		start = null;
		end = null;

		numSlices = 0;
		increment = null;
		
		sliceLabelExtractor = null;
	}

	public int getNumSlices() {
		return numSlices;
	}

	public Period getIncrement() {
		return increment;
	}
	
	public Mode getIncrementMode() {
		return incrementMode;
	}

	public boolean canZoomInFurther() {
		if (allIntervals == null || incrementMode == null) {
			return false;
		}
		return numSlices > 1 || incrementMode != Mode.Days;
	}

	public boolean canZoomOutFurther() {
		if (allIntervals == null || start == null || end == null) {
			return false;
		}
		return start.isAfter(allIntervals.getFirstStart()) || end.isBefore(allIntervals.getLastEnd());
	}

	public Interval convertSliceToInterval(int row) {
		if (row > -1) {
			DateTime periodStart = start;
			for (int i = 0; i < row; i++) {
				Duration addDuration = increment.toDurationFrom(periodStart);
				periodStart = periodStart.plus(addDuration);
			}
			Duration addDuration = increment.toDurationFrom(periodStart);
			DateTime periodEnd = periodStart.plus(addDuration);
			if (periodEnd.isAfter(end)) {
				periodEnd = end;
			}
			periodEnd = periodEnd.minus(Minutes.ONE);
			return new Interval(periodStart, periodEnd); 
		} else {
			return null;
		}
	}

	private interface SliceLabelExtractor {
		String extractLabel(DateTime from);
	}

	public String extractLabel(DateTime sliceStart) {
		return sliceLabelExtractor.extractLabel(sliceStart);
	}

	public String extractLabel(int slice) {
		Interval interval;
		try {
			interval = convertSliceToInterval(slice);
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
			return "";
		}
		DateTimeFormatter format = DateTimeFormat.shortDate();
		if (incrementMode == Mode.Days) {
			return interval.getStart().toString(format);
		} else {
			String incrementString = "";
			switch (incrementMode) {
			case Years:
				incrementString = "Year " + sliceLabelExtractor.extractLabel(interval.getStart()) + " ("; 
				break;
			case Months:
				incrementString = "Month " + sliceLabelExtractor.extractLabel(interval.getStart()) + " (";
				break;
			case Weeks:
				incrementString = "Week " + sliceLabelExtractor.extractLabel(interval.getStart()) + " (";
				break;
			}
			return incrementString + interval.getStart().toString(format) + " to " + interval.getEnd().toString(format) + ")";
		}
	}

	public int countIntervalsWithinRange(Interval sliceInterval) {
		Set<PayloadInterval<T>> intervals = calculateIntervalsWithinRange(sliceInterval.getStart(), sliceInterval.getEnd());
		return intervals.size();
	}

}
