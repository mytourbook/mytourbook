/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMarker;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String		ID			= "net.tourbook.views.TourMarkerView";			//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state		= TourbookPlugin.getState("TourMarkerView");	//$NON-NLS-1$

	private TourData				_tourData;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private ActionOpenMarkerDialog	_actionEditTourMarkers;
	private ActionModifyColumns		_actionModifyColumns;

	private PixelConverter			_pc;

	private TableViewer				_markerViewer;
	private ColumnManager			_columnManager;

	private boolean					_isInUpdate;

	private ColumnDefinition		_colDefName;
	private ColumnDefinition		_colDefVisibility;

	private final NumberFormat		_nf3		= NumberFormat.getNumberInstance();
	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoTour;
	private Composite				_viewerContainer;

	private Font					_boldFont;

	class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (_tourData == null) {
				return new Object[0];
			} else {
				return _tourData.getTourMarkers().toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the markers by time
	 */
	private static class MarkerViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

//			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
// time is disabled because it's not always available in gpx files

			return ((TourMarker) (obj1)).getSerieIndex() - ((TourMarker) (obj2)).getSerieIndex();
		}
	}

	public TourMarkerView() {
		super();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMarkerView.this) {

//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourMarkerView.this);
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

				if (part == TourMarkerView.this) {
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

				if (_isInUpdate) {
					return;
				}

				if ((_tourData == null) || (part == TourMarkerView.this)) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						final long viewTourId = _tourData.getTourId();

						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								// get modified tour
								_tourData = tourData;

								updateUI_MarkerViewer();

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								// nothing more to do, the view contains only one tour
								return;
							}
						}
					}

				} else if (eventId == TourEventId.MARKER_SELECTION) {

					onTourEvent_TourMarker(eventData);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;

		updateUI_MarkerViewer();

		_postSelectionProvider.clearSelection();
	}

	private void createActions() {

		_actionEditTourMarkers = new ActionOpenMarkerDialog(this, true);
		_actionModifyColumns = new ActionModifyColumns(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

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

		// show default page
		_pageBook.showPage(_pageNoTour);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoTour = new Label(_pageBook, SWT.NONE);
		_pageNoTour.setText(Messages.UI_Label_no_chart_is_selected);

		_viewerContainer = new Composite(_pageBook, SWT.NONE);
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
//		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
		table.setLinesVisible(false);

		/*
		 * create table viewer
		 */
		_markerViewer = new TableViewer(table);

//		// set editing support after the viewer is created but before the columns are created
//		net.tourbook.common.UI.setCellEditSupport(_markerViewer);
//
//		_colDefName.setEditingSupport(new MarkerEditingSupportLabel(_markerViewer));
//		_colDefVisibility.setEditingSupport(new MarkerEditingSupportVisibility(_markerViewer));

		_columnManager.createColumns(_markerViewer);

		_markerViewer.setUseHashlookup(true);
		_markerViewer.setContentProvider(new MarkerViewerContentProvicer());
		_markerViewer.setSorter(new MarkerViewerSorter());

		_markerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelect_TourMarker((StructuredSelection) event.getSelection());
			}
		});

		_markerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (isTourSavedInDb() == false) {
					return;
				}

				// edit selected marker
				final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
				if (selection.size() > 0) {
					_actionEditTourMarkers.setTourMarker((TourMarker) selection.getFirstElement());
					_actionEditTourMarkers.run();
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

		defineColumn_IsVisible();

		defineColumn_Time();
		defineColumn_Distance();

		defineColumn_TimeDelta();
		defineColumn_DistanceDelta();

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

				final TourMarker marker = (TourMarker) cell.getElement();
				cell.setText(marker.getDescription());
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

				final TourMarker tourMarker = (TourMarker) cell.getElement();

				final float markerDistance = tourMarker.getDistance();
				if (markerDistance == -1) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf3.format(markerDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
				}

				if (tourMarker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});
	}

	/**
	 * Column: Distance delta km/mi
	 */
	private void defineColumn_DistanceDelta() {

		final ColumnDefinition colDef = TableColumnFactory.DISTANCE_DELTA.createColumn(_columnManager, _pc);
//		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();

				final float markerDistance = tourMarker.getDistance();

				if (markerDistance == -1) {

					cell.setText(UI.EMPTY_STRING);

				} else {

					float prevDistance = 0;
					final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);

					if (null != lastRow) {
						final TourMarker element = (TourMarker) lastRow.getElement();
						if (element instanceof TourMarker) {
							prevDistance = element.getDistance();
						}
						prevDistance = prevDistance < 0 ? 0 : prevDistance;
					}

					cell.setText(_nf3.format((markerDistance - prevDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
				}
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

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				cell.setText(tourMarker.isMarkerVisible()
						? Messages.App_Label_BooleanYes
						: Messages.App_Label_BooleanNo);
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

				final TourMarker marker = (TourMarker) cell.getElement();
				cell.setText(marker.getLabel());
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

				final TourMarker marker = (TourMarker) cell.getElement();
				final long time = marker.getTime();

				cell.setText(UI.format_hh_mm_ss(time));
			}
		});
	}

	/**
	 * Column: Time
	 */
	private void defineColumn_TimeDelta() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_DELTA.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);
				int lastTime = 0;
				if (null != lastRow) {
					final Object element = lastRow.getElement();
					if (element instanceof TourMarker) {
						lastTime = ((TourMarker) element).getTime();
					}
				}

				cell.setText(UI.format_hh_mm_ss(((TourMarker) cell.getElement()).getTime() - lastTime));

				final String text = ((TourMarker) cell.getElement()).getLabel();

				/*
				 * Show text in red/bold when the text ends with a !, this hidden feature was
				 * introduced by helmling
				 */
				if (text.endsWith(UI.SYMBOL_EXCLAMATION_POINT)) {

					final Display display = Display.getCurrent();

					if (null != display) {
						cell.setForeground(display.getSystemColor(SWT.COLOR_RED));
					}

					cell.setFont(_boldFont);
				}
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

				final TourMarker marker = (TourMarker) cell.getElement();

				String columnText = UI.EMPTY_STRING;

				/*
				 * Url
				 */
				final String urlText = marker.getUrlText();
				final String urlAddress = marker.getUrlAddress();
				final boolean isText = urlText.length() > 0;
				final boolean isAddress = urlAddress.length() > 0;

				if (isText || isAddress) {

					if (isAddress == false) {

						// only text is in the link -> this is not a internet address but create a link of it

						columnText = urlText;

					} else {

						columnText = urlAddress;
					}
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

	/**
	 * enable actions
	 */
	private void enableActions() {

		final boolean isTourInDb = isTourSavedInDb();

		_actionEditTourMarkers.setEnabled(isTourInDb);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionEditTourMarkers);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// set the marker which should be selected in the marker dialog
		final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
		_actionEditTourMarkers.setTourMarker((TourMarker) selection.getFirstElement());

		enableActions();
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

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public Object getMarkerViewer() {
		return _markerViewer;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

		if (_tourData != null) {
			selectedTours.add(_tourData);
		}

		return selectedTours;
	}

	@Override
	public ColumnViewer getViewer() {
		return _markerViewer;
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database.
	 */
	private boolean isTourSavedInDb() {

		if ((_tourData != null) && (_tourData.getTourPerson() != null)) {
			return true;
		}

		return false;
	}

	private void onSelect_TourMarker(final StructuredSelection selection) {

		if (_isInUpdate) {
			return;
		}

		final ArrayList<TourMarker> selectedTourMarker = new ArrayList<TourMarker>();

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			selectedTourMarker.add((TourMarker) iterator.next());
		}

		TourManager.fireEvent(//
				TourEventId.MARKER_SELECTION,
				new SelectionTourMarker(_tourData, selectedTourMarker),
				getSite().getPart());
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_isInUpdate) {
			return;
		}

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the marker viewer

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

		if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

			final TourData tourData = TourManager.getInstance().getTourData(tourId);
			if (tourData != null) {
				_tourData = tourData;
			}
		}

		updateUI_MarkerViewer();
	}

	private void onTourEvent_TourMarker(final Object eventData) {

		if (eventData instanceof SelectionTourMarker) {

			/*
			 * Select the tourmarker in the view
			 */
			final SelectionTourMarker selection = (SelectionTourMarker) eventData;

			final TourData tourData = selection.getTourData();
			final ArrayList<TourMarker> tourMarker = selection.getSelectedTourMarker();

			if (tourData != _tourData) {

				_tourData = tourData;

				updateUI_MarkerViewer();
			}

			_isInUpdate = true;
			{
				_markerViewer.setSelection(new StructuredSelection(tourMarker), true);
			}
			_isInUpdate = false;
		}
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

		updateUI_MarkerViewer();
	}

	private void saveState() {

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

		_markerViewer.getTable().setFocus();
	}

	private void showTourFromTourProvider() {

		_pageBook.showPage(_pageNoTour);

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

	private void updateUI_MarkerViewer() {

		if (isTourSavedInDb()) {

			_isInUpdate = true;
			{
				_markerViewer.setInput(new Object[0]);
			}
			_isInUpdate = false;

			_pageBook.showPage(_viewerContainer);

		} else {

			_pageBook.showPage(_pageNoTour);
		}

		enableActions();
	}
}
