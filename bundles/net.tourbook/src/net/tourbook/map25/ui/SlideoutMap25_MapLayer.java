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

import java.time.Duration;
import java.time.LocalDateTime;

import net.tourbook.Messages;
import net.tourbook.common.Bool;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25App.SunDayTime;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.oscim.renderer.light.Sun;

/**
 * Map 2.5D layer slideout
 */
public class SlideoutMap25_MapLayer extends ToolbarSlideout {

   /**
    * The zoom level in the UI starts with 1 but internally it starts with 0
    */
   private static final int   ZOOM_UI_OFFSET = 1;

   private SelectionListener  _layerSelectionListener;
   private MouseWheelListener _mouseWheelListener;
   private FocusListener      _keepOpenListener;

   private Map25App           _mapApp;

   private Action_ResetValue  _actionResetValue_SunTime_Coarse;
   private Action_ResetValue  _actionResetValue_SunTime_Fine;

   private LocalDateTime      _sunriseTime;
   private LocalDateTime      _sunsetTime;

   /*
    * UI controls
    */
   private Composite         _parent;

   private ControlDecoration _decoratorShowLayer_Building;
   private ControlDecoration _decoratorShowLayer_Label;

   private Button            _chkShowLayer_Cartography;
   private Button            _chkShowLayer_Building;
   private Button            _chkShowLayer_Building_Shadow;
   private Button            _chkShowLayer_Hillshading;
   private Button            _chkShowLayer_Satellite;
   private Button            _chkShowLayer_Label;
   private Button            _chkShowLayer_Label_IsBeforeBuilding;
   private Button            _chkShowLayer_Scale;
   private Button            _chkShowLayer_TileInfo;

   private Combo             _comboBuilding_SunPosition;

   private Label             _lblBuilding_MinZoomLevel;
   private Label             _lblBuilding_SelectedSunTime;
   private Label             _lblBuilding_SunPosition;
   private Label             _lblBuilding_Sunrise;
   private Label             _lblBuilding_Sunset;
   private Label             _lblBuilding_SunTime;

   private Scale             _scaleBuilding_SunTime;

   private Spinner           _spinnerBuilding_MinZoomLevel;
   private Spinner           _spinnerBuilding_SunTime_Coarse;
   private Spinner           _spinnerBuilding_SunTime_Fine;
   private Spinner           _spinnerHillShadingOpacity;

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

