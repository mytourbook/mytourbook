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
package net.tourbook.ui.views.tagging;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Wizard dialog to set/remove tour tags.
 */
public class Dialog_SaveTags extends WizardDialog {

   public static final int SAVE_TAG_ACTION_APPEND_NEW_TAGS      = 0;
   public static final int SAVE_TAG_ACTION_REPLACE_TAGS         = 10;
   public static final int SAVE_TAG_ACTION_REMOVE_SELECTED_TAGS = 50;
   public static final int SAVE_TAG_ACTION_REMOVE_ALL_TAGS      = 60;

   //

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.views.tagging.Dialog_SaveTags"); //$NON-NLS-1$

   public Dialog_SaveTags(final Shell parentShell, final IWizard wizard) {

      super(parentShell, wizard);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.SaveTags).createImage());
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      final Button button = getButton(IDialogConstants.FINISH_ID);

      button.setText(Messages.App_Action_Apply);

//      // ensure the button is wide enough
//      // -> this is not OK when the text is small !!!
//
//      final GridData gd = (GridData) button.getLayoutData();
//      gd.widthHint = SWT.DEFAULT;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // use state to keep window position
      return _state;
   }

}
