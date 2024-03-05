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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourLocationPoint;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DialogSetTourLocationInMap extends TitleAreaDialog {

// SET_FORMATTING_OFF

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.tour.location.DialogSetTourLocationInMap");   //$NON-NLS-1$

// SET_FORMATTING_ON

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite _containerTourLocation;
   private Composite _shellContainer;

   public DialogSetTourLocationInMap() {

      super(Display.getDefault().getActiveShell());

      setShellStyle(SWT.DIALOG_TRIM
//          | SWT.APPLICATION_MODAL
//          | SWT.MAX
            | SWT.RESIZE
            | getDefaultOrientation());

      // this is a non-modal dialog
      setBlockOnOpen(false);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_ResizeTourLocation_Title);

      shell.addDisposeListener(disposeEvent -> dispose());

   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_ResizeTourLocation_Title);
      setMessage(Messages.Dialog_ResizeTourLocation_Message);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      // OK -> Resize
//    _btnResize = createButton(parent, IDialogConstants.OK_ID, Messages.Dialog_ResizeTourLocation_Action_Resize, true);

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      _shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_shellContainer);
      GridLayoutFactory.swtDefaults().margins(6, 6).numColumns(2).applyTo(_shellContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_GREEN);
      {
         _containerTourLocation = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerTourLocation);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_containerTourLocation);
      }

      fillUI();

      restoreState();

      enableControls();

//      _spinnerDistance_Top.setFocus();

      return _shellContainer;
   }

   private void createUI_50_TourLocationInfo(final TourLocation tourLocation) {

      // dispose previous inner container
      final Control[] containerChildren = _containerTourLocation.getChildren();
      if (containerChildren.length > 0) {
         containerChildren[0].dispose();
      }

      // create new inner container
      final Composite container = new Composite(_containerTourLocation, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         TourLocationUI.createUI(container, tourLocation);
      }
   }

   private void dispose() {

   }

   private void enableControls() {

   }

   private void fillUI() {

   }

   private void fireModifyEvent() {

      saveState();

//      TourManager.fireEventWithCustomData(
//
//            TourEventId.TOUR_LOCATION_SELECTION,
//            Arrays.asList(_tourLocation),
//            null);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

// for debugging: test default position/size
//    return null;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

   }

   @Override
   protected void okPressed() {

      // OK button is not available, all changes are live updated

      super.okPressed();
   }

   private void openDialog() {

      final Shell shell = getShell();

      if (shell == null) {

         open();

      } else if (shell.isVisible() == false) {

         shell.setVisible(true);
      }
   }

   private void restoreState() {

      updateUIfromModel();

   }

   private void saveState() {

   }

   private void updateModelFromUI() {}

   private void updateModelFromUI_AndFireEvent() {

      updateModelFromUI();

      enableControls();

      fireModifyEvent();
   }

   public void updateUI(final TourLocationPoint tourLocationPoint) {
      // TODO Auto-generated method stub

      openDialog();

      createUI_50_TourLocationInfo(tourLocationPoint.getTourLocation());

      _shellContainer.layout(true, true);
   }

   /**
    * Update UI from {@link #_tourLocation}
    */
   private void updateUIfromModel() {}

}
