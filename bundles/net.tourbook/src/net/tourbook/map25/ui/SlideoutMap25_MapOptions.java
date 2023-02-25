/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2.5D map options
 */
public class SlideoutMap25_MapOptions extends ToolbarSlideout {

// SET_FORMATTING_OFF

   private static final String MAP_25D_KEYBOARD_SHORTCUTS = "https://mytourbook.sourceforge.io/mytourbook/index.php/documentation/show-tours/2-5d-map/25d-actions#keyboard"; //$NON-NLS-1$

// SET_FORMATTING_ON

   private Map25App           _mapApp;

   private SelectionListener  _defaultSelectionListener;
   private MouseWheelListener _mouseWheelListener;

   /*
    * UI controls
    */
   private Button  _chkIsBackgroundFPS;
   private Button  _chkUseDraggedKeyboardNavigation;
   private Button  _chkMapCenter_VerticalPosition;

   private Link    _linkKeyboardShortcuts;

   private Spinner _spinnerBackgroundFPS;
   private Spinner _spinnerMapCenter_VerticalPosition;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_MapOptions(final Control ownerControl,
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
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//       container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25Options_Label_MapOptions);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .applyTo(container);
      {
         {
            /*
             * Map center vertical position
             */
            _chkMapCenter_VerticalPosition = new Button(container, SWT.CHECK);
            _chkMapCenter_VerticalPosition.setText(Messages.Slideout_Map25Options_Checkbox_MapCenter_VerticalPosition);
            _chkMapCenter_VerticalPosition.setToolTipText(Messages.Slideout_Map25Options_Checkbox_MapCenter_VerticalPosition_Tooltip);
            _chkMapCenter_VerticalPosition.addSelectionListener(_defaultSelectionListener);

            _spinnerMapCenter_VerticalPosition = new Spinner(container, SWT.BORDER);
            _spinnerMapCenter_VerticalPosition.setMinimum((int) -Map25App.MAP_CENTER_VERTICAL_MAX_VALUE);
            _spinnerMapCenter_VerticalPosition.setMaximum((int) Map25App.MAP_CENTER_VERTICAL_MAX_VALUE);
            _spinnerMapCenter_VerticalPosition.setIncrement(1);
            _spinnerMapCenter_VerticalPosition.setPageIncrement(10);
            _spinnerMapCenter_VerticalPosition.setToolTipText(Messages.Slideout_Map25Options_Checkbox_MapCenter_VerticalPosition_Tooltip);

            _spinnerMapCenter_VerticalPosition.addSelectionListener(_defaultSelectionListener);
            _spinnerMapCenter_VerticalPosition.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onChangeUI();
            });
         }
         {
            /*
             * Background FPS
             */
            _chkIsBackgroundFPS = new Button(container, SWT.CHECK);
            _chkIsBackgroundFPS.setText(Messages.Slideout_Map25Options_Checkbox_BackgroundFPS);
            _chkIsBackgroundFPS.setToolTipText(Messages.Slideout_Map25Options_Checkbox_BackgroundFPS_Tooltip);
            _chkIsBackgroundFPS.addSelectionListener(_defaultSelectionListener);

            _spinnerBackgroundFPS = new Spinner(container, SWT.BORDER);
            _spinnerBackgroundFPS.setMinimum(-1);
            _spinnerBackgroundFPS.setMaximum(30);
            _spinnerBackgroundFPS.setIncrement(1);
            _spinnerBackgroundFPS.setPageIncrement(5);
            _spinnerBackgroundFPS.setToolTipText(Messages.Slideout_Map25Options_Checkbox_BackgroundFPS_Tooltip);

            _spinnerBackgroundFPS.addSelectionListener(_defaultSelectionListener);
            _spinnerBackgroundFPS.addMouseWheelListener(_mouseWheelListener);
         }
         {
            /*
             * Keyboard navigation
             */
            _chkUseDraggedKeyboardNavigation = new Button(container, SWT.CHECK);
            _chkUseDraggedKeyboardNavigation.setText(Messages.Slideout_Map25Options_Checkbox_UseDraggedKeyNavigation);
            _chkUseDraggedKeyboardNavigation.setToolTipText(Messages.Slideout_Map25Options_Checkbox_UseDraggedKeyNavigation_Tooltip);
            _chkUseDraggedKeyboardNavigation.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkUseDraggedKeyboardNavigation);
         }
         {
            /*
             * Keyboard shortcuts
             */
            _linkKeyboardShortcuts = new Link(container, SWT.NONE);
            _linkKeyboardShortcuts.setText(Messages.Slideout_Map25Options_Link_KeyboardShortcuts);
            _linkKeyboardShortcuts.setToolTipText(MAP_25D_KEYBOARD_SHORTCUTS);
            _linkKeyboardShortcuts.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(MAP_25D_KEYBOARD_SHORTCUTS)));
            GridDataFactory.fillDefaults().span(2, 1).indent(0, 20).applyTo(_linkKeyboardShortcuts);
         }
      }
   }

   private void enableControls() {

      final boolean isMapCenter_VerticalPosition = _chkMapCenter_VerticalPosition.getSelection();
      final boolean isBackgroundFPS = _chkIsBackgroundFPS.getSelection();

      _spinnerBackgroundFPS.setEnabled(isBackgroundFPS);
      _spinnerMapCenter_VerticalPosition.setEnabled(isMapCenter_VerticalPosition);
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _mouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkIsBackgroundFPS                 .setSelection(Map25App.isBackgroundFPS());
      _chkUseDraggedKeyboardNavigation    .setSelection(Map25ConfigManager.useDraggedKeyboardNavigation);
      _chkMapCenter_VerticalPosition      .setSelection(_mapApp.getMapCenter_VerticalPosition_IsEnabled());

      _spinnerBackgroundFPS               .setSelection(Map25App.getBackgroundFPS());
      _spinnerMapCenter_VerticalPosition  .setSelection(_mapApp.getMapCenter_VerticalPosition());

// SET_FORMATTING_ON
   }

   private void saveState() {

      _mapApp.setMap_VerticalPosition(
            _chkMapCenter_VerticalPosition.getSelection(),
            _spinnerMapCenter_VerticalPosition.getSelection());

      Map25ConfigManager.useDraggedKeyboardNavigation = _chkUseDraggedKeyboardNavigation.getSelection();

      _mapApp.setBackgroundFPS(

            _chkIsBackgroundFPS.getSelection(),
            _spinnerBackgroundFPS.getSelection());

      _mapApp.updateMap();
   }

}
