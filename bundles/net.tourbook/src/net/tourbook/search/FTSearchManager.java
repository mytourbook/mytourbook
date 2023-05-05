/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.CSS;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;

import org.apache.commons.collections4.ListUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
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
import org.apache.lucene.queryparser.classic.ParseException;
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
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class FTSearchManager {

   private static final String                  LUCENE_INDEX_FOLDER_NAME         = "lucene-index";                 //$NON-NLS-1$

   private static final String                  SEARCH_FIELD_DESCRIPTION         = "description";                  //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_DOC_SOURCE_INDEX    = "docSource_Index";              //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_DOC_SOURCE_SAVED    = "docSource_Saved";              //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_MARKER_ID           = "markerID";                     //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TITLE               = "title";                        //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TOUR_ID             = "tourID";                       //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TOUR_LOCATION_START = "startLocation";                //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TOUR_LOCATION_END   = "endLocation";                  //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TOUR_WEATHER        = "weather";                      //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_TIME                = "time";                         //$NON-NLS-1$
   private static final String                  SEARCH_FIELD_WAYPOINT_ID         = "wayPointID";                   //$NON-NLS-1$

   private static final String                  LOG_CREATE_INDEX                 = "Created ft index: %s\t %d ms"; //$NON-NLS-1$

   private static final IPreferenceStore        _prefStore                       = TourbookPlugin.getPrefStore();

   static final int                             DOC_SOURCE_TOUR                  = 1;
   static final int                             DOC_SOURCE_TOUR_MARKER           = 2;
   static final int                             DOC_SOURCE_WAY_POINT             = 3;

   private static final List<LookupResult>      _emptyProposal                   = new ArrayList<>();

   private static Lookup                        _suggester;

   private static IndexReader                   _indexReader;
   private static IndexSearcher                 _indexSearcher;
   private static FSDirectory                   _infixStore;

   private static TopDocs                       _topDocs;

   private static final DefaultPassageFormatter _highlightFormatter;

   private static boolean                       _isSearch_All;
   private static boolean                       _isSearch_Marker;
   private static boolean                       _isSearch_Tour;
   private static boolean                       _isSearch_Tour_LocationStart;
   private static boolean                       _isSearch_Tour_LocationEnd;
   private static boolean                       _isSearch_Tour_Weather;
   private static boolean                       _isSearch_Waypoint;
   private static boolean                       _isShow_TitleDescription;
   private static boolean                       _isSort_DateAscending            = false;                          // -> sort descending

   private static final FieldType               fieldType_Int;
   private static final FieldType               fieldType_Long;

   static {

      fieldType_Int = new FieldType(TextField.TYPE_STORED);
      fieldType_Int.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
      fieldType_Int.setDocValuesType(DocValuesType.NUMERIC);
      fieldType_Int.freeze();

      fieldType_Long = new FieldType(TextField.TYPE_STORED);
      fieldType_Long.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
      fieldType_Long.setDocValuesType(DocValuesType.NUMERIC);
      fieldType_Long.freeze();

      final String graphMarker_ColorDevice = UI.IS_DARK_THEME
            ? ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE_DARK
            : ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE;

      final String cssHighlighterColor = CSS.color(PreferenceConverter.getColor(_prefStore, graphMarker_ColorDevice));

      /*
       * Create custom formatter for the highlighter, the default highlighter displays the
       * highlighted text with bold face
       */
      _highlightFormatter = new DefaultPassageFormatter(

            // pre tag: highlight a hit with another color
            "<span style='color:" + cssHighlighterColor + ";'>", //$NON-NLS-1$ //$NON-NLS-2$

            // post tag
            "</span>", //$NON-NLS-1$

            "... ", //$NON-NLS-1$

            false);
   }

   /**
    * Implements {@link InputIterator} from multiple stored fields.
    * <p>
    * Copied and modified from
    * {@link org.apache.lucene.search.suggest.DocumentDictionary.DocumentInputIterator.DocumentInputIterator}
    */
   private static class DocumentInputIterator implements InputIterator {

      private IndexReader       __indexReader;
      private final Bits        __liveDocs;

      private final int         __docCount;
      private final int         __fieldCount;

      private final Set<String> __fieldsToLoad;
      private ArrayList<String> __fieldNames;

      private int               __currentDocId      = -1;
      private int               __currentFieldIndex = -1;
      private Document          __currentDoc;

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

         __fieldsToLoad.add(SEARCH_FIELD_TOUR_LOCATION_START);
         __fieldsToLoad.add(SEARCH_FIELD_TOUR_LOCATION_END);
         __fieldsToLoad.add(SEARCH_FIELD_TOUR_WEATHER);

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

               final BytesRef tempFieldValue = fieldVal.stringValue() != null
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

   private static class QueryResult {

      public Query    query;

      public String[] allQueryFields;
   }

   public static void closeIndexReaderSuggester() {

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

   private static void closeIndexWriterAndStore(final FSDirectory indexStore, final IndexWriter indexWriter) {

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

   private static Document createLuceneDoc_Marker(final long markerId,
                                                  final long tourId,
                                                  final String title,
                                                  final String description,
                                                  final long time) throws IOException {

      final Document doc = new Document();

      // create a field to identify a tour marker
      doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR_MARKER));

      doc.add(new LongPoint(SEARCH_FIELD_TOUR_ID, tourId));

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

      return doc;
   }

   private static Document createLuceneDoc_Tour(final long tourId,
                                                final long time,
                                                final String title,
                                                final String description,
                                                final String startPlace,
                                                final String endPlace,
                                                final String weather) throws IOException {

      final Document doc = new Document();

      // create a field to identify a tour
      doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR));

      doc.add(new LongPoint(SEARCH_FIELD_TOUR_ID, tourId));

      doc.add(createField_WithIndexOptions_Int(SEARCH_FIELD_DOC_SOURCE_SAVED, DOC_SOURCE_TOUR));
      doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TOUR_ID, tourId));
      doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TIME, time));

      if (title != null) {
         doc.add(new Field(SEARCH_FIELD_TITLE, title, createFieldType_Text()));
      }

      if (description != null) {
         doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, createFieldType_Text()));
      }

      if (startPlace != null) {
         doc.add(new Field(SEARCH_FIELD_TOUR_LOCATION_START, startPlace, createFieldType_Text()));
      }

      if (endPlace != null) {
         doc.add(new Field(SEARCH_FIELD_TOUR_LOCATION_END, endPlace, createFieldType_Text()));
      }

      if (weather != null) {
         doc.add(new Field(SEARCH_FIELD_TOUR_WEATHER, weather, createFieldType_Text()));
      }

      return doc;
   }

   private static Document createLuceneDoc_WayPoint(final long dbWayPointId,
                                                    final long tourId,
                                                    final String title,
                                                    final String description,
                                                    final long time) throws IOException {

      final Document doc = new Document();

      // create a field to identify a waypoint
      doc.add(new IntPoint(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_WAY_POINT));

      doc.add(new LongPoint(SEARCH_FIELD_TOUR_ID, tourId));

      doc.add(createField_WithIndexOptions_Int(SEARCH_FIELD_DOC_SOURCE_SAVED, DOC_SOURCE_WAY_POINT));
      doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TOUR_ID, tourId));
      doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_WAYPOINT_ID, dbWayPointId));

      if (time != 0) {
         doc.add(createField_WithIndexOptions_Long(SEARCH_FIELD_TIME, time));
      }

      if (title != null) {
         doc.add(new Field(SEARCH_FIELD_TITLE, title, createFieldType_Text()));
      }

      if (description != null) {
         doc.add(new Field(SEARCH_FIELD_DESCRIPTION, description, createFieldType_Text()));
      }

      return doc;
   }

   /**
    * Create max passages for the highlighter
    * <p>
    * "If no highlights were found for a document, the first maxPassages from the field will
    * be returned."
    * <p>
    * {@link org.apache.lucene.search.uhighlight.UnifiedHighlighter.highlightFields(String[],
    * Query, TopDocs) }
    *
    * @param numQueryFields
    */
   private static int[] createMaxPassages(final int numQueryFields) {

      final int allMaxPassages[] = new int[numQueryFields];

      Arrays.fill(allMaxPassages, 1);

      return allMaxPassages;
   }

   private static MultiFieldQueryParser createMultiFieldQueryParser(final Analyzer analyzer,
                                                                    final String[] allQueryFields) {

      final MultiFieldQueryParser queryParser = new MultiFieldQueryParser(allQueryFields, analyzer);

      queryParser.setAllowLeadingWildcard(true);

      return queryParser;
   }

   private static Query createQuery_Tour() {

      return IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR);
   }

   private static Query createQuery_TourMarker() {

      return IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR_MARKER);
   }

   private static Query createQuery_Waypoint() {

      return IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_WAY_POINT);
   }

   private static void createStore_TourData(final Connection conn,
                                            final IProgressMonitor monitor)
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
         final String sql = UI.EMPTY_STRING

               + "SELECT" //                 //$NON-NLS-1$

               + " tourId," //            1  //$NON-NLS-1$
               + " tourStartTime," //     2  //$NON-NLS-1$
               + " tourTitle," //         3  //$NON-NLS-1$
               + " tourDescription," //   4  //$NON-NLS-1$
               + " tourStartPlace," //    5  //$NON-NLS-1$
               + " tourEndPlace," //      6  //$NON-NLS-1$
               + " weather" //            7  //$NON-NLS-1$

               + " FROM " + tableName; //    //$NON-NLS-1$

         stmt = conn.prepareStatement(sql);
         final ResultSet rs = stmt.executeQuery();

         int createdDocuments = 0;
         long lastUpdateTime = System.currentTimeMillis();

         while (rs.next()) {

// SET_FORMATTING_OFF

            final long   dbTourId         = rs.getLong(1);
            final Long   dbTourStartTime  = rs.getLong(2);
            final String dbTitle          = rs.getString(3);
            final String dbDescription    = rs.getString(4);
            final String dbStartPlace     = rs.getString(5);
            final String dbEndPlace       = rs.getString(6);
            final String dbWeather        = rs.getString(7);

// SET_FORMATTING_ON

            final Document tourDoc = createLuceneDoc_Tour(

                  dbTourId,
                  dbTourStartTime,
                  dbTitle,
                  dbDescription,
                  dbStartPlace,
                  dbEndPlace,
                  dbWeather);

            indexWriter.addDocument(tourDoc);

            createdDocuments++;

            /*
             * Update monitor every 1/5 seconds
             */
            final long now = System.currentTimeMillis();

            if (now > lastUpdateTime + 200) {
               lastUpdateTime = now;
               monitor.subTask(NLS.bind(Messages.Search_Manager_CreateFTIndex, createdDocuments));
            }
         }

      } catch (final IOException e) {

         StatusUtil.showStatus(e);

      } finally {

         closeIndexWriterAndStore(indexStore, indexWriter);

         Util.closeSql(stmt);

         logCreateIndex(tableName, start);
      }
   }

   private static void createStore_TourMarker(final Connection conn, final IProgressMonitor monitor)
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
         final String sql = UI.EMPTY_STRING

               + "SELECT" //                             //$NON-NLS-1$

               + " markerId," //                      1  //$NON-NLS-1$
               + TourDatabase.KEY_TOUR + "," //       2  //$NON-NLS-1$
               + " label," //                         3  //$NON-NLS-1$
               + " description," //                   4  //$NON-NLS-1$
               + " tourTime" //                       5  //$NON-NLS-1$

               + " FROM " + tableName; //                //$NON-NLS-1$

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

            final Document markerDoc = createLuceneDoc_Marker(
                  dbMarkerId,
                  dbTourId,
                  dbLabel,
                  dbDescription,
                  dbTourTime);

            indexWriter.addDocument(markerDoc);

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

         closeIndexWriterAndStore(indexStore, indexWriter);

         Util.closeSql(stmt);

         logCreateIndex(tableName, start);
      }
   }

   private static void createStore_TourWaypoint(final Connection conn, final IProgressMonitor monitor)
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
         final String sql = UI.EMPTY_STRING

               + "SELECT" //                                         //$NON-NLS-1$

               + " " + TourDatabase.ENTITY_ID_WAY_POINT + "," //  1  //$NON-NLS-1$ //$NON-NLS-2$
               + " " + TourDatabase.KEY_TOUR + "," //             2  //$NON-NLS-1$ //$NON-NLS-2$
               + " name," //                                      3  //$NON-NLS-1$
               + " description," //                               4  //$NON-NLS-1$
               + " time" //                                       5  //$NON-NLS-1$

               + " FROM " + tableName; //                            //$NON-NLS-1$

         stmt = conn.prepareStatement(sql);
         final ResultSet rs = stmt.executeQuery();

         int createdDocuments = 0;
         long lastUpdateTime = System.currentTimeMillis();

         while (rs.next()) {

            final long dbWayPointId = rs.getLong(1);
            final long dbTourId = rs.getLong(2);
            final String dbLabel = rs.getString(3);
            final String dbDescription = rs.getString(4);
            final long dbTourTime = rs.getLong(5);

            final Document wayPointDoc = createLuceneDoc_WayPoint(

                  dbWayPointId,
                  dbTourId,
                  dbLabel,
                  dbDescription,
                  dbTourTime);

            indexWriter.addDocument(wayPointDoc);

            createdDocuments++;

            /*
             * Update monitor every 1/5 seconds
             */
            final long now = System.currentTimeMillis();

            if (now > lastUpdateTime + 200) {
               lastUpdateTime = now;
               monitor.subTask(NLS.bind(Messages.Search_Manager_CreateFTIndex, createdDocuments));
            }
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      } finally {

         closeIndexWriterAndStore(indexStore, indexWriter);

         Util.closeSql(stmt);

         logCreateIndex(tableName, start);
      }
   }

   /**
    * Split all tour id's into smaller parts because of this limit
    *
    * <pre>
    * org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024
    * at org.apache.lucene.search.BooleanQuery$Builder.add(BooleanQuery.java:114)
    * at org.apache.lucene.search.BooleanQuery$Builder.add(BooleanQuery.java:127)
    * at net.tourbook.search.FTSearchManager.updateIndex(FTSearchManager.java:1751)
    * </pre>
    */
   private static List<List<Long>> createTourIdParts(final List<Long> allTourIDs) {

      final ArrayList<Long> allTourIds_List = new ArrayList<>();

      for (final long tourId : allTourIDs) {
         allTourIds_List.add(tourId);
      }

      final int maxClauseCount = BooleanQuery.getMaxClauseCount() - 1;

      final List<List<Long>> allPartitionedTourIDs = ListUtils.partition(allTourIds_List, maxClauseCount);

      return allPartitionedTourIDs;
   }

   /**
    * Deletes the fulltext search index, this is useful when the index has new fields.
    */
   public static void deleteIndex() {

      setupIndexReader();

      FSDirectory indexStore_TourData = null;
      FSDirectory indexStore_Marker = null;
      FSDirectory indexStore_WayPoint = null;

      IndexWriter indexWriter_TourData = null;
      IndexWriter indexWriter_Marker = null;
      IndexWriter indexWriter_WayPoint = null;

      try {

         indexStore_TourData = openStore(TourDatabase.TABLE_TOUR_DATA);
         indexStore_Marker = openStore(TourDatabase.TABLE_TOUR_MARKER);
         indexStore_WayPoint = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

         indexWriter_TourData = new IndexWriter(indexStore_TourData, getIndexWriterConfig());
         indexWriter_Marker = new IndexWriter(indexStore_Marker, getIndexWriterConfig());
         indexWriter_WayPoint = new IndexWriter(indexStore_WayPoint, getIndexWriterConfig());

         indexWriter_TourData.deleteAll();
         indexWriter_Marker.deleteAll();
         indexWriter_WayPoint.deleteAll();

      } catch (final IOException e) {

         StatusUtil.showStatus(e);

      } finally {

         closeIndexWriterAndStore(indexStore_TourData, indexWriter_TourData);
         closeIndexWriterAndStore(indexStore_Marker, indexWriter_Marker);
         closeIndexWriterAndStore(indexStore_WayPoint, indexWriter_WayPoint);
      }

      closeIndexReaderSuggester();
   }

   /**
    * Remove tour from ft index when tour is deleted.
    *
    * @param tourId
    */
   public static void deleteTourFromIndex(final long tourId) {

      setupIndexReader();

      FSDirectory indexStore_TourData = null;
      FSDirectory indexStore_Marker = null;
      FSDirectory indexStore_WayPoint = null;

      IndexWriter indexWriter_TourData = null;
      IndexWriter indexWriter_Marker = null;
      IndexWriter indexWriter_WayPoint = null;

      final Builder deleteDoc_TourData = new BooleanQuery.Builder();
      final Builder deleteDoc_Marker = new BooleanQuery.Builder();
      final Builder deleteDoc_WayPoint = new BooleanQuery.Builder();

      try {

         indexStore_TourData = openStore(TourDatabase.TABLE_TOUR_DATA);
         indexStore_Marker = openStore(TourDatabase.TABLE_TOUR_MARKER);
         indexStore_WayPoint = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

         indexWriter_TourData = new IndexWriter(indexStore_TourData, getIndexWriterConfig());
         indexWriter_Marker = new IndexWriter(indexStore_Marker, getIndexWriterConfig());
         indexWriter_WayPoint = new IndexWriter(indexStore_WayPoint, getIndexWriterConfig());

         /*
          * Delete existing tour, marker and waypoint
          */
         final Query tourIdQuery = LongPoint.newExactQuery(SEARCH_FIELD_TOUR_ID, tourId);

         deleteDoc_TourData.add(tourIdQuery, Occur.FILTER);
         deleteDoc_Marker.add(tourIdQuery, Occur.FILTER);
         deleteDoc_WayPoint.add(tourIdQuery, Occur.FILTER);

         indexWriter_TourData.deleteDocuments(deleteDoc_TourData.build());
         indexWriter_Marker.deleteDocuments(deleteDoc_Marker.build());
         indexWriter_WayPoint.deleteDocuments(deleteDoc_WayPoint.build());

      } catch (final IOException e) {

         StatusUtil.showStatus(e);

      } finally {

         closeIndexWriterAndStore(indexStore_TourData, indexWriter_TourData);
         closeIndexWriterAndStore(indexStore_Marker, indexWriter_Marker);
         closeIndexWriterAndStore(indexStore_WayPoint, indexWriter_WayPoint);
      }

      closeIndexReaderSuggester();
   }

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

         TourLogManager.log_ERROR(e.getMessage());

         final java.nio.file.Path rootPath = getLuceneIndexRootPath();

         TourLogManager.log_INFO(String.format(Messages.Search_Manager_Log_DeletingLuceneRootFolder, rootPath.toString()));

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

         TourLogManager.log_INFO(Messages.Search_Manager_Log_LuceneRootFolderIsDeleted);

         indexWriter = new IndexWriter(indexStore, getIndexWriterConfig());
      }

      return indexWriter;
   }

   private static IndexWriterConfig getIndexWriterConfig() {

      final Analyzer analyzer = getAnalyzer();

      final IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);

      final boolean IS_DELETE_INDEX = false;

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
         closeIndexWriterAndStore(indexStore, indexWriter);
      }

      return false;
   }

   private static void logCreateIndex(final String indexStore, final long start) {

      StatusUtil.logInfo(String.format(LOG_CREATE_INDEX,
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
    * @param searchFromIndex
    * @param searchToIndex
    * @param searchResult
    * @return
    */
   private static void search(final String searchText,
                              final int searchFromIndex,
                              final int searchToIndex,
                              final SearchResult searchResult) {

      try {

         setupIndexReader();

         final int maxDoc = _indexReader.maxDoc();

         if (maxDoc == 0) {

            // there are 0 documents in the ft index

            searchResult.totalHits = 0;

            return;
         }

         final Analyzer analyzer = getAnalyzer();
         QueryResult queryResult;

         /*
          * Set sorting
          */
         final SortField sortByTime = new SortField(SEARCH_FIELD_TIME, Type.LONG, _isSort_DateAscending == false);
         final Sort ftSorting = new Sort(sortByTime);

         if (_isSearch_All) {

            // no filtering

            queryResult = search_10_Search_All(searchText, analyzer);

         } else {

            // search in selected fields/indices

            queryResult = search_20_Search_Parts(searchText, analyzer);
         }

         _topDocs = _indexSearcher.search(queryResult.query, maxDoc, ftSorting);

         searchResult.totalHits = _topDocs.totalHits;

         /**
          * Get doc id's only for the current page
          * <p>
          * It is very cheap to query the doc id's but very expensive to retrieve the documents
          * <p>
          * -> only necessary docs are retrieved
          */
         final int docStartIndex = searchFromIndex;
         int docEndIndex = searchToIndex;

         final ScoreDoc[] allScoreDocs = _topDocs.scoreDocs;
         final int scoreSize = allScoreDocs.length;

         if (docEndIndex >= scoreSize) {
            docEndIndex = scoreSize - 1;
         }

         final int numSearchResultItems = docEndIndex - docStartIndex + 1;
         final int allDocIds[] = new int[numSearchResultItems];

         for (int docIndex = 0; docIndex < numSearchResultItems; docIndex++) {
            allDocIds[docIndex] = allScoreDocs[docStartIndex + docIndex].doc;
         }

         /**
          * Highlight hits in the search result
          * <p>
          * This occurred: field 'description' was indexed without offsets -> cannot highlight
          */
         final UnifiedHighlighter highlighter = new UnifiedHighlighter(_indexSearcher, analyzer);

         highlighter.setFormatter(_highlightFormatter);

         final Map<String, String[]> highlightedSearchResults = highlighter.highlightFields(
               queryResult.allQueryFields,
               queryResult.query,
               allDocIds,
               createMaxPassages(queryResult.allQueryFields.length));

         search_90_CreateResult(
               highlightedSearchResults,
               _indexReader,
               searchResult,
               allDocIds,
               docStartIndex);

      } catch (final Exception e) {

         StatusUtil.showStatus(e);

         searchResult.error = e.getMessage();
      }
   }

   /**
    * Create a {@link Query} for all fields
    *
    * @param searchText
    * @param analyzer
    * @return
    * @throws ParseException
    */
   private static QueryResult search_10_Search_All(final String searchText, final Analyzer analyzer) throws ParseException {

      final String[] allQueryFields = {

            SEARCH_FIELD_TITLE,
            SEARCH_FIELD_DESCRIPTION,

            SEARCH_FIELD_TOUR_LOCATION_START,
            SEARCH_FIELD_TOUR_LOCATION_END,
            SEARCH_FIELD_TOUR_WEATHER
      };

      final Query query = createMultiFieldQueryParser(analyzer, allQueryFields).parse(searchText);

      // create return values
      final QueryResult queryResult = new QueryResult();

      queryResult.allQueryFields = allQueryFields;
      queryResult.query = query;

      return queryResult;
   }

   /**
    * Query text/marker/waypoint with OR
    *
    * @param analyzer
    * @throws ParseException
    */
   private static QueryResult search_20_Search_Parts(final String searchText, final Analyzer analyzer) throws ParseException {

      Builder tourQueryBuilder = null;
      Builder markerQueryBuilder = null;
      Builder waypointQueryBuilder = null;

      final ArrayList<String> allTourQueryFields = new ArrayList<>();
      final ArrayList<String> allMarkerQueryFields = new ArrayList<>();
      final ArrayList<String> allWaypointQueryFields = new ArrayList<>();

      if (_isSearch_Tour) {

         // search in tour: title/description

         allTourQueryFields.add(SEARCH_FIELD_TITLE);
         allTourQueryFields.add(SEARCH_FIELD_DESCRIPTION);
      }

      if (_isSearch_Tour_LocationStart) {

         allTourQueryFields.add(SEARCH_FIELD_TOUR_LOCATION_START);
      }

      if (_isSearch_Tour_LocationEnd) {

         allTourQueryFields.add(SEARCH_FIELD_TOUR_LOCATION_END);
      }

      if (_isSearch_Tour_Weather) {

         allTourQueryFields.add(SEARCH_FIELD_TOUR_WEATHER);
      }

      if (_isSearch_Marker) {

         // search in marker: title/description

         allMarkerQueryFields.add(SEARCH_FIELD_TITLE);
         allMarkerQueryFields.add(SEARCH_FIELD_DESCRIPTION);
      }

      if (_isSearch_Waypoint) {

         // search in waypoint: title/description

         allWaypointQueryFields.add(SEARCH_FIELD_TITLE);
         allWaypointQueryFields.add(SEARCH_FIELD_DESCRIPTION);
      }

      if (allTourQueryFields.size() > 0) {

         // search in tours

         final String[] allQueryFields = allTourQueryFields.toArray(String[]::new);

         final MultiFieldQueryParser field_QueryParser = createMultiFieldQueryParser(analyzer, allQueryFields);
         final Query query_Fields = field_QueryParser.parse(searchText);

         tourQueryBuilder = new BooleanQuery.Builder();
         tourQueryBuilder.add(createQuery_Tour(), Occur.MUST);
         tourQueryBuilder.add(query_Fields, Occur.MUST);
      }

      if (allMarkerQueryFields.size() > 0) {

         // search in tour markers

         final String[] allQueryFields = allMarkerQueryFields.toArray(String[]::new);

         final MultiFieldQueryParser field_QueryParser = createMultiFieldQueryParser(analyzer, allQueryFields);
         final Query query_Fields = field_QueryParser.parse(searchText);

         markerQueryBuilder = new BooleanQuery.Builder();
         markerQueryBuilder.add(createQuery_TourMarker(), Occur.MUST);
         markerQueryBuilder.add(query_Fields, Occur.MUST);
      }

      if (allWaypointQueryFields.size() > 0) {

         // search in waypoints

         final String[] allQueryFields = allWaypointQueryFields.toArray(String[]::new);

         final MultiFieldQueryParser field_QueryParser = createMultiFieldQueryParser(analyzer, allQueryFields);
         final Query query_Fields = field_QueryParser.parse(searchText);

         waypointQueryBuilder = new BooleanQuery.Builder();
         waypointQueryBuilder.add(createQuery_Waypoint(), Occur.MUST);
         waypointQueryBuilder.add(query_Fields, Occur.MUST);
      }

      /*
       * Create return values
       */
      final Builder allQueryBuilder = new BooleanQuery.Builder();

      if (tourQueryBuilder != null) {
         allQueryBuilder.add(tourQueryBuilder.build(), Occur.SHOULD);
      }

      if (markerQueryBuilder != null) {
         allQueryBuilder.add(markerQueryBuilder.build(), Occur.SHOULD);
      }

      if (waypointQueryBuilder != null) {
         allQueryBuilder.add(waypointQueryBuilder.build(), Occur.SHOULD);
      }

      final HashSet<String> setWithAllQueryFields = new HashSet<>();
      setWithAllQueryFields.addAll(allTourQueryFields);
      setWithAllQueryFields.addAll(allMarkerQueryFields);
      setWithAllQueryFields.addAll(allWaypointQueryFields);

      final QueryResult queryResult = new QueryResult();

      queryResult.allQueryFields = setWithAllQueryFields.toArray(String[]::new);
      queryResult.query = allQueryBuilder.build();

      TourLogManager.log_INFO(Messages.Search_Manager_Log_SearchingToursWith + UI.SPACE + queryResult.query);

      return queryResult;
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
   private static void search_90_CreateResult(final Map<String, String[]> highlights,
                                              final IndexReader indexReader,
                                              final SearchResult searchResult,
                                              final int[] docids,
                                              final int docStartIndex) throws IOException {

      if (highlights.isEmpty()) {
         return;
      }

      final Set<Entry<String, String[]>> fields = highlights.entrySet();
      Entry<String, String[]> firstHit;
      try {
         firstHit = fields.iterator().next();
      } catch (final Exception e) {
         return;
      }

      final int numHits = firstHit.getValue().length;

      // create result items
//      final SearchResultItem[] resultItems = new SearchResultItem[numHits];
      final ArrayList<SearchResultItem> allSearchResultItems = searchResult.allItems;

      for (int hitIndex = 0; hitIndex < numHits; hitIndex++) {

         final SearchResultItem resultItem = new SearchResultItem();

//         resultItems[hitIndex] = resultItem;

         allSearchResultItems.add(resultItem);
      }

      boolean isDocRead = false;
      final Set<String> fieldsToLoadFromDocument = new HashSet<>();
      fieldsToLoadFromDocument.add(SEARCH_FIELD_DOC_SOURCE_SAVED);
      fieldsToLoadFromDocument.add(SEARCH_FIELD_TOUR_ID);
      fieldsToLoadFromDocument.add(SEARCH_FIELD_MARKER_ID);
      fieldsToLoadFromDocument.add(SEARCH_FIELD_WAYPOINT_ID);
      fieldsToLoadFromDocument.add(SEARCH_FIELD_TIME);

      if (_isSearch_Tour == false && _isShow_TitleDescription) {

         /*
          * Tour title/description is not in the snippets (it is not searched for, load these fields
          * also from the document when it should be displayed
          */

         fieldsToLoadFromDocument.add(SEARCH_FIELD_DESCRIPTION);
         fieldsToLoadFromDocument.add(SEARCH_FIELD_TITLE);
      }

      for (final Entry<String, String[]> field : fields) {

         final String fieldName = field.getKey();
         final String[] allSnippets = field.getValue();

         for (int hitIndex = 0; hitIndex < allSnippets.length; hitIndex++) {

//            final SearchResultItem resultItem = resultItems[hitIndex];
            final SearchResultItem resultItem = allSearchResultItems.get(hitIndex);

            final String snippet = allSnippets[hitIndex];
            if (snippet != null) {

               switch (fieldName) {

               case SEARCH_FIELD_DESCRIPTION:
                  resultItem.description = snippet;
                  break;

               case SEARCH_FIELD_TITLE:
                  resultItem.title = snippet;
                  break;

               case SEARCH_FIELD_TOUR_LOCATION_START:
                  resultItem.locationStart = snippet;
                  break;

               case SEARCH_FIELD_TOUR_LOCATION_END:
                  resultItem.locationEnd = snippet;
                  break;

               case SEARCH_FIELD_TOUR_WEATHER:
                  resultItem.weather = snippet;
                  break;
               }
            }

            if (isDocRead == false) {

               final int docId = docids[hitIndex];
               final Document doc = indexReader.document(docId, fieldsToLoadFromDocument);

               resultItem.docId = docId;
               // resultItem.score = scoreDocs[docStartIndex + hitIndex].score;

               for (final IndexableField indexField : doc.getFields()) {

                  final String docFieldName = indexField.name();

                  switch (docFieldName) {

                  case SEARCH_FIELD_DESCRIPTION:

                     if (resultItem.description == null) {

                        String description = indexField.stringValue();
                        description = WEB.convertHTML_LineBreaks(description);

                        resultItem.description = description;
                     }

                     break;

                  case SEARCH_FIELD_TITLE:

                     if (resultItem.title == null) {
                        resultItem.title = indexField.stringValue();
                     }

                     break;

                  case SEARCH_FIELD_DOC_SOURCE_SAVED:
                     resultItem.docSource = indexField.numericValue().intValue();
                     break;

                  case SEARCH_FIELD_TOUR_ID:
                     resultItem.tourId = indexField.stringValue();
                     break;

                  case SEARCH_FIELD_MARKER_ID:
                     resultItem.markerId = indexField.stringValue();
                     break;

                  case SEARCH_FIELD_WAYPOINT_ID:
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
    * @param searchPosFrom
    * @param searchPosTo
    * @return Returns {@link SearchResult}
    */
   public static SearchResult searchByPosition(final String searchText,
                                               final int searchPosFrom,
                                               final int searchPosTo) {

      final SearchResult searchResult = new SearchResult();

      search(searchText, searchPosFrom, searchPosTo, searchResult);

      return searchResult;
   }

   static void setSearchOptions(final boolean isSearch_All,
                                final boolean isSearch_Marker,
                                final boolean isSearch_Tour,
                                final boolean isSearch_Tour_LocationStart,
                                final boolean isSearch_Tour_LocationEnd,
                                final boolean isSearch_Tour_Weather,
                                final boolean isSearch_Waypoint,
                                final boolean isSort_DateAscending,
                                final boolean isShow_TitleDescription) {
// SET_FORMATTING_OFF

      _isSearch_All                 = isSearch_All;
      _isSearch_Marker              = isSearch_Marker;
      _isSearch_Tour                = isSearch_Tour;
      _isSearch_Tour_LocationStart  = isSearch_Tour_LocationStart;
      _isSearch_Tour_LocationEnd    = isSearch_Tour_LocationEnd;
      _isSearch_Tour_Weather        = isSearch_Tour_Weather;
      _isSearch_Waypoint            = isSearch_Waypoint;

      _isSort_DateAscending         = isSort_DateAscending;

      _isShow_TitleDescription      = isShow_TitleDescription;

// SET_FORMATTING_ON
   }

   /**
    * Create FT index
    *
    * @param conn
    * @param monitor
    * @throws SQLException
    */
   private static void setupIndex() {

      if (isIndexCreated()) {
         return;
      }

      Display.getDefault().syncExec(() -> {

         try {

            final IRunnableWithProgress runnable = new IRunnableWithProgress() {

               @Override
               public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                     InterruptedException {

                  monitor.subTask(Messages.Database_Monitor_SetupLucene);

                  try (Connection conn = TourDatabase.getInstance().getConnection()) {

                     createStore_TourData(conn, monitor);
                     createStore_TourMarker(conn, monitor);
                     createStore_TourWaypoint(conn, monitor);

                  } catch (final SQLException e) {

                     net.tourbook.ui.UI.showSQLException(e);
                  }
               }
            };

            new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

         } catch (final InvocationTargetException | InterruptedException e) {
            StatusUtil.log(e);
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

      try {

         final FSDirectory tourDataIndex = openStore(TourDatabase.TABLE_TOUR_DATA);
         final FSDirectory tourMarkerIndex = openStore(TourDatabase.TABLE_TOUR_MARKER);
         final FSDirectory tourWayPoint = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

         final IndexReader indexReader1 = DirectoryReader.open(tourDataIndex);
         final IndexReader indexReader2 = DirectoryReader.open(tourMarkerIndex);
         final IndexReader indexReader3 = DirectoryReader.open(tourWayPoint);

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

      Display.getDefault().syncExec(() -> BusyIndicator.showWhile(Display.getDefault(), () -> {

         try {

            final DocumentInputIterator inputIterator = new DocumentInputIterator(_indexReader);

            final Analyzer queryAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));

            suggester[0] = new FreeTextSuggester(queryAnalyzer, queryAnalyzer, 4, (byte) 0x20);

            try {
               suggester[0].build(inputIterator);
            } catch (final IllegalArgumentException e1) {

               // java.lang.IllegalArgumentException: need at least one suggestion

               /*
                * This exception can occure when there are documents available but do not
                * contain any content which the suggester can use.
                */
            }

         } catch (final Exception e2) {
            StatusUtil.showStatus(e2);
         }
      }));

      return suggester[0];
   }

   /**
    * Update fulltext search index when tours are saved. This is not very efficient because all
    * tours and their markers/waypoints are deleted and recreated, however it is fast enought for a
    * few thousand items otherwise it would be more complex.
    *
    * @param allTourIDs
    */
   public static void updateIndex(final List<Long> allTourIDs) {

      final long start = System.nanoTime();

      final int numAllTourIDs = allTourIDs.size();

      final FSDirectory[] indexStore_TourData = { null };
      final FSDirectory[] indexStore_Marker = { null };
      final FSDirectory[] indexStore_WayPoint = { null };

      // these values MUST be final
      final IndexWriter[] indexWriter_TourData = { null };
      final IndexWriter[] indexWriter_Marker = { null };
      final IndexWriter[] indexWriter_WayPoint = { null };

      final boolean[] isIndexClosed = { false };

      try {

         setupIndexReader();

         indexStore_TourData[0] = openStore(TourDatabase.TABLE_TOUR_DATA);
         indexStore_Marker[0] = openStore(TourDatabase.TABLE_TOUR_MARKER);
         indexStore_WayPoint[0] = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

         indexWriter_TourData[0] = new IndexWriter(indexStore_TourData[0], getIndexWriterConfig());
         indexWriter_Marker[0] = new IndexWriter(indexStore_Marker[0], getIndexWriterConfig());
         indexWriter_WayPoint[0] = new IndexWriter(indexStore_WayPoint[0], getIndexWriterConfig());

         if (numAllTourIDs < 5) {

            // run without progress monitor

            for (final List<Long> tourIDPart : createTourIdParts(allTourIDs)) {

               updateIndex_10_Parts(
                     tourIDPart,
                     indexWriter_TourData[0],
                     indexWriter_Marker[0],
                     indexWriter_WayPoint[0],

                     // monitor parameters
                     null,
                     0,
                     null);
            }

         } else {

            Display.getDefault().syncExec(() -> {

               try {

                  final IRunnableWithProgress runnable = (monitor) -> {

                     monitor.beginTask(Messages.Search_Manager_Log_UpdatedFTIndex_Title, numAllTourIDs);

                     try {

                        final int[] numWorked = { 0 };

                        for (final List<Long> tourIDPart : createTourIdParts(allTourIDs)) {

                           updateIndex_10_Parts(

                                 tourIDPart,

                                 indexWriter_TourData[0],
                                 indexWriter_Marker[0],
                                 indexWriter_WayPoint[0],

                                 // monitor parameters
                                 monitor,
                                 numAllTourIDs,
                                 numWorked);
                        }

                     } catch (final IOException e) {

                        StatusUtil.showStatus(e);

                     } finally {

                        updateIndex_20_ClosingStores(

                              indexStore_TourData[0],
                              indexStore_Marker[0],
                              indexStore_WayPoint[0],
                              indexWriter_TourData[0],
                              indexWriter_Marker[0],
                              indexWriter_WayPoint[0],
                              monitor);

                        isIndexClosed[0] = true;
                     }
                  };

                  new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

               } catch (final InvocationTargetException | InterruptedException e) {

                  StatusUtil.showStatus(e);
               }
            });
         }

      } catch (final IOException e) {

         StatusUtil.showStatus(e);

      } finally {

         if (isIndexClosed[0] == false) {

            updateIndex_20_ClosingStores(

                  indexStore_TourData[0],
                  indexStore_Marker[0],
                  indexStore_WayPoint[0],
                  indexWriter_TourData[0],
                  indexWriter_Marker[0],
                  indexWriter_WayPoint[0],
                  null);
         }
      }

      closeIndexReaderSuggester();

      final long end = System.nanoTime();
      final float timeDiff = (end - start) / 1_000_000_000.0f;

      // reduce logging
      if (numAllTourIDs > 1) {

         TourLogManager.subLog_DEFAULT(String.format(Messages.Search_Manager_Log_UpdatedFTIndex_Final,

               numAllTourIDs,
               timeDiff));
      }
   }

   private static void updateIndex_10_Parts(final List<Long> allTourIDParts,
                                            final IndexWriter indexWriter_TourData,
                                            final IndexWriter indexWriter_Marker,
                                            final IndexWriter indexWriter_WayPoint,

                                            // monitor parameters
                                            final IProgressMonitor monitor,
                                            final int numTourIDs,
                                            final int[] numWorked

   ) throws IOException {

      long lastUpdateTime = System.currentTimeMillis();

      final Builder deleteDoc_TourData = new BooleanQuery.Builder();
      final Builder deleteDoc_Marker = new BooleanQuery.Builder();
      final Builder deleteDoc_WayPoint = new BooleanQuery.Builder();

      final ArrayList<Document> newDoc_TourData = new ArrayList<>();
      final ArrayList<Document> newDoc_Marker = new ArrayList<>();
      final ArrayList<Document> newDoc_WayPoint = new ArrayList<>();

      for (final Long tourId : allTourIDParts) {

         if (monitor != null) {

            final long now = System.currentTimeMillis();

            if (now > lastUpdateTime + 500) {

               lastUpdateTime = now;

               monitor.subTask(String.format(Messages.Search_Manager_SubTask_LoadingTours,
                     numWorked[0],
                     numTourIDs));
            }

            numWorked[0]++;
            monitor.worked(1);
         }

         final TourData tourData = TourManager.getTour(tourId);

         if (tourData != null) {

            /*
             * Delete existing tour, marker and waypoint
             */
            final Query tourIdQuery = LongPoint.newExactQuery(SEARCH_FIELD_TOUR_ID, tourId);

            deleteDoc_TourData.add(tourIdQuery, Occur.FILTER);
            deleteDoc_Marker.add(tourIdQuery, Occur.FILTER);
            deleteDoc_WayPoint.add(tourIdQuery, Occur.FILTER);

            /*
             * Recreate tour, marker and waypoint
             */
            final Document tourDoc = createLuceneDoc_Tour(tourId,
                  tourData.getTourStartTimeMS(),
                  tourData.getTourTitle(),
                  tourData.getTourDescription(),
                  tourData.getTourStartPlace(),
                  tourData.getTourEndPlace(),
                  tourData.getWeather());

            newDoc_TourData.add(tourDoc);

            for (final TourMarker tourMarker : tourData.getTourMarkers()) {

               final Document markerDoc = createLuceneDoc_Marker(
                     tourMarker.getMarkerId(),
                     tourId,
                     tourMarker.getLabel(),
                     tourMarker.getDescription(),
                     tourMarker.getTourTime());

               newDoc_Marker.add(markerDoc);
            }

            for (final TourWayPoint wayPoint : tourData.getTourWayPoints()) {

               final Document wayPointDoc = createLuceneDoc_WayPoint(
                     wayPoint.getWayPointId(),
                     tourId,
                     wayPoint.getName(),
                     wayPoint.getDescription(),
                     wayPoint.getTime());

               newDoc_WayPoint.add(wayPointDoc);
            }
         }
      }

      if (monitor != null) {
         monitor.subTask(Messages.Search_Manager_SubTask_UpdatingIndex);
      }

      indexWriter_TourData.deleteDocuments(deleteDoc_TourData.build());
      indexWriter_Marker.deleteDocuments(deleteDoc_Marker.build());
      indexWriter_WayPoint.deleteDocuments(deleteDoc_WayPoint.build());

      for (final Document document : newDoc_TourData) {
         indexWriter_TourData.addDocument(document);
      }
      for (final Document document : newDoc_Marker) {
         indexWriter_Marker.addDocument(document);
      }
      for (final Document document : newDoc_WayPoint) {
         indexWriter_WayPoint.addDocument(document);
      }
   }

   /**
    * This is a time consuming task which can have a duration of several seconds when using
    * many tours
    *
    * @param indexStore_TourData
    * @param indexStore_Marker
    * @param indexStore_WayPoint
    * @param indexWriter_TourData
    * @param indexWriter_Marker
    * @param indexWriter_WayPoint
    * @param monitor
    */
   private static void updateIndex_20_ClosingStores(final FSDirectory indexStore_TourData,
                                                    final FSDirectory indexStore_Marker,
                                                    final FSDirectory indexStore_WayPoint,
                                                    final IndexWriter indexWriter_TourData,
                                                    final IndexWriter indexWriter_Marker,
                                                    final IndexWriter indexWriter_WayPoint,
                                                    final IProgressMonitor monitor) {

      if (monitor != null) {
         monitor.subTask(Messages.Search_Manager_SubTask_ClosingFTIndexStore_Tours);
      }
      closeIndexWriterAndStore(indexStore_TourData, indexWriter_TourData);

      if (monitor != null) {
         monitor.subTask(Messages.Search_Manager_SubTask_ClosingFTIndexStore_Markers);
      }
      closeIndexWriterAndStore(indexStore_Marker, indexWriter_Marker);

      if (monitor != null) {
         monitor.subTask(Messages.Search_Manager_SubTask_ClosingFTIndexStore_Waypoints);
      }
      closeIndexWriterAndStore(indexStore_WayPoint, indexWriter_WayPoint);
   }
}
