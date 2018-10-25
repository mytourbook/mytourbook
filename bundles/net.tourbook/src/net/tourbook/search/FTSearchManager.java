/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourLogManager;
import net.tourbook.ui.UI;

public class FTSearchManager {

	private static final String					LUCENE_INDEX_FOLDER_NAME		= "lucene-index";							//$NON-NLS-1$

	private static final String					SEARCH_FIELD_DESCRIPTION		= "description";							//$NON-NLS-1$
	private static final String					SEARCH_FIELD_DOC_SOURCE_INDEX	= "docSource_Index";						//$NON-NLS-1$
	private static final String					SEARCH_FIELD_DOC_SOURCE_SAVED	= "docSource_Saved";						//$NON-NLS-1$
	private static final String					SEARCH_FIELD_MARKER_ID			= "markerID";								//$NON-NLS-1$
	private static final String					SEARCH_FIELD_TITLE				= "title";									//$NON-NLS-1$
	private static final String					SEARCH_FIELD_TOUR_ID				= "tourID";									//$NON-NLS-1$
	private static final String					SEARCH_FIELD_TIME					= "time";									//$NON-NLS-1$

	private static final String					LOG_CREATE_INDEX					= "Created ft index: %s\t %d ms";	//$NON-NLS-1$

	static final int									DOC_SOURCE_TOUR					= 1;
	static final int									DOC_SOURCE_TOUR_MARKER			= 2;
	static final int									DOC_SOURCE_WAY_POINT				= 3;

	private static final List<LookupResult>	_emptyProposal						= new ArrayList<>();

	private static Lookup							_suggester;

	private static IndexReader						_indexReader;
	private static IndexSearcher					_indexSearcher;
	private static FSDirectory						_infixStore;

	private static TopDocs							_topDocs;
	private static String							_topDocsSearchText;

	private static boolean							_isShowContentAll;
	private static boolean							_isShowContentMarker;
	private static boolean							_isShowContentTour;
	private static boolean							_isShowContentWaypoint;
	private static boolean							_isSortDateAscending				= false;										// -> sort descending

	private static FieldType						fieldType_Int;
	private static FieldType						fieldType_Long;

	static {

		fieldType_Int = new FieldType(TextField.TYPE_STORED);
		fieldType_Int.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		fieldType_Int.setDocValuesType(DocValuesType.NUMERIC);
		fieldType_Int.freeze();

		fieldType_Long = new FieldType(TextField.TYPE_STORED);
		fieldType_Long.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		fieldType_Long.setDocValuesType(DocValuesType.NUMERIC);
		fieldType_Long.freeze();
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

		private final int				__docCount;
		private final int				__fieldCount;

		private final Set<String>	__fieldsToLoad;
		private ArrayList<String>	__fieldNames;

		private int						__currentDocId			= -1;
		private int						__currentFieldIndex	= -1;
		private Document				__currentDoc;

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

			__fieldNames = new ArrayList<>(__fieldsToLoad);
			__fieldCount = __fieldNames.size();
		}

