/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourSegmenter;

import gnu.trove.list.array.TIntArrayList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.algorithm.DPPoint;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.util.ArrayListToArray;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.AltitudeUpDownSegment;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSegment;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageComputedValues;
import net.tourbook.tour.BreakTimeMethod;
import net.tourbook.tour.BreakTimeResult;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class TourSegmenterView extends ViewPart implements ITourViewer {

	public static final String						ID										= "net.tourbook.views.TourSegmenter";	//$NON-NLS-1$

	private static final String						DISTANCE_MILES_1_8						= "1/8";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_1_4						= "1/4";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_3_8						= "3/8";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_1_2						= "1/2";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_5_8						= "5/8";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_3_4						= "3/4";								//$NON-NLS-1$
	private static final String						DISTANCE_MILES_7_8						= "7/8";								//$NON-NLS-1$

	private static final String						FORMAT_ALTITUDE_DIFF					= "%d / %d %s";						//$NON-NLS-1$

	private static final int						SEGMENTER_REQUIRES_ALTITUDE				= 0x01;
	private static final int						SEGMENTER_REQUIRES_DISTANCE				= 0x02;
	private static final int						SEGMENTER_REQUIRES_PULSE				= 0x04;
	private static final int						SEGMENTER_REQUIRES_MARKER				= 0x08;

	private static final int						MAX_DISTANCE_SPINNER_MILE				= 80;
	private static final int						MAX_DISTANCE_SPINNER_METRIC				= 100;

	private static final String						STATE_DP_TOLERANCE_PULSE				= "STATE_DP_TOLERANCE_PULSE";			//$NON-NLS-1$
	private static final String						STATE_MINIMUM_ALTITUDE					= "STATE_MINIMUM_ALTITUDE";			//$NON-NLS-1$
	private static final String						STATE_SELECTED_DISTANCE					= "selectedDistance";					//$NON-NLS-1$
	private static final String						STATE_SELECTED_SEGMENTER_BY_USER		= "STATE_SELECTED_SEGMENTER_BY_USER";	//$NON-NLS-1$

	/**
	 * Initially this was an int value, with 2 it's a string.
	 */
	private static final String						STATE_SELECTED_BREAK_METHOD2			= "selectedBreakMethod2";				//$NON-NLS-1$

	private static final String						STATE_BREAK_TIME_MIN_AVG_SPEED_AS		= "selectedBreakTimeMinAvgSpeedAS";	//$NON-NLS-1$
	private static final String						STATE_BREAK_TIME_MIN_SLICE_SPEED_AS		= "selectedBreakTimeMinSliceSpeedAS";	//$NON-NLS-1$
	private static final String						STATE_BREAK_TIME_MIN_SLICE_TIME_AS		= "selectedBreakTimeMinSliceTimeAS";	//$NON-NLS-1$

	private static final String						STATE_BREAK_TIME_MIN_AVG_SPEED			= "selectedBreakTimeMinAvgSpeed";		//$NON-NLS-1$
	private static final String						STATE_BREAK_TIME_MIN_SLICE_SPEED		= "selectedBreakTimeMinSliceSpeed";	//$NON-NLS-1$

	private static final String						STATE_BREAK_TIME_MIN_DISTANCE_VALUE		= "selectedBreakTimeMinDistance";		//$NON-NLS-1$
	private static final String						STATE_BREAK_TIME_MIN_TIME_VALUE			= "selectedBreakTimeMinTime";			//$NON-NLS-1$
	private static final String						STATE_BREAK_TIME_SLICE_DIFF				= "selectedBreakTimeSliceDiff";		//$NON-NLS-1$

	/*
	 * Tour segmenter
	 */
	public static final String						STATE_IS_SEGMENTER_ACTIVE				= "STATE_IS_SEGMENTER_ACTIVE";			//$NON-NLS-1$
	public static final String						STATE_IS_SHOW_SEGMENTER_MARKER			= "STATE_IS_SHOW_SEGMENTER_MARKER";	//$NON-NLS-1$
	public static final boolean						STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT	= true;
	public static final String						STATE_IS_SHOW_SEGMENTER_VALUE			= "STATE_IS_SHOW_SEGMENTER_VALUE";		//$NON-NLS-1$
	public static final boolean						STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT	= true;
	public static final String						STATE_IS_SHOW_TOUR_SEGMENTS				= "STATE_IS_SHOW_TOUR_SEGMENTS";		//$NON-NLS-1$
	public static final boolean						STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT		= true;
	public static final String						STATE_GRAPH_ALPHA						= "STATE_GRAPH_ALPHA";					//$NON-NLS-1$
	public static final int							STATE_GRAPH_ALPHA_DEFAULT				= 50;
	public static final String						STATE_STACKED_VISIBLE_VALUES			= "STATE_STACKED_VISIBLE_VALUES";		//$NON-NLS-1$
	public static final int							STATE_STACKED_VISIBLE_VALUES_DEFAULT	= 0;

	private static final int						COLUMN_DEFAULT							= 0;									// sort by time
	private static final int						COLUMN_SPEED							= 10;
	private static final int						COLUMN_PACE								= 20;
	private static final int						COLUMN_GRADIENT							= 30;
	private static final int						COLUMN_PULSE							= 40;
	private static final int						COLUMN_CADENCE							= 50;

	private static final float						SPEED_DIGIT_VALUE						= 10.0f;

	private static final IPreferenceStore			_prefStore								= TourbookPlugin
																									.getPrefStore();
	private static final IDialogSettings			_state									= TourbookPlugin
																									.getState(ID);

	private final boolean							_isOSX									= net.tourbook.common.UI.IS_OSX;

	private ColumnManager							_columnManager;

	private TourData								_tourData;

	private float									_dpToleranceAltitude;
	private float									_dpTolerancePulse;
	private float									_savedDpToleranceAltitude				= -1;

	private PostSelectionProvider					_postSelectionProvider;

	private ISelectionListener						_postSelectionListener;
	private IPartListener2							_partListener;
	private IPropertyChangeListener					_prefChangeListener;
	private ITourEventListener						_tourEventListener;

	private final NumberFormat						_nf_0_0									= NumberFormat
																									.getNumberInstance();
	private final NumberFormat						_nf_1_0									= NumberFormat
																									.getNumberInstance();
	private final NumberFormat						_nf_1_1									= NumberFormat
																									.getNumberInstance();
	private final NumberFormat						_nf_3_3									= NumberFormat
																									.getNumberInstance();
	{
		_nf_0_0.setMinimumFractionDigits(0);
		_nf_0_0.setMaximumFractionDigits(0);

		_nf_1_0.setMinimumFractionDigits(1);
		_nf_1_0.setMaximumFractionDigits(0);

		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);

		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}

	private int										_maxDistanceSpinner;
	private int										_spinnerDistancePage;

	private boolean									_isTourDirty							= false;
	private boolean									_isSaving;

	/**
	 * when <code>true</code>, the tour dirty flag is disabled to load data into the fields
	 */
	private boolean									_isDirtyDisabled						= false;

	private boolean									_isClearView;
	private float									_altitudeUp;
	private float									_altitudeDown;

	/**
	 * Contains all available segmenters.
	 * <p>
	 * The sequence defines how they are displayed in the combobox.
	 */
	private static final ArrayList<TourSegmenter>	_allTourSegmenter						= new ArrayList<TourSegmenter>();

	static {

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByAltitudeWithDP,
				Messages.tour_segmenter_type_byAltitude,
				SEGMENTER_REQUIRES_ALTITUDE | SEGMENTER_REQUIRES_DISTANCE));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByAltitudeWithDPMerged,
				Messages.Tour_Segmenter_Type_ByAltitude_Merged,
				SEGMENTER_REQUIRES_ALTITUDE | SEGMENTER_REQUIRES_DISTANCE));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByComputedAltiUpDown,
				Messages.tour_segmenter_type_byComputedAltiUpDown,
				SEGMENTER_REQUIRES_ALTITUDE));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByMarker,
				Messages.tour_segmenter_type_byMarker,
				SEGMENTER_REQUIRES_MARKER));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByDistance,
				Messages.tour_segmenter_type_byDistance,
				SEGMENTER_REQUIRES_DISTANCE));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByBreakTime,
				Messages.Tour_Segmenter_Type_ByBreakTime,
				SEGMENTER_REQUIRES_DISTANCE));

		_allTourSegmenter.add(new TourSegmenter(
				SegmenterType.ByPulseWithDP,
				Messages.tour_segmenter_type_byPulse,
				SEGMENTER_REQUIRES_PULSE));
	}

	private ArrayList<TourSegmenter>				_availableSegmenter						= new ArrayList<TourSegmenter>();

	/**
	 * segmenter type which the user has selected
	 */
	private SegmenterType							_userSelectedSegmenterType;

	private long									_tourBreakTime;
	private float									_breakUIMinAvgSpeedAS;
	private float									_breakUIMinSliceSpeedAS;
	private int										_breakUIMinSliceTimeAS;
	private float									_breakUIMinAvgSpeed;
	private float									_breakUIMinSliceSpeed;
	private int										_breakUIShortestBreakTime;
	private int										_breakUISliceDiff;
	private float									_breakUIMaxDistance;

	private PixelConverter							_pc;
	private int										_spinnerWidth;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>				_firstColBreakTime						= new ArrayList<Control>();

	private ActionModifyColumns						_actionModifyColumns;
	private ActionTourChartSegmenterConfig			_actionTCSegmenterConfig;

	/*
	 * UI controls
	 */
	private Composite								_parent;

	private PageBook								_pageBookUI;
	private PageBook								_pageBookSegmenter;
	private PageBook								_pageBookBreakTime;

	private Button									_btnSaveTourDP;
	private Button									_btnSaveTourMin;

	private Composite								_containerBreakTime;
	private Composite								_containerViewer;
	private Composite								_pageSegmenter;
	private Composite								_pageBreakByAvgSliceSpeed;
	private Composite								_pageBreakByAvgSpeed;
	private Composite								_pageBreakBySliceSpeed;
	private Composite								_pageBreakByTimeDistance;
	private Composite								_pageNoData;
	private Composite								_pageSegTypeDPAltitude;
	private Composite								_pageSegTypeDPPulse;
	private Composite								_pageSegTypeByMarker;
	private Composite								_pageSegTypeByDistance;
	private Composite								_pageSegTypeByAltiUpDown;
	private Composite								_pageSegTypeByBreakTime;

	private Combo									_comboBreakMethod;
	private Combo									_comboSegmenterType;

	private ImageComboLabel							_lblTitle;

	private Label									_lblAltitudeUpDP;
	private Label									_lblAltitudeUpMin;
	private Label									_lblBreakDistanceUnit;
	private Label									_lblDistanceValue;
	private Label									_lblMinAltitude;
	private Label									_lblTourBreakTime;

	private Spinner									_spinnerBreakMinAvgSpeedAS;
	private Spinner									_spinnerBreakMinSliceSpeedAS;
	private Spinner									_spinnerBreakMinSliceTimeAS;
	private Spinner									_spinnerBreakMinAvgSpeed;
	private Spinner									_spinnerBreakMinSliceSpeed;
	private Spinner									_spinnerBreakShortestTime;
	private Spinner									_spinnerBreakMaxDistance;
	private Spinner									_spinnerBreakSliceDiff;
	private Spinner									_spinnerDistance;
	private Spinner									_spinnerDPToleranceAltitude;
	private Spinner									_spinnerDPTolerancePulse;
	private Spinner									_spinnerMinAltitude;

	private TableViewer								_segmentViewer;

	/**
	 * {@link TourChart} contains the chart for the tour, this is necessary to move the slider in
	 * the chart to a selected segment
	 */
	private TourChart								_tourChart;

	public static enum SegmenterType {
		ByAltitudeWithDP, //
		ByAltitudeWithDPMerged, //
		ByPulseWithDP, //
		ByMarker, //
		ByDistance, //
		ByComputedAltiUpDown, //
		ByBreakTime,
	}

	/**
	 * The content provider class is responsible for providing objects to the view. It can wrap
	 * existing objects in adapters or simply return objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore it and always show the same content (like Task
	 * List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {

		public ViewContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			if (_tourData == null) {
				return new Object[0];
			} else {

				final Object[] tourSegments = createSegmenterContent();

				updateUIAltitude();
				updateUIBreakTime();

				return tourSegments;
			}
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private static class ViewSorter extends ViewerSorter {

//		private static final int	ASCENDING	= 0;
		private static final int	DESCENDING	= 1;

		private int					_column;
		private int					_direction;

		/**
		 * Compares the object for sorting
		 */
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			final TourSegment segment1 = ((TourSegment) obj1);
			final TourSegment segment2 = ((TourSegment) obj2);

			int rc = 0;

			// Determine which column and do the appropriate sort
			switch (_column) {
			case COLUMN_DEFAULT:
				rc = segment1.serieIndexStart - segment2.serieIndexStart;
				if (_direction == DESCENDING) {
					rc = -rc;
				}
				break;

			case COLUMN_SPEED:
				rc = (int) ((segment1.speed - segment2.speed) * 100);
				break;

			case COLUMN_PACE:
				rc = (int) ((segment1.pace - segment2.pace) * 100);
				break;

			case COLUMN_PULSE:
				rc = (int) (segment1.pulse - segment2.pulse);
				break;

			case COLUMN_GRADIENT:
				rc = (int) ((segment1.gradient - segment2.gradient) * 100);
				break;

			case COLUMN_CADENCE:
				rc = (int) (segment1.cadence - segment2.cadence);
				break;
			}

			// If descending order, flip the direction
			if (_direction == DESCENDING) {
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

			if (column == _column) {
				// Same column as last sort; toggle the direction
				_direction = 1 - _direction;
			} else {
				// New column; do an descending sort
				_column = column;
				_direction = DESCENDING;
			}
		}
	}

	/**
	 * Constructor
	 */
	public TourSegmenterView() {
		super();
	}

	public static IDialogSettings getState() {
		return _state;
	}

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourSegmenterView.this) {
					onPartClosed();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourSegmenterView.this) {
					onPartOpened();
				}
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					net.tourbook.ui.UI.updateUnits();

					/*
					 * update viewer
					 */
					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_containerViewer);

					recreateViewer(null);

					/*
					 * update distance
					 */
					setMaxDistanceSpinner();
					_spinnerDistance.setMaximum(_maxDistanceSpinner);
					_spinnerDistance.setPageIncrement(_spinnerDistancePage);
					updateUIDistance();

					/*
					 * update min altitude
					 */
					_lblMinAltitude.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE);
					_lblMinAltitude.pack(true);

					createSegments(true);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_segmentViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_segmentViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_segmentViewer.getTable().redraw();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourSegmenterView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourSegmenterView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_SELECTION && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else {

					if (_tourData == null) {
						return;
					}

					if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

						final TourEvent tourEvent = (TourEvent) eventData;
						final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

						if (modifiedTours == null || modifiedTours.size() == 0) {
							return;
						}

						final TourData modifiedTourData = modifiedTours.get(0);
						final long viewTourId = _tourData.getTourId();

						if (modifiedTourData.getTourId() == viewTourId) {

							// update existing tour

							if (checkDataValidation(modifiedTourData)) {

								if (tourEvent.isReverted) {

									/*
									 * tour is reverted, saving existing tour is not necessary, just
									 * update the tour
									 */
									setTour(modifiedTourData, true);

								} else {

									// it's the same tour but tour is modified

									onSelectionChanged(new SelectionTourData(null, modifiedTourData));
								}
							}

						} else {

							// display new tour

							onSelectionChanged(new SelectionTourData(null, modifiedTourData));
						}

						// removed old tour data from the selection provider
						_postSelectionProvider.clearSelection();

					} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

						clearView();

					} else if (eventId == TourEventId.UPDATE_UI) {

						// check if a tour must be updated

						if (_tourData == null) {
							return;
						}

						final Long tourId = _tourData.getTourId();

						// update ui
						if (net.tourbook.ui.UI.containsTourId(eventData, tourId) != null) {

							setTour(TourManager.getInstance().getTourData(tourId), true);
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * check if data for the segmenter is valid
	 */
	private boolean checkDataValidation(final TourData tourData) {

		/*
		 * tourdata and time serie are necessary to create any segment
		 */
		if (tourData == null || tourData.timeSerie == null || tourData.timeSerie.length < 2) {

			clearView();

			return false;
		}

		if (checkSegmenterData(tourData) == 0) {

			clearView();

			return false;
		}

		_pageBookUI.showPage(_pageSegmenter);
		enableActions();

		return true;
	}

	private int checkSegmenterData(final TourData tourData) {

		final float[] altitudeSerie = tourData.altitudeSerie;
		final float[] metricDistanceSerie = tourData.getMetricDistanceSerie();
		final float[] pulseSerie = tourData.pulseSerie;
		final Set<TourMarker> markerSerie = tourData.getTourMarkers();

		int checkedSegmenterData = 0;

		checkedSegmenterData |= altitudeSerie != null && altitudeSerie.length > 1 ? //
				SEGMENTER_REQUIRES_ALTITUDE
				: 0;

		checkedSegmenterData |= metricDistanceSerie != null && metricDistanceSerie.length > 1
				? SEGMENTER_REQUIRES_DISTANCE
				: 0;

		checkedSegmenterData |= pulseSerie != null && pulseSerie.length > 1 ? //
				SEGMENTER_REQUIRES_PULSE
				: 0;

		checkedSegmenterData |= markerSerie != null && markerSerie.size() > 0 ? //
				SEGMENTER_REQUIRES_MARKER
				: 0;

		return checkedSegmenterData;
	}

	private void clearView() {

		_pageBookUI.showPage(_pageNoData);

		_tourData = null;
		_tourChart = null;

		_isClearView = true;

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();

		enableActions();
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);
		_actionTCSegmenterConfig = new ActionTourChartSegmenterConfig(this, _parent);

	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;
		_pc = new PixelConverter(parent);
		_spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		setMaxDistanceSpinner();

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);
		createActions();
		fillToolbar();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		_pageBookUI.showPage(_pageNoData);

		restoreState();
		enableActions();

		showTour();
	}

	private Object[] createSegmenterContent() {

		final TourSegmenter selectedSegmenter = getSelectedSegmenter();
		if (selectedSegmenter == null) {
			return new Object[0];
		}

		/*
		 * get break time values: time/distance & speed
		 */
		final BreakTimeTool btConfig;

		if (selectedSegmenter.segmenterType == SegmenterType.ByBreakTime) {

			// use segmenter values

			btConfig = new BreakTimeTool(
					getSelectedBreakMethod().methodId,
					_breakUIShortestBreakTime,
					_breakUIMaxDistance,
					_breakUIMinSliceSpeed,
					_breakUIMinAvgSpeed,
					_breakUISliceDiff,
					_breakUIMinAvgSpeedAS,
					_breakUIMinSliceSpeedAS,
					_breakUIMinSliceTimeAS);

		} else {

			// use pref values for time/distance & speed

			btConfig = BreakTimeTool.getPrefValues();
		}

		return _tourData.createTourSegments(btConfig);
	}

	/**
	 * create points for the simplifier from distance and altitude
	 * 
	 * @param isFireEvent
	 */
	private void createSegments(final boolean isFireEvent) {

		if (_isSaving) {
			return;
		}

		if (_tourData == null) {

			_pageBookUI.showPage(_pageNoData);
			enableActions();

			return;
		}

		// disable computed altitude
		_tourData.segmentSerieComputedAltitudeDiff = null;

		final TourSegmenter selectedSegmenter = getSelectedSegmenter();
		if (selectedSegmenter == null) {
			clearView();
			return;
		}

		final SegmenterType selectedSegmenterType = selectedSegmenter.segmenterType;

		if (selectedSegmenterType == SegmenterType.ByAltitudeWithDP) {

			createSegmentsBy_AltitudeWithDP();

		} else if (selectedSegmenterType == SegmenterType.ByAltitudeWithDPMerged) {

			createSegmentsBy_AltitudeWithDPMerged();

		} else if (selectedSegmenterType == SegmenterType.ByPulseWithDP) {

			createSegmentsBy_PulseWithDP();

		} else if (selectedSegmenterType == SegmenterType.ByDistance) {

			createSegmentsBy_Distance();

		} else if (selectedSegmenterType == SegmenterType.ByMarker) {

			createSegmentsBy_Marker();

		} else if (selectedSegmenterType == SegmenterType.ByComputedAltiUpDown) {

			createSegmentsBy_AltiUpDown();

		} else if (selectedSegmenterType == SegmenterType.ByBreakTime) {

			createSegmentsBy_BreakTime();
		}

		// update table and create the tour segments in tour data
		reloadViewer();

		if (isFireEvent) {
			fireSegmentLayerChanged();
		}
	}

	/**
	 * create Douglas-Peucker segments from distance and altitude
	 */
	private void createSegmentsBy_AltitudeWithDP() {

		final float[] distanceSerie = _tourData.getMetricDistanceSerie();
		final float[] altitudeSerie = _tourData.altitudeSerie;

		// convert data series into dp points
		final DPPoint graphPoints[] = new DPPoint[distanceSerie.length];
		for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
			graphPoints[serieIndex] = new DPPoint(distanceSerie[serieIndex], altitudeSerie[serieIndex], serieIndex);
		}

		final Object[] dpPoints = new DouglasPeuckerSimplifier(_dpToleranceAltitude, graphPoints).simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */

		final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[dpPoints.length];

		for (int iPoint = 0; iPoint < dpPoints.length; iPoint++) {
			final DPPoint point = (DPPoint) dpPoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}
	}

	/**
	 * Create Douglas-Peucker segments from distance and altitude. All segments are merged which
	 * have the same vertical direction.
	 */
	private void createSegmentsBy_AltitudeWithDPMerged() {

		final float[] distanceSerie = _tourData.getMetricDistanceSerie();
		final float[] altitudeSerie = _tourData.altitudeSerie;

		final int serieSize = distanceSerie.length;

		// convert data series into dp points
		final DPPoint graphPoints[] = new DPPoint[serieSize];
		for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
			graphPoints[serieIndex] = new DPPoint(distanceSerie[serieIndex], altitudeSerie[serieIndex], serieIndex);
		}

		final Object[] simplePoints = new DouglasPeuckerSimplifier(_dpToleranceAltitude, graphPoints).simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */

		final TIntArrayList segmentSerieIndex = new TIntArrayList();

		// set first point
		segmentSerieIndex.add(0);

		DPPoint prevDpPoint = (DPPoint) simplePoints[0];

		double prevAltitude = prevDpPoint.y;
		boolean isPrevAltiUp = false;
		boolean isPrevAltiDown = false;

		for (int pointIndex = 1; pointIndex < simplePoints.length; pointIndex++) {

			final DPPoint currentDpPoint = (DPPoint) simplePoints[pointIndex];

			final double currentAltitude = currentDpPoint.y;

			if (pointIndex == 1) {

				// first point

				isPrevAltiUp = (currentAltitude - prevAltitude) >= 0;
				isPrevAltiDown = (currentAltitude - prevAltitude) < 0;

			} else {

				// all other points

				final boolean isCurrentAltiUp = (currentAltitude - prevAltitude) >= 0;
				final boolean isCurrentAltiDown = (currentAltitude - prevAltitude) < 0;

				if (isPrevAltiUp && isCurrentAltiUp || isPrevAltiDown && isCurrentAltiDown) {

					// up or down have not changed

				} else {

					// up or down have changed

					segmentSerieIndex.add(prevDpPoint.serieIndex);

					isPrevAltiUp = isCurrentAltiUp;
					isPrevAltiDown = isCurrentAltiDown;
				}
			}

			prevDpPoint = currentDpPoint;
			prevAltitude = currentAltitude;
		}

		// add last point
		segmentSerieIndex.add(serieSize - 1);

		_tourData.segmentSerieIndex = segmentSerieIndex.toArray();
	}

	private void createSegmentsBy_AltiUpDown() {

		final float selectedMinAltiDiff = (float) (_spinnerMinAltitude.getSelection() / 10.0);

		final ArrayList<AltitudeUpDownSegment> tourSegements = new ArrayList<AltitudeUpDownSegment>();

		// create segment when the altitude up/down is changing
		_tourData.computeAltitudeUpDown(tourSegements, selectedMinAltiDiff);

		// convert segment list into array
		int serieIndex = 0;
		final int segmentLength = tourSegements.size();
		final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[segmentLength];
		final float[] altitudeDiff = _tourData.segmentSerieComputedAltitudeDiff = new float[segmentLength];

		for (final AltitudeUpDownSegment altitudeUpDownSegment : tourSegements) {

			segmentSerieIndex[serieIndex] = altitudeUpDownSegment.serieIndex;
			altitudeDiff[serieIndex] = altitudeUpDownSegment.computedAltitudeDiff;

			serieIndex++;
		}
	}

	private void createSegmentsBy_BreakTime() {

		boolean[] breakTimeSerie = null;
		BreakTimeResult breakTimeResult = null;

		final String breakMethodId = getSelectedBreakMethod().methodId;

		if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

			_breakUIShortestBreakTime = _spinnerBreakShortestTime.getSelection();
			_breakUIMaxDistance = _spinnerBreakMaxDistance.getSelection()
					* net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
			_breakUISliceDiff = _spinnerBreakSliceDiff.getSelection();

			breakTimeResult = BreakTimeTool.computeBreakTimeByTimeDistance(
					_tourData,
					_breakUIShortestBreakTime,
					_breakUIMaxDistance,
					_breakUISliceDiff);

		} else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

			_breakUIMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
					/ SPEED_DIGIT_VALUE
					/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(_tourData, breakMethodId, _breakUIMinSliceSpeed);

		} else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

			_breakUIMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()//
					/ SPEED_DIGIT_VALUE
					/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(_tourData, breakMethodId, _breakUIMinAvgSpeed);

		} else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

			_breakUIMinAvgSpeedAS = _spinnerBreakMinAvgSpeedAS.getSelection()//
					/ SPEED_DIGIT_VALUE
					/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			_breakUIMinSliceSpeedAS = _spinnerBreakMinSliceSpeedAS.getSelection()
					/ SPEED_DIGIT_VALUE
					/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			_breakUIMinSliceTimeAS = _spinnerBreakMinSliceTimeAS.getSelection();

			breakTimeResult = BreakTimeTool.computeBreakTimeByAvgSliceSpeed(
					_tourData,
					_breakUIMinAvgSpeedAS,
					_breakUIMinSliceSpeedAS,
					_breakUIMinSliceTimeAS);
		}

		breakTimeSerie = breakTimeResult.breakTimeSerie;
		_tourBreakTime = breakTimeResult.tourBreakTime;

		/*
		 * convert recognized breaks into segments
		 */
		final ArrayList<Integer> segmentSerieIndex = new ArrayList<Integer>();

		// set start for first segment
		segmentSerieIndex.add(0);

		boolean prevIsBreak = false;
		boolean isBreak = breakTimeSerie[0];

		for (int serieIndex = 1; serieIndex < breakTimeSerie.length; serieIndex++) {

			isBreak = breakTimeSerie[serieIndex];

			if (isBreak != prevIsBreak) {

				// break has toggled, set end index

				segmentSerieIndex.add(serieIndex - 1);
			}

			prevIsBreak = isBreak;
		}

		// ensure the last segment ends at the end of the tour
		final int lastDistanceSerieIndex = _tourData.timeSerie.length - 1;
		final int serieSize = segmentSerieIndex.size();
		if (serieSize == 1 || //

				// ensure the last index is not duplicated
				segmentSerieIndex.get(serieSize - 1) != lastDistanceSerieIndex) {

			segmentSerieIndex.add(lastDistanceSerieIndex);
		}

		_tourData.segmentSerieIndex = ArrayListToArray.toInt(segmentSerieIndex);
		_tourData.setBreakTimeSerie(breakTimeSerie);

	}

	private void createSegmentsBy_Distance() {

		final float[] distanceSerie = _tourData.getMetricDistanceSerie();
		final int lastDistanceSerieIndex = distanceSerie.length - 1;

		final float segmentDistance = getDistance();
		final ArrayList<Integer> segmentSerieIndex = new ArrayList<Integer>();

		// set first segment start
		segmentSerieIndex.add(0);

		float nextSegmentDistance = segmentDistance;

		for (int distanceIndex = 0; distanceIndex < distanceSerie.length; distanceIndex++) {

			final float distance = distanceSerie[distanceIndex];
			if (distance >= nextSegmentDistance) {

				segmentSerieIndex.add(distanceIndex);

				// set minimum distance for the next segment
				nextSegmentDistance += segmentDistance;
			}
		}

		// ensure the last segment ends at the end of the tour
		final int serieSize = segmentSerieIndex.size();
		if (serieSize == 1 || //

				// ensure the last index is not duplicated
				segmentSerieIndex.get(serieSize - 1) != lastDistanceSerieIndex) {

			segmentSerieIndex.add(lastDistanceSerieIndex);
		}

		_tourData.segmentSerieIndex = ArrayListToArray.toInt(segmentSerieIndex);
	}

	private void createSegmentsBy_Marker() {

		final int[] timeSerie = _tourData.timeSerie;
		final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();

		// sort markers by time - they can be unsorted
		final ArrayList<TourMarker> markerList = new ArrayList<TourMarker>(tourMarkers);
		Collections.sort(markerList, new Comparator<TourMarker>() {
			@Override
			public int compare(final TourMarker tm1, final TourMarker tm2) {
				return tm1.getSerieIndex() - tm2.getSerieIndex();
			}
		});

		final ArrayList<Integer> segmentSerieIndex = new ArrayList<Integer>();

		int prevSerieIndex = 0;

		// set first segment at tour start
		segmentSerieIndex.add(prevSerieIndex);

		// create segment for each marker
		for (final TourMarker tourMarker : markerList) {

			final int serieIndex = tourMarker.getSerieIndex();

			// prevent to set a second segment at the same position
			if (serieIndex != prevSerieIndex) {
				segmentSerieIndex.add(serieIndex);
			}

			prevSerieIndex = serieIndex;
		}

		// add segment end at the tour end
		final int lastIndex = timeSerie.length - 1;
		if (prevSerieIndex != lastIndex) {
			segmentSerieIndex.add(lastIndex);
		}

		_tourData.segmentSerieIndex = ArrayListToArray.toInt(segmentSerieIndex);
	}

	/**
	 * create Douglas-Peucker segments from time and pulse
	 */
	private void createSegmentsBy_PulseWithDP() {

		final int[] timeSerie = _tourData.timeSerie;
		final float[] pulseSerie = _tourData.pulseSerie;

		if (pulseSerie == null || pulseSerie.length < 2) {
			_tourData.segmentSerieIndex = null;
			return;
		}

		// convert data series into points
		final DPPoint graphPoints[] = new DPPoint[timeSerie.length];
		for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
			graphPoints[serieIndex] = new DPPoint(timeSerie[serieIndex], pulseSerie[serieIndex], serieIndex);
		}

		final DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(_dpTolerancePulse, graphPoints);
		final Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */
		final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[simplePoints.length];

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			final DPPoint point = (DPPoint) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}
	}

	private void createUI(final Composite parent) {

		_pageBookUI = new PageBook(parent, SWT.NONE);

		_pageNoData = new Composite(_pageBookUI, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageNoData);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoData);
		{
			final Label lblNoData = new Label(_pageNoData, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(lblNoData);
			lblNoData.setText(Messages.Tour_Segmenter_Label_no_chart);
		}

		_pageSegmenter = new Composite(_pageBookUI, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_pageSegmenter);
		{
			createUI_10_Header(_pageSegmenter);
			createUI_70_Viewer(_pageSegmenter);
		}
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(3, 3, 3, 5).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			// tour title
			_lblTitle = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_lblTitle);

			// label: create segments with
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.tour_segmenter_label_createSegmentsWith);

			// combo: segmenter type
			{
				_comboSegmenterType = new Combo(container, SWT.READ_ONLY);
				_comboSegmenterType.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectSegmenterType(true);
					}
				});

				/*
				 * fill the segmenter list that the next layout shows the combo that all segmenter
				 * names are visible
				 */
				for (final TourSegmenter segmenter : _allTourSegmenter) {
					_comboSegmenterType.add(segmenter.name);
				}
				final Point size = _comboSegmenterType.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				GridDataFactory.fillDefaults().hint(size).applyTo(_comboSegmenterType);
			}

			// pagebook: segmenter type
			_pageBookSegmenter = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_pageBookSegmenter);
			{
				_pageSegTypeDPAltitude = createUI_42_SegmenterBy_DPAltitude(_pageBookSegmenter);
				_pageSegTypeDPPulse = createUI_42_SegmenterBy_DPPulse(_pageBookSegmenter);
				_pageSegTypeByMarker = createUI_43_SegmenterBy_Marker(_pageBookSegmenter);
				_pageSegTypeByDistance = createUI_44_SegmenterBy_Distance(_pageBookSegmenter);
				_pageSegTypeByAltiUpDown = createUI_45_SegmenterBy_MinAltitude(_pageBookSegmenter);
				_pageSegTypeByBreakTime = createUI_50_SegmenterBy_BreakTime(_pageBookSegmenter);
			}
		}
	}

	private Composite createUI_42_SegmenterBy_DPAltitude(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
		{
			_spinnerDPToleranceAltitude = createUI_DP_Tolerance(container);
			_lblAltitudeUpDP = createUI_DP_Info(container);
			_btnSaveTourDP = createUI_DB_SaveTour(container);
		}

		return container;
	}

	private Composite createUI_42_SegmenterBy_DPPulse(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_spinnerDPTolerancePulse = createUI_DP_Tolerance(container);
		}

		return container;
	}

	private Composite createUI_43_SegmenterBy_Marker(final Composite parent) {

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

	private Composite createUI_44_SegmenterBy_Distance(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{

			// label: distance
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.tour_segmenter_segType_byDistance_label);

			// spinner: distance
			_spinnerDistance = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(_spinnerDistance);
			_spinnerDistance.setMinimum(1); // 0.1
			_spinnerDistance.setMaximum(_maxDistanceSpinner);
			_spinnerDistance.setPageIncrement(_spinnerDistancePage);
			_spinnerDistance.setDigits(1);
			_spinnerDistance.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangedDistance();
				}
			});
			_spinnerDistance.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangedDistance();
				}
			});

			// text: distance value
			_lblDistanceValue = new Label(container, SWT.NONE);
			_lblDistanceValue.setText(Messages.tour_segmenter_segType_byDistance_defaultDistance);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.grab(true, false)
					.applyTo(_lblDistanceValue);
		}

		return container;
	}

	private Composite createUI_45_SegmenterBy_MinAltitude(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		{
			// label: min alti diff
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.tour_segmenter_segType_byUpDownAlti_label);

			// spinner: minimum altitude
			_spinnerMinAltitude = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(_spinnerMinAltitude);
			_spinnerMinAltitude.setMinimum(1); // 0.1
			_spinnerMinAltitude.setMaximum(10000); // 1000
			_spinnerMinAltitude.setDigits(1);
			_spinnerMinAltitude.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					createSegments(true);
				}
			});
			_spinnerMinAltitude.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeBreakTime();
				}
			});

			// label: unit
			_lblMinAltitude = new Label(container, SWT.NONE);
			_lblMinAltitude.setText(net.tourbook.common.UI.UNIT_LABEL_ALTITUDE);

			_lblAltitudeUpMin = createUI_DP_Info(container);
			_btnSaveTourMin = createUI_DB_SaveTour(container);
		}

		return container;
	}

	private Composite createUI_50_SegmenterBy_BreakTime(final Composite parent) {

		_containerBreakTime = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerBreakTime);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_containerBreakTime);
