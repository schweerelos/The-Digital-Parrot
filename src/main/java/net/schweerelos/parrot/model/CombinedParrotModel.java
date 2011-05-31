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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.schweerelos.parrot.model.filters.ChainLink;
import net.schweerelos.parrot.util.QuadTree;
import net.schweerelos.timeline.model.IntervalChain;

import org.mindswap.pellet.jena.ModelExtractor;
import org.mindswap.pellet.jena.ModelExtractor.StatementType;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class CombinedParrotModel implements ParrotModel {
	private GraphParrotModel graphModel;
	private TableParrotModel listModel;
	
	private ParrotModelHelper delegateModel;
	private OntModel ontModel;
	
	public CombinedParrotModel(OntModel model) {
		this.ontModel = model;
		
		graphModel = new GraphParrotModel(model);
		listModel = new TableParrotModel(model);
	}

	public void addFilter(Filter filter) {
		delegateModel.addFilter(filter);
	}

	public String toString() {
		return "combined list and graph model";
	}

	@Override
	public void addParrotModelListener(ParrotModelListener pml) {
		delegateModel.addParrotModelListener(pml);
	}

	@Override
	public void deleteEdge(NodeWrapper edge) {
		delegateModel.deleteEdge(edge);
	}

	@Override
	public void deleteNode(NodeWrapper vertex) {
		delegateModel.deleteNode(vertex);
	}

	@Override
	public Set<NodeWrapper> getAllNodeWrappers() {
		return delegateModel.getAllNodeWrappers();
	}

	@Override
	public Set<NodeWrapper> getAllNodes() {
		return delegateModel.getAllNodes();
	}

	@Override
	public Set<NodeWrapper> getAllPredicates() {
		return delegateModel.getAllPredicates();
	}

	@Override
	public Set<NodeWrapper> getAllSubjects() {
		return delegateModel.getAllSubjects();
	}

	@Override
	public List<List<NodeWrapper>> getChains(List<ChainLink> links) {
		return delegateModel.getChains(links);
	}

	@Override
	public String getDataIdentifier() {
		return delegateModel.getDataIdentifier();
	}

	@Override
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to) {
		// arbitrarily using graph edges
		Collection<NodeWrapper> graphEdges = graphModel.getEdges(from, to);
		return graphEdges;
	}

	@Override
	public Set<NodeWrapper> getIndividualsForType(NodeWrapper type) {
		return delegateModel.getIndividualsForType(type);
	}

	@Override
	public QuadTree<CenteredThing<NodeWrapper>> getLocatedThings() {
		return delegateModel.getLocatedThings();
	}

	@Override
	public NodeWrapper getNodeWrapper(Individual instance) {
		return delegateModel.getNodeWrapper(instance);
	}

	@Override
	public NodeWrapper getNodeWrapperForString(String url)
			throws NoSuchNodeWrapperException {
		return delegateModel.getNodeWrapperForString(url);
	}

	@Override
	public Set<NodeWrapper> getNodeWrappers(OntClass ontClass) {
		return delegateModel.getNodeWrappers(ontClass);
	}

	@Override
	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain) {
		return delegateModel.getNodeWrappersOnChain(chain);
	}

	@Override
	public OntModel getOntModel() {
		return delegateModel.getOntModel();
	}

	@Override
	public Set<NodeWrapper> getPredicatesForSubject(NodeWrapper subject) {
		return delegateModel.getPredicatesForSubject(subject);
	}

	@Override
	public Set<NodeWrapper> getSubjectTypes() {
		return delegateModel.getSubjectTypes();
	}

	@Override
	public Collection<NodeWrapper> getSuccessorNodes(NodeWrapper node) {
		// arbitrarily using graph successors
		Collection<NodeWrapper> graphSuccessors = graphModel.getSuccessorNodes(node);
		return graphSuccessors;
	}

	@Override
	public Set<NodeWrapper> getSuperPredicates(NodeWrapper node) {
		return delegateModel.getSuperPredicates(node);
	}

	@Override
	public IntervalChain<NodeWrapper> getTimedThings() {
		return delegateModel.getTimedThings();
	}

	@Override
	public Set<NodeWrapper> getTypesForIndividual(NodeWrapper node) {
		return delegateModel.getTypesForIndividual(node);
	}

	@Override
	public boolean hasSuccessor(NodeWrapper node, NodeWrapper maybeSuccessor) {
		// arbitrarily using graph answer
		boolean graphAnswer = graphModel.hasSuccessor(node, maybeSuccessor);
		return graphAnswer;
	}

	@Override
	public boolean isBusy() {
		return delegateModel.isBusy();
	}

	@Override
	public void loadData(String datafile) {
		delegateModel = new ParrotModelHelper(ontModel, this);
		delegateModel.loadData(datafile);
		
		graphModel.setDelegate(delegateModel);
		listModel.setDelegate(delegateModel);
		
		ModelExtractor extractor = new ModelExtractor(ontModel);
		EnumSet<StatementType> selectors = StatementType.PROPERTY_VALUE;
		extractor.setSelector(selectors);
		Model eModel = extractor.extractModel();

		StmtIterator statements = eModel.listStatements();
		while (statements.hasNext()) {
			Statement stat = statements.nextStatement();

			Resource subject = stat.getSubject();
			if (subject.isURIResource()) {
				subject = ontModel.getResource(subject.getURI());
			}
			if (!(subject.canAs(Individual.class)) 
					|| !ParrotModelHelper.isPotentialNode(delegateModel, subject.as(Individual.class))) {
				continue;
			}
			
			Property predicate = stat.getPredicate();
			if (predicate.isURIResource()) {
				predicate = ontModel.getProperty(predicate.getURI());
			}
			if (!ParrotModelHelper.isPotentialEdge(delegateModel, predicate)) {
				continue;
			}

			NodeWrapper subjectWrapper = delegateModel.addSubject(subject.as(Individual.class));

			OntResource predicateResource = ontModel.getOntResource(predicate);
			NodeWrapper predicateWrapper = delegateModel.addPredicate(predicateResource);

			RDFNode object = stat.getObject();
			if (object.isURIResource()) {
				object = ontModel.getResource(object.as(Resource.class).getURI());
			}
			NodeWrapper objectWrapper = delegateModel.addObject(object);
			
			graphModel.addTriplet(subjectWrapper, predicateWrapper, objectWrapper);
			listModel.addTriplet(subjectWrapper, predicateWrapper, objectWrapper);
		}
	}

	@Override
	public void removeFilter(Filter filter) {
		delegateModel.removeFilter(filter);
	}

	@Override
	public void removeParrotModelListener(ParrotModelListener pml) {
		delegateModel.removeParrotModelListener(pml);
	}

	@Override
	public void replaceFilter(Filter oldFilter, Filter newFilter) {
		delegateModel.replaceFilter(oldFilter, newFilter);
	}

	@Override
	public void saveData() {
		delegateModel.saveData();
	}

	@Override
	public Set<NodeWrapper> searchNodeWrappers(String query)
			throws SearchFailedException {
		return delegateModel.searchNodeWrappers(query);
	}

	@Override
	public GraphParrotModel asGraphModel() {
		return graphModel;
	}

	@Override
	public TableParrotModel asListModel() {
		return listModel;
	}
}