		@Override
		public Set<BytesRef> contexts() {
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

	private static class FieldWithOptions_Int extends Field {

		public FieldWithOptions_Int(final String fieldName, final FieldType fieldType, final int value) {

			super(fieldName, fieldType);

			fieldsData = Integer.valueOf(value);
		}
	}

	private static class FieldWithOptions_Long extends Field {

		public FieldWithOptions_Long(final String fieldName, final FieldType fieldType, final long value) {

			super(fieldName, fieldType);

			fieldsData = Long.valueOf(value);
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
			try {
				_infixStore.close();
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
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

		doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR_MARKER));
		doc.add(createField_WithIndexOptions_Int(SEARCH_FIELD_DOC_SOURCE_SAVED, DOC_SOURCE_TOUR_MARKER));

		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_MARKER_ID, markerId));
		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TOUR_ID, tourId));
		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TIME, time));

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, createFieldType_Text()));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, createFieldType_Text()));
		}

		indexWriter.addDocument(doc);
	}

	private static void createDoc_Tour(	final IndexWriter indexWriter,
													final long tourId,
													final String title,
													final String description,
													final long time) throws IOException {

		final Document doc = new Document();

		doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR));
		doc.add(createField_WithIndexOptions_Int(SEARCH_FIELD_DOC_SOURCE_SAVED, DOC_SOURCE_TOUR));

		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TOUR_ID, tourId));
		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TIME, time));

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, createFieldType_Text()));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, createFieldType_Text()));
		}

		indexWriter.addDocument(doc);
	}

	private static void createDoc_WayPoint(final IndexWriter indexWriter,
														final long markerId,
														final long tourId,
														final String title,
														final String description,
														final long time) throws IOException {

		final Document doc = new Document();

		doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_WAY_POINT));
		doc.add(createField_WithIndexOptions_Int(SEARCH_FIELD_DOC_SOURCE_SAVED, DOC_SOURCE_WAY_POINT));

		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_MARKER_ID, markerId));
		doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TOUR_ID, tourId));

		if (time != 0) {
			doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TIME, time));
		}

		if (title != null) {
			doc.add(new Field(SEARCH_FIELD_TITLE, title, createFieldType_Text()));
		}

		if (description != null) {
			doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, createFieldType_Text()));
		}

		indexWriter.addDocument(doc);
	}

	private static IndexableField createField_WithIndexOptions_Int(final String fieldName, final int value) {

		return new FieldWithOptions_Int(fieldName, fieldType_Int, value);
	}

	private static IndexableField createField_WithIndexOptions_Long(final String fieldName, final long value) {

		return new FieldWithOptions_Long(fieldName, fieldType_Long, value);
	}

	/**
	 * This field must be created for each document otherwise the highlighter will throw the
	 * exception
	 * <p>
	 * <b>field 'description' was indexed without offsets, cannot highlight</b>
	 *
	 * @return
	 */
	private static FieldType createFieldType_Text() {

		final FieldType fieldType = new FieldType(TextField.TYPE_STORED);
		fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

		return fieldType;
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
			indexWriter = getIndexWriter(indexStore);

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
				try {
					indexStore.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
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
			indexWriter = getIndexWriter(indexStore);

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
				try {
					indexStore.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
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
			indexWriter = getIndexWriter(indexStore);

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
				try {
					indexStore.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
			}

			Util.closeSql(stmt);

			logCreateIndex(tableName, start);
		}
	}

//	private static InputIterator createTermIterator() throws IOException {
//
//		final TermFreqIteratorListWrapper inputIterator = new TermFreqIteratorListWrapper();
//
////		final List<LeafReaderContext> leaves = _indexReader.leaves();
////
////		for (final LeafReaderContext readerContext : leaves) {
////
////			final LeafReader reader = readerContext.reader();
////
////			FieldInfos fieldInfos = reader.getFieldInfos();
////
////			for (FieldInfo fieldInfo : fieldInfos) {
////
////				String fieldName = fieldInfo.name;
////
////				if (fieldName.equals(SEARCH_FIELD_DESCRIPTION) || fieldName.equals(SEARCH_FIELD_TITLE)) {
////
//////					reader.ter
//////
//////
//////					final Terms terms = fields.terms(fieldName);
//////					final TermsEnum termsEnum = terms.iterator(null);
//////
//////					inputIterator.add(termsEnum);
////				}
////			}
////
////
////
//////			final Fields fields = reader.fields();
//////
//////			for (final String field : fields) {
//////
//////				if (field.equals(SEARCH_FIELD_DESCRIPTION) || field.equals(SEARCH_FIELD_TITLE)) {
//////
//////					final Terms terms = fields.terms(field);
//////					final TermsEnum termsEnum = terms.iterator(null);
//////
//////					inputIterator.add(termsEnum);
//////				}
//////			}
////		}
//
//		return inputIterator;
//	}

	private static Analyzer getAnalyzer() {

		Analyzer analyzer = null;

		// try {
		//
		// final Locale currentLocale = Locale.getDefault();
		//
		// // currentLocale = Locale.GERMAN;
		// // currentLocale = Locale.ENGLISH;
		// //
		// // analyzer = SearchUtils.getAnalyzerForLocale(currentLocale);
		// // analyzer = new GermanAnalyzer();
		// // analyzer = new EnglishAnalyzer();
		//
		//
		// } catch (final SQLException e) {
		// StatusUtil.showStatus(e);
		// }

		analyzer = new StandardAnalyzer(new CharArraySet(0, true));

		return analyzer;
	}

	private static IndexWriter getIndexWriter(final FSDirectory indexStore) throws IOException {

		IndexWriter indexWriter = null;

		try {

			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());

		} catch (final IndexFormatTooOldException e) {

			// this occures when an old index exists -> delete index

			TourLogManager.logEx(e);

			final java.nio.file.Path rootPath = getLuceneIndexRootPath();

			TourLogManager.logInfo(String.format("Deleting lucene root folder \"%s\"", rootPath.toString()));

			Files.walkFileTree(rootPath, new SimpleFileVisitor<java.nio.file.Path>() {

				@Override
				public FileVisitResult postVisitDirectory(final java.nio.file.Path dir, final IOException exc) throws IOException {

					Files.delete(dir);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final java.nio.file.Path file, final BasicFileAttributes attrs) throws IOException {

					Files.delete(file);

					return FileVisitResult.CONTINUE;
				}
			});

			TourLogManager.logInfo("Lucene root folder is deleted");

			indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());
		}

		return indexWriter;
	}

	private static IndexWriterConfig getIndexWriterConfig() {

		final Analyzer analyzer = getAnalyzer();

		final IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);

		final boolean IS_DELETE_INDEX = true;

		if (IS_DELETE_INDEX) {

			// delete old index and create a new
			writerConfig.setOpenMode(OpenMode.CREATE);

		} else {

			writerConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		return writerConfig;
	}

	/**
	 * @return Returns the path of the lucene index root folder.
	 */
	private static java.nio.file.Path getLuceneIndexRootPath() {

		final Path osgiDbPath = new Path(TourDatabase.getDatabasePath());
		final IPath osgiIndexPath = osgiDbPath.append(LUCENE_INDEX_FOLDER_NAME);

		// convert osgi to java path
		final String indexFolderName = osgiIndexPath.toOSString();
		final java.nio.file.Path javaRootPath = Paths.get(indexFolderName);

		return javaRootPath;
	}

	static List<LookupResult> getProposals(final String contents) {

		try {

			if (_suggester == null) {
				setupSuggester();
			}

			if (_indexReader.numDocs() == 0) {

				// Suggester for 0 documents causes an exception
				return null;
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
			indexWriter = getIndexWriter(indexStore);

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
				try {
					indexStore.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
			}
		}
//		try {
//
//		} catch (final IndexFormatTooOldException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			return false;
//		}

		return false;
	}

	private static void logCreateIndex(final String indexStore, final long start) {

		StatusUtil.log(String.format(LOG_CREATE_INDEX, //
				indexStore,
				System.currentTimeMillis() - start));
	}

	private static FSDirectory openStore(final String tableName) throws IOException {

		// append table name to the root path
		final java.nio.file.Path javaPath = getLuceneIndexRootPath().resolve(tableName);

		final FSDirectory indexDirectory = FSDirectory.open(javaPath);

		return indexDirectory;
	}

	/**
	 * @param searchText
	 * @param searchFrom
	 * @param searchTo
	 * @param searchResult
	 * @return
	 */
	private static void search(final String searchText,
										final int searchFrom,
										final int searchTo,
										final SearchResult searchResult) {

		try {

			setupIndexReader();

			final int maxDoc = _indexReader.maxDoc();

			if (maxDoc == 0) {

				// there are 0 documents

				searchResult.totalHits = 0;

				return;
			}

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

			final Query textQuery = queryParser.parse(searchText);

			if (_topDocsSearchText == null || _topDocsSearchText.equals(searchText) == false || true) {

				// this is a new search

				/*
				 * Set sorting
				 */
				final SortField sortByTime = new SortField(SEARCH_FIELD_TIME, Type.LONG, _isSortDateAscending == false);
				final Sort sort = new Sort(sortByTime);

				if (_isShowContentAll) {

					// no filtering
					_topDocs = _indexSearcher.search(textQuery, maxDoc, sort);

				} else {

					// filter by content

					/*
					 * Query text/marker/waypoint with OR
					 */
					final Builder orQueryBuilder = new BooleanQuery.Builder();

					if (_isShowContentTour) {

						final Query query = IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR);

						orQueryBuilder.add(query, Occur.SHOULD);
					}

					if (_isShowContentMarker) {

						final Query query = IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR_MARKER);

						orQueryBuilder.add(query, Occur.SHOULD);
					}

					if (_isShowContentWaypoint) {

						final Query query = IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_WAY_POINT);

						orQueryBuilder.add(query, Occur.SHOULD);
					}

					final BooleanQuery orQuery = orQueryBuilder.build();

					final Builder andQueryBuilder = new BooleanQuery.Builder()

							// add search text
							.add(textQuery, Occur.MUST)

							// add tour text/marker/waypoint
							.add(orQuery, Occur.MUST)

					;

					final BooleanQuery andQuery = andQueryBuilder.build();

					_topDocs = _indexSearcher.search(andQuery, maxDoc, sort);
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

			final UnifiedHighlighter highlighter = new UnifiedHighlighter(_indexSearcher, getAnalyzer());

			final Map<String, String[]> highlights = highlighter.highlightFields(
					queryFields,
					textQuery,
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
	private static void search_CreateResult(	final Map<String, String[]> highlights,
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
		final Set<String> fieldsToLoad = new HashSet<>();
		fieldsToLoad.add(SEARCH_FIELD_DOC_SOURCE_SAVED);
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
					// resultItem.score = scoreDocs[docStartIndex + hitIndex].score;

					for (final IndexableField indexField : doc.getFields()) {

						final String docFieldName = indexField.name();

						switch (docFieldName) {
						case SEARCH_FIELD_DOC_SOURCE_SAVED:
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

			// _suggester = setupSuggester_Analyzing();
			// _suggester = setupSuggester_AnalyzingInfix();
			// _suggester = setupSuggester_NGramAnalyzing();

			_suggester = setupSuggester_FreeText();
		}
	}

//	private static Lookup setupSuggester_Analyzing() {
//
//		setupIndexReader();
//
//		final Lookup suggester[] = new AnalyzingSuggester[1];
//
////		Display.getDefault().syncExec(new Runnable() {
////			@Override
////			public void run() {
////
////				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
////					@Override
////					public void run() {
////
////						try {
////
////							final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));
////							final InputIterator termIterator = createTermIterator();
////
////							suggester[0] = new AnalyzingSuggester(queryAnalyzer);
////							suggester[0].build(termIterator);
////
////						} catch (final Exception e) {
////							StatusUtil.showStatus(e);
////						}
////					}
////				});
////			}
////		});
//
//		return suggester[0];
//	}
//
//	private static Lookup setupSuggester_AnalyzingInfix() {
//
//		setupIndexReader();
//
//		final Lookup suggester[] = new AnalyzingInfixSuggester[1];
//
////		Display.getDefault().syncExec(new Runnable() {
////			@Override
////			public void run() {
////
////				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
////
////					@Override
////					public void run() {
////
////						try {
////
////							final DocumentInputIterator inputIterator = new DocumentInputIterator(_indexReader);
////							final Analyzer indexAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));
////							final Analyzer queryAnalyzer = new WhitespaceAnalyzer();
////
////							_infixStore = openStore("AnalyzingInfixSuggesterSTORE"); //$NON-NLS-1$
////
////							suggester[0] = new AnalyzingInfixSuggester(
////									LUCENE_VERSION,
////									_infixStore,
////									indexAnalyzer,
////									queryAnalyzer,
////									2);
////
////							suggester[0].build(inputIterator);
////
////						} catch (final Exception e) {
////							StatusUtil.showStatus(e);
////						}
////					}
////				});
////			}
////		});
//
//		return suggester[0];
//	}

	private static Lookup setupSuggester_FreeText() {

		setupIndexReader();

		final int numDocs = _indexReader.numDocs();
		if (numDocs == 0) {

			/*
			 * Suggester for 0 documents causes an exception
			 */

			return null;
		}

		final Lookup suggester[] = new FreeTextSuggester[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
					@Override
					public void run() {

						try {

							final DocumentInputIterator inputIterator = new DocumentInputIterator(_indexReader);

							final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));

							suggester[0] = new FreeTextSuggester(queryAnalyzer, queryAnalyzer, 4, (byte) 0x20);

							try {
								suggester[0].build(inputIterator);
							} catch (final IllegalArgumentException e) {

								// java.lang.IllegalArgumentException: need at least one suggestion

								/*
								 * This exception can occure when there are documents available but do not
								 * contain any content which the suggester can use.
								 */
							}

						} catch (final Exception e) {
							StatusUtil.showStatus(e);
						}
					}
				});
			}
		});

		return suggester[0];
	}

