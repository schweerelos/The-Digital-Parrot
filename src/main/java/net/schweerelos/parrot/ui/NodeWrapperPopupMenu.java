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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.schweerelos.parrot.CombinedParrotApp;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public class NodeWrapperPopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;
	private ParrotModel model;
	private NodeWrapper node;
	
	private AbstractAction deleteAction;
	private AbstractAction editAction;
	private Component parentComponent;
	
	public NodeWrapperPopupMenu(Component parentComponent, ParrotModel model) {
		this.parentComponent = parentComponent;
		this.model = model;
		
		initActions();
		
		clearPopup();
	}
	
	@SuppressWarnings("serial")
	private void initActions() {
		deleteAction = new AbstractAction("Delete") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (node != null) {
					NodeWrapperPopupMenu.this.model.deleteEdge(node);
				}
			}
		};
		deleteAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		deleteAction.setEnabled(false);
		
		editAction = new AbstractAction("Edit...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO #13 implement (editing/show edit dialog)
			}
		};
		editAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		editAction.setEnabled(false);
	}
	
	public void setNodeWrapper(NodeWrapper node) {
		this.node = node;
		clearPopup();
		addActionsForNode(node);
		addActionsForTypes(node);
	}

	private void addActionsForTypes(NodeWrapper node) {
		if (node.isOntResource() && node.getOntResource().isProperty()) {
			Set<NodeWrapper> superProps = model.getSuperPredicates(node);
			List<NodeWrapper> sortedSuperProps = new ArrayList<NodeWrapper>(superProps.size());
			sortedSuperProps.addAll(superProps);
			Collections.sort(sortedSuperProps);
			
			List<JMenu> typeMenus = new ArrayList<JMenu>();
			
			for (final NodeWrapper superProp : sortedSuperProps) {
				JMenu propMenu = createTypeMenu(superProp);
				if (propMenu.getComponentCount() > 0) {
					typeMenus.add(propMenu);					
				}
			}

			if (!typeMenus.isEmpty()) {
				addSeparator();
				for (JMenu menu : typeMenus) {
					add(menu);
				}
			}
			
		} else if (node.isOntResource() && node.getOntResource().isIndividual()) {
			Set<NodeWrapper> types = model.getTypesForIndividual(node);
			List<NodeWrapper> sortedTypes = new ArrayList<NodeWrapper>(types.size());
			sortedTypes.addAll(types);
			Collections.sort(sortedTypes);

			List<JMenu> typeMenus = new ArrayList<JMenu>();
			
			for (final NodeWrapper type : sortedTypes) {
				JMenu typeMenu = createTypeMenu(type);
				if (typeMenu.getComponentCount() > 0) {
					typeMenus.add(typeMenu);
				}
			}
			
			if (!typeMenus.isEmpty()) {
				addSeparator();
				for (JMenu menu : typeMenus) {
					add(menu);
				}
			}
		}
	}

	private void addActionsForNode(NodeWrapper node) {
		CombinedParrotApp app = null;
		try {
			Component root = SwingUtilities.getRoot(parentComponent);
			app = (CombinedParrotApp) root;
		} catch (ClassCastException cce) {
			return;
		}

		Map<String, List<Action>> navigatorActions = app
				.getNavigatorActionsForNode(node);

		for (String navigator : navigatorActions.keySet()) {
			List<Action> actions = navigatorActions.get(navigator);
			if (actions.isEmpty()) {
				continue;
			}

			addSeparator();
			for (Action action : actions) {
				add(action);
			}
		}
	}

	private JMenu createTypeMenu(final NodeWrapper type) {
		String typeName = type.toString();
		JMenu propMenu = new JMenu(typeName);
		propMenu.setMnemonic(typeName.charAt(0));
		
		CombinedParrotApp app = null;
		try {
			Component root = SwingUtilities.getRoot(parentComponent);
			app = (CombinedParrotApp) root;
		} catch (ClassCastException cce) {
			return propMenu;
		}
		
		Map<String, List<Action>> navigatorActions = app.getNavigatorActionsForType(type);

		boolean first = true;
		for (String navigator : navigatorActions.keySet()) {
			List<Action> actions = navigatorActions.get(navigator);
			if (actions.isEmpty()) {
				continue;
			}
			
			if (first) {
				first = false;
			} else {
				propMenu.addSeparator();
			}
			for (Action action : actions) {
				propMenu.add(action);
			}
			
		}
		
		return propMenu;
	}

	private void clearPopup() {
		removeAll();
		
		add(editAction);
		add(deleteAction);
	}

}
