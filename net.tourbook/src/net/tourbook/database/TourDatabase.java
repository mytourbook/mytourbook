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
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.MyTourbookSplashHandler;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.joda.time.DateTime;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application works successfully
	 */
	private static final int						TOURBOOK_DB_VERSION							= 21;

//	private static final int						TOURBOOK_DB_VERSION							= 21;	// 12.1.1
//	private static final int						TOURBOOK_DB_VERSION							= 20;	// 12.1
//	private static final int						TOURBOOK_DB_VERSION							= 19;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 18;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 17;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 16;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 15;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 14;	// 11.3
//	private static final int						TOURBOOK_DB_VERSION							= 13;	// 10.11
//	private static final int						TOURBOOK_DB_VERSION							= 12;	// 10.9.1
//	private static final int						TOURBOOK_DB_VERSION							= 11;	// 10.7.0 - 11-07-2010
//	private static final int						TOURBOOK_DB_VERSION							= 10;	// 10.5.0 not released
//	private static final int						TOURBOOK_DB_VERSION							= 9;	// 10.3.0
//	private static final int						TOURBOOK_DB_VERSION							= 8;	// 10.2.1 Mod by Kenny
//	private static final int						TOURBOOK_DB_VERSION							= 7;	// 9.01
//	private static final int						TOURBOOK_DB_VERSION							= 6;	// 8.12
//	private static final int						TOURBOOK_DB_VERSION							= 5;	// 8.11

	public static boolean							IS_POST_UPDATE_019_to_020					= false;

	private static final String						PERSISTENCE_UNIT_NAME						= "tourdatabase";										//$NON-NLS-1$

	private static final String						DERBY_CLIENT_DRIVER							= "org.apache.derby.jdbc.ClientDriver";				//$NON-NLS-1$
	private static final String						DERBY_URL									= "jdbc:derby://localhost:1527/tourbook;create=true";	//$NON-NLS-1$

	private static final int						MAX_TRIES_TO_PING_SERVER					= 10;

	/*
	 * !!! database tables, names are set to uppercase otherwise conn.getMetaData().getColumns()
	 * would not work !!!
	 */
	public static final String						TABLE_SCHEMA								= "USER";												//$NON-NLS-1$
	private static final String						TABLE_DB_VERSION							= "DBVERSION";											// "DbVersion";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_BIKE								= "TOURBIKE";											//"TourBike";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_COMPARED							= "TOURCOMPARED";										// "TourCompared";		//$NON-NLS-1$
	public static final String						TABLE_TOUR_DATA								= "TOURDATA";											// "TourData";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_MARKER							= "TOURMARKER";										// "TourMarker";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_WAYPOINT							= "TOURWAYPOINT";										// "TourWayPoint";		//$NON-NLS-1$
	public static final String						TABLE_TOUR_PERSON							= "TOURPERSON";										// "TourPerson";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_PERSON_HRZONE					= "TOURPERSONHRZONE";									// "TourPersonHRZone";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_REFERENCE						= "TOURREFERENCE";										// "TourReference";		//$NON-NLS-1$
	public static final String						TABLE_TOUR_TAG								= "TOURTAG";											// "TourTag";			//$NON-NLS-1$
	public static final String						TABLE_TOUR_TAG_CATEGORY						= "TOURTAGCATEGORY";									// "TourTagCategory";	//$NON-NLS-1$
	public static final String						TABLE_TOUR_TYPE								= "TOURTYPE";											// "TourType";			//$NON-NLS-1$
//
//  tour category is disabled since version 1.6
//
//	public static final String						TABLE_TOUR_CATEGORY							= "TOURCATEGORY";										// "TourCategory";		//$NON-NLS-1$

	public static final String						JOINTABLE_TOURDATA__TOURTAG					= (TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_TAG);						//$NON-NLS-1$
	public static final String						JOINTABLE_TOURDATA__TOURMARKER				= (TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_MARKER);					//$NON-NLS-1$
	public static final String						JOINTABLE_TOURDATA__TOURWAYPOINT			= (TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_WAYPOINT);					//$NON-NLS-1$
	public static final String						JOINTABLE_TOURDATA__TOURREFERENCE			= (TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_REFERENCE);					//$NON-NLS-1$
	public static final String						JOINTABLE_TOURTAGCATEGORY_TOURTAG			= (TABLE_TOUR_TAG_CATEGORY
																										+ "_" + TABLE_TOUR_TAG);						//$NON-NLS-1$
	public static final String						JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY	= (TABLE_TOUR_TAG_CATEGORY
																										+ "_" + TABLE_TOUR_TAG_CATEGORY);				//$NON-NLS-1$
	public static final String						JOINTABLE_TOURPERSON__TOURPERSON_HRZONE		= (TABLE_TOUR_PERSON
																										+ "_" + TABLE_TOUR_PERSON_HRZONE);				//$NON-NLS-1$
//	public static final String						JOINTABLE_TOURCATEGORY__TOURDATA			= (TABLE_TOUR_CATEGORY
//																									+ "_" + TABLE_TOUR_DATA);						//$NON-NLS-1$
	/**
	 * contains <code>-1</code> which is the Id for a not saved entity
	 */
	public static final int							ENTITY_IS_NOT_SAVED							= -1;

	private static volatile TourDatabase			_instance;

	private static NetworkServerControl				_server;
	private static volatile EntityManagerFactory	_emFactory;
	private static volatile ComboPooledDataSource	_pooledDataSource;

	private static ArrayList<TourType>				_activeTourTypes;

	private static volatile ArrayList<TourType>		_tourTypes;
	private static volatile HashMap<Long, TourTag>	_tourTags;

	private static HashMap<Long, TagCollection>		_tagCollections								= new HashMap<Long, TagCollection>();

	/*
	 * cached distinct fields
	 */
	private static TreeSet<String>					_dbTourTitles;
	private static TreeSet<String>					_dbTourStartPlace;
	private static TreeSet<String>					_dbTourEndPlace;
	private static TreeSet<String>					_dbTourMarkerNames;

	private boolean									_isTableChecked;
	private boolean									_isVersionChecked;

	private final ListenerList						_propertyListeners							= new ListenerList(
																										ListenerList.IDENTITY);

	private final String							_databasePath								= (Platform
																										.getInstanceLocation()
																										.getURL()
																										.getPath() + "derby-database");				//$NON-NLS-1$

	private boolean									_isSQLUpdateError							= false;

	/**
	 * Database version before a db update is performed
	 */
	private int										_dbVersionBeforeUpdate;

	{
		// set storage location for the database
		System.setProperty("derby.system.home", _databasePath); //$NON-NLS-1$

// derby debug properties
//		System.setProperty("derby.language.logQueryPlan", "true"); //$NON-NLS-1$
//		System.setProperty("derby.language.logStatementText", "true"); //$NON-NLS-1$
	}

	private static final Object						DB_LOCK										= new Object();

	private TourDatabase() {}

	/**
	 * removes all tour tags which are loaded from the database so the next time they will be
	 * reloaded
	 */
	public static synchronized void clearTourTags() {

		if (_tourTags != null) {
			_tourTags.clear();
			_tourTags = null;
		}

		if (_tagCollections != null) {
			_tagCollections.clear();
		}
	}

	/**
	 * remove all tour types and set their images dirty that the next time they have to be loaded
	 * from the database and the images are recreated
	 */
	public static synchronized void clearTourTypes() {

		if (_tourTypes != null) {
			_tourTypes.clear();
			_tourTypes = null;
		}

		UI.getInstance().setTourTypeImagesDirty();
	}

	public static void closeConnection(final Connection conn) {

		if (conn != null) {
			try {
				conn.close();
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}
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
				saveTour(tourData, false);
			}
		}
	}

	/**
	 * @param {@link IComputeTourValues} interface to compute values for one tour
	 * @return
	 */
	public static boolean computeValuesForAllTours(final IComputeTourValues runner) {

		final Shell shell = Display.getDefault().getActiveShell();

		final NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(0);

		final int[] tourCounter = new int[] { 0 };
		final int[] tourListSize = new int[] { 0 };
		final boolean[] isCanceled = new boolean[] { false };

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				final ArrayList<Long> tourList = getAllTourIds();
				tourListSize[0] = tourList.size();

				monitor.beginTask(Messages.tour_database_computeComputeValues_mainTask, tourList.size());

				// loop over all tours and compute values
				for (final Long tourId : tourList) {

					final TourData dbTourData = getTourFromDb(tourId);
					TourData savedTourData = null;

					if (dbTourData != null) {
						if (runner.computeTourValues(dbTourData)) {

							// ensure that all computed values are set
							dbTourData.computeComputedValues();

							savedTourData = saveTour(dbTourData, false);
						}
					}

					// create sub task text
					final StringBuilder sb = new StringBuilder();
					sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,//
							new Object[] { tourCounter[0]++, tourListSize[0], }));

					sb.append(UI.DASH_WITH_DOUBLE_SPACE);
					sb.append(tourCounter[0] * 100 / tourListSize[0]);
					sb.append(UI.SYMBOL_PERCENTAGE);

					final String runnerSubTaskText = runner.getSubTaskText(savedTourData);
					if (runnerSubTaskText != null) {
						sb.append(UI.DASH_WITH_DOUBLE_SPACE);
						sb.append(runnerSubTaskText);
					}

					monitor.subTask(sb.toString());
					monitor.worked(1);

					// check if canceled
					if (monitor.isCanceled()) {
						isCanceled[0] = true;
						break;
					}