      /*
       * Is is possible that building/shadow is displayed or hidden, update current sun position in
       * the UI
       */
      _parent.getDisplay().asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         setSunPosition();
         showSelectedSunPosition();
      });

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .applyTo(container);
//       container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_50_Layer(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25Layer_Label_MapLayer);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_50_Layer(final Composite parent) {

      final GridDataFactory indentGridData = GridDataFactory.fillDefaults().grab(true, false).indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//      GridLayoutFactory.fillDefaults().spacing(5, 5).numColumns(1).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Text label
             */
            _chkShowLayer_Label = new Button(container, SWT.CHECK);
            _chkShowLayer_Label.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_LabelSymbol);
            _chkShowLayer_Label.addSelectionListener(_layerSelectionListener);

            _decoratorShowLayer_Label = new ControlDecoration(_chkShowLayer_Label, SWT.TOP | SWT.LEFT);

         }
         {
            /*
             * Text label: Behind building
             */
            _chkShowLayer_Label_IsBeforeBuilding = new Button(container, SWT.CHECK);
            _chkShowLayer_Label_IsBeforeBuilding.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_LabelSymbol_IsBehindBuilding);
            _chkShowLayer_Label_IsBeforeBuilding.addSelectionListener(_layerSelectionListener);
            indentGridData.applyTo(_chkShowLayer_Label_IsBeforeBuilding);
         }

         createUI_60_Building(container);

         {
            /*
             * Scale
             */
            _chkShowLayer_Scale = new Button(container, SWT.CHECK);
            _chkShowLayer_Scale.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_ScaleBar);
            _chkShowLayer_Scale.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Cartography / map
             */
            _chkShowLayer_Cartography = new Button(container, SWT.CHECK);
            _chkShowLayer_Cartography.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_Cartography);
            _chkShowLayer_Cartography.setToolTipText(Messages.Slideout_Map25Layer_Checkbox_Layer_Cartography_Tooltip);
            _chkShowLayer_Cartography.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Satellite images
             */
            _chkShowLayer_Satellite = new Button(container, SWT.CHECK);
            _chkShowLayer_Satellite.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_Satellite);
            _chkShowLayer_Satellite.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Hillshading
             */
            final Composite containerHillshading = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHillshading);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHillshading);
            {
               {
                  _chkShowLayer_Hillshading = new Button(containerHillshading, SWT.CHECK);
                  _chkShowLayer_Hillshading.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_Hillshading);
                  _chkShowLayer_Hillshading.addSelectionListener(_layerSelectionListener);
               }
               {
                  /*
                   * Opacity
                   */

                  final String tooltipText = NLS.bind(Messages.Slideout_Map25Layer_Spinner_Layer_Hillshading, UI.TRANSFORM_OPACITY_MAX);

                  _spinnerHillShadingOpacity = new Spinner(containerHillshading, SWT.BORDER);
                  _spinnerHillShadingOpacity.setMinimum(0);
                  _spinnerHillShadingOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
                  _spinnerHillShadingOpacity.setIncrement(1);
                  _spinnerHillShadingOpacity.setPageIncrement(UI.TRANSFORM_OPACITY_MAX / 10);
                  _spinnerHillShadingOpacity.setToolTipText(tooltipText);
                  _spinnerHillShadingOpacity.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModify_HillShadingOpacity()));
                  _spinnerHillShadingOpacity.addMouseWheelListener(mouseEvent -> {
                     UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
                     onModify_HillShadingOpacity();
                  });
                  GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(_spinnerHillShadingOpacity);
               }
            }
         }
         {
            /*
             * Tile info
             */
            _chkShowLayer_TileInfo = new Button(container, SWT.CHECK);
            _chkShowLayer_TileInfo.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_TileInfo);
            _chkShowLayer_TileInfo.addSelectionListener(_layerSelectionListener);
         }
      }
   }

   private void createUI_60_Building(final Composite parent) {

      final GridDataFactory centerGridData = GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER);
      final GridDataFactory indentGridData = GridDataFactory.fillDefaults().grab(true, false).indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      final SelectionListener sunTimeSelectionListener = widgetSelectedAdapter(selectionEvent -> onModify_SunTime(selectionEvent.widget));

      final MouseWheelListener sunTimeMouseWheelListenerSpinner = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onModify_SunTime(mouseEvent.widget);
      };

      final MouseWheelListener sunTimeMouseWheelListenerScale = mouseEvent -> {
         UI.adjustScaleValueOnMouseScroll(mouseEvent);
         onModify_SunTime(mouseEvent.widget);
      };
      {
         /*
          * Building
          */
         _chkShowLayer_Building = new Button(parent, SWT.CHECK);
         _chkShowLayer_Building.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_3DBuilding);
         _chkShowLayer_Building.addSelectionListener(_layerSelectionListener);

         _decoratorShowLayer_Building = new ControlDecoration(_chkShowLayer_Building, SWT.TOP | SWT.LEFT);
      }
      {
         /*
          * Building: Min zoom level
          */
         final Composite containerBuilding = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(containerBuilding);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBuilding);
         {
            final String tooltipText = Messages.Slideout_Map25Layer_Label_BuildingMinZoomLevel_Tooltip;
            {
               _lblBuilding_MinZoomLevel = new Label(containerBuilding, SWT.NONE);
               _lblBuilding_MinZoomLevel.setText(Messages.Slideout_Map25Layer_Label_BuildingMinZoomLevel);
               _lblBuilding_MinZoomLevel.setToolTipText(tooltipText);
               centerGridData.applyTo(_lblBuilding_MinZoomLevel);
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
         _chkShowLayer_Building_Shadow = new Button(parent, SWT.CHECK);
         _chkShowLayer_Building_Shadow.setText(Messages.Slideout_Map25Layer_Checkbox_Layer_Building_IsShowShadow);
         _chkShowLayer_Building_Shadow.addSelectionListener(_layerSelectionListener);
         indentGridData.applyTo(_chkShowLayer_Building_Shadow);
      }
      {
         /*
          * Sun position
          */
         final Composite containerSunPosition = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).indent(UI.FORM_FIRST_COLUMN_INDENT * 2, 0).applyTo(containerSunPosition);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerSunPosition);
