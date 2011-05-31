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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.filters.TextFilter;

@SuppressWarnings("serial")
public class TextFilterComponent extends AbstractNavigatorPanel {
	private static final String NAME = "Search";
	private static final String ACCELERATOR_KEY = "S";
	
	private JTextField queryBox;
	protected TextFilter lastFilter;
	private JLabel resultsLabel;

	public TextFilterComponent() {
		super();
		setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets.right = 12;
		constraints.insets.left = 12;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
				
		JLabel label = new JLabel("Text:");
		label.setDisplayedMnemonic('t');
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = 1;
		constraints.weighty = 0;
		add(label, constraints);
		
		queryBox = new JTextField();
		queryBox.setToolTipText("Highlight everything that contains this text");
		queryBox.setColumns(20);
		constraints.insets.right = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		add(queryBox, constraints);
		
		label.setLabelFor(queryBox);
		
		
		AbstractAction queryAction = new AbstractAction("Search") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getModel() == null) {
					return;
				}
				if (lastFilter != null) {
					removeFilterFromModel(lastFilter);
				}
				if (queryBox.getText() != null && !queryBox.getText().isEmpty()) {
					lastFilter = new TextFilter(queryBox.getText());
					addFilterToModel(lastFilter);
					queryBox.selectAll();
				}
			}
		};
		queryAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		constraints.fill = GridBagConstraints.NONE;
		constraints.insets.left = 11;
		constraints.weightx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		add(new JButton(queryAction), constraints);
		
		queryBox.addActionListener(queryAction);
		
		resultsLabel = new JLabel();
		resultsLabel.setFont(resultsLabel.getFont().deriveFont(Font.PLAIN));
		clearResultsLabel();
		constraints.insets.right = 12;
		constraints.insets.top = 11;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.weighty = 1;
		add(resultsLabel, constraints);
	}

	private void clearResultsLabel() {
		setResultsLabelText("&nbsp;<br>&nbsp;");
	}
	
	private void setResultsLabelText(String text) {
		resultsLabel.setText("<html>" + text + "</html>");
	}

	@Override
	protected void activateFilters() {
		if (lastFilter != null && getModel() != null) {
			addFilterToModel(lastFilter);
		}
	}

	@Override
	protected void deactivateFilters() {
		if (lastFilter != null && getModel() != null) {
			removeFilterFromModel(lastFilter);
		}
	}

	private void addFilterToModel(final Filter filter) {
		setResultsLabelText("Searching...");
		SwingUtilities.getWindowAncestor(this).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				getModel().addFilter(filter);
				return null;
			}

			@Override
			protected void done() {
				int numberOfHits = lastFilter.getNumberOfMatches();
				String queryString = lastFilter.getQueryString();
				if (numberOfHits <= 0) {
					setResultsLabelText("No results for <em>" +
							queryString +
							"</em>.<br>Try adding * to your query term(s) to get partial matches.");
				} else if (numberOfHits == 1) {
					setResultsLabelText("1 result for <em>" +
							queryString +
							"</em>.");					
				} else {
					setResultsLabelText(numberOfHits + " results for <em>" +
							queryString +
							"</em>.");
				}
				SwingUtilities.getWindowAncestor(TextFilterComponent.this).setCursor(Cursor.getDefaultCursor());
			}
		};
		worker.execute();
	}
	

	private void removeFilterFromModel(final Filter filter) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				getModel().removeFilter(filter);
				return null;
			}

			@Override
			protected void done() {
				clearResultsLabel();
			}
			
		};
		worker.execute();
	}

	@Override
	public List<Action> getActionsForNode(NodeWrapper currentNode) {
		return Collections.emptyList();
	}
	
	@Override
	public List<Action> getActionsForType(NodeWrapper type) {
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