////					// debug test
//					if (tourCounter[0] > 0) {
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
			sb.append(NLS.bind(
					Messages.tour_database_computeComputedValues_resultMessage,
					tourCounter[0],
					tourListSize[0]));

			final String runnerResultText = runner.getResultText();
			if (runnerResultText != null) {
				sb.append(UI.NEW_LINE2);
				sb.append(runnerResultText);
			}

			MessageDialog.openInformation(
					shell,
					Messages.tour_database_computeComputedValues_resultTitle,
					sb.toString());
		}

		return isCanceled[0];
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

		Connection conn = null;
		Statement stmt = null;

		try {

			conn = getInstance().getConnection();
			stmt = conn.createStatement();

			final ResultSet result = stmt.executeQuery("SELECT tourId FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

			while (result.next()) {
				tourIds.add(result.getLong(1));
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.sqlClose(stmt);
			closeConnection(conn);

		}

		return tourIds;
	}

	public static TreeSet<String> getAllTourMarkerNames() {

		if (_dbTourMarkerNames == null) {
			_dbTourMarkerNames = getDistinctValues(TourDatabase.TABLE_TOUR_MARKER, "label"); // $NON-NLS-1$
		}

		return _dbTourMarkerNames;
	}

	/**
	 * Getting all tour place ends from the database sorted by alphabet and without any double
	 * entries.
	 * 
	 * @author Stefan F.
	 * @return places as string array.
	 */
	public static TreeSet<String> getAllTourPlaceEnds() {

		if (_dbTourEndPlace == null) {
			_dbTourEndPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourEndPlace"); // $NON-NLS-1$
		}

		return _dbTourEndPlace;
	}

	/**
	 * Getting all tour start places from the database sorted by alphabet and without any double
	 * entries.
	 * 
	 * @author Stefan F.
	 * @return titles as string array.
	 */
	public static TreeSet<String> getAllTourPlaceStarts() {

		if (_dbTourStartPlace == null) {
			_dbTourStartPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourStartPlace"); // $NON-NLS-1$
		}

		return _dbTourStartPlace;
	}

	/**
	 * this method is synchronized to conform to FindBugs
	 * 
	 * @return Returns all tour tags which are stored in the database, the hash key is the tag id
	 */
	public static HashMap<Long, TourTag> getAllTourTags() {

		if (_tourTags != null) {
			return _tourTags;
		}

		synchronized (DB_LOCK) {

			// check again, field must be volatile to work correctly
			if (_tourTags != null) {
				return _tourTags;
			}

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final Query emQuery = em.createQuery("SELECT tourTag FROM TourTag AS tourTag"); //$NON-NLS-1$

				_tourTags = new HashMap<Long, TourTag>();

				final List<?> resultList = emQuery.getResultList();
				for (final Object result : resultList) {

					if (result instanceof TourTag) {
						final TourTag tourTag = (TourTag) result;
						_tourTags.put(tourTag.getTagId(), tourTag);
					}
				}

				em.close();
			}
		}

		return _tourTags;
	}

	/**
	 * Getting all tour titles from the database sorted by alphabet and without any double entries.
	 * 
	 * @author Stefan F.
	 * @return titles as string array.
	 */
	public static TreeSet<String> getAllTourTitles() {

		if (_dbTourTitles == null) {
			_dbTourTitles = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourTitle"); // $NON-NLS-1$
		}

		return _dbTourTitles;
	}

	/**
	 * @return Returns the backend of all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourType> getAllTourTypes() {

		if (_tourTypes != null) {
			return _tourTypes;
		}

		synchronized (DB_LOCK) {

			// check again, field must be volatile to work correctly
			if (_tourTypes != null) {
				return _tourTypes;
			}

			// create empty list
			_tourTypes = new ArrayList<TourType>();

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final Query emQuery = em.createQuery(//
						//
						"SELECT tourType" //$NON-NLS-1$
								+ (" FROM TourType AS tourType") //$NON-NLS-1$
								+ (" ORDER  BY tourType.name")); //$NON-NLS-1$

				_tourTypes = (ArrayList<TourType>) emQuery.getResultList();

				em.close();
			}
		}

		return _tourTypes;
	}

	/**
	 * Getting one row from the database sorted by alphabet and without any double entries.
	 * 
	 * @author Stefan F.
	 * @param sqlQuery
	 *            must look like: "SELECT tourTitle FROM " + TourDatabase.TABLE_TOUR_DATA +
	 *            " ORDER BY tourTitle"
	 * @return places as string array.
	 */
	private static TreeSet<String> getDistinctValues(final String db, final String fieldname) {

		final TreeSet<String> sortedValues = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(final String s1, final String s2) {
				// sort without case
				return s1.compareToIgnoreCase(s2);
			}
		});

		/*
		 * run in UI thread otherwise the busyindicator fails
		 */
		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			public void run() {

				BusyIndicator.showWhile(display, new Runnable() {
			public void run() {

				Connection conn = null;
				Statement stmt = null;
				String sqlQuery = null;

				try {

					conn = getInstance().getConnection();
					stmt = conn.createStatement();

					sqlQuery = "SELECT" //$NON-NLS-1$
							+ " DISTINCT" //$NON-NLS-1$
							+ " " + fieldname //$NON-NLS-1$
							+ " FROM " + db //$NON-NLS-1$
							+ " ORDER BY " + fieldname; //$NON-NLS-1$

					final ResultSet result = stmt.executeQuery(sqlQuery);

					while (result.next()) {

						String dbValue = result.getString(1);
						if (dbValue != null) {

							dbValue = dbValue.trim();

							if (dbValue.length() > 0) {
								sortedValues.add(dbValue);
							}
						}
					}

				} catch (final SQLException e) {
					UI.showSQLException(e);
				} finally {
					Util.sqlClose(stmt);
					closeConnection(conn);
				}

				/*
				 * log existing values
				 */
//				final StringBuilder sb = new StringBuilder();
//				for (final String text : sortedValues) {
//					sb.append(text);
//					sb.append(UI.NEW_LINE);
//				}
//				System.out.println(UI.NEW_LINE2);
//				System.out.println(sqlQuery);
//				System.out.println(UI.NEW_LINE);
//				System.out.println(sb.toString());
//				// TODO remove SYSTEM.OUT.PRINTLN
			}
		});
			}
		});

		return sortedValues;
	}

	public static TourDatabase getInstance() {

		if (_instance != null) {
			return _instance;
		}

		synchronized (DB_LOCK) {
			// check again
			if (_instance == null) {
				_instance = new TourDatabase();
			}
		}

		return _instance;
	}

	private static Connection getPooledConnection() throws SQLException {

		if (_pooledDataSource == null) {

			synchronized (DB_LOCK) {
				// check again
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
						StatusUtil.log(e);
					}
				}
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
		Query emQuery = em.createQuery(//
				//
				"SELECT ttCategory" //$NON-NLS-1$
						+ (" FROM TourTagCategory AS ttCategory") //$NON-NLS-1$
						+ (" WHERE ttCategory.isRoot=1") //$NON-NLS-1$
						+ (" ORDER  BY ttCategory.name")); //$NON-NLS-1$

		rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) emQuery.getResultList();

		/*
		 * read tour tags from db
		 */
		emQuery = em.createQuery(//
				//
				"SELECT tourTag" //$NON-NLS-1$
						+ (" FROM TourTag AS tourTag ") //$NON-NLS-1$
						+ (" WHERE tourTag.isRoot=1") //$NON-NLS-1$
						+ (" ORDER  BY tourTag.name")); //$NON-NLS-1$

		rootEntry.tourTags = (ArrayList<TourTag>) emQuery.getResultList();

		em.close();

		_tagCollections.put(rootTagId, rootEntry);

		return rootEntry;
	}

	/**
	 * @param categoryId
	 * @return Returns a {@link TagCollection} with all tags and categories for the category Id
	 */
	public static TagCollection getTagEntries(final long categoryId) {

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

//	/**
//	 * @return Returns all tour people in the db sorted by last/first name
//	 */
//	@SuppressWarnings("unchecked")
//	public static ArrayList<TourPerson> getTourPeople() {
//
//		ArrayList<TourPerson> tourPeople = new ArrayList<TourPerson>();
//
//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//		if (em != null) {
//
//			final Query emQuery = em.createQuery(//
//					//
//					"SELECT TourPerson" //$NON-NLS-1$
//							+ (" FROM TourPerson AS TourPerson") //$NON-NLS-1$
//							+ (" ORDER BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$
//
//			tourPeople = (ArrayList<TourPerson>) emQuery.getResultList();
//
//			em.close();
//		}
//
//		return tourPeople;
//	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query emQuery = em.createQuery(//
					//
					"SELECT tourBike" //$NON-NLS-1$
							+ (" FROM TourBike AS tourBike ") //$NON-NLS-1$
							+ (" ORDER  BY tourBike.name")); //$NON-NLS-1$

			bikeList = (ArrayList<TourBike>) emQuery.getResultList();

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
	 * Checks if a field exceeds the max length
	 * 
	 * @param field
	 * @param maxLength
	 * @param uiFieldName
	 * @param isForceTruncation
	 * @return Returns {@link FIELD_VALIDATION} status
	 */
	public static FIELD_VALIDATION isFieldValidForSave(	final String field,
														final int maxLength,
														final String uiFieldName,
														final boolean isForceTruncation) {

		final FIELD_VALIDATION[] returnValue = { FIELD_VALIDATION.IS_VALID };

		if (field != null && field.length() > maxLength) {

			Display.getDefault().syncExec(new Runnable() {
				public void run() {

					if (isForceTruncation) {
						returnValue[0] = FIELD_VALIDATION.TRUNCATE;
						StatusUtil.log(new Exception(NLS.bind(
								"Field \"{0}\" with content \"{1}\" is truncated to {2} characters.",//$NON-NLS-1$
								new Object[] { uiFieldName, field, maxLength })));
						return;
					}

					if (MessageDialog.openConfirm(
							Display.getDefault().getActiveShell(),
							Messages.Tour_Database_Dialog_ValidateFields_Title,
							NLS.bind(Messages.Tour_Database_Dialog_ValidateFields_Message, //
									new Object[] { uiFieldName, field.length(), maxLength }))) {

						returnValue[0] = FIELD_VALIDATION.TRUNCATE;
					} else {
						returnValue[0] = FIELD_VALIDATION.IS_INVALID;
					}
				}
			});
		}

		return returnValue[0];
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

		Connection conn = null;
		PreparedStatement prepStmt = null;

		String sql = UI.EMPTY_STRING;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sqlWhereTourDataTourId = " WHERE " + TABLE_TOUR_DATA + "_tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * tour data
			 */
			sql = "DELETE FROM " + TABLE_TOUR_DATA + " WHERE tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			/*
			 * tour marker
			 */
			sql = "DELETE FROM " + TABLE_TOUR_MARKER + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			sql = "DELETE FROM " + JOINTABLE_TOURDATA__TOURMARKER + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			/*
			 * tour way point
			 */
			sql = "DELETE FROM " + TABLE_TOUR_WAYPOINT + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			sql = "DELETE FROM " + JOINTABLE_TOURDATA__TOURWAYPOINT + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			/*
			 * reference tour
			 */
			sql = "DELETE FROM " + TABLE_TOUR_REFERENCE + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			sql = "DELETE FROM " + JOINTABLE_TOURDATA__TOURREFERENCE + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			/*
			 * tour tags
			 */
			sql = "DELETE FROM " + JOINTABLE_TOURDATA__TOURTAG + sqlWhereTourDataTourId; //$NON-NLS-1$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

			/*
			 * compared tour
			 */
			sql = "DELETE FROM " + TABLE_TOUR_COMPARED + " WHERE tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$
			prepStmt = conn.prepareStatement(sql);
			prepStmt.setLong(1, tourId);
			prepStmt.execute();
			prepStmt.close();

//			/*
//			 * OLD unused table: tour category
//			 */
//			sqlString = ("DELETE FROM " + TABLE_TOUR_CATEGORY) + (" WHERE " + TABLE_TOUR_DATA + "tourId=?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			prepStmt = conn.prepareStatement(sqlString);
//			prepStmt.setLong(1, tourId);
//			prepStmt.execute();
//
//			sqlString = ("DELETE FROM " + JOINTABLE_TOURCATEGORY__TOURDATA) + sqlWhereTourDataTourId; //$NON-NLS-1$
//			prepStmt = conn.prepareStatement(sqlString);
//			prepStmt.setLong(1, tourId);
//			prepStmt.execute();

		} catch (final SQLException e) {
			System.out.println(sql);
			UI.showSQLException(e);
		} finally {

			closeConnection(conn);
		}
	}

	/**
	 * Persists an entity *
	 * <p>
	 * This method is <b>much faster</b> than using this
	 * {@link #saveEntity(Object, long, Class, EntityManager)}
	 * <p>
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
			StatusUtil.showStatus(e);
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
	 * Persists an entity, an error is logged when saving fails.
	 * <p>
	 * This method is <b>much slower</b> than using this {@link #saveEntity(Object, long, Class)}
	 * method without using the same EntityManater.
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
			StatusUtil.showStatus(e);
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
	 * @param isUpdateModifiedDate
	 *            When <code>true</code> the modified date is updated. For updating computed field
	 *            it does not make sence to set the modified date.
	 * @return persisted {@link TourData} or <code>null</code> when saving fails
	 */
	public static TourData saveTour(final TourData tourData, final boolean isUpdateModifiedDate) {

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

		/*
		 * check size of varcar fields
		 */
		if (tourData.isValidForSave() == false) {
			return null;
		}

		final DateTime dtNow = new DateTime();

		final long dtSaved = (dtNow.getYear() * 10000000000L)
				+ (dtNow.getMonthOfYear() * 100000000L)
				+ (dtNow.getDayOfMonth() * 1000000L)
				//
				+ (dtNow.getHourOfDay() * 10000L)
				+ (dtNow.getMinuteOfHour() * 100L)
				+ dtNow.getSecondOfMinute();

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

						tourData.setDateTimeCreated(dtSaved);

						em.persist(tourData);

						persistedEntity = tourData;

					} else {

						if (isUpdateModifiedDate) {
							tourData.setDateTimeModified(dtSaved);
						}

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
				StatusUtil.log(e);
			}

			em.close();

			TourManager.getInstance().updateTourInCache(persistedEntity);

			updateCachedFields(persistedEntity);
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

	private static void updateCachedFields(final TourData tourData) {

		// cache tour title
		final TreeSet<String> allTitles = getAllTourTitles();
		final String tourTitle = tourData.getTourTitle();
		if (tourTitle.length() > 0) {
			allTitles.add(tourTitle);
		}

		// cache tour start place
		final TreeSet<String> allPlaceStarts = getAllTourPlaceStarts();
		final String tourStartPlace = tourData.getTourStartPlace();
		if (tourStartPlace.length() > 0) {
			allPlaceStarts.add(tourStartPlace);
		}

		// cache tour end place
		final TreeSet<String> allPlaceEnds = getAllTourPlaceEnds();
		final String tourEndPlace = tourData.getTourEndPlace();
		if (tourEndPlace.length() > 0) {
			allPlaceEnds.add(tourEndPlace);
		}

		// cache tour marker names
		final TreeSet<String> allMarkerNames = getAllTourMarkerNames();
		final Set<TourMarker> allTourMarker = tourData.getTourMarkers();
		for (final TourMarker tourMarker : allTourMarker) {
			final String label = tourMarker.getLabel();
			if (label.length() > 0) {
				allMarkerNames.add(label);
			}
		}
	}

	/**
	 * Update calendar week for all tours
	 * 
	 * @param conn
	 * @param monitor
	 * @param firstDayOfWeek
	 * @param minimalDaysInFirstWeek
	 * @return Returns <code>true</code> when the week is computed
	 * @throws SQLException
	 */
	public static boolean updateTourWeek(	final Connection conn,
											final IProgressMonitor monitor,
											final int firstDayOfWeek,
											final int minimalDaysInFirstWeek) throws SQLException {

		final ArrayList<Long> tourList = getAllTourIds();

		boolean isUpdated = false;

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
				final String msg = NLS.bind(
						Messages.Tour_Database_Update_TourWeek,
						new Object[] { tourIdx++, tourList.size() });
				monitor.subTask(msg);
			}

			// get tour date
			stmtSelect.setLong(1, tourId);
