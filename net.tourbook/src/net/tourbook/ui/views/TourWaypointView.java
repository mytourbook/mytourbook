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
package net.tourbook.ui.views;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourWaypointView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String		ID						= "net.tourbook.views.TourWaypointView";					//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;

	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private final IDialogSettings	_state					= TourbookPlugin.getDefault().getDialogSettingsSection(ID);

	private TourData				_tourData;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourPropertyListener;
	private IPartListener2			_partListener;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;

	private TableViewer				_wpViewer;
	private Label					_pageNoChart;
	private Composite				_viewerContainer;

	private ActionModifyColumns		_actionModifyColumns;

	/*
	 * none UI
	 */
	private ColumnManager			_columnManager;

	/*
	 * measurement unit values
	 */
	private float					_unitValueAltitude;

	private final DateTimeFormatter	_dateFormatter			= DateTimeFormat.shortDate();
	private final DateTimeFormatter	_timeFormatter			= DateTimeFormat.mediumTime();

	private final NumberFormat		_nf_1_1					= NumberFormat.getNumberInstance();

	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	private static class WayPointComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final TourWayPoint wp1 = (TourWayPoint) e1;
			final TourWayPoint wp2 = (TourWayPoint) e2;

			/*
			 * sort by time
			 */
			final long wp1Time = wp1.getTime();
			final long wp2Time = wp2.getTime();

			if (wp1Time != 0 && wp2Time != 0) {
				return wp1Time > wp2Time ? 1 : -1;
			}

			return wp1Time != 0 ? 1 : -1;

//			/*
//			 * sort by creation sequence
//			 */
//			final long wp1CreateId = wp1.getCreateId();
//			final long wp2CreateId = wp2.getCreateId();
//
//			if (wp1CreateId == 0) {
//
//				if (wp2CreateId == 0) {
//
//					// both way points are persisted
//					return wp1.getWayPointId() > wp2.getWayPointId() ? 1 : -1;
//				}
//
//				return 1;
//
//			} else {
//
//				// _createId != 0
//
//				if (wp2CreateId != 0) {
//
//					// both way points are created and not persisted
//					return wp1CreateId > wp2CreateId ? 1 : -1;
//				}
//
//				return -1;
//			}
		}
	}

	class WaypointViewerContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (_tourData == null) {
				return new Object[0];
			} else {
				return _tourData.getTourWayPoints().toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public TourWaypointView() {
		super();
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourWaypointView.this) {
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

					_wpViewer = (TableViewer) recreateViewer(_wpViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_wpViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_wpViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_wpViewer.getTable().redraw();
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
				if (part == TourWaypointView.this) {
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

				if ((_tourData == null) || (part == TourWaypointView.this)) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

					// check if a tour must be updated

					final long viewTourId = _tourData.getTourId();

					if (UI.containsTourId(eventData, viewTourId) != null) {

						// reload tour data
						_tourData = TourManager.getInstance().getTourData(viewTourId);

						_wpViewer.setInput(new Object[0]);

						// removed old tour data from the selection provider
						_postSelectionProvider.clearSelection();

					} else {
						clearView();
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void clearView() {

		_tourData = null;

		_wpViewer.setInput(new Object[0]);

		_postSelectionProvider.clearSelection();

		_pageBook.showPage(_pageNoChart);
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

		final Control viewerControl = _wpViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);

		getSite().registerContextMenu(menuMgr, _wpViewer);
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
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		// show default page
		_pageBook.showPage(_pageNoChart);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		_pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		_viewerContainer = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI10WaypointViewer(_viewerContainer);
		}
	}

	private void createUI10WaypointViewer(final Composite parent) {

		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
//		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
		table.setLinesVisible(true);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if (isTourInDb() == false) {
					return;
				}

				final IStructuredSelection selection = (IStructuredSelection) _wpViewer.getSelection();
				if ((selection.size() > 0) && (e.keyCode == SWT.CR)) {

					// run async, otherwise it would pop up the dialog two times
//					Display.getCurrent().asyncExec(new Runnable() {
//						public void run() {
//							_actionEditTourWaypoints.setSelectedMarker((TourMarker) selection.getFirstElement());
//							_actionEditTourWaypoints.run();
//						}
//					});
				}
			}
		});

		/*
		 * create table viewer
		 */
		_wpViewer = new TableViewer(table);
		_columnManager.createColumns(_wpViewer);

		_wpViewer.setContentProvider(new WaypointViewerContentProvider());
		_wpViewer.setComparator(new WayPointComparator());

		_wpViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireWaypointPosition(selection);
				}
			}
		});

		_wpViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (isTourInDb() == false) {
					return;
				}

				// edit selected marker
