/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.photo.Camera;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.PicDirView;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionModifyColumns;

public class TourPhotoLinkView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String							ID											= "net.tourbook.photo.PhotosAndToursView.ID";	//$NON-NLS-1$

	private static final String						STATE_FILTER_NOT_SAVED_PHOTOS		= "STATE_FILTER_NOT_SAVED_PHOTOS";					//$NON-NLS-1$
	private static final String						STATE_FILTER_TOUR_WITH_PHOTOS		= "STATE_FILTER_TOUR_WITH_PHOTOS";					//$NON-NLS-1$
	private static final String						STATE_SELECTED_CAMERA_NAME			= "STATE_SELECTED_CAMERA_NAME";						//$NON-NLS-1$

	public static final String							IMAGE_PIC_DIR_VIEW					= "IMAGE_PIC_DIR_VIEW";									//$NON-NLS-1$
	public static final String							IMAGE_PHOTO_PHOTO						= "IMAGE_PHOTO_PHOTO";									//$NON-NLS-1$

	private static final TourPhotoManager			_photoMgr								= TourPhotoManager.getInstance();

	private final IPreferenceStore					_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings						_state									= TourbookPlugin.getState(ID);

	private ArrayList<TourPhotoLink>					_visibleTourPhotoLinks				= new ArrayList<>();

	private ArrayList<Photo>							_allPhotos								= new ArrayList<>();

	/**
	 * Contains all cameras which are used in all displayed tours.
	 */
	private HashMap<String, Camera>					_allTourCameras						= new HashMap<>();

	/**
	 * All cameras sorted by camera name
	 */
	private Camera[]										_allTourCamerasSorted;

	/**
	 * Tour photo link which is currently selected in the tour viewer.
	 */
	private ArrayList<TourPhotoLink>					_selectedLinks							= new ArrayList<>();

	/**
	 * Contains only tour photo links with real tours and which contain geo positions.
	 */
	private List<TourPhotoLink>						_selectedTourPhotoLinksWithGps	= new ArrayList<>();

	private TourPhotoLinkSelection					_tourPhotoLinkSelection;

	private ISelectionListener							_postSelectionListener;
	private IPropertyChangeListener					_prefChangeListener;
	private IPartListener2								_partListener;

	private PixelConverter								_pc;
	private ColumnManager								_columnManager;

	private ActionFilterTourWithoutSavedPhotos	_actionFilterTourWithoutSavedPhotos;
	private ActionFilterTourWithPhotos				_actionFilterTourWithPhotos;
	private ActionFilterOneHistoryTour				_actionFilterOneHistory;
	private ActionModifyColumns						_actionModifyColumns;
	private ActionSavePhotosInTour					_actionSavePhotoInTour;

	private final PeriodFormatter						_durationFormatter;
	private final NumberFormat							_nf_1_1;
	{
		_nf_1_1 = NumberFormat.getNumberInstance();
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);

		_durationFormatter = new PeriodFormatterBuilder()//
				.appendYears()
				.appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
				.appendMonths()
				.appendSuffix("m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
				.appendDays()
				.appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
				.appendHours()
				.appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
				.toFormatter();
	}

	/**
	 * When <code>true</code>, only tours with photos are displayed.
	 */
	private boolean						_isShowToursWithPhotos				= true;

	/**
	 * When <code>true</code>, only tours with <b>NOT</b> saved photos are displayed.
	 */
	private boolean						_isShowToursWithoutSavedPhotos;

	/**
	 * It's dangerous when set to <code>true</code>, it will hide all tours which can confuses the
	 * user, therefore this state is <b>NOT</b> saved.
	 */
	private boolean						_isFilterOneHistoryTour				= false;

	private ArrayList<TourPhotoLink>	_selectionBackupBeforeOneHistory	= new ArrayList<>();

	private ICommandService				_commandService;

	/*
	 * UI controls
	 */
	private PageBook						_pageBook;
	private Composite						_pageNoImage;
	private Composite						_pageViewer;

	private Composite						_viewerContainer;
	private TableViewer					_tourViewer;

	private Label							_lblAdjustTime;
	private Spinner						_spinnerHours;
	private Spinner						_spinnerMinutes;
	private Spinner						_spinnerSeconds;
	private Combo							_comboCamera;

	private static class ContentComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final TourPhotoLink mt1 = (TourPhotoLink) e1;
			final TourPhotoLink mt2 = (TourPhotoLink) e2;

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

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _visibleTourPhotoLinks.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public TourPhotoLinkView() {
		super();
	}

	void actionFilterNotSavedPhotos() {

		_isShowToursWithoutSavedPhotos = _actionFilterTourWithoutSavedPhotos.isChecked();

		updateUI(_selectedLinks, null);
	}

	void actionFilterOneHistoryTour() {

		_isFilterOneHistoryTour = _actionFilterOneHistory.isChecked();

		ArrayList<TourPhotoLink> links = null;

		if (_isFilterOneHistoryTour) {

			// backup current selection

			final ISelection selection = _tourViewer.getSelection();
			if (selection instanceof StructuredSelection) {

				_selectionBackupBeforeOneHistory.clear();

				for (final Object linkElement : ((StructuredSelection) selection).toArray()) {
					if (linkElement instanceof TourPhotoLink) {
						_selectionBackupBeforeOneHistory.add((TourPhotoLink) linkElement);
					}
				}
			}

		} else {

			links = _selectionBackupBeforeOneHistory;
		}

		updateUI(null, links);

		enableControls();

		if (_isFilterOneHistoryTour == false) {

			final Table table = _tourViewer.getTable();

			table.setSelection(table.getSelectionIndices());
		}
	}

	void actionFilterPhotos() {

		_isShowToursWithPhotos = _actionFilterTourWithPhotos.isChecked();

		updateUI(_selectedLinks, null);
	}

	void actionSavePhotoInTour() {

		if (TourManager.isTourEditorModified()) {
			return;
		}

		final TourManager tourManager = TourManager.getInstance();

		// contains all photos which are modified (it also contains not modified photos)
		final HashSet<Photo> modifiedPhotos = new HashSet<>();

		final ArrayList<TourData> modifiedTours = new ArrayList<>();
		final ArrayList<TourPhotoLink> modifiedLinks = new ArrayList<>();

		final Object[] allSelectedPhotoLinks = ((IStructuredSelection) _tourViewer.getSelection()).toArray();

		int historyTours = 0;

		for (final Object linkElement : allSelectedPhotoLinks) {

			if (linkElement instanceof TourPhotoLink) {

				final TourPhotoLink photoLink = (TourPhotoLink) linkElement;
				final boolean isRealTour = photoLink.tourId != Long.MIN_VALUE;

				if (isRealTour) {

					final ArrayList<Photo> linkPhotos = photoLink.linkPhotos;

					if (linkPhotos.size() > 0) {

						final TourData tourData = tourManager.getTourData(photoLink.tourId);

						if (tourData != null) {

							final HashMap<String, TourPhoto> oldTourPhotos = new HashMap<>();
							final Set<TourPhoto> tourPhotosSet = tourData.getTourPhotos();
							for (final TourPhoto tourPhoto : tourPhotosSet) {
								oldTourPhotos.put(tourPhoto.getImageFilePathName(), tourPhoto);
							}

							// keep existing photos
							final ArrayList<Photo> oldGalleryPhotos = tourData.getGalleryPhotos();
							if (oldGalleryPhotos != null) {
								modifiedPhotos.addAll(oldGalleryPhotos);
							}

							final HashSet<TourPhoto> tourPhotos = new HashSet<>();

							for (final Photo galleryPhoto : linkPhotos) {

								// get existing tour photo
								TourPhoto tourPhoto = oldTourPhotos.get(galleryPhoto.imageFilePathName);

								if (tourPhoto == null) {
									tourPhoto = new TourPhoto(tourData, galleryPhoto);
								}

								// set adjusted time / geo location
								tourPhoto.setAdjustedTime(galleryPhoto.adjustedTimeLink);
								tourPhoto.setGeoLocation(
										galleryPhoto.getLinkLatitude(),
										galleryPhoto.getLinkLongitude());

								tourPhotos.add(tourPhoto);

								// add new/old photos
								modifiedPhotos.add(galleryPhoto);
							}

							tourData.setTourPhotos(tourPhotos, linkPhotos);

							modifiedTours.add(tourData);
							modifiedLinks.add(photoLink);
						}
					}

				} else {

					historyTours++;
				}
			}
		}

		// show message that photos can be saved only in real tours
		if (historyTours > 0) {

			if (_prefStore.getBoolean(ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING) == false) {

				final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(//
						Display.getCurrent().getActiveShell(),
						Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Title,
						Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Message,
						Messages.App_ToggleState_DoNotShowAgain,
						false, // toggle default state
						null,
						null);

				// save toggle state
				_prefStore.setValue(
						ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING,
						dialog.getToggleState());
			}

		}

		final ArrayList<TourData> savedTours = TourManager.saveModifiedTours(modifiedTours);

		/*
		 * after saving tour + photos, update the photos and put them into the photo cache
		 */
		for (final TourData savedTourData : savedTours) {
			savedTourData.createGalleryPhotos();
		}

		// update viewer data
		for (final TourPhotoLink photoLink : modifiedLinks) {

			final TourData tourData = tourManager.getTourData(photoLink.tourId);

			if (tourData != null) {
				photoLink.numberOfTourPhotos = tourData.getTourPhotos().size();
				photoLink.photoTimeAdjustment = tourData.getPhotoTimeAdjustment();
			}
		}

		// update viewer UI
		_tourViewer.update(modifiedLinks.toArray(), null);

		PhotoManager.firePhotoEvent(this, //
				PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED,
				new ArrayList<>(modifiedPhotos));
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotoLinkView.this) {
					onPartActivate();
				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourPhotoLinkView.this) {

					// close sql connections
					_photoMgr.resetTourStartEnd();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
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

					// sql filter is dirty, force reloading cached start/end
					_photoMgr.resetTourStartEnd();

					updateUI(_selectedLinks, null);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tourViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new color
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
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourPhotoLinkView.this) {
					return;
				}
				onSelectionChanged(selection, part);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_visibleTourPhotoLinks.clear();
		_allPhotos.clear();
		_selectedLinks.clear();
		_selectedTourPhotoLinksWithGps.clear();
		_tourPhotoLinkSelection = null;

		_tourViewer.setInput(new Object[0]);

		enableControls();

		_pageBook.showPage(_pageNoImage);
	}

	private void createActions() {

		_actionFilterOneHistory = new ActionFilterOneHistoryTour(this);
		_actionFilterTourWithoutSavedPhotos = new ActionFilterTourWithoutSavedPhotos(this);
		_actionFilterTourWithPhotos = new ActionFilterTourWithPhotos(this);
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionSavePhotoInTour = new ActionSavePhotosInTour(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr2) {
				fillContextMenu(menuMgr2);
			}
		});

		final Table table = _tourViewer.getTable();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		table.setMenu(tableContextMenu);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
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

		_commandService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ICommandService.class);

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
			_lblAdjustTime = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblAdjustTime);
			_lblAdjustTime.setText(Messages.Photos_AndTours_Label_AdjustTime);
			_lblAdjustTime.setToolTipText(Messages.Photos_AndTours_Label_AdjustTime_Tooltip);

