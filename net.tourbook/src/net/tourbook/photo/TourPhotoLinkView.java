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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionModifyColumns;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TourPhotoLinkView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String						ID									= "net.tourbook.photo.PhotosAndToursView.ID";	//$NON-NLS-1$

	private static final String						STATE_FILTER_NO_TOURS				= "STATE_FILTER_NO_TOURS";						//$NON-NLS-1$
	private static final String						STATE_FILTER_PHOTOS					= "STATE_FILTER_PHOTOS";						//$NON-NLS-1$
	private static final String						STATE_SELECTED_CAMERA_NAME			= "STATE_SELECTED_CAMERA_NAME";				//$NON-NLS-1$
	private static final String						STATE_TIME_ADJUSTMENT_TOURS			= "STATE_TIME_ADJUSTMENT_TOURS";				//$NON-NLS-1$

	public static final String						IMAGE_PIC_DIR_VIEW					= "IMAGE_PIC_DIR_VIEW";						//$NON-NLS-1$
	public static final String						IMAGE_PHOTO_PHOTO					= "IMAGE_PHOTO_PHOTO";							//$NON-NLS-1$

	private static final String						TEMP_FILE_PREFIX_ORIG				= "_orig_";									//$NON-NLS-1$

	private final IPreferenceStore					_prefStore							= TourbookPlugin
																								.getDefault()
																								.getPreferenceStore();

	private final IDialogSettings					_state								= TourbookPlugin
																								.getDefault()
																								.getDialogSettingsSection(
																										ID);

	private static final PhotoManager				_photoMgr							= PhotoManager.getInstance();

	private ArrayList<TourPhotoLink>				_allTourPhotoLinks					= new ArrayList<TourPhotoLink>();

	private ArrayList<PhotoWrapper>					_allPhotos							= new ArrayList<PhotoWrapper>();

	/**
	 * Contains all cameras which are used in all displayed tours.
	 */
	private HashMap<String, Camera>					_allTourCameras						= new HashMap<String, Camera>();

	/**
	 * All cameras sorted by camera name
	 */
	private Camera[]								_allTourCamerasSorted;

	/**
	 * Tour photo link which is currently selected in the tour viewer.
	 */
	private ArrayList<TourPhotoLink>				_selectedLinks						= new ArrayList<TourPhotoLink>();

	/**
	 * Contains only tour photo links with real tours and which contain geo positions.
	 */
	private List<TourPhotoLink>						_selectedTourPhotoLinksWithGps		= new ArrayList<TourPhotoLink>();

	private TourPhotoLinkSelection					_tourPhotoLinkSelection;

	private ISelectionListener						_postSelectionListener;
	private IPropertyChangeListener					_prefChangeListener;
	private IPartListener2							_partListener;

	private PixelConverter							_pc;
	private ColumnManager							_columnManager;

	private ActionFilterPhotos						_actionFilterPhotos;
	private ActionFilterNoTours						_actionFilterNoTours;
	private ActionModifyColumns						_actionModifyColumns;
//	private ActionResetTimeAdjustment				_actionResetTimeAdjustment;
	private ActionSetTourGPSIntoPhotos				_actionSetTourGPSIntoPhotos;

