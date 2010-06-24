/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * For this dialog box the close event is disabled, the dialog must be closed with OK or Cancel to
 * preventthat it is accidently closed, this happened very often during development of the custom
 * map provider feature
 */
public class MessageDialogNoClose extends MessageDialog {

	public MessageDialogNoClose(final Shell parentShell,
								final String dialogTitle,
								final Image dialogTitleImage,
								final String dialogMessage,
								final int dialogImageType,
								final String[] dialogButtonLabels,
								final int defaultIndex) {

		super(
				parentShell,
				dialogTitle,
				dialogTitleImage,
				dialogMessage,
				dialogImageType,
				dialogButtonLabels,
				defaultIndex);
	}

	@Override
	protected void handleShellCloseEvent() {}

}
