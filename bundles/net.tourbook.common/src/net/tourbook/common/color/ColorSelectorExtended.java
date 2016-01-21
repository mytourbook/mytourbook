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
package net.tourbook.common.color;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.widgets.Composite;

/**
 * Extends the {@link ColorSelector} with an open & close listener, which is fired when the dialog
 * is opened or closes.
 * <p>
 * This can be used to keep parent dialog opened when the color selector dialog is opened.
 */
public class ColorSelectorExtended extends ColorSelector {

	private final ListenerList	_openListeners	= new ListenerList();

	public ColorSelectorExtended(final Composite parent) {
		super(parent);
	}

	public void addOpenListener(final IColorSelectorListener listener) {
		_openListeners.add(listener);
	}

	/**
	 * Fire an open event that the dialog is opened or closes.
	 */
	private void fireOpenEvent(final boolean isOpened) {

		final Object[] listeners = _openListeners.getListeners();

		for (final Object listener : listeners) {

			final IColorSelectorListener colorSelectorListener = (IColorSelectorListener) listener;

			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					colorSelectorListener.colorDialogOpened(isOpened);
				}
			});
		}
	}

	@Override
	public void open() {

		fireOpenEvent(true);

		super.open();

		fireOpenEvent(false);
	}

	public void removeOpenListener(final IColorSelectorListener listener) {
		_openListeners.remove(listener);
	}
}