//		_containerBreakTime.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			final Composite container = new Composite(_containerBreakTime, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
			{
				createUI_51_TourBreakTime(container);
				createUI_52_BreakTimePageBook(container);
			}

			createUI_59_BreakActions(_containerBreakTime);
		}

		_containerBreakTime.layout(true, true);
		UI.setEqualizeColumWidths(_firstColBreakTime);

		return _containerBreakTime;
	}

	private void createUI_51_TourBreakTime(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.span(2, 1)
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			/*
			 * tour break time
			 */
			// label: break time
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_TourBreakTime);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			_firstColBreakTime.add(label);

			// label: value + unit
			_lblTourBreakTime = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.grab(true, false)
//					.span(2, 1)
					.applyTo(_lblTourBreakTime);
		}
	}

	private void createUI_52_BreakTimePageBook(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * label: compute break time by
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_ComputeBreakTimeBy);
			_firstColBreakTime.add(label);

			_comboBreakMethod = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			_comboBreakMethod.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBreakTimeMethod();
				}
			});

			// fill combo
			for (final BreakTimeMethod breakMethod : BreakTimeTool.BREAK_TIME_METHODS) {
				_comboBreakMethod.add(breakMethod.uiText);
			}

			/*
			 * pagebook: break algorithm
			 */
			_pageBookBreakTime = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_pageBookBreakTime);
			{
				_pageBreakByAvgSliceSpeed = createUI_53_BreakBy_AvgSliceSpeed(_pageBookBreakTime);
				_pageBreakByAvgSpeed = createUI_54_BreakBy_AvgSpeed(_pageBookBreakTime);
				_pageBreakBySliceSpeed = createUI_55_BreakBy_SliceSpeed(_pageBookBreakTime);
				_pageBreakByTimeDistance = createUI_56_BreakBy_TimeDistance(_pageBookBreakTime);
			}

			/*
			 * force pages to be displayed otherwise they are hidden or the hint is not computed for
			 * the first column until a resize is done
			 */
