/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionModifyColumns;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class PhotosAndToursView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String						ID								= "net.tourbook.photo.merge.PhotosAndToursView.ID"; //$NON-NLS-1$

	private static final String						STATE_CAMERA_ADJUSTMENT_NAME	= "STATE_CAMERA_ADJUSTMENT_NAME";					//$NON-NLS-1$
	private static final String						STATE_CAMERA_ADJUSTMENT_TIME	= "STATE_CAMERA_ADJUSTMENT_TIME";					//$NON-NLS-1$
	private static final String						STATE_SELECTED_CAMERA_NAME		= "STATE_SELECTED_CAMERA_NAME";					//$NON-NLS-1$
	private static final String						STATE_FILTER_PHOTOS				= "STATE_FILTER_PHOTOS";							//$NON-NLS-1$
	private static final String						STATE_FILTER_NO_TOURS			= "STATE_FILTER_NO_TOURS";							//$NON-NLS-1$

	public static final String						IMAGE_PIC_DIR_VIEW				= "IMAGE_PIC_DIR_VIEW";							//$NON-NLS-1$
	public static final String						IMAGE_PHOTO_PHOTO				= "IMAGE_PHOTO_PHOTO";								//$NON-NLS-1$

	private static final String						CAMERA_UNKNOWN_KEY				= "CAMERA_UNKNOWN_KEY";							//$NON-NLS-1$

	private final IPreferenceStore					_prefStore						= TourbookPlugin
																							.getDefault()
																							.getPreferenceStore();

	private final IDialogSettings					_state							= TourbookPlugin
																							.getDefault()
																							.getDialogSettingsSection(
																									ID);

	private ArrayList<MergeTour>					_allDbTours						= new ArrayList<MergeTour>();
	private ArrayList<MergeTour>					_allMergeTours					= new ArrayList<MergeTour>();

	private ArrayList<PhotoWrapper>					_allPhotos						= new ArrayList<PhotoWrapper>();

	/**
	 * Contains all cameras which are every used, key is the camera name.
	 */
	private static HashMap<String, Camera>			_allAvailableCameras			= new HashMap<String, Camera>();

	/**
	 * Contains all cameras which are used in all displayed tours.
	 */
	private HashMap<String, Camera>					_allTourCameras					= new HashMap<String, Camera>();

	/**
	 * All cameras sorted by camera name
	 */
	private Camera[]								_allTourCamerasSorted;

	/**
	 * Merge tour which is currently selected in the merge tour viewer.
	 */
	private MergeTour								_selectedMergeTour;

	private PostSelectionProvider					_postSelectionProvider;
	private ISelectionListener						_postSelectionListener;

	private IPropertyChangeListener					_prefChangeListener;
	private IPartListener2							_partListener;

	private PixelConverter							_pc;
	private ColumnManager							_columnManager;

	private ActionFilterPhotos						_actionFilterPhotos;
	private ActionFilterNoTours						_actionFilterNoTours;
	private ActionModifyColumns						_actionModifyColumns;
	private ActionSetTourGPSIntoPhotos				_actionSetTourGPSIntoPhotos;

	private Connection								_sqlConnection;
	private PreparedStatement						_sqlStatement;

	private final PeriodFormatter					_durationFormatter				= new PeriodFormatterBuilder()
																							.appendYears()
																							.appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
																							.appendMonths()
																							.appendSuffix("m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
																							.appendDays()
																							.appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
																							.appendHours()
																							.appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
																							.toFormatter();

	private final DateTimeFormatter					_dateFormatter					= DateTimeFormat.shortDate();
	private final DateTimeFormatter					_timeFormatter					= DateTimeFormat.mediumTime();
	private final NumberFormat						_nf_1_1							= NumberFormat.getNumberInstance();
	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	private final Comparator<? super PhotoWrapper>	_adjustTimeComparator;

	/**
	 * When <code>true</code>, only tours with photos are displayed.
	 */
	private boolean									_isShowToursOnlyWithPhotos		= true;
	private boolean									_isFilterNoTours;

	/*
	 * UI controls
	 */
	private PageBook								_pageBook;
	private Composite								_pageNoTour;
	private Composite								_pageNoImage;
	private Composite								_pageViewer;

	private Composite								_viewerContainer;
	private TableViewer								_tourViewer;
	private Combo									_comboCamera;

	private Spinner									_spinnerHours;
	private Spinner									_spinnerMinutes;
	private Spinner									_spinnerSeconds;

	{
		_adjustTimeComparator = new Comparator<PhotoWrapper>() {

			@Override
			public int compare(final PhotoWrapper wrapper1, final PhotoWrapper wrapper2) {

				final long diff = wrapper1.adjustedTime - wrapper2.adjustedTime;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;
			}

		};
	}

	private static class ContentComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final MergeTour mt1 = (MergeTour) e1;
			final MergeTour mt2 = (MergeTour) e2;

			/*
			 * sort by time
			 */
			final long mt1Time = mt1.isHistoryTour ? mt1.historyStartTime : mt1.tourStartTime;
			final long mt2Time = mt2.isHistoryTour ? mt2.historyStartTime : mt2.tourStartTime;

			if (mt1Time != 0 && mt2Time != 0) {
				return mt1Time > mt2Time ? 1 : -1;
			}

			return mt1Time != 0 ? 1 : -1;
		}
	}

	private class ContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _allMergeTours.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PhotosAndToursView() {
		super();
	}

	void actionFilterNoTours() {

		_isFilterNoTours = _actionFilterNoTours.isChecked();

		updateUI(_selectedMergeTour, false);

		enableControls();
	}

	void actionFilterPhotos() {

		_isShowToursOnlyWithPhotos = _actionFilterPhotos.isChecked();

		updateUI(_selectedMergeTour, false);
	}

	void actionSetTourGPSIntoPhotos() {

		for (final PhotoWrapper photoWrapper : _selectedMergeTour.tourPhotos) {

			final File imageFile = photoWrapper.imageFile;

		}
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotosAndToursView.this) {
					closeConnection();
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

//					// measurement system has changed
//
//					UI.updateUnits();
//					updateInternalUnitValues();
//
//					_columnManager.saveState(_state);
//					_columnManager.clearColumns();
//					defineAllColumns(_viewerContainer);
//
//					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

				} else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					// app filter is modified

					updateUI(_selectedMergeTour, true);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tourViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTable().redraw();
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == PhotosAndToursView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_allMergeTours.clear();
		_allPhotos.clear();
		_selectedMergeTour = null;

		_tourViewer.setInput(new Object[0]);

		_postSelectionProvider.clearSelection();

		_pageBook.showPage(_pageNoImage);
	}

	private void closeConnection() {

		if (_sqlConnection != null) {

			Util.sqlClose(_sqlStatement);
			TourDatabase.closeConnection(_sqlConnection);

			_sqlStatement = null;
			_sqlConnection = null;

		}
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);
		_actionFilterPhotos = new ActionFilterPhotos(this);
		_actionFilterNoTours = new ActionFilterNoTours(this);
		_actionSetTourGPSIntoPhotos = new ActionSetTourGPSIntoPhotos(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr2) {
				fillContextMenu(menuMgr2);
			}
		});

		final Table table = _tourViewer.getTable();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		table.setMenu(tableContextMenu);