//			/*
//			 * radio: all/selected tours
//			 */
//			final Composite containerTours = new Composite(container, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(false, false).applyTo(containerTours);
//			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTours);
//			{
//				_rdoAdjustAllTours = new Button(containerTours, SWT.RADIO);
//				_rdoAdjustAllTours.setText(Messages.Photos_AndTours_Radio_AdjustTime_AllTours);
//				_rdoAdjustAllTours.setToolTipText(Messages.Photos_AndTours_Radio_AdjustTime_AllTours_Tooltip);
//
//				_rdoAdjustSelectedTours = new Button(containerTours, SWT.RADIO);
//				_rdoAdjustSelectedTours.setText(Messages.Photos_AndTours_Radio_AdjustTime_SelectedTours);
//				_rdoAdjustSelectedTours.setToolTipText(Messages.Photos_AndTours_Radio_AdjustTime_SelectedTours_Tooltip);
//			}

			createUI_44_AdjustTime(container);

			/*
			 * combo: camera
			 */
			_comboCamera = new Combo(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
//					.hint(_pc.convertWidthInCharsToPixels(15), SWT.DEFAULT)
					.applyTo(_comboCamera);
			_comboCamera.setVisibleItemCount(33);
			_comboCamera.setToolTipText(Messages.Photos_AndTours_Combo_Camera_Tooltip);
			_comboCamera.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectCamera();
				}
			});
		}
	}

	private void createUI_44_AdjustTime(final Composite parent) {

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
			_spinnerHours.setMinimum(-100);
			_spinnerHours.setMaximum(100);
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
				@Override
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
			_spinnerMinutes.setMinimum(-100);
			_spinnerMinutes.setMaximum(100);
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
				@Override
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
			_spinnerSeconds.setMinimum(-100);
			_spinnerSeconds.setMaximum(100);
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
				@Override
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
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final ISelection eventSelection = event.getSelection();
				if (eventSelection instanceof StructuredSelection) {
					onSelectTour(((StructuredSelection) eventSelection).toArray());
				}
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
				final Image picDirIcon = net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PIC_DIR_VIEW);

				final CLabel iconPicDirView = new CLabel(container, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconPicDirView);
				iconPicDirView.setImage(picDirIcon);
				iconPicDirView.setText(UI.EMPTY_STRING);

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
						Util.showView(PicDirView.ID, true);
					}
				});
			}
		}

		return page;
	}

	private void defineAllColumns(final Composite parent) {

		defineColumn_Tour_TypeImage();
		defineColumn_Photo_NumberOfTourPhotos();
		defineColumn_Photo_TimeAdjustment();
		defineColumn_Photo_NumberOfGPSPhotos();
		defineColumn_Photo_NumberOfNoGPSPhotos();

		defineColumn_Time_TourStartDate();
		defineColumn_Time_TourDurationTime();

		defineColumn_Photo_TourCameras();

		defineColumn_Time_TourStartTime();
		defineColumn_Time_TourEndDate();
		defineColumn_Time_TourEndTime();

		defineColumn_Tour_TypeText();
	}

	/**
	 * column: number of photos which contain gps data
	 */
	private void defineColumn_Photo_NumberOfGPSPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_GPS_PHOTOS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final int numberOfGPSPhotos = link.numberOfGPSPhotos;

				cell.setText(numberOfGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfGPSPhotos));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: number of photos which contain gps data
	 */
	private void defineColumn_Photo_NumberOfNoGPSPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_NO_GPS_PHOTOS.createColumn(
				_columnManager,
				_pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final int numberOfNoGPSPhotos = link.numberOfNoGPSPhotos;

				cell.setText(numberOfNoGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfNoGPSPhotos));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: number of photos which are saved in the tour
	 */
	private void defineColumn_Photo_NumberOfTourPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_PHOTOS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final int numberOfPhotos = link.numberOfTourPhotos;

				cell.setText(numberOfPhotos == 0 ? UI.EMPTY_STRING : Integer.toString(numberOfPhotos));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: number of photos which are saved in the tour
	 */
	private void defineColumn_Photo_TimeAdjustment() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_TIME_ADJUSTMENT.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final int numberOfTourPhotos = link.numberOfTourPhotos;
				final int timeAdjustment = link.photoTimeAdjustment;

				cell.setText(numberOfTourPhotos == 0 //
						? UI.EMPTY_STRING
						: UI.formatHhMmSs(timeAdjustment));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumn_Photo_TourCameras() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_TOUR_CAMERA.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;

					cell.setText(link.tourCameras);

					setBgColor(cell, link);
				}
			}
		});
	}

	/**
	 * column: duration time
	 */
	private void defineColumn_Time_TourDurationTime() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_DURATION_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();

				final Period period = link.tourPeriod;

				int periodSum = 0;
				for (final int value : period.getValues()) {
					periodSum += value;
				}

				if (periodSum == 0) {
					// < 1 h
					cell.setText(Messages.Photos_AndTours_Label_DurationLess1Hour);
				} else {
					// > 1 h
					cell.setText(period.toString(_durationFormatter));
				}

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour end date
	 */
	private void defineColumn_Time_TourEndDate() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_END_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
