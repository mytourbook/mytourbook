/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

	ListenerList		postSelectionListeners	= new ListenerList();
	private ISelection	currentSelection;

	public void setSelection(ISelection selection) {

		currentSelection = selection;

		final SelectionChangedEvent event = new SelectionChangedEvent(
				this,
				currentSelection);

		Object[] listeners = postSelectionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					l.selectionChanged(event);
				}
			});
		}
	}

	public ISelection getSelection() {
		return currentSelection;
	}

	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		postSelectionListeners.add(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		postSelectionListeners.remove(listener);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {}
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
}
