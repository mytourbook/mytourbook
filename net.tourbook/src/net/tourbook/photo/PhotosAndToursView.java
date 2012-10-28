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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

	public static final String						IMAGE_PIC_DIR_VIEW				= "IMAGE_PIC_DIR_VIEW";							//$NON-NLS-1$

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

	private DateTime								_allPhotoStartDate;

	/**
	 * Contains all cameras used in all tours, key is the camera name.
	 */
	private HashMap<String, Camera>					_allCameras						= new HashMap<String, Camera>();

	/**
	 * Merge tour which is currently selected in the merge tour viewer.
	 */
	private MergeTour								_selectedMergeTour;

	private PostSelectionProvider					_postSelectionProvider;

	private ISelectionListener						_postSelectionListener;
	private IPropertyChangeListener					_prefChangeListener;

	private IPartListener2							_partListener;
	private PixelConverter							_pc;
	private ActionModifyColumns						_actionModifyColumns;
	private ColumnManager							_columnManager;

	/*
	 * measurement unit values
	 */
//	private float									_unitValueAltitude;

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
			final long mt1Time = mt1.tourStartTime;
			final long mt2Time = mt2.tourStartTime;

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

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotosAndToursView.this) {
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

					updateUI(_selectedMergeTour);

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

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);

	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Control viewerControl = _tourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);

		getSite().registerContextMenu(menuMgr, _tourViewer);
	}

	/**
	 * create pseudo tours for photos which are not contained in a tour and remove all tours which
	 * do not contain any photos
	 */
	private void createMergeTours() {

		MergeTour currentMergeTour = null;
		long realTourEnd;
		boolean isHistoryTour = false;

		if (_allDbTours.size() > 0) {

			// real tours are available

			final MergeTour firstTour = _allDbTours.get(0);

			if (_allPhotoStartDate.isBefore(firstTour.tourStartTime)) {

				// first photo is before the first tour, create dummy tour

			} else {

				// first tour starts before the first photo

				currentMergeTour = firstTour;
			}
		} else {

			// there are no tours, create dummy tour
		}

		if (currentMergeTour == null) {
			currentMergeTour = new MergeTour(_allPhotoStartDate.getMillis());
		}

		realTourEnd = currentMergeTour.tourEndTime;
		isHistoryTour = currentMergeTour.isHistoryTour();

		long photoTime = 0;

		_allMergeTours.clear();
		_selectedMergeTour = null;
		TourData currentTourData = null;
		boolean isTourWithGPS = false;

		final int tourIndexStart[] = new int[] { 0 };

		// loop: all photos
		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final Photo photo = photoWrapper.photo;

			photoTime = photoWrapper.adjustedTime;

			boolean isSetupNewTour = false;

			// first check real tours
			if (isHistoryTour == false) {

				// current merge tour is a real tour

				if (photoTime <= realTourEnd) {

					// current tour contains current image

					if (isTourWithGPS) {

						final double latitude = photo.getLatitude();
						if (latitude == Double.MIN_VALUE) {

							// gps position is not available, set position from current tour

//							final currentTourData.gettour
//							photoTime
//

						}
					}

				} else {

					// photo do not fit into current tour, find/create tour for the current image

					createMergeTours_FinalizeCurrentMergeTour(currentMergeTour, photoTime);

					currentMergeTour = createMergeTours_GetNextDbTour(tourIndexStart, photoTime);

					isSetupNewTour = true;
				}

			} else {

				// current merge tour is a dummy tour

				final MergeTour nextMergeTour = createMergeTours_GetNextDbTour(tourIndexStart, photoTime);

				if (nextMergeTour.isHistoryTour()) {

					// it's again a dummy tour, put photo into current dummy tour

					currentMergeTour.addPhotoTime(photoTime);

				} else {

					// it's a new real tour, setup a new merge tour (real or dummy)

					createMergeTours_FinalizeCurrentMergeTour(currentMergeTour, photoTime);

					currentMergeTour = nextMergeTour;

					isSetupNewTour = true;
				}
			}

			if (isSetupNewTour) {

				realTourEnd = currentMergeTour.tourEndTime;
				isHistoryTour = currentMergeTour.isHistoryTour();

				if (currentMergeTour.isHistoryTour() == false) {

					currentTourData = TourManager.getInstance().getTourData(currentMergeTour.tourId);
					isTourWithGPS = currentTourData.latitudeSerie != null;
				}
			}

			currentMergeTour.tourPhotos.add(photoWrapper);

			currentMergeTour.numberOfPhotos++;

			// set camera
			final Camera camera = getPhotoCamera(photo);
			currentMergeTour.cameras.put(camera.cameraName, camera);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				currentMergeTour.numberOfNoGPSPhotos++;
			} else {
				currentMergeTour.numberOfGPSPhotos++;
			}
		}

		createMergeTours_FinalizeCurrentMergeTour(currentMergeTour, photoTime);
	}

	/**
	 * Keep current merge tour when it contains photos.
	 * 
	 * @param currentMergeTour
	 * @param imageTime
	 */
	private void createMergeTours_FinalizeCurrentMergeTour(final MergeTour currentMergeTour, final long imageTime) {

		// keep only tours which contain photos
		if (currentMergeTour.numberOfPhotos == 0) {
			return;
		}

		// set tour end time
		if (currentMergeTour.isHistoryTour()) {
			currentMergeTour.setTourEndTime(imageTime);
		}

		// sort cameras
		final Collection<Camera> cameraValues = currentMergeTour.cameras.values();
		final Camera[] cameras = cameraValues.toArray(new Camera[cameraValues.size()]);
		Arrays.sort(cameras);
		currentMergeTour.cameraList = cameras;

		_allMergeTours.add(currentMergeTour);
	}

	private MergeTour createMergeTours_GetNextDbTour(final int[] tourIndexStart, final long imageTime) {

		MergeTour newMergeTour = null;

		// loop: all remaining tours from database
		int tourIndex = tourIndexStart[0];
		for (; tourIndex < _allDbTours.size(); tourIndex++) {

			final MergeTour dbTour = _allDbTours.get(tourIndex);

			final long dbTourStart = dbTour.tourStartTime;
			final long dbTourEnd = dbTour.tourEndTime;

			if (imageTime < dbTourStart) {

				// image time is before the next tour start, create dummy tour

				newMergeTour = new MergeTour(imageTime);

				break;
			}

			if (imageTime >= dbTourStart && imageTime <= dbTourEnd) {

				// current tour contains current photo

				newMergeTour = dbTour;

				break;
			}
		}

		// update start index
		tourIndexStart[0] = tourIndex;

		if (newMergeTour == null) {

			// create dummy tour

			newMergeTour = new MergeTour(imageTime);
		}

		return newMergeTour;
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);

		createActions();
		createContextMenu();
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
			_spinnerHours.setMinimum(-999);
			_spinnerHours.setMaximum(999);
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

		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

			}
		});

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

		createUI_99_ContextMenu();
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

	/**
	 * create the views context menu
	 */
	private void createUI_99_ContextMenu() {

		final Table table = (Table) _tourViewer.getControl();

		_columnManager.createHeaderContextMenu(table, null);
	}

	private void defineAllColumns(final Composite parent) {

		defineColumn_TourTypeImage();
		defineColumn_NumberOfPhotos();
		defineColumn_NumberOfGPSPhotos();
		defineColumn_NumberOfNoGPSPhotos();
		defineColumn_TourStartDate();
		defineColumn_DurationTime();
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

				final MergeTour mergeTour = (MergeTour) cell.getElement();

				final Period period = mergeTour.tourPeriod;

				if (period.getHours() == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(period.toString(_durationFormatter));
//					cell.setText(period.toString());
				}
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
				final int numberOfPhotos = mergedTour.numberOfPhotos;

				cell.setText(numberOfPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfPhotos));
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
				final long time = mergedTour.historyEndTime;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _dateFormatter.print(time));
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
				final long time = mergedTour.historyEndTime;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _timeFormatter.print(time));
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
				final long time = mergedTour.historyStartTime;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _dateFormatter.print(time));
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
				final long time = mergedTour.historyStartTime;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _timeFormatter.print(time));
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

					final long tourTypeId = ((MergeTour) element).tourTypeId;

					if (tourTypeId == -1) {

						cell.setImage(null);

					} else {

						final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId);

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

					final long tourTypeId = ((MergeTour) element).tourTypeId;
					if (tourTypeId == -1) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));
					}
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

		final boolean isTourAvailable = _allPhotos.size() > 0;

		_comboCamera.setEnabled(isTourAvailable);
		_spinnerHours.setEnabled(isTourAvailable);
		_spinnerMinutes.setEnabled(isTourAvailable);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

