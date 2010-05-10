/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.MyTourbookSplashHandler;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tag.TagCollection;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.apache.derby.drda.NetworkServerControl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application works successfully
	 */
	private static final int					TOURBOOK_DB_VERSION							= 10;

//	private static final int					TOURBOOK_DB_VERSION							= 10;	// 10.5.0
//	private static final int					TOURBOOK_DB_VERSION							= 9;	// 10.3.0
//	private static final int					TOURBOOK_DB_VERSION							= 8;	// 10.2.1 Mod by Kenny
//	private static final int					TOURBOOK_DB_VERSION							= 7;	// 9.01
//	private static final int					TOURBOOK_DB_VERSION							= 6;	// 8.12
//	private static final int					TOURBOOK_DB_VERSION							= 5;	// 8.11

	private static final String					DERBY_CLIENT_DRIVER							= "org.apache.derby.jdbc.ClientDriver";				//$NON-NLS-1$
	private static final String					DERBY_URL									= "jdbc:derby://localhost:1527/tourbook;create=true";	//$NON-NLS-1$

	/*
	 * database tables, renamed table names to uppercase otherwise conn.getMetaData().getColumns()
	 * would not work !!!
	 */
	public static final String					TABLE_SCHEMA								= "USER";												//$NON-NLS-1$

	private static final String					TABLE_DB_VERSION							= "DBVERSION";											// "DbVersion";			//$NON-NLS-1$

	public static final String					TABLE_TOUR_BIKE								= "TOURBIKE";											//"TourBike";			//$NON-NLS-1$
	public static final String					TABLE_TOUR_CATEGORY							= "TOURCATEGORY";										// "TourCategory";		//$NON-NLS-1$
	public static final String					TABLE_TOUR_COMPARED							= "TOURCOMPARED";										// "TourCompared";		//$NON-NLS-1$
	public static final String					TABLE_TOUR_DATA								= "TOURDATA";											// "TourData";			//$NON-NLS-1$
	public static final String					TABLE_TOUR_MARKER							= "TOURMARKER";										// "TourMarker";			//$NON-NLS-1$
	public static final String					TABLE_TOUR_WAYPOINT							= "TOURWAYPOINT";										// "TourWayPoint";		//$NON-NLS-1$
	public static final String					TABLE_TOUR_PERSON							= "TOURPERSON";										// "TourPerson";			//$NON-NLS-1$
	public static final String					TABLE_TOUR_REFERENCE						= "TOURREFERENCE";										// "TourReference";		//$NON-NLS-1$
	public static final String					TABLE_TOUR_TAG								= "TOURTAG";											// "TourTag";			//$NON-NLS-1$
	public static final String					TABLE_TOUR_TAG_CATEGORY						= "TOURTAGCATEGORY";									// "TourTagCategory";	//$NON-NLS-1$
	public static final String					TABLE_TOUR_TYPE								= "TOURTYPE";											// "TourType";			//$NON-NLS-1$

	public static final String					JOINTABLE_TOURDATA__TOURTAG					= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_TAG);			//$NON-NLS-1$
	public static final String					JOINTABLE_TOURDATA__TOURMARKER				= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER);			//$NON-NLS-1$
	public static final String					JOINTABLE_TOURDATA__TOURWAYPOINT			= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_WAYPOINT);		//$NON-NLS-1$
	public static final String					JOINTABLE_TOURDATA__TOURREFERENCE			= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE);		//$NON-NLS-1$
	public static final String					JOINTABLE_TOURTAGCATEGORY_TOURTAG			= (TABLE_TOUR_TAG_CATEGORY
																									+ "_" + TABLE_TOUR_TAG);						//$NON-NLS-1$
	public static final String					JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY	= (TABLE_TOUR_TAG_CATEGORY
																									+ "_" + TABLE_TOUR_TAG_CATEGORY);				//$NON-NLS-1$
	public static final String					JOINTABLE_TOURCATEGORY__TOURDATA			= (TABLE_TOUR_CATEGORY
																									+ "_" + TABLE_TOUR_DATA);						//$NON-NLS-1$
	/**
	 * contains <code>-1</code> which is the Id for a not saved entity
	 */
	public static final int						ENTITY_IS_NOT_SAVED							= -1;

	/**
	 * maximum length for a tour description
	 */
	public static final int						MAX_DESCRIPTION_LENGTH						= 32000;

	private static TourDatabase					_instance;

	private static NetworkServerControl			_server;
	private static EntityManagerFactory			_emFactory;
	private static ComboPooledDataSource		_pooledDataSource;

	private static ArrayList<TourType>			_activeTourTypes;
	private static ArrayList<TourType>			_tourTypes;

	private static HashMap<Long, TourTag>		_tourTags;
	private static HashMap<Long, TagCollection>	_tagCollections;

	private boolean								_isTableChecked;
	private boolean								_isVersionChecked;

	private final ListenerList					_propertyListeners							= new ListenerList(
																									ListenerList.IDENTITY);

	private final String						_databasePath								= (Platform
																									.getInstanceLocation()
																									.getURL()
																									.getPath() + "derby-database");				//$NON-NLS-1$

	{
		// set storage location for the database
		System.setProperty("derby.system.home", _databasePath); //$NON-NLS-1$

// derby debug properties
//		System.setProperty("derby.language.logQueryPlan", "true"); //$NON-NLS-1$
//		System.setProperty("derby.language.logStatementText", "true"); //$NON-NLS-1$
	}

	class CustomMonitor extends ProgressMonitorDialog {

		private final String	fTitle;

		public CustomMonitor(final Shell parent, final String title) {
			super(parent);
			fTitle = title;
		}

		@Override
		protected Control createDialogArea(final Composite parent) {
			getShell().setText(fTitle);
			return super.createDialogArea(parent);
		}
	}

	private class DatabaseColumn {

	}

	private TourDatabase() {}

