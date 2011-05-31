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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import net.schweerelos.parrot.timeline.IntervalListener;
import net.schweerelos.timeline.model.IntervalChain;
import net.schweerelos.timeline.model.PayloadInterval;
import net.schweerelos.timeline.model.Timeline;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

public class TimelinePanel<T extends Object> extends JPanel {

	private static final float ZOOM_FACTOR = 3.0f;

	private static final long serialVersionUID = 1L;

	private ListSelectionModel selectionModel;
	private AbstractAction zoomOutAction;
	private AbstractAction zoomInAction;

	private Logger logger;

	private Map<ColorKeys, Color> colors;

	private Timeline<T> tModel;

	private static final Map<ColorKeys, Color> DEFAULT_COLORS = new HashMap<ColorKeys, Color>();
	static {
		DEFAULT_COLORS.put(ColorKeys.Background, Color.WHITE);
		DEFAULT_COLORS.put(ColorKeys.Label, new Color(0x6292E4));
		DEFAULT_COLORS.put(ColorKeys.LabelOdd, DEFAULT_COLORS
				.get(ColorKeys.Label));
		DEFAULT_COLORS.put(ColorKeys.BackgroundOdd, new Color(0xF2FAFC));
		DEFAULT_COLORS.put(ColorKeys.SelectedOutline, new Color(0xFFCB77));
		DEFAULT_COLORS.put(ColorKeys.IntervalFill, new Color(0xfff9e9));
		DEFAULT_COLORS.put(ColorKeys.IntervalOutline, new Color(0xf7d891));
		DEFAULT_COLORS.put(ColorKeys.HistogramFill, new Color(0xbacbce));
	}

	private static final int INTERVAL_HEIGHT = 12;
	private static final int MINIMUM_H_GAP = 2;
	private static final int V_GAP = 5;
	private static final int HISTOGRAM_HEIGHT = 12;

	private static final int SHORTEST_VISIBLE_INTERVAL = 3;


	public TimelinePanel() {
		this(DEFAULT_COLORS);
	}

