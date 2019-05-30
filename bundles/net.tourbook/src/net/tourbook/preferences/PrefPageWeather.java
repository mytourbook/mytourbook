/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID           = "net.tourbook.device.PrefPageGPX";          //$NON-NLS-1$

   //  private IPreferenceStore      _prefStore   = Activator.getDefault().getPreferenceStore();
   private final IDialogSettings _importState = TourbookPlugin.getState(RawDataView.ID);

   private PixelConverter        _pc;
   /*
    * UI controls
    */
   private Button _chkConvertWayPoints;
   private Button _chkOneTour;
   IntegerFieldEditor            fieldEditor;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         // checkbox: convert waypoints
         {
            _chkConvertWayPoints = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkConvertWayPoints);
            _chkConvertWayPoints.setText("Utiliser fonction meteo");//Messages.PrefPage_GPX_Checkbox_ConvertWayPoints);
            _chkConvertWayPoints.setToolTipText("TITI");//Messages.PrefPage_GPX_Checkbox_ConvertWayPoints_Tooltip);

            _chkConvertWayPoints.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectExternalWebBrowser(container);
               }
            });


               // text: description height
            fieldEditor = new IntegerFieldEditor(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT,
                  "Cle API", //Messages.pref_tour_editor_description_height,
                     container);
               fieldEditor.setValidRange(2, 100);
               fieldEditor.getLabelControl(container).setToolTipText(Messages.pref_tour_editor_description_height_tooltip);
               UI.setFieldWidth(container, fieldEditor, UI.DEFAULT_FIELD_WIDTH);
//TODO add link to "Get API KEY"
         }

         // checkbox: merge all tracks into one tour
         {
            _chkOneTour = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkOneTour);
            _chkOneTour.setText("TATA");//Messages.PrefPage_GPX_Checkbox_OneTour);
         }
      }

      return container;
   }

   private void enableControls(final Composite container) {
      final boolean useExternalWebBrowser = _chkConvertWayPoints.getSelection();
      fieldEditor.setEnabled(useExternalWebBrowser, container);
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   public boolean okToLeave() {
      return super.okToLeave();
   }

   private void onSelectExternalWebBrowser(final Composite container) {

      enableControls(container);
   }

   @Override
   public boolean performCancel() {
      return super.performCancel();
   }



   @Override
   public boolean performOk() {

      return super.performOk();
   }

   private void restoreState() {

      // merge all tracks into one tour
      final boolean isMergeIntoOneTour = Util.getStateBoolean(
            _importState,
            RawDataView.STATE_IS_MERGE_TRACKS,
            RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);
      _chkOneTour.setSelection(isMergeIntoOneTour);

      // convert waypoints
      final boolean isConvertWayPoints = Util.getStateBoolean(
            _importState,
            RawDataView.STATE_IS_CONVERT_WAYPOINTS,
            RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT);
      //_chkConvertWayPoints.setSelection(isConvertWayPoints);
   }

}