//         containerSunPosition.setBackground(UI.SYS_COLOR_DARK_GREEN);
         {
            {
               // label
               _lblBuilding_SunPosition = new Label(containerSunPosition, SWT.NONE);
               _lblBuilding_SunPosition.setText(Messages.Slideout_Map25Layer_Label_SunPosition);
               centerGridData.applyTo(_lblBuilding_SunPosition);
            }
            {
               // combo
               _comboBuilding_SunPosition = new Combo(containerSunPosition, SWT.READ_ONLY | SWT.BORDER);
               _comboBuilding_SunPosition.setVisibleItemCount(2);
               _comboBuilding_SunPosition.addFocusListener(_keepOpenListener);
               _comboBuilding_SunPosition.addSelectionListener(_layerSelectionListener);
               GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(_comboBuilding_SunPosition);
            }
            final Composite containerSunTime = new Composite(containerSunPosition, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(containerSunTime);
            GridLayoutFactory.fillDefaults().numColumns(5).applyTo(containerSunTime);
//            containerSunTime.setBackground(UI.SYS_COLOR_MAGENTA);
            {
               UI.createSpacer_Horizontal(containerSunTime, 1);

               final Composite containerSunriseSunset = new Composite(containerSunTime, SWT.NONE);
               GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(containerSunriseSunset);
               GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerSunriseSunset);
//               containerSunriseSunset.setBackground(UI.SYS_COLOR_BLUE);
               {
                  {
                     /*
                      * Sunrise time
                      */
                     _lblBuilding_Sunrise = new Label(containerSunriseSunset, SWT.NONE);
                     _lblBuilding_Sunrise.setText(UI.SPACE1);
                     _lblBuilding_Sunrise.setToolTipText(Messages.Slideout_Map25Layer_Label_Sunrise_Tooltip);
                     GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblBuilding_Sunrise);
                  }
                  {
                     /*
                      * Selected sun time
                      */
                     _lblBuilding_SelectedSunTime = new Label(containerSunriseSunset, SWT.NONE);
                     _lblBuilding_SelectedSunTime.setText(UI.SPACE1);
                     _lblBuilding_SelectedSunTime.setToolTipText(Messages.Slideout_Map25Layer_Label_SelectedSunTime_Tooltip);
                     GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(_lblBuilding_SelectedSunTime);
                  }
                  {
                     /*
                      * Sunset time
                      */
                     _lblBuilding_Sunset = new Label(containerSunriseSunset, SWT.NONE);
                     _lblBuilding_Sunset.setText(UI.SPACE1);
                     _lblBuilding_Sunset.setToolTipText(Messages.Slideout_Map25Layer_Label_Sunset_Tooltip);
                     GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(_lblBuilding_Sunset);
                  }
               }

               {

                  /*
                   * Sun time
                   */
                  _lblBuilding_SunTime = new Label(containerSunTime, SWT.NONE);
                  _lblBuilding_SunTime.setText(Messages.Slideout_Map25Layer_Label_SunTime);
                  _lblBuilding_SunTime.setToolTipText(Messages.Slideout_Map25Layer_Label_SunTime_Tooltip);

                  _scaleBuilding_SunTime = new Scale(containerSunTime, SWT.NONE);
                  _scaleBuilding_SunTime.setMaximum(100);
                  _scaleBuilding_SunTime.setToolTipText(Messages.Slideout_Map25Layer_Label_SunTime_Tooltip);
                  _scaleBuilding_SunTime.addSelectionListener(sunTimeSelectionListener);
                  _scaleBuilding_SunTime.addMouseWheelListener(sunTimeMouseWheelListenerScale);
                  GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(_scaleBuilding_SunTime);
               }

               UI.createSpacer_Horizontal(containerSunTime, 1);
               {
                  /*
                   * Spinner for the sun raw time
                   */
                  _spinnerBuilding_SunTime_Coarse = new Spinner(containerSunTime, SWT.BORDER);
                  _spinnerBuilding_SunTime_Coarse.setMinimum(-200);
                  _spinnerBuilding_SunTime_Coarse.setMaximum(200);
                  _spinnerBuilding_SunTime_Coarse.setIncrement(1);
                  _spinnerBuilding_SunTime_Coarse.setPageIncrement(10);
                  _spinnerBuilding_SunTime_Coarse.setToolTipText(Messages.Slideout_Map25Layer_Spinner_SunTime_Coarse);
                  _spinnerBuilding_SunTime_Coarse.addSelectionListener(sunTimeSelectionListener);
                  _spinnerBuilding_SunTime_Coarse.addMouseWheelListener(sunTimeMouseWheelListenerSpinner);

                  _actionResetValue_SunTime_Coarse = createUI_Action_ResetValue(containerSunTime, _spinnerBuilding_SunTime_Coarse);
               }
               {
                  /*
                   * Spinner for the sun detail time
                   */
                  _spinnerBuilding_SunTime_Fine = new Spinner(containerSunTime, SWT.BORDER);
                  _spinnerBuilding_SunTime_Fine.setMinimum(-200);
                  _spinnerBuilding_SunTime_Fine.setMaximum(200);
                  _spinnerBuilding_SunTime_Fine.setIncrement(1);
                  _spinnerBuilding_SunTime_Fine.setPageIncrement(10);
                  _spinnerBuilding_SunTime_Fine.setToolTipText(Messages.Slideout_Map25Layer_Spinner_SunTime_Fine);
                  _spinnerBuilding_SunTime_Fine.addSelectionListener(sunTimeSelectionListener);
                  _spinnerBuilding_SunTime_Fine.addMouseWheelListener(sunTimeMouseWheelListenerSpinner);
                  GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(_spinnerBuilding_SunTime_Fine);

                  _actionResetValue_SunTime_Fine = createUI_Action_ResetValue(containerSunTime, _spinnerBuilding_SunTime_Fine);
               }
            }
         }
      }
   }

   private Action_ResetValue createUI_Action_ResetValue(final Composite parent, final Spinner spinner) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().indent(-5, 0).applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      final Action_ResetValue action = new Action_ResetValue(spinner);

      tbm.add(action);

      tbm.update(true);

      return action;
   }

   private void enableControls() {

      final boolean isCartographyVisible = _chkShowLayer_Cartography.getSelection();
      final boolean isHillShadingVisible = _chkShowLayer_Hillshading.getSelection();
      final boolean isLabelVisible = _chkShowLayer_Label.getSelection();

      final boolean isBuilding_Visible = _chkShowLayer_Building.getSelection();
      final boolean isBuilding_ShowShadow = _chkShowLayer_Building_Shadow.getSelection() && isBuilding_Visible;

      final SunDayTime selectedSunDayTime = getSelected_SunDayTime();
      final boolean isBuilding_Sunrise_Sunset_Time = (selectedSunDayTime.equals(SunDayTime.DAY_TIME)
            || selectedSunDayTime.equals(SunDayTime.NIGHT_TIME))
            && isBuilding_ShowShadow;

      _actionResetValue_SunTime_Coarse.setEnabled(isBuilding_Sunrise_Sunset_Time);
      _actionResetValue_SunTime_Fine.setEnabled(isBuilding_Sunrise_Sunset_Time);

      _chkShowLayer_Building_Shadow.setEnabled(isBuilding_Visible);
      _chkShowLayer_Label_IsBeforeBuilding.setEnabled(isLabelVisible && isBuilding_Visible);

      _comboBuilding_SunPosition.setEnabled(isBuilding_ShowShadow);

      _lblBuilding_MinZoomLevel.setEnabled(isBuilding_Visible);
      _lblBuilding_SunPosition.setEnabled(isBuilding_ShowShadow);
      _lblBuilding_Sunrise.setEnabled(isBuilding_Sunrise_Sunset_Time);
      _lblBuilding_Sunset.setEnabled(isBuilding_Sunrise_Sunset_Time);
      _lblBuilding_SunTime.setEnabled(isBuilding_Sunrise_Sunset_Time);
      _lblBuilding_SelectedSunTime.setEnabled(isBuilding_Sunrise_Sunset_Time);

      _scaleBuilding_SunTime.setEnabled(isBuilding_Sunrise_Sunset_Time);

      _spinnerHillShadingOpacity.setEnabled(isHillShadingVisible);
      _spinnerBuilding_MinZoomLevel.setEnabled(isBuilding_Visible);
      _spinnerBuilding_SunTime_Coarse.setEnabled(isBuilding_Sunrise_Sunset_Time);
      _spinnerBuilding_SunTime_Fine.setEnabled(isBuilding_Sunrise_Sunset_Time);

      if (isCartographyVisible) {

         _decoratorShowLayer_Building.hide();
         _decoratorShowLayer_Label.hide();

      } else {

         final Image decorationImage = FieldDecorationRegistry.getDefault()
               .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING)
               .getImage();

         _decoratorShowLayer_Building.setDescriptionText(Messages.Slideout_Map25Layer_Decorator_Cartography);
         _decoratorShowLayer_Building.setImage(decorationImage);
         _decoratorShowLayer_Building.show();

         _decoratorShowLayer_Label.setDescriptionText(Messages.Slideout_Map25Layer_Decorator_Cartography);
         _decoratorShowLayer_Label.setImage(decorationImage);
         _decoratorShowLayer_Label.show();
      }

      // force UI update otherwise the slideout UI update is done after the map is updated
      _parent.update();
   }

   private void fillUI() {

      // must have the same sorting as net.tourbook.map25.Map25App.SunDayTime

      _comboBuilding_SunPosition.add(Messages.Slideout_Map25Layer_Combo_SunPosition_CurrentTime);
      _comboBuilding_SunPosition.add(Messages.Slideout_Map25Layer_Combo_SunPosition_DayTime);
      _comboBuilding_SunPosition.add(Messages.Slideout_Map25Layer_Combo_SunPosition_NightTime);
   }

   private SunDayTime getSelected_SunDayTime() {

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

   private float getSelected_Sunrise_Sunset_Time(final boolean isUseScaleValue) {

      final int sunTime_Fine = _spinnerBuilding_SunTime_Fine.getSelection();

      int selectedSunTime_Coarse = 0;

      if (isUseScaleValue) {

         selectedSunTime_Coarse = _scaleBuilding_SunTime.getSelection();

         /*
          * Set scale value into spinner
          */
         _spinnerBuilding_SunTime_Coarse.setSelection(selectedSunTime_Coarse);
         _spinnerBuilding_SunTime_Fine.setSelection(0);

      } else {

         selectedSunTime_Coarse = _spinnerBuilding_SunTime_Coarse.getSelection();

         /*
          * Set spinner value into scale
          */
         _scaleBuilding_SunTime.setSelection(selectedSunTime_Coarse);
      }

      final float sunTime = selectedSunTime_Coarse + (sunTime_Fine / 100.0f);
      final float sunrise_Sunset_Time = sunTime / 100;

      return sunrise_Sunset_Time;
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

      saveState();

      enableControls();

      _mapApp.updateLayer();
   }

   private void onModify_SunTime(final Widget widget) {

      saveState_SunTime(widget);

      _mapApp.updateMap();

      showSelectedSunPosition();
   }

   private void onResetValue(final Spinner spinner) {

      spinner.setSelection(0);
      spinner.setFocus();

      onModify_SunTime(spinner);
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkShowLayer_Building                 .setSelection(_mapApp.getLayer_Building_S3DB()              .isEnabled());
      _chkShowLayer_Building_Shadow          .setSelection(_mapApp.getLayer_Building_IsShadow()          == Bool.TRUE);
      _chkShowLayer_Cartography              .setSelection(_mapApp.getLayer_BaseMap()                    .isEnabled());
      _chkShowLayer_Hillshading              .setSelection(_mapApp.getLayer_HillShading()                .isEnabled());
      _chkShowLayer_Label                    .setSelection(_mapApp.getLayer_Label()                      .isEnabled());
      _chkShowLayer_Label_IsBeforeBuilding   .setSelection(_mapApp.getLayer_Label_IsBeforeBuilding());
      _chkShowLayer_Scale                    .setSelection(_mapApp.getLayer_ScaleBar()                   .isEnabled());
      _chkShowLayer_Satellite                .setSelection(_mapApp.getLayer_Satellite()                  .isEnabled());
      _chkShowLayer_TileInfo                 .setSelection(_mapApp.getLayer_TileInfo()                   .isEnabled());

      _comboBuilding_SunPosition             .select(getSunPositionIndex());

      _spinnerBuilding_MinZoomLevel          .setSelection(_mapApp.getLayer_Building_MinZoomLevel() + ZOOM_UI_OFFSET);
      _spinnerHillShadingOpacity             .setSelection(UI.transformOpacity_WhenRestored(_mapApp.getLayer_HillShading_Opacity()));

// SET_FORMATTING_ON
   }

   private void saveState() {

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

      _mapApp.setLayer_Building_Options(
            _chkShowLayer_Building.getSelection(),
            _spinnerBuilding_MinZoomLevel.getSelection() - ZOOM_UI_OFFSET,
            _chkShowLayer_Building_Shadow.getSelection() ? Bool.TRUE : Bool.FALSE,
            getSelected_SunDayTime(),
            getSelected_Sunrise_Sunset_Time(true));
   }

   private void saveState_SunTime(final Widget widget) {

      boolean isUseScaleValue = false;

      if (widget instanceof Scale) {

         // use scale value -> this will adjust spinner coarse/fine values

         isUseScaleValue = true;
      }

      _mapApp.setLayer_Building_SunOptions(
            getSelected_SunDayTime(),
            getSelected_Sunrise_Sunset_Time(isUseScaleValue));
   }

   private void setSunPosition() {

      final Sun sun = _mapApp.getLayer_Building_S3DB().getExtrusionRenderer().getSun();
      final float sunrise_Sunset_Time = _mapApp.getLayer_Building_Sunrise_Sunset_Time(); // 0...1

      final int sunTime = (int) (sunrise_Sunset_Time * 100);
      final int sunTimeDetail = (int) (((sunrise_Sunset_Time * 100) - sunTime) * 100);

      final float sunrise = sun.getSunrise();
      final float sunset = sun.getSunset();
      final int sunsetDayOffset = sunset > 24 ? 1 : 0;

      final float sunriseFixed = (sunrise < 0 ? sunrise + 24 : sunrise) % 24;
      final float sunsetFixed = (sunset < 0 ? sunset + 24 : sunset) % 24;

      final int sunriseHour = (int) sunriseFixed;
      final int sunriseMinutes = (int) ((sunriseFixed - sunriseHour) * 60f);
      final int sunsetHour = (int) sunsetFixed;
      final int sunsetMinutes = (int) ((sunsetFixed - sunsetHour) * 60f);

      _sunriseTime = LocalDateTime.of(2000, 1, 1, sunriseHour, sunriseMinutes);
      _sunsetTime = LocalDateTime.of(2000, 1, 1, sunsetHour, sunsetMinutes);
      _sunsetTime = _sunsetTime.plusDays(sunsetDayOffset);

// SET_FORMATTING_OFF

      _lblBuilding_Sunrise             .setText(TimeTools.Formatter_Time_S.format(_sunriseTime));
      _lblBuilding_Sunset              .setText(TimeTools.Formatter_Time_S.format(_sunsetTime));

      _spinnerBuilding_SunTime_Coarse  .setSelection(sunTime);
      _spinnerBuilding_SunTime_Fine    .setSelection(sunTimeDetail);

      _scaleBuilding_SunTime           .setSelection(sunTime);

// SET_FORMATTING_ON

      // ensure that labels are fully displayed
      _lblBuilding_Sunrise.getParent().layout(true, true);
   }

   private void showSelectedSunPosition() {

      final float sunrise_Sunset_Time = _mapApp.getLayer_Building_Sunrise_Sunset_Time(); // 0...1

      final Duration durationBetweenSunriseAndSunset = Duration.between(_sunriseTime, _sunsetTime);
      final long sunriseSunsetSeconds = durationBetweenSunriseAndSunset.getSeconds();
      final long sunTimeSeconds = (long) (sunriseSunsetSeconds * sunrise_Sunset_Time);
      final LocalDateTime selectedSunTime = _sunriseTime.plusSeconds(sunTimeSeconds);

      _lblBuilding_SelectedSunTime.setText(TimeTools.Formatter_Time_M.format(selectedSunTime));

      // ensure that labels are fully displayed
      _lblBuilding_SelectedSunTime.getParent().layout(true, true);
   }

}
