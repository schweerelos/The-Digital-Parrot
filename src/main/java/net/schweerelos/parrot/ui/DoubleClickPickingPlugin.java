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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.GraphMousePlugin;


	public abstract class DoubleClickPickingPlugin extends
			AbstractGraphMousePlugin implements GraphMousePlugin, MouseListener {

		public DoubleClickPickingPlugin() {
			super(MouseEvent.BUTTON1_DOWN_MASK);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				doubleClickOccurred(e);
			}
		}

		abstract void doubleClickOccurred(MouseEvent e);

		@Override
		public void mouseEntered(MouseEvent e) {
			// ignore
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// ignore
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// ignore
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// ignore
		}

	}
