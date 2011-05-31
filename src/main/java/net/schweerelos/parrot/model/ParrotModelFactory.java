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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.PelletOptions.MonitorType;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public abstract class ParrotModelFactory {

	private static final String URL_CONFERENCES = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/Conferences/2008/11/Conferences.owl";
	private static final String URL_INTERACTION = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/Interaction/2008/11/Interaction.owl";
	private static final String URL_FOAF = "http://www.mindswap.org/2003/owl/foaf";
	private static final String URL_MEMORIES = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/Memories/2008/11/Memories.owl";
	private static final String URL_TIME_AND_PLACE = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl";
	private static final String URL_WGS84_POS = "http://www.w3.org/2003/01/geo/wgs84_pos";
	private static final String URL_DIGITAL_PARROT = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/DigitalParrot/2009/02/DigitalParrot.owl";
	
	public enum Style { TABLE, GRAPH, COMBINED };

	public abstract ParrotModel createModel();
	
	ParrotModelFactory() {
		// hide constructor
	}
	
	public static ParrotModelFactory getInstance(Style style) {
		// get rid of classification status output 
		PelletOptions.USE_CLASSIFICATION_MONITOR = MonitorType.NONE;
		
		OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		
		File cacheDir = new File(System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "owl");
		if (cacheDir.exists() && cacheDir.isDirectory() && cacheDir.canRead()) {
			OntDocumentManager docManager = model.getDocumentManager();
			docManager.addAltEntry(URL_WGS84_POS, "file:" + cacheDir.getAbsolutePath() + File.separator + "wgs84_pos");
			docManager.addAltEntry(URL_TIME_AND_PLACE, "file:" + cacheDir.getAbsolutePath() + File.separator + "TimeAndPlace.owl");
			docManager.addAltEntry(URL_MEMORIES, "file:" + cacheDir.getAbsolutePath() + File.separator + "Memories.owl");
			docManager.addAltEntry(URL_FOAF, "file:" + cacheDir.getAbsolutePath() + File.separator + "foaf");
			docManager.addAltEntry(URL_INTERACTION, "file:" + cacheDir.getAbsolutePath() + File.separator + "Interaction.owl");
			docManager.addAltEntry(URL_CONFERENCES, "file:" + cacheDir.getAbsolutePath() + File.separator + "Conferences.owl");
			docManager.addAltEntry(URL_DIGITAL_PARROT, "file:" + cacheDir.getAbsolutePath() + File.separator + "DigitalParrot.owl");
		
			try {
				model.read(new FileReader(new File(cacheDir + File.separator + "annotated-types.rdf")), null);
			} catch (FileNotFoundException e) {
				e.printStackTrace(System.err);
			}
		}
		
		switch (style) {
		case TABLE:
			return new TableModelFactory(model);
		case GRAPH:
			return new GraphModelFactory(model);
		case COMBINED:
			return new CombinedModelFactory(model);
		default:
			throw new IllegalArgumentException("No such style: " + style);
		}
	}
}
