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
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class TourSegmenterView extends ViewPart implements ITourViewer {

	public static final String		ID							= "net.tourbook.views.TourSegmenter";	//$NON-NLS-1$

	private static final String		MEMENTO_COLUMN_SORT_ORDER	= "tourSegmenter.column_sort_order";	//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_WIDTH		= "tourSegmenter.column_width";		//$NON-NLS-1$

	private static final int		COLUMN_DEFAULT				= 0;									// sort by time
	private static final int		COLUMN_SPEED				= 10;
	private static final int		COLUMN_PACE					= 20;
	private static final int		COLUMN_GRADIENT				= 30;

	private static IMemento			fSessionMemento;

	private PageBook				fPageBook;
	private Composite				fPageSegmenter;
	private Composite				fViewerContainer;

	private Scale					fScaleTolerance;
	private Label					fLabelToleranceValue;
	private Label					fPageNoChart;
	private Label					fLblTitle;

	private TableViewer				fSegmentViewer;
	private ColumnManager			fColumnManager;

	private TourChart				fTourChart;
	private TourData				fTourData;

	private int						fDpTolerance;
	private int						fSavedDpTolerance;

	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private PostSelectionProvider	fPostSelectionProvider;

	private final NumberFormat		fNf							= NumberFormat.getNumberInstance();

	private TourEditor				fTourEditor;

	private boolean					fShowSegmentsInChart;

	private ActionShowSegments		fActionShowSegments;

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
			case COLUMN_DEFAULT:
				rc = segment1.serieIndexStart - segment2.serieIndexStart;
				if (direction == DESCENDING) {
					rc = -rc;
				}
				break;

			case COLUMN_SPEED:
				rc = (int) ((segment1.speed - segment2.speed) * 100);
				break;

			case COLUMN_PACE:
				rc = (int) ((segment1.pace - segment2.pace) * 100);
				break;

			case COLUMN_GRADIENT:
				rc = (int) ((segment1.gradient - segment2.gradient) * 100);
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

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {

				if (ID.equals(partRef.getId())) {
					// keep settings for this part
					savePartSettings();
				}

				if (fTourChart != null) {
					// hide the tour segments
					fShowSegmentsInChart = false;
					fireSegmentLayerChanged();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

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
					saveViewerSettings(fSessionMemento);
					final Control[] children = fViewerContainer.getChildren();
					for (int childIndex = 0; childIndex < children.length; childIndex++) {
						children[childIndex].dispose();
					}

					// recreate viewer
					createTableViewer(fViewerContainer);
					restoreViewerSettings(fSessionMemento);

					fViewerContainer.layout();

					// update viewer content
					setTableInput();

					// refresh tour with the new measurement system
					fireSegmentLayerChanged();
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

	private void createActions() {

		final ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(actionModifyColumns);
	}

	/**
	 * create view menu
	 */
	private void createMenus() {

		fActionShowSegments = new ActionShowSegments();

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(fActionShowSegments);

	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.WRAP);
		fPageNoChart.setText(Messages.Tour_Segmenter_Label_no_chart);

		fPageSegmenter = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 1).applyTo(fPageSegmenter);

		createSegmenterLayout(fPageSegmenter);

		fViewerContainer = new Composite(fPageSegmenter, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fViewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);
		createTableViewer(fViewerContainer);

		createMenus();
		createActions();

		addSelectionListener();
		addPartListener();
		addPrefListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fPageBook.showPage(fPageNoChart);

		// set default value, show segments in opened charts
		fShowSegmentsInChart = true;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		restoreViewerSettings(fSessionMemento);

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
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

	private void createTableColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TableColumnDefinition colDef;

		final SelectionAdapter defaultColumnSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_DEFAULT);
				fSegmentViewer.refresh();
			}
		};

		/*
		 * column: driving time
		 */
		colDef = TableColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(UI.formatSeconds(segment.drivingTime));
			}
		});

		/*
		 * column: distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(2);
				fNf.setMaximumFractionDigits(2);

				cell.setText(fNf.format((segment.distance) / (1000 * UI.UNIT_VALUE_DISTANCE)));
			}
		});

		/*
		 * column: altitude (m/ft)
		 */
		colDef = TableColumnFactory.ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitude / UI.UNIT_VALUE_ALTITUDE));
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					cell.setText(fNf.format(segment.speed));
				}
			}
		});

		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				fSegmentViewer.refresh();
			}
		});

		/*
		 * column: pace
		 */
		colDef = TableColumnFactory.PACE.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					cell.setText(fNf.format(segment.pace));
				}
			}
		});

		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_PACE);
				fSegmentViewer.refresh();
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);

				cell.setText(fNf.format(segment.gradient));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_GRADIENT);
				fSegmentViewer.refresh();
			}
		});

		/*
		 * column: altitude up m/h
		 */
		colDef = TableColumnFactory.ALTITUDE_UP_H.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeUp / UI.UNIT_VALUE_ALTITUDE)
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
		 * column: altitude down m/h
		 */
		colDef = TableColumnFactory.ALTITUDE_DOWN_H.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeDown / UI.UNIT_VALUE_ALTITUDE)
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

	}

	private void createTableViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fSegmentViewer = new TableViewer(table);

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		createTableColumns(parent);
		fColumnManager.createColumns();

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

		final IEditorPart activeEditor = page.getActiveEditor();
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
		for (final IViewReference viewReference : viewReferences) {

			final IViewPart view = viewReference.getView(false);
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

	@SuppressWarnings("unchecked") //$NON-NLS-1$
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fSegmentViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	private int getTolerance() {
		return (int) ((Math.pow(fScaleTolerance.getSelection(), 2.05)) / 50.0);
	}

	public TreeViewer getTreeViewer() {
		return null;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
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

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;

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
		} else {
			if (fTourData.getMetricDistanceSerie() == null || fTourData.altitudeSerie == null) {
				fPageBook.showPage(fPageNoChart);
				return;
			}
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

//		if (distanceSerie == null || altitudeSerie == null) {
//			fPageBook.showPage(fPageNoChart);
//			return;
//		} else {
//			fPageBook.showPage(fPageSegmenter);
//		}

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

	public void reloadViewer() {
		// TODO Auto-generated method stub
		
	}

	private void restoreViewerSettings(final IMemento memento) {

		if (memento == null) {
			return;
		}

		// restore table columns sort order
		final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
		if (mementoColumnSortOrderIds != null) {
			fColumnManager.orderColumns(StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds));
		}

		// restore column width
		final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
		if (mementoColumnWidth != null) {
			fColumnManager.setColumnWidth(StringToArrayConverter.convertStringToArray(mementoColumnWidth));
		}
	}

	private void savePartSettings() {

		fSessionMemento = XMLMemento.createWriteRoot("TourPropertiesView"); //$NON-NLS-1$

		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		saveViewerSettings(memento);
	}

	private void saveViewerSettings(final IMemento memento) {

		if (memento == null) {
			return;
		}

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		final String[] columnIdAndWidth = fColumnManager.getColumnIdAndWidth();
		if (columnIdAndWidth != null) {
			memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(columnIdAndWidth));
		}
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
