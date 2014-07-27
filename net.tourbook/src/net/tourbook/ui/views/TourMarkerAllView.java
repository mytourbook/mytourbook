/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
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
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * This view shows all available {@link TourMarker}.
 */
public class TourMarkerAllView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String			ID				= "net.tourbook.ui.views.TourMarkerAllView";	//$NON-NLS-1$

	private final IPreferenceStore		_prefStore		= TourbookPlugin.getPrefStore();

	private final IDialogSettings		_state			= TourbookPlugin.getState("TourMarkerAllView"); //$NON-NLS-1$

	private PostSelectionProvider		_postSelectionProvider;

	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	private IPartListener2				_partListener;
	private ActionModifyColumns			_actionModifyColumns;

	private PixelConverter				_pc;

	private ArrayList<TourMarkerItem>	_allTourMarker	= new ArrayList<TourMarkerItem>();

	private TableViewer					_markerViewer;

	private ColumnManager				_columnManager;
	private ColumnDefinition			_colDefName;

	private ColumnDefinition			_colDefVisibility;
	private final NumberFormat			_nf_3_3			= NumberFormat.getNumberInstance();

	{
		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}
	/*
	 * UI controls
	 */
	private Composite					_viewerContainer;

	class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _allTourMarker.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class TourMarkerItem {

		public long		markerId;
		public long		tourId;
		public String	label;
		public String	description;
		public String	urlText;
		public String	urlAddress;

	}

	public TourMarkerAllView() {
		super();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMarkerAllView.this) {
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

					_columnManager.saveState(_state);
					_columnManager.clearColumns();

					defineAllColumns();

					_markerViewer = (TableViewer) recreateViewer(_markerViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_markerViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_markerViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_markerViewer.getTable().redraw();

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
				if (part == TourMarkerAllView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourMarkerAllView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

//						final long viewTourId = _tourData.getTourId();
//
//						for (final TourData tourData : modifiedTours) {
//							if (tourData.getTourId() == viewTourId) {
//
//								// get modified tour
//								_tourData = tourData;
//
//								_markerViewer.setInput(new Object[0]);
//
//								// removed old tour data from the selection provider
//								_postSelectionProvider.clearSelection();
//
//								// nothing more to do, the view contains only one tour
//								return;
//							}
//						}
					}

				} else if (eventId == TourEventId.MARKER_SELECTION) {

					onSelectionTourMarker(eventData);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_allTourMarker.clear();

		_markerViewer.setInput(new Object[0]);

		_postSelectionProvider.clearSelection();
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		createActions();
		fillToolbar();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		loadAllMarker();
		_markerViewer.setInput(new Object[0]);
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_TableViewer(_viewerContainer);
		}
	}

	private void createUI_10_TableViewer(final Composite parent) {

		/*
		 * create table
		 */
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//		table.setLinesVisible(false);

		/*
		 * create table viewer
		 */
		_markerViewer = new TableViewer(table);

		_columnManager.createColumns(_markerViewer);

		_markerViewer.setUseHashlookup(true);
		_markerViewer.setContentProvider(new MarkerViewerContentProvicer());
//		_markerViewer.setSorter(new MarkerViewerSorter());

		_markerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					onSelectTourMarker(selection);
				}
			}
		});

		createUI_20_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_20_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Table table = (Table) _markerViewer.getControl();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private void defineAllColumns() {

//		defineColumn_IsVisible();
//		defineColumn_Time();
//		defineColumn_Distance();

		defineColumn_MarkerId();
		defineColumn_TourId();
		defineColumn_Name();
		defineColumn_Description();
		defineColumn_Url();
	}

	/**
	 * Column: Description
	 */
	private void defineColumn_Description() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_DESCRIPTION.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();
				cell.setText(marker.description);
			}
		});
	}

	/**
	 * Column: Distance km/mi
	 */
	private void defineColumn_Distance() {

		final ColumnDefinition colDef = TableColumnFactory.DISTANCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem tourMarker = (TourMarkerItem) cell.getElement();

//				final float markerDistance = tourMarker.distance;
//				if (markerDistance == -1) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(_nf_3_3.format(markerDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
//				}
//
//				if (tourMarker.type() == ChartLabel.MARKER_TYPE_DEVICE) {
//					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//				}
			}
		});
	}

	/**
	 * Column: Is visible
	 */
	private void defineColumn_IsVisible() {

		_colDefVisibility = TableColumnFactory.MAP_MARKER_VISIBLE.createColumn(_columnManager, _pc);
		_colDefVisibility.setIsDefaultColumn();

		_colDefVisibility.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem tourMarker = (TourMarkerItem) cell.getElement();
//				cell.setText(tourMarker.isMarkerVisible()
//						? Messages.App_Label_BooleanYes
//						: Messages.App_Label_BooleanNo);
			}
		});
	}

	/**
	 * Column: Marker ID
	 */
	private void defineColumn_MarkerId() {

		final int pixelWidth = _pc.convertWidthInCharsToPixels(30);
		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "MarkerId", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel("Marker ID");
		colDef.setColumnHeaderText("Marker ID");
		colDef.setDefaultColumnWidth(pixelWidth);
		colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(Long.toString(((TourMarkerItem) cell.getElement()).markerId));
			}
		});
	}

	/**
	 * Column: Name
	 */
	private void defineColumn_Name() {

		_colDefName = TableColumnFactory.WAYPOINT_NAME.createColumn(_columnManager, _pc);
		_colDefName.setIsDefaultColumn();

		_colDefName.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();
				cell.setText(marker.label);
			}
		});
	}

	/**
	 * Column: Time
	 */
	private void defineColumn_Time() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TIME_HH_MM_SS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();

		// hide wrong tooltip
		colDef.setColumnHeaderToolTipText(UI.EMPTY_STRING);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();
