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

public final class UIConstants {

	// the lower the alpha, the *more* transparent the colour
	private static final int DEFAULT_ALPHA = 100;
	private static final int DEFAULT_TT_ALPHA = 20;

	public static final Color ENVIRONMENT_MEDIUM = new Color(0x90aab0);
	public static final Color ENVIRONMENT_LIGHT = new Color(0xbacbce);
	public static final Color ENVIRONMENT_LIGHTEST = new Color(
			0xeff6f8);
	public static final Color ENVIRONMENT_SHADOW_DARK = new Color(
			0x34707d);	
	
	public static final Color TEXT = Color.BLACK;
	private static final Color ALMOST_BLACK = new Color(0x010101);
	public static final Color T_TEXT = transparentVersion(ALMOST_BLACK);
	public static final Color TT_TEXT = transparentVersion(ALMOST_BLACK, DEFAULT_TT_ALPHA);	

	public static final Color T_ENVIRONMENT_MEDIUM = transparentVersion(ENVIRONMENT_MEDIUM);
	public static final Color T_ENVIRONMENT_LIGHT = transparentVersion(ENVIRONMENT_LIGHT);
	public static final Color T_ENVIRONMENT_LIGHTEST = transparentVersion(ENVIRONMENT_LIGHTEST);

	public static final Color TT_ENVIRONMENT_MEDIUM = transparentVersion(ENVIRONMENT_MEDIUM, DEFAULT_TT_ALPHA);
	public static final Color TT_ENVIRONMENT_LIGHT = transparentVersion(ENVIRONMENT_LIGHT, DEFAULT_TT_ALPHA);
	public static final Color TT_ENVIRONMENT_LIGHTEST = transparentVersion(ENVIRONMENT_LIGHTEST, DEFAULT_TT_ALPHA);
	
	public static final Color ACCENT_MEDIUM = new Color(0xf2bf4e);
	public static final Color ACCENT_LIGHT = new Color(0xf7d891);
	public static final Color ACCENT_LIGHTEST = new Color(0xfff9e9);
	
	public static final Color T_ACCENT_MEDIUM = transparentVersion(ACCENT_MEDIUM);
	public static final Color T_ACCENT_LIGHT = transparentVersion(ACCENT_LIGHT);
	public static final Color T_ACCENT_LIGHTEST = transparentVersion(ACCENT_LIGHTEST);
	
	public static final Color SECOND_ACCENT_MEDIUM = new Color(0x9f90b0);
	public static final Color SECOND_ACCENT_LIGHT = new Color(0xc4bace);
	public static final Color SECOND_ACCENT_LIGHTEST = new Color(0xf5eff8);

	public static final Color THIRD_ACCENT_MEDIUM = new Color(0x96b090);
	public static final Color THIRD_ACCENT_LIGHT = new Color(0xbeceba);
	public static final Color THIRD_ACCENT_LIGHTEST = new Color(0xf1f8ef);
	
	private UIConstants() {
		// private constructor to hide it
	}

	private static Color transparentVersion(Color original) {
		return transparentVersion(original, DEFAULT_ALPHA);
	}

	private static Color transparentVersion(Color original, int alpha) {
		return new Color(original.getRed(), original.getGreen(), original.getBlue(), alpha);
	}
	
}
