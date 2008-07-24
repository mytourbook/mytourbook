/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class MessageDialogPage extends DialogPage {

	MessageRegion	fMessageRegion;

	public MessageDialogPage(final Composite parent) {
		createControl(parent);
	}

	public void createControl(final Composite parent) {
		final Composite composite1 = new Composite(parent, SWT.NONE);

		// cumtomized - begin
		composite1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// customized - end

		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite1.setLayout(layout);
		fMessageRegion = new MessageRegion();
		fMessageRegion.createContents(composite1);
		final GridData messageData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fMessageRegion.setMessageLayoutData(messageData);
		setControl(composite1);
	}

	@Override
	public void setMessage(final String newMessage, final int newType) {
		super.setMessage(newMessage, newType);
		fMessageRegion.updateText(newMessage, newType);
	}

	@Override
	public void setErrorMessage(final String newMessage) {
		super.setErrorMessage(newMessage);
		fMessageRegion.updateText(newMessage, IMessageProvider.ERROR);
	}
}