//	public String[] getColumnsNames(String table) {
//		String query = "select * from bornes";
//		String[] names;
//		try {
//			PreparedStatement pstmt;
//			ResultSetMetaData rsmd;
//			int numColumns, i = 1;
//			pstmt = conn.prepareStatement(query);
//			rsmd = pstmt.getMetaData();
//			numColumns = rsmd.getColumnCount();
//			while (i <= numColumns) {
//				String str = rsmd.getColumnName(i);
//				System.out.println(str);
//				vNames.add(str);
//				i++;
//			}
//		} catch (SQLException sqle) {
//			System.out.println(sqle.toString());
//		} catch (Exception e) {
//			System.out.println("getColumnNames: " + e.toString());
//			e.printStackTrace();
//		}
//		names = new String[vNames.size()];
//		for (int i = 0; i < vNames.size(); i++) {
//			names[i] = new String(vNames.elementAt(i));
//		}
//		try {
//			conn.close();
//		} catch (SQLException sqle) {
//			System.out.println("close: " + sqle.toString());
//			sqle.printStackTrace();
//		}
//		return names;
//	}

	public static boolean checkFieldLength(final String fieldContent, final int maxLength) {

		final HashMap<String, DatabaseColumn> _tableTourData = new HashMap<String, DatabaseColumn>();

		Connection conn = null;
		try {

//			/*
//			 * Check if the tourdata table exists
//			 */
//			final DatabaseMetaData metaData = conn.getMetaData();
//
//			final ResultSet columns = metaData.getColumns(null, TABLE_SCHEMA, TABLE_TOUR_DATA, null);
//			while (columns.next()) {
//				final String name = columns.getString("COLUMN_NAME");
//				final String pos = columns.getString("ORDINAL_POSITION");
//				final String type = columns.getString("TYPE_NAME");
//				System.out.println("name = " + name + ", pos = " + pos + ", type = " + type);
//			}

			conn = getPooledConnection();

			final DatabaseMetaData metaData = conn.getMetaData();
			final ResultSet dbTables = metaData.getTables(null, null, null, null);

			while (dbTables.next()) {

				if (dbTables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {

					/**
					 * <pre>
					 * 
					 * Each column description has the following columns:
					 * 
					 * 1 TABLE_CAT			String => table catalog (may be null)
					 * 2 TABLE_SCHEM		String => table schema (may be null)
					 * 3 TABLE_NAME			String => table name
					 * 4 COLUMN_NAME		String => column name
					 * 5 DATA_TYPE			int => SQL type from java.sql.Types
					 * 6 TYPE_NAME			String => Data source dependent type name, for a UDT the type name is fully qualified
					 * 7 COLUMN_SIZE		int => column size.
					 * 8 BUFFER_LENGTH is not used.
					 * 9 DECIMAL_DIGITS		int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable.
					 * 10 NUM_PREC_RADIX	int => Radix (typically either 10 or 2)
					 * 11 NULLABLE			int => is NULL allowed.
					 *  	columnNoNulls - might not allow NULL values
					 *  	columnNullable - definitely allows NULL values
					 *  	columnNullableUnknown - nullability unknown
					 * 12 REMARKS 			String => comment describing column (may be null)
					 * 13 COLUMN_DEF		String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
					 * 14 SQL_DATA_TYPE 	int => unused
					 * 15 SQL_DATETIME_SUB 	int => unused
					 * 16 CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
					 * 17 ORDINAL_POSITION 	int => index of column in table (starting at 1)
					 * 18 IS_NULLABLE 		String => ISO rules are used to determine the nullability for a column.
					 *  	YES --- if the parameter can include NULLs
					 *  	NO --- if the parameter cannot include NULLs
					 *  	empty string --- if the nullability for the parameter is unknown
					 * 19 SCOPE_CATLOG 		String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
					 * 20 SCOPE_SCHEMA 		String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
					 * 21 SCOPE_TABLE 		String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF)
					 * 22 SOURCE_DATA_TYPE	short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
					 * 23 IS_AUTOINCREMENT	String => Indicates whether this column is auto incremented
					 * 		YES --- if the column is auto incremented
					 * 		NO --- if the column is not auto incremented
					 * 		empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown
					 * 
					 * The COLUMN_SIZE column the specified column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
					 * 
					 * </pre>
					 */

					final ResultSet dbColumns = metaData.getColumns(null, null, TABLE_TOUR_DATA, null);

					System.out.println("getRow()\t" + dbColumns.getRow());
					// TODO remove SYSTEM.OUT.PRINTLN

					while (dbColumns.next()) {

						System.out.println(dbColumns.getString(6)
								+ "\t"
								+ dbColumns.getInt(7)
								+ "\t"
								+ dbColumns.getString(4));
						// TODO remove SYSTEM.OUT.PRINTLN

//						dbColumns.

					}

				}
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			try {
				conn.close();
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}

//		if (fieldContent.length() >= maxLength) {
//
//			MessageDialog.openInformation(
//					Display.getDefault().getActiveShell(),
//					Messages.Tour_Database_Dialog_CheckFieldLength_Title,
//					NLS.bind(Messages.Tour_Database_Dialog_CheckFieldLength_Message, new Object[] {}));
//		}

		return false;
	}

	/**
	 * removes all tour tags which are loaded from the database so the next time they will be
	 * reloaded
	 */
	public static void clearTourTags() {

		if (_tourTags != null) {
			_tourTags.clear();
			_tourTags = null;
		}

		if (_tagCollections != null) {
			_tagCollections.clear();
			_tagCollections = null;
		}
	}

	/**
	 * remove all tour types and set their images dirty that the next time they have to be loaded
	 * from the database and the images are recreated
	 */
	public static void clearTourTypes() {

		if (_tourTypes != null) {
			_tourTypes.clear();
			_tourTypes = null;
		}

		UI.getInstance().setTourTypeImagesDirty();
	}

	private static void computeComputedValuesForAllTours(final IProgressMonitor monitor) {

		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours, compute computed fields and save the tour
		int tourCounter = 1;
		for (final Long tourId : tourList) {

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_update_tour,//
						new Object[] { tourCounter++, tourList.size() }));
			}

			final TourData tourData = getTourFromDb(tourId);
			if (tourData != null) {

				tourData.computeComputedValues();
				saveTour(tourData);
			}
		}
	}

	/**
	 * @param {@link IComputeTourValues} interface to compute values for one tour
	 */
	public static void computeValuesForAllTours(final IComputeTourValues runner) {

		final Shell shell = Display.getDefault().getActiveShell();

		final NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(0);

		final int[] tourCounter = new int[] { 0 };

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				final ArrayList<Long> tourList = getAllTourIds();

				monitor.subTask(Messages.tour_database_computeComputeValues_mainTask);

				// loop over all tours and calculate and set new columns
				for (final Long tourId : tourList) {

					final TourData oldTourData = getTourFromDb(tourId);
					TourData savedTourData = null;

					if (oldTourData != null) {
						if (runner.computeTourValues(oldTourData)) {
							savedTourData = saveTour(oldTourData);
						}
					}

					// create sub task text
					final StringBuilder sb = new StringBuilder();
					sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,//
							new Object[] { tourCounter[0]++, tourList.size(), }));

					final String runnerSubTaskText = runner.getSubTaskText(savedTourData);
					if (runnerSubTaskText != null) {
						sb.append(UI.DASH_WITH_SPACE);
						sb.append(runnerSubTaskText);
					}

					monitor.subTask(sb.toString());

					// check if canceled
					if (monitor.isCanceled()) {
						break;
					}

