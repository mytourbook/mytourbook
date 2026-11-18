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
package net.tourbook.database;

import de.byteholder.geoclipse.preferences.IMappingPreferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.font.MTFont;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogCompressH2Database extends Dialog {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.database.DialogCompressH2Database");//$NON-NLS-1$

   /*
    * UI controls
    */
   private Text   _txtLog;
   private Font   _monoFont;

   private Button _btnCompress;

   private Shell  _dialogShell;

   public DialogCompressH2Database() {

      super(Display.getDefault().getActiveShell());

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      _dialogShell = shell;

      // set window title
      shell.setText(Messages.App_Db_Compress_DialogTitle);

      shell.addDisposeListener(e -> {

         if (_monoFont != null) {
            _monoFont.dispose();
         }
      });
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      {
         /*
          * Button: Inplace compress
          */
         _btnCompress = createButton(
               parent,
               IDialogConstants.CLIENT_ID + 2,
               Messages.App_Db_Compress_Button_CompressInplace,
               false);
         _btnCompress.setToolTipText(Messages.App_Db_Compress_Button_CompressInplace_Tooltip);

         _btnCompress.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onCompress(true);
            }
         });
      }

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      final Control ui = createUI(parent);

      updateUI_ShowInitialDbSize();

      return ui;
   }

   private Control createUI(final Composite parent) {

      _txtLog = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
      _txtLog.setFont(_monoFont);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtLog);

      return _txtLog;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private void initUI(final Composite parent) {

      final IPreferenceStore prefStore = TourbookPlugin.getPrefStore();

      final String loggingFontPrefs = prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
      if (loggingFontPrefs.length() > 0) {
         try {
            _monoFont = new Font(parent.getDisplay(), new FontData(loggingFontPrefs));
         } catch (final Exception e) {
            // ignore
         }
      }

      if (_monoFont == null) {
         _monoFont = new Font(parent.getDisplay(), MTFont.DEFAULT_MONO_FONT, 9, SWT.NORMAL);
      }
   }

   private boolean isConfirmCompress() {

      final MessageDialog messageDialog = new MessageDialog(getShell(),

            Messages.App_Db_Compress_DialogTitle,
            null,

            NLS.bind(Messages.App_Db_Compress_Dialog_ConfirmCompress_Message, TourDatabase.H2_DB_FILEPATH),

            MessageDialog.QUESTION,

            // define buttons
            new String[] {
                  Messages.App_Db_Compress_Button_CompressDatabase,
                  IDialogConstants.NO_LABEL },

            // default button index
            1);

      return messageDialog.open() == Window.OK;
   }

   private void onCompress(final boolean isInplace) {

      if (isConfirmCompress() == false) {
         return;
      }

      // disable compress buttons that it cannot be run twice -> db sizes would be wrong when running more than once
      _btnCompress.setEnabled(false);
   }

   private void updateUI_ShowInitialDbSize() {

      _dialogShell.getDisplay().asyncExec(() -> {

         final String logText = H2StorageTools.inspectAll();

         _txtLog.setText(logText);

         final Point dialogSize = _dialogShell.getSize();

         // MUST be run async otherwise it has the wrong location
         // resize dialog to show all data
         final Point defaultSize = _dialogShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

         // keep height
         dialogSize.x = defaultSize.x;

         _dialogShell.setSize(dialogSize);
      });

   }
}
