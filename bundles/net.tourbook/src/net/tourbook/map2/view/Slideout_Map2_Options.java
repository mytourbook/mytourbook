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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
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
   private Button    _chkIsZoomWithMousePosition;

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

      _actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_RestoreDefault));
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
            /*
             * Zoom to mouse position
             */
            {
               // checkbox
               _chkIsZoomWithMousePosition = new Button(container, SWT.CHECK);
               _chkIsZoomWithMousePosition.setText(Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition);
               _chkIsZoomWithMousePosition.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ZoomWithMousePosition_Tooltip);
               _chkIsZoomWithMousePosition.addSelectionListener(_defaultState_SelectionListener);
               GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsZoomWithMousePosition);
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

      _chkIsToggleKeyboardPanning.setSelection(   Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT);
      _chkIsShowHoveredTour.setSelection(          Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);
      _chkIsZoomWithMousePosition.setSelection(    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI_MapUpdate();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkIsToggleKeyboardPanning.setSelection(   Util.getStateBoolean(_state,  Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,   Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));
      _chkIsShowHoveredTour.setSelection(          Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));
      _chkIsZoomWithMousePosition.setSelection(    Util.getStateBoolean(_state,  Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,   _chkIsToggleKeyboardPanning.getSelection());
      _state.put(Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, _chkIsShowHoveredTour.getSelection());
      _state.put(Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   _chkIsZoomWithMousePosition.getSelection());

// SET_FORMATTING_ON
   }

}
