/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map2;

import java.awt.GraphicsEnvironment;
import java.text.NumberFormat;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageTourMarkerTypes;
import net.tourbook.tour.location.CommonLocationView;
import net.tourbook.tour.location.TourLocationView;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.widget.mt.NatComboMT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Slideout for all 2D map locations and marker
 */
public class SlideoutMap2_MapPoints extends AdvancedSlideout implements

      IColorSelectorListener,
      IChangeUIListener,
      IActionResetToDefault {

   //
   private static final String        STATE_EXPANDED_HEIGHT          = "STATE_EXPANDED_HEIGHT";      //$NON-NLS-1$
   private static final String        STATE_IS_SLIDEOUT_EXPANDED     = "STATE_IS_SLIDEOUT_EXPANDED"; //$NON-NLS-1$
   private static final String        STATE_SELECTED_TAB             = "STATE_SELECTED_TAB";         //$NON-NLS-1$
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelLayout_Label}
    */
   private static MapLabelLayout[]    _allMarkerLabelLayout_Value    = {

         MapLabelLayout.RECTANGLE_BOX,
         MapLabelLayout.BORDER_2_PIXEL,
         MapLabelLayout.BORDER_1_PIXEL,
         MapLabelLayout.SHADOW,
         MapLabelLayout.NONE,
   };
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelLayout_Value}
    */
   private static String[]            _allMarkerLabelLayout_Label    = {

         Messages.Map_Points_LabelBackground_RectangleBox,
         Messages.Map_Points_LabelBackground_Border2,
         Messages.Map_Points_LabelBackground_Border1,
         Messages.Map_Points_LabelBackground_Shadow,
         Messages.Map_Points_LabelBackground_None,
   };
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelTime_Label}
    */
   private static MapTourMarkerTime[] _allMarkerLabelTimeStamp_Value = {

         MapTourMarkerTime.NONE,
         MapTourMarkerTime.DATE_NO_YEAR,
         MapTourMarkerTime.DATE,
         MapTourMarkerTime.TIME,
         MapTourMarkerTime.DATE_TIME,
   };
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelTimeStamp_Value}
    */
   private static String[]            _allMarkerLabelTime_Label      = {

         Messages.Map_Points_LabelTime_None,
         Messages.Map_Points_LabelTime_Date_NoYear,
         Messages.Map_Points_LabelTime_Date,
         Messages.Map_Points_LabelTime_Time,
         Messages.Map_Points_LabelTime_DateTime,
   };
   //
   //
   private static final String[] _allFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
   //
   //
   private static final IPreferenceStore  _prefStore      = TourbookPlugin.getPrefStore();
   private IDialogSettings                _state_Map2;
   private IDialogSettings                _state_Slideout;
   //
   private Map2View                       _map2View;
   private ToolItem                       _toolItem;
   private ToolBarManager                 _toolbarManagerExpandCollapseSlideout;
   //
   private SelectionListener              _mapPointSelectionListener;
   private SelectionListener              _mapPointSelectionListener_All;
   private IPropertyChangeListener        _mapPointPropertyChangeListener;
   private MouseWheelListener             _mapPointMouseWheelListener;
   private MouseWheelListener             _mapPointMouseWheelListener4;
   private MouseWheelListener             _mapPointMouseWheelListener10;
   private FocusListener                  _keepOpenListener;
   private IPropertyChangeListener        _prefChangeListener;
   //
   private ActionExpandSlideout           _actionExpandCollapseSlideout;
   private ActionOpenPrefDialog           _actionMarkerType_Prefs;
   private ActionMarkerType_SelectAll     _actionMarkerType_SelectAll;
   private ActionMarkerType_SelectInverse _actionMarkerType_SelectInverse;
   private ActionMarkerType_SelectNone    _actionMarkerType_SelectNone;
   private ActionResetToDefaults          _actionRestoreDefaults;
   private ActionStatistic_CommonLocation _actionStatistic_CommonLocation;
   private ActionStatistic_TourLocation   _actionStatistic_TourLocation;
   private ActionStatistic_TourMarker     _actionStatistic_TourMarker;
   private ActionStatistic_TourPause      _actionStatistic_TourPause;
   private ActionStatistic_TourPhotos     _actionStatistic_TourPhotos;
   private ActionStatistic_TourWayPoint   _actionStatistic_TourWayPoint;
   //
   private PixelConverter                 _pc;
   //
   private boolean                        _isSlideoutExpanded;
   private int                            _expandingCounter;
   private int                            _expandedHeight = -1;
   //
   private boolean                        _isInUpdateUI;
   private TourPauseUI                    _tourPausesUI;
   //
   private final NumberFormat             _nf3            = NumberFormat.getNumberInstance();
   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }
   //
   private GridDataFactory _spinnerGridData;
   //
   /*
    * UI controls
    */
   private Composite             _shellContainer;
   private Composite             _statisticsContainer;
   private Composite             _tabContainer;
   //
   private CTabFolder            _tabFolder;
   //
   private CTabItem              _tabAll;
   private CTabItem              _tabCommonLocations;
   private CTabItem              _tabOptions;
   private CTabItem              _tabTourLocations;
   private CTabItem              _tabTourMarkers;
   private CTabItem              _tabTourMarkerGroups;
   private CTabItem              _tabTourPauses;
   private CTabItem              _tabTourWayPoints;
   //
   private Button                _btnSwapClusterSymbolColor;
   private Button                _btnSwapCommonLocationLabel_Color;
   private Button                _btnSwapTourLocationLabel_Color;
   private Button                _btnSwapTourLocation_StartLabel_Color;
   private Button                _btnSwapTourLocation_EndLabel_Color;
   private Button                _btnSwapTourMarkerLabel_Color;
   private Button                _btnSwapTourPauseLabel_Color;
   private Button                _btnSwapTourWayPointLabel_Color;
   //
   private Button                _chkIsLabelAntialiased;
   private Button                _chkIsDimMap;
   private Button                _chkIsFillClusterSymbol;
   private Button                _chkIsFilterTourMarkers;
   private Button                _chkIsGroupMarkers;
   private Button                _chkIsGroupMarkers_All;
   private Button                _chkIsMarkerClustered;
   private Button                _chkIsMarkerClustered_All;
   private Button                _chkIsShowCommonLocations;
   private Button                _chkIsShowCommonLocations_All;
   private Button                _chkIsShowBoundingBox_All;
   private Button                _chkIsShowBoundingBox_Common;
   private Button                _chkIsShowBoundingBox_Tour;
   private Button                _chkIsShowTourLocations;
   private Button                _chkIsShowTourLocations_All;
   private Button                _chkIsShowTourMarkers;
   private Button                _chkIsShowTourMarkers_All;
   private Button                _chkIsShowTourPauses;
   private Button                _chkIsShowTourPauses_All;
   private Button                _chkIsShowTourWayPoints;
   private Button                _chkIsShowTourWayPoints_All;
   private Button                _chkIsTruncateLabel;
   //
   private Combo                 _comboLabelFont;
   private Combo                 _comboLabelLayout;
   private Combo                 _comboTourMarkerTime;
   //
   private Label                 _lblClusterGrid_Size;
   private Label                 _lblClusterSymbol;
   private Label                 _lblClusterSymbol_Size;
   private Label                 _lblGroupDuplicatedMarkers;
   private Label                 _lblLabelGroupGridSize;
   private Label                 _lblCommonLocationLabel_Color;
   private Label                 _lblFontName;
   private Label                 _lblLabelBackground;
   private Label                 _lblLabelSize;
   private Label                 _lblStats_CommonLocations_All;
   private Label                 _lblStats_CommonLocations_Visible;
   private Label                 _lblStats_TourLocations_All;
   private Label                 _lblStats_TourLocations_Visible;
   private Label                 _lblStats_TourMarkers_All;
   private Label                 _lblStats_TourMarkers_Visible;
   private Label                 _lblStats_Photos_All;
   private Label                 _lblStats_Photos_Visible;
   private Label                 _lblStats_TourWayPoints_All;
   private Label                 _lblStats_TourWayPoints_Visible;
   private Label                 _lblStats_TourPauses_All;
   private Label                 _lblStats_TourPauses_Visible;
   private Label                 _lblTourLocationLabel_Color;
   private Label                 _lblTourLocation_StartLabel_Color;
   private Label                 _lblTourLocation_EndLabel_Color;
   private Label                 _lblTourMarkerLabel_Color;
   private Label                 _lblTourMarkerTime;
   private Label                 _lblTourPauseLabel_Color;
   private Label                 _lblVisibleLabels;
   private Label                 _lblTourWayPointLabel_Color;
   //
   private Spinner               _spinnerClusterGrid_Size;
   private Spinner               _spinnerClusterOutline_Width;
   private Spinner               _spinnerClusterSymbol_Size;
   private Spinner               _spinnerLabelDistributorMaxLabels;
   private Spinner               _spinnerLabelDistributorRadius;
   private Spinner               _spinnerLabelFontSize;
   private Spinner               _spinnerLabelGroupGridSize;
   private Spinner               _spinnerLabelRespectMargin;
   private Spinner               _spinnerLabelTruncateLength;
   private Spinner               _spinnerLocationSymbolSize;
   private Spinner               _spinnerMapDimValue;
   //
   private Text                  _txtGroupDuplicatedMarkers;
   //
   private ColorSelectorExtended _colorClusterSymbol_Fill;
   private ColorSelectorExtended _colorClusterSymbol_Outline;
   private ColorSelectorExtended _colorCommonLocationLabel_Fill;
   private ColorSelectorExtended _colorCommonLocationLabel_Outline;
   private ColorSelectorExtended _colorMapDimColor;
   private ColorSelectorExtended _colorTourMarkerLabel_Fill;
   private ColorSelectorExtended _colorTourMarkerLabel_Outline;
   private ColorSelectorExtended _colorTourLocationLabel_Fill;
   private ColorSelectorExtended _colorTourLocationLabel_Outline;
   private ColorSelectorExtended _colorTourLocation_StartLabel_Outline;
   private ColorSelectorExtended _colorTourLocation_StartLabel_Fill;
   private ColorSelectorExtended _colorTourLocation_EndLabel_Outline;
   private ColorSelectorExtended _colorTourLocation_EndLabel_Fill;
   private ColorSelectorExtended _colorTourPauseLabel_Outline;
   private ColorSelectorExtended _colorTourPauseLabel_Fill;
   private ColorSelectorExtended _colorTourWayPointLabel_Fill;
   private ColorSelectorExtended _colorTourWayPointLabel_Outline;
   //
   private ImageDescriptor       _imageDescriptor_BoundingBox;
   private ImageDescriptor       _imageDescriptor_CommonLocation;
   private ImageDescriptor       _imageDescriptor_SlideoutCollapse;
   private ImageDescriptor       _imageDescriptor_SlideoutExpand;
   private ImageDescriptor       _imageDescriptor_TourLocation;
   private ImageDescriptor       _imageDescriptor_TourMarker;
   private ImageDescriptor       _imageDescriptor_TourMarker_Cluster;
   private ImageDescriptor       _imageDescriptor_TourMarker_Group;
   private ImageDescriptor       _imageDescriptor_TourPause;
   private ImageDescriptor       _imageDescriptor_TourPhoto;
   private ImageDescriptor       _imageDescriptor_TourWayPoint;
   //
   private Image                 _imageMapLocation_BoundingBox;
   private Image                 _imageMapLocation_Common;
   private Image                 _imageMapLocation_Tour;
   private Image                 _imageTourMarker;
   private Image                 _imageTourMarker_Cluster;
   private Image                 _imageTourMarker_Group;
   private Image                 _imageTourPauses;
   private Image                 _imageTourWayPoint;

   private NatComboMT            _comboTourMarkerFilter;

   private class ActionExpandSlideout extends Action {

      public ActionExpandSlideout() {

         setToolTipText(UI.SPACE1);
         setImageDescriptor(_imageDescriptor_SlideoutExpand);
      }

      @Override
      public void run() {

         actionExpandCollapseSlideout();
      }
   }

   private class ActionMarkerType_SelectAll extends Action {

      public ActionMarkerType_SelectAll() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Slideout_MapPoints_Action_MarkerTypes_SelectAll_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Checked));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Checked_Disabled));
      }

      @Override
      public void runWithEvent(final Event event) {

         actionMarkerType_Select(true);
      }
   }

   private class ActionMarkerType_SelectInverse extends Action {

      public ActionMarkerType_SelectInverse() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Slideout_MapPoints_Action_MarkerTypes_InverteSelection_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Inverse));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Inverse_Disabled));
      }

      @Override
      public void runWithEvent(final Event event) {

         actionMarkerType_Select(null);
      }
   }

   private class ActionMarkerType_SelectNone extends Action {

      public ActionMarkerType_SelectNone() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Slideout_MapPoints_Action_MarkerTypes_DeselectAll_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Uncheck));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Uncheck_Disabled));
      }

      @Override
      public void runWithEvent(final Event event) {

         actionMarkerType_Select(false);
      }
   }

   private class ActionStatistic_CommonLocation extends Action {

      public ActionStatistic_CommonLocation() {

         super(Messages.Slideout_MapPoints_Action_CommonLocations_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_CommonLocation);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_CommonLocation(event);
      }
   }

   private class ActionStatistic_TourLocation extends Action {

      public ActionStatistic_TourLocation() {

         super(Messages.Slideout_MapPoints_Action_TourLocations_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_TourLocation);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_TourLocation(event);
      }
   }

   private class ActionStatistic_TourMarker extends Action {

      public ActionStatistic_TourMarker() {

         super(Messages.Slideout_MapPoints_Action_TourMarkers_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_TourMarker);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_TourMarker(event);
      }
   }

   private class ActionStatistic_TourPause extends Action {

      public ActionStatistic_TourPause() {

         super(Messages.Slideout_MapPoints_Action_TourPauses_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_TourPause);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_TourPause(event);
      }
   }

   private class ActionStatistic_TourPhotos extends Action {

      public ActionStatistic_TourPhotos() {

         super(Messages.Slideout_MapPoints_Action_TourPhotos_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_TourPhoto);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_TourPhotos(event);
      }
   }

   private class ActionStatistic_TourWayPoint extends Action {

      public ActionStatistic_TourWayPoint() {

         super(Messages.Slideout_MapPoints_Action_TourWayPoints_Tooltip, AS_CHECK_BOX);

         setImageDescriptor(_imageDescriptor_TourWayPoint);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionStatistic_TourWaypoint(event);
      }
   }

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {}

   /**
    * @param toolItem
    * @param map2State
    * @param slideoutState
    * @param map2View
    */
   public SlideoutMap2_MapPoints(final ToolItem toolItem,
                                 final IDialogSettings map2State,
                                 final IDialogSettings slideoutState,
                                 final Map2View map2View) {

      super(toolItem.getParent(), slideoutState, new int[] { 325, 400, 325, 400 });

      _toolItem = toolItem;

      _state_Map2 = map2State;
      _state_Slideout = slideoutState;
      _map2View = map2View;

      setTitleText(Messages.Slideout_MapPoints_Label_Title2);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      addPrefListener();
   }

   private void actionExpandCollapseSlideout() {

      // toggle expand state
      _isSlideoutExpanded = !_isSlideoutExpanded;

      updateUI_ExpandCollapse();

      onTTShellResize(null);
   }

   private void actionMarkerType_Select(final Boolean isSelectAll) {

      _comboTourMarkerFilter.selectAll(isSelectAll);
   }

   private void actionStatistic_CommonLocation(final Event event) {

      // toggle checkbox
      _chkIsShowCommonLocations.setSelection(!_chkIsShowCommonLocations.getSelection());

      selectTab(_tabCommonLocations, event);

      if (UI.isShiftKey(event)) {

         Util.showView(CommonLocationView.ID, true);
      }
   }

   private void actionStatistic_TourLocation(final Event event) {

      // toggle checkbox
      _chkIsShowTourLocations.setSelection(!_chkIsShowTourLocations.getSelection());

      selectTab(_tabTourLocations, event);

      if (UI.isShiftKey(event)) {

         Util.showView(TourLocationView.ID, true);
      }
   }

   private void actionStatistic_TourMarker(final Event event) {

      // toggle checkbox
      _chkIsShowTourMarkers.setSelection(!_chkIsShowTourMarkers.getSelection());

      selectTab(_tabTourMarkers, event);
   }

   private void actionStatistic_TourPause(final Event event) {

      // toggle checkbox
      _chkIsShowTourPauses.setSelection(!_chkIsShowTourPauses.getSelection());

      selectTab(_tabTourPauses, event);
   }

   private void actionStatistic_TourPhotos(final Event event) {

      final boolean isSelected = _actionStatistic_TourPhotos.isChecked();

      _map2View.setShowPhotos(isSelected);
   }

   private void actionStatistic_TourWaypoint(final Event event) {

      // toggle checkbox
      _chkIsShowTourWayPoints.setSelection(!_chkIsShowTourWayPoints.getSelection());

      selectTab(_tabTourWayPoints, event);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            fillUI_MarkerTypes();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      onDisposeResources();

      super.close();
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionExpandCollapseSlideout    = new ActionExpandSlideout();
      _actionRestoreDefaults           = new ActionResetToDefaults(this);

      _actionMarkerType_Prefs          = new ActionOpenPrefDialog(Messages.Slideout_MapPoints_Action_MarkerTypes_Setup_Tooltip,
                                                                  PrefPageTourMarkerTypes.ID);

      _actionMarkerType_SelectAll      = new ActionMarkerType_SelectAll();
      _actionMarkerType_SelectInverse  = new ActionMarkerType_SelectInverse();
      _actionMarkerType_SelectNone     = new ActionMarkerType_SelectNone();

      _actionStatistic_CommonLocation  = new ActionStatistic_CommonLocation();
      _actionStatistic_TourMarker      = new ActionStatistic_TourMarker();
      _actionStatistic_TourPause       = new ActionStatistic_TourPause();
      _actionStatistic_TourLocation    = new ActionStatistic_TourLocation();
      _actionStatistic_TourPhotos      = new ActionStatistic_TourPhotos();
      _actionStatistic_TourWayPoint    = new ActionStatistic_TourWayPoint();

// SET_FORMATTING_ON
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      createUI(parent);
      fillUI();

      restoreState();
      restoreTabFolder();

      enableControls();

      Map2PointManager.setMapLocationSlideout(this);
      Map2PointManager.updateStatistics();
   }

   @Override
   protected void createTitleBarControls(final Composite parent) {

      // this method is called 1st !!!

      initUI(parent);
      createActions();

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      _toolbarManagerExpandCollapseSlideout = new ToolBarManager(toolbar);
      _toolbarManagerExpandCollapseSlideout.add(_actionExpandCollapseSlideout);
      _toolbarManagerExpandCollapseSlideout.update(true);
   }

   /**
    * Create a list with all available map providers, sorted by preference settings
    */

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_shellContainer);
      GridLayoutFactory.fillDefaults()
            .spacing(0, 0)
            .applyTo(_shellContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         _tabContainer = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabContainer);
         GridLayoutFactory.fillDefaults().applyTo(_tabContainer);