//		menuMgr.add(_actionEditTourWaypoints);
//
//		// add standard group which allows other plug-ins to contribute here
//		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//
//		// set the marker which should be selected in the marker dialog
//		final IStructuredSelection selection = (IStructuredSelection) _wpViewer.getSelection();
//		_actionEditTourWaypoints.setSelectedMarker((TourMarker) selection.getFirstElement());
//
//		/*
//		 * enable actions
//		 */
//		final boolean tourInDb = isTourInDb();
//
//		_actionEditTourWaypoints.setEnabled(tourInDb);
	}

	private void fillToolbar() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	/**
	 * @param photo
	 * @return Returns camera which was used to shot this photo. When a camera is not set in the
	 *         photo, a dummy camera is returned.
	 */
	private Camera getPhotoCamera(final Photo photo) {

		// get camera
		String cameraName = null;
		final PhotoImageMetadata metaData = photo.getImageMetaDataRaw();
		if (metaData != null) {
			cameraName = metaData.model;
		}

		Camera camera = null;

		if (cameraName == null || cameraName.length() == 0) {

			// camera is not set in the photo

			camera = _allCameras.get(CAMERA_UNKNOWN_KEY);

			if (camera == null) {
				camera = new Camera(Messages.Photos_AndTours_Label_NoCamera);
				_allCameras.put(CAMERA_UNKNOWN_KEY, camera);
			}

		} else {

			// camera is set in the photo

			camera = _allCameras.get(cameraName);

			if (camera == null) {
				camera = new Camera(cameraName);
				_allCameras.put(cameraName, camera);
			}
		}

		return camera;
	}

	private Camera getSelectedCamera() {

		final int cameraIndex = _comboCamera.getSelectionIndex();
		if (cameraIndex == -1) {
			return null;
		}

		final Camera[] cameras = _selectedMergeTour.cameraList;
		final Camera camera = cameras[cameraIndex];

		return camera;
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
				+ (" TourStartTime >= " + allDbTourStartDate) //$NON-NLS-1$
				+ (" AND TourEndTime <= " + allDbTourEndDate) //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ UI.NEW_LINE

				+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

		_allDbTours.clear();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {

			conn = TourDatabase.getInstance().getConnection();
			stmt = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(stmt, 1);

			final ResultSet result = stmt.executeQuery();

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
			Util.sqlClose(stmt);
			TourDatabase.closeConnection(conn);
		}

		createMergeTours();

//		System.out.println(UI.timeStampNano() + " \t");
//		System.out.println(UI.timeStampNano() + " \t");
//
//		for (final MergeTour mergeTour : _allMergeTours) {
//			System.out.println(UI.timeStampNano() + " \t" + mergeTour);
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

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

		updateUI(_selectedMergeTour);
	}

	/**
	 */
	private void onSelectTour(final StructuredSelection selection) {

		final Object firstElement = selection.getFirstElement();
		if (firstElement instanceof MergeTour) {

			final MergeTour mergeTour = (MergeTour) firstElement;

			_selectedMergeTour = mergeTour;

			updateUI_Cameras(mergeTour);

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

		final String[] cameraNames = _state.getArray(STATE_CAMERA_ADJUSTMENT_NAME);
		final long[] adjustments = Util.getStateLongArray(_state, STATE_CAMERA_ADJUSTMENT_TIME, null);

		if (cameraNames != null && adjustments != null && cameraNames.length == adjustments.length) {

			// it seems that the values are OK, create cameras with time adjustmens

			for (int index = 0; index < cameraNames.length; index++) {

				final String cameraName = cameraNames[index];

				final Camera camera = new Camera(cameraName);
				camera.timeAdjustment = adjustments[index];

				_allCameras.put(cameraName, camera);
			}
		}
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
		final int size = _allCameras.size();

		final String[] cameras = new String[size];
		final long[] adjustment = new long[size];

		int index = 0;
		for (final Camera camera : _allCameras.values()) {
			cameras[index] = camera.cameraName;
			adjustment[index] = camera.timeAdjustment;
			index++;
		}
		_state.put(STATE_CAMERA_ADJUSTMENT_NAME, cameras);
		Util.setState(_state, STATE_CAMERA_ADJUSTMENT_TIME, adjustment);

		_columnManager.saveState(_state);
	}

	private void selectTour(final MergeTour mergeTour) {

		MergeTour selectedTour;
		if (mergeTour == null || mergeTour.isHistoryTour()) {
			// select first tour
			selectedTour = _allMergeTours.get(0);
		} else {
			// select tour by tour id
			selectedTour = mergeTour;
		}
		_tourViewer.setSelection(new StructuredSelection(selectedTour), true);

		// check if tour is selected
		final ISelection newSelection = _tourViewer.getSelection();
		if (newSelection.isEmpty()) {

			// first selection failed, select first tour
			selectedTour = _allMergeTours.get(0);
			_tourViewer.setSelection(new StructuredSelection(selectedTour), true);
		}
	}

	@Override
	public void setFocus() {
		_tourViewer.getTable().setFocus();
	}

	private void setPhotoTimeAdjustment() {

		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final Photo photo = photoWrapper.photo;

			final Camera camera = getPhotoCamera(photo);

			long adjustedTime = photoWrapper.imageSortingTime;

			final long timeAdjustment = camera.timeAdjustment;
			if (timeAdjustment != 0) {
				adjustedTime += timeAdjustment;
			}

			photoWrapper.adjustedTime = adjustedTime;
		}

		Collections.sort(_allPhotos, _adjustTimeComparator);
	}

	void updatePhotosAndTours(final MergePhotoTourSelection photoMergeSelection) {

		final ArrayList<PhotoWrapper> tourPhotos = photoMergeSelection.selectedPhotos;

		if (tourPhotos.size() == 0) {
			clearView();
			return;
		}

		_allPhotos.clear();
		_allPhotos.addAll(tourPhotos);

		updateUI(null);
	}

	private void updateUI(final MergeTour mergeTour) {

		if (_allPhotos.size() == 0) {
			// view is not fully initialized, this happend in the pref listener
			return;
		}

		setPhotoTimeAdjustment();

		/*
		 * get date for 1st and last photo
		 */
		long allDbTourStartDate = _allPhotos.get(0).adjustedTime;
		long allDbTourEndDate = allDbTourStartDate;

		for (final PhotoWrapper photoWrapper : _allPhotos) {

			final long imageSortingTime = photoWrapper.adjustedTime;

			if (imageSortingTime < allDbTourStartDate) {
				allDbTourStartDate = imageSortingTime;
			} else if (imageSortingTime > allDbTourEndDate) {
				allDbTourEndDate = imageSortingTime;
			}
		}

		_allPhotoStartDate = new DateTime(allDbTourStartDate);

		loadToursFromDb(allDbTourStartDate, allDbTourEndDate);

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
	 */
	private void updateUI_Cameras(final MergeTour mergeTour) {

		// get previous camera
		String currentSelectedCamera = null;
		final int currentSelectedCameraIndex = _comboCamera.getSelectionIndex();
		if (currentSelectedCameraIndex != -1) {
			currentSelectedCamera = _comboCamera.getItem(currentSelectedCameraIndex);
		}

		_comboCamera.removeAll();


		final Camera[] cameraList = mergeTour.cameraList;
		int cameraComboIndex = -1;

		for (int cameraIndex = 0; cameraIndex < cameraList.length; cameraIndex++) {

			final Camera camera = cameraList[cameraIndex];
			final Camera existingCamera = _allCameras.get(camera.cameraName);

			if (existingCamera != null && camera.cameraName.equals(existingCamera.cameraName)) {
				_comboCamera.add(existingCamera.cameraName);
			} else {
				_comboCamera.add(camera.cameraName);
			}

			// get index for the last selected camera
			if (cameraComboIndex == -1
					&& currentSelectedCamera != null
					&& currentSelectedCamera.equals(camera.cameraName)) {
				cameraComboIndex = cameraIndex;
			}
		}

		_comboCamera.getParent().layout();

		// select previous camera
		_comboCamera.select(cameraComboIndex == -1 ? 0 : cameraComboIndex);

		// update camera time adjustment spinner
		onSelectCamera();
	}
}
