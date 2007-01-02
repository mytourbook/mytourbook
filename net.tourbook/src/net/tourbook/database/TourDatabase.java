/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;

import org.apache.derby.drda.NetworkServerControl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application
	 * works successfully
	 */
	private static final int			TOURBOOK_DB_VERSION		= 3;

	public final static String			TABLE_TOUR_DATA			= "TourData";
	public final static String			TABLE_TOUR_MARKER		= "TourMarker";
	public final static String			TABLE_TOUR_REFERENCE	= "TourReference";
	public final static String			TABLE_TOUR_COMPARED		= "TourCompared";
	public final static String			TABLE_TOUR_CATEGORY		= "TourCategory";
	public final static String			TABLE_TOUR_TYPE			= "TourType";
	public static final String			TABLE_TOUR_PERSON		= "TourPerson";
	public static final String			TABLE_TOUR_BIKE			= "TourBike";

	private static final String			TABLE_DB_VERSION			= "DbVersion";

	private static TourDatabase			instance;

	private static NetworkServerControl	server;

	private static EntityManagerFactory	emFactory;

	private boolean						fIsTableChecked;
	private boolean						fIsVersionChecked;

	private TourDatabase() {}

	public static TourDatabase getInstance() {
		if (instance == null) {
			instance = new TourDatabase();
		}
		return instance;
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

		checkServer();
		checkTable();

		if (checkVersion() == false) {
			return null;
		}

		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(
							"Starting Persistent Service (Hibernate)",
							IProgressMonitor.UNKNOWN);
					emFactory = Persistence.createEntityManagerFactory("tourdatabase");
				}
			};

			CustomMonitor progressMonitorDialog = new CustomMonitor(Display
					.getCurrent()
					.getActiveShell(), "Persistent Service");
			progressMonitorDialog.run(true, false, runnable);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} else {
			EntityManager em = emFactory.createEntityManager();
			
//			System.out.println(em.toString());
			
			return em;
		}
	}

	class CustomMonitor extends ProgressMonitorDialog {

		private String	fTitle;

		public CustomMonitor(Shell parent, String title) {
			super(parent);
			fTitle = title;
		}

		protected Control createDialogArea(Composite parent) {
			getShell().setText(fTitle);
			return super.createDialogArea(parent);
		}
	}

	private void checkServer() {

		// when the server is started, nothing is to do here
		if (server != null) {
			return;
		}

		final IRunnableWithProgress startServerRunnable = createStartServerRunnable();

		if (startServerRunnable != null) {

			try {
				CustomMonitor databaseMonitor = new CustomMonitor(Display
						.getCurrent()
						.getActiveShell(), "Database Service");
				databaseMonitor.run(true, false, startServerRunnable);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks if the database server is running, if not it will start the
	 * server. startServerJob has a job when the server is not yet started
	 */
	private IRunnableWithProgress createStartServerRunnable() {

		IRunnableWithProgress startServerRunnable = null;

		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return startServerRunnable;
		}

		/*
		 * start derby server
		 */

		startServerRunnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) {

				monitor.beginTask("Starting database server (Derby)", 5);

				// set storage location for the database
				System.setProperty("derby.system.home", getDatabasePath());

				try {
					server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527);
				} catch (UnknownHostException e2) {
					e2.printStackTrace();
				} catch (Exception e2) {
					e2.printStackTrace();
				}

				monitor.worked(1);

				try {
					/*
					 * check if another derby server is already running (this
					 * can happen during development)
					 */
					server.ping();
					monitor.worked(1);

				} catch (Exception e) {

					try {
						server.start(null);
						monitor.worked(1);
					} catch (Exception e2) {
						e2.printStackTrace();
					}

					// wait until the server is started
					while (true) {

						try {
							server.ping();
							monitor.worked(1);
							break;
						} catch (Exception e1) {
							try {
								Thread.sleep(1);
								monitor.worked(1);
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}

					// make the first connection, this takes longer as the
					// subsequent ones
					try {
						monitor.worked(1);
						Connection connection = createConnection();
						connection.close();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
				monitor.done();
			}

		};

		return startServerRunnable;
	}

	/**
	 * Create the tables in the tourdatabase if it does not exis
	 */
	private void checkTable() {

		if (fIsTableChecked) {
			return;
		}

		try {

			Connection conn = createConnection();

			/*
			 * Check if the tourdata table exists
			 */
			DatabaseMetaData metaData = conn.getMetaData();
			ResultSet tables = metaData.getTables(null, null, null, null);
			while (tables.next()) {
				if (tables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {
					conn.close();
					return;
				}
			}

			Statement stmt = null;

			try {
				stmt = conn.createStatement();

				/*
				 * CREATE TABLE TourBike
				 */
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_BIKE)
								+ "("
								+ "bikeId	 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ "name				VARCHAR(255),	\n"
								+ "weight 			FLOAT,			\n" // kg
								+ "typeId 			INTEGER,		\n"
								+ "frontTyreId 		INTEGER,		\n"
								+ "rearTyreId 		INTEGER			\n"
								+ ")");

				/*
				 * ALTER TABLE TourBike
				 */
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_BIKE)
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_BIKE + "_pk "))
						+ (" PRIMARY KEY (bikeId)"));

				/*
				 * CREATE TABLE TourPerson
				 */
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_PERSON)
								+ "("
								+ "personId 		BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ "lastName 		VARCHAR(80),	\n"
								+ "firstName 		VARCHAR(80),	\n"
								+ "weight 			FLOAT,			\n" // kg
								+ "height 			FLOAT,			\n" // m
								+ "rawDataPath		VARCHAR(255),	\n"
								+ "deviceReaderId	VARCHAR(255),	\n"
								+ "tourBike_bikeId 	BIGINT			\n"
								+ ")");
				/*
				 * ALTER TABLE TourPerson
				 */
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_PERSON)
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_PERSON + "_pk "))
						+ (" PRIMARY KEY (personId)"));

				/*
				 * CREATE TABLE TourType
				 */
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_TYPE)
								+ "("
								+ "typeId 				BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ "name 				VARCHAR(100),"
								+ "colorBrightRed 		SMALLINT NOT NULL,"
								+ "colorBrightGreen 	SMALLINT NOT NULL,"
								+ "colorBrightBlue 		SMALLINT NOT NULL,"
								+ "colorDarkRed 		SMALLINT NOT NULL,"
								+ "colorDarkGreen 		SMALLINT NOT NULL,"
								+ "colorDarkBlue 		SMALLINT NOT NULL,"
								+ "colorLineRed 		SMALLINT NOT NULL,"
								+ "colorLineGreen 		SMALLINT NOT NULL,"
								+ "colorLineBlue 		SMALLINT NOT NULL"
								+ ")");
				/*
				 * ALTER TABLE TourType
				 */
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_TYPE)
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_TYPE + "_pk "))
						+ (" PRIMARY KEY (typeId)"));

				/*
				 * CREATE TABLE TOURCATEGORY
				 */
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY)
								+ "("
								+ "categoryId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ (TABLE_TOUR_DATA + "tourId	BIGINT,")
								+ "category 					VARCHAR(100)"
								+ ")");

				// Create Table: TourCategory_TourData_Data
				stmt.addBatch(""
						+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA)
						+ "("
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,")
						+ (TABLE_TOUR_CATEGORY + "_categoryId	BIGINT NOT NULL")
						+ ")");

				// Create Table: TourMarker
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_MARKER)
								+ "("
								+ "markerId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ (TABLE_TOUR_DATA + "_tourId	BIGINT,")
								+ "time 						INTEGER NOT NULL,"
								+ "distance 					INTEGER NOT NULL,"
								+ "serieIndex 					INTEGER NOT NULL,"
								+ "type 						INTEGER NOT NULL,"
								+ "visualPosition				INTEGER NOT NULL,"
								+ "label 						VARCHAR(255),"
								+ "category 					VARCHAR(100),"
								// Version 2
								+ "labelXOffset					INTEGER,"
								+ "labelYOffset					INTEGER,"
								+ "markerType					BIGINT"
								+ ")");

				// Create Table: TourData_TourMarker
				stmt.addBatch(""
						+ ("CREATE TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER)
						+ "("
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,")
						+ (TABLE_TOUR_MARKER + "_markerId		BIGINT NOT NULL")
						+ ")");

				// Create Table: TourReference
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_REFERENCE)
								+ "("
								+ "refId 						BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ (TABLE_TOUR_DATA + "_tourId	BIGINT,")
								+ "startIndex					INTEGER NOT NULL,"
								+ "endIndex 					INTEGER NOT NULL,"
								+ "label 						VARCHAR(80)"
								+ ")");

				// Create Table: TourData_TourReference
				stmt.addBatch(""
						+ ("CREATE TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE)
						+ "("
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,")
						+ (TABLE_TOUR_REFERENCE + "_refId 		BIGINT NOT NULL")
						+ ")");

				// Create Table: TourCompared
				stmt
						.addBatch(""
								+ ("CREATE TABLE " + TABLE_TOUR_COMPARED)
								+ "("
								+ "comparedId 			BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1),"
								+ "refTourId			BIGINT,"
								+ "tourId				BIGINT,"
								+ "startIndex			INTEGER NOT NULL,"
								+ "endIndex 			INTEGER NOT NULL,"
								+ "tourDate	 			DATE NOT NULL,"
								+ "startYear			INTEGER NOT NULL,"
								+ "tourSpeed	 		FLOAT"
								+ ")");

				// Create Table: TourData
				stmt.addBatch(""
						+ ("CREATE TABLE " + TABLE_TOUR_DATA)
						+ "("
						+ "tourId 				BIGINT NOT NULL,	\n"
						+ "startYear 			SMALLINT NOT NULL,\n"
						+ "startMonth 			SMALLINT NOT NULL,	\n"
						+ "startDay 			SMALLINT NOT NULL,	\n"
						+ "startHour 			SMALLINT NOT NULL,	\n"
						+ "startMinute 			SMALLINT NOT NULL,	\n"
						+ "startWeek 			SMALLINT NOT NULL,	\n"
						+ "startDistance 		INTEGER NOT NULL,	\n"
						+ "distance 			INTEGER NOT NULL,	\n"
						+ "startAltitude 		SMALLINT NOT NULL,	\n"
						+ "startPulse 			SMALLINT NOT NULL,	\n"
						+ "dpTolerance 			SMALLINT NOT NULL,	\n"
						+ "tourDistance 		INTEGER NOT NULL,	\n"
						+ "tourRecordingTime 	INTEGER NOT NULL,	\n"
						+ "tourDrivingTime 		INTEGER NOT NULL,	\n"
						+ "tourAltUp 			INTEGER NOT NULL,	\n"
						+ "tourAltDown 			INTEGER NOT NULL,	\n"
						+ "deviceTourType 		VARCHAR(2),			\n"
						+ "deviceTravelTime 	BIGINT NOT NULL,	\n"
						+ "deviceDistance 		INTEGER NOT NULL,	\n"
						+ "deviceWheel 			INTEGER NOT NULL,	\n"
						+ "deviceWeight 		INTEGER NOT NULL,	\n"
						+ "deviceTotalUp 		INTEGER NOT NULL,	\n"
						+ "deviceTotalDown 		INTEGER NOT NULL,	\n"
						+ "devicePluginId	 	VARCHAR(255),		\n"
						
						// version 3 start
						+ "deviceMode 			SMALLINT,			\n"
						+ "deviceTimeInterval	SMALLINT,			\n"
						// version 3 end

						+ "tourType_typeId 		BIGINT,				\n"
						+ "tourPerson_personId 	BIGINT,				\n"
						+ "serieData 			BLOB NOT NULL		\n"
						+ ")");

				/*
				 * Alter Table
				 */

				// ALTER TABLE TourData ADD CONSTRAINT TourData_pk PRIMARY KEY
				// (tourId);
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_DATA)
						+ (" ADD CONSTRAINT " + TABLE_TOUR_DATA + "_pk")
						+ (" PRIMARY KEY (tourId)"));

				// ALTER TABLE TourReference ADD CONSTRAINT TourReference_pk
				// PRIMARY KEY (refId);
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_REFERENCE)
						+ (" ADD CONSTRAINT " + TABLE_TOUR_REFERENCE + "_pk ")
						+ (" PRIMARY KEY (refId)"));

				// ALTER TABLE TourMarker ADD CONSTRAINT TourMarker_pk PRIMARY
				// KEY (markerId);
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_MARKER)
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_MARKER + "_pk "))
						+ (" PRIMARY KEY (markerId)"));

				/*
				 * ALTER TABLE TourCategory ADD CONSTRAINT TourCategory_pk
				 * PRIMARY KEY (categoryId);
				 */
				stmt.addBatch(""
						+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY)
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_CATEGORY + "_pk "))
						+ (" PRIMARY KEY (categoryId)"));

				/*
				 * ALTER TABLE TourData_TourMarker ADD CONSTRAINT
				 * TourData_TourMarker_pk PRIMARY KEY (TourData_tourId);
				 */
				stmt
						.addBatch(""
								+ ("ALTER TABLE " + (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER))
								+ (" ADD CONSTRAINT " + (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER + "_pk"))
								+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"));

				/*
				 * ALTER TABLE TourData_TourReference ADD CONSTRAINT
				 * TourData_TourReference_pk PRIMARY KEY (TourData_tourId);
				 */
				stmt
						.addBatch(""
								+ ("ALTER TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE)
								+ (" ADD CONSTRAINT "
										+ TABLE_TOUR_DATA
										+ "_"
										+ TABLE_TOUR_REFERENCE + "_pk")
								+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)"));

				/*
				 * ALTER TABLE TourCategory_TourData ADD CONSTRAINT
				 * TourCategory_TourData_pk PRIMARY KEY
				 * (tourCategory_categoryId);
				 */
				stmt
						.addBatch(""
								+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA)
								+ (" ADD CONSTRAINT " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA + "_pk")
								+ (" PRIMARY KEY (" + TABLE_TOUR_CATEGORY + "_categoryId)"));

				/*
				 * CREATE TABLE Version
				 */
				stmt.addBatch(("CREATE TABLE " + TABLE_DB_VERSION)
						+ " ("
						+ "version 		INTEGER	NOT NULL"
						+ ")");

				stmt.executeBatch();
				stmt.close();

				fIsTableChecked = true;

			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() throws SQLException {

		checkServer();
		checkTable();

		if (checkVersion() == false) {
			return null;
		}

		Connection conn = createConnection();

		return conn;
	}

	private boolean checkVersion() {

		if (fIsVersionChecked) {
			return true;
		}

		try {

			Connection conn = createConnection();

			String sqlString = "SELECT * FROM " + TABLE_DB_VERSION;
			PreparedStatement prepStatement = conn.prepareStatement(sqlString);
			ResultSet result = prepStatement.executeQuery();

			if (result.next()) {

				// version record was found

				int currentDbVersion = result.getInt(1);

				// check if the database contains the correct version
				if (currentDbVersion != TOURBOOK_DB_VERSION) {
					if (updateDbDesign(conn, currentDbVersion) == false) {
						return false;
					}
				}

			} else {

				// a version record is not available

				/*
				 * insert the version for the current database design into the database
				 */
				sqlString = ("INSERT INTO " + TABLE_DB_VERSION)
						+ " VALUES ("
						+ Integer.toString(TOURBOOK_DB_VERSION)
						+ ")";

				conn.createStatement().executeUpdate(sqlString);
			}

			fIsVersionChecked = true;
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean updateDbDesign(Connection conn, int currentDbVersion) {

		/*
		 * this must be implemented or updated when the database version must be
		 * updated
		 */

		// confirm update
		String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

		String message = "The tour database needs to be updated from Version "
				+ (currentDbVersion + " to Version " + TOURBOOK_DB_VERSION + "\n\n")
				+ "It is STRONGLY recommended to make a backup "
				+ "for the current tour database before the update is applied.\n\n"
				+ "BACKUP:\n"
				+ "- this application must beclosed before a backup is done\n"
				+ ("- the database is located in the folder:\n  " + getDatabasePath())
				+ "\n\n"
				+ "Click\n"
				+ "  Yes:\tTo update the database\n"
				+ "  No:\tTo close the application"
				+ "";

		MessageDialog dialog = new MessageDialog(
				Display.getCurrent().getActiveShell(),
				"Update Tour Database",
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
			newVersion = 2;
		}

		if (currentDbVersion == 2) {
			updateDbDesign_2_3(conn);
			newVersion = TOURBOOK_DB_VERSION;
		}

		// update the version number
		try {
			String sqlString = ""
					+ ("update " + TABLE_DB_VERSION)
					+ (" set VERSION=" + newVersion)
					+ (" where 1=1");
			conn.createStatement().executeUpdate(sqlString);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void updateDbDesign_1_2(Connection conn) {

		try {
			Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0";
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0";
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0";
			statement.addBatch(sql);

			statement.executeBatch();
			statement.close();

		} catch (SQLException e) {
			printSQLException(e);
		}
	}

	private void updateDbDesign_2_3(Connection conn) {
		
		try {
			Statement statement = conn.createStatement();
			
			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1";
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1";
			statement.addBatch(sql);
			
			statement.executeBatch();
			statement.close();
			
		} catch (SQLException e) {
			printSQLException(e);
		}
	}

	private Connection createConnection() throws SQLException {

		final Connection conn = DriverManager.getConnection(
				"jdbc:derby://localhost:1527/tourbook;create=true",
				"User",
				"adsf");
		return conn;
	}

	private String getDatabasePath() {

		String pluginPath = Platform
				.getStateLocation(TourbookPlugin.getDefault().getBundle())
				.removeLastSegments(4)
				.toFile()
				.getAbsolutePath();

		return pluginPath + "/derby-database";
	}

	public static void printSQLException(SQLException sqle) {
		while (sqle != null) {
			System.out.println("\n---SQLException Caught---\n");
			System.out.println("SQLState: " + (sqle).getSQLState());
			System.out.println("Severity: " + (sqle).getErrorCode());
			System.out.println("Message: " + (sqle).getMessage());
			sqle.printStackTrace();
			sqle = sqle.getNextException();
		}
	}

	/**
	 * Return a tour from the database by it's id
	 * 
	 * @param tourId
	 * @return Returns the tour object or null if the tour was not found
	 */
	public static TourData getTourDataByTourId(final Long tourId) {

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		TourData tourData = em.find(TourData.class, tourId);

		// if (tourData != null) {
		// tourData.onPostLoad();
		// }
		em.close();

		return tourData;
	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	public static ArrayList<TourType> getTourTypes() {

		ArrayList<TourType> tourTypeList = new ArrayList<TourType>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourType "
					+ ("FROM " + TourDatabase.TABLE_TOUR_TYPE + " TourType ")
					+ (" ORDER  BY TourType.name"));

			tourTypeList = (ArrayList<TourType>) query.getResultList();

			em.close();
		}

		return tourTypeList;
	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourBike "
					+ ("FROM " + TourDatabase.TABLE_TOUR_BIKE + " TourBike ")
					+ (" ORDER  BY TourBike.name"));

			bikeList = (ArrayList<TourBike>) query.getResultList();

			em.close();
		}

		return bikeList;
	}

	/**
	 * @return Returns all tour people in the db sorted by last/first name
	 */
	public static ArrayList<TourPerson> getTourPeople() {

		ArrayList<TourPerson> tourPeople = new ArrayList<TourPerson>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourPerson "
					+ ("FROM " + TourDatabase.TABLE_TOUR_PERSON + " TourPerson ")
					+ (" ORDER  BY TourPerson.lastName, TourPerson.firstName"));

			tourPeople = (ArrayList<TourPerson>) query.getResultList();

			em.close();
		}

		return tourPeople;
	}

	/**
	 * Persist the tourData in the database
	 * 
	 * @param tourData
	 * @return returns true is the save was successful
	 */
	public static boolean saveTour(TourData tourData) {

		boolean isSaved = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			EntityTransaction ts = em.getTransaction();

			try {

				TourData tourDataEntity = em.find(TourData.class, tourData.getTourId());

				if (tourDataEntity == null) {
					// tour is not yet persisted
					ts.begin();

					tourData.onPrePersist();
					em.persist(tourData);

					ts.commit();

				} else {

					ts.begin();

					tourData.onPrePersist();
					em.merge(tourData);

					ts.commit();
				}

			} catch (Exception e) {
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
		return isSaved;
	}

	/**
	 * Remove a tour from the database
	 * 
	 * @param tourId
	 */
	public static boolean removeTour(long tourId) {

		boolean returnResult = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();
		EntityTransaction ts = em.getTransaction();

		try {
			TourData tour = em.find(TourData.class, tourId);

			if (tour != null) {
				ts.begin();
				em.remove(tour);
				ts.commit();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				returnResult = true;
			}
			em.close();
		}

		return returnResult;
	}
}
