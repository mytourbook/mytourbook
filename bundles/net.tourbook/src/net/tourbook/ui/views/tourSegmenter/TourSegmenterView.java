/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import net.tourbook.Messages;
import net.tourbook.algorithm.DPPoint;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
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

/**
 *
 */
public class TourSegmenterView extends ViewPart implements ITourViewer {

   public static final String ID = "net.tourbook.views.TourSegmenter"; //$NON-NLS-1$

   //
   private static final float  UNIT_MILE                                          = net.tourbook.ui.UI.UNIT_MILE;
   private static final float  UNIT_YARD                                          = net.tourbook.ui.UI.UNIT_YARD;
   //
   private static final String DISTANCE_MILES_1_8                                 = "1/8";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_1_4                                 = "1/4";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_3_8                                 = "3/8";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_1_2                                 = "1/2";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_5_8                                 = "5/8";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_3_4                                 = "3/4";                                        //$NON-NLS-1$
   private static final String DISTANCE_MILES_7_8                                 = "7/8";                                        //$NON-NLS-1$
   //
   private static final String FORMAT_ALTITUDE_DIFF                               = "%d / %d %s";                                 //$NON-NLS-1$
   //
   private static final int    SEGMENTER_REQUIRES_ALTITUDE                        = 0x01;
   private static final int    SEGMENTER_REQUIRES_DISTANCE                        = 0x02;
   private static final int    SEGMENTER_REQUIRES_PULSE                           = 0x04;
   private static final int    SEGMENTER_REQUIRES_MARKER                          = 0x08;
   private static final int    SEGMENTER_REQUIRES_POWER                           = 0x10;
   //
   private static final int    MAX_DISTANCE_SPINNER_MILE                          = 80;
   private static final int    MAX_DISTANCE_SPINNER_METRIC                        = 100;
   //
   private static final String STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS         = "STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS"; //$NON-NLS-1$
   private static final int    STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS_DEFAULT = 100;
   private static final String STATE_DP_TOLERANCE_POWER                           = "STATE_DP_TOLERANCE_POWER";                   //$NON-NLS-1$
   private static final String STATE_DP_TOLERANCE_PULSE                           = "STATE_DP_TOLERANCE_PULSE";                   //$NON-NLS-1$
   private static final String STATE_MINIMUM_ALTITUDE                             = "STATE_MINIMUM_ALTITUDE";                     //$NON-NLS-1$
   private static final String STATE_SELECTED_DISTANCE                            = "selectedDistance";                           //$NON-NLS-1$
   private static final String STATE_SELECTED_SEGMENTER_BY_USER                   = "STATE_SELECTED_SEGMENTER_BY_USER";           //$NON-NLS-1$
   //
   /**
    * Initially this was an int value, with 2 it's a string.
    */
   private static final String STATE_SELECTED_BREAK_METHOD2                       = "selectedBreakMethod2";                       //$NON-NLS-1$
   //
   private static final String STATE_BREAK_TIME_MIN_AVG_SPEED_AS                  = "selectedBreakTimeMinAvgSpeedAS";             //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_SLICE_SPEED_AS                = "selectedBreakTimeMinSliceSpeedAS";           //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_SLICE_TIME_AS                 = "selectedBreakTimeMinSliceTimeAS";            //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_AVG_SPEED                     = "selectedBreakTimeMinAvgSpeed";               //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_SLICE_SPEED                   = "selectedBreakTimeMinSliceSpeed";             //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_DISTANCE_VALUE                = "selectedBreakTimeMinDistance";               //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_MIN_TIME_VALUE                    = "selectedBreakTimeMinTime";                   //$NON-NLS-1$
   private static final String STATE_BREAK_TIME_SLICE_DIFF                        = "selectedBreakTimeSliceDiff";                 //$NON-NLS-1$
   //
   /*
    * Tour segmenter
    */
   public static final String  STATE_IS_SEGMENTER_ACTIVE                      = "STATE_IS_SEGMENTER_ACTIVE";              //$NON-NLS-1$
   public static final String  STATE_IS_HIDE_SMALL_VALUES                     = "STATE_IS_HIDE_SMALL_VALUES";             //$NON-NLS-1$
   public static final boolean STATE_IS_HIDE_SMALL_VALUES_DEFAULT             = true;
   public static final String  STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES         = "STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES"; //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT = false;
   public static final String  STATE_IS_SHOW_SEGMENTER_LINE                   = "STATE_IS_SHOW_SEGMENTER_LINE";           //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_SEGMENTER_LINE_DEFAULT           = true;
   public static final String  STATE_IS_SHOW_SEGMENTER_MARKER                 = "STATE_IS_SHOW_SEGMENTER_MARKER";         //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT         = false;
   public static final String  STATE_IS_SHOW_SEGMENTER_TOOLTIP                = "STATE_IS_SHOW_SEGMENTER_TOOLTIP";        //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_SEGMENTER_TOOLTIP_DEFAULT        = true;
   public static final String  STATE_IS_SHOW_SEGMENTER_VALUE                  = "STATE_IS_SHOW_SEGMENTER_VALUE";          //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT          = true;
   public static final String  STATE_IS_SHOW_TOUR_SEGMENTS                    = "STATE_IS_SHOW_TOUR_SEGMENTS";            //$NON-NLS-1$
   public static final boolean STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT            = true;
   public static final String  STATE_GRAPH_OPACITY                            = "STATE_GRAPH_OPACITY";                    //$NON-NLS-1$
   public static final int     STATE_GRAPH_OPACITY_DEFAULT                    = 10;
   public static final String  STATE_LINE_OPACITY                             = "STATE_LINE_OPACITY";                     //$NON-NLS-1$
   public static final int     STATE_LINE_OPACITY_DEFAULT                     = 100;
   public static final String  STATE_SMALL_VALUE_SIZE                         = "STATE_SMALL_VALUE_SIZE";                 //$NON-NLS-1$
   public static final int     STATE_SMALL_VALUE_SIZE_DEFAULT                 = 50;
   public static final String  STATE_STACKED_VISIBLE_VALUES                   = "STATE_STACKED_VISIBLE_VALUES";           //$NON-NLS-1$
   public static final int     STATE_STACKED_VISIBLE_VALUES_DEFAULT           = 2;
   //
   /*
    * Surfing
    */
   private static final String            STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS         = "STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS";//$NON-NLS-1$
   private static final boolean           STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS_DEFAULT = false;
   private static final String            STATE_SURFING_IS_MIN_DISTANCE                        = "STATE_SURFING_IS_MIN_DISTANCE";               //$NON-NLS-1$
   private static final boolean           STATE_SURFING_IS_MIN_DISTANCE_DEFAULT                = false;
   private static final String            STATE_SURFING_MIN_SPEED_START_STOP                   = "STATE_SURFING_MIN_SPEED_START_STOP";          //$NON-NLS-1$
   private static final int               STATE_SURFING_MIN_SPEED_START_STOP_DEFAULT           = 10;
   private static final String            STATE_SURFING_MIN_SPEED_SURFING                      = "STATE_SURFING_MIN_SPEED_SURFING";             //$NON-NLS-1$
   private static final int               STATE_SURFING_MIN_SPEED_SURFING_DEFAULT              = 10;
   private static final String            STATE_SURFING_MIN_TIME_DURATION                      = "STATE_SURFING_MIN_TIME_DURATION";             //$NON-NLS-1$
   private static final int               STATE_SURFING_MIN_TIME_DURATION_DEFAULT              = 6;
   private static final String            STATE_SURFING_MIN_DISTANCE                           = "STATE_SURFING_MIN_DISTANCE";                  //$NON-NLS-1$
   private static final int               STATE_SURFING_MIN_DISTANCE_DEFAULT                   = 10;
   private static final String            STATE_SURFING_SEGMENT_FILTER                         = "STATE_SURFING_SEGMENT_FILTER";                //$NON-NLS-1$
   private static final SurfingFilterType STATE_SURFING_SEGMENT_FILTER_DEFAULT                 = SurfingFilterType.All;
   //
   /*
    * Colors
    */
   private static final String SEGMENTER_FILTER_1_BACKGROUND            = "SEGMENTER_FILTER_1_BACKGROUND";        //$NON-NLS-1$
   private static final String SEGMENTER_FILTER_1_BACKGROUND_HEADER     = "SEGMENTER_FILTER_1_BACKGROUND_HEADER"; //$NON-NLS-1$
   private static final String SEGMENTER_FILTER_2_BACKGROUND            = "SEGMENTER_FILTER_2_BACKGROUND";        //$NON-NLS-1$
   private static final String SEGMENTER_FILTER_2_BACKGROUND_HEADER     = "SEGMENTER_FILTER_2_BACKGROUND_HEADER"; //$NON-NLS-1$
   private static final RGB    SEGMENTER_FILTER_1_BACKGROUND_RGB        = new RGB(250, 255, 232);
   private static final RGB    SEGMENTER_FILTER_1_BACKGROUND_RGB_HEADER = new RGB(224, 250, 155);
   private static final RGB    SEGMENTER_FILTER_2_BACKGROUND_RGB        = new RGB(229, 242, 255);
   private static final RGB    SEGMENTER_FILTER_2_BACKGROUND_RGB_HEADER = new RGB(167, 214, 255);
   //
   static final String         STATE_COLOR_ALTITUDE_DOWN                = "STATE_COLOR_ALTITUDE_DOWN";            //$NON-NLS-1$
   static final String         STATE_COLOR_ALTITUDE_UP                  = "STATE_COLOR_ALTITUDE_UP";              //$NON-NLS-1$
   static final String         STATE_COLOR_TOTALS                       = "STATE_COLOR_TOTALS";                   //$NON-NLS-1$
   static final RGB            STATE_COLOR_ALTITUDE_DOWN_DEFAULT        = new RGB(0, 240, 0);
   static final RGB            STATE_COLOR_ALTITUDE_UP_DEFAULT          = new RGB(255, 66, 22);
   static final RGB            STATE_COLOR_TOTALS_DEFAULT               = new RGB(255, 232, 144);
   //
   //
   private static final float                    SPEED_DIGIT_VALUE = 10.0f;
   //
   private static final IPreferenceStore         _prefStore        = TourbookPlugin.getPrefStore();
   private static final IDialogSettings          _state            = TourbookPlugin.getState(ID);
   //
   /**
    * Contains all available segmenters.
    * <p>
    * The sequence defines how they are displayed in the combobox.
    */
   private static final ArrayList<TourSegmenter> _allTourSegmenter = new ArrayList<>();
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
            SegmenterType.ByAltitudeWithMarker,
            Messages.Tour_Segmenter_Type_ByAltitude_Marker,
            SEGMENTER_REQUIRES_ALTITUDE | SEGMENTER_REQUIRES_DISTANCE | SEGMENTER_REQUIRES_MARKER));

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
            SegmenterType.ByPowerWithDP,
            Messages.tour_segmenter_type_byPower,
            SEGMENTER_REQUIRES_POWER));

      _allTourSegmenter.add(new TourSegmenter(
            SegmenterType.ByPulseWithDP,
            Messages.tour_segmenter_type_byPulse,
            SEGMENTER_REQUIRES_PULSE));

      _allTourSegmenter.add(new TourSegmenter(
            SegmenterType.ByComputedAltiUpDown,
            Messages.tour_segmenter_type_byComputedAltiUpDown,
            SEGMENTER_REQUIRES_ALTITUDE));

      _allTourSegmenter.add(new TourSegmenter(
            SegmenterType.Surfing,
            Messages.Tour_Segmenter_Type_Surfing,
            SEGMENTER_REQUIRES_DISTANCE));

   }
   /**
    * This must be in sync with the columns in the {@link SegmenterComparator}
    */
   private static final ArrayList<String> _allSortableColumns = new ArrayList<>();
   static {

      _allSortableColumns.add(TableColumnFactory.DATA_SEQUENCE_ID); // this is the default sort column
      _allSortableColumns.add(TableColumnFactory.DATA_SERIE_START_END_INDEX_ID);

      _allSortableColumns.add(TableColumnFactory.ALTITUDE_GRADIENT_ID);
      _allSortableColumns.add(TableColumnFactory.BODY_AVG_PULSE_ID);
      _allSortableColumns.add(TableColumnFactory.MOTION_AVG_SPEED_ID);
      _allSortableColumns.add(TableColumnFactory.MOTION_DISTANCE_ID);
      _allSortableColumns.add(TableColumnFactory.MOTION_AVG_PACE_ID);
      _allSortableColumns.add(TableColumnFactory.POWERTRAIN_AVG_CADENCE_ID);
      _allSortableColumns.add(TableColumnFactory.TIME_RECORDING_TIME_ID);
   }
   private static final SurfingFilter[] _allSurfingSegmentFilter = new SurfingFilter[] {

         new SurfingFilter(SurfingFilterType.All, Messages.Tour_Segmenter_SurfingFilter_All),
         new SurfingFilter(SurfingFilterType.Surfing, Messages.Tour_Segmenter_SurfingFilter_Surfing),
         new SurfingFilter(SurfingFilterType.NotSurfing, Messages.Tour_Segmenter_SurfingFilter_Paddling),
   };

   private final boolean                _isOSX                   = net.tourbook.common.UI.IS_OSX;

   //
   private TableViewer             _segmentViewer;
   private SegmenterComparator     _segmentComparator        = new SegmenterComparator();
   private ColumnManager           _columnManager;
   //
   private TourData                _tourData;
   //
   private float                   _dpToleranceAltitude;
   private float                   _dpToleranceAltitudeMultipleTours;
   private float                   _dpTolerancePower;
   private float                   _dpTolerancePulse;
   //
   private float                   _savedDpToleranceAltitude = -1;
   //
   private SelectionAdapter        _columnSortListener;
   private MouseWheelListener      _defaultSurfing_MouseWheelListener;
   private SelectionAdapter        _defaultSurfing_SelectionListener;
   private IPartListener2          _partListener;
   private PostSelectionProvider   _postSelectionProvider;
   private ISelectionListener      _postSelectionListener;
   private IPropertyChangeListener _prefChangeListener;
   private ITourEventListener      _tourEventListener;
   //
   private final NumberFormat      _nf_0_0                   = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf_1_0                   = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf_1_1                   = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf_3_3                   = NumberFormat.getNumberInstance();
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
   //
   private int                            _maxDistanceSpinner;
   private int                            _spinnerDistancePage;
   //
   /**
    * when <code>true</code>, the tour dirty flag is disabled to load data into the fields
    */
   private boolean                        _isDirtyDisabled;
   private boolean                        _isClearView;
   private boolean                        _isInSelection;
   private boolean                        _isSaving;
   private boolean                        _isSegmenterFiltered;
   private boolean                        _isTourDirty        = false;
   //
   private int                            _selectedSurfingFilter;
   private float                          _altitudeUp;
   private float                          _altitudeDown;
   //
   private ArrayList<TourSegmenter>       _availableSegmenter = new ArrayList<>();
   /**
    * segmenter type which the user has selected
    */
   private SegmenterType                  _userSelectedSegmenterType;
   private long                           _tourBreakTime;
   private float                          _breakUIMinAvgSpeedAS;
   private float                          _breakUIMinSliceSpeedAS;

   private int                            _breakUIMinSliceTimeAS;

   private float                          _breakUIMinAvgSpeed;

   private float                          _breakUIMinSliceSpeed;
   private int                            _breakUIShortestBreakTime;
   private int                            _breakUISliceDiff;
   private float                          _breakUIMaxDistance;
   //
   private PixelConverter                 _pc;
   private int                            _spinnerWidth;
   //
   /**
    * contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the differenct section to the same width
    */
   private final ArrayList<Control>       _firstColBreakTime  = new ArrayList<>();
   //
   private ActionModifyColumns            _actionModifyColumns;
   private ActionTourChartSegmenterConfig _actionTourChartSegmenterConfig;
   //
   private boolean                        _isGetInitialTours;
   private ArrayList<TourSegment>         _allTourSegments;
   /*
    * UI resources
    */
   private final ColorCache               _colorCache         = new ColorCache();
   /*
    * UI controls
    */
   private Composite                      _parent;
   //
   private PageBook                       _pageBookUI;
   private PageBook                       _pageBookSegmenter;
   private PageBook                       _pageBookBreakTime;
   //
   private Button                         _btnSaveTourDP;
   private Button                         _btnSaveTourMin;
   //
   private Composite                      _containerBreakTime;
   private Composite                      _viewerContainer;
   //
   private Composite                      _pageSegmenter;
   private Composite                      _pageBreakBy_AvgSliceSpeed;
   private Composite                      _pageBreakBy_AvgSpeed;
   private Composite                      _pageBreakBy_SliceSpeed;
   private Composite                      _pageBreakBy_TimeDistance;
   private Composite                      _pageNoData;
   private Composite                      _pageSegType_ByAltiUpDown;
   private Composite                      _pageSegType_ByBreakTime;
   private Composite                      _pageSegType_ByDistance;
   private Composite                      _pageSegType_ByMarker;
   private Composite                      _pageSegType_DPAltitude;
   private Composite                      _pageSegType_DPPower;
   private Composite                      _pageSegType_DPPulse;
   private Composite                      _pageSegType_Surfing;
   //
   private Button                         _btnSurfing_DeleteTourSegments;
   private Button                         _btnSurfing_RestoreFrom_Defaults;
   private Button                         _btnSurfing_RestoreFrom_Tour;
   private Button                         _btnSurfing_SaveTourSegments;
   //
   private Button                         _chkIsMinSurfingDistance;
   private Button                         _chkIsShowOnlySelectedSegments;
   //
   private Combo                          _comboBreakMethod;
   private Combo                          _comboSegmenterType;
   private Combo                          _comboSurfing_SegmenterFilter;
   //
   private ImageComboLabel                _lblTitle;
   //
   private CLabel                         _iconSaveSurfingState;
   //
   private Image                          _imageSurfing_SaveState;
   private Image                          _imageSurfing_NotSaveState;
   //
   private Label                          _lblAltitudeUpDP;
   private Label                          _lblAltitudeUpMin;
   private Label                          _lblBreakDistanceUnit;
   private Label                          _lblDistanceValue;
   private Label                          _lblMinAltitude;
   private Label                          _lblNumSegments;
   private Label                          _lblSurfing_MinStartStopSpeed;
   private Label                          _lblSurfing_MinStartStopSpeed_Unit;
   private Label                          _lblSurfing_MinSurfingDistance_Unit;
   private Label                          _lblSurfing_MinSurfingSpeed;
   private Label                          _lblSurfing_MinSurfingSpeed_Unit;
   private Label                          _lblSurfing_MinSurfingTimeDuration;
   private Label                          _lblSurfing_MinSurfingTimeDuration_Unit;
   private Label                          _lblTourBreakTime;
   //
   private Spinner                        _spinnerBreak_MinAvgSpeedAS;
   private Spinner                        _spinnerBreak_MinSliceSpeedAS;
   private Spinner                        _spinnerBreak_MinSliceTimeAS;
   private Spinner                        _spinnerBreak_MinAvgSpeed;
   private Spinner                        _spinnerBreak_MinSliceSpeed;
   private Spinner                        _spinnerBreak_ShortestTime;
   private Spinner                        _spinnerBreak_MaxDistance;
   private Spinner                        _spinnerBreak_SliceDiff;
   private Spinner                        _spinnerDistance;
   private Spinner                        _spinnerDPTolerance_Altitude;
   private Spinner                        _spinnerDPTolerance_Power;
   private Spinner                        _spinnerDPTolerance_Pulse;
   private Spinner                        _spinnerMinAltitude;
   private Spinner                        _spinnerSurfing_MinSurfingDistance;
   private Spinner                        _spinnerSurfing_MinSpeed_Surfing;
   private Spinner                        _spinnerSurfing_MinTimeDuration;
   private Spinner                        _spinnerSurfing_MinSpeed_StartStop;
   /**
    * {@link TourChart} contains the chart for the tour, this is necessary to move the slider in the
    * chart to a selected segment
    */
   private TourChart                      _tourChart;

   private class SegmenterComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = TableColumnFactory.DATA_SEQUENCE_ID;
      private int              __sortDirection = ASCENDING;

      /**
       * Compares the object for sorting
       */
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         final TourSegment segment1 = ((TourSegment) obj1);
         final TourSegment segment2 = ((TourSegment) obj2);

         // sort total item to the end of the view
         if (segment1.isTotal && segment2.isTotal) {

            return -2;

         } else if (segment1.isTotal) {

            return +1;

         } else if (segment2.isTotal) {

            return -1;
         }

         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case TableColumnFactory.ALTITUDE_GRADIENT_ID:
            rc = (segment1.gradient - segment2.gradient) * 100;
            break;

         case TableColumnFactory.BODY_AVG_PULSE_ID:
            rc = segment1.pulse - segment2.pulse;
            break;

         case TableColumnFactory.MOTION_AVG_PACE_ID:
            rc = (segment1.pace - segment2.pace) * 100;
            break;

         case TableColumnFactory.MOTION_AVG_SPEED_ID:
            rc = (segment1.speed - segment2.speed) * 100;
            break;

         case TableColumnFactory.MOTION_DISTANCE_ID:
            rc = segment1.distance_Diff - segment2.distance_Diff;
            break;

         case TableColumnFactory.POWERTRAIN_AVG_CADENCE_ID:
            rc = segment1.cadence - segment2.cadence;
            break;

         case TableColumnFactory.TIME_RECORDING_TIME_ID:
            rc = segment1.time_Recording - segment2.time_Recording;
            break;

         case TableColumnFactory.DATA_SERIE_START_END_INDEX_ID:
            // sort start ... end
            rc = segment1.serieIndex_Start - segment2.serieIndex_Start;
            break;

         case TableColumnFactory.DATA_SEQUENCE_ID:
         default:
            rc = segment1.sequence - segment2.sequence;
         }

         // flip the direction
         if (__sortDirection == DESCENDING) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0 //
               ? 1
               : rc < 0 //
                     ? -1
                     : 0;
      }

      /**
       * Does the sort. If it's a different column from the previous sort, do an ascending sort. If
       * it's the same column as the last sort, toggle the sort direction.
       *
       * @param columnId
       */

      private void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         final boolean isSortableColumn = _allSortableColumns.contains(columnId);

         if (isSortableColumn) {

            if (columnId.equals(__sortColumnId)) {

               // Same column as last sort; toggle the direction

               __sortDirection = 1 - __sortDirection;

            } else {

               // New column; do an ascent sorting

               __sortColumnId = columnId;
               __sortDirection = ASCENDING;
            }

            sort_UpdateUI_SetSortDirection(__sortColumnId, __sortDirection);
         }
      }
   }

   /**
    * The content provider class is responsible for providing objects to the view. It can wrap
    * existing objects in adapters or simply return objects as-is. These objects may be sensitive to
    * the current input of the view, or ignore it and always show the same content (like Task List,
    * for example).
    */
   private class SegmenterContentProvider implements IStructuredContentProvider {

      public SegmenterContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         if (_tourData == null) {
            return new Object[0];
         } else {

            final Object[] tourSegments = createSegmenterContent();

            updateUI_Altitude();
            updateUI_BreakTime();
            updateUI_SegmenterInfo(tourSegments);

            return tourSegments;
         }
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   public class SegmenterFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_isSegmenterFiltered == false) {

            // all segments are displayed
            return true;
         }

         // segments are filtered

         final TourSegment segment = (TourSegment) element;

         if (_selectedSurfingFilter == 1) {

            if (segment.filter == 1) {
               return true;
            }

         } else if (_selectedSurfingFilter == 2) {

            if (segment.filter == 2) {
               return true;
            }
         }

         return false;
      }

   }

   public static enum SegmenterType {

      ByAltitudeWithDP, //
      ByAltitudeWithDPMerged, //
      ByAltitudeWithMarker, //

      ByPowerWithDP, //
      ByPulseWithDP, //
      ByMarker, //
      ByDistance, //
      ByComputedAltiUpDown, //
      ByBreakTime, //

      Surfing, //
   }

   private class SurfingData {

      private boolean[] __visibleDataPointSerie;
      private int       __numSurfingEvents;

      public SurfingData(final boolean[] visibleDataPointSerie, final int numSurfingEvents) {

         __visibleDataPointSerie = visibleDataPointSerie;
         __numSurfingEvents = numSurfingEvents;
      }
   }

   private static class SurfingFilter {

      private String            label;
      private SurfingFilterType surfingFilterId;

      public SurfingFilter(final SurfingFilterType surfingFilterId, final String label) {

         this.surfingFilterId = surfingFilterId;
         this.label = label;
      }
   }

   private enum SurfingFilterType {

      All, //
      Surfing, //
      NotSurfing, //
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

               _state.put(STATE_IS_SEGMENTER_ACTIVE, false);

               restoreVisibleDataPointsBeforeNextTourIsSet();

               fireSegmentLayerChanged();
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
               defineAllColumns();

               recreateViewer(null);

               /*
                * update distance
                */
               setMaxDistanceSpinner();
               _spinnerDistance.setMaximum(_maxDistanceSpinner);
               _spinnerDistance.setPageIncrement(_spinnerDistancePage);
               updateUI_Distance();

               /*
                * update min altitude
                */
               _lblMinAltitude.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE);
               _lblMinAltitude.pack(true);

               updateUI_Surfing_MeasurementValues();

               createSegments(true);

            } else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

               // marker is hidden/visible

               final TourSegmenter selectedSegmenter = getSelectedSegmenter();
               if (SegmenterType.ByAltitudeWithMarker.equals(selectedSegmenter.segmenterType)) {

                  // this could be optimized to check if marker visibility has changed or not

                  onSelect_SegmenterType(false);
               }

            } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _segmentViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _segmentViewer.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
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
                            * tour is reverted, saving existing tour is not necessary, just update
                            * the tour
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

               } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED
                     && eventData instanceof SelectionChartXSliderPosition) {

                  final SelectionChartXSliderPosition xSliderSelection = (SelectionChartXSliderPosition) eventData;

                  final Object customData = xSliderSelection.getCustomData();
                  if (customData instanceof SelectedTourSegmenterSegments) {

                     /*
                      * This event is fired in the tour chart when a toursegmenter segment is
                      * selected
                      */

                     _isInSelection = true;
                     {
                        selectTourSegments((SelectedTourSegmenterSegments) customData);
                     }
                     _isInSelection = false;
                  }

               } else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

                  // tour chart smoothing can be modified

                  createSegments(false);

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

      final float[] altitudeSerie = tourData.getAltitudeSmoothedSerie(false);
      final float[] powerSerie = tourData.getPowerSerie();
      final float[] metricDistanceSerie = tourData.getMetricDistanceSerie();
      final float[] pulseSerie = tourData.pulseSerie;

      final Object[] markerSerie;
      if (tourData.isMultipleTours()) {
         markerSerie = tourData.multiTourMarkers.toArray();
      } else {
         markerSerie = tourData.getTourMarkers().toArray();
      }

      int checkedSegmenterData = 0;

      checkedSegmenterData |= altitudeSerie != null && altitudeSerie.length > 1 ? //
            SEGMENTER_REQUIRES_ALTITUDE
            : 0;

      checkedSegmenterData |= metricDistanceSerie != null && metricDistanceSerie.length > 1
            ? SEGMENTER_REQUIRES_DISTANCE
            : 0;

      checkedSegmenterData |= powerSerie != null && powerSerie.length > 1 ? //
            SEGMENTER_REQUIRES_POWER
            : 0;

      checkedSegmenterData |= pulseSerie != null && pulseSerie.length > 1 ? //
            SEGMENTER_REQUIRES_PULSE
            : 0;

      checkedSegmenterData |= markerSerie != null && markerSerie.length > 0 ? //
            SEGMENTER_REQUIRES_MARKER
            : 0;

      return checkedSegmenterData;
   }

   private void clearView() {

      _pageBookUI.showPage(_pageNoData);

      restoreVisibleDataPointsBeforeNextTourIsSet();
      _tourData = null;
      _tourChart = null;

      _isGetInitialTours = true;
      _isClearView = true;

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();

      enableActions();
   }

   private void createActions() {

      _actionModifyColumns = new ActionModifyColumns(this);
      _actionTourChartSegmenterConfig = new ActionTourChartSegmenterConfig(this, _parent);
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      setMaxDistanceSpinner();

      // define all columns
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createActions();
      createUI(parent);
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

      _allTourSegments = _tourData.createSegmenterSegments(btConfig);

      return _allTourSegments == null //
            ? new Object[0]
            : _allTourSegments.toArray();
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
      _tourData.segmentSerie_Altitude_Diff_Computed = null;

      // reset other indices
      _tourData.segmentSerieIndex2nd = null;
      _tourData.segmentSerieFilter = null;

      final TourSegmenter selectedSegmenter = getSelectedSegmenter();
      if (selectedSegmenter == null) {
         clearView();
         return;
      }

      boolean isUpdateVisibleDataPoints = false;
      int[] forcedIndices;

      final SegmenterType selectedSegmenterType = selectedSegmenter.segmenterType;

      switch (selectedSegmenterType) {

      case ByAltitudeWithDP:
         createSegmentsBy_AltitudeWithDP();
         break;

      case ByAltitudeWithDPMerged:

         forcedIndices = getTourIndices();
         _tourData.segmentSerieIndex = createSegmentsBy_AltitudeWithDPMerged(forcedIndices);

         break;

      case ByAltitudeWithMarker:

         forcedIndices = getTourAndMarkerIndices();
         final int[] segmentSerieIndices = createSegmentsBy_AltitudeWithDPMerged(forcedIndices);

         createSegmentsBy_AltitudeWithMarker(forcedIndices, segmentSerieIndices);

         break;

      case ByBreakTime:
         createSegmentsBy_BreakTime();
         break;

      case ByComputedAltiUpDown:
         createSegmentsBy_AltiUpDown();
         break;

      case ByDistance:
         createSegmentsBy_Distance();
         break;

      case ByMarker:
         createSegmentsBy_Marker();
         break;

      case ByPowerWithDP:
         createSegmentsBy_PowerWithDP();
         break;

      case ByPulseWithDP:
         createSegmentsBy_PulseWithDP();
         break;

      case Surfing:
         createSegmentsBy_Surfing();
         isUpdateVisibleDataPoints = true;
         break;
      }

      // update table and create the tour segments in TourData
      reloadViewer();

      if (isUpdateVisibleDataPoints) {
         _tourData.visibleDataPointSerie = createVisibleDataPoints(getSelectedSurfingFilter()).__visibleDataPointSerie;
      }

      if (isFireEvent) {
         fireSegmentLayerChanged();
      }
   }

   /**
    * create Douglas-Peucker segments from distance and altitude
    */
   private void createSegmentsBy_AltitudeWithDP() {

      final float[] distanceSerie = _tourData.getMetricDistanceSerie();
      final float[] altitudeSerie = _tourData.getAltitudeSmoothedSerie(false);

      // convert data series into dp points
      final DPPoint graphPoints[] = new DPPoint[distanceSerie.length];
      for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
         graphPoints[serieIndex] = new DPPoint(distanceSerie[serieIndex], altitudeSerie[serieIndex], serieIndex);
      }

      final Object[] dpPoints = new DouglasPeuckerSimplifier(//
            _dpToleranceAltitude,
            graphPoints,
            getTourIndices()).simplify();

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
    * Create Douglas-Peucker segments from distance and altitude. All segments are merged which have
    * the same vertical direction.
    *
    * @return
    */
   private int[] createSegmentsBy_AltitudeWithDPMerged(final int[] forcedIndices) {

      final float[] distanceSerie = _tourData.getMetricDistanceSerie();
      final float[] altitudeSerie = _tourData.getAltitudeSmoothedSerie(false);

      final int serieSize = distanceSerie.length;

      // convert data series into dp points
      final DPPoint graphPoints[] = new DPPoint[serieSize];
      for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {

         graphPoints[serieIndex] = new DPPoint(//
               distanceSerie[serieIndex],
               altitudeSerie[serieIndex],
               serieIndex);
      }

      final Object[] simplePoints = new DouglasPeuckerSimplifier(//
            _dpToleranceAltitude,
            graphPoints,
            forcedIndices).simplify();

      /*
       * copie the data index for the simplified points into the tour data
       */

      int forcedIndex = 0;
      int forcedIndexIndex = 0;
      if (forcedIndices != null && forcedIndices.length > 0) {
         forcedIndexIndex++;
         forcedIndex = forcedIndices[forcedIndexIndex];
      }

      final TIntArrayList segmentSerieIndex = new TIntArrayList();

      // set first point
      segmentSerieIndex.add(0);

      DPPoint prevDpPoint = (DPPoint) simplePoints[0];

      double prevAltitude = prevDpPoint.y;
      boolean isPrevAltiUp = false;
      boolean isPrevAltiDown = false;

      for (int simpleIndex = 1; simpleIndex < simplePoints.length; simpleIndex++) {

         final DPPoint currentDpPoint = (DPPoint) simplePoints[simpleIndex];

         boolean isAddPoint = false;

         if (forcedIndices != null && forcedIndex == prevDpPoint.serieIndex) {

            // this is a forced point

            /*
             * This algorithm ensures that the points are set only once, highly complicated
             * algorithm but now it works.
             */
            isAddPoint = true;

            // get next forced index
            forcedIndexIndex++;
            if (forcedIndexIndex < forcedIndices.length) {
               forcedIndex = forcedIndices[forcedIndexIndex];
            }

         }

         final double currentAltitude = currentDpPoint.y;

         if (simpleIndex == 1) {

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

               isAddPoint = true;

               isPrevAltiUp = isCurrentAltiUp;
               isPrevAltiDown = isCurrentAltiDown;
            }
         }

         if (isAddPoint) {
            segmentSerieIndex.add(prevDpPoint.serieIndex);
         }

         prevDpPoint = currentDpPoint;
         prevAltitude = currentAltitude;
      }

      // add last point
      segmentSerieIndex.add(serieSize - 1);

      return segmentSerieIndex.toArray();
   }

   private void createSegmentsBy_AltitudeWithMarker(final int[] forcedIndices, final int[] segmentSerieIndices) {

      _tourData.segmentSerieIndex = forcedIndices;
      _tourData.segmentSerieIndex2nd = segmentSerieIndices;
   }

   private void createSegmentsBy_AltiUpDown() {

      final float selectedMinAltiDiff = (float) (_spinnerMinAltitude.getSelection() / 10.0);

      final ArrayList<AltitudeUpDownSegment> tourSegements = new ArrayList<>();

      // create segment when the altitude up/down is changing
      _tourData.computeAltitudeUpDown(tourSegements, selectedMinAltiDiff);

      // convert segment list into array
      int serieIndex = 0;
      final int segmentLength = tourSegements.size();
      final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[segmentLength];
      final float[] altitudeDiff = _tourData.segmentSerie_Altitude_Diff_Computed = new float[segmentLength];

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

         _breakUIShortestBreakTime = _spinnerBreak_ShortestTime.getSelection();
         _breakUIMaxDistance = _spinnerBreak_MaxDistance.getSelection()
               * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
         _breakUISliceDiff = _spinnerBreak_SliceDiff.getSelection();

         breakTimeResult = BreakTimeTool.computeBreakTimeByTimeDistance(
               _tourData,
               _breakUIShortestBreakTime,
               _breakUIMaxDistance,
               _breakUISliceDiff);

      } else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

         _breakUIMinSliceSpeed = _spinnerBreak_MinSliceSpeed.getSelection()
               / SPEED_DIGIT_VALUE
               / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

         breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(_tourData, breakMethodId, _breakUIMinSliceSpeed);

      } else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

         _breakUIMinAvgSpeed = _spinnerBreak_MinAvgSpeed.getSelection()//
               / SPEED_DIGIT_VALUE
               / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

         breakTimeResult = BreakTimeTool.computeBreakTimeBySpeed(_tourData, breakMethodId, _breakUIMinAvgSpeed);

      } else if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

         _breakUIMinAvgSpeedAS = _spinnerBreak_MinAvgSpeedAS.getSelection()//
               / SPEED_DIGIT_VALUE
               / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

         _breakUIMinSliceSpeedAS = _spinnerBreak_MinSliceSpeedAS.getSelection()
               / SPEED_DIGIT_VALUE
               / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

         _breakUIMinSliceTimeAS = _spinnerBreak_MinSliceTimeAS.getSelection();

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
      final TIntArrayList segmentSerieIndex = new TIntArrayList();

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

      _tourData.segmentSerieIndex = segmentSerieIndex.toArray();
      _tourData.setBreakTimeSerie(breakTimeSerie);

   }

   private void createSegmentsBy_Distance() {

      final float[] distanceSerie = _tourData.getMetricDistanceSerie();
      final int lastDistanceSerieIndex = distanceSerie.length - 1;

      final float segmentDistance = getDistance();
      final TIntArrayList segmentSerieIndex = new TIntArrayList();

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

      _tourData.segmentSerieIndex = segmentSerieIndex.toArray();
   }

   private void createSegmentsBy_Marker() {

      final boolean isMultipleTours = _tourData.isMultipleTours();
      final int[] timeSerie = _tourData.timeSerie;
      final int numTimeSlices = timeSerie.length;

      final Collection<TourMarker> tourMarkers = isMultipleTours //
            ? _tourData.multiTourMarkers
            : _tourData.getTourMarkers();

      // sort markers by time - they can be unsorted
      final ArrayList<TourMarker> sortedMarkers = new ArrayList<>(tourMarkers);
      Collections.sort(sortedMarkers, new Comparator<TourMarker>() {
         @Override
         public int compare(final TourMarker tm1, final TourMarker tm2) {

            final int result = isMultipleTours //
                  ? tm1.getMultiTourSerieIndex() - tm2.getMultiTourSerieIndex()
                  : tm1.getSerieIndex() - tm2.getSerieIndex();

            return result;
         }
      });

      final TIntArrayList segmenterIndices = new TIntArrayList();

      int prevSerieIndex = 0;

      // set first segment at tour start
      segmenterIndices.add(prevSerieIndex);

      final float[] distanceSerie = _tourData.getMetricDistanceSerie();
      final float[] altitudeSerie = _tourData.getAltitudeSmoothedSerie(false);

      // ensure required data are available
      if (altitudeSerie != null //
            && altitudeSerie.length > 1
            && distanceSerie != null
            && distanceSerie.length > 1) {

         // Merge tour with marker indices

         final int[] tourSerieIndices;
         if (isMultipleTours) {

            final int[] forcedIndices = getTourIndices();

            tourSerieIndices = Arrays.copyOf(forcedIndices, forcedIndices.length + 1);

            // add last timeslice index
            tourSerieIndices[forcedIndices.length] = numTimeSlices - 1;

         } else {
            tourSerieIndices = new int[] { 0, numTimeSlices - 1 };
         }

         final TIntArrayList markerIndices = new TIntArrayList();

         // get a list with all marker indices
         for (final TourMarker tourMarker : sortedMarkers) {

            final int serieIndex = isMultipleTours //
                  ? tourMarker.getMultiTourSerieIndex()
                  : tourMarker.getSerieIndex();

            markerIndices.add(serieIndex);
         }

         final int[] markerSerieIndices = markerIndices.toArray();
         int markerIndex = 0;

         for (final int tourSerieIndex : tourSerieIndices) {

            for (; markerIndex < markerSerieIndices.length; markerIndex++) {

               final int markerSerieIndex = markerSerieIndices[markerIndex];

               if (markerSerieIndex >= tourSerieIndex) {
                  break;
               }

               // prevent to set a second segment at the same position
               if (markerSerieIndex != prevSerieIndex) {
                  segmenterIndices.add(markerSerieIndex);
               }
               prevSerieIndex = markerSerieIndex;
            }

            // prevent to set a second segment at the same position
            if (tourSerieIndex != prevSerieIndex) {
               segmenterIndices.add(tourSerieIndex);
            }
            prevSerieIndex = tourSerieIndex;
         }

      } else {

         // altitude or distance is not available, use the old algorithm

         // create segment for each marker
         for (final TourMarker tourMarker : sortedMarkers) {

            final int serieIndex = isMultipleTours //
                  ? tourMarker.getMultiTourSerieIndex()
                  : tourMarker.getSerieIndex();

            // prevent to set a second segment at the same position
            if (serieIndex != prevSerieIndex) {
               segmenterIndices.add(serieIndex);
            }

            prevSerieIndex = serieIndex;
         }
      }

      // add segment end at the tour end
      final int lastIndex = numTimeSlices - 1;
      if (prevSerieIndex != lastIndex) {
         segmenterIndices.add(lastIndex);
      }

      _tourData.segmentSerieIndex = segmenterIndices.toArray();
   }

   /**
    * Create Douglas-Peucker segments from time and power.
    */
   private void createSegmentsBy_PowerWithDP() {

      final int[] timeSerie = _tourData.timeSerie;
      final float[] powerSerie = _tourData.getPowerSerie();

      if (powerSerie == null || powerSerie.length < 2) {
         _tourData.segmentSerieIndex = null;
         return;
      }

      // convert data series into points
      final DPPoint graphPoints[] = new DPPoint[timeSerie.length];
      for (int serieIndex = 0; serieIndex < graphPoints.length; serieIndex++) {
         graphPoints[serieIndex] = new DPPoint(timeSerie[serieIndex], powerSerie[serieIndex], serieIndex);
      }

      final Object[] simplePoints = new DouglasPeuckerSimplifier(//
            _dpTolerancePower,
            graphPoints,
            getTourIndices()).simplify();

      /*
       * copie the data index for the simplified points into the tour data
       */
      final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[simplePoints.length];

      for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
         final DPPoint point = (DPPoint) simplePoints[iPoint];
         segmentSerieIndex[iPoint] = point.serieIndex;
      }
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

      final Object[] simplePoints = new DouglasPeuckerSimplifier(//
            _dpTolerancePulse,
            graphPoints,
            getTourIndices()).simplify();

      /*
       * copie the data index for the simplified points into the tour data
       */
      final int[] segmentSerieIndex = _tourData.segmentSerieIndex = new int[simplePoints.length];

      for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
         final DPPoint point = (DPPoint) simplePoints[iPoint];
         segmentSerieIndex[iPoint] = point.serieIndex;
      }
   }

   private void createSegmentsBy_Surfing() {

      final int[] timeSerie = _tourData.timeSerie;
      final float[] distanceSerie = _tourData.getMetricDistanceSerie();
      final float[] speedSerie = _tourData.getSpeedSerieMetric();

      final int lastSerieIndex = timeSerie.length - 1;

      // get surfing values in metric measurement
      float minSpeed_StartStop = _spinnerSurfing_MinSpeed_StartStop.getSelection();
      float minSpeed_Surfing = _spinnerSurfing_MinSpeed_Surfing.getSelection();
      final float minTimeDuration = _spinnerSurfing_MinTimeDuration.getSelection();
      float minDistance = _spinnerSurfing_MinSurfingDistance.getSelection();
      final boolean isMinDistance = _chkIsMinSurfingDistance.getSelection();

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // convert imperial -> metric

         minSpeed_StartStop = (float) (minSpeed_StartStop / UNIT_MILE + 0.5);
         minSpeed_Surfing = (float) (minSpeed_Surfing / UNIT_MILE + 0.5);

         minDistance = (float) (minDistance / UNIT_YARD + 0.5);
      }

      final TIntArrayList segmentSerieIndex = new TIntArrayList();
      final TIntArrayList segmentSerieFilter = new TIntArrayList();

      // set first segment start
      segmentSerieIndex.add(0);
      segmentSerieFilter.add(0);

      boolean isSurfing = false;
      boolean isSurfing_Distance = false;
      boolean isSurfing_MinSpeed = false;
      boolean isSurfing_StartStop = false;
      boolean isSurfing_Time = false;

      int surfing_TimeDuration = 0;
      float surfing_Distance = 0;

      int prevTime = 0;
      float prevDistance = 0;

      int segmentStartIndex = 0;

      for (int serieIndex = 0; serieIndex < distanceSerie.length; serieIndex++) {

         final int currentTime = timeSerie[serieIndex];
         final float currentDistance = distanceSerie[serieIndex];
         final float currentSpeed = speedSerie[serieIndex];

         // diffs to the previous time slice
         final int timeDiff = currentTime - prevTime;
         final float distanceDiff = currentDistance - prevDistance;

         if (currentSpeed >= minSpeed_StartStop) {

            if (isSurfing_StartStop == false) {

               // start new segment

               segmentStartIndex = serieIndex;

               isSurfing_MinSpeed = false;
               isSurfing_Time = false;
               isSurfing_Distance = false;

               surfing_Distance = 0;
               surfing_TimeDuration = 0;
            }

            isSurfing_StartStop = true;

         } else {

            isSurfing_StartStop = false;
         }

         if (currentSpeed >= minSpeed_Surfing) {
            isSurfing_MinSpeed = true;
         }

         if (isSurfing_StartStop && isSurfing_MinSpeed) {

            surfing_TimeDuration += timeDiff;

            if (surfing_TimeDuration >= minTimeDuration) {
               isSurfing_Time = true;
            }

         } else {

            isSurfing_Time = false;
         }

         if (isSurfing_StartStop && isSurfing_MinSpeed) {

            surfing_Distance += distanceDiff;

            if (surfing_Distance >= minDistance) {
               isSurfing_Distance = true;
            }

         } else {

            isSurfing_Distance = false;
         }

         final boolean prevIsSurfing = isSurfing;

         if (isSurfing_StartStop && isSurfing_MinSpeed && isSurfing_Time) {

            // distance is optional
            if (isMinDistance) {

               //check distance
               if (isSurfing_Distance) {
                  isSurfing = true;
               } else {
                  isSurfing = false;
               }

            } else {
               isSurfing = true;
            }

         } else {
            isSurfing = false;
         }

         if (prevIsSurfing && isSurfing == false) {

            // surfing has stopped

            final int segmentEndIndex = serieIndex - 1;

            segmentStartIndex = segmentStartIndex > 0 ? segmentStartIndex - 1 : 0;

            if (segmentStartIndex == segmentEndIndex) {

               // ignore, this occured

            } else {

               // setup surfing values

               segmentSerieIndex.add(segmentStartIndex);
               segmentSerieIndex.add(segmentEndIndex);

               // mark as not surfing segment
               segmentSerieFilter.add(2);

               // mark as surfing segment
               segmentSerieFilter.add(1);
            }
         }

         prevTime = currentTime;
         prevDistance = currentDistance;
      }

      // ensure the last segment ends at the end of the tour
      final int serieSize = segmentSerieIndex.size();
      if (serieSize == 1 ||

      // ensure the last index is not duplicated
            segmentSerieIndex.get(serieSize - 1) != lastSerieIndex) {

         segmentSerieIndex.add(lastSerieIndex);
         segmentSerieFilter.add(0);
      }

      _tourData.segmentSerieIndex = segmentSerieIndex.toArray();
      _tourData.segmentSerieFilter = segmentSerieFilter.toArray();
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
      GridLayoutFactory.fillDefaults().numColumns(4).extendedMargins(3, 3, 3, 5).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         {
            // tour title

            _lblTitle = new ImageComboLabel(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(_lblTitle);
         }

         {
            // label: create segments with

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
            label.setText(Messages.tour_segmenter_label_createSegmentsWith);
         }

         {
            // combo: segmenter type

            _comboSegmenterType = new Combo(container, SWT.READ_ONLY);
            _comboSegmenterType.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_SegmenterType(true);
               }
            });

            /*
             * fill the segmenter list that the next layout shows the combo that all segmenter names
             * are visible
             */
            for (final TourSegmenter segmenter : _allTourSegmenter) {
               _comboSegmenterType.add(segmenter.name);
            }
            final Point size = _comboSegmenterType.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            GridDataFactory.fillDefaults().hint(size).applyTo(_comboSegmenterType);
         }

         {
            // label: number of segments

            _lblNumSegments = new Label(container, SWT.LEAD);
            _lblNumSegments.setToolTipText(Messages.Tour_Segmenter_Label_NumberOfSegments_Tooltip);

            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(12), SWT.DEFAULT)
                  .applyTo(_lblNumSegments);
         }

         // pagebook: segmenter type
         _pageBookSegmenter = new PageBook(container, SWT.NONE);
         GridDataFactory.fillDefaults()
