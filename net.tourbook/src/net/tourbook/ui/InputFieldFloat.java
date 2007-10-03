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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * input component to enter floating numbers
 */
public class InputFieldFloat {

	private Text	fText;

	public InputFieldFloat(Composite container, String label, int width) {

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText(label);

		fText = new Text(container, SWT.BORDER | SWT.TRAIL);

		GridData gd = new GridData();
		gd.widthHint = width;
		fText.setLayoutData(gd);
	}

	public Text getTextField() {
		return fText;
	}

}
