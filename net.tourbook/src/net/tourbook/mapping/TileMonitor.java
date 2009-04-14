/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class TileMonitor extends ContributionItem {

	private Label	fLblWorked;
	private Label	fLblAllWork;

	@Override
	public void fill(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
		{
			/*
			 * label: loading tiles
			 */
			label = new Label(container, SWT.NONE);
			label.setText("Get Tiles:");//$NON-NLS-1$

			/*
			 * label: work
			 */
			fLblWorked = new Label(container, SWT.TRAIL);
			fLblWorked.setText("33");
 
			/*
			 * label: separator
			 */
			label = new Label(container, SWT.NONE);
			label.setText("/");//$NON-NLS-1$

			/*
			 * label: all work
			 */
			fLblAllWork = new Label(container, SWT.LEAD);
			fLblAllWork.setText("99");
		}

//		final StatusLineLayoutData statusLineLayoutData = new StatusLineLayoutData();
//		container.setLayoutData(statusLineLayoutData);
//
//		final int widthHint = 30;
//		final int heightHint = 30;
//		statusLineLayoutData.widthHint = widthHint;
//		statusLineLayoutData.heightHint = heightHint;
		
	}

}