//		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE //
						? TimeTools.getZonedDateTime(link.tourEndTime).format(TimeTools.Formatter_Date_S)
						: TimeTools.getZonedDateTime(historyTime).format(TimeTools.Formatter_Date_S));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour end time
	 */
	private void defineColumn_Time_TourEndTime() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_END_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE //
						? TimeTools.getZonedDateTime(link.tourEndTime).format(TimeTools.Formatter_Time_M)
						: TimeTools.getZonedDateTime(historyTime).format(TimeTools.Formatter_Time_M));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour start date
	 */
	private void defineColumn_Time_TourStartDate() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE //
						? TimeTools.getZonedDateTime(link.tourStartTime).format(TimeTools.Formatter_Date_S)
						: TimeTools.getZonedDateTime(historyTime).format(TimeTools.Formatter_Date_S));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour start time
	 */
	private void defineColumn_Time_TourStartTime() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE //
						? TimeTools.getZonedDateTime(link.tourStartTime).format(TimeTools.Formatter_Time_M)
						: TimeTools.getZonedDateTime(historyTime).format(TimeTools.Formatter_Time_M));

				setBgColor(cell, link);
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumn_Tour_TypeImage() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;

					if (link.isHistoryTour) {

						cell.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PHOTO_PHOTO));

					} else {

						final long tourTypeId = link.tourTypeId;
						if (tourTypeId == -1) {

							cell.setImage(null);

						} else {

							final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

							/*
							 * when a tour type image is modified, it will keep the same image resource
							 * only the content is modified but in the rawDataView the modified image is
							 * not displayed compared with the tourBookView which displays the correct
							 * image
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
	private void defineColumn_Tour_TypeText() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;
					if (link.isHistoryTour) {

						cell.setText(Messages.Photos_AndTours_Label_HistoryTour);

					} else {

						final long tourTypeId = link.tourTypeId;
						if (tourTypeId == -1) {
							cell.setText(UI.EMPTY_STRING);
						} else {
							cell.setText(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
						}
					}

					setBgColor(cell, link);
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
		final boolean isOneHistory = _actionFilterOneHistory.isChecked();
		final boolean isNoHistory = !isOneHistory;
		final boolean isPhotoFilter = isPhotoAvailable && isNoHistory;

		_lblAdjustTime.setEnabled(isPhotoFilter);
		_spinnerHours.setEnabled(isPhotoFilter);
		_spinnerMinutes.setEnabled(isPhotoFilter);
		_spinnerSeconds.setEnabled(isPhotoFilter);
		_comboCamera.setEnabled(isPhotoFilter);

		_actionFilterTourWithPhotos.setEnabled(isPhotoFilter && _isShowToursWithoutSavedPhotos == false);
		_actionFilterTourWithoutSavedPhotos.setEnabled(isPhotoFilter);
		_actionFilterOneHistory.setEnabled(isPhotoAvailable);
		_actionSavePhotoInTour.setEnabled(isPhotoAvailable);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionSavePhotoInTour);
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

		tbm.add(_actionFilterTourWithPhotos);
		tbm.add(_actionFilterTourWithoutSavedPhotos);
		tbm.add(_actionFilterOneHistory);
		tbm.add(new Separator());
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private PicDirView getPicDirView(final IWorkbenchWindow wbWindow) {

		final IWorkbenchPage wbPage = wbWindow.getActivePage();
		if (wbPage != null) {

			for (final IViewReference viewRef : wbPage.getViewReferences()) {

				if (viewRef.getId().equals(PicDirView.ID)) {

					final IViewPart viewPart = viewRef.getView(false);
					if (viewPart instanceof PicDirView) {
						return (PicDirView) viewPart;
					}
				}
			}
		}

		return null;
	}

	private Camera getSelectedCamera() {

		final int cameraIndex = _comboCamera.getSelectionIndex();
		if (cameraIndex == -1) {
			return null;
		}

		return _allTourCamerasSorted[cameraIndex];
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {
		return new ArrayList<>();
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void onPartActivate() {

		// fire selection
		if (_tourPhotoLinkSelection != null) {
			PhotoManager.firePhotoEvent(this, PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
		}
	}

	private void onSelectCamera() {

		final Camera camera = getSelectedCamera();
		if (camera == null) {
			return;
		}

		// update UI

		final long timeAdjustment = camera.timeAdjustment / 1000;

		final int hours = (int) (timeAdjustment / 3600);
		final int minutes = (int) ((timeAdjustment % 3600) / 60);
		final int seconds = (int) ((timeAdjustment % 3600) % 60);

		_spinnerHours.setSelection(hours);
		_spinnerMinutes.setSelection(minutes);
		_spinnerSeconds.setSelection(seconds);
	}

	private void onSelectionChanged(final ISelection selection, final IWorkbenchPart part) {

//		System.out.println(UI.timeStampNano() + " onSelectionChanged\t" + selection);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (selection instanceof SyncSelection) {

			final ISelection originalSelection = ((SyncSelection) selection).getSelection();

			if (originalSelection instanceof PhotoSelection) {
				showPhotosAndTours(((PhotoSelection) originalSelection).galleryPhotos);
			}

		} else if (selection instanceof PhotoSelection && part instanceof PicDirView) {

			/**
			 * accept photo selection ONLY from the pic dir view, otherwise other photo selections will
			 * cause a view update
			 */

			final PhotoSelection photoSelection = (PhotoSelection) selection;

			final Command command = _commandService.getCommand(ActionHandlerSyncPhotoWithTour.COMMAND_ID);
			final State state = command.getState(RegistryToggleState.STATE_ID);
			final boolean isSync = (Boolean) state.getValue();

			if (isSync) {
				showPhotosAndTours(photoSelection.galleryPhotos);
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();

			_photoMgr.resetTourStartEnd();
		}
	}

	private void onSelectTimeAdjustment() {

		if (_selectedLinks.size() == 0) {
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

		updateUI(_selectedLinks, null);
	}

	/**
	 * Creates a {@link TourPhotoLinkSelection}
	 *
	 * @param allSelectedLinks
	 *           All elements of type {@link TourPhotoLink}
	 */
	private void onSelectTour(final Object[] allSelectedLinks) {

		// get all real tours with geo positions
		_selectedTourPhotoLinksWithGps.clear();

		// contains tour id's for all real tours
		final ArrayList<Long> selectedTourIds = new ArrayList<>();

		final ArrayList<TourPhotoLink> selectedLinks = new ArrayList<>();

		for (final Object linkElement : allSelectedLinks) {

			if (linkElement instanceof TourPhotoLink) {

				final TourPhotoLink selectedLink = (TourPhotoLink) linkElement;

				selectedLinks.add(selectedLink);

				final boolean isRealTour = selectedLink.tourId != Long.MIN_VALUE;

				if (isRealTour) {
					selectedTourIds.add(selectedLink.tourId);
				}

				if (isRealTour && selectedLink.linkPhotos.size() > 0) {

					final TourData tourData = TourManager.getInstance().getTourData(selectedLink.tourId);

					if (tourData != null && tourData.latitudeSerie != null) {
						_selectedTourPhotoLinksWithGps.add(selectedLink);
					}
				}
			}
		}

		if (_selectedLinks.equals(selectedLinks)) {
			// currently selected tour is already selected and selection is fired
			return;
		}

		_selectedLinks.clear();
		_selectedLinks.addAll(selectedLinks);

		enableControls();

		// create tour selection
		_tourPhotoLinkSelection = new TourPhotoLinkSelection(_selectedLinks, selectedTourIds);

		PhotoManager.firePhotoEvent(this, PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
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
		_isShowToursWithPhotos = Util.getStateBoolean(_state, STATE_FILTER_TOUR_WITH_PHOTOS, true);
		_isShowToursWithoutSavedPhotos = Util.getStateBoolean(_state, STATE_FILTER_NOT_SAVED_PHOTOS, false);

		_actionFilterOneHistory.setChecked(_isFilterOneHistoryTour);
		_actionFilterTourWithoutSavedPhotos.setChecked(_isShowToursWithoutSavedPhotos);
		_actionFilterTourWithPhotos.setChecked(_isShowToursWithPhotos);

		final String prevCameraName = Util.getStateString(_state, STATE_SELECTED_CAMERA_NAME, null);
		updateUI_Cameras(prevCameraName);
	}

	@PersistState
	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

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
		_state.put(STATE_FILTER_NOT_SAVED_PHOTOS, _actionFilterTourWithoutSavedPhotos.isChecked());
		_state.put(STATE_FILTER_TOUR_WITH_PHOTOS, _actionFilterTourWithPhotos.isChecked());

		_columnManager.saveState(_state);
	}

	/**
	 * @param prevTourPhotoLink
	 *           Previously selected link, can be <code>null</code>.
	 */
	private void selectTour(final TourPhotoLink prevTourPhotoLink) {

		if (_visibleTourPhotoLinks.size() == 0) {
			return;
		}

		TourPhotoLink selectedTour = null;

		/*
		 * 1st try to select a tour
		 */
		if (prevTourPhotoLink == null) {

			// select first tour
			selectedTour = _visibleTourPhotoLinks.get(0);

		} else if (prevTourPhotoLink.isHistoryTour == false) {

			// select a real tour by tour id
			selectedTour = prevTourPhotoLink;
		}

		ISelection newSelection = null;
		if (selectedTour != null) {
			_tourViewer.setSelection(new StructuredSelection(selectedTour), true);
			newSelection = _tourViewer.getSelection();
		}

		if (prevTourPhotoLink == null) {
			// there is nothing which can be compared in equals()
			return;
		}

		/*
		 * 2nd try to select a tour
		 */
		// check if tour is selected
		if (newSelection == null || newSelection.isEmpty()) {

			TourPhotoLink linkSelection = null;

			final ArrayList<Photo> tourPhotos = prevTourPhotoLink.linkPhotos;
			if (tourPhotos.size() > 0) {

				// get tour for the first photo

				final long tourPhotoTime = tourPhotos.get(0).adjustedTimeLink;

				for (final TourPhotoLink link : _visibleTourPhotoLinks) {

					final long linkStartTime = link.isHistoryTour //
							? link.historyStartTime
							: link.tourStartTime;

					final long linkEndTime = link.isHistoryTour //
							? link.historyEndTime
							: link.tourEndTime;

					if (tourPhotoTime >= linkStartTime && tourPhotoTime <= linkEndTime) {
						linkSelection = link;
						break;
					}
				}

			} else {

				// get tour by checking intersection

				final long requestedStartTime = prevTourPhotoLink.isHistoryTour
						? prevTourPhotoLink.historyStartTime
						: prevTourPhotoLink.tourStartTime;
				final long requestedEndTime = prevTourPhotoLink.isHistoryTour //
						? prevTourPhotoLink.historyEndTime
						: prevTourPhotoLink.tourEndTime;

				final long requestedTime = requestedStartTime + ((requestedEndTime - requestedStartTime) / 2);

				for (final TourPhotoLink link : _visibleTourPhotoLinks) {

					final long linkStartTime = link.isHistoryTour ? link.historyStartTime : link.tourStartTime;
					final long linkEndTime = link.isHistoryTour //
							? link.historyEndTime
							: link.tourEndTime;

					final boolean isIntersects = requestedTime > linkStartTime && requestedTime < linkEndTime;

					if (isIntersects) {
						linkSelection = link;
						break;
					}
				}
			}

			if (linkSelection != null) {

				_tourViewer.setSelection(new StructuredSelection(linkSelection), false);
				newSelection = _tourViewer.getSelection();
			}
		}

		/*
		 * 3rd try to select a tour
		 */
		if (newSelection == null || newSelection.isEmpty()) {

			// previous selections failed, select first tour
			final TourPhotoLink firstTour = _visibleTourPhotoLinks.get(0);

			_tourViewer.setSelection(new StructuredSelection(firstTour), true);
		}

		// set focus rubberband to selected item, most of the time it is not at the correct position
		final Table table = _tourViewer.getTable();
		table.setSelection(table.getSelectionIndex());
	}

	private void setBgColor(final ViewerCell cell, final TourPhotoLink linkTour) {

//		if (linkTour.isHistoryTour()) {
//			cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
//		} else {
//			cell.setBackground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_BG_HISTORY_TOUR));
//		}
	}

	@Override
	public void setFocus() {
		_tourViewer.getTable().setFocus();
	}

	private void setPhotoTimeAdjustment() {

		for (final Photo photo : _allPhotos) {

			final long exifTime = photo.imageExifTime;
			final long cameraTimeAdjustment = photo.camera.timeAdjustment;

			photo.adjustedTimeLink = exifTime + cameraTimeAdjustment;

			// force that the position are updated
			photo.resetLinkWorldPosition();
		}

		Collections.sort(_allPhotos, TourPhotoManager.AdjustTimeComparatorLink);
	}

	/**
	 * @param tourPhotos
	 */
	void showPhotosAndTours(final ArrayList<Photo> tourPhotos) {

		final int numberOfPhotos = tourPhotos.size();

		if (numberOfPhotos == 0) {
			clearView();
			return;
		}

		_allPhotos.clear();
		_allPhotos.addAll(tourPhotos);

		_allTourCameras.clear();

		// ensure camera is set in all photos
		for (final Photo photo : _allPhotos) {
			if (photo.camera == null) {
				_photoMgr.setCamera(photo, _allTourCameras);
			}
		}

		if (numberOfPhotos > 100) {

			BusyIndicator.showWhile(_pageBook.getDisplay(), new Runnable() {
				@Override
				public void run() {
					updateUI(null, _visibleTourPhotoLinks);
				}
			});

		} else {

			updateUI(null, _visibleTourPhotoLinks);
		}
	}

	/**
	 * Update GPS annotation in the image gallery.
	 */
	private void updateAnnotationsInPicDirView() {

		PicDirView picDirView = null;

		final IWorkbench wb = PlatformUI.getWorkbench();

		picDirView = getPicDirView(wb.getActiveWorkbenchWindow());

		if (picDirView == null) {

			for (final IWorkbenchWindow wbWindow : wb.getWorkbenchWindows()) {
				picDirView = getPicDirView(wbWindow);
				if (picDirView != null) {
					break;
				}
			}
		}

		if (picDirView != null) {
			picDirView.refreshUI();
		}
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {
		// TODO Auto-generated method stub

	}

	private void updateUI(	final ArrayList<TourPhotoLink> tourPhotoLinksWhichShouldBeSelected,
									final ArrayList<TourPhotoLink> allLinksWhichShouldBeSelected) {

		if (_allPhotos.size() == 0) {
			// view is not fully initialized, this happend in the pref listener
			return;
		}

		// get previous selected tour
		final TourPhotoLink prevTourPhotoLink[] = { null };
		if (tourPhotoLinksWhichShouldBeSelected != null && tourPhotoLinksWhichShouldBeSelected.size() > 0) {
			prevTourPhotoLink[0] = tourPhotoLinksWhichShouldBeSelected.get(0);
		}

		// this must be called BEFORE start/end date are set
		setPhotoTimeAdjustment();

		_visibleTourPhotoLinks.clear();
		_selectedLinks.clear();
		_selectedTourPhotoLinksWithGps.clear();

		if (_isFilterOneHistoryTour) {

			_photoMgr.createTourPhotoLinks_01_OneHistoryTour(//
					_allPhotos,
					_visibleTourPhotoLinks,
					_allTourCameras);
		} else {

			_photoMgr.createTourPhotoLinks(//
					_allPhotos,
					_visibleTourPhotoLinks,
					_allTourCameras,
					_isShowToursWithPhotos,
					_isShowToursWithoutSavedPhotos);
		}

		updateUI_Cameras(null);

		enableControls();

		// tour viewer update can be a longer task, update other UI element before
		_pageBook.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_tourViewer.getTable().isDisposed()) {
					return;
				}

				_tourViewer.setInput(new Object[0]);
				_pageBook.showPage(_pageViewer);

				updateAnnotationsInPicDirView();

				if (allLinksWhichShouldBeSelected != null && allLinksWhichShouldBeSelected.size() > 0) {

					_tourViewer.setSelection(new StructuredSelection(allLinksWhichShouldBeSelected), true);

				} else {

					selectTour(prevTourPhotoLink[0]);
				}
			}
		});
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
