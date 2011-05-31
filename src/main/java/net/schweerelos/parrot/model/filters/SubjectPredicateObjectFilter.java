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

import java.util.HashSet;
import java.util.Set;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntResource;

public class SubjectPredicateObjectFilter extends Filter {

	private NodeWrapper subjectType;
	private NodeWrapper predicateType;

	public void setSubjectType(NodeWrapper subjectType) {
		this.subjectType = subjectType;
	}

	public void setPredicateType(NodeWrapper predicateType) {
		this.predicateType = predicateType;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		switch (getMode()) {
		case HIGHLIGHT:
			result.append("Highlight");
			break;
		case RESTRICT:
			result.append("Restrict to");
			break;
		}
		if (subjectType == null) {
			result.append(" everything");
		} else {
			result.append(" every ");
			result.append(subjectType);
		}
		if (predicateType != null) {
			result.append(" that ");
			result.append(predicateType);
		}
		return result.toString();
	}

	public boolean hasSubjectType() {
		return subjectType != null;
	}

	/**
	 * @return the filter's subject type. May be null if hasSubjectType() is false.
	 */
	public NodeWrapper getSubjectType() {
		return subjectType;
	}

	@Override
	public Set<NodeWrapper> getMatching(ParrotModel parrotModel) {
		Set<NodeWrapper> subjects = parrotModel.getAllSubjects();
		Set<NodeWrapper> matching = new HashSet<NodeWrapper>();
		for (NodeWrapper subject : subjects) {
			if (matchesSubject(subject)) {
				matching.add(subject);
			}
		}
		// TODO #25 actually implement this for more than just subjects
		return matching;
	}

	private boolean matchesSubject(NodeWrapper subject) {
		if (subjectType == null) {
			return true;
		}
		if (!subject.isOntResource()) {
			return false;
		}
		OntResource res = subject.getOntResource();
		if (!res.isIndividual()) {
			return false;
		}
		Individual ind = res.asIndividual();
		return ind.hasOntClass(subjectType.getOntResource());
	}

}
