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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import net.schweerelos.parrot.model.filters.ChainLink;
import net.schweerelos.parrot.util.QuadTree;
import net.schweerelos.timeline.model.IntervalChain;

import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletInfGraph;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

class ParrotModelHelper extends Object implements ParrotModel {

	private static final String PRIMARY_TYPE_LITERAL = "primary";
	private static final String URI_SHOW_THIS_TYPE = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/DigitalParrot/2009/02/DigitalParrot.owl#showThisType";
	private static final String SECONDARY_TYPE_LITERAL = "secondary";

	private OntModel model;

	private Map<OntResource, NodeWrapper> nodes;
	private Map<OntResource, Set<NodeWrapper>> edges;
	private Map<OntResource, NodeWrapper> types;

	private static Set<Property> knownNonEdgeProperties = new HashSet<Property>();
	private static Set<Property> knownEdgeProperties = new HashSet<Property>();
	private static Map<OntResource, Boolean> secondaryTypes = new HashMap<OntResource, Boolean>();
	private static Map<OntResource, Boolean> primaryTypes = new HashMap<OntResource, Boolean>();

	private EventListenerList modelListeners;

	private Filter highlightFilter;
	private Set<Filter> restrictFilters;
	
	private TextSearchEngine textSearchSupport;
	private boolean busy = false;
	private List<NodeWrapper> currentlyRestricted;
	private ParrotModel realModel;
	private IntervalChain<NodeWrapper> timedThings;
	private net.schweerelos.parrot.util.QuadTree<CenteredThing<NodeWrapper>> locatedThings;
	private String datafile;
	
	public ParrotModelHelper(OntModel model, ParrotModel realModel) {
		this.model = model;
		this.realModel = realModel;

		nodes = new HashMap<OntResource, NodeWrapper>();
		edges = new HashMap<OntResource, Set<NodeWrapper>>();
		types = new HashMap<OntResource, NodeWrapper>();

		modelListeners = new EventListenerList();

		restrictFilters = new HashSet<Filter>();
		textSearchSupport = new TextSearchEngine();
		currentlyRestricted = new ArrayList<NodeWrapper>();
	}
	
	@Override
	public Set<NodeWrapper> getAllSubjects() {
		Set<NodeWrapper> subjects = new HashSet<NodeWrapper>();
		for (ResIterator iterator = model.listSubjects(); iterator.hasNext();) {
			Resource subRes = (Resource) iterator.next();
			if (!subRes.canAs(OntResource.class)) {
				continue;
			}
			OntResource sub = model.getOntResource(subRes);
			if (nodes.containsKey(sub)) {
				subjects.add(nodes.get(sub));
			}
		}
		return subjects;
	}

	@Override
	public Set<NodeWrapper> getAllPredicates() {
		Set<NodeWrapper> predicates = new HashSet<NodeWrapper>();
		for (ExtendedIterator<ObjectProperty> objProps = model.listObjectProperties(); objProps
		.hasNext();) {
			ObjectProperty nextProp = objProps.next();
			if (!(nextProp instanceof OntProperty)) {
				continue;
			}
			OntProperty prop = (OntProperty) nextProp;
			if (showTypeAsPrimary(model, prop)) {
				predicates.add(new NodeWrapper(prop));
			}
		}
		for (ExtendedIterator<DatatypeProperty> dataProps = model.listDatatypeProperties(); dataProps
		.hasNext();) {
			DatatypeProperty nextProp = dataProps.next();
			if (nextProp instanceof OntProperty) {
				continue;
			}
			OntProperty prop = (OntProperty) nextProp;
			if (showTypeAsPrimary(model, prop)) {
				predicates.add(new NodeWrapper(prop));
			}
		}
		return predicates;
	}

	@Override
	public NodeWrapper getNodeWrapper(Individual instance) {
		if (nodes.containsKey(instance)) {
			return nodes.get(instance);
		} else {
			Logger logger = Logger.getLogger(ParrotModelHelper.class);
			logger.warn("don't have a wrapper for " + instance);
			// TODO does this cause problems?
			return null;
		}
	}

