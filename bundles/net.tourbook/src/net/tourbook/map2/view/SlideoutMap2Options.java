/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourPauseUI;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
public class SlideoutMap2Options extends ToolbarSlideout implements

      IColorSelectorListener,
      IActionResetToDefault,
      IChangeUIListener {

   private static IDialogSettings         _state;

   private IPropertyChangeListener        _defaultChangePropertyListener;
   private SelectionListener              _defaultSelectionListener;
   private MouseWheelListener             _defaultMouseWheelListener;

   private ActionResetToDefaults          _actionRestoreDefaults;

   private Map2View                       _map2View;

   private final TourPainterConfiguration _tourPainterConfig = TourPainterConfiguration.getInstance();

   /*
    * UI controls
    */
   private Button                _chkIsDimMap;
   private Button                _chkIsToggleKeyboardPanning;
   private Button                _chkIsZoomWithMousePosition;

   private Spinner               _spinnerDimValue;

   private ColorSelectorExtended _colorMapDimmColor;
   private TourPauseUI           _tourPausesUI;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2Options(final Control ownerControl,
                              final ToolBar toolBar,
                              final Map2View map2View,
                              final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;

      _tourPausesUI = new TourPauseUI(map2State, this, this);
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

         _tourPausesUI.createContent(shellContainer, _tourPainterConfig.isShowTourPauses);
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
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()//
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

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Zoom to mouse position
             */
            _chkIsZoomWithMousePosition = new Button(container, SWT.CHECK);
            _chkIsZoomWithMousePosition.setText(Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition);
            _chkIsZoomWithMousePosition.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition_Tooltip);
            _chkIsZoomWithMousePosition.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsZoomWithMousePosition);
         }
         {
            /*
             * Inverse keyboard panning
             */
            _chkIsToggleKeyboardPanning = new Button(container, SWT.CHECK);
            _chkIsToggleKeyboardPanning.setText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning);
            _chkIsToggleKeyboardPanning.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning_Tooltip);
            _chkIsToggleKeyboardPanning.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsToggleKeyboardPanning);
         }
         {
            /*
             * Dim map
             */
            // checkbox
            _chkIsDimMap = new Button(container, SWT.CHECK);
            _chkIsDimMap.setText(Messages.Slideout_Map_Options_Checkbox_DimMap);
            _chkIsDimMap.addSelectionListener(_defaultSelectionListener);

            final Composite dimContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(dimContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(dimContainer);
            {
               // spinner
               _spinnerDimValue = new Spinner(dimContainer, SWT.BORDER);
               _spinnerDimValue.setToolTipText(Messages.Slideout_Map_Options_Spinner_DimValue_Tooltip);
               _spinnerDimValue.setMinimum(0);
               _spinnerDimValue.setMaximum(Map2View.MAX_DIM_STEPS);
               _spinnerDimValue.setIncrement(1);
               _spinnerDimValue.setPageIncrement(4);
               _spinnerDimValue.addSelectionListener(_defaultSelectionListener);
               _spinnerDimValue.addMouseWheelListener(_defaultMouseWheelListener);

               // dimming color
               _colorMapDimmColor = new ColorSelectorExtended(dimContainer);
               _colorMapDimmColor.setToolTipText(Messages.Slideout_Map_Options_Color_DimColor_Tooltip);
               _colorMapDimmColor.addListener(_defaultChangePropertyListener);
               _colorMapDimmColor.addOpenListener(this);
            }
         }
      }
   }

   private void enableControls() {

      final boolean isDimMap = _chkIsDimMap.getSelection();

      _colorMapDimmColor.setEnabled(isDimMap);
      _spinnerDimValue.setEnabled(isDimMap);

      _tourPausesUI.enableControls();
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI_UpdateMap());

      _defaultChangePropertyListener = propertyChangeEvent -> onChangeUI_UpdateMap();

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI_UpdateMap();
      };
   }

   @Override
   public void onChangeUI_External() {

      _map2View.restoreState_Map2_Options();
   }

   private void onChangeUI_UpdateMap() {

      saveState();

      enableControls();

      _map2View.restoreState_Map2_Options();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(    Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT);
      _chkIsZoomWithMousePosition.setSelection(    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT);

      /*
       * Map dimming
       */
      _chkIsDimMap.setSelection(                   Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
      _spinnerDimValue.setSelection(               Map2View.STATE_DIM_MAP_VALUE_DEFAULT);
      _colorMapDimmColor.setColorValue(            Map2View.STATE_DIM_MAP_COLOR_DEFAULT);

// SET_FORMATTING_ON

      _tourPausesUI.resetToDefaults();

      onChangeUI_UpdateMap();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,      Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));
      _chkIsZoomWithMousePosition.setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,     Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

      /*
       * Map dimming
       */
      _chkIsDimMap.setSelection(          Util.getStateBoolean(_state,  Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT));
      _spinnerDimValue.setSelection(      Util.getStateInt(    _state,  Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT));
      _colorMapDimmColor.setColorValue(   Util.getStateRGB(    _state,  Map2View.STATE_DIM_MAP_COLOR, Map2View.STATE_DIM_MAP_COLOR_DEFAULT));

// SET_FORMATTING_ON

      _tourPausesUI.restoreState();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,    _chkIsToggleKeyboardPanning.getSelection());
      _state.put(Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   _chkIsZoomWithMousePosition.getSelection());

      /*
       * Map dimming
       */
      _state.put(Map2View.STATE_IS_MAP_DIMMED,                 _chkIsDimMap.getSelection());
      _state.put(Map2View.STATE_DIM_MAP_VALUE,                 _spinnerDimValue.getSelection());
      Util.setState(_state,Map2View.STATE_DIM_MAP_COLOR,       _colorMapDimmColor.getColorValue());

// SET_FORMATTING_ON

      _tourPausesUI.saveState();
   }

}