//					// debug test
//					if (tourCounter[0] > 100) {
//						break;
//					}
				}
			}
		};

		try {

			new ProgressMonitorDialog(shell).run(true, true, runnable);

		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {

			// create result text
			final StringBuilder sb = new StringBuilder();
			{
				sb.append(NLS.bind(
						Messages.tour_database_computeComputedValues_resultMessage,
						Integer.toString(tourCounter[0])));

				final String runnerResultText = runner.getResultText();
				if (runnerResultText != null) {
					sb.append(UI.NEW_LINE2);
					sb.append(runnerResultText);
				}
			}

			MessageDialog.openInformation(
					shell,
					Messages.tour_database_computeComputedValues_resultTitle,
					sb.toString());
		}
	}

	/**
	 * Disable runtime statistics by putting this stagement after the result set was read
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static void disableRuntimeStatistic(final Connection conn) throws SQLException {

		CallableStatement cs;

		cs = conn.prepareCall("VALUES SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS()"); //$NON-NLS-1$
		cs.execute();

		// log runtime statistics
		final ResultSet rs = cs.getResultSet();
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(0)"); //$NON-NLS-1$
		cs.execute();
		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(0)"); //$NON-NLS-1$
		cs.execute();
		cs.close();
	}

	/**
	 * Get runtime statistics by putting this stagement before the query is executed
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static void enableRuntimeStatistics(final Connection conn) throws SQLException {

		CallableStatement cs;

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1)"); //$NON-NLS-1$
		cs.execute();
		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(1)"); //$NON-NLS-1$
		cs.execute();
		cs.close();
	}

	/**
	 * @param tourTypeList
	 * @return Returns a list with all {@link TourType}'s which are currently used (with filter) to
	 *         display tours.<br>
	 *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
	 *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
	 */
	public static ArrayList<TourType> getActiveTourTypes() {
		return _activeTourTypes;
	}

	private static ArrayList<Long> getAllTourIds() {

		final ArrayList<Long> tourIds = new ArrayList<Long>();

		try {
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT"); //$NON-NLS-1$
			sb.append(" tourId"); // 	1 //$NON-NLS-1$
			sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();
			final ResultSet result = conn.prepareStatement(sb.toString()).executeQuery();

			while (result.next()) {
				tourIds.add(result.getLong(1));
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return tourIds;
	}

	/**
	 * @return Returns all tour tags which are stored in the database, the hash key is the tag id
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<Long, TourTag> getAllTourTags() {

		if (_tourTags != null) {
			return _tourTags;
		}

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT tourTag FROM TourTag AS tourTag"); //$NON-NLS-1$

			_tourTags = new HashMap<Long, TourTag>();

			for (final TourTag tourTag : ((List<TourTag>) query.getResultList())) {
				_tourTags.put(tourTag.getTagId(), tourTag);
			}

			em.close();
		}

		return _tourTags;
	}

	/**
	 * @return Returns all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourType> getAllTourTypes() {

		if (_tourTypes != null) {
			return _tourTypes;
		}

		_tourTypes = new ArrayList<TourType>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery(//
					//
					"SELECT tourType" //$NON-NLS-1$
							+ (" FROM TourType AS tourType") //$NON-NLS-1$
							+ (" ORDER  BY tourType.name")); //$NON-NLS-1$

			_tourTypes = (ArrayList<TourType>) query.getResultList();

			em.close();
		}

		return _tourTypes;
	}

	public static TourDatabase getInstance() {
		if (_instance == null) {
			_instance = new TourDatabase();
		}
		return _instance;
	}

	private static Connection getPooledConnection() throws SQLException {

		if (_pooledDataSource == null) {
			try {

				_pooledDataSource = new ComboPooledDataSource();

				//loads the jdbc driver
				_pooledDataSource.setDriverClass(DERBY_CLIENT_DRIVER);
				_pooledDataSource.setJdbcUrl(DERBY_URL);
				_pooledDataSource.setUser(TABLE_SCHEMA);
				_pooledDataSource.setPassword(TABLE_SCHEMA);

				_pooledDataSource.setMaxPoolSize(100);
				_pooledDataSource.setMaxStatements(100);
				_pooledDataSource.setMaxStatementsPerConnection(20);

			} catch (final PropertyVetoException e) {
				e.printStackTrace();
			}
		}

		Connection conn = null;
		try {
			conn = _pooledDataSource.getConnection();
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return conn;
	}

	@SuppressWarnings("unchecked")
	public static TagCollection getRootTags() {

		if (_tagCollections == null) {
			_tagCollections = new HashMap<Long, TagCollection>();
		}

		final long rootTagId = -1L;

		TagCollection rootEntry = _tagCollections.get(Long.valueOf(rootTagId));
		if (rootEntry != null) {
			return rootEntry;
		}

		/*
		 * read root tags from the database
		 */
		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		rootEntry = new TagCollection();

		/*
		 * read tag categories from db
		 */
		Query query = em.createQuery(//
				//
				"SELECT ttCategory" //$NON-NLS-1$
						+ (" FROM TourTagCategory AS ttCategory") //$NON-NLS-1$
						+ (" WHERE ttCategory.isRoot=1") //$NON-NLS-1$
						+ (" ORDER  BY ttCategory.name")); //$NON-NLS-1$

		rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

		/*
		 * read tour tags from db
		 */
		query = em.createQuery(//
				//
				"SELECT tourTag" //$NON-NLS-1$
						+ (" FROM TourTag AS tourTag ") //$NON-NLS-1$
						+ (" WHERE tourTag.isRoot=1") //$NON-NLS-1$
						+ (" ORDER  BY tourTag.name")); //$NON-NLS-1$

		rootEntry.tourTags = (ArrayList<TourTag>) query.getResultList();

		em.close();

		_tagCollections.put(rootTagId, rootEntry);

		return rootEntry;
	}

	/**
	 * @param categoryId
	 * @return Returns a {@link TagCollection} with all tags and categories for the category Id
	 */
	public static TagCollection getTagEntries(final long categoryId) {

		if (_tagCollections == null) {
			_tagCollections = new HashMap<Long, TagCollection>();
		}

		final Long categoryIdValue = Long.valueOf(categoryId);

		TagCollection categoryEntries = _tagCollections.get(categoryIdValue);
		if (categoryEntries != null) {
			return categoryEntries;
		}

		/*
		 * read tag entries from the database
		 */

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		categoryEntries = new TagCollection();

		final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, categoryIdValue);

		// get tags
		final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
		categoryEntries.tourTags = new ArrayList<TourTag>(lazyTourTags);
		Collections.sort(categoryEntries.tourTags);

		// get categories
		final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();
		categoryEntries.tourTagCategories = new ArrayList<TourTagCategory>(lazyTourTagCategories);
		Collections.sort(categoryEntries.tourTagCategories);

		em.close();

		_tagCollections.put(categoryIdValue, categoryEntries);

		return categoryEntries;
	}

	/**
	 * @param tagIds
	 * @return Returns the tag names separated with a comma or an empty string when tagIds are
	 *         <code>null</code>
	 */
	public static String getTagNames(final ArrayList<Long> tagIds) {

		if (tagIds == null) {
			return UI.EMPTY_STRING;
		}

		final HashMap<Long, TourTag> hashTags = getAllTourTags();
		final ArrayList<String> tagList = new ArrayList<String>();

		final StringBuilder sb = new StringBuilder();

		// get tag name for each tag id
		for (final Long tagId : tagIds) {
			final TourTag tag = hashTags.get(tagId);

			if (tag != null) {
				tagList.add(tag.getTagName());
			} else {
				try {
					throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (final MyTourbookException e) {
					e.printStackTrace();
				}
			}
		}

		// sort tags by name
		Collections.sort(tagList);

		// convert list into visible string
		int tagIndex = 0;
		for (final String tagName : tagList) {
			if (tagIndex++ > 0) {
				sb.append(", ");//$NON-NLS-1$
			}
			sb.append(tagName);
		}

		return sb.toString();
	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery(//
					//
					"SELECT tourBike" //$NON-NLS-1$
							+ (" FROM TourBike AS tourBike ") //$NON-NLS-1$
							+ (" ORDER  BY tourBike.name")); //$NON-NLS-1$

			bikeList = (ArrayList<TourBike>) query.getResultList();

			em.close();
		}

		return bikeList;
	}

	/**
	 * Get a tour from the database
	 * 
	 * @param tourId
	 * @return Returns the tour data or <code>null</code> if the tour is not in the database
	 */
	public static TourData getTourFromDb(final Long tourId) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		final TourData tourData = em.find(TourData.class, tourId);

		em.close();

		return tourData;
	}

	/**
	 * @return Returns all tour people in the db sorted by last/first name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourPerson> getTourPeople() {

		ArrayList<TourPerson> tourPeople = new ArrayList<TourPerson>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery(//
					//
					"SELECT TourPerson" //$NON-NLS-1$
							+ (" FROM TourPerson AS TourPerson") //$NON-NLS-1$
							+ (" ORDER BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$

			tourPeople = (ArrayList<TourPerson>) query.getResultList();

			em.close();
		}

		return tourPeople;
	}

	/**
	 * Get tour type from id
	 * 
	 * @param tourTypeId
	 * @return Returns a {@link TourType} from the id or <code>null</code> when tour type is not
	 *         available for the id.
	 */
	public static TourType getTourType(final long tourTypeId) {

		for (final TourType tourType : getAllTourTypes()) {
			if (tourType.getTypeId() == tourTypeId) {
				return tourType;
			}
		}

		return null;
	}

	/**
	 * @param typeId
	 * @return Returns the name for the {@link TourType} or an empty string when the tour type id
	 *         was not found
	 */
	public static String getTourTypeName(final long typeId) {

		String tourTypeName = Messages.ui_tour_not_defined;

		for (final TourType tourType : getAllTourTypes()) {
			if (tourType.getTypeId() == typeId) {
				tourTypeName = tourType.getName();
				break;
			}
		}

		return tourTypeName;
	}

	/**
	 * Remove a tour from the database
	 * 
	 * @param tourId
	 */
	public static boolean removeTour(final long tourId) {

		boolean isRemoved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourData tourData = em.find(TourData.class, tourId);

			if (tourData != null) {
				ts.begin();
				em.remove(tourData);
				ts.commit();
			}

		} catch (final Exception e) {

			e.printStackTrace();

			/*
			 * an error could have been occured when loading the tour with em.find, remove the tour
			 * with sql commands
			 */
			removeTourWithSQL(tourId);

		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isRemoved = true;
			}
			em.close();
		}

		if (isRemoved) {

			removeTourWithSQL(tourId);
			TourManager.getInstance().removeTourFromCache(tourId);
		}

		return true;
	}

	/**
	 * Remove tour from all tables which contain data for the removed tour
	 * 
	 * @param tourId
	 *            Tour Id for the tour which is removed
	 */
	private static void removeTourWithSQL(final long tourId) {

		Connection conn;
		PreparedStatement prepStmt;
		String sqlString = UI.EMPTY_STRING;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sqlWhereTourDataTourId = " WHERE " + TABLE_TOUR_DATA + "_tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * tour data
			 */
			sqlString = "DELETE FROM " + TABLE_TOUR_DATA + " WHERE tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * tour marker
			 */
			sqlString = "DELETE FROM " + TABLE_TOUR_MARKER + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			sqlString = "DELETE FROM " + JOINTABLE_TOURDATA__TOURMARKER + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * tour way point
			 */
			sqlString = "DELETE FROM " + TABLE_TOUR_WAYPOINT + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			sqlString = "DELETE FROM " + JOINTABLE_TOURDATA__TOURWAYPOINT + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * reference tour
			 */
			sqlString = "DELETE FROM " + TABLE_TOUR_REFERENCE + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			sqlString = "DELETE FROM " + JOINTABLE_TOURDATA__TOURREFERENCE + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * tour tags
			 */
			sqlString = "DELETE FROM " + JOINTABLE_TOURDATA__TOURTAG + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * compared tour
			 */
			sqlString = "DELETE FROM " + TABLE_TOUR_COMPARED + " WHERE tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			/*
			 * OLD unused table: tour category
			 */
			sqlString = ("DELETE FROM " + TABLE_TOUR_CATEGORY) + (" WHERE " + TABLE_TOUR_DATA + "tourId=?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			sqlString = ("DELETE FROM " + JOINTABLE_TOURCATEGORY__TOURDATA) + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sqlString);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();

			conn.close();

		} catch (final SQLException e) {
			System.out.println(sqlString);
			UI.showSQLException(e);
		}
	}

	/**
	 * Persists an entity
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return saved entity
	 */
	public static <T> T saveEntity(final T entity, final long id, final Class<?> entityClass) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		T savedEntity = null;
		boolean isSaved = false;

		try {

			ts.begin();
			{
				final Object entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);
					savedEntity = entity;

				} else {

					savedEntity = em.merge(entity);
				}
			}
			ts.commit();

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}

		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return savedEntity;
	}

	/**
	 * Persists an entity
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return Returns the saved entity
	 */
	public static <T> T saveEntity(final T entity, final long id, final Class<T> entityClass, final EntityManager em) {

		final EntityTransaction ts = em.getTransaction();

		T savedEntity = null;
		boolean isSaved = false;

		try {

			ts.begin();
			{
				final T entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);
					savedEntity = entity;

				} else {

					savedEntity = em.merge(entity);
				}
			}
			ts.commit();

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
		}

		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return savedEntity;
	}

	/**
	 * Persist {@link TourData} in the database and updates the tour data cache with the persisted
	 * tour<br>
	 * <br>
	 * When a tour has no person the tour will not be saved, a person must be set first before the
	 * tour can be saved
	 * 
	 * @param tourData
	 * @return persisted {@link TourData} or <code>null</code> when saving fails
	 */
	public static TourData saveTour(final TourData tourData) {

		/*
		 * prevent saving a tour which was deleted before
		 */
		if (tourData.isTourDeleted) {
			return null;
		}

		/*
		 * prevent saving a tour when a person is not set, this check is for internal use that all
		 * data are valid
		 */
		if (tourData.getTourPerson() == null) {
			StatusUtil.log("Cannot save a tour without a person: " + tourData); //$NON-NLS-1$
			return null;
		}

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		TourData persistedEntity = null;

		if (em != null) {

			final EntityTransaction ts = em.getTransaction();

			try {

				tourData.onPrePersist();

				ts.begin();
				{
					final TourData tourDataEntity = em.find(TourData.class, tourData.getTourId());
					if (tourDataEntity == null) {

						// tour is not yet persisted

						em.persist(tourData);

						persistedEntity = tourData;

					} else {

						persistedEntity = em.merge(tourData);
					}
				}
				ts.commit();

			} catch (final Exception e) {

				StatusUtil.showStatus(Messages.Tour_Database_TourSaveError, e);

			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}

		if (persistedEntity != null) {

			em = TourDatabase.getInstance().getEntityManager();
			try {

				persistedEntity = em.find(TourData.class, tourData.getTourId());

			} catch (final Exception e) {
				e.printStackTrace();
			}
			em.close();

			TourManager.getInstance().updateTourInCache(persistedEntity);
		}

		return persistedEntity;
	}

	public static void updateActiveTourTypeList(final TourTypeFilter tourTypeFilter) {

		switch (tourTypeFilter.getFilterType()) {
		case TourTypeFilter.FILTER_TYPE_SYSTEM:

			if (tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {

				// all tour types are selected

				_activeTourTypes = _tourTypes;
				return;

			} else {

				// tour type is not defined

			}

			break;

		case TourTypeFilter.FILTER_TYPE_DB:

			_activeTourTypes = new ArrayList<TourType>();
			_activeTourTypes.add(tourTypeFilter.getTourType());

			return;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

			final Object[] tourTypes = tourTypeFilter.getTourTypeSet().getTourTypes();

			if (tourTypes.length != 0) {

				// create a list with all tour types from the set

				_activeTourTypes = new ArrayList<TourType>();

				for (final Object item : tourTypes) {
					_activeTourTypes.add((TourType) item);
				}
				return;
			}

			break;

		default:
			break;
		}

		// set default empty list
		_activeTourTypes = new ArrayList<TourType>();
	}

	/**
	 * Update calendar week for all tours
	 * 
	 * @param conn
	 * @param monitor
	 * @param firstDayOfWeek
	 * @param minimalDaysInFirstWeek
	 * @return Returns <code>true</code> when the week is computed
	 */
	public static boolean updateTourWeek(	final Connection conn,
											final IProgressMonitor monitor,
											final int firstDayOfWeek,
											final int minimalDaysInFirstWeek) {

		final ArrayList<Long> tourList = getAllTourIds();

		boolean isUpdated = false;

		try {

			final PreparedStatement stmtSelect = conn.prepareStatement(//
					"SELECT" //							//$NON-NLS-1$
							+ " StartYear," // 				// 1 //$NON-NLS-1$
							+ " StartMonth," // 			// 2 //$NON-NLS-1$
							+ " StartDay" // 				// 3 //$NON-NLS-1$
							+ (" FROM " + TABLE_TOUR_DATA) //	$NON-NLS-1$ //$NON-NLS-1$
							+ " WHERE TourId=?"); //			$NON-NLS-1$ //$NON-NLS-1$

			final PreparedStatement stmtUpdate = conn.prepareStatement(//
					"UPDATE " + TABLE_TOUR_DATA//  //$NON-NLS-1$
							+ " SET" //$NON-NLS-1$
							+ " startWeek=?, " //$NON-NLS-1$
							+ " startWeekYear=? " //$NON-NLS-1$
							+ " WHERE tourId=?"); //$NON-NLS-1$

			int tourIdx = 1;
			final Calendar calendar = GregorianCalendar.getInstance();

			// set ISO 8601 week date
			calendar.setFirstDayOfWeek(firstDayOfWeek);
			calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);

			// loop over all tours and calculate and set new columns
			for (final Long tourId : tourList) {

				if (monitor != null) {
					final String msg = NLS.bind(Messages.Tour_Database_Update_TourWeek, new Object[] {
							tourIdx++,
							tourList.size() });
					monitor.subTask(msg);
				}

				// get tour date
				stmtSelect.setLong(1, tourId);
				stmtSelect.execute();

				final ResultSet result = stmtSelect.executeQuery();
				while (result.next()) {

					// get date from database
					final short dbYear = result.getShort(1);
					final short dbMonth = result.getShort(2);
					final short dbDay = result.getShort(3);

					calendar.set(dbYear, dbMonth - 1, dbDay);

					final short weekNo = (short) calendar.get(Calendar.WEEK_OF_YEAR);
					final short weekYear = (short) Util.getYearForWeek(calendar);

					// update week number/week year in the database
					stmtUpdate.setShort(1, weekNo);
					stmtUpdate.setShort(2, weekYear);
					stmtUpdate.setLong(3, tourId);
					stmtUpdate.executeUpdate();

					isUpdated = true;
				}
			}
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return isUpdated;
	}

	public void addPropertyListener(final IPropertyListener listener) {
		_propertyListeners.add(listener);
	}

	private boolean checkDb() {

		try {
			checkServer();
		} catch (final MyTourbookException e) {
			e.printStackTrace();
		}

		checkTable();

		if (checkVersion(null) == false) {
			return false;
		}

		return true;
	}

	/**
	 * Check if the server is available
	 * 
	 * @throws MyTourbookException
	 */
	private void checkServer() throws MyTourbookException {

		// when the server is started, nothing is to do here
		if (_server != null) {
			return;
		}

		@SuppressWarnings("unused")
		Class<?> derbyClientDriver;

		// check if the derby driver can be loaded
		try {
			derbyClientDriver = Class.forName(DERBY_CLIENT_DRIVER);
		} catch (final ClassNotFoundException e) {
			StatusUtil.showStatus(e.getMessage(), e);
			return;
		}

		try {

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getDefault().getSplashHandler();

			if (splashHandler == null) {
				checkServerCreateRunnable().run(null);
//				throw new MyTourbookException("Cannot get Splash Handler"); //$NON-NLS-1$
			} else {
				checkServerCreateRunnable().run(splashHandler.getBundleProgressMonitor());
			}

		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the database server is running, if not it will start the server. startServerJob has
	 * a job when the server is not yet started
	 */
	private IRunnableWithProgress checkServerCreateRunnable() {

		// create runnable for stating the derby server

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) {

				if (monitor != null) {
					monitor.subTask(Messages.Database_Monitor_db_service_task);
				}

				try {
					_server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
				} catch (final UnknownHostException e2) {
					e2.printStackTrace();
				} catch (final Exception e2) {
					e2.printStackTrace();
				}

				try {

					/*
					 * check if another derby server is already running (this can happen during
					 * development)
					 */
					_server.ping();

				} catch (final Exception e) {

					try {
						_server.start(null);
					} catch (final Exception e2) {
						e2.printStackTrace();
					}

					int pingCounter = 0;

					// wait until the server is started
					while (true) {

						try {
							_server.ping();
							break;
						} catch (final Exception e1) {
							StatusUtil.log(NLS.bind("Starting derby server: {0}", ++pingCounter), e); //$NON-NLS-1$
							try {
								Thread.sleep(1);
							} catch (final InterruptedException e2) {
								StatusUtil.log(e2);
							}
						}
					}

					// make the first connection, this takes longer as the subsequent ones
					try {
						final Connection connection = getPooledConnection();
						connection.close();

						// log database path
						StatusUtil.handleStatus("Database path: " + _databasePath, Status.INFO); //$NON-NLS-1$

					} catch (final SQLException e1) {
						UI.showSQLException(e1);
					}
				}
			}
		};

		return runnable;
	}

	/**
	 * Check if the table in the database exist
	 */
	private void checkTable() {

		if (_isTableChecked) {
			return;
		}

		Connection conn = null;

		try {

			conn = getPooledConnection();

			/*
			 * Check if the tourdata table exists
			 */
			final DatabaseMetaData metaData = conn.getMetaData();
			final ResultSet tables = metaData.getTables(null, null, null, null);
			while (tables.next()) {
				if (tables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {
					return;
				}
			}

			Statement stmt = null;

			try {

				stmt = conn.createStatement();
				{
					createTableTourData(stmt);

					createTableTourBike(stmt);
					createTableTourPerson(stmt);
					createTableTourType(stmt);
					createTableTourCategory(stmt);
					createTableTourMarker(stmt);
					createTableTourReference(stmt);
					createTableTourCompared(stmt);
					createTableVersion(stmt);

					createTableTourTagV5(stmt);
					createTableTourTagCategoryV5(stmt);

					createTableTourWayPointV10(stmt);

					stmt.executeBatch();
				}
				stmt.close();

				_isTableChecked = true;

			} catch (final SQLException e) {
				UI.showSQLException(e);
			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (final SQLException e) {
						UI.showSQLException(e);
					}
				}
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}
	}

	/**
	 * @param monitor
	 *            Progress monitor or <code>null</code> when the monitor is not available
	 * @return
	 */
	private boolean checkVersion(final IProgressMonitor monitor) {

		if (_isVersionChecked) {
			return true;
		}

		try {

			final Connection conn = getPooledConnection();

			String sqlString = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$
			final PreparedStatement prepStatement = conn.prepareStatement(sqlString);
			final ResultSet result = prepStatement.executeQuery();

			if (result.next()) {

				// version record was found, check if the database contains the correct version

				final int currentDbVersion = result.getInt(1);

				System.out.println("Database version: " + currentDbVersion); //$NON-NLS-1$

				if (currentDbVersion < TOURBOOK_DB_VERSION) {

					if (updateDbDesign(conn, currentDbVersion, monitor) == false) {
						return false;
					}

				} else if (currentDbVersion > TOURBOOK_DB_VERSION) {

					MessageDialog.openInformation(
							Display.getCurrent().getActiveShell(),
							Messages.tour_database_version_info_title,
							NLS
									.bind(
											Messages.tour_database_version_info_message,
											currentDbVersion,
											TOURBOOK_DB_VERSION));
				}

			} else {

				// a version record is not available

				/*
				 * insert the version for the current database design into the database
				 */
				sqlString = ("INSERT INTO " + TABLE_DB_VERSION) //$NON-NLS-1$
						+ " VALUES (" //$NON-NLS-1$
						+ Integer.toString(TOURBOOK_DB_VERSION)
						+ ")"; //$NON-NLS-1$

				conn.createStatement().executeUpdate(sqlString);
			}

			conn.close();

			_isVersionChecked = true;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
		return true;
	}

	/**
	 * Create index for {@link TourData} will dramatically improve performance
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createIndexTourDataV5(final Statement stmt) throws SQLException {

		final StringBuilder sb = new StringBuilder();

		// CREATE INDEX YearMonth
		sb.setLength(0);
		sb.append("CREATE INDEX YearMonth"); //$NON-NLS-1$
		sb.append(" ON " + TABLE_TOUR_DATA); //$NON-NLS-1$
		sb.append(" (startYear, startMonth)"); //$NON-NLS-1$

		stmt.execute(sb.toString());
		System.out.println(sb.toString());

		// CREATE INDEX TourType
		sb.setLength(0);
		sb.append("CREATE INDEX TourType"); //$NON-NLS-1$
		sb.append(" ON " + TABLE_TOUR_DATA); //$NON-NLS-1$
		sb.append(" (tourType_typeId)"); //$NON-NLS-1$

		stmt.execute(sb.toString());
		System.out.println(sb.toString());

		// CREATE INDEX TourPerson
		sb.setLength(0);
		sb.append("CREATE INDEX TourPerson"); //$NON-NLS-1$
		sb.append(" ON " + TABLE_TOUR_DATA); //$NON-NLS-1$
		sb.append(" (tourPerson_personId)"); //$NON-NLS-1$

		stmt.execute(sb.toString());
		System.out.println(sb.toString());
	}

	/**
	 * create table {@link #TABLE_TOUR_BIKE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourBike(final Statement stmt) throws SQLException {

		// CREATE TABLE TourBike
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_BIKE) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "bikeId	 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ "name				VARCHAR(255),	\n" //$NON-NLS-1$
				+ "weight 			FLOAT,			\n" //$NON-NLS-1$ // kg
				+ "typeId 			INTEGER,		\n" //$NON-NLS-1$
				+ "frontTyreId 		INTEGER,		\n" //$NON-NLS-1$
				+ "rearTyreId 		INTEGER			\n" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourBike
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_BIKE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_BIKE + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (bikeId)")); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_CATEGORY}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourCategory(final Statement stmt) throws SQLException {

		// CREATE TABLE TourCategory
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "categoryId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "tourId	BIGINT,") //$NON-NLS-1$
				+ "category 					VARCHAR(100)" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourCategory ADD CONSTRAINT TourCategory_pk PRIMARY KEY (categoryId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_CATEGORY + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (categoryId)")); //$NON-NLS-1$

		// CREATE TABLE TourCategory_TourData
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_CATEGORY + "_categoryId	BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourCategory_TourData ADD CONSTRAINT TourCategory_TourData_pk PRIMARY KEY (tourCategory_categoryId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURCATEGORY__TOURDATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (" + TABLE_TOUR_CATEGORY + "_categoryId)")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * create table {@link #TABLE_TOUR_COMPARED}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourCompared(final Statement stmt) throws SQLException {

		// CREATE TABLE TourCompared
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_COMPARED) //	//$NON-NLS-1$
				+ "(" //										//$NON-NLS-1$
				+ "comparedId 			BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ "refTourId			BIGINT," //				//$NON-NLS-1$
				+ "tourId				BIGINT," //				//$NON-NLS-1$
				+ "startIndex			INTEGER NOT NULL," //	//$NON-NLS-1$
				+ "endIndex 			INTEGER NOT NULL," //	//$NON-NLS-1$
				+ "tourDate	 			DATE NOT NULL," //		//$NON-NLS-1$
				+ "startYear			INTEGER NOT NULL," //	//$NON-NLS-1$
				+ "tourSpeed	 		FLOAT" //				//$NON-NLS-1$
				+ ")"); //										//$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_DATA}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourData(final Statement stmt) throws SQLException {

		// CREATE TABLE TourData
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_DATA) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "tourId 				BIGINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startYear 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startMonth 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startDay 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startHour 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startMinute 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startWeek 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startDistance 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "distance 			INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "startAltitude 		SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "startPulse 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "dpTolerance 			SMALLINT NOT NULL,	\n" //$NON-NLS-1$
				+ "tourDistance 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "tourRecordingTime 	INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "tourDrivingTime 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "tourAltUp 			INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "tourAltDown 			INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceTourType 		VARCHAR(2),			\n" //$NON-NLS-1$
				+ "deviceTravelTime 	BIGINT NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceDistance 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceWheel 			INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceWeight 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceTotalUp 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "deviceTotalDown 		INTEGER NOT NULL,	\n" //$NON-NLS-1$
				+ "devicePluginId	 	VARCHAR(255),		\n" //$NON-NLS-1$

				// version 3 start
				+ "deviceMode 			SMALLINT,			\n" //$NON-NLS-1$
				+ "deviceTimeInterval	SMALLINT,			\n" //$NON-NLS-1$
				// version 3 end

				// version 4 start

				// from markus
				+ "maxAltitude			INTEGER,			\n" //$NON-NLS-1$
				+ "maxPulse				INTEGER,			\n" //$NON-NLS-1$
				+ "avgPulse				INTEGER,			\n" //$NON-NLS-1$
				+ "avgCadence			INTEGER,			\n" //$NON-NLS-1$
				+ "avgTemperature		INTEGER,			\n" //$NON-NLS-1$
				+ "maxSpeed				FLOAT,				\n" //$NON-NLS-1$
				+ "tourTitle			VARCHAR(255),		\n" //$NON-NLS-1$
// OLD			+ "tourDescription		VARCHAR(4096),		\n" //$NON-NLS-1$ 										// version <= 9
				+ "tourDescription		VARCHAR(" + MAX_DESCRIPTION_LENGTH + "),	\n" //$NON-NLS-1$ //$NON-NLS-2$	// modified in version 10
				+ "tourStartPlace		VARCHAR(255),		\n" //$NON-NLS-1$
				+ "tourEndPlace			VARCHAR(255),		\n" //$NON-NLS-1$
				+ "calories				INTEGER,			\n" //$NON-NLS-1$
				+ "bikerWeight			FLOAT,				\n" //$NON-NLS-1$
				+ "tourBike_bikeId		BIGINT,				\n" //$NON-NLS-1$

				// from wolfgang
				+ "devicePluginName		VARCHAR(255),		\n" //$NON-NLS-1$
				+ "deviceModeName		VARCHAR(255),		\n" //$NON-NLS-1$

				// version 4 end

				// version 5 start
				/**
				 * disabled because when two blob object's are deserialized then the error occures:
				 * <p>
				 * java.io.StreamCorruptedException: invalid stream header: 00ACED00
				 * <p>
				 * therefor the gpsData are put into the serieData object
				 */
				//	+ "gpsData 				BLOB,				\n" //$NON-NLS-1$
				//
				// version 5 end
				//
				+ "tourType_typeId 			BIGINT,					\n" //$NON-NLS-1$
				+ "tourPerson_personId 		BIGINT,					\n" //$NON-NLS-1$

				// version 6 start
				//
				+ "tourImportFilePath		VARCHAR(255),			\n" //$NON-NLS-1$
				//
				// version 6 end

				// version 7 start
				//
				+ "mergeSourceTourId		BIGINT,					\n" //$NON-NLS-1$
				+ "mergeTargetTourId		BIGINT,					\n" //$NON-NLS-1$
				+ "mergedTourTimeOffset		INTEGER DEFAULT 0,		\n" //$NON-NLS-1$
				+ "mergedAltitudeOffset		INTEGER DEFAULT 0,		\n" //$NON-NLS-1$
				+ "startSecond	 			SMALLINT DEFAULT 0,		\n" //$NON-NLS-1$
				//
				// version 7 end

				// version 8 start
				//
				+ "weatherWindDir			INTEGER DEFAULT 0,		\n" //$NON-NLS-1$
				+ "weatherWindSpd			INTEGER DEFAULT 0,		\n" //$NON-NLS-1$
				+ "weatherClouds    	    VARCHAR(255), 			\n" //$NON-NLS-1$
				+ "restPulse        	    INTEGER DEFAULT 0,		\n" //$NON-NLS-1$
				+ "isDistanceFromSensor 	SMALLINT DEFAULT 0, 	\n" //$NON-NLS-1$
				//
				// version 8 end

				// version 9 start
				//
				+ "startWeekYear			SMALLINT DEFAULT 1977,	\n" //$NON-NLS-1$
				//
				// version 9 end

				// version 10 start
				//
				// tourWayPoints is mapped by tourData
				//
				// version 10 end

				+ "serieData 				BLOB NOT NULL			\n" //$NON-NLS-1$

				+ ")"); //$NON-NLS-1$

		/*
		 * Alter Table
		 */

		// ALTER TABLE TourData ADD CONSTRAINT TourData_pk PRIMARY KEY (tourId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_DATA) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_DATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (tourId)")); //$NON-NLS-1$

		createIndexTourDataV5(stmt);
	}

	/**
	 * create table {@link #TABLE_TOUR_MARKER}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourMarker(final Statement stmt) throws SQLException {

		// CREATE TABLE TourMarker
		stmt.execute("" //													//$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_MARKER) //					//$NON-NLS-1$
				+ "(" //													//$NON-NLS-1$
				+ "markerId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId	BIGINT,") //				//$NON-NLS-1$
				+ "time 						INTEGER NOT NULL," //		//$NON-NLS-1$
				+ "distance 					INTEGER NOT NULL," //		//$NON-NLS-1$
				+ "serieIndex 					INTEGER NOT NULL," //		//$NON-NLS-1$
				+ "type 						INTEGER NOT NULL," //		//$NON-NLS-1$
				+ "visualPosition				INTEGER NOT NULL," //		//$NON-NLS-1$
				+ "label 						VARCHAR(255)," //			//$NON-NLS-1$
				+ "category 					VARCHAR(100)," //			//$NON-NLS-1$
				// Version 2
				+ "labelXOffset					INTEGER," //				//$NON-NLS-1$
				+ "labelYOffset					INTEGER," //				//$NON-NLS-1$
				+ "markerType					BIGINT" //					//$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourMarker ADD CONSTRAINT TourMarker_pk PRIMARY KEY (markerId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_MARKER) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_MARKER + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (markerId)")); //$NON-NLS-1$

		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURMARKER) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_MARKER + "_markerId		BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourData_TourMarker ADD CONSTRAINT TourData_TourMarker_pk PRIMARY KEY (TourData_tourId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURMARKER) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURMARKER + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * create table {@link #TABLE_TOUR_PERSON}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourPerson(final Statement stmt) throws SQLException {

		// CREATE TABLE TourPerson
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_PERSON) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "personId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ "lastName 		VARCHAR(80),	\n" //$NON-NLS-1$
				+ "firstName 		VARCHAR(80),	\n" //$NON-NLS-1$
				+ "weight 			FLOAT,			\n" //$NON-NLS-1$ // kg
				+ "height 			FLOAT,			\n" //$NON-NLS-1$ // m
				+ "rawDataPath		VARCHAR(255),	\n" //$NON-NLS-1$
				+ "deviceReaderId	VARCHAR(255),	\n" //$NON-NLS-1$
				+ "tourBike_bikeId 	BIGINT			\n" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourPerson
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_PERSON) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_PERSON + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (personId)")); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_REFERENCE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourReference(final Statement stmt) throws SQLException {

		// CREATE TABLE TourReference
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "	refId 						BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId	BIGINT,\n") //$NON-NLS-1$
				+ "	startIndex					INTEGER NOT NULL,\n" //$NON-NLS-1$
				+ "	endIndex 					INTEGER NOT NULL,\n" //$NON-NLS-1$
				+ "	label 						VARCHAR(80)\n" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourReference ADD CONSTRAINT TourReference_pk PRIMARY KEY (refId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_REFERENCE + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (refId)")); //$NON-NLS-1$

		// CREATE TABLE TourData_TourReference

		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_REFERENCE + "_refId 		BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourData_TourReference ADD CONSTRAINT TourData_TourReference_pk PRIMARY KEY (TourData_tourId);
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURREFERENCE + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createTableTourTagCategoryV5(final Statement stmt) throws SQLException {

		/*
		 * creates the tables for the tour tag categories for VERSION 5
		 */

		String sql;

		/*
		 * TABLE TourTagCategory
		 */
		sql = ("CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ "	tagCategoryId 	BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	isRoot 			INTEGER,\n" //$NON-NLS-1$
				+ "	name 			VARCHAR(255)\n" //$NON-NLS-1$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		stmt.execute(sql);

		sql = ("ALTER TABLE " + TABLE_TOUR_TAG_CATEGORY) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_TAG_CATEGORY + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (tagCategoryId)"); //$NON-NLS-1$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql);

		/*
		 * JOIN TABLE TourTagCategory_TourTag
		 */

		sql = ("CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG + "_tagId						BIGINT NOT NULL,\n") //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId	BIGINT NOT NULL\n") //$NON-NLS-1$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		stmt.execute(sql);

		/*
		 * Add constraints
		 */

		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId"; //$NON-NLS-1$
		final String field_TourTagCategory_tagCategoryId = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId"; //$NON-NLS-1$

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$
				//
				+ (" ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTag_tagId) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId)") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" REFERENCES " + TABLE_TOUR_TAG + " (tagId)"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		stmt.execute(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$
				//
				+ (" ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTagCategory_tagCategoryId) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId)") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql);

		/*
		 * JOIN TABLE TourTagCategory_TourTagCategory
		 */

		sql = ("CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1	BIGINT NOT NULL,\n") //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2	BIGINT NOT NULL\n") //$NON-NLS-1$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		stmt.execute(sql);

		/*
		 * Add constraints
		 */

		final String field_TourTagCategory_tagCategoryId1 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1"; //$NON-NLS-1$
		final String field_TourTagCategory_tagCategoryId2 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2"; //$NON-NLS-1$

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_" //$NON-NLS-1$
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_" //$NON-NLS-1$
						+ field_TourTagCategory_tagCategoryId1 + "\n") //$NON-NLS-1$
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1)\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)\n"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		stmt.execute(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_" //$NON-NLS-1$
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_" //$NON-NLS-1$
						+ field_TourTagCategory_tagCategoryId2 + "\n") //$NON-NLS-1$
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2)\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)\n"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_TAG}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourTagV5(final Statement stmt) throws SQLException {

		/*
		 * creates the tables for the tour tags for VERSION 5
		 */

		// CREATE TABLE TourTag
		String sql;

		sql = ("CREATE TABLE " + TABLE_TOUR_TAG) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ "	tagId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	isRoot 		INTEGER,\n" //$NON-NLS-1$
				+ "	expandType 	INTEGER,\n" //$NON-NLS-1$
				+ "	name 		VARCHAR(255)\n" //$NON-NLS-1$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);

		stmt.execute(sql);

		// ALTER TABLE TourTag ADD CONSTRAINT TourTag_pk PRIMARY KEY (refId);
		sql = ("ALTER TABLE " + TABLE_TOUR_TAG) //$NON-NLS-1$
				+ ("	ADD CONSTRAINT " + TABLE_TOUR_TAG + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	PRIMARY KEY (tagId)"); //$NON-NLS-1$

		System.out.println(sql);
		System.out.println();

		stmt.execute(sql);

		/*
		 * CREATE TABLE TourData_TourTag
		 */
		final String field_TourData_tourId = TABLE_TOUR_DATA + "_tourId"; //$NON-NLS-1$
		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId"; //$NON-NLS-1$

		sql = ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG + "_tagId" + "	BIGINT NOT NULL,\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (TABLE_TOUR_DATA + "_tourId" + "	BIGINT NOT NULL\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		stmt.execute(sql);

		/*
		 * Add Constrainsts
		 */

//		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //						//$NON-NLS-1$ //$NON-NLS-2$
//				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURTAG + "_pk") //		//$NON-NLS-1$
//				+ (" PRIMARY KEY (" + field_TourTag_tagId + ")"); //				//$NON-NLS-1$ //$NON-NLS-2$
//
//		System.out.println(sql);
//		stmt.addBatch(sql);
		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$
				//
				+ ("	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourTag_tagId) //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId)") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	REFERENCES " + TABLE_TOUR_TAG + " (tagId)"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		stmt.execute(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$
				//
				+ ("	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourData_tourId) //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	FOREIGN KEY (" + TABLE_TOUR_DATA + "_tourId)") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	REFERENCES " + TABLE_TOUR_DATA + " (tourId)"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_TYPE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourType(final Statement stmt) throws SQLException {

		// CREATE TABLE TourType
		stmt.execute("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_TYPE) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "typeId 				BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ "name 				VARCHAR(100)," //$NON-NLS-1$
				+ "colorBrightRed 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorBrightGreen 	SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorBrightBlue 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorDarkRed 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorDarkGreen 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorDarkBlue 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorLineRed 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorLineGreen 		SMALLINT NOT NULL," //$NON-NLS-1$
				+ "colorLineBlue 		SMALLINT NOT NULL" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourType
		stmt.execute("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_TYPE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_TYPE + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (typeId)")); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_WAYPOINT}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourWayPointV10(final Statement stmt) throws SQLException {

		// CREATE TABLE TourWayPoint
		String sql = ("CREATE TABLE " + TABLE_TOUR_WAYPOINT + "\n") // 							//$NON-NLS-1$ //$NON-NLS-2$
				+ "(\n" //																		//$NON-NLS-1$
				+ "   wayPointId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "   " + (TABLE_TOUR_DATA + "_tourId	BIGINT,\n") //							//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "   latitude 			DOUBLE NOT NULL,		\n" //							//$NON-NLS-1$
				+ "   longitude 		DOUBLE NOT NULL,		\n" //							//$NON-NLS-1$
				+ "   time				BIGINT,					\n" //							//$NON-NLS-1$
				+ "   altitude			FLOAT,					\n" //							//$NON-NLS-1$
				+ "   name				VARCHAR(1024),			\n" //							//$NON-NLS-1$
				+ "   description		VARCHAR(" + MAX_DESCRIPTION_LENGTH + "),	\n" //		//$NON-NLS-1$ //$NON-NLS-2$
				+ "   comment			VARCHAR(" + MAX_DESCRIPTION_LENGTH + "),	\n" //		//$NON-NLS-1$ //$NON-NLS-2$
				+ "   symbol			VARCHAR(4096),			\n" //							//$NON-NLS-1$
				+ "   category			VARCHAR(4096)			\n" //							//$NON-NLS-1$
				//
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql); //

		// ALTER TABLE TourWayPoint ADD CONSTRAINT TourWayPoint_pk PRIMARY KEY (wayPointId);
		sql = ("ALTER TABLE " + TABLE_TOUR_WAYPOINT) //											//$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_WAYPOINT + "_pk ")) //						//$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (wayPointId)"); //$NON-NLS-1$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql); //

		// CREATE TABLE	TourData_TourWayPoint
		sql = ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURWAYPOINT) //								//$NON-NLS-1$
				+ "(\n" //																			//$NON-NLS-1$
				+ "   " + (TABLE_TOUR_DATA + "_tourId				BIGINT NOT NULL,	\n") //		//$NON-NLS-1$ //$NON-NLS-2$
				+ "   " + (TABLE_TOUR_WAYPOINT + "_wayPointId		BIGINT NOT NULL		\n") //		//$NON-NLS-1$ //$NON-NLS-2$
				+ ")"; //$NON-NLS-1$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql); //

		// ALTER TABLE TourData_TourWayPoint ADD CONSTRAINT TourData_TourWayPoint_pk PRIMARY KEY (TourData_tourId);
		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURWAYPOINT) //				//$NON-NLS-1$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURWAYPOINT + "_pk") //	//$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();
		stmt.execute(sql); //
	}

	/**
	 * create table {@link #TABLE_DB_VERSION}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableVersion(final Statement stmt) throws SQLException {

		// CREATE TABLE Version
		stmt.execute(("CREATE TABLE " + TABLE_DB_VERSION) //$NON-NLS-1$
				+ " (" //$NON-NLS-1$
				+ "version 		INTEGER	NOT NULL" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$
	}

	public void firePropertyChange(final int propertyId) {
		final Object[] allListeners = _propertyListeners.getListeners();
		for (final Object allListener : allListeners) {
			final IPropertyListener listener = (IPropertyListener) allListener;
			listener.propertyChanged(TourDatabase.this, propertyId);
		}
	}

	public Connection getConnection() throws SQLException {

		if (checkDb()) {
			return getPooledConnection();
		} else {
			return null;
		}
	}

	/**
	 * Creates an entity manager which is used to persist entities
	 * 
	 * @return
	 */
	public EntityManager getEntityManager() {

		if (_emFactory != null) {
			return _emFactory.createEntityManager();
		}

		try {
			final IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {
						checkServer();
					} catch (final MyTourbookException e) {
						e.printStackTrace();
						return;
					}
					checkTable();
					checkVersion(monitor);

					monitor.subTask(Messages.Database_Monitor_persistent_service_task);

					_emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$
				}
			};

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getDefault().getSplashHandler();

			if (splashHandler == null) {
				try {
					checkServer();
				} catch (final MyTourbookException e) {
					StatusUtil.showStatus(e);
					return null;
				}
				checkTable();
				checkVersion(null);

				_emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

			} else {
				runnableWithProgress.run(splashHandler.getBundleProgressMonitor());
			}

