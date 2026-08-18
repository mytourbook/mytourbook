/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.util.concurrent.ConcurrentSkipListSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.TourPhotoManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhoto_PathConversion extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String           ID                                        = "net.tourbook.preferences.PrefPagePhoto_PathConversion"; //$NON-NLS-1$

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_1   = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_1";               //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_2   = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_2";               //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_3   = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_3";               //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_1 = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_1";             //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_2 = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_2";             //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_3 = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_3";             //$NON-NLS-1$
   public static final String           STATE_IS_CONVERSION_1                     = "STATE_IS_CONVERSION_1";                                 //$NON-NLS-1$
   public static final String           STATE_IS_CONVERSION_2                     = "STATE_IS_CONVERSION_2";                                 //$NON-NLS-1$
   public static final String           STATE_IS_CONVERSION_3                     = "STATE_IS_CONVERSION_3";                                 //$NON-NLS-1$
   public static final String           STATE_CONVERSION_LINUX_1                  = "STATE_CONVERSION_LINUX_1";                              //$NON-NLS-1$
   public static final String           STATE_CONVERSION_LINUX_2                  = "STATE_CONVERSION_LINUX_2";                              //$NON-NLS-1$
   public static final String           STATE_CONVERSION_LINUX_3                  = "STATE_CONVERSION_LINUX_3";                              //$NON-NLS-1$
   public static final String           STATE_CONVERSION_WINDOWS_1                = "STATE_CONVERSION_WINDOWS_1";                            //$NON-NLS-1$
   public static final String           STATE_CONVERSION_WINDOWS_2                = "STATE_CONVERSION_WINDOWS_2";                            //$NON-NLS-1$
   public static final String           STATE_CONVERSION_WINDOWS_3                = "STATE_CONVERSION_WINDOWS_3";                            //$NON-NLS-1$

   private static final int             PATH_WIDTH                                = 100;

   private static final IDialogSettings _state                                    = TourbookPlugin.getState(ID);

   private PixelConverter               _pc;

   private SelectionListener            _defaultSelectionListener;
   private ModifyListener               _defaultModifyListener;

   private boolean                      _isModified;
   private boolean                      _isInUpdate;

   /*
    * UI controls
    */
   private Button                    _chkConversion1;
   private Button                    _chkConversion2;
   private Button                    _chkConversion3;

   private Label                     _lblLinux1;
   private Label                     _lblLinux2;
   private Label                     _lblLinux3;
   private Label                     _lblWindows1;
   private Label                     _lblWindows2;
   private Label                     _lblWindows3;

   private Combo                     _comboLinux1;
   private Combo                     _comboLinux2;
   private Combo                     _comboLinux3;
   private Combo                     _comboWindows1;
   private Combo                     _comboWindows2;
   private Combo                     _comboWindows3;

   private AutoComplete_ComboInputMT _autoComplete_Linux1;
   private AutoComplete_ComboInputMT _autoComplete_Linux2;
   private AutoComplete_ComboInputMT _autoComplete_Linux3;
   private AutoComplete_ComboInputMT _autoComplete_Windows1;
   private AutoComplete_ComboInputMT _autoComplete_Windows2;
   private AutoComplete_ComboInputMT _autoComplete_Windows3;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      fillUI();

      restoreState();

      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final GridDataFactory gdLabel = GridDataFactory.fillDefaults()
            .indent(16, 0);

      final GridDataFactory gdCombo = GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(PATH_WIDTH, SWT.DEFAULT);

      final GridDataFactory gdCheckbox = GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Title
             */
            final Label label = UI.createLabel(
                  container,
                  Messages.Pref_Photo_Convert_Label_ConvertSavedPhotoImageFilepath,
                  SWT.WRAP);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT)
                  .applyTo(label);
         }
         {
            /*
             * 1. Conversion
             */

            {
               _chkConversion1 = new Button(container, SWT.CHECK);
               _chkConversion1.setText(Messages.Pref_Photo_Convert_Checkbox_Conversion1);
               _chkConversion1.addSelectionListener(_defaultSelectionListener);

               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .span(2, 1).indent(0, 10).applyTo(_chkConversion1);
            }
            {
               /*
                * Linux 1
                */

               _lblLinux1 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Linux);
               gdLabel.applyTo(_lblLinux1);

               _comboLinux1 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboLinux1.setText(UI.EMPTY_STRING);
               _comboLinux1.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboLinux1);

               _autoComplete_Linux1 = new AutoComplete_ComboInputMT(_comboLinux1);
            }
            {
               /*
                * Windows 1
                */

               _lblWindows1 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Windows);
               gdLabel.applyTo(_lblWindows1);

               _comboWindows1 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboWindows1.setText(UI.EMPTY_STRING);
               _comboWindows1.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboWindows1);

               _autoComplete_Windows1 = new AutoComplete_ComboInputMT(_comboWindows1);
            }
         }
         {
            /*
             * 2. Conversion
             */

            {
               _chkConversion2 = new Button(container, SWT.CHECK);
               _chkConversion2.setText(Messages.Pref_Photo_Convert_Checkbox_Conversion2);
               _chkConversion2.addSelectionListener(_defaultSelectionListener);
               gdCheckbox.applyTo(_chkConversion2);
            }
            {
               /*
                * Linux 2
                */

               _lblLinux2 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Linux);
               gdLabel.applyTo(_lblLinux2);

               _comboLinux2 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboLinux2.setText(UI.EMPTY_STRING);
               _comboLinux2.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboLinux2);

               _autoComplete_Linux2 = new AutoComplete_ComboInputMT(_comboLinux2);
            }
            {
               /*
                * Windows 2
                */

               _lblWindows2 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Windows);
               gdLabel.applyTo(_lblWindows2);

               _comboWindows2 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboWindows2.setText(UI.EMPTY_STRING);
               _comboWindows2.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboWindows2);

               _autoComplete_Windows2 = new AutoComplete_ComboInputMT(_comboWindows2);
            }
         }
         {
            /*
             * 3. Conversion
             */

            {
               _chkConversion3 = new Button(container, SWT.CHECK);
               _chkConversion3.setText(Messages.Pref_Photo_Convert_Checkbox_Conversion3);
               _chkConversion3.addSelectionListener(_defaultSelectionListener);
               gdCheckbox.applyTo(_chkConversion3);
            }
            {
               /*
                * Linux 3
                */

               _lblLinux3 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Linux);
               gdLabel.applyTo(_lblLinux3);

               _comboLinux3 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboLinux3.setText(UI.EMPTY_STRING);
               _comboLinux3.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboLinux3);

               _autoComplete_Linux3 = new AutoComplete_ComboInputMT(_comboLinux3);
            }
            {
               /*
                * Windows 3
                */

               _lblWindows3 = UI.createLabel(container, Messages.Pref_Photo_Convert_Label_Windows);
               gdLabel.applyTo(_lblWindows3);

               _comboWindows3 = new Combo(container, SWT.BORDER | SWT.FLAT);
               _comboWindows3.setText(UI.EMPTY_STRING);
               _comboWindows3.addModifyListener(_defaultModifyListener);
               gdCombo.applyTo(_comboWindows3);

               _autoComplete_Windows3 = new AutoComplete_ComboInputMT(_comboWindows3);
            }
         }
      }

      return container;
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isConversion1 = _chkConversion1.getSelection();
      final boolean isConversion2 = _chkConversion2.getSelection();
      final boolean isConversion3 = _chkConversion3.getSelection();

      _lblLinux1        .setEnabled(isConversion1);
      _lblLinux2        .setEnabled(isConversion2);
      _lblLinux3        .setEnabled(isConversion3);
      _lblWindows1      .setEnabled(isConversion1);
      _lblWindows2      .setEnabled(isConversion2);
      _lblWindows3      .setEnabled(isConversion3);

      _comboLinux1      .setEnabled(isConversion1);
      _comboLinux2      .setEnabled(isConversion2);
      _comboLinux3      .setEnabled(isConversion3);
      _comboWindows1    .setEnabled(isConversion1);
      _comboWindows2    .setEnabled(isConversion2);
      _comboWindows3    .setEnabled(isConversion3);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      // fill combobox
      final ConcurrentSkipListSet<String> allImagePaths = TourDatabase.getCachedFields_AllPhotoImageFilePath();

      for (final String imagePath : allImagePaths) {

         _comboLinux1.add(imagePath);
         _comboLinux2.add(imagePath);
         _comboLinux3.add(imagePath);

         _comboWindows1.add(imagePath);
         _comboWindows2.add(imagePath);
         _comboWindows3.add(imagePath);
      }
   }

   @Override
   public void init(final IWorkbench workbench) {

   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());
      _defaultModifyListener = modifyEvent -> onModify();
   }

   private void onModify() {

      if (_isInUpdate) {
         return;
      }

      _isModified = true;

      enableControls();
   }

   @Override
   public boolean performOk() {

      savedState();

      return true;
   }

   private void restoreState() {

      _isInUpdate = true;

// SET_FORMATTING_OFF

      _chkConversion1         .setSelection(_state.getBoolean(STATE_IS_CONVERSION_1));
      _chkConversion2         .setSelection(_state.getBoolean(STATE_IS_CONVERSION_2));
      _chkConversion3         .setSelection(_state.getBoolean(STATE_IS_CONVERSION_3));

      _comboLinux1            .setText(Util.getStateString(_state, STATE_CONVERSION_LINUX_1, UI.EMPTY_STRING));
      _comboLinux2            .setText(Util.getStateString(_state, STATE_CONVERSION_LINUX_2, UI.EMPTY_STRING));
      _comboLinux3            .setText(Util.getStateString(_state, STATE_CONVERSION_LINUX_3, UI.EMPTY_STRING));

      _comboWindows1          .setText(Util.getStateString(_state, STATE_CONVERSION_WINDOWS_1, UI.EMPTY_STRING));
      _comboWindows2          .setText(Util.getStateString(_state, STATE_CONVERSION_WINDOWS_2, UI.EMPTY_STRING));
      _comboWindows3          .setText(Util.getStateString(_state, STATE_CONVERSION_WINDOWS_3, UI.EMPTY_STRING));

      _autoComplete_Linux1    .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_1);
      _autoComplete_Linux2    .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_2);
      _autoComplete_Linux3    .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_3);

      _autoComplete_Windows1  .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_1);
      _autoComplete_Windows2  .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_2);
      _autoComplete_Windows3  .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_3);

