/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to set tour location names without geo positions
 */
public class DialogStartEndLocation extends Dialog {

   private static final String       ID                                             = "net.tourbook.tour.location.DialogStartEndLocation"; //$NON-NLS-1$

   private static final String       STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_START = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_START";    //$NON-NLS-1$
   private static final String       STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_END   = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_END";      //$NON-NLS-1$
   private static final String       STATE_IS_LOCATION_ENABELED_START               = "STATE_IS_LOCATION_ENABELED_START";                  //$NON-NLS-1$
   private static final String       STATE_IS_LOCATION_ENABELED_END                 = "STATE_IS_LOCATION_ENABELED_END";                    //$NON-NLS-1$
   private static final String       STATE_IS_REMOVE_LOCATION_ASSOCIATION_START     = "STATE_IS_REMOVE_LOCATION_ASSOCIATION_START";        //$NON-NLS-1$
   private static final String       STATE_IS_REMOVE_LOCATION_ASSOCIATION_END       = "STATE_IS_REMOVE_LOCATION_ASSOCIATION_END";          //$NON-NLS-1$

   private final IDialogSettings     _state                                         = TourbookPlugin.getState(ID);

   private List<TourData>            _allSelectedTours;

   private AutoComplete_ComboInputMT _autocomplete_Location_Start;
   private AutoComplete_ComboInputMT _autocomplete_Location_End;

   private boolean                   _isInRestore;

   /*
    * UI controls
    */
   private Image  _imageDialog;

   private Button _chkLocation_Start;
   private Button _chkLocation_End;
   private Button _chkRemoveLocationAssoc_Start;
   private Button _chkRemoveLocationAssoc_End;

   private Combo  _comboLocation_Start;
   private Combo  _comboLocation_End;

   public DialogStartEndLocation(final List<TourData> allSelectedTours) {

      super(PlatformUI.getWorkbench().getDisplay().getActiveShell());

      _allSelectedTours = allSelectedTours;

      // dialog image
      _imageDialog = TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd).createImage();
      setDefaultImage(_imageDialog);
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
      shell.setText(Messages.Dialog_StartEndLocation_Title);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Save
      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI();

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      fillUI();

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

      final PixelConverter pc = new PixelConverter(parent);

      final GridDataFactory gdText = GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(16, 0)
            .hint(pc.convertWidthInCharsToPixels(80), SWT.DEFAULT);

      final GridDataFactory gdIndent = GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(16, 0);

