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

package net.schweerelos.timeline.ui;

import java.awt.Color;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import net.schweerelos.timeline.model.PayloadInterval;

public class IntervalView extends JPanel {

	private static final long serialVersionUID = 1L;

	private PayloadInterval<?> interval;

	public IntervalView(PayloadInterval<?> interval) {
		this.interval = interval;
		ToolTipManager.sharedInstance().registerComponent(this);
		setOpaque(true);
	}
	
	@Override
	public String getToolTipText() {
		return interval.getPayload().toString();
	}

	public PayloadInterval<?> getInterval() {
		return interval;
	}

	public void setColors(Map<ColorKeys, Color> colors) {
		setBackground(colors.get(ColorKeys.IntervalFill));
		setBorder(BorderFactory.createLineBorder(colors.get(ColorKeys.IntervalOutline)));
	}
	
}
