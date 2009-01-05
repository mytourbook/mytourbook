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
package net.tourbook.application;

import net.tourbook.ui.ImageCombo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * Because the {@link ImageCombo} looks very bad on osx, a normal combo box is used on osx and the
 * image combo is used on win32 and linux
 */
public class TourTypeCombo {

	private static final boolean	osx	= "carbon".equals(SWT.getPlatform());	//$NON-NLS-1$

	private Composite				fContainer;

	private Combo					fTourTypeComboOSX;
	private ImageCombo				fTourTypeCombo;

	TourTypeCombo(final Composite parent, final int style) {

		if (osx) {

			fTourTypeComboOSX = new Combo(parent, style);

		} else {

			/*
			 * wrap the combo into a container to align it vertically to the center
			 */
			fContainer = new Composite(parent, SWT.NONE);
			final GridLayout gl = new GridLayout();
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			gl.horizontalSpacing = 0;
			gl.verticalSpacing = 0;
			fContainer.setLayout(gl);

			fTourTypeCombo = new ImageCombo(fContainer, style);
			fTourTypeCombo.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		}
	}

	void add(final String filterName, final Image filterImage) {
		if (osx) {
			fTourTypeComboOSX.add(filterName);
		} else {
			fTourTypeCombo.add(filterName, filterImage);
		}
	}

	void addDisposeListener(final DisposeListener disposeListener) {
		if (osx) {
			fTourTypeComboOSX.addDisposeListener(disposeListener);
		} else {
			fTourTypeCombo.addDisposeListener(disposeListener);
		}
	}

	void addSelectionListener(final SelectionListener selectionListener) {
		if (osx) {
			fTourTypeComboOSX.addSelectionListener(selectionListener);
		} else {
			fTourTypeCombo.addSelectionListener(selectionListener);
		}
	}

	int getSelectionIndex() {
		if (osx) {
			return fTourTypeComboOSX.getSelectionIndex();
		} else {
			return fTourTypeCombo.getSelectionIndex();
		}
	}

	void removeAll() {
		if (osx) {
			fTourTypeComboOSX.removeAll();
		} else {
			fTourTypeCombo.removeAll();
		}
	}

	void select(final int index) {
		if (osx) {
			fTourTypeComboOSX.select(index);
		} else {
			fTourTypeCombo.select(index);
		}
	}

	void setLayoutData(final GridData gridData) {
		if (osx) {
			fTourTypeComboOSX.setLayoutData(gridData);
		} else {
			fTourTypeCombo.setLayoutData(gridData);
		}
	}

	void setToolTipText(final String tooltip) {
		if (osx) {
			fTourTypeComboOSX.setToolTipText(tooltip);
		} else {
			fTourTypeCombo.setToolTipText(tooltip);
		}
	}

	void setVisibleItemCount(final int count) {
		if (osx) {
			fTourTypeComboOSX.setVisibleItemCount(count);
		} else {
			fTourTypeCombo.setVisibleItemCount(count);
		}
	}

	Composite getControl() {
		if (osx) {
			return fTourTypeComboOSX;
		} else {
			return fContainer;
		}
	}

}