//               .grab(true, false)
               .span(4, 1)
               .applyTo(_pageBookSegmenter);
         {
            _pageSegType_DPAltitude = createUI_42_SegmenterBy_DPAltitude(_pageBookSegmenter);
            _pageSegType_DPPulse = createUI_43_SegmenterBy_DPPulse(_pageBookSegmenter);
            _pageSegType_DPPower = createUI_44_SegmenterBy_DPPower(_pageBookSegmenter);
            _pageSegType_ByMarker = createUI_45_SegmenterBy_Marker(_pageBookSegmenter);
            _pageSegType_ByDistance = createUI_46_SegmenterBy_Distance(_pageBookSegmenter);
            _pageSegType_ByAltiUpDown = createUI_48_SegmenterBy_MinAltitude(_pageBookSegmenter);
            _pageSegType_ByBreakTime = createUI_50_SegmenterBy_BreakTime(_pageBookSegmenter);
            _pageSegType_Surfing = createUI_60_SegmenterBy_Surfing(_pageBookSegmenter);
         }
      }
   }

   private Composite createUI_42_SegmenterBy_DPAltitude(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         _spinnerDPTolerance_Altitude = createUI_DP_Tolerance(container);
         _lblAltitudeUpDP = createUI_DP_Info(container);
         _btnSaveTourDP = createUI_DB_SaveTour(container);
      }

      return container;
   }

   private Composite createUI_43_SegmenterBy_DPPulse(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         _spinnerDPTolerance_Pulse = createUI_DP_Tolerance(container);
      }

      return container;
   }

   private Composite createUI_44_SegmenterBy_DPPower(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         _spinnerDPTolerance_Power = createUI_DP_Tolerance(container);
      }

      return container;
   }

   private Composite createUI_45_SegmenterBy_Marker(final Composite parent) {

      /*
       * display NONE, this is not easy to do - or I didn't find an easier way
       */
      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         final Canvas canvas = new Canvas(container, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(1, 1).applyTo(canvas);
      }

      return container;
   }

   private Composite createUI_46_SegmenterBy_Distance(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {

         // label: distance
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.tour_segmenter_segType_byDistance_label);

         // spinner: distance
         _spinnerDistance = new Spinner(container, SWT.BORDER);
         _spinnerDistance.setMinimum(1); // 0.1
         _spinnerDistance.setMaximum(_maxDistanceSpinner);
         _spinnerDistance.setPageIncrement(_spinnerDistancePage);
         _spinnerDistance.setDigits(1);
         _spinnerDistance.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_Distance();
            }
         });
         _spinnerDistance.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_Distance();
            }
         });

         // text: distance value
         _lblDistanceValue = new Label(container, SWT.NONE);
         _lblDistanceValue.setText(Messages.tour_segmenter_segType_byDistance_defaultDistance);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblDistanceValue);
      }

      return container;
   }

   private Composite createUI_48_SegmenterBy_MinAltitude(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
      {
         // label: min alti diff
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.tour_segmenter_segType_byUpDownAlti_label);

         // spinner: minimum altitude
         _spinnerMinAltitude = new Spinner(container, SWT.BORDER);
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
               onSelect_BreakTime();
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
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_containerBreakTime);
//		_containerBreakTime.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         final Composite container = new Composite(_containerBreakTime, SWT.NONE);
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
               .applyTo(_lblTourBreakTime);
      }
   }

   private void createUI_52_BreakTimePageBook(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()//
            .numColumns(2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
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
               onSelect_BreakTimeMethod();
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
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_pageBookBreakTime);
         {
            _pageBreakBy_AvgSliceSpeed = createUI_53_BreakBy_AvgSliceSpeed(_pageBookBreakTime);
            _pageBreakBy_AvgSpeed = createUI_54_BreakBy_AvgSpeed(_pageBookBreakTime);
            _pageBreakBy_SliceSpeed = createUI_55_BreakBy_SliceSpeed(_pageBookBreakTime);
            _pageBreakBy_TimeDistance = createUI_56_BreakBy_TimeDistance(_pageBookBreakTime);
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
            _spinnerBreak_MinAvgSpeedAS = new Spinner(container, SWT.BORDER);
            _spinnerBreak_MinAvgSpeedAS.setMinimum(0); // 0.0 km/h
            _spinnerBreak_MinAvgSpeedAS.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
            _spinnerBreak_MinAvgSpeedAS.setDigits(1);
            _spinnerBreak_MinAvgSpeedAS.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
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
            _spinnerBreak_MinSliceSpeedAS = new Spinner(container, SWT.BORDER);
            _spinnerBreak_MinSliceSpeedAS.setMinimum(0); // 0.0 km/h
            _spinnerBreak_MinSliceSpeedAS.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
            _spinnerBreak_MinSliceSpeedAS.setDigits(1);
            _spinnerBreak_MinSliceSpeedAS.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
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
            _spinnerBreak_MinSliceTimeAS = new Spinner(container, SWT.BORDER);
            _spinnerBreak_MinSliceTimeAS.setMinimum(0); // 0 sec
            _spinnerBreak_MinSliceTimeAS.setMaximum(10); // 10 sec
            _spinnerBreak_MinSliceTimeAS.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
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
         _spinnerBreak_MinAvgSpeed = new Spinner(container, SWT.BORDER);
         _spinnerBreak_MinAvgSpeed.setMinimum(0); // 0.0 km/h
         _spinnerBreak_MinAvgSpeed.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
         _spinnerBreak_MinAvgSpeed.setDigits(1);
         _spinnerBreak_MinAvgSpeed.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_BreakTime();
            }
         });
         _spinnerBreak_MinAvgSpeed.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_BreakTime();
            }
         });
         GridDataFactory.fillDefaults()
               .hint(_spinnerWidth, SWT.DEFAULT)
               .applyTo(_spinnerBreak_MinAvgSpeed);

         // label: km/h
         label = new Label(container, SWT.NONE);
         label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
      }

      return container;
   }

   private Composite createUI_55_BreakBy_SliceSpeed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
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
         _spinnerBreak_MinSliceSpeed = new Spinner(container, SWT.BORDER);
         _spinnerBreak_MinSliceSpeed.setMinimum(0); // 0.0 km/h
         _spinnerBreak_MinSliceSpeed.setMaximum(PrefPageComputedValues.BREAK_MAX_SPEED_KM_H); // 10.0 km/h
         _spinnerBreak_MinSliceSpeed.setDigits(1);
         _spinnerBreak_MinSliceSpeed.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_BreakTime();
            }
         });
         _spinnerBreak_MinSliceSpeed.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_BreakTime();
            }
         });
         GridDataFactory.fillDefaults()
               .hint(_spinnerWidth, SWT.DEFAULT)
               .applyTo(_spinnerBreak_MinSliceSpeed);

         // label: km/h
         label = new Label(container, SWT.NONE);
         label.setText(net.tourbook.common.UI.UNIT_LABEL_SPEED);
      }

      return container;
   }

   private Composite createUI_56_BreakBy_TimeDistance(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
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
            _spinnerBreak_ShortestTime = new Spinner(container, SWT.BORDER);
            _spinnerBreak_ShortestTime.setMinimum(1);
            _spinnerBreak_ShortestTime.setMaximum(120); // 120 seconds
            _spinnerBreak_ShortestTime.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_BreakTime();
               }
            });
            _spinnerBreak_ShortestTime.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
               }
            });
            GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreak_ShortestTime);

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
            _spinnerBreak_MaxDistance = new Spinner(container, SWT.BORDER);
            _spinnerBreak_MaxDistance.setMinimum(1);
            _spinnerBreak_MaxDistance.setMaximum(1000); // 1000 m/yards
            _spinnerBreak_MaxDistance.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_BreakTime();
               }
            });
            _spinnerBreak_MaxDistance.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
               }
            });
            GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreak_MaxDistance);

            // label: unit
            _lblBreakDistanceUnit = new Label(container, SWT.NONE);
            _lblBreakDistanceUnit.setText(net.tourbook.common.UI.UNIT_LABEL_DISTANCE_M_OR_YD);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblBreakDistanceUnit);
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
            _spinnerBreak_SliceDiff = new Spinner(container, SWT.BORDER);
            _spinnerBreak_SliceDiff.setMinimum(0);
            _spinnerBreak_SliceDiff.setMaximum(60); // minutes
            _spinnerBreak_SliceDiff.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_BreakTime();
               }
            });
            _spinnerBreak_SliceDiff.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  UI.adjustSpinnerValueOnMouseScroll(event);
                  onSelect_BreakTime();
               }
            });
            GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreak_SliceDiff);

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
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
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

   private Composite createUI_60_SegmenterBy_Surfing(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
      {
         createUI_62_Surfing_Fields(container);
         createUI_64_Surfing_Options(container);
      }

      return container;
   }

   private Composite createUI_62_Surfing_Fields(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Min start/stop speed
             */

            // label
            _lblSurfing_MinStartStopSpeed = new Label(container, SWT.NONE);
            _lblSurfing_MinStartStopSpeed.setText(Messages.Tour_Segmenter_Surfing_Label_MinSpeed_StartStop);
            _lblSurfing_MinStartStopSpeed.setToolTipText(Messages.Tour_Segmenter_Surfing_Label_MinSpeed_StartStop_Tooltip);

            // spinner: speed
            _spinnerSurfing_MinSpeed_StartStop = new Spinner(container, SWT.BORDER);
            _spinnerSurfing_MinSpeed_StartStop.setMinimum(1);
            _spinnerSurfing_MinSpeed_StartStop.setMaximum(100);
            _spinnerSurfing_MinSpeed_StartStop.setPageIncrement(5);
            _spinnerSurfing_MinSpeed_StartStop.addSelectionListener(_defaultSurfing_SelectionListener);
            _spinnerSurfing_MinSpeed_StartStop.addMouseWheelListener(_defaultSurfing_MouseWheelListener);

            // label: unit
            _lblSurfing_MinStartStopSpeed_Unit = new Label(container, SWT.NONE);
            _lblSurfing_MinStartStopSpeed_Unit.setText(UI.UNIT_LABEL_SPEED);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblSurfing_MinStartStopSpeed_Unit);
         }
         {
            /*
             * Min surfing speed
             */

            // label
            _lblSurfing_MinSurfingSpeed = new Label(container, SWT.NONE);
            _lblSurfing_MinSurfingSpeed.setText(Messages.Tour_Segmenter_Surfing_Label_MinSpeed_Surfing);
            _lblSurfing_MinSurfingSpeed.setToolTipText(Messages.Tour_Segmenter_Surfing_Label_MinSpeed_Surfing_Tooltip);

            // spinner: seconds
            _spinnerSurfing_MinSpeed_Surfing = new Spinner(container, SWT.BORDER);
            _spinnerSurfing_MinSpeed_Surfing.setMinimum(1);
            _spinnerSurfing_MinSpeed_Surfing.setMaximum(100);
            _spinnerSurfing_MinSpeed_Surfing.setPageIncrement(5);
            _spinnerSurfing_MinSpeed_Surfing.addSelectionListener(_defaultSurfing_SelectionListener);
            _spinnerSurfing_MinSpeed_Surfing.addMouseWheelListener(_defaultSurfing_MouseWheelListener);

            // label: unit
            _lblSurfing_MinSurfingSpeed_Unit = new Label(container, SWT.NONE);
            _lblSurfing_MinSurfingSpeed_Unit.setText(UI.UNIT_LABEL_SPEED);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblSurfing_MinSurfingSpeed_Unit);
         }
         {
            /*
             * Min surf time duration
             */

            // label
            _lblSurfing_MinSurfingTimeDuration = new Label(container, SWT.NONE);
            _lblSurfing_MinSurfingTimeDuration.setText(Messages.Tour_Segmenter_Surfing_Label_MinTimeDuration);
            _lblSurfing_MinSurfingTimeDuration.setToolTipText(Messages.Tour_Segmenter_Surfing_Label_MinTimeDuration_Tooltip);

            // spinner: seconds
            _spinnerSurfing_MinTimeDuration = new Spinner(container, SWT.BORDER);
            _spinnerSurfing_MinTimeDuration.setMinimum(1);
            _spinnerSurfing_MinTimeDuration.setMaximum(100);
            _spinnerSurfing_MinTimeDuration.setPageIncrement(5);
            _spinnerSurfing_MinTimeDuration.addSelectionListener(_defaultSurfing_SelectionListener);
            _spinnerSurfing_MinTimeDuration.addMouseWheelListener(_defaultSurfing_MouseWheelListener);

            // label: unit
            _lblSurfing_MinSurfingTimeDuration_Unit = new Label(container, SWT.NONE);
            _lblSurfing_MinSurfingTimeDuration_Unit.setText(Messages.App_Unit_Seconds_Small);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblSurfing_MinSurfingTimeDuration_Unit);
         }
         {
            /*
             * Min surfing distance
             */

            {
               _chkIsMinSurfingDistance = new Button(container, SWT.CHECK);
               _chkIsMinSurfingDistance.setText(Messages.Tour_Segmenter_Surfing_Checkbox_IsMinDistance);
               _chkIsMinSurfingDistance.setToolTipText(Messages.Tour_Segmenter_Surfing_Checkbox_IsMinDistance_Tooltip);
               _chkIsMinSurfingDistance.addSelectionListener(_defaultSurfing_SelectionListener);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_chkIsMinSurfingDistance);

               // spinner: seconds
               _spinnerSurfing_MinSurfingDistance = new Spinner(container, SWT.BORDER);
               _spinnerSurfing_MinSurfingDistance.setMinimum(0);
               _spinnerSurfing_MinSurfingDistance.setMaximum(1000);
               _spinnerSurfing_MinSurfingDistance.setPageIncrement(10);
               _spinnerSurfing_MinSurfingDistance.addSelectionListener(_defaultSurfing_SelectionListener);
               _spinnerSurfing_MinSurfingDistance.addMouseWheelListener(_defaultSurfing_MouseWheelListener);

               // label: unit
               _lblSurfing_MinSurfingDistance_Unit = new Label(container, SWT.NONE);
               _lblSurfing_MinSurfingDistance_Unit.setText(UI.UNIT_LABEL_DISTANCE_M_OR_YD);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblSurfing_MinSurfingDistance_Unit);
            }
         }

         createUI_63_Surfing_RestoreActions(container);
      }
      return container;
   }

   private void createUI_63_Surfing_RestoreActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
