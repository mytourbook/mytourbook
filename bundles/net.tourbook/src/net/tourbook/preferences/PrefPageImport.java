/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.Util;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.tour.CadenceMultiplier;
import net.tourbook.ui.ComboViewerCadence;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class PrefPageImport extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID          = "net.tourbook.preferences.PrefPageImport"; //$NON-NLS-1$

   private final IDialogSettings _state      = TourbookPlugin.getState(RawDataView.ID);

   private RawDataManager        _rawDataMgr = RawDataManager.getInstance();

   private PixelConverter        _pc;
   private SelectionAdapter      _defaultSelectionListener;
   private int                   _checkboxIndent;

   /*
    * UI controls
    */
   private Button             _chkAutoOpenImportLog;
   private Button             _chkCreateTourIdWithTime;
   private Button             _chkIgnoreInvalidFile;
   private Button             _chkSetBodyWeight;

   private Label              _lblIdInfo;
   private Label              _lblInvalidFilesInfo;

   private PreferenceLinkArea _linkBodyWeight;
   private ComboViewerCadence _comboDefaultCadence;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_10_General(container);
      }

      return container;
   }

   private void createUI_10_General(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Label: common info
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.PrefPage_Import_Label_Info);
            GridDataFactory.fillDefaults().applyTo(label);
         }

         {
            /*
             * Checkbox: Open import log
             */
            _chkAutoOpenImportLog = new Button(container, SWT.CHECK);
            _chkAutoOpenImportLog.setText(Messages.PrefPage_Import_Checkbox_AutoOpenTourLogView);
            _chkAutoOpenImportLog.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()//
                  .indent(0, _pc.convertVerticalDLUsToPixels(4))
                  .applyTo(_chkAutoOpenImportLog);
         }

         {
            /*
             * Checkbox: create tour id with time
             */
            _chkCreateTourIdWithTime = new Button(container, SWT.CHECK);
            _chkCreateTourIdWithTime.setText(Messages.PrefPage_Import_Checkbox_CreateTourIdWithTime);
            _chkCreateTourIdWithTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()//
//						.indent(0, _pc.convertVerticalDLUsToPixels(4))
                  .applyTo(_chkCreateTourIdWithTime);
         }

         {
            /*
             * Label: id info
             */
            _lblIdInfo = new Label(container, SWT.WRAP | SWT.READ_ONLY);
            _lblIdInfo.setText(Messages.PrefPage_Import_Checkbox_CreateTourIdWithTime_Tooltip);
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .indent(_checkboxIndent, 0)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(_lblIdInfo);
         }
         {
            /*
             * Checkbox: Ignore invalid files
             */
            _chkIgnoreInvalidFile = new Button(container, SWT.CHECK);
            _chkIgnoreInvalidFile.setText(Messages.PrefPage_Import_Checkbox_IgnoreInvalidFiles);
            _chkIgnoreInvalidFile.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .indent(0, 10)
                  .applyTo(_chkIgnoreInvalidFile);
         }
         {
            /*
             * Label:
             */
            _lblInvalidFilesInfo = new Label(container, SWT.WRAP | SWT.READ_ONLY);
            _lblInvalidFilesInfo.setText(Messages.PrefPage_Import_Checkbox_IgnoreInvalidFiles_Tooltip);
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .indent(_checkboxIndent, 0)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(_lblInvalidFilesInfo);
         }
         {
            /*
             * Set the person's body weight when importing and saving a tour
             */

            // checkbox
            _chkSetBodyWeight = new Button(container, SWT.CHECK);
            _chkSetBodyWeight.setText(Messages.PrefPage_Import_Checkbox_SetBodyWeight);
            _chkSetBodyWeight.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .indent(0, 10)
                  .applyTo(_chkSetBodyWeight);

            // link
            _linkBodyWeight = new PreferenceLinkArea(
                  container,
                  SWT.NONE,
                  PrefPagePeople.ID,
                  Messages.Pref_People_Link_BodyWeight,
                  (IWorkbenchPreferenceContainer) getContainer(),
                  new PrefPagePeopleData(null, TourbookPlugin.getActivePerson()));

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(_checkboxIndent, 0)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(_linkBodyWeight.getControl());
         }

         {
            /*
             * Tour type cadence default
             */
            final Composite cadenceContainer = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cadenceContainer);

            final Label lblDefaultCadence = new Label(cadenceContainer, SWT.FILL | SWT.LEFT);

            lblDefaultCadence.setText(Messages.PrefPage_Import_Default_Cadence);

            _comboDefaultCadence = new ComboViewerCadence(cadenceContainer, SWT.DROP_DOWN | SWT.READ_ONLY);

            /*
             * Label:
             */
            final Label lblDefaultCadenceInfo = new Label(parent, SWT.WRAP | SWT.READ_ONLY);
            lblDefaultCadenceInfo.setText(Messages.PrefPage_Import_Default_Cadence_Tooltip);
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .indent(_checkboxIndent, 0)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(lblDefaultCadenceInfo);
         }
      }
   }

   private void enableControls() {

      final boolean isTourIdWithTime = _chkCreateTourIdWithTime.getSelection();
      _lblIdInfo.setEnabled(isTourIdWithTime);

      final boolean areInvalidFilesToBeIgnored = _chkIgnoreInvalidFile.getSelection();
      _lblInvalidFilesInfo.setEnabled(areInvalidFilesToBeIgnored);

      final boolean isSetBodyWeight = _chkSetBodyWeight.getSelection();
      _linkBodyWeight.getControl().setEnabled(isSetBodyWeight);
   }

   @Override
   public void init(final IWorkbench workbench) {

   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _checkboxIndent = _pc.convertHorizontalDLUsToPixels(10);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableControls();
         }
      };
   }

   @Override
   protected void performDefaults() {

      _chkCreateTourIdWithTime.setSelection(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT);
      _chkIgnoreInvalidFile.setSelection(RawDataView.STATE_IS_IGNORE_INVALID_FILE_DEFAULT);
      _chkSetBodyWeight.setSelection(RawDataView.STATE_IS_SET_BODY_WEIGHT_DEFAULT);
      _comboDefaultCadence.setSelection(RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT);

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         saveState();
      }

      return isOK;
   }

   private void restoreState() {

      final boolean isCreateTourIdWithTime = Util.getStateBoolean(
            _state,
            RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME,
            RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT);

      final boolean isOpenImportLog = Util.getStateBoolean(
            _state,
            RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW,
            RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW_DEFAULT);

      _chkCreateTourIdWithTime.setSelection(isCreateTourIdWithTime);
      _chkAutoOpenImportLog.setSelection(isOpenImportLog);

      final boolean isIgnoreInvalidFile = Util.getStateBoolean(
            _state,
            RawDataView.STATE_IS_IGNORE_INVALID_FILE,
            RawDataView.STATE_IS_IGNORE_INVALID_FILE_DEFAULT);
      _chkIgnoreInvalidFile.setSelection(isIgnoreInvalidFile);

      final boolean isSetBodyWeight = Util.getStateBoolean(
            _state,
            RawDataView.STATE_IS_SET_BODY_WEIGHT,
            RawDataView.STATE_IS_SET_BODY_WEIGHT_DEFAULT);
      _chkSetBodyWeight.setSelection(isSetBodyWeight);

      final CadenceMultiplier defaultCadence = (CadenceMultiplier) Util.getStateEnum(_state,
            RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER,
            RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT);

      _comboDefaultCadence.setSelection(defaultCadence);
   }

   private void saveState() {

      final boolean isCreateTourIdWithTime = _chkCreateTourIdWithTime.getSelection();
      final boolean isOpenImportLog = _chkAutoOpenImportLog.getSelection();
      final boolean isIgnoreInvalidFile = _chkIgnoreInvalidFile.getSelection();
      final boolean isSetBodyWeight = _chkSetBodyWeight.getSelection();
      final CadenceMultiplier defaultCadenceMultiplier = _comboDefaultCadence.getSelectedCadence();

      _state.put(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME, isCreateTourIdWithTime);
      _state.put(RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW, isOpenImportLog);
      _state.put(RawDataView.STATE_IS_IGNORE_INVALID_FILE, isIgnoreInvalidFile);
      _state.put(RawDataView.STATE_IS_SET_BODY_WEIGHT, isSetBodyWeight);
      Util.setStateEnum(_state, RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER, defaultCadenceMultiplier);

      _rawDataMgr.setState_CreateTourIdWithTime(isCreateTourIdWithTime);
      _rawDataMgr.setState_IsOpenImportLogView(isOpenImportLog);
      _rawDataMgr.setState_IsIgnoreInvalidFile(isIgnoreInvalidFile);
      _rawDataMgr.setState_IsSetBodyWeight(isSetBodyWeight);
      _rawDataMgr.setState_DefaultCadenceMultiplier(defaultCadenceMultiplier);
   }
}
