/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.search.FTSearchManager;
import net.tourbook.tour.TourManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class ActionHandler_Database_DeleteFulltextIndexFiles extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return null;
      }

      final Display display = Display.getDefault();

      if (new MessageDialog(display.getActiveShell(),

            Messages.Dialog_Fulltext_DeleteFulltextIndexFiles_Title,
            null, // no title image

            Messages.Dialog_Fulltext_DeleteFulltextIndexFiles_Message,
            MessageDialog.QUESTION,

            1, // default index

            Messages.Dialog_Fulltext_Action_DeleteFulltextIndexFiles,
            Messages.App_Action_Cancel

      ).open() == IDialogConstants.OK_ID) {

         FTSearchManager.deleteCorruptIndex();

         display.asyncExec(() -> PlatformUI.getWorkbench().restart());
      }

      return null;
   }

}
