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

import static org.eclipse.swt.events.FocusListener.focusGainedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;

import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogGotoLocation extends TitleAreaDialog {

   private static final String          GEO_LOCATION_FORMAT = "%.6f";                                                                 //$NON-NLS-1$

   private static final IDialogSettings _state              = TourbookPlugin.getState("net.tourbook.map.bookmark.DialogGotoLocation");//$NON-NLS-1$

   private GeoPosition                  _currentGeoPosition;
   private GeoPosition                  _enteredGeoPosition;

   private boolean                      _isCreateMapBookmark;
   private String                       _bookmarkName;

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Button _chkIsCreateMapBookmark;

   private Label  _lblBookmarkName;

   private Text   _txtBookmarkName;
   private Text   _txtLatitude;
   private Text   _txtLongitude;

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
      shell.setText(Messages.Map_Location_Dialog_Title);
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Map_Location_Dialog_Title);
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

      initUI(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(shellContainer);
//      shellContainer.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI(shellContainer);
      }

      restoreState();
      enableControls();

      setMessage(Messages.Map_Location_Dialog_Message);

      return shellContainer;
   }

   private Control createUI(final Composite parent) {

      final GridDataFactory latLonGridData = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT);

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
            _txtLatitude.addFocusListener(focusGainedAdapter(focusEvent -> onFocusGained(focusEvent)));
            _txtLatitude.addModifyListener(modifyEvent -> onLatLon_Modify(modifyEvent));
            _txtLatitude.addVerifyListener(verifyEvent -> onLatLon_Verify(verifyEvent));
            latLonGridData.applyTo(_txtLatitude);
         }
         {
            /*
             * Longitude
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Map_Location_Label_LocationLongitude);

            _txtLongitude = new Text(container, SWT.BORDER);
            _txtLongitude.addFocusListener(FocusListener.focusGainedAdapter(focusEvent -> onFocusGained(focusEvent)));
            _txtLongitude.addModifyListener(modifyEvent -> onLatLon_Modify(modifyEvent));
            _txtLongitude.addVerifyListener(verifyEvent -> onLatLon_Verify(verifyEvent));
            latLonGridData.applyTo(_txtLongitude);
         }

         {
            /*
             * Create map bookmark ?
             */
            _chkIsCreateMapBookmark = new Button(container, SWT.CHECK);
            _chkIsCreateMapBookmark.setText(Messages.Map_Location_Checkbox_IsCreateBookmark);
            _chkIsCreateMapBookmark.addSelectionListener(widgetSelectedAdapter(selectionEvent -> enableControls()));
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsCreateMapBookmark);
         }
         {
            /*
             * Bookmark name
             */
            _lblBookmarkName = new Label(container, SWT.NONE);
            _lblBookmarkName.setText(Messages.Map_Bookmark_Dialog_AddBookmark_Message);
            GridDataFactory.fillDefaults().indent(16, 0).applyTo(_lblBookmarkName);

            _txtBookmarkName = new Text(container, SWT.BORDER);
            _txtBookmarkName.addModifyListener(modifyEvent -> enableControls());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtBookmarkName);
         }
      }

      return container;
   }

   private void enableControls() {

      final boolean isCreateMapBookmark = _chkIsCreateMapBookmark.getSelection();
      final boolean isLatLonValid = isInputValid(false);

      _chkIsCreateMapBookmark.setEnabled(isLatLonValid);
      _lblBookmarkName.setEnabled(isLatLonValid && isCreateMapBookmark);
      _txtBookmarkName.setEnabled(isLatLonValid && isCreateMapBookmark);
   }

   public String getBookmarkName() {
      return _bookmarkName;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

      // for debugging: test default position/size
//    return null;
   }

   public GeoPosition getEnteredGeoPosition() {
      return _enteredGeoPosition;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   public boolean isCreateMapBookmark() {
      return _isCreateMapBookmark;
   }

   private boolean isInputValid(final boolean isShowErrorState) {

      if (isLatLonValid(_txtLatitude.getText().trim(), isShowErrorState) == false) {

         if (isShowErrorState) {
            _txtLatitude.setFocus();
         }

         return false;
      }

      if (isLatLonValid(_txtLongitude.getText().trim(), isShowErrorState) == false) {

         if (isShowErrorState) {
            _txtLongitude.setFocus();
         }

         return false;
      }

      return true;
   }

   private boolean isLatLonValid(final String valueText, final boolean isShowErrorMessage) {

      if (valueText.length() > 0) {

         try {

            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //
            // Float.parseFloat() ignores localized strings therefore the databinding converter
            // is used which provides also a good error message
            //
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            StringToNumberConverter.toFloat(true).convert(valueText);

            if (isShowErrorMessage) {
               setErrorMessage(null);
            }

            // hide initial message
            setMessage(null);

            return true;

         } catch (final IllegalArgumentException e) {

            // wrong characters are entered, display an error message

            if (isShowErrorMessage) {
               setErrorMessage(e.getLocalizedMessage());
            }

            return false;
         }

      } else {

         if (isShowErrorMessage) {
            setErrorMessage(Messages.Map_Location_Error_ValueCannotBeEmpty);
         }

         return false;
      }
   }

   @Override
   protected void okPressed() {

      if (isInputValid(true) == false) {
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

      final Text widget = (Text) modifyEvent.widget;
      final String valueText = widget.getText().trim();

      isLatLonValid(valueText, true);

      enableControls();
   }

   private void onLatLon_Verify(final VerifyEvent verifyEvent) {

      // trim text which is helpful when pasting text

      final String fieldText = verifyEvent.text;

      verifyEvent.text = fieldText.trim();
   }

   private void restoreState() {

      // fill location with current map position

      _txtLatitude.setText(String.format(GEO_LOCATION_FORMAT, _currentGeoPosition.latitude));
      _txtLongitude.setText(String.format(GEO_LOCATION_FORMAT, _currentGeoPosition.longitude));
   }

   private void saveState() {

      // convert into geoposition -> this will ensure that lat/lon values are in the correct range
      _enteredGeoPosition = new GeoPosition(
            Float.parseFloat(_txtLatitude.getText().trim()),
            Float.parseFloat(_txtLongitude.getText().trim()));

      _isCreateMapBookmark = _chkIsCreateMapBookmark.getSelection();
      _bookmarkName = _txtBookmarkName.getText();
   }

}