	@Override
	public Set<NodeWrapper> getNodeWrappers(OntClass ontClass) {
		if (edges.containsKey(ontClass)) {
			return edges.get(ontClass);
		} else {
			Logger logger = Logger.getLogger(ParrotModelHelper.class);
			logger.warn("don't have wrappers for " + ontClass);
			// TODO does this cause problems?
			return null;
		}
	}

	@Override
	public OntModel getOntModel() {
		return model;
	}

	@Override
	public Set<NodeWrapper> getPredicatesForSubject(NodeWrapper subject) {
		Set<NodeWrapper> predicates = new HashSet<NodeWrapper>();
		OntResource subjectNode = subject.getOntResource();
		if (!subjectNode.isClass()) {
			// something is broken if we ever get here
			return predicates;
		}
		OntClass subjectClass = subjectNode.asClass();
		for (ExtendedIterator<OntProperty> iterator = subjectClass
				.listDeclaredProperties(true); iterator.hasNext();) {
			OntProperty prop = iterator.next();
			NodeWrapper nodeWrapper = new NodeWrapper(prop);
			predicates.add(nodeWrapper);
		}
		return predicates;
	}

	@Override
	public Set<NodeWrapper> getSubjectTypes() {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		for (ExtendedIterator<OntClass> namedClasses = model.listNamedClasses(); namedClasses
		.hasNext();) {
			OntClass ontClass = namedClasses.next();
			if (showTypeAsPrimary(model, ontClass)) {
				NodeWrapper classWrapper;
				if (types.containsKey(ontClass)) {
					classWrapper = types.get(ontClass);
				} else {
					classWrapper = new NodeWrapper(ontClass);
					types.put(ontClass, classWrapper);
				}
				result.add(classWrapper);
			}
		}
		return result;
	}

	@Override
	public IntervalChain<NodeWrapper> getTimedThings() {
		if (timedThings == null) {
			timedThings = TimedThingsHelper.extractTimedThings(realModel);
		}
		return timedThings;
	}

	@Override
	public QuadTree<CenteredThing<NodeWrapper>> getLocatedThings() {
		if (locatedThings == null) {
			locatedThings = LocatedThingsHelper.extractLocatedThings(realModel);
		}
		return locatedThings;
	}

	@Override
	public void loadData(String datafile) {
		this.datafile = datafile;
		
		model.read(datafile);
		model.rebind();
		
		Graph graph = model.getGraph();
		if (graph instanceof PelletInfGraph) {
			PelletInfGraph pellet = (PelletInfGraph) graph;
			pellet.classify();
			pellet.realize();
		}
	}
	
	@Override
	public String getDataIdentifier() {
		return datafile;
	}

	@Override
	public void saveData() {
		// TODO #13 Auto-generated method stub
	}