// SET_FORMATTING_ON

      _isInUpdate = false;
   }

   private void savedState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_CONVERSION_1,         _chkConversion1.getSelection());
      _state.put(STATE_IS_CONVERSION_2,         _chkConversion2.getSelection());
      _state.put(STATE_IS_CONVERSION_3,         _chkConversion3.getSelection());

      _state.put(STATE_CONVERSION_LINUX_1,      _comboLinux1.getText());
      _state.put(STATE_CONVERSION_LINUX_2,      _comboLinux2.getText());
      _state.put(STATE_CONVERSION_LINUX_3,      _comboLinux3.getText());

      _state.put(STATE_CONVERSION_WINDOWS_1,    _comboWindows1.getText());
      _state.put(STATE_CONVERSION_WINDOWS_2,    _comboWindows2.getText());
      _state.put(STATE_CONVERSION_WINDOWS_3,    _comboWindows3.getText());

      _autoComplete_Linux1    .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_1);
      _autoComplete_Linux2    .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_2);
      _autoComplete_Linux3    .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_LINUX_3);

      _autoComplete_Windows1  .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_1);
      _autoComplete_Windows2  .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_2);
      _autoComplete_Windows3  .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_WINDOWS_3);

// SET_FORMATTING_ON

      if (_isModified) {

         /*
          * Discard all tours that all tour photo paths are recomputed
          */

         _isModified = false;

         TourPhotoManager.updatePathConversions();

         // cached tours are not valid any more
         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         TourManager.fireEvent(TourEventId.UPDATE_UI);
      }
   }
}
