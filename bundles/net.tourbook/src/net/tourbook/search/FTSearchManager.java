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
import net.tourbook.web.WEB;

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
import org.apache.lucene.search.uhighlight.PassageFormatter;
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

   private static final String             LUCENE_INDEX_FOLDER_NAME         = "lucene-index";                 //$NON-NLS-1$

   private static final String             SEARCH_FIELD_DESCRIPTION         = "description";                  //$NON-NLS-1$
   private static final String             SEARCH_FIELD_DOC_SOURCE_INDEX    = "docSource_Index";              //$NON-NLS-1$
   private static final String             SEARCH_FIELD_DOC_SOURCE_SAVED    = "docSource_Saved";              //$NON-NLS-1$
   private static final String             SEARCH_FIELD_MARKER_ID           = "markerID";                     //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TITLE               = "title";                        //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TOUR_ID             = "tourID";                       //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TOUR_LOCATION_START = "startLocation";                //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TOUR_LOCATION_END   = "endLocation";                  //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TOUR_WEATHER        = "weather";                      //$NON-NLS-1$
   private static final String             SEARCH_FIELD_TIME                = "time";                         //$NON-NLS-1$
   private static final String             SEARCH_FIELD_WAYPOINT_ID         = "wayPointID";                   //$NON-NLS-1$

   private static final String             LOG_CREATE_INDEX                 = "Created ft index: %s\t %d ms"; //$NON-NLS-1$

   private static final IPreferenceStore   _prefStore                       = TourbookPlugin.getPrefStore();

   static final int                        DOC_SOURCE_TOUR                  = 1;
   static final int                        DOC_SOURCE_TOUR_MARKER           = 2;
   static final int                        DOC_SOURCE_WAY_POINT             = 3;

   private static final List<LookupResult> _emptyProposal                   = new ArrayList<>();

   private static Lookup                   _suggester;

   private static IndexReader              _indexReader;
   private static IndexSearcher            _indexSearcher;
   private static FSDirectory              _infixStore;

   private static TopDocs                  _topDocs;
   private static String                   _topDocs_SearchText;

   private static boolean                  _isSearch_All;
   private static boolean                  _isSearch_Marker;
   private static boolean                  _isSearch_Tour;
   private static boolean                  _isSearch_Tour_LocationStart;
   private static boolean                  _isSearch_Tour_LocationEnd;
   private static boolean                  _isSearch_Tour_Weather;
   private static boolean                  _isSearch_Waypoint;
   private static boolean                  _isShow_TitleDescription;
   private static boolean                  _isSort_DateAscending            = false;                          // -> sort descending

   private static FieldType                fieldType_Int;
   private static FieldType                fieldType_Long;

   private static String                   _cssHighlighterColor;

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

      _cssHighlighterColor = CSS.color(PreferenceConverter.getColor(_prefStore, graphMarker_ColorDevice));
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

   private static void createStore_TourData(final Connection conn, final IProgressMonitor monitor)
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

   private static void createStore_TourWayPoint(final Connection conn, final IProgressMonitor monitor)
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

         TourLogManager.logError(e.getMessage());

         final java.nio.file.Path rootPath = getLuceneIndexRootPath();

         TourLogManager.logInfo(String.format(Messages.Search_Manager_Log_DeletingLuceneRootFolder, rootPath.toString()));

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

         TourLogManager.logInfo(Messages.Search_Manager_Log_LuceneRootFolderIsDeleted);

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

      StatusUtil.log(String.format(LOG_CREATE_INDEX,
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

         final ArrayList<String> queryFields = new ArrayList<>();

         if (_isSearch_All) {

            queryFields.add(SEARCH_FIELD_TITLE);
            queryFields.add(SEARCH_FIELD_DESCRIPTION);

            queryFields.add(SEARCH_FIELD_TOUR_LOCATION_START);
            queryFields.add(SEARCH_FIELD_TOUR_LOCATION_END);
            queryFields.add(SEARCH_FIELD_TOUR_WEATHER);

         } else {

            if (_isSearch_Tour
                  || _isSearch_Marker
                  || _isSearch_Waypoint) {

               queryFields.add(SEARCH_FIELD_TITLE);
               queryFields.add(SEARCH_FIELD_DESCRIPTION);
            }

            if (_isSearch_Tour_LocationStart) {
               queryFields.add(SEARCH_FIELD_TOUR_LOCATION_START);
            }
            if (_isSearch_Tour_LocationEnd) {
               queryFields.add(SEARCH_FIELD_TOUR_LOCATION_END);
            }
            if (_isSearch_Tour_Weather) {
               queryFields.add(SEARCH_FIELD_TOUR_WEATHER);
            }
         }

         final int numQueryFields = queryFields.size();
         final String[] queryFieldsAsArray = queryFields.toArray(new String[numQueryFields]);

         final Analyzer analyzer = getAnalyzer();

         final MultiFieldQueryParser queryParser = new MultiFieldQueryParser(queryFieldsAsArray, analyzer);
         queryParser.setAllowLeadingWildcard(true);

         final Query searchTextQuery = queryParser.parse(searchText);

         if (_topDocs_SearchText == null

               || _topDocs_SearchText.equals(searchText) == false

               // debugging
               || true

         ) {

            // this is a new search

            /*
             * Set sorting
             */
            final SortField sortByTime = new SortField(SEARCH_FIELD_TIME, Type.LONG, _isSort_DateAscending == false);
            final Sort sort = new Sort(sortByTime);

            if (_isSearch_All) {

               // no filtering
               _topDocs = _indexSearcher.search(searchTextQuery, maxDoc, sort);

            } else {

               // filter by content

               final BooleanQuery filterQuery = search_10_FilterByContent(searchTextQuery);

               _topDocs = _indexSearcher.search(filterQuery, maxDoc, sort);
            }

            _topDocs_SearchText = searchText;
         }

         searchResult.totalHits = _topDocs.totalHits;

         /**
          * Get doc id's only for the current page.
          * <p>
          * It is very cheap to query the doc id's but very expensive to retrieve the documents !!!
          */
         final int docStartIndex = searchFrom;
         int docEndIndex = searchTo;

         final ScoreDoc[] allScoreDocs = _topDocs.scoreDocs;
         final int scoreSize = allScoreDocs.length;

         if (docEndIndex >= scoreSize) {
            docEndIndex = scoreSize - 1;
         }

         final int numSearchResultItems = docEndIndex - docStartIndex + 1;
         final int allSearchResultDocIds[] = new int[numSearchResultItems];

         for (int docIndex = 0; docIndex < numSearchResultItems; docIndex++) {
            allSearchResultDocIds[docIndex] = allScoreDocs[docStartIndex + docIndex].doc;
         }

         final int maxPassages[] = new int[numQueryFields];
         Arrays.fill(maxPassages, 1);

         // this can occure: field 'description' was indexed without offsets, cannot highlight

         final UnifiedHighlighter highlighter = new UnifiedHighlighter(_indexSearcher, getAnalyzer());

         // create custom formatter for the highlighter, the default displays the highlighted text in bold
         final PassageFormatter highlightFormatter = new DefaultPassageFormatter(

               // pre tag: highlight a hit with another color
               "<span style='color:" + _cssHighlighterColor + ";'>", //$NON-NLS-1$ //$NON-NLS-2$

               // post tag
               "</span>", //$NON-NLS-1$

               "... ", //$NON-NLS-1$

               false);

         highlighter.setFormatter(highlightFormatter);

         final Map<String, String[]> highlights = highlighter.highlightFields(
               queryFieldsAsArray,
               searchTextQuery,
               allSearchResultDocIds,
               maxPassages);

         search_20_CreateResult(highlights, _indexReader, searchResult, allSearchResultDocIds, docStartIndex);

      } catch (final Exception e) {

         StatusUtil.showStatus(e);
         searchResult.error = e.getMessage();
      }
   }

   /**
    * Query text/marker/waypoint with OR
    */
   private static BooleanQuery search_10_FilterByContent(final Query textQuery) {

      final Builder orQueryBuilder = new BooleanQuery.Builder();

      if (_isSearch_Tour
            || _isSearch_Tour_LocationStart
            || _isSearch_Tour_LocationEnd
            || _isSearch_Tour_Weather) {

         final Query query = IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR);

         orQueryBuilder.add(query, Occur.SHOULD);
      }

      if (_isSearch_Marker) {

         final Query query = IntPoint.newExactQuery(SEARCH_FIELD_DOC_SOURCE_INDEX, DOC_SOURCE_TOUR_MARKER);

         orQueryBuilder.add(query, Occur.SHOULD);
      }

      if (_isSearch_Waypoint) {

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

      return andQuery;
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
   private static void search_20_CreateResult(final Map<String, String[]> highlights,
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
         final String[] snippets = field.getValue();

         for (int hitIndex = 0; hitIndex < snippets.length; hitIndex++) {

            final SearchResultItem resultItem = resultItems[hitIndex];

            final String snippet = snippets[hitIndex];
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
   public static SearchResult searchByPosition(final String searchText, final int searchPosFrom, final int searchPosTo) {

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

      Display.getDefault().syncExec(new Runnable() {
         @Override
         public void run() {

            try {

               final IRunnableWithProgress runnable = new IRunnableWithProgress() {

                  @Override
                  public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {

                     monitor.subTask(Messages.Database_Monitor_SetupLucene);

                     try (Connection conn = TourDatabase.getInstance().getConnection()) {

                        createStore_TourData(conn, monitor);
                        createStore_TourMarker(conn, monitor);
                        createStore_TourWayPoint(conn, monitor);

                     } catch (final SQLException e) {

                        net.tourbook.ui.UI.showSQLException(e);
                     }
                  }
               };

               new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

            } catch (final InvocationTargetException | InterruptedException e) {
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

   /**
    * Update fulltext search index when tours are saved. This is not very efficient because all
    * tours and their markers/waypoints are deleted and recreated, however it is fast enought for a
    * few thousand items otherwise it would be more complex.
    *
    * @param modifiedTours
    */
   public static void updateIndex(final ArrayList<TourData> modifiedTours) {

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

      final ArrayList<Document> newDoc_TourData = new ArrayList<>();
      final ArrayList<Document> newDoc_Marker = new ArrayList<>();
      final ArrayList<Document> newDoc_WayPoint = new ArrayList<>();

      try {

         indexStore_TourData = openStore(TourDatabase.TABLE_TOUR_DATA);
         indexStore_Marker = openStore(TourDatabase.TABLE_TOUR_MARKER);
         indexStore_WayPoint = openStore(TourDatabase.TABLE_TOUR_WAYPOINT);

         indexWriter_TourData = new IndexWriter(indexStore_TourData, getIndexWriterConfig());
         indexWriter_Marker = new IndexWriter(indexStore_Marker, getIndexWriterConfig());
         indexWriter_WayPoint = new IndexWriter(indexStore_WayPoint, getIndexWriterConfig());

         for (final TourData tourData : modifiedTours) {

            final long tourId = tourData.getTourId();

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

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      } finally {

         closeIndexWriterAndStore(indexStore_TourData, indexWriter_TourData);
         closeIndexWriterAndStore(indexStore_Marker, indexWriter_Marker);
         closeIndexWriterAndStore(indexStore_WayPoint, indexWriter_WayPoint);
      }

      closeIndexReaderSuggester();
   }
}