	@Override
	public synchronized void addFilter(Filter filter) {
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		if (filter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			Object oldHighlight = highlightFilter;
			if (oldHighlight != filter) {
				highlightFilter = filter;
				updateHighlights();
				fireHighlightsChanged();
			}
		} else if (filter.getMode() == Filter.Mode.RESTRICT) {
			boolean added = restrictFilters.add(filter);
			if (added) {
				currentlyRestricted.addAll(filter.getMatching(realModel));
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	@Override
	public void replaceFilter(Filter oldFilter, Filter newFilter) {
		if (oldFilter == null && newFilter == null) {
			// do nothing
			return;
		}
		if (oldFilter == null) {
			addFilter(newFilter);
			return;
		} else if (newFilter == null) {
			removeFilter(oldFilter);
			return;
		}
		if (oldFilter.getMode() != newFilter.getMode()) {
			Logger logger = Logger.getLogger(ParrotModelHelper.class);
			logger.warn("trying to replace a filter with one with a different mode, aborting");
			// do nothing
			return;
		}
		// ok, now we know we are genuinely replacing filters
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		// TODO #21 other filter types
		if (newFilter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			if (highlightFilter != oldFilter) {
				Logger logger = Logger.getLogger(ParrotModelHelper.class);
				logger.warn("trying to replace filters, but old filter doesn't exist. Updating the filter anyway.");
			}
			highlightFilter = newFilter;
			updateHighlights();
			fireHighlightsChanged();
		} else if (newFilter.getMode() == Filter.Mode.RESTRICT) {
			boolean removed = restrictFilters.remove(oldFilter);
			boolean added = restrictFilters.add(newFilter);
			if (removed || added) {
				currentlyRestricted.clear();
				for (Filter theFilter : restrictFilters) {
					currentlyRestricted.addAll(theFilter.getMatching(realModel));
				}
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	@Override
	public synchronized void removeFilter(Filter filter) {
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		// TODO #21 other filter types
		if (filter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			highlightFilter = null;
			// update highlight information in nodewrappers
			updateHighlights();
			fireHighlightsChanged();
		} else if (filter.getMode() == Filter.Mode.RESTRICT) {
			boolean removed = restrictFilters.remove(filter);
			if (removed) {
				currentlyRestricted.clear();
				for (Filter theFilter : restrictFilters) {
					currentlyRestricted.addAll(theFilter.getMatching(realModel));
				}
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	private void updateHighlights() {
		// first reset all highlights
		List<NodeWrapper> allNodeWrappers = new ArrayList<NodeWrapper>();
		allNodeWrappers.addAll(nodes.values());
		for (OntResource key : edges.keySet()) {
			allNodeWrappers.addAll(edges.get(key));
		}
		for (NodeWrapper nodeWrapper : allNodeWrappers) {
			nodeWrapper.setHighlighted(false);
		}
		// now highlight those that should be, if we have a highlight filter
		if (highlightFilter == null) {
			return;
		}
		Set<NodeWrapper> matching = highlightFilter.getMatching(realModel);
		for (NodeWrapper nodeWrapper : matching) {
			nodeWrapper.setHighlighted(true);
		}
	}

	public static boolean isPotentialNode(OntModel model, Individual individual) {
		if (individual.canAs(OntClass.class)) {
			// don't include classes
			return false;
		}
		ExtendedIterator<OntClass> classes = individual.listOntClasses(false);
		while (classes.hasNext()) {
			OntClass ontClass = classes.next();
			if (ontClass.isResource() && showTypeAsPrimary(model, ontClass)) {
				// there is at least one vote in favour of showing this node
				return true;
			}
		}
		// if we get here, then there was no vote in favour of showing this node;
		return false;
	}

	public static boolean isPotentialEdge(OntModel model, Property predicate) {
		if (knownNonEdgeProperties.contains(predicate)) {
			return false;
		}
		if (knownEdgeProperties.contains(predicate)) {
			return true;
		}
		if (!predicate.isURIResource()) {
			knownNonEdgeProperties.add(predicate);
			return false; // TODO is that a good idea?
		}

		OntProperty prop = model.getOntProperty(predicate.getURI());
		if (prop == null) {
			knownNonEdgeProperties.add(predicate);
			return false;
		}
		if (showTypeAsPrimary(model, prop)) {
			knownEdgeProperties.add(predicate);
			return true;
		}
		// if the property itself isn't tagged as to be shown, look at its super-properties
		ExtendedIterator<? extends OntProperty> superProps = prop.listSuperProperties(false);
		while (superProps.hasNext()) {
			OntProperty superProp = superProps.next();
			if (knownEdgeProperties.contains(superProp)) {
				return true;
			}
			if (showTypeAsPrimary(model, superProp)) {
				// there is at least one vote in favour of showing this node
				knownEdgeProperties.add(superProp);
				return true;
			}
		}
		// if we get here, then there was no vote in favour of showing this node
		knownNonEdgeProperties.add(predicate);
		return false;
	}

	public static boolean isPotentialNode(ParrotModel pModel, Individual individual) {
		return isPotentialNode(pModel.getOntModel(), individual);
	}

	public static boolean isPotentialEdge(ParrotModel pModel, Property predicate) {
		return isPotentialEdge(pModel.getOntModel(), predicate);
	}

	public static boolean showTypeAsPrimary(OntModel model, OntResource ontRes) {
		if (primaryTypes.containsKey(ontRes)) {
			return primaryTypes.get(ontRes);
		}
		boolean show = showTypeAs(model, ontRes, PRIMARY_TYPE_LITERAL);
		primaryTypes.put(ontRes, show);
		return show;
	}

	public static boolean showTypeAsSecondary(OntModel model, OntResource ontRes) {
		if (secondaryTypes.containsKey(ontRes)) {
			return secondaryTypes.get(ontRes);
		}
		boolean show = showTypeAs(model, ontRes, SECONDARY_TYPE_LITERAL);
		secondaryTypes.put(ontRes, show);
		return show;
	}

	private static boolean showTypeAs(OntModel model, OntResource ontRes,
			String showTypeValue) {
		Property prop = model.getProperty(URI_SHOW_THIS_TYPE);
		if (prop == null) {
			return false;
		}
		RDFNode value = ontRes.getPropertyValue(prop);
		if (value != null && value.isLiteral()) {
			String realValue = value.as(Literal.class).getLexicalForm();
			return realValue.equals(showTypeValue);
		}
		return false;
	}



	public NodeWrapper addSubject(Individual individual) {
		NodeWrapper subject;
		if (nodes.containsKey(individual)) {
			subject = nodes.get(individual);
		} else {
			subject = new NodeWrapper(individual);
			nodes.put(individual, subject);
			textSearchSupport.add(subject);
		}
		return subject;
	}

	public NodeWrapper addPredicate(OntResource predicateResource) {
		if (!edges.containsKey(predicateResource)) {
			edges.put(predicateResource, new HashSet<NodeWrapper>());
		}
		Set<NodeWrapper> predicateList = edges.get(predicateResource);
		NodeWrapper predicate = new NodeWrapper(predicateResource);
		predicateList.add(predicate);
		textSearchSupport.add(predicate);
		return predicate;
	}

	public NodeWrapper addObject(RDFNode objectNode) {
		NodeWrapper object = null;
		if (objectNode.isLiteral()) {
			// TODO make this list like for predicates?
			object = new NodeWrapper(objectNode.as(Literal.class));
			textSearchSupport.add(object);
		} else if (objectNode.isURIResource()) {
			OntResource objectResource = model.getOntResource(objectNode.as(Resource.class));
			if (nodes.containsKey(objectResource)) {
				object = nodes.get(objectResource);
			} else {
				object = new NodeWrapper(objectResource);
				nodes.put(objectResource, object);
				textSearchSupport.add(object);
			}
		}
		// TODO other cases
		return object;
	}

	@Override
	public void deleteEdge(NodeWrapper edge) {
		// TODO #13 implement (editing/delete edge)
	}

	@Override
	public void deleteNode(NodeWrapper vertex) {
		// TODO #13 implement (editing/delete node)
	}
	@Override
	public synchronized void addParrotModelListener(ParrotModelListener pml) {
		modelListeners.add(ParrotModelListener.class, pml);
	}

	@Override
	public synchronized void removeParrotModelListener(ParrotModelListener pml) {
		modelListeners.remove(ParrotModelListener.class, pml);
	}
	
	private void fireBusyChanged(final boolean nowBusy) {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners
				.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
					if (nowBusy) {
						listeners[i].modelBusy();
					} else {
						listeners[i].modelIdle();
					}
				}
			}
		});
	}

	private void fireHighlightsChanged() {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners
				.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
						listeners[i].highlightsChanged();
				}
			}
		});
	}

	private void fireRestrictionsChanged(final Collection<NodeWrapper> newRestricted) {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners
				.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
						listeners[i].restrictionsChanged(newRestricted);
				}
			}
		});
	}
	
	@Override
	public Set<NodeWrapper> getTypesForIndividual(NodeWrapper node) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (node.isOntResource() && node.getOntResource().isIndividual()) {
			Individual ind = node.getOntResource().asIndividual();
			ExtendedIterator<OntClass> classes = ind.listOntClasses(false);
			while (classes.hasNext()) {
				OntClass ontClass = classes.next();
				if (showTypeAsPrimary(model, ontClass) || showTypeAsSecondary(model, ontClass)) {
					NodeWrapper classWrapper;
					if (types.containsKey(ontClass)) {
						classWrapper = types.get(ontClass);
					} else {
						classWrapper = new NodeWrapper(ontClass);
						types.put(ontClass, classWrapper);
					}
					result.add(classWrapper);
				}
			}
		}
		return result;
	}
	
	@Override
	public Set<NodeWrapper> getIndividualsForType(NodeWrapper type) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (type.isType() && type.getOntResource().isClass()) {
			OntClass ontClass = type.getOntResource().asClass();
			ExtendedIterator<? extends OntResource> individuals = ontClass.listInstances(false);
			while (individuals.hasNext()) {
				OntResource individual = individuals.next();
				if (individual.isIndividual()) {
					result.add(getNodeWrapper(individual.asIndividual()));
				}
			}
		}
		return result;
	}

	@Override
	public Set<NodeWrapper> getSuperPredicates(NodeWrapper node) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (node.isOntResource() && node.getOntResource().isProperty()) {
			OntProperty prop = node.getOntResource().asProperty();
			ExtendedIterator<? extends OntProperty> superProps = prop.listSuperProperties(false);
			while (superProps.hasNext()) {
				OntProperty superProp = superProps.next();
				if (showTypeAsPrimary(model, superProp) || showTypeAsSecondary(model, superProp)) {
					result.add(new NodeWrapper(superProp));
				}
			}
		}
		return result;
	}

	@Override
	public Set<NodeWrapper> getAllNodes() {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>(nodes.values().size());
		result.addAll(nodes.values());
		return result;
	}
	
	@Override
	public Set<NodeWrapper> getAllNodeWrappers() {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		result.addAll(nodes.values());
		for (OntResource edgeResource : edges.keySet()) {
			result.addAll(edges.get(edgeResource));
		}
		return result;
	}
	
	@Override
	public Set<NodeWrapper> searchNodeWrappers(String query) throws SearchFailedException {
		return textSearchSupport.search(query);
	}

	@Override
	public boolean isBusy() {
		return busy;
	}

	/** 
	 * Should be overridden. 
	 */
	@Override
	public boolean hasSuccessor(NodeWrapper first, NodeWrapper second) {
		return false;
	}
	
	/** 
	 * Should be overridden. 
	 */
	@Override
	public Collection<NodeWrapper> getSuccessorNodes(NodeWrapper node) {
		return Collections.emptyList();
	}
	
	/** 
	 * Should be overridden.
	 */
	@Override
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to) {
		return Collections.emptyList();
	}

	@Override
	public NodeWrapper getNodeWrapperForString(String url) throws NoSuchNodeWrapperException {
		Individual individual = model.getIndividual(url);
		if (individual == null) {
			throw new NoSuchNodeWrapperException("no nodewrapper found for string " + url);
		}
		return getNodeWrapper(individual);
	}

	@Override
	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();

		if (chain.isEmpty()) {
			// chain is empty, done
			return result;
		}
		
		if (chain.size() == 1) {
			ChainLink firstInChain = chain.get(0);
			if (!firstInChain.hasType() && !firstInChain.hasInstance()) {
				result.addAll(nodes.values());
				for (Collection<NodeWrapper> edgeSet : edges.values()) {
					result.addAll(edgeSet);
				}
				return result;
			}
		}
		
		List<List<NodeWrapper>> nodeChains = getChains(chain);

		// now that potentialNodeChains only has NodeWrappers remaining that
		// actually match the chain, we can go and add them all to the result
		// set
		// but we need to make sure we add the edges along the chains too
		for (List<NodeWrapper> nodeChain : nodeChains) {
			Set<NodeWrapper> chainWithEdges = makeNodeChainWithEdges(nodeChain);
			result.addAll(chainWithEdges);
		}
		return result;
	}

	@Override
	public List<List<NodeWrapper>> getChains(List<ChainLink> chain) {
		// create data structure to hold all potentially matching NodeWrappers
		// it's a list of lists of matching NodeWrappers (each "inner" list
		// mirroring the chain up to the current iteration step)
		List<List<NodeWrapper>> potentialNodeChains = new ArrayList<List<NodeWrapper>>();

		// first step: initialise the inner lists (one for each starting point)
		ChainLink firstLink = chain.get(0);
		if (firstLink.hasInstance()) {
			// there is only one starting point
			List<NodeWrapper> potentialNodeChain = new ArrayList<NodeWrapper>(
					chain.size());
			potentialNodeChain.add(firstLink.getInstance());
			potentialNodeChains.add(potentialNodeChain);
		} else if (firstLink.hasType()) {
			// every instance of firstLink's type is a starting point
			Set<NodeWrapper> instances = getIndividualsForType(firstLink
					.getType());
			for (NodeWrapper instance : instances) {
				List<NodeWrapper> potentialNodeChain = new ArrayList<NodeWrapper>(
						chain.size());
				potentialNodeChain.add(instance);
				potentialNodeChains.add(potentialNodeChain);
			}
		} else {
			// firstLink is any/any type 
			// this means *all* subjects match
			List<NodeWrapper> allSubjects = new ArrayList<NodeWrapper>(nodes.values());
			potentialNodeChains.add(allSubjects);
			return potentialNodeChains;
		}

		// now that we're done initialising potentialNodeChains, go and iterate
		// along the chain (starting at the *second* item since we've already
		// looked at the first one)
		for (int i = 1; i < chain.size(); i++) {
			ChainLink link = chain.get(i);
			
			List<List<NodeWrapper>> keepChains = new ArrayList<List<NodeWrapper>>();
			List<List<NodeWrapper>> newChains = new ArrayList<List<NodeWrapper>>();
			
			if (link.hasInstance()) {
				NodeWrapper instance = link.getInstance();
				// keep all potentialNodeChains whose end is one step before instance
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					// check whether we can get from endOfChain to instance
					if (realModel.hasSuccessor(endOfChain, instance)) {
						// yup -> keep this potentialNodeChain
						potentialNodeChain.add(instance);
						keepChains.add(potentialNodeChain);
					}
				}
			} else if (link.hasType()) {
				Set<NodeWrapper> instances = getIndividualsForType(link.getType());
				// keep all potentialNodeChains whose end is one step before *one of* the instances
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					
					// endOfChain could have *several* of the instances as successors
					// in that case we need to branch out
					boolean alreadyFoundOne = false;
					for (NodeWrapper instance : instances) {
						if (realModel.hasSuccessor(endOfChain, instance)) {
							if (!alreadyFoundOne) {
								// the easy case: this is the first time we're finding a match
								potentialNodeChain.add(instance);
								keepChains.add(potentialNodeChain);
								alreadyFoundOne = true;
							} else {
								// the tricky case: we have already found a different match before
								// we need to copy the whole potentialNodeChain
								List<NodeWrapper> newPotentialNodeChain = new ArrayList<NodeWrapper>(chain.size());
								// need to add instance separately because it's a different one
								newPotentialNodeChain.addAll(potentialNodeChain.subList(0, potentialNodeChain.size() - 1));
								newPotentialNodeChain.add(instance);
								// and add the copy to newChains
								newChains.add(newPotentialNodeChain);
							}
						}
					}
				}
			} else {
				// link is any/any type 
				// -> keep all potentialNodeChains whose end has at least one outgoing edge
				// while doing the same branchy stuff as in the 'else if' case above
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					Collection<NodeWrapper> successors = realModel.getSuccessorNodes(endOfChain);
					for (NodeWrapper successor : successors) {
						List<NodeWrapper> newPotentialNodeChain = new ArrayList<NodeWrapper>();
						newPotentialNodeChain.addAll(potentialNodeChain);
						newPotentialNodeChain.add(successor);
						newChains.add(newPotentialNodeChain);
					}
				}
			}

			// only keep those potentialNodeChains that 'survived' this iteration
			potentialNodeChains.retainAll(keepChains);
			// and add the new ones
			potentialNodeChains.addAll(newChains);
		}
		return potentialNodeChains;
	}

	private Set<NodeWrapper> makeNodeChainWithEdges(List<NodeWrapper> chain) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (chain.isEmpty()) {
			return result;
		}
		NodeWrapper lastNode = null;
		for (NodeWrapper node : chain) {
			if (lastNode == null) {
				// looking at first in chain
				lastNode = chain.get(0);
			} else {
				result.addAll(realModel.getEdges(lastNode, node));
				lastNode = node;
			}
			result.add(lastNode);
		}
		return result;
	}

	@Override
	public GraphParrotModel asGraphModel() {
		throw new UnsupportedOperationException("can't make a graph model out of this");
	}

	@Override
	public TableParrotModel asListModel() {
		throw new UnsupportedOperationException("can't make a list model out of this");
	}
	
}