//          .grab(true, false)
            .span(3, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Button: Restore defaults
             */
            _btnSurfing_RestoreFrom_Defaults = new Button(container, SWT.NONE);
            _btnSurfing_RestoreFrom_Defaults.setText(Messages.App_Action_RestoreDefault);
            _btnSurfing_RestoreFrom_Defaults.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_RestoreFromDefaults_Tooltip);
            _btnSurfing_RestoreFrom_Defaults.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSurfing_ResetToDefaults();
               }
            });
         }
         {
            /*
             * Button: Restore from tour
             */
            _btnSurfing_RestoreFrom_Tour = new Button(container, SWT.NONE);
            _btnSurfing_RestoreFrom_Tour.setText(Messages.Tour_Segmenter_Surfing_Button_RestoreFromTour);
            _btnSurfing_RestoreFrom_Tour.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_RestoreFromTour_Tooltip);
            _btnSurfing_RestoreFrom_Tour.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSurfing_ResetFromTour();
               }
            });

// Sometimes the button are toooooo wide
//            UI.setButtonLayoutData(_btnSurfing_RestoreFrom_Defaults);
//            UI.setButtonLayoutData(_btnSurfing_RestoreFrom_Tour);
         }
      }
   }

   private void createUI_64_Surfing_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .indent(20, 0)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Combo: segment filter
             */

            _comboSurfing_SegmenterFilter = new Combo(container, SWT.READ_ONLY);
            _comboSurfing_SegmenterFilter.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSurfing_SelectSegmentFilter();
               }
            });

            // fill filter list
            for (final SurfingFilter surfingFilter : _allSurfingSegmentFilter) {
               _comboSurfing_SegmenterFilter.add(surfingFilter.label);
            }
         }
         {
            /*
             * Show only selected segment
             */
            _chkIsShowOnlySelectedSegments = new Button(container, SWT.CHECK);
            _chkIsShowOnlySelectedSegments.setText(Messages.Tour_Segmenter_Surfing_Checkbox_IsShowOnlySelectedSegments);
            _chkIsShowOnlySelectedSegments.setToolTipText(Messages.Tour_Segmenter_Surfing_Label_IsShowOnlySelectedSegments_Tooltip);
            _chkIsShowOnlySelectedSegments.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSurfing_ShowOnlySelectedSegments();
               }
            });
         }

         createUI_66_Surfing_Actions(container);
      }
   }

   private void createUI_66_Surfing_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         final Composite actionContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(false, true)
               .align(SWT.FILL, SWT.END)
               .applyTo(actionContainer);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(actionContainer);
         {
            {
               /*
                * Button: Save surfing segments
                */
               _btnSurfing_SaveTourSegments = new Button(actionContainer, SWT.NONE);
               _btnSurfing_SaveTourSegments.setText(Messages.Tour_Segmenter_Surfing_Button_SaveWaves);
               _btnSurfing_SaveTourSegments.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_SaveWaves_Tooltip);
               _btnSurfing_SaveTourSegments.addSelectionListener(new SelectionAdapter() {
                  @Override
                  public void widgetSelected(final SelectionEvent e) {
                     onSurfing_SaveTour(true);
                  }
               });
               GridDataFactory.fillDefaults().applyTo(_btnSurfing_SaveTourSegments);
            }
            {
               /*
                * Button: Delete surfing segments
                */
               _btnSurfing_DeleteTourSegments = new Button(actionContainer, SWT.NONE);
               _btnSurfing_DeleteTourSegments.setText(Messages.Tour_Segmenter_Surfing_Button_DeleteWaves);
               _btnSurfing_DeleteTourSegments.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_DeleteWaves_Tooltip);
               _btnSurfing_DeleteTourSegments.addSelectionListener(new SelectionAdapter() {
                  @Override
                  public void widgetSelected(final SelectionEvent e) {
                     onSurfing_SaveTour(false);
                  }
               });
               GridDataFactory.fillDefaults().applyTo(_btnSurfing_DeleteTourSegments);
            }
         }
         {
            /*
             * Save indicator
             */
            _iconSaveSurfingState = new CLabel(container, SWT.NONE);
            _iconSaveSurfingState.setImage(_imageSurfing_SaveState);

            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.END)
                  .grab(false, true)
                  .applyTo(_iconSaveSurfingState);
         }
      }

      /*
       * Set button width lately otherwise sometimes they are toooo wide
       */
