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

package net.schweerelos.timeline.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class TimelineLayout implements LayoutManager {

	private int minimumHorizontalGap = 5;
	private int verticalGap = 5;
	private int intervalHeight = 10;
	private int bottomMargin = 12;
	private int numRows;

	public TimelineLayout(int minimumHorizontalGap, int verticalGap, int intervalHeight, int bottomMargin) {
		this.minimumHorizontalGap = minimumHorizontalGap;
		this.verticalGap = verticalGap;
		this.intervalHeight = intervalHeight;
		this.bottomMargin = bottomMargin;
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		// we're not associating components with names
		// -> don't need to do anything here
	}

	@Override
	public void layoutContainer(Container parent) {
		TimelinePanel<?> timeline = null;
		if (parent instanceof TimelinePanel) {
			timeline = (TimelinePanel<?>) parent;	
		}
		if (timeline == null) {
			// do nothing if parent isn't a timelinePanel
			return;
		}

		List<Row> rows = new ArrayList<Row>();
		
		Component[] components = timeline.getComponents();
		for (int i = 0; i < components.length; i++) {
			Component component = components[i];
			IntervalView view = null;
			if (component instanceof IntervalView) {
				view = (IntervalView) component;
			}
			if (view == null) {
				// do nothing if component isn't an intervalView
				continue;
			}
			int startX = timeline.convertDateToXCoord(view.getInterval().getStart());
			int endX = timeline.convertDateToXCoord(view.getInterval().getEnd());
			int width = endX - startX;

			int rowIndex = 0;
			boolean foundRow = false;
			while (!foundRow) {
				Row row;
				if (rowIndex < rows.size()) {
					row = rows.get(rowIndex);
				} else {
					row = new Row();
					rows.add(row);
				}
				try {
					row.add(startX, endX);
					foundRow = true;
				} catch (TakenException te) {
					rowIndex++;
				}
			}
			
			int startY = convertRowToY(rowIndex);
			int height = intervalHeight;
			
			view.setBounds(startX, startY, width, height);
		}
		numRows = rows.size();
	}

	private int convertRowToY(int row) {
		int minY = 22; // TODO calculate based on maxAscent of timeline's font
		return minY + row * (intervalHeight + verticalGap);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		Insets insets = parent.getInsets();
		int width = insets.left + 8 + insets.right;
		// TODO calculate minY based on maxAscent of timeline's font
		int height = insets.top + 22 + numRows * (intervalHeight + verticalGap) + bottomMargin + insets.bottom;
		return new Dimension(width, height);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Dimension minimumSize = minimumLayoutSize(parent);
		int width = Math.max(360, minimumSize.width);
		int height = Math.max(150, minimumSize.height);
		return new Dimension(width, height);
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		// we're not caching anything -> don't need to do anything here
	}
	

	private class Row {
		SortedSet<Range> taken = new TreeSet<Range>();
		
		boolean canFit(int from, int to) {
			if (taken.isEmpty()) {
				return true;
			}
			
			Range[] takenArray = (Range[]) taken.toArray(new Range[taken.size()]);
			
			if (to + minimumHorizontalGap <= takenArray[0].from) {
				return true;
			}
			
			for (int i = 0; i < takenArray.length; i++) {
				Range range = takenArray[i];
				if (range.to + minimumHorizontalGap <= from) {
					if (i + 1 < takenArray.length) {
						Range nextRange = takenArray[i+1];
						if (to + minimumHorizontalGap <= nextRange.from) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
			// couldn't find a fitting space
			return false;
		}

		void add(int from, int to) throws TakenException {
			if (canFit(from, to)) {
				taken.add(new Range(from, to));
			} else {
				throw new TakenException("range from " + from + " to " + to + " is taken");
			}
		}

	}
	
	private class Range implements Comparable<Range> {
		private int from;
		private int to;

		Range(int from, int to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public int compareTo(Range o) {
			Integer thisFrom = new Integer(from);
			Integer otherFrom = new Integer(o.from);
			return thisFrom.compareTo(otherFrom);
		}
	}

	private class TakenException extends Exception {
		private static final long serialVersionUID = 1L;
		TakenException(String message) {
			super(message);
		}
		TakenException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