//		getSite().registerContextMenu(menuMgr, _tourViewer);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	/**
	 * create pseudo tours for photos which are not contained in a tour and remove all tours which
	 * do not contain any photos
	 */
	private void createMergeTours() {

		MergeTour currentMergeTour = createMergeTours_10_GetFirstTour();

		_allMergeTours.clear();
		_selectedMergeTour = null;

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final int numberOfRealTours = _allDbTours.size();
		long nextDbTourStartTime = numberOfRealTours > 0 ? _allDbTours.get(0).tourStartTime : Long.MIN_VALUE;

		int tourIndex = 0;
		long photoTime = 0;

		// loop: all photos
		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final Photo photo = photoWrapper.photo;
			photoTime = photoWrapper.adjustedTime;

			// check if current photo can be put into current merge tour
			if (currentMergeTour.isHistoryTour == false && photoTime <= currentMergeTour.tourEndTime) {

				// current photo can be put into current real tour

			} else if (currentMergeTour.isHistoryTour && photoTime < nextDbTourStartTime) {

				// current photo can be put into current history tour

			} else {

				// current photo do not fit into current merge tour

				// finalize current merge tour
				createMergeTours_30_FinalizeCurrentMergeTour(currentMergeTour, tourCameras);

				currentMergeTour = null;
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

						final MergeTour dbMergeTour = _allDbTours.get(tourIndex);

						final long dbTourStart = dbMergeTour.tourStartTime;
						final long dbTourEnd = dbMergeTour.tourEndTime;

						if (photoTime < dbTourStart) {

							// image time is before the next tour start, create history tour

							nextDbTourStartTime = dbTourStart;

							break;
						}

						if (photoTime >= dbTourStart && photoTime <= dbTourEnd) {

							// current photo can be put into current tour

							currentMergeTour = dbMergeTour;

							break;
						}

						// current tour do not contain any images

						if (_isShowToursOnlyWithPhotos == false) {

							// tours without photos are displayed

							createMergeTours_40_AddTour(dbMergeTour);
						}

						// get start time for the next tour
						if (tourIndex + 1 < numberOfRealTours) {
							nextDbTourStartTime = _allDbTours.get(tourIndex + 1).tourStartTime;
						} else {
							nextDbTourStartTime = Long.MAX_VALUE;
						}
					}
				}

				if (currentMergeTour == null) {

					// create history tour

					currentMergeTour = new MergeTour(photoTime);
				}
			}

			currentMergeTour.tourPhotos.add(photoWrapper);

			// set camera into the photo
			final Camera camera = setCamera(photo);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				currentMergeTour.numberOfNoGPSPhotos++;
			} else {
				currentMergeTour.numberOfGPSPhotos++;
			}
		}

		createMergeTours_30_FinalizeCurrentMergeTour(currentMergeTour, tourCameras);

		createMergeTours_60_MergeHistoryTours();

