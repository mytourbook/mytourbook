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
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.algorithm.Point;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSegment;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourSegmenterView extends ViewPart {

	public static final String		ID						= "net.tourbook.views.TourSegmenter"; //$NON-NLS-1$

	public static final int			COLUMN_TIME				= 0;
	public static final int			COLUMN_DISTANCE			= 1;
	public static final int			COLUMN_ALTITUDE			= 2;
	public static final int			COLUMN_SPEED			= 3;
	public static final int			COLUMN_GRADIENT			= 4;
	public static final int			COLUMN_ALTITUDE_UP		= 5;
	public static final int			COLUMN_ALTITUDE_DOWN	= 6;

	private PageBook				fPageBook;
	private Composite				fPageSegmenter;

	private Button					fChkShowInChart;
	private Scale					fScaleTolerance;
	private Label					fLabelToleranceValue;

	private TableViewer				fTableViewer;

	private TourChart				fTourChart;
	private TourData				fTourData;

	private int						fDpTolerance;
	private int						fSavedDpTolerance;

	private ISelectionListener		fPostSelectionListener;
	private PostSelectionProvider	fPostSelectionProvider;
	private IPartListener			fPartListener;

	private DateFormat				fTimeInstance			= DateFormat
																	.getTimeInstance(DateFormat.DEFAULT);

	private Label	fPageNoChart;

	/**
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {

		public ViewContentProvider() {}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}

		public void dispose() {}

		public Object[] getElements(Object parent) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.createTourSegments();
			}
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object obj, int index) {

			if (obj == null) {
				return null;
			}

			final TourSegment segment = (TourSegment) obj;

			final NumberFormat nf = NumberFormat.getNumberInstance();
			final Calendar calendar = GregorianCalendar.getInstance();

			switch (index) {

			case COLUMN_TIME:
				calendar.set(
						0,
						0,
						0,
						segment.drivingTime / 3600,
						((segment.drivingTime % 3600) / 60),
						((segment.drivingTime % 3600) % 60));

				return fTimeInstance.format(calendar.getTime());

			case COLUMN_DISTANCE:
				nf.setMinimumFractionDigits(2);
				nf.setMaximumFractionDigits(2);
				return nf.format(((float) segment.distance) / 1000);

			case COLUMN_ALTITUDE:
				nf.setMinimumFractionDigits(0);
				return nf.format(segment.altitude);

			case COLUMN_SPEED:
				nf.setMinimumFractionDigits(1);
				nf.setMaximumFractionDigits(1);

				if (segment.drivingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					return nf.format(segment.speed);
				}

			case COLUMN_ALTITUDE_UP:

				nf.setMinimumFractionDigits(1);
				nf.setMaximumFractionDigits(0);

				if (segment.drivingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					float result = (float) (segment.altitudeUp) / segment.drivingTime * 3600;
					if (result == 0) {
						return ""; //$NON-NLS-1$
					} else {
						return nf.format(result);
					}
				}

			case COLUMN_ALTITUDE_DOWN:

				nf.setMinimumFractionDigits(1);
				nf.setMaximumFractionDigits(0);

				if (segment.drivingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					float result = (float) (segment.altitudeDown) / segment.drivingTime * 3600;
					if (result == 0) {
						return ""; //$NON-NLS-1$
					} else {
						return nf.format(result);
					}
				}

			case COLUMN_GRADIENT:
				nf.setMinimumFractionDigits(1);
				nf.setMaximumFractionDigits(1);
				return nf.format(segment.gradient);

			default:
				break;
			}

			return (getText(obj));
		}

		public Image getColumnImage(Object element, int columnIndex) {

			return null;
		}
	}

	private class ViewSorter extends ViewerSorter {

		// private static final int ASCENDING = 0;

		private static final int	DESCENDING	= 1;

		private int					column;

		private int					direction;

		/**
		 * Does the sort. If it's a different column from the previous sort, do
		 * an ascending sort. If it's the same column as the last sort, toggle
		 * the sort direction.
		 * 
		 * @param column
		 */
		public void sortColumn(int column) {

			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an descending sort
				this.column = column;
				direction = DESCENDING;
			}
		}

		/**
		 * Compares the object for sorting
		 */
		public int compare(Viewer viewer, Object obj1, Object obj2) {

			TourSegment segment1 = ((TourSegment) obj1);
			TourSegment segment2 = ((TourSegment) obj2);

			int rc = 0;

			// Determine which column and do the appropriate sort
			switch (column) {
			case COLUMN_GRADIENT:
				rc = (int) ((segment1.gradient - segment2.gradient) * 100);
				break;

			case COLUMN_SPEED:
				rc = (int) ((segment1.speed - segment2.speed) * 100);
				break;
			}

			// If descending order, flip the direction
			if (direction == DESCENDING)
				rc = -rc;

			return rc;
		}
	}

	/**
	 * Constructor
	 */
	public TourSegmenterView() {
		super();
	}

	private void addSelectionListener() {
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionTourChart) {
					onTourSelection((SelectionTourChart) selection);
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addPartListener() {

		// listen to part events
		fPartListener = new IPartListener() {

			public void partActivated(IWorkbenchPart part) {}
			public void partBroughtToTop(IWorkbenchPart part) {}
			public void partDeactivated(IWorkbenchPart part) {}
			public void partOpened(IWorkbenchPart part) {}

			public void partClosed(IWorkbenchPart part) {

				if (fTourChart != null) {
					// hide the tour segments
					fTourChart.updateSegmentLayer(false);
				}
			}

		};

		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
	}

	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fPageSegmenter = new Composite(fPageBook, SWT.NONE);
		fPageSegmenter.setLayout(new GridLayout());

		createSegmenterLayout(fPageSegmenter);
		createTableViewer(fPageSegmenter);

		addSelectionListener();
		addPartListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());
	}

	private void createSegmenterLayout(Composite parent) {

		Composite segmentContainer = new Composite(parent, SWT.NONE);
		segmentContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		segmentContainer.setLayout(new GridLayout(3, false));

		GridData gd;

		fChkShowInChart = new Button(segmentContainer, SWT.CHECK);
		fChkShowInChart.setText(Messages.TourSegmenter_Check_show_segments_in_chart);
		fChkShowInChart.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 3;
		fChkShowInChart.setLayoutData(gd);

		fChkShowInChart.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.Selection) {
					fireSegmentLayerSelection();
				}
			}
		});

		Label label = new Label(segmentContainer, SWT.NONE);
		label.setText(Messages.TourSegmenter_Label_tolerance);

		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = GridData.CENTER;
		fScaleTolerance = new Scale(segmentContainer, SWT.HORIZONTAL);
		fScaleTolerance.setMaximum(100);
		fScaleTolerance.setLayoutData(gd);
		fScaleTolerance.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChangeTolerance(getTolerance(), false);
			}
		});

		fScaleTolerance.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
				onChangeTolerance(getTolerance(), false);
			}
		});

		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.CENTER;
		gd.widthHint = 30;
		fLabelToleranceValue = new Label(segmentContainer, SWT.NONE);
		fLabelToleranceValue.setText(Messages.TourSegmenter_Label_default_tolerance);
		fLabelToleranceValue.setLayoutData(gd);
	}

	private void createTableViewer(Composite parent) {

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
		TableColumn tc;
		PixelConverter pixelConverter = new PixelConverter(table);

		// the first column is always left aligned
		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_time);
		tc.setToolTipText(Messages.TourSegmenter_Column_time_tooltip);
		tableLayouter.addColumnData(UI.getColumnPixelWidth(pixelConverter, 11));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_distance);
		tc.setToolTipText(Messages.TourSegmenter_Column_distance_tooltip);
		tableLayouter.addColumnData(UI.getColumnPixelWidth(pixelConverter, 10));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_altitude);
		tc.setToolTipText(Messages.TourSegmenter_Column_altitude_tooltip);
		tableLayouter.addColumnData(UI.getColumnPixelWidth(pixelConverter, 10));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_speed);
		tc.setToolTipText(Messages.TourSegmenter_Column_speed_tooltip);
		tableLayouter.addColumnData(UI.getColumnPixelWidth(pixelConverter, 9));
		
		tc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((ViewSorter) fTableViewer.getSorter()).sortColumn(COLUMN_SPEED);
				fTableViewer.refresh();
			}
		});

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_gradient);
		tc.setToolTipText(Messages.TourSegmenter_Column_gradient_tooltip);
		tableLayouter.addColumnData(UI.getColumnPixelWidth(pixelConverter, 8));
		tc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((ViewSorter) fTableViewer.getSorter()).sortColumn(COLUMN_GRADIENT);
				fTableViewer.refresh();
			}
		});

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_altimeter_up);
		tc.setToolTipText(Messages.TourSegmenter_Column_altimeter_up_tooltip);
		tableLayouter.addColumnData(new ColumnWeightData(5, true));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.TourSegmenter_Column_altimeter_down);
		tc.setToolTipText(Messages.TourSegmenter_Column_altimeter_down_tooltip);
		tableLayouter.addColumnData(new ColumnWeightData(5, true));

		/*
		 * create table viewer
		 */
		fTableViewer = new TableViewer(table);

		fTableViewer.setContentProvider(new ViewContentProvider());
		fTableViewer.setLabelProvider(new ViewLabelProvider());
		fTableViewer.setSorter(new ViewSorter());

		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {

				StructuredSelection selection = (StructuredSelection) event.getSelection();

				if (selection != null) {

					/*
					 * select the chart sliders according to the selected
					 * segment(s)
					 */

					Object[] segments = selection.toArray();

					if (segments.length > 0) {

						fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
								fTourChart,
								((TourSegment) (segments[0])).serieIndexStart,
								((TourSegment) (segments[segments.length - 1])).serieIndexEnd));
					}
				}
			}
		});
	}
	private void onChangeTolerance(int dpTolerance, boolean forceRecalc) {

		// update label in the ui
		fLabelToleranceValue.setText(Integer.toString(dpTolerance));

		if (fTourData == null || (fDpTolerance == dpTolerance && forceRecalc == false)) {
			return;
		}

		fDpTolerance = dpTolerance;

		// update tolerance into the tour data
		fTourData.setDpTolerance((short) dpTolerance);

		// create points for the simplifier from distance and altitude
		int[] distanceSerie = fTourData.distanceSerie;
		int[] altitudeSerie = fTourData.altitudeSerie;

		Point graphPoints[] = new Point[distanceSerie.length];
		for (int iPoint = 0; iPoint < graphPoints.length; iPoint++) {
			graphPoints[iPoint] = new Point(distanceSerie[iPoint], altitudeSerie[iPoint], iPoint);
		}

		DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(
				dpTolerance,
				graphPoints);
		Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */
		fTourData.segmentSerieIndex = new int[simplePoints.length];

		final int[] segmentSerieIndex = fTourData.segmentSerieIndex;

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			Point point = (Point) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}

		// update table and create the tour segments in tour data
		setTableInput();

		fireSegmentLayerSelection();
	}

	private int getTolerance() {
		return (int) ((Math.pow(fScaleTolerance.getSelection(), 2.05)) / (double) 50.0);
	}

	private void fireSegmentLayerSelection() {

		// show the segments in the chart
		fPostSelectionProvider.setSelection(new SelectionTourSegmentLayer(fChkShowInChart
				.getSelection()));

		// move the markers to start/end position
		fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(
				fTourChart,
				SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER,
				SelectionChartXSliderPosition.SLIDER_POSITION_AT_CHART_BORDER));

	}

	private void setTableInput() {

		// disposeCustomCellRenderer();

		fTableViewer.setInput(this);

		// setCustomCellRenderer();
	}

	/**
	 * handle a tour selection event
	 * 
	 * @param tourChartSelection
	 */
	private void onTourSelection(SelectionTourChart tourChartSelection) {

		saveDPTolerance();

		fTourChart = tourChartSelection.getTourChart();
		
		if (fTourChart == null) {
			// hide the segmenter
			fPageBook.showPage(fPageNoChart);
			return;
		}
		
		fPageBook.showPage(fPageSegmenter);
		
		fTourData = fTourChart.getTourData();

		if (fTourData == null) {
			fPageBook.showPage(fPageNoChart);
			return;
		}
		// update segmenter values
		fSavedDpTolerance = fDpTolerance = fTourData.getDpTolerance();

		float factor = 1 / 2.05f;

		double tolerance = Math.pow(fDpTolerance * 50, factor);

		fScaleTolerance.setSelection((int) tolerance);
		fLabelToleranceValue.setText(Integer.toString(fTourData.getDpTolerance()));

		// force the segements to be rebuild for the current tour
		onChangeTolerance(fDpTolerance, true);

		/*
		 * update the chart, this can't be done in the selectionProvider because
		 * currently the segmenter part is inactive
		 */
		fTourChart.updateSegmentLayer(fChkShowInChart.getSelection());
	}

	/**
	 * save dp tolerance when it was changed
	 */
	private void saveDPTolerance() {
		if (fTourData != null && fSavedDpTolerance != fTourData.getDpTolerance()) {
			TourDatabase.saveTour(fTourData);
		}
	}

	public void dispose() {

		saveDPTolerance();

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		getSite().setSelectionProvider(null);

		super.dispose();
	}

	public void setFocus() {
		fScaleTolerance.setFocus();
	}

	// private void disposeCustomCellRenderer() {
	//
	// for (TableEditor editor : editorList) {
	//
	// // dispose editor canvas
	// editor.getEditor().dispose();
	//
	// editor.dispose();
	// }
	//
	// editorList.clear();
	// }
	//
	// private void setCustomCellRenderer() {
	//
	// final Table table = tableViewer.getTable();
	// final Display display = Display.getCurrent();
	// final Color blue = display.getSystemColor(SWT.COLOR_DARK_BLUE);
	// final Color white = display.getSystemColor(SWT.COLOR_WHITE);
	//
	// TableItem[] items = table.getItems();
	//
	// for (int i = 0; i < items.length; i++) {
	//
	// TableItem tableItem = items[i];
	// TableEditor editor = new TableEditor(table);
	// editor.grabHorizontal = true;
	// editor.grabVertical = true;
	//
	// final Canvas canvas = new Canvas(table, SWT.NONE);
	//
	// canvas.setData("EXAMPLE DATA", new Integer(i * 100 / items.length));
	//
	// canvas.addPaintListener(new PaintListener() {
	//
	// public void paintControl(PaintEvent e) {
	//
	// Rectangle area = canvas.getClientArea();
	// Integer data = (Integer) canvas.getData("EXAMPLE DATA");
	// if (data == null)
	// return;
	//
	// e.gc.setBackground(table.getBackground());
	// e.gc.fillRectangle(area.x, area.y, area.width, area.height);
	//
	// e.gc.setBackground(blue);
	// e.gc.setForeground(white);
	//
	// e.gc.fillRectangle(
	// area.x,
	// area.y,
	// (int) (data.doubleValue() * area.width / 100.0),
	// area.height);
	// // e.gc.fillGradientRectangle(area.x, area.y, (int)
	// // (data.doubleValue()
	// // * area.width / 100.0), area.height, false);
	// }
	// });
	// editor.setEditor(canvas, tableItem, 1);
	// editorList.add(editor);
	// }
	//
	// }

	// private void hookUpListener() {
	//
	// // listen to part events
	// partListener = new IPartListener() {
	//
	// public void partActivated(IWorkbenchPart part) {
	//
	// // System.out.println("Activated: " + part);
	//
	// if (part instanceof TourEditorPart && part != currentTourEditor) {
	//
	// // set current editor
	// currentTourEditor = (TourEditorPart) part;
	//
	// updateSegmenterFromEditor();
	// }
	// }
	//
	// public void partBroughtToTop(IWorkbenchPart part) {
	// // System.out.println("BroughtToTop: " + part);
	// }
	//
	// public void partClosed(IWorkbenchPart part) {
	//
	// // System.out.println("Closed: " + part);
	//
	// /*
	// * when all tour editors are closed, the content of the tour
	// * segmenter will be hidden
	// */
	// IEditorReference[] editorRefs =
	// getSite().getPage().getEditorReferences();
	//
	// for (IEditorReference editorRef : editorRefs) {
	// IEditorPart editorPart = editorRef.getEditor(false);
	// if (editorPart instanceof TourEditorPart) {
	// // at least one tour editor is open
	// return;
	// }
	// }
	// currentTourEditor = null;
	// createEmptySegmenterLayout();
	//
	// }
	//
	// public void partDeactivated(IWorkbenchPart part) {
	// // System.out.println("Deactivated: " + part);
	// }
	//
	// public void partOpened(IWorkbenchPart part) {
	// // System.out.println("Opened: " + part);
	// }
	// };
	//
	// // register the listener in the page
	// getSite().getPage().addPartListener(partListener);
	// }

}