//				stmtSelect.execute();

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

		return isUpdated;
	}

	public void addPropertyListener(final IPropertyListener listener) {
		_propertyListeners.add(listener);
	}

	private boolean checkDb() {

		try {
			checkServer();
		} catch (final Throwable e) {
			StatusUtil.log(e);
			return false;
		}

		checkTable(null);

		if (checkVersion(null) == false) {
			return false;
		}

		return true;
	}

	/**
	 * Check if the server is available
	 * 
	 * @throws Throwable
	 * @throws MyTourbookException
	 */
	private void checkServer() throws Throwable {

		// when the server is started, nothing is to do here
		if (_server != null) {
			return;
		}

		// check if the derby driver can be loaded
		try {
			Class.forName(DERBY_CLIENT_DRIVER);
		} catch (final ClassNotFoundException e) {
			StatusUtil.showStatus(e.getMessage(), e);
			return;
		}

		try {

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getSplashHandler();

			checkServerCreateRunnable().run(splashHandler == null ? null : splashHandler.getBundleProgressMonitor());

		} catch (final InvocationTargetException e) {

			StatusUtil.log(e);

			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					Messages.Tour_Database_CannotConnectToDerbyServer_Title,
					NLS.bind(Messages.Tour_Database_CannotConnectToDerbyServer_Message, e
							.getTargetException()
							.getMessage()));

			PlatformUI.getWorkbench().close();

			throw e.getTargetException();

		} catch (final InterruptedException e) {
			StatusUtil.log(e);
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
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				if (monitor != null) {
					monitor.subTask(createUIServerStateMessage(0));
				}

				try {
					_server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
				} catch (final UnknownHostException e) {
					StatusUtil.log(e);
				} catch (final Exception e) {
					StatusUtil.log(e);
				}

				try {

					/*
					 * check if another derby server is already running (this can happen during
					 * development)
					 */
					StatusUtil.logInfo("checking if derby server is already running before server.start");//$NON-NLS-1$
					_server.ping();

				} catch (final Exception e) {

					try {
						StatusUtil.logInfo("starting derby server");//$NON-NLS-1$

						_server.start(null);

					} catch (final Exception e2) {
						StatusUtil.log(e2);
					}

					StatusUtil.logInfo("checking if derby server is running after server.start");//$NON-NLS-1$

					int pingCounter = 1;
					final int threadSleepTime = 100;

					// wait until the server is started
					while (true) {

						try {

							if (monitor != null) {
								monitor.subTask(createUIServerStateMessage(pingCounter));
							}
							_server.ping();

							StatusUtil.logInfo("derby server has started");//$NON-NLS-1$

							break;

						} catch (final Exception e1) {

							if (pingCounter > MAX_TRIES_TO_PING_SERVER) {

								StatusUtil.log("Cannot connect to derby server", e1);//$NON-NLS-1$

								throw new InvocationTargetException(e1);
							}

							StatusUtil.logInfo(NLS.bind("...waiting ({0} ms) for derby server startup: {1}", //$NON-NLS-1$
									threadSleepTime,
									pingCounter++));

							try {
								Thread.sleep(threadSleepTime);
							} catch (final InterruptedException e2) {
								StatusUtil.log(e2);
							}
						}
					}

					// make the first connection, this takes longer as the subsequent ones
					try {

						if (monitor != null) {
							monitor.subTask(Messages.Database_Monitor_SetupPooledConnection);
						}

						final Connection connection = getPooledConnection();
						connection.close();

						// log database path
						StatusUtil.logInfo("Database path: " + _databasePath); //$NON-NLS-1$

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
	 * 
	 * @param monitor
	 */
	private void checkTable(final IProgressMonitor monitor) {

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
					// table exists
					return;
				}
			}

			if (monitor != null) {
				monitor.subTask(Messages.Database_Monitor_CreateDatabase);
			}

			Statement stmt = null;

			try {

				stmt = conn.createStatement();

				createTableTourData(stmt);

				createTableTourPerson(stmt);
				createTableTourPersonHRZone(stmt);
				createTableTourType(stmt);
				createTableTourMarker(stmt);
				createTableTourReference(stmt);
				createTableTourCompared(stmt);
				createTableTourBike(stmt);

				createTableVersion(stmt);

				createTableTourTag(stmt);
				createTableTourTagCategory(stmt);

				createTableTourWayPoint(stmt);

				_isTableChecked = true;

			} catch (final SQLException e) {
				UI.showSQLException(e);
			} finally {
				Util.sqlClose(stmt);
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

		if (_isSQLUpdateError) {
			return false;
		}

		Connection conn = null;
		Statement stmt1 = null;
		Statement stmt2 = null;

		try {

			conn = getPooledConnection();
			{
				String sql = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$

				stmt1 = conn.createStatement();
				final ResultSet result = stmt1.executeQuery(sql);

				if (result.next()) {

					// version record was found, check if the database contains the correct version

					_dbVersionBeforeUpdate = result.getInt(1);

					StatusUtil.logInfo("Database version: " + _dbVersionBeforeUpdate); //$NON-NLS-1$

					if (_dbVersionBeforeUpdate < TOURBOOK_DB_VERSION) {

						if (updateDbDesign(conn, _dbVersionBeforeUpdate, monitor) == false) {
							return false;
						}

					} else if (_dbVersionBeforeUpdate > TOURBOOK_DB_VERSION) {

						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.tour_database_version_info_title,
								NLS.bind(
										Messages.tour_database_version_info_message,
										_dbVersionBeforeUpdate,
										TOURBOOK_DB_VERSION));
					}

				} else {

					// a version record is not available

					/*
					 * insert the version for the current database design into the database
					 */
					sql = "INSERT INTO " + TABLE_DB_VERSION //								//$NON-NLS-1$
							+ " VALUES (" + Integer.toString(TOURBOOK_DB_VERSION) + ")"; //			//$NON-NLS-1$ //$NON-NLS-2$

					stmt2 = conn.createStatement();
					stmt2.executeUpdate(sql);
				}
			}

			_isVersionChecked = true;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			try {
				conn.close();
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
			Util.sqlClose(stmt1);
			Util.sqlClose(stmt2);
		}

		return true;
	}

//	/**
//	 * create table {@link #TABLE_TOUR_CATEGORY}
//	 *
//	 * @param stmt
//	 * @throws SQLException
//	 */
//	private void createTableTourCategory(final Statement stmt) throws SQLException {
//
//		// CREATE TABLE TourCategory
//		stmt.execute("" //$NON-NLS-1$
//				+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
//				+ "(" //$NON-NLS-1$
//				+ "categoryId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
//				+ (TABLE_TOUR_DATA + "tourId	BIGINT,") //$NON-NLS-1$
//				+ "category 					VARCHAR(100)" //$NON-NLS-1$
//				+ ")"); //$NON-NLS-1$
//
//		// ALTER TABLE TourCategory ADD CONSTRAINT TourCategory_pk PRIMARY KEY (categoryId);
//		stmt.execute("" //$NON-NLS-1$
//				+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
//				+ (" ADD CONSTRAINT " + (TABLE_TOUR_CATEGORY + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
//				+ (" PRIMARY KEY (categoryId)")); //$NON-NLS-1$
//
//		// CREATE TABLE TourCategory_TourData
//		stmt.execute("" //$NON-NLS-1$
//				+ ("CREATE TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$
//				+ "(" //$NON-NLS-1$
//				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
//				+ (TABLE_TOUR_CATEGORY + "_categoryId	BIGINT NOT NULL") //$NON-NLS-1$
//				+ ")"); //$NON-NLS-1$
//
//		// ALTER TABLE TourCategory_TourData ADD CONSTRAINT TourCategory_TourData_pk PRIMARY KEY (tourCategory_categoryId);
//		stmt.execute("" //$NON-NLS-1$
//				+ ("ALTER TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$
//				+ (" ADD CONSTRAINT " + JOINTABLE_TOURCATEGORY__TOURDATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
//				+ (" PRIMARY KEY (" + TABLE_TOUR_CATEGORY + "_categoryId)")); //$NON-NLS-1$ //$NON-NLS-2$
//	}

	/**
	 * Create index for {@link TourData} will dramatically improve performance *
	 * <p>
	 * since db version 5
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createIndexTourData(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE INDEX YearMonth
		 */
		sql = "CREATE INDEX YearMonth" + " ON " + TABLE_TOUR_DATA + " (startYear, startMonth)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		/*
		 * CREATE INDEX TourType
		 */
		sql = "CREATE INDEX TourType" + " ON " + TABLE_TOUR_DATA + " (tourType_typeId)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		/*
		 * CREATE INDEX TourPerson
		 */
		sql = "CREATE INDEX TourPerson" + " ON " + TABLE_TOUR_DATA + " (tourPerson_personId)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_BIKE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourBike(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourBike
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_BIKE //									//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	bikeId	 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1), \n" //$NON-NLS-1$
				+ "	name			" + varCharKomma(TourBike.DB_LENGTH_NAME) //	//$NON-NLS-1$
				+ "	weight 			FLOAT,										\n" //$NON-NLS-1$ // kg
				+ "	typeId 			INTEGER,									\n" //$NON-NLS-1$
				+ "	frontTyreId	 	INTEGER,									\n" //$NON-NLS-1$
				+ "	rearTyreId 		INTEGER										\n" //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourBike
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_BIKE + "								\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_BIKE + "_pk					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (bikeId)"; //										//$NON-NLS-1$

		exec(stmt, sql);

	}

	/**
	 * create table {@link #TABLE_TOUR_COMPARED}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourCompared(final Statement stmt) throws SQLException {

		String sql;

		// CREATE TABLE TourCompared
		sql = "CREATE TABLE " + TABLE_TOUR_COMPARED //								//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	comparedId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	refTourId		BIGINT,										\n" //$NON-NLS-1$
				+ "	tourId			BIGINT,										\n" //$NON-NLS-1$
				+ "	startIndex		INTEGER NOT NULL,							\n" //$NON-NLS-1$
				+ "	endIndex 		INTEGER NOT NULL,							\n" //$NON-NLS-1$
				+ "	tourDate	 	DATE NOT NULL,								\n" //$NON-NLS-1$
				+ "	startYear		INTEGER NOT NULL,							\n" //$NON-NLS-1$
				+ "	tourSpeed	 	FLOAT										\n" //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_DATA}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourData(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourData
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_DATA //										//$NON-NLS-1$
				+ "(																\n" //$NON-NLS-1$
				//
				+ "	tourId 					BIGINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startYear 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startMonth 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startDay 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startHour 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startMinute 			SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startWeek 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startDistance 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	distance 				INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	startAltitude 			SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	startPulse 				SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	dpTolerance 			SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	tourDistance 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	tourRecordingTime 		INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	tourDrivingTime 		INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	tourAltUp 				INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	tourAltDown 			INTEGER NOT NULL,						\n" //$NON-NLS-1$

				+ ("	deviceTourType 		" + varCharKomma(TourData.DB_LENGTH_DEVICE_TOUR_TYPE)) //$NON-NLS-1$
				+ ("	devicePluginId		" + varCharKomma(TourData.DB_LENGTH_DEVICE_PLUGIN_ID)) //$NON-NLS-1$

				+ "	deviceTravelTime 		BIGINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	deviceDistance 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	deviceWheel 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	deviceWeight 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	deviceTotalUp 			INTEGER NOT NULL,						\n" //$NON-NLS-1$
				+ "	deviceTotalDown 		INTEGER NOT NULL,						\n" //$NON-NLS-1$

				// version 3 start
				+ "	deviceMode 				SMALLINT,								\n" //$NON-NLS-1$
				+ "	deviceTimeInterval		SMALLINT,								\n" //$NON-NLS-1$
				// version 3 end

				// version 4 start

				// from markus
// replaced with float values in version 21
//				+ "	maxAltitude				INTEGER,								\n" //$NON-NLS-1$
//				+ "	maxPulse				INTEGER,								\n" //$NON-NLS-1$
//				+ "	avgPulse				INTEGER,								\n" //$NON-NLS-1$
//				+ "	avgCadence				INTEGER,								\n" //$NON-NLS-1$
//				+ "	avgTemperature			INTEGER,								\n" //$NON-NLS-1$
				+ "	maxSpeed				FLOAT,									\n" //$NON-NLS-1$
				+ ("	tourTitle			" + varCharKomma(TourData.DB_LENGTH_TOUR_TITLE)) //$NON-NLS-1$

// OLD			+ "tourDescription		VARCHAR(4096),								\n" 		   // version <= 9
				+ ("	tourDescription		" + varCharKomma(TourData.DB_LENGTH_TOUR_DESCRIPTION_V10)) // modified in version 10 //$NON-NLS-1$

				+ ("	tourStartPlace		" + varCharKomma(TourData.DB_LENGTH_TOUR_START_PLACE)) //$NON-NLS-1$
				+ ("	tourEndPlace		" + varCharKomma(TourData.DB_LENGTH_TOUR_END_PLACE)) //$NON-NLS-1$
				+ "	calories				INTEGER,								\n" //$NON-NLS-1$
				+ "	bikerWeight				FLOAT,									\n" //$NON-NLS-1$
				+ "	tourBike_bikeId			BIGINT,									\n" //$NON-NLS-1$

				// from wolfgang
				+ ("	devicePluginName	" + varCharKomma(TourData.DB_LENGTH_DEVICE_PLUGIN_NAME)) //$NON-NLS-1$
				+ ("	deviceModeName		" + varCharKomma(TourData.DB_LENGTH_DEVICE_MODE_NAME)) //$NON-NLS-1$

				// version 4 end

				// version 5 start
				/**
				 * disabled because when two blob object's are deserialized then the error occures:
				 * <p>
				 * java.io.StreamCorruptedException: invalid stream header: 00ACED00
				 * <p>
				 * therefor the gpsData are put into the serieData object
				 */
				//	+ "gpsData 				BLOB,								\n" //$NON-NLS-1$
				//
				// version 5 end
				//
				+ "	tourType_typeId 		BIGINT,								\n" //$NON-NLS-1$
				+ "	tourPerson_personId 	BIGINT,								\n" //$NON-NLS-1$

				// version 6 start
				//
				+ ("	tourImportFilePath		" + varCharKomma(TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH)) //$NON-NLS-1$
				//
				// version 6 end

				// version 7 start
				//
				+ "	mergeSourceTourId		BIGINT,								\n" //$NON-NLS-1$
				+ "	mergeTargetTourId		BIGINT,								\n" //$NON-NLS-1$
				+ "	mergedTourTimeOffset	INTEGER DEFAULT 0,					\n" //$NON-NLS-1$
				+ "	mergedAltitudeOffset	INTEGER DEFAULT 0,					\n" //$NON-NLS-1$
				+ "	startSecond	 			SMALLINT DEFAULT 0,					\n" //$NON-NLS-1$
				//
				// version 7 end

				// version 8 start
				//
				+ "	weatherWindDir			INTEGER DEFAULT 0,					\n" //$NON-NLS-1$
				+ "	weatherWindSpd			INTEGER DEFAULT 0,					\n" //$NON-NLS-1$
				+ ("	weatherClouds    	" + varCharKomma(TourData.DB_LENGTH_WEATHER_CLOUDS)) //$NON-NLS-1$
				+ "	restPulse        	    INTEGER DEFAULT 0,					\n" //$NON-NLS-1$
				+ "	isDistanceFromSensor 	SMALLINT DEFAULT 0, 				\n" //$NON-NLS-1$
				//
				// version 8 end ----------

				// version 9 start
				//
				+ "	startWeekYear			SMALLINT DEFAULT 1977,				\n" //$NON-NLS-1$
				//
				// version 9 end ----------

				// version 10 start
				//
				// tourWayPoints is mapped in TourData
				//
				// version 10 end----------

				// version 11 start
				//
				+ "	DateTimeCreated				BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
				+ "	DateTimeModified			BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
				//
				// version 11 end ---------

				// version 12 start
				//
				+ "	IsPulseSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
				+ "	IsPowerSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
				+ "	DeviceAvgSpeed				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				+ ("	DeviceFirmwareVersion	" + varCharKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION)) //$NON-NLS-1$
				//
				// version 12 end ---------

				// version 13 start
				//
				+ "	TemperatureScale			INTEGER DEFAULT 1, 				\n" //$NON-NLS-1$
				+ " Weather 					" + varCharKomma(TourData.DB_LENGTH_WEATHER) //$NON-NLS-1$
				//
				// version 13 end ---------

				// version 14 start
				//
				+ "	ConconiDeflection			INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
				//
				// version 14 end ---------

				// version 17 start
				//
				+ "	hrZone0						INTEGER DEFAULT -1,				\n" //$NON-NLS-1$
				+ "	hrZone1						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone2						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone3						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone4						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone5						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone6						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone7						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone8						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				+ "	hrZone9						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
				//
				// version 17 end ---------

				// version 18 start
				//
				+ "	NumberOfHrZones				INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
				//
				// version 18 end ---------

				// version 21 start
				//
				+ "	maxAltitude					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				+ "	maxPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				+ "	avgPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				+ "	avgCadence					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				+ "	avgTemperature				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				//
				// version 21 end ---------

				+ "	serieData					BLOB 							\n" //$NON-NLS-1$

				+ ")"; //														//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourData ADD CONSTRAINT TourData_pk PRIMARY KEY (tourId);
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_DATA + "								\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_DATA + "_pk					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (tourId)"; //										//$NON-NLS-1$

		exec(stmt, sql);

		createIndexTourData(stmt);
	}

	/**
	 * create table {@link #TABLE_TOUR_MARKER}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourMarker(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourMarker
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_MARKER //								//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	markerId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	" + (TABLE_TOUR_DATA + "_tourId	BIGINT,						\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "	time 						INTEGER NOT NULL,				\n" //$NON-NLS-1$

				// before version 20
//				+ "	distance 					INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ "	distance 					INTEGER,						\n" //$NON-NLS-1$

				// Version 20 - begin
				//
				+ "	distance20 					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
				//
				// Version 20 - end

				+ "	serieIndex 					INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ "	type 						INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ "	visualPosition				INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ ("	label 					" + varCharKomma(TourMarker.DB_LENGTH_LABEL)) //$NON-NLS-1$
				+ ("	category 				" + varCharKomma(TourMarker.DB_LENGTH_CATEGORY)) //$NON-NLS-1$
				//
				// Version 2
				+ "	labelXOffset				INTEGER,						\n" //$NON-NLS-1$
				+ "	labelYOffset				INTEGER,						\n" //$NON-NLS-1$
				+ "	markerType					BIGINT							\n" //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourMarker ADD CONSTRAINT TourMarker_pk PRIMARY KEY (markerId);
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_MARKER + "							\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_MARKER + "_pk					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (markerId)"; //										//$NON-NLS-1$

		exec(stmt, sql);

		sql = "CREATE TABLE " + JOINTABLE_TOURDATA__TOURMARKER //							//$NON-NLS-1$
				+ "(																	\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,		\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_MARKER + "_markerId		BIGINT NOT NULL			\n")//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //																//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourData_TourMarker ADD CONSTRAINT TourData_TourMarker_pk PRIMARY KEY
		 * (TourData_tourId);
		 */
		sql = "ALTER TABLE " + JOINTABLE_TOURDATA__TOURMARKER + "				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURMARKER + "_pk	\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"; //				//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_PERSON}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourPerson(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourPerson
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_PERSON //								//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	personId 			BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ ("	lastName 		" + varCharKomma(TourPerson.DB_LENGTH_LAST_NAME)) //$NON-NLS-1$
				+ ("	firstName 		" + varCharKomma(TourPerson.DB_LENGTH_FIRST_NAME)) //$NON-NLS-1$
				+ "	weight 				FLOAT,									\n" //$NON-NLS-1$ // kg
				+ "	height 				FLOAT,									\n" //$NON-NLS-1$ // m

				// version 15 start
				//
				+ "	BirthDay			BIGINT DEFAULT 0,						\n" //$NON-NLS-1$
				//
				// version 15 end ---------

				// version 16 start
				//
				+ "	Gender				INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
				+ "	RestPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
				+ "	MaxPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
				+ "	HrMaxFormula		INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
				//
				// version 16 end ---------

				+ ("	rawDataPath		" + varCharKomma(TourPerson.DB_LENGTH_RAW_DATA_PATH)) //$NON-NLS-1$
				+ ("	deviceReaderId	" + varCharKomma(TourPerson.DB_LENGTH_DEVICE_READER_ID)) //$NON-NLS-1$
				+ "	tourBike_bikeId 	BIGINT									\n" //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourPerson
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_PERSON + "							\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_PERSON + "_pk					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (personId)"; //										//$NON-NLS-1$

		exec(stmt, sql);
	}

	/**
	 * Create table {@link #TABLE_TOUR_PERSON_HRZONE}
	 * <p>
	 * Table is available since db version 16
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourPersonHRZone(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourPersonHRZone
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_PERSON_HRZONE //						//$NON-NLS-1$
				+ "(														\n" //$NON-NLS-1$
				//
				+ "	hrZoneId			BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	" + (TABLE_TOUR_PERSON + "_personId	BIGINT,				\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ("	zoneName		" + varCharKomma(TourPersonHRZone.DB_LENGTH_ZONE_NAME)) //$NON-NLS-1$
				+ ("	nameShortcut	" + varCharKomma(TourPersonHRZone.DB_LENGTH_ZONE_NAME)) //$NON-NLS-1$
				+ ("	description		" + varCharKomma(TourPersonHRZone.DB_LENGTH_DESCRIPTION)) //$NON-NLS-1$

				// version 18 start
				//
				+ "	ColorRed			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
				+ "	ColorGreen			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
				+ "	ColorBlue			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
				//
				// version 18 end ---------

				//
				+ "	zoneMinValue		INTEGER NOT NULL,					\n" //$NON-NLS-1$
				+ "	zoneMaxValue		INTEGER NOT NULL					\n" //$NON-NLS-1$
				//
				+ ")"; //														//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourPersonHRZone ADD CONSTRAINT TourPersonHRZone_pk PRIMARY KEY (hrZoneId);
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + "					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_PERSON_HRZONE + "_pk		\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (hrZoneId)"; //									//$NON-NLS-1$

		exec(stmt, sql);

		sql = "CREATE TABLE " + JOINTABLE_TOURPERSON__TOURPERSON_HRZONE //							//$NON-NLS-1$
				+ "(																			\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_PERSON + "_personId				BIGINT NOT NULL,		\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_PERSON_HRZONE + "_hrZoneId		BIGINT NOT NULL			\n")//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //																			//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourPerson_TourPersonHRZone ADD CONSTRAINT TourPerson_TourPersonHRZone_pk
		 * PRIMARY KEY (TourPerson_personId);
		 */
		sql = "ALTER TABLE " + JOINTABLE_TOURPERSON__TOURPERSON_HRZONE + "						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + JOINTABLE_TOURPERSON__TOURPERSON_HRZONE + "_pk			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (" + TABLE_TOUR_PERSON + "_personId)"; //							//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_REFERENCE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourReference(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourReference
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_REFERENCE //								//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	refId 						BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ ("	" + TABLE_TOUR_DATA + "_tourId	BIGINT,					\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ "	startIndex					INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ "	endIndex 					INTEGER NOT NULL,				\n" //$NON-NLS-1$
				+ ("	label 					" + varCharNoKomma(TourReference.DB_LENGTH_LABEL)) //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourReference ADD CONSTRAINT TourReference_pk PRIMARY KEY (refId);
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_REFERENCE + "							\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_REFERENCE + "_pk				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (refId)"; //										//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * CREATE TABLE TourData_TourReference
		 */
		sql = "CREATE TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE //					//$NON-NLS-1$
				+ "(																\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,	\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_REFERENCE + "_refId 		BIGINT NOT NULL		\n")//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //																//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourData_TourReference ADD CONSTRAINT TourData_TourReference_pk PRIMARY KEY
		 * (TourData_tourId);
		 */
		sql = "ALTER TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE + "				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURREFERENCE + "_pk 	\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"; //					//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_TAG} *
	 * <p>
	 * since db version 5
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourTag(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * creates the tables for the tour tags for VERSION 5
		 */

		/*
		 * CREATE TABLE TourTag
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_TAG //									//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	tagId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	isRoot 		INTEGER,										\n" //$NON-NLS-1$
				+ "	expandType 	INTEGER,										\n" //$NON-NLS-1$
				+ ("	name 	" + varCharNoKomma(TourTag.DB_LENGTH_NAME)) //		//$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourTag ADD CONSTRAINT TourTag_pk PRIMARY KEY (refId);
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_TAG + "								\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_TAG + "_pk 					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (tagId)"; //										//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * CREATE TABLE TourData_TourTag
		 */
		final String field_TourData_tourId = TABLE_TOUR_DATA + "_tourId"; //		//$NON-NLS-1$
		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId"; //			//$NON-NLS-1$

		sql = "CREATE TABLE " + JOINTABLE_TOURDATA__TOURTAG //						//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_TAG + "_tagId" + "		BIGINT NOT NULL,	\n")//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("	" + TABLE_TOUR_DATA + "_tourId" + "		BIGINT NOT NULL		\n")//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * Add Constrainsts
		 */
		sql = "ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG + "					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourTag_tagId + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "	FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId) 					\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG + " (tagId)"; //					//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);

		sql = "ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG + "					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourData_tourId //$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + TABLE_TOUR_DATA + "_tourId)				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_DATA + " (tourId)"; //			//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * *
	 * <p>
	 * since db version 5
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourTagCategory(final Statement stmt) throws SQLException {

		/*
		 * creates the tables for the tour tag categories for VERSION 5
		 */

		String sql;

		/*
		 * TABLE TourTagCategory
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY //							//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	tagCategoryId 	BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	isRoot 			INTEGER,									\n" //$NON-NLS-1$
				+ ("	name 		" + varCharNoKomma(TourTagCategory.DB_LENGTH_NAME)) //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		sql = "ALTER TABLE " + TABLE_TOUR_TAG_CATEGORY + "						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_TAG_CATEGORY + "_pk			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (tagCategoryId)"; //								//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * JOIN TABLE TourTagCategory_TourTag
		 */

		sql = "CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG //					//$NON-NLS-1$
				+ "(																\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_TAG + "_tagId						BIGINT NOT NULL,\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId		BIGINT NOT NULL	\n")//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //																//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * Add constraints
		 */
		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId"; //			//$NON-NLS-1$
		final String field_TourTagCategory_tagCategoryId = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId"; //$NON-NLS-1$

		sql = "ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTag_tagId + "\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "	FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId)							\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG + " (tagId)"; //							//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);

		sql = "ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTagCategory_tagCategoryId + "\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "	FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId)			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)"; //			//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);

		/*
		 * JOIN TABLE TourTagCategory_TourTagCategory
		 */
		sql = "CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY //		//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1	BIGINT NOT NULL,\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2	BIGINT NOT NULL	\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * Add constraints
		 */
		final String field_TourTagCategory_tagCategoryId1 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1"; //$NON-NLS-1$
		final String field_TourTagCategory_tagCategoryId2 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2"; //$NON-NLS-1$

		sql = "ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	ADD CONSTRAINT fk_" //													//$NON-NLS-1$
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_" + field_TourTagCategory_tagCategoryId1 + "				\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1)		\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)			\n";//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);

		sql = "ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	ADD CONSTRAINT fk_" //													//$NON-NLS-1$
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_" //															//$NON-NLS-1$
						+ field_TourTagCategory_tagCategoryId2 + "						\n") //$NON-NLS-1$
				+ "	FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2)		\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)			\n";//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_TYPE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourType(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourType
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_TYPE //									//$NON-NLS-1$
				+ "(															\n" //$NON-NLS-1$
				//
				+ "	typeId 				BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ ("	name 			" + varCharKomma(TourType.DB_LENGTH_NAME)) //$NON-NLS-1$
				+ "	colorBrightRed 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorBrightGreen 	SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorBrightBlue 	SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorDarkRed 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorDarkGreen 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorDarkBlue 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorLineRed 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorLineGreen 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$
				+ "	colorLineBlue 		SMALLINT NOT NULL,						\n" //$NON-NLS-1$

				// version 19 start
				//
				+ "	colorTextRed 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
				+ "	colorTextGreen 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
				+ "	colorTextBlue 		SMALLINT DEFAULT 0						\n" //$NON-NLS-1$
				//
				// version 19 end ---------
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourType
		 */
		sql = "ALTER TABLE " + TABLE_TOUR_TYPE + "								\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + TABLE_TOUR_TYPE + "_pk					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	PRIMARY KEY (typeId)"; //										//$NON-NLS-1$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_WAYPOINT} *
	 * <p>
	 * since db version 10
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourWayPoint(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE TourWayPoint
		 */
		sql = "CREATE TABLE " + TABLE_TOUR_WAYPOINT //												//$NON-NLS-1$
				+ "(																			\n" //$NON-NLS-1$
				//
				+ "	wayPointId 			BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ ("	" + TABLE_TOUR_DATA + "_tourId	BIGINT,										\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "	latitude 			DOUBLE NOT NULL,										\n" //$NON-NLS-1$
				+ "	longitude 			DOUBLE NOT NULL,										\n" //$NON-NLS-1$
				+ "	time				BIGINT,													\n" //$NON-NLS-1$
				+ "	altitude			FLOAT,													\n" //$NON-NLS-1$
				+ ("	name			" + varCharKomma(TourWayPoint.DB_LENGTH_NAME)) //			//$NON-NLS-1$
				+ ("	description		" + varCharKomma(TourWayPoint.DB_LENGTH_DESCRIPTION)) //	//$NON-NLS-1$
				+ ("	comment			" + varCharKomma(TourWayPoint.DB_LENGTH_COMMENT)) //		//$NON-NLS-1$
				+ ("	symbol			" + varCharKomma(TourWayPoint.DB_LENGTH_SYMBOL)) //			//$NON-NLS-1$
				+ ("	category		" + varCharNoKomma(TourWayPoint.DB_LENGTH_CATEGORY)) //		//$NON-NLS-1$
				//
				+ ")"; //																			//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourWayPoint ADD CONSTRAINT TourWayPoint_pk PRIMARY KEY (wayPointId);
		 */
		sql = ("ALTER TABLE " + TABLE_TOUR_WAYPOINT) + "									\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	ADD CONSTRAINT " + TABLE_TOUR_WAYPOINT + "_pk 							\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	PRIMARY KEY (wayPointId)"); //												//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * CREATE TABLE TourData_TourWayPoint
		 */
		sql = ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURWAYPOINT) //							//$NON-NLS-1$
				+ "(																		\n" //$NON-NLS-1$
				//
				+ ("	" + TABLE_TOUR_DATA + "_tourId				BIGINT NOT NULL,		\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	" + TABLE_TOUR_WAYPOINT + "_wayPointId		BIGINT NOT NULL			\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"; //																		//$NON-NLS-1$

		exec(stmt, sql);

		/*
		 * ALTER TABLE TourData_TourWayPoint ADD CONSTRAINT TourData_TourWayPoint_pk PRIMARY KEY
		 * (TourData_tourId);
		 */
		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURWAYPOINT) + "						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURWAYPOINT + "_pk				\n")//$NON-NLS-1$ //$NON-NLS-2$
				+ ("	PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"); //							//$NON-NLS-1$ //$NON-NLS-2$

		exec(stmt, sql);
	}

	/**
	 * create table {@link #TABLE_DB_VERSION}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableVersion(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE TABLE Version
		 */
		sql = "CREATE TABLE " + TABLE_DB_VERSION //									//$NON-NLS-1$
				+ " (															\n" //$NON-NLS-1$
				//
				+ "	version 	INTEGER	NOT NULL								\n" //$NON-NLS-1$
				//
				+ ")"; //															//$NON-NLS-1$

		exec(stmt, sql);
	}

	private String createUIServerStateMessage(final int stateCounter) {

		final StringBuilder sb = new StringBuilder();

		for (int stateIndex = 1; stateIndex <= MAX_TRIES_TO_PING_SERVER + 1; stateIndex++) {
			sb.append(stateIndex <= stateCounter ? ':' : '.');
		}

		return NLS.bind(Messages.Database_Monitor_db_service_task, sb.toString());
	}

	private void exec(final Statement stmt, final String sql) throws SQLException {

		StatusUtil.log(sql);
		System.out.println();

		stmt.execute(sql);
	}

	private void execUpdate(final Statement stmt, final String sql) throws SQLException {

		StatusUtil.log(sql);
		System.out.println();

		stmt.executeUpdate(sql);
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

		if (_emFactory == null) {
			getEntityManagerCreate();
		}

		if (_emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
			} catch (final Exception e) {
				StatusUtil.log(e);
			}
			return null;
		} else {
			return _emFactory.createEntityManager();
		}
	}

	private synchronized void getEntityManagerCreate() {

		try {
			final IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {
						checkServer();
					} catch (final Throwable e) {
						return;
					}

					checkTable(monitor);
					checkVersion(monitor);

					monitor.subTask(Messages.Database_Monitor_persistent_service_task);

					_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
				}
			};

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getSplashHandler();

			if (splashHandler == null) {
				try {
					checkServer();
				} catch (final Throwable e) {
					StatusUtil.showStatus(e);
					return;
				}
				checkTable(null);
				checkVersion(null);

				_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

			} else {
				runnableWithProgress.run(splashHandler.getBundleProgressMonitor());
			}

		} catch (final InvocationTargetException e) {
			StatusUtil.log(e);
		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}
	}

	private void logDbUpdateEnd(final int dbVersion) {
		System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, dbVersion));
		System.out.println();
	}

	private void logDbUpdateStart(final int dbVersion) {
		System.out.println();
		System.out.println(NLS.bind(Messages.Tour_Database_Update, dbVersion));
	}

	private void modifyColumnType(	final String table,
									final String fieldName,
									final String newFieldType,
									final Statement stmt,
									final IProgressMonitor monitor,
									final int no,
									final int max) throws SQLException {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update_ModifyColumn, new Object[] { no, max }));
		}

		//	"ALTER TABLE t ADD COLUMN c1_newtype NEWTYPE"
		//	"UPDATE t SET c1_newtype = c1"
		//	"ALTER TABLE t DROP COLUMN c1"
		//	"RENAME COLUMN t.c1_newtype TO c1"

		String sql;
		final String tempFieldName = fieldName + "_temp";//$NON-NLS-1$

		sql = "ALTER TABLE " + table + " ADD COLUMN " + tempFieldName + " " + newFieldType; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		sql = "UPDATE " + table + " SET " + tempFieldName + " = " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		execUpdate(stmt, sql);

		sql = "ALTER TABLE " + table + " DROP COLUMN " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$
		exec(stmt, sql);

		sql = "RENAME COLUMN " + table + "." + tempFieldName + " TO " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);
	}

	public void removePropertyListener(final IPropertyListener listener) {
		_propertyListeners.remove(listener);
	}

	/**
	 * this must be implemented or updated when the database version must be updated
	 */
	private boolean updateDbDesign(final Connection conn, int currentDbVersion, final IProgressMonitor monitor) {

		/*
		 * confirm update
		 */

		// define buttons with default to NO
		final String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

		if ((new MessageDialog(
				Display.getDefault().getActiveShell(),
				Messages.Database_Confirm_update_title,
				null,
				NLS.bind(Messages.Database_Confirm_update, new Object[] {
						currentDbVersion,
						TOURBOOK_DB_VERSION,
						_databasePath }),
				MessageDialog.QUESTION,
				buttons,
				1).open()) != Window.OK) {

			// no update -> close application
			PlatformUI.getWorkbench().close();

			return false;
		}

		/*
		 * do an additional check because version 20 is restructuring the data series
		 */
		if (currentDbVersion < 20) {

			if ((new MessageDialog(
					Display.getDefault().getActiveShell(),
					Messages.Database_Confirm_update_title,
					null,
					NLS.bind(Messages.Database_Confirm_Update20, _databasePath),
					MessageDialog.QUESTION,
					buttons,
					1).open()) != Window.OK) {

				// no update -> close application
				PlatformUI.getWorkbench().close();

				return false;
			}
		}

		boolean isPostUpdate5 = false;
		boolean isPostUpdate9 = false;
		boolean isPostUpdate11 = false;
		boolean isPostUpdate13 = false;
		boolean isPostUpdate20 = false;

		int newVersion = currentDbVersion;
		final int oldVersion = currentDbVersion;

		/*
		 * database update
		 */
		try {

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

			if (currentDbVersion == 8) {
				updateDbDesign_008_009(conn, monitor);
				currentDbVersion = newVersion = 9;
				isPostUpdate9 = true;
			}

			if (currentDbVersion == 9) {
				updateDbDesign_009_010(conn, monitor);
				currentDbVersion = newVersion = 10;
			}

			if (currentDbVersion == 10) {
				currentDbVersion = newVersion = updateDbDesign_010_011(conn, monitor);
				isPostUpdate11 = true;
			}

			if (currentDbVersion == 11) {
				currentDbVersion = newVersion = updateDbDesign_011_012(conn, monitor);
			}

			if (currentDbVersion == 12) {
				currentDbVersion = newVersion = updateDbDesign_012_013(conn, monitor);
				isPostUpdate13 = true;
			}

			if (currentDbVersion == 13) {
				currentDbVersion = newVersion = updateDbDesign_013_014(conn, monitor);
			}

			if (currentDbVersion == 14) {
				currentDbVersion = newVersion = updateDbDesign_014_015(conn, monitor);
			}

			if (currentDbVersion == 15) {
				currentDbVersion = newVersion = updateDbDesign_015_to_016(conn, monitor);
			}

			if (currentDbVersion == 16) {
				currentDbVersion = newVersion = updateDbDesign_016_to_017(conn, monitor);
			}

			if (currentDbVersion == 17) {
				currentDbVersion = newVersion = updateDbDesign_017_to_018(conn, monitor);
			}

			if (currentDbVersion == 18) {
				currentDbVersion = newVersion = updateDbDesign_018_to_019(conn, monitor);
			}

			if (currentDbVersion == 19) {
				currentDbVersion = newVersion = updateDbDesign_019_to_020(conn, monitor);
				isPostUpdate20 = true;
			}

			/*
			 * update version number
			 */
			updateDbVersionNumber(conn, newVersion);

			/**
			 * do the post update after the version number is updated because the post update uses
			 * connections which is checking the version number
			 */
			if (isPostUpdate5) {
				TourDatabase.computeComputedValuesForAllTours(monitor);
				TourManager.getInstance().removeAllToursFromCache();
			}
			if (isPostUpdate9) {
				updateDbDesign_008_009_PostUpdate(conn, monitor);
			}
			if (isPostUpdate11) {
				updateDbDesign_010_011_PostUpdate(conn, monitor);
			}
			if (isPostUpdate13) {
				updateDbDesign_012_013_PostUpdate(conn, monitor);
			}
			if (isPostUpdate20) {
				updateDbDesign_019_to_020_PostUpdate(conn, monitor);
			}

			/*
			 * 21
			 */
			if (currentDbVersion == 20) {

				currentDbVersion = newVersion = updateDbDesign_020_to_021(conn, monitor);

				updateDbVersionNumber(conn, newVersion);
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
			_isSQLUpdateError = true;
			return false;
		}

		// display info for the successful update
		MessageDialog.openInformation(
				Display.getCurrent().getActiveShell(),
				Messages.tour_database_version_info_title,
				NLS.bind(Messages.Tour_Database_UpdateInfo, oldVersion, newVersion));

		return true;
	}

	private void updateDbDesign_001_002(final Connection conn) throws SQLException {

		final int dbVersion = 2;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_002_003(final Connection conn) throws SQLException {

		final int dbVersion = 3;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_003_004(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 4;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxAltitude			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgCadence			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgTemperature		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxSpeed				FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourTitle			" + varCharNoKomma(TourData.DB_LENGTH_TOUR_TITLE)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourDescription		" + varCharNoKomma(TourData.DB_LENGTH_TOUR_DESCRIPTION)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourStartPlace		" + varCharNoKomma(TourData.DB_LENGTH_TOUR_START_PLACE)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourEndPlace			" + varCharNoKomma(TourData.DB_LENGTH_TOUR_END_PLACE)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN calories				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN bikerWeight			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourBike_bikeId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			// from wolfgang
			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN devicePluginName		" + varCharNoKomma(TourData.DB_LENGTH_DEVICE_PLUGIN_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			// from wolfgang
			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceModeName		" + varCharNoKomma(TourData.DB_LENGTH_DEVICE_MODE_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}

		stmt.close();

		// Create a EntityManagerFactory here, so we can access TourData with EJB
		if (monitor != null) {
			monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		}
		_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

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

			saveTour(tourData, false);
		}

		// cleanup everything as if nothing has happened
		_emFactory.close();
		_emFactory = null;

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_004_005(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 5;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 5));
		}

		final Statement stmt = conn.createStatement();
		{
			createTableTourTag(stmt);
			createTableTourTagCategory(stmt);
			createIndexTourData(stmt);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_005_006(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 6;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 6));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourImportFilePath" + varCharNoKomma(TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_006_007(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 7;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 7));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeSourceTourId		BIGINT"; //				//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeTargetTourId		BIGINT"; //				//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset	INTEGER DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset	INTEGER DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startSecond			SMALLINT DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_007_008(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 8;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 8));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherWindDir		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherWindSpd		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN isDistanceFromSensor  SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN weatherClouds			" + varCharNoKomma(TourData.DB_LENGTH_WEATHER_CLOUDS); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN restPulse    		    INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_008_009(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 9;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startWeekYear		SMALLINT DEFAULT 1977 "; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_008_009_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

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

	private void updateDbDesign_009_010(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 10;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
			createTableTourWayPoint(stmt);

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

			final String sql = //
			"ALTER TABLE " + TABLE_TOUR_DATA + "							\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "	ALTER COLUMN tourDescription						\n" //$NON-NLS-1$
					+ "	SET DATA TYPE " + varCharNoKomma(TourData.DB_LENGTH_TOUR_DESCRIPTION_V10); //$NON-NLS-1$

			exec(stmt, sql);
		}

		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private int updateDbDesign_010_011(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 11;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeCreated		BIGINT	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeModified		BIGINT	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);

		return dbVersion;
	}

	/**
	 * Set create date/time from the tour date
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_010_011_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final PreparedStatement stmtSelect = conn.prepareStatement(//
				//
				"SELECT" //									//$NON-NLS-1$
							//
						+ " StartYear," // 					// 1 //$NON-NLS-1$
						+ " StartMonth," // 				// 2 //$NON-NLS-1$
						+ " StartDay," // 					// 3 //$NON-NLS-1$
						+ " StartHour," // 					// 4 //$NON-NLS-1$
						+ " StartMinute," // 				// 5 //$NON-NLS-1$
						+ " StartSecond" // 				// 6 //$NON-NLS-1$
						//
						+ " FROM " + TABLE_TOUR_DATA //		//$NON-NLS-1$
						+ " WHERE TourId=?" //				$NON-NLS-1$ //$NON-NLS-1$
				);

		final PreparedStatement stmtUpdate = conn.prepareStatement(//
				//
				"UPDATE " + TABLE_TOUR_DATA //				//$NON-NLS-1$
						//
						+ " SET" //							//$NON-NLS-1$
						//
						+ " DateTimeCreated=?" //			// 1 //$NON-NLS-1$
						//
						+ " WHERE tourId=?"); //			// 2 //$NON-NLS-1$

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours
		for (final Long tourId : tourList) {

			if (monitor != null) {
				monitor.subTask(NLS.bind(//
						Messages.Tour_Database_PostUpdate011_SetTourCreateTime,
						new Object[] { tourIdx++, tourList.size() }));
			}

			// get tour date
			stmtSelect.setLong(1, tourId);
			final ResultSet result = stmtSelect.executeQuery();

			while (result.next()) {

				// get date from database
				final short dbYear = result.getShort(1);
				final short dbMonth = result.getShort(2);
				final short dbDay = result.getShort(3);
				final short dbHour = result.getShort(4);
				final short dbMinute = result.getShort(5);
				final short dbSecond = result.getShort(6);

				final long dtCreated = (dbYear * 10000000000L)
						+ (dbMonth * 100000000L)
						+ (dbDay * 1000000L)
						+ (dbHour * 10000L)
						+ (dbMinute * 100L)
						+ dbSecond;

				// update DateTimeCreated in the database
				stmtUpdate.setLong(1, dtCreated);
				stmtUpdate.setLong(2, tourId);
				stmtUpdate.executeUpdate();
			}
		}
	}

	private int updateDbDesign_011_012(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 12;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
//				+ "	IsPulseSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//				+ "	IsPowerSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//				+ "	DeviceAvgSpeed				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//				+ "	DeviceFirmwareVersion	" + varCharKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION)) //$NON-NLS-1$

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN IsPulseSensorPresent		INTEGER	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN IsPowerSensorPresent		INTEGER	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DeviceAvgSpeed			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DeviceFirmwareVersion		" + varCharNoKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_012_013(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 13;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//			+ "	TemperatureScale			INTEGER DEFAULT 1, 				\n" //$NON-NLS-1$
//			+ " Weather 					" + varCharNoKomma(TourData.DB_LENGTH_WEATHER) //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TemperatureScale		INTEGER	DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN Weather " + varCharNoKomma(TourData.DB_LENGTH_WEATHER); //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * Set temperature scale default value
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_012_013_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final String sql = "UPDATE " + TABLE_TOUR_DATA + " SET TemperatureScale=1"; //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();

		conn.createStatement().executeUpdate(sql);
	}

	private int updateDbDesign_013_014(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 14;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//		+ "	ConconiDeflection			INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN ConconiDeflection			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_014_015(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 15;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//		+ "	BirthDay			BIGINT DEFAULT 0,						\n" //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN BirthDay			BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_015_to_016(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 16;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{

			createTableTourPersonHRZone(stmt);

//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//		+ "	Gender				INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	RestPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	MaxPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	HrMaxFormula		INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//
//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN Gender				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN RestPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN MaxPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN HrMaxFormula		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_016_to_017(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 17;

		logDbUpdateStart(newDbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{

//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//
//			+ "	hrZone0						INTEGER DEFAULT -1,				\n" //$NON-NLS-1$
//			+ "	hrZone1						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone2						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone3						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone4						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone5						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone6						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone7						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone8						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone9						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 0));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone0		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 1));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone1		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 2));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone2		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 3));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone3		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 4));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone4		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 5));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone5		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 6));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone6		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 7));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone7		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 8));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone8		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 9));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone9		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_017_to_018(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 18;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{

			if (_dbVersionBeforeUpdate > 16) {

				/*
				 * db update 16 creates the HR zone db, doing this update causes an sql exception
				 * because the fields are already available
				 */

