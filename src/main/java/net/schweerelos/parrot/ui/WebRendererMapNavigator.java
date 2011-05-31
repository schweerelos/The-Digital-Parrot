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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.schweerelos.parrot.model.CenteredThing;
import net.schweerelos.parrot.model.CoordinatePrecision;
import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.LocatedThingsHelper;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.NotPlacedThingException;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.Filter.Mode;
import net.schweerelos.parrot.model.filters.MapAreaFilter;
import net.schweerelos.parrot.model.filters.SimpleNodeFilter;
import net.schweerelos.parrot.util.LatLonBounds;

@SuppressWarnings("serial")
public class WebRendererMapNavigator extends AbstractNavigatorPanel {

	private final class ShowInMapAction extends AbstractAction {
		private final NodeWrapper node;

		private ShowInMapAction(NodeWrapper node) {
			super("Show in map");
			this.node = node;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						mapBrowser.focusMapOn(LocatedThingsHelper
								.getAsLocatedThing(node));
						Window window = SwingUtilities.getWindowAncestor(
								WebRendererMapNavigator.this);
						if (window != null) {
							window.setVisible(true);
						}
					} catch (NotPlacedThingException e) {
						/* ignore */
					}
				}
			});
		}
	}

	private static final String NAME = "Map";
	private static final String ACCELERATOR_KEY = "M";

	private static final Color COLOR_EVEN_ROW_UNSELECTED_BG = Color.WHITE;
	private static final Color COLOR_ODD_ROW_UNSELECTED_BG = UIConstants.ENVIRONMENT_LIGHTEST;
	private static final Color COLOR_SELECTED_BG = UIConstants.THIRD_ACCENT_LIGHT;
	private static final Color COLOR_SELECTED_BORDER = UIConstants.THIRD_ACCENT_MEDIUM;

	private Filter lastRestrictingFilter;
	private Filter lastHighlightFilter;
	private JList placesList;
	private MapBrowser mapBrowser;
	
	public WebRendererMapNavigator(Properties properties) {
		super();
		setLayout(new BorderLayout());
		placesList = new JList();

		FontMetrics metrics = placesList.getFontMetrics(placesList.getFont());
		int rowHeight = 2 * metrics.getMaxAscent() + 2
				* metrics.getMaxDescent() + metrics.getLeading();
		placesList.setFixedCellHeight(rowHeight);

		placesList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component renderer = super.getListCellRendererComponent(list,
						value, index, isSelected, cellHasFocus);
				if (renderer instanceof JLabel
						&& value instanceof CenteredThing<?>) {
					JLabel label = (JLabel) renderer;
					CenteredThing<?> thing = (CenteredThing<?>) value;
					label.setText(thing.getLabel());

					label.setFont(label.getFont().deriveFont(Font.PLAIN));

					if (isSelected || cellHasFocus) {
						label.setBackground(COLOR_SELECTED_BG);
						label.setBorder(BorderFactory.createLineBorder(
								COLOR_SELECTED_BORDER, 2));
					} else {
						if (index % 2 == 0) {
							label.setBackground(COLOR_EVEN_ROW_UNSELECTED_BG);
						} else {
							label.setBackground(COLOR_ODD_ROW_UNSELECTED_BG);
						}
					}
				}
				return renderer;
			}
		});

		placesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placesList.addListSelectionListener(new ListSelectionListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void valueChanged(ListSelectionEvent lse) {
				if (lse.getValueIsAdjusting()) {
					return;
				}
				Object selectedValue = placesList.getSelectedValue();
				try {
					final CenteredThing<NodeWrapper> selected = (CenteredThing<NodeWrapper>) selectedValue;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							mapBrowser.focusMapOn(selected);
						}
					});
				} catch (ClassCastException cce) { /* ignore */
				}
			}
		});

		add(new JScrollPane(placesList), BorderLayout.LINE_START);
		mapBrowser = new MapBrowser(properties.getProperty("webrenderer.license.type"), properties.getProperty("webrenderer.license.data"));
		mapBrowser.addMapBrowserListener(new MapBrowserListener() {

			@Override
			public void panned(final LatLonBounds newBounds) {
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						Window parent = SwingUtilities
								.getWindowAncestor(WebRendererMapNavigator.this);
						parent.setCursor(Cursor
								.getPredefinedCursor(Cursor.WAIT_CURSOR));

						adjustRestrictingFilter(newBounds);

						return null;
					}

					@Override
					protected void done() {
						Window parent = SwingUtilities
								.getWindowAncestor(WebRendererMapNavigator.this);
						parent.setCursor(Cursor.getDefaultCursor());
					}
				};
				worker.execute();
			}

			@Override
			public void markerClicked(NodeWrapper marker) {
				adjustHighlightingFilter(marker);
			}

			@Override
			public void zoomed(int zoom) {
				// ignore for now
			}

		});
		add(mapBrowser, BorderLayout.CENTER);
	}

	protected void adjustHighlightingFilter(final NodeWrapper marker) {
		if (getModel() == null) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Filter oldHighlightFilter = lastHighlightFilter;
				lastHighlightFilter = new SimpleNodeFilter() {
					@Override
					protected boolean matches(NodeWrapper nodeWrapper) {
						return nodeWrapper.equals(marker);
					}
				};
				lastHighlightFilter.setMode(Mode.HIGHLIGHT);
				replaceFilter(oldHighlightFilter, lastHighlightFilter);
			}
		});
	}

	private void adjustRestrictingFilter(final LatLonBounds newBounds) {
		if (getModel() == null) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Filter oldRestrictingFilter = lastRestrictingFilter;
				lastRestrictingFilter = new MapAreaFilter(newBounds);
				lastRestrictingFilter.setMode(Mode.RESTRICT);
				replaceFilter(oldRestrictingFilter, lastRestrictingFilter);
			}
		});
	}

	@Override
	public void setModel(ParrotModel model) {
		List<CenteredThing<NodeWrapper>> locatedThings = model
				.getLocatedThings().getAll();
		initPlacesList(locatedThings);
		mapBrowser.setModel(model);
		super.setModel(model);
	}

	private void initPlacesList(List<CenteredThing<NodeWrapper>> locatedThings) {
		List<CenteredThing<NodeWrapper>> bigEnoughLocatedThings = new ArrayList<CenteredThing<NodeWrapper>>();
		for (CenteredThing<NodeWrapper> thing : locatedThings) {
			if (thing.getPrecision().compareTo(
					CoordinatePrecision.BlockPrecision) >= 0) {
				bigEnoughLocatedThings.add(thing);
			}
		}
		Collections.sort(bigEnoughLocatedThings,
				new Comparator<CenteredThing<NodeWrapper>>() {
					@Override
					public int compare(CenteredThing<NodeWrapper> o1,
							CenteredThing<NodeWrapper> o2) {
						return o1.getLabel().compareTo(o2.getLabel());
					}
				});
		placesList.setListData(bigEnoughLocatedThings.toArray());
	}

	@Override
	protected void activateFilters() {
		if (getModel() == null) {
			return;
		}
		if (lastRestrictingFilter != null) {
			applyFilter(lastRestrictingFilter);
		}
		if (lastHighlightFilter != null) {
			applyFilter(lastHighlightFilter);
		}
	}

	@Override
	protected void deactivateFilters() {
		if (getModel() == null) {
			return;
		}
		if (lastRestrictingFilter != null) {
			removeFilter(lastRestrictingFilter);
		}
		if (lastHighlightFilter != null) {
			applyFilter(lastHighlightFilter);
		}
	}

	@Override
	public List<Action> getActionsForNode(final NodeWrapper currentNode) {
		ArrayList<Action> result = new ArrayList<Action>();
		if (LocatedThingsHelper.isLocatedThing(currentNode, getModel())) {
			Action showInMap = new ShowInMapAction(currentNode);
			result.add(showInMap);
		}
		return result;
	}

	@Override
	public List<Action> getActionsForType(NodeWrapper type) {
		// map doesn't deal with types
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
}
