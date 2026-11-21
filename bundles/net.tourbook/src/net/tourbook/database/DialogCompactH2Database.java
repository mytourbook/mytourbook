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

import java.io.IOException;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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
import org.eclipse.ui.PlatformUI;

public class DialogCompactH2Database extends Dialog {

   private static final IDialogSettings  _state     = TourbookPlugin.getState("net.tourbook.database.DialogCompactH2Database");//$NON-NLS-1$
   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * UI controls
    */
   private Text   _txtLog;
   private Font   _monoFont;

   private Button _btnCompact;

   private Shell  _dialogShell;

   public DialogCompactH2Database() {

      super(Display.getDefault().getActiveShell());

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      _dialogShell = shell;

      // set window title
      shell.setText(Messages.Dialog_CompactDb_Title);

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
          * Button: Compact
          */
         _btnCompact = createButton(
               parent,
               IDialogConstants.CLIENT_ID + 2,
               Messages.Dialog_CompactDb_Button_Compact,
               false);
         _btnCompact.setToolTipText(Messages.Dialog_CompactDb_Button_Compact_Tooltip);

         _btnCompact.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onCompact(true);
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

   private boolean isConfirmCompact() {

      final MessageDialog messageDialog = new MessageDialog(getShell(),

            Messages.Dialog_CompactDb_Title,
            null,

            NLS.bind(Messages.Dialog_CompactDb_Message_BackupWarning, TourDatabase.H2_DB_FOLDER_PATH),

            MessageDialog.QUESTION,

            1, // default button index

            // set button labels
            Messages.Dialog_CompactDb_Action_Compact,
            Messages.App_Action_Cancel);

      return messageDialog.open() == Window.OK;
   }

   private void onCompact(final boolean isInplace) {

      if (isConfirmCompact() == false) {
         return;
      }

      // disable compact buttons that it cannot be run twice -> db sizes would be wrong when running more than once
      _btnCompact.setEnabled(false);

      final Display display = Display.getDefault();

      if (new MessageDialog(display.getActiveShell(),

            Messages.Dialog_CompactDb_Title,
            null, // no title image

            Messages.Dialog_CompactDb_Message_CompactAndRestart,
            MessageDialog.QUESTION,

            1, // default index

            // define buttons
            Messages.Dialog_CompactDb_Action_RestartAndCompact,
            Messages.App_Action_Cancel

      ).open() == IDialogConstants.OK_ID) {

         // set flag that the h2 should be compacted
         _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_RUN_TOOL_COMPACT_H2, true);

         // force save pref store to file
         if (_prefStore instanceof final IPersistentPreferenceStore prefStoreWithSave) {
            try {
               prefStoreWithSave.save();
            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
         display.asyncExec(() -> PlatformUI.getWorkbench().restart());
      }
   }

   private void updateUI_ShowInitialDbSize() {

      final Display display = _dialogShell.getDisplay();

      display.asyncExec(() -> {

         BusyIndicator.showWhile(display, () -> {

            final String logText = H2CompactDatabase.inspectAll();

            _txtLog.setText(logText);
         });

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
