/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
   private Button _chkUseMovingTime;
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

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_10_Header(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Label: Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_TMVOptions_Label_Title);

            MTFont.setBannerFont(label);
         }
         {
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

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
      GridLayoutFactory.swtDefaults().applyTo(container);
      {
         {
            /*
             * Use Elapsed time
             */
            _chkUseElapsedTime = new Button(container, SWT.RADIO);
            _chkUseElapsedTime.setText(Messages.Slideout_TMVOptions_Checkbox_UseElapsedTime);
            _chkUseElapsedTime.setToolTipText(Messages.Slideout_HVROptions_Checkbox_2xValues_Tooltip);
            _chkUseElapsedTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkUseElapsedTime);
         }
         {
            /*
             * Use Moving time
             */
            _chkUseMovingTime = new Button(container, SWT.RADIO);
            _chkUseMovingTime.setText(Messages.Slideout_TMVOptions_Checkbox_UseMovingTime);
            _chkUseMovingTime.setToolTipText(Messages.Slideout_HVROptions_Checkbox_2xValues_Tooltip);
            _chkUseMovingTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkUseMovingTime);
         }
         {
            /*
             * Use Recorded time
             */
            _chkUseRecordedTime = new Button(container, SWT.RADIO);
            _chkUseRecordedTime.setText(Messages.Slideout_TMVOptions_Checkbox_UseRecordedTime);
            _chkUseRecordedTime.setToolTipText(Messages.Slideout_HVROptions_Checkbox_2xValues_Tooltip);
            _chkUseRecordedTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkUseRecordedTime);
         }
      }
   }

   /*
    * UI controls
    */
   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      saveState();
   }

   @Override
   public void resetToDefaults() {

      _chkUseElapsedTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME));
      _chkUseMovingTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME));
      _chkUseRecordedTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME));

      onChangeUI();
   }

   private void restoreState() {

      _chkUseElapsedTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME));
      _chkUseMovingTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME));
      _chkUseRecordedTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME));

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME, _chkUseElapsedTime.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME, _chkUseMovingTime.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME, _chkUseRecordedTime.getSelection());
   }

}
