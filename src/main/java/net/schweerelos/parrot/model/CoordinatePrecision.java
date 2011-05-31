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


package net.schweerelos.parrot.model;

public enum CoordinatePrecision {
	RoomPrecision (0.001),
	BuildingPrecision (0.01),
	BlockPrecision (0.1),
	SuburbPrecision (1.0),
	CityPrecision (10.0),
	SmallCountryPrecision (100.0),
	MediumCountryPrecision (1000.0),
	LargeCountryPrecision (5000.0),
	ContinentPrecision (10000.0);
	
	private final double maxKilometers;
	
	CoordinatePrecision(double maxKilometers) {
		this.maxKilometers = maxKilometers;
	}

	public double getMaxKilometers() {
		return maxKilometers;
	}
	
	public boolean showOnMap(CoordinatePrecision mapPrecision) {
		// TODO #10 this calculation can probably be improved
		int difference = ordinal() - mapPrecision.ordinal();
		if (difference < 0) {
			return difference * -1 < 3; 
		} else {
			return difference < 2;
		}
	}
	
	public int toZoomLevel() {
		return 18 - ordinal() * 2;
	}

	public static CoordinatePrecision precisionForScale(float scale) {
		// TODO #10 this calculation is off somehow
		float kmPerPixel = scale / 10000.0f;
		CoordinatePrecision[] precisionValues = CoordinatePrecision.values();
		for (int i = precisionValues.length - 1; i >= 0; i--) {
			if (kmPerPixel > precisionValues[i].maxKilometers) {
				return precisionValues[i];
			}
		}
		return RoomPrecision;
	}
	
}
