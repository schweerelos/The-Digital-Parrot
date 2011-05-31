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

package net.schweerelos.parrot.model.filters;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;

import org.apache.log4j.Logger;

import net.schweerelos.parrot.model.NodeWrapper;

public class ChainLink implements Cloneable {
	public static class CloneComparator implements Comparator<ChainLink> {
		@Override
		public int compare(ChainLink o1, ChainLink o2) {
			// if they're both null, they're the same
			if (o1 == null && o2 == null) {
				return 0;
			}
			
			// if one is null and the other isn't, the non-null one is bigger
			if (o1 != null && o2 == null) {
				return 1;
			}
			if (o1 == null && o2 != null) {
				return -1;
			}
			
			// if both have types, compare the types
			if (o1.hasType() && o2.hasType()) {
				return o1.getType().compareTo(o2.getType());
			}
			
			// or: if both have instances, compare the instances
			if (o1.hasInstance() && o2.hasInstance()) {
				return o1.getInstance().compareTo(o2.getInstance());
			}
			
			// or: if one has a type and the other doesn't, the one with type is bigger
			if (o1.hasType() && !o2.hasType()) {
				return 1;
			} else if (!o1.hasType() && o2.hasType()){
				return -1;				
			}
			
			// or: if one has an instance and the other doesn't, the one with instance is bigger
			if (o1.hasInstance() && !o2.hasInstance()) {
				return 1;
			} else if (!o1.hasInstance() && o2.hasInstance()) {
				return -1;
			}
			
			// or: if both are any/any, they are equal
			if (!o1.hasType() && !o2.hasType() && !o1.hasInstance() && !o2.hasInstance()) {
				return 0;
			}
			
			// don't know if we can ever get here
			// return hashCode comp because we have to return something
			Logger logger = Logger.getLogger(CloneComparator.class);
			logger.warn("got to weird case in comparing two ChainLinks -- comparing " + o1 + " and " + o2);
			return new Integer(o1.hashCode()).compareTo(o2.hashCode());
		}
	}

	public static final String INSTANCE_PROPERTY = "INSTANCE";
	public static final String TYPE_PROPERTY = "TYPE";
	
	private NodeWrapper type;
	private NodeWrapper instance;
	
	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	
	public ChainLink(NodeWrapper type, NodeWrapper instance) {
		this.type = type;
		this.instance = instance;
	}

	@Override
	public String toString() {
		return String.format("Chain link: type='%s', instance='%s'", type, instance);
	}

	public NodeWrapper getType() {
		return type;
	}

	public NodeWrapper getInstance() {
		return instance;
	}
	
	public boolean hasType() {
		return type != null;
	}
	
	public boolean hasInstance() {
		return instance != null;
	}

	public void setInstance(NodeWrapper node) {
		NodeWrapper oldInstance = instance;
		instance = node;
		changeSupport.firePropertyChange(INSTANCE_PROPERTY, oldInstance, instance);
	}
	
	public void setType(NodeWrapper node) {
		NodeWrapper oldType = type;
		type = node;
		changeSupport.firePropertyChange(TYPE_PROPERTY, oldType, type);
	}
	
	/**
	 * Does *not* clone the listener list.
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		ChainLink result = (ChainLink) super.clone();
		result.instance = instance;
		result.type = type;
		return result;
	}

	public static Comparator<ChainLink> getCloneComparator() {
		return new CloneComparator(); 
	}
	
	/* listener support */
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}
	
}
