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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
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

	public static final String		ID					= "net.tourbook.photo.merge.PhotosAndToursView.ID";		//$NON-NLS-1$

	public static final String		IMAGE_PIC_DIR_VIEW	= "IMAGE_PIC_DIR_VIEW";									//$NON-NLS-1$

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings	_state				= TourbookPlugin.getDefault().getDialogSettingsSection(ID);

	private ArrayList<MergeTour>	_allMergeTours		= new ArrayList<MergeTour>();
	private ArrayList<PhotoWrapper>	_allPhotos			= new ArrayList<PhotoWrapper>();

	private PostSelectionProvider	_postSelectionProvider;

	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private IPartListener2			_partListener;
	private PixelConverter			_pc;

	private ActionModifyColumns		_actionModifyColumns;

	private ColumnManager			_columnManager;

	/*
	 * measurement unit values
	 */
	private float					_unitValueAltitude;

	private final PeriodFormatter	_durationFormatter	= new PeriodFormatterBuilder()
																.appendYears()
																.appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
																.appendDays()
																.appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
																.appendHours()
																.appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
																.toFormatter();

	private final DateTimeFormatter	_dateFormatter		= DateTimeFormat.shortDate();
	private final DateTimeFormatter	_timeFormatter		= DateTimeFormat.mediumTime();

	private final NumberFormat		_nf_1_1				= NumberFormat.getNumberInstance();
	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;

	private Composite				_pageNoTour;
	private Composite				_pageNoImage;
	private Composite				_pageViewer;

	private Composite				_viewerContainer;
	private TableViewer				_tourViewer;

