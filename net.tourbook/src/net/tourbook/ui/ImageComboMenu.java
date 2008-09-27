/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.awt.Menu;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

/**
 * Idea: this control should be a combo box with images but implemented with a {@link Menu} widget
 */
public class ImageComboMenu extends Composite {

	public ImageComboMenu(final Composite parent, final int style) {
		super(parent, style);
	}

	public void add(final String name, final Image image) {
	// TODO Auto-generated method stub

	}

	public void addSelectionListener(final SelectionListener listener) {
	// TODO Auto-generated method stub

	}

	public void removeAll() {
	// TODO Auto-generated method stub

	}

	public void select(final int index) {
	// TODO Auto-generated method stub

	}

	public void setVisibleItemCount(final int count) {
	// TODO Auto-generated method stub

	}

}
