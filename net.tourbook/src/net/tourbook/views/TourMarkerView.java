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
package net.tourbook.views;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
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
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart {

	private static final String		MARKER_REMARK			= "remark";									//$NON-NLS-1$
	private static final String		MARKER_POSITION			= "position";									//$NON-NLS-1$
	private static final String		MARKER_X_OFFSET			= "x-offset";
	private static final String		MARKER_Y_OFFSET			= "y-offset";

	public static final String		ID						= "net.tourbook.views.TourMarkerView";			//$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_REMARK			= 2;
	public static final int			COLUMN_VISUAL_POSITION	= 3;
	public static final int			COLUMN_X_OFFSET			= 4;
	public static final int			COLUMN_Y_OFFSET			= 5;

	private static final String[]	COLUMN_PROPERTIES		= new String[] { "time", //$NON-NLS-1$
			"distance", //$NON-NLS-1$
			MARKER_REMARK,
			MARKER_POSITION,
			MARKER_X_OFFSET,
			MARKER_Y_OFFSET								};

	private TableViewer				fMarkerViewer;

	private TourChart				fTourChart;
	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;
	private PostSelectionProvider	fPostSelectionProvider;

	private NumberFormat			fNF						= NumberFormat.getNumberInstance();
	private final DateFormat		fDF						= DateFormat
																	.getTimeInstance(DateFormat.DEFAULT);
	final Calendar					fCalendar				= GregorianCalendar.getInstance();

	private boolean					fIsMarkerDirty;

	private ActionDeleteMarker		fActionDeleteMarker;
	private TextCellEditor			fLabelEditor;

	private PageBook				fPageBook;
	private Label					fPageNoChart;
	private Composite				fPageViewer;

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

	private class MarkerViewerSorter extends ViewerSorter {

		/**
		 * Sort the markers by time
		 */
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
		}
	}

	public TourMarkerView() {
		super();
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (selection instanceof SelectionTourChart) {

					saveMarker();

					// a tour was selected, update the marker viewer

					fTourChart = ((SelectionTourChart) selection).getTourChart();

					if (fTourChart == null) {

						// hide the marker editor

						fPageBook.showPage(fPageNoChart);

					} else {

						// show the markers for the given tour

						fTourData = fTourChart.getTourData();
						fMarkerViewer.setInput(this);

						fPageBook.showPage(fPageViewer);
					}
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void createActions() {
		fActionDeleteMarker = new ActionDeleteMarker(this);
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

	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fPageViewer = createTableViewer(fPageBook);

		createActions();
		createContextMenu();
		addSelectionListener();

		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());
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

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		PixelConverter pixelConverter = new PixelConverter(table);

		final MarkerViewerLabelProvider labelProvider = new MarkerViewerLabelProvider();
		fMarkerViewer = new TableViewer(table);

		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		// tvc.setEditingSupport(new EditingSupport());
		tvc.setLabelProvider(labelProvider);

		tvc.getColumn().setText(Messages.TourMarker_Column_time);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter
				.convertWidthInCharsToPixels(12), false));

		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.TourMarker_Column_km);
		tvc.getColumn().setToolTipText(Messages.TourMarker_Column_km_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter
				.convertWidthInCharsToPixels(8), false));

		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvc.getColumn().setText(Messages.TourMarker_Column_remark);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnWeightData(50, true));

		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvc.getColumn().setText(Messages.TourMarker_Column_position);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnWeightData(20, true));

		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText("H");
		tvc.getColumn().setToolTipText("Horizontal offset");
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter
				.convertWidthInCharsToPixels(8), false));

		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText("V");
		tvc.getColumn().setToolTipText("Vertical offset");
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter
				.convertWidthInCharsToPixels(8), false));

		/*
		 * create table viewer
		 */

		fMarkerViewer.setContentProvider(new MarkerViewerContentProvicer());
		fMarkerViewer.setLabelProvider(labelProvider);
		fMarkerViewer.setSorter(new MarkerViewerSorter());
		fMarkerViewer.setColumnProperties(COLUMN_PROPERTIES);

		fLabelEditor = new TextCellEditor(table);

		fMarkerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editMarker();
			}
		});

		ComboBoxCellEditor positionEditor = new ComboBoxCellEditor(
				table,
				TourMarker.visualPositionLabels,
				SWT.READ_ONLY);
		((CCombo) positionEditor.getControl()).setVisibleItemCount(20);

		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {

				IStructuredSelection selection = (IStructuredSelection) fMarkerViewer
						.getSelection();

				if (selection.size() > 0) {
					if (e.keyCode == SWT.CR) {
						if (e.stateMask == SWT.CONTROL) {
							// edit visual position
							fMarkerViewer.editElement(
									selection.getFirstElement(),
									COLUMN_VISUAL_POSITION);
						} else {
							editMarker();
						}
					}
				}
			}
		});

		fMarkerViewer.setCellEditors(new CellEditor[] {
				null,
				null,
				fLabelEditor,
				positionEditor,
				null,
				null, });

		fMarkerViewer.setCellModifier(new ICellModifier() {

			public boolean canModify(Object element, String property) {
				return true;
			}

			public Object getValue(Object element, String property) {

				TourMarker tourMarker = (TourMarker) element;

				if (MARKER_REMARK.equals(property)) {
					return tourMarker.getLabel();
				} else if (MARKER_POSITION.equals(property)) {
					return tourMarker.getVisualPosition();
				} else {
					return null;
				}
			}

			public void modify(Object element, String property, Object value) {

				if (element instanceof Item) {
					element = ((Item) element).getData();

					if (element instanceof TourMarker) {
						TourMarker marker = (TourMarker) element;

						boolean isModified = false;

						if (MARKER_REMARK.equals(property)) {

							// check if marker was modified
							String newValue = (String) value;
							if (newValue.equals(marker.getLabel()) == false) {
								marker.setLabel(newValue);
								isModified = true;
							}

						} else if (MARKER_POSITION.equals(property)) {

							// check if position was modified
							int newValue = (Integer) value;
							if (newValue != marker.getVisualPosition()) {
								marker.setVisualPosition(newValue);
								isModified = true;
							}
						}

						if (isModified) {
							// update viewer
							fMarkerViewer.update(element, null);

							// update chart
							fTourChart.updateMarkerLayer(true);

							fIsMarkerDirty = true;
						}
					}
				}
			}
		});

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

	public void dispose() {

		saveMarker();

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		super.dispose();
	}

	/**
	 * edit the marker label
	 */
	private void editMarker() {

		IStructuredSelection selection = (IStructuredSelection) fMarkerViewer.getSelection();

		if (selection.size() > 0) {

			if (fMarkerViewer.isCellEditorActive() == false) {
				fMarkerViewer.editElement(selection.getFirstElement(), COLUMN_REMARK);

				// unselect text
				// Text textControl = ((Text) fLabelEditor.getControl());
				// textControl.setSelection(textControl.getText().length());
			}
		}
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();

		menuMgr.add(fActionDeleteMarker);

		/*
		 * check if custom markers are selected
		 */
		boolean isCustomMarker = false;
		for (Iterator iter = markerSelection.iterator(); iter.hasNext();) {
			TourMarker tourMarker = (TourMarker) iter.next();
			if (tourMarker.getType() != ChartMarker.MARKER_TYPE_DEVICE) {
				isCustomMarker = true;
				break;
			}
		}

		fActionDeleteMarker.setEnabled(isCustomMarker);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public Object getMarkerViewer() {
		return fMarkerViewer;
	}

	/**
	 * remove selected markers from the view and update dependened structures
	 */
	void removeSelectedMarkers() {

		IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();

		/*
		 * get a list of markers which can be removed, device markers can't be removed
		 */
		ArrayList<TourMarker> removedMarkers = new ArrayList<TourMarker>();
		for (Iterator iter = markerSelection.iterator(); iter.hasNext();) {
			TourMarker tourMarker = (TourMarker) iter.next();
			if (tourMarker.getType() != ChartMarker.MARKER_TYPE_DEVICE) {
				removedMarkers.add(tourMarker);
			}
		}

		// update data model
		fTourData.getTourMarkers().removeAll(removedMarkers);

		// update the viewer
		fMarkerViewer.remove(removedMarkers.toArray());

		// update chart
		fTourChart.updateMarkerLayer(true);

		fIsMarkerDirty = true;
	}

	private void saveMarker() {
		if (fIsMarkerDirty && fTourData != null) {
			TourDatabase.saveTour(fTourData);
		}

		fIsMarkerDirty = false;
	}

	public void setFocus() {
		fMarkerViewer.getTable().setFocus();
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(StructuredSelection selection) {

		Object[] segments = selection.toArray();

		if (segments.length > 1) {

			// two or more markers are selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					fTourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					((TourMarker) segments[segments.length - 1]).getSerieIndex()));

		} else if (segments.length > 0) {

			// one marker is selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					fTourChart,
					((TourMarker) segments[0]).getSerieIndex(),
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		} else {
			/*
			 * no markers are selected, move the markers to start/end position
			 */
			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					fTourChart,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

}