      final SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_StartEndLocation_Label_SetLocationName.formatted(_allSelectedTours.size()));
            GridDataFactory.fillDefaults().applyTo(label);
         }
         {
            /*
             * Start location
             */
            _chkLocation_Start = new Button(container, SWT.CHECK);
            _chkLocation_Start.setText(Messages.Dialog_StartEndLocation_Checkbox_StartLocationName);
            _chkLocation_Start.addSelectionListener(selectionListener);
            GridDataFactory.fillDefaults()

                  // more vertical space
                  .indent(0, 10)

                  .applyTo(_chkLocation_Start);

            {
               // autocomplete combo

               _comboLocation_Start = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboLocation_Start.setText(UI.EMPTY_STRING);
               _comboLocation_Start.addModifyListener(modifyEvent -> onModify());
               gdText.applyTo(_comboLocation_Start);

               _autocomplete_Location_Start = new AutoComplete_ComboInputMT(_comboLocation_Start);
            }
            {
               _chkRemoveLocationAssoc_Start = new Button(container, SWT.CHECK);
               _chkRemoveLocationAssoc_Start.setText(Messages.Dialog_StartEndLocation_Checkbox_RemoveStartLocationAssociation);
               _chkRemoveLocationAssoc_Start.setToolTipText(
                     Messages.Dialog_StartEndLocation_Checkbox_RemoveStartLocationAssociation_Tooltip);
               _chkRemoveLocationAssoc_Start.addSelectionListener(selectionListener);
               gdIndent.applyTo(_chkRemoveLocationAssoc_Start);
            }
         }
         {
            /*
             * End location
             */
            _chkLocation_End = new Button(container, SWT.CHECK);
            _chkLocation_End.setText(Messages.Dialog_StartEndLocation_Checkbox_EndLocationName);
            _chkLocation_End.addSelectionListener(selectionListener);
            GridDataFactory.fillDefaults().indent(0, 15).applyTo(_chkLocation_End);

            {
               // autocomplete combo

               _comboLocation_End = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboLocation_End.setText(UI.EMPTY_STRING);
               _comboLocation_End.addModifyListener(modifyEvent -> onModify());
               gdText.applyTo(_comboLocation_End);

               _autocomplete_Location_End = new AutoComplete_ComboInputMT(_comboLocation_End);
            }
            {
               _chkRemoveLocationAssoc_End = new Button(container, SWT.CHECK);
               _chkRemoveLocationAssoc_End.setText(Messages.Dialog_StartEndLocation_Checkbox_RemoveEndLocationAssociation);
               _chkRemoveLocationAssoc_End.setToolTipText(
                     Messages.Dialog_StartEndLocation_Checkbox_RemoveEndLocationAssociation_Tooltip);
               _chkRemoveLocationAssoc_End.addSelectionListener(selectionListener);
               gdIndent.applyTo(_chkRemoveLocationAssoc_End);
            }
         }
      }
   }

   private void enableControls() {

      final Button okButton = getButton(IDialogConstants.OK_ID);

// SET_FORMATTING_OFF

      final boolean isLocationEnabled_Start  = _chkLocation_Start.getSelection();
      final boolean isLocationEnabled_End    = _chkLocation_End.getSelection();

      _chkRemoveLocationAssoc_Start .setEnabled(isLocationEnabled_Start);
      _chkRemoveLocationAssoc_End   .setEnabled(isLocationEnabled_End);

      _comboLocation_Start          .setEnabled(isLocationEnabled_Start);
      _comboLocation_End            .setEnabled(isLocationEnabled_End);

      okButton.setEnabled(isLocationEnabled_Start || isLocationEnabled_End);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      // fill combobox
      final ConcurrentSkipListSet<String> allStartPlaces = TourDatabase.getCachedFields_AllTourPlaceStarts();
      for (final String startPlace : allStartPlaces) {
         if (startPlace != null) {
            _comboLocation_Start.add(startPlace);
         }
      }

      // fill combobox
      final ConcurrentSkipListSet<String> allEndPlaces = TourDatabase.getCachedFields_AllTourPlaceEnds();
      for (final String endPlace : allEndPlaces) {
         if (endPlace != null) {
            _comboLocation_End.add(endPlace);
         }
      }
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

      final boolean isStartLocation = _chkLocation_Start.getSelection();
      final boolean isEndLocation = _chkLocation_End.getSelection();

      final String locationName_Start = isStartLocation ? _comboLocation_Start.getText().trim() : null;
      final String locationName_End = isEndLocation ? _comboLocation_End.getText().trim() : null;

      TourLocationManager.setTourLocations(_allSelectedTours,

            locationName_Start,
            locationName_End,

            _chkRemoveLocationAssoc_Start.getSelection(),
            _chkRemoveLocationAssoc_End.getSelection()

      );

      super.okPressed();
   }

   private void onDispose() {

      UI.disposeResource(_imageDialog);
   }

   private void onModify() {

      if (_isInRestore) {
         return;
      }

      enableControls();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final boolean isLocationEnabeled_Start = Util.getStateBoolean(_state, STATE_IS_LOCATION_ENABELED_START, true);
      final boolean isLocationEnabeled_End   = Util.getStateBoolean(_state, STATE_IS_LOCATION_ENABELED_END, true);

      _chkLocation_Start            .setSelection(isLocationEnabeled_Start);
      _chkLocation_End              .setSelection(isLocationEnabeled_End);
      _chkRemoveLocationAssoc_Start .setSelection(Util.getStateBoolean(_state, STATE_IS_REMOVE_LOCATION_ASSOCIATION_START, false));
      _chkRemoveLocationAssoc_End   .setSelection(Util.getStateBoolean(_state, STATE_IS_REMOVE_LOCATION_ASSOCIATION_END, false));

      _autocomplete_Location_Start  .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_START);
      _autocomplete_Location_End    .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_END);

// SET_FORMATTING_ON

      setupLocationNames();

      // set focus to the first enabled field
      if (isLocationEnabeled_Start) {

         _comboLocation_Start.setFocus();

      } else if (isLocationEnabeled_End) {

         _comboLocation_End.setFocus();
      }
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_LOCATION_ENABELED_START,             _chkLocation_Start.getSelection());
      _state.put(STATE_IS_LOCATION_ENABELED_END,               _chkLocation_End.getSelection());
      _state.put(STATE_IS_REMOVE_LOCATION_ASSOCIATION_START,   _chkRemoveLocationAssoc_Start.getSelection());
      _state.put(STATE_IS_REMOVE_LOCATION_ASSOCIATION_END,     _chkRemoveLocationAssoc_End.getSelection());

      _autocomplete_Location_Start  .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_START);
      _autocomplete_Location_End    .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LOCATION_END);

// SET_FORMATTING_ON
   }

   /**
    * Set location content
    */
   private void setupLocationNames() {

      _isInRestore = true;

      String commonTourStartPlace = null;
      String commonTourEndPlace = null;

      boolean isCommonStart = true;
      boolean isCommonEnd = true;

      for (final TourData tourData : _allSelectedTours) {

         final String tourStartPlace = tourData.getTourStartPlace().trim();
         final String tourEndPlace = tourData.getTourEndPlace().trim();

         if (isCommonStart && tourStartPlace.length() > 0) {

            if (commonTourStartPlace == null) {

               commonTourStartPlace = tourStartPlace;

            } else {

               if (commonTourStartPlace.equals(tourStartPlace) == false) {

                  isCommonStart = false;
               }
            }
         }

         if (isCommonEnd && tourEndPlace.length() > 0) {

            if (commonTourEndPlace == null) {

               commonTourEndPlace = tourEndPlace;

            } else {

               if (commonTourEndPlace.equals(tourEndPlace) == false) {

                  isCommonEnd = false;
               }
            }
         }
      }

      if (isCommonStart && commonTourStartPlace != null) {

         _comboLocation_Start.setText(commonTourStartPlace);
      }

      if (isCommonEnd && commonTourEndPlace != null) {

         _comboLocation_End.setText(commonTourEndPlace);
      }

      _isInRestore = false;
   }
}
