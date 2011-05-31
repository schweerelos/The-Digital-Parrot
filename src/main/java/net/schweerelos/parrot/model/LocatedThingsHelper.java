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

import net.schweerelos.parrot.util.QuadTree;
import net.schweerelos.parrot.util.QuadTreeImpl;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public abstract class LocatedThingsHelper {
	static private final String ABSOLUTELY_PLACED_THING = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#AbsolutelyPlacedThing";
	static private final String PLACED_THING = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#PlacedThing";

	static private final String LATITUDE = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#lat";
	static private final String LONGITUDE = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#long";
	private static final String COORD_PRECISION = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#coordPrecision";
	private static final String LOCATED_IN = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#locatedIn";

	public static net.schweerelos.parrot.util.QuadTree<CenteredThing<NodeWrapper>> extractLocatedThings(
			ParrotModel pModel) {
		OntModel model = pModel.getOntModel();
		Resource placedThingClass = model.createClass(PLACED_THING);

		QuadTree<CenteredThing<NodeWrapper>> result = new QuadTreeImpl<CenteredThing<NodeWrapper>>();
		ExtendedIterator<Individual> instances = model
				.listIndividuals(placedThingClass);
		while (instances.hasNext()) {
			Individual instance = instances.next();
			NodeWrapper wrapper = pModel.getNodeWrapper(instance);
			try {
				CenteredThing<NodeWrapper> thing = createCenteredThingFor(
						instance, model, wrapper);
				result.put(thing.getLat(), thing.getLon(), thing);
			} catch (NotPlacedThingException e) { /* ignore */
			}
		}
		return result;
	}

	public static CenteredThing<NodeWrapper> getAsLocatedThing(
			NodeWrapper currentNode) throws NotPlacedThingException {
		try {
			return createCenteredThingFor(currentNode.getOntResource()
					.asIndividual(),
					currentNode.getOntResource().getOntModel(), currentNode);
		} catch (NullPointerException npe) {
			throw new NotPlacedThingException(currentNode
					+ " isn't a placed thing", npe);
		}
	}

	private static CenteredThing<NodeWrapper> createCenteredThingFor(
			Individual instance, OntModel model, NodeWrapper wrapper)
			throws NotPlacedThingException {
		float latitude = extractLatitude(instance, model);
		float longitude = extractLongitude(instance, model);

		CoordinatePrecision precision;
		try {
			precision = extractPrecision(instance, model);
		} catch (NotPlacedThingException e) {
			// use default precision
			// TODO is this a good idea?
			precision = CoordinatePrecision.RoomPrecision;
		}

		return new LocatedThing(longitude, latitude, precision, wrapper);
	}

	public static boolean isLocatedThing(NodeWrapper currentNode,
			ParrotModel model) {
		return isLocatedThing(currentNode, model.getOntModel());
	}

	private static boolean isLocatedThing(NodeWrapper currentNode,
			OntModel model) {
		if (!currentNode.isOntResource()) {
			return false;
		}
		OntResource res = currentNode.getOntResource();
		if (isAbsolutelyPlacedThing(res)) {
			return true;
		}
		return isIndirectlyPlacedThing(res, model);
	}

	private static boolean isIndirectlyPlacedThing(OntResource node,
			OntModel model) {
		Property locatedInProperty = model.createProperty(LOCATED_IN);
		if (!node.hasProperty(locatedInProperty)) {
			return false;
		}
		NodeIterator values = node.listPropertyValues(locatedInProperty);
		while (values.hasNext()) {
			RDFNode value = values.next();
			if (isAbsolutelyPlacedThing(value)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAbsolutelyPlacedThing(RDFNode node) {
		if (!node.canAs(OntResource.class)) {
			return false;
		}
		OntResource res = node.as(OntResource.class);
		return isAbsolutelyPlacedThing(res);
	}

	private static boolean isAbsolutelyPlacedThing(OntResource res) {
		if (!res.isIndividual()) {
			return false;
		}
		return res.asIndividual().hasOntClass(ABSOLUTELY_PLACED_THING);
	}

	private static float extractLatitude(OntResource node, OntModel model)
			throws NotPlacedThingException {
		// TODO #42 do it this way for timed things too
		Property latitudeProperty = model.createProperty(LATITUDE);
		RDFNode latitudeNode;
		// System.out.println("extracting latitude for " + node.getLocalName());
		if (node.hasProperty(latitudeProperty)) {
			// System.out.println("directly getting latitude value");
			latitudeNode = node.getPropertyValue(latitudeProperty);
		} else {
			// System.out.println("trying to find indirect latitude value");
			latitudeNode = findNearest(node, model, latitudeProperty);
			// System.out.println("success finding indirect latitude value");
		}
		return extractFloat(latitudeNode);
	}

	/**
	 * Goes through enclosing placed things and takes property value of the one
	 * with the smallest precision
	 * 
	 * @param node
	 * @param model
	 * @param latitudeProperty
	 * @return
	 * @throws NotPlacedThingException
	 */
	private static RDFNode findNearest(OntResource node, OntModel model,
			Property property) throws NotPlacedThingException {
		Property locatedInProperty = model.createProperty(LOCATED_IN);
		Property coordPrecisionProperty = model.createProperty(COORD_PRECISION);

		if (!node.hasProperty(locatedInProperty)) {
			throw new NotPlacedThingException("couldn't find a value for "
					+ property.getLocalName() + " from " + node.getLocalName());
		}

		CoordinatePrecision currentSmallestPrecision = null;
		RDFNode currentBest = null;
		NodeIterator locatedIns = node.listPropertyValues(locatedInProperty);
		// System.out.println("things that " + node.getLocalName() +
		// " is located in");
		while (locatedIns.hasNext()) {
			RDFNode locatedIn = locatedIns.next();
			// System.out.println("looking at " + locatedIn);
			if (locatedIn.canAs(OntResource.class)) {
				OntResource res = locatedIn.as(OntResource.class);
				if (res.hasProperty(property)
						&& res.hasProperty(coordPrecisionProperty)) {
					RDFNode precisionNode = res
							.getPropertyValue(coordPrecisionProperty);
					if (precisionNode.canAs(Individual.class)) {
						Individual precisionIndiv = precisionNode
								.as(Individual.class);
						CoordinatePrecision thisPrecision = CoordinatePrecision
								.valueOf(precisionIndiv.getLocalName());
						if (currentSmallestPrecision == null
								|| thisPrecision
										.compareTo(currentSmallestPrecision) < 0) {
							currentBest = res.getPropertyValue(property);
							currentSmallestPrecision = thisPrecision;
						}
					}
				}
			}
		}
		if (currentBest == null) {
			throw new NotPlacedThingException("couldn't find a value for "
					+ property.getLocalName() + " from " + node.getLocalName());
		}
		return currentBest;
	}

	private static float extractLongitude(OntResource node, OntModel model)
			throws NotPlacedThingException {
		Property longitudeProperty = model.createProperty(LONGITUDE);
		RDFNode longitudeValue;
		if (node.hasProperty(longitudeProperty)) {
			longitudeValue = node.getPropertyValue(longitudeProperty);
		} else {
			longitudeValue = findNearest(node, model, longitudeProperty);
		}
		return extractFloat(longitudeValue);
	}

	private static CoordinatePrecision extractPrecision(OntResource node,
			OntModel model) throws NotPlacedThingException {
		Property coordPrecisionProperty = model.createProperty(COORD_PRECISION);
		RDFNode coordPrecisionValue;
		if (node.hasProperty(coordPrecisionProperty)) {
			coordPrecisionValue = node.getPropertyValue(coordPrecisionProperty);
		} else {
			coordPrecisionValue = findNearest(node, model,
					coordPrecisionProperty);
		}

		Individual coordPrecision = coordPrecisionValue.as(Individual.class);
		CoordinatePrecision precision = CoordinatePrecision
				.valueOf(coordPrecision.getLocalName());
		return precision;
	}

	private static float extractFloat(RDFNode node) {
		if (!node.isLiteral()) {
			throw new IllegalArgumentException("Node is not a literal");
		}
		Object value = ((Literal) node).getValue();
		if (value != null && !value.toString().equals("")) {
			return Float.parseFloat(value.toString());
		} else {
			return 0f;
		}
	}

	private static final class LocatedThing implements
			CenteredThing<NodeWrapper> {
		private final float longitude;
		private final float latitude;
		private final CoordinatePrecision precision;
		private final NodeWrapper node;

		private LocatedThing(float longitude, float latitude,
				CoordinatePrecision precision, NodeWrapper node) {
			this.longitude = longitude;
			this.latitude = latitude;
			this.precision = precision;
			this.node = node;
		}

		@Override
		public String getLabel() {
			return node.toString();
		}

		@Override
		public float getLat() {
			return latitude;
		}

		@Override
		public float getLon() {
			return longitude;
		}

		@Override
		public NodeWrapper getValue() {
			return node;
		}

		@Override
		public CoordinatePrecision getPrecision() {
			return precision;
		}

		public String toString() {
			return "Located thing (" + getLabel() + ")";
		}
	}

}