//	private Label					_labelPhotoDates;

	private static class Comparator extends ViewerComparator {

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

					// measurement system has changed

					UI.updateUnits();
					updateInternalUnitValues();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

				} else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					updateUI(_allPhotos);

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

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		updateInternalUnitValues();

		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);

		createActions();
		createContextMenu();
		fillToolbar();

		addSelectionListener();
		addPrefListener();
		addPartListener();

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
				createUI_10_Tours(_pageViewer);
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

	private void createUI_10_Tours(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_viewerContainer);
		{
//			createUI_40_Header(_viewerContainer);
			createUI_50_TourViewer(_viewerContainer);
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

		_tourViewer.setContentProvider(new ContentProvider());
		_tourViewer.setComparator(new Comparator());

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
				final long time = mergedTour.tourEndTime;

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
				final long time = mergedTour.tourEndTime;

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
				final long time = mergedTour.tourStartTime;

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
				final long time = mergedTour.tourStartTime;

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

						final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourTypeId);

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
						cell.setText(UI.getInstance().getTourTypeLabel(tourTypeId));
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

	private MergeTour getNextMergeTour(	final ArrayList<MergeTour> allMergeTours,
										final int[] tourIndexStart,
										final long imageTime) {

		MergeTour newMergeTour = null;

		// loop: all remaining tours
		int tourIndex = tourIndexStart[0];
		for (; tourIndex < allMergeTours.size(); tourIndex++) {

			final MergeTour mergeTour = allMergeTours.get(tourIndex);

			final long tourStart = mergeTour.tourStartTime;
			final long tourEnd = mergeTour.tourEndTime;

			if (imageTime < tourStart) {

				// image time is before the next tour start, create dummy tour

				newMergeTour = new MergeTour(null);
				newMergeTour.setTourStartTime(imageTime);

				break;
			}

			if (imageTime >= tourStart && imageTime <= tourEnd) {

				// current tour contains current photo

				newMergeTour = mergeTour;

				break;
			}
		}

		// update start index
		tourIndexStart[0] = tourIndex;

		if (newMergeTour == null) {

			// create dummy tour

			newMergeTour = new MergeTour(null);
			newMergeTour.setTourStartTime(imageTime);
		}

		return newMergeTour;
	}

	public ArrayList<TourData> getSelectedTours() {
//		return _tourList;
		return new ArrayList<TourData>();
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void loadToursFromDb(	final ArrayList<PhotoWrapper> selectedPhotos,
									final long photoStartDate,
									final long photoEndDate) {

		BusyIndicator.showWhile(_pageBook.getDisplay(), new Runnable() {
			public void run() {
				loadToursFromDb_10_Runnable(selectedPhotos, photoStartDate, photoEndDate);
			}
		});
	}

	private void loadToursFromDb_10_Runnable(	final ArrayList<PhotoWrapper> selectedPhotos,
											final long photoStartDate,
											final long photoEndDate) {

		final DateTime photoStart = new DateTime(photoStartDate).minusDays(1);
		final DateTime photoEnd = new DateTime(photoEndDate).plusDays(1);

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
				+ (" TourStartTime >= " + photoStart.getMillis()) //$NON-NLS-1$
				+ (" AND TourEndTime <= " + photoEnd.getMillis()) //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ UI.NEW_LINE

				+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

		final ArrayList<MergeTour> allMergeTours = new ArrayList<MergeTour>();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {

			conn = TourDatabase.getInstance().getConnection();
			stmt = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(stmt, 1);

			final ResultSet result = stmt.executeQuery();

			while (result.next()) {

				final MergeTour mergeTour = new MergeTour();

				mergeTour.tourId = result.getLong(1);

				mergeTour.setTourStartTime(result.getLong(2));
				mergeTour.setTourEndTime(result.getLong(3));

				final Object dbTourTypeId = result.getObject(4);
				mergeTour.tourTypeId = (dbTourTypeId == null ? //
						TourDatabase.ENTITY_IS_NOT_SAVED
						: (Long) dbTourTypeId);

				allMergeTours.add(mergeTour);
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.sqlClose(stmt);
			TourDatabase.closeConnection(conn);
		}

		/*
		 * create pseudo tours for photos which are not contained in a tour and remove all tours
		 * which do not contain any photos
		 */
		MergeTour currentMergeTour = null;
		long mergeTourEnd;
		boolean isDummyTour = false;

		if (allMergeTours.size() > 0) {

			// real tours are available

			final MergeTour firstTour = allMergeTours.get(0);

			if (photoStart.isBefore(firstTour.tourStartTime)) {

				// first photo is before the first tour, create dummy tour

				currentMergeTour = new MergeTour(null);
				currentMergeTour.setTourStartTime(photoStartDate);

			} else {

				// first tour starts before the first photo

				currentMergeTour = firstTour;
			}
		} else {

			// there are no tours, create dummy tour

			currentMergeTour = new MergeTour(null);
			currentMergeTour.setTourStartTime(photoStartDate);
		}

		mergeTourEnd = currentMergeTour.tourEndTime;
		isDummyTour = currentMergeTour.isDummyTour;
		long imageTime = 0;

		_allMergeTours.clear();
		final int tourIndexStart[] = new int[] { 0 };

		// loop: all photos
		for (final PhotoWrapper photoWrapper : selectedPhotos) {

			imageTime = photoWrapper.imageSortingTime;

			// first check real tours
			if (!isDummyTour) {

				// current merge tour is a real tour

				if (imageTime <= mergeTourEnd) {

					// current tour contains current image

				} else {

					// find/create tour for the current image

					loadToursFromDb_20_KeepMergeTour(currentMergeTour, imageTime);

					currentMergeTour = getNextMergeTour(allMergeTours, tourIndexStart, imageTime);

					mergeTourEnd = currentMergeTour.tourEndTime;
					isDummyTour = currentMergeTour.isDummyTour;
				}

			} else {

				// current merge tour is a dummy tour

				final MergeTour nextMergeTour = getNextMergeTour(allMergeTours, tourIndexStart, imageTime);

				if (nextMergeTour.isDummyTour) {

					// it's again a dummy tour, keep current dummy tour

				} else {

					// it's a new real tour, setup new real merge tour

					loadToursFromDb_20_KeepMergeTour(currentMergeTour, imageTime);

					currentMergeTour = nextMergeTour;

					mergeTourEnd = currentMergeTour.tourEndTime;
					isDummyTour = currentMergeTour.isDummyTour;
				}
			}

			currentMergeTour.tourPhotos.add(photoWrapper);

			currentMergeTour.numberOfPhotos++;

			// set number of GPS/No GPS photos
			final double latitude = photoWrapper.photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				currentMergeTour.numberOfNoGPSPhotos++;
			} else {
				currentMergeTour.numberOfGPSPhotos++;
			}
		}

		loadToursFromDb_20_KeepMergeTour(currentMergeTour, imageTime);
	}

	/**
	 * Keep current merge tour when it contains photos.
	 * 
	 * @param currentMergeTour
	 * @param imageTime
	 */
	private void loadToursFromDb_20_KeepMergeTour(final MergeTour currentMergeTour, final long imageTime) {

		if (currentMergeTour.numberOfPhotos > 0) {

			// tour contains photos

			if (currentMergeTour.isDummyTour) {
				currentMergeTour.setTourEndTime(imageTime);
			}

			_allMergeTours.add(currentMergeTour);
		}
	}

	private void onSelectionChanged(final ISelection selection) {

	}

	/**
	 * Fire selected tour
	 */
	private void onSelectTour(final StructuredSelection selection) {

		final Object firstElement = selection.getFirstElement();
		if (firstElement instanceof MergeTour) {

			final MergeTour mergeTour = (MergeTour) firstElement;
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

	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {
		_tourViewer.getTable().setFocus();
	}

	private void updateInternalUnitValues() {

		_unitValueAltitude = UI.UNIT_VALUE_ALTITUDE;
	}

	void updatePhotosAndTours(final MergePhotoTourSelection photoMergeSelection) {
		updateUI(photoMergeSelection.selectedPhotos);
	}

	private void updateUI(final ArrayList<PhotoWrapper> selectedPhotos) {

		if (selectedPhotos.size() == 0) {
			clearView();
			return;
		}

		_allPhotos = selectedPhotos;

		/*
		 * get photo start/end date
		 */
		long photoStartDate = selectedPhotos.get(0).imageSortingTime;
		long photoEndDate = photoStartDate;

		for (final PhotoWrapper photoWrapper : selectedPhotos) {

			final long imageSortingTime = photoWrapper.imageSortingTime;

			if (imageSortingTime < photoStartDate) {
				photoStartDate = imageSortingTime;
			} else if (imageSortingTime > photoEndDate) {
				photoEndDate = imageSortingTime;
			}
		}

		loadToursFromDb(selectedPhotos, photoStartDate, photoEndDate);

		_tourViewer.setInput(new Object[0]);

		_pageBook.showPage(_pageViewer);

		// select first tour
		if (_allMergeTours.size() > 0) {
			_tourViewer.setSelection(new StructuredSelection(_allMergeTours.get(0)), true);
		}
	}
}
