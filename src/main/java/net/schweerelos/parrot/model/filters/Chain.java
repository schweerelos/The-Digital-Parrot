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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public class Chain {

	public static final String LAST_LINK_PROPERTY = "LAST_NODE_PROPERTY";
	public static final String SIZE_PROPERTY = "SIZE_PROPERTY";
	public static final String CONTENTS_PROPERTY = "CONTENTS_PROPERTY";

	private ParrotModel model;
	private PropertyChangeSupport changeSupport;
	private List<ChainLink> chain;

	private List<List<NodeWrapper>> nodeChains;

	private PropertyChangeListener lastLinkListener;
	private PropertyChangeListener secondLastLinkListener;
	private PropertyChangeListener anyLinkChangeListener;

	public Chain(ParrotModel model) {
		this.model = model;
		chain = new ArrayList<ChainLink>();

		changeSupport = new PropertyChangeSupport(this);
		 
		lastLinkListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				if (!pce.getSource().equals(getLastLink())) {
					return;
				}

				String propName = pce.getPropertyName();
				boolean addAnyAny = false;
				if (propName.equals(ChainLink.INSTANCE_PROPERTY)) {
					// do we need to add a new link?
					addAnyAny = pce.getOldValue() == null // if the instance
							// used to be 'any'
							&& pce.getNewValue() != null // but isn't 'any' now
							&& !getLastLink().hasType(); // and there is no type
				} else if (propName.equals(ChainLink.TYPE_PROPERTY)) {
					// same as above (just type/instance reversed)
					addAnyAny = pce.getOldValue() == null
							&& pce.getNewValue() != null
							&& !getLastLink().hasInstance();
				}

				if (addAnyAny) {
					// don't automatically do this
					// add(null, null);
				}
			}
		};
		secondLastLinkListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				ChainLink secondLastLink = getSecondLastLink();
				if (!pce.getSource().equals(secondLastLink)) {
					return;
				}
				if (!secondLastLink.hasInstance() && !secondLastLink.hasType()) {
					remove(getLastLink());
				}
			}
		};
		anyLinkChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				String propName = pce.getPropertyName();
				if (propName.equals(ChainLink.INSTANCE_PROPERTY)
						|| propName.equals(ChainLink.TYPE_PROPERTY)) {
					Object source = pce.getSource();
					int index = chain.indexOf(source);
					changeSupport.fireIndexedPropertyChange(CONTENTS_PROPERTY,
							index, null, source);
				}
			}
		};
		
		addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// if anything changes, reset cached nodeChains
				nodeChains = null;
			}
		});

		add(null, null);
	}
	
	public void expandOrRestart(NodeWrapper node) {
		if (chain.isEmpty()) {
			add(null, node);
			return;
		}
		
		ChainLink lastLink = getLastLink();
		if (lastLink.hasInstance() && lastLink.getInstance().equals(node)) {
			// add new any/any
			add(null, null);
			return;
		}
		
		ChainLink chainLink = getChainLink(node);
		int index = chain.indexOf(chainLink);
		if (index < chain.size() - 1) {
			if (chainLink.hasType()) {
				// if it used to be a type link, make it an instance link instead
				chainLink.setInstance(node);
			}
			// this node is "in" the middle of the chain somewhere
			// -> re-start the chain at that instance
			ChainLink oldLast = getLastLink();
			int oldSize = chain.size();
			
			chain = chain.subList(index, chain.size());
			
			changeSupport.firePropertyChange(LAST_LINK_PROPERTY, oldLast, getLastLink());
			changeSupport.firePropertyChange(SIZE_PROPERTY, oldSize, chain.size());
			
			return;
		} else {
			// must be the last chain link then
			// set the last link's instance to node
			getLastLink().setInstance(node);
		}

	}

	private ChainLink getChainLink(NodeWrapper node) {
		ChainLink lastLink = getLastLink();
		if (chain.size() == 1 && !lastLink.hasType() && !lastLink.hasInstance()) {
			return lastLink;
		}
		if (nodeChains == null) {
			nodeChains = model.getChains(chain);
		}
		for (int i = 0; i < chain.size(); i++) {
			ChainLink currentLink = chain.get(i);
			for (List<NodeWrapper> chain : nodeChains) {
				NodeWrapper chainWrapper = chain.get(i);
				if (chainWrapper.equals(node)) {
					return currentLink;		
				}
			}
		}
		// TODO Auto-generated method stub
		return null;
	}


	public Set<NodeWrapper> getPossibleTypes(ChainLink link) {
		if (link.hasInstance()) {
			return getPossibleTypesForIndividual(link.getInstance());
		}
		
		int index = chain.indexOf(link);
		if (index == 0) {
			// start of chain -> everything is possible
			return model.getSubjectTypes();
		}
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (nodeChains == null) {
			nodeChains = model.getChains(chain);
		}
		for (List<NodeWrapper> chain : nodeChains) {
			if (chain.size() > index) {
				NodeWrapper chainWrapper = chain.get(index);
				result.addAll(model.getTypesForIndividual(chainWrapper));
			} else {
				Logger logger = Logger.getLogger(Chain.class);
				logger.warn("unexpectedly short chain -- was expecting " 
						+ (index + 1) + ", is " + (chain.size()));
			}
		}
		return result;
	}

	public Set<NodeWrapper> getPossibleTypesForIndividual(NodeWrapper instance) {
		return model.getTypesForIndividual(instance);
	}

	public Set<NodeWrapper> getPossibleInstancesForType(ChainLink link) {
		if (!link.hasType()) {
			return Collections.emptySet();
		}
		
		NodeWrapper type = link.getType();
		int index = chain.indexOf(link);
		if (index == 0) {
			return model.getIndividualsForType(type);
		}
		
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (nodeChains == null) {
			nodeChains = model.getChains(chain);
		}
		for (List<NodeWrapper> chain : nodeChains) {
			NodeWrapper chainWrapper = chain.get(index);
			Set<NodeWrapper> wrapperTypes = model.getTypesForIndividual(chainWrapper);
			if (wrapperTypes.contains(type)) {
				result.add(chainWrapper);
			}
		}
		return result;
	}

	public ChainLink getLastLink() {
		if (chain.size() < 1) {
			return null;
		}
		return chain.get(chain.size() - 1);
	}

	private ChainLink getSecondLastLink() {
		if (chain.size() < 2) {
			return null;
		}
		return chain.get(chain.size() - 2);
	}	

	/**
	 * The filter is empty in one of two cases:
	 * <ul>
	 * <li>it has no links at all (which can't really happen, but hey); or</li>
	 * <li>it has exactly one link, and this link is an &quot;any/any&quot; link
	 * (ie {@code !getLastLink().hasInstance() && !getLastLink().hasType()})</li>
	 * </ul>
	 */
	public boolean isEmpty() {
		if (chain.isEmpty()) {
			return true;
		}
		if (chain.size() > 1) {
			return false;
		}
		return !getLastLink().hasInstance() && !getLastLink().hasType();
	}

	private boolean isLastLink(ChainLink link) {
		if (chain.isEmpty()) {
			return false;
		}
		return getLastLink().equals(link);
	}

	private boolean isSecondLastLink(ChainLink link) {
		if (chain.size() < 2) {
			return false;
		}
		return chain.get(chain.size() - 2).equals(link);
	}

	/**
	 * Adds a new chain link to the chain, with the specified type and instance
	 * (both of which can be {@code null}).
	 * 
	 * @param type
	 * @param instance
	 */
	public void add(NodeWrapper type, NodeWrapper instance) {
		ChainLink oldLast = getLastLink();
		ChainLink oldSecondLast = getSecondLastLink();
		int oldSize = chain.size();

		ChainLink chainLink = new ChainLink(type, instance);
		chain.add(chainLink);

		chainLink.addPropertyChangeListener(anyLinkChangeListener);

		int newSize = chain.size();
		if (oldLast != null) {
			oldLast.removePropertyChangeListener(lastLinkListener);
		}
		chainLink.addPropertyChangeListener(lastLinkListener);

		if (oldSecondLast != null) {
			oldSecondLast.removePropertyChangeListener(secondLastLinkListener);
		}
		ChainLink newSecondLast = getSecondLastLink();
		if (newSecondLast != null) {
			newSecondLast.addPropertyChangeListener(secondLastLinkListener);
		}

		changeSupport
				.firePropertyChange(LAST_LINK_PROPERTY, oldLast, chainLink);
		changeSupport.firePropertyChange(SIZE_PROPERTY, oldSize, newSize);
	}

	public void remove(ChainLink link) {
		if (chain.isEmpty() || !chain.contains(link)) {
			return;
		}
		ChainLink oldLast = getLastLink();
		ChainLink oldSecondLast = getSecondLastLink();
		int oldSize = chain.size();

		chain.remove(link);

		link.removePropertyChangeListener(anyLinkChangeListener);

		int newSize = chain.size();
		
		if (newSize == 0) {
			chain.add(new ChainLink(null, null));
		}
		
		ChainLink newLast = getLastLink();
		ChainLink newSecondLast = getSecondLastLink();

		if (oldLast != newLast) {
			if (oldLast != null) {
				oldLast.removePropertyChangeListener(lastLinkListener);
			}
			if (newLast != null) {
				newLast.addPropertyChangeListener(lastLinkListener);
			}
		}

		if (oldSecondLast != newSecondLast) {
			if (oldSecondLast != null) {
				oldSecondLast
						.removePropertyChangeListener(secondLastLinkListener);
			}
			if (newSecondLast != null) {
				newSecondLast.addPropertyChangeListener(secondLastLinkListener);
			}
		}

		changeSupport.firePropertyChange(LAST_LINK_PROPERTY, oldLast, newLast);
		changeSupport.firePropertyChange(SIZE_PROPERTY, oldSize, newSize);
	}

	public void clear() {
		if (chain.isEmpty()) {
			return;
		}

		ChainLink oldLast = getLastLink();
		ChainLink oldSecondLast = getSecondLastLink();
		if (oldLast != null) {
			oldLast.removePropertyChangeListener(lastLinkListener);
		}
		if (oldSecondLast != null) {
			oldSecondLast.removePropertyChangeListener(secondLastLinkListener);
		}
		int oldSize = chain.size();

		for (ChainLink link : chain) {
			link.removePropertyChangeListener(anyLinkChangeListener);
		}

		chain.clear();
		chain.add(new ChainLink(null, null));

		getLastLink().addPropertyChangeListener(anyLinkChangeListener);

		int newSize = chain.size();
		getLastLink().addPropertyChangeListener(lastLinkListener);
		// we know there is no new second last link at this point

		changeSupport.firePropertyChange(LAST_LINK_PROPERTY, oldLast,
				getLastLink());
		changeSupport.firePropertyChange(SIZE_PROPERTY, oldSize, newSize);
	}

	public boolean canRemove(ChainLink link) {
		return canClear() && isLastLink(link);
	}

	public boolean canChange(ChainLink link) {
		return isLastLink(link)
				|| (isSecondLastLink(link) && !getLastLink().hasInstance() && !getLastLink()
						.hasType());
	}

	public boolean canClear() {
		if (chain.isEmpty()) {
			return false;
		}
		return chain.size() > 1 || (getLastLink().hasInstance() || getLastLink().hasType());
	}

	public boolean canAddAnyAny() {
		return chain.size() > 1 || (getLastLink().hasInstance() || getLastLink().hasType());
	}
	
	public List<ChainLink> getLinks() {
		return Collections.unmodifiableList(chain);
	}

	public synchronized List<ChainLink> getLinksSnapshot() {
		List<ChainLink> result = new ArrayList<ChainLink>(chain.size());
		for (ChainLink chainLink : chain) {
			try {
				result.add((ChainLink) chainLink.clone());
			} catch (CloneNotSupportedException e) {
				// we know that ChainLink is Cloneable, so we should never get here
				Logger logger = Logger.getLogger(Chain.class);
				logger.warn("got a clone not supported exception even though we know that ChainLink is Cloneable", e);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return String.format("Chain: [%s]", chain);
	}
	
	/* listener support */
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		changeSupport.addPropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
		changeSupport.addPropertyChangeListener(propertyName, l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		changeSupport.removePropertyChangeListener(l);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
		changeSupport.removePropertyChangeListener(propertyName, l);
	}

}
