/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.common.util.Util;
import net.tourbook.tour.location.CommonLocationView;
import net.tourbook.tour.location.TourLocationView;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
   private static final String            STATE_EXPANDED_HEIGHT       = "STATE_EXPANDED_HEIGHT";         //$NON-NLS-1$
   private static final String            STATE_IS_SLIDEOUT_EXPANDED  = "STATE_IS_SLIDEOUT_EXPANDED";    //$NON-NLS-1$
   private static final String            STATE_SELECTED_TAB          = "STATE_SELECTED_TAB";            //$NON-NLS-1$
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelLayout_Label}
    */
   private static MapLabelLayout[]        _allMarkerLabelLayout_Value = {

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
   private static String[]                _allMarkerLabelLayout_Label = {

         Messages.Map_Points_LabelBackground_RectangleBox,
         Messages.Map_Points_LabelBackground_Border2,
         Messages.Map_Points_LabelBackground_Border1,
         Messages.Map_Points_LabelBackground_Shadow,
         Messages.Map_Points_LabelBackground_None,

   };
   //
   private final IPreferenceStore         _prefStore                  = TourbookPlugin.getPrefStore();
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
   private MouseWheelListener             _mapPointMouseWheelListener10;
   private FocusListener                  _keepOpenListener;
   private IPropertyChangeListener        _prefChangeListener;
   //
   private ActionExpandSlideout           _actionExpandCollapseSlideout;
   private ActionResetToDefaults          _actionRestoreDefaults;
   private ActionStatistic_CommonLocation _actionStatistic_CommonLocation;
   private ActionStatistic_TourLocation   _actionStatistic_TourLocation;
   private ActionStatistic_TourMarker     _actionStatistic_TourMarker;
   private ActionStatistic_TourPause      _actionStatistic_TourPause;
   //
   private PixelConverter                 _pc;
   //
   private boolean                        _isSlideoutExpanded;
   private int                            _expandingCounter;
   private int                            _expandedHeight             = -1;
   //
   private TourPauseUI                    _tourPausesUI;
   //
   private final NumberFormat             _nf3                        = NumberFormat.getNumberInstance();
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
   //
   private Button                _btnSwapClusterSymbolColor;
   private Button                _btnSwapCommonLocationLabel_Color;
   private Button                _btnSwapTourLocationLabel_Color;
   private Button                _btnSwapTourLocation_StartLabel_Color;
   private Button                _btnSwapTourLocation_EndLabel_Color;
   private Button                _btnSwapTourMarkerLabel_Color;
   private Button                _btnSwapTourPauseLabel_Color;
   //
   private Button                _chkIsLabelAntialiased;
   private Button                _chkIsSymbolAntialiased;
   private Button                _chkIsDimMap;
   private Button                _chkIsFillClusterSymbol;
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
   private Button                _chkIsTruncateLabel;
   private Button                _chkIsWrapLabel;
   private Button                _chkUseMapDimColor;
   //
   private Combo                 _comboLabelLayout;
   //
   private Label                 _lblClusterGrid_Size;
   private Label                 _lblClusterSymbol;
   private Label                 _lblClusterSymbol_Size;
   private Label                 _lblGroupDuplicatedMarkers;
   private Label                 _lblLabelGroupGridSize;
   private Label                 _lblCommonLocationLabel_Color;
   private Label                 _lblLabelBackground;
   private Label                 _lblStats_CommonLocations_All;
   private Label                 _lblStats_CommonLocations_Visible;
   private Label                 _lblStats_TourLocations_All;
   private Label                 _lblStats_TourLocations_Visible;
   private Label                 _lblStats_TourMarkers_All;
   private Label                 _lblStats_TourMarkers_Visible;
   private Label                 _lblStats_TourPauses_All;
   private Label                 _lblStats_TourPauses_Visible;
   private Label                 _lblTourLocationLabel_Color;
   private Label                 _lblTourLocation_StartLabel_Color;
   private Label                 _lblTourLocation_EndLabel_Color;
   private Label                 _lblTourMarkerLabel_Color;
   private Label                 _lblTourPauseLabel_Color;
   private Label                 _lblVisibleLabels;
   //
   private Spinner               _spinnerClusterGrid_Size;
   private Spinner               _spinnerClusterOutline_Width;
   private Spinner               _spinnerClusterSymbol_Size;
   private Spinner               _spinnerLabelDistributorMaxLabels;
   private Spinner               _spinnerLabelDistributorRadius;
   private Spinner               _spinnerLabelGroupGridSize;
   private Spinner               _spinnerLabelTruncateLength;
   private Spinner               _spinnerLabelWrapLength;
   private Spinner               _spinnerMapDimValue;
   //
   private Text                  _txtGroupDuplicatedMarkers;
   //
   private ColorSelectorExtended _colorClusterSymbol_Fill;
   private ColorSelectorExtended _colorClusterSymbol_Outline;
   private ColorSelectorExtended _colorCommonLocationLabel_Fill;
   private ColorSelectorExtended _colorCommonLocationLabel_Outline;
   private ColorSelectorExtended _colorMapDimColor;
   private ColorSelectorExtended _colorMapTransparencyColor;
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
   //
   private Image                 _imageMapLocation_BoundingBox;
   private Image                 _imageMapLocation_Common;
   private Image                 _imageMapLocation_Tour;
   private Image                 _imageTourMarker;
   private Image                 _imageTourMarker_Cluster;
   private Image                 _imageTourMarker_Group;
   private Image                 _imageTourPauses;

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

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {}

   /**
    * @param map2State
    * @param map2View
    * @param map2View
    * @param ownerControl
    * @param toolBar
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
   }

   private void actionExpandCollapseSlideout() {

      // toggle expand state
      _isSlideoutExpanded = !_isSlideoutExpanded;

      updateUI_ExpandCollapse();

      onTTShellResize(null);
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

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

      _actionExpandCollapseSlideout = new ActionExpandSlideout();
      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionStatistic_CommonLocation = new ActionStatistic_CommonLocation();
      _actionStatistic_TourMarker = new ActionStatistic_TourMarker();
      _actionStatistic_TourPause = new ActionStatistic_TourPause();
      _actionStatistic_TourLocation = new ActionStatistic_TourLocation();
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
               _tabTourPauses.setControl(createUI_400_Tab_TourPauses(_tabFolder));

               _tabTourLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourLocations.setImage(_imageMapLocation_Tour);
               _tabTourLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_TourLocations_Tooltip);
               _tabTourLocations.setControl(createUI_600_Tab_TourLocations(_tabFolder));

               _tabCommonLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabCommonLocations.setImage(_imageMapLocation_Common);
               _tabCommonLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_CommonLocations_Tooltip);
               _tabCommonLocations.setControl(createUI_700_Tab_CommonLocations(_tabFolder));
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
             * Marker background
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
               _spinnerLabelDistributorMaxLabels.setToolTipText(
                     Messages.Slideout_MapPoints_Spinner_LabelDistributor_MaxLabels_Tooltip);

               // label distributor radius
               _spinnerLabelDistributorRadius = new Spinner(container, SWT.BORDER);
               _spinnerLabelDistributorRadius.setMinimum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MIN);
               _spinnerLabelDistributorRadius.setMaximum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MAX);
               _spinnerLabelDistributorRadius.setIncrement(10);
               _spinnerLabelDistributorRadius.setPageIncrement(100);
               _spinnerLabelDistributorRadius.addSelectionListener(_mapPointSelectionListener);
               _spinnerLabelDistributorRadius.addMouseWheelListener(_mapPointMouseWheelListener10);
               _spinnerLabelDistributorRadius.setToolTipText(Messages.Slideout_MapPoints_Spinner_LabelDistributor_Radius_Tooltip);
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
             * Wrap label
             */
            _chkIsWrapLabel = new Button(tabContainer, SWT.CHECK);
            _chkIsWrapLabel.setText(Messages.Slideout_MapPoints_Checkbox_WrapLabel);
            _chkIsWrapLabel.addSelectionListener(_mapPointSelectionListener);
            gdHCenter.applyTo(_chkIsWrapLabel);

            // spinner
            _spinnerLabelWrapLength = new Spinner(tabContainer, SWT.BORDER);
            _spinnerLabelWrapLength.setMinimum(Map2ConfigManager.LABEL_WRAP_LENGTH_MIN);
            _spinnerLabelWrapLength.setMaximum(Map2ConfigManager.LABEL_WRAP_LENGTH_MAX);
            _spinnerLabelWrapLength.setIncrement(1);
            _spinnerLabelWrapLength.setPageIncrement(10);
            _spinnerLabelWrapLength.addSelectionListener(_mapPointSelectionListener);
            _spinnerLabelWrapLength.addMouseWheelListener(_mapPointMouseWheelListener10);
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
             * Antialias symbol
             */
            _chkIsSymbolAntialiased = new Button(tabContainer, SWT.CHECK);
            _chkIsSymbolAntialiased.setText(Messages.Slideout_MapPoints_Checkbox_AntialiasSymbol);
            _chkIsSymbolAntialiased.addSelectionListener(_mapPointSelectionListener);
            gdSpan2.applyTo(_chkIsSymbolAntialiased);
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
         {
            /*
             * Map transparency color
             */
            {
               final Label label = new Label(tabContainer, SWT.NONE);
               label.setText(Messages.Slideout_MapPoints_Label_MapTransparencyColor);
               label.setToolTipText(Messages.Slideout_MapPoints_Label_MapTransparencyColor_Tooltip);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               _colorMapTransparencyColor = new ColorSelectorExtended(tabContainer);
               _colorMapTransparencyColor.setToolTipText(Messages.Slideout_MapPoints_Label_MapTransparencyColor_Tooltip);
               _colorMapTransparencyColor.addListener(_mapPointPropertyChangeListener);
               _colorMapTransparencyColor.addOpenListener(this);
            }
            {
               /*
                * Use map dim color
                */
               _chkUseMapDimColor = new Button(tabContainer, SWT.CHECK);
               _chkUseMapDimColor.setText(Messages.Slideout_MapPoints_Checkbox_UseMapDimColor);
               _chkUseMapDimColor.addSelectionListener(_mapPointSelectionListener);
               gdSpan2.indent(16, 0).applyTo(_chkUseMapDimColor);
            }
         }
      }

      return tabContainer;
   }

   private Control createUI_200_Tab_TourMarkers(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(tabContainer);
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
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_220_TourMarker_Label(container);
            createUI_230_TourMarker_Cluster(container);
         }
      }

      return tabContainer;
   }

   private void createUI_220_TourMarker_Label(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
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
            labelGridData.applyTo(_lblTourMarkerLabel_Color);
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
            _btnSwapTourMarkerLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapMarkerColor()));
         }
      }
   }

   private void createUI_230_TourMarker_Cluster(final Composite parent) {

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
            _spinnerLabelGroupGridSize.setMinimum(Map2ConfigManager.LABEL_WRAP_LENGTH_MIN);
            _spinnerLabelGroupGridSize.setMaximum(Map2ConfigManager.LABEL_WRAP_LENGTH_MAX);
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

   private Control createUI_400_Tab_TourPauses(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).applyTo(container);
      {
         createUI_410_TourPauses_Label(container);

         _tourPausesUI.createContent(container);
      }

      return container;
   }

   private void createUI_410_TourPauses_Label(final Composite parent) {

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
         createUI_610_TourLocation_Label(container);
      }

      return container;
   }

   private void createUI_610_TourLocation_Label(final Composite parent) {

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
         createUI_710_CommonLocation_Label(container);
      }

      return container;
   }

   private void createUI_710_CommonLocation_Label(final Composite parent) {

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

   private void createUI_900_Statistics(final Composite shellContainer) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END);
      final int horizontalSpacing = 0;

      final String tooltipCommonLocations = Messages.Slideout_MapPoints_Label_Stats_CommonLocations;
      final String tooltipTourLocations = Messages.Slideout_MapPoints_Label_Stats_TourLocations;
      final String tooltipMarkers = Messages.Slideout_MapPoints_Label_Stats_TourMarkers;
      final String tooltipPauses = Messages.Slideout_MapPoints_Label_Stats_TourPauses;

      _statisticsContainer = new Composite(shellContainer, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(_statisticsContainer);
      GridLayoutFactory.fillDefaults().numColumns(13).applyTo(_statisticsContainer);
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

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isGroupDuplicatedMarkers = _chkIsGroupMarkers             .getSelection();
      final boolean isMarkerClustered        = _chkIsMarkerClustered          .getSelection();
      final boolean isShowTourMarker         = _chkIsShowTourMarkers          .getSelection();
      final boolean isShowTourPauses         = _chkIsShowTourPauses           .getSelection();
      final boolean isShowTourLocations      = _chkIsShowTourLocations        .getSelection();
      final boolean isShowCommonLocations    = _chkIsShowCommonLocations      .getSelection();

      final boolean isTruncateLabel          = _chkIsTruncateLabel            .getSelection();
      final boolean isWrapLabel              = _chkIsWrapLabel                .getSelection();

      final boolean isDimMap                 = _chkIsDimMap                   .getSelection();
      final boolean isUseMapDimColor         = _chkUseMapDimColor             .getSelection();
      final boolean isUseTransparencyColor   = isUseMapDimColor == false;

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

      // common
      _chkIsLabelAntialiased                 .setEnabled(isShowLabels);
      _chkIsSymbolAntialiased                .setEnabled(isShowLabels);
      _chkIsTruncateLabel                    .setEnabled(isShowLabels);
      _chkIsWrapLabel                        .setEnabled(isShowLabels);
      _comboLabelLayout                      .setEnabled(isShowLabels);
      _lblLabelBackground                    .setEnabled(isShowLabels);
      _lblVisibleLabels                      .setEnabled(isShowLabels);
      _spinnerLabelDistributorMaxLabels      .setEnabled(isShowLabels);
      _spinnerLabelDistributorRadius         .setEnabled(isShowLabels);
      _spinnerLabelTruncateLength            .setEnabled(isShowLabels && isTruncateLabel);
      _spinnerLabelWrapLength                .setEnabled(isShowLabels && isWrapLabel);

      _btnSwapClusterSymbolColor             .setEnabled(isShowClusteredMarker);
      _btnSwapTourMarkerLabel_Color          .setEnabled(isShowTourMarker);

      _chkIsFillClusterSymbol                .setEnabled(isShowClusteredMarker);
      _chkIsGroupMarkers                     .setEnabled(isShowTourMarker);
      _chkIsGroupMarkers_All                 .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered                  .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered_All              .setEnabled(isShowTourMarker);

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
      _chkUseMapDimColor                     .setEnabled(isDimMap);
      _spinnerMapDimValue                    .setEnabled(isDimMap);
      _colorMapDimColor                      .setEnabled(isDimMap);
      _colorMapTransparencyColor             .setEnabled(isDimMap == false || isUseTransparencyColor);

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

// SET_FORMATTING_ON

      _tourPausesUI.enableControls(isShowTourPauses);

      updateUI_TabLabel();
   }

   private void fillUI() {

      for (final String label : _allMarkerLabelLayout_Label) {
         _comboLabelLayout.add(label);
      }
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private MapLabelLayout getSelectedMarkerLabelLayout() {

      final int selectedIndex = _comboLabelLayout.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allMarkerLabelLayout_Value[selectedIndex];
      } else {
         return Map2ConfigManager.LABEL_LAYOUT_DEFAULT;
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

      _imageMapLocation_BoundingBox       = _imageDescriptor_BoundingBox               .createImage();
      _imageMapLocation_Common            = _imageDescriptor_CommonLocation            .createImage();
      _imageMapLocation_Tour              = _imageDescriptor_TourLocation              .createImage();
      _imageTourMarker                    = _imageDescriptor_TourMarker                .createImage();
      _imageTourMarker_Cluster            = _imageDescriptor_TourMarker_Cluster        .createImage();
      _imageTourMarker_Group              = _imageDescriptor_TourMarker_Group          .createImage();
      _imageTourPauses                    = _imageDescriptor_TourPause                 .createImage();

// SET_FORMATTING_ON

      _tourPausesUI = new TourPauseUI(this, this);

      // force spinner controls to have the same width
      _spinnerGridData = GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT);

      _mapPointSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyConfig(selectionEvent.widget));
      _mapPointSelectionListener_All = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyConfigAll());
      _mapPointPropertyChangeListener = propertyChangeEvent -> onModifyConfig(null);

      _mapPointMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
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

      UI.disposeResource(_imageMapLocation_BoundingBox);
      UI.disposeResource(_imageMapLocation_Common);
      UI.disposeResource(_imageMapLocation_Tour);
      UI.disposeResource(_imageTourMarker);
      UI.disposeResource(_imageTourMarker_Cluster);
      UI.disposeResource(_imageTourMarker_Group);

      super.onDispose();
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

      _actionStatistic_CommonLocation  .setChecked(isShowCommonLocations);
      _actionStatistic_TourLocation    .setChecked(isShowTourLocations);
      _actionStatistic_TourMarker      .setChecked(isShowTourMarkers);
      _actionStatistic_TourPause       .setChecked(isShowTourPauses);


      if (isFromAllControls == false) {

         // update "all" controls
         _chkIsGroupMarkers_All                 .setSelection(_chkIsGroupMarkers          .getSelection());
         _chkIsMarkerClustered_All              .setSelection(_chkIsMarkerClustered       .getSelection());
         _chkIsShowBoundingBox_All              .setSelection(isShowBoundingBox);
         _chkIsShowCommonLocations_All          .setSelection(isShowCommonLocations);
         _chkIsShowTourLocations_All            .setSelection(isShowTourLocations);
         _chkIsShowTourMarkers_All              .setSelection(isShowTourMarkers);
         _chkIsShowTourPauses_All               .setSelection(isShowTourPauses);
      }

