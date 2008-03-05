/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart {

	public static final String		ID						= "net.tourbook.views.TourMarkerView";	//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private TableViewer				fMarkerViewer;

	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;
	private IPropertyChangeListener	fPrefChangeListener;
	private PostSelectionProvider	fPostSelectionProvider;

	private final NumberFormat		fNF						= NumberFormat.getNumberInstance();

	private PageBook				fPageBook;
	private Label					fPageNoChart;
	private Composite				fViewerContainer;

	private IPropertyListener		fTourChangeListener;

	private Chart					fTourChart;

	// private TourMarker fBackupMarker = new TourMarker();

	class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(Object inputElement) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.getTourMarkers().toArray();
			}
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

//	class MarkerViewerLabelProvider extends CellLabelProvider {
//
//		@Override
//		public void update(ViewerCell cell) {
//
//			TourMarker tourMarker = (TourMarker) cell.getElement();
//
//			switch (cell.getColumnIndex()) {
//
//			case COLUMN_TIME:
//				int time = tourMarker.getTime();
//				fCalendar.set(0, 0, 0, time / 3600, ((time % 3600) / 60), ((time % 3600) % 60));
//				cell.setText(fDF.format(fCalendar.getTime()));
//				break;
//
//			case COLUMN_DISTANCE:
//				fNF.setMinimumFractionDigits(1);
//				fNF.setMaximumFractionDigits(1);
//				cell.setText(fNF.format(((float) tourMarker.getDistance()) / 1000 / UI.UNIT_VALUE_DISTANCE));
//
//				if (tourMarker.getType() == ChartMarker.MARKER_TYPE_DEVICE) {
//					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//				}
//				break;
//
//			case COLUMN_REMARK:
//				cell.setText(tourMarker.getLabel());
//				break;
//
//			case COLUMN_X_OFFSET:
//				cell.setText(Integer.toString(tourMarker.getLabelXOffset()));
//				break;
//
//			case COLUMN_Y_OFFSET:
//				cell.setText(Integer.toString(tourMarker.getLabelYOffset()));
//				break;
//
//			case COLUMN_VISUAL_POSITION:
//				int visualPosition = tourMarker.getVisualPosition();
//				if (visualPosition == -1 || visualPosition >= TourMarker.visualPositionLabels.length) {
//					cell.setText(TourMarker.visualPositionLabels[0]);
//				} else {
//					cell.setText(TourMarker.visualPositionLabels[visualPosition]);
//				}
//				break;
//
//			default:
//				break;
//			}
//		}
//	}

	/**
	 * Sort the markers by time
	 */
	private class MarkerViewerSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
		}
	}

	public TourMarkerView() {
		super();
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					// dispose viewer
					Control[] children = fViewerContainer.getChildren();
					for (int childIndex = 0; childIndex < children.length; childIndex++) {
						children[childIndex].dispose();
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
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourChangeListener() {

		fTourChangeListener = new IPropertyListener() {

			public void propertyChanged(Object source, int propId) {
				if (propId == TourDatabase.TOUR_IS_CHANGED) {
					fMarkerViewer.setInput(this);
				}
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourChangeListener);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		Control viewerControl = fMarkerViewer.getControl();
		Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);

		getSite().registerContextMenu(menuMgr, fMarkerViewer);
	}

	@Override
	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fViewerContainer = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);
		createTableViewer(fViewerContainer);

		createContextMenu();

		addSelectionListener();
		addTourChangeListener();
		addPrefListener();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		/*
		 * get markers from current selection
		 */
		final ISelection curretSelection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (curretSelection instanceof SelectionActiveEditor) {
			onSelectionChanged(curretSelection);
		} else {
			IEditorPart activeEditor = getSite().getPage().getActiveEditor();
			if (activeEditor != null) {
				onSelectionChanged(new SelectionActiveEditor(activeEditor));
			}
		}
	}

	private void createTableViewer(Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();

				if (selection.size() > 0) {
					if (e.keyCode == SWT.CR) {
						if (e.stateMask == SWT.CONTROL) {
							// edit visual position
							fMarkerViewer.editElement(selection.getFirstElement(), COLUMN_VISUAL_POSITION);
						} else {
							if (fMarkerViewer.isCellEditorActive() == false) {
								fMarkerViewer.editElement(selection.getFirstElement(), COLUMN_REMARK);
							}
						}
					}
				}
			}
		});

		fMarkerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;
		PixelConverter pixelConverter = new PixelConverter(table);

		// column: time
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Marker_Column_time);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				TourMarker tourMarker = (TourMarker) cell.getElement();

				int time = tourMarker.getTime();

				cell.setText(new Formatter().format(Messages.Format_hhmm, (time / 3600), ((time % 3600) / 60))
						.toString());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(8), false));

		// column: distance km/mi
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_DISTANCE);
		tvcColumn.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				TourMarker tourMarker = (TourMarker) cell.getElement();

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) tourMarker.getDistance()) / 1000 / UI.UNIT_VALUE_DISTANCE));
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(8), false));

		// column: remark
		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Marker_Column_remark);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				TourMarker tourMarker = (TourMarker) cell.getElement();

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
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});
	}

	@Override
	public void dispose() {

		TourDatabase.getInstance().removePropertyListener(fTourChangeListener);
		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	@SuppressWarnings("unchecked") //$NON-NLS-1$
	private void fillContextMenu(IMenuManager menuMgr) {

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(StructuredSelection selection) {

		// a chart must be available
		if (fTourChart == null) {

			TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if (tourChart == null || tourChart.isDisposed()) {
				return;
			} else {
				fTourChart = tourChart;
			}
		}

		Object[] segments = selection.toArray();

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

	private void onSelectionChanged(ISelection selection) {

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the marker viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			fTourData = tourDataSelection.getTourData();

			if (fTourData == null) {

				// hide the marker editor

				fPageBook.showPage(fPageNoChart);
				fTourChart = null;
			} else {

				// show the markers for the given tour

				fMarkerViewer.setInput(this);
				fPageBook.showPage(fViewerContainer);

				fTourChart = tourDataSelection.getTourChart();
			}

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			if (tourData != null) {
				fTourData = tourData;
				fMarkerViewer.setInput(this);
				fPageBook.showPage(fViewerContainer);
			}

			fTourChart = null;

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
					fMarkerViewer.setInput(this);
					fPageBook.showPage(fViewerContainer);

					fTourChart = tourChart;
				}
			}
		}
	}

	@Override
	public void setFocus() {
		fMarkerViewer.getTable().setFocus();
	}

}
