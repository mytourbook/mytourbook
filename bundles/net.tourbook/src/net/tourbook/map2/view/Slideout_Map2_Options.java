/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import java.util.LinkedHashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapZoomPosition;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2D properties slideout
 */
public class Slideout_Map2_Options extends ToolbarSlideout implements IColorSelectorListener {

   final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   final private IDialogSettings _state;

   private SelectionAdapter      _defaultState_SelectionListener;

   private Action                _actionRestoreDefaults;

   private Map2View              _map2View;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkIsToggleKeyboardPanning;
   private Button    _chkIsShowHoveredTour;

   private Button    _rdoZoom_CenterToMousePosition;
   private Button    _rdoZoom_CenterTour;
   private Button    _rdoZoom_KeepCurrentCenterPosition;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public Slideout_Map2_Options(final Control ownerControl,
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

      _actionRestoreDefaults = new Action() {
         @Override
         public void run() {
            resetToDefaults();
         }
      };

      _actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
      _actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
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
             * Tour tooltip
             */
            {
               // checkbox
               _chkIsShowHoveredTour = new Button(container, SWT.CHECK);
               _chkIsShowHoveredTour.setText(Messages.Slideout_Map_Options_Checkbox_ShowHoveredSelectedTour);
               _chkIsShowHoveredTour.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowHoveredSelectedTour_Tooltip);
               _chkIsShowHoveredTour.addSelectionListener(new SelectionAdapter() {
                  @Override
                  public void widgetSelected(final SelectionEvent e) {
                     onChangeUI_ShowHoveredTour();
                  }
               });
               GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowHoveredTour);
            }
         }
         {
            {
               /*
                * Map zoom position
                */
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Slideout_MapOptions_Label_ZoomPosition);
               GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

            }
            {
               final Composite zoomContainer = new Composite(container, SWT.NONE);
               GridDataFactory.fillDefaults()
                     .span(2, 1)
                     .indent(16, 0)
                     .applyTo(zoomContainer);
               GridLayoutFactory.fillDefaults().numColumns(1).applyTo(zoomContainer);
               {
                  /*
                   * Keep current center position, this is useful when selecting markers and zooming
                   * in/out, this is the old behaviour before 18.5
                   */
                  _rdoZoom_KeepCurrentCenterPosition = new Button(zoomContainer, SWT.RADIO);
                  _rdoZoom_KeepCurrentCenterPosition.setText(Messages.Slideout_MapOptions_Radio_Zoom_KeepCurrentCenter);
                  _rdoZoom_KeepCurrentCenterPosition.addSelectionListener(_defaultState_SelectionListener);
               }
               {
                  /*
                   * Center to mouse position, this behaviour is often used in web maps
                   */
                  _rdoZoom_CenterToMousePosition = new Button(zoomContainer, SWT.RADIO);
                  _rdoZoom_CenterToMousePosition.setText(Messages.Slideout_MapOptions_Radio_Zoom_CenterToMousePosition);
                  _rdoZoom_CenterToMousePosition.addSelectionListener(_defaultState_SelectionListener);

//                  final String oldtext = Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition;
//                  &Zoom + center to the mouse position

//                  final String oldToolTipText = Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition_Tooltip;
//                  When this feature is selected then the map is centered to the current
//                  mouse position when zooming in/out with the mouse wheel
//                  (this behavior is often used in web maps),
//
//                  otherwise the center of the map will be kept when zooming in/out
//                  (this is the old behavior before version 18.5)
               }
               {
                  /*
                   * Center tour
                   */
                  _rdoZoom_CenterTour = new Button(zoomContainer, SWT.RADIO);
                  _rdoZoom_CenterTour.setText(Messages.Slideout_MapOptions_Radio_Zoom_CenterTour);
                  _rdoZoom_CenterTour.addSelectionListener(_defaultState_SelectionListener);

//                  final String oldToolTipText = net.tourbook.map2.Messages.map_action_zoom_centered;
//                  Center tour when map is zoomed

//                  setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.map2.Messages.Image_Action_Zoom_CenterTour));
               }
            }
         }
         {
            /*
             * Inverse keyboard panning
             */
            {
               // checkbox
               _chkIsToggleKeyboardPanning = new Button(container, SWT.CHECK);
               _chkIsToggleKeyboardPanning.setText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning);
               _chkIsToggleKeyboardPanning.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ToggleKeyboardPanning_Tooltip);
               _chkIsToggleKeyboardPanning.addSelectionListener(_defaultState_SelectionListener);
               GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsToggleKeyboardPanning);
            }
         }
      }
   }

   private void enableControls() {

   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _defaultState_SelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI_MapUpdate();
         }
      };
   }

   private void onChangeUI_MapUpdate() {

      saveState();

      enableControls();

      _map2View.restoreState_Map2_Options();
   }

   private void onChangeUI_ShowHoveredTour() {

      if (_chkIsShowHoveredTour.getSelection()) {

         // get current painting method
         final String prefPaintingMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
         final boolean isEnhancedPainting = PrefPageMap2Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(prefPaintingMethod);

         if (isEnhancedPainting) {

            // show warning that enhanced painting mode is selected

            final LinkedHashMap<String, Integer> buttonLabelToIdMap = new LinkedHashMap<>();
            buttonLabelToIdMap.put(Messages.Slideout_Map2MapOptions_Action_SetTourPaintingModeBasic, IDialogConstants.OK_ID);
            buttonLabelToIdMap.put(Messages.App_Action_Cancel, IDialogConstants.CANCEL_ID);

            final MessageDialog dialog = new MessageDialog(

                  _parent.getShell(),

                  Messages.Slideout_Map2MapOptions_Dialog_EnhancePaintingWarning_Title,
                  null,

                  Messages.Slideout_Map2MapOptions_Dialog_EnhancePaintingWarning_Message,
                  MessageDialog.INFORMATION,

                  // default index
                  0,

                  Messages.Slideout_Map2MapOptions_Action_SetTourPaintingModeBasic,
                  Messages.App_Action_Cancel);

            setIsAnotherDialogOpened(true);
            {
               final int choice = dialog.open();

               if (choice == IDialogConstants.OK_ID) {

                  // set painting method to basic
                  _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,
                        PrefPageMap2Appearance.TOUR_PAINT_METHOD_SIMPLE);
               }
            }
            setIsAnotherDialogOpened(false);
         }
      }

      onChangeUI_MapUpdate();
   }

   private void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(    Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT);
      _chkIsShowHoveredTour.setSelection(          Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);

      final Enum<MapZoomPosition> defaultMapZoomPosition = Map2View.STATE_MAP_ZOOM_POSITION_DEFAULT;

      _rdoZoom_CenterToMousePosition.setSelection(       defaultMapZoomPosition.equals(MapZoomPosition.CENTER_TO_MOUSE_POSITION));
      _rdoZoom_CenterTour.setSelection(                  defaultMapZoomPosition.equals(MapZoomPosition.CENTER_TOUR));
      _rdoZoom_KeepCurrentCenterPosition.setSelection(   defaultMapZoomPosition.equals(MapZoomPosition.KEEP_CURRENT_CENTER_POSITION));

