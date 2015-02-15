/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
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
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class FTSearchManager {

	private static final Version			LUCENE_VERSION				= Version.LUCENE_4_10_1;

	private static final String				LUCENE_INDEX_FOLDER_NAME	= "lucene-index";						//$NON-NLS-1$

	private static final String				SEARCH_FIELD_DESCRIPTION	= "description";						//$NON-NLS-1$
	private static final String				SEARCH_FIELD_DOC_SOURCE		= "docSource";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_MARKER_ID		= "markerID";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TITLE			= "title";								//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TOUR_ID		= "tourID";							//$NON-NLS-1$
	private static final String				SEARCH_FIELD_TIME			= "time";								//$NON-NLS-1$

	private static final String				LOG_CREATE_INDEX			= "Created ft index: %s\t %d ms";		//$NON-NLS-1$

	static final int						DOC_SOURCE_TOUR				= 1;
	static final int						DOC_SOURCE_TOUR_MARKER		= 2;
	static final int						DOC_SOURCE_WAY_POINT		= 3;

	private static final List<LookupResult>	_emptyProposal				= new ArrayList<LookupResult>();

	private static Lookup					_suggester;
	private static IndexReader				_indexReader;
	private static IndexSearcher			_indexSearcher;
	private static FSDirectory				_infixStore;

	private static TopDocs					_topDocs;
	private static String					_topDocsSearchText;

	private static boolean					_isShowContentAll;
	private static boolean					_isShowContentMarker;
	private static boolean					_isShowContentTour;
	private static boolean					_isShowContentWaypoint;
	private static boolean					_isSortDateAscending		= false;								// -> sort descending

	// configure field with offsets at index time
	private static final FieldType			_longSearchField			= new FieldType(LongField.TYPE_STORED);
	private static final FieldType			_textSearchField			= new FieldType(TextField.TYPE_STORED);
	{
		_longSearchField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		_textSearchField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	}

	/**
	 * Implements {@link InputIterator} from multiple stored fields.
	 * <p>
	 * Copied and modified from
	 * {@link org.apache.lucene.search.suggest.DocumentDictionary.DocumentInputIterator.DocumentInputIterator}
	 */
	private static class DocumentInputIterator implements InputIterator {

		private IndexReader			__indexReader;
		private final Bits			__liveDocs;

		private final int			__docCount;
		private final int			__fieldCount;

		private final Set<String>	__fieldsToLoad;
		private ArrayList<String>	__fieldNames;

		private int					__currentDocId		= -1;
		private int					__currentFieldIndex	= -1;
		private Document			__currentDoc;

		/**
		 * Creates an iterator over fields from the lucene index.
		 * 
		 * @param indexReader
		 */
		public DocumentInputIterator(final IndexReader indexReader) throws IOException {

			__indexReader = indexReader;
			__docCount = __indexReader.maxDoc() - 1;

			__liveDocs = (__indexReader.leaves().size() > 0) ? MultiFields.getLiveDocs(__indexReader) : null;

			__fieldsToLoad = new HashSet<>();
			__fieldsToLoad.add(SEARCH_FIELD_TITLE);
			__fieldsToLoad.add(SEARCH_FIELD_DESCRIPTION);

			__fieldNames = new ArrayList<String>(__fieldsToLoad);
			__fieldCount = __fieldNames.size();
		}

		@Override
		public Set<BytesRef> contexts() {
			return null;
		}

		@Override
		public Comparator<BytesRef> getComparator() {
			return null;
		}

		@Override
		public boolean hasContexts() {
			return false;
		}

		@Override
		public boolean hasPayloads() {
			return false;
		}

		@Override
		public BytesRef next() throws IOException {

			while (__currentDocId < __docCount) {

				if (__currentFieldIndex == -1) {

					// get a new document

					__currentDocId++;

					if (__liveDocs != null && !__liveDocs.get(__currentDocId)) {
						continue;
					}

					__currentDoc = __indexReader.document(__currentDocId, __fieldsToLoad);
				}

				while (__currentFieldIndex < __fieldCount - 1) {

					__currentFieldIndex++;

					final String fieldName = __fieldNames.get(__currentFieldIndex);

					final IndexableField fieldVal = __currentDoc.getField(fieldName);

					if (fieldVal == null || (fieldVal.binaryValue() == null && fieldVal.stringValue() == null)) {
						continue;
					}

					final BytesRef tempFieldValue = fieldVal.stringValue() != null //
							? new BytesRef(fieldVal.stringValue())
							: fieldVal.binaryValue();

					return tempFieldValue;
				}

				__currentFieldIndex = -1;
			}

			return null;
		}

		@Override
		public BytesRef payload() {
			return null;
		}

		@Override
		public long weight() {
			return 1;
		}
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

	public static void close() {

		if (_suggester instanceof AnalyzingInfixSuggester) {
			try {
				final AnalyzingInfixSuggester suggester = (AnalyzingInfixSuggester) _suggester;
				suggester.close();
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}
		_suggester = null;

		if (_indexReader != null) {
			try {
				_indexReader.close();
				_indexReader = null;
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}

		if (_infixStore != null) {
			_infixStore.close();
			_infixStore = null;
		}

	}

	private static void createDoc_Marker(	final IndexWriter indexWriter,
											final long markerId,
											final long tourId,
											final String title,
											final String description,
											final long time) throws IOException {

		final Document doc = new Document();

		doc.add(new IntField(SEARCH_FIELD_DOC_SOURCE, DOC_SOURCE_TOUR_MARKER, Store.YES));

		doc.add(new LongField(SEARCH_FIELD_MARKER_ID, markerId, Store.YES));
		doc.add(new LongField(SEARCH_FIELD_TOUR_ID, tourId, Store.YES));
		doc.add(new LongField(SEARCH_FIELD_TIME, time, _longSearchField));

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, _textSearchField));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, _textSearchField));
		}

		indexWriter.addDocument(doc);
	}

	private static void createDoc_Tour(	final IndexWriter indexWriter,
										final long tourId,
										final String title,
										final String description,
										final long time) throws IOException {

		final Document doc = new Document();

		doc.add(new IntField(SEARCH_FIELD_DOC_SOURCE, DOC_SOURCE_TOUR, Store.YES));

		doc.add(new LongField(SEARCH_FIELD_TOUR_ID, tourId, Store.YES));
		doc.add(new LongField(SEARCH_FIELD_TIME, time, _longSearchField));

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, _textSearchField));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, _textSearchField));
		}

		indexWriter.addDocument(doc);
	}

	private static void createDoc_WayPoint(	final IndexWriter indexWriter,
											final long markerId,
											final long tourId,
											final String title,
											final String description,
											final long time) throws IOException {

		final Document doc = new Document();

		doc.add(new IntField(SEARCH_FIELD_DOC_SOURCE, DOC_SOURCE_WAY_POINT, Store.YES));

		doc.add(new LongField(SEARCH_FIELD_MARKER_ID, markerId, Store.YES));
		doc.add(new LongField(SEARCH_FIELD_TOUR_ID, tourId, Store.YES));

		if (time != 0) {
			doc.add(new LongField(SEARCH_FIELD_TIME, time, _longSearchField));
		}

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, _textSearchField));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, _textSearchField));
		}

		indexWriter.addDocument(doc);
	}

	private static void createStores_TourData(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final long start = System.currentTimeMillis();

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;

		final String tableName = TourDatabase.TABLE_TOUR_DATA;

		try {

			indexStore = openStore(tableName);
			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());

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
					+ (" FROM " + tableName); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final long dbTourId = rs.getLong(1);
				final String dbTitle = rs.getString(2);
				final String dbDescription = rs.getString(3);
				final Long dbTourStartTime = rs.getLong(4);

				createDoc_Tour(indexWriter, dbTourId, dbTitle, dbDescription, dbTourStartTime);

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

			logCreateIndex(tableName, start);
		}
	}

	private static void createStores_TourMarker(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final long start = System.currentTimeMillis();

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;

		final String tableName = TourDatabase.TABLE_TOUR_MARKER;

		try {

			indexStore = openStore(tableName);
			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());

			/*
			 * Get sql data
			 */
			final String sql = "" // //$NON-NLS-1$
					//
					+ "SELECT" //$NON-NLS-1$
					//
					+ " markerId," //						1 //$NON-NLS-1$
					+ (TourDatabase.KEY_TOUR + ",") //		2 //$NON-NLS-1$
					+ " label," //							3 //$NON-NLS-1$
					+ " description," //					4 //$NON-NLS-1$
					+ " tourTime" //						5 //$NON-NLS-1$
					//
					+ (" FROM " + tableName); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final long dbMarkerId = rs.getLong(1);
				final long dbTourId = rs.getLong(2);
				final String dbLabel = rs.getString(3);
				final String dbDescription = rs.getString(4);
				final long dbTourTime = rs.getLong(5);

				createDoc_Marker(indexWriter, dbMarkerId, dbTourId, dbLabel, dbDescription, dbTourTime);

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

			logCreateIndex(tableName, start);
		}
	}

	private static void createStores_TourWayPoint(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final long start = System.currentTimeMillis();

		final String tableName = TourDatabase.TABLE_TOUR_WAYPOINT;

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;
		PreparedStatement stmt = null;

		try {

			indexStore = openStore(tableName);
			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());

			/*
			 * Get sql data
			 */
			final String sql = "" // //$NON-NLS-1$
					//
					+ "SELECT" //$NON-NLS-1$
					//
					+ (" " + TourDatabase.ENTITY_ID_WAY_POINT + ",") //		1 //$NON-NLS-1$ //$NON-NLS-2$
					+ (TourDatabase.KEY_TOUR + ",") //						2 //$NON-NLS-1$
					+ " name," //											3 //$NON-NLS-1$
					+ " description," //									4 //$NON-NLS-1$
					+ " time" //											5 //$NON-NLS-1$
					//
					+ (" FROM " + tableName); //$NON-NLS-1$

			stmt = conn.prepareStatement(sql);
			final ResultSet rs = stmt.executeQuery();

			int createdDocuments = 0;
			long lastUpdateTime = System.currentTimeMillis();

			while (rs.next()) {

				final long dbMarkerId = rs.getLong(1);
				final long dbTourId = rs.getLong(2);
				final String dbLabel = rs.getString(3);
				final String dbDescription = rs.getString(4);
				final long dbTourTime = rs.getLong(5);

				createDoc_WayPoint(indexWriter, dbMarkerId, dbTourId, dbLabel, dbDescription, dbTourTime);

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

			logCreateIndex(tableName, start);
		}
	}

	private static InputIterator createTermIterator() throws IOException {

		final TermFreqIteratorListWrapper inputIterator = new TermFreqIteratorListWrapper();

		final List<AtomicReaderContext> leaves = _indexReader.leaves();

		for (final AtomicReaderContext readerContext : leaves) {

			final AtomicReader reader = readerContext.reader();
			final Fields fields = reader.fields();

			for (final String field : fields) {

				if (field.equals(SEARCH_FIELD_DESCRIPTION) || field.equals(SEARCH_FIELD_TITLE)) {

					final Terms terms = fields.terms(field);
					final TermsEnum termsEnum = terms.iterator(null);

					inputIterator.add(termsEnum);
				}
			}
		}

		return inputIterator;
	}

	private static Analyzer getAnalyzer() {

		Analyzer analyzer = null;

//		try {
//
//			final Locale currentLocale = Locale.getDefault();
//
////			currentLocale = Locale.GERMAN;
////			currentLocale = Locale.ENGLISH;
////
////			analyzer = SearchUtils.getAnalyzerForLocale(currentLocale);
////			analyzer = new GermanAnalyzer();
////			analyzer = new EnglishAnalyzer();
//
//
//		} catch (final SQLException e) {
//			StatusUtil.showStatus(e);
//		}

		analyzer = new StandardAnalyzer(new CharArraySet(0, true));

		return analyzer;
	}

	private static IndexWriterConfig getIndexWriterConfig() {

		final Analyzer analyzer = getAnalyzer();

		final IndexWriterConfig writerConfig = new IndexWriterConfig(LUCENE_VERSION, analyzer);

		final boolean IS_DELETE_INDEX = true;

		if (IS_DELETE_INDEX) {
			// delete old index and create a new
			writerConfig.setOpenMode(OpenMode.CREATE);
		} else {
			writerConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		return writerConfig;
	}

	static List<LookupResult> getProposals(final String contents) {

		try {

			if (_suggester == null) {
				setupSuggester();
			}

			final List<LookupResult> suggestions = _suggester.lookup(contents, false, 10000);

			return suggestions;

		} catch (final Exception e) {
			return _emptyProposal;
		}
	}

	/**
	 * @return Returns <code>true</code> when the ft index is created.
	 */
	private static boolean isIndexCreated() {

		FSDirectory indexStore = null;
		IndexWriter indexWriter = null;

		try {

			indexStore = openStore(TourDatabase.TABLE_TOUR_DATA);
			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());

			// check if index is already created
			if (indexWriter.numDocs() > 0) {
				return true;
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
		}

		return false;
	}

	private static void logCreateIndex(final String indexStore, final long start) {

		StatusUtil.log(String.format(LOG_CREATE_INDEX, //
				indexStore,
				System.currentTimeMillis() - start));
	}

	private static FSDirectory openStore(final String tableName) throws IOException {

		final Path dbPath = new Path(TourDatabase.getDatabasePath());

		final IPath luceneIndexPath = dbPath.append(LUCENE_INDEX_FOLDER_NAME).append(tableName);
		final String indexFolder = luceneIndexPath.toOSString();

		final FSDirectory indexDirectory = FSDirectory.open(new File(indexFolder));

		return indexDirectory;
	}

	/**
	 * @param searchText
	 * @param searchFrom
	 * @param searchTo
	 * @param searchResult
	 * @return
	 */
	private static void search(	final String searchText,
								final int searchFrom,
								final int searchTo,
								final SearchResult searchResult) {

		try {

			setupIndexReader();

			final String[] queryFields = {
					//
					SEARCH_FIELD_TITLE,
					SEARCH_FIELD_DESCRIPTION,
			//
			};

			final int maxPassages[] = new int[queryFields.length];
			Arrays.fill(maxPassages, 1);

			final Analyzer analyzer = getAnalyzer();

			final MultiFieldQueryParser queryParser = new MultiFieldQueryParser(queryFields, analyzer);
			queryParser.setAllowLeadingWildcard(true);

			final Query query = queryParser.parse(searchText);

			if (_topDocsSearchText == null || _topDocsSearchText.equals(searchText) == false || true) {

				// this is a new search

				/*
				 * Set sorting
				 */
				final SortField sortByTime = new SortField(SEARCH_FIELD_TIME, Type.LONG, _isSortDateAscending == false);
				final Sort sort = new Sort(sortByTime);

				if (_isShowContentAll) {

					// no filtering
					_topDocs = _indexSearcher.search(query, _indexReader.maxDoc(), sort);

				} else {

					// filter by content

					final BooleanFilter searchFilter = new BooleanFilter();

					if (_isShowContentMarker) {

						final NumericRangeFilter<Integer> filter = NumericRangeFilter.newIntRange(
								SEARCH_FIELD_DOC_SOURCE,
								DOC_SOURCE_TOUR_MARKER,
								DOC_SOURCE_TOUR_MARKER,
								true,
								true);

						searchFilter.add(new FilterClause(filter, Occur.SHOULD));
					}

					if (_isShowContentTour) {

						final NumericRangeFilter<Integer> filter = NumericRangeFilter.newIntRange(
								SEARCH_FIELD_DOC_SOURCE,
								DOC_SOURCE_TOUR,
								DOC_SOURCE_TOUR,
								true,
								true);

						searchFilter.add(new FilterClause(filter, Occur.SHOULD));
					}

					if (_isShowContentWaypoint) {

						final NumericRangeFilter<Integer> filter = NumericRangeFilter.newIntRange(
								SEARCH_FIELD_DOC_SOURCE,
								DOC_SOURCE_WAY_POINT,
								DOC_SOURCE_WAY_POINT,
								true,
								true);

						searchFilter.add(new FilterClause(filter, Occur.SHOULD));
					}

					_topDocs = _indexSearcher.search(query, searchFilter, _indexReader.maxDoc(), sort);
				}

				_topDocsSearchText = searchText;
			}

			searchResult.totalHits = _topDocs.totalHits;

			/**
			 * Get doc id's only for the current page.
			 * <p>
			 * It is very cheap to query the doc id's but very expensive to retrieve the documents.
			 */
			final int docStartIndex = searchFrom;
			int docEndIndex = searchTo;

			final ScoreDoc[] scoreDocs = _topDocs.scoreDocs;
			final int scoreSize = scoreDocs.length;

			if (docEndIndex >= scoreSize) {
				docEndIndex = scoreSize - 1;
			}

			final int resultSize = docEndIndex - docStartIndex + 1;
			final int docids[] = new int[resultSize];

			for (int docIndex = 0; docIndex < resultSize; docIndex++) {
				docids[docIndex] = scoreDocs[docStartIndex + docIndex].doc;
			}

			// this can occure: field 'description' was indexed without offsets, cannot highlight

			final MyPostingsHighlighter highlighter = new MyPostingsHighlighter();
			final Map<String, String[]> highlights = highlighter.highlightFields(
					queryFields,
					query,
					_indexSearcher,
					docids,
					maxPassages);

			search_CreateResult(highlights, _indexReader, searchResult, docids, docStartIndex);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
			searchResult.error = e.getMessage();
		}
	}

	/**
	 * Creating the result is complicated because the highlights are listed by field and not by hit,
	 * therefor the structure must be inverted.
	 * 
	 * @param highlights
	 * @param indexReader
	 * @param searchResult
	 * @param docStartIndex
	 * @param docids2
	 * @throws IOException
	 */
	private static void search_CreateResult(final Map<String, String[]> highlights,
											final IndexReader indexReader,
											final SearchResult searchResult,
											final int[] docids,
											final int docStartIndex) throws IOException {

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
		fieldsToLoad.add(SEARCH_FIELD_DOC_SOURCE);
		fieldsToLoad.add(SEARCH_FIELD_TOUR_ID);
		fieldsToLoad.add(SEARCH_FIELD_MARKER_ID);
		fieldsToLoad.add(SEARCH_FIELD_TIME);

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
					resultItem.title = snippet;
					break;
				}

				if (isDocRead == false) {

					final int docId = docids[hitIndex];
					final Document doc = indexReader.document(docId, fieldsToLoad);

					resultItem.docId = docId;
//					resultItem.score = scoreDocs[docStartIndex + hitIndex].score;

					for (final IndexableField indexField : doc.getFields()) {

						final String docFieldName = indexField.name();

						switch (docFieldName) {
						case SEARCH_FIELD_DOC_SOURCE:
							resultItem.docSource = indexField.numericValue().intValue();
							break;

						case SEARCH_FIELD_TOUR_ID:
							resultItem.tourId = indexField.stringValue();
							break;

						case SEARCH_FIELD_MARKER_ID:
							resultItem.markerId = indexField.stringValue();
							break;

						case SEARCH_FIELD_TIME:
							resultItem.tourStartTime = indexField.numericValue().longValue();
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
	 * @param searchText
	 * @param pageNumber
	 *            Starting from 0.
	 * @param hitsPerPage
	 * @return
	 */
	public static SearchResult searchByPage(final String searchText, final int pageNumber, final int hitsPerPage) {

		final int searchPosFrom = pageNumber * hitsPerPage;
		final int searchPosTo = (pageNumber + 1) * hitsPerPage - 1;

		final SearchResult searchResult = new SearchResult();

		searchResult.pageNumber = pageNumber;
		searchResult.hitsPerPage = hitsPerPage;

		search(searchText, searchPosFrom, searchPosTo, searchResult);

		return searchResult;
	}

	public static SearchResult searchByPosition(final String searchText, final int searchPosFrom, final int searchPosTo) {

		final SearchResult searchResult = new SearchResult();

		search(searchText, searchPosFrom, searchPosTo, searchResult);

		return searchResult;
	}

	static void setSearchOptions(	final boolean isShowContentAll,
									final boolean isShowContentMarker,
									final boolean isShowContentTour,
									final boolean isShowContentWaypoint,
									final boolean isSortDateAscending) {

		_isShowContentAll = isShowContentAll;
		_isShowContentMarker = isShowContentMarker;
		_isShowContentTour = isShowContentTour;
		_isShowContentWaypoint = isShowContentWaypoint;

		_isSortDateAscending = isSortDateAscending;
	}

	/**
	 * Create FT index.
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private static void setupIndex() {

		if (isIndexCreated()) {
			return;
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				try {

					final IRunnableWithProgress runnable = new IRunnableWithProgress() {

						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							monitor.subTask(Messages.Database_Monitor_SetupLucene);

							Connection conn = null;

							try {

								conn = TourDatabase.getInstance().getConnection();

								createStores_TourData(conn, monitor);
								createStores_TourMarker(conn, monitor);
								createStores_TourWayPoint(conn, monitor);

							} catch (final SQLException e) {
								UI.showSQLException(e);
							} finally {
								Util.closeSql(conn);
							}
						}
					};

					new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

				} catch (final InvocationTargetException e) {
					StatusUtil.log(e);
				} catch (final InterruptedException e) {
					StatusUtil.log(e);
				}
			}
		});
	}

	/**
	 * Once you have a new IndexReader, it's relatively cheap to create a new IndexSearcher from it.
	 */
	private static void setupIndexReader() {

		if (_indexReader != null) {
			// index reader is initialized
			return;
		}

		setupIndex();

		IndexReader indexReader1 = null;
		IndexReader indexReader2 = null;
		IndexReader indexReader3 = null;

		try {

			final FSDirectory tourDataIndex = openStore(TourDatabase.TABLE_TOUR_DATA);
			final FSDirectory tourMarkerIndex = openStore(TourDatabase.TABLE_TOUR_MARKER);
			final FSDirectory tourWayPoint = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

			indexReader1 = DirectoryReader.open(tourDataIndex);
			indexReader2 = DirectoryReader.open(tourMarkerIndex);
			indexReader3 = DirectoryReader.open(tourWayPoint);

			_indexReader = new MultiReader(indexReader1, indexReader2, indexReader3);

			_indexSearcher = new IndexSearcher(_indexReader);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}
	}

	public static void setupSuggester() {

		if (_suggester == null) {

//			_suggester = setupSuggester_Analyzing();
//			_suggester = setupSuggester_AnalyzingInfix();
//			_suggester = setupSuggester_NGramAnalyzing();
			_suggester = setupSuggester_FreeText();
		}
	}

	private static Lookup setupSuggester_Analyzing() {

		setupIndexReader();

		final Lookup suggester[] = new AnalyzingSuggester[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
					@Override
					public void run() {

						try {

							final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));
							final InputIterator termIterator = createTermIterator();

							suggester[0] = new AnalyzingSuggester(queryAnalyzer);
							suggester[0].build(termIterator);

						} catch (final Exception e) {
							StatusUtil.showStatus(e);
						}
					}
				});
			}
		});

		return suggester[0];
	}

	private static Lookup setupSuggester_AnalyzingInfix() {

		setupIndexReader();

		final Lookup suggester[] = new AnalyzingInfixSuggester[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {

					@Override
					public void run() {

						try {

							final DocumentInputIterator inputIterator = new DocumentInputIterator(_indexReader);
							final Analyzer indexAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));
							final Analyzer queryAnalyzer = new WhitespaceAnalyzer();

							_infixStore = openStore("AnalyzingInfixSuggesterSTORE");

							suggester[0] = new AnalyzingInfixSuggester(
									LUCENE_VERSION,
									_infixStore,
									indexAnalyzer,
									queryAnalyzer,
									2);

							suggester[0].build(inputIterator);

						} catch (final Exception e) {
							StatusUtil.showStatus(e);
						}
					}
				});
			}
		});

		return suggester[0];
	}

	private static Lookup setupSuggester_FreeText() {

		setupIndexReader();

		final Lookup suggester[] = new FreeTextSuggester[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
					@Override
					public void run() {

						try {

							final DocumentInputIterator inputIterator = new DocumentInputIterator(_indexReader);
//							final Analyzer queryAnalyzer = new WhitespaceAnalyzer();
							final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));

							suggester[0] = new FreeTextSuggester(queryAnalyzer, queryAnalyzer, 4, (byte) 0x20);
							suggester[0].build(inputIterator);

						} catch (final Exception e) {
							StatusUtil.showStatus(e);
						}
					}
				});
			}
		});

		return suggester[0];
	}

	private static Lookup setupSuggester_NGramAnalyzing() {

		setupIndexReader();

		final Lookup suggester[] = new AnalyzingSuggester[1];

		try {

//			static {
//
//					analyzer = new Analyzer() {
//
//						@Override
//						public TokenStream tokenStream(final String fieldName, final Reader reader) {
//
//							TokenStream result = new StandardTokenizer(reader);
//
//							result = new StandardFilter(result);
//							result = new LowerCaseFilter(result);
//							result = new ISOLatin1AccentFilter(result);
//							result = new StopFilter(result, ENGLISH_STOP_WORDS);
//							result = new EdgeNGramTokenFilter(result, Side.FRONT, 1, 20);
//
//							return result;
//						}
//					};
//
//					autocompletionAnalyzer = new AnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {
//
//						@Override
//						protected Analyzer getWrappedAnalyzer(final String fieldName) {
//							return analyzer;
//						}
//
//						@Override
//						protected TokenStreamComponents wrapComponents(	final String fieldName,
//																		final TokenStreamComponents components) {
//
//							final NGramTokenFilter filter = new NGramTokenFilter(components.getTokenStream(), 2, 100);
//							final Tokenizer tokenizer = components.getTokenizer();
//
//							return new TokenStreamComponents(tokenizer, filter);
//						}
//					};
//
//					newInfixSuggester = new AnalyzingSuggester(autocompletionAnalyzer, analyzer);
//				}

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}

		return suggester[0];
	}

	public static void updateIndex(final ArrayList<TourData> modifiedTours) {

//		setupIndexReader();
//
//		FSDirectory indexStore = null;
//		final PreparedStatement stmt = null;
//		IndexWriter indexWriter = null;
//
//		try {
//
//			indexStore = openStore(TourDatabase.TABLE_TOUR_DATA);
//			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());
//
//		} catch (final IOException e) {
//			StatusUtil.showStatus(e);
//		} finally {
//
//			if (indexWriter != null) {
//				try {
//					indexWriter.close();
//				} catch (final IOException e) {
//					StatusUtil.showStatus(e);
//				}
//			}
//
//			if (indexStore != null) {
//				indexStore.close();
//			}
//
//			Util.closeSql(stmt);
//		}

	}
}
