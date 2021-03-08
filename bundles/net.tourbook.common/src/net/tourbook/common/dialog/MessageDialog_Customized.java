/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.common.dialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * Customized {@link MessageDialog} which can be set on top of other windows. This is needed when
 * the message dialog is opened in a slideout to ensure that it is not hidden.
 */
public class MessageDialog_Customized extends MessageDialog {

   public MessageDialog_Customized(final Shell parentShell,
                                   final String dialogTitle,
                                   final Image dialogTitleImage,
                                   final String dialogMessage,
                                   final int dialogImageType,
                                   final int defaultIndex,
                                   final String... dialogButtonLabels) {

      super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, defaultIndex, dialogButtonLabels);
   }

   /**
    * Add shell style {@link SWT#ON_TOP}
    *
    * @return
    */
   public MessageDialog_Customized withStyleOnTop() {

      setShellStyle(getShellStyle() | SWT.ON_TOP);

      return this;
   }
}