// SET_FORMATTING_ON

      onChangeUI_MapUpdate();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,   Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));
      _chkIsShowHoveredTour.setSelection(          Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));

      final Enum<MapZoomPosition> mapZoomPosition = Util.getStateEnum(_state, Map2View.STATE_MAP_ZOOM_POSITION, Map2View.STATE_MAP_ZOOM_POSITION_DEFAULT);

      _rdoZoom_CenterToMousePosition.setSelection(       mapZoomPosition.equals(MapZoomPosition.CENTER_TO_MOUSE_POSITION));
      _rdoZoom_CenterTour.setSelection(                  mapZoomPosition.equals(MapZoomPosition.CENTER_TOUR));
      _rdoZoom_KeepCurrentCenterPosition.setSelection(   mapZoomPosition.equals(MapZoomPosition.KEEP_CURRENT_CENTER_POSITION));

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,    _chkIsToggleKeyboardPanning.getSelection());
      _state.put(Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, _chkIsShowHoveredTour.getSelection());


      _state.put(Map2View.STATE_MAP_ZOOM_POSITION,

            _rdoZoom_CenterToMousePosition.getSelection()      ? MapZoomPosition.CENTER_TO_MOUSE_POSITION.name()
          : _rdoZoom_CenterTour.getSelection()                 ? MapZoomPosition.CENTER_TOUR.name()
          : _rdoZoom_KeepCurrentCenterPosition.getSelection()  ? MapZoomPosition.KEEP_CURRENT_CENTER_POSITION.name()

          // use default
          : Map2View.STATE_MAP_ZOOM_POSITION_DEFAULT.name()
      );

// SET_FORMATTING_ON
   }

}
