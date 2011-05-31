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

import java.awt.event.ComponentListener;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public interface NavigatorComponent {
	public void setModel(ParrotModel model);
	public JComponent asJComponent();
	
	/**
	 * Lets the navigator component indicate whether it has
	 * a window listener that it would like to have attached to its parent window.	 
	 * @return true if this navigator component has a window listener
	 */
	public boolean hasShowHideListener();
	/**
	 * The window listener that this navigator component would like to have attached
	 * to its parent window.
	 * @return a window listener iff hasShowHideListener == true; null otherwise.
	 */
	public ComponentListener getShowHideListener();
	public String getNavigatorName();
	public List<Action> getActionsForNode(NodeWrapper currentNode);
	public List<Action> getActionsForType(NodeWrapper type);
	public String getAcceleratorKey();
	
	public boolean tellSelectionWhenShown();
	public void setSelectedNodes(Collection<NodeWrapper> selected);
}