// SET_FORMATTING_ON

      saveConfig();

      enableControls();

      repaintMap();
   }

   private void onModifyConfig(final Widget widget) {

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

// SET_FORMATTING_ON

      onModifyConfig(true, null);
   }

   @Override
   protected Point onResize(final int contentWidth, final int contentHeight) {

      final int newContentWidth = contentWidth;
      int newContentHeight = contentHeight;

      if (_expandedHeight == -1) {

         // setup initial height

         _expandedHeight = contentHeight;
      }

      if (_isSlideoutExpanded) {

         // slideout is expanded collappsed

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
      _chkIsGroupMarkers                     .setSelection( config.isGroupDuplicatedMarkers);
      _chkIsGroupMarkers_All                 .setSelection( config.isGroupDuplicatedMarkers);
      _chkIsLabelAntialiased                 .setSelection( config.isLabelAntialiased);
      _chkIsMarkerClustered                  .setSelection( config.isTourMarkerClustered);
      _chkIsMarkerClustered_All              .setSelection( config.isTourMarkerClustered);
      _chkIsShowBoundingBox_All              .setSelection( config.isShowLocationBoundingBox);
      _chkIsShowBoundingBox_Common           .setSelection( config.isShowLocationBoundingBox);
      _chkIsShowBoundingBox_Tour             .setSelection( config.isShowLocationBoundingBox);
      _chkIsSymbolAntialiased                .setSelection( config.isSymbolAntialiased);
      _chkIsTruncateLabel                    .setSelection( config.isTruncateLabel);
      _chkIsWrapLabel                        .setSelection( config.isWrapLabel);

      _spinnerClusterGrid_Size               .setSelection( config.clusterGridSize);
      _spinnerClusterOutline_Width           .setSelection( config.clusterOutline_Width);
      _spinnerClusterSymbol_Size             .setSelection( config.clusterSymbol_Size);
      _spinnerLabelDistributorMaxLabels      .setSelection( config.labelDistributorMaxLabels);
      _spinnerLabelDistributorRadius         .setSelection( config.labelDistributorRadius);
      _spinnerLabelGroupGridSize             .setSelection( config.groupGridSize);
      _spinnerLabelTruncateLength            .setSelection( config.labelTruncateLength);
      _spinnerLabelWrapLength                .setSelection( config.labelWrapLength);

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

      _txtGroupDuplicatedMarkers             .setText(      config.groupedMarkers);

      /*
       * Map dimming & transparency
       */
      _chkIsDimMap               .setSelection(    Util.getStateBoolean(_state_Map2,  Map2View.STATE_IS_MAP_DIMMED,                       Map2View.STATE_IS_MAP_DIMMED_DEFAULT));
      _chkUseMapDimColor         .setSelection(    Util.getStateBoolean(_state_Map2,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR_DEFAULT));
      _colorMapDimColor          .setColorValue(   Util.getStateRGB(    _state_Map2,  Map2View.STATE_DIM_MAP_COLOR,                       Map2View.STATE_DIM_MAP_COLOR_DEFAULT));
      _colorMapTransparencyColor .setColorValue(   Util.getStateRGB(    _state_Map2,  Map2View.STATE_MAP_TRANSPARENCY_COLOR,              Map2View.STATE_MAP_TRANSPARENCY_COLOR_DEFAULT));
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

// SET_FORMATTING_ON

      _tourPausesUI.restoreState();

      selectMarkerLabelLayout(config.labelLayout);

      updateUI_TabLabel();
      updateUI_ExpandCollapse();
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

      config.isGroupDuplicatedMarkers     = _chkIsGroupMarkers                   .getSelection();
      config.groupGridSize                = _spinnerLabelGroupGridSize           .getSelection();
      config.groupedMarkers               = _txtGroupDuplicatedMarkers           .getText();

      config.isLabelAntialiased           = _chkIsLabelAntialiased               .getSelection();
      config.isSymbolAntialiased          = _chkIsSymbolAntialiased              .getSelection();

      config.isFillClusterSymbol          = _chkIsFillClusterSymbol              .getSelection();
      config.isTourMarkerClustered        = _chkIsMarkerClustered                .getSelection();

      config.clusterGridSize              = _spinnerClusterGrid_Size             .getSelection();
      config.clusterOutline_Width         = _spinnerClusterOutline_Width         .getSelection();
      config.clusterSymbol_Size           = _spinnerClusterSymbol_Size           .getSelection();
      config.clusterFill_RGB              = _colorClusterSymbol_Fill             .getColorValue();
      config.clusterOutline_RGB           = _colorClusterSymbol_Outline          .getColorValue();

      config.isTruncateLabel              = _chkIsTruncateLabel                  .getSelection();
      config.isWrapLabel                  = _chkIsWrapLabel                      .getSelection();
      config.labelDistributorMaxLabels    = _spinnerLabelDistributorMaxLabels    .getSelection();
      config.labelDistributorRadius       = _spinnerLabelDistributorRadius       .getSelection();
      config.labelTruncateLength          = _spinnerLabelTruncateLength          .getSelection();
      config.labelWrapLength              = _spinnerLabelWrapLength              .getSelection();

      config.labelLayout                  = getSelectedMarkerLabelLayout();

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


      config.setupColors();

      /*
       * Map dimming & transparency
       */
      _state_Map2.put(Map2View.STATE_IS_MAP_DIMMED,                     _chkIsDimMap               .getSelection());
      _state_Map2.put(Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,_chkUseMapDimColor         .getSelection());

      _state_Map2.put(Map2View.STATE_DIM_MAP_VALUE,                     _spinnerMapDimValue        .getSelection());

      Util.setState(_state_Map2, Map2View.STATE_DIM_MAP_COLOR,          _colorMapDimColor          .getColorValue());
      Util.setState(_state_Map2, Map2View.STATE_MAP_TRANSPARENCY_COLOR, _colorMapTransparencyColor .getColorValue());

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

   private void selectMarkerLabelLayout(final Enum<MapLabelLayout> filterOperator) {

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

      if (stats.numTourMarkers_All_IsTruncated) {
         numTourMarkers_All += UI.SYMBOL_STAR;
      }

      if (stats.numTourPauses_All_IsTruncated) {
         numTourPauses_All += UI.SYMBOL_STAR;
      }

      _lblStats_CommonLocations_All.setText(numCommonLocations_All);
      _lblStats_CommonLocations_Visible.setText(Integer.toString(stats.numCommonLocations_Painted));

      _lblStats_TourLocations_All.setText(numTourLocations_All);
      _lblStats_TourLocations_Visible.setText(Integer.toString(stats.numTourLocations_Painted));

      _lblStats_TourMarkers_All.setText(numTourMarkers_All);
      _lblStats_TourMarkers_Visible.setText(Integer.toString(stats.numTourMarkers_Painted));

      _lblStats_TourPauses_All.setText(numTourPauses_All);
      _lblStats_TourPauses_Visible.setText(Integer.toString(stats.numTourPauses_Painted));

      _statisticsContainer.pack();
   }

   public void updateUI() {

      restoreState();
      enableControls();
   }

   private void updateUI_ExpandCollapse() {

      if (_isSlideoutExpanded) {

         _actionExpandCollapseSlideout.setToolTipText(Messages.Slideout_MapPoints_Action_CollapseSlideout_Tooltip);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutCollapse);

      } else {

         _actionExpandCollapseSlideout.setToolTipText(Messages.Slideout_MapPoints_Action_ExpandSlideout_Tooltip);
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

      _tabCommonLocations  .setText(isShowCommonLocations   ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourLocations    .setText(isShowTourLocations     ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkers      .setText(isShowTourMarkers       ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkerGroups .setText(isShowTourMarkers && isGroupDuplicatedMarkers ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourPauses       .setText(isShowTourPauses        ? UI.SYMBOL_STAR : UI.EMPTY_STRING);

// SET_FORMATTING_ON
   }

}
