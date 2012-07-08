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
package net.tourbook.photo.merge;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.Messages;
import net.tourbook.photo.manager.PhotoWrapper;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PhotoMergeTourView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String		ID				= "net.tourbook.photo.merge.PhotoMergeTourView.ID";		//$NON-NLS-1$

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings	_state			= TourbookPlugin.getDefault().getDialogSettingsSection(ID);
	private ArrayList<MergedTour>	_tourList		= new ArrayList<MergedTour>();

	private PostSelectionProvider	_postSelectionProvider;

	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourPropertyListener;
	private IPartListener2			_partListener;
	private PixelConverter			_pc;

	private ActionModifyColumns		_actionModifyColumns;

	private ColumnManager			_columnManager;

	private long					_photoStartDate;
	private long					_photoEndDate;

	/*
	 * measurement unit values
	 */
	private float					_unitValueAltitude;

	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.forStyle("SS");							//$NON-NLS-1$

	private final DateTimeFormatter	_dateFormatter	= DateTimeFormat.shortDate();
	private final DateTimeFormatter	_timeFormatter	= DateTimeFormat.mediumTime();

	private final NumberFormat		_nf_1_1			= NumberFormat.getNumberInstance();
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

	private Label					_labelPhotoDates;

	private static class Comparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final MergedTour wp1 = (MergedTour) e1;
			final MergedTour wp2 = (MergedTour) e2;

			/*
			 * sort by time
			 */
			final long wp1Time = wp1.time;
			final long wp2Time = wp2.time;

			if (wp1Time != 0 && wp2Time != 0) {
				return wp1Time > wp2Time ? 1 : -1;
			}

			return wp1Time != 0 ? 1 : -1;
		}
	}

	private class ContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _tourList.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class MergedTour {

		private long	time;

	}

	public PhotoMergeTourView() {
		super();
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoMergeTourView.this) {
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
				if (part == PhotoMergeTourView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if ((_tourList.size() == 0) || (part == PhotoMergeTourView.this)) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

					// check if a tour must be updated

//					if (containsTourIds(eventData, _tourList) == false) {
//
//						// reload tours
//						loadPhotoTours();
//
//						_tourViewer.setInput(new Object[0]);
//
//						// removed old tour data from the selection provider
//						_postSelectionProvider.clearSelection();
//
//					} else {
//						clearView();
//					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void clearView() {

		_tourList.clear();

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

//		_actionEditTourWaypoints = new ActionOpenMarkerDialog(this, true);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		// show default page
		_pageBook.showPage(_pageNoImage);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourList.size() == 0) {
//			showTourFromTourProvider();
		}
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
				label.setText(Messages.Pic_Dir_StatusLabel_NoTourIsAvailable);
			}

			_pageNoImage = new Composite(_pageBook, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageNoImage);
			GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoImage);
			{
				final Label label = new Label(_pageNoImage, SWT.WRAP);
				label.setText(Messages.Pic_Dir_StatusLabel_NoSelectedPhoto);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
			}
		}
	}

	private void createUI_10_Tours(final Composite parent) {

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
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_labelPhotoDates = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelPhotoDates);
			_labelPhotoDates.setToolTipText(Messages.Photo_Merge_Label_SelectedPhotoDates_Tooltip);
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
					fireWaypointPosition(selection);
				}
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

			}
		});

		createUI_99_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_99_ContextMenu() {

		final Table table = (Table) _tourViewer.getControl();

		_columnManager.createHeaderContextMenu(table, null);
	}

	private void defineAllColumns(final Composite parent) {

		defineColumnTime();
		defineColumnDate();
	}

	/**
	 * column: date/time
	 */
	private void defineColumnDate() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_DATE.createColumn(_columnManager, _pc);
		colDef.setCanModifyVisibility(false);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergedTour mergedTour = (MergedTour) cell.getElement();
				final long time = mergedTour.time;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _dateFormatter.print(time));
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TIME.createColumn(_columnManager, _pc);
		colDef.setCanModifyVisibility(false);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MergedTour mergedTour = (MergedTour) cell.getElement();
				final long time = mergedTour.time;

				cell.setText(time == 0 ? UI.EMPTY_STRING : _timeFormatter.print(time));
			}
		});
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getViewSite().getPage();

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
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

	/**
	 * fire waypoint position
	 */
	private void fireWaypointPosition(final StructuredSelection selection) {
		_postSelectionProvider.setSelection(selection);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public ArrayList<TourData> getSelectedTours() {
//		return _tourList;
		return new ArrayList<TourData>();
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private ArrayList<Long> loadTours() {

		final DateTime dtStart = new DateTime(_photoStartDate).minusDays(1);
		final DateTime dtEnd = new DateTime(_photoEndDate).plusDays(1);

		final String sqlString = "" //

				+ "SELECT " //$NON-NLS-1$

				+ " TourId," //					1 //$NON-NLS-1$
				+ " StartYear," //				2 //$NON-NLS-1$
				+ " StartMonth," //				3 //$NON-NLS-1$
				+ " StartDay," //				4 //$NON-NLS-1$
				+ " StartHour," //				5 //$NON-NLS-1$
				+ " StartMinute," //			6 //$NON-NLS-1$
				+ " TourRecordingTime" //		7 //$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				+ " WHERE"
				+ (" StartYear >= " + dtStart.getYear())
				+ (" AND StartMonth >= " + dtStart.getMonthOfYear())
				+ (" AND StartDay >= " + dtStart.getDayOfMonth())
				+ (" AND StartHour >= " + dtStart.getHourOfDay())
				+ (" AND StartMinute >= " + dtStart.getMinuteOfHour())
				+ ""

				+ UI.NEW_LINE

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute");

		final ArrayList<Long> tourIds = new ArrayList<Long>();

		Connection conn = null;
		Statement stmt = null;

		try {

			conn = TourDatabase.getInstance().getConnection();
			stmt = conn.createStatement();

			final ResultSet result = stmt.executeQuery(sqlString);

			while (result.next()) {
				tourIds.add(result.getLong(1));
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.sqlClose(stmt);
			TourDatabase.closeConnection(conn);

		}

		return tourIds;
	}

	private void onSelectionChanged(final ISelection selection) {

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

	void updateUI(final ArrayList<PhotoWrapper> selectedPhotos) {

		if (selectedPhotos.size() == 0) {
			clearView();
			return;
		}

		_pageBook.showPage(_pageViewer);

		_photoStartDate = selectedPhotos.get(0).imageSortingTime;
		_photoEndDate = _photoStartDate;

		for (final PhotoWrapper photoWrapper : selectedPhotos) {

			final long imageSortingTime = photoWrapper.imageSortingTime;

			if (imageSortingTime < _photoStartDate) {
				_photoStartDate = imageSortingTime;
			} else if (imageSortingTime > _photoEndDate) {
				_photoEndDate = imageSortingTime;
			}
		}

		if (_photoStartDate == _photoEndDate) {

			_labelPhotoDates.setText(NLS.bind(
					Messages.Photo_Merge_Label_SelectedPhotoDate,
					_dtFormatter.print(_photoStartDate)));

		} else {

			_labelPhotoDates.setText(NLS.bind(
					Messages.Photo_Merge_Label_SelectedPhotoDates,
					_dtFormatter.print(_photoStartDate),
					_dtFormatter.print(_photoEndDate)));
		}

		_viewerContainer.layout();

		loadTours();
	}
}
