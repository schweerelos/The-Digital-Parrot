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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.model.filters.ChainLink;
import net.schweerelos.parrot.util.QuadTree;
import net.schweerelos.timeline.model.IntervalChain;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;

public interface ParrotModel {

	/**
	 * Initialises the model from the datafile.
	 * @param datafile file containing RDF data in a format readable by Jena.
	 */
	public void loadData(String datafile);
	
	/**
	 */
	public void saveData();

	/**
	 * Gets the OntModel. Should probably be outlawed because it's too tight coupling.
	 * @return the underlying OntModel.
	 */
	public OntModel getOntModel();

	public Set<NodeWrapper> getSubjectTypes();

	/**
	 * Returns a list of all showable predicates that have the supplied NodeWrapper 
	 * as their subject.
	 * @param subject the subject for which to get all predicates. 
	 * Must not be <tt>null</tt>.
	 * @return all showable predicates for the supplied subject,
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getPredicatesForSubject(NodeWrapper subject);
	
	/**
	 * Returns a list of all showable predicates in the model.
	 * @return all showable predicates in the model, 
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getAllPredicates();
	
	/**
	 * Returns a list of all showable subjects in the model.
	 * @return all showable subjects in the model,
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getAllSubjects();
	
	/**
	 * Returns a list of all showable types for the individual.
	 * @param node the individual for which to get the types. Must be an individual, 
	 * ie <tt>isOntResource() == true</tt> and <tt>getOntResource().isIndividual() == true</tt>.
	 * Must not be null.  
	 * @return all showable types for the individual,
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getTypesForIndividual(NodeWrapper node);
	/**
	 * Returns a list of all showable individuals for the type.
	 * @param node the type for which to get the individuals. Must be a type, 
	 * ie <tt>isType() == true</tt> and <tt>getOntResource().isClass() == true</tt>.
	 * Must not be null.  
	 * @return all showable individuals for the type,
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getIndividualsForType(NodeWrapper type);
	

	/**
	 * Returns a list of all showable super properties of the property.
	 * @param node the property whose super properties are requested. Must be a property,
	 * ie <tt>isOntResource() == true</tt> and <tt>getOntResource().isProperty() == true</tt>.
	 * Must not be null.
	 * @return all showable super properties for the property,
	 * or an empty list if there aren't any.
	 */
	public Set<NodeWrapper> getSuperPredicates(NodeWrapper node);
	
	public void addFilter(Filter filter);
	public void removeFilter(Filter filter);
	public void replaceFilter(Filter oldFilter, Filter newFilter);

	
	public NodeWrapper getNodeWrapper(Individual instance);
	public Set<NodeWrapper> getNodeWrappers(OntClass ontClass);
	public Set<NodeWrapper> getAllNodeWrappers();
	public Set<NodeWrapper> getAllNodes();
	
	public IntervalChain<NodeWrapper> getTimedThings();
	public QuadTree<CenteredThing<NodeWrapper>> getLocatedThings();

	
	/**
	 * Adds <tt>pml</tt> to the list of listeners. 
	 * @param pml the new <tt>ParrotModelListener</tt>.
	 */
	public void addParrotModelListener(ParrotModelListener pml);
	
	/**
	 * Removes <tt>pml</tt> from the list of listeners.
	 * @param pml the <tt>ParrotModelListener</tt> to remove.
	 */
	public void removeParrotModelListener(ParrotModelListener pml);

	
	public void deleteEdge(NodeWrapper edge);
	public void deleteNode(NodeWrapper vertex);

	public Set<NodeWrapper> searchNodeWrappers(String query) throws SearchFailedException;
	
	public boolean isBusy();

	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain);
	public List<List<NodeWrapper>> getChains(List<ChainLink> links);

	public boolean hasSuccessor(NodeWrapper node, NodeWrapper maybeSuccessor);
	public Collection<NodeWrapper> getSuccessorNodes(NodeWrapper node);
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to);

	public NodeWrapper getNodeWrapperForString(String url) throws NoSuchNodeWrapperException;

	public String getDataIdentifier();

	public GraphParrotModel asGraphModel();
	public TableParrotModel asListModel();

}