//		System.out.println(UI.timeStampNano() + " \t");
//		System.out.println(UI.timeStampNano() + " \t");
//
//		for (final MergeTour mergeTour : _allMergeTours) {
//			System.out.println(UI.timeStampNano() + " \t" + mergeTour);
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
	}

	/**
	 * Get/Create first merge tour
	 */
	private MergeTour createMergeTours_10_GetFirstTour() {

		MergeTour currentMergeTour = null;

		if (_allDbTours.size() > 0) {

			// real tours are available

			final MergeTour firstTour = _allDbTours.get(0);
			final PhotoWrapper firstPhotoWrapper = _allPhotos.get(0);

			final DateTime firstPhotoTime = new DateTime(firstPhotoWrapper.adjustedTime);
			if (firstPhotoTime.isBefore(firstTour.tourStartTime)) {

				// first photo is before the first tour, create dummy tour

			} else {

				// first tour starts before the first photo

				currentMergeTour = firstTour;
			}
		} else {

			// there are no real tours, create dummy tour
		}

		if (currentMergeTour == null) {

			// 1st tour is a history tour

			final long tourStartUTC = _allPhotos.get(0).adjustedTime;
//			final int tourStartUTCZoneOffset = DateTimeZone.getDefault().getOffset(tourStartUTC);

			currentMergeTour = new MergeTour(tourStartUTC);
		}

		return currentMergeTour;
	}

	/**
	 * Keep current merge tour when it contains photos.
	 * 
	 * @param currentMergeTour
	 * @param tourCameras
	 */
	private void createMergeTours_30_FinalizeCurrentMergeTour(	final MergeTour currentMergeTour,
																final HashMap<String, String> tourCameras) {

		// keep only tours which contain photos
		final boolean isNoPhotos = currentMergeTour.tourPhotos.size() == 0;

		if (isNoPhotos && currentMergeTour.isHistoryTour || isNoPhotos && _isShowToursOnlyWithPhotos) {
			return;
		}

		// set tour end time
		if (currentMergeTour.isHistoryTour) {
			currentMergeTour.setTourEndTime(Long.MAX_VALUE);
		}

		setTourCameras(tourCameras, currentMergeTour);

		createMergeTours_40_AddTour(currentMergeTour);
	}

	private void createMergeTours_40_AddTour(final MergeTour mergeTour) {

		boolean isAddMergeTour = true;
		final int numberOfMergeTours = _allMergeTours.size();

		if (numberOfMergeTours > 0) {

			// check if this tour is already added, this algorithm to add tours is a little bit complex

			final MergeTour prevTour = _allMergeTours.get(numberOfMergeTours - 1);
			if (prevTour.equals(mergeTour)) {
				isAddMergeTour = false;
			}
		}

		if (isAddMergeTour) {
			_allMergeTours.add(mergeTour);
		}
	}

	/**
	 * History tours can occure multiple times in sequence, when tours between history tours do not
	 * contain photos. This will merge multiple history tours into one.
	 */
	private void createMergeTours_60_MergeHistoryTours() {

		if (_allMergeTours.size() == 0) {
			return;
		}

		boolean isSubsequentHistory = false;
		boolean isHistory = false;

		for (final MergeTour mergeTour : _allMergeTours) {

			if (isHistory && mergeTour.isHistoryTour == isHistory) {

				// 2 subsequent tours contains the same tour type
				isSubsequentHistory = true;
				break;
			}

			isHistory = mergeTour.isHistoryTour;
		}

		if (isSubsequentHistory == false) {
			// there is nothing to merge
			return;
		}

		final ArrayList<MergeTour> mergedMergeTours = new ArrayList<MergeTour>();
		MergeTour prevHistoryTour = null;

		for (final MergeTour mergeTour : _allMergeTours) {

			final boolean isHistoryTour = mergeTour.isHistoryTour;

			if (isHistoryTour && prevHistoryTour == null) {

				// first history tour

				prevHistoryTour = mergeTour;

				continue;
			}

			if (isHistoryTour && prevHistoryTour != null) {

				// this is a subsequent history tour, it is merged into previous history tour

				prevHistoryTour.tourPhotos.addAll(mergeTour.tourPhotos);
				prevHistoryTour.numberOfGPSPhotos += mergeTour.numberOfGPSPhotos;
				prevHistoryTour.numberOfNoGPSPhotos += mergeTour.numberOfNoGPSPhotos;

				continue;
			}

			if (isHistoryTour == false && prevHistoryTour != null) {

				// this is a real tour, finalize previous history tour

				prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
				mergedMergeTours.add(prevHistoryTour);
			}

			prevHistoryTour = null;

			// this is a real tour

			mergedMergeTours.add(mergeTour);
		}

		if (prevHistoryTour != null) {

			// finalize previous history tour
			prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
			mergedMergeTours.add(prevHistoryTour);
		}

		_allMergeTours.clear();
		_allMergeTours.addAll(mergedMergeTours);
	}

	private void createMergeTours_90_FilterNoTours() {

		_allMergeTours.clear();
		_selectedMergeTour = null;

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final MergeTour historyTour = new MergeTour(_allPhotos.get(0).adjustedTime);
		historyTour.tourPhotos.addAll(_allPhotos);

		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final Photo photo = photoWrapper.photo;

			// set camera into the photo
			final Camera camera = setCamera(photo);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				historyTour.numberOfNoGPSPhotos++;
			} else {
				historyTour.numberOfGPSPhotos++;
			}
		}

		setTourCameras(tourCameras, historyTour);

		// finalize history tour
		historyTour.setTourEndTime(Long.MAX_VALUE);

		_allMergeTours.clear();
		_allMergeTours.add(historyTour);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);

		createActions();
		fillToolbar();

		addSelectionListener();
		addPrefListener();
		addPartListener();

		restoreState();

		enableControls();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		// show default page
		_pageBook.showPage(_pageNoImage);
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		{
			_pageViewer = new Composite(_pageBook, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageViewer);
			GridLayoutFactory.fillDefaults().applyTo(_pageViewer);
			_pageViewer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_20_Tours(_pageViewer);
			}

			_pageNoTour = new Composite(_pageBook, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageNoTour);
			GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoTour);
			{
				final Label label = new Label(_pageNoTour, SWT.WRAP);
				label.setText(Messages.Photos_AndTours_Label_NoTourIsAvailable);
			}

			_pageNoImage = createUI_90_PageNoImage(_pageBook);
		}
	}

	private void createUI_20_Tours(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_viewerContainer);
		{
			createUI_40_Header(_viewerContainer);
			createUI_50_TourViewer(_viewerContainer);
		}
	}

	private void createUI_40_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.margins(2, 2)
				.applyTo(container);
		{
			/*
			 * label: adjust time
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Photos_AndTours_Label_AdjustTime);
			label.setToolTipText(Messages.Photos_AndTours_Label_AdjustTimeTooltip);

			createUI_42_AdjustTime(container);

			/*
			 * combo: camera
			 */
			_comboCamera = new Combo(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().applyTo(_comboCamera);
			_comboCamera.setVisibleItemCount(33);
			_comboCamera.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectCamera();
				}
			});
		}
	}

	private void createUI_42_AdjustTime(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(0, 0).applyTo(container);
		{
			/*
			 * spinner: adjust hours
			 */
			_spinnerHours = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.applyTo(_spinnerHours);
			_spinnerHours.setMinimum(-99);
			_spinnerHours.setMaximum(99);
			_spinnerHours.setIncrement(1);
			_spinnerHours.setPageIncrement(24);
			_spinnerHours.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustHours_Tooltip);
			_spinnerHours.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectTimeAdjustment();
				}

			});
			_spinnerHours.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelectTimeAdjustment();
				}
			});

			/*
			 * spinner: adjust minutes
			 */
			_spinnerMinutes = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.applyTo(_spinnerMinutes);
			_spinnerMinutes.setMinimum(-99);
			_spinnerMinutes.setMaximum(99);
			_spinnerMinutes.setIncrement(1);
			_spinnerMinutes.setPageIncrement(10);
			_spinnerMinutes.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustMinutes_Tooltip);
			_spinnerMinutes.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectTimeAdjustment();
				}

			});
			_spinnerMinutes.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelectTimeAdjustment();
				}
			});

			/*
			 * spinner: adjust seconds
			 */
			_spinnerSeconds = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.applyTo(_spinnerSeconds);
			_spinnerSeconds.setMinimum(-99);
			_spinnerSeconds.setMaximum(99);
			_spinnerSeconds.setIncrement(1);
			_spinnerSeconds.setPageIncrement(10);
			_spinnerSeconds.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustSeconds_Tooltip);
			_spinnerSeconds.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectTimeAdjustment();
				}

			});
			_spinnerSeconds.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelectTimeAdjustment();
				}
			});
		}
	}

	private void createUI_50_TourViewer(final Composite parent) {

		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * create table viewer
		 */
		_tourViewer = new TableViewer(table);
		_columnManager.createColumns(_tourViewer);

		_tourViewer.setUseHashlookup(true);
		_tourViewer.setContentProvider(new ContentProvider());
		_tourViewer.setComparator(new ContentComparator());

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					onSelectTour(selection);
				}
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

			}
		});

		createContextMenu();
	}

	private Composite createUI_90_PageNoImage(final Composite parent) {

		final int defaultWidth = 200;

		final Composite page = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(page);
		{
			final Composite container = new Composite(page, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
			{
				final Label label = new Label(container, SWT.WRAP);
				label.setText(Messages.Photos_AndTours_Label_NoSelectedPhoto);
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

				/*
				 * link: import
				 */
				final CLabel iconPicDirView = new CLabel(container, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconPicDirView);
				iconPicDirView.setImage(UI.IMAGE_REGISTRY.get(IMAGE_PIC_DIR_VIEW));

				final Link linkImport = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.indent(0, 10)
						.applyTo(linkImport);
				linkImport.setText(Messages.Photos_AndTours_Link_PhotoDirectory);
				linkImport.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						Util.showView(PicDirView.ID);
					}
				});
			}
		}

		return page;
	}

	private void defineAllColumns(final Composite parent) {

		defineColumn_TourTypeImage();
		defineColumn_NumberOfPhotos();
		defineColumn_NumberOfGPSPhotos();
		defineColumn_NumberOfNoGPSPhotos();
		defineColumn_TourStartDate();
		defineColumn_DurationTime();
		defineColumn_TourCameras();
		defineColumn_TourStartTime();
		defineColumn_TourEndDate();
		defineColumn_TourEndTime();
		defineColumn_TourTypeText();
	}

	/**
	 * column: duration time
	 */
	private void defineColumn_DurationTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_DURATION_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();

				final Period period = mergedTour.tourPeriod;

				int periodSum = 0;
				for (final int value : period.getValues()) {
					periodSum += value;
				}

				if (periodSum == 0) {
					// < 1 h
					cell.setText(Messages.PhotosAndToursView_Photos_AndTours_Label_DurationLess1Hour);
				} else {
					// > 1 h
					cell.setText(period.toString(_durationFormatter));
				}

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: number of photos which contain gps data
	 */
	private void defineColumn_NumberOfGPSPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.NUMBER_OF_GPS_PHOTOS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final int numberOfGPSPhotos = mergedTour.numberOfGPSPhotos;

				cell.setText(numberOfGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfGPSPhotos));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: number of photos which contain gps data
	 */
	private void defineColumn_NumberOfNoGPSPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.NUMBER_OF_NO_GPS_PHOTOS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final int numberOfNoGPSPhotos = mergedTour.numberOfNoGPSPhotos;

				cell.setText(numberOfNoGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfNoGPSPhotos));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: number of photos
	 */
	private void defineColumn_NumberOfPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.NUMBER_OF_PHOTOS.createColumn(_columnManager, _pc);
