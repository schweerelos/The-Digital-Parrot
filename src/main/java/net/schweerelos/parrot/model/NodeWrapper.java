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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;
import org.mindswap.pellet.jena.ModelExtractor;
import org.mindswap.pellet.jena.ModelExtractor.StatementType;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class NodeWrapper implements Comparable<NodeWrapper> {
	
	public static final class ToStringComparator implements
			Comparator<NodeWrapper> {
		@Override
		public int compare(NodeWrapper o1, NodeWrapper o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);

	private OntResource ontResource;
	private Literal literal;

	private boolean highlighted;
	private boolean hereTooSelected;
	
	private String label;
	private String toolTip;

	private boolean hasCreatedToolTip = false;

	public NodeWrapper(OntResource node) {
		this.ontResource = node;
	}

	public NodeWrapper(Literal literal) {
		this.literal = literal;
	}

	public String toString() {
		if (label == null) {
			if (isOntResource()) {
				label = extractLabel(ontResource);
			} else if (isLiteral()) {
				label = extractLabel(literal);
			} else {
				label = ""; // TODO does this cause problems?
			}
		}
		return label;
	}

	@Override
	public int compareTo(NodeWrapper o) {
		//System.out.println("NodeWrapper: comparing " + this + " (" + this.hashCode() + ") to " + o + " (" + o.hashCode() + ")");
		int comparison = toString().compareTo(o.toString());
		if (comparison != 0) {
			return comparison;
		}
		// equal strings
		if (isLiteral() && o.isLiteral()) {
			return literal.toString().compareTo(o.literal.toString());
		} else if (isOntResource() && o.isOntResource()) {
			if (ontResource.isProperty() && o.ontResource.isProperty()) {
				return new Integer(ontResource.hashCode()).compareTo(o.ontResource.hashCode());
			} else {
				String thisURI = ontResource.getURI();
				String otherURI = o.ontResource.getURI();
				if (thisURI != null && otherURI != null) {
					return thisURI.compareTo(otherURI);
				} else if (thisURI != null) {
					return -1;
				} else if (otherURI != null) {
					return 1;
				} else {
					// both null
					return 0;
				}
			}
		} else {
			// highly unlikely that we ever get here: a literal looks like a resource
			return comparison;
		}
	}


//
//	@Override
//	public boolean equals(Object obj) {
//		if (!(obj instanceof NodeWrapper)) {
//			return false;
//		}
//		NodeWrapper other = (NodeWrapper) obj;
//		return compareTo(other) == 0;
//	}

	public boolean isLiteral() {
		return literal != null;
	}

	public boolean isOntResource() {
		return !isLiteral();
	}

	public OntResource getOntResource() {
		return ontResource;
	}

	public Literal getLiteral() {
		return literal;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHereTooSelected(boolean hereTooSelected) {
		this.hereTooSelected = hereTooSelected;
	}

	public boolean isHereTooSelected() {
		return hereTooSelected;
	}
	

	private static String extractLabel(Literal literal) {
		String label;
		if (literal.getDatatype() == null) {
			label = literal.getLexicalForm();
		} else {
			Object literalValue = literal.getValue();
			if (literalValue instanceof XSDDateTime) {
				Calendar date = ((XSDDateTime) literalValue).asCalendar();
				label = DATE_FORMAT.format(date.getTime());
			} else {
				label = literalValue.toString();
			}
		}
		return label;
	}
	
	private static String extractLabel(OntResource resource) {
		String result;
		String enLabel = resource.getLabel("en");
		if (enLabel != null && !enLabel.equals("")) {
			result = enLabel;
		} else {
			String label = resource.getLabel(null);
			if (label != null && !label.equals("")) {
				result = label;
			} else {
				result = resource.getLocalName();
			}
		}
		return result;
	}

	public String getToolTipText(ParrotModel model) {
		if (!hasCreatedToolTip) {
			toolTip = createToolTip(model);
			hasCreatedToolTip = true;
		}
		return toolTip;
	}

	private String createToolTip(ParrotModel model) {
		if (!isOntResource()) {
			return "";
		}
		OntResource resource = getOntResource();
		if (!resource.isIndividual() || resource.isProperty()) {
			return "";
		}
		
		StringBuilder text = new StringBuilder();
		
		// use extracted model to speed up reasoning
		OntModel ontModel = model.getOntModel();
		ModelExtractor extractor = new ModelExtractor(ontModel);
		extractor.setSelector(StatementType.PROPERTY_VALUE);
		Model eModel = extractor.extractModel();

		Resource eResource = eModel.getResource(resource.getURI());

		StmtIterator props = eResource.listProperties();
		while (props.hasNext()) {
			Statement statement = props.nextStatement();
			Property pred = statement.getPredicate();
			if (!pred.isURIResource()) {
				continue;
			}
			OntProperty ontPred = ontModel.getOntProperty(pred.getURI());
			if (ontPred == null) {
				continue;
			}
			if (ParrotModelHelper.showTypeAsSecondary(ontModel, ontPred)) {
				// anything in the tooltip yet? if so, add line break
				text.append(text.length() > 0 ? "<br>" : "");
				// put in extracted predicate label
				text.append(extractLabel(ontPred));
				text.append(" ");

				RDFNode object = statement.getObject();
				if (object.isLiteral()) {
					Literal literal = (Literal) object.as(Literal.class);
					String lexicalForm = literal.getLexicalForm();
					if (literal.getDatatype().equals(XSDDatatype.XSDdateTime)) {
						DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
						DateTime dateTime = parser.parseDateTime(lexicalForm);
						DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder()
							.appendMonthOfYearShortText()
							.appendLiteral(' ')
							.appendDayOfMonth(1)
							.appendLiteral(", ")
							.appendYear(4, 4)
							.appendLiteral(", ")
							.appendHourOfHalfday(1)
							.appendLiteral(':')
							.appendMinuteOfHour(2)
							.appendHalfdayOfDayText()
							.appendLiteral(" (")
							.appendTimeZoneName()
							.appendLiteral(", UTC")
							.appendTimeZoneOffset("", true, 1, 1)
							.appendLiteral(')');
						String prettyDateTime = formatter.toFormatter().print(dateTime);
						text.append(prettyDateTime);
					} else {
						text.append(lexicalForm);
					}
				} else if (object.isURIResource()) {
					OntResource ontObject = ontModel.getOntResource((Resource) object.as(Resource.class));
					if (ontObject == null) {
						continue;
					}
					text.append(extractLabel(ontObject));
				}
			}
		}
		// surround with html tags
		text.insert(0, "<html>");
		text.append("</html>");
		
		String result = text.toString();
		if (result.equals("<html></html>")) {
			result = "";
		}
		return result;
	}

	public boolean isType() {
		if (!isOntResource()) {
			return false;
		}
		return getOntResource().isClass();
	}

}
