/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2D Map properties
 */
public class SlideoutMap2_Options extends ToolbarSlideout implements

      IColorSelectorListener,
      IActionResetToDefault,
      IChangeUIListener {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private static IDialogSettings        _state;

   private IPropertyChangeListener       _defaultPropertyChangeListener;
   private MouseWheelListener            _defaultMouseWheelListener;
   private SelectionListener             _defaultSelectionListener;

   private ActionResetToDefaults         _actionRestoreDefaults;

   private Map2View                      _map2View;

   /*
    * UI controls
    */
   private Button                _chkIsDimMap;
   private Button                _chkIsToggleKeyboardPanning;
   private Button                _chkSelectInbetweenTimeSlices;
   private Button                _chkShowGeoGridBorder;
   private Button                _chkShowTileInfo;
   private Button                _chkShowTileBorder;
   private Button                _chkShowValuePointTooltip;
   private Button                _chkUseMapDimColor;

   private Spinner               _spinnerMapDimValue;

   private ColorSelectorExtended _colorMapDimColor;
   private ColorSelectorExtended _colorMapTransparencyColor;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2_Options(final Control ownerControl,
                               final ToolBar toolBar,
                               final Map2View map2View,
                               final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {
      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_20_MapOptions(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_Options_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_MapOptions(final Composite parent) {

      final GridDataFactory gdSpan2 = GridDataFactory.fillDefaults().span(2, 1);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show value point tooltip
             */
            _chkShowValuePointTooltip = new Button(container, SWT.CHECK);
            _chkShowValuePointTooltip.setText(Messages.Tour_Action_ValuePointToolTip_IsVisible);
            _chkShowValuePointTooltip.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> saveState_ValuePointTooltip()));
            gdSpan2.applyTo(_chkShowValuePointTooltip);
         }
         {
            /*
             * Options to select all the time slices in between the left and right sliders or only
             * the current slider's one
             */
            _chkSelectInbetweenTimeSlices = new Button(container, SWT.CHECK);
            _chkSelectInbetweenTimeSlices.setText(Messages.Tour_Action_Select_Inbetween_Timeslices);
            _chkSelectInbetweenTimeSlices.setToolTipText(Messages.Tour_Action_Select_Inbetween_Timeslices_Tooltip);
            _chkSelectInbetweenTimeSlices.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkSelectInbetweenTimeSlices);
         }
         {
            /*
             * Inverse keyboard panning
             */
            _chkIsToggleKeyboardPanning = new Button(container, SWT.CHECK);
            _chkIsToggleKeyboardPanning.setText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning);
            _chkIsToggleKeyboardPanning.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning_Tooltip);
            _chkIsToggleKeyboardPanning.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkIsToggleKeyboardPanning);
         }
         {
            /*
             * Dim map
             */
            final Composite dimContainer = new Composite(container, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(dimContainer);
            {
               // checkbox
               _chkIsDimMap = new Button(dimContainer, SWT.CHECK);
               _chkIsDimMap.setText(Messages.Slideout_Map_Options_Checkbox_DimMap);
               _chkIsDimMap.addSelectionListener(_defaultSelectionListener);

               // spinner
               _spinnerMapDimValue = new Spinner(dimContainer, SWT.BORDER);
               _spinnerMapDimValue.setToolTipText(Messages.Slideout_Map_Options_Spinner_DimValue_Tooltip);
               _spinnerMapDimValue.setMinimum(0);
               _spinnerMapDimValue.setMaximum(Map2View.MAX_DIM_STEPS);
               _spinnerMapDimValue.setIncrement(1);
               _spinnerMapDimValue.setPageIncrement(4);
               _spinnerMapDimValue.addSelectionListener(_defaultSelectionListener);
               _spinnerMapDimValue.addMouseWheelListener(_defaultMouseWheelListener);
               GridDataFactory.fillDefaults().indent(10, 0).applyTo(_spinnerMapDimValue);
            }

            // dimming color
            _colorMapDimColor = new ColorSelectorExtended(container);
            _colorMapDimColor.setToolTipText(Messages.Slideout_Map_Options_Color_DimColor_Tooltip);
            _colorMapDimColor.addListener(_defaultPropertyChangeListener);
            _colorMapDimColor.addOpenListener(this);
         }
         {
            /*
             * Map transparency color
             */
            {
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Slideout_MapPoints_Label_MapTransparencyColor);
               label.setToolTipText(Messages.Slideout_MapPoints_Label_MapTransparencyColor_Tooltip);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               _colorMapTransparencyColor = new ColorSelectorExtended(container);
               _colorMapTransparencyColor.setToolTipText(Messages.Slideout_MapPoints_Label_MapTransparencyColor_Tooltip);
               _colorMapTransparencyColor.addListener(_defaultPropertyChangeListener);
               _colorMapTransparencyColor.addOpenListener(this);
            }
            {
               /*
                * Use map dim color
                */
               _chkUseMapDimColor = new Button(container, SWT.CHECK);
               _chkUseMapDimColor.setText(Messages.Slideout_MapPoints_Checkbox_UseMapDimColor);
               _chkUseMapDimColor.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults().span(2, 1).indent(16, 0).applyTo(_chkUseMapDimColor);
            }
         }
         {
            /*
             * Tile info
             */
            _chkShowTileInfo = new Button(container, SWT.CHECK);
            _chkShowTileInfo.setText(OtherMessages.MAP_PROPERTIES_SHOW_TILE_INFO);
            _chkShowTileInfo.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowTileInfo);
         }
         {
            /*
             * Tile border
             */
            _chkShowTileBorder = new Button(container, SWT.CHECK);
            _chkShowTileBorder.setText(OtherMessages.MAP_PROPERTIES_SHOW_TILE_BORDER);
            _chkShowTileBorder.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowTileBorder);
         }
         {
            /*
             * Geo grid border
             */
            _chkShowGeoGridBorder = new Button(container, SWT.CHECK);
            _chkShowGeoGridBorder.setText(OtherMessages.MAP_PROPERTIES_SHOW_GEO_GRID);
            _chkShowGeoGridBorder.addSelectionListener(_defaultSelectionListener);
            gdSpan2.applyTo(_chkShowGeoGridBorder);
         }
      }
   }

   private void enableControls() {

      final boolean isDimMap = _chkIsDimMap.getSelection();
      final boolean isUseMapDimColor = _chkUseMapDimColor.getSelection();
      final boolean isUseTransparencyColor = isUseMapDimColor == false;

// SET_FORMATTING_OFF

      // map dimming
      _chkUseMapDimColor            .setEnabled(isDimMap);
      _spinnerMapDimValue           .setEnabled(isDimMap);

      _colorMapDimColor             .setEnabled(isDimMap);
      _colorMapTransparencyColor    .setEnabled(isDimMap == false || isUseTransparencyColor);

// SET_FORMATTING_ON
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI_UpdateMap());

      _defaultPropertyChangeListener = propertyChangeEvent -> onChangeUI_UpdateMap();

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
         onChangeUI_UpdateMap();
      };
   }

   @Override
   public void onChangeUI_External() {

      _map2View.updateState_Map2_Options();
   }

   private void onChangeUI_UpdateMap() {

      saveState();

      enableControls();

      Map2PointManager.updateMapPointSlideout();

      _map2View.updateState_Map2_Options();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(    Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT);
      _chkSelectInbetweenTimeSlices.setSelection(  _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES));

