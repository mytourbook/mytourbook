/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * This is a template for a title area dialog
 */
public class DialogTemplate extends TitleAreaDialog {

   private static final String    ID         = "net.tourbook.ui.DialogTemplate"; //$NON-NLS-1$

   private final IDialogSettings  _state     = TourbookPlugin.getState(ID);
   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   public DialogTemplate(final Shell parentShell) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

//      shell.setText(Messages.Dialog_JoinTours_DlgArea_Title);

   }

   @Override
   public void create() {

      super.create();

//      setTitle(Messages.Dialog_JoinTours_DlgArea_Title);
//      setMessage(Messages.Dialog_JoinTours_DlgArea_Message);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */

   private void createUI(final Composite parent) {

      final Composite dlgContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
      GridLayoutFactory.swtDefaults().numColumns(3).spacing(10, 8).applyTo(dlgContainer);
//      dlgContainer(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
//
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
//      return _state;
      return null;
   }

   @Override
   protected void okPressed() {

      saveState();

      super.okPressed();
   }

   private void restoreState() {

   }

   private void saveState() {

   }
}
