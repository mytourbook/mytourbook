/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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
import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTrainingStress extends PreferencePage implements IWorkbenchPreferencePage {

   //TODO I am hesitating with between Training Stress and Training Stress Modeling
   public static final String ID = "net.tourbook.preferences.PrefPageTrainingStress"; //$NON-NLS-1$

   // private final IDialogSettings _state      = TourbookPlugin.getState(RawDataView.ID);

   // private RawDataManager        _rawDataMgr = RawDataManager.getInstance();

   private PixelConverter     _pc;
   private SelectionAdapter   _defaultSelectionListener;
   private MouseWheelListener _defaultMouseWheelListener;
//   private int                   _checkboxIndent;
   private int                _hintDefaultSpinnerWidth;

   /*
    * UI controls
    */
/*
 * private Button _chkAutoOpenImportLog;
 * private Button _chkCreateTourIdWithTime;
 * private Button _chkIgnoreInvalidFile;
 * private Button _chkSetBodyWeight;
 */
   private Label   _labelDays;
   private Spinner _spinnerFitnessDecayTime;
   private Spinner _spinnerFatigueDecayTime;

   /*
    * private Label _lblIdInfo;
    * private Label _lblInvalidFilesInfo;
    */
   // private PreferenceLinkArea _linkBodyWeight;

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
         createUI_10_Tagging(container);
      }

      return container;
   }

   private void createUI_10_Tagging(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText(Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         /*
          * number of recent tags
          */
         Label label = new Label(container, NONE);
         label.setText("Fitness decay");//Messages.pref_appearance_number_of_recent_tags);
         label.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);

         // Fitness decay spinner
         _spinnerFitnessDecayTime = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinnerFitnessDecayTime);
         _spinnerFitnessDecayTime.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
         _spinnerFitnessDecayTime.setMinimum(0);
         _spinnerFitnessDecayTime.setMaximum(9);
         _spinnerFitnessDecayTime.addSelectionListener(_defaultSelectionListener);
         _spinnerFitnessDecayTime.addMouseWheelListener(_defaultMouseWheelListener);

         // label: ms
         _labelDays = new Label(container, SWT.NONE);
         _labelDays.setText("days");

         /*
          * number of recent tags
          */
         label = new Label(container, NONE);
         label.setText("Fatigue decay");//Messages.pref_appearance_number_of_recent_tags);
         label.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);

         // Fatigue decay spinner
         _spinnerFatigueDecayTime = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinnerFatigueDecayTime);
         _spinnerFatigueDecayTime.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
         _spinnerFatigueDecayTime.setMinimum(0);
         _spinnerFatigueDecayTime.setMaximum(9);
         _spinnerFatigueDecayTime.addSelectionListener(_defaultSelectionListener);
         _spinnerFatigueDecayTime.addMouseWheelListener(_defaultMouseWheelListener);

         // label: ms
         _labelDays = new Label(container, SWT.NONE);
         _labelDays.setText("days");

      }
   }

   private void enableControls() {

      /*
       * final boolean isTourIdWithTime = _chkCreateTourIdWithTime.getSelection();
       * _lblIdInfo.setEnabled(isTourIdWithTime);
       * final boolean areInvalidFilesToBeIgnored = _chkIgnoreInvalidFile.getSelection();
       * _lblInvalidFilesInfo.setEnabled(areInvalidFilesToBeIgnored);
       * final boolean isSetBodyWeight = _chkSetBodyWeight.getSelection();
       * _linkBodyWeight.getControl().setEnabled(isSetBodyWeight);
       */
   }

   @Override
   public void init(final IWorkbench workbench) {

   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      // _checkboxIndent = _pc.convertHorizontalDLUsToPixels(10);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableControls();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
         }
      };

      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);

   }

   @Override
   protected void performDefaults() {

      /*
       * _chkCreateTourIdWithTime.setSelection(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT
       * );
       * _chkIgnoreInvalidFile.setSelection(RawDataView.STATE_IS_IGNORE_INVALID_FILE_DEFAULT);
       * _chkSetBodyWeight.setSelection(RawDataView.STATE_IS_SET_BODY_WEIGHT_DEFAULT);
       */
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

      /*
       * final boolean isCreateTourIdWithTime = Util.getStateBoolean(
       * _state,
       * RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME,
       * RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT);
       * final boolean isOpenImportLog = Util.getStateBoolean(
       * _state,
       * RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW,
       * RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW_DEFAULT);
       * _chkCreateTourIdWithTime.setSelection(isCreateTourIdWithTime);
       * _chkAutoOpenImportLog.setSelection(isOpenImportLog);
       * final boolean isIgnoreInvalidFile = Util.getStateBoolean(
       * _state,
       * RawDataView.STATE_IS_IGNORE_INVALID_FILE,
       * RawDataView.STATE_IS_IGNORE_INVALID_FILE_DEFAULT);
       * _chkIgnoreInvalidFile.setSelection(isIgnoreInvalidFile);
       * final boolean isSetBodyWeight = Util.getStateBoolean(
       * _state,
       * RawDataView.STATE_IS_SET_BODY_WEIGHT,
       * RawDataView.STATE_IS_SET_BODY_WEIGHT_DEFAULT);
       * _chkSetBodyWeight.setSelection(isSetBodyWeight);
       */
   }

   private void saveState() {
//
//      final boolean isCreateTourIdWithTime = _chkCreateTourIdWithTime.getSelection();
//      final boolean isOpenImportLog = _chkAutoOpenImportLog.getSelection();
//      final boolean isIgnoreInvalidFile = _chkIgnoreInvalidFile.getSelection();
//      final boolean isSetBodyWeight = _chkSetBodyWeight.getSelection();
//
//      _state.put(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME, isCreateTourIdWithTime);
//      _state.put(RawDataView.STATE_IS_AUTO_OPEN_IMPORT_LOG_VIEW, isOpenImportLog);
//      _state.put(RawDataView.STATE_IS_IGNORE_INVALID_FILE, isIgnoreInvalidFile);
//      _state.put(RawDataView.STATE_IS_SET_BODY_WEIGHT, isSetBodyWeight);
//
//      _rawDataMgr.setState_CreateTourIdWithTime(isCreateTourIdWithTime);
//      _rawDataMgr.setState_IsOpenImportLogView(isOpenImportLog);
//      _rawDataMgr.setState_IsIgnoreInvalidFile(isIgnoreInvalidFile);
//      _rawDataMgr.setState_IsSetBodyWeight(isSetBodyWeight);
   }
}
