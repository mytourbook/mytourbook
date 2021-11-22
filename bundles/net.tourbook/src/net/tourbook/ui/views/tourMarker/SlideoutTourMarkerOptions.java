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

public class SlideoutTourMarkerOptions extends ToolbarSlideout implements IActionResetToDefault {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private ActionResetToDefaults  _actionRestoreDefaults;

   private SelectionListener      _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _rdoElapsedTime;
   private Button _rdoMovingTime;
   private Button _rdoRecordedTime;

   public SlideoutTourMarkerOptions(final Control ownerControl, final ToolBar toolBar) {

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
         GridLayoutFactory.fillDefaults().applyTo(container);
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
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Label: Time used between markers
          */
         final Label lblTimeUsedBetweenMarkers = new Label(container, SWT.NONE);
         lblTimeUsedBetweenMarkers.setText(Messages.Slideout_TMVOptions_Label_TimeUsedBetweenMarkers);
         lblTimeUsedBetweenMarkers.setToolTipText(Messages.Slideout_TMVOptions_Label_TimeUsedBetweenMarkers_Tooltip);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(lblTimeUsedBetweenMarkers);

         final Composite timeContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(timeContainer);
         {
            /*
             * Use Elapsed time
             */
            _rdoElapsedTime = new Button(timeContainer, SWT.RADIO);
            _rdoElapsedTime.setText(Messages.Slideout_TMVOptions_Radio_ElapsedTime);
            _rdoElapsedTime.addSelectionListener(_defaultSelectionListener);

            /*
             * Use Moving time
             */
            _rdoMovingTime = new Button(timeContainer, SWT.RADIO);
            _rdoMovingTime.setText(Messages.Slideout_TMVOptions_Radio_MovingTime);
            _rdoMovingTime.addSelectionListener(_defaultSelectionListener);

            /*
             * Use Recorded time
             */
            _rdoRecordedTime = new Button(timeContainer, SWT.RADIO);
            _rdoRecordedTime.setText(Messages.Slideout_TMVOptions_Radio_RecordedTime);
            _rdoRecordedTime.addSelectionListener(_defaultSelectionListener);
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

      _rdoElapsedTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME));
      _rdoMovingTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME));
      _rdoRecordedTime.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME));

      onChangeUI();
   }

   private void restoreState() {

      _rdoElapsedTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME));
      _rdoMovingTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME));
      _rdoRecordedTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME));

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME, _rdoElapsedTime.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME, _rdoMovingTime.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME, _rdoRecordedTime.getSelection());
   }

}