//	private Connection								_sqlConnection;
//	private PreparedStatement						_sqlStatement;

	private final PeriodFormatter					_durationFormatter					= new PeriodFormatterBuilder()
																								.appendYears()
																								.appendSuffix(
																										"y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
																								.appendMonths()
																								.appendSuffix(
																										"m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
																								.appendDays()
																								.appendSuffix(
																										"d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
																								.appendHours()
																								.appendSuffix(
																										"h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
																								.toFormatter();

	private final DateTimeFormatter					_dateFormatter						= DateTimeFormat.shortDate();
	private final DateTimeFormatter					_timeFormatter						= DateTimeFormat.mediumTime();
	private final NumberFormat						_nf_1_1								= NumberFormat
																								.getNumberInstance();
	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	private final Comparator<? super PhotoWrapper>	_adjustTimeComparator;

	/**
	 * When <code>true</code>, only tours with photos are displayed.
	 */
	private boolean									_isShowToursOnlyWithPhotos			= true;
	private boolean									_isFilterNoTours;

	private boolean									_isOverwritePhotoGPS				= true;
	private boolean									_isAddPhotosToExistingTourPhotos	= false;

	private ICommandService							_commandService;

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

	private Button									_rdoAdjustAllTours;
	private Button									_rdoAdjustSelectedTours;

	{
		_adjustTimeComparator = new Comparator<PhotoWrapper>() {

			@Override
			public int compare(final PhotoWrapper wrapper1, final PhotoWrapper wrapper2) {

				final long diff = wrapper1.adjustedTime - wrapper2.adjustedTime;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;
			}
		};
	}

//	private class ActionResetTimeAdjustment extends Action {
//
//		public ActionResetTimeAdjustment() {
//
//			setText(Messages.Action_PhotosAndTours_ResetTimeAdjustment);
//		}
//
//		@Override
//		public void run() {
//			actionResetTimeAdjustment();
//		}
//
//	}

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

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _allTourPhotoLinks.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public TourPhotoLinkView() {
		super();
	}

	void actionFilterNoTours() {

		_isFilterNoTours = _actionFilterNoTours.isChecked();

		updateUI(_selectedLinks, false, false);

		enableControls();
	}

	void actionFilterPhotos() {

		_isShowToursOnlyWithPhotos = _actionFilterPhotos.isChecked();

		updateUI(_selectedLinks, false, false);
	}

//	private void actionResetTimeAdjustment() {
//
//		for (final TourPhotoLink tourPhotoLink : _selectedLinks) {
//			for (final PhotoWrapper photoWrapper : tourPhotoLink.tourPhotos) {
//				photoWrapper.isGpsSetFromTour = false;
//			}
//		}
//
//		updateUI(_selectedLinks, false, false);
//	}

	void actionSetTourGPSIntoPhotos() {

		if (TourManager.isTourEditorModified()) {
			return;
		}

		final ArrayList<PhotoWrapper> updatedPhotos = new ArrayList<PhotoWrapper>();

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				PhotoManager.getInstance().setTourGpsIntoPhotos(updatedPhotos, _selectedTourPhotoLinksWithGps);

				// update UI with new number of GPS photos
				for (final TourPhotoLink tourPhotoLink : _selectedTourPhotoLinksWithGps) {
					_tourViewer.update(tourPhotoLink, null);
				}
			}
		});

		// fire update event
		PhotoManager.fireEvent(PhotoEventId.GPS_DATA_IS_UPDATED, updatedPhotos);

		// force that selection is fired
		_selectedLinks.clear();

		onSelectTour(((StructuredSelection) _tourViewer.getSelection()).toArray(), true);
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotoLinkView.this) {
					onPartActivate();
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotoLinkView.this) {
					onPartClosed();
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

					// sql filter is dirty, force reloading cached start/end
					_photoMgr.resetTourStartEnd();

					updateUI(_selectedLinks, true, false);

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
				if (part == TourPhotoLinkView.this) {
					return;
				}
				onSelectionChanged(selection, part);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_allTourPhotoLinks.clear();
		_allPhotos.clear();
		_selectedLinks.clear();
		_selectedTourPhotoLinksWithGps.clear();
		_tourPhotoLinkSelection = null;

		_tourViewer.setInput(new Object[0]);

		_pageBook.showPage(_pageNoImage);
	}

	private void createActions() {

		_actionFilterNoTours = new ActionFilterNoTours(this);
		_actionFilterPhotos = new ActionFilterPhotos(this);
		_actionModifyColumns = new ActionModifyColumns(this);
//		_actionResetTimeAdjustment = new ActionResetTimeAdjustment();
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

		_commandService = ((ICommandService) PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getService(ICommandService.class));

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
				.numColumns(2)
				.margins(2, 2)
				.applyTo(container);
		{
			/*
			 * label: adjust time
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Photos_AndTours_Label_AdjustTime);
			label.setToolTipText(Messages.Photos_AndTours_Label_AdjustTime_Tooltip);

			/*
			 * radio: all/selected tours
			 */
			final Composite containerTours = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(containerTours);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTours);
			{
				_rdoAdjustAllTours = new Button(containerTours, SWT.RADIO);
				_rdoAdjustAllTours.setText(Messages.Photos_AndTours_Radio_AdjustTime_AllTours);
				_rdoAdjustAllTours.setToolTipText(Messages.Photos_AndTours_Radio_AdjustTime_AllTours_Tooltip);

				_rdoAdjustSelectedTours = new Button(containerTours, SWT.RADIO);
				_rdoAdjustSelectedTours.setText(Messages.Photos_AndTours_Radio_AdjustTime_SelectedTours);
				_rdoAdjustSelectedTours.setToolTipText(Messages.Photos_AndTours_Radio_AdjustTime_SelectedTours_Tooltip);
			}

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
				final ISelection eventSelection = event.getSelection();
				if (eventSelection instanceof StructuredSelection) {
					onSelectTour(((StructuredSelection) eventSelection).toArray(), true);
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

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();

				final Period period = link.tourPeriod;

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

				setBgColor(cell, link);
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
	private void defineColumn_NumberOfNoGPSPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.NUMBER_OF_NO_GPS_PHOTOS.createColumn(_columnManager, _pc);
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
	 * column: number of photos
	 */
	private void defineColumn_NumberOfPhotos() {

		final ColumnDefinition colDef = TableColumnFactory.NUMBER_OF_PHOTOS.createColumn(_columnManager, _pc);
//		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final int numberOfPhotos = link.tourPhotos.size();

				cell.setText(numberOfPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfPhotos));

				setBgColor(cell, link);
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
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;

					cell.setText(link.tourCameras);

					setBgColor(cell, link);
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

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE ? _dateFormatter.print(link.tourEndTime) : _dateFormatter
						.print(historyTime));

				setBgColor(cell, link);
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

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyEndTime;

				cell.setText(historyTime == Long.MIN_VALUE ? _timeFormatter.print(link.tourEndTime) : _timeFormatter
						.print(historyTime));

				setBgColor(cell, link);
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

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE ? _dateFormatter.print(link.tourStartTime) : _dateFormatter
						.print(historyTime));

				setBgColor(cell, link);
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

				final TourPhotoLink link = (TourPhotoLink) cell.getElement();
				final long historyTime = link.historyStartTime;

				cell.setText(historyTime == Long.MIN_VALUE ? _timeFormatter.print(link.tourStartTime) : _timeFormatter
						.print(historyTime));

				setBgColor(cell, link);
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
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;

					if (link.isHistoryTour) {

						cell.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PHOTO_PHOTO));

					} else {

						final long tourTypeId = link.tourTypeId;
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
				if (element instanceof TourPhotoLink) {

					final TourPhotoLink link = (TourPhotoLink) element;
					final long tourTypeId = link.tourTypeId;
					if (tourTypeId == -1) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));
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
		final boolean isPhotoFilter = _actionFilterNoTours.isChecked() == false;
		final boolean isTourWithGPS = _selectedTourPhotoLinksWithGps.size() > 0;

		_comboCamera.setEnabled(isPhotoAvailable);
		_spinnerHours.setEnabled(isPhotoAvailable);
		_spinnerMinutes.setEnabled(isPhotoAvailable);
		_spinnerSeconds.setEnabled(isPhotoAvailable);

		_actionFilterNoTours.setEnabled(isPhotoAvailable);
		_actionFilterPhotos.setEnabled(isPhotoAvailable && isPhotoFilter);

