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


package net.schweerelos.parrot.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.Filter.Mode;
import net.schweerelos.parrot.model.filters.IntervalChainBasedFilter;
import net.schweerelos.parrot.model.filters.TimeBasedFilter;
import net.schweerelos.parrot.timeline.IntervalListener;
import net.schweerelos.timeline.model.IntervalChain;
import net.schweerelos.timeline.model.Timeline;
import net.schweerelos.timeline.ui.ColorKeys;
import net.schweerelos.timeline.ui.TimelinePanel;

import org.joda.time.Interval;

@SuppressWarnings("serial")
public class TimelineNavigator extends AbstractNavigatorPanel {
	private static final String INTERVAL_LABEL_DATE_FORMAT = "d MMM yyyy";
	private static final Color SELECTED_OUTLINE = UIConstants.ACCENT_MEDIUM;
	private static final Color LABEL = UIConstants.ENVIRONMENT_MEDIUM;
	private static final Color BACKGROUND_ODD = UIConstants.ENVIRONMENT_LIGHTEST;
	private static final Color BACKGROUND = Color.WHITE;
	private static final Color INTERVAL_FILL = UIConstants.SECOND_ACCENT_LIGHT;
	private static final Color INTERVAL_OUTLINE = UIConstants.SECOND_ACCENT_MEDIUM;
	private static final Color HISTOGRAM_FILL = UIConstants.THIRD_ACCENT_LIGHT;

	private static final String NAME = "Timeline";
	private static final String ACCELERATOR_KEY = "T";

	private TimelinePanel<NodeWrapper> timelinePanel;
	private Filter lastHighlightFilter;
	private Filter lastRestrictingFilter;
	private IntervalListener intervalListener;
	private ListSelectionListener selectionListener;
	private JLabel intervalLabel;
	