//         _tabContainer.setBackground(UI.SYS_COLOR_RED);
         {
            _tabFolder = new CTabFolder(_tabContainer, SWT.TOP);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);
            GridLayoutFactory.fillDefaults().applyTo(_tabFolder);
//            _tabFolder.setBackground(UI.SYS_COLOR_CYAN);

            {
               _tabAll = new CTabItem(_tabFolder, SWT.NONE);
               _tabAll.setText(Messages.Slideout_MapPoints_Tab_All);
               _tabAll.setToolTipText(Messages.Slideout_MapPoints_Tab_All_Tooltip);
               _tabAll.setControl(createUI_100_Tab_All(_tabFolder));

               _tabOptions = new CTabItem(_tabFolder, SWT.NONE);
               _tabOptions.setText(Messages.Slideout_MapPoints_Tab_Options);
               _tabOptions.setToolTipText(Messages.Slideout_MapPoints_Tab_Options_Tooltip);
               _tabOptions.setControl(createUI_150_Tab_Common(_tabFolder));

               _tabTourMarkers = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourMarkers.setImage(_imageTourMarker);
               _tabTourMarkers.setToolTipText(Messages.Slideout_MapPoints_Tab_TourMarkers_Tooltip);
               _tabTourMarkers.setControl(createUI_200_Tab_TourMarkers(_tabFolder));

               _tabTourMarkerGroups = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourMarkerGroups.setImage(_imageTourMarker_Group);
               _tabTourMarkerGroups.setToolTipText(Messages.Slideout_MapPoints_Tab_TourMarkerGroups_Tooltip);
               _tabTourMarkerGroups.setControl(createUI_300_Tab_Groups(_tabFolder));

               _tabTourPauses = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourPauses.setImage(_imageTourPauses);
               _tabTourPauses.setToolTipText(Messages.Slideout_MapPoints_Tab_TourPauses_Tooltip);
               _tabTourPauses.setControl(createUI_500_Tab_TourPauses(_tabFolder));

               _tabTourLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourLocations.setImage(_imageMapLocation_Tour);
               _tabTourLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_TourLocations_Tooltip);
               _tabTourLocations.setControl(createUI_600_Tab_TourLocations(_tabFolder));

               _tabCommonLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabCommonLocations.setImage(_imageMapLocation_Common);
               _tabCommonLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_CommonLocations_Tooltip);
               _tabCommonLocations.setControl(createUI_700_Tab_CommonLocations(_tabFolder));

               _tabTourWayPoints = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourWayPoints.setImage(_imageTourWayPoint);
               _tabTourWayPoints.setToolTipText(Messages.Slideout_MapPoints_Tab_TourWayPoints_Tooltip);
               _tabTourWayPoints.setControl(createUI_800_Tab_TourWayPoints(_tabFolder));
            }
         }

         createUI_900_Statistics(_shellContainer);
      }

      return _shellContainer;
   }

   private Control createUI_100_Tab_All(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(tabContainer);
      {
         {
            /*
             * Show tour marker
             */
            _chkIsShowTourMarkers_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourMarkers_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourMarkers);
            _chkIsShowTourMarkers_All.setImage(_imageTourMarker);
            _chkIsShowTourMarkers_All.addSelectionListener(_mapPointSelectionListener_All);
         }
         {
            /*
             * Group duplicate tour markers
             */
            _chkIsGroupMarkers_All = new Button(tabContainer, SWT.CHECK);
            _chkIsGroupMarkers_All.setText(Messages.Slideout_MapPoints_Checkbox_GroupTourMarkers);
            _chkIsGroupMarkers_All.setToolTipText(Messages.Slideout_MapPoints_Checkbox_GroupTourMarkers_Tooltip);
            _chkIsGroupMarkers_All.setImage(_imageTourMarker_Group);
            _chkIsGroupMarkers_All.addSelectionListener(_mapPointSelectionListener_All);
            GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_chkIsGroupMarkers_All);
         }
         {
            /*
             * Cluster tour markers
             */
            _chkIsMarkerClustered_All = new Button(tabContainer, SWT.CHECK);
            _chkIsMarkerClustered_All.setText(Messages.Slideout_MapPoints_Checkbox_ClusterTourMarkers);
            _chkIsMarkerClustered_All.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ClusterTourMarkers_Tooltip);
            _chkIsMarkerClustered_All.setImage(_imageTourMarker_Cluster);
            _chkIsMarkerClustered_All.addSelectionListener(_mapPointSelectionListener_All);
            GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_chkIsMarkerClustered_All);

         }
         {
            /*
             * Show tour pauses
             */
            _chkIsShowTourPauses_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourPauses_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourPauses);
            _chkIsShowTourPauses_All.setImage(_imageTourPauses);
            _chkIsShowTourPauses_All.addSelectionListener(_mapPointSelectionListener_All);
         }
         {
            /*
             * Show tour locations
             */
            _chkIsShowTourLocations_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourLocations_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations);
            _chkIsShowTourLocations_All.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations_Tooltip);
            _chkIsShowTourLocations_All.setImage(_imageMapLocation_Tour);
            _chkIsShowTourLocations_All.addSelectionListener(_mapPointSelectionListener_All);
         }
         {
            /*
             * Show common locations
             */
            _chkIsShowCommonLocations_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowCommonLocations_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations);
            _chkIsShowCommonLocations_All.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations_Tooltip);
            _chkIsShowCommonLocations_All.setImage(_imageMapLocation_Common);
            _chkIsShowCommonLocations_All.addSelectionListener(_mapPointSelectionListener_All);
         }
         {
            /*
             * Show tour waypoints
             */
            _chkIsShowTourWayPoints_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourWayPoints_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourWayPoints);
            _chkIsShowTourWayPoints_All.setImage(_imageTourWayPoint);
            _chkIsShowTourWayPoints_All.addSelectionListener(_mapPointSelectionListener_All);
         }
         {
            /*
             * Show location bounding box
             */
            _chkIsShowBoundingBox_All = new Button(tabContainer, SWT.CHECK);
            _chkIsShowBoundingBox_All.setText(Messages.Slideout_MapPoints_Checkbox_ShowLocationBoundingBox);
            _chkIsShowBoundingBox_All.setImage(_imageMapLocation_BoundingBox);
            _chkIsShowBoundingBox_All.addSelectionListener(_mapPointSelectionListener_All);
         }
      }

      return tabContainer;
   }

   private Control createUI_150_Tab_Common(final Composite parent) {

      final GridDataFactory gdHCenter = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);
      final GridDataFactory gdSpan2 = GridDataFactory.fillDefaults().span(2, 1);

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(tabContainer);
      {
         {
            /*
             * Label background
             */

            // label
            _lblLabelBackground = new Label(tabContainer, SWT.NONE);
            _lblLabelBackground.setText(Messages.Slideout_MapPoints_Label_LabelBackground);
            gdHCenter.applyTo(_lblLabelBackground);

            // combo
            _comboLabelLayout = new Combo(tabContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboLabelLayout.setVisibleItemCount(20);
            _comboLabelLayout.addSelectionListener(_mapPointSelectionListener);
            _comboLabelLayout.addFocusListener(_keepOpenListener);
         }
         {
            /*
             * Label font name
             */

            // label
            _lblFontName = new Label(tabContainer, SWT.NONE);
            _lblFontName.setText(Messages.Slideout_MapPoints_Label_Font);
            gdHCenter.applyTo(_lblFontName);

            // font names
            _comboLabelFont = new Combo(tabContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboLabelFont.setVisibleItemCount(50);
            _comboLabelFont.addSelectionListener(_mapPointSelectionListener);
            _comboLabelFont.addFocusListener(_keepOpenListener);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .applyTo(_comboLabelFont);
         }
         {
            /*
             * Label/symbol/respect size
             */
            final String tooltip = Messages.Slideout_MapPoints_Label_FontSize_Tooltip;

            // label
            _lblLabelSize = new Label(tabContainer, SWT.NONE);
            _lblLabelSize.setText(Messages.Slideout_MapPoints_Label_FontSize);
            _lblLabelSize.setToolTipText(tooltip);
            gdHCenter.applyTo(_lblLabelSize);

            final Composite container = new Composite(tabContainer, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
            {
               // font size
               _spinnerLabelFontSize = new Spinner(container, SWT.BORDER);
               _spinnerLabelFontSize.setToolTipText(tooltip);
               _spinnerLabelFontSize.setMinimum(Map2ConfigManager.LABEL_FONT_SIZE_MIN);
               _spinnerLabelFontSize.setMaximum(Map2ConfigManager.LABEL_FONT_SIZE_MAX);
               _spinnerLabelFontSize.setIncrement(1);
               _spinnerLabelFontSize.setPageIncrement(10);
               _spinnerLabelFontSize.addSelectionListener(_mapPointSelectionListener);
               _spinnerLabelFontSize.addMouseWheelListener(_mapPointMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerLabelFontSize);

               // symbol size
               _spinnerLocationSymbolSize = new Spinner(container, SWT.BORDER);
               _spinnerLocationSymbolSize.setToolTipText(tooltip);
               _spinnerLocationSymbolSize.setMinimum(Map2ConfigManager.LOCATION_SYMBOL_SIZE_MIN);
               _spinnerLocationSymbolSize.setMaximum(Map2ConfigManager.LOCATION_SYMBOL_SIZE_MAX);
               _spinnerLocationSymbolSize.setIncrement(1);
               _spinnerLocationSymbolSize.setPageIncrement(10);
               _spinnerLocationSymbolSize.addSelectionListener(_mapPointSelectionListener);
               _spinnerLocationSymbolSize.addMouseWheelListener(_mapPointMouseWheelListener4);
               _spinnerGridData.applyTo(_spinnerLocationSymbolSize);

               // item distance
               _spinnerLabelRespectMargin = new Spinner(container, SWT.BORDER);
               _spinnerLabelRespectMargin.setToolTipText(tooltip);
               _spinnerLabelRespectMargin.setMinimum(Map2ConfigManager.LABEL_RESPECT_MARGIN_MIN);
               _spinnerLabelRespectMargin.setMaximum(Map2ConfigManager.LABEL_RESPECT_MARGIN_MAX);
               _spinnerLabelRespectMargin.setIncrement(1);
               _spinnerLabelRespectMargin.setPageIncrement(10);
               _spinnerLabelRespectMargin.addSelectionListener(_mapPointSelectionListener);
               _spinnerLabelRespectMargin.addMouseWheelListener(_mapPointMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerLabelRespectMargin);
            }
         }
         {
            /*
             * Visible labels
             */
            // label
            _lblVisibleLabels = new Label(tabContainer, SWT.NONE);
            _lblVisibleLabels.setText(Messages.Slideout_MapPoints_Label_VisibleLabels);
            _lblVisibleLabels.setToolTipText(Messages.Slideout_MapPoints_Label_VisibleLabels_Tooltip);
            gdHCenter.applyTo(_lblVisibleLabels);

            final Composite container = new Composite(tabContainer, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               // number of visible labels
               _spinnerLabelDistributorMaxLabels = new Spinner(container, SWT.BORDER);
               _spinnerLabelDistributorMaxLabels.setMinimum(Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_MIN);
               _spinnerLabelDistributorMaxLabels.setMaximum(Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_MAX);
               _spinnerLabelDistributorMaxLabels.setIncrement(10);
               _spinnerLabelDistributorMaxLabels.setPageIncrement(100);
               _spinnerLabelDistributorMaxLabels.addSelectionListener(_mapPointSelectionListener);
               _spinnerLabelDistributorMaxLabels.addMouseWheelListener(_mapPointMouseWheelListener10);
               _spinnerLabelDistributorMaxLabels.setToolTipText(Messages.Slideout_MapPoints_Spinner_LabelDistributor_MaxLabels_Tooltip);
               _spinnerGridData.applyTo(_spinnerLabelDistributorMaxLabels);

               // label distributor radius
               _spinnerLabelDistributorRadius = new Spinner(container, SWT.BORDER);
               _spinnerLabelDistributorRadius.setMinimum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MIN);
               _spinnerLabelDistributorRadius.setMaximum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MAX);
               _spinnerLabelDistributorRadius.setIncrement(10);
               _spinnerLabelDistributorRadius.setPageIncrement(100);
               _spinnerLabelDistributorRadius.addSelectionListener(_mapPointSelectionListener);
               _spinnerLabelDistributorRadius.addMouseWheelListener(_mapPointMouseWheelListener10);
               _spinnerLabelDistributorRadius.setToolTipText(Messages.Slideout_MapPoints_Spinner_LabelDistributor_Radius_Tooltip);
               _spinnerGridData.applyTo(_spinnerLabelDistributorRadius);
            }
         }
         {
            /*
             * Truncate label
             */
            _chkIsTruncateLabel = new Button(tabContainer, SWT.CHECK);
            _chkIsTruncateLabel.setText(Messages.Slideout_MapPoints_Checkbox_TruncateLabel);
            _chkIsTruncateLabel.addSelectionListener(_mapPointSelectionListener);
            gdHCenter.applyTo(_chkIsTruncateLabel);

            // spinner
            _spinnerLabelTruncateLength = new Spinner(tabContainer, SWT.BORDER);
            _spinnerLabelTruncateLength.setMinimum(Map2ConfigManager.LABEL_TRUNCATE_LENGTH_MIN);
            _spinnerLabelTruncateLength.setMaximum(Map2ConfigManager.LABEL_TRUNCATE_LENGTH_MAX);
            _spinnerLabelTruncateLength.setIncrement(1);
            _spinnerLabelTruncateLength.setPageIncrement(10);
            _spinnerLabelTruncateLength.addSelectionListener(_mapPointSelectionListener);
            _spinnerLabelTruncateLength.addMouseWheelListener(_mapPointMouseWheelListener);
         }
         {
            /*
             * Antialias label
             */
            _chkIsLabelAntialiased = new Button(tabContainer, SWT.CHECK);
            _chkIsLabelAntialiased.setText(Messages.Slideout_MapPoints_Checkbox_AntialiasLabel);
            _chkIsLabelAntialiased.addSelectionListener(_mapPointSelectionListener);
            gdSpan2.applyTo(_chkIsLabelAntialiased);
         }
         {
            /*
             * Dim map
             */
            final Composite dimContainer = new Composite(tabContainer, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(dimContainer);
            {
               // checkbox
               _chkIsDimMap = new Button(dimContainer, SWT.CHECK);
               _chkIsDimMap.setText(Messages.Slideout_Map_Options_Checkbox_DimMap);
               _chkIsDimMap.addSelectionListener(_mapPointSelectionListener);

               // spinner
               _spinnerMapDimValue = new Spinner(dimContainer, SWT.BORDER);
               _spinnerMapDimValue.setToolTipText(Messages.Slideout_Map_Options_Spinner_DimValue_Tooltip);
               _spinnerMapDimValue.setMinimum(0);
               _spinnerMapDimValue.setMaximum(Map2View.MAX_DIM_STEPS);
               _spinnerMapDimValue.setIncrement(1);
               _spinnerMapDimValue.setPageIncrement(4);
               _spinnerMapDimValue.addSelectionListener(_mapPointSelectionListener);
               _spinnerMapDimValue.addMouseWheelListener(_mapPointMouseWheelListener);
               GridDataFactory.fillDefaults().indent(10, 0).applyTo(_spinnerMapDimValue);
            }

            // dimming color
            _colorMapDimColor = new ColorSelectorExtended(tabContainer);
            _colorMapDimColor.setToolTipText(Messages.Slideout_Map_Options_Color_DimColor_Tooltip);
            _colorMapDimColor.addListener(_mapPointPropertyChangeListener);
            _colorMapDimColor.addOpenListener(this);
         }
      }

      return tabContainer;
   }

   private Control createUI_200_Tab_TourMarkers(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(tabContainer);
//      tabContainer.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Show tour marker
             */
            _chkIsShowTourMarkers = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourMarkers.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourMarkers);
            _chkIsShowTourMarkers.addSelectionListener(_mapPointSelectionListener);
         }

         final Composite container = new Composite(tabContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_220_TourMarker_Label(container);
            createUI_230_TourMarker_Filter(container);
            createUI_250_TourMarker_Cluster(container);
         }
      }

      return tabContainer;
   }

   private void createUI_220_TourMarker_Label(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      {
         /*
          * Marker label
          */
         {
            // label
            _lblTourMarkerLabel_Color = new Label(parent, SWT.NONE);
            _lblTourMarkerLabel_Color.setText(Messages.Slideout_MapPoints_Label_MarkerColor);
            _lblTourMarkerLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);
            gd.applyTo(_lblTourMarkerLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorTourMarkerLabel_Outline = new ColorSelectorExtended(container);
            _colorTourMarkerLabel_Outline.addListener(_mapPointPropertyChangeListener);
            _colorTourMarkerLabel_Outline.addOpenListener(this);
            _colorTourMarkerLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // background color
            _colorTourMarkerLabel_Fill = new ColorSelectorExtended(container);
            _colorTourMarkerLabel_Fill.addListener(_mapPointPropertyChangeListener);
            _colorTourMarkerLabel_Fill.addOpenListener(this);
            _colorTourMarkerLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // button: swap color
            _btnSwapTourMarkerLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapTourMarkerLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapTourMarkerLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapTourMarkerLabel_Color.addSelectionListener(
                  SelectionListener.widgetSelectedAdapter(selectionEvent -> onSwapMarkerColor()));
         }
      }
      {
         /*
          * Marker time
          */

         // label
         _lblTourMarkerTime = new Label(parent, SWT.NONE);
         _lblTourMarkerTime.setText(Messages.Slideout_MapPoints_Label_MarkerTime);
         _lblTourMarkerTime.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerTime_Tooltip);
         gd.applyTo(_lblTourMarkerTime);

         // combo
         _comboTourMarkerTime = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboTourMarkerTime.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerTime_Tooltip);
         _comboTourMarkerTime.setVisibleItemCount(20);
         _comboTourMarkerTime.addSelectionListener(_mapPointSelectionListener);
         _comboTourMarkerTime.addFocusListener(_keepOpenListener);
      }
   }

   private void createUI_230_TourMarker_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            _chkIsFilterTourMarkers = new Button(container, SWT.CHECK);
            _chkIsFilterTourMarkers.setText(Messages.Slideout_MapPoints_Checkbox_MarkerTypes_Filter);
            _chkIsFilterTourMarkers.addSelectionListener(_mapPointSelectionListener);
         }
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionMarkerType_SelectAll);
            tbm.add(_actionMarkerType_SelectNone);
            tbm.add(_actionMarkerType_SelectInverse);
            tbm.add(_actionMarkerType_Prefs);

            tbm.update(true);
         }
      }
      {
         /**
          * NatCombo was the best which I could find as a checkbox dropdown BUT it needed special
          * hacking, that it worked how I expected it :-(((
          */
         final Style comboStyle = new Style();

         _comboTourMarkerFilter = new NatComboMT(
               parent,
               comboStyle,
               33, //                     maxVisibleItems
               SWT.CHECK
                     | SWT.MULTI
                     | SWT.BORDER
                     | SWT.READ_ONLY, //  style,
               false, //                  showDropdownFilter,
               true //                    linkItemAndCheckbox
         );

         _comboTourMarkerFilter.addSelectionListener(_mapPointSelectionListener);
         _comboTourMarkerFilter.addSelectionAllListener(_mapPointSelectionListener);

         _comboTourMarkerFilter.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
               _comboTourMarkerFilter.hideDropdownControl();
            }
         });

         // hide brackets
         _comboTourMarkerFilter.setMultiselectTextBracket(UI.EMPTY_STRING, UI.EMPTY_STRING);

         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .grab(true, false)
               .span(2, 1)
               .indent(UI.FORM_FIRST_COLUMN_INDENT + 0, 0)
               .applyTo(_comboTourMarkerFilter);
      }
   }

   private void createUI_250_TourMarker_Cluster(final Composite parent) {

      final int firstColumnIndent = UI.FORM_FIRST_COLUMN_INDENT;

      {
         /*
          * Cluster
          */

         // checkbox: Is clustering
         _chkIsMarkerClustered = new Button(parent, SWT.CHECK);
         _chkIsMarkerClustered.setText(Messages.Slideout_MapPoints_Checkbox_ClusterTourMarkers);
         _chkIsMarkerClustered.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ClusterTourMarkers_Tooltip);
         _chkIsMarkerClustered.addSelectionListener(_mapPointSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, 10)
               .applyTo(_chkIsMarkerClustered);
      }
      {
         /*
          * Cluster size
          */
         // label
         _lblClusterGrid_Size = new Label(parent, SWT.NONE);
         _lblClusterGrid_Size.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterGridSize);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(firstColumnIndent, 0)
               .applyTo(_lblClusterGrid_Size);

         // spinner
         _spinnerClusterGrid_Size = new Spinner(parent, SWT.BORDER);
         _spinnerClusterGrid_Size.setMinimum(0);
         _spinnerClusterGrid_Size.setMaximum(Map2ConfigManager.CLUSTER_GRID_SIZE_MAX);
         _spinnerClusterGrid_Size.setIncrement(1);
         _spinnerClusterGrid_Size.setPageIncrement(10);
         _spinnerClusterGrid_Size.addSelectionListener(_mapPointSelectionListener);
         _spinnerClusterGrid_Size.addMouseWheelListener(_mapPointMouseWheelListener10);
      }
      {
         /*
          * Cluster symbol size
          */
         {
            // label
            _lblClusterSymbol_Size = new Label(parent, SWT.NONE);
            _lblClusterSymbol_Size.setText(Messages.Slideout_MapPoints_Label_ClusterSymbolSize);
            _lblClusterSymbol_Size.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(firstColumnIndent, 0)
                  .applyTo(_lblClusterSymbol_Size);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               // spinner: symbol size
               _spinnerClusterSymbol_Size = new Spinner(container, SWT.BORDER);
               _spinnerClusterSymbol_Size.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
               _spinnerClusterSymbol_Size.setMinimum(1);
               _spinnerClusterSymbol_Size.setMaximum(Map2ConfigManager.CLUSTER_SYMBOL_SIZE_MAX);
               _spinnerClusterSymbol_Size.setIncrement(1);
               _spinnerClusterSymbol_Size.setPageIncrement(10);
               _spinnerClusterSymbol_Size.addSelectionListener(_mapPointSelectionListener);
               _spinnerClusterSymbol_Size.addMouseWheelListener(_mapPointMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterSymbol_Size);

               // outline width
               _spinnerClusterOutline_Width = new Spinner(container, SWT.BORDER);
               _spinnerClusterOutline_Width.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
               _spinnerClusterOutline_Width.setMinimum(Map2ConfigManager.CLUSTER_OUTLINE_WIDTH_MIN);
               _spinnerClusterOutline_Width.setMaximum(Map2ConfigManager.CLUSTER_OUTLINE_WIDTH_MAX);
               _spinnerClusterOutline_Width.setIncrement(1);
               _spinnerClusterOutline_Width.setPageIncrement(10);
               _spinnerClusterOutline_Width.addSelectionListener(_mapPointSelectionListener);
               _spinnerClusterOutline_Width.addMouseWheelListener(_mapPointMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterOutline_Width);
            }
         }
      }
      {
         /*
          * Symbol color
          */
         {
            // label
            _lblClusterSymbol = new Label(parent, SWT.NONE);
            _lblClusterSymbol.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor);
            _lblClusterSymbol.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(firstColumnIndent, 0)
                  .applyTo(_lblClusterSymbol);
         }

         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // foreground color
            _colorClusterSymbol_Outline = new ColorSelectorExtended(container);
            _colorClusterSymbol_Outline.addListener(_mapPointPropertyChangeListener);
            _colorClusterSymbol_Outline.addOpenListener(this);
            _colorClusterSymbol_Outline.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);

            // foreground color
            _colorClusterSymbol_Fill = new ColorSelectorExtended(container);
            _colorClusterSymbol_Fill.addListener(_mapPointPropertyChangeListener);
            _colorClusterSymbol_Fill.addOpenListener(this);
            _colorClusterSymbol_Fill.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);

            // button: swap color
            _btnSwapClusterSymbolColor = new Button(container, SWT.PUSH);
            _btnSwapClusterSymbolColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapClusterSymbolColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapClusterSymbolColor.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSwapClusterColor()));
         }
      }
      {
         /*
          * Fill symbol
          */
         {
            // checkbox: fill cluster symbol
            _chkIsFillClusterSymbol = new Button(parent, SWT.CHECK);
            _chkIsFillClusterSymbol.setText(Messages.Slideout_MapPoints_Checkbox_FillClusterSymbol);
            _chkIsFillClusterSymbol.addSelectionListener(_mapPointSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(firstColumnIndent, 0)
                  .applyTo(_chkIsFillClusterSymbol);
         }
      }
   }

   private Control createUI_300_Tab_Groups(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(tabContainer);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);

      {
         {
            /*
             * Group duplicate markers
             */
            _chkIsGroupMarkers = new Button(tabContainer, SWT.CHECK | SWT.WRAP);
            _chkIsGroupMarkers.setText(Messages.Slideout_MapPoints_Checkbox_GroupTourMarkers_Extended);
            _chkIsGroupMarkers.addSelectionListener(_mapPointSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(_chkIsGroupMarkers);

         }
         {
            /*
             * Label group grid size
             */

            // label
            _lblLabelGroupGridSize = new Label(tabContainer, SWT.NONE);
            _lblLabelGroupGridSize.setText(Messages.Slideout_MapPoints_Label_GroupGridSize);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_lblLabelGroupGridSize);

            // spinner
            _spinnerLabelGroupGridSize = new Spinner(tabContainer, SWT.BORDER);
            _spinnerLabelGroupGridSize.setMinimum(Map2ConfigManager.LABEL_GROUP_GRID_SIZE_MIN);
            _spinnerLabelGroupGridSize.setMaximum(Map2ConfigManager.LABEL_GROUP_GRID_SIZE_MAX);
            _spinnerLabelGroupGridSize.setIncrement(1);
            _spinnerLabelGroupGridSize.setPageIncrement(10);
            _spinnerLabelGroupGridSize.addSelectionListener(_mapPointSelectionListener);
            _spinnerLabelGroupGridSize.addMouseWheelListener(_mapPointMouseWheelListener10);
         }
         {
            // label
            _lblGroupDuplicatedMarkers = new Label(tabContainer, SWT.NONE);
            _lblGroupDuplicatedMarkers.setText(Messages.Slideout_MapPoints_Label_GroupDuplicatedMarkers);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
                  .applyTo(_lblGroupDuplicatedMarkers);

            // group list
            _txtGroupDuplicatedMarkers = new Text(tabContainer, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
            _txtGroupDuplicatedMarkers.addFocusListener(FocusListener.focusLostAdapter(focusEvent -> onModifyConfig(focusEvent.widget)));
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
                  .applyTo(_txtGroupDuplicatedMarkers);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_txtGroupDuplicatedMarkers);

         }
      }

      return tabContainer;
   }

   private Control createUI_500_Tab_TourPauses(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(container);
      {
         createUI_510_TourPauses(container);

         _tourPausesUI.createContent(container);
      }

      return container;
   }

   private void createUI_510_TourPauses(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         {
            /*
             * Show tour pauses
             */
            _chkIsShowTourPauses = new Button(container, SWT.CHECK);
            _chkIsShowTourPauses.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourPauses);
            _chkIsShowTourPauses.addSelectionListener(_mapPointSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourPauses);

         }
         {
            /*
             * Pause label
             */
            {
               // label
               _lblTourPauseLabel_Color = new Label(container, SWT.NONE);
               _lblTourPauseLabel_Color.setText(Messages.Slideout_MapPoints_Label_TourPauseColor);
               _lblTourPauseLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_TourPauseColor_Tooltip);
               labelGridData.applyTo(_lblTourPauseLabel_Color);
            }
            {
               final Composite labelContainer = new Composite(container, SWT.NONE);
               GridLayoutFactory.fillDefaults().numColumns(3).applyTo(labelContainer);

               // outline/text color
               _colorTourPauseLabel_Outline = new ColorSelectorExtended(labelContainer);
               _colorTourPauseLabel_Outline.addListener(_mapPointPropertyChangeListener);
               _colorTourPauseLabel_Outline.addOpenListener(this);
               _colorTourPauseLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_TourPauseColor_Tooltip);

               // background color
               _colorTourPauseLabel_Fill = new ColorSelectorExtended(labelContainer);
               _colorTourPauseLabel_Fill.addListener(_mapPointPropertyChangeListener);
               _colorTourPauseLabel_Fill.addOpenListener(this);
               _colorTourPauseLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_TourPauseColor_Tooltip);

               // button: swap color
               _btnSwapTourPauseLabel_Color = new Button(labelContainer, SWT.PUSH);
               _btnSwapTourPauseLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
               _btnSwapTourPauseLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
               _btnSwapTourPauseLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> onSwapTourPauseColor()));
            }
         }
      }
   }

   private Control createUI_600_Tab_TourLocations(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(container);
      {
         createUI_610_TourLocation(container);
      }

      return container;
   }

   private void createUI_610_TourLocation(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      {
         /*
          * Show tour locations
          */
         _chkIsShowTourLocations = new Button(parent, SWT.CHECK);
         _chkIsShowTourLocations.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations);
         _chkIsShowTourLocations.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations_Tooltip);
         _chkIsShowTourLocations.addSelectionListener(_mapPointSelectionListener);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourLocations);
      }
      {
         /*
          * Tour location label
          */
         {
            // label
            _lblTourLocationLabel_Color = new Label(parent, SWT.NONE);
            _lblTourLocationLabel_Color.setText(Messages.Slideout_MapPoints_Label_TourLocationColor);
            _lblTourLocationLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
            labelGridData.applyTo(_lblTourLocationLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorTourLocationLabel_Outline = new ColorSelectorExtended(container);
            _colorTourLocationLabel_Outline.addListener(_mapPointPropertyChangeListener);
            _colorTourLocationLabel_Outline.addOpenListener(this);
            _colorTourLocationLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // background color
            _colorTourLocationLabel_Fill = new ColorSelectorExtended(container);
            _colorTourLocationLabel_Fill.addListener(_mapPointPropertyChangeListener);
            _colorTourLocationLabel_Fill.addOpenListener(this);
            _colorTourLocationLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // button: swap color
            _btnSwapTourLocationLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapTourLocationLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapTourLocationLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapTourLocationLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapTourLocationColor()));
         }
      }
      {
         /*
          * Start location label
          */
         {
            // label
            _lblTourLocation_StartLabel_Color = new Label(parent, SWT.NONE);
            _lblTourLocation_StartLabel_Color.setText(Messages.Slideout_MapPoints_Label_TourLocation_StartColor);
            _lblTourLocation_StartLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
            labelGridData.applyTo(_lblTourLocation_StartLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorTourLocation_StartLabel_Outline = new ColorSelectorExtended(container);
            _colorTourLocation_StartLabel_Outline.addListener(_mapPointPropertyChangeListener);
            _colorTourLocation_StartLabel_Outline.addOpenListener(this);
            _colorTourLocation_StartLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // background color
            _colorTourLocation_StartLabel_Fill = new ColorSelectorExtended(container);
            _colorTourLocation_StartLabel_Fill.addListener(_mapPointPropertyChangeListener);
            _colorTourLocation_StartLabel_Fill.addOpenListener(this);
            _colorTourLocation_StartLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // button: swap color
            _btnSwapTourLocation_StartLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapTourLocation_StartLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapTourLocation_StartLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapTourLocation_StartLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapTourLocation_StartColor()));
         }
      }
      {
         /*
          * End location label
          */
         {
            // label
            _lblTourLocation_EndLabel_Color = new Label(parent, SWT.NONE);
            _lblTourLocation_EndLabel_Color.setText(Messages.Slideout_MapPoints_Label_TourLocation_EndColor);
            _lblTourLocation_EndLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
            labelGridData.applyTo(_lblTourLocation_EndLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorTourLocation_EndLabel_Outline = new ColorSelectorExtended(container);
            _colorTourLocation_EndLabel_Outline.addListener(_mapPointPropertyChangeListener);
            _colorTourLocation_EndLabel_Outline.addOpenListener(this);
            _colorTourLocation_EndLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // background color
            _colorTourLocation_EndLabel_Fill = new ColorSelectorExtended(container);
            _colorTourLocation_EndLabel_Fill.addListener(_mapPointPropertyChangeListener);
            _colorTourLocation_EndLabel_Fill.addOpenListener(this);
            _colorTourLocation_EndLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // button: swap color
            _btnSwapTourLocation_EndLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapTourLocation_EndLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapTourLocation_EndLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapTourLocation_EndLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapTourLocation_EndColor()));
         }
      }
      {
         /*
          * Show location bounding box
          */
         _chkIsShowBoundingBox_Tour = new Button(parent, SWT.CHECK);
         _chkIsShowBoundingBox_Tour.setText(Messages.Slideout_MapPoints_Checkbox_ShowLocationBoundingBox);
         _chkIsShowBoundingBox_Tour.addSelectionListener(_mapPointSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_chkIsShowBoundingBox_Tour);

      }
   }

   private Control createUI_700_Tab_CommonLocations(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(container);
      {
         createUI_710_CommonLocation(container);
      }

      return container;
   }

   private void createUI_710_CommonLocation(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show common locations
             */
            _chkIsShowCommonLocations = new Button(container, SWT.CHECK);
            _chkIsShowCommonLocations.setText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations);
            _chkIsShowCommonLocations.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations_Tooltip);
            _chkIsShowCommonLocations.addSelectionListener(_mapPointSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowCommonLocations);
         }
         {
            /*
             * Common location label
             */
            {
               // label
               _lblCommonLocationLabel_Color = new Label(container, SWT.NONE);
               _lblCommonLocationLabel_Color.setText(Messages.Slideout_MapPoints_Label_LocationColor);
               _lblCommonLocationLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
               labelGridData.applyTo(_lblCommonLocationLabel_Color);
            }
            {
               final Composite labelContainer = new Composite(container, SWT.NONE);
               GridLayoutFactory.fillDefaults().numColumns(3).applyTo(labelContainer);

               // outline/text color
               _colorCommonLocationLabel_Outline = new ColorSelectorExtended(labelContainer);
               _colorCommonLocationLabel_Outline.addListener(_mapPointPropertyChangeListener);
               _colorCommonLocationLabel_Outline.addOpenListener(this);
               _colorCommonLocationLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

               // background color
               _colorCommonLocationLabel_Fill = new ColorSelectorExtended(labelContainer);
               _colorCommonLocationLabel_Fill.addListener(_mapPointPropertyChangeListener);
               _colorCommonLocationLabel_Fill.addOpenListener(this);
               _colorCommonLocationLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

               // button: swap color
               _btnSwapCommonLocationLabel_Color = new Button(labelContainer, SWT.PUSH);
               _btnSwapCommonLocationLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
               _btnSwapCommonLocationLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
               _btnSwapCommonLocationLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> onSwapCommonLocationColor()));
            }
         }
         {
            /*
             * Show location bounding box
             */
            _chkIsShowBoundingBox_Common = new Button(parent, SWT.CHECK);
            _chkIsShowBoundingBox_Common.setText(Messages.Slideout_MapPoints_Checkbox_ShowLocationBoundingBox);
            _chkIsShowBoundingBox_Common.addSelectionListener(_mapPointSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
                  .applyTo(_chkIsShowBoundingBox_Common);
         }
      }
   }

   private Control createUI_800_Tab_TourWayPoints(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(container);
      {
         createUI_810_TourWayPoints(container);
      }

      return container;
   }

   private void createUI_810_TourWayPoints(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      {
         /*
          * Show tour waypoints
          */
         _chkIsShowTourWayPoints = new Button(parent, SWT.CHECK);
         _chkIsShowTourWayPoints.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourWayPoints);
         _chkIsShowTourWayPoints.addSelectionListener(_mapPointSelectionListener);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourWayPoints);
      }
      {
         /*
          * Waypoint label
          */
         {
            // label
            _lblTourWayPointLabel_Color = new Label(parent, SWT.NONE);
            _lblTourWayPointLabel_Color.setText(Messages.Slideout_MapPoints_Label_TourWayPointColor);
            _lblTourWayPointLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_TourWayPointColor_Tooltip);
            labelGridData.applyTo(_lblTourWayPointLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorTourWayPointLabel_Outline = new ColorSelectorExtended(container);
            _colorTourWayPointLabel_Outline.addListener(_mapPointPropertyChangeListener);
            _colorTourWayPointLabel_Outline.addOpenListener(this);
            _colorTourWayPointLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_TourWayPointColor_Tooltip);

            // background color
            _colorTourWayPointLabel_Fill = new ColorSelectorExtended(container);
            _colorTourWayPointLabel_Fill.addListener(_mapPointPropertyChangeListener);
            _colorTourWayPointLabel_Fill.addOpenListener(this);
            _colorTourWayPointLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_TourWayPointColor_Tooltip);

            // button: swap color
            _btnSwapTourWayPointLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapTourWayPointLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapTourWayPointLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapTourWayPointLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapTourWayPointColor()));
         }
      }
   }

   private void createUI_900_Statistics(final Composite shellContainer) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END);
      final int horizontalSpacing = 0;

      final String tooltipCommonLocations = Messages.Slideout_MapPoints_Label_Stats_CommonLocations;
      final String tooltipTourLocations = Messages.Slideout_MapPoints_Label_Stats_TourLocations;
      final String tooltipMarkers = Messages.Slideout_MapPoints_Label_Stats_TourMarkers;
      final String tooltipPauses = Messages.Slideout_MapPoints_Label_Stats_TourPauses;
      final String tooltipPhotos = Messages.Slideout_MapPoints_Label_Stats_TourPhotos;

      _statisticsContainer = new Composite(shellContainer, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(_statisticsContainer);
      GridLayoutFactory.fillDefaults().numColumns(19).applyTo(_statisticsContainer);
//      _statisticsContainer.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Tour marker
             */
            UI.createToolbarAction(_statisticsContainer, _actionStatistic_TourMarker);

            _lblStats_TourMarkers_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourMarkers_Visible.setToolTipText(tooltipMarkers);
            gd.applyTo(_lblStats_TourMarkers_Visible);

            _lblStats_TourMarkers_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourMarkers_All.setToolTipText(tooltipMarkers);
            gd.applyTo(_lblStats_TourMarkers_All);
         }
         {
            /*
             * Tour pauses
             */
            final ToolBar actionToolbar = UI.createToolbarAction(_statisticsContainer, _actionStatistic_TourPause);
            GridDataFactory.fillDefaults().indent(horizontalSpacing, 0).applyTo(actionToolbar);

            _lblStats_TourPauses_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourPauses_Visible.setToolTipText(tooltipPauses);
            gd.applyTo(_lblStats_TourPauses_Visible);

            _lblStats_TourPauses_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourPauses_All.setToolTipText(tooltipPauses);
            gd.applyTo(_lblStats_TourPauses_All);
         }
         {
            /*
             * Tour locations
             */
            final ToolBar actionToolbar = UI.createToolbarAction(_statisticsContainer, _actionStatistic_TourLocation);
            GridDataFactory.fillDefaults().indent(horizontalSpacing, 0).applyTo(actionToolbar);

            _lblStats_TourLocations_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourLocations_Visible.setToolTipText(tooltipTourLocations);
            gd.applyTo(_lblStats_TourLocations_Visible);

            _lblStats_TourLocations_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourLocations_All.setToolTipText(tooltipTourLocations);
            gd.applyTo(_lblStats_TourLocations_All);
         }
         {
            /*
             * Common locations
             */
            final ToolBar actionToolbar = UI.createToolbarAction(_statisticsContainer, _actionStatistic_CommonLocation);
            GridDataFactory.fillDefaults().indent(horizontalSpacing, 0).applyTo(actionToolbar);

            _lblStats_CommonLocations_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_CommonLocations_Visible.setToolTipText(tooltipCommonLocations);
            gd.applyTo(_lblStats_CommonLocations_Visible);

            _lblStats_CommonLocations_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_CommonLocations_All.setToolTipText(tooltipCommonLocations);
            gd.applyTo(_lblStats_CommonLocations_All);
         }
         {
            /*
             * Waypoints
             */
            UI.createToolbarAction(_statisticsContainer, _actionStatistic_TourWayPoint);

            _lblStats_TourWayPoints_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourWayPoints_Visible.setToolTipText(tooltipMarkers);
            gd.applyTo(_lblStats_TourWayPoints_Visible);

            _lblStats_TourWayPoints_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_TourWayPoints_All.setToolTipText(tooltipMarkers);
            gd.applyTo(_lblStats_TourWayPoints_All);
         }
         {
            /*
             * Photos
             */
            UI.createToolbarAction(_statisticsContainer, _actionStatistic_TourPhotos);

            _lblStats_Photos_Visible = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_Photos_Visible.setToolTipText(tooltipPhotos);
            gd.applyTo(_lblStats_Photos_Visible);

            _lblStats_Photos_All = new Label(_statisticsContainer, SWT.TRAIL);
            _lblStats_Photos_All.setToolTipText(tooltipPhotos);
            gd.applyTo(_lblStats_Photos_All);
         }
         {
            /*
             * Reset values
             */
            final ToolBar actionToolbar = UI.createToolbarAction(_statisticsContainer, _actionRestoreDefaults);
            GridDataFactory.fillDefaults()
                  .indent(10, 0)

                  // this is sometimes not working !!!
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)

                  .applyTo(actionToolbar);
         }
      }
   }

   void enableControls() {

      if (_shellContainer == null || _shellContainer.isDisposed()) {
         return;
      }

// SET_FORMATTING_OFF

      final boolean isFilterTourMarkers      = _chkIsFilterTourMarkers        .getSelection();
      final boolean isGroupDuplicatedMarkers = _chkIsGroupMarkers             .getSelection();
      final boolean isMarkerClustered        = _chkIsMarkerClustered          .getSelection();
      final boolean isShowTourMarker         = _chkIsShowTourMarkers          .getSelection();
      final boolean isShowTourPauses         = _chkIsShowTourPauses           .getSelection();
      final boolean isShowTourLocations      = _chkIsShowTourLocations        .getSelection();
      final boolean isShowCommonLocations    = _chkIsShowCommonLocations      .getSelection();
      final boolean isShowTourWayPoints      = _chkIsShowTourWayPoints        .getSelection();

      final boolean isTruncateLabel          = _chkIsTruncateLabel            .getSelection();

      final boolean isDimMap                 = _chkIsDimMap                   .getSelection();

      final boolean isShowTourPhotos         = Map2PainterConfig.isShowPhotos;

      final boolean isFilterMarkerType       = isShowTourMarker && isFilterTourMarkers;
      final boolean isGroupMarkers           = isShowTourMarker && isGroupDuplicatedMarkers;
      final boolean isShowClusteredMarker    = isShowTourMarker && isMarkerClustered;
      final boolean isShowLabels             = isShowTourMarker || isShowTourLocations || isShowTourPauses || isShowCommonLocations;

      // statistics
      _lblStats_CommonLocations_All          .setEnabled(isShowCommonLocations);
      _lblStats_CommonLocations_Visible      .setEnabled(isShowCommonLocations);
      _lblStats_TourLocations_All            .setEnabled(isShowTourLocations);
      _lblStats_TourLocations_Visible        .setEnabled(isShowTourLocations);
      _lblStats_TourMarkers_All              .setEnabled(isShowTourMarker);
      _lblStats_TourMarkers_Visible          .setEnabled(isShowTourMarker);
      _lblStats_TourPauses_All               .setEnabled(isShowTourPauses);
      _lblStats_TourPauses_Visible           .setEnabled(isShowTourPauses);
      _lblStats_Photos_All                   .setEnabled(isShowTourPhotos);
      _lblStats_Photos_Visible               .setEnabled(isShowTourPhotos);
      _lblStats_TourWayPoints_All            .setEnabled(isShowTourWayPoints);
      _lblStats_TourWayPoints_Visible        .setEnabled(isShowTourWayPoints);

      // common
      _chkIsLabelAntialiased                 .setEnabled(isShowLabels);
      _chkIsTruncateLabel                    .setEnabled(isShowLabels);
      _comboLabelFont                        .setEnabled(isShowLabels);
      _comboLabelLayout                      .setEnabled(isShowLabels);
      _comboTourMarkerTime                   .setEnabled(isShowLabels);
      _lblFontName                           .setEnabled(isShowLabels);
      _lblLabelBackground                    .setEnabled(isShowLabels);
      _lblLabelSize                          .setEnabled(isShowLabels || isShowTourPhotos);
      _lblTourMarkerTime                     .setEnabled(isShowLabels);
      _lblVisibleLabels                      .setEnabled(isShowLabels || isShowTourPhotos);
      _spinnerLabelFontSize                  .setEnabled(isShowLabels || isShowTourPhotos);
      _spinnerLabelTruncateLength            .setEnabled(isShowLabels && isTruncateLabel);
      _spinnerLocationSymbolSize             .setEnabled(isShowLabels || isShowTourPhotos);
      _spinnerLabelRespectMargin             .setEnabled(isShowLabels || isShowTourPhotos);
      _spinnerLabelDistributorMaxLabels      .setEnabled(isShowLabels || isShowTourPhotos);
      _spinnerLabelDistributorRadius         .setEnabled(isShowLabels || isShowTourPhotos);

      // markers
      _btnSwapClusterSymbolColor             .setEnabled(isShowClusteredMarker);
      _btnSwapTourMarkerLabel_Color          .setEnabled(isShowTourMarker);

      _actionMarkerType_Prefs                .setEnabled(isFilterMarkerType);
      _actionMarkerType_SelectAll            .setEnabled(isFilterMarkerType);
      _actionMarkerType_SelectInverse        .setEnabled(isFilterMarkerType);
      _actionMarkerType_SelectNone           .setEnabled(isFilterMarkerType);

      _chkIsFillClusterSymbol                .setEnabled(isShowClusteredMarker);
      _chkIsFilterTourMarkers                .setEnabled(isShowTourMarker);
      _chkIsGroupMarkers                     .setEnabled(isShowTourMarker);
      _chkIsGroupMarkers_All                 .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered                  .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered_All              .setEnabled(isShowTourMarker);

      _comboTourMarkerFilter                 .setEnabled(isFilterMarkerType);

      _lblClusterGrid_Size                   .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol                      .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol_Size                 .setEnabled(isShowClusteredMarker);
      _lblTourMarkerLabel_Color              .setEnabled(isShowTourMarker);

      _spinnerClusterGrid_Size               .setEnabled(isShowClusteredMarker);
      _spinnerClusterSymbol_Size             .setEnabled(isShowClusteredMarker);
      _spinnerClusterOutline_Width           .setEnabled(isShowClusteredMarker);

      _colorClusterSymbol_Fill               .setEnabled(isShowClusteredMarker);
      _colorClusterSymbol_Outline            .setEnabled(isShowClusteredMarker);
      _colorTourMarkerLabel_Fill             .setEnabled(isShowTourMarker);
      _colorTourMarkerLabel_Outline          .setEnabled(isShowTourMarker);

      // groups
      _lblGroupDuplicatedMarkers             .setEnabled(isGroupMarkers);
      _lblLabelGroupGridSize                 .setEnabled(isGroupMarkers);
      _spinnerLabelGroupGridSize             .setEnabled(isGroupMarkers);
      _txtGroupDuplicatedMarkers             .setEnabled(isGroupMarkers);

      // map dimming
      _spinnerMapDimValue                    .setEnabled(isDimMap);
      _colorMapDimColor                      .setEnabled(isDimMap);

      // common location
      _btnSwapCommonLocationLabel_Color      .setEnabled(isShowCommonLocations);
      _chkIsShowBoundingBox_Common           .setEnabled(isShowCommonLocations);
      _lblCommonLocationLabel_Color          .setEnabled(isShowCommonLocations);
      _colorCommonLocationLabel_Fill         .setEnabled(isShowCommonLocations);
      _colorCommonLocationLabel_Outline      .setEnabled(isShowCommonLocations);

      // tour location
      _btnSwapTourLocationLabel_Color        .setEnabled(isShowTourLocations);
      _chkIsShowBoundingBox_Tour             .setEnabled(isShowTourLocations);
      _lblTourLocationLabel_Color            .setEnabled(isShowTourLocations);
      _lblTourLocation_StartLabel_Color      .setEnabled(isShowTourLocations);
      _lblTourLocation_EndLabel_Color        .setEnabled(isShowTourLocations);
      _colorTourLocationLabel_Fill           .setEnabled(isShowTourLocations);
      _colorTourLocationLabel_Outline        .setEnabled(isShowTourLocations);
      _colorTourLocation_StartLabel_Fill     .setEnabled(isShowTourLocations);
      _colorTourLocation_StartLabel_Outline  .setEnabled(isShowTourLocations);
      _colorTourLocation_EndLabel_Fill       .setEnabled(isShowTourLocations);
      _colorTourLocation_EndLabel_Outline    .setEnabled(isShowTourLocations);

      // tour pause
      _btnSwapTourPauseLabel_Color           .setEnabled(isShowTourPauses);
      _lblTourPauseLabel_Color               .setEnabled(isShowTourPauses);
      _colorTourPauseLabel_Fill              .setEnabled(isShowTourPauses);
      _colorTourPauseLabel_Outline           .setEnabled(isShowTourPauses);

      _chkIsShowBoundingBox_All              .setEnabled(isShowCommonLocations || isShowTourLocations);

      // way points
      _btnSwapTourWayPointLabel_Color        .setEnabled(isShowTourWayPoints);
      _lblTourWayPointLabel_Color            .setEnabled(isShowTourWayPoints);
      _colorTourWayPointLabel_Fill           .setEnabled(isShowTourWayPoints);
      _colorTourWayPointLabel_Outline        .setEnabled(isShowTourWayPoints);

// SET_FORMATTING_ON

      _tourPausesUI.enableControls(isShowTourPauses);

      updateUI_TabLabel();

      // update photo action which could be enabled/disabled in the 2D map
      _actionStatistic_TourPhotos.setChecked(isShowTourPhotos);
   }

   private void fillUI() {

      for (final String label : _allMarkerLabelLayout_Label) {
         _comboLabelLayout.add(label);
      }

      for (final String label : _allFontNames) {
         _comboLabelFont.add(label);
      }

      for (final String label : _allMarkerLabelTime_Label) {
         _comboTourMarkerTime.add(label);
      }

      fillUI_MarkerTypes();
   }

   /**
    * Marker types
    */
   private void fillUI_MarkerTypes() {

      // fix NPE
      if (_comboTourMarkerFilter == null) {
         return;
      }

      final List<TourMarkerType> allMarkerTypes = TourDatabase.getAllTourMarkerTypes();

      final String[] allItems = new String[allMarkerTypes.size()];
      for (int itemIndex = 0; itemIndex < allMarkerTypes.size(); itemIndex++) {
         allItems[itemIndex] = allMarkerTypes.get(itemIndex).getTypeName();
      }

      _comboTourMarkerFilter.setItems(allItems);
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private String getSelectedLabelFont() {

      final int selectedIndex = _comboLabelFont.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allFontNames[selectedIndex];
      } else {
         return Map2ConfigManager.LABEL_FONT_NAME_DEFAULT;
      }
   }

   private MapLabelLayout getSelectedLabelLayout() {

      final int selectedIndex = _comboLabelLayout.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allMarkerLabelLayout_Value[selectedIndex];
      } else {
         return Map2ConfigManager.LABEL_LAYOUT_DEFAULT;
      }
   }

   /**
    * @return Returns the ID's of the seleted marker types
    */
   private long[] getSelectedMarkerFilter() {

      final int[] allSelectionIndices = _comboTourMarkerFilter.getSelectionIndices();
      final List<TourMarkerType> allMarkerTypes = TourDatabase.getAllTourMarkerTypes();

      final long[] allSelectedMarkerTypeIDs = new long[allSelectionIndices.length];
      int idIndex = 0;

      for (final int selectionIndex : allSelectionIndices) {

         final TourMarkerType markerType = allMarkerTypes.get(selectionIndex);

         allSelectedMarkerTypeIDs[idIndex++] = markerType.getId();
      }

      return allSelectedMarkerTypeIDs;
   }

   private MapTourMarkerTime getSelectedMarkerTimeStamp() {

      final int selectedIndex = _comboTourMarkerTime.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allMarkerLabelTimeStamp_Value[selectedIndex];
      } else {
         return Map2ConfigManager.TOUR_MARKER_TIME_STAMP_DEFAULT;
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

// SET_FORMATTING_OFF

      _imageDescriptor_SlideoutCollapse   = CommonActivator.getThemedImageDescriptor(  CommonImages.Slideout_Collapse);
      _imageDescriptor_SlideoutExpand     = CommonActivator.getThemedImageDescriptor(  CommonImages.Slideout_Expand);

      _imageDescriptor_BoundingBox        = TourbookPlugin.getThemedImageDescriptor(   Images.MapLocation_BoundingBox);
      _imageDescriptor_CommonLocation     = TourbookPlugin.getImageDescriptor(         Images.MapLocation_Common);
      _imageDescriptor_TourLocation       = TourbookPlugin.getImageDescriptor(         Images.MapLocation_Tour);
      _imageDescriptor_TourMarker         = TourbookPlugin.getThemedImageDescriptor(   Images.TourMarker);
      _imageDescriptor_TourMarker_Cluster = TourbookPlugin.getThemedImageDescriptor(   Images.TourMarker_Cluster);
      _imageDescriptor_TourMarker_Group   = TourbookPlugin.getThemedImageDescriptor(   Images.TourMarker_Group);
      _imageDescriptor_TourPause          = TourbookPlugin.getThemedImageDescriptor(   Images.TourPauses);
      _imageDescriptor_TourPhoto          = TourbookPlugin.getThemedImageDescriptor(   Images.ShowPhotos_InMap);
      _imageDescriptor_TourWayPoint       = TourbookPlugin.getThemedImageDescriptor(   Images.TourWayPoint);

      _imageMapLocation_BoundingBox       = _imageDescriptor_BoundingBox               .createImage();
      _imageMapLocation_Common            = _imageDescriptor_CommonLocation            .createImage();
      _imageMapLocation_Tour              = _imageDescriptor_TourLocation              .createImage();
      _imageTourMarker                    = _imageDescriptor_TourMarker                .createImage();
      _imageTourMarker_Cluster            = _imageDescriptor_TourMarker_Cluster        .createImage();
      _imageTourMarker_Group              = _imageDescriptor_TourMarker_Group          .createImage();
      _imageTourPauses                    = _imageDescriptor_TourPause                 .createImage();
      _imageTourWayPoint                  = _imageDescriptor_TourWayPoint              .createImage();

// SET_FORMATTING_ON

      _tourPausesUI = new TourPauseUI(this, this);

      // force spinner controls to have the same width
      _spinnerGridData = GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT);

      _mapPointSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyConfig(selectionEvent.widget));
      _mapPointSelectionListener_All = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyConfigAll());
      _mapPointPropertyChangeListener = propertyChangeEvent -> onModifyConfig(null);

      _mapPointMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
         onModifyConfig(mouseEvent.widget);
      };

      _mapPointMouseWheelListener4 = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 4);
         onModifyConfig(mouseEvent.widget);
      };

      _mapPointMouseWheelListener10 = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
         onModifyConfig(mouseEvent.widget);
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };

   }

   @Override
   public void onChangeUI_External() {

      saveConfig();

      repaintMap();
   }

   @Override
   protected void onDispose() {

      if (_prefChangeListener != null) {

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }

      onDisposeResources();

      super.onDispose();
   }

   private void onDisposeResources() {

      UI.disposeResource(_imageMapLocation_BoundingBox);
      UI.disposeResource(_imageMapLocation_Common);
      UI.disposeResource(_imageMapLocation_Tour);
      UI.disposeResource(_imageTourMarker);
      UI.disposeResource(_imageTourMarker_Cluster);
      UI.disposeResource(_imageTourMarker_Group);
      UI.disposeResource(_imageTourWayPoint);
      UI.disposeResource(_imageTourPauses);
   }

   @Override
   protected void onFocus() {

   }

   /**
    * @param isFromAllControls
    * @param widget
    *           Can be <code>null</code>
    */
   private void onModifyConfig(final boolean isFromAllControls, final Widget widget) {

      boolean isShowBoundingBox = _chkIsShowBoundingBox_All.getSelection();

      if (widget != null) {

         if (widget == _chkIsShowBoundingBox_Common) {

            isShowBoundingBox = _chkIsShowBoundingBox_Common.getSelection();

            _chkIsShowBoundingBox_Tour.setSelection(isShowBoundingBox);

         } else if (widget == _chkIsShowBoundingBox_Tour) {

            isShowBoundingBox = _chkIsShowBoundingBox_Tour.getSelection();

            _chkIsShowBoundingBox_Common.setSelection(isShowBoundingBox);

         } else if (widget == _chkIsShowBoundingBox_All) {

            _chkIsShowBoundingBox_Common.setSelection(isShowBoundingBox);
            _chkIsShowBoundingBox_Tour.setSelection(isShowBoundingBox);
         }
      }

// SET_FORMATTING_OFF

      final boolean isShowCommonLocations    = _chkIsShowCommonLocations      .getSelection();
      final boolean isShowTourLocations      = _chkIsShowTourLocations        .getSelection();
      final boolean isShowTourMarkers        = _chkIsShowTourMarkers          .getSelection();
      final boolean isShowTourPauses         = _chkIsShowTourPauses           .getSelection();
      final boolean isShowTourWayPoints      = _chkIsShowTourWayPoints        .getSelection();

      final boolean isShowTourPhotos         = Map2PainterConfig.isShowPhotos;

      _actionStatistic_CommonLocation  .setChecked(isShowCommonLocations);
      _actionStatistic_TourLocation    .setChecked(isShowTourLocations);
      _actionStatistic_TourMarker      .setChecked(isShowTourMarkers);
      _actionStatistic_TourPause       .setChecked(isShowTourPauses);
      _actionStatistic_TourPhotos      .setChecked(isShowTourPhotos);
      _actionStatistic_TourWayPoint    .setChecked(isShowTourWayPoints);


      if (isFromAllControls == false) {

         // update "all" controls
         _chkIsGroupMarkers_All                 .setSelection(_chkIsGroupMarkers          .getSelection());
         _chkIsMarkerClustered_All              .setSelection(_chkIsMarkerClustered       .getSelection());
         _chkIsShowBoundingBox_All              .setSelection(isShowBoundingBox);
         _chkIsShowCommonLocations_All          .setSelection(isShowCommonLocations);
         _chkIsShowTourLocations_All            .setSelection(isShowTourLocations);
         _chkIsShowTourMarkers_All              .setSelection(isShowTourMarkers);
         _chkIsShowTourPauses_All               .setSelection(isShowTourPauses);
         _chkIsShowTourWayPoints_All            .setSelection(isShowTourWayPoints);
      }

// SET_FORMATTING_ON

      saveConfig();

      enableControls();

      repaintMap();
   }

   private void onModifyConfig(final Widget widget) {

      if (_isInUpdateUI) {
         return;
      }

      onModifyConfig(false, widget);
   }

   private void onModifyConfigAll() {

      final boolean isShowBoundingBox = _chkIsShowBoundingBox_All.getSelection();

// SET_FORMATTING_OFF

      _chkIsGroupMarkers            .setSelection(_chkIsGroupMarkers_All            .getSelection());
      _chkIsMarkerClustered         .setSelection(_chkIsMarkerClustered_All         .getSelection());
      _chkIsShowBoundingBox_Common  .setSelection(isShowBoundingBox);
      _chkIsShowBoundingBox_Tour    .setSelection(isShowBoundingBox);
      _chkIsShowCommonLocations     .setSelection(_chkIsShowCommonLocations_All     .getSelection());
      _chkIsShowTourLocations       .setSelection(_chkIsShowTourLocations_All       .getSelection());
      _chkIsShowTourMarkers         .setSelection(_chkIsShowTourMarkers_All         .getSelection());
      _chkIsShowTourPauses          .setSelection(_chkIsShowTourPauses_All          .getSelection());
      _chkIsShowTourWayPoints       .setSelection(_chkIsShowTourWayPoints_All       .getSelection());

// SET_FORMATTING_ON

      onModifyConfig(true, null);
   }

   @Override
   protected void onResetLocation() {

      // show expanded, there is a bug in Linux where the tooltip header actions could not be selected
      _isSlideoutExpanded = true;

      // ensure that this is also set when the slideout was not yet open
      _state_Slideout.put(STATE_IS_SLIDEOUT_EXPANDED, _isSlideoutExpanded);

      if (_tabContainer != null && _tabContainer.isDisposed() == false) {

         // slideout was created

         updateUI_ExpandCollapse();

         onTTShellResize(null);
      }
   }

   @Override
   protected Point onResize(final int contentWidth, final int contentHeight) {

      if (_tabContainer.isDisposed()) {
         return null;
      }

      final int newContentWidth = contentWidth;
      int newContentHeight = contentHeight;

      if (_expandedHeight == -1) {

         // setup initial height

         _expandedHeight = contentHeight;
      }

      if (_isSlideoutExpanded) {

         // slideout is expanded

         _expandingCounter++;

         if (_expandingCounter < 2) {

            // the first height is the old/collapsed height

            newContentHeight = _expandedHeight;

         } else {

            newContentHeight = _expandedHeight = contentHeight;
         }

         _tabContainer.setVisible(true);

      } else {

         // slideout is collappsed

         _expandingCounter = 0;

         final int titleHeight = getTitleContainer().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
         final int statHeight = _statisticsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

         newContentHeight = titleHeight + statHeight

         // is needs additional spacing to see the stats
               + 10;

         _tabContainer.setVisible(false);
      }

      final Point newContentSize = new Point(newContentWidth, newContentHeight);

      return newContentSize;
   }

   private void onSwapClusterColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.clusterOutline_RGB;
      final RGB bgColor = mapConfig.clusterFill_RGB;

      mapConfig.clusterOutline_RGB = bgColor;
      mapConfig.clusterFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapCommonLocationColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.commonLocationOutline_RGB;
      final RGB bgColor = mapConfig.commonLocationFill_RGB;

      mapConfig.commonLocationOutline_RGB = bgColor;
      mapConfig.commonLocationFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapMarkerColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourMarkerOutline_RGB;
      final RGB bgColor = mapConfig.tourMarkerFill_RGB;

      mapConfig.tourMarkerOutline_RGB = bgColor;
      mapConfig.tourMarkerFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapTourLocation_EndColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourLocation_EndOutline_RGB;
      final RGB bgColor = mapConfig.tourLocation_EndFill_RGB;

      mapConfig.tourLocation_EndOutline_RGB = bgColor;
      mapConfig.tourLocation_EndFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapTourLocation_StartColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourLocation_StartOutline_RGB;
      final RGB bgColor = mapConfig.tourLocation_StartFill_RGB;

      mapConfig.tourLocation_StartOutline_RGB = bgColor;
      mapConfig.tourLocation_StartFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapTourLocationColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourLocationOutline_RGB;
      final RGB bgColor = mapConfig.tourLocationFill_RGB;

      mapConfig.tourLocationOutline_RGB = bgColor;
      mapConfig.tourLocationFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapTourPauseColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourPauseOutline_RGB;
      final RGB bgColor = mapConfig.tourPauseFill_RGB;

      mapConfig.tourPauseOutline_RGB = bgColor;
      mapConfig.tourPauseFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapTourWayPointColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.tourWayPointOutline_RGB;
      final RGB bgColor = mapConfig.tourWayPointFill_RGB;

      mapConfig.tourWayPointOutline_RGB = bgColor;
      mapConfig.tourWayPointFill_RGB = fgColor;

      mapConfig.setupColors();

      restoreState();
      repaintMap();
   }

   @Override
   protected void onTTShellMoved(final ControlEvent event) {

      // hide the dropdown which is a separate shell, it would keep open and is not moved
      _comboTourMarkerFilter.hideDropdownControl();
   }

   private void repaintMap() {

      final Map2 map2 = _map2View.getMap();

      map2.resetMapPoints();
      map2.paint();
   }

   @Override
   public void resetToDefaults() {

      _tourPausesUI.resetToDefaults();

      Map2ConfigManager.resetActiveMapPointConfiguration();

      restoreState();
      enableControls();

      repaintMap();
   }

   private void restoreState() {

      _isInUpdateUI = true;

      final Map2Config config = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      _chkIsFillClusterSymbol                .setSelection( config.isFillClusterSymbol);
      _chkIsShowCommonLocations              .setSelection( config.isShowCommonLocation);
      _chkIsShowCommonLocations_All          .setSelection( config.isShowCommonLocation);
      _chkIsShowTourLocations                .setSelection( config.isShowTourLocation);
      _chkIsShowTourLocations_All            .setSelection( config.isShowTourLocation);
      _chkIsShowTourMarkers                  .setSelection( config.isShowTourMarker);
      _chkIsShowTourMarkers_All              .setSelection( config.isShowTourMarker);
      _chkIsShowTourPauses                   .setSelection( config.isShowTourPauses);
      _chkIsShowTourPauses_All               .setSelection( config.isShowTourPauses);
      _chkIsFilterTourMarkers                .setSelection( config.isFilterTourMarkers);
      _chkIsGroupMarkers                     .setSelection( config.isGroupDuplicatedMarkers);
      _chkIsGroupMarkers_All                 .setSelection( config.isGroupDuplicatedMarkers);
      _chkIsLabelAntialiased                 .setSelection( config.isLabelAntialiased);
      _chkIsMarkerClustered                  .setSelection( config.isTourMarkerClustered);
      _chkIsMarkerClustered_All              .setSelection( config.isTourMarkerClustered);
      _chkIsShowBoundingBox_All              .setSelection( config.isShowLocationBoundingBox);
      _chkIsShowBoundingBox_Common           .setSelection( config.isShowLocationBoundingBox);
      _chkIsShowBoundingBox_Tour             .setSelection( config.isShowLocationBoundingBox);
      _chkIsTruncateLabel                    .setSelection( config.isTruncateLabel);
      _chkIsShowTourWayPoints                .setSelection( config.isShowTourWayPoint);
      _chkIsShowTourWayPoints_All            .setSelection( config.isShowTourWayPoint);

      _spinnerClusterGrid_Size               .setSelection( config.clusterGridSize);
      _spinnerClusterOutline_Width           .setSelection( config.clusterOutline_Width);
      _spinnerClusterSymbol_Size             .setSelection( config.clusterSymbol_Size);
      _spinnerLabelDistributorMaxLabels      .setSelection( config.labelDistributorMaxLabels);
      _spinnerLabelDistributorRadius         .setSelection( config.labelDistributorRadius);
      _spinnerLabelFontSize                  .setSelection( config.labelFontSize);
      _spinnerLabelGroupGridSize             .setSelection( config.groupGridSize);
      _spinnerLabelRespectMargin             .setSelection( config.labelRespectMargin);
      _spinnerLabelTruncateLength            .setSelection( config.labelTruncateLength);
      _spinnerLocationSymbolSize             .setSelection( config.locationSymbolSize);

      _colorClusterSymbol_Fill               .setColorValue(config.clusterFill_RGB);
      _colorClusterSymbol_Outline            .setColorValue(config.clusterOutline_RGB);
      _colorTourMarkerLabel_Fill             .setColorValue(config.tourMarkerFill_RGB);
      _colorTourMarkerLabel_Outline          .setColorValue(config.tourMarkerOutline_RGB);

      _colorCommonLocationLabel_Fill         .setColorValue(config.commonLocationFill_RGB);
      _colorCommonLocationLabel_Outline      .setColorValue(config.commonLocationOutline_RGB);

      _colorTourLocationLabel_Fill           .setColorValue(config.tourLocationFill_RGB);
      _colorTourLocationLabel_Outline        .setColorValue(config.tourLocationOutline_RGB);
      _colorTourLocation_StartLabel_Fill     .setColorValue(config.tourLocation_StartFill_RGB);
      _colorTourLocation_StartLabel_Outline  .setColorValue(config.tourLocation_StartOutline_RGB);
      _colorTourLocation_EndLabel_Fill       .setColorValue(config.tourLocation_EndFill_RGB);
      _colorTourLocation_EndLabel_Outline    .setColorValue(config.tourLocation_EndOutline_RGB);

      _colorTourPauseLabel_Fill              .setColorValue(config.tourPauseFill_RGB);
      _colorTourPauseLabel_Outline           .setColorValue(config.tourPauseOutline_RGB);

      _colorTourWayPointLabel_Fill           .setColorValue(config.tourWayPointFill_RGB);
      _colorTourWayPointLabel_Outline        .setColorValue(config.tourWayPointOutline_RGB);

      _txtGroupDuplicatedMarkers             .setText(      config.groupedMarkers);

      /*
       * Map dimming & transparency
       */
      _chkIsDimMap               .setSelection(    Util.getStateBoolean(_state_Map2,  Map2View.STATE_IS_MAP_DIMMED,                       Map2View.STATE_IS_MAP_DIMMED_DEFAULT));
      _colorMapDimColor          .setColorValue(   Util.getStateRGB(    _state_Map2,  Map2View.STATE_DIM_MAP_COLOR,                       Map2View.STATE_DIM_MAP_COLOR_DEFAULT));
      _spinnerMapDimValue        .setSelection(    Util.getStateInt(    _state_Map2,  Map2View.STATE_DIM_MAP_VALUE,                       Map2View.STATE_DIM_MAP_VALUE_DEFAULT));

      /*
       * Slideout expand/collapse
       */
      final int defaultHeight    = _shellContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

      _expandedHeight            = Util.getStateInt(     _state_Slideout, STATE_EXPANDED_HEIGHT, defaultHeight);
      _isSlideoutExpanded        = Util.getStateBoolean( _state_Slideout, STATE_IS_SLIDEOUT_EXPANDED, true);

      _tabContainer.setVisible(_isSlideoutExpanded);

      _actionStatistic_CommonLocation  .setChecked(config.isShowCommonLocation);
      _actionStatistic_TourLocation    .setChecked(config.isShowTourLocation);
      _actionStatistic_TourMarker      .setChecked(config.isShowTourMarker);
      _actionStatistic_TourPause       .setChecked(config.isShowTourPauses);
      _actionStatistic_TourWayPoint    .setChecked(config.isShowTourWayPoint);

      _tourPausesUI.restoreState();

      selectLabelFont(        config.labelFontName);
      selectLabelLayout(      config.labelLayout);
      selectMarkerTimeStamp(  config.tourMarkerDateTimeFormat);
      selectMarkerFilter(     config.tourMarkerFilter);

// SET_FORMATTING_ON

      updateUI_TabLabel();
      updateUI_ExpandCollapse();

      _isInUpdateUI = false;
   }

   private void restoreTabFolder() {

      _tabFolder.setSelection(Util.getStateInt(_state_Slideout, STATE_SELECTED_TAB, 0));
   }

   private void saveConfig() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      config.isShowLocationBoundingBox    = _chkIsShowBoundingBox_All            .getSelection();
      config.isShowCommonLocation         = _chkIsShowCommonLocations            .getSelection();
      config.isShowTourLocation           = _chkIsShowTourLocations              .getSelection();
      config.isShowTourMarker             = _chkIsShowTourMarkers                .getSelection();
      config.isShowTourPauses             = _chkIsShowTourPauses                 .getSelection();
      config.isShowTourWayPoint           = _chkIsShowTourWayPoints              .getSelection();

      config.isGroupDuplicatedMarkers     = _chkIsGroupMarkers                   .getSelection();
      config.groupGridSize                = _spinnerLabelGroupGridSize           .getSelection();
      config.groupedMarkers               = _txtGroupDuplicatedMarkers           .getText();

      config.isFillClusterSymbol          = _chkIsFillClusterSymbol              .getSelection();
      config.isFilterTourMarkers          = _chkIsFilterTourMarkers              .getSelection();
      config.isLabelAntialiased           = _chkIsLabelAntialiased               .getSelection();
      config.isTourMarkerClustered        = _chkIsMarkerClustered                .getSelection();

      config.clusterGridSize              = _spinnerClusterGrid_Size             .getSelection();
      config.clusterOutline_Width         = _spinnerClusterOutline_Width         .getSelection();
      config.clusterSymbol_Size           = _spinnerClusterSymbol_Size           .getSelection();
      config.clusterFill_RGB              = _colorClusterSymbol_Fill             .getColorValue();
      config.clusterOutline_RGB           = _colorClusterSymbol_Outline          .getColorValue();

      config.isTruncateLabel              = _chkIsTruncateLabel                  .getSelection();
      config.labelDistributorMaxLabels    = _spinnerLabelDistributorMaxLabels    .getSelection();
      config.labelDistributorRadius       = _spinnerLabelDistributorRadius       .getSelection();
      config.labelFontSize                = _spinnerLabelFontSize                .getSelection();
      config.labelRespectMargin           = _spinnerLabelRespectMargin           .getSelection();
      config.labelTruncateLength          = _spinnerLabelTruncateLength          .getSelection();
      config.locationSymbolSize           = _spinnerLocationSymbolSize           .getSelection();

      config.labelFontName                = getSelectedLabelFont();
      config.labelLayout                  = getSelectedLabelLayout();
      config.tourMarkerDateTimeFormat     = getSelectedMarkerTimeStamp();
      config.tourMarkerFilter             = getSelectedMarkerFilter();

      config.commonLocationFill_RGB             = _colorCommonLocationLabel_Fill             .getColorValue();
      config.commonLocationOutline_RGB          = _colorCommonLocationLabel_Outline          .getColorValue();

      config.tourLocationFill_RGB               = _colorTourLocationLabel_Fill               .getColorValue();
      config.tourLocationOutline_RGB            = _colorTourLocationLabel_Outline            .getColorValue();
      config.tourLocation_StartFill_RGB         = _colorTourLocation_StartLabel_Fill         .getColorValue();
      config.tourLocation_StartOutline_RGB      = _colorTourLocation_StartLabel_Outline      .getColorValue();
      config.tourLocation_EndFill_RGB           = _colorTourLocation_EndLabel_Fill           .getColorValue();
      config.tourLocation_EndOutline_RGB        = _colorTourLocation_EndLabel_Outline        .getColorValue();

      config.tourMarkerFill_RGB                 = _colorTourMarkerLabel_Fill                 .getColorValue();
      config.tourMarkerOutline_RGB              = _colorTourMarkerLabel_Outline              .getColorValue();

      config.tourPauseFill_RGB                  = _colorTourPauseLabel_Fill                  .getColorValue();
      config.tourPauseOutline_RGB               = _colorTourPauseLabel_Outline               .getColorValue();

      config.tourWayPointFill_RGB               = _colorTourWayPointLabel_Fill               .getColorValue();
      config.tourWayPointOutline_RGB            = _colorTourWayPointLabel_Outline            .getColorValue();


      config.setupColors();

      /*
       * Map dimming & transparency
       */
      _state_Map2.put(Map2View.STATE_IS_MAP_DIMMED,                     _chkIsDimMap               .getSelection());
      _state_Map2.put(Map2View.STATE_DIM_MAP_VALUE,                     _spinnerMapDimValue        .getSelection());

      Util.setState(_state_Map2, Map2View.STATE_DIM_MAP_COLOR,          _colorMapDimColor          .getColorValue());

      _map2View.setupMapDimLevel();

