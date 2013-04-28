/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.SQLFilter;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.joda.time.DateTime;

public class TourPhotoManager implements IPhotoServiceProvider {

	private static final String						STATE_CAMERA_ADJUSTMENT_NAME	= "STATE_CAMERA_ADJUSTMENT_NAME";	//$NON-NLS-1$
	private static final String						STATE_CAMERA_ADJUSTMENT_TIME	= "STATE_CAMERA_ADJUSTMENT_TIME";	//$NON-NLS-1$
	private static final String						STATE_REPLACE_IMAGE_FOLDER		= "STATE_REPLACE_IMAGE_FOLDER";	//$NON-NLS-1$

	private static final String						CAMERA_UNKNOWN_KEY				= "CAMERA_UNKNOWN_KEY";			//$NON-NLS-1$

	private final IPreferenceStore					_prefStore						= TourbookPlugin.getDefault() //
																							.getPreferenceStore();

	private static final IDialogSettings			_state							= TourbookPlugin.getDefault() //
																							.getDialogSettingsSection(
																									"PhotoManager");	//$NON-NLS-1$

	private static TourPhotoManager					_instance;
	/**
	 * Contains all cameras which are every used, key is the camera name.
	 */
	private static HashMap<String, Camera>			_allAvailableCameras			= new HashMap<String, Camera>();

	private Connection								_sqlConnection;

	private PreparedStatement						_sqlStatement;
	private long									_sqlTourStart					= Long.MAX_VALUE;
	private long									_sqlTourEnd						= Long.MIN_VALUE;
	private ArrayList<TourPhotoLink>				_allDbTourPhotoLinks			= new ArrayList<TourPhotoLink>();

	private ArrayList<TourPhotoLink>				_dbTourPhotoLinks				= new ArrayList<TourPhotoLink>();

	private static String							_replaceImageFolder;

	/**
	 * Compares 2 photos by the adjusted time.
	 */
	public static final Comparator<? super Photo>	AdjustTimeComparatorLink;
	public static final Comparator<? super Photo>	AdjustTimeComparatorTour;

	static {

		AdjustTimeComparatorLink = new Comparator<Photo>() {

			@Override
			public int compare(final Photo photo1, final Photo photo2) {

				final long diff = photo1.adjustedTimeLink - photo2.adjustedTimeLink;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;
			}
		};

		AdjustTimeComparatorTour = new Comparator<Photo>() {

			@Override
			public int compare(final Photo photo1, final Photo photo2) {

				final long diff = photo1.adjustedTimeTour - photo2.adjustedTimeTour;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;
			}
		};
	}

	private TourPhotoManager() {

		// set photo service provider into the Photo plugin
		Photo.setPhotoServiceProvider(this);
	}

	public static TourPhotoManager getInstance() {

		if (_instance == null) {
			_instance = new TourPhotoManager();
		}

		return _instance;
	}

