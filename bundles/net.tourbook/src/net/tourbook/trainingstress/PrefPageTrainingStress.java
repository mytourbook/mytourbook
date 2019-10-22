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
package net.tourbook.trainingstress;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Activator;
import net.tourbook.common.UI;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTrainingStress extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID           = "net.tourbook.trainingstress.PrefPageTrainingStress"; //$NON-NLS-1$

   private IPreferenceStore      _prefStore   = Activator.getDefault().getPreferenceStore();
   private final IDialogSettings _importState = TourbookPlugin.getState(RawDataView.ID);

   private RawDataManager        _rawDataMgr  = RawDataManager.getInstance();

   private PixelConverter        _pc;
   private int                   _hintDefaultSpinnerWidth;
   private int                   DEFAULT_DESCRIPTION_WIDTH;

   private boolean               _isUpdateUI;

   /*
    * UI controls
    */
   private TabFolder _tabFolder;

   /*
    * private Button _chkConvertWayPoints;
    * private Button _chkOneTour;
    * private Button _rdoDistanceRelative;
    * private Button _rdoDistanceAbsolute;
    */
   private Label _labelApiKey;
   private Text  _textApiKey;
   private Label _lblAutoOpenMS;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
      {
         /*
          * label: info
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory.fillDefaults().hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT).applyTo(label);
         label.setText(Messages.Compute_Values_Label_Info);

         /*
          * tab folder: computed values
          */
         _tabFolder = new TabFolder(container, SWT.TOP);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            //tab GOVSS
            final TabItem tabGovss = new TabItem(_tabFolder, SWT.NONE);
            tabGovss.setControl(createUI_10_Govss(_tabFolder));
            tabGovss.setText("GOVSS");//Messages.Compute_Values_Group_Smoothing);
         }
      }

      return _tabFolder;
   }

   /**
    * UI for ....TODO
    */
   private Control createUI_10_Govss(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText(Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         // checkbox: convert waypoints
         {
            // label
            _labelApiKey = new Label(container, SWT.WRAP);
            _labelApiKey.setText("Critical velocity");//Messages.Pref_Weather_Label_ApiKey);
            GridDataFactory.fillDefaults()
                  .indent(DEFAULT_DESCRIPTION_WIDTH, 0)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_labelApiKey);

            // text
            _textApiKey = new Text(container, SWT.BORDER);
            _textApiKey.setToolTipText(Messages.Pref_Weather_Label_ApiKey_Tooltip);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_textApiKey);

            // label: ms
            _lblAutoOpenMS = new Label(container, SWT.NONE);
            _lblAutoOpenMS.setText(UI.UNIT_LABEL_PACE);
         }
         {
            // label
            {
               final Label label = new Label(container, SWT.NONE);
               GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
               //label.setText(Messages.PrefPage_GPX_Label_DistanceValues);
            }

            // radio
            {
               GridDataFactory.fillDefaults()//
                     .indent(_pc.convertWidthInCharsToPixels(3), 0)
                     .applyTo(container);
               GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
               {
                  //_rdoDistanceAbsolute = new Button(container, SWT.RADIO);
                  // _rdoDistanceAbsolute.setText(Messages.PrefPage_GPX_Radio_DistanceAbsolute);
                  // _rdoDistanceAbsolute.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceAbsolute_Tooltip);

                  //_rdoDistanceRelative = new Button(container, SWT.RADIO);
                  // _rdoDistanceRelative.setText(Messages.PrefPage_GPX_Radio_DistanceRelative);
                  // _rdoDistanceRelative.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceRelative_Tooltip);
               }
            }
         }
      }
      return container;

   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      DEFAULT_DESCRIPTION_WIDTH = _pc.convertWidthInCharsToPixels(80);
      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);

   }

   @Override
   protected void performDefaults() {

      // merge all tracks into one tour
      //	_chkOneTour.setSelection(RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);

      // convert waypoints
      //	_chkConvertWayPoints.setSelection(RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT);

      // relative/absolute distance
      //final boolean isRelativeDistance = _prefStore.getDefaultBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

      //_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
      //	_rdoDistanceRelative.setSelection(isRelativeDistance);

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
       * // merge all tracks into one tour
       * final boolean isMergeIntoOneTour = Util.getStateBoolean(
       * _importState,
       * RawDataView.STATE_IS_MERGE_TRACKS,
       * RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);
       * //_chkOneTour.setSelection(isMergeIntoOneTour);
       * // convert waypoints
       * final boolean isConvertWayPoints = Util.getStateBoolean(
       * _importState,
       * RawDataView.STATE_IS_CONVERT_WAYPOINTS,
       * RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT);
       */
      //_chkConvertWayPoints.setSelection(isConvertWayPoints);

      // relative/absolute distance
      //final boolean isRelativeDistance = _prefStore.getBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

      //	_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
      //	_rdoDistanceRelative.setSelection(isRelativeDistance);
   }

   private void saveState() {

      // merge all tracks into one tour
      //	final boolean isMergeIntoOneTour = _chkOneTour.getSelection();
      //	_importState.put(RawDataView.STATE_IS_MERGE_TRACKS, isMergeIntoOneTour);
      //	_rawDataMgr.setMergeTracks(isMergeIntoOneTour);

      // convert waypoints
      //	final boolean isConvertWayPoints = _chkConvertWayPoints.getSelection();
//		_importState.put(RawDataView.STATE_IS_CONVERT_WAYPOINTS, isConvertWayPoints);
      //	_rawDataMgr.setState_ConvertWayPoints(isConvertWayPoints);

      // relative/absolute distance
      //_prefStore.setValue(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE, _rdoDistanceRelative.getSelection());
   }
}
