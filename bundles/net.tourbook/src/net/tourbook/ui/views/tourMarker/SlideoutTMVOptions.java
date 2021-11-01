/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.ui.views.tourMarker;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutTMVOptions extends ToolbarSlideout implements IActionResetToDefault {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private ActionResetToDefaults  _actionRestoreDefaults;

   private SelectionListener      _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _chkUseElapsedTime;
   private Button _chkUseRecordedTime;

   public SlideoutTMVOptions(final Control ownerControl,
                             final ToolBar toolBar) {

      super(ownerControl, toolBar);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      enableControls();

      updateUI();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()//
               .numColumns(2)
               .applyTo(container);
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
            createUI_20_Options(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText("Tour Marker View Options");//Messages.Slideout_HVROptions_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   private void createUI_20_Options(final Composite parent) {

      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(parent);
      GridLayoutFactory.swtDefaults().applyTo(parent);
      {
         {
            /*
             * Show distance
             */
            _chkUseElapsedTime = new Button(parent, SWT.RADIO);
            _chkUseElapsedTime.setText("Use elapsed time");//Messages.Slideout_HVROptions_Checkbox_2xValues);
            _chkUseElapsedTime.setToolTipText(Messages.Slideout_HVROptions_Checkbox_2xValues_Tooltip);
            _chkUseElapsedTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkUseElapsedTime);
         }

         {
            /*
             * Label: 2x tolerance
             */
            _chkUseRecordedTime = new Button(parent, SWT.RADIO);
            _chkUseRecordedTime.setText("Use recorded time");
            GridDataFactory.fillDefaults().applyTo(_chkUseRecordedTime);

         }
      }
   }

   private void enableControls() {

   }

   /*
    * UI controls
    */
   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      enableControls();

      // update chart async (which is done when a pref store value is modified) that the UI is updated immediately
      Display.getCurrent().asyncExec(() -> {

         saveState();
         updateUI();
      });
   }

   @Override
   public void resetToDefaults() {

      _chkUseElapsedTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR));

      onChangeUI();
   }

   private void restoreState() {

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR, _chkUseElapsedTime.getSelection());
   }

   private void updateUI() {

//      _lbl2xToleranceResult_Value.setText(_tourMarkerView.getFixed2xErrors_0()
//            + UI.DASH_WITH_SPACE
//            + _tourMarkerView.getFixed2xErrors_1());
   }

}
