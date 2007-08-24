/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart {

	public static final String		ID						= "net.tourbook.views.TourMarkerView";				//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private TableViewer				fMarkerViewer;

	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;
	private PostSelectionProvider	fPostSelectionProvider;

	private final NumberFormat		fNF						= NumberFormat.getNumberInstance();
	private final DateFormat		fDF						= DateFormat.getTimeInstance(DateFormat.DEFAULT);
	final Calendar					fCalendar				= GregorianCalendar.getInstance();

//	private boolean				fIsMarkerDirty;

//	private ActionEditMarker		fActionEditMarker;
//	private ActionDeleteMarker		fActionDeleteMarker;

	private PageBook				fPageBook;
	private Label					fPageNoChart;
	private Composite				fPageViewer;

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

	class MarkerViewerLabelProvider extends CellLabelProvider {

		@Override
		public void update(ViewerCell cell) {

			TourMarker tourMarker = (TourMarker) cell.getElement();

			switch (cell.getColumnIndex()) {

			case COLUMN_TIME:
				int time = tourMarker.getTime();
				fCalendar.set(0, 0, 0, time / 3600, ((time % 3600) / 60), ((time % 3600) % 60));
				cell.setText(fDF.format(fCalendar.getTime()));
				break;

			case COLUMN_DISTANCE:
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) tourMarker.getDistance()) / 1000));

				if (tourMarker.getType() == ChartMarker.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
				break;

			case COLUMN_REMARK:
				cell.setText(tourMarker.getLabel());
				break;

			case COLUMN_X_OFFSET:
				cell.setText(Integer.toString(tourMarker.getLabelXOffset()));
				break;

			case COLUMN_Y_OFFSET:
				cell.setText(Integer.toString(tourMarker.getLabelYOffset()));
				break;

			case COLUMN_VISUAL_POSITION:
				int visualPosition = tourMarker.getVisualPosition();
				if (visualPosition == -1
						|| visualPosition >= TourMarker.visualPositionLabels.length) {
					cell.setText(TourMarker.visualPositionLabels[0]);
				} else {
					cell.setText(TourMarker.visualPositionLabels[visualPosition]);
				}
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Sort the markers by time
	 */
	private class MarkerViewerSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
		}
	}

//	class PositionEditor extends EditingSupport {
//
//		public PositionEditor(ColumnViewer viewer) {
//			super(viewer);
//		}
//
//		@Override
//		protected boolean canEdit(Object element) {
//			return true;
//		}
//
//		@Override
//		protected CellEditor getCellEditor(Object element) {
//
//			ComboBoxCellEditor positionEditor = new ComboBoxCellEditor(fMarkerViewer.getTable(),
//					TourMarker.visualPositionLabels,
//					SWT.READ_ONLY);
//
//			((CCombo) positionEditor.getControl()).setVisibleItemCount(20);
//
//			return positionEditor;
//		}
//
//		@Override
//		protected Object getValue(Object element) {
//			return ((TourMarker) element).getVisualPosition();
//		}
//
//		@Override
//		protected void setValue(Object element, Object value) {
//
//			TourMarker marker = (TourMarker) element;
//			int newValue = (Integer) value;
//
//			// check if position was modified
//			if (newValue != marker.getVisualPosition()) {
//
//				marker.setVisualPosition(newValue);
//
//				updateChangedMarker(marker);
//			}
//		}
//	}

