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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
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
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
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

	private static final String				LUCENE_INDEX_FOLDER_NAME	= "lucene-index";

	private static final String				SEARCH_FIELD_DESCRIPTION	= "description";					//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TITLE			= "title";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TOUR_ID		= "tourID";						//$NON-NLS-1$

	private static final List<LookupResult>	_emptyProposal				= new ArrayList<LookupResult>();

	private static AnalyzingSuggester		_suggester;

	public static class MyPostingsHighlighter extends PostingsHighlighter {

		private Analyzer	_analyzer;

		private MyPostingsHighlighter(final Analyzer analyzer) {

			super();

			_analyzer = analyzer;
		}

		@Override
		protected Analyzer getIndexAnalyzer(final String field) {
			return _analyzer;
		}

	}

	private static void closeReader(final DirectoryReader indexReader) {

		if (indexReader != null) {
			try {
				indexReader.close();
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
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

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;

		// configure field with offsets at index time
		final FieldType offsetsType = new FieldType(TextField.TYPE_STORED);
		offsetsType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

		try {

			monitor.subTask(Messages.Database_Monitor_SetupLucene);

			/*
			 * Setup index writer
			 */
			indexStore = openIndex(TourDatabase.TABLE_TOUR_DATA);

			final Analyzer analyzer = getAnalyzer();
			final IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);

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
					+ " tourDescription" //		3 //$NON-NLS-1$
					//
					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final Document doc = new Document();

				final String dbTourId = rs.getString(1);
				final String dbTitle = rs.getString(2);
				final String dbDescription = rs.getString(3);

				doc.add(new StringField(SEARCH_FIELD_TOUR_ID, dbTourId, Store.YES));

				if (dbTitle != null) {
					doc.add(new Field(SEARCH_FIELD_TITLE, dbTitle, offsetsType));
				}

				if (dbDescription != null) {
					doc.add(new Field(SEARCH_FIELD_DESCRIPTION, dbDescription, offsetsType));
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

	/**
	 * Creating the result is complicated because the highlights are listed by field and not by hit,
	 * therefor the structure must be inverted.
	 * 
	 * @param highlights
	 * @param topDocs
	 * @param searchResult
	 */
	private static void createSearchResult(	final Map<String, String[]> highlights,
											final TopDocs topDocs,
											final SearchResult searchResult) {

		if (highlights.size() == 0) {
			return;
		}

		final Set<Entry<String, String[]>> fields = highlights.entrySet();
		Entry<String, String[]> firstHit;
		try {
			firstHit = fields.iterator().next();
		} catch (final Exception e) {
			return;
		}

		final int numberOfHits = firstHit.getValue().length;

		final TreeMap<String, SearchResultItem> searchResultItems = searchResult.items;

		// create result items
		final SearchResultItem[] resultItems = new SearchResultItem[numberOfHits];
		for (int hitIndex = 0; hitIndex < numberOfHits; hitIndex++) {

			final SearchResultItem resultItem = new SearchResultItem();

			resultItems[hitIndex] = resultItem;
			searchResultItems.put(Integer.toString(hitIndex + 1), resultItem);
		}

		for (final Entry<String, String[]> field : fields) {

			final String fieldName = field.getKey();
			final String[] snippets = field.getValue();

			for (int hitIndex = 0; hitIndex < snippets.length; hitIndex++) {

				final SearchResultItem resultItem = resultItems[hitIndex];
				final String snippet = snippets[hitIndex];

				switch (fieldName) {

				case SEARCH_FIELD_DESCRIPTION:
					resultItem.tourDescription = snippet;
					break;

				case SEARCH_FIELD_TITLE:
					resultItem.tourTitle = snippet;
					break;
				}
			}
		}

		searchResult.totalHits = topDocs.totalHits;
	}

	private static void createSearchResult(	final TopScoreDocCollector collector,
											final IndexSearcher searcher,
											final SearchResult searchResult) throws IOException {

		final ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length == 0) {
			return;
		}

		final TreeMap<String, SearchResultItem> searchResultItems = searchResult.items;

		final StringBuilder sqlTourIds = new StringBuilder();

		for (int hitIndex = 0; hitIndex < hits.length; ++hitIndex) {

			final ScoreDoc scoreDoc = hits[hitIndex];

			final int docId = scoreDoc.doc;
			final Document doc = searcher.doc(docId);

			final String tourId = doc.get(SEARCH_FIELD_TOUR_ID);

			final SearchResultItem resultItem = new SearchResultItem();

			resultItem.tourId = tourId;
			resultItem.score = scoreDoc.score;

			searchResultItems.put(tourId, resultItem);

			// Create tour id list for the sql statement
			if (hitIndex > 0) {
				sqlTourIds.append(',');
			}
			sqlTourIds.append(tourId);
		}

		/*
		 * Get tour data for each hit
		 */
		PreparedStatement stmt = null;
		Connection conn = null;

		try {
			conn = TourDatabase.getInstance().getConnection();

			final String sql = "" // //$NON-NLS-1$
					//
					+ "SELECT" //$NON-NLS-1$
					//
					+ " tourId," //				1 //$NON-NLS-1$
					+ " tourTitle," //			2 //$NON-NLS-1$
					+ " tourDescription" //		3 //$NON-NLS-1$
					//
					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA)

					+ (" WHERE tourId IN (" + sqlTourIds + ")")
			//
			;

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			while (rs.next()) {

				final String dbTourId = rs.getString(1);
				final String dbTitle = rs.getString(2);
				final String dbDescription = rs.getString(3);

				final SearchResultItem resultItem = searchResultItems.get(dbTourId);
				if (resultItem != null) {

					resultItem.tourTitle = dbTitle;
					resultItem.tourDescription = dbDescription;
				}
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.closeSql(stmt);
			Util.closeSql(conn);
		}
	}

	private static Analyzer getAnalyzer() throws SQLException {

		final Locale currentLocale = Locale.getDefault();

//		currentLocale = Locale.GERMAN;
//		currentLocale = Locale.ENGLISH;
//
		Analyzer analyzer = SearchUtils.getAnalyzerForLocale(currentLocale);
//		analyzer = new GermanAnalyzer();
//		analyzer = new EnglishAnalyzer();

		analyzer = new StandardAnalyzer(new CharArraySet(0, true));

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

	public static SearchResult search(final String searchText) {

		final SearchResult searchResult = new SearchResult();
		IndexReader indexReader1 = null;

		try {

			final String[] fields = {
					//
					SEARCH_FIELD_TITLE,
					SEARCH_FIELD_DESCRIPTION,
			//
			};

			final int[] maxPassages = { 2, 2 };
			final int hitsPerPage = 10;

////			final MultiReader multiReader = new MultiReader(reader1, reader2);
////			final IndexSearcher multiSearcher = new IndexSearcher(multiReader);
//
//			final TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
//			searcher.search(query, collector);
//			createSearchResult(collector, searcher, searchResult);
//			searchResult.totalHits = collector.getTotalHits();

			final Analyzer analyzer = getAnalyzer();

			final MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, analyzer);
			final Query query = queryParser.parse(searchText);

			final FSDirectory tourDataIndex = openIndex(TourDatabase.TABLE_TOUR_DATA);
			indexReader1 = DirectoryReader.open(tourDataIndex);
			final IndexSearcher searcher = new IndexSearcher(indexReader1);

			final TopDocs topDocs = searcher.search(query, hitsPerPage);

			final MyPostingsHighlighter highlighter = new MyPostingsHighlighter(analyzer);
			final Map<String, String[]> highlights = highlighter.highlightFields(
					fields,
					query,
					searcher,
					topDocs,
					maxPassages);

//			final PostingsHighlighter highlighter = new PostingsHighlighter();
//			final String[] highlights = highlighter.highlight(SEARCH_FIELD_DESCRIPTION, query, searcher, topDocs);

			createSearchResult(highlights, topDocs, searchResult);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		} finally {

			// reader can only be closed when there
			// is no need to access the documents any more.
			try {
				indexReader1.close();
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}

		return searchResult;
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

	private static AnalyzingSuggester setupSuggester() {

		final AnalyzingSuggester suggester[] = new AnalyzingSuggester[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				DirectoryReader indexReader = null;

				try {

					final TermFreqIteratorListWrapper inputIterator = new TermFreqIteratorListWrapper();

					final FSDirectory dir = openIndex(TourDatabase.TABLE_TOUR_DATA);
					indexReader = DirectoryReader.open(dir);

					final List<AtomicReaderContext> leaves = indexReader.leaves();

					for (final AtomicReaderContext readerContext : leaves) {

						final AtomicReader reader = readerContext.reader();
						final Fields fields = reader.fields();

						for (final String field : fields) {

							if (field.equals(SEARCH_FIELD_DESCRIPTION) //
									|| field.equals(SEARCH_FIELD_TITLE)) {

								final Terms terms = fields.terms(field);
								final TermsEnum termsEnum = terms.iterator(null);

								inputIterator.add(termsEnum);
							}
						}
					}

//					final Analyzer analyzer = getAnalyzer();
					final Analyzer analyzer = new StandardAnalyzer(new CharArraySet(0, true));

					suggester[0] = new AnalyzingSuggester(analyzer);
					suggester[0].build(inputIterator);

				} catch (final Exception e) {
					StatusUtil.showStatus(e);
				} finally {
					closeReader(indexReader);
				}
			}
		});

		return suggester[0];
	}
}
