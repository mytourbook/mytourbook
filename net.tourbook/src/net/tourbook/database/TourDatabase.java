/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
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
	private static final int			TOURBOOK_DB_VERSION				= 4;

	public final static String			TABLE_TOUR_DATA					= "TourData";								//$NON-NLS-1$
	public final static String			TABLE_TOUR_MARKER				= "TourMarker";							//$NON-NLS-1$
	public final static String			TABLE_TOUR_REFERENCE			= "TourReference";							//$NON-NLS-1$
	public final static String			TABLE_TOUR_COMPARED				= "TourCompared";							//$NON-NLS-1$
	public final static String			TABLE_TOUR_CATEGORY				= "TourCategory";							//$NON-NLS-1$
	public final static String			TABLE_TOUR_TYPE					= "TourType";								//$NON-NLS-1$
	public static final String			TABLE_TOUR_PERSON				= "TourPerson";							//$NON-NLS-1$
	public static final String			TABLE_TOUR_BIKE					= "TourBike";								//$NON-NLS-1$

	private static final String			TABLE_DB_VERSION				= "DbVersion";								//$NON-NLS-1$

	/**
	 * Db property: tour was changed and saved in the database
	 */
	public static final int				TOUR_IS_CHANGED_AND_PERSISTED	= 1;

	/**
	 * Db property: tour was changed but not yet saved in the database
	 */
	public static final int				TOUR_IS_CHANGED					= 2;

	private static TourDatabase			instance;

	private static NetworkServerControl	server;

	private static EntityManagerFactory	emFactory;

	private static ArrayList<TourType>	fTourTypes;

	private boolean						fIsTableChecked;
	private boolean						fIsVersionChecked;

	private final ListenerList			fPropertyListeners				= new ListenerList(ListenerList.IDENTITY);

	class CustomMonitor extends ProgressMonitorDialog {

		private final String	fTitle;

		public CustomMonitor(Shell parent, String title) {
			super(parent);
			fTitle = title;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(fTitle);
			return super.createDialogArea(parent);
		}
	}

	private TourDatabase() {}

	/**
	 * dispose tour types and their images so the next time they have to be loaded from the database
	 * and the images are recreated
	 */
	public static void disposeTourTypes() {

		if (fTourTypes != null) {
			fTourTypes.clear();
			fTourTypes = null;
		}

		UI.getInstance().disposeTourTypeImages();
	}

	/**
	 * @return Returns all tours in database
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<Long> getAllTourIds() {

		ArrayList<Long> tourList = new ArrayList<Long>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourData.tourId " //$NON-NLS-1$
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
	@SuppressWarnings("unchecked")
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourBike " //$NON-NLS-1$
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

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		TourData tourData = em.find(TourData.class, tourId);

		em.close();

//		long startTime = System.currentTimeMillis();
//		long endTime = System.currentTimeMillis();
//		System.out.println("Execution time : " + (endTime - startTime) + " ms");

		return tourData;
	}

	/**
	 * @return Returns all tour people in the db sorted by last/first name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourPerson> getTourPeople() {

		ArrayList<TourPerson> tourPeople = new ArrayList<TourPerson>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourPerson " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_PERSON + " TourPerson ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$

			tourPeople = (ArrayList<TourPerson>) query.getResultList();

			em.close();
		}

		return tourPeople;
	}

	/**
	 * @return Returns all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourType> getTourTypes() {

		if (fTourTypes != null) {
			return fTourTypes;
		}

		fTourTypes = new ArrayList<TourType>();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourType " //$NON-NLS-1$
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
	public static boolean removeTour(long tourId) {

		boolean isRemoved = false;

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

				tourData.onPrePersist();

				ts.begin();
				{
					TourData tourDataEntity = em.find(TourData.class, tourData.getTourId());

					if (tourDataEntity == null) {

						// tour is not yet persisted

						em.persist(tourData);

					} else {

						em.merge(tourData);
					}
				}
				ts.commit();

//			} catch (PersistenceException e) {
//
//				try {
//					em.refresh(tourData);
//				} catch (Exception e2) {
//					e.printStackTrace();
//				}
//
//				if (em.contains(tourData)) {
//
//					em.merge(tourData);
//
//				} else {
//
//				}

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

		if (isSaved) {
			TourManager.getInstance().removeTourFromCache(tourData.getTourId());
		}

		return isSaved;
	}

	public void addPropertyListener(IPropertyListener listener) {
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
					IProgressMonitor splashProgressMonitor = splashHandler.getBundleProgressMonitor();

					runnableStartServer.run(splashProgressMonitor);
				}

			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
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

				/*
				 * ALTER TABLE TourBike
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_BIKE) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_BIKE + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (bikeId)")); //$NON-NLS-1$

				/*
				 * CREATE TABLE TourPerson
				 */
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
				/*
				 * ALTER TABLE TourPerson
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_PERSON) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_PERSON + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (personId)")); //$NON-NLS-1$

				/*
				 * CREATE TABLE TourType
				 */
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
				/*
				 * ALTER TABLE TourType
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_TYPE) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_TYPE + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (typeId)")); //$NON-NLS-1$

				/*
				 * CREATE TABLE TOURCATEGORY
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
						+ "(" //$NON-NLS-1$
						+ "categoryId 					BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
						+ (TABLE_TOUR_DATA + "tourId	BIGINT,") //$NON-NLS-1$
						+ "category 					VARCHAR(100)" //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

				// Create Table: TourCategory_TourData_Data
				stmt.addBatch("" //$NON-NLS-1$
						+ ("CREATE TABLE " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA) //$NON-NLS-1$ //$NON-NLS-2$
						+ "(" //$NON-NLS-1$
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
						+ (TABLE_TOUR_CATEGORY + "_categoryId	BIGINT NOT NULL") //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

				// Create Table: TourMarker
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

				// Create Table: TourData_TourMarker
				stmt.addBatch("" //$NON-NLS-1$
						+ ("CREATE TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER) //$NON-NLS-1$ //$NON-NLS-2$
						+ "(" //$NON-NLS-1$
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
						+ (TABLE_TOUR_MARKER + "_markerId		BIGINT NOT NULL") //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

				// Create Table: TourReference
				stmt.addBatch("" //$NON-NLS-1$
						+ ("CREATE TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
						+ "(" //$NON-NLS-1$
						+ "refId 						BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)," //$NON-NLS-1$
						+ (TABLE_TOUR_DATA + "_tourId	BIGINT,") //$NON-NLS-1$
						+ "startIndex					INTEGER NOT NULL," //$NON-NLS-1$
						+ "endIndex 					INTEGER NOT NULL," //$NON-NLS-1$
						+ "label 						VARCHAR(80)" //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

				// Create Table: TourData_TourReference
				stmt.addBatch("" //$NON-NLS-1$
						+ ("CREATE TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE) //$NON-NLS-1$ //$NON-NLS-2$
						+ "(" //$NON-NLS-1$
						+ (TABLE_TOUR_DATA + "_tourId			BIGINT NOT NULL,") //$NON-NLS-1$
						+ (TABLE_TOUR_REFERENCE + "_refId 		BIGINT NOT NULL") //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

				// Create Table: TourCompared
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

				// Create Table: TourData
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
						 * disabled because when two blob object's are deserialized then the error
						 * occures:
						 * <p>
						 * java.io.StreamCorruptedException: invalid stream header: 00ACED00
						 * <p>
						 * therefor the gpsData are put into the serieData object
						 */