//		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final int numberOfPhotos = mergedTour.tourPhotos.size();

				cell.setText(numberOfPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfPhotos));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumn_TourCameras() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_CAMERA.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof MergeTour) {

					final MergeTour mergedTour = (MergeTour) element;

					cell.setText(mergedTour.tourCameras);

					setBgColor(cell, mergedTour);
				}
			}
		});
	}

	/**
	 * column: tour end date
	 */
	private void defineColumn_TourEndDate() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_END_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
//		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final long historyTime = mergedTour.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE
						? _dateFormatter.print(mergedTour.tourEndTime)
						: _dateFormatter.print(historyTime));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: tour end time
	 */
	private void defineColumn_TourEndTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_END_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final long historyTime = mergedTour.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE
						? _timeFormatter.print(mergedTour.tourEndTime)
						: _timeFormatter.print(historyTime));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: tour start date
	 */
	private void defineColumn_TourStartDate() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_START_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final long historyTime = mergedTour.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE
						? _dateFormatter.print(mergedTour.tourStartTime)
						: _dateFormatter.print(historyTime));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: tour start time
	 */
	private void defineColumn_TourStartTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_START_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergeTour mergedTour = (MergeTour) cell.getElement();
				final long historyTime = mergedTour.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE
						? _timeFormatter.print(mergedTour.tourStartTime)
						: _timeFormatter.print(historyTime));

				setBgColor(cell, mergedTour);
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumn_TourTypeImage() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof MergeTour) {

					final MergeTour mergedTour = (MergeTour) element;

					if (mergedTour.isHistoryTour) {

						cell.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PHOTO_PHOTO));

					} else {

						final long tourTypeId = mergedTour.tourTypeId;
						if (tourTypeId == -1) {

							cell.setImage(null);

						} else {

							final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId);

							/*
							 * when a tour type image is modified, it will keep the same image
							 * resource only the content is modified but in the rawDataView the
							 * modified image is not displayed compared with the tourBookView which
							 * displays the correct image
							 */
							cell.setImage(tourTypeImage);
						}
					}
				}
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumn_TourTypeText() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof MergeTour) {

					final MergeTour mergedTour = (MergeTour) element;
					final long tourTypeId = mergedTour.tourTypeId;
					if (tourTypeId == -1) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));
					}

					setBgColor(cell, mergedTour);
				}
			}
		});
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getViewSite().getPage();

		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableControls() {

		final boolean isPhotoAvailable = _allPhotos.size() > 0;
		final boolean isPhotoFilter = _actionFilterNoTours.isChecked() == false;
		final boolean isTourWithPhotos = _selectedMergeTour != null && _selectedMergeTour.tourPhotos.size() > 0;

		boolean isTourWithGPS = false;

		if (isTourWithPhotos && _selectedMergeTour.tourId != Long.MIN_VALUE) {

			final TourData tourData = TourManager.getInstance().getTourData(_selectedMergeTour.tourId);

			isTourWithGPS = tourData != null && tourData.latitudeSerie != null;
		}

		_comboCamera.setEnabled(isPhotoAvailable);
		_spinnerHours.setEnabled(isPhotoAvailable);
		_spinnerMinutes.setEnabled(isPhotoAvailable);
		_spinnerSeconds.setEnabled(isPhotoAvailable);

		_actionFilterNoTours.setEnabled(isPhotoAvailable);
		_actionFilterPhotos.setEnabled(isPhotoAvailable && isPhotoFilter);

		// is true when selected tour contains photos
		_actionSetTourGPSIntoPhotos.setEnabled(isTourWithGPS

		// it's too dangerous when all photos are contained in 1 tour
				&& _isFilterNoTours == false);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionSetTourGPSIntoPhotos);
	}

	private void fillToolbar() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionFilterPhotos);
		tbm.add(_actionFilterNoTours);
		tbm.add(new Separator());
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private Camera getSelectedCamera() {

		final int cameraIndex = _comboCamera.getSelectionIndex();
		if (cameraIndex == -1) {
			return null;
		}

		return _allTourCamerasSorted[cameraIndex];
	}

	public ArrayList<TourData> getSelectedTours() {
		return new ArrayList<TourData>();
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void loadToursFromDb(final long allDbTourStartDate, final long allDbTourEndDate) {

		BusyIndicator.showWhile(_pageBook.getDisplay(), new Runnable() {
			public void run() {
				loadToursFromDb_Runnable(allDbTourStartDate, allDbTourEndDate);
			}
		});
	}

	private void loadToursFromDb_Runnable(final long allDbTourStartDate, final long allDbTourEndDate) {

//		final long start = System.currentTimeMillis();

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "" // //$NON-NLS-1$

				+ "SELECT " //$NON-NLS-1$

				+ " TourId," //						1 //$NON-NLS-1$
				+ " TourStartTime," //				2 //$NON-NLS-1$
				+ " TourEndTime," //				3 //$NON-NLS-1$
				+ " TourType_TypeId" //			4 //$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				+ " WHERE" //$NON-NLS-1$
				+ (" TourStartTime >= ?") //$NON-NLS-1$
				+ (" AND TourEndTime <= ?") //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ UI.NEW_LINE

				+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

		_allDbTours.clear();

		try {

			if (_sqlConnection == null) {
				_sqlConnection = TourDatabase.getInstance().getConnection();
				_sqlStatement = _sqlConnection.prepareStatement(sqlString);
			}

			_sqlStatement.setLong(1, allDbTourStartDate);
			_sqlStatement.setLong(2, allDbTourEndDate);

			sqlFilter.setParameters(_sqlStatement, 3);

			final ResultSet result = _sqlStatement.executeQuery();

			while (result.next()) {

				final MergeTour dbTour = new MergeTour(result.getLong(1), result.getLong(2), result.getLong(3));

				final Object dbTourTypeId = result.getObject(4);
				dbTour.tourTypeId = (dbTourTypeId == null ? //
						TourDatabase.ENTITY_IS_NOT_SAVED
						: (Long) dbTourTypeId);

				_allDbTours.add(dbTour);
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		} finally {
			closeConnection();
		}

//		System.out.println(UI.timeStampNano() + " loadToursFromDb_Runnable\t");
//		System.out.println(UI.timeStampNano() + " time\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void onSelectCamera() {

		final Camera camera = getSelectedCamera();
		if (camera == null) {
			return;
		}

		// update UI

		final long timeAdjustment = camera.timeAdjustment / 1000;

//		timeAdjustment = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000);

//		_formatter.format(//
//				Messages.Format_hhmmss,
//				(time / 3600),
//				((time % 3600) / 60),
//				((time % 3600) % 60)).toString()

		final int hours = (int) (timeAdjustment / 3600);
		final int minutes = (int) ((timeAdjustment % 3600) / 60);
		final int seconds = (int) ((timeAdjustment % 3600) % 60);

		_spinnerHours.setSelection(hours);
		_spinnerMinutes.setSelection(minutes);
		_spinnerSeconds.setSelection(seconds);
	}

	private void onSelectionChanged(final ISelection selection) {

	}

	private void onSelectTimeAdjustment() {

		if (_selectedMergeTour == null) {
			// a tour is not selected
			return;
		}

		final Camera camera = getSelectedCamera();
		if (camera == null) {
			return;
		}

		camera.setTimeAdjustment(
				_spinnerHours.getSelection(),
				_spinnerMinutes.getSelection(),
				_spinnerSeconds.getSelection());

		updateUI(_selectedMergeTour, false);
	}

	/**
	 */
	private void onSelectTour(final StructuredSelection selection) {

		final Object firstElement = selection.getFirstElement();
		if (firstElement instanceof MergeTour) {

			final MergeTour mergeTour = (MergeTour) firstElement;

			_selectedMergeTour = mergeTour;

			enableControls();

			// fire selected tour
			final ISelection tourSelection = new TourPhotoSelection(mergeTour);
			_postSelectionProvider.setSelection(tourSelection);
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_tourViewer.getTable().dispose();

			createUI_50_TourViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _tourViewer;
	}

	@Override
	public void reloadViewer() {
		_tourViewer.setInput(new Object[0]);
	}

	private void restoreState() {

		// photo filter
		_isFilterNoTours = Util.getStateBoolean(_state, STATE_FILTER_NO_TOURS, true);
		_isShowToursOnlyWithPhotos = Util.getStateBoolean(_state, STATE_FILTER_PHOTOS, true);

		_actionFilterNoTours.setChecked(_isFilterNoTours);
		_actionFilterPhotos.setChecked(_isShowToursOnlyWithPhotos);

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

		final String prevCameraName = Util.getStateString(_state, STATE_SELECTED_CAMERA_NAME, null);
		updateUI_Cameras(prevCameraName);
	}

	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

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
		 * selected camera
		 */
		final Camera selectedCamera = getSelectedCamera();
		if (selectedCamera != null) {

			final String cameraName = selectedCamera.cameraName;

			if (cameraName != null) {
				_state.put(STATE_SELECTED_CAMERA_NAME, cameraName);
			}
		}

		// photo filter
		_state.put(STATE_FILTER_PHOTOS, _actionFilterPhotos.isChecked());
		_state.put(STATE_FILTER_NO_TOURS, _actionFilterNoTours.isChecked());

		_columnManager.saveState(_state);
	}

	private void selectTour(final MergeTour requestedTour) {

		/*
		 * 1st try to select a tour
		 */
		MergeTour selectedTour = null;
		if (requestedTour == null) {

			// select first tour
			selectedTour = _allMergeTours.get(0);

		} else if (requestedTour.isHistoryTour == false) {

			// select a real tour by tour id
			selectedTour = requestedTour;
		}

		ISelection newSelection = null;
		if (selectedTour != null) {
			_tourViewer.setSelection(new StructuredSelection(selectedTour), true);
			newSelection = _tourViewer.getSelection();
		}

		if (requestedTour == null) {
			// there is nothing which can be compared in equals()
			return;
		}

		/*
		 * 2nd try to select a tour
		 */
		// check if tour is selected
		if (newSelection == null || newSelection.isEmpty()) {

			MergeTour mergeTourSelection = null;

			final ArrayList<PhotoWrapper> tourPhotos = requestedTour.tourPhotos;
			if (tourPhotos.size() > 0) {

				// get tour for the first photo

				final long tourPhotoTime = tourPhotos.get(0).imageUTCTime;

				for (final MergeTour mergeTour : _allMergeTours) {

					final long mergeStartTime = mergeTour.isHistoryTour
							? mergeTour.historyStartTime
							: mergeTour.tourStartTime;
					final long mergeEndTime = mergeTour.isHistoryTour //
							? mergeTour.historyEndTime
							: mergeTour.tourEndTime;

					if (tourPhotoTime >= mergeStartTime && tourPhotoTime <= mergeEndTime) {
						mergeTourSelection = mergeTour;
						break;
					}
				}

			} else {

				// get tour by checking intersection

				final long requestedStartTime = requestedTour.isHistoryTour
						? requestedTour.historyStartTime
						: requestedTour.tourStartTime;
				final long requestedEndTime = requestedTour.isHistoryTour //
						? requestedTour.historyEndTime
						: requestedTour.tourEndTime;

				final long requestedTime = requestedStartTime + ((requestedEndTime - requestedStartTime) / 2);

				for (final MergeTour mergeTour : _allMergeTours) {

					final long mergeStartTime = mergeTour.isHistoryTour
							? mergeTour.historyStartTime
							: mergeTour.tourStartTime;
					final long mergeEndTime = mergeTour.isHistoryTour //
							? mergeTour.historyEndTime
							: mergeTour.tourEndTime;

					final boolean isIntersects = requestedTime > mergeStartTime && requestedTime < mergeEndTime;

					if (isIntersects) {
						mergeTourSelection = mergeTour;
						break;
					}
				}
			}

			if (mergeTourSelection != null) {

				_tourViewer.setSelection(new StructuredSelection(mergeTourSelection), false);
				newSelection = _tourViewer.getSelection();
			}
		}

		/*
		 * 3rd try to select a tour
		 */
		if (newSelection == null || newSelection.isEmpty()) {

			// previous selections failed, select first tour
			final MergeTour firstTour = _allMergeTours.get(0);

			_tourViewer.setSelection(new StructuredSelection(firstTour), true);
		}

		// set focus rubberband to selected item, most of the time it is not at the correct position
		final Table table = _tourViewer.getTable();
		table.setSelection(table.getSelectionIndex());
	}

	private void setBgColor(final ViewerCell cell, final MergeTour mergeTour) {

//		if (mergeTour.isHistoryTour()) {
//			cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
//		} else {
//			cell.setBackground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_BG_HISTORY_TOUR));
//		}
	}

	/**
	 * Creates a camera when not yet created and sets it into the photo.
	 * 
	 * @param photo
	 * @return Returns camera which is set into the photo.
	 */
	private Camera setCamera(final Photo photo) {

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

		_allTourCameras.put(photoCameraName, camera);
		photo.getPhotoWrapper().camera = camera;

		return camera;
	}

	/**
	 * This example illustrates how to set the GPS values in JPEG EXIF metadata.
	 * 
	 * @param jpegImageFile
	 *            A source image file.
	 * @param dst
	 *            The output file.
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	public void setExifGPSTag(final File jpegImageFile, final File dst) throws IOException, ImageReadException, ImageWriteException {
		
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
				final double longitude = -74.0; // 74 degrees W (in Degrees East)
				final double latitude = 40 + 43 / 60.0; // 40 degrees N (in Degrees
				// North)

				outputSet.setGPSInDegrees(longitude, latitude);
			}

			os = new FileOutputStream(dst);
			os = new BufferedOutputStream(os);

			new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);

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

	@Override
	public void setFocus() {
		_tourViewer.getTable().setFocus();
	}

	private void setPhotoTimeAdjustment() {

		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final long utcTime = photoWrapper.imageUTCTime;
			final long cameraTimeAdjustment = photoWrapper.camera.timeAdjustment;

			photoWrapper.adjustedTime = utcTime + cameraTimeAdjustment;
		}

		Collections.sort(_allPhotos, _adjustTimeComparator);
	}

	private void setTourCameras(final HashMap<String, String> cameras, final MergeTour historyTour) {

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

	void updatePhotosAndTours(final MergePhotoTourSelection photoMergeSelection) {

		final ArrayList<PhotoWrapper> tourPhotos = photoMergeSelection.selectedPhotos;

		if (tourPhotos.size() == 0) {
			clearView();
			return;
		}

		_allPhotos.clear();
		_allPhotos.addAll(tourPhotos);

		_allTourCameras.clear();

		// ensure camera is set for all photos
		for (final PhotoWrapper photoWrapper : _allPhotos) {
			if (photoWrapper.camera == null) {
				setCamera(photoWrapper.photo);
			}
		}

		updateUI(null, true);
	}

	private void updateUI(final MergeTour mergeTour, final boolean isLoadToursFromDb) {

		if (_allPhotos.size() == 0) {
			// view is not fully initialized, this happend in the pref listener
			return;
		}

		// this must be called BEFORE start/end date are set
		setPhotoTimeAdjustment();

		if (isLoadToursFromDb) {

			/*
			 * get date for 1st and last photo
			 */
			long startDateForAllDbTours = _allPhotos.get(0).adjustedTime;
			long endDateForAllDbTours = startDateForAllDbTours;

			for (final PhotoWrapper photoWrapper : _allPhotos) {

				final long imageSortingTime = photoWrapper.imageUTCTime;

				if (imageSortingTime < startDateForAllDbTours) {
					startDateForAllDbTours = imageSortingTime;
				} else if (imageSortingTime > endDateForAllDbTours) {
					endDateForAllDbTours = imageSortingTime;
				}
			}

			// adjust by 5 days that time adjustments are covered
			startDateForAllDbTours -= 5 * UI.DAY_IN_SECONDS * 1000;
			endDateForAllDbTours += 5 * UI.DAY_IN_SECONDS * 1000;

			loadToursFromDb(startDateForAllDbTours, endDateForAllDbTours);

		} else {

			// reset merge tours

			for (final MergeTour mergeDbTour : _allDbTours) {

				mergeDbTour.tourPhotos.clear();

				mergeDbTour.numberOfGPSPhotos = 0;
				mergeDbTour.numberOfNoGPSPhotos = 0;
			}
		}

		if (_isFilterNoTours) {
			createMergeTours_90_FilterNoTours();
		} else {
			createMergeTours();
		}

		updateUI_Cameras(null);

		_tourViewer.setInput(new Object[0]);

		enableControls();

		_pageBook.showPage(_pageViewer);

		if (_allMergeTours.size() == 0) {
			return;
		}

		selectTour(mergeTour);
	}

	/**
	 * fill camera combo and select previous selection
	 * 
	 * @param defaultCameraName
	 */
	private void updateUI_Cameras(final String defaultCameraName) {

		// get previous camera
		String currentSelectedCameraName = null;
		if (defaultCameraName == null) {

			final int currentSelectedCameraIndex = _comboCamera.getSelectionIndex();
			if (currentSelectedCameraIndex != -1) {
				currentSelectedCameraName = _comboCamera.getItem(currentSelectedCameraIndex);
			}

		} else {
			currentSelectedCameraName = defaultCameraName;
		}

		_comboCamera.removeAll();

		// sort cameras
		final Collection<Camera> cameraValues = _allTourCameras.values();
		_allTourCamerasSorted = cameraValues.toArray(new Camera[cameraValues.size()]);
		Arrays.sort(_allTourCamerasSorted);

		int cameraComboIndex = -1;

		for (int cameraIndex = 0; cameraIndex < _allTourCamerasSorted.length; cameraIndex++) {

			final Camera camera = _allTourCamerasSorted[cameraIndex];
			_comboCamera.add(camera.cameraName);

			// get index for the last selected camera
			if (cameraComboIndex == -1
					&& currentSelectedCameraName != null
					&& currentSelectedCameraName.equals(camera.cameraName)) {
				cameraComboIndex = cameraIndex;
			}
		}

		_comboCamera.getParent().layout();

		// select previous camera
		_comboCamera.select(cameraComboIndex == -1 ? 0 : cameraComboIndex);

		// update spinners for camera time adjustment
		onSelectCamera();
	}
}
