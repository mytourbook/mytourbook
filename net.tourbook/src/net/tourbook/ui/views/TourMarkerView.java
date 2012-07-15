/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
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
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart implements ITourProvider {

	public static final String		ID						= "net.tourbook.views.TourMarkerView";					//$NON-NLS-1$

	private final String			STATE_SHOW_DELTA		= "showDelta";											//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private final IDialogSettings	_state					= TourbookPlugin.getDefault()//
																	.getDialogSettingsSection("TourMarkerView");	//$NON-NLS-1$

	private TourData				_tourData;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourPropertyListener;
	private IPartListener2			_partListener;

	private ActionOpenMarkerDialog	_actionEditTourMarkers;
	private Action					_actionShowDelta;

	private PixelConverter			_pc;

	private boolean					_isShowDelta			= false;
	private Font					_boldFont				= null;

	private final NumberFormat		_nf_3_3					= NumberFormat.getNumberInstance();
	{
		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;
	private Composite				_viewerContainer;

	private TableViewer				_markerViewer;
	private Chart					_tourChart;

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

					UI.updateUnits();

					// dispose viewer
					final Control[] children = _viewerContainer.getChildren();
					for (final Control element : children) {
						element.dispose();
					}

					createUI10TableViewer(_viewerContainer);
					_viewerContainer.layout();

					// update the viewer
					_markerViewer.setInput(this);

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

		_tourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

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

								_markerViewer.setInput(new Object[0]);

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								// nothing more to do, the view contains only one tour
								return;
							}
						}
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

		_markerViewer.setInput(new Object[0]);

		_postSelectionProvider.clearSelection();

		_pageBook.showPage(_pageNoChart);
	}

	private void createActions() {

		_actionEditTourMarkers = new ActionOpenMarkerDialog(this, true);

		_actionShowDelta = new Action(null, Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_isShowDelta = this.isChecked();
				final Control[] children = _viewerContainer.getChildren();
				for (final Control element : children) {
					element.dispose();
				}
				createUI10TableViewer(_viewerContainer);
				_viewerContainer.layout();
				// update the viewer
				_markerViewer.setInput(this);
			}
		};
		_actionShowDelta.setText(UI.SYMBOL_DIFFERENCE);
		_actionShowDelta.setChecked(_isShowDelta);

		/*
		 * fill view toolbar
		 */
		getViewSite().getActionBars().getToolBarManager().add(_actionShowDelta);
	}

	@Override
	public void createPartControl(final Composite parent) {

		restoreStateBeforeUI();

		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		createActions();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		// show default page
		_pageBook.showPage(_pageNoChart);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_pageBook = new PageBook(parent, SWT.NONE);
		_pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		_viewerContainer = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI10TableViewer(_viewerContainer);
		}

		createUI20ContextMenu();
	}

	private void createUI10TableViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