//						+ "gpsData 				BLOB,				\n" //$NON-NLS-1$
						// version 5 end
						+ "tourType_typeId 		BIGINT,				\n" //$NON-NLS-1$
						+ "tourPerson_personId 	BIGINT,				\n" //$NON-NLS-1$

						+ "serieData 			BLOB NOT NULL		\n" //$NON-NLS-1$

						+ ")"); //$NON-NLS-1$

				/*
				 * Alter Table
				 */

				// ALTER TABLE TourData ADD CONSTRAINT TourData_pk PRIMARY KEY
				// (tourId);
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_DATA) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + TABLE_TOUR_DATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (tourId)")); //$NON-NLS-1$

				// ALTER TABLE TourReference ADD CONSTRAINT TourReference_pk
				// PRIMARY KEY (refId);
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_REFERENCE) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + TABLE_TOUR_REFERENCE + "_pk ") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (refId)")); //$NON-NLS-1$

				// ALTER TABLE TourMarker ADD CONSTRAINT TourMarker_pk PRIMARY
				// KEY (markerId);
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_MARKER) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_MARKER + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (markerId)")); //$NON-NLS-1$

				/*
				 * ALTER TABLE TourCategory ADD CONSTRAINT TourCategory_pk PRIMARY KEY (categoryId);
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY) //$NON-NLS-1$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_CATEGORY + "_pk ")) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" PRIMARY KEY (categoryId)")); //$NON-NLS-1$

				/*
				 * ALTER TABLE TourData_TourMarker ADD CONSTRAINT TourData_TourMarker_pk PRIMARY KEY
				 * (TourData_tourId);
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER)) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" ADD CONSTRAINT " + (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER + "_pk")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)")); //$NON-NLS-1$ //$NON-NLS-2$

				/*
				 * ALTER TABLE TourData_TourReference ADD CONSTRAINT TourData_TourReference_pk
				 * PRIMARY KEY (TourData_tourId);
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_DATA + "_" + TABLE_TOUR_REFERENCE) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" ADD CONSTRAINT " //$NON-NLS-1$
								+ TABLE_TOUR_DATA
								+ "_" //$NON-NLS-1$
								+ TABLE_TOUR_REFERENCE + "_pk") //$NON-NLS-1$
						+ (" PRIMARY KEY (" + TABLE_TOUR_DATA + "_tourId)")); //$NON-NLS-1$ //$NON-NLS-2$

				/*
				 * ALTER TABLE TourCategory_TourData ADD CONSTRAINT TourCategory_TourData_pk PRIMARY
				 * KEY (tourCategory_categoryId);
				 */
				stmt.addBatch("" //$NON-NLS-1$
						+ ("ALTER TABLE " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA) //$NON-NLS-1$ //$NON-NLS-2$
						+ (" ADD CONSTRAINT " + TABLE_TOUR_CATEGORY + "_" + TABLE_TOUR_DATA + "_pk") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ (" PRIMARY KEY (" + TABLE_TOUR_CATEGORY + "_categoryId)")); //$NON-NLS-1$ //$NON-NLS-2$

				/*
				 * CREATE TABLE Version
				 */
				stmt.addBatch(("CREATE TABLE " + TABLE_DB_VERSION) //$NON-NLS-1$
						+ " (" //$NON-NLS-1$
						+ "version 		INTEGER	NOT NULL" //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$

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

	private boolean checkVersion(IProgressMonitor monitor) {

		if (fIsVersionChecked) {
			return true;
		}

		try {

			Connection conn = createConnection();

			String sqlString = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$
			PreparedStatement prepStatement = conn.prepareStatement(sqlString);
			ResultSet result = prepStatement.executeQuery();

			if (result.next()) {

				// version record was found

				int currentDbVersion = result.getInt(1);

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

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private Connection createConnection() throws SQLException {

		final Connection conn = DriverManager.getConnection("jdbc:derby://localhost:1527/tourbook;create=true", //$NON-NLS-1$
				"user", //$NON-NLS-1$
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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return startServerRunnable;
		}

		// start derby server
		startServerRunnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) {
				runStartServer(monitor);
			}

		};

		return startServerRunnable;
	}

	public void firePropertyChange(int propertyId) {
		Object[] allListeners = fPropertyListeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			final IPropertyListener listener = (IPropertyListener) allListeners[i];
			listener.propertyChanged(TourDatabase.this, propertyId);
		}
	}

	public Connection getConnection() throws SQLException {

		try {
			checkServer();
		} catch (MyTourbookException e) {
			e.printStackTrace();
		}

		checkTable();

		if (checkVersion(null) == false) {
			return null;
		}

		Connection conn = createConnection();

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
			IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {
						checkServer();
					} catch (MyTourbookException e) {
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
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} else {
			EntityManager em = emFactory.createEntityManager();

			return em;
		}
	}

	public void removePropertyListener(IPropertyListener listener) {
		fPropertyListeners.remove(listener);
	}

	private void runStartServer(IProgressMonitor monitor) {

		monitor.subTask(Messages.Database_Monitor_db_service_task);

		String databasePath = getDatabasePath();

		// set storage location for the database
		System.setProperty("derby.system.home", databasePath); //$NON-NLS-1$

		try {
			server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		try {
			/*
			 * check if another derby server is already running (this can happen during development)
			 */
			server.ping();

		} catch (Exception e) {

			try {
				server.start(null);
				// monitor.worked(1);
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			// wait until the server is started
			while (true) {

				try {
					server.ping();
					break;
				} catch (Exception e1) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}

			// make the first connection, this takes longer as the subsequent ones
			try {
				Connection connection = createConnection();
				connection.close();

				System.out.println("Database path: " + databasePath); //$NON-NLS-1$

			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	private boolean updateDbDesign(Connection conn, int currentDbVersion, IProgressMonitor monitor) {

		/*
		 * this must be implemented or updated when the database version must be updated
		 */

		// confirm update
		String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

		String message = NLS.bind(Messages.Database_Confirm_update, new Object[] {
				currentDbVersion,
				TOURBOOK_DB_VERSION,
				getDatabasePath() });

		MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
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
//		if (currentDbVersion == 4) {
//			updateDbDesign_4_5(conn);
//			currentDbVersion = newVersion = 5;
//		}

		// update the version number
		try {
			String sqlString = "" //$NON-NLS-1$
					+ ("update " + TABLE_DB_VERSION) //$NON-NLS-1$
					+ (" set VERSION=" + newVersion) //$NON-NLS-1$
					+ (" where 1=1"); //$NON-NLS-1$
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

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
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

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			statement.addBatch(sql);

			statement.executeBatch();
			statement.close();

		} catch (SQLException e) {
			printSQLException(e);
		}
	}

	private void updateDbDesign_3_4(Connection conn, IProgressMonitor monitor) {

		try {
			Statement statement = conn.createStatement();

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

		} catch (SQLException e) {
			printSQLException(e);
		}

		// Create a EntityManagerFactory here, so we can access TourData with EJB
		monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

		monitor.subTask(Messages.Tour_Database_load_all_tours);
		ArrayList<Long> tourList = getAllTourIds();

		// loop over all tours and calculate and set new columns
		int tourIdx = 1;
		for (Long tourId : tourList) {

			TourData tourData = getTourData(tourId);

			if (monitor != null) {
				String msg = NLS.bind(Messages.Tour_Database_update_tour, new Object[] { tourIdx++, tourList.size() });
				monitor.subTask(msg);
			}

			tourData.computeAvgFields();

			TourPerson person = tourData.getTourPerson();
			tourData.setTourBike(person.getTourBike());
			tourData.setBikerWeight(person.getWeight());

			saveTour(tourData);
		}

		// cleanup everything as if nothing has happened
		emFactory.close();
		emFactory = null;
	}

	public static String getTourTypeName(long typeId) {

		String tourTypeName = UI.EMPTY_STRING;

		for (TourType tourType : getTourTypes()) {
			if (tourType.getTypeId() == typeId) {
				tourTypeName = tourType.getName();
				break;
			}
		}

		return tourTypeName;
	}

//	private void updateDbDesign_4_5(Connection conn) {
//
//		try {
//			Statement statement = conn.createStatement();
//
//			String sql;
//
//			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN gpsData BLOB DEFAULT NULL"; //$NON-NLS-1$ //$NON-NLS-2$
//			statement.addBatch(sql);
//
//			statement.executeBatch();
//			statement.close();
//
//		} catch (SQLException e) {
//			printSQLException(e);
//		}
//	}
}
