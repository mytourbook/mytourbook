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
public class SlideoutMap25_MapOptions extends ToolbarSlideout {

   private SelectionListener _defaultSelectionListener;
   private SelectionListener _layerSelectionListener;

   private Map25View         _map25View;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkShowLayer_Cartography;
   private Button    _chkShowLayer_Building;
   private Button    _chkShowLayer_Hillshading;
   private Button    _chkShowLayer_Satellite;
   private Button    _chkShowLayer_Label;
   private Button    _chkShowLayer_Scale;
   private Button    _chkShowLayer_TileInfo;
   private Button    _chkUseDraggedKeyboardNavigation;

   private Spinner   _spinnerHillshadingOpacity;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_MapOptions(final Control ownerControl,
                                   final ToolBar toolBar,
                                   final Map25View map25View) {

      super(ownerControl, toolBar);

      _map25View = map25View;
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
      GridDataFactory
            .fillDefaults()//
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_50_Layer(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_Map25MapOptions_Group_MapLayer);
      GridDataFactory
            .fillDefaults()//
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
             * Building
             */
            _chkShowLayer_Building = new Button(group, SWT.CHECK);
            _chkShowLayer_Building.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_3DBuilding);
            _chkShowLayer_Building.addSelectionListener(_layerSelectionListener);
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

                  _spinnerHillshadingOpacity = new Spinner(containerHillshading, SWT.BORDER);
                  _spinnerHillshadingOpacity.setMinimum(0);
                  _spinnerHillshadingOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
                  _spinnerHillshadingOpacity.setIncrement(1);
                  _spinnerHillshadingOpacity.setPageIncrement(10);
                  _spinnerHillshadingOpacity.setToolTipText(tooltipText);

                  _spinnerHillshadingOpacity.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModify_HillShadingOpacity()));
                  _spinnerHillshadingOpacity.addMouseWheelListener(mouseEvent -> {
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

      _spinnerHillshadingOpacity.setEnabled(isHillShading);

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

      final Map25App mapApp = _map25View.getMapApp();

      // updade model
      final int hillShadingOpacity = UI.transformOpacity_WhenSaved(_spinnerHillshadingOpacity.getSelection());
      mapApp.setLayer_HillShading_Opacity(hillShadingOpacity);

      // update UI
      final float hillshadingAlpha = hillShadingOpacity / 255f;
      mapApp.getLayer_HillShading().setBitmapAlpha(hillshadingAlpha, true);

      enableActions();

      mapApp.updateMap();
   }

   private void onModify_Layer() {

      final Map25App mapApp = _map25View.getMapApp();

      mapApp.getLayer_BaseMap().setEnabled(_chkShowLayer_Cartography.getSelection());
      mapApp.getLayer_HillShading().setEnabled(_chkShowLayer_Hillshading.getSelection());

      // satellite maps
      mapApp.getLayer_Satellite().setEnabled(_chkShowLayer_Satellite.getSelection());

      mapApp.getLayer_Label().setEnabled(_chkShowLayer_Label.getSelection());
      mapApp.getLayer_ScaleBar().setEnabled(_chkShowLayer_Scale.getSelection());
      mapApp.getLayer_TileInfo().setEnabled(_chkShowLayer_TileInfo.getSelection());

      // switching on/off both building layers
      mapApp.getLayer_Building_Default().setEnabled(_chkShowLayer_Building.getSelection());
      mapApp.getLayer_Building_S3DB().setEnabled(_chkShowLayer_Building.getSelection());

      enableActions();

      mapApp.updateLayer();
   }



   private void restoreState() {

      final Map25App mapApp = _map25View.getMapApp();

// SET_FORMATTING_OFF

      _chkShowLayer_Building     .setSelection(mapApp.getLayer_Building_Default()   .isEnabled());
      _chkShowLayer_Cartography  .setSelection(mapApp.getLayer_BaseMap()            .isEnabled());
      _chkShowLayer_Hillshading  .setSelection(mapApp.getLayer_HillShading()        .isEnabled());
      _chkShowLayer_Label        .setSelection(mapApp.getLayer_Label()              .isEnabled());
      _chkShowLayer_Scale        .setSelection(mapApp.getLayer_ScaleBar()           .isEnabled());
      _chkShowLayer_Satellite    .setSelection(mapApp.getLayer_Satellite()          .isEnabled());
      _chkShowLayer_TileInfo     .setSelection(mapApp.getLayer_TileInfo()           .isEnabled());

      _spinnerHillshadingOpacity .setSelection(UI.transformOpacity_WhenRestored(mapApp.getLayer_HillShading_Opacity()));

// SET_FORMATTING_ON

      _chkUseDraggedKeyboardNavigation.setSelection(Map25ConfigManager.useDraggedKeyboardNavigation);
   }

   private void saveState() {

      Map25ConfigManager.useDraggedKeyboardNavigation = _chkUseDraggedKeyboardNavigation.getSelection();
   }

}