//				TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE
//
//				// version 18 start
//				//
//				+ "	ColorRed			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				+ "	ColorGreen			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				+ "	ColorBlue			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				//
//				// version 18 end ---------
//
//				TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorRed		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorGreen	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorBlue	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);
			}

//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//
//			// version 18 start
//			//
//			+ "	NumberOfHrZones				INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//			//
//			// version 18 end ---------
//
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN NumberOfHrZones	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_018_to_019(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 19;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
//			TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE
//			//
//			// version 19 start
//			//
//			+ "	colorTextRed 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
//			+ "	colorTextGreen 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
//			+ "	colorTextBlue 		SMALLINT DEFAULT 0						\n" //$NON-NLS-1$
//			//
//			// version 19 end ---------
//			//
//			TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextRed		SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextGreen	SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextBlue		SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_019_to_020(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 20;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		{
			updateDbDesign_019_to_020_10DataSerieBlobSize(conn);
			updateDbDesign_019_to_020_20AlterColumns(conn);
		}

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * Increase {@link TourData#serieData} blob size.
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	private void updateDbDesign_019_to_020_10DataSerieBlobSize(final Connection conn) throws SQLException {

		final DatabaseMetaData meta = conn.getMetaData();

		final ResultSet rsColumns = meta.getColumns(null, TABLE_SCHEMA, TABLE_TOUR_DATA, "SERIEDATA"); //$NON-NLS-1$

		while (rsColumns.next()) {

			final int size = rsColumns.getInt("COLUMN_SIZE"); //$NON-NLS-1$
			if (size == 1048576) {

				/*
				 * database is from a derby version before 10.5 which creates BLOB's with a default
				 * size of 1M, increase size to 2G because a tour with 53000 can not be saves and
				 * causes an exception
				 */

				String sql;
				final Statement stmt = conn.createStatement();
				{
					// ALTER TABLE TourData ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)

					sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)"; //$NON-NLS-1$ //$NON-NLS-2$
					exec(stmt, sql);
				}
				stmt.close();
			}

			break;
		}
	}

	private void updateDbDesign_019_to_020_20AlterColumns(final Connection conn) throws SQLException {

		String sql;
		final Statement stmt = conn.createStatement();
		{
			// remove the NOT NULL constraint from the "distance" column
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ALTER COLUMN distance	NULL"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			//			+ "	distance20 					FLOAT DEFAULT 0,					\n" //$NON-NLS-1$
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN distance20	FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();
	}

	/**
	 * Set create date/time from the tour date
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_019_to_020_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		IS_POST_UPDATE_019_to_020 = true;

		final long startTime = System.currentTimeMillis();

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		try {

			// loop: all tours
			for (final Long tourId : tourList) {

				final long currentTime = System.currentTimeMillis();

				if (monitor != null) {

					final float durationInSeconds = (currentTime - startTime) / 1000;

					monitor.subTask(NLS.bind(//
							Messages.Tour_Database_PostUpdate020_ConvertIntToFloat,
							new Object[] { tourIdx, tourList.size(), (int) durationInSeconds }));

					tourIdx++;
				}

				final TourData tourData = em.find(TourData.class, tourId);
				if (tourData != null) {

					tourData.updateDatabase_019_to_020();

					TourDatabase.saveEntity(tourData, tourId, TourData.class);
				}

			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			IS_POST_UPDATE_019_to_020 = false;

			em.close();
		}
	}

	private int updateDbDesign_020_to_021(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 21;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//			// version 21 start
//			//
//			+ "	maxAltitude					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	maxPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgCadence					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgTemperature				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			//
//			// version 21 end ---------
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			int no = 0;
			final int max = 5;

			modifyColumnType(TABLE_TOUR_DATA, "maxAltitude", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //			//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumnType(TABLE_TOUR_DATA, "maxPulse", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //				//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumnType(TABLE_TOUR_DATA, "avgPulse", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //				//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumnType(TABLE_TOUR_DATA, "avgCadence", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //			//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumnType(TABLE_TOUR_DATA, "avgTemperature", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //		//$NON-NLS-1$ //$NON-NLS-2$
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private void updateDbVersionNumber(final Connection conn, final int newVersion) throws SQLException {

		final String sql = "UPDATE " + TABLE_DB_VERSION + " SET VERSION=" + newVersion; //$NON-NLS-1$ //$NON-NLS-2$

		conn.createStatement().executeUpdate(sql);
	}

	/**
	 * @param varCharLength
	 * @return Returns: " VARCHAR(" + varCharLength + "),\n"
	 */
	private String varCharKomma(final int varCharLength) {
		return " VARCHAR(" + varCharLength + "),\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param varCharLength
	 * @return Returns: " VARCHAR(" + varCharLength + ")\n"
	 */
	private String varCharNoKomma(final int varCharLength) {
		return " VARCHAR(" + varCharLength + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