	public TimelineNavigator() {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth = GridBagConstraints.REMAINDER;

		Map<ColorKeys, Color> colors = new HashMap<ColorKeys, Color>();
		colors.put(ColorKeys.Background, BACKGROUND);
		colors.put(ColorKeys.BackgroundOdd, BACKGROUND_ODD);
		colors.put(ColorKeys.Label, LABEL);
		colors.put(ColorKeys.LabelOdd, LABEL);
		colors.put(ColorKeys.SelectedOutline, SELECTED_OUTLINE);
		colors.put(ColorKeys.IntervalFill, INTERVAL_FILL);
		colors.put(ColorKeys.IntervalOutline, INTERVAL_OUTLINE);
		colors.put(ColorKeys.HistogramFill, HISTOGRAM_FILL);

		constraints.weightx = 1;
		constraints.weighty = 0; 
		constraints.insets.bottom = 5;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.LINE_START;
		intervalLabel = new JLabel(" ");
		intervalLabel.setFont(intervalLabel.getFont().deriveFont(Font.PLAIN));
		add(intervalLabel, constraints);

		constraints.weightx = 0;
		constraints.weighty = 1;
		constraints.insets.bottom = 17;
		constraints.fill = GridBagConstraints.BOTH;
		timelinePanel = new TimelinePanel<NodeWrapper>(colors);
		JScrollPane scrollPane = new JScrollPane(timelinePanel);
		scrollPane.setAutoscrolls(true);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, constraints);

		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.gridwidth = 1;
		constraints.weighty = 0;
		constraints.weightx = 0;
		constraints.insets.bottom = 0;
		constraints.insets.right = 5;
		add(new JButton(timelinePanel.getZoomInAction()), constraints);
		constraints.weightx = 1;
		constraints.insets.right = 0;
		add(new JButton(timelinePanel.getZoomOutAction()), constraints);
	}

	@Override
	protected void deactivateFilters() {
		if (getModel() == null) {
			return;
		}
		if (lastHighlightFilter != null) {
			removeFilter(lastHighlightFilter);
		}
		if (lastRestrictingFilter != null) {
			removeFilter(lastRestrictingFilter);
		}
	}

	@Override
	protected void activateFilters() {
		if (getModel() == null) {
			return;
		}
		if (lastHighlightFilter != null) {
			applyFilter(lastHighlightFilter);
		}
		if (lastRestrictingFilter != null) {
			applyFilter(lastRestrictingFilter);
		}
	}

	@Override
	public void setModel(ParrotModel model) {
		super.setModel(model);
		if (model == null) {
			timelinePanel.clearAll();
			return;
		}

		if (selectionListener != null) {
			timelinePanel.removeListSelectionListener(selectionListener);
		}
		if (intervalListener != null) {
			timelinePanel.getModel().removeIntervalListener(intervalListener);
		}

		final Timeline<NodeWrapper> timeline = new Timeline<NodeWrapper>(model
				.getTimedThings());

		intervalListener = new TimelineIntervalListener();
		timeline.addIntervalListener(intervalListener);
		timeline.addIntervalListener(new IntervalListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				SwingUtilities.invokeLater(new Runnable() {
				
					@Override
					public void run() {
						updateIntervalLabel(timeline);
					}
				});
			}
		});

		timelinePanel.setModel(timeline);
		updateIntervalLabel(timeline);
		
		selectionListener = new SelectionListener();
		timelinePanel.addListSelectionListener(selectionListener);
	}

	@Override
	public List<Action> getActionsForNode(NodeWrapper currentNode) {
		ArrayList<Action> result = new ArrayList<Action>();
//		// TODO #5 show node in timeline
//		if (currentNode.isOntResource()) {
//			OntResource ontResource = currentNode.getOntResource();
//			if (TimedThingsHelper.isTimedThing(ontResource, getModel())) {
//				Action showInTimeline = new AbstractAction("Show in timeline") {
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						// TODO #5 show node in timeline
//					}
//				};
//				showInTimeline.setEnabled(false);
//				result.add(showInTimeline);
//			}
//		}
		return result;
	}

	@Override
	public List<Action> getActionsForType(NodeWrapper type) {
		// timeline doesn't deal with types
		return Collections.emptyList();
	}

	@Override
	public String getNavigatorName() {
		return NAME;
	}

	@Override
	public String getAcceleratorKey() {
		return ACCELERATOR_KEY;
	}

	private void updateIntervalLabel(final Timeline<NodeWrapper> timeline) {
		StringBuilder intervalText = new StringBuilder();
		intervalText.append("<html>");
		intervalText.append(timeline.getIncrementMode().toString());
		intervalText.append(" between ");
		intervalText.append(timeline.getStart().toString(INTERVAL_LABEL_DATE_FORMAT));
		intervalText.append(" and ");
		intervalText.append(timeline.getEnd().toString(INTERVAL_LABEL_DATE_FORMAT));
		intervalText.append("</html>");
		intervalLabel.setText(intervalText.toString());
	}

	private final class TimelineIntervalListener implements IntervalListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final Interval interval = (Interval) evt.getNewValue();
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					TimeBasedFilter filter = new TimeBasedFilter(interval,
							getModel());
					filter.setMode(Mode.RESTRICT);
					if (lastRestrictingFilter != null) {
						removeFilter(lastRestrictingFilter);
					}
					lastRestrictingFilter = filter;
					applyFilter(lastRestrictingFilter);
					return null;
				}

				@Override
				protected void done() {
					revalidate();
				}
			}.execute();
		}
	}

	private final class SelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() || getModel() == null) {
				return;
			}
			final IntervalChain<NodeWrapper> chain = timelinePanel
					.getSelections();
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					IntervalChainBasedFilter filter = new IntervalChainBasedFilter(
							chain, getModel());
					filter.setMode(Mode.HIGHLIGHT);
					if (lastHighlightFilter != null) {
						removeFilter(lastHighlightFilter);
					}
					lastHighlightFilter = filter;
					applyFilter(lastHighlightFilter);
					return null;
				}
			}.execute();

		}
	}

}
