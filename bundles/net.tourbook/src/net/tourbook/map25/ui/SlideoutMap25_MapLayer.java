/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25App.SunDayTime;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2.5D properties slideout
 */
public class SlideoutMap25_MapLayer extends ToolbarSlideout {

   /**
    * The zoom level in the UI starts with 1 but internally it starts with 0
    */
   private static final int   ZOOM_UI_OFFSET = 1;

   private SelectionListener  _defaultSelectionListener;
   private SelectionListener  _layerSelectionListener;
   private MouseWheelListener _mouseWheelListener;
   private FocusListener      _keepOpenListener;

   private Map25App           _mapApp;

   private Action_ResetValue  _actionResetValue_SunTime_Hour;
   private Action_ResetValue  _actionResetValue_SunTime_Minute;

   private PixelConverter     _pc;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkShowLayer_Cartography;
   private Button    _chkShowLayer_Building;
   private Button    _chkShowLayer_Building_Shadow;
   private Button    _chkShowLayer_Hillshading;
   private Button    _chkShowLayer_Satellite;
   private Button    _chkShowLayer_Label;
   private Button    _chkShowLayer_Label_IsBeforeBuilding;
   private Button    _chkShowLayer_Scale;
   private Button    _chkShowLayer_TileInfo;
   private Button    _chkUseDraggedKeyboardNavigation;

   private Combo     _comboBuilding_SunPosition;

   private Label     _lblBuilding_MinZoomLevel;
   private Label     _lblBuilding_SunPosition;

   private Spinner   _spinnerBuilding_MinZoomLevel;
   private Spinner   _spinnerBuilding_SunTime_Hour;
   private Spinner   _spinnerBuilding_SunTime_Minute;
   private Spinner   _spinnerHillShadingOpacity;

   /**
    * Reset spinner value
    */
   private class Action_ResetValue extends Action {

      private Spinner _spinner;

      public Action_ResetValue(final Spinner spinner) {

         super(UI.RESET_LABEL, AS_PUSH_BUTTON);

         setToolTipText(Messages.App_Action_ResetValue_Tooltip);

         _spinner = spinner;
      }

      @Override
      public void run() {

         onResetValue(_spinner);
      }
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_MapLayer(final Control ownerControl,
                                 final ToolBar toolBar,
                                 final Map25View map25View) {

      super(ownerControl, toolBar);

      _mapApp = map25View.getMapApp();
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      fillUI();
      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory
               .fillDefaults()//
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);