	public static TourPhotoLinkView openLinkView() {

//		final IWorkbench wb = PlatformUI.getWorkbench();
		final IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		TourPhotoLinkView linkView = null;

		if (wbWindow != null) {
			try {

				final IWorkbenchPage activePage = wbWindow.getActivePage();

				final IViewPart linkViewPart = activePage.findView(TourPhotoLinkView.ID);

				if (linkViewPart instanceof TourPhotoLinkView) {

					// link view is available in the current perspective

					linkView = (TourPhotoLinkView) linkViewPart;

				} else {

//					final String currentPerspectiveId = activePage.getPerspective().getId();
//
//					if (currentPerspectiveId.equals(TourPhotoLinkView.ID)) {
//
//						// open link view in current perspective
//
//					} else {
//
//						// open link perspective
//
//						wb.showPerspective(PerspectiveFactoryPhoto.PERSPECTIVE_ID, wbWindow);
//					}

					linkView = (TourPhotoLinkView) Util.showView(TourPhotoLinkView.ID, false);
				}

				// ensure link view is visible

				if (linkView != null) {

					// show view but do not make it active
					if (activePage.isPartVisible(linkViewPart) == false) {
						activePage.showView(TourPhotoLinkView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
					}
				}

			} catch (final PartInitException e) {
				StatusUtil.showStatus(e);
//			} catch (final WorkbenchException e) {
//				StatusUtil.showStatus(e);
			}
		}

		return linkView;
	}

	public static void restoreState() {

		// ensure photo service provider is set in the photo
		getInstance();

		/*
		 * cameras + time adjustment
		 */
		final String[] cameraNames = _state.getArray(STATE_CAMERA_ADJUSTMENT_NAME);
		final long[] adjustments = Util.getStateLongArray(_state, STATE_CAMERA_ADJUSTMENT_TIME, null);

		if (cameraNames != null && adjustments != null && cameraNames.length == adjustments.length) {

			// it seems that the values are OK, create cameras with time adjustmens

			for (int index = 0; index < cameraNames.length; index++) {

				final String cameraName = cameraNames[index];

				final Camera camera = new Camera(cameraName);
				camera.timeAdjustment = adjustments[index];

				_allAvailableCameras.put(cameraName, camera);
			}
		}

		/*
		 * replace image folder
		 */
		_replaceImageFolder = _state.get(STATE_REPLACE_IMAGE_FOLDER);
	}

	public static void saveState() {

		/*
		 * camera time adjustment
		 */
		final int size = _allAvailableCameras.size();

		final String[] cameras = new String[size];
		final long[] adjustment = new long[size];

		int index = 0;
		for (final Camera camera : _allAvailableCameras.values()) {
			cameras[index] = camera.cameraName;
			adjustment[index] = camera.timeAdjustment;
			index++;
		}
		_state.put(STATE_CAMERA_ADJUSTMENT_NAME, cameras);
		Util.setState(_state, STATE_CAMERA_ADJUSTMENT_TIME, adjustment);

		/*
		 * replace image folder
		 */
		if (_replaceImageFolder != null) {
			_state.put(STATE_REPLACE_IMAGE_FOLDER, _replaceImageFolder);
		}
	}

	private static void setTourCameras(final HashMap<String, String> cameras, final TourPhotoLink historyTour) {

		final Collection<String> allCameras = cameras.values();
		Collections.sort(new ArrayList<String>(allCameras));

		final StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for (final String camera : allCameras) {
			if (isFirst) {
				isFirst = false;
				sb.append(camera);
			} else {
				sb.append(UI.COMMA_SPACE);
				sb.append(camera);
			}
		}
		historyTour.tourCameras = sb.toString();
	}

	@Override
	public boolean canSaveStarRating(final int selectedPhotos, final int ratingStars) {

		final int warningLevel = 5;

		if (selectedPhotos > warningLevel) {

			final boolean isShowWarning = _prefStore.getBoolean(//
					ITourbookPreferences.TOGGLE_STATE_SHOW_STAR_RATING_SAVE_WARNING) == false;

			if (isShowWarning) {

				final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(//
						Display.getCurrent().getActiveShell(),
						Messages.Photo_TourPhotoMgr_Dialog_SaveStarRating_Title,
						NLS.bind(//
								Messages.Photo_TourPhotoMgr_Dialog_SaveStarRating_Message,
								new Object[] { ratingStars, selectedPhotos, warningLevel }),
						Messages.App_ToggleState_DoNotShowAgain,
						false, // toggle default state
						null,
						null);

				if (dialog.getReturnCode() == Window.OK) {

					// save toggle state only when OK is pressed
					_prefStore.setValue(
							ITourbookPreferences.TOGGLE_STATE_SHOW_STAR_RATING_SAVE_WARNING,
							dialog.getToggleState());

					return true;

				} else {

					return false;
				}
			}
		}

		return true;
	}

	/**
	 * create pseudo tours for photos which are not contained in a tour and remove all tours which
	 * do not contain any photos
	 * 
	 * @param allPhotos
	 * @param visibleTourPhotoLinks
	 * @param isShowToursOnlyWithPhotos
	 * @param isShowToursWithoutSavedPhotos
	 * @param allTourCameras
	 */
	void createTourPhotoLinks(	final ArrayList<Photo> allPhotos,
								final ArrayList<TourPhotoLink> visibleTourPhotoLinks,
								final HashMap<String, Camera> allTourCameras,
								final boolean isShowToursOnlyWithPhotos,
								final boolean isShowToursWithoutSavedPhotos) {

		loadToursFromDb(allPhotos, true);

		TourPhotoLink currentTourPhotoLink = createTourPhotoLinks_10_GetFirstTour(allPhotos);

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final int numberOfRealTours = _dbTourPhotoLinks.size();
		long nextDbTourStartTime = numberOfRealTours > 0 ? _dbTourPhotoLinks.get(0).tourStartTime : Long.MIN_VALUE;

		int tourIndex = 0;
		long photoTime = 0;

		// loop: all photos
		for (final Photo photo : allPhotos) {

			photoTime = photo.adjustedTimeLink;

			// check if current photo can be put into current tour photo link
			if (currentTourPhotoLink.isHistoryTour == false && photoTime <= currentTourPhotoLink.tourEndTime) {

				// current photo can be put into current real tour

			} else if (currentTourPhotoLink.isHistoryTour && photoTime < nextDbTourStartTime) {

				// current photo can be put into current history tour

			} else {

				// current photo do not fit into current photo link

				// finalize current tour photo link
				createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(
						currentTourPhotoLink,
						tourCameras,
						visibleTourPhotoLinks,
						isShowToursOnlyWithPhotos,
						isShowToursWithoutSavedPhotos);

				currentTourPhotoLink = null;
				tourCameras.clear();

				/*
				 * create/get new merge tour
				 */
				if (tourIndex >= numberOfRealTours) {

					/*
					 * there are no further tours which can contain photos, put remaining photos
					 * into a history tour
					 */

					nextDbTourStartTime = Long.MAX_VALUE;

				} else {

					for (; tourIndex < numberOfRealTours; tourIndex++) {

						final TourPhotoLink dbTourPhotoLink = _dbTourPhotoLinks.get(tourIndex);

						final long dbTourStart = dbTourPhotoLink.tourStartTime;
						final long dbTourEnd = dbTourPhotoLink.tourEndTime;

						if (photoTime < dbTourStart) {

							// image time is before the next tour start, create history tour

							nextDbTourStartTime = dbTourStart;

							break;
						}

						if (photoTime >= dbTourStart && photoTime <= dbTourEnd) {

							// current photo can be put into current tour

							currentTourPhotoLink = dbTourPhotoLink;

							break;
						}

						// current tour do not contain any images

						if (isShowToursOnlyWithPhotos == false && isShowToursWithoutSavedPhotos == false) {

							// tours without photos are displayed

							createTourPhotoLinks_40_AddTour(dbTourPhotoLink, visibleTourPhotoLinks);
						}

						// get start time for the next tour
						if (tourIndex + 1 < numberOfRealTours) {
							nextDbTourStartTime = _dbTourPhotoLinks.get(tourIndex + 1).tourStartTime;
						} else {
							nextDbTourStartTime = Long.MAX_VALUE;
						}
					}
				}

				if (currentTourPhotoLink == null) {

					// create history tour

					currentTourPhotoLink = new TourPhotoLink(photoTime);
				}
			}

			currentTourPhotoLink.linkPhotos.add(photo);

			// set camera into the photo
			final Camera camera = setCamera(photo, allTourCameras);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLinkLatitude();
			if (latitude == 0) {
				currentTourPhotoLink.numberOfNoGPSPhotos++;
			} else {
				currentTourPhotoLink.numberOfGPSPhotos++;
			}
		}

		createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(
				currentTourPhotoLink,
				tourCameras,
				visibleTourPhotoLinks,
				isShowToursOnlyWithPhotos,
				isShowToursWithoutSavedPhotos);

		createTourPhotoLinks_60_MergeHistoryTours(visibleTourPhotoLinks);

		/*
		 * set tour GPS into photo
		 */
		final List<TourPhotoLink> tourPhotoLinksWithGps = new ArrayList<TourPhotoLink>();

		for (final TourPhotoLink tourPhotoLink : visibleTourPhotoLinks) {
			if (tourPhotoLink.tourId != Long.MIN_VALUE) {
				tourPhotoLinksWithGps.add(tourPhotoLink);
			}
		}

		if (tourPhotoLinksWithGps.size() > 0) {
			setTourGpsIntoPhotos(tourPhotoLinksWithGps);
		}
	}

	void createTourPhotoLinks_01_OneHistoryTour(final ArrayList<Photo> allPhotos,
												final ArrayList<TourPhotoLink> visibleTourPhotoLinks,
												final HashMap<String, Camera> allTourCameras) {

		loadToursFromDb(allPhotos, false);

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final TourPhotoLink historyTour = new TourPhotoLink(allPhotos.get(0).adjustedTimeLink);
		historyTour.linkPhotos.addAll(allPhotos);

		for (final Photo photo : allPhotos) {

			// set camera into the photo
			final Camera camera = setCamera(photo, allTourCameras);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLinkLatitude();
			if (latitude == 0) {
				historyTour.numberOfNoGPSPhotos++;
			} else {
				historyTour.numberOfGPSPhotos++;
			}
		}

		setTourCameras(tourCameras, historyTour);

		// finalize history tour
		historyTour.setTourEndTime(Long.MAX_VALUE);

		visibleTourPhotoLinks.add(historyTour);
	}

	/**
	 * Get/Create first tour photo link
	 * 
	 * @param allPhotos
	 */
	private TourPhotoLink createTourPhotoLinks_10_GetFirstTour(final ArrayList<Photo> allPhotos) {

		TourPhotoLink currentTourPhotoLink = null;

		if (_dbTourPhotoLinks.size() > 0) {

			// real tours are available

			final TourPhotoLink firstTour = _dbTourPhotoLinks.get(0);
			final Photo firstPhoto = allPhotos.get(0);

			final DateTime firstPhotoTime = new DateTime(firstPhoto.adjustedTimeLink);
			if (firstPhotoTime.isBefore(firstTour.tourStartTime)) {

				// first photo is before the first tour, create dummy tour

			} else {

				// first tour starts before the first photo

				currentTourPhotoLink = firstTour;
			}
		} else {

			// there are no real tours, create dummy tour
		}

		if (currentTourPhotoLink == null) {

			// 1st tour is a history tour

			final long tourStartUTC = allPhotos.get(0).adjustedTimeLink;
//			final int tourStartUTCZoneOffset = DateTimeZone.getDefault().getOffset(tourStartUTC);

			currentTourPhotoLink = new TourPhotoLink(tourStartUTC);
		}

		return currentTourPhotoLink;
	}

	/**
	 * Keep current tour when it contains photos.
	 * 
	 * @param currentTourPhotoLink
	 * @param tourCameras
	 * @param allTourPhotoLinks
	 * @param isShowToursOnlyWithPhotos
	 * @param isShowToursWithoutSavedPhotos
	 */
	private void createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(	final TourPhotoLink currentTourPhotoLink,
																		final HashMap<String, String> tourCameras,
																		final ArrayList<TourPhotoLink> allTourPhotoLinks,
																		final boolean isShowToursOnlyWithPhotos,
																		final boolean isShowToursWithoutSavedPhotos) {

		// keep only tours which contain photos
		final boolean isNoPhotos = currentTourPhotoLink.linkPhotos.size() == 0;
		final boolean isTourPhotos = currentTourPhotoLink.numberOfTourPhotos > 0;

		if (//
			//
			// exclude history tour without photos
		(isNoPhotos && currentTourPhotoLink.isHistoryTour) //
				//
				// exclude real tours without photos
				|| (isNoPhotos && isShowToursOnlyWithPhotos)
				//
				// exclude real tours with saved photos
				|| (isTourPhotos && isShowToursWithoutSavedPhotos)
		//
		) {
			return;
		}

		// set tour end time
		if (currentTourPhotoLink.isHistoryTour) {
			currentTourPhotoLink.setTourEndTime(Long.MAX_VALUE);
		}

		setTourCameras(tourCameras, currentTourPhotoLink);

		createTourPhotoLinks_40_AddTour(currentTourPhotoLink, allTourPhotoLinks);
	}

	private void createTourPhotoLinks_40_AddTour(	final TourPhotoLink tourPhotoLink,
													final ArrayList<TourPhotoLink> allTourPhotoLinks) {

		boolean isAddLink = true;
		final int numberOfLinks = allTourPhotoLinks.size();

		if (numberOfLinks > 0) {

			// check if this tour is already added, this algorithm to add tours is a little bit complex

			final TourPhotoLink prevTour = allTourPhotoLinks.get(numberOfLinks - 1);
			if (prevTour.equals(tourPhotoLink)) {
				isAddLink = false;
			}
		}

		if (isAddLink) {
			allTourPhotoLinks.add(tourPhotoLink);
		}
	}

	/**
	 * History tours can occure multiple times in sequence, when tours between history tours do not
	 * contain photos. This will merge multiple history tours into one.
	 * 
	 * @param allTourPhotoLinks
	 */
	private void createTourPhotoLinks_60_MergeHistoryTours(final ArrayList<TourPhotoLink> allTourPhotoLinks) {

		if (allTourPhotoLinks.size() == 0) {
			return;
		}

		boolean isSubsequentHistory = false;
		boolean isHistory = false;

		for (final TourPhotoLink tourPhotoLink : allTourPhotoLinks) {

			if (isHistory && tourPhotoLink.isHistoryTour == isHistory) {

				// 2 subsequent tours contains the same tour type
				isSubsequentHistory = true;
				break;
			}

			isHistory = tourPhotoLink.isHistoryTour;
		}

		if (isSubsequentHistory == false) {
			// there is nothing to merge
			return;
		}

		final ArrayList<TourPhotoLink> mergedLinks = new ArrayList<TourPhotoLink>();
		TourPhotoLink prevHistoryTour = null;

		for (final TourPhotoLink tourPhotoLink : allTourPhotoLinks) {

			final boolean isHistoryTour = tourPhotoLink.isHistoryTour;

			if (isHistoryTour && prevHistoryTour == null) {

				// first history tour

				prevHistoryTour = tourPhotoLink;

				continue;
			}

			if (isHistoryTour && prevHistoryTour != null) {

				// this is a subsequent history tour, it is merged into previous history tour

				prevHistoryTour.linkPhotos.addAll(tourPhotoLink.linkPhotos);
				prevHistoryTour.numberOfGPSPhotos += tourPhotoLink.numberOfGPSPhotos;
				prevHistoryTour.numberOfNoGPSPhotos += tourPhotoLink.numberOfNoGPSPhotos;

				continue;
			}

			if (isHistoryTour == false && prevHistoryTour != null) {

				// this is a real tour, finalize previous history tour

				prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
				mergedLinks.add(prevHistoryTour);
			}

			prevHistoryTour = null;

			// this is a real tour

			mergedLinks.add(tourPhotoLink);
		}

		if (prevHistoryTour != null) {

			// finalize previous history tour
			prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
			mergedLinks.add(prevHistoryTour);
		}

		allTourPhotoLinks.clear();
		allTourPhotoLinks.addAll(mergedLinks);
	}

	/**
	 * @param imageFolder
	 * @return Returns number of photos which set in {@link TourPhoto}s for a given folder.
	 */
	private ArrayList<String> getTourPhotos(final String imageFolder) {

		final ArrayList<String> tourPhotoImages = new ArrayList<String>();

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sql = "SELECT imageFileName" // 						//$NON-NLS-1$
					+ " FROM " + TourDatabase.TABLE_TOUR_PHOTO //			//$NON-NLS-1$
					+ " WHERE imageFilePath=?"; //							//$NON-NLS-1$

			final PreparedStatement stmt = conn.prepareStatement(sql);

			stmt.setString(1, imageFolder);

			final ResultSet result = stmt.executeQuery();

			while (result.next()) {
				tourPhotoImages.add(result.getString(1));
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (final SQLException e) {
					net.tourbook.ui.UI.showSQLException(e);
				}
			}
		}

		return tourPhotoImages;
	}