// SET_FORMATTING_ON

      /*
       * Value point tooltip
       */
      final boolean isShowValuePointTooltip = _prefStore.getDefaultBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2);
      _chkShowValuePointTooltip.setSelection(isShowValuePointTooltip);

      // this is not set in saveState()
      _prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2, isShowValuePointTooltip);

      onChangeUI_UpdateMap();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning   .setSelection( Util.getStateBoolean(_state,  Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,      Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));
      _chkSelectInbetweenTimeSlices .setSelection( _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES));
      _chkShowValuePointTooltip     .setSelection( _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2));

      // tile debug info/border
      _chkShowGeoGridBorder             .setSelection(_prefStore.getBoolean(Map2View.PREF_DEBUG_MAP_SHOW_GEO_GRID));
      _chkShowTileInfo                  .setSelection(_prefStore.getBoolean(Map2View.PREF_SHOW_TILE_INFO));
      _chkShowTileBorder                .setSelection(_prefStore.getBoolean(Map2View.PREF_SHOW_TILE_BORDER));

      /*
       * Map dimming & transparency
       */
      _chkIsDimMap                  .setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_IS_MAP_DIMMED,                       Map2View.STATE_IS_MAP_DIMMED_DEFAULT));
      _chkUseMapDimColor            .setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR_DEFAULT));
      _spinnerMapDimValue           .setSelection(    Util.getStateInt(    _state,  Map2View.STATE_DIM_MAP_VALUE,                       Map2View.STATE_DIM_MAP_VALUE_DEFAULT));

      _colorMapDimColor             .setColorValue(   Util.getStateRGB(    _state,  Map2View.STATE_DIM_MAP_COLOR,                       Map2View.STATE_DIM_MAP_COLOR_DEFAULT));
      _colorMapTransparencyColor    .setColorValue(   Util.getStateRGB(    _state,  Map2View.STATE_MAP_TRANSPARENCY_COLOR,              Map2View.STATE_MAP_TRANSPARENCY_COLOR_DEFAULT));

// SET_FORMATTING_ON

   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,       _chkIsToggleKeyboardPanning.getSelection());

      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES, _chkSelectInbetweenTimeSlices.getSelection());

      // tile debug info/border
      _prefStore.setValue(Map2View.PREF_DEBUG_MAP_SHOW_GEO_GRID,  _chkShowGeoGridBorder.getSelection());
      _prefStore.setValue(Map2View.PREF_SHOW_TILE_BORDER,         _chkShowTileBorder.getSelection());
      _prefStore.setValue(Map2View.PREF_SHOW_TILE_INFO,           _chkShowTileInfo.getSelection());

      /*
       * Map dimming & transparency
       */
      _state.put(Map2View.STATE_IS_MAP_DIMMED,                     _chkIsDimMap               .getSelection());
      _state.put(Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,_chkUseMapDimColor         .getSelection());
      _state.put(Map2View.STATE_DIM_MAP_VALUE,                     _spinnerMapDimValue        .getSelection());

      Util.setState(_state, Map2View.STATE_DIM_MAP_COLOR,          _colorMapDimColor          .getColorValue());
      Util.setState(_state, Map2View.STATE_MAP_TRANSPARENCY_COLOR, _colorMapTransparencyColor .getColorValue());

// SET_FORMATTING_ON
   }

   private void saveState_ValuePointTooltip() {

      // set in pref store, tooltip is listening pref store modifications
      _prefStore.setValue(
            ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2,
            _chkShowValuePointTooltip.getSelection());
   }

}