	public TimelinePanel(Map<ColorKeys, Color> colors) {
		super();

		logger = Logger.getLogger(TimelinePanel.class);

		this.colors = colors;

		setLayout(new TimelineLayout(MINIMUM_H_GAP, V_GAP, INTERVAL_HEIGHT, HISTOGRAM_HEIGHT));

		setOpaque(true);
		setBackground(colors.get(ColorKeys.Background));

		ToolTipManager.sharedInstance().registerComponent(this);

		selectionModel = new DefaultListSelectionModel();
		selectionModel
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		MouseInputAdapter mouseAdapter = new MouseInputAdapter() {
			private boolean dragging;
			private int lastDraggedOverSlice;

			@Override
			public void mousePressed(MouseEvent e) {
				selectionModel.setValueIsAdjusting(true);

				int slice = convertXCoordToRow(e.getX());
				
				if (selectionModel.isSelectedIndex(slice)) {
					return;
				}
				
				if (!e.isShiftDown() && !e.isControlDown()) {
					// "naked" click -> start selection process from scratch
					selectionModel.clearSelection();
				}
				if (selectionModel.isSelectionEmpty()) {
					// empty selection -> start a new selection
					selectionModel.setSelectionInterval(slice, slice);
				} else if (e.isControlDown()) {
					// ctrl -> toggle selection of selected slice
					int anchor = selectionModel.getAnchorSelectionIndex();
					if (selectionModel.isSelectedIndex(slice)) {
						selectionModel.removeSelectionInterval(slice, slice);
					} else {
						selectionModel.addSelectionInterval(slice, slice);
					}
					selectionModel.setAnchorSelectionIndex(anchor);
				} else if (e.isShiftDown()) {
					// shift -> select from anchor (first clicked cell) to
					// current
					int anchor = selectionModel.getAnchorSelectionIndex();
					selectionModel.addSelectionInterval(anchor, slice);
					selectionModel.setAnchorSelectionIndex(anchor);
				}
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!dragging) {
					return; // ignore
				}
				selectionModel.setValueIsAdjusting(false);
				dragging = false;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				selectionModel.setValueIsAdjusting(false);
				if (e.getClickCount() == 2) {
					zoomToSelection();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int slice = convertXCoordToRow(e.getX());
				if (e.isControlDown() || e.isShiftDown()
						|| slice == lastDraggedOverSlice) {
					return; // ignore
				}
				lastDraggedOverSlice = slice;
				dragging = true;

				int anchor = selectionModel.getAnchorSelectionIndex();
				if (slice < anchor) {
					// selection is between start and anchor
					selectionModel.setSelectionInterval(slice, anchor);
					selectionModel.setAnchorSelectionIndex(anchor);
				} else {
					// selection is between anchor and end
					selectionModel.setSelectionInterval(anchor, slice);
					selectionModel.setAnchorSelectionIndex(anchor);
				}
				repaint();
			}

		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);

		zoomOutAction = new AbstractAction("Zoom out", new ImageIcon(
				"images/zoom-out.png")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// add a (zoom factor) proportion of the current interval at the
				// beginning
				// and add another one at the end
				long seconds = tModel.getDuration().getStandardSeconds();
				int difference = (int) Math.ceil(seconds / ZOOM_FACTOR);
				
				DateTime newStart = tModel.getStart().minusSeconds(difference);
				DateTime newEnd = tModel.getEnd().plusSeconds(difference);
				tModel.setInterval(newStart, newEnd);
			}
		};
		zoomOutAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		zoomOutAction.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		zoomInAction = new AbstractAction("Zoom in", new ImageIcon(
				"images/zoom-in.png")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!selectionModel.isSelectionEmpty()) {
					zoomToSelection();
				} else {
					// otherwise: 
					// remove a (zoom factor) proportion of the current interval at
					// the beginning and remove another one at the end
					zoomIn();
				}
			}
		};
		zoomInAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);

		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					zoomInAction.setEnabled(canZoomInFurther());
				}
			}
		});
		
		clearAll();

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				adjustVisibleIntervals();
			}
		});
	}

	@Override
	public String getToolTipText() {
		return "Timeline";
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		try {
			int slice = event.getX() / calculateSliceWidth(getWidth());
			return tModel.extractLabel(slice);
		} catch (IllegalArgumentException iae) {
			return getToolTipText();
		}
	}

	private int calculateSliceWidth(double totalWidth) {
		return (int) Math.floor((double) totalWidth
				/ (double) tModel.getNumSlices());
	}

	private int convertXCoordToRow(int xCoord) {
		return xCoord / calculateSliceWidth(getWidth());
	}

	public int convertDateToXCoord(DateTime date) {
		if (!tModel.isWithinRange(date)) {
			if (tModel.isBeforeStart(date)) {
				return 0;
			} else {
				return getWidth();
			}
		}
		Duration visibleDuration = tModel.getDuration();
		Duration fromStart = new Duration(tModel.getStart(), date);
		float ratio = fromStart.getMillis()
				/ (float) visibleDuration.getMillis();
		int xCoord = (int) Math.floor(ratio * getWidth());
		return xCoord;
	}

	public void addListSelectionListener(ListSelectionListener listener) {
		selectionModel.addListSelectionListener(listener);
	}

	public void removeListSelectionListener(ListSelectionListener listener) {
		selectionModel.removeListSelectionListener(listener);
	}

	public void addIntervalListener(IntervalListener listener) {
		tModel.addIntervalListener(listener);
	}

	public void removeIntervalListener(IntervalListener listener) {
		tModel.removeIntervalListener(listener);
	}

	public void setModel(Timeline<T> tModel) {
		if (this.tModel != null) {
			clearAll();
		}
		if (tModel == null) {
			clearAll();
			return;
		}
		this.tModel = tModel;

		tModel.addIntervalListener(new IntervalListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				modelChanged();
			}
		});

		modelChanged();
	}

	private void modelChanged() {
		selectionModel.clearSelection();

		zoomInAction.setEnabled(canZoomInFurther());
		zoomOutAction.setEnabled(canZoomOutFurther());

		int numSlices = tModel.getNumSlices();
		setPreferredSize(new Dimension(40 * numSlices, 120));
		setMinimumSize(new Dimension(20 * numSlices, 70));
		setSize(getPreferredSize());

		adjustVisibleIntervals();
	}

	private void adjustVisibleIntervals() {
		removeAll();

		if (tModel != null) {
			Duration smallestVisibleDuration = calculateSmallestVisibleDuration();

			for (PayloadInterval<T> interval : tModel
					.getVisibleIntervals(smallestVisibleDuration)) {
				IntervalView intervalView = new IntervalView(interval);
				intervalView.setColors(colors);
				add(intervalView);
			}
		}

		validate();
		if (isVisible()) {
			repaint();
		}
	}

	private Duration calculateSmallestVisibleDuration() {
		Duration visibleDuration = tModel.getDuration();
		float millisPerPixel = (float) visibleDuration.getMillis()
				/ (float) getWidth();
		float secondsPerPixel = millisPerPixel / 1000;
		long minSeconds = (long) Math.ceil(secondsPerPixel
				* SHORTEST_VISIBLE_INTERVAL);
		return Duration.standardSeconds(minSeconds);
	}

	@Override
	public void paintComponent(Graphics g) {
		// draw "normal" panel stuff below everything else
		super.paintComponent(g);
		if (tModel == null) {
			return;
		}
		// set up graphics config
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		// draw slices
		if (tModel.getNumSlices() > 0) {
			drawSlices(graphics);
		}
	}

	private void drawSlices(Graphics2D graphics) {
		int sliceWidth = calculateSliceWidth(getWidth());
		int width = getWidth();
		if (sliceWidth < 10) {
			// TODO real error handling
			logger.error("slices too thin, only " + sliceWidth + " pixels.");
			return;
		}
		Paint originalPaint = graphics.getPaint();
		Font originalFont = graphics.getFont();

		Font font = graphics.getFont().deriveFont(Font.BOLD);
		graphics.setFont(font);

		int[] intervalsInSlice = new int[tModel.getNumSlices()];
		int maxIntervalsPerSlice = 0;
		for (int slice = 0; slice < tModel.getNumSlices(); slice++) {
			Interval sliceInterval = tModel.convertSliceToInterval(slice);
			int numIntervals = tModel.countIntervalsWithinRange(sliceInterval);
			intervalsInSlice[slice] = numIntervals;
			if (numIntervals > maxIntervalsPerSlice) {
				maxIntervalsPerSlice = numIntervals;
			}
		}
		
		int xCoord = 0;
		DateTime sliceStart = tModel.getStart();
		for (int slice = 0; slice < tModel.getNumSlices(); slice++) {
			if (xCoord + sliceWidth > width) {
				sliceWidth = width - xCoord;
				logger.info("next slice will be only " + sliceWidth
						+ " wide");
			}
			// draw background (or not)
			boolean oddSlice = slice % 2 == 1;
			if (oddSlice) {
				// odd slice -> draw bg
				graphics.setColor(colors.get(ColorKeys.BackgroundOdd));
				graphics.fillRect(xCoord, 0, sliceWidth, getHeight());
			}
			// draw histogram
			if (maxIntervalsPerSlice > 0 && intervalsInSlice[slice] > 0) {
				double histProportion = intervalsInSlice[slice] / (double) maxIntervalsPerSlice;
				int histHeight = (int) Math.round(histProportion * HISTOGRAM_HEIGHT);
				if (histHeight > 0) {
					graphics.setColor(colors.get(ColorKeys.HistogramFill));
					graphics.fillRect(xCoord + 1, getHeight() - histHeight, sliceWidth - 1, histHeight);
				}
			}
			// draw selection outline
			if (selectionModel.isSelectedIndex(slice)) {
				graphics.setColor(colors.get(ColorKeys.SelectedOutline));
				graphics.setStroke(new BasicStroke(2));
				graphics.drawRect(xCoord, 0, sliceWidth - 1,
						getHeight() - 1);
			}
			// draw label
			if (oddSlice) {
				// set colour for label
				graphics.setColor(colors.get(ColorKeys.LabelOdd));
			} else {
				// set colour for label
				graphics.setColor(colors.get(ColorKeys.Label));
			}
			String sliceName = tModel.extractLabel(sliceStart);
			int stringWidth = graphics.getFontMetrics().stringWidth(
					sliceName);
			int textXCoord = xCoord + (sliceWidth - stringWidth) / 2;
			int textYCoord = graphics.getFontMetrics().getMaxAscent() + 2;
			graphics.drawString(sliceName, textXCoord, textYCoord);

			// update variables for next slice
			xCoord += sliceWidth;
			sliceStart = sliceStart.plus(tModel.getIncrement());
		}
		graphics.setPaint(originalPaint);
		graphics.setFont(originalFont);
	}

	public void clearAll() {
		if (tModel != null) {
			tModel.clear();
		}
		tModel = null;

		selectionModel.clearSelection();
		zoomInAction.setEnabled(false);
		zoomOutAction.setEnabled(false);
	}

	public IntervalChain<T> getSelections() {
		IntervalChain<T> result = new IntervalChain<T>();
		if (selectionModel.isSelectionEmpty()) {
			return result;
		}
		List<Interval> selections = new ArrayList<Interval>();
		int minSelectedIndex = selectionModel.getMinSelectionIndex();
		int maxSelectedIndex = selectionModel.getMaxSelectionIndex();

		DateTime lastStart = null;
		int currentIndex = minSelectedIndex;

		while (currentIndex <= maxSelectedIndex) {
			if (selectionModel.isSelectedIndex(currentIndex)) {
				if (lastStart == null) {
					// start of a new interval
					lastStart = tModel.convertSliceToInterval(currentIndex)
							.getStart();
				}
			} else {
				if (lastStart != null) {
					// end of a new interval
					DateTime end = tModel.convertSliceToInterval(currentIndex)
							.getEnd();
					Interval newInterval = new Interval(lastStart, end);
					selections.add(newInterval);
					lastStart = null;
				}
			}
			currentIndex++;
		}
		// lastStart should be non-null now
		if (lastStart != null) {
			// end of a new interval
			DateTime end = tModel.convertSliceToInterval(maxSelectedIndex)
					.getEnd();
			Interval newInterval = new Interval(lastStart, end);
			selections.add(newInterval);
		} else {
			logger.warn("last start is null, shouldn't happen");
		}

		// TODO see if we can get this to be more efficient
		for (PayloadInterval<T> interval : tModel
				.getIntervalsWithinRange()) {
			for (Interval selection : selections) {
				if (selection.contains(interval.toInterval())) {
					result.add(interval);
				}
			}
		}
		return result;
	}

	public boolean isSelectionEmpty() {
		return selectionModel.isSelectionEmpty();
	}

	public Action getZoomInAction() {
		return zoomInAction;
	}

	public Action getZoomOutAction() {
		return zoomOutAction;
	}

	private boolean canZoomInFurther() {
		if (tModel == null) {
			return false;
		}
		boolean canZoomToSelection = !selectionModel.isSelectionEmpty()
				&& tModel.getNumSlices() > 1;
		return canZoomToSelection || tModel.canZoomInFurther();
	}

	private boolean canZoomOutFurther() {
		if (tModel == null) {
			return false;
		}
		return tModel.canZoomOutFurther();
	}

	public Timeline<T> getModel() {
		return tModel;
	}

	@Override
	public Dimension getMinimumSize() {
		return getLayout().minimumLayoutSize(this);
	}

	@Override
	public Dimension getPreferredSize() {
		return getLayout().preferredLayoutSize(this);
	}

	private void zoomToSelection() {
		DateTime newStart = tModel.convertSliceToInterval(
				selectionModel.getMinSelectionIndex()).getStart();
		DateTime newEnd = tModel.convertSliceToInterval(
				selectionModel.getMaxSelectionIndex()).getEnd();
		tModel.setInterval(newStart, newEnd);
	}

	private void zoomIn() {
		long seconds = tModel.getDuration().getStandardSeconds();
		int difference = (int) Math.floor(seconds / ZOOM_FACTOR);
		DateTime newStart = tModel.getStart().plusSeconds(difference);
		DateTime newEnd = tModel.getEnd().minusSeconds(difference);
		tModel.setInterval(newStart, newEnd);
	}
	
	

}
