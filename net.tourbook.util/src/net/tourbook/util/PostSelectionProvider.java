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
package net.tourbook.util;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class PostSelectionProvider implements IPostSelectionProvider {

	private ListenerList	_postSelectionListeners	= new ListenerList();
	private ISelection		_currentSelection;

	public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
		_postSelectionListeners.add(listener);
	}

	public void addSelectionChangedListener(final ISelectionChangedListener listener) {}

	/**
	 * Clears the current selection in the selection provider
	 */
	public void clearSelection() {
		_currentSelection = null;
	}

	public ISelection getSelection() {
		return _currentSelection;
	}

	public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
		_postSelectionListeners.remove(listener);
	}

	public void removeSelectionChangedListener(final ISelectionChangedListener listener) {}

	public void setSelection(final ISelection selection) {

		if (selection == null) {
			return;
		}

		_currentSelection = selection;

		final SelectionChangedEvent event = new SelectionChangedEvent(this, _currentSelection);

		final Object[] listeners = _postSelectionListeners.getListeners();

		for (int i = 0; i < listeners.length; ++i) {
			final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					l.selectionChanged(event);
				}
			});
		}
	}
}
