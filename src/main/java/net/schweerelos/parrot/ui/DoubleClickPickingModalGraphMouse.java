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

import net.schweerelos.parrot.model.NodeWrapper;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMousePlugin;

public class DoubleClickPickingModalGraphMouse<T1, T2> extends
		DefaultModalGraphMouse<NodeWrapper, NodeWrapper> {

	private GraphMousePlugin doubleClickPickingPlugin;

	public DoubleClickPickingModalGraphMouse() {
		super();
	}

	public void setDoubleClickPickingPlugin(GraphMousePlugin dcpp) {
		if (doubleClickPickingPlugin == dcpp) {
			return;
		}
		if (doubleClickPickingPlugin != null && mode == Mode.PICKING) {
			remove(doubleClickPickingPlugin);
		}
		if (dcpp != null && mode == Mode.PICKING) {
			add(doubleClickPickingPlugin);
		}
		doubleClickPickingPlugin = dcpp;
	}

	@Override
	protected void setPickingMode() {
		super.setPickingMode();
		if (doubleClickPickingPlugin != null) {
			add(doubleClickPickingPlugin);
		}
	}

	@Override
	protected void setTransformingMode() {
		super.setTransformingMode();
		if (doubleClickPickingPlugin != null) {
			remove(doubleClickPickingPlugin);
		}
	}

}
