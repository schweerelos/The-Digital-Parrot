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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.filters.Chain;
import net.schweerelos.parrot.model.filters.ChainLink;
import net.schweerelos.parrot.model.filters.HighlightChainFilter;
import net.schweerelos.parrot.model.filters.RestrictToChainFilter;

import com.hp.hpl.jena.ontology.OntResource;

@SuppressWarnings("serial")
public class ChainNavigator extends AbstractNavigatorPanel implements
		PickListener {

	private static final String CLEAR_ICON = "images/clear.png";
	private static final String PLUS_ONE_ICON = "images/chain-plus-one.png";
	private static final String NAME = "Connections";
	private static final String ACCELERATOR_KEY = "C";

	private Chain chain;
	private RestrictToChainFilter restrictingFilter;
	private HighlightChainFilter highlightingFilter;

	private JPanel chainPanel;

	private ClearChainAction clearAction;
	private Action oneOutAction;

	private PropertyChangeListener lastLinkListener;
	private Map<ChainLink, ChainLinkView> linkViews = new TreeMap<ChainLink, ChainLinkView>(
			ChainLink.getCloneComparator());
	private Collection<NodeWrapper> selectedNodes;

	public ChainNavigator() {
		super();

		setLayout(new GridBagLayout());

		chainPanel = new JPanel();
		chainPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridheight = 2;

		JScrollPane chainScrollPane = new JScrollPane(chainPanel);
		chainScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		chainScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		chainScrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(chainScrollPane, constraints);

		clearAction = new ClearChainAction();
		constraints.anchor = GridBagConstraints.LINE_END;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0;
		constraints.gridheight = 1;
		constraints.gridx = 1;

		add(new JButton(clearAction), constraints);
		oneOutAction = new AbstractAction("Next", new ImageIcon(PLUS_ONE_ICON)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (chain != null) {
							chain.add(null, null);
						}
					}
				});
			}
		};
		oneOutAction.putValue(Action.LONG_DESCRIPTION,
				"Show all direct neighbours of the last chain link");
		add(new JButton(oneOutAction), constraints);
	}

	@Override
	public void setModel(ParrotModel model) {
		super.setModel(model);

		if (model == null) {
			return;
		}

		chain = new Chain(model);

		// make sure the 'clear' action is in the right state
		// (whenever the last link itself changes)
		lastLinkListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				clearAction.setEnabled(chain.canClear());
			}
		};
		// initially listen to the last link
		if (chain.getLastLink() != null) {
			chain.getLastLink().addPropertyChangeListener(lastLinkListener);
		}

		// make sure the 'clear' action is in the right state
		// and make sure @code{lastLinkListener} is listening to the right link
		// and make sure the right link views are being shown
		// (whenever the chain changes)
		chain.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				// make sure the 'clear' action is in the right
				// state
				clearAction.setEnabled(chain.canClear());
				oneOutAction.setEnabled(chain.canAddAnyAny());

				String propName = pce.getPropertyName();
				if (propName.equals(Chain.LAST_LINK_PROPERTY)) {

					// make sure @code{lastLinkListener} is listening to
					// the right link
					if (pce.getOldValue() != null
							&& pce.getOldValue() instanceof ChainLink) {
						((ChainLink) pce.getOldValue())
								.removePropertyChangeListener(lastLinkListener);
					}
					if (pce.getNewValue() != null
							&& pce.getNewValue() instanceof ChainLink) {
						((ChainLink) pce.getNewValue())
								.addPropertyChangeListener(lastLinkListener);
					}
				} else if (propName.equals(Chain.SIZE_PROPERTY)
						|| propName.equals(Chain.CONTENTS_PROPERTY)) {

					if (restrictingFilter != null
							&& restrictingFilter.sameChain(chain)) {
						// nothing has changed
						// -> ignore
						return;
					}

					Filter oldRestrictingFilter = restrictingFilter;
					Filter oldHighlightingFilter = highlightingFilter;

					if (chain.isEmpty()) {
						restrictingFilter = null;
						highlightingFilter = null;
					} else {
						restrictingFilter = new RestrictToChainFilter(chain);
						highlightingFilter = new HighlightChainFilter(
								restrictingFilter);
					}

					replaceFilter(oldRestrictingFilter, restrictingFilter);
					replaceFilter(oldHighlightingFilter, highlightingFilter);

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							updateChainPanel();
						}
					});
				}
			}
		});

		// initially put 'clear' action into the right state
		clearAction.setEnabled(chain.canClear());
		oneOutAction.setEnabled(chain.canAddAnyAny());

		// initially show all links in the chain
		updateChainPanel();
	}

	@Override
	protected void activateFilters() {
		if (getModel() == null) {
			return;
		}
		if (restrictingFilter != null) {
			applyFilter(restrictingFilter);
		} else {
			if (selectedNodes != null && !selectedNodes.isEmpty()) {
				NodeWrapper firstSelectedNode = selectedNodes.iterator().next();
				if (firstSelectedNode != null) {
					highlightingFilter = null;
					chain.expandOrRestart(firstSelectedNode);
				}
			}
		}
		if (highlightingFilter != null) {
			applyFilter(highlightingFilter);
		}
	}

	@Override
	protected void deactivateFilters() {
		if (getModel() == null) {
			return;
		}
		if (restrictingFilter != null) {
			removeFilter(restrictingFilter);
		}
		if (highlightingFilter != null) {
			removeFilter(highlightingFilter);
		}
	}

	@Override
	public List<Action> getActionsForNode(final NodeWrapper currentNode) {
		List<Action> actions = new ArrayList<Action>();
		if (!currentNode.isOntResource()) {
			// don't deal with literals etc
			return actions;
		} else {
			OntResource res = currentNode.getOntResource();
			if (res.isProperty()) {
				// don't deal with properties
				return actions;
			}
		}
		return actions;
	}

	@Override
	public List<Action> getActionsForType(NodeWrapper type) {
		if (!type.isType()) {
			return getActionsForNode(type);
		}

		List<Action> actions = new ArrayList<Action>();
		return actions;
	}

	@Override
	public String getNavigatorName() {
		return NAME;
	}

	@Override
	public String getAcceleratorKey() {
		return ACCELERATOR_KEY;
	}

	private ChainLinkView createLinkComponent(final ChainLink link) {
		final ChainLinkView chainComponent = new ChainLinkView(chain, link);

		final RemoveFromChainAction removeAction = new RemoveFromChainAction(
				link);
		removeAction.setEnabled(chain.canRemove(link));
		chainComponent.setCanChange(chain.canChange(link));
		chain.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				removeAction.setEnabled(chain.canRemove(link));
				chainComponent.setCanChange(chain.canChange(link));
			}
		});
		chainComponent.setRemoveAction(removeAction);

		return chainComponent;
	}

	@Override
	public void picked(NodeWrapper node) {
		if (!isVisible()) {
			// ignore
			return;
		}
		// only react to selection of individuals (and not, eg, properties)
		if (node.isType() || !node.isOntResource()
				|| !node.getOntResource().isIndividual()) {
			return;
		}
		chain.expandOrRestart(node);
	}

	@Override
	public void setSelectedNodes(Collection<NodeWrapper> selected) {
		selectedNodes = selected;
	}

	@Override
	public boolean tellSelectionWhenShown() {
		return true;
	}

	private void updateChainPanel() {
		chainPanel.removeAll();
		Map<ChainLink, ChainLinkView> newLinkViews = new HashMap<ChainLink, ChainLinkView>();

		for (ChainLink link : chain.getLinks()) {
			ChainLinkView view;
			if (linkViews.containsKey(link)) {
				view = linkViews.get(link);
			} else {
				view = createLinkComponent(link);
			}
			newLinkViews.put(link, view);
			view.setCanChange(chain.canChange(link));
			chainPanel.add(view);
		}
		linkViews.clear();
		linkViews = newLinkViews;

		revalidate();
		repaint();
	}

	private final class ClearChainAction extends AbstractAction {
		public ClearChainAction() {
			super("Clear", new ImageIcon(CLEAR_ICON));
			super.putValue(Action.LONG_DESCRIPTION,
					"Remove all items from the chain");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!chain.canClear()) {
				return;
			}
			removeFilter(restrictingFilter);
			if (highlightingFilter != null) {
				removeFilter(highlightingFilter);
			}
			chain.clear();
		}
	}

	private final class RemoveFromChainAction extends AbstractAction {
		private final ChainLink link;

		private RemoveFromChainAction(ChainLink link) {
			super("Remove from chain");
			this.link = link;
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!chain.canRemove(link)) {
				return;
			}
			chain.remove(link);
		}

	}
}
