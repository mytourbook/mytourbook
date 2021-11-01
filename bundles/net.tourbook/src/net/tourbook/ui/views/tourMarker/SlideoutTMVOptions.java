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
package net.tourbook.ui.views.tourMarker;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 */
public class SlideoutTMVOptions extends ToolbarSlideout implements IActionResetToDefault {

   private final IPreferenceStore   _prefStore = TourbookPlugin.getPrefStore();

   private ActionResetToDefaults    _actionRestoreDefaults;

   private ChartOptions_Grid        _gridUI;

   private SelectionAdapter         _defaultSelectionListener;

   private TourMarkerView         _tourMarkerView;

   /*
    * UI controls
    */
   private Button  _chkFix2xErrors;

   private Button  _lbl2xErrorTolerance;
   private Label   _lbl2xErrorTolerance_Unit;
   private Label   _lbl2xToleranceResult;
   private Label   _lbl2xToleranceResult_Value;

   private Spinner _spinner2xErrorTolerance;

   public SlideoutTMVOptions(final Control ownerControl,
                             final ToolBar toolBar,
                             final String prefStoreGridPrefix,
                             final TourMarkerView tourMarkerView) {

      super(ownerControl, toolBar);

      _tourMarkerView = tourMarkerView;

      _gridUI = new ChartOptions_Grid(prefStoreGridPrefix);
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
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            createUI_20_Options(container);

            // _gridUI.createUI(container);
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

      //  final Group group = new Group(parent, SWT.NONE);
      //  group.setText(Messages.Slideout_HVROptions_Group);
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
            _chkFix2xErrors = new Button(parent, SWT.RADIO);
            _chkFix2xErrors.setText("Use elapsed time");//Messages.Slideout_HVROptions_Checkbox_2xValues);
            _chkFix2xErrors.setToolTipText(Messages.Slideout_HVROptions_Checkbox_2xValues_Tooltip);
            _chkFix2xErrors.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkFix2xErrors);
         }

         {
            /*
             * Label: 2x tolerance
             */
            _lbl2xErrorTolerance = new Button(parent, SWT.RADIO);
            _lbl2xErrorTolerance.setText("Use recorded time");
            GridDataFactory.fillDefaults().applyTo(_lbl2xErrorTolerance);

         }
      }
   }

   private void enableControls() {
//
//      final boolean isRemove2xValue = _chkFix2xErrors.getSelection();
//
//      _lbl2xErrorTolerance.setEnabled(isRemove2xValue);
//      _lbl2xErrorTolerance_Unit.setEnabled(isRemove2xValue);
//      _spinner2xErrorTolerance.setEnabled(isRemove2xValue);
//
//      _lbl2xToleranceResult.setEnabled(isRemove2xValue);
//      _lbl2xToleranceResult_Value.setEnabled(isRemove2xValue);
   }

   /*
    * UI controls
    */
   private void initUI(final Composite parent) {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };


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

      _chkFix2xErrors.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR));
      _spinner2xErrorTolerance.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.HRV_OPTIONS_2X_ERROR_TOLERANCE));

      _gridUI.resetToDefaults();
      _gridUI.saveState();

      onChangeUI();
   }

   private void restoreState() {

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR, _chkFix2xErrors.getSelection());
      _prefStore.setValue(ITourbookPreferences.HRV_OPTIONS_2X_ERROR_TOLERANCE, _spinner2xErrorTolerance.getSelection());
   }

   private void updateUI() {

//      _lbl2xToleranceResult_Value.setText(_tourMarkerView.getFixed2xErrors_0()
//            + UI.DASH_WITH_SPACE
//            + _tourMarkerView.getFixed2xErrors_1());
   }

}
