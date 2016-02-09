/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import net.tourbook.photo.ISelectionConverter;

import org.eclipse.jface.viewers.ISelection;

public class SyncSelection implements ISelection, ISelectionConverter {

	private ISelection	originalSelection;

	@Override
	public ISelection convertSelection(final ISelection selection) {

		originalSelection = selection;

		// ensure link view is opened
		TourPhotoManager.openLinkView();

		return this;
	}

	public ISelection getSelection() {
		return originalSelection;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public String toString() {
		return "SyncSelection [originalSelection=" + originalSelection + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
