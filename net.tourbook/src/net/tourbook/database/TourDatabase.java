/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

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
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application works successfully
	 */
	private static final int			TOURBOOK_DB_VERSION							= 5;

	/*
	 * database tables
	 */
	private static final String			TABLE_DB_VERSION							= "DbVersion";													//$NON-NLS-1$

	public static final String			TABLE_SCHEMA								= "USER";														//$NON-NLS-1$

	public static final String			TABLE_TOUR_BIKE								= "TourBike";													//$NON-NLS-1$
	public static final String			TABLE_TOUR_CATEGORY							= "TourCategory";												//$NON-NLS-1$
	public static final String			TABLE_TOUR_COMPARED							= "TourCompared";												//$NON-NLS-1$
	public static final String			TABLE_TOUR_DATA								= "TourData";													//$NON-NLS-1$
	public static final String			TABLE_TOUR_MARKER							= "TourMarker";												//$NON-NLS-1$
	public static final String			TABLE_TOUR_PERSON							= "TourPerson";												//$NON-NLS-1$
	public static final String			TABLE_TOUR_REFERENCE						= "TourReference";												//$NON-NLS-1$
	public static final String			TABLE_TOUR_TAG								= "TourTag";													//$NON-NLS-1$
	public static final String			TABLE_TOUR_TAG_CATEGORY						= "TourTagCategory";											//$NON-NLS-1$
	public static final String			TABLE_TOUR_TYPE								= "TourType";													//$NON-NLS-1$

	public static final String			JOINTABLE_TOURDATA__TOURTAG					= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_TAG);					//$NON-NLS-1$
	public static final String			JOINTABLE_TOURDATA__TOURMARKER				= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER);					//$NON-NLS-1$
	public static final String			JOINTABLE_TOURDATA__TOURREFERENCE			= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE);				//$NON-NLS-1$
	public static final String			JOINTABLE_TOURTAGCATEGORY_TOURTAG			= (TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG);			//$NON-NLS-1$
	public static final String			JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY	= (TABLE_TOUR_TAG_CATEGORY + "_" + TABLE_TOUR_TAG_CATEGORY);	//$NON-NLS-1$
	public static final String			JOINTABLE_TOURCATEGORY__TOURDATA			= (TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA);				//$NON-NLS-1$

	/**
	 * contains <code>-1</code> which is the Id for a not saved entity
	 */
	public static final int				ENTITY_IS_NOT_SAVED							= -1;

	/**
	 * Db property: tour was changed and saved in the database
	 */
	public static final int				TOUR_IS_CHANGED_AND_PERSISTED				= 1;

	/**
	 * Db property: tour was changed but not yet saved in the database
	 */
	public static final int				TOUR_IS_CHANGED								= 2;

	private static TourDatabase			instance;

	private static NetworkServerControl	server;

	private static EntityManagerFactory	emFactory;

	private static ArrayList<TourType>	fActiveTourTypes;
	private static ArrayList<TourType>	fTourTypes;
	private static ArrayList<TourTag>	fTourTags;

	private boolean						fIsTableChecked;
	private boolean						fIsVersionChecked;

	private final ListenerList			fPropertyListeners							= new ListenerList(ListenerList.IDENTITY);

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

	/**
	 * remove all tour tags that the next time they have to be loaded from the database
	 */
	public static void cleanTourTags() {

		if (fTourTags != null) {
			fTourTags.clear();
			fTourTags = null;
		}
	}

	/**
	 * remove all tour types and dispose their images so the next time they have to be loaded from
	 * the database and the images are recreated
	 */
	public static void cleanTourTypes() {

		if (fTourTypes != null) {
			fTourTypes.clear();
			fTourTypes = null;
		}

		UI.getInstance().disposeTourTypeImages();
	}

	/**
	 * @param tourTypeList
	 * @return Returns a list with all {@link TourType}'s.<br>
	 *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
	 *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
	 */
	public static ArrayList<TourType> getActiveTourTypes() {
		return fActiveTourTypes;
	}

	/**
	 * @return Returns all tours in database
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private static ArrayList<Long> getAllTourIds() {

		ArrayList<Long> tourList = new ArrayList<Long>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourData.tourId " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData ")); //$NON-NLS-1$ //$NON-NLS-2$

			tourList = (ArrayList<Long>) query.getResultList();

			em.close();
		}

		return tourList;
	}

	public static TourDatabase getInstance() {
		if (instance == null) {
			instance = new TourDatabase();
		}
		return instance;
	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourBike " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_BIKE + " TourBike ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourBike.name")); //$NON-NLS-1$

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
	public static TourData getTourData(final Long tourId) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		final TourData tourData = em.find(TourData.class, tourId);

		em.close();

//		long startTime = System.currentTimeMillis();
//		long endTime = System.currentTimeMillis();
//		System.out.println("Execution time : " + (endTime - startTime) + " ms");

		return tourData;
	}

	/**
	 * @return Returns all tour people in the db sorted by last/first name
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public static ArrayList<TourPerson> getTourPeople() {

		ArrayList<TourPerson> tourPeople = new ArrayList<TourPerson>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourPerson " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_PERSON + " TourPerson ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$

			tourPeople = (ArrayList<TourPerson>) query.getResultList();

			em.close();
		}

		return tourPeople;
	}

	/**
	 * @return Returns all tags which are stored in the database ordered by name
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public static ArrayList<TourTag> getTourTags() {

		if (fTourTags != null) {
			return fTourTags;
		}

		fTourTags = new ArrayList<TourTag>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourTag " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TAG + " TourTag ")
					+ (" ORDER BY TourTag.name")
			//
			); //$NON-NLS-1$

			fTourTags = (ArrayList<TourTag>) query.getResultList();

			em.close();
		}

		return fTourTags;
	}

	/**
	 * @param typeId
	 * @return Returns the name for the {@link TourType} or an empty string when the tour type id
	 *         was not found
	 */
	public static String getTourTypeName(final long typeId) {

		String tourTypeName = Messages.ui_tour_not_defined;

		for (final TourType tourType : getTourTypes()) {
			if (tourType.getTypeId() == typeId) {
				tourTypeName = tourType.getName();
				break;
			}
		}

		return tourTypeName;
	}

	/**
	 * @return Returns all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public static ArrayList<TourType> getTourTypes() {

		if (fTourTypes != null) {
			return fTourTypes;
		}

		fTourTypes = new ArrayList<TourType>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourType " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TYPE + " TourType ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourType.name")); //$NON-NLS-1$

			fTourTypes = (ArrayList<TourType>) query.getResultList();

			em.close();
		}

		return fTourTypes;
	}

	public static void printSQLException(SQLException sqle) {
		while (sqle != null) {
			System.out.println("\n---SQLException Caught---\n"); //$NON-NLS-1$
			System.out.println("SQLState: " + (sqle).getSQLState()); //$NON-NLS-1$
			System.out.println("Severity: " + (sqle).getErrorCode()); //$NON-NLS-1$
			System.out.println("Message: " + (sqle).getMessage()); //$NON-NLS-1$
			sqle.printStackTrace();
			sqle = sqle.getNextException();
		}
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
			final TourData tour = em.find(TourData.class, tourId);

			if (tour != null) {
				ts.begin();
				em.remove(tour);
				ts.commit();
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isRemoved = true;
			}
			em.close();
		}

		if (isRemoved) {
			TourManager.getInstance().removeTourFromCache(tourId);
		}

		return isRemoved;
	}

	/**
	 * Persists an entity
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return Returns <code>true</code> when the entity was saved
	 */
	public static boolean saveEntity(Object entity, final long id, final Class<?> entityClass) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		boolean isSaved = false;
		final EntityTransaction ts = em.getTransaction();

		try {

			ts.begin();
			{
				final Object entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);

				} else {

					entity = em.merge(entity);
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

		return isSaved;
	}

	/**
	 * Persists an entity
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return Returns the saved entity
	 */
	public static <T> T saveEntity(T entity, final long id, final Class<T> entityClass, final EntityManager em) {

		boolean isSaved = false;
		final EntityTransaction ts = em.getTransaction();

		try {

			ts.begin();
			{
				final T entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);

				} else {

					entity = em.merge(entity);
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

		return entity;
	}

	/**
	 * Persist the tourData in the database
	 * 
	 * @param tourData
	 * @return returns true is the save was successful
	 */
	public static boolean saveTour(final TourData tourData) {

		boolean isSaved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

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
				e.printStackTrace();
			} finally {
				if (ts.isActive()) {
					ts.rollback();
				} else {
					isSaved = true;
				}
				em.close();
			}
		}

		if (persistedEntity != null) {
			TourManager.getInstance().updateTourInCache(persistedEntity);
		}

		return isSaved;
	}

	public static void updateActiveTourTypeList(final TourTypeFilter tourTypeFilter) {

		switch (tourTypeFilter.getFilterType()) {
		case TourTypeFilter.FILTER_TYPE_SYSTEM:

			if (tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {

				// all tour types are selected

				fActiveTourTypes = fTourTypes;
				return;

			} else {

				// tour type is not defined

			}

			break;

		case TourTypeFilter.FILTER_TYPE_DB:

			fActiveTourTypes = new ArrayList<TourType>();
			fActiveTourTypes.add(tourTypeFilter.getTourType());

			return;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

			final Object[] tourTypes = tourTypeFilter.getTourTypeSet().getTourTypes();

			if (tourTypes.length != 0) {

				// create a list with all tour types from the set

				fActiveTourTypes = new ArrayList<TourType>();

				for (final Object item : tourTypes) {
					fActiveTourTypes.add((TourType) item);
				}
				return;
			}

			break;

		default:
			break;
		}

		// set default empty list
		fActiveTourTypes = new ArrayList<TourType>();
	}

	private TourDatabase() {}

	public void addPropertyListener(final IPropertyListener listener) {
		fPropertyListeners.add(listener);
	}

	/**
	 * Check if the server is available
	 * 
	 * @throws MyTourbookException
	 */
	private void checkServer() throws MyTourbookException {

		// when the server is started, nothing is to do here
		if (server != null) {
			return;
		}

		final IRunnableWithProgress runnableStartServer = createStartServerRunnable();

		if (runnableStartServer != null) {

			try {

				final MyTourbookSplashHandler splashHandler = TourbookPlugin.getDefault().getSplashHandler();

				if (splashHandler == null) {
					throw new MyTourbookException("Cannot get Splash Handler"); //$NON-NLS-1$
				} else {
					final IProgressMonitor splashProgressMonitor = splashHandler.getBundleProgressMonitor();

					runnableStartServer.run(splashProgressMonitor);
				}

			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check if the table in the database exist
	 */
	private void checkTable() {

		if (fIsTableChecked) {
			return;
		}

		try {

			final Connection conn = createConnection();

			/*
			 * Check if the tourdata table exists
			 */
			final DatabaseMetaData metaData = conn.getMetaData();
			final ResultSet tables = metaData.getTables(null, null, null, null);
			while (tables.next()) {
				if (tables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {
					conn.close();
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

					createTableTourTag(stmt);
					createTableTourTagCategory(stmt);

					stmt.executeBatch();
				}
				stmt.close();

				fIsTableChecked = true;

			} catch (final SQLException e) {
				e.getNextException().printStackTrace();
			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (final SQLException e) {
						e.printStackTrace();
					}
				}
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean checkVersion(final IProgressMonitor monitor) {

		if (fIsVersionChecked) {
			return true;
		}

		try {

			final Connection conn = createConnection();

			String sqlString = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$
			final PreparedStatement prepStatement = conn.prepareStatement(sqlString);
			final ResultSet result = prepStatement.executeQuery();

			if (result.next()) {

				// version record was found

				final int currentDbVersion = result.getInt(1);

				// check if the database contains the correct version
				if (currentDbVersion != TOURBOOK_DB_VERSION) {
					if (updateDbDesign(conn, currentDbVersion, monitor) == false) {
						return false;
					}
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

			fIsVersionChecked = true;
			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private Connection createConnection() throws SQLException {

		final Connection conn = DriverManager.getConnection("jdbc:derby://localhost:1527/tourbook;create=true", //$NON-NLS-1$
				TABLE_SCHEMA,
				"adsf"); //$NON-NLS-1$
		return conn;
	}

	/**
	 * Checks if the database server is running, if not it will start the server. startServerJob has
	 * a job when the server is not yet started
	 */
	private IRunnableWithProgress createStartServerRunnable() {

		IRunnableWithProgress startServerRunnable = null;

		// load derby driver
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver"); //$NON-NLS-1$
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			return startServerRunnable;
		}

		// start derby server
		startServerRunnable = new IRunnableWithProgress() {

			public void run(final IProgressMonitor monitor) {
				runStartServer(monitor);
			}

		};

		return startServerRunnable;
	}

	/**
	 * create table {@link #TABLE_TOUR_BIKE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourBike(final Statement stmt) throws SQLException {

		// CREATE TABLE TourBike
		stmt.addBatch("" //$NON-NLS-1$
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
		stmt.addBatch("" //$NON-NLS-1$
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
		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "categoryId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "tourId	BIGINT,") //$NON-NLS-1$
				+ "category 					VARCHAR(100)" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourCategory ADD CONSTRAINT TourCategory_pk PRIMARY KEY (categoryId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_CATEGORY + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (categoryId)")); //$NON-NLS-1$

		// CREATE TABLE TourCategory_TourData
		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_CATEGORY + "_categoryId	BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourCategory_TourData ADD CONSTRAINT TourCategory_TourData_pk PRIMARY KEY (tourCategory_categoryId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURCATEGORY__TOURDATA) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURCATEGORY__TOURDATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		stmt.addBatch("" //$NON-NLS-1$
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
	 *create table {@link #TABLE_TOUR_DATA}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourData(final Statement stmt) throws SQLException {

		// CREATE TABLE TourData
		stmt.addBatch("" //$NON-NLS-1$
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
				+ "tourDescription		VARCHAR(4096),		\n" //$NON-NLS-1$
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
//						+ "gpsData 				BLOB,				\n" //$NON-NLS-1$
				// version 5 end
				//
				+ "tourType_typeId 		BIGINT,				\n" //$NON-NLS-1$
				+ "tourPerson_personId 	BIGINT,				\n" //$NON-NLS-1$

				+ "serieData 			BLOB NOT NULL		\n" //$NON-NLS-1$

				+ ")"); //$NON-NLS-1$

		/*
		 * Alter Table
		 */

		// ALTER TABLE TourData ADD CONSTRAINT TourData_pk PRIMARY KEY (tourId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_DATA) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_DATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (tourId)")); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_MARKER}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourMarker(final Statement stmt) throws SQLException {

		// CREATE TABLE TourMarker
		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_MARKER) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "markerId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId	BIGINT,") //$NON-NLS-1$
				+ "time 						INTEGER NOT NULL," //$NON-NLS-1$
				+ "distance 					INTEGER NOT NULL," //$NON-NLS-1$
				+ "serieIndex 					INTEGER NOT NULL," //$NON-NLS-1$
				+ "type 						INTEGER NOT NULL," //$NON-NLS-1$
				+ "visualPosition				INTEGER NOT NULL," //$NON-NLS-1$
				+ "label 						VARCHAR(255)," //$NON-NLS-1$
				+ "category 					VARCHAR(100)," //$NON-NLS-1$
				// Version 2
				+ "labelXOffset					INTEGER," //$NON-NLS-1$
				+ "labelYOffset					INTEGER," //$NON-NLS-1$
				+ "markerType					BIGINT" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourMarker ADD CONSTRAINT TourMarker_pk PRIMARY KEY (markerId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_MARKER) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_MARKER + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (markerId)")); //$NON-NLS-1$

		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURMARKER) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_MARKER + "_markerId		BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourData_TourMarker ADD CONSTRAINT TourData_TourMarker_pk PRIMARY KEY (TourData_tourId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURMARKER) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURMARKER + "_pk") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		stmt.addBatch("" //$NON-NLS-1$
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
		stmt.addBatch("" //$NON-NLS-1$
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
		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
				+ "(" //$NON-NLS-1$
				+ "	refId 						BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId	BIGINT,\n") //$NON-NLS-1$
				+ "	startIndex					INTEGER NOT NULL,\n" //$NON-NLS-1$
				+ "	endIndex 					INTEGER NOT NULL,\n" //$NON-NLS-1$
				+ "	label 						VARCHAR(80)\n" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourReference ADD CONSTRAINT TourReference_pk PRIMARY KEY (refId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_REFERENCE + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (refId)")); //$NON-NLS-1$

		// CREATE TABLE TourData_TourReference

		stmt.addBatch("" //$NON-NLS-1$
				+ ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(" //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
				+ (TABLE_TOUR_REFERENCE + "_refId 		BIGINT NOT NULL") //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$

		// ALTER TABLE TourData_TourReference ADD CONSTRAINT TourData_TourReference_pk PRIMARY KEY (TourData_tourId);
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURREFERENCE) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURREFERENCE + "_pk") //$NON-NLS-1$
				+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * create table {@link #TABLE_TOUR_TAG}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourTag(final Statement stmt) throws SQLException {

		/*
		 * creates the tables for the tour tags for VERSION 5
		 */

		// CREATE TABLE TourTag
		String sql;

		sql = ("CREATE TABLE " + TABLE_TOUR_TAG) //$NON-NLS-1$
				+ "(\n" //$NON-NLS-1$
				+ "	tagId 	BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),\n" //$NON-NLS-1$
				+ "	isRoot 	INTEGER,\n" //$NON-NLS-1$
				+ "	name 	VARCHAR(255)\n" //$NON-NLS-1$
				+ ")";

		System.out.println(sql);

		stmt.addBatch(sql);

		// ALTER TABLE TourTag ADD CONSTRAINT TourTag_pk PRIMARY KEY (refId);
		sql = ("ALTER TABLE " + TABLE_TOUR_TAG) //$NON-NLS-1$
				+ ("	ADD CONSTRAINT " + TABLE_TOUR_TAG + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	PRIMARY KEY (tagId)");

		System.out.println(sql);
		System.out.println();

		stmt.addBatch(sql); //$NON-NLS-1$

		/*
		 * CREATE TABLE TourData_TourTag
		 */
		final String field_TourData_tourId = TABLE_TOUR_DATA + "_tourId";
		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId";

		sql = ("CREATE TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG + "_tagId" + "	BIGINT NOT NULL,\n") //$NON-NLS-1$
				+ (TABLE_TOUR_DATA + "_tourId" + "	BIGINT NOT NULL\n") //$NON-NLS-1$
				+ ")";

		System.out.println(sql);
		stmt.addBatch(sql); //$NON-NLS-1$

		/*
		 * Add Constrainsts
		 */

//		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //						//$NON-NLS-1$ //$NON-NLS-2$
//				+ (" ADD CONSTRAINT " + JOINTABLE_TOURDATA__TOURTAG + "_pk") //		//$NON-NLS-1$
//				+ (" PRIMARY KEY (" + field_TourTag_tagId + ")"); //				//$NON-NLS-1$ //$NON-NLS-2$
//
//		System.out.println(sql);
//		stmt.addBatch(sql);
		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ("	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourTag_tagId)
				+ ("	FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId)")
				+ ("	REFERENCES " + TABLE_TOUR_TAG + " (tagId)");

		System.out.println(sql);
		stmt.addBatch(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURDATA__TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ("	ADD CONSTRAINT fk_" + JOINTABLE_TOURDATA__TOURTAG + "_" + field_TourData_tourId)
				+ ("	FOREIGN KEY (" + TABLE_TOUR_DATA + "_tourId)")
				+ ("	REFERENCES " + TABLE_TOUR_DATA + " (tourId)");

		System.out.println(sql);
		System.out.println();
		stmt.addBatch(sql);
	}

	private void createTableTourTagCategory(final Statement stmt) throws SQLException {

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
				+ ")";

		System.out.println(sql);
		stmt.addBatch(sql);

		sql = ("ALTER TABLE " + TABLE_TOUR_TAG_CATEGORY) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + TABLE_TOUR_TAG_CATEGORY + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (tagCategoryId)");

		System.out.println(sql);
		System.out.println();
		stmt.addBatch(sql); //$NON-NLS-1$

		/*
		 * TABLE TourTagCategory_TourTag
		 */

		sql = ("CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG + "_tagId						BIGINT NOT NULL,\n") //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId	BIGINT NOT NULL\n") //$NON-NLS-1$
				+ ")";

		System.out.println(sql);
		stmt.addBatch(sql); //$NON-NLS-1$

		/*
		 * Add constraints
		 */

		final String field_TourTag_tagId = TABLE_TOUR_TAG + "_tagId";
		final String field_TourTagCategory_tagCategoryId = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId";

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTag_tagId)
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG + "_tagId)")
				+ (" REFERENCES " + TABLE_TOUR_TAG + " (tagId)");

		System.out.println(sql);
		stmt.addBatch(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAG) //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_" + JOINTABLE_TOURTAGCATEGORY_TOURTAG + "_" + field_TourTagCategory_tagCategoryId)
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId)")
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)");

		System.out.println(sql);
		System.out.println();
		stmt.addBatch(sql);

		/*
		 * TABLE TourTagCategory_TourTagCategory
		 */

		sql = ("CREATE TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY) //$NON-NLS-1$ //$NON-NLS-2$
				+ "(\n" //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1	BIGINT NOT NULL,\n") //$NON-NLS-1$
				+ (TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2	BIGINT NOT NULL\n") //$NON-NLS-1$
				+ ")";

		System.out.println(sql);
		stmt.addBatch(sql); //$NON-NLS-1$

		/*
		 * Add constraints
		 */

		final String field_TourTagCategory_tagCategoryId1 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1";
		final String field_TourTagCategory_tagCategoryId2 = TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2";

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_"
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_"
						+ field_TourTagCategory_tagCategoryId1 + "\n")
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId1)\n")
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)\n");

		System.out.println(sql);
		stmt.addBatch(sql);

		sql = ("ALTER TABLE " + JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ (" ADD CONSTRAINT fk_"
						+ JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY
						+ "_"
						+ field_TourTagCategory_tagCategoryId2 + "\n")
				+ (" FOREIGN KEY (" + TABLE_TOUR_TAG_CATEGORY + "_tagCategoryId2)\n")
				+ (" REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (tagCategoryId)\n");

		System.out.println(sql);
		System.out.println();
		stmt.addBatch(sql);
	}

	/**
	 * create table {@link #TABLE_TOUR_TYPE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableTourType(final Statement stmt) throws SQLException {

		// CREATE TABLE TourType
		stmt.addBatch("" //$NON-NLS-1$
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
		stmt.addBatch("" //$NON-NLS-1$
				+ ("ALTER TABLE " + TABLE_TOUR_TYPE) //$NON-NLS-1$
				+ (" ADD CONSTRAINT " + (TABLE_TOUR_TYPE + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" PRIMARY KEY (typeId)")); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_DB_VERSION}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTableVersion(final Statement stmt) throws SQLException {

		// CREATE TABLE Version
		stmt.addBatch(("CREATE TABLE " + TABLE_DB_VERSION) //$NON-NLS-1$
				+ " (" //$NON-NLS-1$
				+ "version 		INTEGER	NOT NULL" //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$
	}

	public void firePropertyChange(final int propertyId) {
		final Object[] allListeners = fPropertyListeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			final IPropertyListener listener = (IPropertyListener) allListeners[i];
			listener.propertyChanged(TourDatabase.this, propertyId);
		}
	}

	public Connection getConnection() throws SQLException {

		try {
			checkServer();
		} catch (final MyTourbookException e) {
			e.printStackTrace();
		}

		checkTable();

		if (checkVersion(null) == false) {
			return null;
		}

		final Connection conn = createConnection();

		return conn;
	}

	private String getDatabasePath() {
		return Platform.getInstanceLocation().getURL().getPath() + "derby-database"; //$NON-NLS-1$
	}

	/**
	 * Creates an entity manager which is used to persist entities
	 * 
	 * @return
	 */
	public EntityManager getEntityManager() {

		if (emFactory != null) {
			return emFactory.createEntityManager();
		}

		try {
			final IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
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

					emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

					monitor.setTaskName(""); //$NON-NLS-1$
				}
			};

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getDefault().getSplashHandler();

			if (splashHandler != null) {
				runnableWithProgress.run(splashHandler.getBundleProgressMonitor());
			}

//		} catch (MyTourbookException e) {
//			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		if (emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return null;
		} else {
			final EntityManager em = emFactory.createEntityManager();

			return em;
		}
	}

	public void removePropertyListener(final IPropertyListener listener) {
		fPropertyListeners.remove(listener);
	}

	private void runStartServer(final IProgressMonitor monitor) {

		monitor.subTask(Messages.Database_Monitor_db_service_task);

		final String databasePath = getDatabasePath();

		// set storage location for the database
		System.setProperty("derby.system.home", databasePath); //$NON-NLS-1$

		try {
			server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
		} catch (final UnknownHostException e2) {
			e2.printStackTrace();
		} catch (final Exception e2) {
			e2.printStackTrace();
		}

		try {
			/*
			 * check if another derby server is already running (this can happen during development)
			 */
			server.ping();

		} catch (final Exception e) {

			try {
				server.start(null);
				// monitor.worked(1);
			} catch (final Exception e2) {
				e2.printStackTrace();
			}

			// wait until the server is started
			while (true) {

				try {
					server.ping();
					break;
				} catch (final Exception e1) {
					try {
						Thread.sleep(1);
					} catch (final InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}

			// make the first connection, this takes longer as the subsequent ones
			try {
				final Connection connection = createConnection();
				connection.close();

				System.out.println("Database path: " + databasePath); //$NON-NLS-1$

			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		}
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
				getDatabasePath() });

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
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

		if (currentDbVersion == 1) {
			updateDbDesign_1_2(conn);
			currentDbVersion = newVersion = 2;
		}
		if (currentDbVersion == 2) {
			updateDbDesign_2_3(conn);
			currentDbVersion = newVersion = 3;
		}
		if (currentDbVersion == 3) {
			updateDbDesign_3_4(conn, monitor);
			currentDbVersion = newVersion = 4;
		}
		if (currentDbVersion == 4) {
			updateDbDesign_4_5(conn);
			currentDbVersion = newVersion = 5;
		}

		// update the version number
		try {
			final String sqlString = "" //$NON-NLS-1$
					+ ("update " + TABLE_DB_VERSION) //$NON-NLS-1$
					+ (" set VERSION=" + newVersion) //$NON-NLS-1$
					+ (" where 1=1"); //$NON-NLS-1$
			conn.createStatement().executeUpdate(sqlString);
		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void updateDbDesign_1_2(final Connection conn) {

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			printSQLException(e);
		}
	}

	private void updateDbDesign_2_3(final Connection conn) {

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			printSQLException(e);
		}
	}

	private void updateDbDesign_3_4(final Connection conn, final IProgressMonitor monitor) {

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxAltitude			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgPulse				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgCadence			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN avgTemperature		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN maxSpeed				FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourTitle				VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourDescription		VARCHAR(4096)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourStartPlace		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourEndPlace			VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN calories				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN bikerWeight			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN tourBike_bikeId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			// from wolfgang
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN devicePluginName		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			// from wolfgang
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceModeName		VARCHAR(255)"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			printSQLException(e);
		}

		// Create a EntityManagerFactory here, so we can access TourData with EJB
		monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

		monitor.subTask(Messages.Tour_Database_load_all_tours);
		final ArrayList<Long> tourList = getAllTourIds();

		// loop over all tours and calculate and set new columns
		int tourIdx = 1;
		for (final Long tourId : tourList) {

			final TourData tourData = getTourData(tourId);

			if (monitor != null) {
				final String msg = NLS.bind(Messages.Tour_Database_update_tour, new Object[] {
						tourIdx++,
						tourList.size() });
				monitor.subTask(msg);
			}

			tourData.computeAvgFields();

			final TourPerson person = tourData.getTourPerson();
			tourData.setTourBike(person.getTourBike());
			tourData.setBikerWeight(person.getWeight());

			saveTour(tourData);
		}

		// cleanup everything as if nothing has happened
		emFactory.close();
		emFactory = null;
	}

	private void updateDbDesign_4_5(final Connection conn) {

		try {
			final Statement statement = conn.createStatement();

			createTableTourTag(statement);
			createTableTourTagCategory(statement);

			statement.executeBatch();
			statement.close();

		} catch (final SQLException e) {
			printSQLException(e);
		}
	}

}