            createUI_50_Layer(container);
            createUI_80_Other(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25MapOptions_Label_MapOptions);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_50_Layer(final Composite parent) {

      final GridDataFactory indentGridData = GridDataFactory.fillDefaults().grab(true, false).indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_Map25MapOptions_Group_MapLayer);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         {
            /*
             * Text label
             */
            _chkShowLayer_Label = new Button(group, SWT.CHECK);
            _chkShowLayer_Label.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_LabelSymbol);
            _chkShowLayer_Label.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Text label: Behind building
             */
            _chkShowLayer_Label_IsBeforeBuilding = new Button(group, SWT.CHECK);
            _chkShowLayer_Label_IsBeforeBuilding.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_LabelSymbol_IsBehindBuilding);
            _chkShowLayer_Label_IsBeforeBuilding.addSelectionListener(_layerSelectionListener);
            indentGridData.applyTo(_chkShowLayer_Label_IsBeforeBuilding);
         }
         {
            /*
             * Building
             */
            _chkShowLayer_Building = new Button(group, SWT.CHECK);
            _chkShowLayer_Building.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_3DBuilding);
            _chkShowLayer_Building.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Building: Min zoom level
             */
            final Composite containerBuilding = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(containerBuilding);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBuilding);
            {
               final String tooltipText = Messages.Slideout_Map25MapOptions_Label_BuildingMinZoomLevel_Tooltip;
               {
                  _lblBuilding_MinZoomLevel = new Label(containerBuilding, SWT.NONE);
                  _lblBuilding_MinZoomLevel.setText(Messages.Slideout_Map25MapOptions_Label_BuildingMinZoomLevel);
                  _lblBuilding_MinZoomLevel.setToolTipText(tooltipText);
                  GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblBuilding_MinZoomLevel);
               }
               {
                  _spinnerBuilding_MinZoomLevel = new Spinner(containerBuilding, SWT.BORDER);
                  _spinnerBuilding_MinZoomLevel.setMinimum(11 + ZOOM_UI_OFFSET);
                  _spinnerBuilding_MinZoomLevel.setMaximum(17 + ZOOM_UI_OFFSET);
                  _spinnerBuilding_MinZoomLevel.setIncrement(1);
                  _spinnerBuilding_MinZoomLevel.setPageIncrement(1);
                  _spinnerBuilding_MinZoomLevel.setToolTipText(tooltipText);

                  _spinnerBuilding_MinZoomLevel.addSelectionListener(_layerSelectionListener);
                  _spinnerBuilding_MinZoomLevel.addMouseWheelListener(_mouseWheelListener);
               }
            }
         }
         {
            /*
             * Building: Shadow
             */
            _chkShowLayer_Building_Shadow = new Button(group, SWT.CHECK);
            _chkShowLayer_Building_Shadow.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Building_IsShowShadow);
            _chkShowLayer_Building_Shadow.addSelectionListener(_layerSelectionListener);
            indentGridData.applyTo(_chkShowLayer_Building_Shadow);
         }
         {
            /*
             * Sun position
             */
            final Composite containerSunPosition = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).indent(UI.FORM_FIRST_COLUMN_INDENT * 2, 0).applyTo(containerSunPosition);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerSunPosition);
            {
               {
                  // label
                  _lblBuilding_SunPosition = new Label(containerSunPosition, SWT.NONE);
                  _lblBuilding_SunPosition.setText(Messages.Slideout_Map25MapOptions_Label_BuildingSunPosition);
                  GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblBuilding_SunPosition);
               }
               {
                  // combo
                  _comboBuilding_SunPosition = new Combo(containerSunPosition, SWT.READ_ONLY | SWT.BORDER);
                  _comboBuilding_SunPosition.setVisibleItemCount(2);
                  _comboBuilding_SunPosition.addFocusListener(_keepOpenListener);
                  _comboBuilding_SunPosition.addSelectionListener(_layerSelectionListener);
                  GridDataFactory.fillDefaults()
//                        .grab(true, false)
                        .align(SWT.BEGINNING, SWT.CENTER).applyTo(_comboBuilding_SunPosition);
               }
               final Composite containerSunTime = new Composite(containerSunPosition, SWT.NONE);
               GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(containerSunTime);
               GridLayoutFactory.fillDefaults().numColumns(4).applyTo(containerSunTime);
               {
                  {
                     // select sun time hours
                     _spinnerBuilding_SunTime_Hour = new Spinner(containerSunTime, SWT.BORDER);
                     _spinnerBuilding_SunTime_Hour.setMinimum(0);
                     _spinnerBuilding_SunTime_Hour.setMaximum((int) (2 * Map25App.SUN_TIME_HOURS * 2));
                     _spinnerBuilding_SunTime_Hour.setIncrement(1);
                     _spinnerBuilding_SunTime_Hour.setPageIncrement((int) (Map25App.SUN_TIME_HOURS / 10));
                     _spinnerBuilding_SunTime_Hour.addSelectionListener(_layerSelectionListener);
                     _spinnerBuilding_SunTime_Hour.addMouseWheelListener(_mouseWheelListener);

                     _actionResetValue_SunTime_Hour = createUI_Action_ResetValue(containerSunTime, _spinnerBuilding_SunTime_Hour);
                  }
                  {
                     // select sun time minutes
                     _spinnerBuilding_SunTime_Minute = new Spinner(containerSunTime, SWT.BORDER);
                     _spinnerBuilding_SunTime_Minute.setMinimum((int) -Map25App.SUN_TIME_MINUTES);
                     _spinnerBuilding_SunTime_Minute.setMaximum((int) Map25App.SUN_TIME_MINUTES);
                     _spinnerBuilding_SunTime_Minute.setIncrement(1);
                     _spinnerBuilding_SunTime_Minute.setPageIncrement(10);
                     _spinnerBuilding_SunTime_Minute.addSelectionListener(_layerSelectionListener);
                     _spinnerBuilding_SunTime_Minute.addMouseWheelListener(_mouseWheelListener);

                     _actionResetValue_SunTime_Minute = createUI_Action_ResetValue(containerSunTime, _spinnerBuilding_SunTime_Minute);
                  }

//                  {
//                     // time unit
//                     _lblBuilding_SunDayTime_Unit = new Label(containerSunTime, SWT.NONE);
//                     _lblBuilding_SunDayTime_Unit.setText("h:mm");
//                     GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblBuilding_SunDayTime_Unit);
//                  }
               }
            }
         }
         {
            /*
             * Scale
             */
            _chkShowLayer_Scale = new Button(group, SWT.CHECK);
            _chkShowLayer_Scale.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_ScaleBar);
            _chkShowLayer_Scale.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Cartography / map
             */
            _chkShowLayer_Cartography = new Button(group, SWT.CHECK);
            _chkShowLayer_Cartography.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography);
            _chkShowLayer_Cartography.setToolTipText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography_Tooltip);
            _chkShowLayer_Cartography.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Satellite images
             */
            _chkShowLayer_Satellite = new Button(group, SWT.CHECK);
            _chkShowLayer_Satellite.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Satellite);
            _chkShowLayer_Satellite.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Hillshading
             */
            final Composite containerHillshading = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHillshading);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHillshading);
            {
               {
                  _chkShowLayer_Hillshading = new Button(containerHillshading, SWT.CHECK);
                  _chkShowLayer_Hillshading.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Hillshading);
                  _chkShowLayer_Hillshading.addSelectionListener(_layerSelectionListener);
               }
               {
                  /*
                   * Opacity
                   */

                  final String tooltipText = NLS.bind(Messages.Slideout_Map25MapOptions_Spinner_Layer_Hillshading, UI.TRANSFORM_OPACITY_MAX);

                  _spinnerHillShadingOpacity = new Spinner(containerHillshading, SWT.BORDER);
                  _spinnerHillShadingOpacity.setMinimum(0);
                  _spinnerHillShadingOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
                  _spinnerHillShadingOpacity.setIncrement(1);
                  _spinnerHillShadingOpacity.setPageIncrement(10);
                  _spinnerHillShadingOpacity.setToolTipText(tooltipText);

                  _spinnerHillShadingOpacity.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModify_HillShadingOpacity()));
                  _spinnerHillShadingOpacity.addMouseWheelListener(mouseEvent -> {
                     UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
                     onModify_HillShadingOpacity();
                  });
               }
            }
         }
         {
            /*
             * Tile info
             */
            _chkShowLayer_TileInfo = new Button(group, SWT.CHECK);
            _chkShowLayer_TileInfo.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_TileInfo);
            _chkShowLayer_TileInfo.addSelectionListener(_layerSelectionListener);
         }
      }
   }

   private void createUI_80_Other(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()
            .numColumns(2)
            .applyTo(container);
      {
         {
            /*
             * Keyboard navigation
             */

            // checkbox
            _chkUseDraggedKeyboardNavigation = new Button(container, SWT.CHECK);
            _chkUseDraggedKeyboardNavigation.setText(Messages.Slideout_Map25MapOptions_Checkbox_UseDraggedKeyNavigation);
            _chkUseDraggedKeyboardNavigation.setToolTipText(Messages.Slideout_Map25MapOptions_Checkbox_UseDraggedKeyNavigation_Tooltip);
            _chkUseDraggedKeyboardNavigation.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .span(2, 1)
                  .applyTo(_chkUseDraggedKeyboardNavigation);
         }
      }
   }

   private Action_ResetValue createUI_Action_ResetValue(final Composite parent, final Spinner spinner) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      final Action_ResetValue action = new Action_ResetValue(spinner);

      tbm.add(action);

      tbm.update(true);

      return action;
   }

   private void enableControls() {

      final boolean isHillShadingVisible = _chkShowLayer_Hillshading.getSelection();
      final boolean isLabelVisible = _chkShowLayer_Label.getSelection();

      final boolean isBuilding_Visible = _chkShowLayer_Building.getSelection();
      final boolean isBuilding_ShowShadow = _chkShowLayer_Building_Shadow.getSelection() && isBuilding_Visible;

      final SunDayTime selectedSunDayTime = getSelectedSunDayTime();
      final boolean isBuilding_SunRiseDownTime = (selectedSunDayTime.equals(SunDayTime.DAY_TIME)
            || selectedSunDayTime.equals(SunDayTime.NIGHT_TIME))
            && isBuilding_ShowShadow;

      _chkShowLayer_Building_Shadow.setEnabled(isBuilding_Visible);
      _chkShowLayer_Label_IsBeforeBuilding.setEnabled(isLabelVisible && isBuilding_Visible);

      _comboBuilding_SunPosition.setEnabled(isBuilding_ShowShadow);

      _lblBuilding_MinZoomLevel.setEnabled(isBuilding_Visible);
      _lblBuilding_SunPosition.setEnabled(isBuilding_ShowShadow);

      _spinnerHillShadingOpacity.setEnabled(isHillShadingVisible);
      _spinnerBuilding_MinZoomLevel.setEnabled(isBuilding_Visible);
      _spinnerBuilding_SunTime_Hour.setEnabled(isBuilding_SunRiseDownTime);
      _spinnerBuilding_SunTime_Minute.setEnabled(isBuilding_SunRiseDownTime);

      // force UI update otherwise the slideout UI update is done after the map is updated
      _parent.update();
   }

   private void fillUI() {

      // must be the same sorting as SunDayTime
      _comboBuilding_SunPosition.add(Messages.Slideout_Map25MapOptions_Combo_SunPosition_CurrentTime);
      _comboBuilding_SunPosition.add(Messages.Slideout_Map25MapOptions_Combo_SunPosition_DayTime);
      _comboBuilding_SunPosition.add(Messages.Slideout_Map25MapOptions_Combo_SunPosition_NightTime);
   }

   private SunDayTime getSelectedSunDayTime() {

      switch (_comboBuilding_SunPosition.getSelectionIndex()) {
      case 1:
         return SunDayTime.DAY_TIME;
      case 2:
         return SunDayTime.NIGHT_TIME;

      case 0:
      default:
         return SunDayTime.CURRENT_TIME;
      }
   }

   private int getSunPositionIndex() {

      switch (_mapApp.getLayer_Building_SunDayTime()) {
      case DAY_TIME:
         return 1;
      case NIGHT_TIME:
         return 2;

      case CURRENT_TIME:
      default:
         return 0;
      }
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
      _layerSelectionListener = widgetSelectedAdapter(selectionEvent -> onModify_Layer());

      _mouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onModify_Layer();
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

   private void onChangeUI() {

      saveState();

      enableControls();
   }

   private void onModify_HillShadingOpacity() {

      // updade model
      final int hillShadingOpacity = UI.transformOpacity_WhenSaved(_spinnerHillShadingOpacity.getSelection());
      _mapApp.setLayer_HillShading_Options(hillShadingOpacity);

      // update UI
      final float hillshadingAlpha = hillShadingOpacity / 255f;
      _mapApp.getLayer_HillShading().setBitmapAlpha(hillshadingAlpha, true);

      enableControls();

      _mapApp.updateMap();
   }

   private void onModify_Layer() {

      saveState_Layer();

      enableControls();

      _mapApp.updateLayer();
   }

   private void onResetValue(final Spinner spinner) {

      spinner.setSelection(0);
      spinner.setFocus();

      onModify_Layer();
   }

   private void restoreState() {

      final int sunDayTime = _mapApp.getLayer_Building_SunRiseDownTime();
      final int sunTimeHours = (int) (sunDayTime / Map25App.SUN_TIME_MINUTES);
      final int sunTimeMinutes = (int) (sunDayTime % Map25App.SUN_TIME_MINUTES);

// SET_FORMATTING_OFF

      _chkShowLayer_Building              .setSelection(_mapApp.getLayer_Building_VARYING()         .isEnabled());
      _chkShowLayer_Building_Shadow       .setSelection(_mapApp.getLayer_Building_IsShadow());
      _chkShowLayer_Cartography           .setSelection(_mapApp.getLayer_BaseMap()                  .isEnabled());
      _chkShowLayer_Hillshading           .setSelection(_mapApp.getLayer_HillShading()              .isEnabled());
      _chkShowLayer_Label                 .setSelection(_mapApp.getLayer_Label()                    .isEnabled());
      _chkShowLayer_Label_IsBeforeBuilding.setSelection(_mapApp.getLayer_Label_IsBeforeBuilding());
      _chkShowLayer_Scale                 .setSelection(_mapApp.getLayer_ScaleBar()                 .isEnabled());
      _chkShowLayer_Satellite             .setSelection(_mapApp.getLayer_Satellite()                .isEnabled());
      _chkShowLayer_TileInfo              .setSelection(_mapApp.getLayer_TileInfo()                 .isEnabled());

      _spinnerBuilding_MinZoomLevel       .setSelection(_mapApp.getLayer_Building_MinZoomLevel() + ZOOM_UI_OFFSET);
      _spinnerBuilding_SunTime_Hour       .setSelection(sunTimeHours);
      _spinnerBuilding_SunTime_Minute     .setSelection(sunTimeMinutes);
      _spinnerHillShadingOpacity          .setSelection(UI.transformOpacity_WhenRestored(_mapApp.getLayer_HillShading_Opacity()));

      _comboBuilding_SunPosition          .select(getSunPositionIndex());

// SET_FORMATTING_ON

      _chkUseDraggedKeyboardNavigation.setSelection(Map25ConfigManager.useDraggedKeyboardNavigation);
   }

   private void saveState() {

      Map25ConfigManager.useDraggedKeyboardNavigation = _chkUseDraggedKeyboardNavigation.getSelection();
   }

   private void saveState_Layer() {

      _mapApp.getLayer_BaseMap().setEnabled(_chkShowLayer_Cartography.getSelection());
      _mapApp.getLayer_HillShading().setEnabled(_chkShowLayer_Hillshading.getSelection());

      // satellite maps
      _mapApp.getLayer_Satellite().setEnabled(_chkShowLayer_Satellite.getSelection());

      _mapApp.getLayer_Label().setEnabled(_chkShowLayer_Label.getSelection());
      _mapApp.getLayer_ScaleBar().setEnabled(_chkShowLayer_Scale.getSelection());
      _mapApp.getLayer_TileInfo().setEnabled(_chkShowLayer_TileInfo.getSelection());

      _mapApp.setLayer_Label_Options(
            _chkShowLayer_Label.getSelection(),
            _chkShowLayer_Label_IsBeforeBuilding.getSelection());

      final int sunTimeHours = _spinnerBuilding_SunTime_Hour.getSelection();
      final int sunTimeMinutes = _spinnerBuilding_SunTime_Minute.getSelection();
      final float sunRiseDownTime = (sunTimeHours * Map25App.SUN_TIME_MINUTES + sunTimeMinutes);

      final SunDayTime sunDayTime = getSelectedSunDayTime();

      _mapApp.setLayer_Building_Options(
            _chkShowLayer_Building.getSelection(),
            _spinnerBuilding_MinZoomLevel.getSelection() - ZOOM_UI_OFFSET,
            _chkShowLayer_Building_Shadow.getSelection(),
            sunDayTime,
            sunRiseDownTime);
   }

}
