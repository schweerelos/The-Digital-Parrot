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


package net.schweerelos.parrot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatLonBounds {

	@SuppressWarnings("serial")
	public static class InvalidStringException extends Exception {
		public InvalidStringException(String message) {
			super(message);
		}

		public InvalidStringException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public final float topLeftLat;
	public final float topLeftLon;
	public final float bottomRightLat;
	public final float bottomRightLon;

	public LatLonBounds(float topLeftLat, float topLeftLon,
			float bottomRightLat, float bottomRightLon) {
		this.topLeftLat = topLeftLat;
		this.topLeftLon = topLeftLon;
		this.bottomRightLat = bottomRightLat;
		this.bottomRightLon = bottomRightLon;
	}

	public static LatLonBounds fromString(String boundsString)
			throws InvalidStringException {
		float topLeftLat = 0;
		float topLeftLon = 0;
		float bottomRightLon = 0;
		float bottomRightLat = 0;

		Pattern pattern = Pattern.compile("(\\-?\\d+\\.\\d+)");
		Matcher matcher = pattern.matcher(boundsString);
		try {
			if (matcher.find()) {
				bottomRightLat = Float.parseFloat(matcher.group(1));
			} else {
				throw new InvalidStringException(
						"bounds string doesn't follow expected pattern: can't find bottom right latitude");
			}
			if (matcher.find()) {
				topLeftLon = Float.parseFloat(matcher.group(1));
			} else {
				throw new InvalidStringException(
						"bounds string doesn't follow expected pattern: can't find top left longitude");
			}
			if (matcher.find()) {
				topLeftLat = Float.parseFloat(matcher.group(1));
			} else {
				throw new InvalidStringException(
						"bounds string doesn't follow expected pattern: can't find top left latitude");
			}
			if (matcher.find()) {
				bottomRightLon = Float.parseFloat(matcher.group(1));
			} else {
				throw new InvalidStringException(
						"bounds string doesn't follow expected pattern: can't find bottom right longitude");
			}
		} catch (NumberFormatException nfe) {
			throw new InvalidStringException("can't convert to number", nfe);
		}
		return new LatLonBounds(topLeftLat, topLeftLon, bottomRightLat,
				bottomRightLon);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LatLonBounds)) {
			return false;
		}
		LatLonBounds other = (LatLonBounds) obj;
		return topLeftLat == other.topLeftLat 
			&& topLeftLon == other.topLeftLon 
			&& bottomRightLat == other.bottomRightLat
			&& bottomRightLon == other.bottomRightLon;
	}

	@Override
	public int hashCode() {
		return new Float(topLeftLat).hashCode() 
			+ new Float(topLeftLon).hashCode() 
			+ new Float(bottomRightLat).hashCode()
			+ new Float(bottomRightLon).hashCode();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("((");
		sb.append(topLeftLat);
		sb.append(", ");
		sb.append(topLeftLon);
		sb.append("), (");
		sb.append(bottomRightLat);
		sb.append(", ");
		sb.append(bottomRightLon);
		sb.append("))");
		return sb.toString();
	}
	
}
