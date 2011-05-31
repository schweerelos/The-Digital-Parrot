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

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class GraphParrotModel implements Graph<NodeWrapper, NodeWrapper>,
		ParrotModel {

	private DirectedSparseMultigraph<NodeWrapper, NodeWrapper> delegateGraph;
	private ParrotModelHelper delegateModel;
	private OntModel ontModel;

	public GraphParrotModel(OntModel model) {
		ontModel = model;
		delegateGraph = new DirectedSparseMultigraph<NodeWrapper, NodeWrapper>();
	}

	public Graph<NodeWrapper, NodeWrapper> asGraph() {
		return this;
	}

	/* ParrotModel methods */

	@Override
	public OntModel getOntModel() {
		return delegateModel.getOntModel();
	}

	@Override
	public void loadData(String datafile) {
		if (delegateModel == null) {
			delegateModel = new ParrotModelHelper(ontModel, this);
			delegateModel.loadData(datafile);
		}
		populateGraphModel(delegateModel.getOntModel());
	}
	
	private void populateGraphModel(OntModel model) {
		ModelExtractor extractor = new ModelExtractor(model);
		EnumSet<StatementType> selectors = StatementType.PROPERTY_VALUE;
		extractor.setSelector(selectors);
		Model eModel = extractor.extractModel();

		StmtIterator statements = eModel.listStatements();
		while (statements.hasNext()) {
			Statement stat = statements.nextStatement();

			Resource subject = stat.getSubject();
			if (subject.isURIResource()) {
				subject = model.getResource(subject.getURI());
			}
			if (!(subject.canAs(Individual.class))
					|| !ParrotModelHelper.isPotentialNode(delegateModel,
							subject.as(Individual.class))) {
				continue;
			}

			Property predicate = stat.getPredicate();
			if (predicate.isURIResource()) {
				predicate = model.getProperty(predicate.getURI());
			}
			if (!ParrotModelHelper.isPotentialEdge(delegateModel, predicate)) {
				continue;
			}

			NodeWrapper subjectWrapper = delegateModel.addSubject(subject
					.as(Individual.class));
			delegateGraph.addVertex(subjectWrapper);

			OntResource predicateResource = model.getOntResource(predicate);
			NodeWrapper predicateWrapper = delegateModel
					.addPredicate(predicateResource);

			RDFNode object = stat.getObject();
			if (object.isURIResource()) {
				object = model.getResource(object.as(Resource.class).getURI());
			}
			NodeWrapper objectWrapper = delegateModel.addObject(object);

			if (objectWrapper != null) {
				delegateGraph.addVertex(objectWrapper);
				delegateGraph.addEdge(predicateWrapper, subjectWrapper,
						objectWrapper);
			}
		}
	}

	public void addTriplet(NodeWrapper subjectWrapper,
			NodeWrapper predicateWrapper, NodeWrapper objectWrapper) {
		if (subjectWrapper != null) {
			delegateGraph.addVertex(subjectWrapper);
		}
		if (objectWrapper != null) {
			delegateGraph.addVertex(objectWrapper);
		}
		if (subjectWrapper != null && predicateWrapper != null && objectWrapper != null) {
			delegateGraph.addEdge(predicateWrapper, subjectWrapper, objectWrapper);
		}
	}

	
	@Override
	public void saveData() {
		delegateModel.saveData();
	}

	@Override
	public String getDataIdentifier() {
		return delegateModel.getDataIdentifier();
	}

	@Override
	public Set<NodeWrapper> getSubjectTypes() {
		return delegateModel.getSubjectTypes();
	}

	@Override
	public Set<NodeWrapper> getAllSubjects() {
		return delegateModel.getAllSubjects();
	}

	@Override
	public Set<NodeWrapper> getAllPredicates() {
		return delegateModel.getAllPredicates();
	}

	@Override
	public Set<NodeWrapper> getPredicatesForSubject(NodeWrapper subjectType) {
		return delegateModel.getPredicatesForSubject(subjectType);
	}

	@Override
	public Set<NodeWrapper> getTypesForIndividual(NodeWrapper node) {
		return delegateModel.getTypesForIndividual(node);
	}

	@Override
	public Set<NodeWrapper> getIndividualsForType(NodeWrapper type) {
		return delegateModel.getIndividualsForType(type);
	}

	@Override
	public Set<NodeWrapper> getSuperPredicates(NodeWrapper node) {
		return delegateModel.getSuperPredicates(node);
	}

	@Override
	public void addFilter(Filter filter) {
		delegateModel.addFilter(filter);
	}

	@Override
	public void removeFilter(Filter filter) {
		delegateModel.removeFilter(filter);
	}

	@Override
	public void replaceFilter(Filter oldFilter, Filter newFilter) {
		delegateModel.replaceFilter(oldFilter, newFilter);
	}

	@Override
	public NodeWrapper getNodeWrapper(Individual instance) {
		return delegateModel.getNodeWrapper(instance);
	}

	@Override
	public Set<NodeWrapper> getNodeWrappers(OntClass ontClass) {
		return delegateModel.getNodeWrappers(ontClass);
	}

	@Override
	public Set<NodeWrapper> searchNodeWrappers(String query)
			throws SearchFailedException {
		return delegateModel.searchNodeWrappers(query);
	}

	@Override
	public IntervalChain<NodeWrapper> getTimedThings() {
		return delegateModel.getTimedThings();
	}

	@Override
	public QuadTree<CenteredThing<NodeWrapper>> getLocatedThings() {
		return delegateModel.getLocatedThings();
	}

	@Override
	public void addParrotModelListener(ParrotModelListener pml) {
		delegateModel.addParrotModelListener(pml);
	}

	@Override
	public void removeParrotModelListener(ParrotModelListener pml) {
		delegateModel.removeParrotModelListener(pml);
	}

	@Override
	public void deleteEdge(NodeWrapper edge) {
		// TODO #13 actually implement this when sorting out the editing
	}

	@Override
	public void deleteNode(NodeWrapper vertex) {
		// TODO #13 actually implement this when sorting out the editing
	}

	@Override
	public boolean isBusy() {
		return delegateModel.isBusy();
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
	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain) {
		return delegateModel.getNodeWrappersOnChain(chain);
	}

	@Override
	public List<List<NodeWrapper>> getChains(List<ChainLink> chain) {
		return delegateModel.getChains(chain);
	}

	@Override
	public boolean hasSuccessor(NodeWrapper node, NodeWrapper maybeSuccessor) {
		return delegateGraph.isSuccessor(maybeSuccessor, node);
	}

	@Override
	public Collection<NodeWrapper> getSuccessorNodes(NodeWrapper node) {
		return delegateGraph.getSuccessors(node);
	}
	
	@Override
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to) {
		return delegateGraph.findEdgeSet(from, to);
	}

	@Override
	public NodeWrapper getNodeWrapperForString(String url)
			throws NoSuchNodeWrapperException {
		return delegateModel.getNodeWrapperForString(url);
	}

	@Override
	public GraphParrotModel asGraphModel() {
		return this;
	}

	@Override
	public TableParrotModel asListModel() {
		throw new UnsupportedOperationException("can't make a graph model out of this");
	}

	public void setDelegate(ParrotModelHelper delegateModel) {
		this.delegateModel = delegateModel;
	}

	/* delegate methods for delegateGraph */

	public boolean addEdge(NodeWrapper edge, NodeWrapper v1, NodeWrapper v2,
			EdgeType edgeType) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean addEdge(NodeWrapper e, NodeWrapper v1, NodeWrapper v2) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean addEdge(NodeWrapper edge, Pair<NodeWrapper> endpoints,
			EdgeType edgeType) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean addEdge(NodeWrapper edge, Pair<NodeWrapper> endpoints) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean addVertex(NodeWrapper vertex) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean containsEdge(NodeWrapper edge) {
		return delegateGraph.containsEdge(edge);
	}

	public boolean containsVertex(NodeWrapper vertex) {
		return delegateGraph.containsVertex(vertex);
	}

	public int degree(NodeWrapper vertex) {
		return delegateGraph.degree(vertex);
	}

	public boolean equals(Object obj) {
		return delegateGraph.equals(obj);
	}

	public NodeWrapper findEdge(NodeWrapper arg0, NodeWrapper arg1) {
		return delegateGraph.findEdge(arg0, arg1);
	}

	public Collection<NodeWrapper> findEdgeSet(NodeWrapper arg0,
			NodeWrapper arg1) {
		return delegateGraph.findEdgeSet(arg0, arg1);
	}

	public EdgeType getDefaultEdgeType() {
		return delegateGraph.getDefaultEdgeType();    
	}

	public NodeWrapper getDest(NodeWrapper edge) {
		return delegateGraph.getDest(edge);
	}

	public int getEdgeCount() {
		return delegateGraph.getEdgeCount();
	}

	public int getEdgeCount(EdgeType edge_type) {
		return delegateGraph.getEdgeCount(edge_type);
	}

	public Collection<NodeWrapper> getEdges() {
		return delegateGraph.getEdges();
	}

	public Collection<NodeWrapper> getEdges(EdgeType arg0) {
		return delegateGraph.getEdges(arg0);
	}

	public EdgeType getEdgeType(NodeWrapper edge) {
		return delegateGraph.getEdgeType(edge);
	}

	public Pair<NodeWrapper> getEndpoints(NodeWrapper edge) {
		return delegateGraph.getEndpoints(edge);
	}

	public int getIncidentCount(NodeWrapper edge) {
		return delegateGraph.getIncidentCount(edge);
	}

	public Collection<NodeWrapper> getIncidentEdges(NodeWrapper vertex) {
		return delegateGraph.getIncidentEdges(vertex);
	}

	public Collection<NodeWrapper> getIncidentVertices(NodeWrapper edge) {
		return delegateGraph.getIncidentVertices(edge);
	}

	public Collection<NodeWrapper> getInEdges(NodeWrapper vertex) {
		return delegateGraph.getInEdges(vertex);
	}

	public int getNeighborCount(NodeWrapper vertex) {
		return delegateGraph.getNeighborCount(vertex);
	}

	public Collection<NodeWrapper> getNeighbors(NodeWrapper vertex) {
		return delegateGraph.getNeighbors(vertex);
	}

	public NodeWrapper getOpposite(NodeWrapper vertex, NodeWrapper edge) {
		return delegateGraph.getOpposite(vertex, edge);
	}

	public Collection<NodeWrapper> getOutEdges(NodeWrapper vertex) {
		return delegateGraph.getOutEdges(vertex);
	}

	public int getPredecessorCount(NodeWrapper vertex) {
		return delegateGraph.getPredecessorCount(vertex);
	}

	public Collection<NodeWrapper> getPredecessors(NodeWrapper arg0) {
		return delegateGraph.getPredecessors(arg0);
	}

	public NodeWrapper getSource(NodeWrapper edge) {
		return delegateGraph.getSource(edge);
	}

	public int getSuccessorCount(NodeWrapper vertex) {
		return delegateGraph.getSuccessorCount(vertex);
	}

	public Collection<NodeWrapper> getSuccessors(NodeWrapper vertex) {
		return delegateGraph.getSuccessors(vertex);
	}

	public int getVertexCount() {
		return delegateGraph.getVertexCount();
	}

	public Collection<NodeWrapper> getVertices() {
		return delegateGraph.getVertices();
	}

	public int hashCode() {
		return delegateGraph.hashCode();
	}

	public int inDegree(NodeWrapper vertex) {
		return delegateGraph.inDegree(vertex);
	}

	public boolean isDest(NodeWrapper vertex, NodeWrapper edge) {
		return delegateGraph.isDest(vertex, edge);
	}

	public boolean isIncident(NodeWrapper vertex, NodeWrapper edge) {
		return delegateGraph.isIncident(vertex, edge);
	}

	public boolean isNeighbor(NodeWrapper v1, NodeWrapper v2) {
		return delegateGraph.isNeighbor(v1, v2);
	}

	public boolean isPredecessor(NodeWrapper v1, NodeWrapper v2) {
		return delegateGraph.isPredecessor(v1, v2);
	}

	public boolean isSource(NodeWrapper vertex, NodeWrapper edge) {
		return delegateGraph.isSource(vertex, edge);
	}

	public boolean isSuccessor(NodeWrapper v1, NodeWrapper v2) {
		return delegateGraph.isSuccessor(v1, v2);
	}

	public int outDegree(NodeWrapper vertex) {
		return delegateGraph.outDegree(vertex);
	}

	public boolean removeEdge(NodeWrapper edge) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public boolean removeVertex(NodeWrapper vertex) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	public String toString() {
		return delegateGraph.toString();
	}

	@Override
	public boolean addEdge(NodeWrapper arg0,
			Collection<? extends NodeWrapper> arg1) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

	@Override
	public boolean addEdge(NodeWrapper arg0,
			Collection<? extends NodeWrapper> arg1, EdgeType arg2) {
		// TODO #13 actually implement this when sorting out the editing
		return false;
	}

}
