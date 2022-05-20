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
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
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
   private static final int  ZOOM_UI_OFFSET = 1;

   private SelectionListener _defaultSelectionListener;
   private SelectionListener _layerSelectionListener;

   private Map25App          _mapApp;

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

   private Label     _lblBuildingMinZoomLevel;

   private Spinner   _spinnerBuildingMinZoomLevel;
   private Spinner   _spinnerHillShadingOpacity;

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

      restoreState();
      enableActions();

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

      final GridDataFactory indentGridData = GridDataFactory.fillDefaults().grab(true, false).indent(8, 0);

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
             * Building: Shadow
             */
            _chkShowLayer_Building_Shadow = new Button(group, SWT.CHECK);
            _chkShowLayer_Building_Shadow.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Building_IsShowShadow);
            _chkShowLayer_Building_Shadow.addSelectionListener(_layerSelectionListener);
            indentGridData.applyTo(_chkShowLayer_Building_Shadow);
         }
         {
            /*
             * Building: Min zoom level
             */
            final Composite containerBuilding = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().indent(8, 0).applyTo(containerBuilding);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBuilding);
            {
               final String tooltipText = Messages.Slideout_Map25MapOptions_Label_BuildingMinZoomLevel_Tooltip;
               {
                  _lblBuildingMinZoomLevel = new Label(containerBuilding, SWT.NONE);
                  _lblBuildingMinZoomLevel.setText(Messages.Slideout_Map25MapOptions_Label_BuildingMinZoomLevel);
                  _lblBuildingMinZoomLevel.setToolTipText(tooltipText);
                  GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblBuildingMinZoomLevel);
               }
               {
                  _spinnerBuildingMinZoomLevel = new Spinner(containerBuilding, SWT.BORDER);
                  _spinnerBuildingMinZoomLevel.setMinimum(11 + ZOOM_UI_OFFSET);
                  _spinnerBuildingMinZoomLevel.setMaximum(17 + ZOOM_UI_OFFSET);
                  _spinnerBuildingMinZoomLevel.setIncrement(1);
                  _spinnerBuildingMinZoomLevel.setPageIncrement(1);
                  _spinnerBuildingMinZoomLevel.setToolTipText(tooltipText);

                  _spinnerBuildingMinZoomLevel.addSelectionListener(_layerSelectionListener);
                  _spinnerBuildingMinZoomLevel.addMouseWheelListener(mouseEvent -> {
                     UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
                     onModify_Layer();
                  });
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

   private void enableActions() {

      final boolean isHillShading = _chkShowLayer_Hillshading.getSelection();
      final boolean isBuildingVisible = _chkShowLayer_Building.getSelection();
      final boolean isLabelVisible = _chkShowLayer_Label.getSelection();

      _chkShowLayer_Building_Shadow.setEnabled(isBuildingVisible);
      _chkShowLayer_Label_IsBeforeBuilding.setEnabled(isLabelVisible && isBuildingVisible);

      _lblBuildingMinZoomLevel.setEnabled(isBuildingVisible);

      _spinnerHillShadingOpacity.setEnabled(isHillShading);
      _spinnerBuildingMinZoomLevel.setEnabled(isBuildingVisible);

      // force UI update otherwise the slideout UI update is done after the map is updated
      _parent.update();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
      _layerSelectionListener = widgetSelectedAdapter(selectionEvent -> onModify_Layer());

   }

   private void onChangeUI() {

      saveState();

      enableActions();
   }

   private void onModify_HillShadingOpacity() {

      // updade model
      final int hillShadingOpacity = UI.transformOpacity_WhenSaved(_spinnerHillShadingOpacity.getSelection());
      _mapApp.setLayer_HillShading_Options(hillShadingOpacity);

      // update UI
      final float hillshadingAlpha = hillShadingOpacity / 255f;
      _mapApp.getLayer_HillShading().setBitmapAlpha(hillshadingAlpha, true);

      enableActions();

      _mapApp.updateMap();
   }

   private void onModify_Layer() {

      saveState_Layer();

      enableActions();

      _mapApp.updateLayer();
   }

   private void restoreState() {

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

      _spinnerBuildingMinZoomLevel        .setSelection(_mapApp.getLayer_Building_MinZoomLevel() + ZOOM_UI_OFFSET);
      _spinnerHillShadingOpacity          .setSelection(UI.transformOpacity_WhenRestored(_mapApp.getLayer_HillShading_Opacity()));

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

      _mapApp.setLayer_Building_Options(
            _chkShowLayer_Building.getSelection(),
            _chkShowLayer_Building_Shadow.getSelection(),
            _spinnerBuildingMinZoomLevel.getSelection() - ZOOM_UI_OFFSET);
   }

}