//			_pagebookBreakTime.showPage(_pageBreakBySliceSpeed);
//			_pagebookBreakTime.showPage(_pageBreakByAvgSpeed);
//			_pageBreakBySliceSpeed.layout(true, true);
//			_pageBreakByAvgSpeed.layout(true, true);
//			_pagebookBreakTime.layout(true, true);
		}
	}

	private Composite createUI_53_BreakBy_AvgSliceSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * minimum average speed
			 */
			{
				// label: minimum speed
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
				_firstColBreakTime.add(label);

				// spinner: minimum speed
				_spinnerBreakMinAvgSpeedAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinAvgSpeedAS);
				_spinnerBreakMinAvgSpeedAS.setMinimum(0); // 0.0 km/h
				_spinnerBreakMinAvgSpeedAS.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
				_spinnerBreakMinAvgSpeedAS.setDigits(1);
				_spinnerBreakMinAvgSpeedAS.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: km/h
				label = new Label(container, SWT.NONE);
				label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
			}

			/*
			 * minimum slice speed
			 */
			{
				// label: minimum speed
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
				_firstColBreakTime.add(label);

				// spinner: minimum speed
				_spinnerBreakMinSliceSpeedAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinSliceSpeedAS);
				_spinnerBreakMinSliceSpeedAS.setMinimum(0); // 0.0 km/h
				_spinnerBreakMinSliceSpeedAS.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
				_spinnerBreakMinSliceSpeedAS.setDigits(1);
				_spinnerBreakMinSliceSpeedAS.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: km/h
				label = new Label(container, SWT.NONE);
				label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
			}

			/*
			 * minimum slice time
			 */
			{
				// label: minimum slice time
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumSliceTime);
				_firstColBreakTime.add(label);

				// spinner: minimum slice time
				_spinnerBreakMinSliceTimeAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinSliceTimeAS);
				_spinnerBreakMinSliceTimeAS.setMinimum(0); // 0 sec
				_spinnerBreakMinSliceTimeAS.setMaximum(10); // 10 sec
				_spinnerBreakMinSliceTimeAS.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: seconds
				label = new Label(container, SWT.NONE);
				label.setText(Messages.app_unit_seconds);
			}
		}

		return container;
	}

	private Composite createUI_54_BreakBy_AvgSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * minimum average speed
			 */

			// label: minimum speed
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
			_firstColBreakTime.add(label);

			// spinner: minimum speed
			_spinnerBreakMinAvgSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMinAvgSpeed);
			_spinnerBreakMinAvgSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinAvgSpeed.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
			_spinnerBreakMinAvgSpeed.setDigits(1);
			_spinnerBreakMinAvgSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeBreakTime();
				}
			});
			_spinnerBreakMinAvgSpeed.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeBreakTime();
				}
			});

			// label: km/h
			label = new Label(container, SWT.NONE);
			label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
		}

		return container;
	}

	private Composite createUI_55_BreakBy_SliceSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * minimum speed
			 */

			// label: minimum speed
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
			_firstColBreakTime.add(label);

			// spinner: minimum speed
			_spinnerBreakMinSliceSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMinSliceSpeed);
			_spinnerBreakMinSliceSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinSliceSpeed.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
			_spinnerBreakMinSliceSpeed.setDigits(1);
			_spinnerBreakMinSliceSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeBreakTime();
				}
			});
			_spinnerBreakMinSliceSpeed.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeBreakTime();
				}
			});

			// label: km/h
			label = new Label(container, SWT.NONE);
			label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
		}

		return container;
	}

	private Composite createUI_56_BreakBy_TimeDistance(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * shortest break time
			 */
			{
				// label: break min time
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumTime);
				_firstColBreakTime.add(label);

				// spinner: break minimum time
				_spinnerBreakShortestTime = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakShortestTime);
				_spinnerBreakShortestTime.setMinimum(1);
				_spinnerBreakShortestTime.setMaximum(120); // 120 seconds
				_spinnerBreakShortestTime.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeBreakTime();
					}
				});
				_spinnerBreakShortestTime.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: unit
				label = new Label(container, SWT.NONE);
				label.setText(Messages.App_Unit_Seconds_Small);
			}

			/*
			 * recording distance
			 */
			{
				// label: break min distance
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumDistance);
				_firstColBreakTime.add(label);

				// spinner: break minimum time
				_spinnerBreakMaxDistance = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMaxDistance);
				_spinnerBreakMaxDistance.setMinimum(1);
				_spinnerBreakMaxDistance.setMaximum(1000); // 1000 m/yards
				_spinnerBreakMaxDistance.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeBreakTime();
					}
				});
				_spinnerBreakMaxDistance.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: unit
				_lblBreakDistanceUnit = new Label(container, SWT.NONE);
				_lblBreakDistanceUnit.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE_SMALL);
				GridDataFactory.fillDefaults()//
