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
package net.tourbook.tour;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

/**
 * selection is fired when a tour editor get's the focus
 */
public class SelectionActiveEditor implements ISelection {

	private final IEditorPart	fEditorPart;

	public SelectionActiveEditor(final IEditorPart activeEditor) {
		fEditorPart = activeEditor;
	}

	public IEditorPart getEditor() {
		return fEditorPart;
	}

	public boolean isEmpty() {
		return false;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[SelectionActiveEditor] ");//$NON-NLS-1$

		if (fEditorPart instanceof TourEditor) {
			final TourEditor tourEditor = (TourEditor) fEditorPart;
			sb.append(tourEditor);
		}

		return sb.toString();
	}

	
}
