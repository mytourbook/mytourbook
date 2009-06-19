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
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.algorithm.Point;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSegment;
import net.tourbook.database.MyTourbookException;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.util.ArrayListToArray;
import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class TourSegmenterView extends ViewPart implements ITourViewer {

	private static final int				MAX_DISTANCE_SCALE_MILE			= 80;
	private static final int				MAX_DISTANCE_SCALE_METRIC		= 100;

	public static final String				ID								= "net.tourbook.views.TourSegmenter";	//$NON-NLS-1$

	private static final String				STATE_SELECTED_SEGMENTER_INDEX	= "selectedSegmenterIndex";			//$NON-NLS-1$
	private static final String				STATE_SELECTED_DISTANCE			= "selectedDistance";					//$NON-NLS-1$

	final IDialogSettings					fViewState						= TourbookPlugin.getDefault()
																					.getDialogSettingsSection(ID);

	private static final int				COLUMN_DEFAULT					= 0;									// sort by time
	private static final int				COLUMN_SPEED					= 10;
	private static final int				COLUMN_PACE						= 20;
	private static final int				COLUMN_GRADIENT					= 30;
	private static final int				COLUMN_PULSE					= 40;

	private PageBook						fPageBook;
	private Composite						fPageSegmenter;
	private Label							fPageInvalidData;
	private Label							fPageNoData;

	private PageBook						fPageBookSegmenter;
	private Composite						fPageSegTypeDP;
	private Composite						fPageSegTypeByMarker;
	private Composite						fPageSegTypeByDistance;

	private Scale							fScaleDistance;
	private Label							fLabelDistanceValue;

	private Composite						fViewerContainer;

	private Scale							fScaleTolerance;
	private Label							fLabelToleranceValue;
	private ImageComboLabel					fLblTitle;
	private Combo							fCboSegmenterType;
	private Label							fLblAltitudeUp;

	private TableViewer						fSegmentViewer;
	private ColumnManager					fColumnManager;

	/**
	 * {@link TourChart} contains the chart for the tour, this is necessary to move the slider in
	 * the chart to a selected segment
	 */
	private TourChart						fTourChart;
	private TourData						fTourData;

	private int								fDpTolerance;
	private int								fSavedDpTolerance				= -1;

	private ISelectionListener				fPostSelectionListener;
	private IPartListener2					fPartListener;
	private IPropertyChangeListener			fPrefChangeListener;
	private ITourEventListener				fTourEventListener;

	private PostSelectionProvider			fPostSelectionProvider;

	private final NumberFormat				fNf								= NumberFormat.getNumberInstance();
	private int								fMaxDistanceScale;
	private int								fScaleDistancePage;

	private boolean							fShowSegmentsInChart;

	private ActionShowSegments				fActionShowSegments;

	private boolean							fIsTourDirty					= false;

	private boolean							fIsSaving;

	/**
	 * when <code>true</code>, the tour dirty flag is disabled to load data into the fields
	 */
	private boolean							fIsDirtyDisabled				= false;

	private boolean							fIsClearView;
	private Button							fBtnSaveTour;
	private int								fAltitudeUp;
	private int								fAltitudeDown;

	/**
	 * {@link #fSegmenterTypes} and {@link #fSegmenterTypeNames} must be in synch
	 */
	private final static String[]			fSegmenterTypeNames				= new String[] {
			Messages.tour_segmenter_type_byAltitude,
			Messages.tour_segmenter_type_byPulse,
			Messages.tour_segmenter_type_byDistance,
			Messages.tour_segmenter_type_byMarker,
																			//
																			};
	private final static SegmenterType[]	fSegmenterTypes					= new SegmenterType[] {
			SegmenterType.ByAltitudeWithDP, //
			SegmenterType.ByPulseWithDP, //
			SegmenterType.ByDistance,
			SegmenterType.ByMarker, //
																			};

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

	private enum SegmenterType {
		ByAltitudeWithDP, //
		ByPulseWithDP, //
		ByMarker, //
		ByDistance
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

				final Object[] tourSegments = fTourData.createTourSegments();

				updateUIAltitude();

				return tourSegments;
			}
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private static class ViewSorter extends ViewerSorter {

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
				rc = ((segment1.pace - segment2.pace) * 100);
				break;

			case COLUMN_PULSE:
				rc = segment1.pulse - segment2.pulse;
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
				if (partRef.getPart(false) == TourSegmenterView.this) {
					saveTour();
					saveState();
					hideTourSegmentsInChart();
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

					fColumnManager.saveState(fViewState);
					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer(null);

					/*
					 * update distance scale
					 */
					setMaxDistanceScale();
					fScaleDistance.setMaximum(fMaxDistanceScale);
					fScaleDistance.setPageIncrement(fScaleDistancePage);
					updateUIDistance();

					createSegments();
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourSegmenterView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (fTourData == null || part == TourSegmenterView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final TourEvent tourEvent = (TourEvent) eventData;
					final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

					if (modifiedTours == null || modifiedTours.size() == 0) {
						return;
					}

					final TourData modifiedTourData = modifiedTours.get(0);
					final long viewTourId = fTourData.getTourId();

					if (modifiedTourData.getTourId() == viewTourId) {

						// update existing tour

						if (checkDataValidation(modifiedTourData)) {

							if (tourEvent.isReverted) {

								/*
								 * tour is reverted, saving existing tour is not necessary, just
								 * update the tour
								 */
								setTour(modifiedTourData);

							} else {

								createSegments();
								reloadViewer();
							}
						}

					} else {

						// display new tour

						onSelectionChanged(new SelectionTourData(null, modifiedTourData));
					}

					// removed old tour data from the selection provider
					fPostSelectionProvider.clearSelection();

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(fTourEventListener);
	}

	/**
	 * check if data for the segmenter is valid
	 */
	private boolean checkDataValidation(final TourData tourData) {

		if (tourData == null) {

			clearView();

			return false;
		}

		if (tourData.altitudeSerie == null
				|| tourData.altitudeSerie.length == 0
				|| tourData.getMetricDistanceSerie() == null
				|| tourData.getMetricDistanceSerie().length == 0) {

			fPageBook.showPage(fPageInvalidData);

			return false;
		}

		fPageBook.showPage(fPageSegmenter);

		return true;
	}

	private void clearView() {

		fPageBook.showPage(fPageNoData);

		fTourData = null;
		fTourChart = null;

		fIsClearView = true;
	}

	private void createActions() {

		fActionShowSegments = new ActionShowSegments();
		final ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(fActionShowSegments);
	}

	@Override
	public void createPartControl(final Composite parent) {

		setMaxDistanceScale();

		// define all columns
		fColumnManager = new ColumnManager(this, fViewState);
		defineViewerColumns(parent);

		createUI(parent);
		createActions();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// set default value, show segments in opened charts
		fShowSegmentsInChart = true;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		restoreState();

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// when previous onSelectionChanged did not display a tour, get tour from tour manager
		if (fTourData == null) {
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {

					final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

					if (selectedTours != null && selectedTours.size() > 0) {
						onSelectionChanged(new SelectionTourData(null, selectedTours.get(0)));
					}
				}
			});
		}

	}

	/**
	 * create points for the simplifier from distance and altitude
	 */
	private void createSegments() {

		final SegmenterType selectedSegmenter = fSegmenterTypes[fCboSegmenterType.getSelectionIndex()];

		if (selectedSegmenter == SegmenterType.ByAltitudeWithDP) {

			createSegmentsByAltitudeWithDP();

		} else if (selectedSegmenter == SegmenterType.ByPulseWithDP) {

			createSegmentsByPulseWithDP();

		} else if (selectedSegmenter == SegmenterType.ByDistance) {

			createSegmentsByDistance();

		} else if (selectedSegmenter == SegmenterType.ByMarker) {

			createSegmentsByMarker();
		}

		// update table and create the tour segments in tour data
		reloadViewer();

		fireSegmentLayerChanged();
	}

	/**
	 * create Douglas-Peucker segments from distance and altitude
	 */
	private void createSegmentsByAltitudeWithDP() {

		final int[] distanceSerie = fTourData.getMetricDistanceSerie();
		final int[] altitudeSerie = fTourData.altitudeSerie;

		// convert data series into points
		final Point graphPoints[] = new Point[distanceSerie.length];
		for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
			graphPoints[serieIndex] = new Point(distanceSerie[serieIndex], altitudeSerie[serieIndex], serieIndex);
		}

		final DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(fDpTolerance, graphPoints);
		final Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */

		final int[] segmentSerieIndex = fTourData.segmentSerieIndex = new int[simplePoints.length];

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			final Point point = (Point) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}
	}

	private void createSegmentsByDistance() {

		final int[] distanceSerie = fTourData.getMetricDistanceSerie();
		final int lastDistanceSerieIndex = distanceSerie.length - 1;

		final float segmentDistance = getDistance();
		final ArrayList<Integer> segmentSerieIndex = new ArrayList<Integer>();

		// set first segment start
		segmentSerieIndex.add(0);

		float nextSegmentDistance = segmentDistance;

		for (int distanceIndex = 0; distanceIndex < distanceSerie.length; distanceIndex++) {
			final int distance = distanceSerie[distanceIndex];

			if (distance >= nextSegmentDistance) {

				segmentSerieIndex.add(distanceIndex);

				// set minimum distance for the next segment
				nextSegmentDistance += segmentDistance;
			}
		}

		final int serieSize = segmentSerieIndex.size();

		// ensure the last segment ends at the end of the tour
		if (serieSize == 1 || //

				// ensure the last index is not duplicated
				segmentSerieIndex.get(serieSize - 1) != lastDistanceSerieIndex) {

			segmentSerieIndex.add(lastDistanceSerieIndex);
		}

		fTourData.segmentSerieIndex = ArrayListToArray.toInt(segmentSerieIndex);
	}

	private void createSegmentsByMarker() {

		final int[] timeSerie = fTourData.timeSerie;
		final Set<TourMarker> tourMarkers = fTourData.getTourMarkers();

		// sort markers by time - they are unsorted
		final ArrayList<TourMarker> markerList = new ArrayList<TourMarker>(tourMarkers);
		Collections.sort(markerList, new Comparator<TourMarker>() {
			public int compare(final TourMarker tm1, final TourMarker tm2) {
				return tm1.getSerieIndex() - tm2.getSerieIndex();
			}
		});

		final ArrayList<Integer> segmentSerieIndex = new ArrayList<Integer>();

		// set first segment at tour start
		segmentSerieIndex.add(0);

		// create segment for each marker
		for (final TourMarker tourMarker : markerList) {
			segmentSerieIndex.add(tourMarker.getSerieIndex());
		}

		// add segment end at the tour end
		segmentSerieIndex.add(timeSerie.length - 1);

		fTourData.segmentSerieIndex = ArrayListToArray.toInt(segmentSerieIndex);
	}

	/**
	 * create Douglas-Peucker segments from time and pulse
	 */
	private void createSegmentsByPulseWithDP() {

		final int[] timeSerie = fTourData.timeSerie;
		final int[] pulseSerie = fTourData.pulseSerie;

		if (pulseSerie == null || pulseSerie.length < 2) {
			fTourData.segmentSerieIndex = null;
			return;
		}

		// convert data series into points
		final Point graphPoints[] = new Point[timeSerie.length];
		for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
			graphPoints[serieIndex] = new Point(timeSerie[serieIndex], pulseSerie[serieIndex], serieIndex);
		}

		final DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(fDpTolerance, graphPoints);
		final Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */
		final int[] segmentSerieIndex = fTourData.segmentSerieIndex = new int[simplePoints.length];

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			final Point point = (Point) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}
	}

	private void createSegmentViewer(final Composite parent) {

		final Table table = new Table(parent, //
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		fSegmentViewer = new TableViewer(table);
		fColumnManager.createColumns(fSegmentViewer);

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

						if (fTourChart == null) {
							fTourChart = getActiveTourChart(fTourData);
						}

						final SelectionChartXSliderPosition selectionSliderPosition = //
						new SelectionChartXSliderPosition(fTourChart,
								((TourSegment) (segments[0])).serieIndexStart,
								((TourSegment) (segments[segments.length - 1])).serieIndexEnd);

						fPostSelectionProvider.setSelection(selectionSliderPosition);
					}
				}
			}
		});
	}

	private void createUI(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoData = new Label(fPageBook, SWT.WRAP);
		fPageNoData.setText(Messages.Tour_Segmenter_Label_no_chart);

		fPageInvalidData = new Label(fPageBook, SWT.WRAP);
		fPageInvalidData.setText(Messages.Tour_Segmenter_label_invalid_data);

		fPageSegmenter = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(fPageSegmenter);

		fPageBook.showPage(fPageNoData);

		createUIHeader(fPageSegmenter);
		createUIViewer(fPageSegmenter);
	}

	private void createUIHeader(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(3, 3, 3, 2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			// tour title
			fLblTitle = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(fLblTitle);

			// label: create segments with
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.tour_segmenter_label_createSegmentsWith);

			// combo: segmenter type
			fCboSegmenterType = new Combo(container, SWT.READ_ONLY);
			fCboSegmenterType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectSegmenterType();
				}
			});
			for (final String segmenterType : fSegmenterTypeNames) {
				fCboSegmenterType.add(segmenterType);
			}

			// tour/computed altitude
			final Composite altitudeContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(altitudeContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(altitudeContainer);
			{
				// label: computed altitude up
				fLblAltitudeUp = new Label(altitudeContainer, SWT.TRAIL);
				GridDataFactory.fillDefaults()//
						.align(SWT.END, SWT.CENTER)
						.grab(true, false)
						.hint(pc.convertWidthInCharsToPixels(18), SWT.DEFAULT)
						.applyTo(fLblAltitudeUp);
				fLblAltitudeUp.setToolTipText(Messages.tour_segmenter_label_tourAltitude_tooltip);

				// button: update tour
				fBtnSaveTour = new Button(altitudeContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(5, 0).applyTo(fBtnSaveTour);
				fBtnSaveTour.setText(Messages.tour_segmenter_button_updateAltitude);
				fBtnSaveTour.setToolTipText(Messages.tour_segmenter_button_updateAltitude_tooltip);
				fBtnSaveTour.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSaveTourAltitude();
					}
				});
			}

			// pagebook: segmenter type
			fPageBookSegmenter = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(fPageBookSegmenter);
			{
				fPageSegTypeDP = createUISegmenterDP(fPageBookSegmenter);
				fPageSegTypeByMarker = createUISegmenterByMarker(fPageBookSegmenter);
				fPageSegTypeByDistance = createUISegmenterByDistance(fPageBookSegmenter);
			}
		}
	}

	private Composite createUISegmenterByDistance(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{

			// label: distance
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.tour_segmenter_segType_byDistance_label);

			// scale: distance
			fScaleDistance = new Scale(container, SWT.HORIZONTAL);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleDistance);
			fScaleDistance.setMaximum(fMaxDistanceScale);
			fScaleDistance.setPageIncrement(fScaleDistancePage);
			fScaleDistance.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangedDistance();
				}
			});

			// text: distance value
			fLabelDistanceValue = new Label(container, SWT.TRAIL);
			fLabelDistanceValue.setText(Messages.tour_segmenter_segType_byDistance_defaultDistance);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
					.applyTo(fLabelDistanceValue);
		}

		return container;
	}

	private Composite createUISegmenterByMarker(final Composite parent) {

		/*
		 * display NONE, this is not easy to do - or I didn't find an easier way
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			final Canvas canvas = new Canvas(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(1, 1).applyTo(canvas);
		}

		return container;
	}

	private Composite createUISegmenterDP(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{

			// label: tolerance
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Tour_Segmenter_Label_tolerance);

			// scale: tolerance
			fScaleTolerance = new Scale(container, SWT.HORIZONTAL);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleTolerance);
			fScaleTolerance.setMaximum(100);
			fScaleTolerance.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangedTolerance(getDPTolerance());
					setTourDirty();
				}
			});

			// text: tolerance value
			fLabelToleranceValue = new Label(container, SWT.TRAIL);
			fLabelToleranceValue.setText(Messages.Tour_Segmenter_Label_default_tolerance);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(
					pc.convertWidthInCharsToPixels(4),
					SWT.DEFAULT).applyTo(fLabelToleranceValue);
		}

		return container;
	}

	private void createUIViewer(final Composite parent) {

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fViewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createSegmentViewer(fViewerContainer);
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		ColumnDefinition colDef;

		final SelectionAdapter defaultColumnSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_DEFAULT);
				fSegmentViewer.refresh();
			}
		};

		/*
		 * column: TOTAL recording time
		 */
		colDef = TableColumnFactory.RECORDING_TIME_TOTAL.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(UI.format_hh_mm_ss(segment.timeTotal));
			}
		});

		/*
		 * column: TOTAL distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE_TOTAL.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(3);
				fNf.setMaximumFractionDigits(3);

				cell.setText(fNf.format((segment.distanceTotal) / (1000 * UI.UNIT_VALUE_DISTANCE)));
			}
		});

		/*
		 * column: distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(3);
				fNf.setMaximumFractionDigits(3);

				cell.setText(fNf.format((segment.distanceDiff) / (1000 * UI.UNIT_VALUE_DISTANCE)));
			}
		});

		/*
		 * column: recording time
		 */
		colDef = TableColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(UI.format_hh_mm_ss(segment.recordingTime));
			}
		});

		/*
		 * column: driving time
		 */
		colDef = TableColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(UI.format_hh_mm_ss(segment.drivingTime));
			}
		});

		/*
		 * column: break time
		 */
		colDef = TableColumnFactory.PAUSED_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				final int breakTime = segment.breakTime;
				if (breakTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(UI.format_hh_mm_ss(breakTime));
				}
			}
		});

		/*
		 * column: altitude diff (m/ft)
		 */
		colDef = TableColumnFactory.ALTITUDE_DIFF.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitudeDiff / UI.UNIT_VALUE_ALTITUDE));
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_GRADIENT);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);

				cell.setText(fNf.format(segment.gradient));
			}
		});

		/*
		 * column: altitude up m/h
		 */
		colDef = TableColumnFactory.ALTITUDE_UP_H.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeUpH / UI.UNIT_VALUE_ALTITUDE) / segment.drivingTime * 3600;
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
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeDownH / UI.UNIT_VALUE_ALTITUDE) / segment.drivingTime * 3600;
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
		 * column: average speed
		 */
		colDef = TableColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);
				cell.setText(fNf.format(segment.speed));
			}
		});

		/*
		 * column: average pace
		 */
		colDef = TableColumnFactory.AVG_PACE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float pace = segment.pace * UI.UNIT_VALUE_DISTANCE;

				cell.setText(UI.format_mm_ss((long) pace).toString());
			}
		});

		/*
		 * column: pace difference
		 */
		colDef = TableColumnFactory.AVG_PACE_DIFFERENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float paceDiff = segment.paceDiff;

				cell.setText(UI.format_mm_ss((long) paceDiff).toString());
			}
		});

		/*
		 * column: average pulse
		 */
		colDef = TableColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_PULSE);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int pulse = ((TourSegment) cell.getElement()).pulse;

				if (pulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(pulse));
				}
			}
		});

		/*
		 * column: pulse difference
		 */
		colDef = TableColumnFactory.AVG_PULSE_DIFFERENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int pulseDiff = ((TourSegment) cell.getElement()).pulseDiff;

				if (pulseDiff == Integer.MIN_VALUE) {
					cell.setText(UI.EMPTY_STRING);
				} else if (pulseDiff == 0) {
					cell.setText(UI.DASH);
				} else {
					cell.setText(Integer.toString(pulseDiff));
				}
			}
		});

		/*
		 * column: total altitude up (m/ft)
		 */
		colDef = TableColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitudeUp / UI.UNIT_VALUE_ALTITUDE));
			}
		});

		/*
		 * column: total altitude down (m/ft)
		 */
		colDef = TableColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitudeDown / UI.UNIT_VALUE_ALTITUDE));
			}
		});

	}

	@Override
	public void dispose() {

		final IWorkbenchPage wbPage = getSite().getPage();
		wbPage.removePostSelectionListener(fPostSelectionListener);
		wbPage.removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		TourManager.getInstance().removeTourEventListener(fTourEventListener);

		super.dispose();
	}

	/**
	 * notify listeners to show/hide the segments
	 */
	private void fireSegmentLayerChanged() {

		// show/hide the segments in the chart
		TourManager.fireEvent(TourEventId.SEGMENT_LAYER_CHANGED, fShowSegmentsInChart, TourSegmenterView.this);
	}

	/**
	 * try to get the tour chart and/or editor from the active part
	 * 
	 * @param tourData
	 * @return Returns the {@link TourChart} for the requested {@link TourData}
	 */
	private TourChart getActiveTourChart(final TourData tourData) {

		// get tour chart from the active editor part
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IEditorPart activeEditor = wbPage.getActiveEditor();
				if (activeEditor instanceof TourEditor) {

					/*
					 * check if the tour data in the editor is the same
					 */
					final TourChart tourChart = ((TourEditor) activeEditor).getTourChart();
					final TourData tourChartTourData = tourChart.getTourData();
					if (tourChartTourData == tourData) {

						try {
							UI.checkTourData(tourData, tourChartTourData);
						} catch (final MyTourbookException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		// get tour chart from the tour chart view
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IViewReference viewRef = wbPage.findViewReference(TourChartView.ID);
				if (viewRef != null) {

					final IViewPart view = viewRef.getView(false);
					if (view instanceof TourChartView) {

						final TourChartView tourChartView = ((TourChartView) view);

						/*
						 * check if the tour data in the tour chart is the same
						 */
						final TourChart tourChart = tourChartView.getTourChart();
						final TourData tourChartTourData = tourChart.getTourData();
						if (tourChartTourData == tourData) {
							try {
								UI.checkTourData(tourData, tourChartTourData);
							} catch (final MyTourbookException e) {
								e.printStackTrace();
							}

							return tourChart;
						}
					}
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
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

	/**
	 * @return Returns distance in meters
	 */
	private float getDistance() {

		final float selectedDistance = fScaleDistance.getSelection();
		float scaleDistance;

		if (UI.UNIT_VALUE_DISTANCE == UI.UNIT_MILE) {

			// miles are displayed

			scaleDistance = (selectedDistance) * 1000 / 8;

			if (scaleDistance == 0) {
				scaleDistance = 1000 / 8;
			}

			// convert mile -> meters
			scaleDistance *= UI.UNIT_MILE;

		} else {

			// meters are displayed

			scaleDistance = selectedDistance * MAX_DISTANCE_SCALE_METRIC;

			// ensure the distance in not below 100m
			if (scaleDistance < 100) {
				scaleDistance = 100;
			}
		}

		return scaleDistance;
	}

	private int getDPTolerance() {
		return (int) ((Math.pow(fScaleTolerance.getSelection(), 2.05)) / 50.0);
	}

	public ColumnViewer getViewer() {
		return fSegmentViewer;
	}

	/**
	 * hides the tour segments
	 */
	private void hideTourSegmentsInChart() {

		fShowSegmentsInChart = false;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		fireSegmentLayerChanged();
	}

	private void onChangedDistance() {

		updateUIDistance();

		createSegments();
	}

	private void onChangedTolerance(final int dpTolerance/* , final boolean forceRecalc */) {

		// update label in the ui
		fLabelToleranceValue.setText(Integer.toString(dpTolerance));

		if (fTourData == null || (fDpTolerance == dpTolerance /* && forceRecalc == false */)) {
			return;
		}

		fDpTolerance = dpTolerance;

		// update tolerance into the tour data
		fTourData.setDpTolerance((short) dpTolerance);

		createSegments();
	}

	private void onSaveTourAltitude() {

		fTourData.setTourAltUp(fAltitudeUp);
		fTourData.setTourAltDown(fAltitudeDown);

		fIsTourDirty = true;

		fTourData = saveTour();
	}

	/**
	 * handle a tour selection event
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final ISelection selection) {

		fIsClearView = false;

		if (fIsSaving) {
			return;
		}

		/*
		 * run selection async because a tour could be modified and needs to be saved, modifications
		 * are not reported to the tour data editor, saving needs also to be asynch with the tour
		 * data editor
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				if (fPageBook.isDisposed() || fIsClearView) {
					return;
				}

				TourData nextTourData = null;
				TourChart nextTourChart = null;

				if (selection instanceof SelectionActiveEditor) {

					final IEditorPart editorPart = ((SelectionActiveEditor) selection).getEditor();

					if (editorPart instanceof TourEditor) {

						final TourEditor tourEditor = (TourEditor) editorPart;

						// check if editor changed
						if (fTourChart != null && fTourChart == tourEditor.getTourChart()) {
							return;
						}

						nextTourChart = tourEditor.getTourChart();
						nextTourData = nextTourChart.getTourData();

					} else {
						return;
					}

				} else if (selection instanceof SelectionTourData) {

					final SelectionTourData selectionTourData = (SelectionTourData) selection;

					nextTourData = selectionTourData.getTourData();
					nextTourChart = selectionTourData.getTourChart();

				} else if (selection instanceof SelectionTourId) {

					final SelectionTourId tourIdSelection = (SelectionTourId) selection;

					if (fTourData != null) {
						if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
							// don't reload the same tour
							return;
						}
					}

					nextTourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

				} else {
					return;
				}

				if (checkDataValidation(nextTourData) == false) {
					return;
				}

				/*
				 * save previous tour when a new tour is selected
				 */
				if (fTourData != null && fTourData.getTourId() == nextTourData.getTourId()) {

					// nothing to do, it's the same tour

				} else {

					final TourData savedTour = saveTour();
					if (savedTour != null) {

						/*
						 * when a tour is saved, the change notification is not fired because
						 * another tour is already selected, but to update the tour in a TourViewer,
						 * a change nofification must be fired afterwords
						 */
//				Display.getCurrent().asyncExec(new Runnable() {
//					public void run() {
//						TourManager.fireEvent(TourEventId.TOUR_CHANGED,
//								new TourEvent(savedTour),
//								TourSegmenterView.this);
//					}
//				});
					}

					if (nextTourChart == null) {
						nextTourChart = getActiveTourChart(nextTourData);
					}

					fTourChart = nextTourChart;

					setTour(nextTourData);
				}
			}
		});
	}

	private void onSelectSegmenterType() {

		final SegmenterType selectedSegmenter = fSegmenterTypes[fCboSegmenterType.getSelectionIndex()];
		if (selectedSegmenter == SegmenterType.ByAltitudeWithDP || //
				selectedSegmenter == SegmenterType.ByPulseWithDP) {

			fPageBookSegmenter.showPage(fPageSegTypeDP);

		} else if (selectedSegmenter == SegmenterType.ByMarker) {

			fPageBookSegmenter.showPage(fPageSegTypeByMarker);

		} else if (selectedSegmenter == SegmenterType.ByDistance) {

			fPageBookSegmenter.showPage(fPageSegTypeByDistance);
		}

		fPageSegmenter.layout();

		createSegments();
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		fViewerContainer.setRedraw(false);
		{
			fSegmentViewer.getTable().dispose();

			createSegmentViewer(fViewerContainer);
			fViewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		fViewerContainer.setRedraw(true);

		return fSegmentViewer;
	}

	public void reloadViewer() {
		// force input to be reloaded
		fSegmentViewer.setInput(new Object[0]);
	}

	private void restoreState() {

		// selected segmenter
		int segmenterIndex = 0;
		try {
			segmenterIndex = fViewState.getInt(STATE_SELECTED_SEGMENTER_INDEX);
		} catch (final NumberFormatException e) {}
		fCboSegmenterType.select(segmenterIndex);

		// selected distance
		int stateDistance = 10;
		try {
			stateDistance = fViewState.getInt(STATE_SELECTED_DISTANCE);
		} catch (final NumberFormatException e) {}
		fScaleDistance.setSelection(stateDistance);
		updateUIDistance();

	}

	private void saveState() {

		fColumnManager.saveState(fViewState);

		fViewState.put(STATE_SELECTED_SEGMENTER_INDEX, fCboSegmenterType.getSelectionIndex());
		fViewState.put(STATE_SELECTED_DISTANCE, fScaleDistance.getSelection());
	}

	private TourData saveTour() {

		if (fIsTourDirty == false || fTourData == null || fSavedDpTolerance == -1) {
			// nothing to do
			return null;
		}

		TourData savedTour;
		fIsSaving = true;
		{
			savedTour = TourManager.saveModifiedTour(fTourData);
		}
		fIsSaving = false;

		fIsTourDirty = false;

		return savedTour;
	}

	@Override
	public void setFocus() {
		fScaleTolerance.setFocus();
	}

	private void setMaxDistanceScale() {

		if (UI.UNIT_VALUE_DISTANCE == UI.UNIT_MILE) {

			// imperial

			fMaxDistanceScale = MAX_DISTANCE_SCALE_MILE;
			fScaleDistancePage = 8;

		} else {

			// metric

			fMaxDistanceScale = MAX_DISTANCE_SCALE_METRIC;
			fScaleDistancePage = 10;
		}
	}

	/**
	 * Sets the tour for the segmenter
	 * 
	 * @param tourData
	 */
	private void setTour(final TourData tourData) {

		if (tourData == fTourData) {
			return;
		}

		fIsDirtyDisabled = true;
		fTourData = tourData;

		fPageBook.showPage(fPageSegmenter);

		// update tour title
		fLblTitle.setText(TourManager.getTourTitleDetailed(fTourData));

		// keep original dp tolerance
		fSavedDpTolerance = fDpTolerance = fTourData.getDpTolerance();

		// update segmenter values, the factor is defined by experimentals
		final float factor = 1 / 2.05f;
		final double tolerance = Math.pow(fDpTolerance * 50, factor);

		fScaleTolerance.setSelection((int) tolerance);
		fLabelToleranceValue.setText(Integer.toString(fTourData.getDpTolerance()));

		fBtnSaveTour.setEnabled(fTourData.getTourPerson() != null);

		fIsDirtyDisabled = false;

		onSelectSegmenterType();
	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

		if (fIsDirtyDisabled) {
			return;
		}

		if (fTourData != null && fSavedDpTolerance != fTourData.getDpTolerance()) {
			fIsTourDirty = true;
		}
	}

	/**
	 * update ascending altitude computed value
	 */
	private void updateUIAltitude() {

		final int[] altitudeSegments = fTourData.segmentSerieAltitudeDiff;

		if (altitudeSegments == null) {
			fLblAltitudeUp.setText(UI.EMPTY_STRING);
			return;
		}

		fAltitudeUp = 0;
		fAltitudeDown = 0;

		for (final int altitude : altitudeSegments) {
			if (altitude > 0) {
				fAltitudeUp += altitude;
			} else {
				fAltitudeDown += altitude;
			}
		}

		final StringBuilder sb = new StringBuilder();
		sb.append(Integer.toString((int) (fAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));
		sb.append(UI.SLASH_WITH_SPACE);
		sb.append(Integer.toString((int) (fTourData.getTourAltUp() / UI.UNIT_VALUE_ALTITUDE)));
		sb.append(UI.SPACE);
		sb.append(UI.UNIT_LABEL_ALTITUDE);

		fLblAltitudeUp.setText(sb.toString());
	}

	private void updateUIDistance() {

		float scaleDistance = getDistance() / UI.UNIT_VALUE_DISTANCE;

		if (UI.UNIT_VALUE_DISTANCE == UI.UNIT_MILE) {

			// imperial

			scaleDistance /= 1000;

			final int distanceInt = (int) scaleDistance;
			final float distanceFract = scaleDistance - distanceInt;

			// create distance for imperials which shows the fraction with 1/8, 1/4, 3/8 ...
			final StringBuilder sb = new StringBuilder();
			if (distanceInt > 0) {
				sb.append(Integer.toString(distanceInt));
				sb.append(UI.SPACE);
			}

			if (Math.abs(distanceFract - 0.125f) <= 0.01) {
				sb.append("1/8"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.25f) <= 0.01) {
				sb.append("1/4"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.375) <= 0.01) {
				sb.append("3/8"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.5f) <= 0.01) {
				sb.append("1/2"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.625) <= 0.01) {
				sb.append("5/8"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.75f) <= 0.01) {
				sb.append("3/4"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.875) <= 0.01) {
				sb.append("7/8"); //$NON-NLS-1$
				sb.append(UI.SPACE);
			}

			sb.append(UI.UNIT_LABEL_DISTANCE);

			// update UI
			fLabelDistanceValue.setText(sb.toString());

		} else {

			// metric

			// format distance
			final float selectedDistance = scaleDistance / 1000;
			if (selectedDistance >= 10) {
				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);
			} else {
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);
			}

			// update UI
			fLabelDistanceValue.setText(fNf.format(selectedDistance) + UI.SPACE + UI.UNIT_LABEL_DISTANCE);
		}
	}
}