//						.span(2, 1)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblBreakDistanceUnit);
			}

			/*
			 * slice diff break
			 */
			{
				// label: break slice diff
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_SliceDiffBreak);
				label.setToolTipText(Messages.Compute_BreakTime_Label_SliceDiffBreak_Tooltip);
				_firstColBreakTime.add(label);

				// spinner: slice diff break time
				_spinnerBreakSliceDiff = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakSliceDiff);
				_spinnerBreakSliceDiff.setMinimum(0);
				_spinnerBreakSliceDiff.setMaximum(60); // minutes
				_spinnerBreakSliceDiff.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeBreakTime();
					}
				});
				_spinnerBreakSliceDiff.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: unit
				label = new Label(container, SWT.NONE);
				label.setText(Messages.App_Unit_Minute);
			}
		}

		return container;
	}

	private void createUI_59_BreakActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: restore from defaults
			 */
			final Button btnRestore = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.grab(false, true)
					.applyTo(btnRestore);
			btnRestore.setText(Messages.Compute_BreakTime_Button_RestoreDefaultValues);
			btnRestore.setToolTipText(Messages.Compute_BreakTime_Button_RestoreDefaultValues_Tooltip);
			btnRestore.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					restorePrefValues();
				}
			});

			/*
			 * button: set as default values
			 */
			final Button btnSetDefault = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.applyTo(btnSetDefault);
			btnSetDefault.setText(Messages.Compute_BreakTime_Button_SetDefaultValues);
			btnSetDefault.setToolTipText(Messages.Compute_BreakTime_Button_SetDefaultValues_Tooltip);
			btnSetDefault.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSetDefaults(parent);
				}
			});
		}
	}

	private void createUI_70_Viewer(final Composite parent) {

		_containerViewer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_containerViewer);
		GridLayoutFactory.fillDefaults().applyTo(_containerViewer);
		{
			createUI_80_SegmentViewer(_containerViewer);
		}
	}

	private void createUI_80_SegmentViewer(final Composite parent) {

		final Table table = new Table(parent, //
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);

		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		_segmentViewer = new TableViewer(table);
		_columnManager.createColumns(_segmentViewer);

		_segmentViewer.setContentProvider(new ViewContentProvider());
		_segmentViewer.setSorter(new ViewSorter());

		_segmentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {

				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {

					/*
					 * select the chart sliders according to the selected segment(s)
					 */

					final Object[] segments = selection.toArray();

					if (segments.length > 0) {

						if (_tourChart == null) {
							_tourChart = TourManager.getActiveTourChart(_tourData);
						}

						final int startIndex = ((TourSegment) (segments[0])).serieIndexStart;
						final int endIndex = ((TourSegment) (segments[segments.length - 1])).serieIndexEnd;

						final SelectionChartXSliderPosition selectionSliderPosition = new SelectionChartXSliderPosition(
								_tourChart,
								startIndex,
								endIndex,
								true);

						_postSelectionProvider.setSelection(selectionSliderPosition);
					}
				}
			}
		});

		createUI_90_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_90_ContextMenu() {

		final Table table = (Table) _segmentViewer.getControl();

		_columnManager.createHeaderContextMenu(table, null);
	}

	private Button createUI_DB_SaveTour(final Composite parent) {

		final Button btn = new Button(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(btn);
		btn.setText(Messages.tour_segmenter_button_updateAltitude);
		btn.setToolTipText(Messages.Tour_Segmenter_Button_SaveTour_Tooltip);
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSaveTourAltitude();
			}
		});

		return btn;
	}

	private Label createUI_DP_Info(final Composite parent) {

		final Label label = new Label(parent, SWT.TRAIL);

		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.CENTER)
				.grab(true, false)
				.hint(_pc.convertWidthInCharsToPixels(18), SWT.DEFAULT)
				.applyTo(label);

		return label;
	}

	private Spinner createUI_DP_Tolerance(final Composite parent) {

		// label: DP Tolerance
		final Link linkDP = new Link(parent, SWT.NONE);
		linkDP.setText(Messages.Tour_Segmenter_Label_DPTolerance);
		linkDP.setToolTipText(Messages.Tour_Segmenter_Label_DPTolerance_Tooltip);
		linkDP.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				WEB.openUrl(PrefPageComputedValues.URL_DOUGLAS_PEUCKER_ALGORITHM);
			}
		});

		// spinner: DP tolerance
		final Spinner spinner = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(spinner);
		spinner.setMinimum(1); // 0.1
		spinner.setMaximum(10000); // 1000
		spinner.setDigits(1);

		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangedTolerance();
			}
		});

		spinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangedTolerance();
			}
		});

		return spinner;
	}

	private void defineAllColumns(final Composite parent) {

		final SelectionAdapter defaultListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_DEFAULT);
				_segmentViewer.refresh();
			}
		};

		defineColumn_RecordingTimeTotal(defaultListener);

		defineColumn_DistanceTotal(defaultListener);
		defineColumn_Distance(defaultListener);

		defineColumn_RecordingTime(defaultListener);
		defineColumn_DrivingTime(defaultListener);
		defineColumn_PausedTime(defaultListener);

		defineColumn_AltitudeDiffSegmentBorder(defaultListener);
		defineColumn_AltitudeDiffSegmentComputed(defaultListener);
		defineColumn_AltitudeUpSummarizedComputed(defaultListener);
		defineColumn_AltitudeDownSummarizedComputed(defaultListener);

		defineColumn_Gradient();

		defineColumn_AltitudeUpHour(defaultListener);
		defineColumn_AltitudeDownHour(defaultListener);

		defineColumn_AvgSpeed();
		defineColumn_AvgPace();
		defineColumn_AvgPaceDifference();
		defineColumn_AvgPulse();
		defineColumn_AvgPulseDifference();
		defineColumn_AvgCadence();

		defineColumn_AltitudeUpSummarizedBorder(defaultListener);
		defineColumn_AltitudeDownSummarizedBorder(defaultListener);

		defineColumn_SerieStartEndIndex();
	}

	/**
	 * column: altitude diff segment border (m/ft)
	 */
	private void defineColumn_AltitudeDiffSegmentBorder(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_DIFF_SEGMENT_BORDER.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				final float altitudeDiff = segment.altitudeDiffSegmentBorder;
				if (altitudeDiff == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE));
					setCellColor(cell, altitudeDiff);
				}
			}
		});
	}

	/**
	 * column: computed altitude diff (m/ft)
	 */
	private void defineColumn_AltitudeDiffSegmentComputed(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_DIFF_SEGMENT_COMPUTED.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				final float altitudeDiff = segment.altitudeDiffSegmentComputed;
				if (altitudeDiff == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE));
					setCellColor(cell, altitudeDiff);
				}
			}
		});
	}

	/**
	 * column: altitude down m/h
	 */
	private void defineColumn_AltitudeDownHour(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_DOWN_H.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeDownHour / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)
							/ segment.drivingTime
							* 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(_nf_1_0.format(result));
					}
				}
			}
		});
	}

	/**
	 * column: total altitude down (m/ft)
	 */
	private void defineColumn_AltitudeDownSummarizedBorder(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_DOWN_SUMMARIZED_BORDER.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float altitude = segment.altitudeDownSummarizedBorder / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

				if (altitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_0_0.format(altitude));
				}
			}
		});
	}

	/**
	 * column: total altitude down (m/ft)
	 */
	private void defineColumn_AltitudeDownSummarizedComputed(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_DOWN_SUMMARIZED_COMPUTED.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float altitude = segment.altitudeDownSummarizedComputed / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
				if (altitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_0_0.format(altitude));
				}
			}
		});
	}

	/**
	 * column: altitude up m/h
	 */
	private void defineColumn_AltitudeUpHour(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_UP_H.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeUpHour / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)
							/ segment.drivingTime
							* 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(_nf_1_0.format(result));
					}
				}
			}
		});
	}

	/**
	 * column: total altitude up (m/ft)
	 */
	private void defineColumn_AltitudeUpSummarizedBorder(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_UP_SUMMARIZED_BORDER.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float altitude = segment.altitudeUpSummarizedBorder / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
				if (altitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_0_0.format(altitude));
				}
			}
		});
	}

	/**
	 * column: total altitude up (m/ft)
	 */
	private void defineColumn_AltitudeUpSummarizedComputed(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.ALTITUDE_UP_SUMMARIZED_COMPUTED.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float altitude = segment.altitudeUpSummarizedComputed / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
				if (altitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_0_0.format(altitude));
				}
			}
		});
	}

	/**
	 * column: Average cadence
	 */
	private void defineColumn_AvgCadence() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_CADENCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_CADENCE);
				_segmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final float cadence = ((TourSegment) cell.getElement()).cadence;

				if (cadence == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(cadence));
				}
			}
		});
	}

	/**
	 * column: average pace
	 */
	private void defineColumn_AvgPace() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_PACE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				_segmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float pace = segment.pace;

				if (pace == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(net.tourbook.ui.UI.format_mm_ss((long) pace));
				}
			}
		});
	}

	/**
	 * column: pace difference
	 */
	private void defineColumn_AvgPaceDifference() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_PACE_DIFFERENCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float paceDiff = segment.paceDiff;
				if (paceDiff == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(net.tourbook.ui.UI.format_mm_ss((long) paceDiff));
				}
			}
		});
	}

	/**
	 * column: average pulse
	 */
	private void defineColumn_AvgPulse() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_PULSE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_PULSE);
				_segmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final float pulse = ((TourSegment) cell.getElement()).pulse;

				if (pulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(pulse));
				}
			}
		});
	}

	/**
	 * column: pulse difference
	 */
	private void defineColumn_AvgPulseDifference() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_PULSE_DIFFERENCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final float pulseDiff = ((TourSegment) cell.getElement()).pulseDiff;

				if (pulseDiff == Integer.MIN_VALUE) {
					cell.setText(UI.EMPTY_STRING);
				} else if (pulseDiff == 0) {
					cell.setText(UI.DASH);
				} else {
					cell.setText(Integer.toString((int) pulseDiff));
				}
			}
		});
	}

	/**
	 * column: average speed
	 */
	private void defineColumn_AvgSpeed() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.AVG_SPEED.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				_segmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float speed = segment.speed;
				if (speed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(speed));
				}
			}
		});
	}

	/**
	 * column: distance (km/mile)
	 */
	private void defineColumn_Distance(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.DISTANCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float distance = (segment.distanceDiff) / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
				if (distance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_3_3.format(distance));
				}
			}
		});
	}

	/**
	 * column: TOTAL distance (km/mile)
	 */
	private void defineColumn_DistanceTotal(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.DISTANCE_TOTAL.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float distance = (segment.distanceTotal) / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
				if (distance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_3_3.format(distance));
				}
			}
		});
	}

	/**
	 * column: driving time
	 */
	private void defineColumn_DrivingTime(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.DRIVING_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				final int drivingTime = segment.drivingTime;
				if (drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(drivingTime));
				}
			}
		});
	}

	/**
	 * column: gradient
	 */
	private void defineColumn_Gradient() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.GRADIENT.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) _segmentViewer.getSorter()).setSortColumn(COLUMN_GRADIENT);
				_segmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				final float gradient = segment.gradient;

				if (gradient == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf_1_1.format(gradient));
				}
			}
		});
	}

	/**
	 * column: break time
	 */
	private void defineColumn_PausedTime(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.PAUSED_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				final int breakTime = segment.breakTime;
				if (breakTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(breakTime));
				}
			}
		});
	}

	/**
	 * column: recording time
	 */
	private void defineColumn_RecordingTime(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.RECORDING_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(segment.recordingTime));
			}
		});
	}

	/**
	 * column: TOTAL recording time
	 */
	private void defineColumn_RecordingTimeTotal(final SelectionAdapter defaultColumnSelectionListener) {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.RECORDING_TIME_TOTAL.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setColumnSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(segment.timeTotal));
			}
		});
	}

	/**
	 * column: data serie start/end index
	 */
	private void defineColumn_SerieStartEndIndex() {

		final ColumnDefinition colDef;

		colDef = TableColumnFactory.SERIE_START_END_INDEX.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				int startIndex = segment.serieIndexStart;
				final int endIndex = segment.serieIndexEnd;

				if (startIndex > 0) {
					startIndex++;
				}

				cell.setText(startIndex == endIndex ? //
						Integer.toString(startIndex)
						: startIndex + UI.DASH_WITH_SPACE + endIndex);
			}
		});
	}

	@Override
	public void dispose() {

		final IWorkbenchPage wbPage = getSite().getPage();
		wbPage.removePostSelectionListener(_postSelectionListener);
		wbPage.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		super.dispose();
	}

	private void enableActions() {

		final boolean isTourAvailable = _tourData != null;

		_actionTCSegmenterConfig.setEnabled(isTourAvailable);
	}

	private void fillToolbar() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(_actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(_actionTCSegmenterConfig);

		tbm.update(true);
	}

	/**
	 * Notify listeners to show/hide the segments.
	 */
	void fireSegmentLayerChanged() {

		if (_tourData == null) {
			// prevent NPE
			return;
		}

		/*
		 * Ensure segments are created because they can be null when tour is saved and a new
		 * instance is displayed
		 */
		final boolean isSegmentLayerVisible = Util.getStateBoolean(
				_state,
				STATE_IS_SHOW_TOUR_SEGMENTS,
				STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT);
		if (isSegmentLayerVisible && _tourData.segmentSerieIndex == null) {
			createSegments(false);
		}

		// show/hide the segments in the chart
		TourManager.fireEventWithCustomData(//
				TourEventId.SEGMENT_LAYER_CHANGED,
				null,
				TourSegmenterView.this);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	/**
	 * @return Returns distance in meters from the spinner control
	 */
	private float getDistance() {

		final float selectedDistance = _spinnerDistance.getSelection();
		float spinnerDistance;

		if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == net.tourbook.ui.UI.UNIT_MILE) {

			// miles are displayed

			spinnerDistance = (selectedDistance) * 1000 / 8;

			if (spinnerDistance == 0) {
				spinnerDistance = 1000 / 8;
			}

			// convert mile -> meters
			spinnerDistance *= net.tourbook.ui.UI.UNIT_MILE;

		} else {

			// meters are displayed

			spinnerDistance = selectedDistance * MAX_DISTANCE_SPINNER_METRIC;

			// ensure the distance in not below 100m
			if (spinnerDistance < 100) {
				spinnerDistance = 100;
			}
		}

		return spinnerDistance;
	}

	/**
	 * DB tolerance is saved with factor 10.
	 * 
	 * @return Return tour tolerance divided by 10.
	 */
	private float getDPTolerance_FromTour() {

		return (float) (_tourData.getDpTolerance() / 10.0);
	}

	private BreakTimeMethod getSelectedBreakMethod() {

		int selectedIndex = _comboBreakMethod.getSelectionIndex();

		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		return BreakTimeTool.BREAK_TIME_METHODS[selectedIndex];
	}

	private TourSegmenter getSelectedSegmenter() {

		if (_availableSegmenter.size() == 0) {
			return null;
		}

		final int selectedIndex = _comboSegmenterType.getSelectionIndex();

		if (selectedIndex != -1) {
			return _availableSegmenter.get(selectedIndex);
		}

		// should not happen
		return null;
	}

	@Override
	public ColumnViewer getViewer() {
		return _segmentViewer;
	}

	private void onChangeBreakTime() {

		createSegments(true);
	}

	private void onChangedDistance() {

		updateUIDistance();

		createSegments(true);
	}

	private void onChangedTolerance() {

		final float dpToleranceAlti = (float) (_spinnerDPToleranceAltitude.getSelection() / 10.0);
		final float dpTolerancePulse = (float) (_spinnerDPTolerancePulse.getSelection() / 10.0);

		// check if tolerance has changed
		if (_tourData == null || (_dpToleranceAltitude == dpToleranceAlti && _dpTolerancePulse == dpTolerancePulse)) {
			return;
		}

		_dpToleranceAltitude = dpToleranceAlti;
		_dpTolerancePulse = dpTolerancePulse;

		setTourDirty();

		createSegments(true);
	}

	private void onPartClosed() {

		saveState();

		_state.put(STATE_IS_SEGMENTER_ACTIVE, false);

		fireSegmentLayerChanged();
	}

	private void onPartOpened() {

		_state.put(STATE_IS_SEGMENTER_ACTIVE, true);

		if (_tourData != null) {
			fireSegmentLayerChanged();
		}
	}

	private void onSaveTourAltitude() {

		_tourData.setTourAltUp(_altitudeUp);
		_tourData.setTourAltDown(_altitudeDown);

		// update tolerance into the tour data
		_tourData.setDpTolerance((short) (_dpToleranceAltitude * 10));

		_isTourDirty = true;

		_tourData = saveTour();

		// create segments with newly saved tour that it can be displayed in the tour chart
		createSegments(false);
		updateUIAltitude();
	}

	private void onSelectBreakTimeMethod() {

		final BreakTimeMethod selectedBreakMethod = getSelectedBreakMethod();

		if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

			_pageBookBreakTime.showPage(_pageBreakByAvgSpeed);
			_comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

			_pageBookBreakTime.showPage(_pageBreakBySliceSpeed);
			_comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeBySliceSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

			_pageBookBreakTime.showPage(_pageBreakByAvgSliceSpeed);
			_comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSliceSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

			_pageBookBreakTime.showPage(_pageBreakByTimeDistance);
			_comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByTime);
		}

		// break method pages have different heights, enforce layout of the whole view part
		_pageSegmenter.layout(true, true);

		onChangeBreakTime();
	}

	/**
	 * handle a tour selection event
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final ISelection selection) {

		_isClearView = false;

		if (_isSaving) {
			return;
		}

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				+ ("\tonSelectionChanged:\t" + selection));
		// TODO remove SYSTEM.OUT.PRINTLN

		/*
		 * run selection async because a tour could be modified and needs to be saved, modifications
		 * are not reported to the tour data editor, saving needs also to be asynch with the tour
		 * data editor
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// check if view is disposed
				if (_pageBookUI.isDisposed() || _isClearView) {
					return;
				}

				TourData eventTourData = null;
				TourChart eventTourChart = null;

				if (selection instanceof SelectionTourData) {

					final SelectionTourData selectionTourData = (SelectionTourData) selection;

					eventTourData = selectionTourData.getTourData();
					eventTourChart = selectionTourData.getTourChart();

				} else if (selection instanceof SelectionTourId) {

					final SelectionTourId tourIdSelection = (SelectionTourId) selection;

					if (_tourData != null) {
						if (_tourData.getTourId().equals(tourIdSelection.getTourId())) {
							// don't reload the same tour
							return;
						}
					}

					eventTourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

				} else if (selection instanceof SelectionTourIds) {

					final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
					if (tourIds != null && tourIds.size() > 0) {

						final Long tourId = tourIds.get(0);

						if (_tourData != null) {
							if (_tourData.getTourId().equals(tourId)) {
								// don't reload the same tour
								return;
							}
						}

						eventTourData = TourManager.getInstance().getTourData(tourId);
					}

				} else if (selection instanceof SelectionDeletedTours) {

					clearView();

				} else {
					return;
				}

				if (checkDataValidation(eventTourData) == false) {
					return;
				}

				if (_tourData != null) {
					// it's possible that the break time serie was overwritten
					_tourData.setBreakTimeSerie(null);
				}

				/*
				 * save previous tour when a new tour is selected
				 */
				if (_tourData != null && _tourData == eventTourData) {

					// nothing to do, it's the same tour

				} else {

// disabled, tour is only saved with the save button since v14.7
//					final TourData savedTour = saveTour();
//					if (savedTour != null) {
//
//						/*
//						 * when a tour is saved, the change notification is not fired because
//						 * another tour is already selected, but to update the tour in a TourViewer,
//						 * a change nofification must be fired afterwords
//						 */
////				Display.getCurrent().asyncExec(new Runnable() {
////					public void run() {
////						TourManager.fireEvent(TourEventId.TOUR_CHANGED,
////								new TourEvent(savedTour),
////								TourSegmenterView.this);
////					}
////				});
//					}

					if (eventTourChart == null) {
						eventTourChart = TourManager.getActiveTourChart(eventTourData);
					}

					_tourChart = eventTourChart;

					setTour(eventTourData, false);
				}
			}
		});
	}

	private void onSelectSegmenterType(final boolean isUserSelected) {

		final TourSegmenter selectedSegmenter = getSelectedSegmenter();
		if (selectedSegmenter == null) {
			clearView();
			return;
		}

		final SegmenterType selectedSegmenterType = selectedSegmenter.segmenterType;

		/*
		 * keep segmenter type which the user selected, try to reselect this segmenter when a new
		 * tour is displayed
		 */
		if (isUserSelected) {
			_userSelectedSegmenterType = selectedSegmenterType;
		}

		if (selectedSegmenterType == SegmenterType.ByAltitudeWithDP
				|| selectedSegmenterType == SegmenterType.ByAltitudeWithDPMerged) {

			_pageBookSegmenter.showPage(_pageSegTypeDPAltitude);

		} else if (selectedSegmenterType == SegmenterType.ByPulseWithDP) {

			_pageBookSegmenter.showPage(_pageSegTypeDPPulse);

		} else if (selectedSegmenterType == SegmenterType.ByMarker) {

			_pageBookSegmenter.showPage(_pageSegTypeByMarker);

		} else if (selectedSegmenterType == SegmenterType.ByDistance) {

			_pageBookSegmenter.showPage(_pageSegTypeByDistance);

		} else if (selectedSegmenterType == SegmenterType.ByComputedAltiUpDown) {

			_pageBookSegmenter.showPage(_pageSegTypeByAltiUpDown);

		} else if (selectedSegmenterType == SegmenterType.ByBreakTime) {

			_pageBookSegmenter.showPage(_pageSegTypeByBreakTime);

			// update ui + layout
			onSelectBreakTimeMethod();
		}

		_pageSegmenter.layout();

		createSegments(true);
	}

	private void onSetDefaults(final Composite parent) {

		saveBreakTimeValuesInPrefStore();

		PreferencesUtil.createPreferenceDialogOn(
				parent.getShell(),
				PrefPageComputedValues.ID,
				null,
				PrefPageComputedValues.TAB_FOLDER_BREAK_TIME).open();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_containerViewer.setRedraw(false);
		{
			_segmentViewer.getTable().dispose();

			createUI_80_SegmentViewer(_containerViewer);
			_containerViewer.layout();

			// update the viewer
			reloadViewer();
		}
		_containerViewer.setRedraw(true);

		return _segmentViewer;
	}

	@Override
	public void reloadViewer() {
		// force input to be reloaded
		_segmentViewer.setInput(new Object[0]);
	}

	/**
	 * defaults are the values which are stored in the pref store not the default-default which can
	 * be set in the pref page
	 */
	private void restorePrefValues() {

		final BreakTimeTool btConfig = BreakTimeTool.getPrefValues();

		/*
		 * break method
		 */
		selectBreakMethod(btConfig.breakTimeMethodId);

		/*
		 * break by avg + slice speed
		 */
		//
		_spinnerBreakMinAvgSpeedAS.setSelection(//
				(int) (btConfig.breakMinAvgSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinSliceSpeedAS.setSelection(//
				(int) (btConfig.breakMinSliceSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinSliceTimeAS.setSelection(btConfig.breakMinSliceTimeAS);

		/*
		 * break time by time/distance
		 */
		_spinnerBreakShortestTime.setSelection(btConfig.breakShortestTime);

		final float prefBreakDistance = btConfig.breakMaxDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
		_spinnerBreakMaxDistance.setSelection((int) (prefBreakDistance + 0.5));

		_spinnerBreakSliceDiff.setSelection(btConfig.breakSliceDiff);

		/*
		 * break time by speed
		 */
		_spinnerBreakMinSliceSpeed.setSelection(//
				(int) (btConfig.breakMinSliceSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinAvgSpeed.setSelection(//
				(int) (btConfig.breakMinAvgSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

		onSelectBreakTimeMethod();
	}

	private void restoreState() {

		/*
		 * Actions
		 */
		_actionTCSegmenterConfig.setSelected(Util.getStateBoolean(
				_state,
				STATE_IS_SHOW_TOUR_SEGMENTS,
				STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT));

		// selected segmenter
		final String stateSegmenterName = Util.getStateString(
				_state,
				STATE_SELECTED_SEGMENTER_BY_USER,
				SegmenterType.ByAltitudeWithDP.name());
		try {
			_userSelectedSegmenterType = SegmenterType.valueOf(SegmenterType.class, stateSegmenterName);
		} catch (final Exception e) {
			// set default value
			_userSelectedSegmenterType = SegmenterType.ByAltitudeWithDP;
		}

		// selected distance
		final int stateDistance = Util.getStateInt(_state, STATE_SELECTED_DISTANCE, 10);
		_spinnerDistance.setSelection(stateDistance);

		updateUIDistance();

		_spinnerMinAltitude.setSelection(Util.getStateInt(_state, STATE_MINIMUM_ALTITUDE, 50));

		// DP tolerance pulse
		final int stateDPTolerancePulse = Util.getStateInt(_state, STATE_DP_TOLERANCE_PULSE, 50);
		_dpTolerancePulse = stateDPTolerancePulse / 10.0f;
		_spinnerDPTolerancePulse.setSelection(stateDPTolerancePulse);

		/*
		 * break time
		 */
		final BreakTimeTool btConfig = BreakTimeTool.getPrefValues();

		/*
		 * break method
		 */
		selectBreakMethod(Util.getStateString(
				_state,
				STATE_SELECTED_BREAK_METHOD2,
				BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED));

		/*
		 * break by avg + slice speed
		 */
		final float stateAvgSpeedAS = Util.getStateFloat(_state, //
				STATE_BREAK_TIME_MIN_AVG_SPEED_AS,
				btConfig.breakMinAvgSpeedAS);
		final float stateSliceSpeedAS = Util.getStateFloat(_state, //
				STATE_BREAK_TIME_MIN_SLICE_SPEED_AS,
				btConfig.breakMinSliceSpeedAS);
		final int stateSliceTimeAS = Util.getStateInt(_state, //
				STATE_BREAK_TIME_MIN_SLICE_TIME_AS,
				btConfig.breakMinSliceTimeAS);

		_spinnerBreakMinAvgSpeedAS.setSelection(//
				(int) (stateAvgSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinSliceSpeedAS.setSelection(//
				(int) (stateSliceSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinSliceTimeAS.setSelection(stateSliceTimeAS);

		/*
		 * break by slice speed
		 */
		final float stateSliceSpeed = Util.getStateFloat(
				_state,
				STATE_BREAK_TIME_MIN_SLICE_SPEED,
				btConfig.breakMinSliceSpeed);

		_spinnerBreakMinSliceSpeed
				.setSelection((int) (stateSliceSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

		/*
		 * break by avg speed
		 */
		final float stateAvgSpeed = Util.getStateFloat(
				_state,
				STATE_BREAK_TIME_MIN_AVG_SPEED,
				btConfig.breakMinAvgSpeed);

		_spinnerBreakMinAvgSpeed
				.setSelection((int) (stateAvgSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

		/*
		 * break time by time/distance
		 */
		_spinnerBreakShortestTime.setSelection(Util.getStateInt(
				_state,
				STATE_BREAK_TIME_MIN_TIME_VALUE,
				btConfig.breakShortestTime));

		final float breakDistance = Util.getStateFloat(
				_state,
				STATE_BREAK_TIME_MIN_DISTANCE_VALUE,
				btConfig.breakMaxDistance) / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
		_spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));

		_spinnerBreakSliceDiff.setSelection(Util.getStateInt(
				_state,
				STATE_BREAK_TIME_SLICE_DIFF,
				btConfig.breakSliceDiff));
	}

	/**
	 * saves break time values in the pref store
	 */
	private void saveBreakTimeValuesInPrefStore() {

		// break method
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_METHOD2, getSelectedBreakMethod().methodId);

		// break by avg+slice speed
		final float breakMinAvgSpeedAS = _spinnerBreakMinAvgSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		final float breakMinSliceSpeedAS = _spinnerBreakMinSliceSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		final int breakMinSliceTimeAS = _spinnerBreakMinSliceTimeAS.getSelection();
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

		// break by slice speed
		final float breakMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

		// break by avg speed
		final float breakMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

		// break by time/distance
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, _spinnerBreakShortestTime.getSelection());
		final float breakDistance = _spinnerBreakMaxDistance.getSelection()
				* net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, breakDistance);
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_SLICE_DIFF, _spinnerBreakSliceDiff.getSelection());
	}

	/**
	 * save break time values in the viewer settings
	 */
	private void saveBreakTimeValuesInState() {

		// break method
		_state.put(STATE_SELECTED_BREAK_METHOD2, getSelectedBreakMethod().methodId);

		// break by avg+slice speed
		final float breakMinAvgSpeedAS = _spinnerBreakMinAvgSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		final float breakMinSliceSpeedAS = _spinnerBreakMinSliceSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		final int breakMinSliceTimeAS = _spinnerBreakMinSliceTimeAS.getSelection();

		_state.put(STATE_BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
		_state.put(STATE_BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
		_state.put(STATE_BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

		// break by slice speed
		final float breakMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		_state.put(STATE_BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

		// break by avg speed
		final float breakMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
		_state.put(STATE_BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

		// break by time/distance
		final float breakDistance = _spinnerBreakMaxDistance.getSelection()
				* net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
		_state.put(STATE_BREAK_TIME_MIN_DISTANCE_VALUE, breakDistance);
		_state.put(STATE_BREAK_TIME_MIN_TIME_VALUE, _spinnerBreakShortestTime.getSelection());
		_state.put(STATE_BREAK_TIME_SLICE_DIFF, _spinnerBreakSliceDiff.getSelection());
	}

	private void saveState() {

		_columnManager.saveState(_state);

		_state.put(STATE_SELECTED_SEGMENTER_BY_USER, _userSelectedSegmenterType.name());
		_state.put(STATE_SELECTED_DISTANCE, _spinnerDistance.getSelection());
		_state.put(STATE_MINIMUM_ALTITUDE, _spinnerMinAltitude.getSelection());
		_state.put(STATE_DP_TOLERANCE_PULSE, _spinnerDPTolerancePulse.getSelection());

		saveBreakTimeValuesInState();
	}

	private TourData saveTour() {

		if (_tourData != null) {
			// it's possible that the break time serie was overwritten
			_tourData.setBreakTimeSerie(null);
		}

		if (_isTourDirty == false || _tourData == null || _savedDpToleranceAltitude == -1) {
			// nothing to do
			return null;
		}

		TourData savedTour;
		_isSaving = true;
		{
			savedTour = TourManager.saveModifiedTour(_tourData);
		}
		_isSaving = false;

		_isTourDirty = false;

		return savedTour;
	}

	private void selectBreakMethod(final String methodId) {

		final BreakTimeMethod[] breakMethods = BreakTimeTool.BREAK_TIME_METHODS;

		int selectionIndex = -1;

		for (int methodIndex = 0; methodIndex < breakMethods.length; methodIndex++) {
			if (breakMethods[methodIndex].methodId.equals(methodId)) {
				selectionIndex = methodIndex;
				break;
			}
		}

		if (selectionIndex == -1) {
			selectionIndex = 0;
		}

		_comboBreakMethod.select(selectionIndex);
	}

	private void setCellColor(final ViewerCell cell, final float altiDiff) {

		if (altiDiff == 0) {
			cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		} else if (altiDiff > 0) {
			cell.setBackground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_BG_SEGMENTER_UP));
		} else if (altiDiff < 0) {
			cell.setBackground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_BG_SEGMENTER_DOWN));
		}
	}

	@Override
	public void setFocus() {

	}

	private void setMaxDistanceSpinner() {

		if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == net.tourbook.ui.UI.UNIT_MILE) {

			// imperial

			_maxDistanceSpinner = MAX_DISTANCE_SPINNER_MILE;
			_spinnerDistancePage = 8;

		} else {

			// metric

			_maxDistanceSpinner = MAX_DISTANCE_SPINNER_METRIC;
			_spinnerDistancePage = 10;
		}
	}

	/**
	 * Sets the tour for the segmenter
	 * 
	 * @param tourData
	 * @param forceUpdate
	 */
	private void setTour(final TourData tourData, final boolean forceUpdate) {

		if (tourData == null || (forceUpdate == false && tourData == _tourData)) {
			return;
		}

		_isDirtyDisabled = true;
		{
			_tourData = tourData;

			_pageBookUI.showPage(_pageSegmenter);
			enableActions();

			// update tour title
			_lblTitle.setText(TourManager.getTourTitleDetailed(_tourData));

			// keep original dp tolerance
			_savedDpToleranceAltitude = _dpToleranceAltitude = getDPTolerance_FromTour();

			// segmenter value
			_spinnerDPToleranceAltitude.setSelection((int) (getDPTolerance_FromTour() * 10));

			final boolean canSaveTour = _tourData.getTourPerson() != null;
			_btnSaveTourDP.setEnabled(canSaveTour);
			_btnSaveTourMin.setEnabled(canSaveTour);
		}
		_isDirtyDisabled = false;

		updateUISegmenterSelector();
		onSelectSegmenterType(false);
	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

		if (_isDirtyDisabled) {
			return;
		}

		if (_tourData != null && _savedDpToleranceAltitude != getDPTolerance_FromTour()) {
			_isTourDirty = true;
		}
	}

	private void showTour() {

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// when previous onSelectionChanged did not display a tour, get tour from tour manager
		if (_tourData == null) {
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
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
	 * update ascending altitude computed value
	 */
	private void updateUIAltitude() {

		final TourSegmenter selectedSegmenter = getSelectedSegmenter();
		if (selectedSegmenter == null) {
			clearView();
			return;
		}

		Label lblInfo;
		float[] altitudeSegments;

		if (selectedSegmenter.segmenterType == SegmenterType.ByComputedAltiUpDown) {

			// Minimum altitude

			altitudeSegments = _tourData.segmentSerieComputedAltitudeDiff;
			lblInfo = _lblAltitudeUpMin;

		} else {

			// DP tolerance

			altitudeSegments = _tourData.segmentSerieAltitudeDiff;
			lblInfo = _lblAltitudeUpDP;
		}

		if (altitudeSegments == null) {
			lblInfo.setText(UI.EMPTY_STRING);
			lblInfo.setToolTipText(UI.EMPTY_STRING);
			return;
		}

		// compute total alti up/down from the segments
		_altitudeUp = 0;
		_altitudeDown = 0;

		for (final float altitude : altitudeSegments) {
			if (altitude > 0) {
				_altitudeUp += altitude;
			} else {
				_altitudeDown += -altitude;
			}
		}

		/*
		 * Show altitude values not as negative values because the values are displayed left aligned
		 * and it's easier to compare them visually when a minus sign is not displayed.
		 */
		final float compAltiUp = _altitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
		final float compAltiDown = _altitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
		final int tourAltiUp = Math.round(_tourData.getTourAltUp() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);
		final int tourAltiDown = Math.round(_tourData.getTourAltDown() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);

		lblInfo.setText(String.format(
				FORMAT_ALTITUDE_DIFF,
				Math.round(compAltiUp),
				tourAltiUp,
				net.tourbook.common.UI.UNIT_LABEL_ALTITUDE));

		lblInfo.setToolTipText(NLS.bind(Messages.Tour_Segmenter_Label_AltitudeUpDown_Tooltip, new Object[] {

				// Up
				_nf_1_1.format(compAltiUp),
				tourAltiUp,
				net.tourbook.common.UI.UNIT_LABEL_ALTITUDE,
				//
				// Down
				_nf_1_1.format(compAltiDown),
				tourAltiDown,
				net.tourbook.common.UI.UNIT_LABEL_ALTITUDE,
				//
				// Diff
				_nf_1_1.format(compAltiUp - compAltiDown),
				tourAltiUp - tourAltiDown,
				net.tourbook.common.UI.UNIT_LABEL_ALTITUDE,

				// DP
				_nf_1_1.format(_dpToleranceAltitude),
				_nf_1_1.format(_tourData.getDpTolerance() / 10.0f),
		//
				}));
	}

	private void updateUIBreakTime() {

		_lblTourBreakTime.setText(Long.toString(_tourBreakTime)
				+ UI.SPACE
				+ Messages.App_Unit_Seconds_Small
				+ UI.SPACE4
				+ net.tourbook.ui.UI.format_hh_mm_ss(_tourBreakTime));

		_containerBreakTime.layout();
	}

	private void updateUIDistance() {

		float spinnerDistance = getDistance() / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

		if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == net.tourbook.ui.UI.UNIT_MILE) {

			// imperial

			spinnerDistance /= 1000;

			final int distanceInt = (int) spinnerDistance;
			final float distanceFract = spinnerDistance - distanceInt;

			// create distance for imperials which shows the fraction with 1/8, 1/4, 3/8 ...
			final StringBuilder sb = new StringBuilder();

			if (distanceInt > 0) {
				sb.append(Integer.toString(distanceInt));
				sb.append(UI.SPACE);
			}

			if (Math.abs(distanceFract - 0.125f) <= 0.01) {
				sb.append(DISTANCE_MILES_1_8);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.25f) <= 0.01) {
				sb.append(DISTANCE_MILES_1_4);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.375) <= 0.01) {
				sb.append(DISTANCE_MILES_3_8);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.5f) <= 0.01) {
				sb.append(DISTANCE_MILES_1_2);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.625) <= 0.01) {
				sb.append(DISTANCE_MILES_5_8);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.75f) <= 0.01) {
				sb.append(DISTANCE_MILES_3_4);
				sb.append(UI.SPACE);
			} else if (Math.abs(distanceFract - 0.875) <= 0.01) {
				sb.append(DISTANCE_MILES_7_8);
				sb.append(UI.SPACE);
			}

			sb.append(net.tourbook.common.UI.UNIT_LABEL_DISTANCE);

			// update UI
			_lblDistanceValue.setText(sb.toString());

		} else {

			// metric

			// update UI, the spinner already displays the correct value
			_lblDistanceValue.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE);
		}
	}

	private void updateUISegmenterSelector() {

		final TourSegmenter currentSegmenter = getSelectedSegmenter();
		final int availableSegmenterData = checkSegmenterData(_tourData);

		// get all segmenters which can segment current tour
		_availableSegmenter.clear();
		for (final TourSegmenter tourSegmenter : _allTourSegmenter) {

			final int requiredDataSeries = tourSegmenter.requiredDataSeries;

			if ((availableSegmenterData & requiredDataSeries) == requiredDataSeries) {
				_availableSegmenter.add(tourSegmenter);
			}
		}

		// sort by name
//		Collections.sort(_availableSegmenter);

		/*
		 * fill list box
		 */
		int segmenterIndex = 0;
		int previousSegmenterIndex = -1;
		int userSelectedSegmenterIndex = -1;

		_comboSegmenterType.removeAll();
		for (final TourSegmenter tourSegmenter : _availableSegmenter) {

			_comboSegmenterType.add(tourSegmenter.name);

			if (tourSegmenter.segmenterType == _userSelectedSegmenterType) {
				userSelectedSegmenterIndex = segmenterIndex;
			}
			if (tourSegmenter == currentSegmenter) {
				previousSegmenterIndex = segmenterIndex;
			}

			segmenterIndex++;
		}

		// reselect previous segmenter
		if (userSelectedSegmenterIndex != -1) {
			_comboSegmenterType.select(userSelectedSegmenterIndex);
		} else {
			if (previousSegmenterIndex != -1) {
				_comboSegmenterType.select(previousSegmenterIndex);
			} else {
				_comboSegmenterType.select(0);
			}
		}
	}
}