//				final long time = marker.getTime();
//
//				cell.setText(UI.format_hh_mm_ss(time));
			}
		});
	}

	/**
	 * Column: TourID
	 */
	private void defineColumn_TourId() {

		final int pixelWidth = _pc.convertWidthInCharsToPixels(30);
		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "TourId", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel("Tour ID");
		colDef.setColumnHeaderText("Tour ID");
		colDef.setDefaultColumnWidth(pixelWidth);
		colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(Long.toString(((TourMarkerItem) cell.getElement()).tourId));
			}
		});
	}

	/**
	 * Column: Url
	 */
	private void defineColumn_Url() {

		final ColumnDefinition colDef = TableColumnFactory.URL.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();

				String columnText = UI.EMPTY_STRING;

				/*
				 * Url
				 */
				final String urlText = marker.urlText;
				final String urlAddress = marker.urlAddress;
				final boolean isText = urlText != null && urlText.length() > 0;
				final boolean isAddress = urlAddress != null && urlAddress.length() > 0;

				if (isText || isAddress) {

					String linkText;

					if (isAddress == false) {

						// only text is in the link -> this is not a internet address but create a link of it

						linkText = urlText;

					} else if (isText == false) {

						linkText = urlAddress;

					} else {

						linkText = urlAddress;
					}

					columnText = linkText;
				}

				cell.setText(columnText);
			}
		});
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

	}

	private void fillToolbar() {

		final IActionBars actionBars = getViewSite().getActionBars();

		/*
		 * Fill view menu
		 */
		final IMenuManager menuMgr = actionBars.getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

		/*
		 * Fill view toolbar
		 */
//		final IToolBarManager tbm = actionBars.getToolBarManager();
//
//		tbm.add();
	}

	private void fireMarkerPosition(final StructuredSelection selection) {

		final Object[] selectedMarker = selection.toArray();

		if (selectedMarker.length > 0) {

//			final ArrayList<TourMarker> allTourMarker = new ArrayList<TourMarker>();
//
//			for (final Object object : selectedMarker) {
//				allTourMarker.add((TourMarker) object);
//			}
//
//			_postSelectionProvider.setSelection(new SelectionTourMarker(_tourData, allTourMarker));
		}
	}

	/**
	 * Fire a selection for the selected marker(s).
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

//		// a chart must be available
//		if (_tourChart == null) {
//
//			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();
//
//			if ((tourChart == null) || tourChart.isDisposed()) {
//
//				fireMarkerPosition(selection);
//
//				return;
//
//			} else {
//				_tourChart = tourChart;
//			}
//		}
//
//		final Object[] selectedMarker = selection.toArray();
//
//		if (selectedMarker.length > 1) {
//
//			// two or more markers are selected
//
//			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
//					_tourChart,
//					((TourMarker) selectedMarker[0]).getSerieIndex(),
//					((TourMarker) selectedMarker[selectedMarker.length - 1]).getSerieIndex()));
//
//		} else if (selectedMarker.length > 0) {
//
//			// one marker is selected
//
//			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
//					_tourChart,
//					((TourMarker) selectedMarker[0]).getSerieIndex(),
//					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
//		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public Object getMarkerViewer() {
		return _markerViewer;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

//		if (_tourData != null) {
//			selectedTours.add(_tourData);
//		}

		return selectedTours;
	}

	@Override
	public ColumnViewer getViewer() {
		return _markerViewer;
	}

	private void loadAllMarker() {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet result = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			_allTourMarker.clear();

			final String sql = "SELECT " //
					+ "MarkerID, " //						// 1
					+ (TourDatabase.KEY_TOUR + ", ") //		// 2
					+ "Label, " //							// 3
					+ "description, " //					// 4
					+ "urlText, " //						// 5
					+ "urlAddress " //						// 6
					//
					+ (" FROM " + TourDatabase.TABLE_TOUR_MARKER)
					+ " ORDER BY Label";

			statement = conn.prepareStatement(sql);
			result = statement.executeQuery();

			while (result.next()) {

				final TourMarkerItem markerItem = new TourMarkerItem();
				_allTourMarker.add(markerItem);

				markerItem.markerId = result.getLong(1);
				markerItem.tourId = result.getLong(2);
				markerItem.label = result.getString(3);
				markerItem.description = result.getString(4);
				markerItem.urlText = result.getString(5);
				markerItem.urlAddress = result.getString(6);
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.closeSql(conn);
			Util.closeSql(statement);
			Util.closeSql(result);
		}
	}

	private void onSelectionChanged(final ISelection selection) {

	}

	private void onSelectionTourMarker(final Object eventData) {

//		if (eventData instanceof SelectionTourMarker) {
//
//			/*
//			 * Select the tourmarker in the view
//			 */
//			final SelectionTourMarker selection = (SelectionTourMarker) eventData;
//
//			final TourData tourData = selection.getTourData();
//			final ArrayList<TourMarker> tourMarker = selection.getTourMarker();
//
//			if (tourData == _tourData) {
//				_markerViewer.setSelection(new StructuredSelection(tourMarker), true);
//			}
//		}
	}

	private void onSelectTourMarker(final StructuredSelection selection) {

		fireSliderPosition(selection);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_markerViewer.getTable().dispose();

			createUI_10_TableViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _markerViewer;
	}

	@Override
	public void reloadViewer() {

		_markerViewer.setInput(new Object[0]);
	}

	private void saveState() {

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {
		_markerViewer.getTable().setFocus();
	}

}
