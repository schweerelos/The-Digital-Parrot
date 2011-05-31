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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;

public class TextSearchEngine {
	
	private static final String LABEL_FIELD_NAME = "label";
	private static final String HASH_FIELD_NAME = "hash";
	private IndexWriter writer;
	private Directory index;
	private Analyzer analyser;

	private Map<Integer, NodeWrapper> hashToNodeWrapper;
	private IndexSearcher searcher;

	public TextSearchEngine() {
		index = new RAMDirectory();
		analyser = new StandardAnalyzer(); 
		try {
			writer = new IndexWriter(index, analyser, true);
		} catch (CorruptIndexException e) {
			// ignore
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// ignore
			e.printStackTrace();
		} catch (IOException e) {
			// ignore
			e.printStackTrace();
		}
		
		hashToNodeWrapper = new HashMap<Integer, NodeWrapper>();
	}

	public void add(NodeWrapper node) {
		Document doc = new Document();
		doc.add(new Field(LABEL_FIELD_NAME, node.toString(), Field.Store.COMPRESS, Field.Index.TOKENIZED));
		doc.add(new Field(HASH_FIELD_NAME, String.valueOf(node.hashCode()), Field.Store.YES, Field.Index.NO));
		try {
			writer.addDocument(doc);
			hashToNodeWrapper.put(node.hashCode(), node);
			writer.flush();
		} catch (CorruptIndexException e) {
			// ignore
			e.printStackTrace();
		} catch (IOException e) {
			// ignore
			e.printStackTrace();
		}
	}

	public Set<NodeWrapper> search(String queryString) throws SearchFailedException {
		Set<NodeWrapper> results = new HashSet<NodeWrapper>();
		Query query = null;
		try {
			QueryParser queryParser = new QueryParser(LABEL_FIELD_NAME, analyser);
			queryParser.setAllowLeadingWildcard(true);
			query = queryParser.parse(queryString);
		} catch (ParseException e) {
			throw new SearchFailedException("Problem parsing query string '" + queryString + "'. Can't search.", e);
		}
		if (searcher == null) {
			try {
				searcher = new IndexSearcher(index);
			} catch (CorruptIndexException e) {
				throw new SearchFailedException("Internal error. Can't search.", e);
			} catch (IOException e) {
				throw new SearchFailedException("Internal error. Can't search.", e);
			}
		}
		// get up to 10 best hits
		TopDocCollector collector = new TopDocCollector(10);
	    try {
			searcher.search(query, collector);
		} catch (IOException e) {
			throw new SearchFailedException("Internal error. Can't search.", e);
		}
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    for (int i = 0; i < hits.length; i++) {
	    	int docId = hits[i].doc;
	    	try {
				Document doc = searcher.doc(docId);
				int hashCode = Integer.parseInt(doc.get(HASH_FIELD_NAME));
				if (hashToNodeWrapper.containsKey(hashCode)) {
					results.add(hashToNodeWrapper.get(hashCode));
				}
			} catch (CorruptIndexException e) {
				throw new SearchFailedException("Internal error. Can't search.", e);
			} catch (IOException e) {
				throw new SearchFailedException("Internal error. Can't search.", e);
			}
		}

		return results;
	}

}
