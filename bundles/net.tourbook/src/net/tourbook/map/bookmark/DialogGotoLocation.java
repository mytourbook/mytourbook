/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map.bookmark;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;

import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogGotoLocation extends TitleAreaDialog {

//   private static final String       NL = UI.NEW_LINE1;

   private static final NumberFormat _nf0;

   static {

      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.map.bookmark.DialogGotoLocation");//$NON-NLS-1$

   private double                       _latitude;
   private double                       _longitude;

   private ModifyListener               _modifyFloatValue;

   /*
    * UI controls
    */
   private Text        _txtLatitude;
   private Text        _txtLongitude;

   private GeoPosition _currentGeoPosition;

   public DialogGotoLocation(final GeoPosition currentGeoPosition) {

      super(Display.getDefault().getActiveShell());

      _currentGeoPosition = currentGeoPosition;

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Map_Location_DialogTitle);
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Map_Location_DialogTitle);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      // OK -> Goto Location
      createButton(parent, IDialogConstants.OK_ID, Messages.Map_Location_Action_GotoLocation, true);

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI();

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(shellContainer);
//      shellContainer.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI(shellContainer);
      }

      restoreState();

      setMessage("The current position is prefilled");

      return shellContainer;
   }

   private Control createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Latitude
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Map_Location_Label_LocationLatitude);

            _txtLatitude = new Text(container, SWT.BORDER);
            _txtLatitude.addFocusListener(FocusListener.focusGainedAdapter(focusEvent -> onFocusGained(focusEvent)));
//            _txtLatitude.addModifyListener(modifyEvent -> onLatLon_Modify(modifyEvent));
            _txtLatitude.addModifyListener(_modifyFloatValue);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtLatitude);
         }
         {
            /*
             * Latitude
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Map_Location_Label_LocationLongitude);

            _txtLongitude = new Text(container, SWT.BORDER);
            _txtLongitude.addFocusListener(FocusListener.focusGainedAdapter(focusEvent -> onFocusGained(focusEvent)));
            _txtLongitude.addModifyListener(_modifyFloatValue);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtLongitude);
         }
      }

      return container;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

//      return null;
   }

   public double getLatitude() {
      return _latitude;
   }

   public double getLongitude() {
      return _longitude;
   }

   private void initUI() {

      _modifyFloatValue = modifyEvent -> {

         final Text widget = (Text) modifyEvent.widget;
         final String valueText = widget.getText().trim();

         isLatLonValid(valueText);
      };
   }

   private boolean isInputValid() {

      if (isLatLonValid(_txtLatitude.getText()) == false) {

         _txtLatitude.setFocus();
         return false;
      }

      if (isLatLonValid(_txtLongitude.getText()) == false) {

         _txtLongitude.setFocus();
         return false;
      }

      return true;
   }

   private boolean isLatLonValid(final String valueText) {

      if (valueText.length() > 0) {

         try {

            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //
            // Float.parseFloat() ignores localized strings therefore the databinding converter is used
            // which provides also a good error message
            //
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            StringToNumberConverter.toFloat(true).convert(valueText);

            setErrorMessage(null);

            // hide initial message
            setMessage(null);

            return true;

         } catch (final IllegalArgumentException e) {

            // wrong characters are entered, display an error message

            setErrorMessage(e.getLocalizedMessage());

            return false;
         }

      } else {

         setErrorMessage("Value cannot be empty");

         return false;
      }
   }

   @Override
   protected void okPressed() {

      if (isInputValid() == false) {
         return;
      }

      saveState();

      super.okPressed();
   }

   private void onFocusGained(final FocusEvent focusEvent) {

      final Text textField = (Text) focusEvent.widget;
      textField.selectAll();
   }

   private void onLatLon_Modify(final ModifyEvent modifyEvent) {
      // TODO Auto-generated method stub

//      modifyEvent.
   }

   private void onLatLon_Verify(final VerifyEvent verifyEvent) {

      final String fieldText = verifyEvent.text;

      final boolean isValueOK = fieldText.length() == 0

            // allow DEL key
            ? true

            : fieldText.matches("^[+-]?([0-9]*[.])?[0-9]+$");

      verifyEvent.doit = isValueOK;
   }

   private void restoreState() {

      // fill location with current map position

      _txtLatitude.setText(String.format("%.6f", _currentGeoPosition.latitude));
      _txtLongitude.setText(String.format("%.6f", _currentGeoPosition.longitude));
   }

   private void saveState() {

      _latitude = Float.parseFloat(_txtLatitude.getText());
      _longitude = Float.parseFloat(_txtLongitude.getText());
   }

}