//	class RemarkEditor extends EditingSupport {
//
//		public RemarkEditor(ColumnViewer viewer) {
//			super(viewer);
//		}
//
//		@Override
//		protected boolean canEdit(Object element) {
//			return true;
//		}
//
//		@Override
//		protected CellEditor getCellEditor(Object element) {
//			return new TextCellEditor(fMarkerViewer.getTable());
//		}
//
//		@Override
//		protected Object getValue(Object element) {
//			return ((TourMarker) element).getLabel();
//		}
//
//		@Override
//		protected void setValue(Object element, Object value) {
//
//			TourMarker marker = (TourMarker) element;
//			String newValue = (String) value;
//
//			// check if marker was modified
//			if (newValue.equals(marker.getLabel()) == false) {
//
//				marker.setLabel(newValue);
//
////				updateChangedMarker(marker);
//			}
//		}
//	}

	public TourMarkerView() {
		super();
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
				if (propId == TourDatabase.PROPERTY_TOUR_IS_CHANGED) {
					fMarkerViewer.setInput(this);
				}
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourChangeListener);
	}

//	private void createActions() {
//		fActionEditMarker = new ActionEditMarker(this);
//		fActionDeleteMarker = new ActionDeleteMarker(this);
//	}

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

		fPageViewer = createTableViewer(fPageBook);

		createContextMenu();

		addSelectionListener();
		addTourChangeListener();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		/*
		 * get markers from current selection
		 */
		final ISelection curretSelection = getSite().getWorkbenchWindow()
				.getSelectionService()
				.getSelection();
		if (curretSelection instanceof SelectionActiveEditor) {
			onSelectionChanged(curretSelection);
		} else {
			IEditorPart activeEditor = getSite().getPage().getActiveEditor();
			if (activeEditor != null) {
				onSelectionChanged(new SelectionActiveEditor(activeEditor));
			}
		}
	}

	private Composite createTableViewer(Composite parent) {

		TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableLayouter.setLayoutData(gridData);

		/*
		 * create table
		 */
		Table table = new Table(tableLayouter, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

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
							fMarkerViewer.editElement(selection.getFirstElement(),
									COLUMN_VISUAL_POSITION);
						} else {
							if (fMarkerViewer.isCellEditorActive() == false) {
								fMarkerViewer.editElement(selection.getFirstElement(),
										COLUMN_REMARK);
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
		PixelConverter pixelConverter = new PixelConverter(table);

		final MarkerViewerLabelProvider labelProvider = new MarkerViewerLabelProvider();

		// column: time
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.setLabelProvider(labelProvider);
		tvc.getColumn().setText(Messages.TourMarker_Column_time);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12),
				false));

		// column: km
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.TourMarker_Column_km);
		tvc.getColumn().setToolTipText(Messages.TourMarker_Column_km_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(8),
				false));

		// column: remark
		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvc.getColumn().setText(Messages.TourMarker_Column_remark);
		tvc.setLabelProvider(labelProvider);
//		tvc.setEditingSupport(new RemarkEditor(fMarkerViewer));
		tableLayouter.addColumnData(new ColumnWeightData(50, true));

		// column: position
		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvc.getColumn().setText(Messages.TourMarker_Column_position);
		tvc.setLabelProvider(labelProvider);
//		tvc.setEditingSupport(new PositionEditor(fMarkerViewer));
		tableLayouter.addColumnData(new ColumnWeightData(30, true));

		// column: horizontal offset
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.TourMarker_Column_horizontal_offset);
		tvc.getColumn().setToolTipText(Messages.TourMarker_Column_horizontal_offset_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(6),
				false));

		// column: vertical offset
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.TourMarker_Column_vertical_offset);
		tvc.getColumn().setToolTipText(Messages.TourMarker_Column_vertical_offset_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(6),
				false));

		/*
		 * create table viewer
		 */

		fMarkerViewer.setContentProvider(new MarkerViewerContentProvicer());
		fMarkerViewer.setLabelProvider(labelProvider);
		fMarkerViewer.setSorter(new MarkerViewerSorter());

//		fMarkerViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				editMarker();
//			}
//		});

		fMarkerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});
		return tableLayouter;
	}

	@Override
	public void dispose() {

		TourDatabase.getInstance().removePropertyListener(fTourChangeListener);
		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		super.dispose();
	}