//	private static Lookup setupSuggester_NGramAnalyzing() {
//
//		setupIndexReader();
//
//		final Lookup suggester[] = new AnalyzingSuggester[1];
//
//		try {
//
//			// static {
//			//
//			// analyzer = new Analyzer() {
//			//
//			// @Override
////						public TokenStream tokenStream(final String fieldName, final Reader reader) {
//			//
//			// TokenStream result = new StandardTokenizer(reader);
//			//
//			// result = new StandardFilter(result);
//			// result = new LowerCaseFilter(result);
//			// result = new ISOLatin1AccentFilter(result);
//			// result = new StopFilter(result, ENGLISH_STOP_WORDS);
//			// result = new EdgeNGramTokenFilter(result, Side.FRONT, 1, 20);
//			//
//			// return result;
//			// }
//			// };
//			//
////					autocompletionAnalyzer = new AnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {
//			//
//			// @Override
//			// protected Analyzer getWrappedAnalyzer(final String fieldName) {
//			// return analyzer;
//			// }
//			//
//			// @Override
////						protected TokenStreamComponents wrapComponents(	final String fieldName,
//			// final TokenStreamComponents components) {
//			//
////							final NGramTokenFilter filter = new NGramTokenFilter(components.getTokenStream(), 2, 100);
//			// final Tokenizer tokenizer = components.getTokenizer();
//			//
//			// return new TokenStreamComponents(tokenizer, filter);
//			// }
//			// };
//			//
////					newInfixSuggester = new AnalyzingSuggester(autocompletionAnalyzer, analyzer);
//			// }
//
//		} catch (final Exception e) {
//			StatusUtil.showStatus(e);
//		}
//
//		return suggester[0];
//	}

	public static void updateIndex(final ArrayList<TourData> modifiedTours) {

		// setupIndexReader();
		//
		// FSDirectory indexStore = null;
		// final PreparedStatement stmt = null;
		// IndexWriter indexWriter = null;
		//
		// try {
		//
		// indexStore = openStore(TourDatabase.TABLE_TOUR_DATA);
		// indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());
		//
		// } catch (final IOException e) {
		// StatusUtil.showStatus(e);
		// } finally {
		//
		// if (indexWriter != null) {
		// try {
		// indexWriter.close();
		// } catch (final IOException e) {
		// StatusUtil.showStatus(e);
		// }
		// }
		//
		// if (indexStore != null) {
		// indexStore.close();
		// }
		//
		// Util.closeSql(stmt);
		// }

	}
}