//		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
		table.setLinesVisible(true);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if (isTourInDb() == false) {
					return;
				}

				final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
				if ((selection.size() > 0) && (e.keyCode == SWT.CR)) {

					// run async, otherwise it would pop up the dialog two times
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							_actionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());
							_actionEditTourMarkers.run();
						}
					});
				}
			}
		});

		_markerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: time
		tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Marker_Column_time);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(UI.format_hh_mm_ss(((TourMarker) cell.getElement()).getTime()));
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(_pc.convertWidthInCharsToPixels(12), false));

		// column: distance km/mi
		tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_DISTANCE);
		tvcColumn.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();

				final float markerDistance = tourMarker.getDistance();
				if (markerDistance == -1) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_3_3.format(markerDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
				}

				if (tourMarker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(_pc.convertWidthInCharsToPixels(11), false));

		if (_isShowDelta) {

			// column: delta distance km/mi
			tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
			tvcColumn = tvc.getColumn();
			tvcColumn.setText(UI.SYMBOL_DIFFERENCE + UI.UNIT_LABEL_DISTANCE);
			tvc.setLabelProvider(new CellLabelProvider() {
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
							prevDistance = ((TourMarker) lastRow.getElement()).getDistance();
							prevDistance = prevDistance < 0 ? 0 : prevDistance;
						}
						cell.setText(_nf_3_3.format((markerDistance - prevDistance)
								/ 1000
								/ UI.UNIT_VALUE_DISTANCE));
					}
				}
			});
			tableLayout.setColumnData(tvcColumn, new ColumnPixelData(_pc.convertWidthInCharsToPixels(11), false));

			// column: delta-time
			tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
			tvcColumn = tvc.getColumn();
			tvcColumn.setText(UI.SYMBOL_DIFFERENCE + Messages.Tour_Marker_Column_time);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);
					int lastTime = 0;
					if (null != lastRow) {
						lastTime = ((TourMarker) lastRow.getElement()).getTime();
					}
					cell.setText(UI.format_hh_mm_ss(((TourMarker) cell.getElement()).getTime() - lastTime));
					final String text = ((TourMarker) cell.getElement()).getLabel();
					if (text.endsWith(UI.SYMBOL_EXCLAMATION_POINT)) {
						final Display display = Display.getCurrent();
						if (null != display) {
							cell.setForeground(display.getSystemColor(SWT.COLOR_RED));
						}
						if (null == _boldFont) {
							final FontData fd = (cell.getFont().getFontData())[0];
							fd.setStyle(SWT.BOLD);
							_boldFont = new Font(Display.getCurrent(), fd);
						}
						cell.setFont(_boldFont);
					}
				}
			});
			tableLayout.setColumnData(tvcColumn, new ColumnPixelData(_pc.convertWidthInCharsToPixels(12), false));
		}

		// column: remark
		tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Marker_Column_remark);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();

				cell.setText(tourMarker.getLabel());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(50, true));

		/*
		 * create table viewer
		 */

		_markerViewer.setContentProvider(new MarkerViewerContentProvicer());
		_markerViewer.setSorter(new MarkerViewerSorter());

		_markerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		_markerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (isTourInDb() == false) {
					return;
				}

				// edit selected marker
				final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
				if (selection.size() > 0) {
					_actionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());
					_actionEditTourMarkers.run();
				}
			}
		});
	}

	/**
	 * create the views context menu
	 */
	private void createUI20ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Table table = (Table) _markerViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(table);

		table.setMenu(menu);

		getSite().registerContextMenu(menuMgr, _markerViewer);
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);
		if (null != _boldFont) {
			_boldFont.dispose();
		}

		super.dispose();
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionEditTourMarkers);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// set the marker which should be selected in the marker dialog
		final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
		_actionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());

		/*
		 * enable actions
		 */
		final boolean tourInDb = isTourInDb();

		_actionEditTourMarkers.setEnabled(tourInDb);
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

		// a chart must be available
		if (_tourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if ((tourChart == null) || tourChart.isDisposed()) {
				return;
			} else {
				_tourChart = tourChart;
			}
		}

		final Object[] segments = selection.toArray();

		if (segments.length > 1) {

			// two or more markers are selected

			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					_tourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					((TourMarker) segments[segments.length - 1]).getSerieIndex()));

		} else if (segments.length > 0) {

			// one marker is selected

			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					_tourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	public Object getMarkerViewer() {
		return _markerViewer;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(_tourData);

		return selectedTours;
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

			// a tour was selected, get the chart and update the marker viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			_tourData = tourDataSelection.getTourData();

			if (_tourData == null) {
				_tourChart = null;
			} else {
				_tourChart = tourDataSelection.getTourChart();
				tourId = _tourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			_tourChart = null;
			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				_tourChart = null;
				tourId = tourIds.get(0);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				_tourChart = null;
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			_tourChart = null;
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

		final boolean isTour = (tourId >= 0) && (_tourData != null);

		if (isTour) {
			_pageBook.showPage(_viewerContainer);
			_markerViewer.setInput(new Object[0]);
		}

		_actionEditTourMarkers.setEnabled(isTour);
	}

	private void restoreStateBeforeUI() {

		_isShowDelta = Util.getStateBoolean(_state, STATE_SHOW_DELTA, false);

	}

	private void saveState() {

		_state.put(STATE_SHOW_DELTA, _isShowDelta);

	}

	@Override
	public void setFocus() {
		_markerViewer.getTable().setFocus();
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

}
