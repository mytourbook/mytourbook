/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.search;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class MTSearchManager {

	private static final String				LUCENE_INDEX_FOLDER_NAME		= "lucene-index";

	private static final String				SEARCH_FIELD_DESCRIPTION		= "description";						//$NON-NLS-1$
	private static final String				SEARCH_FIELD_MARKER_ID			= "markerID";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TOUR_MARKERLABEL	= "label";								//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TITLE				= "title";								//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TOUR_ID			= "tourID";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TOUR_START_TIME	= "time";								//$NON-NLS-1$

	private static final List<LookupResult>	_emptyProposal					= new ArrayList<LookupResult>();

	private static Lookup					_suggester;
	private static IndexReader				_indexReader;
	private static ScoreDoc					_afterScoreDoc;

	private static TopDocs					_topDocs;
	private static String					_topDocsSearchText;

	// configure field with offsets at index time
	private static final FieldType			_longSearchField				= new FieldType(LongField.TYPE_STORED);
	private static final FieldType			_textSearchField				= new FieldType(TextField.TYPE_STORED);
	{
		_longSearchField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		_textSearchField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	}

	public static class MyPostingsHighlighter extends PostingsHighlighter {

		private MyPostingsHighlighter() {
			super();
		}

		@Override
		protected Analyzer getIndexAnalyzer(final String field) {
			return getAnalyzer();
		}
	}

	/**
	 * Enable Lucene tool.
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private static void createIndex(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		monitor.subTask(Messages.Database_Monitor_SetupLucene);

		createIndex_TourData(conn, monitor);
		createIndex_TourMarker(conn, monitor);
	}

	private static void createIndex_TourData(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final Analyzer analyzer = getAnalyzer();
		final IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;
		try {

			indexStore = openIndex(TourDatabase.TABLE_TOUR_DATA);

			/*
			 * Setup index writer
			 */

			// delete old index and create a new
			writerConfig.setOpenMode(OpenMode.CREATE);

			indexWriter = new IndexWriter(indexStore, writerConfig);

			/*
			 * Get sql data
			 */
			final String sql = "" // //$NON-NLS-1$
					//
					+ "SELECT" //$NON-NLS-1$
					//
					+ " tourId," //				1 //$NON-NLS-1$
					+ " tourTitle," //			2 //$NON-NLS-1$
					+ " tourDescription," //	3 //$NON-NLS-1$
					+ " tourStartTime" //		4 //$NON-NLS-1$
					//
					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final Document doc = new Document();

				final long dbTourId = rs.getLong(1);
				final String dbTitle = rs.getString(2);
				final String dbDescription = rs.getString(3);
				final Long dbTourStartTime = rs.getLong(4);

				doc.add(new LongField(SEARCH_FIELD_TOUR_ID, dbTourId, Store.YES));
				doc.add(new LongField(SEARCH_FIELD_TOUR_START_TIME, dbTourStartTime, _longSearchField));

				if (dbTitle != null) {
					doc.add(new Field(SEARCH_FIELD_TITLE, dbTitle, _textSearchField));
				}

				if (dbDescription != null) {
					doc.add(new Field(SEARCH_FIELD_DESCRIPTION, dbDescription, _textSearchField));
				}

				indexWriter.addDocument(doc);

				createdDocuments++;

				/*
				 * Update monitor every 1/20 seconds
				 */
				final long now = System.currentTimeMillis();

				if (now > lastUpdateTime + 50) {
					lastUpdateTime = now;
					monitor.subTask(NLS.bind(Messages.Search_Manager_CreateFTIndex, createdDocuments));
				}
			}

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		} finally {

			if (indexWriter != null) {
				try {
					indexWriter.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
			}

			if (indexStore != null) {
				indexStore.close();
			}

			Util.closeSql(stmt);
		}
	}

	private static void createIndex_TourMarker(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final Analyzer analyzer = getAnalyzer();
		final IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;

		try {

			indexStore = openIndex(TourDatabase.TABLE_TOUR_MARKER);

			/*
			 * Setup index writer
			 */

			// delete old index and create a new
			writerConfig.setOpenMode(OpenMode.CREATE);

			indexWriter = new IndexWriter(indexStore, writerConfig);

			/*
			 * Get sql data
			 */
			final String sql = "" // //$NON-NLS-1$
					//
					+ "SELECT" //$NON-NLS-1$
					//
					+ " markerId," //						1 //$NON-NLS-1$
					+ (TourDatabase.KEY_TOUR + ",") //		2
					+ " label," //							3 //$NON-NLS-1$
					+ " description," //					4 //$NON-NLS-1$
					+ " tourTime" //						5 //$NON-NLS-1$
					//
					+ (" FROM " + TourDatabase.TABLE_TOUR_MARKER); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final Document doc = new Document();

				final long dbMarkerId = rs.getLong(1);
				final long dbTourId = rs.getLong(2);
				final String dbLabel = rs.getString(3);
				final String dbDescription = rs.getString(4);
				final long dbTourTime = rs.getLong(5);

				doc.add(new LongField(SEARCH_FIELD_MARKER_ID, dbMarkerId, Store.YES));
				doc.add(new LongField(SEARCH_FIELD_TOUR_ID, dbTourId, Store.YES));
				doc.add(new LongField(SEARCH_FIELD_TOUR_START_TIME, dbTourTime, _longSearchField));

				if (dbLabel != null) {
					doc.add(new Field(SEARCH_FIELD_TOUR_MARKERLABEL, dbLabel, _textSearchField));
				}

				if (dbDescription != null) {
					doc.add(new Field(SEARCH_FIELD_DESCRIPTION, dbDescription, _textSearchField));
				}

				indexWriter.addDocument(doc);

				createdDocuments++;

				/*
				 * Update monitor every 1/20 seconds
				 */
				final long now = System.currentTimeMillis();

				if (now > lastUpdateTime + 50) {
					lastUpdateTime = now;
					monitor.subTask(NLS.bind(Messages.Search_Manager_CreateFTIndex, createdDocuments));
				}
			}

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		} finally {

			if (indexWriter != null) {
				try {
					indexWriter.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
			}

			if (indexStore != null) {
				indexStore.close();
			}

			Util.closeSql(stmt);
		}
	}

	private static Analyzer getAnalyzer() {

		Analyzer analyzer = null;

		try {

			final Locale currentLocale = Locale.getDefault();

//		currentLocale = Locale.GERMAN;
//		currentLocale = Locale.ENGLISH;
//
			analyzer = SearchUtils.getAnalyzerForLocale(currentLocale);
//		analyzer = new GermanAnalyzer();
//		analyzer = new EnglishAnalyzer();

			analyzer = new StandardAnalyzer(new CharArraySet(0, true));

		} catch (final SQLException e) {
			StatusUtil.showStatus(e);
		}

		return analyzer;
	}

	static List<LookupResult> getProposals(final String contents, final int position) {

		try {

			if (_suggester == null) {
				_suggester = setupSuggester();
			}

			return _suggester.lookup(contents, false, 999);

		} catch (final Exception e) {
			return _emptyProposal;
		}
	}

	private static FSDirectory openIndex(final String tableName) throws IOException {

		final Path dbPath = new Path(TourDatabase.getDatabasePath());

		final IPath luceneIndexPath = dbPath.append(LUCENE_INDEX_FOLDER_NAME).append(tableName);
		final String indexFolder = luceneIndexPath.toOSString();

		final FSDirectory indexDirectory = FSDirectory.open(new File(indexFolder));

		return indexDirectory;
	}

	public static void saveState() {

		if (_indexReader != null) {
			try {
				_indexReader.close();
				_indexReader = null;
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}
	}

	/**
	 * @param searchText
	 * @param pageNumber
	 *            Starting from 0.
	 * @param hitsPerPage
	 * @return
	 */
	public static SearchResult search(final String searchText, final int pageNumber, final int hitsPerPage) {

		final SearchResult searchResult = new SearchResult();

		searchResult.pageNumber = pageNumber;
		searchResult.hitsPerPage = hitsPerPage;

		try {

			final String[] queryFields = {
					//
					SEARCH_FIELD_TITLE,
					SEARCH_FIELD_DESCRIPTION,
					SEARCH_FIELD_TOUR_MARKERLABEL,
			//
			};

			final int maxPassages[] = new int[queryFields.length];
			Arrays.fill(maxPassages, 1);

			setupIndexReader();
			final Analyzer analyzer = getAnalyzer();

			final MultiFieldQueryParser queryParser = new MultiFieldQueryParser(queryFields, analyzer);
			final Query query = queryParser.parse(searchText);

			final IndexSearcher indexSearcher = new IndexSearcher(_indexReader);

			if (_topDocsSearchText == null || _topDocsSearchText.equals(searchText) == false) {

				// this is a new search

				_topDocs = indexSearcher.search(query, _indexReader.maxDoc());
				_topDocsSearchText = searchText;
			}

			final ScoreDoc[] scoreDocs = _topDocs.scoreDocs;

			/**
			 * Get doc id's only for the current page.
			 * <p>
			 * It is very cheap to query the doc id's but very expensive to retrieve the documents.
			 */
			final int maxDocIndex = scoreDocs.length;
			final int docStartIndex = pageNumber * hitsPerPage;
			int docEndIndex = (pageNumber + 1) * hitsPerPage;

			if (docEndIndex > maxDocIndex) {
				docEndIndex = maxDocIndex;
			}

			final int maxPageIndex = docEndIndex - docStartIndex;
			final int docids[] = new int[maxPageIndex];

			for (int docIndex = 0; docIndex < maxPageIndex; docIndex++) {
				docids[docIndex] = scoreDocs[docStartIndex + docIndex].doc;
			}

			final MyPostingsHighlighter highlighter = new MyPostingsHighlighter();
			final Map<String, String[]> highlights = highlighter.highlightFields(
					queryFields,
					query,
					indexSearcher,
					docids,
					maxPassages);

			search_CreateResult(highlights, _topDocs, _indexReader, searchResult, docids, docStartIndex);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}

		return searchResult;
	}

	/**
	 * Creating the result is complicated because the highlights are listed by field and not by hit,
	 * therefor the structure must be inverted.
	 * 
	 * @param highlights
	 * @param topDocs
	 * @param multiReader
	 * @param searchResult
	 * @param docStartIndex
	 * @param docids2
	 * @throws IOException
	 */
	private static void search_CreateResult(final Map<String, String[]> highlights,
											final TopDocs topDocs,
											final IndexReader multiReader,
											final SearchResult searchResult,
											final int[] docids,
											final int docStartIndex) throws IOException {

		if (highlights.size() == 0) {
			return;
		}

		searchResult.totalHits = topDocs.totalHits;
		final ScoreDoc[] scoreDocs = topDocs.scoreDocs;

		final Set<Entry<String, String[]>> fields = highlights.entrySet();
		Entry<String, String[]> firstHit;
		try {
			firstHit = fields.iterator().next();
		} catch (final Exception e) {
			return;
		}

		final int numberOfHits = firstHit.getValue().length;

		// create result items
		final SearchResultItem[] resultItems = new SearchResultItem[numberOfHits];
		final ArrayList<SearchResultItem> searchResultItems = searchResult.items;

		for (int hitIndex = 0; hitIndex < numberOfHits; hitIndex++) {

			final SearchResultItem resultItem = new SearchResultItem();

			resultItems[hitIndex] = resultItem;

			searchResultItems.add(resultItem);
		}

		boolean isDocRead = false;
		final Set<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add(SEARCH_FIELD_TOUR_ID);
		fieldsToLoad.add(SEARCH_FIELD_MARKER_ID);
		fieldsToLoad.add(SEARCH_FIELD_TOUR_START_TIME);

		for (final Entry<String, String[]> field : fields) {

			final String fieldName = field.getKey();
			final String[] snippets = field.getValue();

			for (int hitIndex = 0; hitIndex < snippets.length; hitIndex++) {

				final SearchResultItem resultItem = resultItems[hitIndex];
				final String snippet = snippets[hitIndex];

				switch (fieldName) {

				case SEARCH_FIELD_DESCRIPTION:
					resultItem.description = snippet;
					break;

				case SEARCH_FIELD_TITLE:
					resultItem.tourTitle = snippet;
					break;

				case SEARCH_FIELD_TOUR_MARKERLABEL:
					resultItem.markerLabel = snippet;
					break;
				}

				if (isDocRead == false) {

					final int docId = docids[hitIndex];
					final Document doc = multiReader.document(docId, fieldsToLoad);

					resultItem.docId = docId;
					resultItem.score = scoreDocs[docStartIndex + hitIndex].score;

					for (final IndexableField indexField : doc.getFields()) {

						final String docFieldName = indexField.name();

						switch (docFieldName) {
						case SEARCH_FIELD_TOUR_ID:
							resultItem.tourId = indexField.stringValue();
							break;

						case SEARCH_FIELD_MARKER_ID:
							resultItem.markerId = indexField.stringValue();
							break;

						case SEARCH_FIELD_TOUR_START_TIME:
							resultItem.tourStartTime = Long.parseLong(indexField.stringValue());
							break;
						}
					}
				}
			}

			// get doc fields only once
			isDocRead = true;
		}
	}

	/**
	 * Once you have a new IndexReader, it's relatively cheap to create a new IndexSearcher from it.
	 */
	private static void setupIndexReader() {

		if (_indexReader != null) {
			// index reader is initialized
			return;
		}

		IndexReader indexReader1 = null;
		IndexReader indexReader2 = null;

		try {

			final FSDirectory tourDataIndex = openIndex(TourDatabase.TABLE_TOUR_DATA);
			final FSDirectory tourMarkerIndex = openIndex(TourDatabase.TABLE_TOUR_MARKER);

			indexReader1 = DirectoryReader.open(tourDataIndex);
			indexReader2 = DirectoryReader.open(tourMarkerIndex);

			_indexReader = new MultiReader(indexReader1, indexReader2);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}
	}

	/**
	 * Enable Lucene tool.
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	public static void setupSearch(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		createIndex(conn, monitor);
	}

	private static Lookup setupSuggester() {

		setupIndexReader();

		final Lookup suggester[] = new AnalyzingSuggester[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				try {

					final TermFreqIteratorListWrapper inputIterator = new TermFreqIteratorListWrapper();

					final List<AtomicReaderContext> leaves = _indexReader.leaves();

					for (final AtomicReaderContext readerContext : leaves) {

						final AtomicReader reader = readerContext.reader();
						final Fields fields = reader.fields();

						for (final String field : fields) {

							if (field.equals(SEARCH_FIELD_DESCRIPTION) //
									|| field.equals(SEARCH_FIELD_TITLE)
									|| field.equals(SEARCH_FIELD_TOUR_MARKERLABEL)
							//
							) {

								final Terms terms = fields.terms(field);
								final TermsEnum termsEnum = terms.iterator(null);

								inputIterator.add(termsEnum);
							}
						}
					}

					final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));

					suggester[0] = new AnalyzingSuggester(queryAnalyzer);

// THIS IS NOT WORKING
//					suggester[0] = new AnalyzingInfixSuggester(
//							Version.LUCENE_4_10_1,
//							dir,
//							getAnalyzer(),
//							queryAnalyzer,
//							4);

					suggester[0].build(inputIterator);

				} catch (final Exception e) {
					StatusUtil.showStatus(e);
				}
			}
		});

		return suggester[0];
	}
}