//      container.getParent().layout(true, true);

//      UI.setButtonLayoutData(_btnSurfing_RestoreDefaults);
//      UI.setButtonLayoutData(_btnSurfing_SaveTour);

   }

   private void createUI_70_Viewer(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_80_SegmentViewer(_viewerContainer);
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

      _segmentViewer.setContentProvider(new SegmenterContentProvider());
      _segmentViewer.setComparator(_segmentComparator);
      _segmentViewer.setFilters(new SegmenterFilter());

      _segmentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_Segment(event);
         }
      });

      sort_UpdateUI_SetSortDirection(
            _segmentComparator.__sortColumnId,
            _segmentComparator.__sortDirection);

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
            onSaveTour_Altitude();
         }
      });

      return btn;
   }

   private Label createUI_DP_Info(final Composite parent) {

      final Label label = new Label(parent, SWT.TRAIL);

      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.CENTER)
//            .grab(true, false)
            .hint(_pc.convertWidthInCharsToPixels(18), SWT.DEFAULT)
            .applyTo(label);

      return label;
   }

   private Spinner createUI_DP_Tolerance(final Composite parent) {

      {
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
      }

      Spinner spinner;
      {
         // spinner: DP tolerance
         spinner = new Spinner(parent, SWT.BORDER);
//         GridDataFactory.fillDefaults().applyTo(spinner);
         spinner.setMinimum(1); // 0.1
         spinner.setMaximum(10000); // 1000
         spinner.setDigits(1);

         spinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_Tolerance();
            }
         });

         spinner.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_Tolerance();
            }
         });
      }

      return spinner;
   }

   /**
    * Create visible datapoints from the surfing filter.
    *
    * @param surfingFilterType
    *           Surfing type for which the visible datapoints are created.
    * @return
    */
   private SurfingData createVisibleDataPoints(final SurfingFilterType surfingFilterType) {

      int numSurfingEvents = 0;

      final int[] timeSerie = _tourData.timeSerie;
      final boolean[] visibleDataPointSerie = new boolean[timeSerie.length];

      switch (surfingFilterType) {

      case All:

         // all segments are visible
         Arrays.fill(visibleDataPointSerie, true);

         break;

      case Surfing:

         if (_allTourSegments != null) {

            for (final TourSegment tourSegment : _allTourSegments) {

               final int segmentStartIndex = tourSegment.serieIndex_Start;
               final int segmentEndIndex = tourSegment.serieIndex_End;

               if (tourSegment.filter == 1) {

                  numSurfingEvents++;

                  for (int surfSerieIndex = segmentStartIndex + 1; surfSerieIndex <= segmentEndIndex; surfSerieIndex++) {
                     visibleDataPointSerie[surfSerieIndex] = true;
                  }
               }
            }
         }
         break;

      case NotSurfing:

         if (_allTourSegments != null) {

            for (final TourSegment tourSegment : _allTourSegments) {

               final int segmentStartIndex = tourSegment.serieIndex_Start;
               final int segmentEndIndex = tourSegment.serieIndex_End;

               if (tourSegment.filter == 2) {

                  for (int surfSerieIndex = segmentStartIndex + 1; surfSerieIndex <= segmentEndIndex; surfSerieIndex++) {
                     visibleDataPointSerie[surfSerieIndex] = true;
                  }
               }
            }
         }
         break;
      }

      return new SurfingData(visibleDataPointSerie, numSurfingEvents);
   }

   private void defineAllColumns() {

      defineColumn_Time_RecordingTimeTotal();
      defineColumn_Time_Recording();
      defineColumn_Time_Driving();
      defineColumn_Time_Paused();

      defineColumn_Motion_DistanceTotal();
      defineColumn_Motion_Distance();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();
      defineColumn_Motion_AvgPace_Difference();

      defineColumn_Altitude_Gradient();
      defineColumn_Altitude_Hour_Up();
      defineColumn_Altitude_Hour_Down();
      defineColumn_Altitude_Diff_SegmentBorder();
      defineColumn_Altitude_Segment_Up();
      defineColumn_Altitude_Segment_Down();
      defineColumn_Altitude_SummarizedBorder_Up();
      defineColumn_Altitude_SummarizedBorder_Down();

      defineColumn_Altitude_Diff_SegmentComputed();
      defineColumn_Altitude_SummarizedComputed_Up();
      defineColumn_Altitude_SummarizedComputed_Down();

      defineColumn_Body_AvgPulse();
      defineColumn_Body_AvgPulse_Difference();

      defineColumn_Powertrain_AvgCadence();
      defineColumn_Powertrain_StrideLength();

      defineColumn_Data_Sequence();
      defineColumn_Data_SerieStartEndIndex();
   }

   /**
    * column: altitude diff segment border (m/ft)
    */
   private void defineColumn_Altitude_Diff_SegmentBorder() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_DIFF_SEGMENT_BORDER.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final double altitudeDiff = segment.altitude_Segment_Border_Diff;
            final double value = altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printDetailValue(cell, value);

            setCellColor(cell, altitudeDiff);
         }
      });
   }

   /**
    * column: computed altitude diff (m/ft)
    */
   private void defineColumn_Altitude_Diff_SegmentComputed() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_DIFF_SEGMENT_COMPUTED.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final double altitudeDiff = segment.altitude_Segment_Computed_Diff;
            final double value = altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printDetailValue(cell, value);

            setCellColor(cell, altitudeDiff);
         }
      });
   }

   /**
    * column: gradient
    */
   private void defineColumn_Altitude_Gradient() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_GRADIENT.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.gradient;

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: altitude down m/h
    */
   private void defineColumn_Altitude_Hour_Down() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_ELEVATION_DOWN.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final int drivingTime = segment.time_Driving;

            if (drivingTime == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               final double altitudeDiff = segment.altitude_Segment_Border_Diff;
               final double value = altitudeDiff > 0 //
                     ? 0
                     : (altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE) / drivingTime * 3600;

               colDef.printValue_0(cell, value);
            }
         }
      });
   }

   /**
    * column: altitude up m/h
    */
   private void defineColumn_Altitude_Hour_Up() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_ELEVATION_UP.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final int drivingTime = segment.time_Driving;

            if (drivingTime == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               final double altitudeDiff = segment.altitude_Segment_Border_Diff;
               final double value = altitudeDiff < 0 //
                     ? 0
                     : (altitudeDiff / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE) / drivingTime * 3600;

               colDef.printValue_0(cell, value);
            }
         }
      });
   }

   /**
    * Column: Altitude segment down
    */
   private void defineColumn_Altitude_Segment_Down() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_ELEVATION_SEGMENT_DOWN.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final double value = segment.altitude_Segment_Down;

            colDef.printValue_0(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * Column: Altitude segment up
    */
   private void defineColumn_Altitude_Segment_Up() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_ELEVATION_SEGMENT_UP.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final double value = segment.altitude_Segment_Up;

            colDef.printValue_0(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: total altitude down (m/ft)
    */
   private void defineColumn_Altitude_SummarizedBorder_Down() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.altitude_Summarized_Border_Down / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: total altitude up (m/ft)
    */
   private void defineColumn_Altitude_SummarizedBorder_Up() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.altitude_Summarized_Border_Up / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: total altitude down (m/ft)
    */
   private void defineColumn_Altitude_SummarizedComputed_Down() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_COMPUTED_DOWN.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.altitude_Summarized_Computed_Down / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: total altitude up (m/ft)
    */
   private void defineColumn_Altitude_SummarizedComputed_Up() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_COMPUTED_UP.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.altitude_Summarized_Computed_Up / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: average pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.pulse;

            colDef.printDetailValue(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: pulse difference
    */
   private void defineColumn_Body_AvgPulse_Difference() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.BODY_AVG_PULSE_DIFFERENCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            final float pulseDiff = segment.pulse_Diff;

            if (segment.isTotal) {
               cell.setText(UI.EMPTY_STRING);
            } else if (pulseDiff == Integer.MIN_VALUE) {
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
    * Column: Segment No.
    */
   private void defineColumn_Data_Sequence() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_columnManager, _pc);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            if (segment.isTotal) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               cell.setText(Integer.toString(segment.sequence));

               if (segment.filter > 0) {
                  setStyle_Filter(cell, segment.filter);
               }
            }

         }
      });
   }

   /**
    * column: data serie start/end index
    */
   private void defineColumn_Data_SerieStartEndIndex() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.DATA_SERIE_START_END_INDEX.createColumn(_columnManager, _pc);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            int startIndex = segment.serieIndex_Start;
            final int endIndex = segment.serieIndex_End;

            if (startIndex > 0) {
               startIndex++;
            }

            if (segment.isTotal) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               // start index by 1 instead of 0 to have the same index as in the tour data editor
               final int uiStartIndex = startIndex + 1;
               final int uiEndIndex = endIndex + 1;

               cell.setText(startIndex == endIndex
                     ? Integer.toString(uiStartIndex)
                     : uiStartIndex + UI.DASH_WITH_SPACE + uiEndIndex);

               if (segment.filter > 0) {
                  setStyle_Filter(cell, segment.filter);
               }
            }
         }
      });
   }

   /**
    * column: average pace
    */
   private void defineColumn_Motion_AvgPace() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final float pace = segment.pace;

            if (pace == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(net.tourbook.common.UI.format_mm_ss((long) pace));
            }

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: pace difference
    */
   private void defineColumn_Motion_AvgPace_Difference() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.MOTION_AVG_PACE_DIFFERENCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final float paceDiff = segment.pace_Diff;

            if (paceDiff == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(net.tourbook.common.UI.format_mm_ss((long) paceDiff));
            }
         }
      });
   }

   /**
    * column: average speed
    */
   private void defineColumn_Motion_AvgSpeed() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.speed;

            colDef.printDetailValue(cell, value);

            if (segment.filter > 0) {

               setStyle_Filter(cell, segment.filter);

            } else if (segment.isTotal) {

               setStyle_ColumnTotal(cell);
            }

         }
      });
   }

   /**
    * column: distance (km/mile)
    */
   private void defineColumn_Motion_Distance() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = (segment.distance_Diff) / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

            colDef.printDetailValue(cell, value);

            if (segment.filter > 0) {
               setStyle_Filter(cell, segment.filter);
            } else if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: TOTAL distance (km/mile)
    */
   private void defineColumn_Motion_DistanceTotal() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.MOTION_DISTANCE_TOTAL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final float value = (segment.distance_Total) / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: Average cadence
    */
   private void defineColumn_Powertrain_AvgCadence() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final float value = segment.cadence;

            colDef.printDetailValue(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: Stride length (meters/stride)
    */
   private void defineColumn_Powertrain_StrideLength() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final double value = segment.strideLength * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH * 1000;

            if (UI.UNIT_IS_METRIC) {
               colDef.printValue_0(cell, value);
            } else {
               colDef.printDetailValue(cell, value);
            }
         }
      });
   }

   /**
    * column: driving time
    */
   private void defineColumn_Time_Driving() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.TIME_DRIVING_TIME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final int value = segment.time_Driving;

            colDef.printDetailValue(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: break time
    */
   private void defineColumn_Time_Paused() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.TIME_PAUSED_TIME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final int value = segment.time_Break;

            colDef.printDetailValue(cell, value);

            if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: recording time
    */
   private void defineColumn_Time_Recording() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.TIME_RECORDING_TIME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();
            final int value = segment.time_Recording;

            colDef.printDetailValue(cell, value);

            if (segment.filter > 0) {
               setStyle_Filter(cell, segment.filter);
            } else if (segment.isTotal) {
               setStyle_ColumnTotal(cell);
            }
         }
      });
   }

   /**
    * column: TOTAL recording time
    */
   private void defineColumn_Time_RecordingTimeTotal() {

      final ColumnDefinition colDef;

      colDef = TableColumnFactory.TIME_RECORDING_TIME_TOTAL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourSegment segment = (TourSegment) cell.getElement();

            if (segment.isTotal) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               final int value = segment.time_Total;
               colDef.printDetailValue(cell, value);
            }
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

      _colorCache.dispose();

      _imageSurfing_SaveState.dispose();
      _imageSurfing_NotSaveState.dispose();

      super.dispose();
   }

   private void enableActions() {

      final boolean isTourAvailable = _tourData != null;

      _actionTourChartSegmenterConfig.setEnabled(isTourAvailable);

      enableActions_Surfing();
   }

   private void enableActions_Surfing() {

      final boolean isTourSaved = _tourData != null && _tourData.getTourPerson() != null;
      final boolean isMultipleTours = _tourData != null && _tourData.isMultipleTours();
      final boolean isVisiblePointsSaved_ForSurfing = _tourData != null && _tourData.isVisiblePointsSaved_ForSurfing();
      final boolean isSurfingParametersSaved = isSurfingParametersSavedInTour();

      final boolean isOneTour = isTourSaved && isMultipleTours == false;
      final boolean isSurfingEnabled = isOneTour;
      final boolean isMinDistance = _chkIsMinSurfingDistance.getSelection();

      _segmentViewer.getTable().setEnabled(isSurfingEnabled);

      _btnSurfing_SaveTourSegments.setEnabled(isSurfingEnabled);
      _btnSurfing_DeleteTourSegments.setEnabled(isOneTour && isVisiblePointsSaved_ForSurfing);

      _btnSurfing_RestoreFrom_Defaults.setEnabled(isSurfingEnabled);
      _btnSurfing_RestoreFrom_Tour.setEnabled(isSurfingEnabled && isSurfingParametersSaved);

      _chkIsMinSurfingDistance.setEnabled(isSurfingEnabled);
      _chkIsShowOnlySelectedSegments.setEnabled(isSurfingEnabled);

      _comboSurfing_SegmenterFilter.setEnabled(isSurfingEnabled);

      _lblSurfing_MinStartStopSpeed.setEnabled(isSurfingEnabled);
      _lblSurfing_MinStartStopSpeed_Unit.setEnabled(isSurfingEnabled);
      _lblSurfing_MinSurfingDistance_Unit.setEnabled(isSurfingEnabled);
      _lblSurfing_MinSurfingSpeed.setEnabled(isSurfingEnabled);
      _lblSurfing_MinSurfingSpeed_Unit.setEnabled(isSurfingEnabled);
      _lblSurfing_MinSurfingTimeDuration.setEnabled(isSurfingEnabled);
      _lblSurfing_MinSurfingTimeDuration_Unit.setEnabled(isSurfingEnabled);

      _spinnerSurfing_MinSurfingDistance.setEnabled(isSurfingEnabled);
      _spinnerSurfing_MinSpeed_Surfing.setEnabled(isSurfingEnabled);
      _spinnerSurfing_MinTimeDuration.setEnabled(isSurfingEnabled);
      _spinnerSurfing_MinSpeed_StartStop.setEnabled(isSurfingEnabled);

      _spinnerSurfing_MinSurfingDistance.setEnabled(isSurfingEnabled && isMinDistance);
      _lblSurfing_MinSurfingDistance_Unit.setEnabled(isSurfingEnabled && isMinDistance);

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
      tbm.add(_actionTourChartSegmenterConfig);

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
       * Ensure segments are created because they can be null when tour is saved and a new instance
       * is displayed
       */
      if (isSegmentLayerVisible() && _tourData.segmentSerieIndex == null) {
         createSegments(false);
      }

      // show/hide the segments in the chart
      TourManager.fireEventWithCustomData(//
            TourEventId.SEGMENT_LAYER_CHANGED,
            _tourData,
            TourSegmenterView.this);
   }

   ColorCache getColorCache() {
      return _colorCache;
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

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // miles are displayed

         spinnerDistance = (selectedDistance) * 1000 / 8;

         if (spinnerDistance == 0) {
            spinnerDistance = 1000 / 8;
         }

         // convert mile -> meters
         spinnerDistance *= UNIT_MILE;

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

      if (_tourData.isMultipleTours()) {

         // DP tolerance is saved in the pref store

         return _dpToleranceAltitudeMultipleTours;

      } else {

         return (float) (_tourData.getDpTolerance() / 10.0);
      }

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

   private SurfingFilterType getSelectedSurfingFilter() {

      final int selectedItemIndex = _comboSurfing_SegmenterFilter.getSelectionIndex();

      if (selectedItemIndex == -1) {
         return SurfingFilterType.All;
      }

      return _allSurfingSegmentFilter[selectedItemIndex].surfingFilterId;
   }

   private int[] getTourAndMarkerIndices() {

      final boolean isMultipleTours = _tourData.isMultipleTours();
      final int[] timeSerie = _tourData.timeSerie;
      final int numTimeSlices = timeSerie.length;

      final Collection<TourMarker> tourMarkers = isMultipleTours //
            ? _tourData.multiTourMarkers
            : _tourData.getTourMarkers();

      // sort markers by time - they can be unsorted
      final ArrayList<TourMarker> sortedMarkers = new ArrayList<>(tourMarkers);
      Collections.sort(sortedMarkers, new Comparator<TourMarker>() {
         @Override
         public int compare(final TourMarker tm1, final TourMarker tm2) {

            final int result = isMultipleTours //
                  ? tm1.getMultiTourSerieIndex() - tm2.getMultiTourSerieIndex()
                  : tm1.getSerieIndex() - tm2.getSerieIndex();

            return result;
         }
      });

      final TIntArrayList forcedIndices = new TIntArrayList();

      int prevSerieIndex = 0;

      // set first segment at tour start
      forcedIndices.add(prevSerieIndex);

      // Merge tour with marker indices

      final int[] tourSerieIndices;
      if (isMultipleTours) {

         final int[] tourIndices = getTourIndices();

         tourSerieIndices = Arrays.copyOf(tourIndices, tourIndices.length + 1);

         // add last timeslice index
         tourSerieIndices[tourIndices.length] = numTimeSlices - 1;

      } else {
         tourSerieIndices = new int[] { 0, numTimeSlices - 1 };
      }

      final TIntArrayList markerIndices = new TIntArrayList();

      // get a list with all marker indices
      for (final TourMarker tourMarker : sortedMarkers) {

         if (tourMarker.isMarkerVisible() == false) {

            // irgnore hidden marker
            continue;
         }

         final int serieIndex = isMultipleTours //
               ? tourMarker.getMultiTourSerieIndex()
               : tourMarker.getSerieIndex();

         markerIndices.add(serieIndex);
      }

      final int[] markerSerieIndices = markerIndices.toArray();
      int markerIndex = 0;

      for (final int tourSerieIndex : tourSerieIndices) {
         for (; markerIndex < markerSerieIndices.length; markerIndex++) {

            final int markerSerieIndex = markerSerieIndices[markerIndex];

            if (markerSerieIndex >= tourSerieIndex) {
               break;
            }

            // prevent to set a second segment at the same position
            if (markerSerieIndex != prevSerieIndex) {
               forcedIndices.add(markerSerieIndex);
            }
            prevSerieIndex = markerSerieIndex;
         }

         // prevent to set a second segment at the same position
         if (tourSerieIndex != prevSerieIndex) {
            forcedIndices.add(tourSerieIndex);
         }
         prevSerieIndex = tourSerieIndex;
      }

      return forcedIndices.toArray();
   }

   private int[] getTourIndices() {

      final boolean isMultipleTours = _tourData.isMultipleTours();
      final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;

      int[] forcedIndices = null;

      if (isMultipleTours) {

         /*
          * Create an extra segment between each tour that a segment do not cover more than 1 tour.
          * This algorithm was introduced that a selected segment do not show also the next tour in
          * the tour map.
          */

         int forcedIndex = 0;
         final int forcedIndicesSize = multipleTourStartIndex.length * 2 - 1;

         forcedIndices = new int[forcedIndicesSize];

         for (final int tourStartIndex : multipleTourStartIndex) {

            if (tourStartIndex == 0) {

               // the first tour do not have an extra segment
               forcedIndices[forcedIndex++] = 0;

            } else {

               if (forcedIndex == forcedIndicesSize - 1) {

                  // this is the last tour, it do not have an extra segment
                  forcedIndices[forcedIndex++] = tourStartIndex;

               } else {

                  // these are all tours between the first and last

                  forcedIndices[forcedIndex++] = tourStartIndex - 1;
                  forcedIndices[forcedIndex++] = tourStartIndex;
               }
            }
         }
      }

      return forcedIndices;
   }

   public ArrayList<TourSegment> getTourSegments() {
      return _allTourSegments;
   }

   private String getTourTitle() {

      if (_tourData.isMultipleTours()) {

         return TourManager.getTourTitleMultiple(_tourData);

      } else {

         return TourManager.getTourTitleDetailed(_tourData);
      }
   }

   @Override
   public ColumnViewer getViewer() {
      return _segmentViewer;
   }

   private void initUI(final Composite parent) {

      _parent = parent;
      _pc = new PixelConverter(parent);
      _spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

      _imageSurfing_SaveState = TourbookPlugin.getImageDescriptor(Messages.Image__State_SavedInTour).createImage(true);
      _imageSurfing_NotSaveState = TourbookPlugin.getImageDescriptor(Messages.Image__State_NotSavedInTour).createImage(true);

      _defaultSurfing_SelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_Surfing();
         }
      };

      _defaultSurfing_MouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onSelect_Surfing();
         }
      };

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            sort_OnSelect_SortColumn(e);
         }
      };
   }

   private boolean isSegmentLayerVisible() {

      final boolean isSegmentLayerVisible = Util.getStateBoolean(
            _state,
            STATE_IS_SHOW_TOUR_SEGMENTS,
            STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT);

      return isSegmentLayerVisible;
   }

   /**
    * @return Returns <code>true</code> when tour contains surfing parameters
    */
   private boolean isSurfingParametersSavedInTour() {

      if (_tourData != null) {

         return _tourData.getSurfing_MinSpeed_StartStop() != TourData.SURFING_VALUE_IS_NOT_SET;
      }

      return false;
   }

   /**
    * @return Returns <code>true</code> when segments are filtered, otherwise all segments are
    *         displayed.
    */
   private boolean isSurfingSegmenterFiltered() {

      return getSelectedSurfingFilter() != SurfingFilterType.All;
   }

   /**
    * @param tourId
    * @return Returns <code>true</code> when the tour is already displayed in the tour segmenter.
    */
   private boolean isTourDisplayed(final Long tourId) {

      if (_tourData != null) {
         if (_tourData.getTourId().equals(tourId)) {
            // don't reload the same tour
            return true;
         }
      }

      return false;
   }

   private void onPartOpened() {

      _state.put(STATE_IS_SEGMENTER_ACTIVE, true);

      if (_tourData != null) {
         fireSegmentLayerChanged();
      }
   }

   private void onSaveTour_Altitude() {

      if (_savedDpToleranceAltitude == -1) {
         return;
      }

      _tourData.setTourAltUp(_altitudeUp);
      _tourData.setTourAltDown(_altitudeDown);

      // update tolerance into the tour data
      _tourData.setDpTolerance((short) (_dpToleranceAltitude * 10));

      _isTourDirty = true;

      restoreVisibleDataPointsBeforeNextTourIsSet();

      final TourData tourData = saveTour();
      if (tourData != null) {

         _tourData = tourData;

         // create segments with newly saved tour that it can be displayed in the tour chart
         createSegments(false);
         updateUI_Altitude();
      }
   }

   private void onSelect_BreakTime() {

      createSegments(true);
   }

   private void onSelect_BreakTimeMethod() {

      final BreakTimeMethod selectedBreakMethod = getSelectedBreakMethod();

      if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

         _pageBookBreakTime.showPage(_pageBreakBy_AvgSpeed);
         _comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

         _pageBookBreakTime.showPage(_pageBreakBy_SliceSpeed);
         _comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeBySliceSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

         _pageBookBreakTime.showPage(_pageBreakBy_AvgSliceSpeed);
         _comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSliceSpeed);

      } else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

         _pageBookBreakTime.showPage(_pageBreakBy_TimeDistance);
         _comboBreakMethod.setToolTipText(Messages.Compute_BreakTime_Label_Description_ComputeByTime);
      }

      // break method pages have different heights, enforce layout of the whole view part
      _pageSegmenter.layout(true, true);

      onSelect_BreakTime();
   }

   private void onSelect_Distance() {

      updateUI_Distance();

      createSegments(true);
   }

   private void onSelect_Segment(final SelectionChangedEvent event) {

      if (_isInSelection) {
         return;
      }

      final IStructuredSelection selection = event.getStructuredSelection();
      if (selection != null) {

         /*
          * select the chart sliders according to the selected segment(s)
          */

         final Object[] segments = selection.toArray();

         if (segments.length > 0) {

            if (_tourChart == null) {
               _tourChart = TourManager.getActiveTourChart(_tourData);
            }

            final TourSegment firstTourSegment = (TourSegment) (segments[0]);
            final int serieStartIndex = firstTourSegment.serieIndex_Start;

            if (serieStartIndex > 0) {
               // adjust start index otherwise it is wrong displayed in the tour editor time slices
//               serieStartIndex--;
            }

            if (segments.length == 1 && firstTourSegment.isTotal) {

               // only the totals item is selected, do nothing
               return;
            }

            int lastIndex = segments.length - 1;

            TourSegment lastTourSegment = (TourSegment) (segments[lastIndex]);

            if (lastTourSegment.isTotal) {

               // the last index is the totals item, ignore this item

               lastIndex--;
               lastTourSegment = (TourSegment) (segments[lastIndex]);
            }

            final int serieEndIndex = lastTourSegment.serieIndex_End;

            final SelectionChartXSliderPosition selectionSliderPosition = new SelectionChartXSliderPosition(
                  _tourChart,
                  serieStartIndex,
                  serieEndIndex,
                  true);

            /*
             * Extend default selection with the segment positions
             */
            final SelectedTourSegmenterSegments selectedSegments = new SelectedTourSegmenterSegments();
            selectedSegments.tourData = _tourData;
            selectedSegments.xSliderSerieIndexLeft = serieStartIndex;
            selectedSegments.xSliderSerieIndexRight = serieEndIndex;

            selectionSliderPosition.setCustomData(selectedSegments);

            /*
             * Do segmenter specific actions
             */
            final TourSegmenter selectedSegmenter = getSelectedSegmenter();
            if (selectedSegmenter != null) {

               final SegmenterType selectedSegmenterType = selectedSegmenter.segmenterType;

               switch (selectedSegmenterType) {
               case Surfing:

                  final boolean isShowOnlySelectedSegments = _chkIsShowOnlySelectedSegments.getSelection();

                  if (isShowOnlySelectedSegments) {
                     onSurfing_ShowOnlySelectedSegments();
                  }

                  break;

               default:
                  break;
               }
            }

            _postSelectionProvider.setSelection(selectionSliderPosition);
         }
      }
   }

   private void onSelect_SegmenterType(final boolean isUserSelected) {

      final TourSegmenter selectedSegmenter = getSelectedSegmenter();
      if (selectedSegmenter == null) {
         clearView();
         return;
      }

      // enable segmenter table by default, segmenter can disable it
      _segmentViewer.getTable().setEnabled(true);

      final SegmenterType selectedSegmenterType = selectedSegmenter.segmenterType;

      /*
       * keep segmenter type which the user selected, try to reselect this segmenter when a new tour
       * is displayed
       */
      if (isUserSelected) {
         _userSelectedSegmenterType = selectedSegmenterType;
      }

      _isSegmenterFiltered = false;

      if (selectedSegmenterType == SegmenterType.ByAltitudeWithDP
            || selectedSegmenterType == SegmenterType.ByAltitudeWithDPMerged
            || selectedSegmenterType == SegmenterType.ByAltitudeWithMarker) {

         _pageBookSegmenter.showPage(_pageSegType_DPAltitude);

      } else if (selectedSegmenterType == SegmenterType.ByPowerWithDP) {

         _pageBookSegmenter.showPage(_pageSegType_DPPower);

      } else if (selectedSegmenterType == SegmenterType.ByPulseWithDP) {

         _pageBookSegmenter.showPage(_pageSegType_DPPulse);

      } else if (selectedSegmenterType == SegmenterType.ByMarker) {

         _pageBookSegmenter.showPage(_pageSegType_ByMarker);

      } else if (selectedSegmenterType == SegmenterType.ByDistance) {

         _pageBookSegmenter.showPage(_pageSegType_ByDistance);

      } else if (selectedSegmenterType == SegmenterType.ByComputedAltiUpDown) {

         _pageBookSegmenter.showPage(_pageSegType_ByAltiUpDown);

      } else if (selectedSegmenterType == SegmenterType.ByBreakTime) {

         _pageBookSegmenter.showPage(_pageSegType_ByBreakTime);

         // update ui + layout
         onSelect_BreakTimeMethod();

      } else if (selectedSegmenterType == SegmenterType.Surfing) {

         _pageBookSegmenter.showPage(_pageSegType_Surfing);

         _isSegmenterFiltered = isSurfingSegmenterFiltered();
         _selectedSurfingFilter = _comboSurfing_SegmenterFilter.getSelectionIndex();

         enableActions_Surfing();

      }

      updateUI_SegmenterBackground();

      _pageSegmenter.layout();

      createSegments(true);
   }

   private void onSelect_Surfing() {

      enableActions_Surfing();

      createSegments(true);
   }

   private void onSelect_Tolerance() {

      final float dpToleranceAlti = (float) (_spinnerDPTolerance_Altitude.getSelection() / 10.0);
      final float dpTolerancePower = (float) (_spinnerDPTolerance_Power.getSelection() / 10.0);
      final float dpTolerancePulse = (float) (_spinnerDPTolerance_Pulse.getSelection() / 10.0);

      // check if tolerance has changed
      if (_tourData == null || //
            (_dpToleranceAltitude == dpToleranceAlti //
                  && _dpTolerancePower == dpTolerancePower //
                  && _dpTolerancePulse == dpTolerancePulse)) {
         return;
      }

      _dpToleranceAltitude = dpToleranceAlti;
      _dpTolerancePower = dpTolerancePower;
      _dpTolerancePulse = dpTolerancePulse;

      if (_tourData.isMultipleTours()) {
         _dpToleranceAltitudeMultipleTours = dpToleranceAlti;
      }

      setTourDirty();

      createSegments(true);
   }

   /**
    * handle a tour selection event
    *
    * @param selection
    */
   private void onSelectionChanged(final ISelection selection) {

      if (selection == null) {
         // this happens when view is created
         return;
      }

      _isClearView = false;

      if (_isSaving) {
         return;
      }

      /*
       * run selection async because a tour could be modified and needs to be saved, modifications
       * are not reported to the tour data editor, saving needs also to be asynch with the tour data
       * editor
       */
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            // check if view is disposed
            if (_pageBookUI.isDisposed() || _isClearView) {
               return;
            }

            if (_isGetInitialTours && _tourData != null) {

               // tours are already setup

               _isGetInitialTours = false;

               return;
            }

            TourData tourData = null;
            TourChart eventTourChart = null;

            if (selection instanceof SelectionTourData) {

               final SelectionTourData selectionTourData = (SelectionTourData) selection;

               tourData = selectionTourData.getTourData();
               eventTourChart = selectionTourData.getTourChart();

            } else if (selection instanceof SelectionTourId) {

               final SelectionTourId tourIdSelection = (SelectionTourId) selection;

               if (isTourDisplayed(tourIdSelection.getTourId())) {
                  return;
               }

               tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

            } else if (selection instanceof SelectionTourIds) {

               final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();

               if (tourIds != null && tourIds.size() > 0) {

                  if (tourIds.size() == 1) {

                     final Long tourId = tourIds.get(0);

                     if (isTourDisplayed(tourId)) {
                        return;
                     }

                     tourData = TourManager.getInstance().getTourData(tourId);

                  } else {

                     tourData = TourManager.createMultipleTourData(tourIds);
                  }
               }

            } else if (selection instanceof SelectionDeletedTours) {

               clearView();

            } else {
               return;
            }

            if (checkDataValidation(tourData) == false) {
               return;
            }

            if (_tourData != null) {
               // it's possible that the break time serie was overwritten
               _tourData.setBreakTimeSerie(null);
            }

            /*
             * save previous tour when a new tour is selected
             */
            if (_tourData != null && _tourData == tourData) {

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
                  eventTourChart = TourManager.getActiveTourChart(tourData);
               }

               _tourChart = eventTourChart;

               setTour(tourData, false);
            }
         }
      });
   }

   private void onSetDefaults(final Composite parent) {

      saveBreakTimeValues_InPrefStore();

      PreferencesUtil.createPreferenceDialogOn(
            parent.getShell(),
            PrefPageComputedValues.ID,
            null,
            PrefPageComputedValues.TAB_FOLDER_BREAK_TIME).open();
   }

   private void onSurfing_ResetFromTour() {

      restoreState_FromTour();
      onSelect_Surfing();
   }

   private void onSurfing_ResetToDefaults() {

      _chkIsMinSurfingDistance.setSelection(STATE_SURFING_IS_MIN_DISTANCE_DEFAULT);
      _chkIsShowOnlySelectedSegments.setSelection(STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS_DEFAULT);
      selectSurfingFilter(STATE_SURFING_SEGMENT_FILTER_DEFAULT);

      final double minDistance_UI = STATE_SURFING_MIN_DISTANCE_DEFAULT / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _spinnerSurfing_MinSurfingDistance.setSelection((int) (minDistance_UI + 0.5));

      _spinnerSurfing_MinTimeDuration.setSelection(STATE_SURFING_MIN_TIME_DURATION_DEFAULT);
      _spinnerSurfing_MinSpeed_StartStop.setSelection((int) (STATE_SURFING_MIN_SPEED_START_STOP_DEFAULT * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerSurfing_MinSpeed_Surfing.setSelection((int) (STATE_SURFING_MIN_SPEED_SURFING_DEFAULT * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      onSelect_Surfing();
   }

   /**
    * @param isUpdateOrRemove
    *           When <code>true</code> visible surfing data are saved otherwise they are removed.
    */
   private void onSurfing_SaveTour(final boolean isUpdateOrRemove) {

      if (isUpdateOrRemove) {

         final SurfingData surfingData = createVisibleDataPoints(SurfingFilterType.Surfing);

         _tourData.visibleDataPointSerie = surfingData.__visibleDataPointSerie;

         _tourData.setVisiblePoints_ForSurfing(_tourData.visibleDataPointSerie);
         _tourData.setSurfing_NumberOfEvents((short) surfingData.__numSurfingEvents);

      } else {

         // delete visible data points for surfing

         _tourData.setVisiblePoints_ForSurfing(null);
         _tourData.setSurfing_NumberOfEvents((short) 0);
      }

      int minDistance = _spinnerSurfing_MinSurfingDistance.getSelection();
      int minSpeed_StartStop = _spinnerSurfing_MinSpeed_StartStop.getSelection();
      int minSpeed_Surfing = _spinnerSurfing_MinSpeed_Surfing.getSelection();

      final boolean isMinDistance = _chkIsMinSurfingDistance.getSelection();
      final int minTimeDuration = _spinnerSurfing_MinTimeDuration.getSelection();

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // convert imperial -> metric

         minSpeed_StartStop = (int) (minSpeed_StartStop / UNIT_MILE + 0.5);
         minSpeed_Surfing = (int) (minSpeed_Surfing / UNIT_MILE + 0.5);

         minDistance = (int) (minDistance / UNIT_YARD + 0.5);
      }

      _tourData.setSurfing_IsMinDistance(isMinDistance);
      _tourData.setSurfing_MinDistance((short) minDistance);
      _tourData.setSurfing_MinSpeed_StartStop((short) minSpeed_StartStop);
      _tourData.setSurfing_MinSpeed_Surfing((short) minSpeed_Surfing);
      _tourData.setSurfing_MinTimeDuration((short) minTimeDuration);

      _isTourDirty = true;
      final TourData tourData = saveTour();

      if (tourData != null) {

         _tourData = tourData;

         setTour(tourData, true);
      }
   }

   private void onSurfing_SelectSegmentFilter() {

      restoreVisibleDataPointsBeforeNextTourIsSet();

      _isSegmenterFiltered = isSurfingSegmenterFiltered();
      _selectedSurfingFilter = _comboSurfing_SegmenterFilter.getSelectionIndex();

      updateUI_SegmenterBackground();

      final SurfingFilterType selectedSurfingFilter = getSelectedSurfingFilter();
      _tourData.visibleDataPointSerie = createVisibleDataPoints(selectedSurfingFilter).__visibleDataPointSerie;

      // this will create tour segments from the visible data points
      _segmentViewer.refresh(false);

      enableActions_Surfing();

      // set default color
      Color bgColor = null;

      switch (selectedSurfingFilter) {
      case NotSurfing:
         bgColor = _colorCache.get(SEGMENTER_FILTER_2_BACKGROUND_HEADER);
         break;
      case Surfing:
         bgColor = _colorCache.get(SEGMENTER_FILTER_1_BACKGROUND_HEADER);
         break;
      case All:
         break;
      }

      _comboSurfing_SegmenterFilter.setBackground(bgColor);

      TourManager.fireEventWithCustomData(//
            TourEventId.SEGMENT_LAYER_CHANGED,
            _tourData,
            TourSegmenterView.this);
   }

   private void onSurfing_ShowOnlySelectedSegments() {

      final boolean isShowOnlySelectedSegments = _chkIsShowOnlySelectedSegments.getSelection();

      if (isShowOnlySelectedSegments) {

         // hide segments in the map which are not selected

         final int[] timeSerie = _tourData.timeSerie;
         final boolean[] visibleDataPointSerie = _tourData.visibleDataPointSerie = new boolean[timeSerie.length];

         final IStructuredSelection selection = _segmentViewer.getStructuredSelection();
         for (final Object selectedItem : selection.toList()) {

            if (selectedItem instanceof TourSegment) {

               final TourSegment tourSegment = (TourSegment) selectedItem;

               final int segmentStartIndex = tourSegment.serieIndex_Start;
               final int segmentEndIndex = tourSegment.serieIndex_End;

               for (int surfSerieIndex = segmentStartIndex + 1; surfSerieIndex <= segmentEndIndex; surfSerieIndex++) {
                  visibleDataPointSerie[surfSerieIndex] = true;
               }
            }
         }

         TourManager.fireEventWithCustomData(//
               TourEventId.SEGMENT_LAYER_CHANGED,
               _tourData,
               TourSegmenterView.this);

         enableActions_Surfing();

      } else {

         // recreate all segments

         onSelect_Surfing();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _segmentViewer.getTable().dispose();

         createUI_80_SegmentViewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _segmentViewer;
   }

   @Override
   public void reloadViewer() {

      // force content to be reloaded
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
      _spinnerBreak_MinAvgSpeedAS.setSelection(//
            (int) (btConfig.breakMinAvgSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerBreak_MinSliceSpeedAS.setSelection(//
            (int) (btConfig.breakMinSliceSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerBreak_MinSliceTimeAS.setSelection(btConfig.breakMinSliceTimeAS);

      /*
       * break time by time/distance
       */
      _spinnerBreak_ShortestTime.setSelection(btConfig.breakShortestTime);

      final float prefBreakDistance = btConfig.breakMaxDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _spinnerBreak_MaxDistance.setSelection((int) (prefBreakDistance + 0.5));

      _spinnerBreak_SliceDiff.setSelection(btConfig.breakSliceDiff);

      /*
       * break time by speed
       */
      _spinnerBreak_MinSliceSpeed.setSelection(//
            (int) (btConfig.breakMinSliceSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerBreak_MinAvgSpeed.setSelection(//
            (int) (btConfig.breakMinAvgSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      onSelect_BreakTimeMethod();

      /*
       * Altitude DP tolerance for multiple tours
       */
      _dpToleranceAltitudeMultipleTours = (float) (STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS_DEFAULT / 10.0);

      if (_tourData != null && _tourData.isMultipleTours()) {
         _spinnerDPTolerance_Altitude.setSelection(STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS_DEFAULT);
      }
   }

   private void restoreState() {

      /*
       * Actions
       */

      // chart segment layer
      final boolean stateIsSegmentLayerVisible = Util.getStateBoolean(_state,
            STATE_IS_SHOW_TOUR_SEGMENTS,
            STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT);
      _actionTourChartSegmenterConfig.setSelected(stateIsSegmentLayerVisible);

      // selected segmenter
      final String stateSegmenterName = Util.getStateString(_state,
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

      updateUI_Distance();

      _spinnerMinAltitude.setSelection(Util.getStateInt(_state, STATE_MINIMUM_ALTITUDE, 50));

      /*
       * DP tolerance
       */
      final int stateDPTolerance_Power = Util.getStateInt(_state, STATE_DP_TOLERANCE_POWER, 200);
      _dpTolerancePower = stateDPTolerance_Power / 10.0f;
      _spinnerDPTolerance_Power.setSelection(stateDPTolerance_Power);

      final int stateDPTolerancePulse = Util.getStateInt(_state, STATE_DP_TOLERANCE_PULSE, 50);
      _dpTolerancePulse = stateDPTolerancePulse / 10.0f;
      _spinnerDPTolerance_Pulse.setSelection(stateDPTolerancePulse);

      final int stateDPToleranceMultipleTours = Util.getStateInt(_state,
            STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS,
            STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS_DEFAULT);
      _dpToleranceAltitudeMultipleTours = (float) (stateDPToleranceMultipleTours / 10.0);

      /*
       * break time
       */
      final BreakTimeTool btConfig = BreakTimeTool.getPrefValues();

      /*
       * break method
       */
      selectBreakMethod(Util.getStateString(_state, STATE_SELECTED_BREAK_METHOD2, BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED));

      /*
       * break by avg + slice speed
       */
      final float stateAvgSpeedAS = Util.getStateFloat(_state, STATE_BREAK_TIME_MIN_AVG_SPEED_AS, btConfig.breakMinAvgSpeedAS);
      final float stateSliceSpeedAS = Util.getStateFloat(_state, STATE_BREAK_TIME_MIN_SLICE_SPEED_AS, btConfig.breakMinSliceSpeedAS);
      final int stateSliceTimeAS = Util.getStateInt(_state, STATE_BREAK_TIME_MIN_SLICE_TIME_AS, btConfig.breakMinSliceTimeAS);

      _spinnerBreak_MinAvgSpeedAS.setSelection((int) (stateAvgSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerBreak_MinSliceSpeedAS.setSelection((int) (stateSliceSpeedAS * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
      _spinnerBreak_MinSliceTimeAS.setSelection(stateSliceTimeAS);

      /*
       * break by slice speed
       */
      final float stateSliceSpeed = Util.getStateFloat(_state,
            STATE_BREAK_TIME_MIN_SLICE_SPEED,
            btConfig.breakMinSliceSpeed);

      _spinnerBreak_MinSliceSpeed.setSelection((int) (stateSliceSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      /*
       * break by avg speed
       */
      final float stateAvgSpeed = Util.getStateFloat(_state,
            STATE_BREAK_TIME_MIN_AVG_SPEED,
            btConfig.breakMinAvgSpeed);

      _spinnerBreak_MinAvgSpeed.setSelection((int) (stateAvgSpeed * SPEED_DIGIT_VALUE * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      /*
       * break time by time/distance
       */
      _spinnerBreak_ShortestTime.setSelection(Util.getStateInt(_state,
            STATE_BREAK_TIME_MIN_TIME_VALUE,
            btConfig.breakShortestTime));

      final float breakDistance = Util.getStateFloat(_state,
            STATE_BREAK_TIME_MIN_DISTANCE_VALUE,
            btConfig.breakMaxDistance) / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _spinnerBreak_MaxDistance.setSelection((int) (breakDistance + 0.5));

      _spinnerBreak_SliceDiff.setSelection(Util.getStateInt(_state,
            STATE_BREAK_TIME_SLICE_DIFF,
            btConfig.breakSliceDiff));

      /*
       * Setup colors
       */
      _colorCache.setColor(SEGMENTER_FILTER_1_BACKGROUND, SEGMENTER_FILTER_1_BACKGROUND_RGB);
      _colorCache.setColor(SEGMENTER_FILTER_2_BACKGROUND, SEGMENTER_FILTER_2_BACKGROUND_RGB);
      _colorCache.setColor(SEGMENTER_FILTER_1_BACKGROUND_HEADER, SEGMENTER_FILTER_1_BACKGROUND_RGB_HEADER);
      _colorCache.setColor(SEGMENTER_FILTER_2_BACKGROUND_HEADER, SEGMENTER_FILTER_2_BACKGROUND_RGB_HEADER);

      _colorCache.setColor(
            STATE_COLOR_ALTITUDE_UP,
            Util.getStateRGB(_state, STATE_COLOR_ALTITUDE_UP, STATE_COLOR_ALTITUDE_UP_DEFAULT));

      _colorCache.setColor(
            STATE_COLOR_ALTITUDE_DOWN,
            Util.getStateRGB(_state, STATE_COLOR_ALTITUDE_DOWN, STATE_COLOR_ALTITUDE_DOWN_DEFAULT));

      _colorCache.setColor(
            STATE_COLOR_TOTALS,
            Util.getStateRGB(_state, STATE_COLOR_TOTALS, STATE_COLOR_TOTALS_DEFAULT));

      /*
       * Surfing
       */
      final Enum<SurfingFilterType> stateSurfingFilterId = Util.getStateEnum(_state,
            STATE_SURFING_SEGMENT_FILTER,
            SurfingFilterType.All);
      selectSurfingFilter(stateSurfingFilterId);

      _chkIsShowOnlySelectedSegments.setSelection(Util.getStateBoolean(_state,
            STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS,
            STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS_DEFAULT));

      /*
       * Surfing states which are also saved in a tour
       */
      _spinnerSurfing_MinTimeDuration.setSelection(Util.getStateInt(_state,
            STATE_SURFING_MIN_TIME_DURATION,
            STATE_SURFING_MIN_TIME_DURATION_DEFAULT));

      final int stateMinStartStopSpeed = Util.getStateInt(_state,
            STATE_SURFING_MIN_SPEED_START_STOP,
            STATE_SURFING_MIN_SPEED_START_STOP_DEFAULT);
      _spinnerSurfing_MinSpeed_StartStop.setSelection((int) (stateMinStartStopSpeed * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      final int stateMinSurfingSpeed = Util.getStateInt(_state,
            STATE_SURFING_MIN_SPEED_SURFING,
            STATE_SURFING_MIN_SPEED_SURFING_DEFAULT);
      _spinnerSurfing_MinSpeed_Surfing.setSelection((int) (stateMinSurfingSpeed * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

      // distance
      _chkIsMinSurfingDistance.setSelection(Util.getStateBoolean(_state,
            STATE_SURFING_IS_MIN_DISTANCE,
            STATE_SURFING_IS_MIN_DISTANCE_DEFAULT));

      final int stateMinDistance = Util.getStateInt(_state,
            STATE_SURFING_MIN_DISTANCE,
            STATE_SURFING_MIN_DISTANCE_DEFAULT);
      final double minDistance_UI = stateMinDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _spinnerSurfing_MinSurfingDistance.setSelection((int) (minDistance_UI + 0.5));
   }

   private void restoreState_FromTour() {

      final boolean surfing_IsMinDistance = _tourData.isSurfing_IsMinDistance();
      final short surfing_MinDistance = _tourData.getSurfing_MinDistance();
      final short surfing_MinSpeed_StartStop = _tourData.getSurfing_MinSpeed_StartStop();
      final short surfing_MinSpeed_Surfing = _tourData.getSurfing_MinSpeed_Surfing();
      final short surfing_MinTimeDuration = _tourData.getSurfing_MinTimeDuration();

      final boolean state_IsMinDistance = surfing_MinDistance == TourData.SURFING_VALUE_IS_NOT_SET
            ? STATE_SURFING_IS_MIN_DISTANCE_DEFAULT
            : surfing_IsMinDistance;

      final int stateMinDistance = surfing_MinDistance == TourData.SURFING_VALUE_IS_NOT_SET
            ? STATE_SURFING_MIN_DISTANCE_DEFAULT
            : surfing_MinDistance;

      final int state_MinSpeed_StartStop = surfing_MinSpeed_StartStop == TourData.SURFING_VALUE_IS_NOT_SET
            ? STATE_SURFING_MIN_SPEED_START_STOP_DEFAULT
            : surfing_MinSpeed_StartStop;

      final int state_MinSpeed_Surfing = surfing_MinSpeed_Surfing == TourData.SURFING_VALUE_IS_NOT_SET
            ? STATE_SURFING_MIN_SPEED_SURFING_DEFAULT
            : surfing_MinSpeed_Surfing;

      final short state_MinTimeDuration = surfing_MinTimeDuration == TourData.SURFING_VALUE_IS_NOT_SET
            ? STATE_SURFING_MIN_TIME_DURATION_DEFAULT
            : surfing_MinTimeDuration;

      final int minDistance_UI = (int) (stateMinDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL + 0.5);
      final int minSpeed_StartStop_UI = (int) (state_MinSpeed_StartStop * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
      final int minSpeed_Surfing_UI = (int) (state_MinSpeed_Surfing * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

      _chkIsMinSurfingDistance.setSelection(state_IsMinDistance);
      _spinnerSurfing_MinSpeed_StartStop.setSelection(minSpeed_StartStop_UI);
      _spinnerSurfing_MinSpeed_Surfing.setSelection(minSpeed_Surfing_UI);
      _spinnerSurfing_MinSurfingDistance.setSelection(minDistance_UI);
      _spinnerSurfing_MinTimeDuration.setSelection(state_MinTimeDuration);

      final boolean isVisibleDataPointSerieSaved = _tourData.isVisiblePointsSaved_ForSurfing();
      if (isVisibleDataPointSerieSaved) {

         _iconSaveSurfingState.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_IsSaveState_Tooltip);
         _iconSaveSurfingState.setImage(_imageSurfing_SaveState);

      } else {

         _iconSaveSurfingState.setToolTipText(Messages.Tour_Segmenter_Surfing_Button_IsNotSaveState_Tooltip);
         _iconSaveSurfingState.setImage(_imageSurfing_NotSaveState);
      }

      updateUI_Surfing_RestoreTour();
   }

   /**
    * Prevent that visible points are saved accidently, visible points must be saved explicitly !!!
    */
   private void restoreVisibleDataPointsBeforeNextTourIsSet() {

      if (_tourData != null) {
         _tourData.restoreVisiblePoints_ForSurfing();
      }
   }

   /**
    * saves break time values in the pref store
    */
   private void saveBreakTimeValues_InPrefStore() {

      // break method
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_METHOD2, getSelectedBreakMethod().methodId);

      // break by avg+slice speed
      final float breakMinAvgSpeedAS = _spinnerBreak_MinAvgSpeedAS.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      final float breakMinSliceSpeedAS = _spinnerBreak_MinSliceSpeedAS.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      final int breakMinSliceTimeAS = _spinnerBreak_MinSliceTimeAS.getSelection();
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

      // break by slice speed
      final float breakMinSliceSpeed = _spinnerBreak_MinSliceSpeed.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

      // break by avg speed
      final float breakMinAvgSpeed = _spinnerBreak_MinAvgSpeed.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

      // break by time/distance
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, _spinnerBreak_ShortestTime.getSelection());
      final float breakDistance = _spinnerBreak_MaxDistance.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, breakDistance);
      _prefStore.setValue(ITourbookPreferences.BREAK_TIME_SLICE_DIFF, _spinnerBreak_SliceDiff.getSelection());
   }

   /**
    * save break time values in the viewer settings
    */
   private void saveBreakTimeValues_InState() {

      // break method
      _state.put(STATE_SELECTED_BREAK_METHOD2, getSelectedBreakMethod().methodId);

      // break by avg+slice speed
      final float breakMinAvgSpeedAS = _spinnerBreak_MinAvgSpeedAS.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      final float breakMinSliceSpeedAS = _spinnerBreak_MinSliceSpeedAS.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      final int breakMinSliceTimeAS = _spinnerBreak_MinSliceTimeAS.getSelection();

      _state.put(STATE_BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
      _state.put(STATE_BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
      _state.put(STATE_BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

      // break by slice speed
      final float breakMinSliceSpeed = _spinnerBreak_MinSliceSpeed.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _state.put(STATE_BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

      // break by avg speed
      final float breakMinAvgSpeed = _spinnerBreak_MinAvgSpeed.getSelection() / SPEED_DIGIT_VALUE / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _state.put(STATE_BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

      // break by time/distance
      final float breakDistance = _spinnerBreak_MaxDistance.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;
      _state.put(STATE_BREAK_TIME_MIN_DISTANCE_VALUE, breakDistance);
      _state.put(STATE_BREAK_TIME_MIN_TIME_VALUE, _spinnerBreak_ShortestTime.getSelection());
      _state.put(STATE_BREAK_TIME_SLICE_DIFF, _spinnerBreak_SliceDiff.getSelection());
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SELECTED_SEGMENTER_BY_USER, _userSelectedSegmenterType.name());
      _state.put(STATE_SELECTED_DISTANCE, _spinnerDistance.getSelection());
      _state.put(STATE_MINIMUM_ALTITUDE, _spinnerMinAltitude.getSelection());
      _state.put(STATE_DP_TOLERANCE_POWER, _spinnerDPTolerance_Power.getSelection());
      _state.put(STATE_DP_TOLERANCE_PULSE, _spinnerDPTolerance_Pulse.getSelection());

      _state.put(STATE_DP_TOLERANCE_ALTITUDE_MULTIPLE_TOURS, (int) (_dpToleranceAltitudeMultipleTours * 10));

      /*
       * Surfing
       */
      int minSurfingDistance = _spinnerSurfing_MinSurfingDistance.getSelection();
      int minSpeed_StartStop = _spinnerSurfing_MinSpeed_StartStop.getSelection();
      int minSpeed_Surfing = _spinnerSurfing_MinSpeed_Surfing.getSelection();

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // convert imperial -> metric

         minSurfingDistance = (int) (minSurfingDistance / UNIT_YARD + 0.5);
         minSpeed_StartStop = (int) (minSpeed_StartStop / UNIT_MILE + 0.5);
         minSpeed_Surfing = (int) (minSpeed_Surfing / UNIT_MILE + 0.5);
      }

      Util.setStateEnum(_state, STATE_SURFING_SEGMENT_FILTER, getSelectedSurfingFilter());

      _state.put(STATE_SURFING_IS_MIN_DISTANCE, _chkIsMinSurfingDistance.getSelection());
      _state.put(STATE_SURFING_IS_SHOW_ONLY_SELECTED_SEGMENTS, _chkIsShowOnlySelectedSegments.getSelection());
      _state.put(STATE_SURFING_MIN_DISTANCE, minSurfingDistance);
      _state.put(STATE_SURFING_MIN_SPEED_START_STOP, minSpeed_StartStop);
      _state.put(STATE_SURFING_MIN_SPEED_SURFING, minSpeed_Surfing);
      _state.put(STATE_SURFING_MIN_TIME_DURATION, _spinnerSurfing_MinTimeDuration.getSelection());

      /*
       * Break time
       */
      saveBreakTimeValues_InState();
   }

   /**
    * @return Returns saved tour entity or <code>null</code> when saving is not done.
    */
   private TourData saveTour() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return null;
      }

      if (_tourData != null) {
         // it's possible that the break time serie was overwritten
         _tourData.setBreakTimeSerie(null);
      }

      if (_isTourDirty == false || _tourData == null) {
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

   private void selectSurfingFilter(final Enum<SurfingFilterType> surfingFilterId) {

      int comboIndex = 0;

      for (final SurfingFilter surfingFilter : _allSurfingSegmentFilter) {

         if (surfingFilter.surfingFilterId == surfingFilterId) {

            _comboSurfing_SegmenterFilter.select(comboIndex);
            return;
         }

         comboIndex++;
      }

      // set default
      _comboSurfing_SegmenterFilter.select(0);
   }

   private void selectTourSegments(final SelectedTourSegmenterSegments selectedSegmenterConfig) {

      if (selectedSegmenterConfig.tourData != _tourData || _allTourSegments == null) {
         return;
      }

      /*
       * Find tour segments for the left and right slider
       */
      final int selectedLeftIndex = selectedSegmenterConfig.xSliderSerieIndexLeft;
      final int selectedRightIndex = selectedSegmenterConfig.xSliderSerieIndexRight;

      TourSegment leftSegment = null;
      TourSegment rightSegment = null;

      final ArrayList<TourSegment> selectedSegments = new ArrayList<>();

      for (final TourSegment tourSegment : _allTourSegments) {

         if (leftSegment == null && tourSegment.serieIndex_Start == selectedLeftIndex) {

            leftSegment = tourSegment;
            selectedSegments.add(leftSegment);
         }

         if (rightSegment == null && tourSegment.serieIndex_End == selectedRightIndex) {

            rightSegment = tourSegment;

            if (leftSegment != rightSegment) {
               selectedSegments.add(rightSegment);
            }
         }

         if (leftSegment != null && rightSegment == null) {

            // add all segments between the left and right
            selectedSegments.add(tourSegment);
         }

         if (leftSegment != null && rightSegment != null) {

            _segmentViewer.setSelection(new StructuredSelection(selectedSegments), true);

            return;
         }
      }
   }

   private void setCellColor(final ViewerCell cell, final double value) {

      if (value == 0) {

         cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

      } else if (value > 0) {

         cell.setBackground(_colorCache.get(STATE_COLOR_ALTITUDE_UP));

      } else if (value < 0) {

         cell.setBackground(_colorCache.get(STATE_COLOR_ALTITUDE_DOWN));
      }
   }

   @Override
   public void setFocus() {

   }

   private void setMaxDistanceSpinner() {

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // imperial

         _maxDistanceSpinner = MAX_DISTANCE_SPINNER_MILE;
         _spinnerDistancePage = 8;

      } else {

         // metric

         _maxDistanceSpinner = MAX_DISTANCE_SPINNER_METRIC;
         _spinnerDistancePage = 10;
      }
   }

   private void setStyle_ColumnTotal(final ViewerCell cell) {

      cell.setBackground(_colorCache.get(STATE_COLOR_TOTALS));
   }

   private void setStyle_Filter(final ViewerCell cell, final int filter) {

      if (_isSegmenterFiltered) {

//         if (filter == 1) {
//
//            cell.setBackground(_colorCache.get(SEGMENTER_FILTER_1_BACKGROUND));
//
//         } else if (filter == 2) {
//
//            cell.setBackground(_colorCache.get(SEGMENTER_FILTER_2_BACKGROUND));
//         }
      } else {

         // show surfing segments with more contrast

         if (filter == 1) {

            cell.setBackground(_colorCache.get(SEGMENTER_FILTER_1_BACKGROUND_HEADER));
         }
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
         restoreVisibleDataPointsBeforeNextTourIsSet();

         _tourData = tourData;

         _pageBookUI.showPage(_pageSegmenter);
         enableActions();

         // update tour title
         _lblTitle.setText(getTourTitle());

         // keep original dp tolerance
         _savedDpToleranceAltitude = _dpToleranceAltitude = getDPTolerance_FromTour();

         // segmenter value
         _spinnerDPTolerance_Altitude.setSelection((int) (getDPTolerance_FromTour() * 10));

         final boolean canSaveTour = _tourData.getTourPerson() != null;
         _btnSaveTourDP.setEnabled(canSaveTour);
         _btnSaveTourMin.setEnabled(canSaveTour);

         updateUI_Surfing_RestoreTour();

         restoreState_FromTour();
      }
      _isDirtyDisabled = false;

      updateUI_SegmenterSelector();
      onSelect_SegmenterType(false);
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

         _isGetInitialTours = true;

         Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {

               onSelectionChanged(TourManager.getSelectedToursSelection());
            }
         });
      }
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn sort_GetSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _segmentViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final ColumnDefinition colDef = (ColumnDefinition) column.getData();
         final String columnId = colDef.getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   private void sort_OnSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _segmentViewer.getSelection();
         {
            // update viewer with new sorting
            _segmentComparator.setSortColumn(e.widget);
            _segmentViewer.refresh();
         }

         // reselect
         _segmentViewer.setSelection(selectionBackup, true);
         _segmentViewer.getTable().showSelection();

      }
      _viewerContainer.setRedraw(true);
   }

   /**
    * Set the sort column direction indicator for a column or hide indicator when it is not a
    * sorable column.
    *
    * @param sortColumnId
    * @param isSortableColumn
    * @param isAscendingSort
    */
   private void sort_UpdateUI_SetSortDirection(final String sortColumnId,
                                               final int sortDirection) {

      final boolean isColumnSortable = _allSortableColumns.contains(_segmentComparator.__sortColumnId);

      if (isColumnSortable) {

         final Table table = _segmentViewer.getTable();
         final TableColumn tc = sort_GetSortColumn(sortColumnId);

         table.setSortColumn(tc);
         table.setSortDirection(sortDirection == SegmenterComparator.ASCENDING ? SWT.UP : SWT.DOWN);
      }
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub

   }

   /**
    * update ascending altitude computed value
    */
   private void updateUI_Altitude() {

      final TourSegmenter selectedSegmenter = getSelectedSegmenter();
      if (selectedSegmenter == null) {
         clearView();
         return;
      }

      Label lblInfo;
      float[] altitudeSegments = null;

      // compute total alti up/down from the segments
      _altitudeUp = 0;
      _altitudeDown = 0;

      if (selectedSegmenter.segmenterType == SegmenterType.ByComputedAltiUpDown) {

         // Minimum altitude

         altitudeSegments = _tourData.segmentSerie_Altitude_Diff_Computed;
         lblInfo = _lblAltitudeUpMin;

      } else {

         // DP tolerance

         if (selectedSegmenter.segmenterType == SegmenterType.ByAltitudeWithMarker) {

            _altitudeDown = _tourData.segmentSerieTotal_Altitude_Down;
            _altitudeUp = _tourData.segmentSerieTotal_Altitude_Up;

         } else {
            altitudeSegments = _tourData.segmentSerie_Altitude_Diff;
         }

         lblInfo = _lblAltitudeUpDP;
      }

      if (altitudeSegments == null && _altitudeDown == 0 && _altitudeUp == 0) {

         lblInfo.setText(UI.EMPTY_STRING);
         lblInfo.setToolTipText(UI.EMPTY_STRING);

         return;
      }

      if (altitudeSegments != null) {

         for (final float altitude : altitudeSegments) {
            if (altitude > 0) {
               _altitudeUp += altitude;
            } else {
               _altitudeDown += -altitude;
            }
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

      lblInfo.setToolTipText(NLS.bind(Messages.Tour_Segmenter_Label_AltitudeUpDown_Tooltip,

            new Object[] {

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

   private void updateUI_BreakTime() {

      _lblTourBreakTime.setText(Long.toString(_tourBreakTime)
            + UI.SPACE
            + Messages.App_Unit_Seconds_Small
            + UI.SPACE4
            + net.tourbook.common.UI.format_hh_mm_ss(_tourBreakTime));

      _containerBreakTime.layout();
   }

   private void updateUI_Distance() {

      float spinnerDistance = getDistance() / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

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

   private void updateUI_SegmenterBackground() {

      final Table segmenterTable = _segmentViewer.getTable();

      if (_isSegmenterFiltered) {

         if (_selectedSurfingFilter == 1) {

//            segmenterTable.setBackground(_colorCache.get(SEGMENTER_FILTER_1_BACKGROUND));
            segmenterTable.setHeaderBackground(_colorCache.get(SEGMENTER_FILTER_1_BACKGROUND_HEADER));

         } else if (_selectedSurfingFilter == 2) {

//            segmenterTable.setBackground(_colorCache.get(SEGMENTER_FILTER_2_BACKGROUND));
            segmenterTable.setHeaderBackground(_colorCache.get(SEGMENTER_FILTER_2_BACKGROUND_HEADER));
         }

      } else {
         segmenterTable.setBackground(null);
         segmenterTable.setHeaderBackground(null);
      }

   }

   private void updateUI_SegmenterInfo(final Object[] tourSegments) {

      final String numSegments = Integer.toString(tourSegments.length - 1);

      _lblNumSegments.setText(numSegments);
   }

   private void updateUI_SegmenterSelector() {

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

   private void updateUI_Surfing_MeasurementValues() {

      final int minDistance = _spinnerSurfing_MinSurfingDistance.getSelection();
      final int minSpeed_StartStop = _spinnerSurfing_MinSpeed_StartStop.getSelection();
      final int minSpeed_Surfing = _spinnerSurfing_MinSpeed_Surfing.getSelection();

      // this conversion is not exactly but the measurement system is not changed very often

      if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == UNIT_MILE) {

         // imperial -> metric

         _spinnerSurfing_MinSpeed_StartStop.setSelection((int) ((minSpeed_StartStop / UNIT_MILE) + 0.5));
         _spinnerSurfing_MinSpeed_Surfing.setSelection((int) ((minSpeed_Surfing / UNIT_MILE) + 0.5));

         _spinnerSurfing_MinSurfingDistance.setSelection((int) (minDistance / UNIT_YARD + 0.5));

      } else {

         // metric -> imperial

         _spinnerSurfing_MinSpeed_StartStop.setSelection((int) (minSpeed_StartStop * UNIT_MILE));
         _spinnerSurfing_MinSpeed_Surfing.setSelection((int) (minSpeed_Surfing * UNIT_MILE));

         _spinnerSurfing_MinSurfingDistance.setSelection((int) (minDistance * UNIT_YARD));
      }

      _lblSurfing_MinStartStopSpeed_Unit.setText(UI.UNIT_LABEL_SPEED);
      _lblSurfing_MinSurfingSpeed_Unit.setText(UI.UNIT_LABEL_SPEED);

      _lblSurfing_MinSurfingDistance_Unit.setText(UI.UNIT_LABEL_DISTANCE_M_OR_YD);
   }

   private void updateUI_Surfing_RestoreTour() {

      _btnSurfing_RestoreFrom_Tour.setToolTipText(String.format(

            Messages.Tour_Segmenter_Surfing_Button_RestoreFromTourWithData_Tooltip,

            // min start/stop speed
            _tourData.getSurfing_MinSpeed_StartStop(),
            UI.UNIT_LABEL_SPEED,

            // min surfing speed
            _tourData.getSurfing_MinSpeed_Surfing(),
            UI.UNIT_LABEL_SPEED,

            // min time duration
            _tourData.getSurfing_MinTimeDuration(),
            Messages.App_Unit_Seconds_Small,

            // min distance
            _tourData.getSurfing_MinDistance(),
            UI.UNIT_LABEL_DISTANCE_M_OR_YD,

            // is min distance
            _tourData.isSurfing_IsMinDistance()
                  ? Messages.App__True
                  : Messages.App__False
      //
      ));
   }
}