//		_actionResetTimeAdjustment.setEnabled(isTourWithGPS);

		// is true when selected tour contains photos
		_actionSetTourGPSIntoPhotos.setEnabled(isTourWithGPS

		// it's too dangerous when all photos are contained in 1 tour
//				&& _isFilterNoTours == false
				);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionSetTourGPSIntoPhotos);
//		menuMgr.add(_actionResetTimeAdjustment);
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

	private void onPartActivate() {

		// fire selection
		if (_tourPhotoLinkSelection != null) {
			PhotoManager.fireEvent(PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
		}
	}

	private void onPartClosed() {

		// close sql connections
		_photoMgr.resetTourStartEnd();

		saveState();
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

				updatePhotosAndTours(((PhotoSelection) originalSelection).photoWrappers, true);

//				final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//
//				// ensure link view is opened
//				for (final IViewReference viewRef : activePage.getViewReferences()) {
//					if (viewRef.getId().equals(TourPhotoLinkView.ID)) {
//						final IViewPart viewPart = viewRef.getView(false);
//						if (viewPart == null) {
//							Util.showViewNotActive(TourPhotoLinkView.ID);
//						}
//					}
//				}
//
//				if (_tourPhotoLinkSelection != null) {
//
//					_pageBook.getDisplay().asyncExec(new Runnable() {
//						public void run() {
//							PhotoManager.fireEvent(PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
//						}
//					});
//				}
			}

		} else if (selection instanceof PhotoSelection && part instanceof PicDirView) {

			/**
			 * accept photo selection ONLY from the pic dir view, otherwise other photo selections
			 * will cause a view update
			 */

			final PhotoSelection photoSelection = (PhotoSelection) selection;

			final Command command = _commandService.getCommand(ActionHandlerSyncPhotoWithTour.COMMAND_ID);
			final State state = command.getState(RegistryToggleState.STATE_ID);
			final boolean isSync = (Boolean) state.getValue();

			if (isSync) {
				updatePhotosAndTours(photoSelection.photoWrappers, true);
			}
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

		updateUI(_selectedLinks, false, false);
	}

	/**
	 * Creates a {@link TourPhotoLinkSelection}
	 * 
	 * @param allSelectedLinks
	 *            All elements of type {@link TourPhotoLink}
	 * @param isFireSelection
	 */
	private void onSelectTour(final Object[] allSelectedLinks, final boolean isFireSelection) {

		// get all real tours with geo positions
		_selectedTourPhotoLinksWithGps.clear();

		// contains tour id's for all real tours
		final ArrayList<Long> selectedTourIds = new ArrayList<Long>();

		final ArrayList<TourPhotoLink> selectedLinks = new ArrayList<TourPhotoLink>();

		for (final Object linkElement : allSelectedLinks) {

			if (linkElement instanceof TourPhotoLink) {

				final TourPhotoLink selectedLink = (TourPhotoLink) linkElement;

				selectedLinks.add(selectedLink);

				final boolean isRealTour = selectedLink.tourId != Long.MIN_VALUE;

				if (isRealTour) {
					selectedTourIds.add(selectedLink.tourId);
				}

				if (isRealTour && selectedLink.tourPhotos.size() > 0) {

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

		if (isFireSelection) {
			PhotoManager.fireEvent(PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
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
		 * time adjustment all/selected tours
		 */
		final boolean isAllTours = Util.getStateBoolean(_state, STATE_TIME_ADJUSTMENT_TOURS, true);
		_rdoAdjustAllTours.setSelection(isAllTours);
		_rdoAdjustSelectedTours.setSelection(isAllTours == false);

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
		 * time adjustment all/selected tours
		 */
		_state.put(STATE_TIME_ADJUSTMENT_TOURS, _rdoAdjustAllTours.getSelection());

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

	/**
	 * @param prevTourPhotoLink
	 *            Previously selected link, can be <code>null</code>.
	 */
	private void selectTour(final TourPhotoLink prevTourPhotoLink) {

		TourPhotoLink selectedTour = null;

		/*
		 * 1st try to select a tour
		 */
		if (prevTourPhotoLink == null) {

			// select first tour
			selectedTour = _allTourPhotoLinks.get(0);

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

			final ArrayList<PhotoWrapper> tourPhotos = prevTourPhotoLink.tourPhotos;
			if (tourPhotos.size() > 0) {

				// get tour for the first photo

				final long tourPhotoTime = tourPhotos.get(0).adjustedTime;

				for (final TourPhotoLink link : _allTourPhotoLinks) {

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

				for (final TourPhotoLink link : _allTourPhotoLinks) {

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
			final TourPhotoLink firstTour = _allTourPhotoLinks.get(0);

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

		if (originalJpegImageFile.canWrite() == false) {

			if (isReadOnlyMessageDisplayed[0] == false) {

				isReadOnlyMessageDisplayed[0] = true;

				MessageDialog.openError(_pageBook.getShell(), //
						Messages.Photos_AndTours_Dialog_ImageIsReadOnly_Title,
						NLS.bind(
								Messages.Photos_AndTours_Dialog_ImageIsReadOnly_Message,
								originalJpegImageFile.getAbsolutePath()));
			}

			return 0;
		}

		File gpsTempFile = null;

		final IPath originalFilePathName = new Path(originalJpegImageFile.getAbsolutePath());
		final String originalFileNameWithoutExt = originalFilePathName.removeFileExtension().lastSegment();

		final File originalFilePath = originalFilePathName.removeLastSegments(1).toFile();
		File renamedOriginalFile = null;

		try {

			boolean returnState = false;

			try {

				gpsTempFile = File.createTempFile(//
						originalFileNameWithoutExt + UI.SYMBOL_UNDERSCORE,
						UI.SYMBOL_DOT + originalFilePathName.getFileExtension(),
						originalFilePath);

				setExifGPSTag_IntoImageFile_WithExifRewriter(originalJpegImageFile, gpsTempFile, latitude, longitude);

				returnState = true;

			} catch (final ImageReadException e) {
				StatusUtil.log(e);
			} catch (final ImageWriteException e) {
				StatusUtil.log(e);
			} catch (final IOException e) {
				StatusUtil.log(e);
			}

			if (returnState == false) {
				return -1;
			}

			/*
			 * replace original file with gps file
			 */

			try {

				/*
				 * rename original file into a temp file
				 */
				final String nanoString = Long.toString(System.nanoTime());
				final String nanoTime = nanoString.substring(nanoString.length() - 4);

				renamedOriginalFile = File.createTempFile(//
						originalFileNameWithoutExt + TEMP_FILE_PREFIX_ORIG + nanoTime,
						UI.SYMBOL_DOT + originalFilePathName.getFileExtension(),
						originalFilePath);

				final String renamedOriginalFileName = renamedOriginalFile.getAbsolutePath();

				Util.deleteTempFile(renamedOriginalFile);

				boolean isRenamed = originalJpegImageFile.renameTo(new File(renamedOriginalFileName));

				if (isRenamed == false) {

					// original file cannot be renamed
					MessageDialog.openError(_pageBook.getShell(), //
							Messages.Photos_AndTours_ErrorDialog_Title,
							NLS.bind(
									Messages.Photos_AndTours_ErrorDialog_OriginalImageFileCannotBeRenamed,
									originalFilePathName.toOSString(),
									renamedOriginalFileName));
					return -1;
				}

				/*
				 * rename gps temp file into original file
				 */
				isRenamed = gpsTempFile.renameTo(originalFilePathName.toFile());

				if (isRenamed == false) {

					// gps file cannot be renamed to original file
					MessageDialog.openError(_pageBook.getShell(), //
							Messages.Photos_AndTours_ErrorDialog_Title,
							NLS.bind(
									Messages.Photos_AndTours_ErrorDialog_SeriousProblemRenamingOriginalImageFile,
									originalFilePathName.toOSString(),
									renamedOriginalFile.getAbsolutePath()));

					/*
					 * prevent of deleting renamed original file because the original file is
					 * renamed into this
					 */
					renamedOriginalFile = null;

					return -1;
				}

				if (renamedOriginalFile.delete() == false) {

					MessageDialog.openError(_pageBook.getShell(), //
							Messages.Photos_AndTours_ErrorDialog_Title,
							NLS.bind(
									Messages.Photos_AndTours_ErrorDialog_RenamedOriginalFileCannotBeDeleted,
									originalFilePathName.toOSString(),
									renamedOriginalFile.getAbsolutePath()));
				}

			} catch (final IOException e) {
				StatusUtil.log(e);
			}

		} finally {

			Util.deleteTempFile(gpsTempFile);
		}

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

	@Override
	public void setFocus() {
		_tourViewer.getTable().setFocus();
	}

	private void setPhotoTimeAdjustment() {

		for (final PhotoWrapper photoWrapper : _allPhotos) {

//			if (photoWrapper.isGpsSetFromTour) {
//
//				// don't overwrite adjusted time when it's already used set geo position
//
//				continue;
//			}

			final long utcTime = photoWrapper.imageExifTime;
			final long cameraTimeAdjustment = photoWrapper.camera.timeAdjustment;

			photoWrapper.adjustedTime = utcTime + cameraTimeAdjustment;

			// force that the position are updated
			photoWrapper.photo.resetWorldPosition();
		}

		Collections.sort(_allPhotos, _adjustTimeComparator);
	}

	/**
	 * @param tourPhotos
	 * @param isSelectAll
	 *            When <code>true</code>, all links will be selected in the link viewer.
	 */
	void updatePhotosAndTours(final ArrayList<PhotoWrapper> tourPhotos, final boolean isSelectAll) {

		if (tourPhotos.size() == 0) {
			clearView();
			return;
		}

		_allPhotos.clear();
		_allPhotos.addAll(tourPhotos);

		_allTourCameras.clear();

		// ensure camera is set in all photos
		for (final PhotoWrapper photoWrapper : _allPhotos) {
			if (photoWrapper.camera == null) {
				_photoMgr.setCamera(photoWrapper.photo, _allTourCameras);
			}
		}

		updateUI(null, true, isSelectAll);
	}

	private void updateUI(	final ArrayList<TourPhotoLink> tourPhotoLinks,
							final boolean isLoadToursFromDb,
							final boolean isSelectAll) {

		if (_allPhotos.size() == 0) {
			// view is not fully initialized, this happend in the pref listener
			return;
		}

		// get previous selected tour
		TourPhotoLink prevTourPhotoLink = null;
		if (tourPhotoLinks != null && tourPhotoLinks.size() > 0) {
			prevTourPhotoLink = tourPhotoLinks.get(0);
		}

		// this must be called BEFORE start/end date are set
		setPhotoTimeAdjustment();

		/*
		 * get tours from db when start/end has changed
		 */
		boolean isLoaded = false;

		if (isLoadToursFromDb) {
			isLoaded = _photoMgr.loadToursFromDb(_allPhotos);
		}

		if (isLoaded == false) {

			// reset data for the 'old' links

			for (final TourPhotoLink tourPhotoLink : _photoMgr.getAllDbTourPhotoLinks()) {

				tourPhotoLink.tourPhotos.clear();

				tourPhotoLink.numberOfGPSPhotos = 0;
				tourPhotoLink.numberOfNoGPSPhotos = 0;

				tourPhotoLink.tourCameras = UI.EMPTY_STRING;
			}
		}

		_allTourPhotoLinks.clear();
		_selectedLinks.clear();
		_selectedTourPhotoLinksWithGps.clear();

		if (_isFilterNoTours) {
			_photoMgr.createTourPhotoLinks_90_FilterNoTours(_allPhotos, _allTourPhotoLinks, _allTourCameras);
		} else {
			_photoMgr.createTourPhotoLinks(_allPhotos, _allTourPhotoLinks, _isShowToursOnlyWithPhotos, _allTourCameras);
		}

		updateUI_Cameras(null);

		_tourViewer.setInput(new Object[0]);

		enableControls();

		_pageBook.showPage(_pageViewer);

		if (_allTourPhotoLinks.size() == 0) {
			return;
		}

		if (isSelectAll) {

			_tourViewer.setSelection(new StructuredSelection(_allTourPhotoLinks), false);

		} else {

			selectTour(prevTourPhotoLink);
		}
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
