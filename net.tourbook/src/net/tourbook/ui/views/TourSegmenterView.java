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

import net.tourbook.Messages;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.algorithm.Point;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSegment;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class TourSegmenterView extends ViewPart {

	public static final String		ID				= "net.tourbook.views.TourSegmenter";	//$NON-NLS-1$

//	public static final int			COLUMN_TIME				= 0;
//	public static final int			COLUMN_DISTANCE			= 1;
//	public static final int			COLUMN_ALTITUDE			= 2;
	public static final int			COLUMN_SPEED	= 3;
	public static final int			COLUMN_GRADIENT	= 4;
//	public static final int			COLUMN_ALTITUDE_UP		= 5;
//	public static final int			COLUMN_ALTITUDE_DOWN	= 6;

	private PageBook				fPageBook;
	private Composite				fPageSegmenter;
	private Composite				fViewerContainer;

	private Scale					fScaleTolerance;
	private Label					fLabelToleranceValue;
	private Label					fPageNoChart;
	private Label					fLblTitle;

	private TableViewer				fSegmentViewer;

	private TourChart				fTourChart;
	private TourData				fTourData;

	private int						fDpTolerance;
	private int						fSavedDpTolerance;

	private ISelectionListener		fPostSelectionListener;
	private IPartListener			fPartListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private PostSelectionProvider	fPostSelectionProvider;

	private final NumberFormat		fNf				= NumberFormat.getNumberInstance();
//	private final DateFormat		fTimeInstance	= DateFormat.getTimeInstance(DateFormat.DEFAULT);
//	private final Calendar			fCalendar		= GregorianCalendar.getInstance();

	private TourEditor				fTourEditor;

	private boolean					fShowSegmentsInChart;

	private ActionShowSegments		fActionShowSegments;

	/**
	 * The content provider class is responsible for providing objects to the view. It can wrap
	 * existing objects in adapters or simply return objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore it and always show the same content (like Task
	 * List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {

		public ViewContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.createTourSegments();
			}
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

//	private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
//
//		public Image getColumnImage(final Object element, final int columnIndex) {
//
//			return null;
//		}
//
//		public String getColumnText(final Object obj, final int index) {
//
//			if (obj == null) {
//				return null;
//			}
//
//			final TourSegment segment = (TourSegment) obj;
//
//			switch (index) {
//
//			case COLUMN_TIME:
//				fCalendar.set(0,
//						0,
//						0,
//						segment.drivingTime / 3600,
//						((segment.drivingTime % 3600) / 60),
//						((segment.drivingTime % 3600) % 60));
//
//				return fTimeInstance.format(fCalendar.getTime());
//
//			case COLUMN_DISTANCE:
//				fNf.setMinimumFractionDigits(2);
//				fNf.setMaximumFractionDigits(2);
//				return fNf.format(((float) segment.distance) / 1000);
//
//			case COLUMN_ALTITUDE:
//				fNf.setMinimumFractionDigits(0);
//				return fNf.format(segment.altitude);
//
//			case COLUMN_SPEED:
//				fNf.setMinimumFractionDigits(1);
//				fNf.setMaximumFractionDigits(1);
//
//				if (segment.drivingTime == 0) {
//					return ""; //$NON-NLS-1$
//				} else {
//					return fNf.format(segment.speed);
//				}
//
//			case COLUMN_ALTITUDE_UP:
//
//				fNf.setMinimumFractionDigits(1);
//				fNf.setMaximumFractionDigits(0);
//
//				if (segment.drivingTime == 0) {
//					return ""; //$NON-NLS-1$
//				} else {
//					final float result = (float) (segment.altitudeUp) / segment.drivingTime * 3600;
//					if (result == 0) {
//						return ""; //$NON-NLS-1$
//					} else {
//						return fNf.format(result);
//					}
//				}
//
//			case COLUMN_ALTITUDE_DOWN:
//
//				fNf.setMinimumFractionDigits(1);
//				fNf.setMaximumFractionDigits(0);
//
//				if (segment.drivingTime == 0) {
//					return ""; //$NON-NLS-1$
//				} else {
//					final float result = (float) (segment.altitudeDown) / segment.drivingTime * 3600;
//					if (result == 0) {
//						return ""; //$NON-NLS-1$
//					} else {
//						return fNf.format(result);
//					}
//				}
//
//			case COLUMN_GRADIENT:
//				fNf.setMinimumFractionDigits(1);
//				fNf.setMaximumFractionDigits(1);
//				return fNf.format(segment.gradient);
//
//			default:
//				break;
//			}
//
//			return (getText(obj));
//		}
//	}

	private class ViewSorter extends ViewerSorter {

		// private static final int ASCENDING = 0;

		private static final int	DESCENDING	= 1;

		private int					column;

		private int					direction;

		/**
		 * Compares the object for sorting
		 */
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			final TourSegment segment1 = ((TourSegment) obj1);
			final TourSegment segment2 = ((TourSegment) obj2);

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
			if (direction == DESCENDING) {
				rc = -rc;
			}

			return rc;
		}

		/**
		 * Does the sort. If it's a different column from the previous sort, do an ascending sort.
		 * If it's the same column as the last sort, toggle the sort direction.
		 * 
		 * @param column
		 */
		public void setSortColumn(final int column) {

			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an descending sort
				this.column = column;
				direction = DESCENDING;
			}
		}
	}

	/**
	 * Constructor
	 */
	public TourSegmenterView() {
		super();
	}

	private void addPartListener() {

		// listen to part events
		fPartListener = new IPartListener() {

			public void partActivated(final IWorkbenchPart part) {}

			public void partBroughtToTop(final IWorkbenchPart part) {}

			public void partClosed(final IWorkbenchPart part) {

				if (fTourChart != null) {
					// hide the tour segments
					fShowSegmentsInChart = false;
					fireSegmentLayerChanged();
				}
			}

			public void partDeactivated(final IWorkbenchPart part) {}

			public void partOpened(final IWorkbenchPart part) {}

		};

		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
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
					setTableInput();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fPageSegmenter = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 1).applyTo(fPageSegmenter);

		createSegmenterLayout(fPageSegmenter);

		fViewerContainer = new Composite(fPageSegmenter, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fViewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);
		createTableViewer(fViewerContainer);

		createMenus();

		addSelectionListener();
		addPartListener();
		addPrefListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fPageBook.showPage(fPageNoChart);

		// set default value, show segments in opened charts
		fShowSegmentsInChart = true;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private class ActionShowSegments extends Action {

		public ActionShowSegments() {

			super(Messages.App_Action_open_tour_segmenter, SWT.TOGGLE);
			setToolTipText(Messages.App_Action_open_tour_segmenter_tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_segmenter));
		}

		@Override
		public void run() {
			fShowSegmentsInChart = !fShowSegmentsInChart;
			fireSegmentLayerChanged();
		}
	}

	/**
	 * create view menu
	 */
	private void createMenus() {

		fActionShowSegments = new ActionShowSegments();

		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(fActionShowSegments);

	}

	private void createSegmenterLayout(final Composite parent) {

		GridData gd;
		Label label;

		final Composite segmentContainer = new Composite(parent, SWT.NONE);
		segmentContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(segmentContainer);

		/*
		 * tour title
		 */
		fLblTitle = new Label(segmentContainer, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.horizontalSpan = 3;
		fLblTitle.setLayoutData(gd);

		/*
		 * scale: tolerance
		 */
		label = new Label(segmentContainer, SWT.NONE);
		label.setText(Messages.Tour_Segmenter_Label_tolerance);

		fScaleTolerance = new Scale(segmentContainer, SWT.HORIZONTAL);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fScaleTolerance.setMaximum(100);
		fScaleTolerance.setLayoutData(gd);
		fScaleTolerance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onToleranceChanged(getTolerance(), false);
				setTourDirty();
			}
		});

		fScaleTolerance.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
				onToleranceChanged(getTolerance(), false);
				setTourDirty();
			}
		});

		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.CENTER;
		gd.widthHint = 30;
		fLabelToleranceValue = new Label(segmentContainer, SWT.NONE);
		fLabelToleranceValue.setText(Messages.Tour_Segmenter_Label_default_tolerance);
		fLabelToleranceValue.setLayoutData(gd);
	}

	private void createTableViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fSegmentViewer = new TableViewer(table);

		TableViewerColumn tvc;
		TableColumn tvcColumn;
		final PixelConverter pixelConverter = new PixelConverter(table);

		/*
		 * column: time
		 */
		// the first column is always left aligned
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Segmenter_Column_time);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_time_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
//		case COLUMN_TIME:
//		fCalendar.set(0,
//				0,
//				0,
//				segment.drivingTime / 3600,
//				((segment.drivingTime % 3600) / 60),
//				((segment.drivingTime % 3600) % 60));
//
//		return fTimeInstance.format(fCalendar.getTime());

				cell.setText(UI.formatSeconds(segment.drivingTime));
			}
		});
		tableLayout.setColumnData(tvcColumn, UI.getColumnPixelWidth(pixelConverter, 11));

		/*
		 * column: distance
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_DISTANCE);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_distance_tooltip);
		tableLayout.setColumnData(tvcColumn, UI.getColumnPixelWidth(pixelConverter, 10));
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(2);
				fNf.setMaximumFractionDigits(2);

				cell.setText(fNf.format(((float) segment.distance) / (1000 * UI.UNIT_VALUE_DISTANCE)));
			}
		});

		/*
		 * column: altitude
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_ALTITUDE);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_altitude_tooltip);
		tableLayout.setColumnData(tvcColumn, UI.getColumnPixelWidth(pixelConverter, 10));
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitude / UI.UNIT_VALUE_ALTITUDE));
			}
		});

		/*
		 * column: speed
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(UI.UNIT_LABEL_SPEED);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_speed_tooltip);
		tableLayout.setColumnData(tvcColumn, UI.getColumnPixelWidth(pixelConverter, 9));
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					cell.setText(fNf.format(segment.speed / UI.UNIT_VALUE_DISTANCE));
				}
				//

			}
		});

		tvcColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				fSegmentViewer.refresh();
			}
		});

		/*
		 * column: gradient
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tour_Segmenter_Column_gradient);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_gradient_tooltip);
		tableLayout.setColumnData(tvcColumn, UI.getColumnPixelWidth(pixelConverter, 8));
		tvcColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_GRADIENT);
				fSegmentViewer.refresh();
			}
		});
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);

				cell.setText(fNf.format(segment.gradient));
			}
		});

		/*
		 * column: altimeter up
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText("+ " + UI.UNIT_LABEL_ALTIMETER);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_altimeter_up_tooltip);
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(5, true));
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (float) (segment.altitudeUp / UI.UNIT_VALUE_ALTITUDE)
							/ segment.drivingTime
							* 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						fNf.setMinimumFractionDigits(1);
						fNf.setMaximumFractionDigits(0);
						cell.setText(fNf.format(result));
					}
				}
			}
		});

		/*
		 * column: altimeter down
		 */
		tvc = new TableViewerColumn(fSegmentViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText("- " + UI.UNIT_LABEL_ALTIMETER);
		tvcColumn.setToolTipText(Messages.Tour_Segmenter_Column_altimeter_down_tooltip);
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(5, true));
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (float) (segment.altitudeDown / UI.UNIT_VALUE_ALTITUDE)
							/ segment.drivingTime
							* 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						fNf.setMinimumFractionDigits(1);
						fNf.setMaximumFractionDigits(0);
						cell.setText(fNf.format(result));
					}
				}
			}
		});

		/*
		 * create table viewer
		 */

		fSegmentViewer.setContentProvider(new ViewContentProvider());
		fSegmentViewer.setSorter(new ViewSorter());

		fSegmentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final StructuredSelection selection = (StructuredSelection) event.getSelection();

				if (selection != null) {

					/*
					 * select the chart sliders according to the selected segment(s)
					 */

					final Object[] segments = selection.toArray();

					if (segments.length > 0) {
						final SelectionChartXSliderPosition selectionSliderPosition = new SelectionChartXSliderPosition(fTourChart,
								((TourSegment) (segments[0])).serieIndexStart,
								((TourSegment) (segments[segments.length - 1])).serieIndexEnd);
						fPostSelectionProvider.setSelection(selectionSliderPosition);
					}
				}
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	/**
	 * notify listeners to show/hide the segments
	 */
	private void fireSegmentLayerChanged() {

		// show/hide the segments in the chart
		TourManager.getInstance().firePropertyChange(TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGED,
				fShowSegmentsInChart);
	}

	/**
	 * try to get the tour chart and/or editor from the active part
	 */
	private void getActiveTourChart() {

		final IWorkbenchPage page = getSite().getPage();

		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null && activeEditor instanceof TourEditor) {

			final TourEditor tourEditor = (TourEditor) activeEditor;

			/*
			 * check if the tour data in the editor are the same as in the selection
			 */
			if (tourEditor.getTourChart().getTourData() == fTourData) {
				fTourEditor = tourEditor;
				fTourChart = fTourEditor.getTourChart();
				return;
			}
		}

		final IViewReference[] viewReferences = page.getViewReferences();
		for (IViewReference viewReference : viewReferences) {

			IViewPart view = viewReference.getView(false);
			if (view != null && view instanceof TourChartView) {

				final TourChartView tourChartView = ((TourChartView) view);

				/*
				 * check if the tour data in the editor are the same as in the selection
				 */
				if (tourChartView.getTourChart().getTourData() == fTourData) {
					fTourChart = tourChartView.getTourChart();
					return;
				}
			}
		}
	}

	private int getTolerance() {
		return (int) ((Math.pow(fScaleTolerance.getSelection(), 2.05)) / (double) 50.0);
	}

	/**
	 * handle a tour selection event
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editorPart = ((SelectionActiveEditor) selection).getEditor();

			if (editorPart instanceof TourEditor) {

				final TourEditor tourEditor = (TourEditor) editorPart;

				// check if editor changed
				if (tourEditor == fTourEditor) {
					return;
				}

				fTourEditor = tourEditor;
				fTourChart = tourEditor.getTourChart();
				fTourData = fTourChart.getTourData();

			} else {
				return;
			}

		} else if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;

			if (selectionTourData.getTourChart() == null) {
				return;
			}

			fTourEditor = null;

			fTourData = selectionTourData.getTourData();
			fTourChart = selectionTourData.getTourChart();

			if (fTourChart == null) {
				getActiveTourChart();
			}

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			if (fTourData != null) {
				if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
					// don't reload the same tour
					return;
				}
			}

			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			if (tourData != null) {

				fTourData = tourData;

				fTourEditor = null;
				fTourChart = null;
				getActiveTourChart();
			}

		} else {
			return;
		}

		if (fTourData == null) {
			fPageBook.showPage(fPageNoChart);
			return;
		}

		fPageBook.showPage(fPageSegmenter);

		// update tour title
		fLblTitle.setText(TourManager.getTourTitleDetailed(fTourData));
		fLblTitle.pack(true);

		// keep original dp tolerance
		fSavedDpTolerance = fDpTolerance = fTourData.getDpTolerance();

		// update segmenter values
		final float factor = 1 / 2.05f;
		final double tolerance = Math.pow(fDpTolerance * 50, factor);

		fScaleTolerance.setSelection((int) tolerance);
		fLabelToleranceValue.setText(Integer.toString(fTourData.getDpTolerance()));

		// force the segements to be rebuild for the new tour
		onToleranceChanged(fDpTolerance, true);
	}

	private void onToleranceChanged(final int dpTolerance, final boolean forceRecalc) {

		// update label in the ui
		fLabelToleranceValue.setText(Integer.toString(dpTolerance));

		if (fTourData == null || (fDpTolerance == dpTolerance && forceRecalc == false)) {
			return;
		}

		fDpTolerance = dpTolerance;

		// update tolerance into the tour data
		fTourData.setDpTolerance((short) dpTolerance);

		// create points for the simplifier from distance and altitude
		final int[] distanceSerie = fTourData.getMetricDistanceSerie();
		final int[] altitudeSerie = fTourData.altitudeSerie;

		final Point graphPoints[] = new Point[distanceSerie.length];
		for (int iPoint = 0; iPoint < graphPoints.length; iPoint++) {
			graphPoints[iPoint] = new Point(distanceSerie[iPoint], altitudeSerie[iPoint], iPoint);
		}

		final DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(dpTolerance, graphPoints);
		final Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */
		fTourData.segmentSerieIndex = new int[simplePoints.length];

		final int[] segmentSerieIndex = fTourData.segmentSerieIndex;

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			final Point point = (Point) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}

		// update table and create the tour segments in tour data
		setTableInput();

		fireSegmentLayerChanged();
	}

	@Override
	public void setFocus() {
		fScaleTolerance.setFocus();
	}

	private void setTableInput() {
		fSegmentViewer.setInput(this);
	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

		if (fTourEditor != null && fTourData != null && fSavedDpTolerance != fTourData.getDpTolerance()) {

			fTourEditor.setTourDirty();
		}
	}

}