	private int getTourPhotoTours(final String imagePath) {

		int numberOfTours = 0;

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sql = "" //													//$NON-NLS-1$

					// get number of tours
					+ " SELECT COUNT(*)" // 											//$NON-NLS-1$
					+ " FROM" //														//$NON-NLS-1$

					// get all tours which contain the image folder
					+ " (" //															//$NON-NLS-1$
					//
					+ (" SELECT DISTINCT " + TourDatabase.TABLE_TOUR_DATA + "_tourId") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" FROM " + TourDatabase.TABLE_TOUR_PHOTO) //						//$NON-NLS-1$
					+ " WHERE imageFilePath=?" //										//$NON-NLS-1$
					//
					+ " ) TourId"; //													//$NON-NLS-1$

			final PreparedStatement stmt = conn.prepareStatement(sql);

			stmt.setString(1, imagePath);

			final ResultSet result = stmt.executeQuery();

			// get first result
			result.next();

			// get first value
			numberOfTours = result.getInt(1);

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (final SQLException e) {
					net.tourbook.ui.UI.showSQLException(e);
				}
			}
		}

		return numberOfTours;
	}

	void linkPhotosWithTours(final PhotosWithExifSelection selectedPhotosWithExif) {

		final TourPhotoLinkView linkView = openLinkView();

		if (linkView != null) {
			linkView.showPhotosAndTours(selectedPhotosWithExif.photos);
		}
	}

	/**
	 * Loads tours from the database for all photos.
	 * 
	 * @param allPhotos
	 * @param isResetGeoPosition
	 * @return Returns <code>true</code> when tours are loaded from the database, <code>false</code>
	 *         is returned when all photo time stamps are within the previously loaded tours.
	 */

	private void loadToursFromDb(final ArrayList<Photo> allPhotos, final boolean isResetGeoPosition) {

		/*
		 * get date for 1st and last photo
		 */
		long firstPhotoTime = allPhotos.get(0).adjustedTimeLink;
		long lastPhotoTime = firstPhotoTime;

		for (final Photo photo : allPhotos) {

			final long imageTime = photo.adjustedTimeLink;

			if (imageTime < firstPhotoTime) {
				firstPhotoTime = imageTime;
			} else if (imageTime > lastPhotoTime) {
				lastPhotoTime = imageTime;
			}

			/*
			 * the adjusted time can set a new position, remove old positions which are not covered
			 * by a tour anymore
			 */
			if (isResetGeoPosition) {
				photo.resetLinkGeoPositions();
			}
		}

		// check if tours are already loaded
		if (firstPhotoTime >= _sqlTourStart && lastPhotoTime <= _sqlTourEnd) {

			// photos are contained in the already loaded tours, data for the 'old' links will be reset

		} else {

			// adjust by 5 days that time adjustments are covered
			final long tourStartDate = firstPhotoTime - 5 * UI.DAY_IN_SECONDS * 1000;
			final long tourEndDate = lastPhotoTime + 5 * UI.DAY_IN_SECONDS * 1000;

			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					loadToursFromDb_Runnable(tourStartDate, tourEndDate);
				}
			});
		}

		_dbTourPhotoLinks.clear();
		boolean isFirstTour = true;

		for (final TourPhotoLink tourPhotoLink : _allDbTourPhotoLinks) {

			final long tourStart = tourPhotoLink.tourStartTime;
			final long tourEnd = tourPhotoLink.tourEndTime;

			if (isFirstTour) {

				// check if this is the first tour

				if (firstPhotoTime > tourEnd) {
					continue;
				} else {
					// first tour is found
					isFirstTour = false;
				}

			} else {

				// subsequent tour

				if (tourStart > lastPhotoTime) {
					break;
				}
			}

			tourPhotoLink.linkPhotos.clear();

			tourPhotoLink.numberOfGPSPhotos = 0;
			tourPhotoLink.numberOfNoGPSPhotos = 0;

			tourPhotoLink.tourCameras = UI.EMPTY_STRING;

			_dbTourPhotoLinks.add(tourPhotoLink);
		}

		return;
	}

	private void loadToursFromDb_Runnable(final long dbStartDate, final long dbEndDate) {

//		final long start = System.currentTimeMillis();

		_allDbTourPhotoLinks.clear();

		try {

			if (_sqlConnection == null) {

				final SQLFilter sqlFilter = new SQLFilter(false);

				final String sql = UI.EMPTY_STRING //

						+ "SELECT " //$NON-NLS-1$

						+ " TourId, " //					1 //$NON-NLS-1$
						+ " TourStartTime, " //				2 //$NON-NLS-1$
						+ " TourEndTime, " //				3 //$NON-NLS-1$
						+ " TourType_TypeId, " //			4 //$NON-NLS-1$

						+ " numberOfPhotos, " //			5 //$NON-NLS-1$
						+ " photoTimeAdjustment " //		6 //$NON-NLS-1$

						+ UI.NEW_LINE

						+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

						+ " WHERE" //$NON-NLS-1$
						+ (" TourStartTime >= ?") //$NON-NLS-1$
						+ (" AND TourEndTime <= ?") //$NON-NLS-1$

						+ sqlFilter.getWhereClause()

						+ UI.NEW_LINE

						+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

				_sqlConnection = TourDatabase.getInstance().getConnection();
				_sqlStatement = _sqlConnection.prepareStatement(sql);

				sqlFilter.setParameters(_sqlStatement, 3);
			}

			_sqlStatement.setLong(1, dbStartDate);
			_sqlStatement.setLong(2, dbEndDate);

			_sqlTourStart = Long.MAX_VALUE;
			_sqlTourEnd = Long.MIN_VALUE;

			final ResultSet result = _sqlStatement.executeQuery();

			while (result.next()) {

				final long dbTourId = result.getLong(1);
				final long dbTourStart = result.getLong(2);
				final long dbTourEnd = result.getLong(3);
				final Object dbTourTypeId = result.getObject(4);
				final int dbNumberOfPhotos = result.getInt(5);
				final int dbPhotoTimeAdjustment = result.getInt(6);

				final TourPhotoLink dbTourPhotoLink = new TourPhotoLink(
						dbTourId,
						dbTourStart,
						dbTourEnd,
						dbNumberOfPhotos,
						dbPhotoTimeAdjustment);

				dbTourPhotoLink.tourTypeId = (dbTourTypeId == null ? //
						TourDatabase.ENTITY_IS_NOT_SAVED
						: (Long) dbTourTypeId);

				_allDbTourPhotoLinks.add(dbTourPhotoLink);

				// get range of all tour start/end
				if (dbTourStart < _sqlTourStart) {
					_sqlTourStart = dbTourStart;
				}
				if (dbTourEnd > _sqlTourEnd) {
					_sqlTourEnd = dbTourEnd;
				}
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}
//		System.out.println("loadToursFromDb_Runnable()\t"
//				+ (System.currentTimeMillis() - start)
//				+ " ms\t"
//				+ (new DateTime(_sqlTourStart))
//				+ "\t"
//				+ new DateTime(_sqlTourEnd));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	@Override
	public void openTour(final HashMap<Long, TourPhotoReference> tourPhotoReferences) {

		for (final TourPhotoReference ref : tourPhotoReferences.values()) {

			// fire a selection for the first tour

			final long tourId = ref.tourId;

			final IWorkbenchWindow wbWin = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (wbWin != null) {

				final IWorkbenchPage wbWinPage = wbWin.getActivePage();
				if (wbWinPage != null) {

					final IWorkbenchPart wbWinPagePart = wbWinPage.getActivePart();
					if (wbWinPagePart != null) {

						final IWorkbenchPartSite site = wbWinPagePart.getSite();
						if (site != null) {

							final ISelectionProvider selectionProvider = site.getSelectionProvider();
							if (selectionProvider instanceof PostSelectionProvider) {

								/*
								 * restore view state when this view is maximized
								 */
								final IWorkbenchPartReference activePartReference = wbWinPage.getActivePartReference();
								final int partState = wbWinPage.getPartState(activePartReference);

								if (partState != IWorkbenchPage.STATE_RESTORED) {
									wbWinPage.setPartState(activePartReference, IWorkbenchPage.STATE_RESTORED);
								}

								// fire selection
								((PostSelectionProvider) selectionProvider).setSelection(new SelectionTourId(tourId));
							}
						}
					}
				}
			}

			break;
		}
	}

	@Override
	public ArrayList<ImagePathReplacement> replaceImageFilePath(final Photo sourcePhoto) {

		final Display display = Display.getDefault();
		final Shell shell = display.getActiveShell();

		final String newImageFolder[] = new String[1];
		final String oldImageFolder = sourcePhoto.imagePathName;

		final ArrayList<String> tourPhotoImageNames = getTourPhotos(oldImageFolder);

		/*
		 * show info when no images are found, this case should not happen because this method is
		 * called with a tour photo and only when the photo image is not found
		 */
		if (tourPhotoImageNames.size() == 0) {

			MessageDialog.openInformation(shell, //
					Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title,
					NLS.bind(//
							Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_NoImage_Message,
							oldImageFolder));

			return null;
		}

		final ArrayList<IPath> validImages = new ArrayList<IPath>();
		final ArrayList<String> inValidImageNames = new ArrayList<String>();
		final int numberOfTourPhotoTours = getTourPhotoTours(oldImageFolder);
		final ArrayList<IPath> modifiedImages = new ArrayList<IPath>();

		if (MessageDialog.openQuestion(shell, //
				Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title,
				NLS.bind(//
						Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Message,
						new Object[] { numberOfTourPhotoTours, //
								tourPhotoImageNames.size(),
								oldImageFolder }))) {

			final DirectoryDialog dialog = new DirectoryDialog(shell, SWT.SAVE);

			dialog.setText(Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title);
			dialog.setMessage(NLS.bind(
					Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_SelectFolder_Message,
					sourcePhoto.imageFileName,
					oldImageFolder));

			if (_replaceImageFolder != null) {
				dialog.setFilterPath(_replaceImageFolder);
			}

			newImageFolder[0] = dialog.open();

			if (newImageFolder[0] != null) {

				// a folder is selected

				_replaceImageFolder = newImageFolder[0];

				// check which images are available at the new location
				BusyIndicator.showWhile(display, new Runnable() {
					public void run() {

						final IPath folderPath = new Path(newImageFolder[0]).addTrailingSeparator();

						for (final String imageName : tourPhotoImageNames) {

							final IPath imagePathName = folderPath.append(imageName);

							if (imagePathName.toFile().exists()) {
								validImages.add(imagePathName);
							} else {
								inValidImageNames.add(imageName);
							}
						}
					}
				});

				if (validImages.size() == 0) {

					// there are no images in the new selected folder

					MessageDialog.openInformation(shell, //
							Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title,
							NLS.bind(//
									Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_NoValidImages_Message,
									newImageFolder[0],
									oldImageFolder));

				} else {

					// there are images in the new selected folder

					if (inValidImageNames.size() == 0) {

						// all images can be replaced

						if (MessageDialog.openQuestion(shell, //
								Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title,
								NLS.bind(//
										Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_ReplaceAll_Message,
										new Object[] { validImages.size(), //
												oldImageFolder,
												newImageFolder[0] }))) {

							modifiedImages.addAll(validImages);
						}

					} else {

						// some images are not available

						if (MessageDialog.openQuestion(shell, //
								Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_Title,
								NLS.bind(//
										Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_ReplacePartly_Message,
										new Object[] {
												validImages.size(),
												inValidImageNames.size(),
												oldImageFolder,
												newImageFolder[0],
												tourPhotoImageNames.size() }))) {

							modifiedImages.addAll(validImages);
						}
					}
				}
			}
		}

		final ArrayList<ImagePathReplacement> replacedImages = new ArrayList<ImagePathReplacement>();

		if (modifiedImages.size() > 0) {

			BusyIndicator.showWhile(display, new Runnable() {
				public void run() {

					final ArrayList<ImagePathReplacement> replacedImagesInDb = replaceImageFilePath_InSQLDb(
							oldImageFolder,
							modifiedImages);

					replacedImages.addAll(replacedImagesInDb);
				}
			});
		}

		/*
		 * show error message with all invalid image names
		 */
		if (validImages.size() > 0 && inValidImageNames.size() > 0) {

			// sort names
			Collections.sort(inValidImageNames);

			final StringBuilder sb = new StringBuilder();

			sb.append(NLS.bind(
					Messages.Photo_TourPhotoMgr_Dialog_ReplacePhotoImage_NoValidImageNames,
					newImageFolder[0]));

			for (final String invalidName : inValidImageNames) {
				sb.append(UI.NEW_LINE + invalidName);
			}

			StatusUtil.showStatus(sb.toString());
		}

		return replacedImages;
	}

	/**
	 * Replace image file path in the tour photo table.
	 * 
	 * @param oldImageFolder
	 * @param modifiedImages
	 * @return
	 */
	private ArrayList<ImagePathReplacement> replaceImageFilePath_InSQLDb(	final String oldImageFolder,
																			final ArrayList<IPath> modifiedImages) {

		final ArrayList<ImagePathReplacement> replacedImages = new ArrayList<ImagePathReplacement>();

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sql = "UPDATE " + TourDatabase.TABLE_TOUR_PHOTO //	//$NON-NLS-1$

					+ " SET" //									//$NON-NLS-1$

					+ " imageFilePath=?, " //				1	//$NON-NLS-1$
					+ " imageFilePathName=? " //			2	//$NON-NLS-1$

					+ " WHERE imageFilePathName=?"; //			3	//$NON-NLS-1$

			final PreparedStatement sqlUpdate = conn.prepareStatement(sql);

			final IPath oldImagePath = new Path(oldImageFolder);

			for (final IPath imagePath : modifiedImages) {

				final String imageFilePath = imagePath.removeLastSegments(1).toOSString();
				final String imageFilePathName = imagePath.toOSString();

				final String imageFileName = imagePath.lastSegment();
				final String oldImageFilePathName = oldImagePath.append(imageFileName).toOSString();

//				if (imageFileName.equals("P1000699.JPG")) {
//					int a = 0;
//					a++;
//				}

				// update photo in db
				sqlUpdate.setString(1, imageFilePath);
				sqlUpdate.setString(2, imageFilePathName);
				sqlUpdate.setString(3, oldImageFilePathName);

				sqlUpdate.executeUpdate();

				replacedImages.add(new ImagePathReplacement(oldImageFilePathName, imagePath));
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (final SQLException e) {
					net.tourbook.ui.UI.showSQLException(e);
				}
			}
		}

		return replacedImages;
	}

	void resetTourStartEnd() {

		if (_sqlConnection != null) {

			Util.sqlClose(_sqlStatement);
			TourDatabase.closeConnection(_sqlConnection);

			_sqlStatement = null;
			_sqlConnection = null;

			// force reloading cached start/end
			_sqlTourStart = Long.MAX_VALUE;
			_sqlTourEnd = Long.MIN_VALUE;
		}
	}

	@Override
	public void saveStarRating(final ArrayList<Photo> photos) {

//		final long start = System.nanoTime();

		Connection conn = null;

		try {
			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement sqlUpdate = conn.prepareStatement(//
					"UPDATE " + TourDatabase.TABLE_TOUR_PHOTO //	//$NON-NLS-1$
							+ " SET" //								//$NON-NLS-1$
							+ " ratingStars=? " //					//$NON-NLS-1$
							+ " WHERE photoId=?"); //				//$NON-NLS-1$

			final ArrayList<Photo> updatedPhotos = new ArrayList<Photo>();

			for (final Photo photo : photos) {

				final int ratingStars = photo.ratingStars;
				final Collection<TourPhotoReference> photoRefs = photo.getTourPhotoReferences().values();

				if (photoRefs.size() > 0) {

					for (final TourPhotoReference photoRef : photoRefs) {

						// update db
						sqlUpdate.setInt(1, ratingStars);
						sqlUpdate.setLong(2, photoRef.photoId);
						sqlUpdate.executeUpdate();

						// update tour photo
						final TourData tourData = TourManager.getInstance().getTourData(photoRef.tourId);
						final Set<TourPhoto> tourPhotos = tourData.getTourPhotos();
						for (final TourPhoto tourPhoto : tourPhotos) {
							if (tourPhoto.getPhotoId() == photoRef.photoId) {
								tourPhoto.setRatingStars(ratingStars);
								break;
							}
						}
					}

					updatedPhotos.add(photo);
				}
			}

			if (updatedPhotos.size() > 0) {

				// fire notification to update all galleries with the modified rating stars

				PhotoManager.firePhotoEvent(null, PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED, photos);
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (final SQLException e) {
					net.tourbook.ui.UI.showSQLException(e);
				}
			}
		}

//		System.out.println(net.tourbook.common.UI.timeStampNano()
//				+ " save photo rating\t"
//				+ ((float) (System.nanoTime() - start) / 1000000)
//				+ " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Creates a camera when not yet created and sets it into the photo.
	 * 
	 * @param photo
	 * @param allTourCameras
	 * @return Returns camera which is set into the photo.
	 */
	Camera setCamera(final Photo photo, final HashMap<String, Camera> allTourCameras) {

		// get camera
		String photoCameraName = null;
		final PhotoImageMetadata metaData = photo.getImageMetaDataRaw();
		if (metaData != null) {
			photoCameraName = metaData.model;
		}

		Camera camera = null;

		if (photoCameraName == null || photoCameraName.length() == 0) {

			// camera is not set in the photo

			camera = _allAvailableCameras.get(CAMERA_UNKNOWN_KEY);

			if (camera == null) {
				camera = new Camera(Messages.Photos_AndTours_Label_NoCamera);
				_allAvailableCameras.put(CAMERA_UNKNOWN_KEY, camera);
			}

		} else {

			// camera is set in the photo

			camera = _allAvailableCameras.get(photoCameraName);

			if (camera == null) {
				camera = new Camera(photoCameraName);
				_allAvailableCameras.put(photoCameraName, camera);
			}
		}

		allTourCameras.put(camera.cameraName, camera);
		photo.camera = camera;

		return camera;
	}

	/**
	 * @param originalJpegImageFile
	 * @param latitude
	 * @param longitude
	 * @return Returns
	 * 
	 *         <pre>
	 * -1 when <b>SERIOUS</b> error occured
	 *  0 when image file is read only
	 *  1 when geo coordinates are written into the image file
	 * </pre>
	 */
	private int setExifGPSTag_IntoImageFile(final File originalJpegImageFile,
											final double latitude,
											final double longitude,
											final boolean[] isReadOnlyMessageDisplayed) {

//		final Shell activeShell = Display.getCurrent().getActiveShell();
//
//		if (originalJpegImageFile.canWrite() == false) {
//
//			if (isReadOnlyMessageDisplayed[0] == false) {
//
//				isReadOnlyMessageDisplayed[0] = true;
//
//				MessageDialog
//						.openError(activeShell, //
//								"Messages.Photos_AndTours_Dialog_ImageIsReadOnly_Title Set Geo Coordinates",
//								NLS
//										.bind(
//												"Messages.Photos_AndTours_Dialog_ImageIsReadOnly_Message The geo coordinates cannot be set into the image file\n\n{0}\n\nbecause the image file is readonly.\n\nFor subsequent image files which are readonly, this message will not be displayed.",
//												originalJpegImageFile.getAbsolutePath()));
//			}
//
//			return 0;
//		}
//
//		File gpsTempFile = null;
//
//		final IPath originalFilePathName = new Path(originalJpegImageFile.getAbsolutePath());
//		final String originalFileNameWithoutExt = originalFilePathName.removeFileExtension().lastSegment();
//
//		final File originalFilePath = originalFilePathName.removeLastSegments(1).toFile();
//		File renamedOriginalFile = null;
//
//		try {
//
//			boolean returnState = false;
//
//			try {
//
//				gpsTempFile = File.createTempFile(//
//						originalFileNameWithoutExt + UI.SYMBOL_UNDERSCORE,
//						UI.SYMBOL_DOT + originalFilePathName.getFileExtension(),
//						originalFilePath);
//
//				setExifGPSTag_IntoImageFile_WithExifRewriter(originalJpegImageFile, gpsTempFile, latitude, longitude);
//
//				returnState = true;
//
//			} catch (final ImageReadException e) {
//				StatusUtil.log(e);
//			} catch (final ImageWriteException e) {
//				StatusUtil.log(e);
//			} catch (final IOException e) {
//				StatusUtil.log(e);
//			}
//
//			if (returnState == false) {
//				return -1;
//			}
//
//			/*
//			 * replace original file with gps file
//			 */
//
//			try {
//
//				/*
//				 * rename original file into a temp file
//				 */
//				final String nanoString = Long.toString(System.nanoTime());
//				final String nanoTime = nanoString.substring(nanoString.length() - 4);
//
//				renamedOriginalFile = File.createTempFile(//
//						originalFileNameWithoutExt + TEMP_FILE_PREFIX_ORIG + nanoTime,
//						UI.SYMBOL_DOT + originalFilePathName.getFileExtension(),
//						originalFilePath);
//
//				final String renamedOriginalFileName = renamedOriginalFile.getAbsolutePath();
//
//				Util.deleteTempFile(renamedOriginalFile);
//
//				boolean isRenamed = originalJpegImageFile.renameTo(new File(renamedOriginalFileName));
//
//				if (isRenamed == false) {
//
//					// original file cannot be renamed
//					MessageDialog.openError(activeShell, //
//							"Messages.Photos_AndTours_ErrorDialog_Title", //$NON-NLS-1$
//							NLS.bind("The image file:\n\n{0}\n\ncannot be renamed into\n\n{1}", //$NON-NLS-1$
//									originalFilePathName.toOSString(),
//									renamedOriginalFileName));
//					return -1;
//				}
//
//				/*
//				 * rename gps temp file into original file
//				 */
//				isRenamed = gpsTempFile.renameTo(originalFilePathName.toFile());
//
//				if (isRenamed == false) {
//
//					// gps file cannot be renamed to original file
//					MessageDialog
//							.openError(activeShell, //
//									"Messages.Photos_AndTours_ErrorDialog_Title", //$NON-NLS-1$
//									NLS
//											.bind(
//													"THERE IS A SERIOUS PROBLEM\n\nThe image file\n\n{0}\n\nwas renamed to\n\n{1}\n\nbut the task of setting the geo\n\n coordinates cannot be\n\n finished.", //$NON-NLS-1$
//													originalFilePathName.toOSString(),
//													renamedOriginalFile.getAbsolutePath()));
//
//					/*
//					 * prevent of deleting renamed original file because the original file is
//					 * renamed into this
//					 */
//					renamedOriginalFile = null;
//
//					return -1;
//				}
//
//				if (renamedOriginalFile.delete() == false) {
//
//					MessageDialog.openError(activeShell, //
//							"Messages.Photos_AndTours_ErrorDialog_Title", //$NON-NLS-1$
//							NLS.bind("The image file:\n\n{0}\n\nwhich was renamed into\n\n{1}\n\ncannot be deleted.", //$NON-NLS-1$
//									originalFilePathName.toOSString(),
//									renamedOriginalFile.getAbsolutePath()));
//				}
//
//			} catch (final IOException e) {
//				StatusUtil.log(e);
//			}
//
//		} finally {
//
//			Util.deleteTempFile(gpsTempFile);
//		}

		return 1;
	}

	/**
	 * This example illustrates how to set the GPS values in JPEG EXIF metadata.
	 * 
	 * @param jpegImageFile
	 *            A source image file.
	 * @param destinationFile
	 *            The output file.
	 * @param latitude
	 * @param longitude
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	private void setExifGPSTag_IntoImageFile_WithExifRewriter(	final File jpegImageFile,
																final File destinationFile,
																final double latitude,
																final double longitude) throws IOException,
			ImageReadException, ImageWriteException {

		OutputStream os = null;

		try {

			TiffOutputSet outputSet = null;

			// note that metadata might be null if no metadata is found.
			final IImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
			final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

			if (null != jpegMetadata) {

				// note that exif might be null if no Exif metadata is found.
				final TiffImageMetadata exif = jpegMetadata.getExif();

				if (null != exif) {
					// TiffImageMetadata class is immutable (read-only).
					// TiffOutputSet class represents the Exif data to write.
					//
					// Usually, we want to update existing Exif metadata by
					// changing
					// the values of a few fields, or adding a field.
					// In these cases, it is easiest to use getOutputSet() to
					// start with a "copy" of the fields read from the image.
					outputSet = exif.getOutputSet();
				}
			}

			// if file does not contain any exif metadata, we create an empty
			// set of exif metadata. Otherwise, we keep all of the other
			// existing tags.
			if (null == outputSet) {
				outputSet = new TiffOutputSet();
			}

			{
				// Example of how to add/update GPS info to output set.

				// New York City
//				final double longitude = -74.0; // 74 degrees W (in Degrees East)
//				final double latitude = 40 + 43 / 60.0; // 40 degrees N (in Degrees
				// North)

				outputSet.setGPSInDegrees(longitude, latitude);
			}

			os = new FileOutputStream(destinationFile);
			os = new BufferedOutputStream(os);

			/**
			 * the lossless method causes an exception after 3 times writing the image file,
			 * therefore the lossy method is used
			 * 
			 * <pre>
			 * 
			 * org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter$ExifOverflowException: APP1 Segment is too long: 65564
			 * 	at org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter.writeSegmentsReplacingExif(ExifRewriter.java:552)
			 * 	at org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter.updateExifMetadataLossless(ExifRewriter.java:393)
			 * 	at org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter.updateExifMetadataLossless(ExifRewriter.java:293)
			 * 	at net.tourbook.photo.PhotosAndToursView.setExifGPSTag_IntoPhoto(PhotosAndToursView.java:2309)
			 * 	at net.tourbook.photo.PhotosAndToursView.setExifGPSTag(PhotosAndToursView.java:2141)
			 * 
			 * </pre>
			 */
//			new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
//			new ExifRewriter().updateExifMetadataLossy(jpegImageFile, os, outputSet);

			os.close();
			os = null;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (final IOException e) {

				}
			}
		}
	}

	private void setTourGpsIntoPhotos(final List<TourPhotoLink> tourPhotoLinksWithGps) {

		for (final TourPhotoLink tourPhotoLink : tourPhotoLinksWithGps) {

			// set tour gps into photos
			setTourGPSIntoPhotos_10(tourPhotoLink);

			/*
			 * update number of photos
			 */
			tourPhotoLink.numberOfGPSPhotos = 0;
			tourPhotoLink.numberOfNoGPSPhotos = 0;

			for (final Photo photo : tourPhotoLink.linkPhotos) {

				// set number of GPS/No GPS photos
				final double latitude = photo.getLinkLatitude();
				if (latitude == 0) {
					tourPhotoLink.numberOfNoGPSPhotos++;
				} else {
					tourPhotoLink.numberOfGPSPhotos++;
				}
			}
		}
	}

	private void setTourGPSIntoPhotos_10(final TourPhotoLink tourPhotoLink) {

		final ArrayList<Photo> allPhotos = tourPhotoLink.linkPhotos;

		final int numberOfPhotos = allPhotos.size();
		if (numberOfPhotos == 0) {
			// no photos are available for this tour
			return;
		}

		final TourData tourData = TourManager.getInstance().getTourData(tourPhotoLink.tourId);

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		if (latitudeSerie == null) {
			// no geo positions
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final int numberOfTimeSlices = timeSerie.length;

		final long tourStart = tourData.getTourStartTime().getMillis() / 1000;
		long timeSliceEnd = tourStart + (long) (timeSerie[1] / 2.0);

		int timeIndex = 0;
		int photoIndex = 0;

		// get first photo
		Photo photo = allPhotos.get(photoIndex);

		// loop: time serie
		while (true) {

			// loop: photo serie, check if a photo is in the current time slice
			while (true) {

				final long imageAdjustedTime = photo.adjustedTimeLink;
				long imageTime = 0;

				if (imageAdjustedTime != Long.MIN_VALUE) {
					imageTime = imageAdjustedTime;
				} else {
					imageTime = photo.imageExifTime;
				}

				final long photoTime = imageTime / 1000;

				if (photoTime <= timeSliceEnd) {

					// photo is contained within the current time slice

					final double tourLatitude = latitudeSerie[timeIndex];
					final double tourLongitude = longitudeSerie[timeIndex];

					setTourGPSIntoPhotos_20(tourData, photo, tourLatitude, tourLongitude);

					photoIndex++;

				} else {

					// advance to the next time slice

					break;
				}

				if (photoIndex < numberOfPhotos) {
					photo = allPhotos.get(photoIndex);
				} else {
					break;
				}
			}

			if (photoIndex >= numberOfPhotos) {
				// no more photos
				break;
			}

			/*
			 * photos are still available
			 */

			// advance to the next time slice on the x-axis
			timeIndex++;

			if (timeIndex >= numberOfTimeSlices - 1) {

				/*
				 * end of tour is reached but there are still photos available, set remaining photos
				 * at the end of the tour
				 */

				while (true) {

					final double tourLatitude = latitudeSerie[timeIndex];
					final double tourLongitude = longitudeSerie[timeIndex];

					setTourGPSIntoPhotos_20(tourData, photo, tourLatitude, tourLongitude);

					photoIndex++;

					if (photoIndex < numberOfPhotos) {
						photo = allPhotos.get(photoIndex);
					} else {
						break;
					}
				}

			} else {

				final long valuePointTime = timeSerie[timeIndex];
				final long sliceDuration = timeSerie[timeIndex + 1] - valuePointTime;

				timeSliceEnd = tourStart + valuePointTime + (sliceDuration / 2);
			}
		}
	}

	private void setTourGPSIntoPhotos_20(	final TourData tourData,
											final Photo photo,
											final double tourLatitude,
											final double tourLongitude) {

		if (photo.isGeoFromExif) {

			// photo contains already EXIF GPS

			// don't overwrite geo from EXIF, use GPS geo from photo wrapper

		} else {

			// set gps from tour into the photo

			photo.setLinkGeoPosition(tourLatitude, tourLongitude);
		}
	}

	@Override
	public void setTourReference(final Photo photo) {

//		final long start = System.nanoTime();

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sql = "SELECT " // 																//$NON-NLS-1$
											//
					+ " photoId, " //											1 //$NON-NLS-1$
					+ (" " + TourDatabase.TABLE_TOUR_DATA + "_tourId, ") // 	2 //$NON-NLS-1$ //$NON-NLS-2$
					//
					+ " adjustedTime, " //										3 //$NON-NLS-1$
					+ " imageExifTime, " //										4 //$NON-NLS-1$
					+ " latitude, " //											5 //$NON-NLS-1$
					+ " longitude, " //											6 //$NON-NLS-1$
					+ " isGeoFromPhoto, " //									7 //$NON-NLS-1$
					+ " ratingStars " //										8 //$NON-NLS-1$
					//
					+ " FROM " + TourDatabase.TABLE_TOUR_PHOTO //				//$NON-NLS-1$
					//
					+ " WHERE imageFilePathName=?"; //							//$NON-NLS-1$

			final PreparedStatement stmt = conn.prepareStatement(sql);

			stmt.setString(1, photo.imageFilePathName);

			final ResultSet result = stmt.executeQuery();

			while (result.next()) {

				final long dbPhotoId = result.getLong(1);
				final long dbTourId = result.getLong(2);

				final long dbAdjustedTime = result.getLong(3);
				final long dbImageExifTime = result.getLong(4);
				final double dbLatitude = result.getDouble(5);
				final double dbLongitude = result.getDouble(6);
				final int dbIsGeoFromExif = result.getInt(7);
				final int dbRatingStars = result.getInt(8);

				photo.addTour(dbTourId, dbPhotoId);

				/*
				 * when a photo is in the photo cache it is possible that the tour is from the file
				 * system, update tour relevant fields
				 */

				photo.isSavedInTour = true;

				photo.adjustedTimeTour = dbAdjustedTime;
				photo.imageExifTime = dbImageExifTime;

				photo.isGeoFromExif = dbIsGeoFromExif == 1;
				photo.isTourPhotoWithGps = dbLatitude != 0;

				photo.ratingStars = dbRatingStars;

				if (photo.getTourLatitude() == 0 && dbLatitude != 0) {
					photo.setTourGeoPosition(dbLatitude, dbLongitude);
				}

				PhotoCache.setPhoto(photo);
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (final SQLException e) {
					net.tourbook.ui.UI.showSQLException(e);
				}
			}
		}

//		System.out.println(net.tourbook.common.UI.timeStampNano()
//				+ " load sql tourId from photo\t"
//				+ ((float) (System.nanoTime() - start) / 1000000)
//				+ " ms");
		// TODO remove SYSTEM.OUT.PRINTLN
	}

}
