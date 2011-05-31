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

import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

/**
 * 
 * @author as151 TODO manage list of navigator listeners
 */
public abstract class AbstractNavigatorPanel extends JPanel implements
		NavigatorComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ParrotModel model;
	private ComponentAdapter showHideListener;

	public AbstractNavigatorPanel() {
		super();
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		showHideListener = new ComponentAdapter() {
			@Override
			public void componentHidden(ComponentEvent e) {
				if (model == null) {
					return;
				}
				deactivateFilters();
			}

			@Override
			public void componentShown(ComponentEvent e) {
				if (model == null) {
					return;
				}
				activateFilters();
			}

		};
	}

	protected abstract void activateFilters();

	protected abstract void deactivateFilters();

	@Override
	public JComponent asJComponent() {
		return this;
	}

	@Override
	public void setModel(ParrotModel model) {
		this.model = model;
	}

	ParrotModel getModel() {
		return model;
	}

	@Override
	public ComponentListener getShowHideListener() {
		return showHideListener;
	}

	@Override
	public boolean hasShowHideListener() {
		return true;
	}

	protected void removeFilter(final Filter filter) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				getModel().removeFilter(filter);
				return null;
			}

			@Override
			protected void done() {
				setCursor(Cursor.getDefaultCursor());
			}
		};
		worker.execute();
	}

	protected void applyFilter(final Filter filter) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				getModel().addFilter(filter);
				return null;
			}

			@Override
			protected void done() {
				setCursor(Cursor.getDefaultCursor());
			}
		};
		worker.execute();
	}

	protected void replaceFilter(final Filter oldFilter, final Filter newFilter) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				getModel().replaceFilter(oldFilter, newFilter);
				return null;
			}

			@Override
			protected void done() {
				setCursor(Cursor.getDefaultCursor());
			}
		};
		worker.execute();
	}

	@Override
	public void setSelectedNodes(Collection<NodeWrapper> selected) {
		// ignore
	}

	@Override
	public boolean tellSelectionWhenShown() {
		return false;
	}
	
	

}
