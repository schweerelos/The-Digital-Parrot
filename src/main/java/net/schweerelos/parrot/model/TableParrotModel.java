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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

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

public class TableParrotModel extends AbstractTableModel implements TableModel,
		ParrotModel {

	private static final long serialVersionUID = 1L;
	private static final String[] COLUMN_NAMES = { "this", "is related to",
			"that" };
	
	private ParrotModelHelper delegateModel;
	private List<NodeWrapper[]> data;
	private OntModel ontModel;

	public TableParrotModel(OntModel model) {
		ontModel = model;
		data = new ArrayList<NodeWrapper[]>();
	}

	public TableModel asTableModel() {
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
		populateTableModel(delegateModel.getOntModel());
	}

	private void populateTableModel(OntModel model) {
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
					|| !ParrotModelHelper.isPotentialNode(delegateModel, subject.as(Individual.class))) {
				continue;
			}
			
			Property predicate = stat.getPredicate();
			if (predicate.isURIResource()) {
				predicate = model.getProperty(predicate.getURI());
			}
			if (!ParrotModelHelper.isPotentialEdge(delegateModel, predicate)) {
				continue;
			}

			NodeWrapper[] row = new NodeWrapper[3];

			NodeWrapper subjectWrapper = delegateModel.addSubject(subject.as(Individual.class));
			row[0] = subjectWrapper;

			OntResource predicateResource = model.getOntResource(predicate);
			NodeWrapper predicateWrapper = delegateModel.addPredicate(predicateResource);
			row[1] = predicateWrapper;

			RDFNode object = stat.getObject();
			if (object.isURIResource()) {
				object = model.getResource(object.as(Resource.class).getURI());
			}
			NodeWrapper objectWrapper = delegateModel.addObject(object);
			row[2] = objectWrapper;

			if (objectWrapper != null) {
				data.add(row);
			}
		}
	}


	@Override
	public void saveData() {
		delegateModel.saveData();
	}

	@Override
	public Set<NodeWrapper> getSubjectTypes() {
		return delegateModel.getSubjectTypes();
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
	public Set<NodeWrapper> searchNodeWrappers(String query) throws SearchFailedException {
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
	public void deleteEdge(NodeWrapper edge) {
		delegateModel.deleteEdge(edge);
	}

	@Override
	public void deleteNode(NodeWrapper vertex) {
		delegateModel.deleteNode(vertex);
	}

	@Override
	public boolean isBusy() {
		return delegateModel.isBusy();
	}

	@Override
	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain) {
		return delegateModel.getNodeWrappersOnChain(chain);
	}
	
	@Override
	public List<List<NodeWrapper>> getChains(List<ChainLink> links) {
		// we need to make sure that the statements are being shown correctly
		return delegateModel.getChains(links);
	}

	@Override
	public boolean hasSuccessor(NodeWrapper node, NodeWrapper maybeSuccessor) {
		for (NodeWrapper[] row : data) {
			if (row[0] == node && row[2] == maybeSuccessor) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<NodeWrapper> getSuccessorNodes(NodeWrapper node) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		for (NodeWrapper[] row : data) {
			// check if there is a node -> edge -> destination row
			if (row[0] == node) {
				result.add(row[2]); // add destination of edge
			}
		}
		return result;
	}
	
	@Override
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		for (NodeWrapper[] row : data) {
			// if subject and object match
			if (row[0] == from && row[2] == to) {
				// add predicate
				result.add(row[1]);
			}
		}
		return result;
	}

	@Override
	public Set<NodeWrapper> getAllNodes() {
		return delegateModel.getAllNodes();
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
	public Set<NodeWrapper> getAllNodeWrappers() {
		return delegateModel.getAllNodeWrappers();
	}

	@Override
	public NodeWrapper getNodeWrapperForString(String url) throws NoSuchNodeWrapperException {
		return delegateModel.getNodeWrapperForString(url);
	}

	@Override
	public String getDataIdentifier() {
		return delegateModel.getDataIdentifier();
	}

	@Override
	public GraphParrotModel asGraphModel() {
		throw new UnsupportedOperationException("can't make a graph model out of this");
	}

	@Override
	public TableParrotModel asListModel() {
		return this;
	}

	public void setDelegate(ParrotModelHelper delegateModel) {
		this.delegateModel = delegateModel;
	}

	/* TableModel methods */

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return NodeWrapper.class;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data.get(rowIndex)[columnIndex];
	}

	public void addTriplet(NodeWrapper subjectWrapper,
			NodeWrapper predicateWrapper, NodeWrapper objectWrapper) {
		if (subjectWrapper == null || predicateWrapper == null || objectWrapper == null) {
			return;
		}
		NodeWrapper[] row = new NodeWrapper[3];
		row[0] = subjectWrapper;
		row[1] = predicateWrapper;
		row[2] = objectWrapper;
		data.add(row);
	}

}
