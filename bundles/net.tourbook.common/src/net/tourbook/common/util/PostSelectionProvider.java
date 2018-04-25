/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class PostSelectionProvider implements IPostSelectionProvider {

	private ListenerList	_postSelectionListeners	= new ListenerList();

	private ISelection		_currentSelection;

	private String			_name;

	@SuppressWarnings("unused")
	private PostSelectionProvider() {}

	public PostSelectionProvider(final String name) {
		_name = name;
	}

	/**
	 * Fires a post selection for the current view.
	 * 
	 * @param selection
	 * @return Returns <code>true</code> when the selection is fired, otherwise <code>false</code>.
	 */
	public static boolean fireSelection(final ISelection selection) {

		final IWorkbenchWindow wbWin = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (wbWin != null) {

			final IWorkbenchPage wbWinPage = wbWin.getActivePage();
			if (wbWinPage != null) {

				final IWorkbenchPart wbWinPagePart = wbWinPage.getActivePart();
				if (wbWinPagePart != null) {

					final IWorkbenchPartSite site = wbWinPagePart.getSite();
					if (site != null) {

						final ISelectionProvider selectionProvider = site.getSelectionProvider();
						if (selectionProvider instanceof PostSelectionProvider) {

							/*
							 * restore view state when this view is maximized
							 */
							final IWorkbenchPartReference activePartReference = wbWinPage.getActivePartReference();
							final int partState = wbWinPage.getPartState(activePartReference);

							if (partState != IWorkbenchPage.STATE_RESTORED) {
								wbWinPage.setPartState(activePartReference, IWorkbenchPage.STATE_RESTORED);
							}

							// fire selection
							((PostSelectionProvider) selectionProvider).setSelection(selection);

							return true;
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
		_postSelectionListeners.add(listener);
	}

	@Override
	public void addSelectionChangedListener(final ISelectionChangedListener listener) {}

	/**
	 * Clears the current selection in the selection provider
	 */
	public void clearSelection() {
		_currentSelection = null;
	}

	@Override
	public ISelection getSelection() {
		return _currentSelection;
	}

	@Override
	public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
		_postSelectionListeners.remove(listener);
	}

	@Override
	public void removeSelectionChangedListener(final ISelectionChangedListener listener) {}

	@Override
	public void setSelection(final ISelection selection) {

		if (selection == null) {
			return;
		}

		_currentSelection = selection;

		final SelectionChangedEvent event = new SelectionChangedEvent(this, _currentSelection);

		final Object[] listeners = _postSelectionListeners.getListeners();

		for (final Object listener : listeners) {
			final ISelectionChangedListener l = (ISelectionChangedListener) listener;
			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					l.selectionChanged(event);
				}
			});
		}

		/**
		 * Remove selection that it is not refired when a part is activated -> this causes lot of
		 * confusion
		 * <p>
		 * This may be a drastic change but the future tests will tell if it works, may be not 100%.
		 */
		_currentSelection = null;
	}

	public void setSelectionNoFireEvent(final ISelection selection) {

		if (selection == null) {
			return;
		}

		_currentSelection = selection;
	}

	@Override
	public String toString() {
		return "PostSelectionProvider [_name=" + _name + "\t_currentSelection=" + _currentSelection + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