//		} catch (MyTourbookException e) {
//			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		if (_emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return null;
		} else {
			final EntityManager em = _emFactory.createEntityManager();

			return em;
		}
	}

	public void removePropertyListener(final IPropertyListener listener) {
		_propertyListeners.remove(listener);
	}

	private boolean updateDbDesign(final Connection conn, int currentDbVersion, final IProgressMonitor monitor) {

		/*
		 * this must be implemented or updated when the database version must be updated
		 */

		// confirm update
		final String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

		final String message = NLS.bind(Messages.Database_Confirm_update, new Object[] {
				currentDbVersion,
				TOURBOOK_DB_VERSION,
				_databasePath });

		final MessageDialog dialog = new MessageDialog(
				Display.getDefault().getActiveShell(),
				Messages.Database_Confirm_update_title,
				null,
				message,
				MessageDialog.QUESTION,
				buttons,
				1);

		if ((dialog.open()) != Window.OK) {
			PlatformUI.getWorkbench().close();
			return false;
		}

		int newVersion = currentDbVersion;
		final int oldVersion = currentDbVersion;

		/*
		 * database update
		 */
		if (currentDbVersion == 1) {
			updateDbDesign_001_002(conn);
			currentDbVersion = newVersion = 2;
		}

		if (currentDbVersion == 2) {
			updateDbDesign_002_003(conn);
			currentDbVersion = newVersion = 3;
		}

		if (currentDbVersion == 3) {
			updateDbDesign_003_004(conn, monitor);
			currentDbVersion = newVersion = 4;
		}

		boolean isPostUpdate5 = false;
		if (currentDbVersion == 4) {
			updateDbDesign_004_005(conn, monitor);
			currentDbVersion = newVersion = 5;
			isPostUpdate5 = true;
		}

		if (currentDbVersion == 5) {
			updateDbDesign_005_006(conn, monitor);
			currentDbVersion = newVersion = 6;
		}

		if (currentDbVersion == 6) {
			updateDbDesign_006_007(conn, monitor);
			currentDbVersion = newVersion = 7;
		}

		if (currentDbVersion == 7) {
			updateDbDesign_007_008(conn, monitor);
			currentDbVersion = newVersion = 8;
		}

		boolean isPostUpdate8 = false;
		if (currentDbVersion == 8) {
			updateDbDesign_008_009(conn, monitor);
			currentDbVersion = newVersion = 9;
			isPostUpdate8 = true;
		}

		if (currentDbVersion == 9) {
			updateDbDesign_009_010(conn, monitor);
			currentDbVersion = newVersion = 10;
		}

		/*
		 * update version number
		 */
		updateDbVersionNumber(conn, newVersion);

		/*
		 * do the post update after the version number is updated because the post update uses
		 * connections and this will check the version number
		 */
		if (isPostUpdate5) {
			TourDatabase.computeComputedValuesForAllTours(monitor);
			TourManager.getInstance().removeAllToursFromCache();
		}
		if (isPostUpdate8) {
			updateDbDesign_008_009_PostUpdate(conn, monitor);
		}

		// display info for the successful update
		MessageDialog.openInformation(
				Display.getCurrent().getActiveShell(),
				Messages.tour_database_version_info_title,
				NLS.bind(Messages.Tour_Database_UpdateInfo, oldVersion, newVersion));

		return true;
	}

	private void updateDbDesign_001_002(final Connection conn) {

		System.out.println("Database update: 2");//$NON-NLS-1$
		System.out.println();

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private void updateDbDesign_002_003(final Connection conn) {

		System.out.println("Database update: 3");//$NON-NLS-1$
		System.out.println();

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private void updateDbDesign_003_004(final Connection conn, final IProgressMonitor monitor) {

		System.out.println("Database update: 4");//$NON-NLS-1$
		System.out.println();

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxAltitude			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgCadence			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgTemperature		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxSpeed				FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourTitle				VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourDescription		VARCHAR(4096)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourStartPlace		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourEndPlace			VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN calories				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN bikerWeight			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourBike_bikeId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			// from wolfgang
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN devicePluginName		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			// from wolfgang
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceModeName		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.execute(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		// Create a EntityManagerFactory here, so we can access TourData with EJB
		if (monitor != null) {
			monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		}
		_emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

		if (monitor != null) {
			monitor.subTask(Messages.Tour_Database_load_all_tours);
		}
		final ArrayList<Long> tourList = getAllTourIds();

		// loop over all tours and calculate and set new columns
		int tourIdx = 1;
		for (final Long tourId : tourList) {

			final TourData tourData = getTourFromDb(tourId);

			if (monitor != null) {
				final String msg = NLS.bind(
						Messages.Tour_Database_update_tour,
						new Object[] { tourIdx++, tourList.size() });
				monitor.subTask(msg);
			}

			tourData.computeComputedValues();

			final TourPerson person = tourData.getTourPerson();
			tourData.setTourBike(person.getTourBike());
			tourData.setBikerWeight(person.getWeight());

			saveTour(tourData);
		}

		// cleanup everything as if nothing has happened
		_emFactory.close();
		_emFactory = null;
	}

	private void updateDbDesign_004_005(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 5));
		}

		System.out.println("Database update: 5");//$NON-NLS-1$
		System.out.println();

		try {
			final Statement statement = conn.createStatement();

			createTableTourTagV5(statement);
			createTableTourTagCategoryV5(statement);
			createIndexTourDataV5(statement);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private void updateDbDesign_005_006(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 6));
		}

		System.out.println();
		System.out.println("Database update: 6");//$NON-NLS-1$

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourImportFilePath		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println("database is updated");//$NON-NLS-1$
		System.out.println();
	}

	private void updateDbDesign_006_007(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 7));
		}

		System.out.println();
		System.out.println("database update: 7");//$NON-NLS-1$

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeSourceTourId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeTargetTourId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startSecond			SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println("database is updated");//$NON-NLS-1$
		System.out.println();
	}

	private void updateDbDesign_007_008(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 8));
		}

		System.out.println();
		System.out.println(NLS.bind(Messages.Tour_Database_Update, 8));

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherWindDir		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherWindSpd		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN isDistanceFromSensor  SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherClouds         VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN restPulse        INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println("database is updated");//$NON-NLS-1$
		System.out.println();
	}

	private void updateDbDesign_008_009(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 9));
		}

		System.out.println();
		System.out.println(NLS.bind(Messages.Tour_Database_Update, 9));

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startWeekYear		SMALLINT DEFAULT 1977 "; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, 9));
		System.out.println();
	}

	private void updateDbDesign_008_009_PostUpdate(final Connection conn, final IProgressMonitor monitor) {

		// set ISO 8601 week number
		final int firstDayOfWeek = Calendar.MONDAY;
		final int minimalDaysInFirstWeek = 4;

		if (updateTourWeek(conn, monitor, firstDayOfWeek, minimalDaysInFirstWeek)) {
			MessageDialog.openInformation(
					Display.getDefault().getActiveShell(),
					Messages.Database_Confirm_update_title,
					Messages.Tour_Database_Update_TourWeek_Info);
		}
	}

	private void updateDbDesign_009_010(final Connection conn, final IProgressMonitor monitor) {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 10));
		}

		System.out.println();
		System.out.println(NLS.bind(Messages.Tour_Database_Update, 10));

		try {
			final Statement statement = conn.createStatement();

			createTableTourWayPointV10(statement);

			/**
			 * resize description column: ref derby docu page 24
			 * 
			 * <pre>
			 * 
			 * ALTER TABLE table-Name
			 * {
			 *     ADD COLUMN column-definition |
			 *     ADD CONSTRAINT clause |
			 *     DROP [ COLUMN ] column-name [ CASCADE | RESTRICT ]
			 *     DROP { PRIMARY KEY | FOREIGN KEY constraint-name | UNIQUE
			 *   		constraint-name | CHECK constraint-name | CONSTRAINT constraint-name }
			 *     ALTER [ COLUMN ] column-alteration |
			 *     LOCKSIZE { ROW | TABLE }
			 * 
			 *     column-alteration
			 * 
			 * 		column-Name SET DATA TYPE VARCHAR(integer) |
			 * 		column-Name SET DATA TYPE VARCHAR FOR BIT DATA(integer) |
			 * 		column-name SET INCREMENT BY integer-constant |
			 * 		column-name RESTART WITH integer-constant |
			 * 		column-name [ NOT ] NULL |
			 * 		column-name [ WITH | SET ] DEFAULT default-value |
			 * 		column-name DROP DEFAULT
			 * }
			 * </pre>
			 */

			final String sql = "ALTER TABLE " + TABLE_TOUR_DATA //$NON-NLS-1$
					+ " ALTER COLUMN tourDescription" //$NON-NLS-1$
					+ " SET DATA TYPE VARCHAR(" + MAX_DESCRIPTION_LENGTH + ") "; //$NON-NLS-1$ //$NON-NLS-2$

			System.out.println(sql);
			statement.execute(sql);

			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, 10));
		System.out.println();
	}

	private void updateDbVersionNumber(final Connection conn, final int newVersion) {
		try {
			final String sqlString = "" //$NON-NLS-1$
					+ ("update " + TABLE_DB_VERSION) //$NON-NLS-1$
					+ (" set VERSION=" + newVersion) //$NON-NLS-1$
					+ (" where 1=1"); //$NON-NLS-1$
			conn.createStatement().executeUpdate(sqlString);
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

}
