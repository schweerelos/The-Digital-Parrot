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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.ParrotModelAdapter;
import net.schweerelos.parrot.model.TableParrotModel;

public class TableViewComponent extends JScrollPane implements MainViewComponent {

	private static final Color COLOR_GRID_LINES = UIConstants.ENVIRONMENT_LIGHT;
	private static final long serialVersionUID = 1L;
	
	private JTable table;
	private NodeWrapper selectedNode;
	private NodeWrapperPopupMenu popup;
	private TableRowSorter<TableParrotModel> rowSorter;
	private EventListenerList pickListeners = new EventListenerList();
	
	public TableViewComponent() {
		super();

		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12), BorderFactory.createLoweredBevelBorder()));
		
		table = new JTable();

		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setCellSelectionEnabled(true);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionListener selectionListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				
				ListSelectionModel rowSelections = table.getSelectionModel();
				ListSelectionModel columnSelections = table.getColumnModel().getSelectionModel();
				if (rowSelections.isSelectionEmpty() || columnSelections.isSelectionEmpty()) {
					// no new selection -> nothing has changed
					return;
				}
				
				int rowIndex = rowSelections.getLeadSelectionIndex();
				int columnIndex = columnSelections.getLeadSelectionIndex();
				int	modelRowIndex = table.convertRowIndexToModel(rowIndex);
				int	modelColumnIndex = table.convertColumnIndexToModel(columnIndex);
				
				if (selectedNode != null) {
					selectedNode.setHereTooSelected(false);
				}

				final NodeWrapper newSelection = (NodeWrapper) table.getModel().getValueAt(modelRowIndex, modelColumnIndex);
				newSelection.setHereTooSelected(true);
				
				selectedNode = newSelection;

				table.invalidate();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						table.repaint();
					}
				});
			}
		};
		table.getSelectionModel().addListSelectionListener(selectionListener);
		table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);

		table.setAutoCreateRowSorter(false);

		table.setFillsViewportHeight(true);

		table.setGridColor(COLOR_GRID_LINES);
		table.setShowGrid(false);
		table.setShowHorizontalLines(true);
		
		// 0px between cells in the same row; 1px between rows
		table.setIntercellSpacing(new Dimension(0, 1));
		
		FontMetrics metrics = table.getFontMetrics(table.getFont());
		int rowHeight = 2 * metrics.getMaxAscent() + 2 * metrics.getMaxDescent() + metrics.getLeading();
		table.setRowHeight(rowHeight);
		
		setViewportView(table);

		// context menus + double-click for chain nav
		table.addMouseListener(new MouseAdapter() {			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2) {
					return;
				}
				NodeWrapper node = getNodeWrapperAtPoint(e);
				if (node != null) {
					fireNodeSelected(node);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				handlePopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopup(e);
			}

			private void handlePopup(MouseEvent e) {
				if (!e.isPopupTrigger()) {
					return;
				}
				NodeWrapper node = getNodeWrapperAtPoint(e);
				if (node != null) {
					popup.setNodeWrapper(node);
					popup.show(table, e.getX(), e.getY());
				}
			}

			private NodeWrapper getNodeWrapperAtPoint(MouseEvent e) {
				int viewColumn = table.columnAtPoint(e.getPoint());
				int viewRow = table.rowAtPoint(e.getPoint());
				if (viewColumn == -1 || viewRow == -1) {
					// this means that we have an invalid point
					return null;
				}
				NodeWrapper node = (NodeWrapper) table.getValueAt(viewRow, viewColumn);
				return node;
			}

		});
	}

	@Override
	public void setModel(ParrotModel model) {
		if (model == null) {
			return;
		}

		model.addParrotModelListener(new TableViewModelListener());
		
		TableModel tableModel = ((TableParrotModel) model).asTableModel();
		table.setModel(tableModel);
		rowSorter = new TableRowSorter<TableParrotModel>((TableParrotModel) tableModel);
		
		List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
		rowSorter.setSortKeys(sortKeys); 
		table.setRowSorter(rowSorter);

		popup = new NodeWrapperPopupMenu(SwingUtilities.getRoot(this), model);
	

		TableCellRenderer cellRenderer = new NodeWrapperRenderer(model);
		table.setDefaultRenderer(NodeWrapper.class, cellRenderer);
	}

	@Override
	public JComponent asJComponent() {
		return this;
	}

	private final class TableViewModelListener extends ParrotModelAdapter {
		@Override
		public void restrictionsChanged(final Collection<NodeWrapper> currentlyHidden) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					rowSorter.setRowFilter(new RowFilter<TableParrotModel, Integer>() {
						@Override
						public boolean include(Entry<? extends TableParrotModel, ? extends Integer> entry) {
							if (currentlyHidden == null || currentlyHidden.isEmpty()) {
								return true;
							}
							// entry represents a statement triple
							// only include entry (=row) if none of its parts is included in currentlyHidden
							for (int i = 0; i < entry.getValueCount(); i++) {
								Object value = entry.getValue(i);
								if (value instanceof NodeWrapper && currentlyHidden.contains(value)) {
									// we found a part that is supposed to be hidden
									return false;
								}
							}
							return true;
						}
					});
					table.repaint();
				}
			});
		}

		@Override
		public void highlightsChanged() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					table.invalidate();
					table.repaint();
				}
			});
		}


		@Override
		public void modelIdle() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					table.setEnabled(true);
					table.setCursor(Cursor.getDefaultCursor());
				}
			});
		}

		@Override
		public void modelBusy() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					table.setEnabled(false);
				}
			});
		}
	}

	private void fireNodeSelected(NodeWrapper newSelection) {
		// from EventListenerList javadocs (minus the bugs)
		PickListener[] listeners = pickListeners
				.getListeners(PickListener.class);
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 1; i >= 0; i--) {
			listeners[i].picked(newSelection);
		}
	}
	
	@Override
	public Collection<NodeWrapper> getSelectedNodes() {
		ArrayList<NodeWrapper> result = new ArrayList<NodeWrapper>(1);
		result.add(selectedNode);
		return result;
	}

	@Override
	public void addPickListener(PickListener listener) {
		pickListeners.add(PickListener.class, listener);
	}

	@Override
	public void removePickListener(PickListener listener) {
		pickListeners.remove(PickListener.class, listener);
	}

	@Override
	public String getTitle() {
		return "List";
	}
}
