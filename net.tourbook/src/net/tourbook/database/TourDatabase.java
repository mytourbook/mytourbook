/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.Collections;
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

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application works successfully
	 */
	private static final int					TOURBOOK_DB_VERSION							= 7;													// 9.01

//	private static final int					TOURBOOK_DB_VERSION							= 6;	// 8.12
//	private static final int					TOURBOOK_DB_VERSION							= 5;	// 8.11

	private static final String					DERBY_CLIENT_DRIVER							= "org.apache.derby.jdbc.ClientDriver";				//$NON-NLS-1$

	private static final String					DERBY_URL									= "jdbc:derby://localhost:1527/tourbook;create=true";	//$NON-NLS-1$
	/*
	 * database tables
	 */
	private static final String					TABLE_DB_VERSION							= "DbVersion";											//$NON-NLS-1$

	public static final String					TABLE_SCHEMA								= "USER";												//$NON-NLS-1$

	public static final String					TABLE_TOUR_BIKE								= "TourBike";											//$NON-NLS-1$

	public static final String					TABLE_TOUR_CATEGORY							= "TourCategory";										//$NON-NLS-1$
	public static final String					TABLE_TOUR_COMPARED							= "TourCompared";										//$NON-NLS-1$
	public static final String					TABLE_TOUR_DATA								= "TourData";											//$NON-NLS-1$
	public static final String					TABLE_TOUR_MARKER							= "TourMarker";										//$NON-NLS-1$
	public static final String					TABLE_TOUR_PERSON							= "TourPerson";										//$NON-NLS-1$
	public static final String					TABLE_TOUR_REFERENCE						= "TourReference";										//$NON-NLS-1$
	public static final String					TABLE_TOUR_TAG								= "TourTag";											//$NON-NLS-1$
	public static final String					TABLE_TOUR_TAG_CATEGORY						= "TourTagCategory";									//$NON-NLS-1$
	public static final String					TABLE_TOUR_TYPE								= "TourType";											//$NON-NLS-1$
	public static final String					JOINTABLE_TOURDATA__TOURTAG					= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_TAG);			//$NON-NLS-1$

	public static final String					JOINTABLE_TOURDATA__TOURMARKER				= (TABLE_TOUR_DATA + "_" + TABLE_TOUR_MARKER);			//$NON-NLS-1$
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

	private static TourDatabase					instance;

	private static NetworkServerControl			server;

	private static EntityManagerFactory			emFactory;

	private ComboPooledDataSource				fPooledDataSource;

	private static ArrayList<TourType>			fActiveTourTypes;
	private static ArrayList<TourType>			fTourTypes;
	
	private static HashMap<Long, TourTag>		fTourTags;
	private static HashMap<Long, TagCollection>	fTagCollections;

	private boolean								fIsTableChecked;
	private boolean								fIsVersionChecked;

	private final ListenerList					fPropertyListeners							= new ListenerList(ListenerList.IDENTITY);

	private String								fDatabasePath								= (Platform.getInstanceLocation()
																									.getURL()
																									.getPath() + "derby-database");				//$NON-NLS-1$

	{
		// set storage location for the database
		System.setProperty("derby.system.home", fDatabasePath); //$NON-NLS-1$

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

	/**
	 * removes all tour tags which are loaded from the database so the next time they will be
	 * reloaded
	 */
	public static void clearTourTags() {

		if (fTourTags != null) {
			fTourTags.clear();
			fTourTags = null;
		}

		if (fTagCollections != null) {
			fTagCollections.clear();
			fTagCollections = null;
		}
	}

	/**
	 * remove all tour types and dispose their images so the next time they have to be loaded from
	 * the database and the images are recreated
	 */
	public static void clearTourTypes() {

		if (fTourTypes != null) {
			fTourTypes.clear();
			fTourTypes = null;
		}

		UI.getInstance().setTourTypeImagesDirty();
	}

	public static void computeComputedValuesForAllTours() {

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				final ArrayList<Long> tourList = getAllTourIds();

				monitor.beginTask(Messages.Tour_Database_update_computed_values, tourList.size());

				// loop over all tours and calculate and set new columns
				int tourCounter = 1;
				for (final Long tourId : tourList) {

					monitor.subTask(NLS.bind(Messages.Tour_Database_update_tour,//
							new Object[] { tourCounter++, tourList.size() }));

					final TourData tourData = getTourFromDb(tourId);
					if (tourData != null) {

						tourData.computeComputedValues();
						saveTour(tourData);
					}

					monitor.worked(1);
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void computeComputedValuesForAllTours(final IProgressMonitor monitor) {

		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours, compute computed fields and save the tour
		int tourCounter = 1;
		for (final Long tourId : tourList) {

			monitor.subTask(NLS.bind(Messages.Tour_Database_update_tour,//
					new Object[] { tourCounter++, tourList.size() }));

			final TourData tourData = getTourFromDb(tourId);
			if (tourData != null) {

				tourData.computeComputedValues();
				saveTour(tourData);
			}
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
	 * @return Returns a list with all {@link TourType}'s.<br>
	 *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
	 *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
	 */
	public static ArrayList<TourType> getActiveTourTypes() {
		return fActiveTourTypes;
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

		if (fTourTags != null) {
			return fTourTags;
		}

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourTag" //$NON-NLS-1$
					+ (" FROM " + TourDatabase.TABLE_TOUR_TAG + " TourTag ")); //$NON-NLS-1$ //$NON-NLS-2$

			fTourTags = new HashMap<Long, TourTag>();

			for (final TourTag tourTag : ((List<TourTag>) query.getResultList())) {
				fTourTags.put(tourTag.getTagId(), tourTag);
			}

			em.close();
		}

		return fTourTags;
	}

	/**
	 * @return Returns all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourType> getAllTourTypes() {

		if (fTourTypes != null) {
			return fTourTypes;
		}

		fTourTypes = new ArrayList<TourType>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourType" //$NON-NLS-1$
					+ (" FROM " + TourDatabase.TABLE_TOUR_TYPE + " TourType ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourType.name")); //$NON-NLS-1$

			fTourTypes = (ArrayList<TourType>) query.getResultList();

			em.close();
		}

		return fTourTypes;
	}

	public static TourDatabase getInstance() {
		if (instance == null) {
			instance = new TourDatabase();
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	public static TagCollection getRootTags() {

		if (fTagCollections == null) {
			fTagCollections = new HashMap<Long, TagCollection>();
		}

		final long rootTagId = -1L;

		TagCollection rootEntry = fTagCollections.get(Long.valueOf(rootTagId));
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
		Query query = em.createQuery("SELECT TourTagCategory " //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY + " AS TourTagCategory ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE TourTagCategory.isRoot = 1") //$NON-NLS-1$
				+ (" ORDER  BY TourTagCategory.name")); //$NON-NLS-1$

		rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

		/*
		 * read tour tags from db
		 */
		query = em.createQuery("SELECT TourTag " //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_TAG + " AS TourTag ") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE TourTag.isRoot = 1") //$NON-NLS-1$
				+ (" ORDER  BY TourTag.name")); //$NON-NLS-1$

		rootEntry.tourTags = (ArrayList<TourTag>) query.getResultList();

		em.close();

		fTagCollections.put(rootTagId, rootEntry);

		return rootEntry;
	}

	/**
	 * @param categoryId
	 * @return Returns a {@link TagCollection} with all tags and categories for the category Id
	 */
	public static TagCollection getTagEntries(final long categoryId) {

		if (fTagCollections == null) {
			fTagCollections = new HashMap<Long, TagCollection>();
		}

		final Long categoryIdValue = Long.valueOf(categoryId);

		TagCollection categoryEntries = fTagCollections.get(categoryIdValue);
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

		fTagCollections.put(categoryIdValue, categoryEntries);

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
					throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-1$
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

			final Query query = em.createQuery("SELECT TourPerson " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_PERSON + " TourPerson ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ORDER  BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$

			tourPeople = (ArrayList<TourPerson>) query.getResultList();

			em.close();
		}

		return tourPeople;
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
	 * tour
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
			System.out.println("Cannot save a tour without a person: " + tourData);//$NON-NLS-1$
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
				e.printStackTrace();
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

			final Connection conn = getPooledConnection();

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

					createTableTourTagV5(stmt);
					createTableTourTagCategoryV5(stmt);

					stmt.executeBatch();
				}
				stmt.close();

				fIsTableChecked = true;

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

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private boolean checkVersion(final IProgressMonitor monitor) {

		if (fIsVersionChecked) {
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

					MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
							Messages.tour_database_version_info_title,
							NLS.bind(Messages.tour_database_version_info_message, currentDbVersion, TOURBOOK_DB_VERSION));
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

			fIsVersionChecked = true;

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
	 * Checks if the database server is running, if not it will start the server. startServerJob has
	 * a job when the server is not yet started
	 */
	private IRunnableWithProgress createStartServerRunnable() {

		IRunnableWithProgress startServerRunnable = null;

		// load derby driver
		try {
			Class.forName(DERBY_CLIENT_DRIVER);
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
	 *create table {@link #TABLE_TOUR_DATA}
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
				//	+ "gpsData 				BLOB,				\n" //$NON-NLS-1$
				//
				// version 5 end
				//
				+ "tourType_typeId 		BIGINT,				\n" //$NON-NLS-1$
				+ "tourPerson_personId 	BIGINT,				\n" //$NON-NLS-1$

				// version 6 start
				//				
				+ "tourImportFilePath	VARCHAR(255),		\n" //$NON-NLS-1$
				//				
				// version 6 end

				// version 7 start
				//				
				+ "mergeFromTourId				BIGINT,				\n" //$NON-NLS-1$
				+ "mergeIntoTourId				BIGINT,				\n" //$NON-NLS-1$
				+ "mergedTourTimeOffset			INTEGER DEFAULT 0,	\n" //$NON-NLS-1$
				+ "mergedAltitudeOffset			INTEGER DEFAULT 0,	\n" //$NON-NLS-1$
				//				
				// version 7 end

				+ "serieData 			BLOB NOT NULL		\n" //$NON-NLS-1$

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
		stmt.execute("" //$NON-NLS-1$
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
		final Object[] allListeners = fPropertyListeners.getListeners();
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

	private Connection getPooledConnection() {

		if (fPooledDataSource == null) {
			try {

				fPooledDataSource = new ComboPooledDataSource();

				//loads the jdbc driver 
				fPooledDataSource.setDriverClass(DERBY_CLIENT_DRIVER);
				fPooledDataSource.setJdbcUrl(DERBY_URL);
				fPooledDataSource.setUser(TABLE_SCHEMA);
				fPooledDataSource.setPassword(TABLE_SCHEMA);

				fPooledDataSource.setMaxPoolSize(100);
				fPooledDataSource.setMaxStatements(100);
				fPooledDataSource.setMaxStatementsPerConnection(20);

			} catch (final PropertyVetoException e) {
				e.printStackTrace();
			}
		}

		Connection conn = null;
		try {
			conn = fPooledDataSource.getConnection();
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return conn;
	}

	public void removePropertyListener(final IPropertyListener listener) {
		fPropertyListeners.remove(listener);
	}

	private void runStartServer(final IProgressMonitor monitor) {

		monitor.subTask(Messages.Database_Monitor_db_service_task);

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
				final Connection connection = getPooledConnection();
				connection.close();

				System.out.println("Database path: " + fDatabasePath); //$NON-NLS-1$

			} catch (final SQLException e1) {
				UI.showSQLException(e1);
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
				fDatabasePath });

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

		/*
		 * database update
		 */
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

		boolean isPostUpdate5 = false;
		if (currentDbVersion == 4) {
			updateDbDesign_4_5(conn, monitor);
			currentDbVersion = newVersion = 5;
			isPostUpdate5 = true;
		}

		if (currentDbVersion == 5) {
			updateDbDesign_5_6(conn);
			currentDbVersion = newVersion = 6;
		}

		if (currentDbVersion == 6) {
			updateDbDesign_6_7(conn);
			currentDbVersion = newVersion = 7;
		}

		/*
		 * update version number
		 */
		updateVersionNumber(conn, newVersion);

		/*
		 * post updates
		 */
		if (isPostUpdate5) {

			/*
			 * do the post update after the version number is updated because the post update uses
			 * connections and this will check the version number
			 */
			TourDatabase.computeComputedValuesForAllTours(monitor);
			TourManager.getInstance().removeAllToursFromCache();
		}

		return true;
	}

	private void updateDbDesign_1_2(final Connection conn) {

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

	private void updateDbDesign_2_3(final Connection conn) {

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

	private void updateDbDesign_3_4(final Connection conn, final IProgressMonitor monitor) {

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
		monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		emFactory = Persistence.createEntityManagerFactory("tourdatabase"); //$NON-NLS-1$

		monitor.subTask(Messages.Tour_Database_load_all_tours);
		final ArrayList<Long> tourList = getAllTourIds();

		// loop over all tours and calculate and set new columns
		int tourIdx = 1;
		for (final Long tourId : tourList) {

			final TourData tourData = getTourFromDb(tourId);

			if (monitor != null) {
				final String msg = NLS.bind(Messages.Tour_Database_update_tour, new Object[] {
						tourIdx++,
						tourList.size() });
				monitor.subTask(msg);
			}

			tourData.computeComputedValues();

			final TourPerson person = tourData.getTourPerson();
			tourData.setTourBike(person.getTourBike());
			tourData.setBikerWeight(person.getWeight());

			saveTour(tourData);
		}

		// cleanup everything as if nothing has happened
		emFactory.close();
		emFactory = null;
	}

	private void updateDbDesign_4_5(final Connection conn, final IProgressMonitor monitor) {

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

	private void updateDbDesign_5_6(final Connection conn) {

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

	private void updateDbDesign_6_7(final Connection conn) {

		System.out.println();
		System.out.println("database update: 7");//$NON-NLS-1$

		try {
			final Statement statement = conn.createStatement();

			String sql;

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeFromTourId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeIntoTourId		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(sql);
			statement.execute(sql);

			statement.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		System.out.println("database is updated");//$NON-NLS-1$
		System.out.println();
	}

	private void updateVersionNumber(final Connection conn, final int newVersion) {
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
