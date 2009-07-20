/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
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
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
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
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart implements ITourProvider {

	public static final String		ID						= "net.tourbook.views.TourMarkerView";	//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private TableViewer				fMarkerViewer;

	private TourData				fTourData;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;
	private IPropertyChangeListener	fPrefChangeListener;
	private ITourEventListener		fTourPropertyListener;
	private IPartListener2			fPartListener;

	private final NumberFormat		fNF						= NumberFormat.getNumberInstance();

	private PageBook				fPageBook;
	private Label					fPageNoChart;
	private Composite				fViewerContainer;

	private Chart					fTourChart;

	private ActionOpenMarkerDialog	fActionEditTourMarkers;

	class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.getTourMarkers().toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the markers by time
	 */
	private class MarkerViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
		}
	}

	public TourMarkerView() {
		super();
	}

	private void addPartListener() {
		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMarkerView.this) {
//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourMarkerView.this);
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					// dispose viewer
					final Control[] children = fViewerContainer.getChildren();
					for (final Control element : children) {
						element.dispose();
					}

					createTableViewer(fViewerContainer);
					fViewerContainer.layout();

					// update the viewer
					fMarkerViewer.setInput(this);

				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourMarkerView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (fTourData == null || part == TourMarkerView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						final long viewTourId = fTourData.getTourId();

						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								// get modified tour
								fTourData = tourData;

								fMarkerViewer.setInput(new Object[0]);

								// removed old tour data from the selection provider
								fPostSelectionProvider.clearSelection();

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

		TourManager.getInstance().addTourEventListener(fTourPropertyListener);
	}

	private void clearView() {
		
		fTourData = null;

		fMarkerViewer.setInput(new Object[0]);

		fPostSelectionProvider.clearSelection();

		fPageBook.showPage(fPageNoChart);
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

		final Control viewerControl = fMarkerViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);

		getSite().registerContextMenu(menuMgr, fMarkerViewer);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fViewerContainer = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);
		createTableViewer(fViewerContainer);

		createContextMenu();

		fActionEditTourMarkers = new ActionOpenMarkerDialog(this, true);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// show default page
		fPageBook.showPage(fPageNoChart);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (fTourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createTableViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if (isTourInDb() == false) {
					return;
				}

				final IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();
				if (selection.size() > 0 && e.keyCode == SWT.CR) {

					// run async, otherwise it would pop up the dialog two times
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							fActionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());
							fActionEditTourMarkers.run();
						}
					});
				}
			}
		});

		fMarkerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;
		final PixelConverter pixelConverter = new PixelConverter(table);

		// column: time
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Marker_Column_time);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(UI.format_hh_mm_ss(((TourMarker) cell.getElement()).getTime()));
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12), false));

		// column: distance km/mi
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_DISTANCE);
		tvcColumn.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();

				fNF.setMinimumFractionDigits(3);
				fNF.setMaximumFractionDigits(3);
				cell.setText(fNF.format(((float) tourMarker.getDistance()) / 1000 / UI.UNIT_VALUE_DISTANCE));

				if (tourMarker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(11), false));

		// column: remark
		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
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

		fMarkerViewer.setContentProvider(new MarkerViewerContentProvicer());
		fMarkerViewer.setSorter(new MarkerViewerSorter());

		fMarkerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		fMarkerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (isTourInDb() == false) {
					return;
				}

				// edit selected marker
				final IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();
				if (selection.size() > 0) {
					fActionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());
					fActionEditTourMarkers.run();
				}
			}
		});
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(fTourPropertyListener);
		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionEditTourMarkers);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// set the marker which should be selected in the marker dialog
		final IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();
		fActionEditTourMarkers.setSelectedMarker((TourMarker) selection.getFirstElement());

		/*
		 * enable actions
		 */
		final boolean tourInDb = isTourInDb();

		fActionEditTourMarkers.setEnabled(tourInDb);
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

		// a chart must be available
		if (fTourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if (tourChart == null || tourChart.isDisposed()) {
				return;
			} else {
				fTourChart = tourChart;
			}
		}

		final Object[] segments = selection.toArray();

		if (segments.length > 1) {

			// two or more markers are selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					((TourMarker) segments[segments.length - 1]).getSerieIndex()));

		} else if (segments.length > 0) {

			// one marker is selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	public Object getMarkerViewer() {
		return fMarkerViewer;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(fTourData);

		return selectedTours;
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database
	 */
	private boolean isTourInDb() {

		if (fTourData != null && fTourData.getTourPerson() != null) {
			return true;
		}

		return false;
	}

	private void onSelectionChanged(final ISelection selection) {

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the marker viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			fTourData = tourDataSelection.getTourData();

			if (fTourData == null) {
				fTourChart = null;
			} else {
				fTourChart = tourDataSelection.getTourChart();
				tourId = fTourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			fTourChart = null;
			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionActiveEditor) {

			fTourChart = null;

			// check tour editor
			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			TourEditor tourEditor;
			if (editor instanceof TourEditor) {

				tourEditor = (TourEditor) editor;

				// update viewer when tour data have change
				final TourChart tourChart = tourEditor.getTourChart();
				final TourData tourData = tourChart.getTourData();
				if (tourData != fTourData) {
					fTourData = tourData;
					fTourChart = tourChart;
					tourId = fTourData.getTourId();
				}
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				fTourChart = null;
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			fTourChart = null;
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
				fTourData = tourData;
			}
		}

		final boolean isTour = tourId >= 0 && fTourData != null;

		if (isTour) {
			fPageBook.showPage(fViewerContainer);
			fMarkerViewer.setInput(new Object[0]);
		}

		fActionEditTourMarkers.setEnabled(isTour);
	}

	@Override
	public void setFocus() {
		fMarkerViewer.getTable().setFocus();
	}

	private void showTourFromTourProvider() {

		fPageBook.showPage(fPageNoChart);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (fPageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (fTourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
					onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
				}
			}
		});
	}

}
