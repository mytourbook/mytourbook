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
package net.tourbook.tour;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * @author: Wolfgang Schramm
 * 
 *          <pre>
 * created: 6th July 2007
 * </pre>
 */
public class TourEditorInput implements IEditorInput, IPersistableElement {

	private long	fTourId;
	String			fEditorTitle;

	public TourEditorInput(final long tourId) {
		fTourId = tourId;
	}

	@Override
	public boolean equals(final Object obj) {

		/*
		 * check if the tour is already open
		 */

		if (obj instanceof TourEditorInput) {

			if (((TourEditorInput) obj).fTourId == fTourId) {
				return true;
			}
		}

		return false;
	}

	public boolean exists() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(final Class adapter) {
		return null;
	}

	public String getFactoryId() {
		return TourEditorInputFactory.getFactoryId();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return "editor name"; //$NON-NLS-1$
	}

	/*
	 * this method is necessary that the editor input is saved when the editor is closed so that the
	 * editor can be restored
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return this;
	}

	public String getToolTipText() {
		return fEditorTitle;
	}

	public long getTourId() {
		return fTourId;
	}

	public void saveState(final IMemento memento) {
		TourEditorInputFactory.saveState(memento, this);
	}

}