//	/**
//	 * edit the marker label
//	 */
//	void editMarker() {
//
//		IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();
//
//		if (selection.size() == 0) {
//			return;
//		}
//
//		TourMarker selectedMarker = (TourMarker) selection.getFirstElement();
//
//		(new MarkerDialog(Display.getCurrent().getActiveShell(), fTourData, selectedMarker)).open();
//
//		// force the tour to be saved
////		fTourChart.setTourDirty(true);
//
//		// update chart
////		fTourChart.updateMarkerLayer(true);
//
//		// update the viewer
//		fMarkerViewer.refresh();
//
//		// update marker list and other listener
////		fTourChart.fireTourChartSelection();
//
//		setFocus();
//	}

	@SuppressWarnings("unchecked")
	private void fillContextMenu(IMenuManager menuMgr) {

//		IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();
//
//		/*
//		 * action: edit marker
//		 */
//		menuMgr.add(fActionEditMarker);
//		fActionEditMarker.setEnabled(markerSelection.isEmpty() == false);
//
//		/*
//		 * action: delete marker
//		 */
//		menuMgr.add(fActionDeleteMarker);
//
//		// check if custom markers are selected
//		boolean isCustomMarker = false;
//		for (Iterator<TourMarker> iter = markerSelection.iterator(); iter.hasNext();) {
//			TourMarker tourMarker = iter.next();
//			if (tourMarker.getType() != ChartMarker.MARKER_TYPE_DEVICE) {
//				isCustomMarker = true;
//				break;
//			}
//		}
//
//		fActionDeleteMarker.setEnabled(isCustomMarker);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(StructuredSelection selection) {

		// a chart must be available
		if (fTourChart == null) {
			return;
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
		} else {
			/*
			 * no markers are selected, move the markers to start/end position
			 */
//			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
//					fTourChart,
//					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION,
//					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
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
				fPageBook.showPage(fPageViewer);

				fTourChart = tourDataSelection.getTourChart();
			}

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			final TourData tourData = TourManager.getInstance()
					.getTourData(tourIdSelection.getTourId());

			if (tourData != null) {
				fTourData = tourData;
				fMarkerViewer.setInput(this);
				fPageBook.showPage(fPageViewer);
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
					fPageBook.showPage(fPageViewer);

					fTourChart = tourChart;
				}
			}
		}
	}

//	/**
//	 * remove selected markers from the view and update dependened structures
//	 */
//	@SuppressWarnings("unchecked")
//	void removeSelectedMarkers() {
//
//		IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();
//
//		/*
//		 * get a list of markers which can be removed, device markers can't be removed
//		 */
//		ArrayList<TourMarker> removedMarkers = new ArrayList<TourMarker>();
//		for (Iterator<TourMarker> iter = markerSelection.iterator(); iter.hasNext();) {
//			TourMarker tourMarker = iter.next();
//			if (tourMarker.getType() != ChartMarker.MARKER_TYPE_DEVICE) {
//				removedMarkers.add(tourMarker);
//			}
//		}
//
//		// update data model
//		fTourData.getTourMarkers().removeAll(removedMarkers);
//
//		// update the viewer
//		fMarkerViewer.remove(removedMarkers.toArray());
//
//		// update chart
////		fTourChart.updateMarkerLayer(true);
//
//		fIsMarkerDirty = true;
//	}

//	private void saveMarker() {
//		if (fIsMarkerDirty && fTourData != null) {
//			TourDatabase.saveTour(fTourData);
//		}
//
//		fIsMarkerDirty = false;
//	}

	@Override
	public void setFocus() {
		fMarkerViewer.getTable().setFocus();
	}

//	/**
//	 * Update the viewer and chart after the marker was changed
//	 * 
//	 * @param tourMarker
//	 */
//	private void updateChangedMarker(TourMarker tourMarker) {
//
//		// update viewer
//		fMarkerViewer.update(tourMarker, null);
//
//		// update chart
////		fTourChart.updateMarkerLayer(true);
//
//		fIsMarkerDirty = true;
//	}

}