// SET_FORMATTING_ON

   }

   @Override
   protected void saveState() {

      _state_Slideout.put(STATE_EXPANDED_HEIGHT, _expandedHeight);
      _state_Slideout.put(STATE_IS_SLIDEOUT_EXPANDED, _isSlideoutExpanded);
      _state_Slideout.put(STATE_SELECTED_TAB, _tabFolder.getSelectionIndex());

      super.saveState();
   }

   private void selectLabelFont(final String selectedFontName) {

      int selectionIndex = 0;

      for (int fontIndex = 0; fontIndex < _allFontNames.length; fontIndex++) {

         final String fontName = _allFontNames[fontIndex];

         if (fontName.equals(selectedFontName)) {
            selectionIndex = fontIndex;
            break;
         }
      }

      _comboLabelFont.select(selectionIndex);
   }

   private void selectLabelLayout(final Enum<MapLabelLayout> filterOperator) {

      int selectionIndex = 0;

      for (int operatorIndex = 0; operatorIndex < _allMarkerLabelLayout_Value.length; operatorIndex++) {

         final MapLabelLayout tourFilterFieldOperator = _allMarkerLabelLayout_Value[operatorIndex];

         if (tourFilterFieldOperator.equals(filterOperator)) {
            selectionIndex = operatorIndex;
            break;
         }
      }

      _comboLabelLayout.select(selectionIndex);
   }

   /**
    * @param allRequestedMarkerTypeIDs
    */
   private void selectMarkerFilter(final long[] allRequestedMarkerTypeIDs) {

      // combo is sorted by name
      final List<TourMarkerType> allMarkerTypes = TourDatabase.getAllTourMarkerTypes();

      final IntArrayList allSelectionIndices = new IntArrayList();

      if (allRequestedMarkerTypeIDs != null) {

         for (int comboIndex = 0; comboIndex < allMarkerTypes.size(); comboIndex++) {

            final TourMarkerType comboMarkerType = allMarkerTypes.get(comboIndex);

            final long comboID = comboMarkerType.getId();

            for (final long requestedID : allRequestedMarkerTypeIDs) {

               if (requestedID == comboID) {

                  allSelectionIndices.add(comboIndex);

                  break;
               }
            }
         }
      }

      /**
       * This code is very hacky because the NatCombo do not check the selected items or deselect
       * thems :-(((
       */
      final int[] allSelectedIndices = allSelectionIndices.toArray();

      _comboTourMarkerFilter.selectAll(false); // deselect all
      _comboTourMarkerFilter.select(allSelectedIndices);
      _comboTourMarkerFilter.check(allSelectedIndices);
   }

   private void selectMarkerTimeStamp(final Enum<MapTourMarkerTime> markerTimestamp) {

      int selectionIndex = 0;

      for (int valueIndex = 0; valueIndex < _allMarkerLabelTimeStamp_Value.length; valueIndex++) {

         final MapTourMarkerTime timeStamp = _allMarkerLabelTimeStamp_Value[valueIndex];

         if (timeStamp.equals(markerTimestamp)) {
            selectionIndex = valueIndex;
            break;
         }
      }

      _comboTourMarkerTime.select(selectionIndex);
   }

   private void selectTab(final CTabItem tabItem, final Event event) {

      // prevent tab selection when ctrl key is hit
      if (UI.isCtrlKey(event) == false) {

         _tabFolder.setSelection(tabItem);

         /**
          * !!! This is needed in the dark mode otherwise the tab background is in bright color when
          * tabs are switch with the action button :-(
          */
         tabItem.getControl().setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
      }

      onModifyConfig(null);
   }

   public void updateStatistics(final MapPointStatistics stats) {

      if (_tabFolder.isDisposed()) {
         // this happened
         return;
      }

      final String numCommonLocations_All = Integer.toString(stats.numCommonLocations_All);
      final String numTourLocations_All = Integer.toString(stats.numTourLocations_All);
      String numTourMarkers_All = Integer.toString(stats.numTourMarkers_All);
      String numTourPauses_All = Integer.toString(stats.numTourPauses_All);
      String numTourPhotos_All = Integer.toString(stats.numTourPhotos_All);
      String numTourWayPoints_All = Integer.toString(stats.numTourWayPoints_All);

      if (stats.numTourMarkers_All_IsTruncated) {
         numTourMarkers_All += UI.SYMBOL_STAR;
      }

      if (stats.numTourPauses_All_IsTruncated) {
         numTourPauses_All += UI.SYMBOL_STAR;
      }

      if (stats.numTourPhotos_All_IsTruncated) {
         numTourPhotos_All += UI.SYMBOL_STAR;
      }

      if (stats.numTourWayPoints_All_IsTruncated) {
         numTourWayPoints_All += UI.SYMBOL_STAR;
      }

      _lblStats_CommonLocations_All.setText(numCommonLocations_All);
      _lblStats_CommonLocations_Visible.setText(Integer.toString(stats.numCommonLocations_Painted));

      _lblStats_TourLocations_All.setText(numTourLocations_All);
      _lblStats_TourLocations_Visible.setText(Integer.toString(stats.numTourLocations_Painted));

      _lblStats_TourMarkers_All.setText(numTourMarkers_All);
      _lblStats_TourMarkers_Visible.setText(Integer.toString(stats.numTourMarkers_Painted));

      _lblStats_TourPauses_All.setText(numTourPauses_All);
      _lblStats_TourPauses_Visible.setText(Integer.toString(stats.numTourPauses_Painted));

      _lblStats_Photos_All.setText(numTourPhotos_All);
      _lblStats_Photos_Visible.setText(Integer.toString(stats.numTourPhotos_Painted));

      _lblStats_TourWayPoints_All.setText(numTourWayPoints_All);
      _lblStats_TourWayPoints_Visible.setText(Integer.toString(stats.numTourWayPoints_Painted));

      _statisticsContainer.pack();
   }

   public void updateUI() {

      restoreState();
      enableControls();
   }

   private void updateUI_ExpandCollapse() {

      if (_isSlideoutExpanded) {

         _actionExpandCollapseSlideout.setToolTipText(Messages.Slideout_Action_CollapseSlideout_Tooltip);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutCollapse);

      } else {

         _actionExpandCollapseSlideout.setToolTipText(Messages.Slideout_Action_ExpandSlideout_Tooltip);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutExpand);
      }

      _toolbarManagerExpandCollapseSlideout.update(true);
   }

   /**
    * Show a flag in the tab label when content is enabled
    */
   private void updateUI_TabLabel() {

// SET_FORMATTING_OFF

      final boolean isGroupDuplicatedMarkers    = _chkIsGroupMarkers.getSelection();
      final boolean isShowCommonLocations       = _chkIsShowCommonLocations.getSelection();
      final boolean isShowTourLocations         = _chkIsShowTourLocations.getSelection();
      final boolean isShowTourMarkers           = _chkIsShowTourMarkers.getSelection();
      final boolean isShowTourPauses            = _chkIsShowTourPauses.getSelection();
      final boolean isShowTourWayPoints         = _chkIsShowTourWayPoints.getSelection();

      _tabCommonLocations  .setText(isShowCommonLocations   ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourLocations    .setText(isShowTourLocations     ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkers      .setText(isShowTourMarkers       ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkerGroups .setText(isShowTourMarkers && isGroupDuplicatedMarkers ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourPauses       .setText(isShowTourPauses        ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourWayPoints    .setText(isShowTourWayPoints     ? UI.SYMBOL_STAR : UI.EMPTY_STRING);

// SET_FORMATTING_ON
   }

}
