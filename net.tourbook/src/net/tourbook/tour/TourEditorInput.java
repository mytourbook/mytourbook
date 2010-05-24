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
package net.tourbook.tour;

import net.tourbook.ui.UI;

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

	private long	_tourId;

	String			editorTitle	= UI.EMPTY_STRING;

	public TourEditorInput(final long tourId) {
		_tourId = tourId;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourEditorInput)) {
			return false;
		}
		final TourEditorInput other = (TourEditorInput) obj;
		if (_tourId != other._tourId) {
			return false;
		}
		return true;
	}

	public boolean exists() {
		/*
		 * !!! requires true to save the editors state, took hours to figure this out !!!
		 */
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
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
		return editorTitle;
	}

	public long getTourId() {
		return _tourId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_tourId ^ (_tourId >>> 32));
		return result;
	}

	public void saveState(final IMemento memento) {
		TourEditorInputFactory.saveState(memento, this);
	}

}
