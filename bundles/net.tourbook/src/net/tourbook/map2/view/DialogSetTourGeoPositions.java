/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to set tour geo positions from the current mouse position
 */
public class DialogSetTourGeoPositions extends Dialog {

   private static final String          ID                              = "net.tourbook.map2.view.DialogSetTourGeoPositions"; //$NON-NLS-1$

   private static final String          STATE_IS_SET_GEO_POSITION_START = "STATE_IS_SET_GEO_POSITION_START";                  //$NON-NLS-1$
   private static final String          STATE_IS_SET_GEO_POSITION_END   = "STATE_IS_SET_GEO_POSITION_END";                    //$NON-NLS-1$

   private static final IDialogSettings _state                          = TourbookPlugin.getState(ID);

   private TourData                     _tourData;
   private GeoPosition                  _geoPosition;

   private boolean                      _isInRestore;

   /*
    * UI controls
    */
   private Button _chkGeoPosition_Start;
   private Button _chkGeoPosition_End;

   public DialogSetTourGeoPositions(final TourData tourData, final GeoPosition geoPosition) {

      super(PlatformUI.getWorkbench().getDisplay().getActiveShell());

      _tourData = tourData;
      _geoPosition = geoPosition;
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText("Set Tour Geo Positions");
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Save
      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Apply);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI();

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      parent.addDisposeListener(disposeEvent -> onDispose());

      restoreState();

      // run async, the OK button is created lately
      parent.getDisplay().asyncExec(() -> enableControls());

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */

   private void createUI(final Composite parent) {

      final SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText("Set geo position %7.4f  %7.4f\n\nfor the tour \"%s\"\n\ninto".formatted(
                  _geoPosition.latitude,
                  _geoPosition.longitude,
                  TourManager.getTourTitle(_tourData)));
         }
         {
            /*
             * Start position
             */
            _chkGeoPosition_Start = new Button(container, SWT.CHECK);
            _chkGeoPosition_Start.setText("Tour s&tart");
            _chkGeoPosition_Start.addSelectionListener(selectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 10)
                  .applyTo(_chkGeoPosition_Start);
         }
         {
            /*
             * End position
             */
            _chkGeoPosition_End = new Button(container, SWT.CHECK);
            _chkGeoPosition_End.setText("Tour &end");
            _chkGeoPosition_End.addSelectionListener(selectionListener);
         }
      }
   }

   private void enableControls() {

      final Button okButton = getButton(IDialogConstants.OK_ID);

// SET_FORMATTING_OFF

      final boolean isLocationEnabled_Start  = _chkGeoPosition_Start.getSelection();
      final boolean isLocationEnabled_End    = _chkGeoPosition_End.getSelection();

      okButton.setEnabled(isLocationEnabled_Start || isLocationEnabled_End);

// SET_FORMATTING_ON
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      return _state;
   }

   @Override
   protected int getDialogBoundsStrategy() {

      // keep only window position
      return DIALOG_PERSISTLOCATION;
   }

   private void initUI() {

   }

   @Override
   protected void okPressed() {

      setGeoPositions();

      super.okPressed();
   }

   private void onDispose() {

//      UI.disposeResource(_imageDialog);
   }

   private void onModify() {

      if (_isInRestore) {
         return;
      }

      enableControls();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final boolean isLocationEnabeled_Start = Util.getStateBoolean(_state, STATE_IS_SET_GEO_POSITION_START,   true);
      final boolean isLocationEnabeled_End   = Util.getStateBoolean(_state, STATE_IS_SET_GEO_POSITION_END,     true);

      _chkGeoPosition_Start   .setSelection(isLocationEnabeled_Start);
      _chkGeoPosition_End     .setSelection(isLocationEnabeled_End);

// SET_FORMATTING_ON

      _chkGeoPosition_Start.setFocus();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_SET_GEO_POSITION_START,  _chkGeoPosition_Start.getSelection());
      _state.put(STATE_IS_SET_GEO_POSITION_END,    _chkGeoPosition_End.getSelection());

// SET_FORMATTING_ON
   }

   private void setGeoPositions() {

      final boolean isStartPosition = _chkGeoPosition_Start.getSelection();
      final boolean isEndPosition = _chkGeoPosition_End.getSelection();

      int[] timeSerie = _tourData.timeSerie; 
      
      _tourData is null when setting into existing geo pos tour
      
//      java.lang.NullPointerException: Cannot read field "timeSerie" because "this._tourData" is null
//         at net.tourbook.map2.view.DialogSetTourGeoPositions.setGeoPositions(DialogSetTourGeoPositions.java:240)
//         at net.tourbook.map2.view.DialogSetTourGeoPositions.okPressed(DialogSetTourGeoPositions.java:191)
//         at org.eclipse.jface.dialogs.Dialog.buttonPressed(Dialog.java:468)
      
      double[] latSerie = _tourData.latitudeSerie;
      double[] lonSerie = _tourData.longitudeSerie;

      if (timeSerie == null) {

         timeSerie = new int[2];

      } else if (timeSerie.length == 1) {

         // preserve old values

         final int timeValue_0 = timeSerie[0];

         timeSerie = new int[2];

         timeSerie[0] = timeValue_0;
      }

      if (latSerie == null) {

         latSerie = new double[2];
         lonSerie = new double[2];

      } else if (latSerie.length == 1) {

         // preserve old values
         final double latValue_0 = latSerie[0];
         final double lonValue_0 = lonSerie[0];

         latSerie = new double[2];
         lonSerie = new double[2];

         latSerie[0] = latValue_0;
         lonSerie[0] = lonValue_0;
      }

      final int tourElapsedTime = (int) _tourData.getTourDeviceTime_Elapsed();
      final double latitude = _geoPosition.latitude;
      final double longitude = _geoPosition.longitude;

      if (isStartPosition && isEndPosition) {

         timeSerie[0] = 0;
         timeSerie[1] = tourElapsedTime;

         latSerie[0] = latitude;
         latSerie[1] = latitude;

         lonSerie[0] = longitude;
         lonSerie[1] = longitude;

      } else if (isStartPosition) {

         timeSerie[0] = 0;

         latSerie[0] = latitude;
         lonSerie[0] = longitude;

      } else if (isEndPosition) {

         timeSerie[1] = tourElapsedTime;

         latSerie[1] = latitude;
         lonSerie[1] = longitude;
      }

// SET_FORMATTING_OFF

      // prevent 0 values

      if (timeSerie[1] == 0)  { timeSerie[1] = tourElapsedTime; }

      if (latSerie[0] == 0)   { latSerie[0]  = latitude; }
      if (latSerie[1] == 0)   { latSerie[1]  = latitude; }

      if (lonSerie[0] == 0)   { lonSerie[0]  = longitude; }
      if (lonSerie[1] == 0)   { lonSerie[1]  = longitude; }

// SET_FORMATTING_ON

      // update tour values
      _tourData.timeSerie = timeSerie;
      _tourData.latitudeSerie = latSerie;
      _tourData.longitudeSerie = lonSerie;

      TourManager.saveModifiedTour(_tourData);
   }

}