//				final IStructuredSelection selection = (IStructuredSelection) _wpViewer.getSelection();
//				if (selection.size() > 0) {
//					_actionEditTourWaypoints.setSelectedMarker((TourMarker) selection.getFirstElement());
//					_actionEditTourWaypoints.run();
//				}
			}
		});

		createUI40ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI40ContextMenu() {

		final Table table = (Table) _wpViewer.getControl();

		_columnManager.createHeaderContextMenu(table, null);
	}

	private void defineAllColumns(final Composite parent) {

		defineColumnName();
		defineColumnDescription();
		defineColumnComment();
		defineColumnCategory();
		defineColumnSymbol();
		defineColumnAltitude();
		defineColumnTime();
		defineColumnDate();
		defineColumnLatitude();
		defineColumnLongitude();
		defineColumnId();
	}

	/**
	 * column: altitude
	 */
	private void defineColumnAltitude() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_ALTITUDE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				final float dbAltitude = wp.getAltitude();

				if (dbAltitude == Float.MIN_VALUE) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					final float altitude = dbAltitude / _unitValueAltitude;

					cell.setText(_nf_1_1.format(altitude));
				}
			}
		});
	}

	/**
	 * column: category
	 */
	private void defineColumnCategory() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_CATEGORY.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(wp.getCategory());
			}
		});
	}

	/**
	 * column: comment
	 */
	private void defineColumnComment() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_COMMENT.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(wp.getComment());
			}
		});
	}

	/**
	 * column: date/time
	 */
	private void defineColumnDate() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_DATE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				final long time = wp.getTime();

				cell.setText(time == 0 ? UI.EMPTY_STRING : _dateFormatter.print(time));
			}
		});
	}

	/**
	 * column: description
	 */
	private void defineColumnDescription() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_DESCRIPTION.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(wp.getDescription());
			}
		});
	}

	/**
	 * column: id
	 */
	private void defineColumnId() {

		final ColumnDefinition colDef = TableColumnFactory.ID.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				long wpId = wp.getWayPointId();

				if (wpId == TourDatabase.ENTITY_IS_NOT_SAVED) {
					wpId = wp.getCreateId();
				}

				cell.setText(Long.toString(wpId));
			}
		});
	}

	/**
	 * column: latitude
	 */
	private void defineColumnLatitude() {

		final ColumnDefinition colDef = TableColumnFactory.LATITUDE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(Double.toString(wp.getLatitude()));
			}
		});
	}

	/**
	 * column: longitude
	 */
	private void defineColumnLongitude() {

		final ColumnDefinition colDef = TableColumnFactory.LONGITUDE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(true);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(Double.toString(wp.getLongitude()));
			}
		});
	}

	/**
	 * column: name
	 */
	private void defineColumnName() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_NAME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(wp.getName());
			}
		});
	}

	/**
	 * column: symbol
	 */
	private void defineColumnSymbol() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_SYMBOL.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				cell.setText(wp.getSymbol());
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourWayPoint wp = (TourWayPoint) cell.getElement();
				final long time = wp.getTime();

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

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(_tourData);

		return selectedTours;
	}

	@Override
	public ColumnViewer getViewer() {
		return _wpViewer;
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database
	 */
	private boolean isTourInDb() {

		if ((_tourData != null) && (_tourData.getTourPerson() != null)) {
			return true;
		}

		return false;
	}

	private void onSelectionChanged(final ISelection selection) {

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the waypoint viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			_tourData = tourDataSelection.getTourData();

			if (_tourData != null) {
				tourId = _tourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				tourId = tourIds.get(0);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				tourId = ((TVICatalogComparedTour) firstElement).getTourId();
			} else if (firstElement instanceof TVICompareResultComparedTour) {
				tourId = ((TVICompareResultComparedTour) firstElement).getComparedTourData().getTourId();
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}

		if (tourId > TourDatabase.ENTITY_IS_NOT_SAVED) {

			final TourData tourData = TourManager.getInstance().getTourData(tourId);
			if (tourData != null) {
				_tourData = tourData;
			}
		}

		final boolean isTour = (tourId >= 0) && (_tourData != null);

		if (isTour) {
			_pageBook.showPage(_viewerContainer);
			_wpViewer.setInput(new Object[0]);
		}

//		_actionEditTourWaypoints.setEnabled(isTour);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_wpViewer.getTable().dispose();

			createUI10WaypointViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _wpViewer;
	}

	@Override
	public void reloadViewer() {
		_wpViewer.setInput(new Object[0]);
	}

	private void saveState() {

		// check if UI is disposed
		final Table table = _wpViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {
		_wpViewer.getTable().setFocus();
	}

	private void showTourFromTourProvider() {

		_pageBook.showPage(_pageNoChart);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_tourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if ((selectedTours != null) && (selectedTours.size() > 0)) {
					onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
				}
			}
		});
	}

	private void updateInternalUnitValues() {

		_unitValueAltitude = UI.UNIT_VALUE_ALTITUDE;
	}

}
